package io.getbit.gim.friend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("im_friend_request")
public class ImFriendRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long fromUserId;
    private Long toUserId;
    private String message;
    /** 状态: 0-待处理 1-已同意 2-已拒绝 3-已过期 */
    private Integer status;
    /** 来源: search/group/qrcode */
    private String source;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
