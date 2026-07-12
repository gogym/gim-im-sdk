package io.getbit.gim.storage.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.getbit.gim.storage.entity.ImMessage;
import io.getbit.gim.storage.manager.ImMessageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 消息管理服务
 * 提供消息同步、历史消息查询等业务逻辑
 *
 * @author gogym
 */
public class MessageService {

    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);

    private final ImMessageManager messageManager;

    public MessageService(ImMessageManager messageManager) {
        this.messageManager = messageManager;
    }

    /**
     * 同步消息（离线消息增量拉取）
     * 客户端上线后调用，拉取 sinceTimestamp 之后发给当前用户的消息。
     * sinceTimestamp 为 0 时返回最近消息。
     *
     * @param userId         当前用户 ID
     * @param sinceTimestamp 起始时间戳（毫秒），为 0 时返回最近消息
     * @param limit          拉取条数，默认 50，最大 200
     * @return 消息列表
     */
    public List<ImMessage> syncMessages(Long userId, Long sinceTimestamp, Integer limit) {
        long since = sinceTimestamp != null ? sinceTimestamp : 0L;
        int maxLimit = Math.min(limit != null ? limit : 50, 200);

        // 最大回溯窗口：7 天
        long minTimestamp = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000;
        if (since < minTimestamp) {
            since = minTimestamp;
        }

        LocalDateTime sinceTime = since > 0
                ? Instant.ofEpochMilli(since).atZone(ZoneId.systemDefault()).toLocalDateTime()
                : null;

        return messageManager.syncMessages(userId, sinceTime, maxLimit);
    }

    /**
     * 查询会话消息历史（游标分页）
     * 读扩散模型：先查用户-消息关联表，再加载消息本体
     *
     * @param userId         当前用户 ID
     * @param conversationId 会话 ID
     * @param beforeMsgId    游标消息 ID（获取此消息之前的历史，为空则从最新开始）
     * @param pageSize       每页条数，默认 30，最大 100
     * @return 分页消息
     */
    public Page<ImMessage> getHistory(Long userId, String conversationId, String beforeMsgId, Integer pageSize) {
        if (conversationId == null || conversationId.isEmpty()) {
            throw new IllegalArgumentException("conversationId 不能为空");
        }

        int size = Math.min(pageSize != null ? pageSize : 30, 100);

        // 解析游标
        LocalDateTime beforeTime = null;
        if (beforeMsgId != null && !beforeMsgId.isEmpty()) {
            ImMessage cursorMsg = messageManager.findByMsgId(beforeMsgId);
            if (cursorMsg != null && cursorMsg.getCreatedAt() != null) {
                beforeTime = cursorMsg.getCreatedAt();
            }
        }

        return messageManager.selectConversationHistory(conversationId, userId, beforeTime, size);
    }
}
