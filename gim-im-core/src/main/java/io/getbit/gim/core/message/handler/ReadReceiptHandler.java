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
 * 处理流程：
 * 1. 解析已读回执（conversationId + lastReadMsgId）
 * 2. 单聊：从 conversationId 解析对方 userId，转发已读回执
 * 3. 群聊：暂不处理（群已读回执需要更复杂的实现）
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

            // 单聊：从 conversationId 解析对方 userId，转发已读回执给对方
            String otherUserId = parseReceiverFromConversation(conversationId, userId);
            if (otherUserId != null) {
                // 构建转发包（携带已读信息）
                ImProto.ReadReceipt fwdReceipt = ImProto.ReadReceipt.newBuilder()
                        .setConversationId(conversationId)
                        .setLastReadMsgId(lastReadMsgId)
                        .build();
                ImProto.Packet fwdPacket = PacketCodec.create(Cmd.READ_RECEIPT, 0, fwdReceipt);
                boolean delivered = routeToUser(otherUserId, fwdPacket);

                if (!delivered) {
                    logger.debug("已读回执目标用户离线: to={}", otherUserId);
                }
            } else {
                // 无法解析对方ID（可能是群聊会话或格式不匹配）
                // 通过回调让使用方处理
                for (ImEventListener listener : eventListeners) {
                    try {
                        listener.onReadReceipt(userId, conversationId, lastReadMsgId);
                    } catch (Exception e) {
                        logger.error("已读回执回调异常: userId={}", userId, e);
                    }
                }
            }

        } catch (Exception e) {
            logger.error("已读回执处理失败, userId={}", userId, e);
        }
    }

    /**
     * 从 conversationId 中解析对方 userId
     * 支持格式：minId_maxId（数字排序的会话ID）
     */
    private String parseReceiverFromConversation(String conversationId, String userId) {
        if (conversationId == null || conversationId.isEmpty()) {
            return null;
        }
        int idx = conversationId.indexOf('_');
        if (idx > 0 && idx < conversationId.length() - 1) {
            String a = conversationId.substring(0, idx);
            String b = conversationId.substring(idx + 1);
            if (a.equals(userId)) return b;
            if (b.equals(userId)) return a;
        }
        return null;
    }
}
