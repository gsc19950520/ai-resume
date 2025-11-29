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
ALTER TABLE interview_log ADD COLUMN depth_level VARCHAR(20) COMMENT '问题深度：usage/implementation/principle/optimization';

-- 2025-11-25 优化AI问题生成逻辑：后续问题不再发送完整简历，基于上下文生成
-- 修改文件：InterviewServiceImpl.java
-- 修改内容：
-- 1. 只有第一次生成问题时才发送完整简历给DeepSeek API
-- 2. 后续问题基于上一题的问答上下文生成，不重复发送完整简历
-- 3. 减少API请求数据量，提高响应速度

-- 2025-11-26 实现流式问题生成功能
-- 修改文件：InterviewServiceImpl.java
-- 修改内容：
-- 1. 实现generateNewQuestionWithAIStream方法，支持流式生成问题
-- 2. 修改getFirstQuestionStream方法，使用流式生成问题
-- 3. 修改submitAnswerStream方法，使用流式生成下一个问题
-- 修改文件：interview.js（前端）
-- 修改内容：
-- 1. 导入requestStream和postStream方法
-- 2. 实现fetchFirstQuestionStream方法，处理流式第一个问题
-- 3. 修改onLoad方法，使用流式请求获取第一个问题
-- 4. 修改submitAnswer方法，使用流式请求提交回答并处理流式响应

-- 2025-11-27 新增InterviewLog表depth_level字段，用于存储问题深度级别
ALTER TABLE interview_log ADD COLUMN depth_level VARCHAR(20) COMMENT '问题深度：usage/implementation/principle/optimization';

-- 2025-11-28 优化AI流式响应处理：只返回问题内容，元数据异步保存
-- 修改文件：AiServiceUtils.java
-- 修改内容：
-- 1. 修改callDeepSeekApiStream方法，移除元数据SSE事件发送，改为异步保存
-- 2. 新增saveMetadataAsync方法，用于异步保存元数据到数据库
-- 修改文件：InterviewServiceImpl.java
-- 修改内容：
-- 1. 更新getFirstQuestionStream和generateNextQuestionStream方法中对callDeepSeekApiStream的调用，添加sessionId参数

-- 2025-11-29 优化简历技术项和项目点提取逻辑：添加缓存机制
-- 修改文件：Resume.java
-- 修改内容：
-- 1. 新增tech_items字段，存储技术项JSON
-- 2. 新增project_points字段，存储项目点JSON
-- 3. 新增last_extracted_time字段，存储最后提取时间
ALTER TABLE resume ADD COLUMN tech_items TEXT COMMENT '技术项，JSON格式存储';
ALTER TABLE resume ADD COLUMN project_points TEXT COMMENT '项目点，JSON格式存储';
ALTER TABLE resume ADD COLUMN last_extracted_time DATETIME COMMENT '最后提取时间';
