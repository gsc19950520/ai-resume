-- 面试报告表建表SQL
CREATE TABLE `interview_report` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `session_id` varchar(64) NOT NULL COMMENT '会话ID',
  `total_score` double NOT NULL COMMENT '总分',
  `overall_feedback` text COMMENT '总体评价',
  `strengths` text COMMENT '优势分析',
  `improvements` text COMMENT '改进点',
  `tech_depth_evaluation` text COMMENT '技术深度评价',
  `logic_expression_evaluation` text COMMENT '逻辑表达评价',
  `communication_evaluation` text COMMENT '沟通表达评价',
  `answer_depth_evaluation` text COMMENT '回答深度评价',
  `detailed_improvement_suggestions` text COMMENT '详细改进建议',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试报告表';
