package io.getbit.gim.core.config;

import io.getbit.gim.core.config.properties.GimProperties;
import io.getbit.gim.core.connection.auth.ConnectionAuthHandler;
import io.getbit.gim.core.connection.channel.ChannelManager;
import io.getbit.gim.core.connection.server.IMServerFacade;
import io.getbit.gim.core.connection.server.NettyServer;
import io.getbit.gim.core.health.ImNodeHealthIndicator;
import io.getbit.gim.core.message.ack.MessageAckTracker;
import io.getbit.gim.core.message.handler.*;
import io.getbit.gim.core.routing.ClusterMessageRouter;
import io.getbit.gim.core.routing.UserRouteService;
import io.getbit.gim.core.spi.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;

/**
 * GimAutoConfiguration.java
 *
 * GIM SDK 自动配置
 * 使用方只需引入依赖 + 提供 SPI 实现即可自动启动
 *
 * @author gogym
 */
@Configuration
@EnableConfigurationProperties(GimProperties.class)
public class GimAutoConfiguration {

    // ==================== 核心组件 ====================

    @Bean
    @ConditionalOnMissingBean
    public ChannelManager channelManager(GimProperties config) {
        return new ChannelManager(config);
    }

    @Bean
    @ConditionalOnMissingBean
    public UserRouteService userRouteService(GimProperties config, ImRedisAdapter redisAdapter) {
        return new UserRouteService(config, redisAdapter);
    }

    @Bean
    @ConditionalOnMissingBean
    public MessageAckTracker messageAckTracker(List<ImEventListener> eventListeners) {
        return new MessageAckTracker(eventListeners);
    }

    // ==================== 认证 ====================

    @Bean
    @ConditionalOnMissingBean
    public ConnectionAuthHandler connectionAuthHandler(ImTokenVerifier tokenVerifier,
                                                       ChannelManager channelManager,
                                                       GimProperties config,
                                                       UserRouteService userRouteService) {
        return new ConnectionAuthHandler(tokenVerifier, channelManager, config, userRouteService);
    }

