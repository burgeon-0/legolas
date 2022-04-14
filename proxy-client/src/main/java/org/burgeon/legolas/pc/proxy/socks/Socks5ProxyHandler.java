package org.burgeon.legolas.pc.proxy.socks;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.SocksVersion;
import io.netty.handler.codec.socksx.v5.*;
import io.netty.util.concurrent.Promise;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.burgeon.legolas.common.handler.socks.ConnectDstHandler;
import org.burgeon.legolas.common.handler.socks.Socks5ConnectHandler;
import org.burgeon.legolas.common.util.NettySocksUtil;

import java.util.Arrays;
import java.util.List;

/**
 * @author Sam Lu
 * @date 2022/4/3
 */
@Slf4j
@AllArgsConstructor
public class Socks5ProxyHandler extends SimpleChannelInboundHandler<SocksMessage> {

    private String host;
    private int port;
    private String secret;
    private int timeout;

    @SneakyThrows
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SocksMessage msg) {
        if (!msg.version().equals(SocksVersion.SOCKS5)) {
            log.error("Only supports socks5 protocol!");
            ctx.writeAndFlush(Unpooled.wrappedBuffer("Protocol version illegal!".getBytes()));
            return;
        }

        if (msg instanceof Socks5InitialRequest) {
            log.info("Receive Socks5InitialRequest: {}", msg);
            ctx.pipeline().addFirst(new Socks5CommandRequestDecoder());
            ctx.write(new DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH));
        } else if (msg instanceof Socks5PasswordAuthRequest) {
            log.info("Receive Socks5PasswordAuthRequest: {}", msg);
            ctx.pipeline().addFirst(new Socks5CommandRequestDecoder());
            ctx.write(new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.SUCCESS));
        } else if (msg instanceof Socks5CommandRequest) {
            log.info("Receive Socks5CommandRequest: {}", msg);
            Socks5CommandRequest socks5CommandRequest = (Socks5CommandRequest) msg;
            if (socks5CommandRequest.type() == Socks5CommandType.CONNECT) {
                ctx.pipeline().addLast(new Socks5ConnectHandler(timeout));
                ctx.pipeline().remove(this);
                ctx.fireChannelRead(msg);
            } else {
                ctx.close();
            }
        } else {
            ctx.close();
        }
    }

    private void connectServer(ChannelHandlerContext ctx, DefaultSocks5CommandRequest msg) {
        List<Class<? extends ChannelHandler>> classes = Arrays.asList(this.getClass());
        Promise<Channel> promise = NettySocksUtil.createPromise(ctx, msg, classes);
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(ctx.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ConnectDstHandler(promise))
                .connect(host, port).addListener((ChannelFutureListener) future -> {
                    if (!future.isSuccess()) {
                        ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE,
                                msg.dstAddrType()));
                        NettySocksUtil.closeOnFlush(ctx.channel());
                    }
                });
    }

    @SneakyThrows
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @SneakyThrows
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error(cause.getMessage(), cause);
        NettySocksUtil.closeOnFlush(ctx.channel());
    }

}
