package io.getbit.gim.core.routing;

import io.getbit.gim.core.connection.channel.ChannelManager;
import io.getbit.gim.core.config.properties.GimProperties;
import io.getbit.gim.core.spi.ImRedisAdapter;
import io.getbit.gim.core.spi.ImRedisSubscriber;
import io.getbit.gim.core.spi.ImEventListener;
import io.getbit.gim.protocol.codec.Cmd;
import io.getbit.gim.protocol.codec.ImProto;
import io.getbit.gim.protocol.codec.PacketCodec;
import com.google.protobuf.util.JsonFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;

/**
 * ClusterMessageRouter.java
 *
 * 集群消息路由器（Redis Pub/Sub 方案）
 *
 * 工作原理：
 * 1. 每个 IM 节点启动时订阅自己的 Redis channel: gim_node:{serverId}
 * 2. 发送消息到远程节点时，PUBLISH 到目标节点的 channel
 * 3. 收到订阅消息后，在本地投递给目标用户
 *
 * @author gogym
 */
public class ClusterMessageRouter {

    private static final Logger logger = LoggerFactory.getLogger(ClusterMessageRouter.class);

    private static final String NODE_CHANNEL_PREFIX = "gim_node:";

    private final GimProperties config;
    private final ChannelManager channelManager;
    private final ImRedisAdapter redisAdapter;
    private final ImRedisSubscriber redisSubscriber;
    private final List<ImEventListener> eventListeners;

    public ClusterMessageRouter(GimProperties config,
                                ChannelManager channelManager,
                                ImRedisAdapter redisAdapter,
                                ImRedisSubscriber redisSubscriber,
                                List<ImEventListener> eventListeners) {
        this.config = config;
        this.channelManager = channelManager;
        this.redisAdapter = redisAdapter;
        this.redisSubscriber = redisSubscriber;
        this.eventListeners = eventListeners;
    }

    // ====================== 生命周期 ======================

    /**
     * 启动：订阅本节点的 Redis channel
     */
    @PostConstruct
    public void start() {
        String serverId = config.getServerId();
        String channel = NODE_CHANNEL_PREFIX + serverId;

        Thread subscribeThread = new Thread(() -> {
            try {
                redisSubscriber.subscribe(channel, this::onClusterMessage);
            } catch (Exception e) {
                logger.error("集群消息路由订阅失败", e);
            }
        }, "cluster-route-subscriber");
        subscribeThread.setDaemon(true);
        subscribeThread.start();

        logger.info("集群消息路由启动, 订阅 channel: {}", channel);
    }

    /**
     * 停止：取消订阅
     */
    @PreDestroy
    public void stop() {
        redisSubscriber.unsubscribe();
        logger.info("集群消息路由已停止");
    }

    // ====================== 消息发送 ======================

    /**
     * 发送消息到远程节点
     */
    public void routeToRemote(String targetServerId, ImProto.Packet packet, String receiverId) {
        if (!redisSubscriber.isSubscribed()) {
            logger.warn("集群路由未就绪, 无法投递到节点: {}", targetServerId);
            return;
        }

        try {
            String channel = NODE_CHANNEL_PREFIX + targetServerId;
            String json = JsonFormat.printer().print(packet);
            redisAdapter.publish(channel, json);
            logger.debug("消息已路由到远程节点: {} -> receiver={}", targetServerId, receiverId);
        } catch (Exception e) {
            logger.error("消息路由失败, targetServer={}, receiver={}", targetServerId, receiverId, e);
        }
    }

    // ====================== 内部方法 ======================

    /**
     * 集群消息回调：收到其他节点发来的消息，在本地投递
     */
    private void onClusterMessage(String message) {
        try {
            ImProto.Packet packet = PacketCodec.packetFromJson(message);
            deliverLocally(packet);
        } catch (Exception e) {
            logger.error("集群消息投递失败", e);
        }
    }

    /**
     * 在本地投递从远程节点收到的消息
     */
    private void deliverLocally(ImProto.Packet packet) {
        int cmd = packet.getCmd();

        if (cmd == Cmd.SINGLE_CHAT_MSG || cmd == Cmd.GROUP_CHAT_MSG) {
            try {
                ImProto.ChatMessage chatMsg = PacketCodec.parseChatMessage(packet);
                String receiverId = chatMsg.getReceiverId();

                var targetChannels = channelManager.getChannels(receiverId);
                if (!targetChannels.isEmpty()) {
                    ImProto.Packet fwdPacket = PacketCodec.create(cmd, 0, chatMsg);
                    targetChannels.values().forEach(ch -> {
                        if (ch.isActive()) {
                            ch.writeAndFlush(fwdPacket);
                        }
                    });
                    logger.debug("集群消息本地投递成功: receiver={}, devices={}", receiverId, targetChannels.size());
                } else {
                    logger.debug("集群消息本地投递: 用户不在线, receiver={}", receiverId);
                    fireDeliveryFailed(chatMsg.getMsgId(), receiverId, "OFFLINE");
                }
            } catch (Exception e) {
                logger.error("集群聊天消息本地投递失败", e);
            }

        } else if (cmd == Cmd.RTC_SIGNAL) {
            try {
                ImProto.RtcSignal signal = PacketCodec.parseRtcSignal(packet);
                String targetId = signal.getToUserId();

                var targetChannels = channelManager.getChannels(targetId);
                if (!targetChannels.isEmpty()) {
                    ImProto.Packet fwdPacket = PacketCodec.create(Cmd.RTC_SIGNAL, 0, signal);
                    targetChannels.values().forEach(ch -> {
                        if (ch.isActive()) {
                            ch.writeAndFlush(fwdPacket);
                        }
                    });
                }
            } catch (Exception e) {
                logger.error("集群RTC信令本地投递失败", e);
            }

        } else {
            // 其他通知类型：直接转发给本地用户
            logger.debug("集群消息投递: cmd={}", cmd);
        }
    }

    private void fireDeliveryFailed(String msgId, String receiverId, String reason) {
        if (eventListeners != null) {
            for (ImEventListener listener : eventListeners) {
                try {
                    listener.onMessageDeliveryFailed(msgId, receiverId, reason);
                } catch (Exception e) {
                    logger.error("事件监听器回调异常", e);
                }
            }
        }
    }
}
