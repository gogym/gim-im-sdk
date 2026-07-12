package io.getbit.gim.group.dto;

import lombok.Data;

import java.util.List;

/**
 * 群组管理请求 DTO 集合
 *
 * @author gogym
 */
public final class GroupRequests {

    private GroupRequests() {}

    @Data
    public static class CreateGroupReq {
        private String name;
        private String avatar;
        private List<Long> memberIds;
    }

    @Data
    public static class GroupIdReq {
        private String groupId;
    }

    @Data
    public static class UpdateGroupReq {
        private String groupId;
        private String name;
        private String avatar;
        private String announcement;
    }

    @Data
    public static class GroupMemberReq {
        private String groupId;
        private List<Long> userIds;
    }

    @Data
    public static class GroupTargetUserReq {
        private String groupId;
        private Long targetUserId;
    }

    @Data
    public static class TransferOwnerReq {
        private String groupId;
        private Long newOwnerId;
    }

    @Data
    public static class SetAdminReq {
        private String groupId;
        private Long targetUserId;
        private boolean admin;
    }

    @Data
    public static class MuteMemberReq {
        private String groupId;
        private Long targetUserId;
        private boolean muted;
    }

    @Data
    public static class MuteAllReq {
        private String groupId;
        private boolean muteAll;
    }

    @Data
    public static class SetNicknameReq {
        private String groupId;
        private String nickname;
    }

    @Data
    public static class ApplyJoinGroupReq {
        private String groupId;
        private String message;
    }

    @Data
    public static class HandleJoinReq {
        private String groupId;
        private Long applicantId;
        private boolean approve;
    }

    @Data
    public static class SetJoinVerifyReq {
        private String groupId;
        private boolean joinVerify;
    }
}
