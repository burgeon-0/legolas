package org.burgeon.legolas.ps.server.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.SneakyThrows;
import org.burgeon.legolas.ps.server.ProxyServer;
import org.springframework.stereotype.Component;

/**
 * HTTP 代理服务器
 *
 * @author Sam Lu
 * @date 2022/3/28
 */
@Component
public class HttpProxyServer implements ProxyServer {

    private String host = "localhost";
    private int port = 8081;

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
                            ChannelPipeline channelPipeline = socketChannel.pipeline();
                            channelPipeline.addLast(new HttpServerCodec());
                            channelPipeline.addLast(new HttpProxyServerHandler());
                        }
                    });

            ChannelFuture channelFuture = serverBootstrap.bind(host, port).sync();
            channelFuture.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    @SneakyThrows
    @Override
    public void stop() {
        workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();
    }

}
