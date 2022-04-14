package org.burgeon.legolas.pc.proxy.socks;

import cn.hutool.core.util.RandomUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.socksx.v5.DefaultSocks5InitialRequest;
import io.netty.handler.codec.socksx.v5.DefaultSocks5PasswordAuthRequest;
import io.netty.handler.codec.socksx.v5.Socks5AuthMethod;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.util.ReferenceCountUtil;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.burgeon.legolas.common.util.EncryptUtil;
import org.burgeon.legolas.common.util.NettySocksUtil;

/**
 * @author Sam Lu
 * @date 2022/4/4
 */
@Slf4j
@AllArgsConstructor
public class Socks5ServerConnectHandler extends ChannelInboundHandlerAdapter {

    private ChannelFuture channelFuture;
    private Socks5CommandRequest request;
    private String secret;
    private State state;

    @SneakyThrows
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
        //ctx.writeAndFlush(new DefaultSocks5InitialRequest(Socks5AuthMethod.PASSWORD));
        //state = State.UN_AUTH;
    }

    @SneakyThrows
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        NettySocksUtil.closeOnFlush(ctx.channel());
    }

    @SneakyThrows
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (ctx.channel().isActive()) {
            switch (state) {
                case INIT:
                    channelFuture.channel().writeAndFlush(new DefaultSocks5InitialRequest(
                            Socks5AuthMethod.PASSWORD));
                    state = State.UN_AUTH;
                    break;
                case UN_AUTH:
                    String username = RandomUtil.randomString(7);
                    channelFuture.channel().writeAndFlush(new DefaultSocks5PasswordAuthRequest(
                            username, EncryptUtil.sha1(username, secret)));
                    state = State.UN_CONNECT;
                    break;
                case UN_CONNECT:
                    channelFuture.channel().writeAndFlush(request);
                    state = State.CONNECTED;
                    break;
                case CONNECTED:
                default:
                    channelFuture.channel().writeAndFlush(msg);
            }
        } else {
            ReferenceCountUtil.release(msg);
        }
    }

    enum State {
        /**
         * 初始状态
         */
        INIT,
        /**
         * 未认证
         */
        UN_AUTH,
        /**
         * 未连接
         */
        UN_CONNECT,
        /**
         * 已连接
         */
        CONNECTED
    }

}
