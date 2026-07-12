package io.getbit.gim.storage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户-消息关联实体（用户收件箱）
 * 读扩散模型：每条消息只存一份，用户通过此表与消息ID关联
 *
 * @author gogym
 */
@Data
@TableName("im_user_message")
public class ImUserMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 所属用户ID */
    private Long userId;

    /** 关联消息ID */
    private String msgId;

    /** 会话ID（冗余，方便按会话维度查询） */
    private String conversationId;

    /** 消息状态: 0-已送达 1-已读 2-已撤回 */
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
}
