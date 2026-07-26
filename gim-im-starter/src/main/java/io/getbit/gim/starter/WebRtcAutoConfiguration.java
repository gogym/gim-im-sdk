package io.getbit.gim.starter;

import io.getbit.gim.core.bootstrap.IMServerFacade;
import io.getbit.gim.core.spi.ImGroupMemberProvider;
import io.getbit.gim.webrtc.handler.RtcGroupHandler;
import io.getbit.gim.webrtc.handler.RtcSignalHandler;
import io.getbit.gim.webrtc.TurnCredentialService;
import io.getbit.gim.webrtc.WebRtcSessionManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * WebRtcAutoConfiguration.java
 *
 * WebRTC 模块 Spring Boot 自动配置
 *
 * @author gogym
 */
@Configuration
public class WebRtcAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public WebRtcSessionManager webRtcSessionManager() {
        return new WebRtcSessionManager();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "gim.webrtc.turn")
    public TurnCredentialService.TurnConfig turnConfig() {
        return new TurnCredentialService.TurnConfig();
    }

    @Bean
    @ConditionalOnMissingBean
    public TurnCredentialService turnCredentialService(TurnCredentialService.TurnConfig turnConfig) {
        return new TurnCredentialService(
                turnConfig.getStunUrl(),
                turnConfig.getTurnUrl(),
                turnConfig.getSharedSecret(),
                turnConfig.getCredentialTtl());
    }

    /**
     * RTC 信令 Handler 注册器
     * 在 Spring 容器启动时自动将 RTC Handler 注册到 MessageDispatcher
     */
    @Bean
    public RtcHandlerRegistrar rtcHandlerRegistrar(IMServerFacade facade,
                                                    ObjectProvider<ImGroupMemberProvider> groupMemberProviderProvider) {
        return new RtcHandlerRegistrar(facade, groupMemberProviderProvider);
    }

    /**
     * RTC Handler 注册器（在构造时完成注册）
     */
    static class RtcHandlerRegistrar {

        RtcHandlerRegistrar(IMServerFacade facade,
                            ObjectProvider<ImGroupMemberProvider> groupMemberProviderProvider) {
            // 注册单聊 RTC 信令 Handler
            facade.getMessageDispatcher().registerHandler(new RtcSignalHandler(facade));

            // 注册群聊 RTC 信令 Handler（需要 ImGroupMemberProvider）
            ImGroupMemberProvider groupProvider = groupMemberProviderProvider.getIfAvailable();
            if (groupProvider != null) {
                facade.getMessageDispatcher().registerHandler(new RtcGroupHandler(facade, groupProvider));
            }
        }
    }
}
