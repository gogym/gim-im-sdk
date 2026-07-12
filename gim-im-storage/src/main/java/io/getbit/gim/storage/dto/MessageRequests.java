package io.getbit.gim.storage.dto;

import lombok.Data;

/**
 * 消息管理请求 DTO 集合
 *
 * @author gogym
 */
public final class MessageRequests {

    private MessageRequests() {}

    @Data
    public static class SyncMessageReq {
        /** 起始时间戳（毫秒），为 0 时返回最近消息 */
        private Long sinceTimestamp;
        /** 拉取条数，默认 50，最大 200 */
        private Integer limit;
    }

    @Data
    public static class MessageHistoryReq {
        /** 会话 ID */
        private String conversationId;
        /** 游标：获取此消息 ID 之前的历史（为空则从最新开始） */
        private String beforeMsgId;
        /** 每页条数，默认 30，最大 100 */
        private Integer pageSize;
    }
}
