-- 1. 创建动态配置表
drop table if exists dynamic_config;
create table dynamic_config (
    id bigint primary key auto_increment,
    config_key varchar(100) not null unique comment '配置键',
    config_value text not null comment '配置值(JSON格式)',
    description varchar(255) comment '配置描述',
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp on update current_timestamp
) comment '动态面试配置表';

-- 2. 修改interview_session表，添加新字段
alter table interview_session
add column persona varchar(50) default 'friendly' comment '面试官风格',
add column session_seconds int default 900 comment '会话总时长(秒)',
add column session_time_remaining int default 900 comment '剩余时间(秒)',
add column consecutive_no_match_count int default 0 comment '连续不匹配次数',
add column tech_items text comment '提取的技术项(JSON格式)',
add column project_points text comment '提取的项目点(JSON格式)',
add column interview_state text comment '面试状态(JSON格式)',
add column interview_mode varchar(20) default 'time_based' comment '面试模式',
add column stop_reason varchar(100) comment '停止原因';

-- 3. 修改interview_log表，添加新字段
alter table interview_log
add column answer_duration int comment '回答时长(秒)',
add column related_tech_items text comment '关联技术项(JSON格式)',
add column related_project_points text comment '关联项目点(JSON格式)',
add column stop_reason varchar(100) comment '停止原因';

-- 4. 插入默认配置
insert into dynamic_config (config_key, config_value, description)
values 
('interview_personas', '{"personas":[{"key":"friendly","name":"友好","description":"语气亲和，注重引导"},{"key":"neutral","name":"中立","description":"客观专业，条理清晰"},{"key":"challenging","name":"挑战性","description":"深入追问，注重细节"}]}', '面试官风格配置'),
('default_interview_template', '{"defaultSessionSeconds":900,"defaultPersona":"friendly","minSessionSeconds":600,"maxSessionSeconds":1800}', '默认面试模板'),
('depth_levels', '{"depthLevels":[{"id":"basic","name":"用法","text":"基础","description":"基础应用"},{"id":"intermediate","name":"实现","text":"进阶","description":"实现细节"},{"id":"advanced","name":"原理","text":"深入","description":"原理机制"},{"id":"expert","name":"优化","text":"高级","description":"性能优化"}]}', '深度级别配置');