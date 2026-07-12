package io.getbit.gim.core.connection.server;

import io.getbit.gim.core.config.properties.GimProperties;
import io.getbit.gim.core.connection.channel.ChannelManager;
import io.getbit.gim.core.connection.auth.ConnectionAuthHandler;
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

    public IMServerFacade(GimProperties config,
                          ChannelManager channelManager,
                          MessageDispatcher messageDispatcher,
                          ConnectionAuthHandler authHandler,
                          UserRouteService userRouteService,
                          List<ImEventListener> eventListeners) {
        this.config = config;
        this.channelManager = channelManager;
        this.messageDispatcher = messageDispatcher;
        this.authHandler = authHandler;
        this.userRouteService = userRouteService;
        this.eventListeners = eventListeners;

        logger.info("IMServerFacade 初始化完成, serverId={}, cluster={}",
                config.getServerId(), config.isEnableCluster());
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
    }
}
