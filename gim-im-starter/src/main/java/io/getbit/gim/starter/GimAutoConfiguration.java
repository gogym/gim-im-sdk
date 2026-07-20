package io.getbit.gim.starter;

import io.getbit.gim.core.bootstrap.GimBootstrap;
import io.getbit.gim.core.config.properties.GimProperties;
import io.getbit.gim.core.connection.server.IMServerFacade;
import io.getbit.gim.core.connection.server.NettyServer;
import io.getbit.gim.core.health.ImNodeHealthIndicator;
import io.getbit.gim.core.spi.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;

/**
 * GimAutoConfiguration.java
 *
 * GIM SDK Spring Boot 自动配置
 * 使用方只需引入 gim-im-starter 依赖 + 提供 SPI 实现即可自动启动
 *
 * @author gogym
 */
@Configuration
@EnableConfigurationProperties(GimSpringProperties.class)
public class GimAutoConfiguration {

    // ==================== 核心组件 ====================

    @Bean
    @ConditionalOnMissingBean
    public GimProperties gimProperties(GimSpringProperties springProperties) {
        return springProperties.toCoreProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    public GimBootstrap.StartContext gimStartContext(GimProperties config,
                                                      ImTokenVerifier tokenVerifier,
                                                      ImRedisAdapter redisAdapter,
                                                      ImIdGenerator idGenerator,
                                                      ObjectProvider<ImRedisSubscriber> redisSubscriberProvider,
                                                      ObjectProvider<ImMessageBroker> messageBrokerProvider,
                                                      ObjectProvider<List<ImEventListener>> eventListenersProvider) {
        GimBootstrap.Builder builder = GimBootstrap.builder()
                .config(config)
                .tokenVerifier(tokenVerifier)
                .redisAdapter(redisAdapter)
                .idGenerator(idGenerator);

        ImRedisSubscriber subscriber = redisSubscriberProvider.getIfAvailable();
        if (subscriber != null) {
            builder.redisSubscriber(subscriber);
        }

        ImMessageBroker broker = messageBrokerProvider.getIfAvailable();
        if (broker != null) {
            builder.messageBroker(broker);
        }

        List<ImEventListener> listeners = eventListenersProvider.getIfAvailable(Collections::emptyList);
        if (listeners != null && !listeners.isEmpty()) {
            builder.eventListeners(listeners);
        }

        return builder.buildWithServer();
    }

    @Bean
    @ConditionalOnMissingBean
    public IMServerFacade imServerFacade(GimBootstrap.StartContext startContext) {
        return startContext.getFacade();
    }

    @Bean
    @ConditionalOnMissingBean
    public NettyServer nettyServer(GimBootstrap.StartContext startContext) {
        return startContext.getNettyServer();
    }

    // ==================== Netty 生命周期适配 ====================

    @Bean
    @ConditionalOnMissingBean
    public SmartLifecycle nettyServerLifecycle(NettyServer nettyServer) {
        return new NettyServerLifecycleAdapter(nettyServer);
    }

    // ==================== 健康指标 ====================

    @Bean
    @ConditionalOnMissingBean
    public ImNodeHealthIndicator imNodeHealthIndicator(IMServerFacade facade) {
        return new ImNodeHealthIndicator(facade.getChannelManager(), facade.getUserRouteService());
    }

    // ==================== 默认占位实现 ====================

    /**
     * 默认 NoOp 消息中间件：当没有引入任何 MQ 实现时，提供空实现避免启动失败
     */
    @Bean
    @ConditionalOnMissingBean(ImMessageBroker.class)
    public ImMessageBroker noOpMessageBroker() {
        return (topic, tag, key, body) -> {
            // no-op
        };
    }

    // ==================== Web 层（可选） ====================

    /**
     * 默认用户上下文解析器：从请求头 X-User-Id 获取用户 ID
     * 仅在 Web 应用环境下自动注册
     */
    @Configuration
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    static class WebContextConfiguration {
        @Bean
        @ConditionalOnMissingBean(ImUserContextResolver.class)
        public HeaderUserContextResolver headerUserContextResolver() {
            return new HeaderUserContextResolver();
        }
    }
}
