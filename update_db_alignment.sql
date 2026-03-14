-- 根据当前代码结构对齐数据库（MySQL 5.7 兼容）

-- users: id_card / enterprise / job_role / industry / is_first_login + uk_id_card
SET @has_uk_email = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND INDEX_NAME = 'uk_email'
);
SET @sql_drop_uk_email = IF(@has_uk_email > 0, 'ALTER TABLE `users` DROP INDEX `uk_email`', 'SELECT 1');
PREPARE stmt_drop_uk_email FROM @sql_drop_uk_email;
EXECUTE stmt_drop_uk_email;
DEALLOCATE PREPARE stmt_drop_uk_email;

SET @has_id_card = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'id_card'
);
SET @has_enterprise = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'enterprise'
);
SET @has_job_role = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'job_role'
);
SET @has_industry = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'industry'
);
SET @has_is_first_login = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'is_first_login'
);
SET @has_uk_id_card = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND INDEX_NAME = 'uk_id_card'
);
SET @sql_add_id_card = IF(
  @has_id_card = 0,
  'ALTER TABLE `users` ADD COLUMN `id_card` varchar(18) NOT NULL DEFAULT '''' COMMENT ''身份证号(登录账号)'' AFTER `avatar`',
  'SELECT 1'
);
PREPARE stmt_add_id_card FROM @sql_add_id_card;
EXECUTE stmt_add_id_card;
DEALLOCATE PREPARE stmt_add_id_card;

SET @sql_add_enterprise = IF(
  @has_enterprise = 0,
  'ALTER TABLE `users` ADD COLUMN `enterprise` varchar(100) NOT NULL DEFAULT '''' COMMENT ''所属企业'' AFTER `id_card`',
  'SELECT 1'
);
PREPARE stmt_add_enterprise FROM @sql_add_enterprise;
EXECUTE stmt_add_enterprise;
DEALLOCATE PREPARE stmt_add_enterprise;

SET @sql_add_job_role = IF(
  @has_job_role = 0,
  'ALTER TABLE `users` ADD COLUMN `job_role` varchar(100) NOT NULL DEFAULT '''' COMMENT ''岗位(关联字典表value)'' AFTER `enterprise`',
  'SELECT 1'
);
PREPARE stmt_add_job_role FROM @sql_add_job_role;
EXECUTE stmt_add_job_role;
DEALLOCATE PREPARE stmt_add_job_role;

SET @sql_add_industry = IF(
  @has_industry = 0,
  'ALTER TABLE `users` ADD COLUMN `industry` varchar(100) NOT NULL DEFAULT '''' COMMENT ''所属行业(关联字典表value)'' AFTER `job_role`',
  'SELECT 1'
);
PREPARE stmt_add_industry FROM @sql_add_industry;
EXECUTE stmt_add_industry;
DEALLOCATE PREPARE stmt_add_industry;

SET @sql_add_is_first_login = IF(
  @has_is_first_login = 0,
  'ALTER TABLE `users` ADD COLUMN `is_first_login` tinyint(1) DEFAULT 1 COMMENT ''是否首次登录 1:是 0:否'' AFTER `password`',
  'SELECT 1'
);
PREPARE stmt_add_is_first_login FROM @sql_add_is_first_login;
EXECUTE stmt_add_is_first_login;
DEALLOCATE PREPARE stmt_add_is_first_login;

SET @sql_add_uk_id_card = IF(
  @has_uk_id_card = 0,
  'ALTER TABLE `users` ADD UNIQUE KEY `uk_id_card` (`id_card`)',
  'SELECT 1'
);
PREPARE stmt_add_uk_id_card FROM @sql_add_uk_id_card;
EXECUTE stmt_add_uk_id_card;
DEALLOCATE PREPARE stmt_add_uk_id_card;

