package org.burgeon.legolas.ps.common.handler.socks;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.burgeon.legolas.ps.common.util.NettySocksUtil;

/**
 * @author Sam Lu
 * @date 2022/4/4
 */
@Slf4j
public class RelayHandler extends ChannelInboundHandlerAdapter {

    private final Channel relayChannel;

    public RelayHandler(Channel relayChannel) {
        this.relayChannel = relayChannel;
    }

    @SneakyThrows
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
    }

    @SneakyThrows
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        NettySocksUtil.closeOnFlush(ctx.channel());
    }

    @SneakyThrows
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (relayChannel.isActive()) {
            relayChannel.writeAndFlush(msg);
        } else {
            ReferenceCountUtil.release(msg);
        }
    }

    @SneakyThrows
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error(cause.getMessage(), cause);
        ctx.close();
    }

}
