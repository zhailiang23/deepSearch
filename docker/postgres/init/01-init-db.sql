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