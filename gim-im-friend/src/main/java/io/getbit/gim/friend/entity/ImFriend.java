package io.getbit.gim.friend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("im_friend")
public class ImFriend implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private Long friendId;
    private Long groupId;
    private String remark;
    /** 状态: 0-已删除 1-正常 2-已拉黑 */
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
