package org.burgeon.legolas.common.handler.socks;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.SocksPortUnificationServerHandler;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

/**
 * @author Sam Lu
 * @date 2022/4/4
 */
@AllArgsConstructor
public class Socks5Initializer extends ChannelInitializer<SocketChannel> {

    private SimpleChannelInboundHandler<SocksMessage> processHandler;

    @SneakyThrows
    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline().addLast(
                new SocksPortUnificationServerHandler(),
                processHandler);
    }

}
