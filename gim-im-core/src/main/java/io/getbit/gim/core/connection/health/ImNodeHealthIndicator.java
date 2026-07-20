package io.getbit.gim.core.connection.health;

import io.getbit.gim.core.connection.channel.ChannelManager;
import io.getbit.gim.core.routing.UserRouteService;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ImNodeHealthIndicator.java
 *
 * IM节点健康指标
 * 提供在线用户数、连接数等运行时信息
 *
 * @author gogym
 */
public class ImNodeHealthIndicator {

    private final ChannelManager channelManager;
    private final UserRouteService userRouteService;

    public ImNodeHealthIndicator(ChannelManager channelManager, UserRouteService userRouteService) {
        this.channelManager = channelManager;
        this.userRouteService = userRouteService;
    }

    /**
     * 获取健康指标数据
     */
    public Map<String, Object> getHealthDetails() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("onlineUsers", channelManager.getOnlineUserCount());
        details.put("totalConnections", channelManager.getTotalConnectionCount());
        details.put("localRouteCacheSize", userRouteService.getLocalCacheSize());
        details.put("status", "UP");
        return details;
    }
}
