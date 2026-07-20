package io.getbit.gim.core.connection.channel;

import io.getbit.gim.protocol.codec.DeviceType;

/**
 * ConnectionInfo.java
 *
 * 连接信息记录
 * 记录用户连接所在的节点、用户ID、通道、设备类型和连接时间
 *
 * @param nodeId      所在 IM 节点 ID（集群模式用，单机可为 null）
 * @param userId      用户 ID
 * @param channelId   Netty ChannelId 字符串
 * @param device      设备类型
 * @param connectedAt 连接建立时间（毫秒时间戳）
 *
 * @author gogym
 */
public record ConnectionInfo(
        String nodeId,
        String userId,
        String channelId,
        DeviceType device,
        long connectedAt
) {

    /**
     * 创建 ConnectionInfo（自动填充连接时间）
     */
    public static ConnectionInfo of(String nodeId, String userId, String channelId, DeviceType device) {
        return new ConnectionInfo(nodeId, userId, channelId, device, System.currentTimeMillis());
    }
}
