package io.getbit.gim.friend.service;

import io.getbit.gim.friend.dto.FriendResponses.*;
import io.getbit.gim.friend.entity.ImFriend;
import io.getbit.gim.friend.entity.ImFriendGroup;
import io.getbit.gim.friend.entity.ImFriendRequest;
import io.getbit.gim.friend.entity.ImFriendRequestLog;
import io.getbit.gim.friend.manager.ImFriendManager;
import io.getbit.gim.friend.manager.ImFriendRequestLogManager;
import io.getbit.gim.friend.manager.ImFriendRequestManager;
import io.getbit.gim.friend.notify.FriendNotifyHandler;
import io.getbit.gim.friend.spi.ImUserInfoProvider;
import io.getbit.gim.friend.spi.ImUserInfoProvider.ImUserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 好友管理服务
 *
 * @author gogym
 */
public class FriendService {

    private static final Logger logger = LoggerFactory.getLogger(FriendService.class);

    private final ImFriendManager friendManager;
    private final ImFriendRequestManager friendRequestManager;
    private final ImFriendRequestLogManager friendRequestLogManager;
    private final FriendNotifyHandler friendNotifyHandler;
    private final ImUserInfoProvider userInfoProvider;

    /** 可选依赖：消息管理器，用于删除好友时清理会话消息 */
    @Autowired(required = false)
    private io.getbit.gim.storage.manager.ImMessageManager messageManager;

    public FriendService(ImFriendManager friendManager,
                         ImFriendRequestManager friendRequestManager,
                         ImFriendRequestLogManager friendRequestLogManager,
                         FriendNotifyHandler friendNotifyHandler,
                         ImUserInfoProvider userInfoProvider) {
        this.friendManager = friendManager;
        this.friendRequestManager = friendRequestManager;
        this.friendRequestLogManager = friendRequestLogManager;
        this.friendNotifyHandler = friendNotifyHandler;
        this.userInfoProvider = userInfoProvider;
    }

    // === 好友分组 ===

    public List<ImFriendGroup> getFriendGroups(Long userId) {
        return friendManager.findFriendGroupsByUserId(userId);
    }

    public ImFriendGroup createFriendGroup(Long userId, String name, Integer sortOrder) {
        ImFriendGroup group = new ImFriendGroup();
        group.setUserId(userId);
        group.setName(name);
        group.setSortOrder(sortOrder != null ? sortOrder : 0);
        friendManager.insertFriendGroup(group);
        return group;
    }

    public boolean updateFriendGroup(Long userId, Long groupId, String name, Integer sortOrder) {
        ImFriendGroup group = friendManager.findFriendGroupById(groupId);
        if (group == null || !group.getUserId().equals(userId)) return false;
        if (name != null) group.setName(name);
        if (sortOrder != null) group.setSortOrder(sortOrder);
        friendManager.updateFriendGroup(group);
        return true;
    }

    public boolean deleteFriendGroup(Long userId, Long groupId) {
        ImFriendGroup group = friendManager.findFriendGroupById(groupId);
        if (group == null || !group.getUserId().equals(userId)) return false;
        friendManager.deleteFriendGroup(groupId);
        return true;
    }

    // === 好友关系（丰富化 DTO）===

    /**
     * 获取好友列表（含用户昵称、头像）
     */
    public List<FriendInfoDTO> getFriends(Long userId) {
        List<ImFriend> friends = friendManager.findFriendsByUserId(userId);
        return friends.stream().map(this::toFriendInfoDTO).collect(Collectors.toList());
    }

    /**
     * 按分组获取好友列表（含用户昵称、头像）
     */
    public List<FriendInfoDTO> getFriendsByGroup(Long userId, Long groupId) {
        List<ImFriend> friends = friendManager.findFriendsByGroupId(userId, groupId);
        return friends.stream().map(this::toFriendInfoDTO).collect(Collectors.toList());
    }

    public boolean setFriendRemark(Long userId, Long friendId, String remark) {
        ImFriend friend = friendManager.findFriend(userId, friendId);
        if (friend == null) return false;
        friend.setRemark(remark);
        friendManager.updateFriend(friend);
        return true;
    }

    public boolean moveFriendToGroup(Long userId, Long friendId, Long groupId) {
        ImFriend friend = friendManager.findFriend(userId, friendId);
        if (friend == null) return false;
        friend.setGroupId(groupId);
        friendManager.updateFriend(friend);
        return true;
    }

    @Transactional
    public boolean deleteFriend(Long userId, Long friendId) {
        friendManager.deleteFriend(userId, friendId);
        friendManager.deleteFriend(friendId, userId);
        friendNotifyHandler.notifyFriendDeleted(userId, friendId);

        // 联动清理会话消息（如果 storage 模块存在）
        if (messageManager != null) {
            long minId = Math.min(userId, friendId);
            long maxId = Math.max(userId, friendId);
            String conversationId = minId + "_" + maxId;
            try {
                messageManager.deleteUserMessages(userId, conversationId);
                messageManager.deleteUserMessages(friendId, conversationId);
            } catch (Exception e) {
                logger.warn("清理好友会话消息失败: conversationId={}", conversationId, e);
            }
        }

        logger.info("删除好友: userId={}, friendId={}", userId, friendId);
        return true;
    }

