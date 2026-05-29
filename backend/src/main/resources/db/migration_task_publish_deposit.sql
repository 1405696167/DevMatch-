-- 已有库升级：需求发布押金（执行一次即可）
ALTER TABLE `t_task`
    ADD COLUMN `publish_deposit_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '已扣发布押金(元)' AFTER `reject_reason`;
-- 若列已存在会报错，可忽略后仅执行 INSERT IGNORE

INSERT IGNORE INTO `t_system_config` (`config_key`, `config_value`, `description`) VALUES
('task_publish_deposit_rate', '0.05', '需求发布押金占预算上限比例(0~1)');
