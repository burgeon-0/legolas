package org.burgeon.legolas.ps.server;

/**
 * 代理服务器
 *
 * @author Sam Lu
 * @date 2022/3/30
 */
public interface ProxyServer {

    /**
     * 启动服务器
     */
    void start();

    /**
     * 关闭服务器
     */
    void stop();

}
