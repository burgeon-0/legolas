package org.burgeon.legolas.server;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * @author Sam Lu
 * @date 2022/3/30
 */
@Slf4j
public class HttpServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            StringBuilder stringBuilder = new StringBuilder();

            try {
                DefaultHttpRequest request = (DefaultHttpRequest) msg;
                String uri = request.uri();
                URL url = new URL("http://www.baidu.com" + uri);
                log.info(url.toString());

                URLConnection urlConnection = url.openConnection();
                HttpURLConnection connection = (HttpURLConnection) urlConnection;
                connection.setRequestMethod("GET");
                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader bufferedReader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                    String l;
                    while ((l = bufferedReader.readLine()) != null) {
                        stringBuilder.append(l).append("\n");
                    }
                }
                connection.disconnect();
            } catch (Exception e) {
                log.error("Call remote fail!", e);
                return;
            }

            FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK,
                    Unpooled.wrappedBuffer(stringBuilder.toString().getBytes()));
            response.headers().set(CONTENT_TYPE, "text/html");
            response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
            response.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            ctx.write(response);
            ctx.flush();
        } else {
            // 这里必须加抛出异常，要不然ab测试的时候一直卡住不动，暂未解决
            throw new Exception();
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }

}
