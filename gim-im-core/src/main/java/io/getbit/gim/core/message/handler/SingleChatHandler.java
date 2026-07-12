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
 * SingleChatHandler.java
 *
 * 单聊消息处理器
 *
 * 处理流程：
 * 1. 解析 ChatMessage，生成 msgId
 * 2. 回复 ServerAck 给发送方
 * 3. 路由消息到接收方（本地/远程）
 * 4. 追踪 ACK（等待接收方送达确认）
 *
 * @author gogym
 */
public class SingleChatHandler extends BaseHandler {

    private final ImIdGenerator idGenerator;
    private final MessageAckTracker ackTracker;

    public SingleChatHandler(ChannelManager channelManager,
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
        return Cmd.SINGLE_CHAT_MSG;
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
            ImProto.Packet fwdPacket = PacketCodec.create(Cmd.SINGLE_CHAT_MSG, 0, enrichedMsg);

            // 3. 回复 ServerAck 给发送方
            String requestId = packet.getRequestId();
            ImProto.Packet ack = PacketCodec.buildServerAck(
                    requestId != null ? requestId : "",
                    msgId,
                    packet.getSequence());
            channel.writeAndFlush(ack);

            // 4. 路由到接收方
            String receiverId = chatMsg.getReceiverId();
            boolean delivered = routeToUser(receiverId, fwdPacket);

            if (delivered) {
                // 5. 追踪 ACK（等待接收方送达确认）
                ackTracker.track(msgId, receiverId, fwdPacket);
            } else {
                logger.debug("单聊消息接收方离线: msgId={}, receiver={}", msgId, receiverId);
                // 触发离线消息回调
                fireOfflineChat(enrichedMsg, receiverId, "OFFLINE");
            }

            logger.debug("单聊消息处理完成: msgId={}, from={}, to={}", msgId, userId, receiverId);

        } catch (Exception e) {
            logger.error("单聊消息处理失败, userId={}", userId, e);
        }
    }
}
