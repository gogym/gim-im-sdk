package io.getbit.gim.core.connection.channel;

import io.getbit.gim.core.config.properties.CacheProperties;
import io.getbit.gim.core.config.properties.GimProperties;
import io.getbit.gim.protocol.codec.DeviceType;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * ChannelManager.java
 *
 * 多设备通道管理器
 * 支持同一用户在多种设备类型上同时在线（分组共存策略）
 * 同一设备类型互踢：新连接替换旧连接
 *
 * @author gogym
 */
public class ChannelManager {

    private static final Logger logger = LoggerFactory.getLogger(ChannelManager.class);

    private final String serverId;

    public ChannelManager(GimProperties config) {
        this(config.getServerId(), config.getCache());
    }

    public ChannelManager(String serverId, CacheProperties cacheProperties) {
        this.serverId = serverId;
        int maxSize = cacheProperties != null ? cacheProperties.getMaxSize() : 100_000;
        int expireSeconds = cacheProperties != null ? cacheProperties.getExpireSeconds() : 300;
        this.connections = Caffeine.newBuilder()
                .expireAfterAccess(expireSeconds, TimeUnit.SECONDS)
                .maximumSize(maxSize)
                .build();
        this.userChannels = Caffeine.newBuilder()
                .expireAfterAccess(expireSeconds, TimeUnit.SECONDS)
                .maximumSize(maxSize)
                .removalListener((String userId, Map<DeviceType, Channel> deviceMap, com.github.benmanes.caffeine.cache.RemovalCause cause) -> {
                    if (deviceMap != null) {
                        for (Channel ch : deviceMap.values()) {
                            connections.invalidate(ch.id().asLongText());
                        }
                        logger.debug("userChannels evicted: userId={}, cause={}", userId, cause);
                    }
                })
                .build();
    }

    /**
     * channelId -> ConnectionInfo（反向映射，用于断连时快速查找）
     */
    private final Cache<String, ConnectionInfo> connections;

    /**
     * userId -> Map<DeviceType, Channel>（多设备支持）
     */
    private final Cache<String, Map<DeviceType, Channel>> userChannels;

    // ====================== 绑定与解绑 ======================

    public Channel bind(String userId, DeviceType device, Channel channel) {
        String channelId = channel.id().asLongText();

        Map<DeviceType, Channel> deviceMap = userChannels.get(userId, k -> new ConcurrentHashMap<>());

        // 同设备类型互踢
        Channel oldChannel = deviceMap.put(device, channel);

        if (oldChannel != null && oldChannel != channel) {
            connections.invalidate(oldChannel.id().asLongText());
            logger.debug("同设备互踢, userId: {}, device: {}, oldChannel: {}", userId, device, oldChannel.id().asShortText());
        }

        connections.put(channelId, ConnectionInfo.of(serverId, userId, channelId, device));

        logger.debug("绑定通道, userId: {}, device: {}, channelId: {}", userId, device, channelId);
        return oldChannel;
    }

    /**
     * 按 userId + 设备类型解绑（服务端主动踢人）
     */
    public void unbind(String userId, DeviceType device) {
        Map<DeviceType, Channel> deviceMap = userChannels.getIfPresent(userId);
        if (deviceMap == null) {
            return;
        }

        Channel removed = deviceMap.remove(device);
        if (removed != null) {
            connections.invalidate(removed.id().asLongText());
        }

        if (deviceMap.isEmpty()) {
            userChannels.invalidate(userId);
        }

        logger.debug("解绑通道, userId: {}, device: {}", userId, device);
    }

    public ConnectionInfo unbindByChannelId(String channelId) {
        ConnectionInfo info = connections.getIfPresent(channelId);
        if (info == null) {
            return null;
        }

        connections.invalidate(channelId);
        String userId = info.userId();
        DeviceType device = info.device();

        if (userId != null) {
            Map<DeviceType, Channel> deviceMap = userChannels.getIfPresent(userId);
            if (deviceMap != null) {
                Channel current = deviceMap.get(device);
                if (current != null && current.id().asLongText().equals(channelId)) {
                    deviceMap.remove(device);
                    if (deviceMap.isEmpty()) {
                        userChannels.invalidate(userId);
                    }
                }
            }
            logger.debug("断连解绑, userId: {}, device: {}, channelId: {}", userId, device, channelId);
        }

        return info;
    }

    // ====================== 查询 ======================

    public Map<DeviceType, Channel> getChannels(String userId) {
        Map<DeviceType, Channel> deviceMap = userChannels.getIfPresent(userId);
        if (deviceMap == null || deviceMap.isEmpty()) {
            return Map.of();
        }
        return Map.copyOf(deviceMap);
    }

    public int getOnlineUserCount() {
        return (int) userChannels.estimatedSize();
    }

    public int getTotalConnectionCount() {
        return (int) connections.estimatedSize();
    }
}
