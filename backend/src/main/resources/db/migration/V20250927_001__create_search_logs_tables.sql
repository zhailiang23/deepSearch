-- =============================================
-- 搜索日志管理系统数据库表创建脚本
-- Version: V20250927_001
-- Description: 创建search_logs和search_click_logs表
-- Author: Claude Code AI
-- Date: 2025-09-27
-- =============================================

-- 1. 创建搜索日志主表
CREATE TABLE search_logs (
    id BIGSERIAL PRIMARY KEY COMMENT '主键ID',
    trace_id VARCHAR(64) COMMENT '链路追踪ID',
    user_id BIGINT COMMENT '用户ID',
    session_id VARCHAR(64) COMMENT '会话ID',
    ip_address VARCHAR(45) COMMENT 'IP地址(支持IPv6)',
    user_agent TEXT COMMENT '用户代理信息',

    -- 搜索请求信息
    search_space_id BIGINT COMMENT '搜索空间ID',
    search_space_code VARCHAR(50) COMMENT '搜索空间代码',
    search_space_name VARCHAR(100) COMMENT '搜索空间名称',
    search_query TEXT COMMENT '搜索关键词',
    search_mode VARCHAR(20) COMMENT '搜索模式',
    enable_pinyin BOOLEAN DEFAULT FALSE COMMENT '是否启用拼音搜索',
    page_number INTEGER DEFAULT 1 COMMENT '页码',
    page_size INTEGER DEFAULT 10 COMMENT '页大小',

    -- 响应信息
    total_results BIGINT DEFAULT 0 COMMENT '结果总数',
    returned_results INTEGER DEFAULT 0 COMMENT '返回结果数',
    max_score DECIMAL(10,6) COMMENT '最大相关性分数',

    -- 性能指标
    elasticsearch_time_ms BIGINT COMMENT 'ES查询耗时(毫秒)',
    total_time_ms BIGINT COMMENT '总处理时间(毫秒)',
    memory_used_mb INTEGER COMMENT '内存使用(MB)',

    -- 状态信息
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS' COMMENT '请求状态',
    error_type VARCHAR(100) COMMENT '错误类型',
    error_message TEXT COMMENT '错误消息',
    error_stack_trace TEXT COMMENT '错误堆栈跟踪',

    -- 审计字段
    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by VARCHAR(50) COMMENT '创建者',
    updated_by VARCHAR(50) COMMENT '更新者'
) COMMENT '搜索日志主表';

-- 2. 创建搜索点击日志表
CREATE TABLE search_click_logs (
    id BIGSERIAL PRIMARY KEY COMMENT '主键ID',
    search_log_id BIGINT NOT NULL COMMENT '关联搜索日志ID',
    document_id VARCHAR(200) NOT NULL COMMENT '点击的文档ID',
    document_index VARCHAR(100) COMMENT '文档索引名称',
    document_title TEXT COMMENT '文档标题',
    click_position INTEGER NOT NULL COMMENT '点击位置（第几条结果，从1开始）',
    click_score DECIMAL(10,6) COMMENT '点击文档的相关性分数',
    click_sequence INTEGER NOT NULL COMMENT '同一次搜索中的点击顺序（1,2,3...）',
    click_time TIMESTAMP WITH TIME ZONE NOT NULL COMMENT '具体点击时间',
    user_id BIGINT COMMENT '点击用户ID',
    session_id VARCHAR(64) COMMENT '用户会话ID',
    ip_address VARCHAR(45) COMMENT '点击IP地址',

    -- 审计字段
    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by VARCHAR(50) COMMENT '创建者',
    updated_by VARCHAR(50) COMMENT '更新者',

    -- 外键约束
    FOREIGN KEY (search_log_id) REFERENCES search_logs(id) ON DELETE CASCADE
) COMMENT '搜索点击日志表';

-- 3. 创建search_logs表索引
-- 主键索引（自动创建）

-- 时间相关索引
CREATE INDEX idx_search_logs_created_at ON search_logs(created_at);
CREATE INDEX idx_search_logs_created_at_desc ON search_logs(created_at DESC);

