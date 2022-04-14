package org.burgeon.legolas.ps.server.socks;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.socksx.SocksPortUnificationServerHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.SneakyThrows;
import org.burgeon.legolas.ps.server.ProxyServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * SOCKS5 代理服务器
 *
 * @author Sam Lu
 * @date 2022/4/1
 */
@Component
public class Socks5ProxyServer implements ProxyServer {

    @Value("${socks.proxy.server.host:localhost}")
    private String host;
    @Value("${socks.proxy.server.port:11080}")
    private int port;
    @Value("${socks.proxy.secret:35b2a720-a08b-ec5e-6fd5-d09a8fc1df0c}")
    private String secret;
    @Value("${socks.proxy.connect.timeout:10000}")
    private int timeout;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    @SneakyThrows
    @Override
    public void start() {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @SneakyThrows
                        @Override
                        protected void initChannel(SocketChannel socketChannel) {
                            socketChannel.pipeline()
                                    .addLast(new SocksPortUnificationServerHandler())
                                    .addLast(new Socks5ProxyServerHandler(secret, timeout));
                        }
                    });

            serverBootstrap.bind(host, port).sync()
                    .channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    @Override
    public void stop() {
        workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();
    }

}
