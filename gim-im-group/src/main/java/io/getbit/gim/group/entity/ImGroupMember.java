package io.getbit.gim.group.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data @TableName("im_group_member")
public class ImGroupMember implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private String groupId;
    private Long userId;
    private String nickname;
    /** 角色: 0-成员 1-管理员 2-群主 */
    private Integer role;
    /** 是否被禁言: 0-否 1-是 */
    private Integer isMuted;
    private LocalDateTime joinTime;
    /** 状态: 0-已退出 1-正常 */
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
