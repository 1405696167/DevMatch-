-- DevMatch 数据库初始化脚本
-- 执行前请确保已创建数据库：CREATE DATABASE devmatch DEFAULT CHARACTER SET utf8mb4;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 用户表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_user` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`      VARCHAR(64)     NOT NULL COMMENT '用户名（手机号或邮箱）',
    `nickname`      VARCHAR(64)     NOT NULL COMMENT '昵称',
    `phone`         VARCHAR(20)     DEFAULT NULL COMMENT '手机号',
    `email`         VARCHAR(128)    DEFAULT NULL COMMENT '邮箱',
    `password_hash` VARCHAR(256)    NOT NULL COMMENT 'BCrypt加密密码',
    `role`          VARCHAR(20)     NOT NULL DEFAULT 'DEVELOPER' COMMENT '角色:DEVELOPER/ENTERPRISE/ADMIN',
    `avatar`        VARCHAR(512)    DEFAULT NULL COMMENT '头像URL',
    `bio`           VARCHAR(500)    DEFAULT NULL COMMENT '个人简介',
    `city`          VARCHAR(64)     DEFAULT NULL COMMENT '城市',
    `homepage`      VARCHAR(256)    DEFAULT NULL COMMENT '个人主页',
    `company_name`  VARCHAR(128)    DEFAULT NULL COMMENT '企业名称（企业用户）',
    `credit_score`  INT             NOT NULL DEFAULT 100 COMMENT '信用分',
    `kyc_status`    VARCHAR(20)     NOT NULL DEFAULT 'NONE' COMMENT 'KYC状态:NONE/AUDITING/VERIFIED',
    `status`        VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT '账号状态:ACTIVE/DISABLED',
    `deleted`       TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`),
    UNIQUE KEY `uk_email` (`email`),
    KEY `idx_role` (`role`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ----------------------------
-- 用户技能表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_user_skill` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT,
    `user_id`    BIGINT   NOT NULL COMMENT '用户ID',
    `name`       VARCHAR(64) NOT NULL COMMENT '技能名称',
    `level`      INT      NOT NULL DEFAULT 3 COMMENT '熟练度 1-5',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户技能表';

