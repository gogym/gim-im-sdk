package io.getbit.gim.storage.config;

import io.getbit.gim.core.config.properties.GimProperties;
import io.getbit.gim.core.spi.ImIdGenerator;
import io.getbit.gim.core.spi.ImMessageBroker;
import io.getbit.gim.storage.controller.MessageController;
import io.getbit.gim.storage.manager.ImMessageManager;
import io.getbit.gim.storage.repository.ImMessageRepository;
import io.getbit.gim.storage.repository.ImUserMessageRepository;
import io.getbit.gim.storage.service.MessageService;
import io.getbit.gim.storage.service.MessageStoreHandler;
import io.getbit.gim.storage.service.MessageStoreProducer;
import io.getbit.gim.storage.spi.ImGroupMemberProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

/**
 * Storage 插件自动配置
 *
 * @author gogym
 */
@Configuration
public class StorageAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ImMessageManager imMessageManager(ImMessageRepository msgRepo,
                                              ImUserMessageRepository userMsgRepo) {
        return new ImMessageManager(msgRepo, userMsgRepo);
    }

    @Bean
    @ConditionalOnMissingBean
    public MessageStoreProducer messageStoreProducer(GimProperties gimProperties,
                                                      ImMessageBroker broker,
                                                      ImIdGenerator idGen) {
        return new MessageStoreProducer(gimProperties, broker, idGen);
    }

    @Bean
    @ConditionalOnMissingBean
    public MessageStoreHandler messageStoreHandler(ImMessageManager manager,
                                                    ImGroupMemberProvider provider,
                                                    ImIdGenerator idGen) {
        return new MessageStoreHandler(manager, provider, idGen);
    }

    /**
     * 默认 NoOp 实现：如果没有 group 插件，群消息不扩散
     */
    @Bean
    @ConditionalOnMissingBean(ImGroupMemberProvider.class)
    public ImGroupMemberProvider noOpGroupMemberProvider() {
        return groupId -> Collections.emptyList();
    }

    // ==================== 业务服务层 ====================

    @Bean
    @ConditionalOnMissingBean
    public MessageService messageService(ImMessageManager messageManager) {
        return new MessageService(messageManager);
    }

    /**
     * Web 环境下自动注册 MessageController
     */
    @Configuration
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    static class StorageWebConfiguration {
        @Bean
        @ConditionalOnMissingBean
        public MessageController messageController(MessageService messageService,
                                                     io.getbit.gim.core.spi.ImUserContextResolver userContextResolver) {
            return new MessageController(messageService, userContextResolver);
        }
    }
}
