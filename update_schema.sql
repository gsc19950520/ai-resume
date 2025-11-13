-- 简历表结构更新脚本
-- 用于添加新功能所需的字段

-- 先检查表结构，避免重复添加字段
-- 以下SQL语句假设简历表名为 resume，根据实际情况修改表名

-- 1. 添加期望薪资字段
ALTER TABLE resume
ADD COLUMN IF NOT EXISTS expected_salary VARCHAR(50) NULL COMMENT '期望薪资';

-- 2. 添加到岗时间字段
ALTER TABLE resume
ADD COLUMN IF NOT EXISTS start_time VARCHAR(50) NULL COMMENT '到岗时间';

-- 3. 添加兴趣爱好字段（JSON格式）
ALTER TABLE resume
ADD COLUMN IF NOT EXISTS hobbies TEXT NULL COMMENT '兴趣爱好，JSON数组格式';

-- 4. 添加技能评分字段（JSON格式）
ALTER TABLE resume
ADD COLUMN IF NOT EXISTS skills_with_level TEXT NULL COMMENT '技能评分，JSON数组格式';

-- 5. 为新添加的字段创建索引（可选，根据查询性能需求）
-- 如果经常根据这些字段查询，可以创建索引
-- CREATE INDEX idx_resume_expected_salary ON resume(expected_salary);

-- 显示更新后的表结构，用于验证
DESCRIBE resume;

-- 提示信息
SELECT '数据库表结构更新完成。已添加以下字段：expected_salary, start_time, hobbies, skills_with_level' AS '更新状态';
