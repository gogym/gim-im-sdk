package io.getbit.gim.starter;

import io.getbit.gim.core.bootstrap.GimBootstrap;
import io.getbit.gim.core.config.properties.GimProperties;
import io.getbit.gim.core.connection.IMServerFacade;
import io.getbit.gim.core.connection.server.NettyServer;
import io.getbit.gim.core.spi.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
                                                      ObjectProvider<ImGroupMemberProvider> groupMemberProviderProvider,
                                                      ObjectProvider<ImFriendProvider> friendProviderProvider,
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

        ImGroupMemberProvider groupProvider = groupMemberProviderProvider.getIfAvailable();
        if (groupProvider != null) {
            builder.groupMemberProvider(groupProvider);
        }

        ImFriendProvider friendProvider = friendProviderProvider.getIfAvailable();
        if (friendProvider != null) {
            builder.friendProvider(friendProvider);
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
}