-- 用户相关索引
CREATE INDEX idx_search_logs_user_id ON search_logs(user_id);
CREATE INDEX idx_search_logs_session_id ON search_logs(session_id);
CREATE INDEX idx_search_logs_ip_address ON search_logs(ip_address);

-- 搜索相关索引
CREATE INDEX idx_search_logs_search_space_id ON search_logs(search_space_id);
CREATE INDEX idx_search_logs_search_space_code ON search_logs(search_space_code);
CREATE INDEX idx_search_logs_trace_id ON search_logs(trace_id);

-- 状态和性能索引
CREATE INDEX idx_search_logs_status ON search_logs(status);
CREATE INDEX idx_search_logs_total_time ON search_logs(total_time_ms);
CREATE INDEX idx_search_logs_elasticsearch_time ON search_logs(elasticsearch_time_ms);

-- 复合索引（常用查询组合）
CREATE INDEX idx_search_logs_user_created ON search_logs(user_id, created_at DESC);
CREATE INDEX idx_search_logs_space_created ON search_logs(search_space_id, created_at DESC);
CREATE INDEX idx_search_logs_status_created ON search_logs(status, created_at DESC);

-- 4. 创建search_click_logs表索引
-- 主键索引（自动创建）

-- 外键索引
CREATE INDEX idx_search_click_logs_search_log_id ON search_click_logs(search_log_id);

-- 时间相关索引
CREATE INDEX idx_search_click_logs_click_time ON search_click_logs(click_time);
CREATE INDEX idx_search_click_logs_created_at ON search_click_logs(created_at);

-- 用户相关索引
CREATE INDEX idx_search_click_logs_user_id ON search_click_logs(user_id);
CREATE INDEX idx_search_click_logs_session_id ON search_click_logs(session_id);

-- 文档相关索引
CREATE INDEX idx_search_click_logs_document_id ON search_click_logs(document_id);
CREATE INDEX idx_search_click_logs_document_index ON search_click_logs(document_index);

-- 复合索引（常用查询组合）
CREATE INDEX idx_search_click_logs_search_click_seq ON search_click_logs(search_log_id, click_sequence);
CREATE INDEX idx_search_click_logs_user_click_time ON search_click_logs(user_id, click_time DESC);

-- 5. 添加数据完整性约束

-- search_logs表约束
ALTER TABLE search_logs
ADD CONSTRAINT chk_search_logs_status
CHECK (status IN ('SUCCESS', 'ERROR', 'TIMEOUT'));

ALTER TABLE search_logs
ADD CONSTRAINT chk_search_logs_page_number
CHECK (page_number > 0);

ALTER TABLE search_logs
ADD CONSTRAINT chk_search_logs_page_size
CHECK (page_size > 0 AND page_size <= 1000);

ALTER TABLE search_logs
ADD CONSTRAINT chk_search_logs_total_results
CHECK (total_results >= 0);

ALTER TABLE search_logs
ADD CONSTRAINT chk_search_logs_returned_results
CHECK (returned_results >= 0);

ALTER TABLE search_logs
ADD CONSTRAINT chk_search_logs_time_ms
CHECK (elasticsearch_time_ms >= 0 AND total_time_ms >= 0);

ALTER TABLE search_logs
ADD CONSTRAINT chk_search_logs_memory_used
CHECK (memory_used_mb >= 0);

-- search_click_logs表约束
ALTER TABLE search_click_logs
ADD CONSTRAINT chk_search_click_logs_click_position
CHECK (click_position > 0);

ALTER TABLE search_click_logs
ADD CONSTRAINT chk_search_click_logs_click_sequence
CHECK (click_sequence > 0);

ALTER TABLE search_click_logs
ADD CONSTRAINT chk_search_click_logs_click_time_order
CHECK (click_time >= created_at - INTERVAL '1 hour');

-- 6. 添加表注释（某些数据库系统可能需要）
COMMENT ON TABLE search_logs IS '搜索日志主表 - 记录所有搜索请求的详细信息';
COMMENT ON TABLE search_click_logs IS '搜索点击日志表 - 记录用户对搜索结果的点击行为';

-- 完成搜索日志管理系统数据库表创建
-- 包含：2个表、17个索引、11个约束、完整注释和外键关系