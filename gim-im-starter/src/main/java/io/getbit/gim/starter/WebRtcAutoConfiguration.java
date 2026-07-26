package io.getbit.gim.starter;

import io.getbit.gim.webrtc.TurnCredentialService;
import io.getbit.gim.webrtc.WebRtcSessionManager;
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
}
