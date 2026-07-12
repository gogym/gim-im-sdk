package io.getbit.gim.core.routing;

import io.getbit.gim.core.cache.CacheKeyBuilder;
import io.getbit.gim.core.config.properties.GimProperties;
import io.getbit.gim.core.spi.ImRedisAdapter;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * UserRouteService.java
 *
 * 用户路由服务
 * 维护 userId → serverId 的映射关系
 * 本地缓存 + Redis 二级缓存
 *
 * Redis Key 结构：
 * - gim_route:{userId} → serverId（TTL 5分钟，心跳续期）
 * - Key 统一由 {@link io.getbit.gim.core.cache.CacheKeyBuilder} 管理
 *
 * @author gogym
 */
public class UserRouteService {

    private static final Logger logger = LoggerFactory.getLogger(UserRouteService.class);

    // key 统一由 CacheKeyBuilder 管理

    /**
     * 路由过期时间（秒）：5 分钟，通过心跳续期
     */
    private static final int ROUTE_EXPIRE_SECONDS = 5 * 60;

    /**
     * 本地缓存：userId → serverId
     */
    private final Cache<String, String> localCache = Caffeine.newBuilder()
            .expireAfterWrite(ROUTE_EXPIRE_SECONDS, TimeUnit.SECONDS)
            .maximumSize(100_000)
            .build();

    private final GimProperties config;
    private final ImRedisAdapter redisAdapter;

    public UserRouteService(GimProperties config, ImRedisAdapter redisAdapter) {
        this.config = config;
        this.redisAdapter = redisAdapter;
    }

    // ====================== 路由管理 ======================

    /**
     * 注册用户路由（绑定连接时调用）
     */
    public void register(String userId) {
        String serverId = config.getServerId();
        String key = CacheKeyBuilder.userRoute(userId);

        redisAdapter.setex(key, ROUTE_EXPIRE_SECONDS, serverId);
        localCache.put(userId, serverId);

        logger.debug("注册用户路由: userId={}, serverId={}", userId, serverId);
    }

    /**
     * 续期用户路由（心跳时调用）
     */
    public void renew(String userId) {
        String key = CacheKeyBuilder.userRoute(userId);
        redisAdapter.setex(key, ROUTE_EXPIRE_SECONDS, config.getServerId());
    }

    /**
     * 注销用户路由（断开连接时调用）
     */
    public void unregister(String userId) {
        String key = CacheKeyBuilder.userRoute(userId);

        redisAdapter.del(key);
        localCache.invalidate(userId);

        logger.debug("注销用户路由: userId={}", userId);
    }

    /**
     * 查询用户所在节点
     */
    public String getServerId(String userId) {
        // 1. 本地缓存
        String cached = localCache.getIfPresent(userId);
        if (cached != null) {
            return cached;
        }

        // 2. Redis
        String key = CacheKeyBuilder.userRoute(userId);
        String serverId = redisAdapter.get(key);

        if (serverId != null) {
            localCache.put(userId, serverId);
        }

        return serverId;
    }

    /**
     * 判断用户是否在当前节点
     */
    public boolean isLocal(String userId) {
        String serverId = getServerId(userId);
        return config.getServerId().equals(serverId);
    }

    /**
     * 判断用户是否在远程节点
     */
    public boolean isRemote(String userId) {
        String serverId = getServerId(userId);
        return serverId != null && !config.getServerId().equals(serverId);
    }

    /**
     * 获取本地缓存大小
     */
    public long getLocalCacheSize() {
        return localCache.estimatedSize();
    }
}
