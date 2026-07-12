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
 * MsgRecallHandler.java
 *
 * 消息撤回处理器
 *
 * 处理流程：
 * 1. 解析撤回请求（msgId, conversationId, chatType）
 * 2. 向会话相关用户推送撤回通知
 *
 * @author gogym
 */
public class MsgRecallHandler extends BaseHandler {

    public MsgRecallHandler(ChannelManager channelManager,
                            UserRouteService userRouteService,
                            ClusterMessageRouter clusterMessageRouter,
                            List<ImEventListener> eventListeners) {
        super(channelManager, userRouteService, clusterMessageRouter, eventListeners);
    }

    @Override
    public int cmd() {
        return Cmd.MSG_RECALL_REQ;
    }

    @Override
    public void handle(ImProto.Packet packet, Channel channel, String userId) {
        try {
            ImProto.MsgRecallRequest recallReq = PacketCodec.parseMsgRecallRequest(packet);
            String msgId = recallReq.getMsgId();
            String conversationId = recallReq.getConversationId();
            int chatType = recallReq.getChatType();

            // 构建撤回通知
            ImProto.Packet recallNotify = PacketCodec.buildMsgRecallNotifyPacket(
                    msgId, conversationId, userId, chatType);

            if (chatType == 1) {
                // 单聊：通知对方
                // conversationId 格式一般为 minUserId_maxUserId
                // 需要从 conversationId 中解析对方 userId，或由使用方扩展实现
                logger.debug("单聊消息撤回: msgId={}, conversation={}, operator={}",
                        msgId, conversationId, userId);

            } else if (chatType == 2) {
                // 群聊：广播给群成员
                // 群成员列表需通过 ImGroupMemberProvider SPI 获取
                logger.debug("群聊消息撤回: msgId={}, group={}, operator={}",
                        msgId, conversationId, userId);
            }

            logger.debug("消息撤回处理完成: msgId={}, userId={}", msgId, userId);

        } catch (Exception e) {
            logger.error("消息撤回处理失败, userId={}", userId, e);
        }
    }
}
