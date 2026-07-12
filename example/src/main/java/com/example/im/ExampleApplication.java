package com.example.im;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * GIM IM SDK 对接使用示例
 * <p>
 * 引入 gim-im-starter 后，SDK 自动装配以下功能：
 * <ul>
 *   <li>Netty IM 长连接服务器</li>
 *   <li>消息路由、ACK、心跳管理</li>
 *   <li>好友管理 HTTP API（/im/friend/*）</li>
 *   <li>群组管理 HTTP API（/im/group/*）</li>
 *   <li>消息同步/历史 HTTP API（/im/message/*）</li>
 *   <li>RocketMQ 消息入库消费者</li>
 *   <li>WebRTC 信令处理</li>
 * </ul>
 * <p>
 * 使用方只需实现 SPI 接口即可运行。
 */
@SpringBootApplication
@MapperScan({
        "io.getbit.gim.friend.repository",
        "io.getbit.gim.group.repository",
        "io.getbit.gim.storage.repository"
})
public class ExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExampleApplication.class, args);
    }
}
