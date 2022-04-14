package org.burgeon.legolas.ps;

import cn.hutool.core.thread.ThreadUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.burgeon.legolas.ps.server.socks.Socks5ProxyServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

/**
 * @author Sam Lu
 * @date 2022/3/26
 */
@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
public class Application {

    private final Socks5ProxyServer socks5ProxyServer;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @PostConstruct
    public void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Server stopping...");
            socks5ProxyServer.stop();
        }));
        ThreadUtil.execute(socks5ProxyServer::start);
    }

}
