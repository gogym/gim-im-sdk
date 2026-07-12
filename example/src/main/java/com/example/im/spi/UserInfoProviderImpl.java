package com.example.im.spi;

import io.getbit.gim.friend.spi.ImUserInfoProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 用户信息提供者实现
 * <p>
 * SDK 在以下场景调用此接口：
 * <ul>
 *   <li>好友列表丰富化 — 返回好友的昵称、头像、账号</li>
 *   <li>好友申请丰富化 — 返回申请人/被申请人的昵称、头像</li>
 *   <li>群成员列表丰富化 — 返回成员的昵称、头像</li>
 * </ul>
 * <p>
 * 实际项目中应查询你的用户服务或用户表。
 */
@Component
public class UserInfoProviderImpl implements ImUserInfoProvider {

    private static final Logger log = LoggerFactory.getLogger(UserInfoProviderImpl.class);

    // ===== 实际项目中请注入 UserService 查询真实用户数据 =====

    @Override
    public ImUserInfo getUserById(Long userId) {
        // TODO: 替换为你的用户查询逻辑
        // 例如:
        //   User user = userService.getById(userId);
        //   if (user == null) return null;
        //   return new ImUserInfo(user.getId(), user.getNickname(), user.getAvatar(), user.getAccount());

        // 示例：返回模拟数据
        return new ImUserInfo(
                userId,
                "用户" + userId,           // nickname
                "https://example.com/avatar/" + userId + ".png",  // avatar
                "account_" + userId        // account
        );
    }
}
