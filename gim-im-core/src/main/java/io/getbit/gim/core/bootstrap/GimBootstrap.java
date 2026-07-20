package io.getbit.gim.core.bootstrap;

import io.getbit.gim.core.config.properties.GimProperties;
import io.getbit.gim.core.connection.auth.ConnectionAuthHandler;
import io.getbit.gim.core.connection.channel.ChannelManager;
import io.getbit.gim.core.connection.server.NettyServer;
import io.getbit.gim.core.notify.friend.FriendNotifyService;
import io.getbit.gim.core.notify.group.GroupNotifyService;
import io.getbit.gim.core.message.ack.MessageAckTracker;
import io.getbit.gim.core.message.handler.*;
import io.getbit.gim.core.routing.ClusterMessageRouter;
import io.getbit.gim.core.routing.UserRouteService;
import io.getbit.gim.core.spi.*;
import io.netty.channel.Channel;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GimBootstrap.java
 * <p>
 * GIM SDK 手动构建入口
 * 非 Spring 环境下，通过 Builder 模式手动组装所有组件
 * <p>
 * 使用示例：
 * <pre>
 * GimProperties config = GimProperties.builder()
 *     .nettyPort(3333)
 *     .enableCluster(false)
 *     .build();
 *
 * // 方式1：只获取门面（自行管理 NettyServer）
 * IMServerFacade facade = GimBootstrap.builder()
 *     .config(config)
 *     .tokenVerifier(myTokenVerifier)
 *     .redisAdapter(myRedisAdapter)
 *     .idGenerator(myIdGenerator)
 *     .friendProvider(myFriendProvider)
 *     .groupMemberProvider(myGroupMemberProvider)
 *     .build();
 *
 * // 方式2：获取完整启动上下文（推荐）
 * GimBootstrap.StartContext ctx = GimBootstrap.builder()
 *     .config(config)
 *     .tokenVerifier(myTokenVerifier)
 *     .redisAdapter(myRedisAdapter)
 *     .idGenerator(myIdGenerator)
 *     .friendProvider(myFriendProvider)
 *     .groupMemberProvider(myGroupMemberProvider)
 *     .addEventListener(myEventListener)
 *     .buildWithServer();
 *
 * ctx.start();
 * </pre>
 *
 * @author gogym
 */
public class GimBootstrap {

    private static final Logger logger = LoggerFactory.getLogger(GimBootstrap.class);

    private GimBootstrap() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private GimProperties config;
        private ImTokenVerifier tokenVerifier;
        private ImRedisAdapter redisAdapter;
        private ImRedisSubscriber redisSubscriber;
        private ImIdGenerator idGenerator;
        private ImGroupMemberProvider groupMemberProvider;
        private ImFriendProvider friendProvider;
        private final List<ImEventListener> eventListeners = new ArrayList<>();

        public Builder config(GimProperties config) {
            this.config = config;
            return this;
        }

        public Builder tokenVerifier(ImTokenVerifier tokenVerifier) {
            this.tokenVerifier = tokenVerifier;
            return this;
        }

        public Builder redisAdapter(ImRedisAdapter redisAdapter) {
            this.redisAdapter = redisAdapter;
            return this;
        }

        public Builder redisSubscriber(ImRedisSubscriber redisSubscriber) {
            this.redisSubscriber = redisSubscriber;
            return this;
        }

        public Builder idGenerator(ImIdGenerator idGenerator) {
            this.idGenerator = idGenerator;
            return this;
        }

        public Builder groupMemberProvider(ImGroupMemberProvider groupMemberProvider) {
            this.groupMemberProvider = groupMemberProvider;
            return this;
        }

        /**
         * 设置好友关系提供者
         * 用于单聊好友校验、在线状态同步等
         */
        public Builder friendProvider(ImFriendProvider friendProvider) {
            this.friendProvider = friendProvider;
            return this;
        }

        public Builder addEventListener(ImEventListener listener) {
            this.eventListeners.add(listener);
            return this;
        }

