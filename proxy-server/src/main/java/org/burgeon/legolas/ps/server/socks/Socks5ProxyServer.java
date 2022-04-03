package org.burgeon.legolas.ps.server.socks;

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

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

}
