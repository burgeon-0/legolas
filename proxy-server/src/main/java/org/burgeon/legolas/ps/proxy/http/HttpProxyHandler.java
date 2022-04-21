package org.burgeon.legolas.ps.proxy.http;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.concurrent.Promise;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.burgeon.legolas.ps.common.util.NettyHttpUtil;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * @author Sam Lu
 * @date 2022/3/30
 */
@Slf4j
@AllArgsConstructor
public class HttpProxyHandler extends SimpleChannelInboundHandler<HttpObject> {

    private int timeout;

    @SneakyThrows
    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
        if (msg instanceof HttpRequest) {
            DefaultHttpRequest request = (DefaultHttpRequest) msg;

            try {
                HttpScheme httpScheme = NettyHttpUtil.getHttpScheme(request);
                URL url = NettyHttpUtil.getRequestUrl(request, httpScheme);
                log.info("{} {}", request.method(), url);

                Promise<Channel> promise = NettyHttpUtil.createPromise(ctx, url.getHost(),
                        NettyHttpUtil.getRequestPort(httpScheme, url), timeout);
                List<Class<? extends ChannelHandler>> classes = Arrays.asList(HttpServerCodec.class,
                        HttpProxyHandler.class);
                if (HttpScheme.HTTP.equals(httpScheme)) {
                    NettyHttpUtil.forwardHttpRequest(ctx, promise, request, classes);
                } else {
                    NettyHttpUtil.forwardHttpsRequest(ctx, promise, request, classes);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                NettyHttpUtil.fail(ctx, request, e.getMessage());
            }
        }
    }

    @SneakyThrows
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @SneakyThrows
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error(cause.getMessage(), cause);
        ctx.close();
    }

}
