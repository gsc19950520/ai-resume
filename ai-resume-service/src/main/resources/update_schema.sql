-- 更新时间: 2024-11-17

-- 更新user表的avatar_url字段长度，确保能存储云存储URL
ALTER TABLE `user` MODIFY COLUMN `avatar_url` VARCHAR(255) COMMENT '用户头像URL';

-- 可选：如果之前存储了base64数据，可以考虑清理或迁移这些数据
-- 注意：以下SQL仅为示例，请根据实际需求执行
-- UPDATE `user` SET `avatar_url` = NULL WHERE `avatar_url` LIKE 'data:image/%';

-- 更新时间: 2024-11-20
-- 添加通过微信云存储fileID生成PDF的功能支持
