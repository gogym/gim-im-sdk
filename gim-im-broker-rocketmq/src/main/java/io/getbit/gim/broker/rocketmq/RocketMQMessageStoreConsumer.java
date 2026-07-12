package io.getbit.gim.broker.rocketmq;

import io.getbit.gim.storage.service.MessageStoreHandler;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * RocketMQ 消息入库消费者
 * 从 RocketMQ 消费聊天消息，调用 MessageStoreHandler 异步入库 MySQL
 * <p>
 * 消息本体只存一份，同时为用户创建关联记录（读扩散模型）
 * <p>
 * 激活条件：配置了 rocketmq.name-server 且 storage 模块在 classpath 中
 *
 * @author gogym
 */
@ConditionalOnProperty(name = "rocketmq.name-server")
@RocketMQMessageListener(
        topic = "${rocketmq.topic.im-store:IM_STORE}",
        consumerGroup = "IM_STORE_CONSUMER_GROUP"
)
public class RocketMQMessageStoreConsumer implements RocketMQListener<String> {

    private static final Logger logger = LoggerFactory.getLogger(RocketMQMessageStoreConsumer.class);

    @Autowired(required = false)
    private MessageStoreHandler messageStoreHandler;

    @Override
    public void onMessage(String message) {
        if (messageStoreHandler == null) {
            logger.debug("MessageStoreHandler 未配置，跳过消息入库");
            return;
        }
        try {
            messageStoreHandler.handleStoreMessage(message);
        } catch (Exception e) {
            logger.error("消息入库消费异常", e);
            throw new RuntimeException("消息入库消费失败", e);
        }
    }
}
