-- 创建数据库
CREATE DATABASE mgmt_db;
CREATE DATABASE mgmt_test;

-- 创建用户
CREATE USER mgmt_user WITH ENCRYPTED PASSWORD 'mgmt_password';
CREATE USER mgmt_test_user WITH ENCRYPTED PASSWORD 'test_password';

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