package org.burgeon.legolas.ps;

import cn.hutool.core.thread.ThreadUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.burgeon.legolas.ps.proxy.http.HttpProxy;
import org.burgeon.legolas.ps.proxy.socks.Socks5Proxy;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

/**
 * @author Sam Lu
 * @date 2022/4/2
 */
@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
public class Application {

    private final HttpProxy httpProxy;
    private final Socks5Proxy socks5Proxy;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @SneakyThrows
    @PostConstruct
    public void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Proxy stopping...");
            httpProxy.stop();
            socks5Proxy.stop();
        }));
        ThreadUtil.execute(httpProxy::start);
        ThreadUtil.execute(socks5Proxy::start);
    }

}
