package io.getbit.gim.core.message.ack;

import io.getbit.gim.core.spi.ImEventListener;
import io.getbit.gim.protocol.codec.ImProto;
import io.getbit.gim.protocol.codec.PacketCodec;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * MessageAckTracker.java
 *
 * 消息ACK追踪器
 * 追踪已发送但未收到ACK的消息，支持超时重发
 * ACK超时时自动触发离线消息回调
 *
 * @author gogym
 */
public class MessageAckTracker {

    private static final Logger logger = LoggerFactory.getLogger(MessageAckTracker.class);

    /**
     * ACK超时时间（秒）
     */
    private static final int ACK_TIMEOUT_SECONDS = 10;

    /**
     * 最大追踪数量
     */
    private static final int MAX_TRACK_SIZE = 100_000;

    private final List<ImEventListener> eventListeners;

    /**
     * msgId -> AckInfo 映射
     */
    private final Cache<String, AckInfo> pendingAcks;

    public MessageAckTracker() {
        this(Collections.emptyList());
    }

    public MessageAckTracker(List<ImEventListener> eventListeners) {
        this.eventListeners = eventListeners != null ? eventListeners : Collections.emptyList();
        this.pendingAcks = Caffeine.newBuilder()
                .expireAfterWrite(ACK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .maximumSize(MAX_TRACK_SIZE)
                .removalListener((String key, AckInfo value, com.github.benmanes.caffeine.cache.RemovalCause cause) -> {
                    if (cause == com.github.benmanes.caffeine.cache.RemovalCause.EXPIRED && value != null) {
                        logger.warn("消息ACK超时: msgId={}, receiver={}", key, value.receiverId);
                        // ACK 超时，触发离线消息回调
                        fireAckTimeout(key, value);
                    }
                })
                .build();
    }

    /**
     * 注册待ACK消息
     *
     * @param msgId      消息ID
     * @param receiverId 接收者ID
     * @param packet     发送的Packet
     */
    public void track(String msgId, String receiverId, ImProto.Packet packet) {
        pendingAcks.put(msgId, new AckInfo(receiverId, System.currentTimeMillis(), packet));
        logger.debug("注册ACK追踪: msgId={}, receiver={}", msgId, receiverId);
    }

    /**
     * 确认收到ACK
     *
     * @param msgId 消息ID
     * @return true if was pending
     */
    public boolean acknowledge(String msgId) {
        AckInfo removed = pendingAcks.getIfPresent(msgId);
        if (removed != null) {
            pendingAcks.invalidate(msgId);
            logger.debug("消息ACK确认: msgId={}, receiver={}", msgId, removed.receiverId);
            return true;
        }
        return false;
    }

    /**
     * 获取待ACK数量
     */
    public long getPendingCount() {
        return pendingAcks.estimatedSize();
    }

    /**
     * ACK超时触发离线回调
     */
    private void fireAckTimeout(String msgId, AckInfo info) {
        try {
            ImProto.ChatMessage chatMsg = PacketCodec.parseChatMessage(info.packet);
            for (ImEventListener listener : eventListeners) {
                try {
                    listener.onOfflineChatMessage(chatMsg, info.receiverId, "ACK_TIMEOUT");
                } catch (Exception e) {
                    logger.error("ACK超时离线回调异常, msgId={}", msgId, e);
                }
            }
        } catch (Exception e) {
            // Packet 可能不是聊天消息（如信令等），忽略解析异常
            logger.debug("ACK超时回调: 非聊天消息类型, msgId={}", msgId);
        }
    }

    /**
     * ACK信息记录
     */
    private record AckInfo(String receiverId, long sentAt, ImProto.Packet packet) {}
}
