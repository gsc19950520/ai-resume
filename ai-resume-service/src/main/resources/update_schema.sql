-- 2025-11-21 新增ResumeProject表字段
ALTER TABLE resume_project ADD COLUMN role VARCHAR(255) COMMENT '项目角色';
ALTER TABLE resume_project ADD COLUMN tech_stack TEXT COMMENT '技术栈';

-- 2024-01-19 新增InterviewQuestions类属性
ALTER TABLE resume_analysis ADD COLUMN questions TEXT COMMENT '面试问题列表';
ALTER TABLE resume_analysis ADD COLUMN analysis_type VARCHAR(50) COMMENT '分析类型';

-- 2024-05-29 添加期望关键点字段用于评分
ALTER TABLE interview_log ADD COLUMN expected_key_points text COMMENT '期望的关键点，JSON格式存储';
