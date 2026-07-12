package io.getbit.gim.storage.service;

import com.google.protobuf.util.JsonFormat;
import io.getbit.gim.core.config.properties.GimProperties;
import io.getbit.gim.core.spi.ImIdGenerator;
import io.getbit.gim.core.spi.ImMessageBroker;
import io.getbit.gim.protocol.codec.ImProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 消息入库 MQ 生产者
 * 将聊天消息异步发送到 MQ，由消费者异步入库 MySQL
 *
 * @author gogym
 */
public class MessageStoreProducer {

    private static final Logger logger = LoggerFactory.getLogger(MessageStoreProducer.class);

    private final GimProperties gimProperties;
    private final ImMessageBroker messageBroker;
    private final ImIdGenerator idGenerator;

    public MessageStoreProducer(GimProperties gimProperties, ImMessageBroker messageBroker, ImIdGenerator idGenerator) {
        this.gimProperties = gimProperties;
        this.messageBroker = messageBroker;
        this.idGenerator = idGenerator;
    }

    /**
     * 异步发送消息到入库队列
     *
     * @param chatMsg  聊天消息
     * @param senderId 发送者ID
     * @return 生成的 msgId
     */
    public String sendToStore(ImProto.ChatMessage chatMsg, String senderId) {
        try {
            String msgId = (chatMsg.getMsgId() == null || chatMsg.getMsgId().isEmpty())
                    ? idGenerator.generateMsgId()
                    : chatMsg.getMsgId();

            // 将生成的 msgId 回写到 ChatMessage
            ImProto.ChatMessage enrichedMsg = chatMsg.toBuilder()
                    .setMsgId(msgId)
                    .build();

            String storeTopic = gimProperties.getMsg().getStoreTopic();
            String msgJson = JsonFormat.printer().print(enrichedMsg);
            messageBroker.send(storeTopic, "STORE", msgId, msgJson);
            logger.debug("消息入库MQ发送成功, msgId={}", msgId);
            return msgId;
        } catch (Exception e) {
            logger.error("消息入库MQ处理异常", e);
            return idGenerator.generateMsgId();
        }
    }
}
