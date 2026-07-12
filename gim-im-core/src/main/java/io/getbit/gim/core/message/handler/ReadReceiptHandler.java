package io.getbit.gim.core.message.handler;

import io.getbit.gim.core.connection.channel.ChannelManager;
import io.getbit.gim.core.routing.ClusterMessageRouter;
import io.getbit.gim.core.routing.UserRouteService;
import io.getbit.gim.core.spi.ImEventListener;
import io.getbit.gim.protocol.codec.Cmd;
import io.getbit.gim.protocol.codec.ImProto;
import io.getbit.gim.protocol.codec.PacketCodec;
import io.netty.channel.Channel;

import java.util.List;

/**
 * ReadReceiptHandler.java
 *
 * 已读回执处理器
 * 收到已读回执后，通知消息发送方标记为已读
 *
 * @author gogym
 */
public class ReadReceiptHandler extends BaseHandler {

    public ReadReceiptHandler(ChannelManager channelManager,
                              UserRouteService userRouteService,
                              ClusterMessageRouter clusterMessageRouter,
                              List<ImEventListener> eventListeners) {
        super(channelManager, userRouteService, clusterMessageRouter, eventListeners);
    }

    @Override
    public int cmd() {
        return Cmd.READ_RECEIPT;
    }

    @Override
    public void handle(ImProto.Packet packet, Channel channel, String userId) {
        try {
            ImProto.ReadReceipt readReceipt = PacketCodec.parseReadReceipt(packet);
            String conversationId = readReceipt.getConversationId();
            String lastReadMsgId = readReceipt.getLastReadMsgId();

            logger.debug("已读回执: userId={}, conversation={}, lastReadMsg={}",
                    userId, conversationId, lastReadMsgId);

            // 已读回执可由使用方通过 ImEventListener 扩展处理
            // 例如：更新消息已读状态、发送已读通知给对方等

        } catch (Exception e) {
            logger.error("已读回执处理失败, userId={}", userId, e);
        }
    }
}
