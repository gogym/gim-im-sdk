package io.getbit.gim.core.config.properties;

import lombok.Getter;
import lombok.Setter;

/**
 * NettyProperties.java
 * <p>
 * Netty服务器配置
 *
 * @author gogym
 */
@Getter
@Setter
public class NettyProperties {

    /**
     * 监听端口
     */
    private int port = 3333;

    /**
     * Boss线程数
     */
    private int bossThreads = 1;

    /**
     * Worker线程数（0=自动检测CPU核心数）
     */
    private int workerThreads = 0;

    /**
     * 连接队列大小
     */
    private int backlog = 512;
}
