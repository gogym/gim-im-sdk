package io.getbit.gim.core.bootstrap;

import io.getbit.gim.core.config.properties.GimProperties;
import io.getbit.gim.core.connection.ConnectionService;
import io.getbit.gim.core.connection.channel.ChannelManager;
import io.getbit.gim.core.connection.auth.ConnectionAuthHandler;
import io.getbit.gim.core.connection.health.ImNodeHealthIndicator;
import io.getbit.gim.core.notify.friend.FriendNotifyService;
import io.getbit.gim.core.notify.group.GroupNotifyService;
import io.getbit.gim.core.message.handler.MessageDispatcher;
import io.getbit.gim.core.routing.ClusterMessageRouter;
import io.getbit.gim.core.routing.UserRouteService;
import io.getbit.gim.core.spi.ImEventListener;
import io.getbit.gim.core.spi.ImRedisAdapter;
import io.getbit.gim.core.spi.ImRedisSubscriber;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * IMServerFacade.java
 * <p>
 * IM 服务器统一门面
 * 提供核心组件的统一获取入口
 *
 * @author gogym
 */
public class IMServerFacade {

    private static final Logger logger = LoggerFactory.getLogger(IMServerFacade.class);

    @Getter
    private final GimProperties config;
    @Getter
    private final ChannelManager channelManager;
    @Getter
    private MessageDispatcher messageDispatcher;
    @Getter
    private final ConnectionAuthHandler authHandler;
    @Getter
    private final UserRouteService userRouteService;
    @Getter
    private final List<ImEventListener> eventListeners;

    /**
     * 好友通知推送服务（可选，未配置 ImFriendProvider 时为 null）
     */
    @Getter
    private FriendNotifyService friendNotifyService;

    /**
     * 群组通知推送服务（可选，未配置 ImGroupMemberProvider 时为 null）
     */
    @Getter
    private GroupNotifyService groupNotifyService;

    /**
     * 节点健康指标
     */
    @Getter
    private ImNodeHealthIndicator healthIndicator;

    /**
     * 连接管理服务
     */
    @Getter
    private ConnectionService connectionService;

    /**
     * 集群消息路由
     */
    @Getter
    private ClusterMessageRouter clusterRouter;

    private IMServerFacade(Builder builder) {
        this.config = builder.config;
        this.channelManager = builder.channelManager;
        this.authHandler = builder.authHandler;
        this.userRouteService = builder.userRouteService;
        this.eventListeners = builder.eventListeners;
        this.friendNotifyService = builder.friendNotifyService;
        this.groupNotifyService = builder.groupNotifyService;
        this.healthIndicator = new ImNodeHealthIndicator(
                channelManager, userRouteService, builder.redisAdapter,
                builder.redisSubscriber, config.isEnableCluster());
        this.connectionService = new ConnectionService(channelManager, userRouteService, this);

        logger.info("IMServerFacade 初始化完成, serverId={}, cluster={}",
                config.getServerId(), config.isEnableCluster());
    }

    /**
     * IMServerFacade 构建器
     */
    public static class Builder {
        private GimProperties config;
        private ChannelManager channelManager;
        private ConnectionAuthHandler authHandler;
        private UserRouteService userRouteService;
        private List<ImEventListener> eventListeners = Collections.emptyList();
        private FriendNotifyService friendNotifyService;
        private GroupNotifyService groupNotifyService;
        private ImRedisAdapter redisAdapter;
        private ImRedisSubscriber redisSubscriber;

        public Builder config(GimProperties config) {
            this.config = config;
            return this;
        }

        public Builder channelManager(ChannelManager channelManager) {
            this.channelManager = channelManager;
            return this;
        }

        public Builder authHandler(ConnectionAuthHandler authHandler) {
            this.authHandler = authHandler;
            return this;
        }

        public Builder userRouteService(UserRouteService userRouteService) {
            this.userRouteService = userRouteService;
            return this;
        }

        public Builder eventListeners(List<ImEventListener> eventListeners) {
            this.eventListeners = eventListeners != null ? eventListeners : Collections.emptyList();
            return this;
        }

        public Builder friendNotifyService(FriendNotifyService friendNotifyService) {
            this.friendNotifyService = friendNotifyService;
            return this;
        }

        public Builder groupNotifyService(GroupNotifyService groupNotifyService) {
            this.groupNotifyService = groupNotifyService;
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

        public IMServerFacade build() {
            return new IMServerFacade(this);
        }
    }

    /**
     * 设置消息分发器（由 GimBootstrap 组装后注入）
     */
    void setMessageDispatcher(MessageDispatcher messageDispatcher) {
        this.messageDispatcher = messageDispatcher;
    }

    /**
     * 设置集群消息路由（由 GimBootstrap 内部组装后注入）
     */
    void setClusterRouter(ClusterMessageRouter clusterRouter) {
        this.clusterRouter = clusterRouter;
    }

    /**
     * 触发用户上线事件
     */
    public void fireUserOnline(String userId, io.getbit.gim.protocol.codec.DeviceType device) {
        if (eventListeners != null) {
            for (ImEventListener listener : eventListeners) {
                try {
                    listener.onUserOnline(userId, device, config.getServerId());
                } catch (Exception e) {
                    logger.error("事件监听器回调异常: onUserOnline", e);
                }
            }
        }

        // 绑定成功后，同步好友在线状态 + 通知好友上线
        if (friendNotifyService != null) {
            friendNotifyService.syncFriendsOnlineStatus(userId);
            friendNotifyService.notifyUserOnline(userId);
        }
    }

    /**
     * 触发用户下线事件
     */
    public void fireUserOffline(String userId) {
        if (eventListeners != null) {
            for (ImEventListener listener : eventListeners) {
                try {
                    listener.onUserOffline(userId);
                } catch (Exception e) {
                    logger.error("事件监听器回调异常: onUserOffline", e);
                }
            }
        }

        // 通知好友下线
        if (friendNotifyService != null) {
            friendNotifyService.notifyUserOffline(userId);
        }
    }
}
