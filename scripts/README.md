# 数据迁移脚本

## 敏感词数据迁移

将 PostgreSQL 数据库中的 `sensitive_words` 表数据迁移到 MySQL 数据库。

### 前置条件

1. **PostgreSQL 数据库运行中**（端口 5432）
2. **MySQL 数据库运行中**（端口 3306）
3. **Python 环境**（使用 uv 管理）

### 使用步骤

#### 1. 安装依赖

```bash
cd scripts
uv pip install -r requirements.txt
```

#### 2. 确保数据库服务运行

```bash
# 检查 PostgreSQL（应该能连接）
docker-compose -f docker-compose-postgre.yml ps postgres

# 检查 MySQL（应该能连接）
docker-compose ps mysql
```

#### 3. 运行迁移脚本

```bash
uv run python migrate_sensitive_words.py
```

### 脚本功能

- ✅ 从 PostgreSQL 读取 `sensitive_words` 表的所有数据
- ✅ 清空 MySQL 目标表（防止重复数据）
- ✅ 批量插入数据到 MySQL
- ✅ 处理重复键冲突（自动跳过）
- ✅ 验证迁移结果

### 数据库连接配置

#### PostgreSQL（源数据库）
- Host: `localhost`
- Port: `5432`
- Database: `mgmt_db`
- User: `mgmt_user`
- Password: `mgmt_password`

#### MySQL（目标数据库）
- Host: `localhost`
- Port: `3306`
- Database: `mgmt_db`
- User: `mgmt_user`
- Password: `mgmt_password`

### 表结构

迁移的字段包括：
- `id` - 主键
- `name` - 敏感词名称
- `harm_level` - 危害等级（1-5）
- `enabled` - 是否启用
- `created_at` - 创建时间
- `updated_at` - 更新时间
- `created_by` - 创建人
- `updated_by` - 更新人
- `version` - 版本号（乐观锁）

### 注意事项

⚠️ **警告**：脚本会先清空 MySQL 目标表，请确保已备份重要数据！

如需保留现有数据，请修改脚本中的清空表逻辑：
```python
# 注释掉这两行
# cursor.execute("DELETE FROM sensitive_words")
# mysql_conn.commit()
```

### 故障排除

#### 1. 连接失败
- 确保 PostgreSQL 和 MySQL 容器正在运行
- 检查端口是否被占用
- 验证用户名和密码是否正确

#### 2. 权限错误
- 确保数据库用户有相应的读写权限

#### 3. 重复键错误
- 脚本会自动跳过重复记录
- 检查日志中的警告信息

### 输出示例

```
============================================================
敏感词数据迁移工具
PostgreSQL → MySQL
============================================================

✓ 成功连接到 PostgreSQL: localhost:5432/mgmt_db
✓ 成功连接到 MySQL: localhost:3306/mgmt_db

[1/3] 读取源数据...
✓ 从 PostgreSQL 读取到 1234 条敏感词记录

[2/3] 写入目标数据库...
⚠ 清空 MySQL 目标表...
✓ 已清空目标表
✓ 成功插入 1234 条记录

[3/3] 验证迁移结果...
✓ 验证完成: MySQL 中共有 1234 条敏感词记录

============================================================
✓ 数据迁移完成！共迁移 1234 条记录
============================================================

✓ 已关闭 PostgreSQL 连接
✓ 已关闭 MySQL 连接
```
