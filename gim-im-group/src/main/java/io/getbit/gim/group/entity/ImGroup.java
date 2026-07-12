package io.getbit.gim.group.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data @TableName("im_group")
public class ImGroup implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private String groupId;
    private String name;
    private String avatar;
    private Long ownerId;
    private String announcement;
    private Integer maxMembers;
    /** 入群验证: 0-不需要 1-需要 */
    private Integer joinVerify;
    /** 全员禁言: 0-否 1-是 */
    private Integer muteAll;
    /** 状态: 0-已解散 1-正常 */
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
