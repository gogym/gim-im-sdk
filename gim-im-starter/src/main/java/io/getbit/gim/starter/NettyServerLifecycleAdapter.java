package io.getbit.gim.starter;

import io.getbit.gim.core.connection.server.NettyServer;
import org.springframework.context.SmartLifecycle;

/**
 * NettyServerLifecycleAdapter.java
 *
 * 将 core 的 GimLifecycle（NettyServer）适配为 Spring SmartLifecycle
 * 使 Netty 服务器随 Spring 容器自动启停
 *
 * @author gogym
 */
public class NettyServerLifecycleAdapter implements SmartLifecycle {

    private final NettyServer nettyServer;

    public NettyServerLifecycleAdapter(NettyServer nettyServer) {
        this.nettyServer = nettyServer;
    }

    @Override
    public void start() {
        nettyServer.start();
    }

    @Override
    public void stop() {
        nettyServer.stop();
    }

    @Override
    public boolean isRunning() {
        return nettyServer.isRunning();
    }

    @Override
    public int getPhase() {
        // 较高 phase，确保在其他 Bean 之后启动、之前停止
        return Integer.MAX_VALUE - 100;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }
}
