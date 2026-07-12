package io.getbit.gim.friend.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.getbit.gim.friend.entity.ImFriendGroup;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ImFriendGroupRepository extends BaseMapper<ImFriendGroup> {

    default List<ImFriendGroup> findByUserId(Long userId) {
        LambdaQueryWrapper<ImFriendGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ImFriendGroup::getUserId, userId).orderByAsc(ImFriendGroup::getSortOrder);
        return selectList(wrapper);
    }
}
