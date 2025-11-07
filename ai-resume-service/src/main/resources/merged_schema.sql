-- AI简历系统完整数据库脚本
-- 合并了所有表结构和初始化数据

-- 创建数据库
CREATE DATABASE IF NOT EXISTS ai_resume CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ai_resume;

-- ========================================
-- 表结构定义部分
-- ========================================

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

-- 模板表
CREATE TABLE IF NOT EXISTS template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '模板名称',
    description TEXT COMMENT '模板描述',
    thumbnail_url VARCHAR(255) NOT NULL COMMENT '缩略图URL',
    template_url VARCHAR(255) NOT NULL COMMENT '模板URL',
    job_type VARCHAR(50) NOT NULL COMMENT '职位类型',
    price INT NOT NULL COMMENT '价格(分)',
    is_free TINYINT NOT NULL DEFAULT 0 COMMENT '是否免费 0:否 1:是',
    vip_only TINYINT NOT NULL DEFAULT 0 COMMENT '是否仅VIP可用 0:否 1:是',
    use_count INT NOT NULL DEFAULT 0 COMMENT '使用次数',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_job_type (job_type),
    INDEX idx_is_free (is_free),
    INDEX idx_vip_only (vip_only)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板表';

-- 简历表
CREATE TABLE IF NOT EXISTS resume (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    original_filename VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_path VARCHAR(255) NOT NULL COMMENT '文件路径',
    file_type VARCHAR(50) NOT NULL COMMENT '文件类型',
    job_type VARCHAR(100) NOT NULL COMMENT '职位类型',
    original_content TEXT COMMENT '原始内容',
    optimized_content TEXT COMMENT '优化后内容',
    ai_score INT DEFAULT NULL COMMENT 'AI评分',
    ai_suggestion TEXT COMMENT 'AI建议',
    download_url_pdf VARCHAR(255) DEFAULT NULL COMMENT 'PDF下载链接',
    download_url_word VARCHAR(255) DEFAULT NULL COMMENT 'Word下载链接',
    template_id BIGINT DEFAULT NULL COMMENT '模板ID',
    template_config TEXT COMMENT '模板配置信息',
    job_type_id BIGINT DEFAULT NULL COMMENT '关联 job_type(id)',
    status INT NOT NULL DEFAULT 0 COMMENT '状态 0:上传成功 1:优化中 2:优化成功 3:优化失败',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_template_id (template_id),
    INDEX idx_job_type (job_type),
    INDEX idx_job_type_id (job_type_id),
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (template_id) REFERENCES template(id) ON DELETE SET NULL,
    CONSTRAINT fk_resume_job_type FOREIGN KEY (job_type_id) REFERENCES job_type (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='简历表';

-- 领域表
CREATE TABLE IF NOT EXISTS domain (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    domain_name VARCHAR(100) NOT NULL COMMENT '领域名称',
    description TEXT COMMENT '领域描述',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_domain_name (domain_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='领域表';

-- 职位类型表
CREATE TABLE IF NOT EXISTS job_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_name VARCHAR(100) NOT NULL COMMENT '职位名称',
    domain_id BIGINT NOT NULL COMMENT '所属领域ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_job_name (job_name),
    KEY idx_domain_id (domain_id),
    CONSTRAINT fk_job_type_domain FOREIGN KEY (domain_id) REFERENCES domain (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='职位类型表';

-- 面试会话表
CREATE TABLE IF NOT EXISTS interview_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(100) NOT NULL UNIQUE COMMENT '会话ID',
    user_id BIGINT NOT NULL COMMENT '用户ID，引用 user(id)',
    resume_id BIGINT NOT NULL COMMENT '简历ID',
    job_type VARCHAR(100) NOT NULL COMMENT '职位类型',
    city VARCHAR(50) NOT NULL COMMENT '城市',
    status VARCHAR(20) NOT NULL COMMENT '状态：pending, in_progress, completed, canceled',
    total_score DOUBLE DEFAULT NULL COMMENT '总分',
    tech_score DOUBLE DEFAULT NULL COMMENT '技术得分',
    logic_score DOUBLE DEFAULT NULL COMMENT '逻辑得分',
    clarity_score DOUBLE DEFAULT NULL COMMENT '表达清晰得分',
    depth_score DOUBLE DEFAULT NULL COMMENT '深度得分',
    ai_estimated_years VARCHAR(20) DEFAULT NULL COMMENT 'AI预估工作年限',
    ai_salary_range VARCHAR(20) DEFAULT NULL COMMENT 'AI预估薪资范围',
    confidence DOUBLE DEFAULT NULL COMMENT '置信度',
    report_url VARCHAR(255) DEFAULT NULL COMMENT '报告URL',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    start_time DATETIME DEFAULT NULL COMMENT '开始时间',
    end_time DATETIME DEFAULT NULL COMMENT '结束时间',
    session_seconds INT DEFAULT 900 COMMENT '会话时长（秒）',
    session_time_remaining INT DEFAULT 900 COMMENT '剩余时间（秒）',
    tech_items LONGTEXT COMMENT '技术项（JSON格式）',
    project_points LONGTEXT COMMENT '项目点（JSON格式）',
    interview_state LONGTEXT COMMENT '面试状态（JSON格式）',
    consecutive_no_match_count INT DEFAULT 0 COMMENT '连续不匹配次数',
    stop_reason VARCHAR(100) DEFAULT NULL COMMENT '停止原因',
    persona VARCHAR(50) DEFAULT 'friendly' COMMENT '面试官风格',
    ai_question_seed INT DEFAULT NULL COMMENT 'AI问题随机种子',
    adaptive_level VARCHAR(20) DEFAULT 'auto' COMMENT '题目深度模式：auto/fixed',
    question_count INT DEFAULT 0 COMMENT '当前提问数量',
    INDEX idx_user_id (user_id),
    INDEX idx_resume_id (resume_id),
    INDEX idx_status (status),
    INDEX idx_job_type (job_type),
    INDEX idx_session_id (session_id),
    CONSTRAINT fk_interview_session_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试会话表';

-- 面试日志表
CREATE TABLE IF NOT EXISTS interview_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id VARCHAR(100) NOT NULL UNIQUE COMMENT '问题ID',
    session_id VARCHAR(100) NOT NULL COMMENT '会话ID',
    question_text TEXT NOT NULL COMMENT '问题文本',
    user_answer_text TEXT COMMENT '用户回答文本',
    user_answer_audio_url VARCHAR(255) DEFAULT NULL COMMENT '用户回答音频URL',
    depth_level VARCHAR(20) DEFAULT NULL COMMENT '深度级别',
    tech_score DOUBLE DEFAULT NULL COMMENT '技术得分',
    logic_score DOUBLE DEFAULT NULL COMMENT '逻辑得分',
    clarity_score DOUBLE DEFAULT NULL COMMENT '表达清晰得分',
    depth_score DOUBLE DEFAULT NULL COMMENT '深度得分',
    feedback TEXT COMMENT '反馈',
    matched_points TEXT COMMENT '匹配点（JSON格式）',
    round_number INT NOT NULL COMMENT '轮次',
    answer_duration INT DEFAULT NULL COMMENT '回答时长（秒）',
    related_tech_items TEXT COMMENT '关联技术项（JSON格式）',
    related_project_points TEXT COMMENT '关联项目点（JSON格式）',
    stop_reason VARCHAR(100) DEFAULT NULL COMMENT '停止原因',
    persona VARCHAR(50) DEFAULT NULL COMMENT '当前题目使用的面试官语气风格',
    ai_feedback_json LONGTEXT COMMENT 'AI原始评分和分析结果',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_session_id (session_id),
    INDEX idx_question_id (question_id),
    INDEX idx_round_number (round_number),
    CONSTRAINT fk_interview_log_session FOREIGN KEY (session_id) REFERENCES interview_session (session_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试日志表';

-- 面试问题表
CREATE TABLE IF NOT EXISTS interview_question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_text TEXT NOT NULL COMMENT '问题内容',
    expected_key_points TEXT COMMENT '期望的关键点(JSON格式)',
    job_type_id BIGINT NOT NULL COMMENT '适用职位ID',
    skill_tag VARCHAR(100) COMMENT '技能标签',
    depth_level VARCHAR(20) NOT NULL COMMENT '深度级别：usage/implementation/principle/optimization',
    persona VARCHAR(50) COMMENT '语言风格标签：friendly/formal/challenging',
    ai_generated BOOLEAN DEFAULT TRUE COMMENT '是否AI生成',
    usage_count INT DEFAULT 0 COMMENT '使用次数',
    avg_score FLOAT DEFAULT 0 COMMENT '平均得分',
    similarity_hash VARCHAR(64) COMMENT '相似性哈希值',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_job_type_id (job_type_id),
    KEY idx_depth_level (depth_level),
    KEY idx_skill_tag (skill_tag),
    KEY idx_similarity_hash (similarity_hash),
    KEY idx_usage_count (usage_count),
    CONSTRAINT fk_interview_question_job_type FOREIGN KEY (job_type_id) REFERENCES job_type (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试问题表';

-- 职位技能表
CREATE TABLE IF NOT EXISTS job_skill (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_type_id BIGINT NOT NULL COMMENT '职位ID',
    skill_name VARCHAR(100) NOT NULL COMMENT '技能名称',
    skill_type VARCHAR(50) COMMENT '技能类型',
    skill_level VARCHAR(20) DEFAULT 'intermediate' COMMENT '技能级别：entry/intermediate/advanced/expert',
    importance INT DEFAULT 1 COMMENT '重要程度',
    ai_generated BOOLEAN DEFAULT TRUE COMMENT '是否AI生成',
    generated_at DATETIME DEFAULT NULL COMMENT 'AI生成时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_job_type_id (job_type_id),
    KEY idx_skill_name (skill_name),
    KEY idx_skill_level (skill_level),
    KEY idx_ai_generated (ai_generated),
    CONSTRAINT fk_job_skill_job_type FOREIGN KEY (job_type_id) REFERENCES job_type (id) ON DELETE CASCADE,
    UNIQUE KEY uk_job_skill_level (job_type_id, skill_name, skill_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='职位技能表';

-- 评分体系表
CREATE TABLE IF NOT EXISTS scoring_system (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    metric_name VARCHAR(100) NOT NULL COMMENT '评分指标名称',
    weight DOUBLE NOT NULL COMMENT '权重',
    description TEXT COMMENT '指标描述',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_metric_name (metric_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评分体系表';

-- 薪资信息表
CREATE TABLE IF NOT EXISTS salary_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_type_id BIGINT NOT NULL COMMENT '职位ID',
    city VARCHAR(50) NOT NULL COMMENT '城市',
    salary_range VARCHAR(50) NOT NULL COMMENT '薪资范围',
    experience VARCHAR(50) COMMENT '经验要求',
    confidence INT DEFAULT 80 COMMENT '置信度',
    salary_level VARCHAR(50) COMMENT '薪资水平',
    trend_change VARCHAR(20) COMMENT '趋势变化',
    ai_generated BOOLEAN DEFAULT FALSE COMMENT '是否由AI生成',
    prompt_template TEXT COMMENT '生成提示模板',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_job_type_id (job_type_id),
    KEY idx_city (city),
    KEY idx_ai_generated (ai_generated),
    CONSTRAINT fk_salary_info_job_type FOREIGN KEY (job_type_id) REFERENCES job_type (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='薪资信息表';

-- 成长建议表
CREATE TABLE IF NOT EXISTS growth_advice (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_type_id BIGINT NOT NULL COMMENT '职位ID',
    recommended_skills TEXT COMMENT '推荐技能，JSON数组格式',
    long_term_path TEXT COMMENT '长期发展路径，JSON数组格式',
    short_term_advice TEXT COMMENT '短期建议',
    mid_term_advice TEXT COMMENT '中期建议',
    long_term_advice TEXT COMMENT '长期建议',
    ai_generated BOOLEAN DEFAULT FALSE COMMENT '是否由AI生成',
    prompt_template TEXT COMMENT '生成提示模板',
    user_performance_data TEXT COMMENT '用户表现数据，用于个性化生成',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_job_type_id (job_type_id),
    KEY idx_ai_generated (ai_generated),
    CONSTRAINT fk_growth_advice_job_type FOREIGN KEY (job_type_id) REFERENCES job_type (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成长建议表';

-- 系统配置表
CREATE TABLE IF NOT EXISTS system_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    config_value TEXT NOT NULL COMMENT '配置值',
    description VARCHAR(255) COMMENT '配置描述',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- AI运行日志表
CREATE TABLE IF NOT EXISTS ai_trace_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(100) NOT NULL COMMENT '会话ID',
    action_type VARCHAR(50) NOT NULL COMMENT '动作类型：generate_question / score_answer / give_advice',
    prompt_input LONGTEXT COMMENT '输入Prompt',
    ai_response LONGTEXT COMMENT 'AI响应结果',
    tokens_used INT DEFAULT NULL COMMENT '使用的Token数',
    latency_ms INT DEFAULT NULL COMMENT '响应耗时',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_session_id (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI运行日志表';

-- 动态配置表
CREATE TABLE IF NOT EXISTS dynamic_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_type VARCHAR(50) NOT NULL COMMENT '配置类型：template, persona等',
    config_key VARCHAR(100) NOT NULL COMMENT '配置键名',
    config_value TEXT COMMENT '配置值（JSON格式）',
    description VARCHAR(255) DEFAULT NULL COMMENT '配置描述',
    is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_type_key (config_type, config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态配置表';

-- ========================================
-- 初始化数据部分
-- ========================================

-- 插入领域数据
INSERT INTO `domain` (`domain_name`, `description`) VALUES
('软件工程', '软件开发相关职业'),
('硬件与嵌入式', '硬件设计与嵌入式开发'),
('科研与医疗', '科学研究与医疗健康'),
('运营与市场', '产品运营与市场营销'),
('金融与数据', '金融分析与数据科学'),
('教育与研究', '教育培训与学术研究'),
('设计与创意', 'UI/UX设计与创意领域'),
('法律与咨询', '法律咨询与商业顾问');

-- 插入职位类型数据
INSERT INTO `job_type` (`job_name`, `domain_id`) VALUES
-- 软件工程领域 (domain_id: 1)
('Java开发', 1),
('前端工程师', 1),
('大数据工程师', 1),
('AI算法工程师', 1),
('测试开发工程师', 1),
('后端开发工程师', 1),
('全栈开发工程师', 1),
('移动开发工程师', 1),
('DevOps工程师', 1),
('系统架构师', 1),
('技术总监', 1),
('数据科学家', 1),
('机器学习工程师', 1),
('网络安全工程师', 1),
('数据库工程师', 1),
('Python开发工程师', 1),
('C++开发工程师', 1),
('Go开发工程师', 1),
('Ruby开发工程师', 1),
('PHP开发工程师', 1),
('JavaScript开发工程师', 1),
('Android开发工程师', 1),
('iOS开发工程师', 1),
('软件测试工程师', 1),
('自动化测试工程师', 1),

-- 硬件与嵌入式领域 (domain_id: 2)
('芯片设计工程师', 2),
('FPGA工程师', 2),
('嵌入式开发工程师', 2),
('物联网工程师', 2),
('电子工程师', 2),
('硬件测试工程师', 2),
('PCB设计工程师', 2),
('数字电路设计', 2),
('模拟电路设计', 2),
('射频工程师', 2),
('固件工程师', 2),
('集成电路设计工程师', 2),
('ARM开发工程师', 2),
('单片机开发工程师', 2),
('传感器工程师', 2),
('硬件系统架构师', 2),
('电磁兼容工程师', 2),
('硬件可靠性工程师', 2),

-- 科研与医疗领域 (domain_id: 3)
('生物信息学研究员', 3),
('医学数据分析工程师', 3),
('生物技术研究员', 3),
('临床数据分析师', 3),
('医学影像工程师', 3),
('药物研发科学家', 3),
('基因测序分析师', 3),
('生物统计学专家', 3),
('医疗AI算法研究员', 3),
('医疗软件工程师', 3),
('生物医学工程师', 3),
('病理科医生', 3),
('医学研究员', 3),
('临床试验协调员', 3),
('医疗器械工程师', 3),
('流行病学家', 3),

-- 运营与市场领域 (domain_id: 4)
('产品经理', 4),
('品牌策划师', 4),
('市场运营专员', 4),
('内容运营专员', 4),
('用户运营专员', 4),
('活动运营专员', 4),
('数据分析运营', 4),
('市场推广专员', 4),
('渠道运营专员', 4),
('社交媒体运营', 4),
('产品运营经理', 4),
('市场总监', 4),
('用户增长专员', 4),
('SEO优化专员', 4),
('SEM推广专员', 4),
('内容营销专员', 4),
('品牌运营经理', 4),
('电商运营专员', 4),
('海外市场运营', 4),
('市场分析师', 4),

-- 金融与数据领域 (domain_id: 5)
('数据分析师', 5),
('量化研究员', 5),
('金融分析师', 5),
('风控分析师', 5),
('财务分析师', 5),
('投资顾问', 5),
('金融产品经理', 5),
('金融科技工程师', 5),
('数据仓库工程师', 5),
('商业智能分析师', 5),
('算法交易工程师', 5),
('投资银行分析师', 5),
('基金经理助理', 5),
('证券分析师', 5),
('数据挖掘工程师', 5),
('量化交易员', 5),
('精算师', 5),
('金融数据工程师', 5),

-- 教育与研究领域 (domain_id: 6)
('教育培训讲师', 6),
('教育产品经理', 6),
('教育内容开发', 6),
('学术研究员', 6),
('课程设计师', 6),
('教育技术专家', 6),
('学习体验设计师', 6),
('教育数据分析师', 6),
('高校教师', 6),
('教育咨询师', 6),
('幼儿教育教师', 6),
('中小学教师', 6),
('留学顾问', 6),
('教育出版编辑', 6),
('教育研究员', 6),
('教育评估专家', 6),
('在线教育产品经理', 6),
('教育软件开发', 6),

-- 设计与创意领域 (domain_id: 7)
('UI设计师', 7),
('交互设计师', 7),
('UX设计师', 7),
('视觉设计师', 7),
('产品设计师', 7),
('动效设计师', 7),
('插画师', 7),
('平面设计师', 7),
('三维设计师', 7),
('品牌设计师', 7),
('设计总监', 7),
('用户研究分析师', 7),
('原型设计师', 7),
('图形设计师', 7),
('UI/UX设计师', 7),
('网页设计师', 7),
('包装设计师', 7),
('游戏UI设计师', 7),
('用户体验研究员', 7),
('色彩设计师', 7),
('排版设计师', 7),
('数字营销设计师', 7),
('创意总监', 7),

-- 法律与咨询领域 (domain_id: 8)
('法律顾问', 8),
('商业顾问', 8),
('知识产权律师', 8),
('企业管理咨询', 8),
('财务顾问', 8),
('人力资源咨询', 8),
('战略咨询顾问', 8),
('税务顾问', 8),
('风险管理顾问', 8),
('法律咨询专员', 8),
('诉讼律师', 8),
('合同律师', 8),
('合规顾问', 8),
('并购顾问', 8),
('财务尽职调查', 8),
('组织架构咨询', 8),
('薪酬福利顾问', 8),
('管理培训师', 8);

-- 职位技能数据由系统在运行时通过AI动态生成和管理
-- 不需要预设初始数据，系统会根据职位类型自动生成对应的不同级别的技能要求

-- 插入评分体系数据
INSERT INTO `scoring_system` (`metric_name`, `weight`, `description`) VALUES
('专业技能', 0.4, '评估候选人的专业知识水平和技术能力'),
('逻辑思维', 0.3, '评估候选人的逻辑推理和问题分析能力'),
('沟通表达', 0.2, '评估候选人的表达清晰度和沟通能力'),
('创新潜力', 0.1, '评估候选人的创新思维和潜力');

-- 薪资信息数据由系统在运行时通过AI动态生成和管理
-- 不需要预设初始数据，系统会根据职位类型、城市、用户表现等信息自动生成个性化薪资评估
-- 系统会基于面试评分、技术能力、行业趋势等多维度因素，生成更准确的薪资建议
-- 示例提示模板：
/*
请基于以下信息为用户生成个性化薪资评估：
职位类型：{{jobType}}
所在城市：{{city}}
专业技能评分：{{techScore}}
逻辑思维评分：{{logicScore}}
沟通表达评分：{{clarityScore}}
创新潜力评分：{{innovationScore}}
总评分：{{totalScore}}
估算工作经验：{{experienceYears}}
行业平均薪资趋势：{{industryTrend}}
*/

-- 成长建议数据由系统在运行时通过AI动态生成和管理
-- 不需要预设初始数据，系统会根据职位类型、用户表现、技能短板等信息自动生成个性化成长建议
-- 系统会基于用户的面试表现、技能评分、行业趋势等因素，生成更有针对性的成长路径和建议
-- 示例提示模板：
/*
请基于以下信息为用户生成个性化的职业成长建议：
职位类型：{{jobType}}
所在领域：{{domain}}
专业技能评分：{{techScore}}
逻辑思维评分：{{logicScore}}
沟通表达评分：{{clarityScore}}
创新潜力评分：{{innovationScore}}
总评分：{{totalScore}}
技能短板：{{weakSkills}}
行业趋势：{{industryTrends}}

请生成：
1. 推荐学习的技能（3-5项）
2. 长期职业发展路径（3个阶段）
3. 短期建议（1-3个月，具体可行的行动建议）
4. 中期建议（3-6个月，能力提升方向）
5. 长期建议（6-12个月，职业发展规划）

建议语言风格要口语化、亲切自然，避免过于生硬的专业术语，让用户感觉像是一个经验丰富的前辈在给出建议。
*/

-- 面试问题数据由系统在运行时自动生成和管理
-- 不需要预设初始数据，系统会根据用户面试情况动态生成并存储问题

-- 插入动态配置数据
INSERT INTO `dynamic_config` (`config_type`, `config_key`, `config_value`, `description`, `is_active`)
VALUES
('persona', 'friendly', '{"name":"友善面试官","description":"以友好、鼓励的方式进行面试","prompt":"你是一位友善的面试官，问题需自然、亲和、不死板。从用法或项目实践切入，逐步深入原理/优化。"}', '友善风格的面试官配置', 1),
('persona', 'neutral', '{"name":"中性面试官","description":"保持客观、专业的面试风格","prompt":"你是一位专业的面试官，请保持中性、客观的语气。从用法或项目实践切入，逐步深入原理/优化。"}', '中性风格的面试官配置', 1),
('persona', 'challenging', '{"name":"挑战性面试官","description":"提出深入的技术问题，挑战候选人的极限","prompt":"你是一位挑战性的面试官，问题需深入、有技术深度。从原理或优化角度切入，要求候选人提供详细实现思路。"}', '挑战性风格的面试官配置', 1),
('template', 'default', '{"sessionSeconds":900,"minQuestions":3,"maxQuestions":8,"depthLevels":["basic","intermediate","advanced"]}', '默认面试模板配置', 1);

-- 插入系统配置数据
INSERT INTO `system_config` (`config_key`, `config_value`, `description`) VALUES
('system_name', 'AI简历助手', '系统名称'),
('system_version', '1.0.0', '系统版本号'),
('max_file_size', '10485760', '最大文件大小限制(10MB)');

-- 脚本执行完成
SELECT '数据库初始化完成' AS status;
