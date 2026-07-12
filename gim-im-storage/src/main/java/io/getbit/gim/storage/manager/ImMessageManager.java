package io.getbit.gim.storage.manager;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.getbit.gim.storage.entity.ImMessage;
import io.getbit.gim.storage.entity.ImUserMessage;
import io.getbit.gim.storage.repository.ImMessageRepository;
import io.getbit.gim.storage.repository.ImUserMessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 消息数据管理器（读扩散模型）
 *
 * @author gogym
 */
@Slf4j
@Component
public class ImMessageManager {

    private final ImMessageRepository imMessageRepository;
    private final ImUserMessageRepository imUserMessageRepository;

    public ImMessageManager(ImMessageRepository imMessageRepository,
                            ImUserMessageRepository imUserMessageRepository) {
        this.imMessageRepository = imMessageRepository;
        this.imUserMessageRepository = imUserMessageRepository;
    }

    public boolean insertMessage(ImMessage entity) {
        return imMessageRepository.insert(entity) > 0;
    }

    public ImMessage findByMsgId(String msgId) {
        return imMessageRepository.findByMsgId(msgId);
    }

    public int recallByMsgId(String msgId, String newContent, String newExt) {
        int rows = imMessageRepository.recallByMsgId(msgId, newContent, newExt);
        imUserMessageRepository.markAsRecalled(msgId);
        return rows;
    }

    public boolean insertUserMessage(ImUserMessage entity) {
        return imUserMessageRepository.insert(entity) > 0;
    }

    public ImUserMessage findUserMessage(Long userId, String msgId) {
        return imUserMessageRepository.findByUserIdAndMsgId(userId, msgId);
    }

    public List<ImMessage> syncMessages(Long userId, LocalDateTime sinceTime, int maxLimit) {
        List<ImUserMessage> userMessages = imUserMessageRepository.syncMessages(userId, sinceTime, maxLimit);
        if (userMessages.isEmpty()) {
            return Collections.emptyList();
        }
        return batchLoadMessages(userMessages);
    }

    public Page<ImMessage> selectConversationHistory(String conversationId, Long userId,
                                                      LocalDateTime beforeTime, int pageSize) {
        List<ImUserMessage> userMessages = imUserMessageRepository.findUserMessages(
                userId, conversationId, beforeTime, pageSize);
        List<ImMessage> messages = batchLoadMessages(userMessages);
        Page<ImMessage> page = new Page<>(1, pageSize, 0);
        page.setRecords(messages);
        return page;
    }

    public Long countUnread(Long userId) {
        return imUserMessageRepository.countUnread(userId);
    }

    public int markAsRead(Long userId, String conversationId) {
        return imUserMessageRepository.markAsRead(userId, conversationId);
    }

    public List<Long> findUserIdsByMsgId(String msgId) {
        List<ImUserMessage> userMessages = imUserMessageRepository.findByMsgId(msgId);
        return userMessages.stream()
                .map(ImUserMessage::getUserId)
                .collect(Collectors.toList());
    }

    public int deleteUserMessages(Long userId, String conversationId) {
        return imUserMessageRepository.deleteByConversationId(userId, conversationId);
    }

    private List<ImMessage> batchLoadMessages(List<ImUserMessage> userMessages) {
        if (userMessages == null || userMessages.isEmpty()) {
            return Collections.emptyList();
        }
        List<ImMessage> messages = new ArrayList<>(userMessages.size());
        for (ImUserMessage um : userMessages) {
            ImMessage msg = imMessageRepository.findByMsgId(um.getMsgId());
            if (msg != null) {
                messages.add(msg);
            }
        }
        return messages;
    }
}
