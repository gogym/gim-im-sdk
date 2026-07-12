package io.getbit.gim.friend.config;

import io.getbit.gim.core.connection.channel.ChannelManager;
import io.getbit.gim.core.routing.ClusterMessageRouter;
import io.getbit.gim.core.routing.UserRouteService;
import io.getbit.gim.core.spi.ImEventListener;
import io.getbit.gim.friend.controller.FriendController;
import io.getbit.gim.friend.manager.ImFriendManager;
import io.getbit.gim.friend.manager.ImFriendRequestLogManager;
import io.getbit.gim.friend.manager.ImFriendRequestManager;
import io.getbit.gim.friend.notify.FriendNotifyHandler;
import io.getbit.gim.friend.repository.ImFriendGroupRepository;
import io.getbit.gim.friend.repository.ImFriendRepository;
import io.getbit.gim.friend.repository.ImFriendRequestLogRepository;
import io.getbit.gim.friend.repository.ImFriendRequestRepository;
import io.getbit.gim.friend.service.FriendService;
import io.getbit.gim.friend.spi.ImUserInfoProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class FriendAutoConfiguration {

    @Bean @ConditionalOnMissingBean
    public ImFriendManager imFriendManager(ImFriendRepository friendRepo, ImFriendGroupRepository groupRepo) {
        return new ImFriendManager(friendRepo, groupRepo);
    }

    @Bean @ConditionalOnMissingBean
    public ImFriendRequestManager imFriendRequestManager(ImFriendRequestRepository repo) {
        return new ImFriendRequestManager(repo);
    }

    @Bean @ConditionalOnMissingBean
    public ImFriendRequestLogManager imFriendRequestLogManager(ImFriendRequestLogRepository repo) {
        return new ImFriendRequestLogManager(repo);
    }

    @Bean @ConditionalOnMissingBean
    public FriendNotifyHandler friendNotifyHandler(ChannelManager channelManager,
                                                    UserRouteService userRouteService,
                                                    ClusterMessageRouter clusterMessageRouter,
                                                    ImFriendManager friendManager,
                                                    List<ImEventListener> eventListeners) {
        return new FriendNotifyHandler(channelManager, userRouteService, clusterMessageRouter, friendManager, eventListeners);
    }

    @Bean @ConditionalOnMissingBean
    public FriendService friendService(ImFriendManager friendManager,
                                        ImFriendRequestManager requestManager,
                                        ImFriendRequestLogManager logManager,
                                        FriendNotifyHandler notifyHandler,
                                        ImUserInfoProvider userInfoProvider) {
        return new FriendService(friendManager, requestManager, logManager, notifyHandler, userInfoProvider);
    }

    /** 默认 NoOp 用户信息提供者 */
    @Bean @ConditionalOnMissingBean(ImUserInfoProvider.class)
    public ImUserInfoProvider noOpUserInfoProvider() {
        return userId -> null;
    }

    /**
     * Web 环境下自动注册 FriendController
     */
    @Configuration
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    static class FriendWebConfiguration {
        @Bean
        @ConditionalOnMissingBean
        public FriendController friendController(FriendService friendService,
                                                  io.getbit.gim.core.spi.ImUserContextResolver userContextResolver) {
            return new FriendController(friendService, userContextResolver);
        }
    }
}
