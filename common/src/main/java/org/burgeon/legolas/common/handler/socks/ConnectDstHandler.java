package org.burgeon.legolas.common.handler.socks;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.Promise;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Sam Lu
 * @date 2022/4/4
 */
@Slf4j
public class ConnectDstHandler extends ChannelInboundHandlerAdapter {

    private final Promise<Channel> promise;

    public ConnectDstHandler(Promise<Channel> promise) {
        this.promise = promise;
    }

    @SneakyThrows
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.pipeline().remove(this);
        promise.setSuccess(ctx.channel());
    }

    @SneakyThrows
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error(cause.getMessage(), cause);
        promise.setFailure(cause);
    }

}
