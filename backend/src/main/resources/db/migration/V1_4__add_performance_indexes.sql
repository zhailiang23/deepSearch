-- 数据库性能优化索引
-- 为搜索日志表添加必要的索引，提升查询性能

-- 1. 创建时间范围查询的复合索引（最重要的查询模式）
CREATE INDEX IF NOT EXISTS idx_search_logs_created_at_status
ON search_logs (created_at, status);

-- 2. 用户相关查询的复合索引
CREATE INDEX IF NOT EXISTS idx_search_logs_user_created_at
ON search_logs (user_id, created_at)
WHERE user_id IS NOT NULL;

-- 3. 搜索空间相关查询的复合索引
CREATE INDEX IF NOT EXISTS idx_search_logs_search_space_created_at
ON search_logs (search_space_id, created_at)
WHERE search_space_id IS NOT NULL;

-- 4. 热词查询优化索引（覆盖索引，包含查询字段）
CREATE INDEX IF NOT EXISTS idx_search_logs_hot_words
ON search_logs (created_at, status, search_query)
WHERE status = 'SUCCESS' AND search_query IS NOT NULL AND search_query != '';

-- 5. 统计查询优化索引
CREATE INDEX IF NOT EXISTS idx_search_logs_statistics
ON search_logs (created_at, status, total_time_ms, total_results);

-- 6. 会话查询索引
CREATE INDEX IF NOT EXISTS idx_search_logs_session
ON search_logs (session_id, created_at)
WHERE session_id IS NOT NULL;

-- 7. 链路追踪查询索引
CREATE INDEX IF NOT EXISTS idx_search_logs_trace_id
ON search_logs (trace_id)
WHERE trace_id IS NOT NULL;

-- 8. IP地址查询索引（用于安全分析）
CREATE INDEX IF NOT EXISTS idx_search_logs_ip_created_at
ON search_logs (ip_address, created_at)
WHERE ip_address IS NOT NULL;

-- 9. 搜索空间代码查询索引
CREATE INDEX IF NOT EXISTS idx_search_logs_space_code
ON search_logs (search_space_code, created_at)
WHERE search_space_code IS NOT NULL;

-- 10. 响应时间范围查询索引（性能分析）
CREATE INDEX IF NOT EXISTS idx_search_logs_response_time
ON search_logs (total_time_ms, created_at)
WHERE total_time_ms IS NOT NULL;

-- 为搜索点击日志表添加性能索引
-- 11. 搜索日志ID外键索引（通常会自动创建，但确保存在）
CREATE INDEX IF NOT EXISTS idx_search_click_logs_search_log_id
ON search_click_logs (search_log_id);

-- 12. 点击时间查询索引
CREATE INDEX IF NOT EXISTS idx_search_click_logs_click_time
ON search_click_logs (click_time);

-- 13. 用户点击行为分析索引
CREATE INDEX IF NOT EXISTS idx_search_click_logs_user_click_time
ON search_click_logs (user_id, click_time)
WHERE user_id IS NOT NULL;

-- 14. 文档点击统计索引
CREATE INDEX IF NOT EXISTS idx_search_click_logs_document_id
ON search_click_logs (document_id, click_time)
WHERE document_id IS NOT NULL;

-- 添加表级统计信息注释，便于DBA理解索引用途
COMMENT ON INDEX idx_search_logs_created_at_status IS '主要查询模式：按时间范围和状态筛选搜索日志';
COMMENT ON INDEX idx_search_logs_hot_words IS '热词统计查询优化：覆盖索引包含所需字段';
COMMENT ON INDEX idx_search_logs_statistics IS '统计分析查询优化：支持性能指标计算';
COMMENT ON INDEX idx_search_logs_user_created_at IS '用户行为分析：按用户和时间查询';
COMMENT ON INDEX idx_search_logs_search_space_created_at IS '搜索空间分析：按空间和时间查询';

-- 分析表以更新统计信息，帮助查询优化器选择最佳执行计划
ANALYZE search_logs;
ANALYZE search_click_logs;