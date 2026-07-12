package io.getbit.gim.broker.rocketmq;

import io.getbit.gim.core.spi.ImMessageBroker;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 * RocketMQMessageBroker.java
 *
 * ImMessageBroker 的 RocketMQ 实现
 *
 * @author gogym
 */
public class RocketMQMessageBroker implements ImMessageBroker {

    private static final Logger logger = LoggerFactory.getLogger(RocketMQMessageBroker.class);

    private final RocketMQTemplate rocketMQTemplate;

    public RocketMQMessageBroker(RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
    }

    @Override
    public void send(String topic, String tag, String key, String body) {
        try {
            String destination = tag != null ? topic + ":" + tag : topic;

            Message<String> message = MessageBuilder.withPayload(body)
                    .setHeader("KEYS", key)
                    .build();

            rocketMQTemplate.send(destination, message);
            logger.debug("RocketMQ消息发送成功: destination={}, key={}", destination, key);
        } catch (Exception e) {
            logger.error("RocketMQ消息发送失败: topic={}, tag={}, key={}", topic, tag, key, e);
        }
    }
}
