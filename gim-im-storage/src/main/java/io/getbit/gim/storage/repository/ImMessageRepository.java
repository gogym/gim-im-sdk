package io.getbit.gim.storage.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.getbit.gim.storage.entity.ImMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * IM 消息数据访问层
 *
 * @author gogym
 */
@Mapper
public interface ImMessageRepository extends BaseMapper<ImMessage> {

    default ImMessage findByMsgId(String msgId) {
        return selectById(msgId);
    }

    default int recallByMsgId(String msgId, String newContent, String newExt) {
        LambdaUpdateWrapper<ImMessage> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ImMessage::getMsgId, msgId)
               .set(ImMessage::getContent, newContent)
               .set(ImMessage::getExt, newExt)
               .set(ImMessage::getStatus, 1);
        return update(null, wrapper);
    }

    default ImMessage findByClientMsgId(String clientMsgId) {
        LambdaQueryWrapper<ImMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ImMessage::getClientMsgId, clientMsgId)
               .last("LIMIT 1");
        return selectOne(wrapper);
    }
}
