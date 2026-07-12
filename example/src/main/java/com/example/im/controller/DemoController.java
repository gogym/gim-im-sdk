package com.example.im.controller;

import io.getbit.gim.core.api.ApiResult;
import io.getbit.gim.friend.service.FriendService;
import io.getbit.gim.group.service.GroupService;
import io.getbit.gim.storage.service.MessageService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 业务对接示例 Controller
 * <p>
 * 展示如何在你的业务代码中直接注入和使用 SDK 提供的 Service。
 * <p>
 * 注意：SDK 已自动注册以下 HTTP API，你无需手动创建 Controller：
 * <ul>
 *   <li>好友管理: POST /im/friend/* (15 个端点)</li>
 *   <li>群组管理: POST /im/group/* (18 个端点)</li>
 *   <li>消息管理: POST /im/message/* (2 个端点)</li>
 * </ul>
 * <p>
 * 本示例展示如何在业务层组合使用 SDK Service 实现更复杂的业务逻辑。
 */
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    // SDK 自动注册的 Service，可直接注入使用
    private final FriendService friendService;
    private final GroupService groupService;
    private final MessageService messageService;

    public DemoController(FriendService friendService,
                          GroupService groupService,
                          MessageService messageService) {
        this.friendService = friendService;
        this.groupService = groupService;
        this.messageService = messageService;
    }

    /**
     * 示例：获取用户 IM 概览信息
     * 组合好友数量、群组数量、未读消息等业务数据
     */
    @PostMapping("/overview")
    public ApiResult<Map<String, Object>> getUserOverview(@RequestParam Long userId) {
        Map<String, Object> overview = new HashMap<>();

        // 使用 FriendService 获取好友列表
        overview.put("friendCount", friendService.getFriends(userId).size());

        // 使用 GroupService 获取群组列表
        overview.put("groupCount", groupService.getMyGroups(userId).size());

        // 可以继续使用 MessageService 获取未读消息数等
        // overview.put("recentMessages", messageService.syncMessages(userId, 0L, 10));

        return ApiResult.ok(overview);
    }

    /**
     * 示例：检查用户是否在指定群组中且与指定用户是好友
     */
    @PostMapping("/check-relationship")
    public ApiResult<Map<String, Boolean>> checkRelationship(
            @RequestParam Long userId,
            @RequestParam Long targetUserId,
            @RequestParam(required = false) String groupId) {

        Map<String, Boolean> result = new HashMap<>();
        result.put("isFriend", friendService.isFriend(userId, targetUserId));

        if (groupId != null) {
            result.put("isGroupMember", groupService.isGroupMember(groupId, userId));
        }

        return ApiResult.ok(result);
    }
}