        public Builder eventListeners(List<ImEventListener> listeners) {
            this.eventListeners.clear();
            if (listeners != null) {
                this.eventListeners.addAll(listeners);
            }
            return this;
        }

        /**
         * 构建 IMServerFacade
         *
         * @return IMServerFacade 统一门面
         */
        public IMServerFacade build() {
            return assemble().facade;
        }

        /**
         * 构建 IMServerFacade 并创建 NettyServer（不自动启动）
         *
         * @return 包含 facade、nettyServer、clusterRouter 的启动上下文
         */
        public StartContext buildWithServer() {
            Assembly assembly = assemble();
            NettyServer nettyServer = new NettyServer(config, assembly.facade);
            // 延迟注入 NettyServer 到健康指标，用于检查 Netty 运行状态
            assembly.facade.getHealthIndicator().setNettyServer(nettyServer);
            return new StartContext(assembly.facade, nettyServer, assembly.clusterRouter);
        }

        // ==================== 内部组装逻辑 ====================

        private Assembly assemble() {
            // 参数校验
            requireNonNull(config, "config");
            requireNonNull(tokenVerifier, "tokenVerifier");
            requireNonNull(redisAdapter, "redisAdapter");
            requireNonNull(idGenerator, "idGenerator");

            // 可选组件默认值
            ImRedisSubscriber subscriber = redisSubscriber != null ? redisSubscriber : new NoOpRedisSubscriber();
            ImGroupMemberProvider groupProvider = groupMemberProvider != null ? groupMemberProvider : new NoOpGroupMemberProvider();

            List<ImEventListener> listeners = eventListeners.isEmpty()
                    ? Collections.emptyList() : Collections.unmodifiableList(eventListeners);

            // ========== 组装核心组件 ==========

            // 使用 CacheProperties 配置缓存参数
            ChannelManager channelManager = new ChannelManager(config);

            UserRouteService userRouteService = new UserRouteService(config, redisAdapter, config.getCache());

            // 读取配置
            boolean autoRewrite = config.isAutoRewrite();

            // 集群路由（先创建，供 ResendCallback 使用）
            ClusterMessageRouter clusterRouter = new ClusterMessageRouter(
                    config, channelManager, redisAdapter, subscriber, listeners);

            // 自动重发回调：本地投递 → 集群路由
            MessageAckTracker.ResendCallback resendCallback = (receiverId, packet) -> {
                var channels = channelManager.getChannels(receiverId);
                if (!channels.isEmpty()) {
                    for (var entry : channels.entrySet()) {
                        Channel ch = entry.getValue();
                        if (ch != null && ch.isActive()) {
                            ch.writeAndFlush(packet);
                        }
                    }
                } else {
                    String serverId = userRouteService.getServerId(receiverId);
                    if (serverId != null) {
                        clusterRouter.routeToRemote(serverId, packet, receiverId);
                    }
                }
            };

            // 使用 MessageProperties 配置 ACK 超时，结合 autoRewrite 配置自动重发
            MessageAckTracker messageAckTracker = new MessageAckTracker(
                    config.getMsg().getAckTimeoutSeconds(), listeners,
                    autoRewrite, config.getReWriteNum(), config.getReWriteDelay(),
                    resendCallback);

            ConnectionAuthHandler authHandler = new ConnectionAuthHandler(
                    tokenVerifier, channelManager, config, userRouteService);

            // ========== 组装通知服务 ==========

            // 好友通知服务（仅在配置了 ImFriendProvider 时启用）
            FriendNotifyService friendNotifyService = null;
            if (friendProvider != null) {
                friendNotifyService = new FriendNotifyService(
                        channelManager, userRouteService, clusterRouter, friendProvider, listeners);
            }

            // 群组通知服务（使用 ImGroupMemberProvider）
            GroupNotifyService groupNotifyService = new GroupNotifyService(
                    channelManager, userRouteService, clusterRouter, groupProvider, listeners);

            // ========== 消息处理器 ==========

            HeartbeatHandler heartbeatHandler = new HeartbeatHandler(
                    channelManager, userRouteService, clusterRouter, listeners);

            SingleChatHandler singleChatHandler = new SingleChatHandler(
                    channelManager, userRouteService, clusterRouter, listeners,
                    idGenerator, messageAckTracker, friendProvider);

            GroupChatHandler groupChatHandler = new GroupChatHandler(
                    channelManager, userRouteService, clusterRouter, listeners,
                    idGenerator, messageAckTracker, groupProvider);

            DeliveryAckHandler deliveryAckHandler = new DeliveryAckHandler(
                    channelManager, userRouteService, clusterRouter, listeners, messageAckTracker);

            ReadReceiptHandler readReceiptHandler = new ReadReceiptHandler(
                    channelManager, userRouteService, clusterRouter, listeners);

            MsgRecallHandler msgRecallHandler = new MsgRecallHandler(
                    channelManager, userRouteService, clusterRouter, listeners, groupProvider);

            RtcSignalHandler rtcSignalHandler = new RtcSignalHandler(
                    channelManager, userRouteService, clusterRouter, listeners);

            // 消息分发器
            List<BaseHandler> handlers = List.of(
                    heartbeatHandler, singleChatHandler, groupChatHandler,
                    deliveryAckHandler, readReceiptHandler, msgRecallHandler, rtcSignalHandler);
            DefaultMessageDispatcher messageDispatcher = new DefaultMessageDispatcher(handlers);

            // 门面（内置 ImNodeHealthIndicator，传入 Redis 组件用于健康检查）
            IMServerFacade facade = new IMServerFacade(
                    config, channelManager, messageDispatcher, authHandler, userRouteService,
                    listeners, friendNotifyService, groupNotifyService, redisAdapter, subscriber);

            // 注入 MessageAckTracker 到健康指标，用于监控待确认消息数
            facade.getHealthIndicator().setMessageAckTracker(messageAckTracker);

            logger.info("GIM SDK 组件组装完成, serverId={}, cluster={}, friend={}, ackTimeout={}s, autoRewrite={}",
                    config.getServerId(), config.isEnableCluster(),
                    friendNotifyService != null ? "enabled" : "disabled",
                    config.getMsg().getAckTimeoutSeconds(), autoRewrite);

            return new Assembly(facade, clusterRouter);
        }

