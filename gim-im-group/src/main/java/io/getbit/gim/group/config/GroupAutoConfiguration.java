package io.getbit.gim.group.config;

import io.getbit.gim.core.connection.channel.ChannelManager;
import io.getbit.gim.core.routing.ClusterMessageRouter;
import io.getbit.gim.core.routing.UserRouteService;
import io.getbit.gim.core.spi.ImIdGenerator;
import io.getbit.gim.core.spi.ImEventListener;
import io.getbit.gim.group.controller.GroupController;
import io.getbit.gim.group.manager.ImGroupJoinRequestManager;
import io.getbit.gim.group.manager.ImGroupManager;
import io.getbit.gim.group.manager.ImGroupMemberManager;
import io.getbit.gim.group.notify.GroupNotifyService;
import io.getbit.gim.group.repository.ImGroupJoinRequestRepository;
import io.getbit.gim.group.repository.ImGroupMemberRepository;
import io.getbit.gim.group.repository.ImGroupRepository;
import io.getbit.gim.group.service.GroupService;
import io.getbit.gim.storage.spi.ImGroupMemberProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class GroupAutoConfiguration {

    @Bean @ConditionalOnMissingBean
    public ImGroupManager imGroupManager(ImGroupRepository repo) { return new ImGroupManager(repo); }

    @Bean @ConditionalOnMissingBean
    public ImGroupMemberManager imGroupMemberManager(ImGroupMemberRepository repo) { return new ImGroupMemberManager(repo); }

    @Bean @ConditionalOnMissingBean
    public ImGroupJoinRequestManager imGroupJoinRequestManager(ImGroupJoinRequestRepository repo) { return new ImGroupJoinRequestManager(repo); }

    @Bean @ConditionalOnMissingBean
    public GroupNotifyService groupNotifyService(ChannelManager channelManager, UserRouteService userRouteService,
                                                  ClusterMessageRouter clusterMessageRouter, ImGroupMemberManager memberManager,
                                                  List<ImEventListener> eventListeners) {
        return new GroupNotifyService(channelManager, userRouteService, clusterMessageRouter, memberManager, eventListeners);
    }

    @Bean @ConditionalOnMissingBean
    public GroupService groupService(ImGroupManager groupManager, ImGroupMemberManager memberManager,
                                      ImGroupJoinRequestManager joinRequestManager, ImIdGenerator idGenerator,
                                      GroupNotifyService groupNotifyService) {
        return new GroupService(groupManager, memberManager, joinRequestManager, idGenerator, groupNotifyService);
    }

    /** 为 storage 插件提供群成员列表 */
    @Bean @ConditionalOnMissingBean(ImGroupMemberProvider.class)
    public ImGroupMemberProvider groupMemberProviderAdapter(ImGroupMemberManager memberManager) {
        return memberManager::findActiveMemberUserIds;
    }

    /**
     * Web 环境下自动注册 GroupController
     */
    @Configuration
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    static class GroupWebConfiguration {
        @Bean
        @ConditionalOnMissingBean
        public GroupController groupController(GroupService groupService,
                                                io.getbit.gim.core.spi.ImUserContextResolver userContextResolver) {
            return new GroupController(groupService, userContextResolver);
        }
    }
}
