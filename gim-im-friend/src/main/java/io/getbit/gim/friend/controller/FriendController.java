package io.getbit.gim.friend.controller;

import io.getbit.gim.core.api.ApiResult;
import io.getbit.gim.core.spi.ImUserContextResolver;
import io.getbit.gim.friend.dto.FriendRequests;
import io.getbit.gim.friend.dto.FriendResponses;
import io.getbit.gim.friend.dto.FriendResponses.*;
import io.getbit.gim.friend.entity.ImFriendGroup;
import io.getbit.gim.friend.entity.ImFriendRequest;
import io.getbit.gim.friend.entity.ImFriendRequestLog;
import io.getbit.gim.friend.service.FriendService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 好友管理 HTTP 接口
 *
 * @author gogym
 */
@RestController
@RequestMapping("/im/friend")
public class FriendController {

    private final FriendService friendService;
    private final ImUserContextResolver userContextResolver;

    public FriendController(FriendService friendService, ImUserContextResolver userContextResolver) {
        this.friendService = friendService;
        this.userContextResolver = userContextResolver;
    }

    // ====================== 好友分组 ======================

    @PostMapping("/group/list")
    public ApiResult<List<ImFriendGroup>> listGroups() {
        Long userId = requireUserId();
        return ApiResult.ok(friendService.getFriendGroups(userId));
    }

    @PostMapping("/group/create")
    public ApiResult<ImFriendGroup> createGroup(@RequestBody FriendRequests.CreateFriendGroupReq req) {
        Long userId = requireUserId();
        return ApiResult.ok(friendService.createFriendGroup(userId, req.getName(), req.getSortOrder()));
    }

    @PostMapping("/group/update")
    public ApiResult<Void> updateGroup(@RequestBody FriendRequests.UpdateFriendGroupReq req) {
        Long userId = requireUserId();
        boolean ok = friendService.updateFriendGroup(userId, req.getGroupId(), req.getName(), req.getSortOrder());
        return ok ? ApiResult.ok() : ApiResult.fail("分组不存在或无权限");
    }

    @PostMapping("/group/delete")
    public ApiResult<Void> deleteGroup(@RequestBody FriendRequests.DeleteFriendGroupReq req) {
        Long userId = requireUserId();
        boolean ok = friendService.deleteFriendGroup(userId, req.getGroupId());
        return ok ? ApiResult.ok() : ApiResult.fail("分组不存在或无权限");
    }

    // ====================== 好友关系 ======================

    @PostMapping("/list")
    public ApiResult<List<FriendInfoDTO>> listFriends(@RequestBody(required = false) FriendRequests.ListFriendsReq req) {
        Long userId = requireUserId();
        List<FriendInfoDTO> friends = (req != null && req.getGroupId() != null)
                ? friendService.getFriendsByGroup(userId, req.getGroupId())
                : friendService.getFriends(userId);
        return ApiResult.ok(friends);
    }

    @PostMapping("/remark")
    public ApiResult<Void> setRemark(@RequestBody FriendRequests.SetFriendRemarkReq req) {
        Long userId = requireUserId();
        boolean ok = friendService.setFriendRemark(userId, req.getFriendId(), req.getRemark());
        return ok ? ApiResult.ok() : ApiResult.fail("好友不存在");
    }

    @PostMapping("/move")
    public ApiResult<Void> moveFriend(@RequestBody FriendRequests.MoveFriendReq req) {
        Long userId = requireUserId();
        boolean ok = friendService.moveFriendToGroup(userId, req.getFriendId(), req.getGroupId());
        return ok ? ApiResult.ok() : ApiResult.fail("好友不存在");
    }

    @PostMapping("/delete")
    public ApiResult<Void> deleteFriend(@RequestBody FriendRequests.DeleteFriendReq req) {
        Long userId = requireUserId();
        boolean ok = friendService.deleteFriend(userId, req.getFriendId());
        return ok ? ApiResult.ok() : ApiResult.fail("删除失败");
    }

    @PostMapping("/checkFriendship")
    public ApiResult<Boolean> checkFriendship(@RequestBody FriendRequests.CheckFriendshipReq req) {
        Long userId = requireUserId();
        return ApiResult.ok(friendService.isFriend(userId, req.getFriendId()));
    }

    // ====================== 好友申请 ======================

    @PostMapping("/request")
    public ApiResult<ImFriendRequest> sendRequest(@RequestBody FriendRequests.SendFriendReq req) {
        Long userId = requireUserId();
        try {
            ImFriendRequest request = friendService.sendFriendRequest(userId, req.getToUserId(), req.getMessage(), req.getSource());
            return ApiResult.ok(request);
        } catch (IllegalArgumentException e) {
            return ApiResult.fail(e.getMessage());
        }
    }

    @PostMapping("/request/handle")
    public ApiResult<Void> handleRequest(@RequestBody FriendRequests.HandleFriendReq req) {
        Long userId = requireUserId();
        boolean ok = friendService.handleFriendRequest(userId, req.getRequestId(), req.isAccept());
        return ok ? ApiResult.ok() : ApiResult.fail("申请不存在或已处理");
    }

    @PostMapping("/request/reply")
    public ApiResult<Void> replyRequest(@RequestBody FriendRequests.ReplyFriendRequestReq req) {
        Long userId = requireUserId();
        try {
            boolean ok = friendService.replyFriendRequest(userId, req.getRequestId(), req.getMessage());
            return ok ? ApiResult.ok() : ApiResult.fail("回复失败");
        } catch (IllegalArgumentException e) {
            return ApiResult.fail(e.getMessage());
        }
    }

    @PostMapping("/request/pending")
    public ApiResult<List<ReceivedFriendRequestDTO>> pendingRequests() {
        Long userId = requireUserId();
        return ApiResult.ok(friendService.getReceivedRequests(userId));
    }

    @PostMapping("/request/logs")
    public ApiResult<List<ImFriendRequestLog>> requestLogs(@RequestBody FriendRequests.FriendRequestLogsReq req) {
        requireUserId();
        return ApiResult.ok(friendService.getRequestLogs(req.getRequestId()));
    }

    @PostMapping("/request/detail")
    public ApiResult<FriendRequestHistoryDTO> requestDetail(@RequestBody FriendRequests.FriendRequestLogsReq req) {
        requireUserId();
        return ApiResult.ok(friendService.getRequestDetail(req.getRequestId()));
    }

    @PostMapping("/request/sent")
    public ApiResult<List<SentFriendRequestDTO>> sentRequests() {
        Long userId = requireUserId();
        return ApiResult.ok(friendService.getSentRequests(userId));
    }

    @PostMapping("/request/between")
    public ApiResult<FriendRequestHistoryDTO> requestBetween(@RequestBody FriendRequests.FriendRequestBetweenReq req) {
        Long userId = requireUserId();
        return ApiResult.ok(friendService.getRequestHistory(userId, req.getTargetUserId()));
    }

    private Long requireUserId() {
        Long userId = userContextResolver.getCurrentUserId();
        if (userId == null) throw new IllegalStateException("未登录");
        return userId;
    }
}