    public boolean isFriend(Long userId, Long friendId) {
        return friendManager.isFriend(userId, friendId);
    }

    // === 好友申请 ===

    @Transactional
    public ImFriendRequest sendFriendRequest(Long fromUserId, Long toUserId, String message, String source) {
        if (fromUserId.equals(toUserId)) throw new IllegalArgumentException("不能添加自己为好友");
        if (friendManager.isFriend(fromUserId, toUserId)) throw new IllegalArgumentException("已经是好友");

        ImFriendRequest existing = friendRequestManager.findLatestRequest(fromUserId, toUserId);
        ImFriendRequest request;
        if (existing != null) {
            existing.setStatus(0);
            existing.setMessage(message);
            if (source != null) existing.setSource(source);
            friendRequestManager.updateById(existing);
            request = existing;
        } else {
            request = new ImFriendRequest();
            request.setFromUserId(fromUserId);
            request.setToUserId(toUserId);
            request.setMessage(message);
            request.setSource(source);
            request.setStatus(0);
            friendRequestManager.insert(request);
        }

        ImFriendRequestLog log = new ImFriendRequestLog();
        log.setRequestId(request.getId());
        log.setFromUserId(fromUserId);
        log.setMessage(message);
        friendRequestLogManager.insert(log);

        String nickname = null, avatar = null;
        try {
            ImUserInfo info = userInfoProvider.getUserById(fromUserId);
            if (info != null) { nickname = info.nickname(); avatar = info.avatar(); }
        } catch (Exception e) { logger.warn("获取申请人信息失败: userId={}", fromUserId, e); }

        friendNotifyHandler.notifyFriendRequest(fromUserId, toUserId, nickname, avatar, message);
        return request;
    }

    @Transactional
    public boolean handleFriendRequest(Long toUserId, Long requestId, boolean accept) {
        ImFriendRequest request = friendRequestManager.selectById(requestId);
        if (request == null || !request.getToUserId().equals(toUserId) || request.getStatus() != 0) return false;

        if (accept) {
            request.setStatus(1);
            ImFriend friend1 = new ImFriend();
            friend1.setUserId(request.getFromUserId()); friend1.setFriendId(request.getToUserId()); friend1.setStatus(1);
            friendManager.insertFriend(friend1);
            ImFriend friend2 = new ImFriend();
            friend2.setUserId(request.getToUserId()); friend2.setFriendId(request.getFromUserId()); friend2.setStatus(1);
            friendManager.insertFriend(friend2);
            friendNotifyHandler.syncSingleFriendOnlineStatus(request.getFromUserId().toString(), toUserId.toString());
            friendNotifyHandler.syncSingleFriendOnlineStatus(toUserId.toString(), request.getFromUserId().toString());
        } else {
            request.setStatus(2);
        }
        friendRequestManager.updateById(request);

        ImFriendRequestLog log = new ImFriendRequestLog();
        log.setRequestId(requestId); log.setFromUserId(toUserId);
        log.setMessage(accept ? "已同意好友申请" : "已拒绝好友申请");
        friendRequestLogManager.insert(log);

        friendNotifyHandler.notifyFriendRequestResult(toUserId, request.getFromUserId(), accept);
        return true;
    }

    /**
     * 回复好友申请（发送消息但不改变状态）
     */
    @Transactional
    public boolean replyFriendRequest(Long userId, Long requestId, String message) {
        ImFriendRequest request = friendRequestManager.selectById(requestId);
        if (request == null) throw new IllegalArgumentException("申请不存在");
        if (!request.getFromUserId().equals(userId) && !request.getToUserId().equals(userId)) {
            throw new IllegalArgumentException("无权回复此申请");
        }

        ImFriendRequestLog log = new ImFriendRequestLog();
        log.setRequestId(requestId);
        log.setFromUserId(userId);
        log.setMessage(message);
        friendRequestLogManager.insert(log);
        return true;
    }