-- ----------------------------
-- 用户作品集（项目经验）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_user_portfolio` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT       NOT NULL,
    `name`        VARCHAR(128) NOT NULL COMMENT '项目名称',
    `description` TEXT         COMMENT '项目描述',
    `tags`        JSON         COMMENT '技术标签JSON数组',
    `link`        VARCHAR(256) COMMENT '项目链接',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户作品集';

-- ----------------------------
-- KYC认证记录
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_kyc_record` (
    `id`            BIGINT      NOT NULL AUTO_INCREMENT,
    `user_id`       BIGINT      NOT NULL,
    `type`          VARCHAR(20) NOT NULL COMMENT 'DEVELOPER/ENTERPRISE',
    `real_name`     VARCHAR(64) COMMENT '真实姓名',
    `id_number`     VARCHAR(32) COMMENT '身份证号',
    `credit_code`   VARCHAR(32) COMMENT '统一社会信用代码',
    `id_front_url`  VARCHAR(512) COMMENT '证件正面',
    `id_back_url`   VARCHAR(512) COMMENT '证件背面',
    `license_url`   VARCHAR(512) COMMENT '营业执照',
    `status`        VARCHAR(20) NOT NULL DEFAULT 'AUDITING' COMMENT 'AUDITING/VERIFIED/REJECTED',
    `remark`        VARCHAR(256) COMMENT '审核备注',
    `auditor_id`    BIGINT      COMMENT '审核员ID',
    `created_at`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='KYC认证记录';

-- ----------------------------
-- 任务表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_task` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `title`         VARCHAR(200) NOT NULL COMMENT '任务标题',
    `description`   TEXT         COMMENT '需求描述',
    `company_id`    BIGINT       NOT NULL COMMENT '发布企业ID',
    `category`      VARCHAR(64)  COMMENT '项目类型',
    `skills`        JSON         COMMENT '技术栈JSON数组',
    `experience`    VARCHAR(20)  DEFAULT 'NONE' COMMENT '经验要求',
    `require_kyc`   TINYINT      DEFAULT 0 COMMENT '是否需要KYC认证',
    `contract_type` VARCHAR(20)  DEFAULT 'FIXED' COMMENT 'FIXED/HOURLY',
    `payment_type`  VARCHAR(20)  DEFAULT 'MILESTONE' COMMENT 'MILESTONE/ONCE',
    `milestone_plan_by` VARCHAR(20) NOT NULL DEFAULT 'DEVELOPER' COMMENT 'DEVELOPER=开发者拆里程碑 ENTERPRISE=企业规划',
    `budget_min`    DECIMAL(12,2) NOT NULL COMMENT '预算下限',
    `budget_max`    DECIMAL(12,2) NOT NULL COMMENT '预算上限',
    `deadline`      DATE         COMMENT '截止日期',
    `duration_days` INT          COMMENT '项目周期（天）',
    `status`        VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态:DRAFT/AUDITING/PUBLISHED/IN_PROGRESS/CLOSED/COMPLETED',
    `bid_count`     INT          NOT NULL DEFAULT 0 COMMENT '投标人数',
    `view_count`    INT          NOT NULL DEFAULT 0 COMMENT '浏览次数',
    `reject_reason` VARCHAR(256) COMMENT '审核驳回原因',
    `publish_deposit_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '已扣发布押金(元)',
    `attachments`   JSON         COMMENT '附件列表JSON',
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_company_id` (`company_id`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`),
    FULLTEXT KEY `ft_title_desc` (`title`, `description`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务表';

-- ----------------------------
-- 投标表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_bid` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `task_id`     BIGINT       NOT NULL,
    `developer_id` BIGINT      NOT NULL,
    `amount`      DECIMAL(12,2) NOT NULL COMMENT '报价',
    `days`        INT          NOT NULL COMMENT '预计工期（天）',
    `proposal`    TEXT         NOT NULL COMMENT '投标说明',
    `status`      VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SELECTED/REJECTED/CANCELLED',
    `project_id`  BIGINT       COMMENT '中标后关联的项目ID',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_task_id` (`task_id`),
    KEY `idx_developer_id` (`developer_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投标表';

-- ----------------------------
-- 项目表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_project` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `task_id`       BIGINT       COMMENT '来源任务ID',
    `bid_id`        BIGINT       COMMENT '来源投标ID',
    `name`          VARCHAR(200) NOT NULL COMMENT '项目名称',
    `developer_id`  BIGINT       NOT NULL COMMENT '开发者ID',
    `enterprise_id` BIGINT       NOT NULL COMMENT '企业ID',
    `amount`        DECIMAL(12,2) NOT NULL COMMENT '合同金额',
    `payment_type`  VARCHAR(20)  NOT NULL DEFAULT 'MILESTONE' COMMENT 'MILESTONE/ONCE，来自需求',
    `milestone_plan_by` VARCHAR(20) NOT NULL DEFAULT 'DEVELOPER' COMMENT 'DEVELOPER/ENTERPRISE',
    `status`        VARCHAR(20)  NOT NULL DEFAULT 'IN_PROGRESS' COMMENT 'IN_PROGRESS/PENDING_REVIEW/COMPLETED/DISPUTE',
    `progress`      INT          NOT NULL DEFAULT 0 COMMENT '进度0-100',
    `start_date`    DATE         COMMENT '开始日期',
    `end_date`      DATE         COMMENT '结束日期',
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_developer_id` (`developer_id`),
    KEY `idx_enterprise_id` (`enterprise_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目表';

-- ----------------------------
-- 里程碑表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_milestone` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `project_id`  BIGINT       NOT NULL,
    `name`        VARCHAR(128) NOT NULL COMMENT '里程碑名称',
    `description` VARCHAR(500) COMMENT '描述',
    `amount`      DECIMAL(12,2) NOT NULL COMMENT '里程碑金额',
    `deadline`    DATE         COMMENT '截止日期',
    `status`      VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/IN_PROGRESS/SUBMITTED/ACCEPTED/REJECTED',
    `sort_order`  INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `reject_reason` VARCHAR(256) COMMENT '驳回原因',
    `accepted_at` DATETIME     COMMENT '验收时间',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='里程碑表';

-- ----------------------------
-- 交付物表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_deliverable` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `project_id`   BIGINT       NOT NULL,
    `milestone_id` BIGINT       NOT NULL,
    `name`         VARCHAR(256) NOT NULL COMMENT '文件名',
    `path`         VARCHAR(512) NOT NULL COMMENT '存储路径',
    `size`         BIGINT       COMMENT '文件大小（字节）',
    `uploader_id`  BIGINT       NOT NULL COMMENT '上传者ID',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_milestone_id` (`milestone_id`),
    KEY `idx_project_id` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交付物表';

-- ----------------------------
-- 钱包账户表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_wallet` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT,
    `user_id`       BIGINT        NOT NULL UNIQUE COMMENT '用户ID',
    `balance`       DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '可用余额',
    `frozen`        DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '冻结金额',
    `total_income`  DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '累计收入',
    `total_expense` DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '累计支出',
    `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='钱包账户';

-- ----------------------------
-- 交易流水表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_wallet_transaction` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT        NOT NULL,
    `type`        VARCHAR(20)   NOT NULL COMMENT 'INCOME/EXPENSE/RECHARGE/WITHDRAW/FREEZE/UNFREEZE',
    `amount`      DECIMAL(14,2) NOT NULL COMMENT '变动金额（正为增加，负为减少）',
    `balance`     DECIMAL(14,2) NOT NULL COMMENT '变动后余额',
    `description` VARCHAR(256)  COMMENT '描述',
    `ref_id`      VARCHAR(64)   COMMENT '关联业务ID',
    `ref_type`    VARCHAR(32)   COMMENT '关联业务类型',
    `status`      VARCHAR(20)   NOT NULL DEFAULT 'SUCCESS' COMMENT 'SUCCESS/PENDING/FAILED',
    `created_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_type` (`type`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易流水';

-- ----------------------------
-- 充值订单表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_pay_order` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT,
    `order_no`       VARCHAR(64)   NOT NULL UNIQUE COMMENT '订单号',
    `user_id`        BIGINT        NOT NULL,
    `amount`         DECIMAL(12,2) NOT NULL,
    `pay_method`     VARCHAR(20)   COMMENT 'ALIPAY/WECHAT',
    `pay_url`        VARCHAR(1024) COMMENT '支付链接',
    `third_party_no` VARCHAR(128)  COMMENT '第三方交易号',
    `status`         VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SUCCESS/FAILED/CLOSED',
    `paid_at`        DATETIME      COMMENT '支付时间',
    `created_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='充值订单';

-- ----------------------------
-- 提现申请表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_withdraw_record` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT        NOT NULL,
    `amount`      DECIMAL(12,2) NOT NULL,
    `method`      VARCHAR(20)   NOT NULL COMMENT 'ALIPAY/BANK',
    `account`     VARCHAR(128)  NOT NULL COMMENT '收款账号',
    `real_name`   VARCHAR(64)   NOT NULL COMMENT '真实姓名',
    `status`      VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/COMPLETED',
    `remark`      VARCHAR(256)  COMMENT '备注',
    `auditor_id`  BIGINT        COMMENT '审核员',
    `created_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提现申请';

-- ----------------------------
-- 会话表（IM）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_conversation` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `type`         VARCHAR(20)  NOT NULL DEFAULT 'PRIVATE' COMMENT 'PRIVATE/GROUP',
    `project_id`   BIGINT       COMMENT '关联项目ID（项目讨论组）',
    `name`         VARCHAR(128) COMMENT '会话名称（群组）',
    `last_message` VARCHAR(256) COMMENT '最后一条消息',
    `last_time`    DATETIME     COMMENT '最后消息时间',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话表';

-- ----------------------------
-- 会话成员表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_conversation_member` (
    `id`              BIGINT  NOT NULL AUTO_INCREMENT,
    `conversation_id` BIGINT  NOT NULL,
    `user_id`         BIGINT  NOT NULL,
    `unread_count`    INT     NOT NULL DEFAULT 0,
    `joined_at`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_conv_user` (`conversation_id`, `user_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话成员';

-- ----------------------------
-- 聊天消息表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_chat_message` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `conversation_id` BIGINT       NOT NULL,
    `sender_id`       BIGINT       NOT NULL,
    `content`         TEXT         COMMENT '消息内容',
    `type`            VARCHAR(20)  NOT NULL DEFAULT 'TEXT' COMMENT 'TEXT/FILE/IMAGE',
    `file_name`       VARCHAR(256) COMMENT '文件名',
    `file_size`       BIGINT       COMMENT '文件大小',
    `file_url`        VARCHAR(512) COMMENT '文件URL',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_conversation_id` (`conversation_id`),
    KEY `idx_sender_id` (`sender_id`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息';

-- ----------------------------
-- 站内通知表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_notification` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`    BIGINT       NOT NULL,
    `type`       VARCHAR(20)  NOT NULL COMMENT 'PROJECT/PAYMENT/AUDIT/SYSTEM/CHAT',
    `content`    VARCHAR(512) NOT NULL,
    `link`       VARCHAR(256) COMMENT '跳转链接',
    `is_read`    TINYINT      NOT NULL DEFAULT 0,
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_is_read` (`is_read`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内通知';

-- ----------------------------
-- 评价表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_review` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT,
    `project_id`  BIGINT      NOT NULL,
    `reviewer_id` BIGINT      NOT NULL COMMENT '评价方',
    `reviewee_id` BIGINT      NOT NULL COMMENT '被评价方',
    `rating`      INT         NOT NULL COMMENT '评分 1-5',
    `content`     VARCHAR(500) COMMENT '文字评价',
    `tags`        JSON        COMMENT '评价标签',
    `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_reviewee_id` (`reviewee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价表';

-- ----------------------------
-- 申诉表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_complaint` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `project_id`    BIGINT       NOT NULL,
    `complainant_id` BIGINT      NOT NULL COMMENT '申诉方',
    `respondent_id`  BIGINT      NOT NULL COMMENT '被申诉方',
    `title`         VARCHAR(200) NOT NULL,
    `content`       TEXT         NOT NULL,
    `status`        VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSING/RESOLVED',
    `result`        VARCHAR(30)  COMMENT 'COMPLAINANT_WIN/RESPONDENT_WIN/COMPROMISE',
    `remark`        VARCHAR(500) COMMENT '处理说明',
    `handler_id`    BIGINT       COMMENT '处理员',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='申诉表';

-- ----------------------------
-- 系统配置表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_system_config` (
    `id`          INT          NOT NULL AUTO_INCREMENT,
    `config_key`  VARCHAR(64)  NOT NULL UNIQUE,
    `config_value` VARCHAR(512) NOT NULL,
    `description` VARCHAR(256),
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置';

-- ----------------------------
-- 公告表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_announcement` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `title`      VARCHAR(200) NOT NULL,
    `content`    TEXT         NOT NULL,
    `type`       VARCHAR(20)  NOT NULL DEFAULT 'SYSTEM' COMMENT 'SYSTEM/MAINTENANCE/ACTIVITY',
    `active`     TINYINT      NOT NULL DEFAULT 1,
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告表';

SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------
-- 初始化数据
-- ----------------------------
-- 默认管理员账号（密码: admin123456）
INSERT IGNORE INTO `t_user` (`username`, `nickname`, `phone`, `password_hash`, `role`, `kyc_status`, `status`)
VALUES ('admin', '系统管理员', '10000000000', '$2a$10$7EqJtq98hPqEX7fNZaFWoO3S4g5bZ0y3hT9/pJqDfZYfFI2j4C9Gy', 'ADMIN', 'VERIFIED', 'ACTIVE');

-- 为管理员创建钱包
INSERT IGNORE INTO `t_wallet` (`user_id`, `balance`, `frozen`) SELECT id, 0, 0 FROM `t_user` WHERE username = 'admin';

-- 系统默认配置
INSERT IGNORE INTO `t_system_config` (`config_key`, `config_value`, `description`) VALUES
('commission_rate', '0.05', '平台服务费率'),
('withdraw_rate', '0', '提现手续费率'),
('min_withdraw', '100', '最低提现金额'),
('max_withdraw', '50000', '最大单次提现金额'),
('auto_audit_task', 'false', '任务自动审核'),
('kyc_mode', 'MANUAL', 'KYC审核方式'),
('sla_duration', '48', '审核SLA小时数'),
('task_publish_deposit_rate', '0.05', '需求发布押金占预算上限比例(0~1)');
