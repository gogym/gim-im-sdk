package io.getbit.gim.friend.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.getbit.gim.friend.entity.ImFriendRequestLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ImFriendRequestLogRepository extends BaseMapper<ImFriendRequestLog> {

    default List<ImFriendRequestLog> findByRequestId(Long requestId) {
        LambdaQueryWrapper<ImFriendRequestLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ImFriendRequestLog::getRequestId, requestId)
               .orderByAsc(ImFriendRequestLog::getCreatedAt);
        return selectList(wrapper);
    }
}
