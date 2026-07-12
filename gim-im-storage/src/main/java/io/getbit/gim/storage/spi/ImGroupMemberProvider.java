package io.getbit.gim.storage.spi;

import java.util.List;

/**
 * SPI：群成员ID提供者
 * Storage插件通过此接口获取群成员列表（用于群消息读扩散）
 * 如果引入了 gim-im-group 插件，会自动提供实现
 *
 * @author gogym
 */
public interface ImGroupMemberProvider {

    /**
     * 获取群的活跃成员userId列表
     *
     * @param groupId 群组ID
     * @return 成员userId列表
     */
    List<Long> getGroupMemberUserIds(String groupId);
}
