package org.bg181.legolas.ps.common.handler.http;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Sam Lu
 * @date 2022/3/31
 */
@Slf4j
public class ForwardHandler extends ChannelInboundHandlerAdapter {

    private final Channel outChannel;

    public ForwardHandler(Channel outChannel) {
        this.outChannel = outChannel;
    }

    @SneakyThrows
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        outChannel.write(msg);
    }

    @SneakyThrows
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        outChannel.flush();
    }

    @SneakyThrows
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error(cause.getMessage(), cause);
        ctx.close();
    }

}
