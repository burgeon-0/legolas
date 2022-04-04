package org.burgeon.legolas.pc.proxy.socks;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandRequest;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.burgeon.legolas.common.handler.DirectClientHandler;
import org.burgeon.legolas.common.handler.RelayHandler;
import org.burgeon.legolas.common.util.NettySocksUtil;

/**
 * @author Sam Lu
 * @date 2022/4/4
 */
@Slf4j
@AllArgsConstructor
public class Socks5ConnectHandler extends SimpleChannelInboundHandler<DefaultSocks5CommandRequest> {

    private int timeout;

    @SneakyThrows
    @Override
    public void channelRead0(ChannelHandlerContext ctx, DefaultSocks5CommandRequest msg) {
        Bootstrap bootstrap = new Bootstrap();
        Promise<Channel> promise = ctx.executor().newPromise();
        promise.addListener((FutureListener<Channel>) future -> {
            Channel outboundChannel = future.getNow();
            if (future.isSuccess()) {
                ChannelFuture responseFuture = ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(
                        Socks5CommandStatus.SUCCESS,
                        msg.dstAddrType(),
                        msg.dstAddr(),
                        msg.dstPort()));

                responseFuture.addListener((ChannelFutureListener) channelFuture -> {
                    ctx.pipeline().remove(Socks5ConnectHandler.this);
                    outboundChannel.pipeline().addLast(new RelayHandler(ctx.channel()));
                    ctx.pipeline().addLast(new RelayHandler(outboundChannel));
                });
            } else {
                ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE,
                        msg.dstAddrType()));
                NettySocksUtil.closeOnFlush(ctx.channel());
            }
        });

        Channel inboundChannel = ctx.channel();
        bootstrap.group(inboundChannel.eventLoop())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new DirectClientHandler(promise));

        bootstrap.connect(msg.dstAddr(), msg.dstPort()).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE,
                        msg.dstAddrType()));
                NettySocksUtil.closeOnFlush(ctx.channel());
            }
        });
    }

    @SneakyThrows
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error(cause.getMessage(), cause);
        NettySocksUtil.closeOnFlush(ctx.channel());
    }

}
