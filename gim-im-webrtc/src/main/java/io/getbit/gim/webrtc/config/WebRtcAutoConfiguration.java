package io.getbit.gim.webrtc.config;

import io.getbit.gim.webrtc.TurnCredentialService;
import io.getbit.gim.webrtc.WebRtcSessionManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebRtcAutoConfiguration {

    @Bean @ConditionalOnMissingBean
    public WebRtcSessionManager webRtcSessionManager() { return new WebRtcSessionManager(); }

    @Bean @ConditionalOnMissingBean
    public TurnCredentialService turnCredentialService() { return new TurnCredentialService(); }
}
