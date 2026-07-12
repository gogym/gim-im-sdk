package io.getbit.gim.core.config.properties;

import lombok.Getter;
import lombok.Setter;

/**
 * MessageProperties.java
 *
 * 消息发送配置
 *
 * @author gogym
 */
@Getter
@Setter
public class MessageProperties {

    /** 消息存储 MQ Topic */
    private String storeTopic = "im-message-store";

    /** 离线消息 MQ Topic */
    private String offlineTopic = "im-message-offline";

    /** ACK 超时时间（秒） */
    private int ackTimeoutSeconds = 10;

    /** 最大重发次数 */
    private int maxRetries = 3;
}
