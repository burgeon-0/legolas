package org.burgeon.legolas.common.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.SneakyThrows;

/**
 * @author Sam Lu
 * @date 2022/3/31
 */
public class ForwardInboundHandler extends ChannelInboundHandlerAdapter {

    private Channel outChannel;

    public ForwardInboundHandler(Channel outChannel) {
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

}