-- course_categories: industry
SET @has_course_category_industry = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'course_categories' AND COLUMN_NAME = 'industry'
);
SET @sql_alter_course_categories = IF(
  @has_course_category_industry = 0,
  'ALTER TABLE `course_categories` ADD COLUMN `industry` varchar(100) DEFAULT NULL COMMENT ''所属行业(关联字典表value)''',
  'SELECT 1'
);
PREPARE stmt_alter_course_categories FROM @sql_alter_course_categories;
EXECUTE stmt_alter_course_categories;
DEALLOCATE PREPARE stmt_alter_course_categories;

-- courses: credit_hours / target_roles / training_batch / course_mode / offline_location
SET @has_credit_hours = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'courses' AND COLUMN_NAME = 'credit_hours'
);
SET @has_target_roles = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'courses' AND COLUMN_NAME = 'target_roles'
);
SET @has_training_batch = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'courses' AND COLUMN_NAME = 'training_batch'
);
SET @has_course_mode = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'courses' AND COLUMN_NAME = 'course_mode'
);
SET @has_offline_location = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'courses' AND COLUMN_NAME = 'offline_location'
);
SET @sql_add_credit_hours = IF(
  @has_credit_hours = 0,
  'ALTER TABLE `courses` ADD COLUMN `credit_hours` decimal(5,1) DEFAULT ''0.0'' COMMENT ''课程学时''',
  'SELECT 1'
);
PREPARE stmt_add_credit_hours FROM @sql_add_credit_hours;
EXECUTE stmt_add_credit_hours;
DEALLOCATE PREPARE stmt_add_credit_hours;

SET @sql_add_target_roles = IF(
  @has_target_roles = 0,
  'ALTER TABLE `courses` ADD COLUMN `target_roles` varchar(255) DEFAULT NULL COMMENT ''适用岗位(多个岗位用逗号分隔，关联字典表value)''',
  'SELECT 1'
);
PREPARE stmt_add_target_roles FROM @sql_add_target_roles;
EXECUTE stmt_add_target_roles;
DEALLOCATE PREPARE stmt_add_target_roles;

SET @sql_add_training_batch = IF(
  @has_training_batch = 0,
  'ALTER TABLE `courses` ADD COLUMN `training_batch` varchar(100) DEFAULT NULL COMMENT ''培训批次''',
  'SELECT 1'
);
PREPARE stmt_add_training_batch FROM @sql_add_training_batch;
EXECUTE stmt_add_training_batch;
DEALLOCATE PREPARE stmt_add_training_batch;

SET @sql_add_course_mode = IF(
  @has_course_mode = 0,
  'ALTER TABLE `courses` ADD COLUMN `course_mode` tinyint DEFAULT 1 COMMENT ''授课模式: 1-线上录播 2-线上直播 3-线下集中''',
  'SELECT 1'
);
PREPARE stmt_add_course_mode FROM @sql_add_course_mode;
EXECUTE stmt_add_course_mode;
DEALLOCATE PREPARE stmt_add_course_mode;

SET @sql_add_offline_location = IF(
  @has_offline_location = 0,
  'ALTER TABLE `courses` ADD COLUMN `offline_location` varchar(255) DEFAULT NULL COMMENT ''线下授课地点''',
  'SELECT 1'
);
PREPARE stmt_add_offline_location FROM @sql_add_offline_location;
EXECUTE stmt_add_offline_location;
DEALLOCATE PREPARE stmt_add_offline_location;

-- course_hours: type 注释 + live_url + playback_url
ALTER TABLE `course_hours`
MODIFY COLUMN `type` tinyint NOT NULL DEFAULT '1' COMMENT '类型: 1-视频, 2-图文, 3-文档附件';

SET @has_live_url = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'course_hours' AND COLUMN_NAME = 'live_url'
);
SET @has_playback_url = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'course_hours' AND COLUMN_NAME = 'playback_url'
);
SET @sql_add_live_url = IF(
  @has_live_url = 0,
  'ALTER TABLE `course_hours` ADD COLUMN `live_url` varchar(500) DEFAULT NULL COMMENT ''直播链接''',
  'SELECT 1'
);
PREPARE stmt_add_live_url FROM @sql_add_live_url;
EXECUTE stmt_add_live_url;
DEALLOCATE PREPARE stmt_add_live_url;

