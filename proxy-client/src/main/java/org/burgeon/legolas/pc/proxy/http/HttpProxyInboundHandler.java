package org.burgeon.legolas.pc.proxy.http;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpScheme;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.util.concurrent.Promise;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.burgeon.legolas.common.util.NettyHttpUtil;

import java.net.URL;

/**
 * @author Sam Lu
 * @date 2022/3/30
 */
@Slf4j
@AllArgsConstructor
public class HttpProxyInboundHandler extends ChannelInboundHandlerAdapter {

    private int timeout;

    @SneakyThrows
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HttpRequest) {
            DefaultHttpRequest request = (DefaultHttpRequest) msg;

            try {
                HttpScheme httpScheme = NettyHttpUtil.getHttpScheme(request);
                URL url = NettyHttpUtil.getRequestUrl(request, httpScheme);
                log.info("{} {}", request.method(), url);

                Promise<Channel> promise = NettyHttpUtil.createPromise(ctx, url.getHost(),
                        NettyHttpUtil.getRequestPort(httpScheme, url), timeout);
                if (HttpScheme.HTTP.equals(httpScheme)) {
                    NettyHttpUtil.forwardHttpRequest(ctx, promise, request,
                            new Class[]{HttpServerCodec.class, HttpProxyInboundHandler.class});
                } else {
                    NettyHttpUtil.forwardHttpsRequest(ctx, promise, request,
                            new Class[]{HttpServerCodec.class, HttpProxyInboundHandler.class});
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
