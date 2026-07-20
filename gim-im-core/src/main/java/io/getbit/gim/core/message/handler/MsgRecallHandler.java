package io.getbit.gim.core.message.handler;

import io.getbit.gim.core.connection.channel.ChannelManager;
import io.getbit.gim.core.routing.ClusterMessageRouter;
import io.getbit.gim.core.routing.UserRouteService;
import io.getbit.gim.core.spi.ImEventListener;
import io.getbit.gim.core.spi.ImGroupMemberProvider;
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
 * 2. 回复发送方 ServerAck（成功）
 * 3. 推送撤回通知给对方
 *    - 单聊 → 通知接收者
 *    - 群聊 → 通知所有群成员（排除操作者）
 * 4. 触发 ImEventListener.onMessageRecalled 回调（使用方负责 DB 更新）
 *
 * 注意：权限校验（发送者验证）和时间窗口校验（如5分钟内可撤回）
 * 由使用方在 onMessageRecalled 回调中或前置拦截中处理。
 * SDK 层面只做通知推送，不做 DB 操作。
 *
 * @author gogym
 */
public class MsgRecallHandler extends BaseHandler {

    private final ImGroupMemberProvider groupMemberProvider;

    public MsgRecallHandler(ChannelManager channelManager,
                            UserRouteService userRouteService,
                            ClusterMessageRouter clusterMessageRouter,
                            List<ImEventListener> eventListeners,
                            ImGroupMemberProvider groupMemberProvider) {
        super(channelManager, userRouteService, clusterMessageRouter, eventListeners);
        this.groupMemberProvider = groupMemberProvider;
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

            logger.info("收到撤回请求: from={}, msgId={}, conversationId={}, chatType={}",
                    userId, msgId, conversationId, chatType);

            // 1. 回复发送方 ServerAck（成功）
            String requestId = packet.getRequestId();
            ImProto.Packet ack = PacketCodec.buildServerAck(
                    requestId != null ? requestId : "",
                    msgId,
                    packet.getSequence());
            channel.writeAndFlush(ack);

            // 2. 推送撤回通知给对方
            pushRecallNotify(msgId, conversationId, userId, chatType);

            // 3. 触发回调（使用方负责 DB 更新、权限校验等）
            for (ImEventListener listener : eventListeners) {
                try {
                    listener.onMessageRecalled(msgId, conversationId, userId, chatType);
                } catch (Exception e) {
                    logger.error("撤回回调异常: msgId={}", msgId, e);
                }
            }

            logger.debug("消息撤回处理完成: msgId={}, userId={}", msgId, userId);

        } catch (Exception e) {
            logger.error("消息撤回处理失败, userId={}", userId, e);
            ImProto.Packet failAck = PacketCodec.buildServerAckFail(
                    packet.getRequestId(), 500, packet.getSequence());
            channel.writeAndFlush(failAck);
        }
    }

    /**
     * 推送撤回通知：单聊推给接收者，群聊推给所有群成员（排除操作者）
     */
    private void pushRecallNotify(String msgId, String conversationId, String userId, int chatType) {
        ImProto.Packet recallNotify = PacketCodec.buildMsgRecallNotifyPacket(
                msgId, conversationId, userId, chatType);

        if (chatType == 1) {
            // 单聊：从 conversationId 解析对方 userId
            // conversationId 格式由使用方定义，常见格式为 minId_maxId
            // 这里通过 routeToUser 投递给会话对方
            // 如果无法确定对方ID，使用方可通过 onMessageRecalled 回调自行处理
            String receiverId = parseReceiverFromConversation(conversationId, userId);
            if (receiverId != null) {
                routeToUser(receiverId, recallNotify);
                logger.debug("单聊撤回通知已推送: msgId={}, to={}", msgId, receiverId);
            } else {
                logger.warn("单聊撤回: 无法从 conversationId={} 解析接收者", conversationId);
            }

        } else if (chatType == 2) {
            // 群聊：推送给所有群成员（排除发送者）
            String groupId = conversationId != null && conversationId.startsWith("group_")
                    ? conversationId.substring(6) : conversationId;
            List<String> memberUserIds = groupMemberProvider.getGroupMemberUserIds(groupId);
            if (memberUserIds != null) {
                for (String memberId : memberUserIds) {
                    if (memberId.equals(userId)) {
                        continue;
                    }
                    routeToUser(memberId, recallNotify);
                }
                logger.debug("群聊撤回通知已推送群成员: msgId={}, group={}, members={}",
                        msgId, groupId, memberUserIds.size());
            }
        }
    }

    /**
     * 从 conversationId 中解析对方 userId
     * 支持格式：minId_maxId（数字排序的会话ID）
     * 如果 userId 是其中一方，返回另一方；否则返回 null
     */
    private String parseReceiverFromConversation(String conversationId, String userId) {
        if (conversationId == null || conversationId.isEmpty()) {
            return null;
        }
        // 尝试 minId_maxId 格式
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
