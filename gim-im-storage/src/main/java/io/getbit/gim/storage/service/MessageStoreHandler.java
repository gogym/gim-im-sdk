package io.getbit.gim.storage.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.util.JsonFormat;
import io.getbit.gim.core.spi.ImIdGenerator;
import io.getbit.gim.storage.entity.ImMessage;
import io.getbit.gim.storage.entity.ImUserMessage;
import io.getbit.gim.storage.manager.ImMessageManager;
import io.getbit.gim.storage.spi.ImGroupMemberProvider;
import io.getbit.gim.protocol.codec.ImProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息入库处理器
 * 从MQ消息中解析聊天消息并入库MySQL
 * 使用方可自行对接MQ消费者调用 {@link #handleStoreMessage(String)}
 *
 * @author gogym
 */
public class MessageStoreHandler {

    private static final Logger logger = LoggerFactory.getLogger(MessageStoreHandler.class);

    private final ImMessageManager messageManager;
    private final ImGroupMemberProvider groupMemberProvider;
    private final ImIdGenerator idGenerator;

    public MessageStoreHandler(ImMessageManager messageManager,
                               ImGroupMemberProvider groupMemberProvider,
                               ImIdGenerator idGenerator) {
        this.messageManager = messageManager;
        this.groupMemberProvider = groupMemberProvider;
        this.idGenerator = idGenerator;
    }

    /**
     * 处理入库消息（由MQ消费者调用）
     *
     * @param messageJson Protobuf JSON 格式的 ChatMessage
     */
    public void handleStoreMessage(String messageJson) {
        try {
            ImProto.ChatMessage.Builder builder = ImProto.ChatMessage.newBuilder();
            JsonFormat.parser().merge(messageJson, builder);
            ImProto.ChatMessage chatMsg = builder.build();

            String msgId = chatMsg.getMsgId();

            // 幂等检查
            ImMessage existing = messageManager.findByMsgId(msgId);
            if (existing != null) {
                logger.debug("消息已入库，跳过重复: msgId={}", msgId);
                return;
            }

            // 1. 插入消息本体
            ImMessage messageEntity = buildImMessage(chatMsg);
            messageManager.insertMessage(messageEntity);

            // 2. 创建用户-消息关联
            int chatType = chatMsg.getChatType();
            String conversationId = buildConversationId(chatMsg);

            if (chatType == 1) {
                Long senderId = Long.parseLong(chatMsg.getSenderId());
                Long receiverId = Long.parseLong(chatMsg.getReceiverId());
                insertUserMessageIgnoreDuplicate(senderId, msgId, conversationId);
                insertUserMessageIgnoreDuplicate(receiverId, msgId, conversationId);
            } else if (chatType == 2) {
                Long senderId = Long.parseLong(chatMsg.getSenderId());
                insertUserMessageIgnoreDuplicate(senderId, msgId, conversationId);

                String groupId = chatMsg.getReceiverId();
                List<Long> memberUserIds = groupMemberProvider.getGroupMemberUserIds(groupId);
                for (Long memberUserId : memberUserIds) {
                    if (!memberUserId.equals(senderId)) {
                        insertUserMessageIgnoreDuplicate(memberUserId, msgId, conversationId);
                    }
                }
            }

            logger.debug("消息入库成功: msgId={}, from={}, type={}",
                    msgId, chatMsg.getSenderId(), chatType == 1 ? "单聊" : "群聊");

        } catch (Exception e) {
            logger.error("消息入库失败", e);
            throw new RuntimeException("消息入库失败", e);
        }
    }

    private void insertUserMessageIgnoreDuplicate(Long userId, String msgId, String conversationId) {
        try {
            ImUserMessage um = new ImUserMessage();
            um.setUserId(userId);
            um.setMsgId(msgId);
            um.setConversationId(conversationId);
            um.setStatus(0);
            um.setCreatedAt(LocalDateTime.now());
            messageManager.insertUserMessage(um);
        } catch (DuplicateKeyException e) {
            logger.debug("用户消息关联已存在，跳过: userId={}, msgId={}", userId, msgId);
        }
    }

    private ImMessage buildImMessage(ImProto.ChatMessage chatMsg) {
        ImMessage entity = new ImMessage();
        entity.setMsgId(chatMsg.getMsgId());
        entity.setConversationId(buildConversationId(chatMsg));
        entity.setSenderId(Long.parseLong(chatMsg.getSenderId()));
        entity.setChatType(chatMsg.getChatType());
        entity.setContentType(chatMsg.getContentType());
        entity.setContent(chatMsg.getContent());
        if (chatMsg.getExtCount() > 0) {
            try {
                entity.setExt(new ObjectMapper().writeValueAsString(chatMsg.getExtMap()));
            } catch (Exception e) {
                logger.warn("ext 序列化失败, msgId={}", chatMsg.getMsgId(), e);
            }
        }
        entity.setStatus(0);
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }

    private String buildConversationId(ImProto.ChatMessage chatMsg) {
        if (chatMsg.getConversationId() != null && !chatMsg.getConversationId().isEmpty()) {
            return chatMsg.getConversationId();
        }
        if (chatMsg.getChatType() == 2) {
            return "group_" + chatMsg.getReceiverId();
        }
        try {
            long senderId = Long.parseLong(chatMsg.getSenderId());
            long receiverId = Long.parseLong(chatMsg.getReceiverId());
            long minId = Math.min(senderId, receiverId);
            long maxId = Math.max(senderId, receiverId);
            return minId + "_" + maxId;
        } catch (NumberFormatException e) {
            return chatMsg.getSenderId() + "_" + chatMsg.getReceiverId();
        }
    }
}
