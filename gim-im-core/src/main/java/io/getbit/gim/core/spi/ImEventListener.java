package io.getbit.gim.core.spi;

import io.getbit.gim.protocol.codec.DeviceType;
import io.getbit.gim.protocol.codec.ImProto;

/**
 * ImEventListener.java
 *
 * SPI接口：IM事件监听器
 * 使用方可实现此接口来接收用户上下线、消息投递失败、离线消息等事件
 * 用于业务扩展（如推送在线状态、记录登录日志、触发离线推送等）
 *
 * @author gogym
 */
public interface ImEventListener {

    /**
     * 用户上线（绑定连接成功时触发）
     *
     * @param userId   用户ID
     * @param device   设备类型
     * @param serverId 所在节点ID
     */
    default void onUserOnline(String userId, DeviceType device, String serverId) {}

    /**
     * 用户下线（所有设备断开连接时触发）
     *
     * @param userId 用户ID
     */
    default void onUserOffline(String userId) {}

    /**
     * 消息投递失败（接收者离线且无路由时触发）
     *
     * @param msgId      消息ID
     * @param receiverId 接收者ID
     * @param reason     失败原因
     */
    default void onMessageDeliveryFailed(String msgId, String receiverId, String reason) {}

    /**
     * 离线聊天消息回调（接收方不在线时触发）
     * 使用方可实现此方法来触发离线推送（APNs/FCM）或入库待拉取
     *
     * @param chatMsg    聊天消息
     * @param receiverId 接收者ID
     * @param reason     离线原因（OFFLINE / ROUTE_NOT_FOUND）
     */
    default void onOfflineChatMessage(ImProto.ChatMessage chatMsg, String receiverId, String reason) {}

    /**
     * 离线通知消息回调（接收方不在线时触发）
     * 使用方可实现此方法来触发离线推送（好友申请、群通知等）
     *
     * @param packet     通知包
     * @param receiverId 接收者ID
     */
    default void onOfflineNotify(ImProto.Packet packet, String receiverId) {}
}
