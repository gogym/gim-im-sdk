package io.getbit.gim.friend.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.getbit.gim.friend.entity.ImFriend;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ImFriendRepository extends BaseMapper<ImFriend> {

    default List<ImFriend> findFriendsByUserId(Long userId) {
        LambdaQueryWrapper<ImFriend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ImFriend::getUserId, userId).eq(ImFriend::getStatus, 1)
               .orderByDesc(ImFriend::getCreatedAt);
        return selectList(wrapper);
    }

    default List<ImFriend> findFriendsByGroupId(Long userId, Long groupId) {
        LambdaQueryWrapper<ImFriend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ImFriend::getUserId, userId).eq(ImFriend::getGroupId, groupId)
               .eq(ImFriend::getStatus, 1);
        return selectList(wrapper);
    }

    default ImFriend selectByUserIdAndFriendId(Long userId, Long friendId) {
        LambdaQueryWrapper<ImFriend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ImFriend::getUserId, userId).eq(ImFriend::getFriendId, friendId);
        return selectOne(wrapper);
    }

    default int deleteByUserIdAndFriendId(Long userId, Long friendId) {
        LambdaQueryWrapper<ImFriend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ImFriend::getUserId, userId).eq(ImFriend::getFriendId, friendId);
        return delete(wrapper);
    }
}
