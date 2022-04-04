# Netty Proxy

## 支持协议

HTTP、SOCKS5

## 支持模式

- 全局模式：全部请求经过代理服务器；
- 直连模式：全部请求不经过代理服务器；
- Pac模式：根据 Pac 配置文件，选择性代理请求。

## 参考资料

- [Introduction to Netty](https://www.baeldung.com/netty#6-server-bootstrap)
- [HTTP Server with Netty](https://www.baeldung.com/java-netty-http-server)
- [Netty实现简单HTTP代理服务器](https://cloud.tencent.com/developer/article/1550332)
- [45 张图深度解析 Netty 架构与原理](https://cloud.tencent.com/developer/article/1754078)
- [socks5协议原理学习](https://cloud.tencent.com/developer/article/1802233)
- [SOCKS Protocol Version 5](https://www.ietf.org/rfc/rfc1928.txt)
- [Username/Password Authentication for SOCKS V5](https://www.ietf.org/rfc/rfc1929.txt)
- [netty-proxy-server](https://github.com/kongwu-/netty-proxy-server)
- [trojan-client-netty](https://github.com/kdyzm/trojan-client-netty)
- [Netty in the Middle](https://github.com/chhsiao90/nitmproxy)