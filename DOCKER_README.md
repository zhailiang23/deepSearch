# Docker 部署指南

本项目提供完整的Docker容器化部署方案，支持开发、测试和生产环境。

## 项目结构

```
.
├── backend/
│   ├── Dockerfile              # 后端开发环境
│   └── Dockerfile.prod         # 后端生产环境
├── frontend/
│   ├── Dockerfile              # 前端开发环境
│   └── Dockerfile.prod         # 前端生产环境
├── docker/
│   ├── postgres/
│   │   ├── init/
│   │   │   └── 01-init-db.sql  # 数据库初始化脚本
│   │   └── postgresql.conf     # PostgreSQL配置
│   ├── redis/
│   │   └── redis.conf          # Redis配置
│   └── nginx/
│       ├── nginx.conf          # Nginx主配置
│       └── default.conf        # 反向代理配置
├── docker-compose.yml          # 开发环境
├── docker-compose.test.yml     # 测试环境
├── docker-compose.prod.yml     # 生产环境
├── .env.example               # 环境变量模板
├── deploy.sh                  # 部署脚本
└── health-check.sh           # 健康检查脚本
```

## 快速开始

### 1. 环境准备

确保已安装：
- Docker 20.10+
- Docker Compose 2.0+

### 2. 开发环境

```bash
# 启动开发环境
./deploy.sh dev up

# 查看服务状态
docker-compose ps

# 查看日志
./deploy.sh dev logs

# 停止服务
./deploy.sh dev down
```

访问地址：
- 前端：http://localhost:3000
- 后端API：http://localhost:8080
- PostgreSQL：localhost:5432
- Redis：localhost:6379

### 3. 测试环境

```bash
# 启动测试环境
./deploy.sh test up

# 健康检查
./health-check.sh test
```

访问地址：
- 后端API：http://localhost:8081
- PostgreSQL：localhost:5433
- Redis：localhost:6380

### 4. 生产环境

```bash
# 复制环境变量配置
cp .env.example .env

# 编辑环境变量（重要！）
vim .env

# 启动生产环境
./deploy.sh prod up

# 健康检查
./health-check.sh prod
```

## 环境配置

### 开发环境特点
- 使用默认密码和配置
- 暴露所有端口便于调试
- 包含开发工具和热重载
- 数据不持久化到宿主机

### 生产环境特点
- 使用环境变量配置敏感信息
- 网络隔离（内部/外部网络）
- 资源限制和优化配置
- 包含Nginx反向代理
- 数据持久化和备份支持

## 命令参考

### 部署脚本 (deploy.sh)

```bash
./deploy.sh <环境> <动作>

环境：
  dev   - 开发环境
  test  - 测试环境
  prod  - 生产环境

动作：
  up      - 启动服务
  down    - 停止服务
  restart - 重启服务
  logs    - 查看日志
  build   - 重新构建并启动
  clean   - 清理所有容器和数据
```

### 健康检查 (health-check.sh)

```bash
./health-check.sh <环境>

# 检查开发环境
./health-check.sh dev

# 检查生产环境
./health-check.sh prod
```

## 数据持久化

### 开发环境
- PostgreSQL：`postgres_data` volume
- Redis：`redis_data` volume

### 生产环境
- PostgreSQL：`postgres_data` volume + `postgres_backup` volume
- Redis：`redis_data` volume
- 应用日志：`./logs` 目录映射

## 网络配置

### 开发环境
- 单一bridge网络：`mgmt-network`
- 所有服务在同一网络

### 生产环境
- 内部网络：`mgmt-internal`（数据库、缓存）
- 外部网络：`mgmt-external`（Web服务）
- Nginx提供反向代理和SSL终止

## 监控和日志

### 健康检查
所有服务都配置了健康检查：
- PostgreSQL：`pg_isready`
- Redis：`redis-cli ping`
- 后端：`/actuator/health`
- 前端：HTTP响应检查

### 日志收集
- 应用日志：映射到宿主机`./logs`目录
- 容器日志：使用`docker-compose logs`查看
- Nginx日志：标准输出，便于容器化环境收集

## 故障排查

### 常见问题

1. **容器启动失败**
   ```bash
   # 查看容器日志
   docker-compose logs <服务名>

   # 查看构建过程
   docker-compose up --build
   ```

2. **数据库连接失败**
   ```bash
   # 检查数据库状态
   docker-compose exec postgres pg_isready -U mgmt_user

   # 查看数据库日志
   docker-compose logs postgres
   ```

3. **端口冲突**
   ```bash
   # 检查端口占用
   lsof -i :8080

   # 修改docker-compose.yml中的端口映射
   ```

4. **磁盘空间不足**
   ```bash
   # 清理未使用的镜像和容器
   docker system prune -a

   # 清理所有数据（谨慎使用）
   ./deploy.sh <环境> clean
   ```

### 调试技巧

1. **进入容器调试**
   ```bash
   docker-compose exec backend bash
   docker-compose exec postgres psql -U mgmt_user -d mgmt_db
   ```

2. **查看网络连接**
   ```bash
   docker network ls
   docker network inspect <网络名>
   ```

3. **资源使用情况**
   ```bash
   docker stats
   ```

## 性能优化

### 镜像优化
- 使用多阶段构建减小镜像大小
- 合理使用.dockerignore
- 选择合适的基础镜像（Alpine）

### 运行时优化
- 设置合适的JVM参数（生产环境）
- 配置资源限制防止资源耗尽
- 使用健康检查确保服务稳定性

### 网络优化
- 生产环境使用内外网络隔离
- Nginx启用压缩和缓存
- 合理设置连接池大小

## 安全考虑

1. **密码管理**
   - 生产环境必须使用强密码
   - 敏感信息通过环境变量传递
   - 定期轮换密码

2. **网络安全**
   - 生产环境使用网络隔离
   - 只暴露必要的端口
   - 配置防火墙规则

3. **容器安全**
   - 使用非root用户运行应用
   - 定期更新基础镜像
   - 扫描镜像安全漏洞

## 备份策略

### 数据库备份
```bash
# 创建备份
docker-compose exec postgres pg_dump -U mgmt_user mgmt_db > backup.sql

# 恢复备份
docker-compose exec -T postgres psql -U mgmt_user mgmt_db < backup.sql
```

### 配置备份
- 定期备份`.env`文件
- 版本控制docker配置文件
- 备份SSL证书（生产环境）

---

如有问题，请查看容器日志或联系开发团队。