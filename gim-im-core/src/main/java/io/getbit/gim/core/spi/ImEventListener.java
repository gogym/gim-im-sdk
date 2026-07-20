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

    /**
     * 消息撤回回调
     * 使用方可实现此方法来更新 DB 中消息状态（如标记为已撤回）
     *
     * @param msgId         消息ID
     * @param conversationId 会话ID
     * @param operatorId    操作者ID
     * @param chatType      聊天类型（1=单聊 2=群聊）
     */
    default void onMessageRecalled(String msgId, String conversationId, String operatorId, int chatType) {}

    /**
     * 已读回执回调
     * 当无法自动路由已读回执时（如群聊场景），通过此回调通知使用方处理
     *
     * @param readerId       已读用户ID
     * @param conversationId 会话ID
     * @param lastReadMsgId  最后已读的消息ID
     */
    default void onReadReceipt(String readerId, String conversationId, String lastReadMsgId) {}

    /**
     * 聊天消息回调（所有聊天消息均触发，无论接收方是否在线）
     * 使用方可实现此方法进行消息持久化（写入 DB / 发送到 MQ 等）
     *
     * @param chatMsg   聊天消息
     * @param senderId  发送者ID
     * @param receiverId 接收方ID（单聊为用户ID，群聊为群组ID）
     * @param chatType  聊天类型（1=单聊 2=群聊）
     */
    default void onChatMessage(ImProto.ChatMessage chatMsg, String senderId, String receiverId, int chatType) {}
}
