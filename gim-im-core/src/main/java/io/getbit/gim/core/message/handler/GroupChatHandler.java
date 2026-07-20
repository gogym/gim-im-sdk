package io.getbit.gim.core.message.handler;

import io.getbit.gim.core.connection.channel.ChannelManager;
import io.getbit.gim.core.message.ack.MessageAckTracker;
import io.getbit.gim.core.routing.ClusterMessageRouter;
import io.getbit.gim.core.routing.UserRouteService;
import io.getbit.gim.core.spi.ImEventListener;
import io.getbit.gim.core.spi.ImGroupMemberProvider;
import io.getbit.gim.core.spi.ImIdGenerator;
import io.getbit.gim.protocol.codec.Cmd;
import io.getbit.gim.protocol.codec.ImProto;
import io.getbit.gim.protocol.codec.PacketCodec;
import io.netty.channel.Channel;

import java.util.List;

/**
 * GroupChatHandler.java
 *
 * 群聊消息处理器
 *
 * 处理流程：
 * 1. 解析 ChatMessage，生成 msgId
 * 2. 禁言检查（通过 ImGroupMemberProvider SPI）
 * 3. 回复 ServerAck 给发送方
 * 4. 遍历群成员，逐一投递（排除发送者）
 *    - 本地在线 → 直接投递
 *    - 远程节点 → Redis Pub/Sub 路由
 *    - 离线 → 触发离线消息回调
 * 5. 追踪每个在线成员的 ACK
 *
 * @author gogym
 */
public class GroupChatHandler extends BaseHandler {

    private final ImIdGenerator idGenerator;
    private final MessageAckTracker ackTracker;
    private final ImGroupMemberProvider groupMemberProvider;

    public GroupChatHandler(ChannelManager channelManager,
                            UserRouteService userRouteService,
                            ClusterMessageRouter clusterMessageRouter,
                            List<ImEventListener> eventListeners,
                            ImIdGenerator idGenerator,
                            MessageAckTracker ackTracker,
                            ImGroupMemberProvider groupMemberProvider) {
        super(channelManager, userRouteService, clusterMessageRouter, eventListeners);
        this.idGenerator = idGenerator;
        this.ackTracker = ackTracker;
        this.groupMemberProvider = groupMemberProvider;
    }

    @Override
    public int cmd() {
        return Cmd.GROUP_CHAT_MSG;
    }

    @Override
    public void handle(ImProto.Packet packet, Channel channel, String userId) {
        try {
            ImProto.ChatMessage chatMsg = PacketCodec.parseChatMessage(packet);
            String groupId = chatMsg.getReceiverId();

            // 1. 禁言检查
            String muteReason = groupMemberProvider.checkCanSendMessage(groupId, userId);
            if (muteReason != null) {
                logger.info("群消息被拒绝: userId={}, groupId={}, reason={}", userId, groupId, muteReason);
                ImProto.Packet muteAck = PacketCodec.buildServerAckFail(
                        packet.getRequestId(), 403, packet.getSequence());
                channel.writeAndFlush(muteAck);
                return;
            }

            // 2. 生成消息ID
            String msgId = (chatMsg.getMsgId() == null || chatMsg.getMsgId().isEmpty())
                    ? idGenerator.generateMsgId()
                    : chatMsg.getMsgId();

            // 3. 构建带 msgId 的完整消息
            ImProto.ChatMessage enrichedMsg = chatMsg.toBuilder()
                    .setMsgId(msgId)
                    .build();

            // 4. 回复 ServerAck 给发送方
            String requestId = packet.getRequestId();
            ImProto.Packet ack = PacketCodec.buildServerAck(
                    requestId != null ? requestId : "",
                    msgId,
                    packet.getSequence());
            channel.writeAndFlush(ack);

            // 5. 路由投递给在线群成员
            routeToMembers(enrichedMsg, userId, groupId);

            logger.debug("群聊消息处理完成: msgId={}, from={}, group={}", msgId, userId, groupId);

        } catch (Exception e) {
            logger.error("群聊消息处理失败, userId={}", userId, e);
            ImProto.Packet failAck = PacketCodec.buildServerAckFail(
                    packet.getRequestId(), 500, packet.getSequence());
            channel.writeAndFlush(failAck);
        }
    }

    /**
     * 群聊消息路由投递
     *
     * 流程：
     * 1. 获取群内所有活跃成员 userId 列表
     * 2. 为每个成员（排除发送者）路由投递
     * 3. 本地 → 直接投递 + ACK 追踪，远程 → Redis Pub/Sub，离线 → 离线消息回调
     */
    private void routeToMembers(ImProto.ChatMessage chatMsg, String senderId, String groupId) {
        List<String> memberUserIds = groupMemberProvider.getGroupMemberUserIds(groupId);
        if (memberUserIds == null || memberUserIds.isEmpty()) {
            logger.warn("群消息路由: 群 {} 无活跃成员", groupId);
            return;
        }

        int deliveredCount = 0;
        int offlineCount = 0;

        for (String memberId : memberUserIds) {
            // 跳过发送者
            if (memberId.equals(senderId)) {
                continue;
            }

            // 路由投递
            ImProto.Packet downstreamPacket = PacketCodec.create(Cmd.GROUP_CHAT_MSG, 0, chatMsg);
            boolean delivered = routeToUser(memberId, downstreamPacket);

            if (delivered) {
                ackTracker.track(chatMsg.getMsgId(), memberId, downstreamPacket);
                deliveredCount++;
            } else {
                fireOfflineChat(chatMsg, memberId, "OFFLINE");
                offlineCount++;
            }
        }

        logger.debug("群消息路由完成: group={}, msgId={}, members={}, delivered={}, offline={}",
                groupId, chatMsg.getMsgId(), memberUserIds.size(), deliveredCount, offlineCount);
    }
}
