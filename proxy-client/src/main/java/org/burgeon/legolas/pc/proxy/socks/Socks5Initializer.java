package org.burgeon.legolas.pc.proxy.socks;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.socksx.SocksPortUnificationServerHandler;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

/**
 * @author Sam Lu
 * @date 2022/4/4
 */
@AllArgsConstructor
public class Socks5Initializer extends ChannelInitializer<SocketChannel> {

    private int timeout;

    @SneakyThrows
    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline().addLast(
                new SocksPortUnificationServerHandler(),
                new Socks5ProxyHandler(timeout));
    }

}
