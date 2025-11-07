-- 动态面试系统数据库表结构

-- 1. 面试会话表
CREATE TABLE IF NOT EXISTS interview_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL UNIQUE COMMENT '会话ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    resume_id BIGINT NOT NULL COMMENT '简历ID',
    position VARCHAR(100) COMMENT '职位',
    industry VARCHAR(100) COMMENT '行业',
    persona VARCHAR(50) DEFAULT 'friendly' COMMENT '面试官风格：friendly/neutral/challenging',
    session_seconds INT DEFAULT 900 COMMENT '会话时长（秒）',
    session_time_remaining INT DEFAULT 900 COMMENT '剩余时间（秒）',
    status VARCHAR(20) DEFAULT 'STARTED' COMMENT '状态：STARTED/FINISHED',
    total_score DOUBLE COMMENT '总分',
    consecutive_no_match_count INT DEFAULT 0 COMMENT '连续不匹配次数',
    stop_reason VARCHAR(100) COMMENT '停止原因',
    tech_items TEXT COMMENT '技术项JSON',
    project_points TEXT COMMENT '项目点JSON',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '面试会话表';

-- 2. 面试答案表（替代原有的interview_logs）
CREATE TABLE IF NOT EXISTS interview_answers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    answer_id VARCHAR(64) NOT NULL UNIQUE COMMENT '答案ID',
    session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    round_number INT NOT NULL COMMENT '回合数',
    question_text TEXT NOT NULL COMMENT '问题文本',
    user_answer_text TEXT COMMENT '用户回答文本',
    answer_duration INT COMMENT '回答时长（秒）',
    expected_key_points TEXT COMMENT '期望关键点JSON',
    depth_level VARCHAR(20) COMMENT '深度级别',
    tech_score DOUBLE COMMENT '技术评分',
    logic_score DOUBLE COMMENT '逻辑评分',
    clarity_score DOUBLE COMMENT '清晰度评分',
    depth_score DOUBLE COMMENT '深度评分',
    feedback TEXT COMMENT '反馈内容',
    related_tech_items TEXT COMMENT '相关技术项JSON',
    related_project_points TEXT COMMENT '相关项目点JSON',
    timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES interview_sessions(session_id) ON DELETE CASCADE
) COMMENT '面试答案表';

-- 3. 动态配置表
CREATE TABLE IF NOT EXISTS dynamic_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(50) NOT NULL UNIQUE COMMENT '配置键',
    config_value TEXT NOT NULL COMMENT '配置值',
    description VARCHAR(200) COMMENT '配置描述',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '动态配置表';

-- 插入默认配置
INSERT INTO dynamic_config (config_key, config_value, description) VALUES
('default_session_seconds', '900', '默认会话时长（秒）'),
('persona_config', '{"friendly":"友好型面试官，问题温和自然","neutral":"中性面试官，问题客观直接","challenging":"挑战性面试官，问题深入细节"}', '面试官风格配置'),
('stop_conditions', '{"min_session_time":60,"max_consecutive_no_match":2}', '停止条件配置');

-- 创建索引
CREATE INDEX idx_interview_sessions_user_id ON interview_sessions(user_id);
CREATE INDEX idx_interview_sessions_status ON interview_sessions(status);
CREATE INDEX idx_interview_answers_session_id ON interview_answers(session_id);
CREATE INDEX idx_interview_answers_round_number ON interview_answers(round_number);
