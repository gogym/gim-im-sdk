package com.example.im.spi;

import io.getbit.gim.core.spi.ImFriendProvider;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * FriendProviderImpl.java
 *
 * 好友关系提供者示例实现
 *
 * TODO: 实际项目中应对接数据库/缓存查询好友关系
 *
 * @author gogym
 */
@Component
public class FriendProviderImpl implements ImFriendProvider {

    // TODO: 注入好友关系 Repository
    // private final ImFriendRepository friendRepository;

    @Override
    public boolean isFriend(String userId, String friendId) {
        // TODO: 查询数据库判断是否为好友
        // return friendRepository.isFriend(Long.parseLong(userId), Long.parseLong(friendId));
        return true; // 示例默认允许
    }

    @Override
    public List<String> getFriendIds(String userId) {
        // TODO: 查询好友列表
        // return friendRepository.findFriendIds(Long.parseLong(userId))
        //         .stream().map(String::valueOf).toList();
        return Collections.emptyList(); // 示例返回空
    }
}