SET @sql_add_playback_url = IF(
  @has_playback_url = 0,
  'ALTER TABLE `course_hours` ADD COLUMN `playback_url` varchar(500) DEFAULT NULL COMMENT ''回放链接''',
  'SELECT 1'
);
PREPARE stmt_add_playback_url FROM @sql_add_playback_url;
EXECUTE stmt_add_playback_url;
DEALLOCATE PREPARE stmt_add_playback_url;

-- 新增字典与RBAC表
CREATE TABLE IF NOT EXISTS `sys_dicts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `dict_type` varchar(50) NOT NULL COMMENT '字典类型(如 job_role, industry)',
  `dict_label` varchar(100) NOT NULL COMMENT '展示标签',
  `dict_value` varchar(100) NOT NULL COMMENT '实际存储值',
  `sort` int DEFAULT '0' COMMENT '排序',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统数据字典表';

CREATE TABLE IF NOT EXISTS `admin_menus` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父级ID',
  `name` varchar(100) NOT NULL COMMENT '菜单名称',
  `path` varchar(200) DEFAULT NULL COMMENT '路由路径',
  `component` varchar(200) DEFAULT NULL COMMENT '组件路径',
  `perms` varchar(200) DEFAULT NULL COMMENT '权限标识',
  `type` tinyint NOT NULL COMMENT '类型:1目录 2菜单 3按钮',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='后台菜单权限表';

CREATE TABLE IF NOT EXISTS `admin_role_menu` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`role_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色菜单关联表';

-- 学习交互表
CREATE TABLE IF NOT EXISTS `user_course_enrollments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '学员ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态 0-学习中 1-已学完',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_course` (`user_id`, `course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='选课表';

CREATE TABLE IF NOT EXISTS `course_assignments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `title` varchar(255) NOT NULL COMMENT '作业标题',
  `content` text COMMENT '作业内容',
  `attachment_url` varchar(500) DEFAULT NULL COMMENT '附件链接',
  `deadline` datetime DEFAULT NULL COMMENT '截止时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_course_id` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业表';

CREATE TABLE IF NOT EXISTS `assignment_submissions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `assignment_id` bigint NOT NULL COMMENT '作业ID',
  `user_id` bigint NOT NULL COMMENT '学员ID',
  `content` text COMMENT '提交内容',
  `attachment_url` varchar(500) DEFAULT NULL COMMENT '附件链接',
  `score` decimal(5,2) DEFAULT NULL COMMENT '得分',
  `comment` varchar(500) DEFAULT NULL COMMENT '评语',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态 0-待批改 1-已批改',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_assignment_user` (`assignment_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业提交表';

CREATE TABLE IF NOT EXISTS `process_evaluations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '学员ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `score_progress` decimal(5,2) DEFAULT '0.00' COMMENT '进度分',
  `score_prep` decimal(5,2) DEFAULT '0.00' COMMENT '预习分',
  `score_interaction` decimal(5,2) DEFAULT '0.00' COMMENT '互动分',
  `score_discussion` decimal(5,2) DEFAULT '0.00' COMMENT '讨论分',
  `score_practical` decimal(5,2) DEFAULT '0.00' COMMENT '实操分',
  `total_score` decimal(5,2) DEFAULT '0.00' COMMENT '总分,满分30',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_course` (`user_id`, `course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='过程评价表';

CREATE TABLE IF NOT EXISTS `offline_attendance` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '学员ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `punch_time` datetime NOT NULL COMMENT '打卡时间',
  `punch_type` tinyint NOT NULL COMMENT '打卡类型 1-早打卡 2-晚打卡',
  `location` varchar(255) DEFAULT NULL COMMENT '打卡地点',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_course` (`user_id`, `course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='线下打卡表';
