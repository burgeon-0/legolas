package org.burgeon.legolas.pc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.burgeon.legolas.pc.proxy.http.HttpProxy;
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

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @PostConstruct
    public void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Proxy stopping...");
            httpProxy.stop();
        }));
        httpProxy.start();
    }

}
