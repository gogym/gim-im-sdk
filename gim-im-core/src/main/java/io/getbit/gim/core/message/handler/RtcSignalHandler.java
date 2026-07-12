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
 * RtcSignalHandler.java
 *
 * WebRTC 信令处理器
 * 在两端之间转发 WebRTC 信令（offer/answer/ICE candidate 等）
 *
 * @author gogym
 */
public class RtcSignalHandler extends BaseHandler {

    public RtcSignalHandler(ChannelManager channelManager,
                            UserRouteService userRouteService,
                            ClusterMessageRouter clusterMessageRouter,
                            List<ImEventListener> eventListeners) {
        super(channelManager, userRouteService, clusterMessageRouter, eventListeners);
    }

    @Override
    public int cmd() {
        return Cmd.RTC_SIGNAL;
    }

    @Override
    public void handle(ImProto.Packet packet, Channel channel, String userId) {
        try {
            ImProto.RtcSignal signal = PacketCodec.parseRtcSignal(packet);
            String targetId = signal.getToUserId();

            if (targetId == null || targetId.isEmpty()) {
                logger.warn("RTC信令缺少目标用户: signalType={}, from={}", signal.getSignalType(), userId);
                return;
            }

            // 转发信令到目标用户（本地/远程）
            ImProto.Packet fwdPacket = PacketCodec.create(Cmd.RTC_SIGNAL, 0, signal);
            boolean delivered = routeToUser(targetId, fwdPacket);

            if (!delivered) {
                logger.debug("RTC信令目标用户离线: signalType={}, to={}", signal.getSignalType(), targetId);
                // 触发离线通知回调
                fireOfflineNotify(fwdPacket, targetId);
            }

            logger.debug("RTC信令转发: signalType={}, from={}, to={}, delivered={}",
                    signal.getSignalType(), userId, targetId, delivered);

        } catch (Exception e) {
            logger.error("RTC信令处理失败, userId={}", userId, e);
        }
    }
}
