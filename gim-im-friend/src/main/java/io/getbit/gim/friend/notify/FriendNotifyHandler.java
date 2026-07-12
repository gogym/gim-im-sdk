package io.getbit.gim.friend.notify;

import io.getbit.gim.core.connection.channel.ChannelManager;
import io.getbit.gim.core.routing.ClusterMessageRouter;
import io.getbit.gim.core.routing.UserRouteService;
import io.getbit.gim.core.spi.ImEventListener;
import io.getbit.gim.friend.entity.ImFriend;
import io.getbit.gim.friend.manager.ImFriendManager;
import io.getbit.gim.protocol.codec.ImProto;
import io.getbit.gim.protocol.codec.PacketCodec;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 好友 IM 通知处理器
 *
 * @author gogym
 */
public class FriendNotifyHandler {

    private static final Logger logger = LoggerFactory.getLogger(FriendNotifyHandler.class);

    private final ChannelManager channelManager;
    private final UserRouteService userRouteService;
    private final ClusterMessageRouter clusterMessageRouter;
    private final ImFriendManager friendManager;
    private final List<ImEventListener> eventListeners;

    public FriendNotifyHandler(ChannelManager channelManager,
                               UserRouteService userRouteService,
                               ClusterMessageRouter clusterMessageRouter,
                               ImFriendManager friendManager,
                               List<ImEventListener> eventListeners) {
        this.channelManager = channelManager;
        this.userRouteService = userRouteService;
        this.clusterMessageRouter = clusterMessageRouter;
        this.friendManager = friendManager;
        this.eventListeners = eventListeners != null ? eventListeners : Collections.emptyList();
    }

    public void notifyFriendRequest(Long fromUserId, Long toUserId, String nickname, String avatar, String message) {
        ImProto.Packet packet = PacketCodec.buildFriendRequestNotifyPacket(
                fromUserId.toString(), toUserId.toString(), nickname, avatar, message);
        deliverToUser(toUserId.toString(), packet);
    }

    public void notifyFriendRequestResult(Long fromUserId, Long toUserId, boolean accepted) {
        ImProto.Packet packet = PacketCodec.buildFriendStatusNotifyPacket(
                fromUserId.toString(), toUserId.toString(), accepted ? 1 : 2);
        deliverToUser(toUserId.toString(), packet);
    }

    public void notifyFriendDeleted(Long deletedUserId, Long notifyUserId) {
        ImProto.Packet packet = PacketCodec.buildFriendStatusNotifyPacket(
                deletedUserId.toString(), notifyUserId.toString(), 3);
        deliverToUser(notifyUserId.toString(), packet);
    }

    public void notifyUserOnline(String userId) {
        try {
            List<ImFriend> friends = friendManager.findFriendsByUserId(Long.parseLong(userId));
            if (friends.isEmpty()) return;
            ImProto.Packet packet = PacketCodec.buildOnlineStatusNotifyPacket(userId, 1);
            for (ImFriend f : friends) {
                deliverToUser(f.getFriendId().toString(), packet);
            }
        } catch (Exception e) {
            logger.error("上线通知处理失败: userId={}", userId, e);
        }
    }

    public void syncFriendsOnlineStatus(String userId) {
        try {
            List<ImFriend> friends = friendManager.findFriendsByUserId(Long.parseLong(userId));
            if (friends.isEmpty()) return;
            for (ImFriend friend : friends) {
                String friendId = friend.getFriendId().toString();
                boolean online = !channelManager.getChannels(friendId).isEmpty();
                if (!online) {
                    online = userRouteService.getServerId(friendId) != null;
                }
                int status = online ? 1 : 0;
                ImProto.Packet packet = PacketCodec.buildOnlineStatusNotifyPacket(friendId, status);
                deliverToUser(userId, packet);
            }
        } catch (Exception e) {
            logger.error("好友在线状态同步失败: userId={}", userId, e);
        }
    }

    public void syncSingleFriendOnlineStatus(String userId, String friendId) {
        try {
            boolean online = !channelManager.getChannels(friendId).isEmpty();
            if (!online) {
                online = userRouteService.getServerId(friendId) != null;
            }
            int status = online ? 1 : 0;
            ImProto.Packet packet = PacketCodec.buildOnlineStatusNotifyPacket(friendId, status);
            deliverToUser(userId, packet);
        } catch (Exception e) {
            logger.error("单个好友在线状态同步失败: userId={}, friendId={}", userId, friendId, e);
        }
    }

    public void notifyUserOffline(String userId) {
        try {
            List<ImFriend> friends = friendManager.findFriendsByUserId(Long.parseLong(userId));
            if (friends.isEmpty()) return;
            ImProto.Packet packet = PacketCodec.buildOnlineStatusNotifyPacket(userId, 0);
            for (ImFriend f : friends) {
                deliverToUser(f.getFriendId().toString(), packet);
            }
        } catch (Exception e) {
            logger.error("离线通知处理失败: userId={}", userId, e);
        }
    }

    private void deliverToUser(String targetUserId, ImProto.Packet packet) {
        var deviceChannels = channelManager.getChannels(targetUserId);
        if (!deviceChannels.isEmpty()) {
            for (Map.Entry<?, Channel> entry : deviceChannels.entrySet()) {
                Channel ch = entry.getValue();
                if (ch != null && ch.isActive()) {
                    ch.writeAndFlush(packet);
                }
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
                logger.error("好友离线通知回调异常, receiver={}", targetUserId, e);
            }
        }
    }
}
