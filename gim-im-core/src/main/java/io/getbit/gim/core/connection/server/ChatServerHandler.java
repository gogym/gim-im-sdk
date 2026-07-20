package io.getbit.gim.core.connection.server;

import io.getbit.gim.core.bootstrap.IMServerFacade;
import io.getbit.gim.protocol.codec.*;
import io.getbit.gim.core.connection.channel.ConnectionInfo;
import io.getbit.gim.core.connection.auth.ConnectionAuthHandler;
import io.netty.handler.codec.DecoderException;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ChatServerHandler.java
 *
 * 聊天服务处理器
 *
 * 核心逻辑：
 * 1. channelActive: 连接建立，仅添加到全局通道组
 * 2. channelRead0: 按 cmd 分发
 *    - 未认证时：只接受 BIND_REQ，否则拒绝
 *    - 已认证后：按 cmd 路由到对应业务处理
 * 3. userEventTriggered: 空闲超时断开
 * 4. channelInactive: 解绑通道
 *
 * @author gogym
 */
public class ChatServerHandler extends SimpleChannelInboundHandler<ImProto.Packet> {

    private static final Logger logger = LoggerFactory.getLogger(ChatServerHandler.class);

    private final IMServerFacade facade;
    private final ConnectionAuthHandler authHandler;

    public ChatServerHandler(IMServerFacade facade, ConnectionAuthHandler authHandler) {
        this.facade = facade;
        this.authHandler = authHandler;
    }

    /**
     * 通道激活 - 客户端连接成功
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        logger.info("[{}] 新连接建立, 等待认证 ({}s 超时)",
                channel.id().asShortText(), ConnectionAuthHandler.AUTH_TIMEOUT);
    }

    /**
     * 读取消息 - 按 cmd 分发
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ImProto.Packet packet) throws Exception {
        Channel channel = ctx.channel();
        int cmd = packet.getCmd();

        // 未认证状态：只接受 BIND_REQ
        if (!authHandler.isAuthenticated(channel)) {
            if (cmd == Cmd.BIND_REQ) {
                boolean success = authHandler.handleBind(packet, channel);
                if (!success) {
                    channel.close();
                } else {
                    // 绑定成功，触发上线事件
                    String userId = authHandler.getUserId(channel);
                    DeviceType device = authHandler.getDevice(channel);
                    if (userId != null) {
                        facade.fireUserOnline(userId, device);
                    }
                }
            } else {
                logger.warn("[{}] 未认证连接发送了 cmd={}, 拒绝", channel.id().asShortText(), cmd);
                channel.close();
            }
            return;
        }

        // 已认证状态：按 cmd 路由
        String userId = authHandler.getUserId(channel);
        facade.getMessageDispatcher().dispatch(packet, channel, userId);
    }

    /**
     * 空闲超时事件
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent idleEvent) {
            if (idleEvent.state() == IdleState.READER_IDLE) {
                String userId = authHandler.getUserId(ctx.channel());
                logger.info("[{}] 读超时, userId={}, 断开连接",
                        ctx.channel().id().asShortText(), userId);
                ctx.close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    /**
     * 通道失活 - 解绑用户通道
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();

        ConnectionInfo connInfo = facade.getChannelManager().unbindByChannelId(channel.id().asLongText());
        String userId = connInfo != null ? connInfo.userId() : null;
        DeviceType device = connInfo != null ? connInfo.device() : null;

        if (userId != null) {
            logger.info("[{}] 用户断开, userId={}, device={}",
                    channel.id().asShortText(), userId, device);

            // 检查用户是否还有其他在线设备
            var remainingChannels = facade.getChannelManager().getChannels(userId);
            if (remainingChannels.isEmpty()) {
                facade.getUserRouteService().unregister(userId);
                facade.fireUserOffline(userId);
            }
        } else {
            logger.info("[{}] 未认证连接断开", channel.id().asShortText());
        }
    }

    /**
     * 异常捕获
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        String channelId = ctx.channel().id().asShortText();
        String userId = authHandler.getUserId(ctx.channel());

        if (cause instanceof DecoderException) {
            logger.warn("[{}] 协议解码失败, userId={}, 断开连接. 原因: {}",
                    channelId, userId, cause.getMessage());
        } else {
            logger.error("[{}] 通道异常, userId={}", channelId, userId, cause);
        }
        ctx.close();
    }
}
