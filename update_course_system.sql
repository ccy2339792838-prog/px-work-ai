ALTER TABLE `course_categories`
ADD COLUMN `industry` varchar(100) DEFAULT NULL COMMENT '所属行业(关联字典表value)';

ALTER TABLE `courses`
ADD COLUMN `credit_hours` decimal(5,1) DEFAULT '0.0' COMMENT '课程学时',
ADD COLUMN `target_roles` varchar(255) DEFAULT NULL COMMENT '适用岗位(多个岗位用逗号分隔，关联字典表value)';

ALTER TABLE `course_hours`
MODIFY COLUMN `type` tinyint NOT NULL DEFAULT '1' COMMENT '类型: 1-视频, 2-图文, 3-文档附件';
