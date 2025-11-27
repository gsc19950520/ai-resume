-- 2025-11-21 新增ResumeProject表字段
ALTER TABLE resume_project ADD COLUMN role VARCHAR(255) COMMENT '项目角色';
ALTER TABLE resume_project ADD COLUMN tech_stack TEXT COMMENT '技术栈';

-- 2024-01-19 新增InterviewQuestions类属性
ALTER TABLE resume_analysis ADD COLUMN questions TEXT COMMENT '面试问题列表';
ALTER TABLE resume_analysis ADD COLUMN analysis_type VARCHAR(50) COMMENT '分析类型';

-- 2024-05-29 添加期望关键点字段用于评分
ALTER TABLE interview_log ADD COLUMN expected_key_points text COMMENT '期望的关键点，JSON格式存储';

-- 2025-11-22 新增InterviewSession表字段
ALTER TABLE interview_session ADD COLUMN persona VARCHAR(50) DEFAULT 'friendly' COMMENT '面试官风格：friendly, neutral, challenging';
ALTER TABLE interview_session ADD COLUMN session_seconds INT DEFAULT 900 COMMENT '会话总时长（秒）';
ALTER TABLE interview_session ADD COLUMN session_time_remaining INT DEFAULT 900 COMMENT '剩余时间（秒）';
ALTER TABLE interview_session ADD COLUMN consecutive_no_match_count INT DEFAULT 0 COMMENT '连续不匹配次数';
ALTER TABLE interview_session ADD COLUMN stop_reason VARCHAR(100) COMMENT '停止原因';
ALTER TABLE interview_session ADD COLUMN start_time DATETIME COMMENT '开始时间';
ALTER TABLE interview_session ADD COLUMN end_time DATETIME COMMENT '结束时间';
ALTER TABLE interview_session ADD COLUMN tech_items TEXT COMMENT '技术项';
ALTER TABLE interview_session ADD COLUMN project_points TEXT COMMENT '项目点';
ALTER TABLE interview_session ADD COLUMN interview_state TEXT COMMENT '面试状态';
ALTER TABLE interview_session ADD COLUMN interview_mode VARCHAR(20) DEFAULT 'time_based' COMMENT '面试模式：time_based/question_based';
ALTER TABLE interview_session ADD COLUMN max_depth_per_point INT COMMENT '每个知识点最大深度';
ALTER TABLE interview_session ADD COLUMN max_followups INT COMMENT '最大追问次数';
ALTER TABLE interview_session ADD COLUMN time_limit_secs INT COMMENT '时间限制（秒）';
ALTER TABLE interview_session ADD COLUMN actual_duration_secs INT COMMENT '实际时长（秒）';
ALTER TABLE interview_session ADD COLUMN ai_question_seed INT COMMENT 'AI问题种子';
ALTER TABLE interview_session ADD COLUMN adaptive_level VARCHAR(20) DEFAULT 'auto' COMMENT '题目深度模式：auto/fixed';
ALTER TABLE interview_session ADD COLUMN question_count INT DEFAULT 0 COMMENT '问题数量';

-- 2025-11-22 新增InterviewQuestion表字段
ALTER TABLE interview_question ADD COLUMN expected_key_points TEXT COMMENT '期望的关键点，JSON格式存储';
ALTER TABLE interview_question ADD COLUMN job_type_id BIGINT COMMENT '职位类型ID';
ALTER TABLE interview_question ADD COLUMN skill_tag VARCHAR(100) COMMENT '技能标签';
ALTER TABLE interview_question ADD COLUMN depth_level VARCHAR(20) COMMENT '问题深度：usage/implementation/principle/optimization';
ALTER TABLE interview_question ADD COLUMN persona VARCHAR(50) COMMENT '语言风格标签：friendly/formal/challenging';
ALTER TABLE interview_question ADD COLUMN ai_generated BOOLEAN DEFAULT TRUE COMMENT '是否AI生成';
ALTER TABLE interview_question ADD COLUMN usage_count INT DEFAULT 0 COMMENT '使用次数';
ALTER TABLE interview_question ADD COLUMN avg_score FLOAT DEFAULT 0 COMMENT '平均得分';
ALTER TABLE interview_question ADD COLUMN similarity_hash VARCHAR(64) COMMENT '相似度哈希值';

-- 2024-01-01 添加问题库相关功能
-- 创建索引以提高问题查找效率
CREATE INDEX idx_interview_question_skill_depth ON interview_question(skill_tag, depth_level);
CREATE INDEX idx_interview_question_usage_count ON interview_question(usage_count);
CREATE INDEX idx_interview_question_similarity_hash ON interview_question(similarity_hash);

-- 2025-11-22 新增InterviewLog表字段
ALTER TABLE interview_log ADD COLUMN answer_duration INT COMMENT '回答时长（秒）';
ALTER TABLE interview_log ADD COLUMN related_tech_items TEXT COMMENT '相关技术项';
ALTER TABLE interview_log ADD COLUMN related_project_points TEXT COMMENT '相关项目点';
ALTER TABLE interview_log ADD COLUMN stop_reason VARCHAR(100) COMMENT '停止原因';
ALTER TABLE interview_log ADD COLUMN persona VARCHAR(50) COMMENT '当前题目使用的面试官语气风格';
ALTER TABLE interview_log ADD COLUMN ai_feedback_json LONGTEXT COMMENT 'AI原始评分和分析结果';
