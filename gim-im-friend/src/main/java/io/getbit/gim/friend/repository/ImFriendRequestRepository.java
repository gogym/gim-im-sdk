package io.getbit.gim.friend.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.getbit.gim.friend.entity.ImFriendRequest;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ImFriendRequestRepository extends BaseMapper<ImFriendRequest> {

    default List<ImFriendRequest> findAllByToUserId(Long toUserId) {
        LambdaQueryWrapper<ImFriendRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ImFriendRequest::getToUserId, toUserId)
               .orderByDesc(ImFriendRequest::getUpdatedAt, ImFriendRequest::getCreatedAt);
        return selectList(wrapper);
    }

    default ImFriendRequest findLatestRequest(Long fromUserId, Long toUserId) {
        LambdaQueryWrapper<ImFriendRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ImFriendRequest::getFromUserId, fromUserId)
               .eq(ImFriendRequest::getToUserId, toUserId)
               .orderByDesc(ImFriendRequest::getCreatedAt).last("LIMIT 1");
        return selectOne(wrapper);
    }

    default List<ImFriendRequest> findByFromUserId(Long fromUserId) {
        LambdaQueryWrapper<ImFriendRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ImFriendRequest::getFromUserId, fromUserId)
               .orderByDesc(ImFriendRequest::getCreatedAt);
        return selectList(wrapper);
    }

    default ImFriendRequest findLatestBetween(Long userId1, Long userId2) {
        LambdaQueryWrapper<ImFriendRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                .and(inner -> inner.eq(ImFriendRequest::getFromUserId, userId1)
                                   .eq(ImFriendRequest::getToUserId, userId2))
                .or(inner -> inner.eq(ImFriendRequest::getFromUserId, userId2)
                                   .eq(ImFriendRequest::getToUserId, userId1)))
               .orderByDesc(ImFriendRequest::getCreatedAt).last("LIMIT 1");
        return selectOne(wrapper);
    }
}
