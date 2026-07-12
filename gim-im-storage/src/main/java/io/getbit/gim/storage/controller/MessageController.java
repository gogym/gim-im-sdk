package io.getbit.gim.storage.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.getbit.gim.core.api.ApiResult;
import io.getbit.gim.core.spi.ImUserContextResolver;
import io.getbit.gim.storage.dto.MessageRequests;
import io.getbit.gim.storage.entity.ImMessage;
import io.getbit.gim.storage.service.MessageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 消息管理 HTTP 接口（读扩散模型）
 * 提供消息历史拉取、离线消息同步等 API
 *
 * @author gogym
 */
@RestController
@RequestMapping("/im/message")
public class MessageController {

    private final MessageService messageService;
    private final ImUserContextResolver userContextResolver;

    public MessageController(MessageService messageService, ImUserContextResolver userContextResolver) {
        this.messageService = messageService;
        this.userContextResolver = userContextResolver;
    }

    /** 同步消息（离线消息增量拉取） */
    @PostMapping("/sync")
    public ApiResult<List<ImMessage>> syncMessages(@RequestBody MessageRequests.SyncMessageReq req) {
        Long userId = requireUserId();
        return ApiResult.ok(messageService.syncMessages(userId, req.getSinceTimestamp(), req.getLimit()));
    }

    /** 查询会话消息历史（游标分页） */
    @PostMapping("/history")
    public ApiResult<Page<ImMessage>> getHistory(@RequestBody MessageRequests.MessageHistoryReq req) {
        Long userId = requireUserId();
        try {
            Page<ImMessage> page = messageService.getHistory(
                    userId, req.getConversationId(), req.getBeforeMsgId(), req.getPageSize());
            return ApiResult.ok(page);
        } catch (IllegalArgumentException e) {
            return ApiResult.fail(e.getMessage());
        }
    }

    private Long requireUserId() {
        Long userId = userContextResolver.getCurrentUserId();
        if (userId == null) throw new IllegalStateException("未登录");
        return userId;
    }
}
