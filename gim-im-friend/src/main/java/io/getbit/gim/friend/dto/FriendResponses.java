package io.getbit.gim.friend.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 好友管理响应 DTO 集合
 *
 * @author gogym
 */
public final class FriendResponses {

    private FriendResponses() {}

    /**
     * 好友信息（含用户昵称、头像）
     */
    @Data
    public static class FriendInfoDTO {
        private Long id;
        private Long friendId;
        private Long groupId;
        private String remark;
        private Integer status;
        private LocalDateTime createdAt;
        private String nickname;
        private String avatar;
        private String account;
    }

    /**
     * 收到的好友申请（含发送方用户信息 + 最新消息）
     */
    @Data
    public static class ReceivedFriendRequestDTO {
        private Long id;
        private Long fromUserId;
        private String fromUserNickname;
        private String fromUserAvatar;
        private String fromUserAccount;
        private String message;
        private Integer status;
        private String source;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String latestMessage;
    }

    /**
     * 发送的好友申请（含目标用户信息 + 最新消息）
     */
    @Data
    public static class SentFriendRequestDTO {
        private Long id;
        private Long toUserId;
        private String toUserNickname;
        private String toUserAvatar;
        private String toUserAccount;
        private String message;
        private Integer status;
        private String source;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String latestMessage;
    }

    /**
     * 好友申请详情（含消息日志）
     */
    @Data
    public static class FriendRequestHistoryDTO {
        private Long id;
        private Long fromUserId;
        private Long toUserId;
        private String message;
        private Integer status;
        private String source;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<RequestLogItem> logs;

        @Data
        public static class RequestLogItem {
            private Long id;
            private Long requestId;
            private Long fromUserId;
            private String message;
            private LocalDateTime createdAt;
        }
    }
}
