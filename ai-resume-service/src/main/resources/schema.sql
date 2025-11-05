-- 创建数据库
CREATE DATABASE IF NOT EXISTS ai_resume CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ai_resume;

-- 用户表
CREATE TABLE IF NOT EXISTS user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    open_id VARCHAR(100) NOT NULL UNIQUE COMMENT '微信openId',
    nickname VARCHAR(50) COMMENT '用户昵称',
    avatar_url VARCHAR(255) COMMENT '头像URL',
    gender INT COMMENT '性别 0:未知 1:男 2:女',
    country VARCHAR(50) COMMENT '国家',
    province VARCHAR(50) COMMENT '省份',
    city VARCHAR(50) COMMENT '城市',
    remaining_optimize_count INT NOT NULL DEFAULT 0 COMMENT '剩余优化次数',
    is_vip TINYINT NOT NULL DEFAULT 0 COMMENT '是否VIP 0:否 1:是',
    vip_expire_time DATETIME COMMENT 'VIP过期时间',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    INDEX idx_open_id (open_id)
) COMMENT='用户表';

-- 添加创建时间和更新时间触发器
DELIMITER //
CREATE TRIGGER trg_user_before_insert BEFORE INSERT ON user
FOR EACH ROW
BEGIN
    SET NEW.create_time = NOW();
    SET NEW.update_time = NOW();
END //

CREATE TRIGGER trg_user_before_update BEFORE UPDATE ON user
FOR EACH ROW
BEGIN
    SET NEW.update_time = NOW();
END //
DELIMITER ;

-- 产品表
CREATE TABLE IF NOT EXISTS product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id VARCHAR(50) NOT NULL UNIQUE COMMENT '产品ID',
    name VARCHAR(100) NOT NULL COMMENT '产品名称',
    description TEXT COMMENT '产品描述',
    price INT NOT NULL COMMENT '价格(分)',
    type VARCHAR(50) NOT NULL COMMENT '产品类型',
    is_active TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用 0:否 1:是',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    INDEX idx_product_id (product_id),
    INDEX idx_type (type)
) COMMENT='产品表';

-- 添加创建时间和更新时间触发器
DELIMITER //
CREATE TRIGGER trg_product_before_insert BEFORE INSERT ON product
FOR EACH ROW
BEGIN
    SET NEW.create_time = NOW();
    SET NEW.update_time = NOW();
END //

CREATE TRIGGER trg_product_before_update BEFORE UPDATE ON product
FOR EACH ROW
BEGIN
    SET NEW.update_time = NOW();
END //
DELIMITER ;

-- 订单表
CREATE TABLE IF NOT EXISTS ai_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(50) NOT NULL UNIQUE COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_id VARCHAR(50) NOT NULL COMMENT '产品ID',
    product_type VARCHAR(50) NOT NULL COMMENT '产品类型',
    amount INT NOT NULL COMMENT '订单金额(分)',
    status INT NOT NULL DEFAULT 0 COMMENT '订单状态 0:待支付 1:支付成功 2:支付失败 3:已取消',
    transaction_id VARCHAR(100) COMMENT '微信支付交易ID',
    pay_type VARCHAR(20) COMMENT '支付方式',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    pay_time DATETIME COMMENT '支付时间',
    INDEX idx_order_no (order_no),
    INDEX idx_user_id (user_id),
    INDEX idx_product_id (product_id),
    INDEX idx_status (status),
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) COMMENT='订单表';

-- 添加创建时间和更新时间触发器
DELIMITER //
CREATE TRIGGER trg_ai_order_before_insert BEFORE INSERT ON ai_order
FOR EACH ROW
BEGIN
    SET NEW.create_time = NOW();
    SET NEW.update_time = NOW();
END //

CREATE TRIGGER trg_ai_order_before_update BEFORE UPDATE ON ai_order
FOR EACH ROW
BEGIN
    SET NEW.update_time = NOW();
END //
DELIMITER ;

