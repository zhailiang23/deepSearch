-- 创建数据库（如果不存在）
SELECT 'CREATE DATABASE mgmt_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'mgmt_db')\gexec
SELECT 'CREATE DATABASE mgmt_test' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'mgmt_test')\gexec

-- 创建用户（如果不存在）
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'mgmt_user') THEN
        CREATE USER mgmt_user WITH ENCRYPTED PASSWORD 'mgmt_password';
    END IF;
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'mgmt_test_user') THEN
        CREATE USER mgmt_test_user WITH ENCRYPTED PASSWORD 'test_password';
    END IF;
END $$;

-- 授予权限
GRANT ALL PRIVILEGES ON DATABASE mgmt_db TO mgmt_user;
GRANT ALL PRIVILEGES ON DATABASE mgmt_test TO mgmt_test_user;

-- 连接到mgmt_db数据库
\c mgmt_db;

-- 创建扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- 设置时区
SET timezone = 'UTC';

-- 数据库迁移脚本：移除 vector_enabled 列
-- 在开发环境中，如果已存在 search_spaces 表，需要移除 vector_enabled 列
DO $$
BEGIN
    -- 检查是否存在 vector_enabled 列和相关索引
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'search_spaces' AND column_name = 'vector_enabled'
    ) THEN
        -- 删除索引
        IF EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_search_space_vector_enabled') THEN
            DROP INDEX idx_search_space_vector_enabled;
        END IF;

        -- 删除列
        ALTER TABLE search_spaces DROP COLUMN vector_enabled;

        RAISE NOTICE 'Removed vector_enabled column and related index from search_spaces table';
    END IF;
EXCEPTION
    WHEN undefined_table THEN
        RAISE NOTICE 'Table search_spaces does not exist yet, will be created by JPA';
END $$;