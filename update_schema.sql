-- AI简历系统数据库更新脚本
-- 基于实体类定义的最新结构与原结构的差异更新

-- 使用数据库
USE ai_resume;

-- 备份表前缀
SET @backup_prefix = 'backup_';
SET @current_date = DATE_FORMAT(NOW(), '%Y%m%d_%H%i%S');

-- ========================================
-- 1. 用户表 (user) 更新
-- ========================================

-- 先备份原表
SET @backup_user = CONCAT(@backup_prefix, 'user_', @current_date);
SET @sql = CONCAT('CREATE TABLE IF NOT EXISTS ', @backup_user, ' LIKE user');
PREPARE stmt FROM @sql;
EXECUTE stmt;
SET @sql = CONCAT('INSERT INTO ', @backup_user, ' SELECT * FROM user');
PREPARE stmt FROM @sql;
EXECUTE stmt;

-- 重命名原表为临时表
ALTER TABLE user RENAME TO user_old;

-- 创建新结构的用户表
CREATE TABLE IF NOT EXISTS user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    open_id VARCHAR(50) NOT NULL COMMENT '用户openId',
    name VARCHAR(50) COMMENT '姓名',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '电话',
    address VARCHAR(255) COMMENT '地址',
    birth_date VARCHAR(20) COMMENT '出生日期',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_open_id (open_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 迁移数据到新表
INSERT INTO user (id, open_id, name, email, phone, address, birth_date, create_time, update_time)
SELECT 
    id, 
    LEFT(open_id, 50) as open_id, 
    nickname as name,
    NULL as email,
    NULL as phone,
    CONCAT(IFNULL(country, ''), IFNULL(province, ''), IFNULL(city, '')) as address,
    NULL as birth_date,
    create_time,
    update_time
FROM user_old;

-- 删除原表
DROP TABLE user_old;

-- ========================================
-- 2. 产品表 (product) 更新
-- ========================================

-- 先备份原表
SET @backup_product = CONCAT(@backup_prefix, 'product_', @current_date);
SET @sql = CONCAT('CREATE TABLE IF NOT EXISTS ', @backup_product, ' LIKE product');
PREPARE stmt FROM @sql;
EXECUTE stmt;
SET @sql = CONCAT('INSERT INTO ', @backup_product, ' SELECT * FROM product');
PREPARE stmt FROM @sql;
EXECUTE stmt;

-- 重命名原表为临时表
ALTER TABLE product RENAME TO product_old;

-- 创建新结构的产品表
CREATE TABLE IF NOT EXISTS product (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL COMMENT '产品名称',
    price DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '产品价格',
    description VARCHAR(255) COMMENT '产品描述',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品表';

-- 迁移数据到新表
INSERT INTO product (name, price, description, create_time, update_time)
SELECT 
    LEFT(name, 50) as name, 
    price / 100.00 as price,  -- 分转元
    LEFT(description, 255) as description,
    create_time,
    update_time
FROM product_old;

-- 删除原表
DROP TABLE product_old;

-- ========================================
-- 3. 订单表 (ai_order) 更新
-- ========================================

-- 先备份原表
SET @backup_ai_order = CONCAT(@backup_prefix, 'ai_order_', @current_date);
SET @sql = CONCAT('CREATE TABLE IF NOT EXISTS ', @backup_ai_order, ' LIKE ai_order');
PREPARE stmt FROM @sql;
EXECUTE stmt;
SET @sql = CONCAT('INSERT INTO ', @backup_ai_order, ' SELECT * FROM ai_order');
PREPARE stmt FROM @sql;
EXECUTE stmt;

-- 重命名原表为临时表
ALTER TABLE ai_order RENAME TO ai_order_old;

-- 创建新结构的AI订单表
CREATE TABLE IF NOT EXISTS ai_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR(50) NOT NULL COMMENT '订单编号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_id INT NOT NULL COMMENT '产品ID',  -- 注意：这里改为INT类型
    amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '订单状态',
    pay_time DATETIME COMMENT '支付时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    UNIQUE KEY uk_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI订单表';

-- 迁移数据到新表（这里假设产品ID可以从字符串转为整数，实际情况可能需要更复杂的映射）
INSERT INTO ai_order (order_id, user_id, product_id, amount, status, pay_time, create_time, update_time)
SELECT 
    LEFT(order_no, 50) as order_id, 
    user_id, 
    1 as product_id,  -- 临时设置为1，实际应该建立映射关系
    amount / 100.00 as amount,  -- 分转元
    CASE 
        WHEN status = 0 THEN 'pending'
        WHEN status = 1 THEN 'completed'
        WHEN status = 2 THEN 'failed'
        WHEN status = 3 THEN 'canceled'
        ELSE 'pending'
    END as status,
    pay_time,
    create_time,
    update_time
FROM ai_order_old;

-- 删除原表
DROP TABLE ai_order_old;

-- ========================================
-- 4. 简历表 (resume) 更新
-- ========================================

-- 先备份原表
SET @backup_resume = CONCAT(@backup_prefix, 'resume_', @current_date);
SET @sql = CONCAT('CREATE TABLE IF NOT EXISTS ', @backup_resume, ' LIKE resume');
PREPARE stmt FROM @sql;
EXECUTE stmt;
SET @sql = CONCAT('INSERT INTO ', @backup_resume, ' SELECT * FROM resume');
PREPARE stmt FROM @sql;
EXECUTE stmt;

-- 重命名原表为临时表
ALTER TABLE resume RENAME TO resume_old;

-- 创建新结构的简历表
CREATE TABLE IF NOT EXISTS resume (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    job_type_id BIGINT COMMENT '职位类型ID',
    expected_salary VARCHAR(50) COMMENT '期望薪资',
    start_time VARCHAR(20) COMMENT '到岗时间',
    template_id VARCHAR(50) DEFAULT NULL COMMENT '模板ID',
    original_filename VARCHAR(255) NOT NULL COMMENT '原始文件名',
    content TEXT COMMENT '简历内容',
    parse_status VARCHAR(20) DEFAULT 'pending' COMMENT '解析状态',
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_job_type_id (job_type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='简历表';

-- 迁移数据到新表
INSERT INTO resume (id, user_id, job_type_id, original_filename, content, parse_status, is_deleted, create_time, update_time)
SELECT 
    id, 
    user_id, 
    job_type_id,
    original_filename,
    CONCAT(IFNULL(original_content, ''), IFNULL(optimized_content, '')) as content,
    CASE 
        WHEN status = 0 THEN 'pending'
        WHEN status = 2 THEN 'completed'
        WHEN status = 3 THEN 'failed'
        ELSE 'pending'
    END as parse_status,
    0 as is_deleted, -- 默认为未删除
    create_time,
    update_time
FROM resume_old;

-- 删除原表
DROP TABLE resume_old;

-- ========================================
-- 5. 创建新的简历相关表
-- ========================================

-- 创建简历教育经历表
CREATE TABLE IF NOT EXISTS resume_education (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resume_id BIGINT NOT NULL COMMENT '简历ID',
    school VARCHAR(100) COMMENT '学校名称',
    degree VARCHAR(50) COMMENT '学位',
    major VARCHAR(100) COMMENT '专业',
    start_date VARCHAR(20) COMMENT '开始日期',
    end_date VARCHAR(20) COMMENT '结束日期',
    description TEXT COMMENT '描述',
    order_index INT DEFAULT 0 COMMENT '排序索引',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_resume_id (resume_id),
    FOREIGN KEY (resume_id) REFERENCES resume(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='简历教育经历表';

-- 创建简历工作经历表
CREATE TABLE IF NOT EXISTS resume_work_experience (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resume_id BIGINT NOT NULL COMMENT '简历ID',
    company_name VARCHAR(100) COMMENT '公司名称',
    position_name VARCHAR(100) COMMENT '职位名称',
    start_date VARCHAR(20) COMMENT '开始日期',
    end_date VARCHAR(20) COMMENT '结束日期',
    description TEXT COMMENT '工作描述',
    order_index INT DEFAULT 0 COMMENT '排序索引',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_resume_id (resume_id),
    FOREIGN KEY (resume_id) REFERENCES resume(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='简历工作经历表';

-- 创建简历项目经历表
CREATE TABLE IF NOT EXISTS resume_project (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resume_id BIGINT NOT NULL COMMENT '简历ID',
    project_name VARCHAR(100) COMMENT '项目名称',
    start_date VARCHAR(20) COMMENT '开始日期',
    end_date VARCHAR(20) COMMENT '结束日期',
    description TEXT COMMENT '项目描述',
    order_index INT DEFAULT 0 COMMENT '排序索引',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_resume_id (resume_id),
    FOREIGN KEY (resume_id) REFERENCES resume(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='简历项目经历表';

-- 创建简历技能表
CREATE TABLE IF NOT EXISTS resume_skill (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resume_id BIGINT NOT NULL COMMENT '简历ID',
    name VARCHAR(50) COMMENT '技能名称',
    level INT DEFAULT 3 COMMENT '熟练度（1-5）',
    order_index INT DEFAULT 0 COMMENT '排序索引',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_resume_id (resume_id),
    FOREIGN KEY (resume_id) REFERENCES resume(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='简历技能表';

-- ========================================
-- 6. 领域表 (domain) 更新
-- ========================================

-- 先备份原表
SET @backup_domain = CONCAT(@backup_prefix, 'domain_', @current_date);
SET @sql = CONCAT('CREATE TABLE IF NOT EXISTS ', @backup_domain, ' LIKE domain');
PREPARE stmt FROM @sql;
EXECUTE stmt;
SET @sql = CONCAT('INSERT INTO ', @backup_domain, ' SELECT * FROM domain');
PREPARE stmt FROM @sql;
EXECUTE stmt;

-- 重命名原表为临时表
ALTER TABLE domain RENAME TO domain_old;

-- 创建新结构的领域表
CREATE TABLE IF NOT EXISTS domain (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL COMMENT '领域名称',
    code VARCHAR(20) NOT NULL COMMENT '领域代码',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='领域表';

-- 插入新的领域数据
INSERT INTO domain (name, code) VALUES
('运营与市场', 'OM'),
('金融与数据', 'FD'),
('计算机与互联网', 'CI'),
('设计与创意', 'DC'),
('教育与培训', 'ET'),
('其他行业', 'OT')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- 删除原表
DROP TABLE domain_old;

-- ========================================
-- 7. 职位类型表 (job_type) 更新
-- ========================================

-- 先备份原表
SET @backup_job_type = CONCAT(@backup_prefix, 'job_type_', @current_date);
SET @sql = CONCAT('CREATE TABLE IF NOT EXISTS ', @backup_job_type, ' LIKE job_type');
PREPARE stmt FROM @sql;
EXECUTE stmt;
SET @sql = CONCAT('INSERT INTO ', @backup_job_type, ' SELECT * FROM job_type');
PREPARE stmt FROM @sql;
EXECUTE stmt;

-- 重命名原表为临时表
ALTER TABLE job_type RENAME TO job_type_old;

-- 创建新结构的职位类型表
CREATE TABLE IF NOT EXISTS job_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    domain_id BIGINT NOT NULL COMMENT '所属领域ID',
    name VARCHAR(50) NOT NULL COMMENT '职位名称',
    code VARCHAR(20) NOT NULL COMMENT '职位代码',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_domain_id (domain_id),
    UNIQUE KEY uk_code (code),
    FOREIGN KEY (domain_id) REFERENCES domain(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='职位类型表';

-- 获取计算机与互联网领域ID并插入职位类型数据
SET @domain_ci_id = (SELECT id FROM domain WHERE code = 'CI');

INSERT INTO job_type (domain_id, name, code) VALUES
(@domain_ci_id, 'Java工程师', 'JAVE'),
(@domain_ci_id, 'Python工程师', 'PYTH'),
(@domain_ci_id, '前端工程师', 'FRNT'),
(@domain_ci_id, '后端工程师', 'BACK'),
(@domain_ci_id, '全栈工程师', 'FULL'),
(@domain_ci_id, '测试工程师', 'TEST'),
(@domain_ci_id, '算法工程师', 'ALGO'),
(@domain_ci_id, '数据分析师', 'DANA'),
(@domain_ci_id, 'DevOps工程师', 'DEVO'),
(@domain_ci_id, '运维工程师', 'OPS')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- 删除原表
DROP TABLE job_type_old;

-- ========================================
-- 8. 面试会话表 (interview_session) 更新
-- ========================================

-- 先备份原表
SET @backup_interview_session = CONCAT(@backup_prefix, 'interview_session_', @current_date);
SET @sql = CONCAT('CREATE TABLE IF NOT EXISTS ', @backup_interview_session, ' LIKE interview_session');
PREPARE stmt FROM @sql;
EXECUTE stmt;
SET @sql = CONCAT('INSERT INTO ', @backup_interview_session, ' SELECT * FROM interview_session');
PREPARE stmt FROM @sql;
EXECUTE stmt;

-- 重命名原表为临时表
ALTER TABLE interview_session RENAME TO interview_session_old;

-- 创建新结构的面试会话表
CREATE TABLE IF NOT EXISTS interview_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(50) NOT NULL COMMENT '会话ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    resume_id BIGINT NOT NULL COMMENT '简历ID',
    job_type_id BIGINT NOT NULL COMMENT '职位类型ID',
    interview_mode VARCHAR(20) NOT NULL DEFAULT 'time_based' COMMENT '面试模式',
    persona VARCHAR(50) DEFAULT 'professional' COMMENT '面试官风格',
    session_seconds INT DEFAULT 600 COMMENT '会话总时长（秒）',
    max_depth_per_point INT DEFAULT NULL COMMENT '每个知识点的最大深度',
    max_followups INT DEFAULT NULL COMMENT '最大追问次数',
    time_limit_secs INT DEFAULT NULL COMMENT '时间限制（秒）',
    actual_duration_secs INT DEFAULT NULL COMMENT '实际时长（秒）',
    ai_question_seed VARCHAR(100) DEFAULT NULL COMMENT 'AI问题种子',
    adaptive_level VARCHAR(20) DEFAULT 'medium' COMMENT '自适应级别',
    question_count INT DEFAULT 10 COMMENT '问题数量',
    status VARCHAR(20) DEFAULT 'active' COMMENT '会话状态',
    create_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_resume_id (resume_id),
    INDEX idx_job_type_id (job_type_id),
    UNIQUE KEY uk_session_id (session_id),
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (resume_id) REFERENCES resume(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试会话表';

-- 迁移数据到新表
INSERT INTO interview_session (session_id, user_id, resume_id, status, persona, session_seconds, create_at, update_at)
SELECT 
    LEFT(session_id, 50) as session_id, 
    user_id, 
    resume_id,
    status,
    persona,
    session_seconds,
    created_at as create_at,
    updated_at as update_at
FROM interview_session_old;

-- 删除原表
DROP TABLE interview_session_old;

-- ========================================
-- 9. 面试日志表 (interview_log) 更新
-- ========================================

-- 先备份原表
SET @backup_interview_log = CONCAT(@backup_prefix, 'interview_log_', @current_date);
SET @sql = CONCAT('CREATE TABLE IF NOT EXISTS ', @backup_interview_log, ' LIKE interview_log');
PREPARE stmt FROM @sql;
EXECUTE stmt;
SET @sql = CONCAT('INSERT INTO ', @backup_interview_log, ' SELECT * FROM interview_log');
PREPARE stmt FROM @sql;
EXECUTE stmt;

-- 重命名原表为临时表
ALTER TABLE interview_log RENAME TO interview_log_old;

-- 创建新结构的面试日志表
CREATE TABLE IF NOT EXISTS interview_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id VARCHAR(50) NOT NULL COMMENT '问题ID',
    session_id VARCHAR(50) NOT NULL COMMENT '会话ID',
    question_order INT DEFAULT 1 COMMENT '问题顺序',
    question_content TEXT NOT NULL COMMENT '问题内容',
    user_answer TEXT COMMENT '用户回答',
    answer_duration INT DEFAULT 0 COMMENT '回答用时（秒）',
    related_tech_items VARCHAR(255) DEFAULT NULL COMMENT '相关技术点',
    related_project_points VARCHAR(255) DEFAULT NULL COMMENT '相关项目点',
    stop_reason VARCHAR(50) DEFAULT NULL COMMENT '停止原因',
    persona VARCHAR(50) DEFAULT NULL COMMENT '面试官风格',
    ai_feedback_json LONGTEXT COMMENT 'AI原始评分和分析结果',
    create_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_session_id (session_id),
    INDEX idx_question_id (question_id),
    FOREIGN KEY (session_id) REFERENCES interview_session(session_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试日志表';

-- 迁移数据到新表
INSERT INTO interview_log (question_id, session_id, question_order, question_content, user_answer, answer_duration, persona, ai_feedback_json, create_at, update_at)
SELECT 
    LEFT(question_id, 50) as question_id, 
    LEFT(session_id, 50) as session_id, 
    round_number as question_order,
    question_text as question_content,
    user_answer_text as user_answer,
    answer_duration,
    persona,
    ai_feedback_json,
    created_at as create_at,
    updated_at as update_at
FROM interview_log_old;

-- 删除原表
DROP TABLE interview_log_old;

-- ========================================
-- 10. 面试问题表 (interview_question) 更新
-- ========================================

-- 先备份原表
SET @backup_interview_question = CONCAT(@backup_prefix, 'interview_question_', @current_date);
SET @sql = CONCAT('CREATE TABLE IF NOT EXISTS ', @backup_interview_question, ' LIKE interview_question');
PREPARE stmt FROM @sql;
EXECUTE stmt;
SET @sql = CONCAT('INSERT INTO ', @backup_interview_question, ' SELECT * FROM interview_question');
PREPARE stmt FROM @sql;
EXECUTE stmt;

-- 重命名原表为临时表
ALTER TABLE interview_question RENAME TO interview_question_old;

-- 创建新结构的面试问题表
CREATE TABLE IF NOT EXISTS interview_question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id VARCHAR(50) NOT NULL COMMENT '问题ID',
    job_type_id BIGINT NOT NULL COMMENT '职位类型ID',
    question_content TEXT NOT NULL COMMENT '问题内容',
    question_type VARCHAR(20) DEFAULT 'tech' COMMENT '问题类型',
    difficulty VARCHAR(20) DEFAULT 'medium' COMMENT '难度级别',
    knowledge_point VARCHAR(100) DEFAULT NULL COMMENT '知识点',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_job_type_id (job_type_id),
    UNIQUE KEY uk_question_id (question_id),
    FOREIGN KEY (job_type_id) REFERENCES job_type(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试问题表';

-- 迁移数据到新表（需要job_type_id映射）
INSERT INTO interview_question (question_id, job_type_id, question_content, difficulty)
SELECT 
    CONCAT('Q', id) as question_id, 
    job_type_id,
    question_text as question_content,
    CASE 
        WHEN depth_level = 'usage' THEN 'easy'
        WHEN depth_level = 'implementation' THEN 'medium'
        WHEN depth_level = 'principle' THEN 'hard'
        WHEN depth_level = 'optimization' THEN 'expert'
        ELSE 'medium'
    END as difficulty
FROM interview_question_old;

-- 删除原表
DROP TABLE interview_question_old;

-- ========================================
-- 11. 职位技能表 (job_skill) 更新
-- ========================================

-- 先备份原表
SET @backup_job_skill = CONCAT(@backup_prefix, 'job_skill_', @current_date);
SET @sql = CONCAT('CREATE TABLE IF NOT EXISTS ', @backup_job_skill, ' LIKE job_skill');
PREPARE stmt FROM @sql;
EXECUTE stmt;
SET @sql = CONCAT('INSERT INTO ', @backup_job_skill, ' SELECT * FROM job_skill');
PREPARE stmt FROM @sql;
EXECUTE stmt;

-- 重命名原表为临时表
ALTER TABLE job_skill RENAME TO job_skill_old;

-- 创建新结构的职位技能表
CREATE TABLE IF NOT EXISTS job_skill (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_type_id BIGINT NOT NULL COMMENT '职位类型ID',
    skill_name VARCHAR(50) NOT NULL COMMENT '技能名称',
    skill_level VARCHAR(20) DEFAULT 'medium' COMMENT '技能要求级别',
    priority INT DEFAULT 5 COMMENT '优先级',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_job_type_id (job_type_id),
    FOREIGN KEY (job_type_id) REFERENCES job_type(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='职位技能表';

-- 迁移数据到新表
INSERT INTO job_skill (job_type_id, skill_name, skill_level, priority)
SELECT 
    job_type_id, 
    LEFT(skill_name, 50) as skill_name,
    skill_level,
    importance as priority
FROM job_skill_old;

-- 删除原表
DROP TABLE job_skill_old;

-- ========================================
-- 12. 评分体系表 (scoring_system) 更新
-- ========================================

-- 先备份原表
SET @backup_scoring_system = CONCAT(@backup_prefix, 'scoring_system_', @current_date);
SET @sql = CONCAT('CREATE TABLE IF NOT EXISTS ', @backup_scoring_system, ' LIKE scoring_system');
PREPARE stmt FROM @sql;
EXECUTE stmt;
SET @sql = CONCAT('INSERT INTO ', @backup_scoring_system, ' SELECT * FROM scoring_system');
PREPARE stmt FROM @sql;
EXECUTE stmt;

-- 重命名原表为临时表
ALTER TABLE scoring_system RENAME TO scoring_system_old;

-- 创建新结构的评分体系表
CREATE TABLE IF NOT EXISTS scoring_system (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    system_name VARCHAR(50) NOT NULL COMMENT '评分体系名称',
    system_code VARCHAR(20) NOT NULL COMMENT '评分体系代码',
    score_max INT NOT NULL DEFAULT 100 COMMENT '满分',
    score_pass INT NOT NULL DEFAULT 60 COMMENT '及格分数',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_system_code (system_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评分体系表';

-- 插入新的评分体系数据
INSERT INTO scoring_system (system_name, system_code, score_max, score_pass) VALUES
('技术面试评分', 'TECH_INTERVIEW', 100, 60),
('项目经验评分', 'PROJECT_EXP', 100, 60),
('沟通能力评分', 'COMM_SKILL', 100, 60)
ON DUPLICATE KEY UPDATE score_max=VALUES(score_max), score_pass=VALUES(score_pass);

-- 删除原表
DROP TABLE scoring_system_old;

-- ========================================
-- 13. 薪资信息表 (salary_info) 更新
-- ========================================

-- 先备份原表
SET @backup_salary_info = CONCAT(@backup_prefix, 'salary_info_', @current_date);
SET @sql = CONCAT('CREATE TABLE IF NOT EXISTS ', @backup_salary_info, ' LIKE salary_info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
SET @sql = CONCAT('INSERT INTO ', @backup_salary_info, ' SELECT * FROM salary_info');
PREPARE stmt FROM @sql;
EXECUTE stmt;

-- 重命名原表为临时表
ALTER TABLE salary_info RENAME TO salary_info_old;

-- 创建新结构的薪资信息表
CREATE TABLE IF NOT EXISTS salary_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_type_id BIGINT NOT NULL COMMENT '职位类型ID',
    experience_years VARCHAR(20) NOT NULL COMMENT '工作年限',
    salary_min INT DEFAULT 0 COMMENT '最低薪资',
    salary_max INT DEFAULT 0 COMMENT '最高薪资',
    salary_avg INT DEFAULT 0 COMMENT '平均薪资',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_job_type_id (job_type_id),
    FOREIGN KEY (job_type_id) REFERENCES job_type(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='薪资信息表';

-- 删除原表
DROP TABLE salary_info_old;

-- ========================================
-- 14. 成长建议表 (growth_advice) 更新
-- ========================================

-- 先备份原表
SET @backup_growth_advice = CONCAT(@backup_prefix, 'growth_advice_', @current_date);
SET @sql = CONCAT('CREATE TABLE IF NOT EXISTS ', @backup_growth_advice, ' LIKE growth_advice');
PREPARE stmt FROM @sql;
EXECUTE stmt;
SET @sql = CONCAT('INSERT INTO ', @backup_growth_advice, ' SELECT * FROM growth_advice');
PREPARE stmt FROM @sql;
EXECUTE stmt;

-- 重命名原表为临时表
ALTER TABLE growth_advice RENAME TO growth_advice_old;

-- 创建新结构的成长建议表
CREATE TABLE IF NOT EXISTS growth_advice (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    advice_type VARCHAR(50) NOT NULL COMMENT '建议类型',
    content TEXT NOT NULL COMMENT '建议内容',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成长建议表';

-- 删除原表
DROP TABLE growth_advice_old;

-- ========================================
-- 15. 系统配置表 (system_config) 更新
-- ========================================

-- 先备份原表
SET @backup_system_config = CONCAT(@backup_prefix, 'system_config_', @current_date);
SET @sql = CONCAT('CREATE TABLE IF NOT EXISTS ', @backup_system_config, ' LIKE system_config');
PREPARE stmt FROM @sql;
EXECUTE stmt;
SET @sql = CONCAT('INSERT INTO ', @backup_system_config, ' SELECT * FROM system_config');
PREPARE stmt FROM @sql;
EXECUTE stmt;

-- 重命名原表为临时表
ALTER TABLE system_config RENAME TO system_config_old;

-- 创建新结构的系统配置表
CREATE TABLE IF NOT EXISTS system_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(50) NOT NULL COMMENT '配置键',
    config_value TEXT COMMENT '配置值',
    config_desc VARCHAR(255) COMMENT '配置描述',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 插入新的系统配置数据
INSERT INTO system_config (config_key, config_value, config_desc) VALUES
('system_version', '1.0.0', '系统版本号'),
('maintenance_mode', 'false', '系统维护模式'),
('max_resume_size_mb', '10', '最大简历文件大小(MB)'),
('support_file_types', '.pdf,.doc,.docx', '支持的文件类型'),
('max_interview_duration_secs', '3600', '最大面试时长(秒)'),
('min_interview_duration_secs', '600', '最小面试时长(秒)')
ON DUPLICATE KEY UPDATE config_value=VALUES(config_value), config_desc=VALUES(config_desc);

-- 保留原有可能的自定义配置
INSERT INTO system_config (config_key, config_value, config_desc)
SELECT 
    LEFT(config_key, 50) as config_key,
    config_value,
    description as config_desc
FROM system_config_old
WHERE config_key NOT IN (SELECT config_key FROM system_config);

-- 删除原表
DROP TABLE system_config_old;

-- ========================================
-- 16. AI跟踪日志表 (ai_trace_log) 更新
-- ========================================

-- 先备份原表
SET @backup_ai_trace_log = CONCAT(@backup_prefix, 'ai_trace_log_', @current_date);
SET @sql = CONCAT('CREATE TABLE IF NOT EXISTS ', @backup_ai_trace_log, ' LIKE ai_trace_log');
PREPARE stmt FROM @sql;
EXECUTE stmt;
SET @sql = CONCAT('INSERT INTO ', @backup_ai_trace_log, ' SELECT * FROM ai_trace_log');
PREPARE stmt FROM @sql;
EXECUTE stmt;

-- 重命名原表为临时表
ALTER TABLE ai_trace_log RENAME TO ai_trace_log_old;

-- 创建新结构的AI跟踪日志表
CREATE TABLE IF NOT EXISTS ai_trace_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT COMMENT '用户ID',
    function_name VARCHAR(100) NOT NULL COMMENT '函数名称',
    params TEXT COMMENT '请求参数',
    response TEXT COMMENT '响应结果',
    cost_time DOUBLE DEFAULT 0 COMMENT '耗时（毫秒）',
    status VARCHAR(20) DEFAULT 'success' COMMENT '执行状态',
    error_message TEXT COMMENT '错误信息',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_function_name (function_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI跟踪日志表';

-- 迁移数据到新表
INSERT INTO ai_trace_log (function_name, params, response, cost_time, status, create_time)
SELECT 
    action_type as function_name, 
    prompt_input as params,
    ai_response as response,
    latency_ms as cost_time,
    'success' as status,
    created_at as create_time
FROM ai_trace_log_old;

-- 删除原表
DROP TABLE ai_trace_log_old;

-- ========================================
-- 17. 动态配置表 (dynamic_config) 更新
-- ========================================

-- 先备份原表
SET @backup_dynamic_config = CONCAT(@backup_prefix, 'dynamic_config_', @current_date);
SET @sql = CONCAT('CREATE TABLE IF NOT EXISTS ', @backup_dynamic_config, ' LIKE dynamic_config');
PREPARE stmt FROM @sql;
EXECUTE stmt;
SET @sql = CONCAT('INSERT INTO ', @backup_dynamic_config, ' SELECT * FROM dynamic_config');
PREPARE stmt FROM @sql;
EXECUTE stmt;

-- 重命名原表为临时表
ALTER TABLE dynamic_config RENAME TO dynamic_config_old;

-- 创建新结构的动态配置表
CREATE TABLE IF NOT EXISTS dynamic_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_name VARCHAR(50) NOT NULL COMMENT '配置名称',
    config_type VARCHAR(20) NOT NULL COMMENT '配置类型',
    config_value TEXT COMMENT '配置值',
    config_desc VARCHAR(255) COMMENT '配置描述',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_config_name_type (config_name, config_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态配置表';

-- 插入新的动态配置数据
INSERT INTO dynamic_config (config_name, config_type, config_value, config_desc) VALUES
-- 系统设置
('api_timeout', 'system', '30000', 'API超时时间(毫秒)'),
('page_size', 'system', '20', '默认分页大小'),
('token_expire_hours', 'system', '24', 'Token过期时间(小时)'),

-- 面试官风格配置
('professional', 'persona', '专业严谨型：注重技术细节，问题深入', '专业严谨型面试官'),
('friendly', 'persona', '友好引导型：循循善诱，注重候选人表达', '友好引导型面试官'),
('challenging', 'persona', '挑战创新型：提出开放式问题，考察思维能力', '挑战创新型面试官'),
('balanced', 'persona', '平衡综合型：全面考察技术与非技术能力', '平衡综合型面试官'),

-- 深度级别配置
('beginner', 'depth', '初级：基础知识为主，简单应用场景', '初级深度'),
('intermediate', 'depth', '中级：基础+进阶知识，中等复杂度应用', '中级深度'),
('advanced', 'depth', '高级：深入原理，复杂应用场景，设计能力', '高级深度'),
('expert', 'depth', '专家级：系统设计，架构能力，前沿技术', '专家级深度'),

-- 面试模式配置
('time_based', 'interview_mode', '时间模式：按固定时长进行面试', '时间模式'),
('question_based', 'interview_mode', '问题模式：按固定问题数量进行面试', '问题模式'),
('comprehensive', 'interview_mode', '综合模式：结合时间和问题数量', '综合模式')
ON DUPLICATE KEY UPDATE config_value=VALUES(config_value), config_desc=VALUES(config_desc);

-- 删除原表
DROP TABLE dynamic_config_old;

-- ========================================
-- 18. 创建必要的索引
-- ========================================
CREATE INDEX idx_interview_session_status ON interview_session (status);
CREATE INDEX idx_resume_parse_status ON resume (parse_status);
CREATE INDEX idx_resume_is_deleted ON resume (is_deleted);

-- ========================================
-- 更新完成
-- ========================================
SELECT '数据库更新完成' AS status;

-- 提示：备份表以 backup_ 开头，包含时间戳，可以在确认更新成功后删除