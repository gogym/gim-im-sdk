package io.getbit.gim.group.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.getbit.gim.group.entity.ImGroupMember;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface ImGroupMemberRepository extends BaseMapper<ImGroupMember> {
    default List<ImGroupMember> findActiveMembers(String groupId) {
        LambdaQueryWrapper<ImGroupMember> w = new LambdaQueryWrapper<>();
        w.eq(ImGroupMember::getGroupId, groupId).eq(ImGroupMember::getStatus, 1)
         .orderByDesc(ImGroupMember::getRole).orderByAsc(ImGroupMember::getJoinTime);
        return selectList(w);
    }
    default List<Long> findActiveMemberUserIds(String groupId) {
        LambdaQueryWrapper<ImGroupMember> w = new LambdaQueryWrapper<>();
        w.select(ImGroupMember::getUserId).eq(ImGroupMember::getGroupId, groupId).eq(ImGroupMember::getStatus, 1);
        return selectList(w).stream().map(ImGroupMember::getUserId).toList();
    }
    default ImGroupMember findMember(String groupId, Long userId) {
        LambdaQueryWrapper<ImGroupMember> w = new LambdaQueryWrapper<>();
        w.eq(ImGroupMember::getGroupId, groupId).eq(ImGroupMember::getUserId, userId).eq(ImGroupMember::getStatus, 1);
        return selectOne(w);
    }
    default List<String> findGroupIdsByUserId(Long userId) {
        LambdaQueryWrapper<ImGroupMember> w = new LambdaQueryWrapper<>();
        w.select(ImGroupMember::getGroupId).eq(ImGroupMember::getUserId, userId).eq(ImGroupMember::getStatus, 1);
        return selectList(w).stream().map(ImGroupMember::getGroupId).toList();
    }
    default long countActiveMembers(String groupId) {
        LambdaQueryWrapper<ImGroupMember> w = new LambdaQueryWrapper<>();
        w.eq(ImGroupMember::getGroupId, groupId).eq(ImGroupMember::getStatus, 1);
        return selectCount(w);
    }
    default boolean removeMember(String groupId, Long userId) {
        LambdaUpdateWrapper<ImGroupMember> w = new LambdaUpdateWrapper<>();
        w.eq(ImGroupMember::getGroupId, groupId).eq(ImGroupMember::getUserId, userId).set(ImGroupMember::getStatus, 0);
        return update(null, w) > 0;
    }
    default boolean setMemberRole(String groupId, Long userId, int role) {
        LambdaUpdateWrapper<ImGroupMember> w = new LambdaUpdateWrapper<>();
        w.eq(ImGroupMember::getGroupId, groupId).eq(ImGroupMember::getUserId, userId).set(ImGroupMember::getRole, role);
        return update(null, w) > 0;
    }
    default boolean setMemberMuted(String groupId, Long userId, boolean muted) {
        LambdaUpdateWrapper<ImGroupMember> w = new LambdaUpdateWrapper<>();
        w.eq(ImGroupMember::getGroupId, groupId).eq(ImGroupMember::getUserId, userId).set(ImGroupMember::getIsMuted, muted ? 1 : 0);
        return update(null, w) > 0;
    }
    default List<ImGroupMember> findAdminMembers(String groupId) {
        LambdaQueryWrapper<ImGroupMember> w = new LambdaQueryWrapper<>();
        w.eq(ImGroupMember::getGroupId, groupId).eq(ImGroupMember::getStatus, 1).ge(ImGroupMember::getRole, 1);
        return selectList(w);
    }
}
