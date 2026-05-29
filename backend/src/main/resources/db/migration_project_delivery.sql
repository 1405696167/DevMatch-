-- 项目交付/管控模式：与需求发布时的付款方式、里程碑规划方同步
-- 在已存在库上执行一次即可（若列已存在会报错，可忽略或手动调整）

ALTER TABLE `t_task`
    ADD COLUMN `milestone_plan_by` VARCHAR(20) NOT NULL DEFAULT 'DEVELOPER'
        COMMENT 'DEVELOPER=开发者拆分里程碑 ENTERPRISE=企业统一规划' AFTER `payment_type`;

ALTER TABLE `t_project`
    ADD COLUMN `payment_type` VARCHAR(20) NOT NULL DEFAULT 'MILESTONE'
        COMMENT 'MILESTONE=分阶段 ONCE=一次性整单' AFTER `amount`;

ALTER TABLE `t_project`
    ADD COLUMN `milestone_plan_by` VARCHAR(20) NOT NULL DEFAULT 'DEVELOPER'
        COMMENT 'DEVELOPER/ENTERPRISE，与立项时任务一致' AFTER `payment_type`;