    // ==================== 集群路由（条件装配） ====================

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "gim.enable-cluster", havingValue = "true")
    public ClusterMessageRouter clusterMessageRouter(GimProperties config,
                                                     ChannelManager channelManager,
                                                     ImRedisAdapter redisAdapter,
                                                     ImRedisSubscriber redisSubscriber,
                                                     List<ImEventListener> eventListeners) {
        return new ClusterMessageRouter(config, channelManager, redisAdapter, redisSubscriber, eventListeners);
    }

    @Bean
    @ConditionalOnMissingBean(ClusterMessageRouter.class)
    public ClusterMessageRouter noOpClusterMessageRouter(GimProperties config,
                                                         ChannelManager channelManager,
                                                         ImRedisAdapter redisAdapter) {
        // 单机模式：提供一个不实际路由的占位实现
        return new ClusterMessageRouter(config, channelManager, redisAdapter,
                new NoOpRedisSubscriber(), Collections.emptyList());
    }

    // ==================== 消息分发 ====================

    @Bean
    @ConditionalOnMissingBean(MessageDispatcher.class)
    public DefaultMessageDispatcher defaultMessageDispatcher(List<BaseHandler> handlers) {
        return new DefaultMessageDispatcher(handlers);
    }

    @Bean
    @ConditionalOnMissingBean
    public HeartbeatHandler heartbeatHandler(ChannelManager channelManager,
                                             UserRouteService userRouteService,
                                             ClusterMessageRouter clusterMessageRouter,
                                             List<ImEventListener> eventListeners) {
        return new HeartbeatHandler(channelManager, userRouteService, clusterMessageRouter, eventListeners);
    }

    @Bean
    @ConditionalOnMissingBean(name = "singleChatHandler")
    public SingleChatHandler singleChatHandler(ChannelManager channelManager,
                                                UserRouteService userRouteService,
                                                ClusterMessageRouter clusterMessageRouter,
                                                List<ImEventListener> eventListeners,
                                                ImIdGenerator idGenerator,
                                                MessageAckTracker ackTracker) {
        return new SingleChatHandler(channelManager, userRouteService, clusterMessageRouter, eventListeners, idGenerator, ackTracker);
    }

    @Bean
    @ConditionalOnMissingBean(name = "groupChatHandler")
    public GroupChatHandler groupChatHandler(ChannelManager channelManager,
                                              UserRouteService userRouteService,
                                              ClusterMessageRouter clusterMessageRouter,
                                              List<ImEventListener> eventListeners,
                                              ImIdGenerator idGenerator,
                                              MessageAckTracker ackTracker) {
        return new GroupChatHandler(channelManager, userRouteService, clusterMessageRouter, eventListeners, idGenerator, ackTracker);
    }

    @Bean
    @ConditionalOnMissingBean(name = "deliveryAckHandler")
    public DeliveryAckHandler deliveryAckHandler(ChannelManager channelManager,
                                                  UserRouteService userRouteService,
                                                  ClusterMessageRouter clusterMessageRouter,
                                                  List<ImEventListener> eventListeners,
                                                  MessageAckTracker ackTracker) {
        return new DeliveryAckHandler(channelManager, userRouteService, clusterMessageRouter, eventListeners, ackTracker);
    }

    @Bean
    @ConditionalOnMissingBean(name = "readReceiptHandler")
    public ReadReceiptHandler readReceiptHandler(ChannelManager channelManager,
                                                  UserRouteService userRouteService,
                                                  ClusterMessageRouter clusterMessageRouter,
                                                  List<ImEventListener> eventListeners) {
        return new ReadReceiptHandler(channelManager, userRouteService, clusterMessageRouter, eventListeners);
    }

    @Bean
    @ConditionalOnMissingBean(name = "msgRecallHandler")
    public MsgRecallHandler msgRecallHandler(ChannelManager channelManager,
                                              UserRouteService userRouteService,
                                              ClusterMessageRouter clusterMessageRouter,
                                              List<ImEventListener> eventListeners) {
        return new MsgRecallHandler(channelManager, userRouteService, clusterMessageRouter, eventListeners);
    }

    @Bean
    @ConditionalOnMissingBean(name = "rtcSignalHandler")
    public RtcSignalHandler rtcSignalHandler(ChannelManager channelManager,
                                              UserRouteService userRouteService,
                                              ClusterMessageRouter clusterMessageRouter,
                                              List<ImEventListener> eventListeners) {
        return new RtcSignalHandler(channelManager, userRouteService, clusterMessageRouter, eventListeners);
    }

    // ==================== 门面 ====================

    @Bean
    @ConditionalOnMissingBean
    public IMServerFacade imServerFacade(GimProperties config,
                                         ChannelManager channelManager,
                                         MessageDispatcher messageDispatcher,
                                         ConnectionAuthHandler authHandler,
                                         UserRouteService userRouteService,
                                         List<ImEventListener> eventListeners) {
        return new IMServerFacade(config, channelManager, messageDispatcher, authHandler,
                userRouteService, eventListeners);
    }

    // ==================== 健康指标 ====================

    @Bean
    @ConditionalOnMissingBean
    public ImNodeHealthIndicator imNodeHealthIndicator(ChannelManager channelManager,
                                                        UserRouteService userRouteService) {
        return new ImNodeHealthIndicator(channelManager, userRouteService);
    }

    // ==================== 消息中间件（默认占位） ====================

    /**
     * 默认 NoOp 消息中间件：当没有引入任何 MQ 实现（如 RocketMQ）时，
     * 提供一个空实现避免启动失败。消息将被静默丢弃。
     */
    @Bean
    @ConditionalOnMissingBean(ImMessageBroker.class)
    public ImMessageBroker noOpMessageBroker() {
        return (topic, tag, key, body) -> {
            // no-op: 未配置 MQ 时消息不实际发送
        };
    }

    // ==================== 用户上下文 ====================

    @Bean
    @ConditionalOnMissingBean(ImUserContextResolver.class)
    public ImUserContextResolver headerUserContextResolver() {
        return new HeaderUserContextResolver();
    }

    // ==================== Netty 服务器 ====================

    @Bean
    @ConditionalOnMissingBean
    public NettyServer nettyServer(GimProperties config, IMServerFacade facade) {
        return new NettyServer(config, facade);
    }

    // ==================== Web 层（可选） ====================

    /**
     * 默认用户上下文解析器：从请求头 X-User-Id 获取用户 ID
     * 仅在 Web 应用环境下自动注册，使用方可自定义 ImUserContextResolver Bean 覆盖
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

    // ==================== 内部占位实现 ====================

    /**
     * 单机模式下的 Redis Pub/Sub 占位实现（不实际订阅）
     */
    private static class NoOpRedisSubscriber implements ImRedisSubscriber {
        @Override
        public void subscribe(String channel, java.util.function.Consumer<String> callback) {
            // no-op
        }

        @Override
        public void unsubscribe() {
            // no-op
        }

        @Override
        public boolean isSubscribed() {
            return false;
        }
    }
}
