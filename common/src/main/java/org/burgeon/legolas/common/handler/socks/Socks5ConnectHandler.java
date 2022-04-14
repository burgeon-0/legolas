package org.burgeon.legolas.common.handler.socks;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandRequest;
import io.netty.util.concurrent.Promise;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.burgeon.legolas.common.util.NettySocksUtil;

import java.util.Arrays;
import java.util.List;

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
        List<Class<? extends ChannelHandler>> classes = Arrays.asList(Socks5ConnectHandler.class);
        Promise<Channel> promise = NettySocksUtil.createPromise(ctx, msg, classes);
        NettySocksUtil.connectDst(ctx, msg, bootstrap, promise, timeout);
    }

    @SneakyThrows
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error(cause.getMessage(), cause);
        NettySocksUtil.closeOnFlush(ctx.channel());
    }

}
