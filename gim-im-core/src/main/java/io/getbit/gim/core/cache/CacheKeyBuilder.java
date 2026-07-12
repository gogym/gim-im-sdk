package io.getbit.gim.core.cache;

/**
 * CacheKeyBuilder.java
 *
 * IM 模块 Redis 缓存 Key 构建器
 * 统一管理所有 IM 缓存的 key 前缀和格式，避免各模块硬编码 key
 *
 * @author gogym
 */
public class CacheKeyBuilder {

    private static final String PREFIX = "im:";

    // ====================== 用户路由 ======================

    /**
     * 用户所在节点ID（用于集群路由）
     * 对应 UserRouteService 中的 gim_route:{userId}
     */
    public static String userRoute(String userId) {
        return "gim_route:" + userId;
    }

    // ====================== 群组 ======================

    /**
     * 群组信息
     */
    public static String groupInfo(String groupId) {
        return PREFIX + "group:info:" + groupId;
    }

    // ====================== 群成员 ======================

    /**
     * 群活跃成员 userId 列表
     */
    public static String groupMemberUserIds(String groupId) {
        return PREFIX + "group:members:" + groupId;
    }

    /**
     * 群成员信息（单个）
     */
    public static String groupMember(String groupId, Long userId) {
        return PREFIX + "group:member:" + groupId + ":" + userId;
    }

    // ====================== 好友 ======================

    /**
     * 用户好友列表
     */
    public static String friendList(Long userId) {
        return PREFIX + "friend:list:" + userId;
    }
}
