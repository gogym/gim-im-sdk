package io.getbit.gim.starter;

import io.getbit.gim.core.connection.server.NettyServer;
import io.getbit.gim.core.routing.ClusterMessageRouter;
import org.springframework.context.SmartLifecycle;

/**
 * NettyServerLifecycleAdapter.java
 *
 * 将 core 的 GimLifecycle（NettyServer + ClusterMessageRouter）适配为 Spring SmartLifecycle
 * 使 Netty 服务器和集群路由随 Spring 容器自动启停
 *
 * @author gogym
 */
public class NettyServerLifecycleAdapter implements SmartLifecycle {

    private final NettyServer nettyServer;
    private final ClusterMessageRouter clusterRouter;

    public NettyServerLifecycleAdapter(NettyServer nettyServer, ClusterMessageRouter clusterRouter) {
        this.nettyServer = nettyServer;
        this.clusterRouter = clusterRouter;
    }

    @Override
    public void start() {
        clusterRouter.start();
        nettyServer.start();
    }

    @Override
    public void stop() {
        nettyServer.stop();
        clusterRouter.stop();
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
