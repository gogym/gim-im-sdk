package io.getbit.gim.core.spi;

/**
 * ImMessageBroker.java
 *
 * SPI接口：消息中间件（MQ）适配器
 * 用于消息异步入库、离线消息推送等
 * 默认提供 RocketMQ 实现（gim-im-broker-rocketmq），也可对接 Kafka / RabbitMQ
 *
 * @author gogym
 */
public interface ImMessageBroker {

    /**
     * 发送消息到 MQ（异步入库）
     *
     * @param topic    MQ topic
     * @param tag      MQ tag（可为null）
     * @param key      消息key（可为null）
     * @param body     消息体（JSON字符串）
     */
    void send(String topic, String tag, String key, String body);

    /**
     * 发送离线消息通知
     *
     * @param topic MQ topic
     * @param body  消息体
     */
    default void sendOffline(String topic, String body) {
        send(topic, "OFFLINE", null, body);
    }
}
