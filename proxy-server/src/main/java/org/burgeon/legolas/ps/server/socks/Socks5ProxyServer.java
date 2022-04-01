package org.burgeon.legolas.ps.server.socks;

import org.burgeon.legolas.ps.server.ProxyServer;
import org.springframework.beans.factory.annotation.Value;

/**
 * SOCKS5 代理服务器
 *
 * @author Sam Lu
 * @date 2022/4/1
 */
public class Socks5ProxyServer implements ProxyServer {

    @Value("${socks.proxy.host:localhost}")
    private String host;
    @Value("${http.proxy.port:1080}")
    private int port;

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

}
