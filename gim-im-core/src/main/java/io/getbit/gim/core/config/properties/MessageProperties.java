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

    /** ACK 超时时间（秒） */
    private int ackTimeoutSeconds = 10;

    /** 最大重发次数 */
    private int maxRetries = 3;
}
