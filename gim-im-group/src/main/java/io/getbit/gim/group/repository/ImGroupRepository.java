package io.getbit.gim.group.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.getbit.gim.group.entity.ImGroup;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ImGroupRepository extends BaseMapper<ImGroup> {
    default ImGroup findByGroupId(String groupId) {
        LambdaQueryWrapper<ImGroup> w = new LambdaQueryWrapper<>();
        w.eq(ImGroup::getGroupId, groupId).eq(ImGroup::getStatus, 1);
        return selectOne(w);
    }
    default boolean updateGroupInfo(String groupId, String name, String avatar, String announcement) {
        LambdaUpdateWrapper<ImGroup> w = new LambdaUpdateWrapper<>();
        w.eq(ImGroup::getGroupId, groupId);
        if (name != null) w.set(ImGroup::getName, name);
        if (avatar != null) w.set(ImGroup::getAvatar, avatar);
        if (announcement != null) w.set(ImGroup::getAnnouncement, announcement);
        return update(null, w) > 0;
    }
    default boolean dissolveGroup(String groupId) {
        LambdaUpdateWrapper<ImGroup> w = new LambdaUpdateWrapper<>();
        w.eq(ImGroup::getGroupId, groupId).set(ImGroup::getStatus, 0);
        return update(null, w) > 0;
    }
    default boolean setMuteAll(String groupId, boolean muteAll) {
        LambdaUpdateWrapper<ImGroup> w = new LambdaUpdateWrapper<>();
        w.eq(ImGroup::getGroupId, groupId).set(ImGroup::getMuteAll, muteAll ? 1 : 0);
        return update(null, w) > 0;
    }
    default boolean transferOwner(String groupId, Long newOwnerId) {
        LambdaUpdateWrapper<ImGroup> w = new LambdaUpdateWrapper<>();
        w.eq(ImGroup::getGroupId, groupId).set(ImGroup::getOwnerId, newOwnerId);
        return update(null, w) > 0;
    }
    default boolean setJoinVerify(String groupId, boolean joinVerify) {
        LambdaUpdateWrapper<ImGroup> w = new LambdaUpdateWrapper<>();
        w.eq(ImGroup::getGroupId, groupId).set(ImGroup::getJoinVerify, joinVerify ? 1 : 0);
        return update(null, w) > 0;
    }
}
