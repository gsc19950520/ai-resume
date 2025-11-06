-- 创建数据库
CREATE DATABASE IF NOT EXISTS ai_resume CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ai_resume;

-- 用户表
CREATE TABLE IF NOT EXISTS user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    open_id VARCHAR(100) NOT NULL UNIQUE COMMENT '微信openId',
    nickname VARCHAR(50) COMMENT '用户昵称',
    avatar_url VARCHAR(255) COMMENT '头像URL',
    gender INT COMMENT '性别 0:未知 1:男 2:女',
    country VARCHAR(50) COMMENT '国家',
    province VARCHAR(50) COMMENT '省份',
    city VARCHAR(50) COMMENT '城市',
    remaining_optimize_count INT NOT NULL DEFAULT 0 COMMENT '剩余优化次数',
    is_vip TINYINT NOT NULL DEFAULT 0 COMMENT '是否VIP 0:否 1:是',
    vip_expire_time DATETIME COMMENT 'VIP过期时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_open_id (open_id)
) COMMENT='用户表';

-- 产品表
CREATE TABLE IF NOT EXISTS product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id VARCHAR(50) NOT NULL UNIQUE COMMENT '产品ID',
    name VARCHAR(100) NOT NULL COMMENT '产品名称',
    description TEXT COMMENT '产品描述',
    price INT NOT NULL COMMENT '价格(分)',
    type VARCHAR(50) NOT NULL COMMENT '产品类型',
    is_active TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用 0:否 1:是',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_product_id (product_id),
    INDEX idx_type (type)
) COMMENT='产品表';

-- 订单表
CREATE TABLE IF NOT EXISTS ai_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(50) NOT NULL UNIQUE COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_id VARCHAR(50) NOT NULL COMMENT '产品ID',
    product_type VARCHAR(50) NOT NULL COMMENT '产品类型',
    amount INT NOT NULL COMMENT '订单金额(分)',
    status INT NOT NULL DEFAULT 0 COMMENT '订单状态 0:待支付 1:支付成功 2:支付失败 3:已取消',
    transaction_id VARCHAR(100) COMMENT '微信支付交易ID',
    pay_type VARCHAR(20) COMMENT '支付方式',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    pay_time DATETIME COMMENT '支付时间',
    INDEX idx_order_no (order_no),
    INDEX idx_user_id (user_id),
    INDEX idx_product_id (product_id),
    INDEX idx_status (status),
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) COMMENT='订单表';



-- 模板表（另一个模板表，可能有不同用途）
CREATE TABLE IF NOT EXISTS template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '模板名称',
    description TEXT COMMENT '模板描述',
    thumbnail_url VARCHAR(255) NOT NULL COMMENT '缩略图URL',
    template_url VARCHAR(255) NOT NULL COMMENT '模板URL',
    job_type VARCHAR(50) NOT NULL COMMENT '职位类型',
    price INT NOT NULL COMMENT '价格(分)',
    is_free TINYINT NOT NULL DEFAULT 0 COMMENT '是否免费 0:否 1:是',
    is_vip_only TINYINT NOT NULL DEFAULT 0 COMMENT '是否仅VIP可用 0:否 1:是',
    use_count INT NOT NULL DEFAULT 0 COMMENT '使用次数',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_job_type (job_type),
    INDEX idx_is_free (is_free),
    INDEX idx_is_vip_only (is_vip_only)
) COMMENT='模板表';

-- 简历表
CREATE TABLE IF NOT EXISTS resume (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    original_filename VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_path VARCHAR(255) NOT NULL COMMENT '文件路径',
    file_type VARCHAR(20) NOT NULL COMMENT '文件类型',
    job_type VARCHAR(50) NOT NULL COMMENT '职位类型',
    original_content LONGTEXT COMMENT '原始内容',
    optimized_content LONGTEXT COMMENT '优化后内容',
    ai_score INT COMMENT 'AI评分',
    ai_suggestion TEXT COMMENT 'AI建议',
    download_url_pdf VARCHAR(255) COMMENT 'PDF下载链接',
    download_url_word VARCHAR(255) COMMENT 'Word下载链接',
    template_id BIGINT COMMENT '使用的模板ID',
    template_config TEXT COMMENT '模板配置信息',
    status INT NOT NULL DEFAULT 0 COMMENT '状态 0:上传成功 1:优化中 2:优化成功 3:优化失败',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_template_id (template_id),
    INDEX idx_job_type (job_type),
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (template_id) REFERENCES template(id) ON DELETE SET NULL
) COMMENT='简历表';

-- 面试会话表
CREATE TABLE IF NOT EXISTS `interview_session` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `session_id` varchar(100) NOT NULL COMMENT '会话ID',
  `user_id` varchar(50) NOT NULL COMMENT '用户ID',
  `resume_id` bigint(20) NOT NULL COMMENT '简历ID',
  `job_type` varchar(100) NOT NULL COMMENT '工作类型',
  `city` varchar(50) NOT NULL COMMENT '城市',
  `status` varchar(20) NOT NULL COMMENT '状态：pending, in_progress, completed, canceled',
  `total_score` double DEFAULT NULL COMMENT '总分',
  `tech_score` double DEFAULT NULL COMMENT '技术评分',
  `logic_score` double DEFAULT NULL COMMENT '逻辑评分',
  `clarity_score` double DEFAULT NULL COMMENT '表达清晰度评分',
  `depth_score` double DEFAULT NULL COMMENT '深度评分',
  `ai_estimated_years` varchar(20) DEFAULT NULL COMMENT 'AI估计经验年限',
  `ai_salary_range` varchar(20) DEFAULT NULL COMMENT 'AI薪资范围',
  `confidence` double DEFAULT NULL COMMENT '置信度',
  `report_url` varchar(255) DEFAULT NULL COMMENT '报告URL',
  `max_depth_per_point` int(11) DEFAULT NULL COMMENT '每个点的最大深度',
  `max_followups` int(11) DEFAULT NULL COMMENT '最大追问次数',
  `time_limit_secs` int(11) DEFAULT NULL COMMENT '时间限制(秒)',
  `actual_duration_secs` int(11) DEFAULT NULL COMMENT '实际时长(秒)',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_id` (`session_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_resume_id` (`resume_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试会话表';

-- 面试日志表
CREATE TABLE IF NOT EXISTS `interview_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `question_id` varchar(100) NOT NULL COMMENT '问题ID',
  `session_id` varchar(100) NOT NULL COMMENT '会话ID',
  `question_text` text NOT NULL COMMENT '问题内容',
  `user_answer_text` text COMMENT '用户文字答案',
  `user_answer_audio_url` varchar(255) DEFAULT NULL COMMENT '用户音频答案URL',
  `depth_level` varchar(20) DEFAULT NULL COMMENT '深度级别：basic, intermediate, advanced',
  `tech_score` double DEFAULT NULL COMMENT '技术评分',
  `logic_score` double DEFAULT NULL COMMENT '逻辑评分',
  `clarity_score` double DEFAULT NULL COMMENT '表达清晰度评分',
  `depth_score` double DEFAULT NULL COMMENT '深度评分',
  `feedback` text COMMENT '反馈内容',
  `matched_points` text COMMENT '匹配的关键点',
  `round_number` int(11) NOT NULL COMMENT '轮次',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `answer_duration_secs` int(11) DEFAULT NULL COMMENT '回答时长(秒)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_question_id` (`question_id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_round_number` (`round_number`),
  CONSTRAINT `fk_interview_log_session` FOREIGN KEY (`session_id`) REFERENCES `interview_session` (`session_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试日志表';
