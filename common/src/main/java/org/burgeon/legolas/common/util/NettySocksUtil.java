package org.burgeon.legolas.common.util;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandRequest;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import org.burgeon.legolas.common.handler.socks.ConnectDstHandler;
import org.burgeon.legolas.common.handler.socks.RelayHandler;

import java.util.List;

/**
 * @author Sam Lu
 * @date 2022/4/4
 */
public class NettySocksUtil {

    public static Promise<Channel> createPromise(ChannelHandlerContext ctx,
                                                 DefaultSocks5CommandRequest msg,
                                                 List<Class<? extends ChannelHandler>> handlerClasses) {
        Promise<Channel> promise = ctx.executor().newPromise();
        promise.addListener((FutureListener<Channel>) future -> {
            if (future.isSuccess()) {
                ChannelFuture responseFuture = ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(
                        Socks5CommandStatus.SUCCESS,
                        msg.dstAddrType(),
                        msg.dstAddr(),
                        msg.dstPort()));

                Channel outboundChannel = future.getNow();
                responseFuture.addListener((ChannelFutureListener) channelFuture -> {
                    DefaultChannelPipeline channelPipeline = (DefaultChannelPipeline) ctx.pipeline();
                    if (handlerClasses != null) {
                        handlerClasses.forEach(channelPipeline::removeIfExists);
                    }
                    outboundChannel.pipeline().addLast(new RelayHandler(ctx.channel()));
                    ctx.pipeline().addLast(new RelayHandler(outboundChannel));
                });
            } else {
                ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE,
                        msg.dstAddrType()));
                NettySocksUtil.closeOnFlush(ctx.channel());
            }
        });
        return promise;
    }

    public static void connectDst(ChannelHandlerContext ctx,
                                  DefaultSocks5CommandRequest msg,
                                  Bootstrap bootstrap,
                                  Promise<Channel> promise,
                                  int timeout) {
        bootstrap.group(ctx.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ConnectDstHandler(promise))
                .connect(msg.dstAddr(), msg.dstPort()).addListener((ChannelFutureListener) future -> {
                    if (!future.isSuccess()) {
                        ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE,
                                msg.dstAddrType()));
                        NettySocksUtil.closeOnFlush(ctx.channel());
                    }
                });
    }

    public static void closeOnFlush(Channel ch) {
        if (ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

}
