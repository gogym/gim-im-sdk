package io.getbit.gim.group.notify;

import io.getbit.gim.core.connection.channel.ChannelManager;
import io.getbit.gim.core.routing.ClusterMessageRouter;
import io.getbit.gim.core.routing.UserRouteService;
import io.getbit.gim.core.spi.ImEventListener;
import io.getbit.gim.group.entity.ImGroupMember;
import io.getbit.gim.group.manager.ImGroupMemberManager;
import io.getbit.gim.protocol.codec.ImProto;
import io.getbit.gim.protocol.codec.PacketCodec;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GroupNotifyService {
    private static final Logger logger = LoggerFactory.getLogger(GroupNotifyService.class);
    private final ChannelManager channelManager;
    private final UserRouteService userRouteService;
    private final ClusterMessageRouter clusterMessageRouter;
    private final ImGroupMemberManager memberManager;
    private final List<ImEventListener> eventListeners;

    public GroupNotifyService(ChannelManager channelManager, UserRouteService userRouteService,
                              ClusterMessageRouter clusterMessageRouter, ImGroupMemberManager memberManager,
                              List<ImEventListener> eventListeners) {
        this.channelManager = channelManager;
        this.userRouteService = userRouteService;
        this.clusterMessageRouter = clusterMessageRouter;
        this.memberManager = memberManager;
        this.eventListeners = eventListeners != null ? eventListeners : Collections.emptyList();
    }

    public void notifyMemberJoined(String groupId, Long userId) { notifyMemberChange(groupId, 1, userId.toString(), null); }
    public void notifyMemberQuit(String groupId, Long userId) { notifyMemberChange(groupId, 2, userId.toString(), null); }
    public void notifyMemberKicked(String groupId, Long userId, Long operatorId) { notifyMemberChange(groupId, 3, userId.toString(), operatorId.toString()); }
    public void notifyMemberInvited(String groupId, Long userId, Long inviterId) { notifyMemberChange(groupId, 4, userId.toString(), inviterId.toString()); }

    public void notifyGroupInfoChanged(String groupId, Long operatorId, String content) {
        broadcastToGroup(groupId, PacketCodec.buildGroupNotifyPacket(groupId, 1, operatorId.toString(), null, content), null);
    }
    public void notifyAnnouncementUpdated(String groupId, Long operatorId, String announcement) {
        broadcastToGroup(groupId, PacketCodec.buildGroupNotifyPacket(groupId, 2, operatorId.toString(), null, announcement), null);
    }
    public void notifyMuteAll(String groupId, Long operatorId, boolean muteAll) {
        broadcastToGroup(groupId, PacketCodec.buildGroupNotifyPacket(groupId, 3, operatorId.toString(), null, muteAll ? "1" : "0"), null);
    }
    public void notifyMemberMuted(String groupId, Long operatorId, Long targetUserId, boolean muted) {
        broadcastToGroup(groupId, PacketCodec.buildGroupNotifyPacket(groupId, 4, operatorId.toString(), targetUserId.toString(), muted ? "1" : "0"), null);
    }
    public void notifyRoleChanged(String groupId, Long operatorId, Long targetUserId, boolean isAdmin) {
        broadcastToGroup(groupId, PacketCodec.buildGroupNotifyPacket(groupId, 5, operatorId.toString(), targetUserId.toString(), isAdmin ? "1" : "0"), null);
    }
    public void notifyOwnerTransferred(String groupId, Long oldOwnerId, Long newOwnerId) {
        broadcastToGroup(groupId, PacketCodec.buildGroupNotifyPacket(groupId, 6, oldOwnerId.toString(), newOwnerId.toString(), null), null);
    }

    public void notifyJoinRequest(String groupId, Long applicantId, String message) {
        ImProto.Packet packet = PacketCodec.buildGroupJoinRequestNotifyPacket(groupId, applicantId.toString(), null, 0, message);
        List<ImGroupMember> admins = memberManager.findAdminMembers(groupId);
        for (ImGroupMember m : admins) deliverToUser(m.getUserId().toString(), packet);
    }
    public void notifyJoinRequestResult(String groupId, Long applicantId, Long operatorId, boolean approved) {
        ImProto.Packet packet = PacketCodec.buildGroupJoinRequestNotifyPacket(groupId, applicantId.toString(), operatorId.toString(), approved ? 1 : 2, null);
        deliverToUser(applicantId.toString(), packet);
    }

    private void notifyMemberChange(String groupId, int action, String userId, String operatorId) {
        broadcastToGroup(groupId, PacketCodec.buildGroupMemberNotifyPacket(groupId, action, userId, operatorId), userId);
    }

    public void broadcastToGroup(String groupId, ImProto.Packet packet, String excludeUserId) {
        List<Long> memberUserIds = memberManager.findActiveMemberUserIds(groupId);
        for (Long userId : memberUserIds) {
            String userIdStr = userId.toString();
            if (userIdStr.equals(excludeUserId)) continue;
            deliverToUser(userIdStr, packet);
        }
    }

    private void deliverToUser(String targetUserId, ImProto.Packet packet) {
        var deviceChannels = channelManager.getChannels(targetUserId);
        if (!deviceChannels.isEmpty()) {
            for (Map.Entry<?, Channel> entry : deviceChannels.entrySet()) {
                Channel ch = entry.getValue();
                if (ch != null && ch.isActive()) ch.writeAndFlush(packet);
            }
            return;
        }
        if (userRouteService.isRemote(targetUserId)) {
            String targetServerId = userRouteService.getServerId(targetUserId);
            clusterMessageRouter.routeToRemote(targetServerId, packet, targetUserId);
            return;
        }
        // 用户离线，触发离线通知回调
        for (ImEventListener listener : eventListeners) {
            try {
                listener.onOfflineNotify(packet, targetUserId);
            } catch (Exception e) {
                logger.error("群组离线通知回调异常, receiver={}", targetUserId, e);
            }
        }
    }
}
