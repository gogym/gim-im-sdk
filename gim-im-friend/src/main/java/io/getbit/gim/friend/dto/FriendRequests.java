package io.getbit.gim.friend.dto;

import lombok.Data;

/**
 * 好友管理请求 DTO 集合
 *
 * @author gogym
 */
public final class FriendRequests {

    private FriendRequests() {}

    @Data
    public static class CreateFriendGroupReq {
        private String name;
        private Integer sortOrder;
    }

    @Data
    public static class UpdateFriendGroupReq {
        private Long groupId;
        private String name;
        private Integer sortOrder;
    }

    @Data
    public static class DeleteFriendGroupReq {
        private Long groupId;
    }

    @Data
    public static class ListFriendsReq {
        private Long groupId;
    }

    @Data
    public static class SetFriendRemarkReq {
        private Long friendId;
        private String remark;
    }

    @Data
    public static class MoveFriendReq {
        private Long friendId;
        private Long groupId;
    }

    @Data
    public static class DeleteFriendReq {
        private Long friendId;
    }

    @Data
    public static class CheckFriendshipReq {
        private Long friendId;
    }

    @Data
    public static class SendFriendReq {
        private Long toUserId;
        private String message;
        private String source;
    }

    @Data
    public static class HandleFriendReq {
        private Long requestId;
        private boolean accept;
    }

    @Data
    public static class ReplyFriendRequestReq {
        private Long requestId;
        private String message;
    }

    @Data
    public static class FriendRequestLogsReq {
        private Long requestId;
    }

    @Data
    public static class FriendRequestBetweenReq {
        private Long targetUserId;
    }
}
