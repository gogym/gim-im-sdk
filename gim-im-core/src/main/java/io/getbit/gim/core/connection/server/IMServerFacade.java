package io.getbit.gim.core.connection.server;

import io.getbit.gim.core.config.properties.GimProperties;
import io.getbit.gim.core.connection.channel.ChannelManager;
import io.getbit.gim.core.connection.auth.ConnectionAuthHandler;
import io.getbit.gim.core.friend.FriendNotifyService;
import io.getbit.gim.core.group.GroupNotifyService;
import io.getbit.gim.core.message.handler.MessageDispatcher;
import io.getbit.gim.core.routing.UserRouteService;
import io.getbit.gim.core.spi.ImEventListener;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * IMServerFacade.java
 *
 * IM 服务器统一门面
 * 提供核心组件的统一获取入口
 *
 * @author gogym
 */
public class IMServerFacade {

    private static final Logger logger = LoggerFactory.getLogger(IMServerFacade.class);

    @Getter private final GimProperties config;
    @Getter private final ChannelManager channelManager;
    @Getter private final MessageDispatcher messageDispatcher;
    @Getter private final ConnectionAuthHandler authHandler;
    @Getter private final UserRouteService userRouteService;
    @Getter private final List<ImEventListener> eventListeners;

    /** 好友通知推送服务（可选，未配置 ImFriendProvider 时为 null） */
    @Getter private FriendNotifyService friendNotifyService;

    /** 群组通知推送服务（可选，未配置 ImGroupMemberProvider 时为 null） */
    @Getter private GroupNotifyService groupNotifyService;

    public IMServerFacade(GimProperties config,
                          ChannelManager channelManager,
                          MessageDispatcher messageDispatcher,
                          ConnectionAuthHandler authHandler,
                          UserRouteService userRouteService,
                          List<ImEventListener> eventListeners) {
        this(config, channelManager, messageDispatcher, authHandler, userRouteService, eventListeners, null, null);
    }

    public IMServerFacade(GimProperties config,
                          ChannelManager channelManager,
                          MessageDispatcher messageDispatcher,
                          ConnectionAuthHandler authHandler,
                          UserRouteService userRouteService,
                          List<ImEventListener> eventListeners,
                          FriendNotifyService friendNotifyService,
                          GroupNotifyService groupNotifyService) {
        this.config = config;
        this.channelManager = channelManager;
        this.messageDispatcher = messageDispatcher;
        this.authHandler = authHandler;
        this.userRouteService = userRouteService;
        this.eventListeners = eventListeners;
        this.friendNotifyService = friendNotifyService;
        this.groupNotifyService = groupNotifyService;

        logger.info("IMServerFacade 初始化完成, serverId={}, cluster={}",
                config.getServerId(), config.isEnableCluster());
    }

    /**
     * 设置好友通知服务（由 GimBootstrap 组装后设置）
     */
    public void setFriendNotifyService(FriendNotifyService friendNotifyService) {
        this.friendNotifyService = friendNotifyService;
    }

    /**
     * 设置群组通知服务（由 GimBootstrap 组装后设置）
     */
    public void setGroupNotifyService(GroupNotifyService groupNotifyService) {
        this.groupNotifyService = groupNotifyService;
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
