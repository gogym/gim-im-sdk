package io.getbit.gim.group.service;

import io.getbit.gim.core.spi.ImIdGenerator;
import io.getbit.gim.friend.spi.ImUserInfoProvider;
import io.getbit.gim.group.dto.GroupMemberInfoDTO;
import io.getbit.gim.group.entity.ImGroup;
import io.getbit.gim.group.entity.ImGroupJoinRequest;
import io.getbit.gim.group.entity.ImGroupMember;
import io.getbit.gim.group.manager.ImGroupJoinRequestManager;
import io.getbit.gim.group.manager.ImGroupManager;
import io.getbit.gim.group.manager.ImGroupMemberManager;
import io.getbit.gim.group.notify.GroupNotifyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class GroupService {
    private static final Logger logger = LoggerFactory.getLogger(GroupService.class);
    private final ImGroupManager groupManager;
    private final ImGroupMemberManager memberManager;
    private final ImGroupJoinRequestManager joinRequestManager;
    private final ImIdGenerator idGenerator;
    private final GroupNotifyService groupNotifyService;

    /** 可选依赖：用户信息提供者，用于丰富群成员信息 */
    @Autowired(required = false)
    private ImUserInfoProvider userInfoProvider;

    public GroupService(ImGroupManager groupManager, ImGroupMemberManager memberManager,
                        ImGroupJoinRequestManager joinRequestManager, ImIdGenerator idGenerator,
                        GroupNotifyService groupNotifyService) {
        this.groupManager = groupManager;
        this.memberManager = memberManager;
        this.joinRequestManager = joinRequestManager;
        this.idGenerator = idGenerator;
        this.groupNotifyService = groupNotifyService;
    }

    @Transactional
    public ImGroup createGroup(Long ownerId, String name, String avatar, List<Long> memberIds) {
        String groupId = idGenerator.generateMsgId();
        ImGroup group = new ImGroup();
        group.setGroupId(groupId); group.setName(name); group.setAvatar(avatar);
        group.setOwnerId(ownerId); group.setMaxMembers(200); group.setJoinVerify(0);
        group.setMuteAll(0); group.setStatus(1);
        groupManager.insert(group);
        addMember(groupId, ownerId, 2);
        if (memberIds != null) {
            for (Long memberId : memberIds) {
                if (!memberId.equals(ownerId)) {
                    addMember(groupId, memberId, 0);
                    groupNotifyService.notifyMemberInvited(groupId, memberId, ownerId);
                }
            }
        }
        return group;
    }

    public ImGroup getGroup(String groupId) { return groupManager.findByGroupId(groupId); }

    public boolean updateGroupInfo(Long userId, String groupId, String name, String avatar, String announcement) {
        ImGroup group = groupManager.findByGroupId(groupId);
        if (group == null) return false;
        ImGroupMember member = memberManager.findMember(groupId, userId);
        if (member == null || member.getRole() < 1) return false;
        boolean ok = groupManager.updateGroupInfo(groupId, name, avatar, announcement);
        if (ok && announcement != null && !announcement.isEmpty()) {
            groupNotifyService.notifyAnnouncementUpdated(groupId, userId, announcement);
        } else if (ok) {
            groupNotifyService.notifyGroupInfoChanged(groupId, userId, String.format("{\"name\":\"%s\"}", name != null ? name : ""));
        }
        return ok;
    }

    @Transactional
    public boolean dissolveGroup(Long userId, String groupId) {
        ImGroup group = groupManager.findByGroupId(groupId);
        if (group == null || !group.getOwnerId().equals(userId)) return false;
        groupManager.dissolveGroup(groupId);
        for (ImGroupMember m : memberManager.findActiveMembers(groupId)) memberManager.removeMember(groupId, m.getUserId());
        return true;
    }

    @Transactional
    public boolean addMember(String groupId, Long userId, int role) {
        if (memberManager.isMember(groupId, userId)) return false;
        long count = memberManager.countActiveMembers(groupId);
        ImGroup group = groupManager.findByGroupId(groupId);
        if (group == null || count >= group.getMaxMembers()) return false;
        ImGroupMember member = new ImGroupMember();
        member.setGroupId(groupId); member.setUserId(userId); member.setRole(role);
        member.setIsMuted(0); member.setJoinTime(LocalDateTime.now()); member.setStatus(1);
        return memberManager.insert(member);
    }

    @Transactional
    public boolean inviteMembers(Long inviterId, String groupId, List<Long> userIds) {
        if (!memberManager.isMember(groupId, inviterId)) return false;
        for (Long userId : userIds) {
            if (addMember(groupId, userId, 0)) groupNotifyService.notifyMemberInvited(groupId, userId, inviterId);
        }
        return true;
    }

    public boolean removeMember(Long operatorId, String groupId, Long targetUserId) {
        if (!memberManager.isAdminOrOwner(groupId, operatorId)) return false;
        ImGroup group = groupManager.findByGroupId(groupId);
        if (group == null || group.getOwnerId().equals(targetUserId)) return false;
        boolean removed = memberManager.removeMember(groupId, targetUserId);
        if (removed) groupNotifyService.notifyMemberKicked(groupId, targetUserId, operatorId);
        return removed;
    }

    public boolean quitGroup(Long userId, String groupId) {
        ImGroup group = groupManager.findByGroupId(groupId);
        if (group == null || group.getOwnerId().equals(userId)) return false;
        boolean quit = memberManager.removeMember(groupId, userId);
        if (quit) groupNotifyService.notifyMemberQuit(groupId, userId);
        return quit;
    }

    @Transactional
    public boolean transferOwner(Long currentOwnerId, String groupId, Long newOwnerId) {
        ImGroup group = groupManager.findByGroupId(groupId);
        if (group == null || !group.getOwnerId().equals(currentOwnerId)) return false;
        if (!memberManager.isMember(groupId, newOwnerId)) return false;
        memberManager.setMemberRole(groupId, currentOwnerId, 0);
        memberManager.setMemberRole(groupId, newOwnerId, 2);
        groupManager.transferOwner(groupId, newOwnerId);
        groupNotifyService.notifyOwnerTransferred(groupId, currentOwnerId, newOwnerId);
        return true;
    }

    public boolean setAdmin(Long ownerId, String groupId, Long targetUserId, boolean isAdmin) {
        ImGroup group = groupManager.findByGroupId(groupId);
        if (group == null || !group.getOwnerId().equals(ownerId)) return false;
        boolean ok = memberManager.setMemberRole(groupId, targetUserId, isAdmin ? 1 : 0);
        if (ok) groupNotifyService.notifyRoleChanged(groupId, ownerId, targetUserId, isAdmin);
        return ok;
    }

    public boolean muteMember(Long operatorId, String groupId, Long targetUserId, boolean muted) {
        if (!memberManager.isAdminOrOwner(groupId, operatorId)) return false;
        boolean ok = memberManager.setMemberMuted(groupId, targetUserId, muted);
        if (ok) groupNotifyService.notifyMemberMuted(groupId, operatorId, targetUserId, muted);
        return ok;
    }

    public boolean muteAll(Long operatorId, String groupId, boolean muteAll) {
        if (!memberManager.isAdminOrOwner(groupId, operatorId)) return false;
        boolean ok = groupManager.setMuteAll(groupId, muteAll);
        if (ok) groupNotifyService.notifyMuteAll(groupId, operatorId, muteAll);
        return ok;
    }

    public List<ImGroup> getMyGroups(Long userId) {
        List<String> groupIds = memberManager.findGroupIdsByUserId(userId);
        if (groupIds.isEmpty()) return List.of();
        return groupIds.stream().map(groupManager::findByGroupId).filter(g -> g != null).toList();
    }

    public List<ImGroupMember> getGroupMembers(String groupId) { return memberManager.findActiveMembers(groupId); }
    public List<Long> getGroupMemberUserIds(String groupId) { return memberManager.findActiveMemberUserIds(groupId); }
    public List<ImGroupMember> getGroupAdmins(String groupId) { return memberManager.findAdminMembers(groupId); }
    public boolean isGroupMember(String groupId, Long userId) { return memberManager.isMember(groupId, userId); }

    /**
     * 获取群成员列表（含用户昵称、头像）
     */
    public List<GroupMemberInfoDTO> getGroupMembersWithUserInfo(String groupId) {
        List<ImGroupMember> members = memberManager.findActiveMembers(groupId);
        return members.stream().map(m -> {
            GroupMemberInfoDTO dto = new GroupMemberInfoDTO();
            dto.setId(m.getId());
            dto.setGroupId(m.getGroupId());
            dto.setUserId(m.getUserId());
            dto.setNickname(m.getNickname());
            dto.setRole(m.getRole());
            dto.setIsMuted(m.getIsMuted());
            dto.setJoinTime(m.getJoinTime());
            dto.setStatus(m.getStatus());
            if (userInfoProvider != null) {
                try {
                    ImUserInfoProvider.ImUserInfo info = userInfoProvider.getUserById(m.getUserId());
                    if (info != null) {
                        dto.setUserName(info.nickname());
                        dto.setUserAvatar(info.avatar());
                    }
                } catch (Exception e) {
                    logger.warn("获取用户信息失败: userId={}", m.getUserId(), e);
                }
            }
            return dto;
        }).collect(Collectors.toList());
    }

    public String checkCanSendMessage(String groupId, Long userId) {
        ImGroup group = groupManager.findByGroupId(groupId);
        if (group == null) return "群组不存在";
        ImGroupMember member = memberManager.findMember(groupId, userId);
        if (member == null) return "不是群成员";
        if (group.getMuteAll() != null && group.getMuteAll() == 1 && member.getRole() < 1) return "全员禁言中";
        if (member.getIsMuted() != null && member.getIsMuted() == 1) return "你已被禁言";
        return null;
    }

    @Transactional
    public boolean applyJoinGroup(Long userId, String groupId, String message) {
        ImGroup group = groupManager.findByGroupId(groupId);
        if (group == null || group.getStatus() != 1 || memberManager.isMember(groupId, userId)) return false;
        if (joinRequestManager.findPendingRequest(groupId, userId) != null) return false;
        if (group.getJoinVerify() == null || group.getJoinVerify() == 0) {
            boolean added = addMember(groupId, userId, 0);
            if (added) groupNotifyService.notifyMemberJoined(groupId, userId);
            return added;
        }
        ImGroupJoinRequest request = new ImGroupJoinRequest();
        request.setGroupId(groupId); request.setUserId(userId); request.setMessage(message); request.setStatus(0);
        joinRequestManager.insert(request);
        groupNotifyService.notifyJoinRequest(groupId, userId, message);
        return true;
    }

    @Transactional
    public boolean handleJoinRequest(Long handlerId, String groupId, Long applicantId, boolean approve) {
        if (!memberManager.isAdminOrOwner(groupId, handlerId)) return false;
        ImGroupJoinRequest request = joinRequestManager.findPendingRequest(groupId, applicantId);
        if (request == null) return false;
        request.setStatus(approve ? 1 : 2); request.setHandlerId(handlerId); request.setHandledAt(LocalDateTime.now());
        joinRequestManager.updateById(request);
        if (approve) {
            boolean added = addMember(groupId, applicantId, 0);
            if (added) { groupNotifyService.notifyMemberJoined(groupId, applicantId); groupNotifyService.notifyJoinRequestResult(groupId, applicantId, handlerId, true); }
        } else {
            groupNotifyService.notifyJoinRequestResult(groupId, applicantId, handlerId, false);
        }
        return true;
    }

    public List<ImGroupJoinRequest> getPendingJoinRequests(Long userId, String groupId) {
        if (!memberManager.isAdminOrOwner(groupId, userId)) return List.of();
        return joinRequestManager.findPendingRequests(groupId);
    }

    public boolean setJoinVerify(Long userId, String groupId, boolean joinVerify) {
        ImGroup group = groupManager.findByGroupId(groupId);
        if (group == null || !group.getOwnerId().equals(userId)) return false;
        return groupManager.setJoinVerify(groupId, joinVerify);
    }

    public boolean setNickname(Long userId, String groupId, String nickname) {
        ImGroupMember member = memberManager.findMember(groupId, userId);
        if (member == null) return false;
        member.setNickname(nickname);
        return memberManager.updateById(member);
    }
}
