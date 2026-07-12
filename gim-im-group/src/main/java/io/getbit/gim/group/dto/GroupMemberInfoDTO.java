package io.getbit.gim.group.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 群成员信息 DTO（含用户昵称、头像）
 *
 * @author gogym
 */
@Data
public class GroupMemberInfoDTO {
    private Long id;
    private String groupId;
    private Long userId;
    private String nickname;
    private Integer role;
    private Integer isMuted;
    private LocalDateTime joinTime;
    private Integer status;
    private String userName;
    private String userAvatar;
}
