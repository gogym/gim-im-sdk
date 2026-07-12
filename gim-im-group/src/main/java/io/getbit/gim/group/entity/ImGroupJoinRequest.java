package io.getbit.gim.group.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data @TableName("im_group_join_request")
public class ImGroupJoinRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private String groupId;
    private Long userId;
    private String message;
    /** 状态: 0-待审批 1-已同意 2-已拒绝 */
    private Integer status;
    private Long handlerId;
    private LocalDateTime createdAt;
    private LocalDateTime handledAt;
}
