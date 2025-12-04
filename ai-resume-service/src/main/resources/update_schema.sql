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

-- 2025-11-30 修复获取第一个问题失败：question_text不能为空
-- 修改文件：InterviewLog.java
-- 修改内容：
-- 1. 将question_text字段的nullable属性改为true，允许为空
-- 修改数据库：
-- 1. 将interview_log表的question_text字段改为允许为空
ALTER TABLE interview_log MODIFY COLUMN question_text TEXT NULL;

-- 2025-11-30 重构面试问题生成逻辑，合并冗余方法
-- 修改文件：InterviewServiceImpl.java
-- 修改内容：
-- 1. 重构generateNextQuestion方法，直接构建prompt调用AI服务
-- 2. 删除冗余的generateNewQuestionWithAI方法
-- 3. 删除冗余的generateNewQuestionWithAIStream方法
-- 4. 优化代码结构，减少重复代码
-- 5. 提高方法调用效率

-- 2025-11-30 完善面试报告功能
-- 修改内容：
-- 1. 创建新的VO类：SalaryInfoVO、SessionLogVO、GrowthAdviceVO
-- 2. 修改InterviewReportVO，添加更多字段以支持完整的面试报告
-- 3. 修改InterviewController的finishInterview方法，返回完整的面试报告数据
-- 4. 修改InterviewServiceImpl的finishInterview方法，设置totalScore和createdAt字段
-- 5. 前端调整：修改interview.js中finishInterview方法的跳转路径为正确的report页面

-- 2025-01-17 添加AI面试报告功能
-- 修改表结构，添加report_id字段
ALTER TABLE interview_log ADD COLUMN report_id BIGINT NULL COMMENT '关联的报告ID';

-- 创建面试报告表
CREATE TABLE interview_report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '报告ID',
    session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    total_score DOUBLE NOT NULL COMMENT '总分',
    overall_feedback TEXT NULL COMMENT '总体反馈',
    strengths TEXT NULL COMMENT '优势',
    improvements TEXT NULL COMMENT '改进点',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_session_id (session_id),
    FOREIGN KEY (session_id) REFERENCES interview_session(session_id) ON DELETE CASCADE
) COMMENT '面试报告表';

-- 2025-01-18 完善面试报告功能
-- 1. 前端与后端交互函数使用云托管请求方式
-- 2. 调整InterviewReportVO和InterviewReportDTO，添加strengths、improvements、overallFeedback等字段
-- 3. 修改InterviewServiceImpl.finishInterview方法，使用DeepSeek API全面分析面试会话
-- 4. 修改InterviewController.finishInterview方法，确保返回的VO对象包含前端所需的所有字段
-- 5. 更新前端report页面，增加面试反馈、推荐技能、职业发展路径等新模块

-- 将评分字段从interview_session表迁移到interview_report表
-- 2025-03-21 10:15:30
ALTER TABLE interview_report ADD COLUMN tech_score DOUBLE DEFAULT 0.0;
ALTER TABLE interview_report ADD COLUMN logic_score DOUBLE DEFAULT 0.0;
ALTER TABLE interview_report ADD COLUMN clarity_score DOUBLE DEFAULT 0.0;
ALTER TABLE interview_report ADD COLUMN depth_score DOUBLE DEFAULT 0.0;

-- 从interview_session表移除评分字段
ALTER TABLE interview_session DROP COLUMN total_score;
ALTER TABLE interview_session DROP COLUMN tech_score;
ALTER TABLE interview_session DROP COLUMN logic_score;
ALTER TABLE interview_session DROP COLUMN clarity_score;
ALTER TABLE interview_session DROP COLUMN depth_score;

-- 2025-12-02 优化AI面试系统
-- 1. 确认前端所有API请求均使用云托管callContainer方式
-- 2. 修复实体类与数据库表字段的一致性
-- 3. 确保所有实体类的变更都已同步到前后端代码
-- 4. 优化SQL更新脚本，移除重复的ALTER TABLE语句
-- 5. 确认所有API接口都支持云托管请求方式

-- 2025-12-03 修复面试页面下拉历史面试区域无数据问题
-- 1. 创建InterviewHistoryItemVO类，用于将InterviewLog实体转换为前端需要的格式
-- 2. 修改InterviewService接口中的getInterviewHistory方法，将返回类型从List<InterviewLog>改为List<InterviewHistoryItemVO>
-- 3. 修改InterviewServiceImpl中的getInterviewHistory方法，实现了InterviewLog到InterviewHistoryItemVO的转换
-- 4. 修改interview.js中的fetchInterviewHistory方法，添加了数据格式化逻辑
-- 5. 确保前端使用了正确的云托管请求方式

-- 2025-12-03 修复回答问题时创建新记录而非更新现有记录的问题
-- 1. 修改InterviewServiceImpl.submitAnswerStream方法
-- 2. 改为查找包含问题文本的最新记录，而不是简单使用最后一条记录
-- 3. 确保用户回答更新到正确的问题记录中
-- 4. 修改AiServiceUtils.saveMetadataAsync和saveQuestionAsync方法
-- 5. 确保异步保存元数据和问题文本时能找到正确的记录

-- 2025-12-04 修复了前端面试历史列表中重复显示当前问题的bug
-- 1. 在fetchInterviewHistory方法中过滤掉与当前问题相同的问题记录

-- 2025-12-04
-- 修复了面试剩余时间(session_time_remaining)未正确更新的问题：
-- 1. 确认后端submitAnswerStream方法中已正确计算并保存剩余时间
-- 2. 前端在提交回答后重新获取会话详情，确保剩余时间实时更新

-- 2025-12-04 17:00:00 - 删除interview_session表中未使用的字段
ALTER TABLE `interview_session`
DROP COLUMN `ai_estimated_years`,
DROP COLUMN `ai_salary_range`,
DROP COLUMN `confidence`,
DROP COLUMN `report_url`,
DROP COLUMN `consecutive_no_match_count`,
DROP COLUMN `interview_mode`,
DROP COLUMN `max_depth_per_point`,
DROP COLUMN `max_followups`,
DROP COLUMN `time_limit_secs`,
DROP COLUMN `actual_duration_secs`;

-- 2025-12-05 新增面试历史记录删除功能
-- 1. 后端添加删除面试记录接口，支持删除所有关联数据
-- 2. 前端实现向左滑动显示删除按钮的功能

-- 2025-12-05 删除Order和Product相关表
-- 1. 删除order表
DROP TABLE IF EXISTS `order`;
-- 2. 删除product表
DROP TABLE IF EXISTS `product`;
