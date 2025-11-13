-- 为简历表添加新字段的SQL脚本

-- 1. 添加期望薪资字段
ALTER TABLE resume 
ADD COLUMN expected_salary VARCHAR(50) COMMENT '期望薪资';

-- 2. 添加到岗时间字段
ALTER TABLE resume 
ADD COLUMN start_time VARCHAR(50) COMMENT '到岗时间';

-- 3. 添加兴趣爱好字段（JSON格式存储）
ALTER TABLE resume 
ADD COLUMN hobbies TEXT COMMENT '兴趣爱好（JSON格式存储）';

-- 4. 修改技能字段，从简单字符串改为技能评分的JSON格式
-- 注意：保留原skills字段，添加新字段以避免数据丢失
ALTER TABLE resume 
ADD COLUMN skills_with_level TEXT COMMENT '技能评分（JSON格式存储，包含技能名称和熟练度）';

-- 提交更改
COMMIT;
