package io.getbit.gim.core.bootstrap;

/**
 * GimLifecycle.java
 *
 * GIM 组件生命周期接口
 * 替代 Spring SmartLifecycle，使核心模块不依赖 Spring
 *
 * @author gogym
 */
public interface GimLifecycle {

    /**
     * 启动组件
     */
    void start();

    /**
     * 停止组件
     */
    void stop();

    /**
     * 是否正在运行
     */
    boolean isRunning();
}
