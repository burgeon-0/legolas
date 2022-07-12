package org.bg181.legolas.ps.common.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import lombok.SneakyThrows;
import org.bg181.legolas.ps.common.handler.http.ForwardHandler;

import java.net.URL;
import java.util.List;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

/**
 * @author Sam Lu
 * @date 2022/4/2
 */
public class NettyHttpUtil {

    public static HttpScheme getHttpScheme(HttpRequest request) {
        if (HttpMethod.CONNECT.equals(request.method())) {
            return HttpScheme.HTTPS;
        } else {
            return HttpScheme.HTTP;
        }
    }

    @SneakyThrows
    public static URL getRequestUrl(HttpRequest request, HttpScheme httpScheme) {
        // 例如：request.getUri() == http://www.gstatic.com/generate_204
        String schemeTemplate = "{}://";
        if (request.uri().contains(StrUtil.format(schemeTemplate, httpScheme.name()))) {
            return new URL(request.uri());
        }
        // 例如：request.getUri() == dss1.bdstatic.com:443
        String urlTemplate1 = "{}://{}";
        if (request.uri().contains(request.headers().get(HOST))) {
            return new URL(StrUtil.format(urlTemplate1, httpScheme.name(), request.uri()));
        }
        // 例如：request.getUri() == /index.html
        String urlTemplate2 = "{}://{}{}";
        return new URL(StrUtil.format(urlTemplate2, httpScheme.name(), request.headers().get(HOST), request.uri()));
    }

    public static int getRequestPort(HttpScheme httpScheme, URL url) {
        return url.getPort() != -1 ? url.getPort() : httpScheme.port();
    }

    public static Promise<Channel> createPromise(ChannelHandlerContext ctx,
                                                 String host,
                                                 int port,
                                                 int timeout) {
        Promise<Channel> promise = ctx.executor().newPromise();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(ctx.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .remoteAddress(host, port)
                .handler(new ForwardHandler(ctx.channel()))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout)
                .connect()
                .addListener((ChannelFutureListener) channelFuture -> {
                    if (channelFuture.isSuccess()) {
                        promise.setSuccess(channelFuture.channel());
                    } else {
                        ctx.close();
                        channelFuture.cancel(true);
                    }
                });
        return promise;
    }

    public static void forwardHttpRequest(ChannelHandlerContext ctx,
                                          Promise<Channel> promise,
                                          HttpRequest request,
                                          List<Class<? extends ChannelHandler>> handlerClasses) {
        EmbeddedChannel embeddedChannel = new EmbeddedChannel(new HttpRequestEncoder());
        embeddedChannel.writeOutbound(request);
        Object object = embeddedChannel.readOutbound();

        promise.addListener((FutureListener<Channel>) channelFuture -> {
            DefaultChannelPipeline channelPipeline = (DefaultChannelPipeline) ctx.pipeline();
            if (handlerClasses != null) {
                handlerClasses.forEach(channelPipeline::removeIfExists);
            }
            channelPipeline.addLast(new ForwardHandler(channelFuture.getNow()));
            channelFuture.get().writeAndFlush(object);
        });
    }

    public static void forwardHttpsRequest(ChannelHandlerContext ctx,
                                           Promise<Channel> promise,
                                           HttpRequest request,
                                           List<Class<? extends ChannelHandler>> handlerClasses) {
        FullHttpResponse response = new DefaultFullHttpResponse(request.protocolVersion(), OK);

        promise.addListener((FutureListener<Channel>) channelFuture -> {
            DefaultChannelPipeline channelPipeline = (DefaultChannelPipeline) ctx.pipeline();
            ctx.writeAndFlush(response).addListener((ChannelFutureListener) future -> {
                if (handlerClasses != null) {
                    handlerClasses.forEach(channelPipeline::removeIfExists);
                }
            });
            ctx.pipeline().addLast(new ForwardHandler(channelFuture.getNow()));
        });
    }

    public static void fail(ChannelHandlerContext ctx, HttpRequest request, String result) {
        fail(ctx, request.protocolVersion(), result);
    }

    public static void fail(ChannelHandlerContext ctx, HttpVersion version, String result) {
        result = result == null ? "" : result;
        FullHttpResponse response = new DefaultFullHttpResponse(version, BAD_REQUEST,
                Unpooled.wrappedBuffer(result.getBytes()));
        response.headers().set(CONTENT_TYPE, ContentType.TEXT_HTML);
        response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
        response.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        ctx.write(response);
        ctx.flush();
    }

}
