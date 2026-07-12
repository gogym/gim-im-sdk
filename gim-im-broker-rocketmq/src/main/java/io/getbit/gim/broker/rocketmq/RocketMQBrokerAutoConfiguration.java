package io.getbit.gim.broker.rocketmq;

import io.getbit.gim.core.spi.ImMessageBroker;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RocketMQBrokerAutoConfiguration.java
 *
 * RocketMQ Broker 自动配置
 * 引入 gim-im-broker-rocketmq 依赖后自动装配
 *
 * @author gogym
 */
@Configuration
@ConditionalOnClass(RocketMQTemplate.class)
public class RocketMQBrokerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ImMessageBroker.class)
    @ConditionalOnProperty(name = "rocketmq.name-server")
    public ImMessageBroker rocketMQMessageBroker(RocketMQTemplate rocketMQTemplate) {
        return new RocketMQMessageBroker(rocketMQTemplate);
    }

    /**
     * 消息入库消费者：配置了 rocketmq.name-server 且 storage 模块在 classpath 中时激活
     */
    @Bean
    @ConditionalOnProperty(name = "rocketmq.name-server")
    @ConditionalOnClass(name = "io.getbit.gim.storage.service.MessageStoreHandler")
    public RocketMQMessageStoreConsumer rocketMQMessageStoreConsumer() {
        return new RocketMQMessageStoreConsumer();
    }
}
