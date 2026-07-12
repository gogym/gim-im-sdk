-- =====================================================
-- IM 模块数据库表
-- =====================================================

-- 清理已有表（按依赖倒序）
DROP TABLE IF EXISTS `im_friend_request_log`;
DROP TABLE IF EXISTS `im_friend_request`;
DROP TABLE IF EXISTS `im_friend`;
DROP TABLE IF EXISTS `im_friend_group`;
DROP TABLE IF EXISTS `im_group_join_request`;
DROP TABLE IF EXISTS `im_group_member`;
DROP TABLE IF EXISTS `im_group`;
DROP TABLE IF EXISTS `im_user_message`;
DROP TABLE IF EXISTS `im_message`;

-- 1. 消息表（读扩散模型：每条消息只存一份，用户通过 im_user_message 关联）
CREATE TABLE IF NOT EXISTS `im_message` (
    `msg_id`          VARCHAR(64)     NOT NULL COMMENT '消息唯一ID（雪花ID）',
    `conversation_id` VARCHAR(128)    NOT NULL COMMENT '会话ID（单聊: 较小userId_较大userId, 群聊: group_{groupId}）',
    `sender_id`       BIGINT          NOT NULL COMMENT '发送者ID',
    `chat_type`       TINYINT         NOT NULL DEFAULT 1 COMMENT '聊天类型: 1-单聊 2-群聊',
    `content_type`    TINYINT         NOT NULL DEFAULT 1 COMMENT '内容类型: 1-文字 2-图片 3-音频 4-视频 5-文件 6-位置 7-自定义',
    `content`         TEXT            NULL COMMENT '消息内容（JSON格式）',
    `ext`             VARCHAR(512)    NULL COMMENT '扩展字段（JSON，语音时长/图片尺寸等元数据）',
    `status`          TINYINT         NOT NULL DEFAULT 0 COMMENT '消息状态: 0-正常 1-已撤回',
    `client_msg_id`   VARCHAR(64)     NULL COMMENT '客户端消息ID（去重用）',
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`msg_id`),
    KEY `idx_conversation_created` (`conversation_id`, `created_at`),
    KEY `idx_client_msg_id` (`client_msg_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='IM消息表（消息本体）';

-- 1.1 用户-消息关联表（用户收件箱）
CREATE TABLE IF NOT EXISTS `im_user_message` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`         BIGINT          NOT NULL COMMENT '所属用户ID',
    `msg_id`          VARCHAR(64)     NOT NULL COMMENT '关联消息ID',
    `conversation_id` VARCHAR(128)    NOT NULL COMMENT '会话ID（冗余，方便按会话维度查询）',
    `status`          TINYINT         NOT NULL DEFAULT 0 COMMENT '消息状态: 0-已送达 1-已读 2-已撤回',
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_msg` (`user_id`, `msg_id`),
    KEY `idx_user_conversation` (`user_id`, `conversation_id`, `created_at`),
    KEY `idx_user_status` (`user_id`, `status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-消息关联表（用户收件箱）';

-- 2. 好友分组表
CREATE TABLE IF NOT EXISTS `im_friend_group` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`         BIGINT          NOT NULL COMMENT '所属用户ID',
    `name`            VARCHAR(50)     NOT NULL COMMENT '分组名称',
    `sort_order`      INT             NOT NULL DEFAULT 0 COMMENT '排序序号',
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME        NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    UNIQUE KEY `uk_user_name` (`user_id`, `name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友分组表';

-- 3. 好友关系表
CREATE TABLE IF NOT EXISTS `im_friend` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`         BIGINT          NOT NULL COMMENT '用户ID',
    `friend_id`       BIGINT          NOT NULL COMMENT '好友ID',
    `group_id`        BIGINT          NULL COMMENT '好友分组ID（NULL表示未分组）',
    `remark`          VARCHAR(50)     NULL COMMENT '好友备注名',
    `status`          TINYINT         NOT NULL DEFAULT 1 COMMENT '状态: 0-已删除 1-正常 2-已拉黑',
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME        NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_friend` (`user_id`, `friend_id`),
    KEY `idx_friend_id` (`friend_id`),
    KEY `idx_group_id` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友关系表';

-- 4. 好友申请表
CREATE TABLE IF NOT EXISTS `im_friend_request` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `from_user_id`    BIGINT          NOT NULL COMMENT '申请人ID',
    `to_user_id`      BIGINT          NOT NULL COMMENT '被申请人ID',
    `message`         VARCHAR(200)    NULL COMMENT '申请留言',
    `status`          TINYINT         NOT NULL DEFAULT 0 COMMENT '状态: 0-待处理 1-已同意 2-已拒绝 3-已过期',
    `source`          VARCHAR(20)     NULL COMMENT '来源: search/group/qrcode',
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME        NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_from_user` (`from_user_id`),
    KEY `idx_to_user` (`to_user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友申请表';

-- 5. 好友申请消息日志表
CREATE TABLE IF NOT EXISTS `im_friend_request_log` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `request_id`      BIGINT          NOT NULL COMMENT '好友申请ID',
    `from_user_id`    BIGINT          NOT NULL COMMENT '发送者ID',
    `message`         VARCHAR(200)    NULL COMMENT '消息内容',
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_request_id` (`request_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友申请消息日志表';

-- =====================================================
-- 群组相关表
-- =====================================================

-- 6. 群组表
CREATE TABLE IF NOT EXISTS `im_group` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `group_id`        VARCHAR(64)     NOT NULL COMMENT '群组唯一ID（雪花ID）',
    `name`            VARCHAR(100)    NOT NULL COMMENT '群名称',
    `avatar`          VARCHAR(500)    NULL COMMENT '群头像URL',
    `owner_id`        BIGINT          NOT NULL COMMENT '群主用户ID',
    `announcement`    TEXT            NULL COMMENT '群公告',
    `max_members`     INT             NOT NULL DEFAULT 200 COMMENT '最大成员数',
    `join_verify`     TINYINT         NOT NULL DEFAULT 0 COMMENT '入群验证: 0-不需要 1-需要群主审批',
    `mute_all`        TINYINT         NOT NULL DEFAULT 0 COMMENT '全员禁言: 0-否 1-是',
    `status`          TINYINT         NOT NULL DEFAULT 1 COMMENT '状态: 0-已解散 1-正常',
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME        NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_id` (`group_id`),
    KEY `idx_owner_id` (`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组表';

-- 7. 群成员表
CREATE TABLE IF NOT EXISTS `im_group_member` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `group_id`        VARCHAR(64)     NOT NULL COMMENT '群组ID',
    `user_id`         BIGINT          NOT NULL COMMENT '用户ID',
    `nickname`        VARCHAR(50)     NULL COMMENT '群昵称',
    `role`            TINYINT         NOT NULL DEFAULT 0 COMMENT '角色: 0-成员 1-管理员 2-群主',
    `is_muted`        TINYINT         NOT NULL DEFAULT 0 COMMENT '是否被禁言: 0-否 1-是',
    `join_time`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入群时间',
    `status`          TINYINT         NOT NULL DEFAULT 1 COMMENT '状态: 0-已退出/被移除 1-正常',
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME        NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_user` (`group_id`, `user_id`),
    KEY `idx_user_id` (`user_id`, `status`),
    KEY `idx_group_status` (`group_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群成员表';

-- 8. 入群申请表
CREATE TABLE IF NOT EXISTS `im_group_join_request` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `group_id`        VARCHAR(64)     NOT NULL COMMENT '群组ID',
    `user_id`         BIGINT          NOT NULL COMMENT '申请用户ID',
    `message`         VARCHAR(200)    NULL COMMENT '申请留言',
    `status`          TINYINT         NOT NULL DEFAULT 0 COMMENT '状态: 0-待审批 1-已同意 2-已拒绝',
    `handler_id`      BIGINT          NULL COMMENT '处理人 ID',
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `handled_at`      DATETIME        NULL COMMENT '处理时间',
    PRIMARY KEY (`id`),
    KEY `idx_group_status` (`group_id`, `status`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='入群申请表';