        private void requireNonNull(Object obj, String name) {
            if (obj == null) {
                throw new IllegalArgumentException("GimBootstrap: '" + name + "' is required");
            }
        }
    }

    /**
     * 启动上下文：包含 facade 和 nettyServer，统一管理启停
     */
    public static class StartContext {
        @Getter
        private final IMServerFacade facade;
        @Getter
        private final NettyServer nettyServer;
        private final ClusterMessageRouter clusterRouter;

        public StartContext(IMServerFacade facade, NettyServer nettyServer, ClusterMessageRouter clusterRouter) {
            this.facade = facade;
            this.nettyServer = nettyServer;
            this.clusterRouter = clusterRouter;
        }

        /**
         * 启动 IM 服务器（包括集群路由订阅和 Netty 监听）
         */
        public void start() {
            clusterRouter.start();
            nettyServer.start();
        }

        /**
         * 停止 IM 服务器
         */
        public void stop() {
            nettyServer.stop();
            clusterRouter.stop();
        }
    }

    // ==================== 内部占位实现 ====================

    /**
     * 内部组装结果
     */
    private static class Assembly {
        private final IMServerFacade facade;
        private final ClusterMessageRouter clusterRouter;

        private Assembly(IMServerFacade facade, ClusterMessageRouter clusterRouter) {
            this.facade = facade;
            this.clusterRouter = clusterRouter;
        }
    }


    private static class NoOpRedisSubscriber implements ImRedisSubscriber {
        @Override
        public void subscribe(String channel, java.util.function.Consumer<String> callback) {
        }

        @Override
        public void unsubscribe() {
        }

        @Override
        public boolean isSubscribed() {
            return false;
        }
    }

    private static class NoOpGroupMemberProvider implements ImGroupMemberProvider {
        @Override
        public List<String> getGroupMemberUserIds(String groupId) {
            return Collections.emptyList();
        }
    }
}
