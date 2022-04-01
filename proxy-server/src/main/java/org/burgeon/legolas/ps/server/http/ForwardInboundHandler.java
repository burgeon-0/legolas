package org.burgeon.legolas.ps.server.http;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @author Sam Lu
 * @date 2022/3/31
 */
public class ForwardInboundHandler extends ChannelInboundHandlerAdapter {

    private Channel outChannel;

    public ForwardInboundHandler(Channel outChannel) {
        this.outChannel = outChannel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        outChannel.write(msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        outChannel.flush();
    }

}
