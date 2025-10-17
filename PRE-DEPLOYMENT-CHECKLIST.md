# 生产环境部署前检查清单

## 🔍 部署前检查

### 1. 本地环境检查

- [ ] Node.js 20+ 已安装
- [ ] Java 17 已安装
- [ ] Maven 3.9+ 已安装
- [ ] sshpass 已安装 (`brew install hudochenkov/sshpass/sshpass`)
- [ ] 网络连接正常,可访问 192.168.153.111

### 2. 服务器环境检查

登录服务器确认:

```bash
ssh root@192.168.153.111
```

检查项:
- [ ] Docker 已安装 (`docker --version`)
- [ ] Docker Compose 已安装 (`docker-compose --version`)
- [ ] 磁盘空间充足 (`df -h`, 至少 20GB 可用)
- [ ] 端口未被占用:
  ```bash
  netstat -tulpn | grep -E ':(80|3306|6379|8080|9200|5002)\s'
  ```

### 3. 代码检查

- [ ] 所有代码已提交到git
- [ ] 前端代码可以构建成功 (`cd frontend && npm run build`)
- [ ] 后端代码可以构建成功 (`cd backend && ./mvnw clean package -DskipTests`)
- [ ] application-prod.yml 配置正确
- [ ] docker-compose-prod.yml 配置正确

### 4. 配置检查

#### 数据库配置
- [ ] MySQL密码: `Ynet@2024`
- [ ] MySQL root密码: `Ynet@2024Root`
- [ ] 数据库名: `mgmt_db`
- [ ] 用户名: `mgmt_user`

#### 服务端口
- [ ] Frontend: 80
- [ ] Backend: 8080
- [ ] MySQL: 3306
- [ ] Redis: 6379
- [ ] Elasticsearch: 9200, 9300
- [ ] Python Service: 5002

## 🚀 部署步骤

### 方式一: 自动化部署 (推荐)

```bash
# 执行部署脚本
./deploy.sh
```

### 方式二: 手动部署

按照 DEPLOYMENT.md 文档中的步骤操作

## ✅ 部署后验证

### 1. 服务状态检查

```bash
ssh root@192.168.153.111
cd /opt/deepsearch
docker-compose -f docker-compose-prod.yml ps
```

所有服务应该显示 "Up" 和 "healthy" 状态

### 2. 健康检查

在本地执行:

```bash
# MySQL
ssh root@192.168.153.111 "docker exec deepsearch-mysql-prod mysqladmin ping"

# Redis
ssh root@192.168.153.111 "docker exec deepsearch-redis-prod redis-cli ping"

# Elasticsearch
curl http://192.168.153.111:9200/_cluster/health

# Python服务
curl http://192.168.153.111:5002/health

# Backend
curl http://192.168.153.111:8080/api/actuator/health

# Frontend
curl -I http://192.168.153.111
```

### 3. 功能测试

- [ ] 访问前端首页: http://192.168.153.111
- [ ] 用户登录功能正常
- [ ] 搜索功能正常
- [ ] 数据统计功能正常
- [ ] 后台管理功能正常

### 4. 日志检查

```bash
# 查看后端日志,确认无ERROR
ssh root@192.168.153.111 "docker logs deepsearch-backend-prod | grep -i error"

# 查看Python服务日志
ssh root@192.168.153.111 "docker logs deepsearch-python-service-prod | grep -i error"
```

## 📊 性能基准测试

### 1. 响应时间测试

```bash
# Frontend响应时间
curl -o /dev/null -s -w '%{time_total}\n' http://192.168.153.111

# Backend API响应时间
curl -o /dev/null -s -w '%{time_total}\n' http://192.168.153.111:8080/api/actuator/health
```

期望值:
- Frontend: < 1s
- Backend: < 500ms

### 2. 并发测试

使用 Apache Bench 或 JMeter 进行压力测试

```bash
# 安装ab (如果需要)
brew install apache2

# 简单并发测试
ab -n 100 -c 10 http://192.168.153.111/
```

### 3. 资源使用监控

```bash
ssh root@192.168.153.111 "docker stats --no-stream"
```

检查:
- [ ] CPU使用率 < 80%
- [ ] 内存使用率 < 80%
- [ ] 无异常高负载容器

## 🔒 安全检查

- [ ] 修改默认密码(如果使用默认密码部署)
- [ ] 配置防火墙规则
- [ ] 确认只开放必要端口
- [ ] 检查日志文件权限
- [ ] 配置定期备份计划

## 📝 部署后任务

### 立即执行
- [ ] 创建首次数据备份: `./backup-restore.sh backup`
- [ ] 记录所有服务访问地址和凭证
- [ ] 配置监控告警(如果有)

### 后续计划
- [ ] 配置SSL证书,启用HTTPS
- [ ] 设置自动备份cron任务
- [ ] 配置日志轮转
- [ ] 建立性能监控dashboard
- [ ] 编写操作手册

## 🚨 回滚计划

如果部署失败需要回滚:

```bash
# 1. 停止新服务
ssh root@192.168.153.111 "cd /opt/deepsearch && docker-compose -f docker-compose-prod.yml down"

# 2. 恢复备份(如果有)
./backup-restore.sh restore

# 3. 启动旧版本服务
# (需要提前备份旧版本镜像)
```

## 📞 问题上报

如遇到问题,收集以下信息:

1. 错误信息和日志
2. 服务状态 (`docker-compose ps`)
3. 资源使用情况 (`docker stats`)
4. 网络连通性测试结果

## 📚 相关文档

- [部署文档](DEPLOYMENT.md)
- [项目文档](CLAUDE.md)
- [Docker Compose配置](docker-compose-prod.yml)
