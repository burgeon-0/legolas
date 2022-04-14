package org.burgeon.legolas.ps.server.socks;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.SocksVersion;
import io.netty.handler.codec.socksx.v5.*;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.burgeon.legolas.common.handler.socks.Socks5ConnectHandler;
import org.burgeon.legolas.common.util.EncryptUtil;
import org.burgeon.legolas.common.util.NettySocksUtil;

/**
 * @author Sam Lu
 * @date 2022/4/3
 */
@Slf4j
@AllArgsConstructor
public class Socks5ProxyServerHandler extends SimpleChannelInboundHandler<SocksMessage> {

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
            ctx.write(new DefaultSocks5InitialResponse(Socks5AuthMethod.PASSWORD));
        } else if (msg instanceof Socks5PasswordAuthRequest) {
            log.info("Receive Socks5PasswordAuthRequest: {}", msg);
            Socks5PasswordAuthRequest socks5PasswordAuthRequest = (Socks5PasswordAuthRequest) msg;
            String username = socks5PasswordAuthRequest.username();
            String password = socks5PasswordAuthRequest.password();
            ctx.pipeline().addFirst(new Socks5CommandRequestDecoder());
            if (EncryptUtil.sha1(username, secret).equals(password)) {
                ctx.write(new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.SUCCESS));
            } else {
                ctx.write(new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.FAILURE));
            }
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
