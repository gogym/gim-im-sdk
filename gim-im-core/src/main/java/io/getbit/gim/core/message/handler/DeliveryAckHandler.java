package io.getbit.gim.core.message.handler;

import io.getbit.gim.core.connection.channel.ChannelManager;
import io.getbit.gim.core.message.ack.MessageAckTracker;
import io.getbit.gim.core.routing.ClusterMessageRouter;
import io.getbit.gim.core.routing.UserRouteService;
import io.getbit.gim.core.spi.ImEventListener;
import io.getbit.gim.protocol.codec.Cmd;
import io.getbit.gim.protocol.codec.ImProto;
import io.getbit.gim.protocol.codec.PacketCodec;
import io.netty.channel.Channel;

import java.util.List;

/**
 * DeliveryAckHandler.java
 *
 * 送达 ACK 处理器
 * 接收方客户端确认收到消息后，服务端取消 ACK 追踪
 *
 * @author gogym
 */
public class DeliveryAckHandler extends BaseHandler {

    private final MessageAckTracker ackTracker;

    public DeliveryAckHandler(ChannelManager channelManager,
                              UserRouteService userRouteService,
                              ClusterMessageRouter clusterMessageRouter,
                              List<ImEventListener> eventListeners,
                              MessageAckTracker ackTracker) {
        super(channelManager, userRouteService, clusterMessageRouter, eventListeners);
        this.ackTracker = ackTracker;
    }

    @Override
    public int cmd() {
        return Cmd.DELIVERY_ACK;
    }

    @Override
    public void handle(ImProto.Packet packet, Channel channel, String userId) {
        try {
            ImProto.DeliveryAck deliveryAck = PacketCodec.parseDeliveryAck(packet);
            String msgId = deliveryAck.getMsgId();

            // 取消 ACK 追踪
            boolean wasPending = ackTracker.acknowledge(msgId);
            logger.debug("送达ACK处理: msgId={}, userId={}, wasPending={}", msgId, userId, wasPending);

        } catch (Exception e) {
            logger.error("送达ACK处理失败, userId={}", userId, e);
        }
    }
}
