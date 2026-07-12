package io.getbit.gim.storage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * IM 消息实体（读扩散模型：消息本体全局唯一存储）
 *
 * @author gogym
 */
@Data
@TableName("im_message")
public class ImMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 消息唯一ID（雪花ID），作为主键 */
    @TableId(type = IdType.INPUT)
    private String msgId;

    /** 会话ID */
    private String conversationId;

    /** 发送者ID */
    private Long senderId;

    /** 聊天类型: 1-单聊 2-群聊 */
    private Integer chatType;

    /** 内容类型: 1-文字 2-图片 3-音频 4-视频 5-文件 6-位置 7-自定义 */
    private Integer contentType;

    /** 消息内容（JSON） */
    private String content;

    /** 扩展字段（JSON，语音时长/图片尺寸等元数据） */
    private String ext;

    /** 消息状态: 0-正常 1-已撤回 */
    private Integer status;

    /** 客户端消息ID（去重用） */
    private String clientMsgId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
}
