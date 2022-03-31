package org.burgeon.legolas.ps;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.burgeon.legolas.ps.server.http.HttpProxyServer;
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

    private final HttpProxyServer httpProxyServer;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @PostConstruct
    public void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Server stopping...");
            httpProxyServer.stop();
        }));
        httpProxyServer.start();
    }

}
