package com.example.im.spi;

import io.getbit.gim.core.spi.ImEventListener;
import io.getbit.gim.protocol.codec.DeviceType;
import io.getbit.gim.protocol.codec.ImProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * IM 事件监听器实现
 * <p>
 * SDK 在以下场景触发事件回调：
 * <ul>
 *   <li>onUserOnline — 用户上线（可记录登录日志、更新在线状态）</li>
 *   <li>onUserOffline — 用户下线（可清理在线状态、记录离线时间）</li>
 *   <li>onOfflineChatMessage — 离线聊天消息（可触发 APNs/FCM 推送）</li>
 *   <li>onOfflineNotify — 离线通知消息（好友申请、群通知等）</li>
 *   <li>onMessageDeliveryFailed — 消息投递失败</li>
 *   <li>onMessageRecalled — 消息撤回（可更新 DB 消息状态）</li>
 *   <li>onChatMessage — 聊天消息（可持久化到 DB / MQ）</li>
 * </ul>
 */
@Component
public class ImEventListenerImpl implements ImEventListener {

    private static final Logger log = LoggerFactory.getLogger(ImEventListenerImpl.class);

    @Override
    public void onUserOnline(String userId, DeviceType device, String serverId) {
        log.info("[IM事件] 用户上线: userId={}, device={}, serverId={}", userId, device, serverId);
        // TODO: 更新用户在线状态、记录登录日志
    }

    @Override
    public void onUserOffline(String userId) {
        log.info("[IM事件] 用户下线: userId={}", userId);
        // TODO: 更新用户离线状态、记录离线时间
    }

    @Override
    public void onOfflineChatMessage(ImProto.ChatMessage chatMsg, String receiverId, String reason) {
        log.info("[IM事件] 离线消息: receiverId={}, msgId={}, reason={}",
                receiverId, chatMsg.getMsgId(), reason);
        // TODO: 触发离线推送（APNs/FCM/华为推送等）
        // pushService.sendOfflinePush(receiverId, chatMsg);
    }

    @Override
    public void onOfflineNotify(ImProto.Packet packet, String receiverId) {
        log.info("[IM事件] 离线通知: receiverId={}, cmd={}", receiverId, packet.getCmd());
        // TODO: 触发离线通知推送（好友申请、群通知等）
    }

    @Override
    public void onMessageDeliveryFailed(String msgId, String receiverId, String reason) {
        log.warn("[IM事件] 消息投递失败: msgId={}, receiverId={}, reason={}", msgId, receiverId, reason);
        // TODO: 记录投递失败日志、触发告警
    }

    @Override
    public void onMessageRecalled(String msgId, String conversationId, String operatorId, int chatType) {
        log.info("[IM事件] 消息撤回: msgId={}, conversationId={}, operatorId={}, chatType={}",
                msgId, conversationId, operatorId, chatType);
        // TODO: 更新 DB 中消息状态为已撤回
        // messageRepository.updateRecalled(msgId);
    }

    @Override
    public void onChatMessage(ImProto.ChatMessage chatMsg, String senderId, String receiverId, int chatType) {
        log.info("[IM事件] 聊天消息: msgId={}, from={}, to={}, chatType={}",
                chatMsg.getMsgId(), senderId, receiverId, chatType);
        // TODO: 消息持久化到 DB 或发送到 MQ
        // messageRepository.save(chatMsg);
    }
}
