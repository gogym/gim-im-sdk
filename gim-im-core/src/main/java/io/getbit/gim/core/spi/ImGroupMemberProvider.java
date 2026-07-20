package io.getbit.gim.core.spi;

import java.util.List;

/**
 * ImGroupMemberProvider.java
 *
 * SPI接口：群组成员提供者
 * 群聊消息投递时，SDK 通过此接口获取群成员列表和禁言状态
 * 使用方需实现此接口对接自己的群组数据源（DB / Redis / 微服务等）
 *
 * @author gogym
 */
public interface ImGroupMemberProvider {

    /**
     * 获取群活跃成员的 userId 列表
     * 用于群聊消息路由投递
     *
     * @param groupId 群组ID
     * @return 群成员 userId 列表（不含发送者，由 SDK 过滤）
     */
    List<String> getGroupMemberUserIds(String groupId);

    /**
     * 检查用户是否可以在群中发送消息
     * 用于群消息禁言检查（全员禁言 + 个人禁言）
     *
     * @param groupId 群组ID
     * @param userId  用户ID
     * @return null=可以发送, 非null=禁止原因（如"全员禁言中"、"你已被禁言"）
     */
    default String checkCanSendMessage(String groupId, String userId) {
        return null;
    }

    /**
     * 检查用户是否为群成员
     *
     * @param groupId 群组ID
     * @param userId  用户ID
     * @return true=是群成员
     */
    default boolean isGroupMember(String groupId, String userId) {
        return true;
    }
}