-- 简历模板表
CREATE TABLE IF NOT EXISTS resume_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '模板名称',
    description TEXT COMMENT '模板描述',
    thumbnail_url VARCHAR(255) COMMENT '缩略图URL',
    template_file_url VARCHAR(255) COMMENT '模板文件URL',
    usage_count INT NOT NULL DEFAULT 0 COMMENT '使用次数',
    is_premium TINYINT NOT NULL DEFAULT 0 COMMENT '是否高级模板 0:否 1:是',
    active TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用 0:否 1:是',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    INDEX idx_active (active)
) COMMENT='简历模板表';

-- 添加创建时间和更新时间触发器
DELIMITER //
CREATE TRIGGER trg_resume_template_before_insert BEFORE INSERT ON resume_template
FOR EACH ROW
BEGIN
    SET NEW.create_time = NOW();
    SET NEW.update_time = NOW();
END //

CREATE TRIGGER trg_resume_template_before_update BEFORE UPDATE ON resume_template
FOR EACH ROW
BEGIN
    SET NEW.update_time = NOW();
END //
DELIMITER ;

-- 模板表（另一个模板表，可能有不同用途）
CREATE TABLE IF NOT EXISTS template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '模板名称',
    description TEXT COMMENT '模板描述',
    thumbnail_url VARCHAR(255) NOT NULL COMMENT '缩略图URL',
    template_url VARCHAR(255) NOT NULL COMMENT '模板URL',
    job_type VARCHAR(50) NOT NULL COMMENT '职位类型',
    price INT NOT NULL COMMENT '价格(分)',
    is_free TINYINT NOT NULL DEFAULT 0 COMMENT '是否免费 0:否 1:是',
    is_vip_only TINYINT NOT NULL DEFAULT 0 COMMENT '是否仅VIP可用 0:否 1:是',
    use_count INT NOT NULL DEFAULT 0 COMMENT '使用次数',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    INDEX idx_job_type (job_type),
    INDEX idx_is_free (is_free),
    INDEX idx_is_vip_only (is_vip_only)
) COMMENT='模板表';

-- 添加创建时间和更新时间触发器
DELIMITER //
CREATE TRIGGER trg_template_before_insert BEFORE INSERT ON template
FOR EACH ROW
BEGIN
    SET NEW.create_time = NOW();
    SET NEW.update_time = NOW();
END //

CREATE TRIGGER trg_template_before_update BEFORE UPDATE ON template
FOR EACH ROW
BEGIN
    SET NEW.update_time = NOW();
END //
DELIMITER ;

-- 简历表
CREATE TABLE IF NOT EXISTS resume (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    original_filename VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_path VARCHAR(255) NOT NULL COMMENT '文件路径',
    file_type VARCHAR(20) NOT NULL COMMENT '文件类型',
    job_type VARCHAR(50) NOT NULL COMMENT '职位类型',
    original_content LONGTEXT COMMENT '原始内容',
    optimized_content LONGTEXT COMMENT '优化后内容',
    ai_score INT COMMENT 'AI评分',
    ai_suggestion TEXT COMMENT 'AI建议',
    download_url_pdf VARCHAR(255) COMMENT 'PDF下载链接',
    download_url_word VARCHAR(255) COMMENT 'Word下载链接',
    template_id BIGINT COMMENT '使用的模板ID',
    status INT NOT NULL DEFAULT 0 COMMENT '状态 0:上传成功 1:优化中 2:优化成功 3:优化失败',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_template_id (template_id),
    INDEX idx_job_type (job_type),
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (template_id) REFERENCES resume_template(id) ON DELETE SET NULL
) COMMENT='简历表';

-- 添加创建时间和更新时间触发器
DELIMITER //
CREATE TRIGGER trg_resume_before_insert BEFORE INSERT ON resume
FOR EACH ROW
BEGIN
    SET NEW.create_time = NOW();
    SET NEW.update_time = NOW();
END //

CREATE TRIGGER trg_resume_before_update BEFORE UPDATE ON resume
FOR EACH ROW
BEGIN
    SET NEW.update_time = NOW();
END //
DELIMITER ;