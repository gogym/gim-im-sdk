package io.getbit.gim.core.message.handler;

import io.getbit.gim.protocol.codec.ImProto;
import io.netty.channel.Channel;

/**
 * MessageDispatcher.java
 *
 * 消息分发器接口
 * ChatServerHandler 将已认证的消息按 cmd 分发到具体业务处理器
 *
 * @author gogym
 */
public interface MessageDispatcher {

    /**
     * 按 packet.cmd 路由到对应 Handler
     */
    void dispatch(ImProto.Packet packet, Channel channel, String userId);
}
