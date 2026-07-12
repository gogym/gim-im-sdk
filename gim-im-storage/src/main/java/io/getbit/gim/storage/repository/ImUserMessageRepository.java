package io.getbit.gim.storage.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.getbit.gim.storage.entity.ImUserMessage;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户-消息关联数据访问层
 *
 * @author gogym
 */
@Mapper
public interface ImUserMessageRepository extends BaseMapper<ImUserMessage> {

    default List<ImUserMessage> findUserMessages(Long userId, String conversationId,
                                                  LocalDateTime beforeTime, int pageSize) {
        LambdaQueryWrapper<ImUserMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ImUserMessage::getUserId, userId)
               .eq(ImUserMessage::getConversationId, conversationId);
        if (beforeTime != null) {
            wrapper.lt(ImUserMessage::getCreatedAt, beforeTime);
        }
        wrapper.orderByDesc(ImUserMessage::getCreatedAt);
        wrapper.last("LIMIT " + pageSize);
        return selectList(wrapper);
    }

    default List<ImUserMessage> syncMessages(Long userId, LocalDateTime sinceTime, int maxLimit) {
        LambdaQueryWrapper<ImUserMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ImUserMessage::getUserId, userId);
        if (sinceTime != null) {
            wrapper.gt(ImUserMessage::getCreatedAt, sinceTime);
        }
        wrapper.orderByAsc(ImUserMessage::getCreatedAt);
        wrapper.last("LIMIT " + maxLimit);
        return selectList(wrapper);
    }

    default Long countUnread(Long userId) {
        LambdaQueryWrapper<ImUserMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ImUserMessage::getUserId, userId)
               .eq(ImUserMessage::getStatus, 0);
        return selectCount(wrapper);
    }

    default Long countUnreadByConversation(Long userId, String conversationId) {
        LambdaQueryWrapper<ImUserMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ImUserMessage::getUserId, userId)
               .eq(ImUserMessage::getConversationId, conversationId)
               .eq(ImUserMessage::getStatus, 0);
        return selectCount(wrapper);
    }

    default int markAsRead(Long userId, String conversationId) {
        LambdaUpdateWrapper<ImUserMessage> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ImUserMessage::getUserId, userId)
               .eq(ImUserMessage::getConversationId, conversationId)
               .eq(ImUserMessage::getStatus, 0)
               .set(ImUserMessage::getStatus, 1);
        return update(null, wrapper);
    }

    default int markAsRecalled(String msgId) {
        LambdaUpdateWrapper<ImUserMessage> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ImUserMessage::getMsgId, msgId)
               .set(ImUserMessage::getStatus, 2);
        return update(null, wrapper);
    }

    default ImUserMessage findByUserIdAndMsgId(Long userId, String msgId) {
        LambdaQueryWrapper<ImUserMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ImUserMessage::getUserId, userId)
               .eq(ImUserMessage::getMsgId, msgId);
        return selectOne(wrapper);
    }

    default List<ImUserMessage> findByMsgId(String msgId) {
        LambdaQueryWrapper<ImUserMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ImUserMessage::getMsgId, msgId);
        return selectList(wrapper);
    }

    default int deleteByConversationId(Long userId, String conversationId) {
        LambdaQueryWrapper<ImUserMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ImUserMessage::getUserId, userId)
               .eq(ImUserMessage::getConversationId, conversationId);
        return delete(wrapper);
    }
}
