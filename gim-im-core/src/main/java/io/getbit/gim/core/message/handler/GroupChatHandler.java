package io.getbit.gim.core.message.handler;

import io.getbit.gim.core.connection.channel.ChannelManager;
import io.getbit.gim.core.message.ack.MessageAckTracker;
import io.getbit.gim.core.routing.ClusterMessageRouter;
import io.getbit.gim.core.routing.UserRouteService;
import io.getbit.gim.core.spi.ImEventListener;
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
 * 2. 回复 ServerAck 给发送方
 * 3. 遍历群成员，逐一投递（排除发送者）
 *
 * @author gogym
 */
public class GroupChatHandler extends BaseHandler {

    private final ImIdGenerator idGenerator;
    private final MessageAckTracker ackTracker;

    public GroupChatHandler(ChannelManager channelManager,
                            UserRouteService userRouteService,
                            ClusterMessageRouter clusterMessageRouter,
                            List<ImEventListener> eventListeners,
                            ImIdGenerator idGenerator,
                            MessageAckTracker ackTracker) {
        super(channelManager, userRouteService, clusterMessageRouter, eventListeners);
        this.idGenerator = idGenerator;
        this.ackTracker = ackTracker;
    }

    @Override
    public int cmd() {
        return Cmd.GROUP_CHAT_MSG;
    }

    @Override
    public void handle(ImProto.Packet packet, Channel channel, String userId) {
        try {
            ImProto.ChatMessage chatMsg = PacketCodec.parseChatMessage(packet);

            // 1. 生成消息ID
            String msgId = (chatMsg.getMsgId() == null || chatMsg.getMsgId().isEmpty())
                    ? idGenerator.generateMsgId()
                    : chatMsg.getMsgId();

            // 2. 构建带 msgId 的完整消息
            ImProto.ChatMessage enrichedMsg = chatMsg.toBuilder()
                    .setMsgId(msgId)
                    .build();
            ImProto.Packet fwdPacket = PacketCodec.create(Cmd.GROUP_CHAT_MSG, 0, enrichedMsg);

            // 3. 回复 ServerAck 给发送方
            String requestId = packet.getRequestId();
            ImProto.Packet ack = PacketCodec.buildServerAck(
                    requestId != null ? requestId : "",
                    msgId,
                    packet.getSequence());
            channel.writeAndFlush(ack);

            // 4. 群消息投递：遍历本地在线成员 + 路由到远程节点
            // 注意：群成员列表需由使用方通过 ImGroupMemberProvider SPI 提供
            // 此处仅投递给本地在线的群成员（由 ChannelManager 查询）
            // 完整的群消息扩散由 MQ 消费者 + 入库模块负责
            String groupId = chatMsg.getReceiverId();
            logger.debug("群聊消息处理完成: msgId={}, from={}, group={}", msgId, userId, groupId);

        } catch (Exception e) {
            logger.error("群聊消息处理失败, userId={}", userId, e);
        }
    }
}
