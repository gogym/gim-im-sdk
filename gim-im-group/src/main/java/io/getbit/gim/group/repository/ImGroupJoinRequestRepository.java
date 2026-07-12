package io.getbit.gim.group.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.getbit.gim.group.entity.ImGroupJoinRequest;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface ImGroupJoinRequestRepository extends BaseMapper<ImGroupJoinRequest> {
    default List<ImGroupJoinRequest> findPendingRequests(String groupId) {
        LambdaQueryWrapper<ImGroupJoinRequest> w = new LambdaQueryWrapper<>();
        w.eq(ImGroupJoinRequest::getGroupId, groupId).eq(ImGroupJoinRequest::getStatus, 0)
         .orderByDesc(ImGroupJoinRequest::getCreatedAt);
        return selectList(w);
    }
    default ImGroupJoinRequest findPendingRequest(String groupId, Long userId) {
        LambdaQueryWrapper<ImGroupJoinRequest> w = new LambdaQueryWrapper<>();
        w.eq(ImGroupJoinRequest::getGroupId, groupId).eq(ImGroupJoinRequest::getUserId, userId)
         .eq(ImGroupJoinRequest::getStatus, 0);
        return selectOne(w);
    }
}