    /**
     * 获取收到的好友申请列表（含发送方用户信息）
     */
    public List<ReceivedFriendRequestDTO> getReceivedRequests(Long toUserId) {
        List<ImFriendRequest> requests = friendRequestManager.findAllByToUserId(toUserId);
        return requests.stream().map(req -> {
            ReceivedFriendRequestDTO dto = new ReceivedFriendRequestDTO();
            dto.setId(req.getId());
            dto.setFromUserId(req.getFromUserId());
            dto.setMessage(req.getMessage());
            dto.setStatus(req.getStatus());
            dto.setSource(req.getSource());
            dto.setCreatedAt(req.getCreatedAt());
            dto.setUpdatedAt(req.getUpdatedAt());

            // 丰富化用户信息
            try {
                ImUserInfo info = userInfoProvider.getUserById(req.getFromUserId());
                if (info != null) {
                    dto.setFromUserNickname(info.nickname());
                    dto.setFromUserAvatar(info.avatar());
                    dto.setFromUserAccount(info.account());
                }
            } catch (Exception e) { logger.warn("获取用户信息失败: userId={}", req.getFromUserId(), e); }

            // 获取最新消息
            List<ImFriendRequestLog> logs = friendRequestLogManager.findByRequestId(req.getId());
            if (!logs.isEmpty()) {
                dto.setLatestMessage(logs.get(logs.size() - 1).getMessage());
            }
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 获取发送的好友申请列表（含目标用户信息）
     */
    public List<SentFriendRequestDTO> getSentRequests(Long fromUserId) {
        List<ImFriendRequest> requests = friendRequestManager.findSentByUserId(fromUserId);
        return requests.stream().map(req -> {
            SentFriendRequestDTO dto = new SentFriendRequestDTO();
            dto.setId(req.getId());
            dto.setToUserId(req.getToUserId());
            dto.setMessage(req.getMessage());
            dto.setStatus(req.getStatus());
            dto.setSource(req.getSource());
            dto.setCreatedAt(req.getCreatedAt());
            dto.setUpdatedAt(req.getUpdatedAt());

            try {
                ImUserInfo info = userInfoProvider.getUserById(req.getToUserId());
                if (info != null) {
                    dto.setToUserNickname(info.nickname());
                    dto.setToUserAvatar(info.avatar());
                    dto.setToUserAccount(info.account());
                }
            } catch (Exception e) { logger.warn("获取用户信息失败: userId={}", req.getToUserId(), e); }

            List<ImFriendRequestLog> logs = friendRequestLogManager.findByRequestId(req.getId());
            if (!logs.isEmpty()) {
                dto.setLatestMessage(logs.get(logs.size() - 1).getMessage());
            }
            return dto;
        }).collect(Collectors.toList());
    }

    public List<ImFriendRequestLog> getRequestLogs(Long requestId) {
        return friendRequestLogManager.findByRequestId(requestId);
    }

    public ImFriendRequest getRequestById(Long requestId) {
        return friendRequestManager.selectById(requestId);
    }

    /**
     * 获取好友申请详情（含消息日志）
     */
    public FriendRequestHistoryDTO getRequestDetail(Long requestId) {
        ImFriendRequest request = friendRequestManager.selectById(requestId);
        if (request == null) return null;

        FriendRequestHistoryDTO dto = new FriendRequestHistoryDTO();
        dto.setId(request.getId());
        dto.setFromUserId(request.getFromUserId());
        dto.setToUserId(request.getToUserId());
        dto.setMessage(request.getMessage());
        dto.setStatus(request.getStatus());
        dto.setSource(request.getSource());
        dto.setCreatedAt(request.getCreatedAt());
        dto.setUpdatedAt(request.getUpdatedAt());

        List<ImFriendRequestLog> logs = friendRequestLogManager.findByRequestId(requestId);
        List<FriendRequestHistoryDTO.RequestLogItem> logItems = logs.stream().map(log -> {
            FriendRequestHistoryDTO.RequestLogItem item = new FriendRequestHistoryDTO.RequestLogItem();
            item.setId(log.getId());
            item.setRequestId(log.getRequestId());
            item.setFromUserId(log.getFromUserId());
            item.setMessage(log.getMessage());
            item.setCreatedAt(log.getCreatedAt());
            return item;
        }).collect(Collectors.toList());
        dto.setLogs(logItems);

        return dto;
    }

    /**
     * 获取两个用户之间的好友申请历史
     */
    public FriendRequestHistoryDTO getRequestHistory(Long currentUserId, Long targetUserId) {
        ImFriendRequest request = friendRequestManager.findLatestBetween(currentUserId, targetUserId);
        if (request == null) return null;
        return getRequestDetail(request.getId());
    }

    // === 内部方法 ===

    private FriendInfoDTO toFriendInfoDTO(ImFriend friend) {
        FriendInfoDTO dto = new FriendInfoDTO();
        dto.setId(friend.getId());
        dto.setFriendId(friend.getFriendId());
        dto.setGroupId(friend.getGroupId());
        dto.setRemark(friend.getRemark());
        dto.setStatus(friend.getStatus());
        dto.setCreatedAt(friend.getCreatedAt());

        try {
            ImUserInfo info = userInfoProvider.getUserById(friend.getFriendId());
            if (info != null) {
                dto.setNickname(info.nickname());
                dto.setAvatar(info.avatar());
                dto.setAccount(info.account());
            }
        } catch (Exception e) {
            logger.warn("获取好友用户信息失败: friendId={}", friend.getFriendId(), e);
        }
        return dto;
    }
}
