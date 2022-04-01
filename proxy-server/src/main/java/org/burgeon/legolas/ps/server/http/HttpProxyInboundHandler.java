package org.burgeon.legolas.ps.server.http;

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
import lombok.extern.slf4j.Slf4j;

import java.net.URL;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

/**
 * @author Sam Lu
 * @date 2022/3/30
 */
@Slf4j
public class HttpProxyInboundHandler extends ChannelInboundHandlerAdapter {

    @SneakyThrows
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HttpRequest) {
            DefaultHttpRequest request = (DefaultHttpRequest) msg;

            try {
                HttpScheme httpScheme = getHttpScheme(request);
                URL url = getUrl(httpScheme, request);
                log.info("{} {}", request.method(), url);

                Promise<Channel> promise = createPromise(ctx, url.getHost(), url.getPort());
                if (HttpScheme.HTTP.equals(httpScheme)) {
                    forwardHttpRequest(ctx, promise, request);
                } else {
                    forwardHttpsRequest(ctx, promise, request);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                fail(ctx, request, e.toString());
            }
        }
    }

    private HttpScheme getHttpScheme(HttpRequest request) {
        if (HttpMethod.CONNECT.equals(request.method())) {
            return HttpScheme.HTTPS;
        } else {
            return HttpScheme.HTTP;
        }
    }

    @SneakyThrows
    private URL getUrl(HttpScheme httpScheme, HttpRequest request) {
        if (request.uri().contains(StrUtil.format("{}://", httpScheme.name()))) {
            return new URL(request.uri());
        }
        if (request.uri().contains(request.headers().get(HOST))) {
            return new URL(StrUtil.format("{}://{}", httpScheme.name(), request.uri()));
        }
        return new URL(StrUtil.format("{}://{}{}", httpScheme.name(),
                request.headers().get(HOST), request.uri()));
    }

    private Promise<Channel> createPromise(ChannelHandlerContext ctx, String host, int port) {
        Promise<Channel> promise = ctx.executor().newPromise();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(ctx.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .remoteAddress(host, port)
                .handler(new ForwardInboundHandler(ctx.channel()))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
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

    private void forwardHttpRequest(ChannelHandlerContext ctx, Promise<Channel> promise, HttpRequest request) {
        EmbeddedChannel embeddedChannel = new EmbeddedChannel(new HttpRequestEncoder());
        embeddedChannel.writeOutbound(request);
        Object object = embeddedChannel.readOutbound();

        promise.addListener((FutureListener<Channel>) channelFuture -> {
            DefaultChannelPipeline channelPipeline = (DefaultChannelPipeline) ctx.pipeline();
            channelPipeline.removeIfExists(HttpServerCodec.class);
            channelPipeline.removeIfExists(HttpProxyInboundHandler.class);
            channelPipeline.addLast(new ForwardInboundHandler(channelFuture.getNow()));
            channelFuture.get().writeAndFlush(object);
        });
    }

    private void forwardHttpsRequest(ChannelHandlerContext ctx, Promise<Channel> promise, HttpRequest request) {
        FullHttpResponse response = new DefaultFullHttpResponse(request.protocolVersion(), OK);

        promise.addListener((FutureListener<Channel>) channelFuture -> {
            ctx.writeAndFlush(response).addListener((ChannelFutureListener) future -> {
                DefaultChannelPipeline channelPipeline = (DefaultChannelPipeline) ctx.pipeline();
                channelPipeline.removeIfExists(HttpServerCodec.class);
                channelPipeline.removeIfExists(HttpProxyInboundHandler.class);
            });
            ChannelPipeline channelPipeline = ctx.pipeline();
            channelPipeline.addLast(new ForwardInboundHandler(channelFuture.getNow()));
        });
    }

    private void fail(ChannelHandlerContext ctx, HttpRequest request, String result) {
        FullHttpResponse response = new DefaultFullHttpResponse(request.protocolVersion(), BAD_REQUEST,
                Unpooled.wrappedBuffer(result.getBytes()));
        response.headers().set(CONTENT_TYPE, ContentType.TEXT_HTML);
        response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
        response.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        ctx.write(response);
        ctx.flush();
    }

    @SneakyThrows
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @SneakyThrows
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }

}
