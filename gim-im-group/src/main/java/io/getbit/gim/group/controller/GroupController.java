package io.getbit.gim.group.controller;

import io.getbit.gim.core.api.ApiResult;
import io.getbit.gim.core.spi.ImUserContextResolver;
import io.getbit.gim.group.dto.GroupMemberInfoDTO;
import io.getbit.gim.group.dto.GroupRequests;
import io.getbit.gim.group.entity.ImGroup;
import io.getbit.gim.group.entity.ImGroupJoinRequest;
import io.getbit.gim.group.service.GroupService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 群组管理 HTTP 接口
 *
 * @author gogym
 */
@RestController
@RequestMapping("/im/group")
public class GroupController {

    private final GroupService groupService;
    private final ImUserContextResolver userContextResolver;

    public GroupController(GroupService groupService, ImUserContextResolver userContextResolver) {
        this.groupService = groupService;
        this.userContextResolver = userContextResolver;
    }

    // ====================== 群组管理 ======================

    @PostMapping("/create")
    public ApiResult<ImGroup> createGroup(@RequestBody GroupRequests.CreateGroupReq req) {
        Long userId = requireUserId();
        return ApiResult.ok(groupService.createGroup(userId, req.getName(), req.getAvatar(), req.getMemberIds()));
    }

    @PostMapping("/info")
    public ApiResult<ImGroup> getGroupInfo(@RequestBody GroupRequests.GroupIdReq req) {
        requireUserId();
        ImGroup group = groupService.getGroup(req.getGroupId());
        return group != null ? ApiResult.ok(group) : ApiResult.fail("群组不存在");
    }

    @PostMapping("/update")
    public ApiResult<Void> updateGroup(@RequestBody GroupRequests.UpdateGroupReq req) {
        Long userId = requireUserId();
        boolean ok = groupService.updateGroupInfo(userId, req.getGroupId(), req.getName(), req.getAvatar(), req.getAnnouncement());
        return ok ? ApiResult.ok() : ApiResult.fail("无权修改或群不存在");
    }

    @PostMapping("/dissolve")
    public ApiResult<Void> dissolveGroup(@RequestBody GroupRequests.GroupIdReq req) {
        Long userId = requireUserId();
        boolean ok = groupService.dissolveGroup(userId, req.getGroupId());
        return ok ? ApiResult.ok() : ApiResult.fail("仅群主可解散群");
    }

    // ====================== 成员管理 ======================

    @PostMapping("/invite")
    public ApiResult<Void> inviteMembers(@RequestBody GroupRequests.GroupMemberReq req) {
        Long userId = requireUserId();
        boolean ok = groupService.inviteMembers(userId, req.getGroupId(), req.getUserIds());
        return ok ? ApiResult.ok() : ApiResult.fail("邀请失败");
    }

    @PostMapping("/remove")
    public ApiResult<Void> removeMember(@RequestBody GroupRequests.GroupTargetUserReq req) {
        Long userId = requireUserId();
        boolean ok = groupService.removeMember(userId, req.getGroupId(), req.getTargetUserId());
        return ok ? ApiResult.ok() : ApiResult.fail("无权移除或目标不是群成员");
    }

    @PostMapping("/quit")
    public ApiResult<Void> quitGroup(@RequestBody GroupRequests.GroupIdReq req) {
        Long userId = requireUserId();
        boolean ok = groupService.quitGroup(userId, req.getGroupId());
        return ok ? ApiResult.ok() : ApiResult.fail("群主不能退出，请先转让群主");
    }

    @PostMapping("/transfer")
    public ApiResult<Void> transferOwner(@RequestBody GroupRequests.TransferOwnerReq req) {
        Long userId = requireUserId();
        boolean ok = groupService.transferOwner(userId, req.getGroupId(), req.getNewOwnerId());
        return ok ? ApiResult.ok() : ApiResult.fail("仅群主可转让");
    }

    @PostMapping("/setAdmin")
    public ApiResult<Void> setAdmin(@RequestBody GroupRequests.SetAdminReq req) {
        Long userId = requireUserId();
        boolean ok = groupService.setAdmin(userId, req.getGroupId(), req.getTargetUserId(), req.isAdmin());
        return ok ? ApiResult.ok() : ApiResult.fail("仅群主可设置管理员");
    }

    @PostMapping("/mute")
    public ApiResult<Void> muteMember(@RequestBody GroupRequests.MuteMemberReq req) {
        Long userId = requireUserId();
        boolean ok = groupService.muteMember(userId, req.getGroupId(), req.getTargetUserId(), req.isMuted());
        return ok ? ApiResult.ok() : ApiResult.fail("无权禁言");
    }

    @PostMapping("/muteAll")
    public ApiResult<Void> muteAll(@RequestBody GroupRequests.MuteAllReq req) {
        Long userId = requireUserId();
        boolean ok = groupService.muteAll(userId, req.getGroupId(), req.isMuteAll());
        return ok ? ApiResult.ok() : ApiResult.fail("无权操作");
    }

    @PostMapping("/nickname")
    public ApiResult<Void> setNickname(@RequestBody GroupRequests.SetNicknameReq req) {
        Long userId = requireUserId();
        boolean ok = groupService.setNickname(userId, req.getGroupId(), req.getNickname());
        return ok ? ApiResult.ok() : ApiResult.fail("设置失败");
    }

    // ====================== 查询 ======================

    @PostMapping("/myGroups")
    public ApiResult<List<ImGroup>> myGroups() {
        Long userId = requireUserId();
        return ApiResult.ok(groupService.getMyGroups(userId));
    }

    @PostMapping("/members")
    public ApiResult<List<GroupMemberInfoDTO>> getMembers(@RequestBody GroupRequests.GroupIdReq req) {
        requireUserId();
        return ApiResult.ok(groupService.getGroupMembersWithUserInfo(req.getGroupId()));
    }

    // ====================== 入群验证 ======================

    @PostMapping("/apply")
    public ApiResult<Void> applyJoinGroup(@RequestBody GroupRequests.ApplyJoinGroupReq req) {
        Long userId = requireUserId();
        boolean ok = groupService.applyJoinGroup(userId, req.getGroupId(), req.getMessage());
        return ok ? ApiResult.ok() : ApiResult.fail("申请失败，请检查是否已是群成员或有待审批申请");
    }

    @PostMapping("/handleJoin")
    public ApiResult<Void> handleJoinRequest(@RequestBody GroupRequests.HandleJoinReq req) {
        Long userId = requireUserId();
        boolean ok = groupService.handleJoinRequest(userId, req.getGroupId(), req.getApplicantId(), req.isApprove());
        return ok ? ApiResult.ok() : ApiResult.fail("无权处理或申请不存在");
    }

    @PostMapping("/joinRequests")
    public ApiResult<List<ImGroupJoinRequest>> getJoinRequests(@RequestBody GroupRequests.GroupIdReq req) {
        Long userId = requireUserId();
        return ApiResult.ok(groupService.getPendingJoinRequests(userId, req.getGroupId()));
    }

    @PostMapping("/setJoinVerify")
    public ApiResult<Void> setJoinVerify(@RequestBody GroupRequests.SetJoinVerifyReq req) {
        Long userId = requireUserId();
        boolean ok = groupService.setJoinVerify(userId, req.getGroupId(), req.isJoinVerify());
        return ok ? ApiResult.ok() : ApiResult.fail("仅群主可设置");
    }

    private Long requireUserId() {
        Long userId = userContextResolver.getCurrentUserId();
        if (userId == null) throw new IllegalStateException("未登录");
        return userId;
    }
}
