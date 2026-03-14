ALTER TABLE `courses`
ADD COLUMN `training_batch` varchar(100) DEFAULT NULL COMMENT '培训批次',
ADD COLUMN `course_mode` tinyint DEFAULT 1 COMMENT '授课模式: 1-线上录播 2-线上直播 3-线下集中',
ADD COLUMN `offline_location` varchar(255) DEFAULT NULL COMMENT '线下授课地点';

ALTER TABLE `course_hours`
ADD COLUMN `live_url` varchar(500) DEFAULT NULL COMMENT '直播链接',
ADD COLUMN `playback_url` varchar(500) DEFAULT NULL COMMENT '回放链接';

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
