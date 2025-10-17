# deepSearch 生产环境部署文档

## 服务器信息

- **IP地址**: 192.168.153.111
- **架构**: x86_64
- **操作系统**: Linux
- **用户名**: root
- **密码**: Ynet@2024

## 部署前准备

### 1. 本地环境要求

- macOS/Linux
- Node.js 20+
- Java 17
- Maven 3.9+
- Docker (可选,用于本地测试)
- sshpass (用于自动化部署)

### 2. 安装依赖

```bash
# 安装 sshpass (macOS)
brew install hudochenkov/sshpass/sshpass
```

### 3. 服务器环境要求

服务器需要预先安装:
- Docker 20.10+
- Docker Compose 2.0+

## 部署步骤

### 方式一: 自动化部署 (推荐)

使用提供的部署脚本一键部署:

```bash
# 执行部署脚本
./deploy.sh
```

脚本会自动完成以下步骤:
1. 构建前端 (npm run build)
2. 构建后端 (mvn package)
3. 准备部署文件
4. 上传文件到服务器
5. 在服务器构建Docker镜像
6. 部署并启动所有服务
7. 执行健康检查

### 方式二: 手动部署

#### 1. 构建项目

```bash
# 构建前端
cd frontend
npm install
npm run build
cd ..

# 构建后端
cd backend
./mvnw clean package -DskipTests
cd ..
```

#### 2. 上传文件到服务器

```bash
# 使用rsync上传
rsync -avz --progress \
    --exclude 'node_modules' \
    --exclude '.git' \
    --exclude 'target' \
    --exclude '__pycache__' \
    -e ssh \
    ./ root@192.168.153.111:/opt/deepsearch/
```

#### 3. 登录服务器构建镜像

```bash
# SSH登录服务器
ssh root@192.168.153.111

# 进入项目目录
cd /opt/deepsearch

# 构建镜像
docker-compose -f docker-compose-prod.yml build

# 启动服务
docker-compose -f docker-compose-prod.yml up -d

# 查看服务状态
docker-compose -f docker-compose-prod.yml ps
```

## 服务端口

- **Frontend**: 80 (HTTP)
- **Backend**: 8080 (HTTP API)
- **MySQL**: 3306
- **Redis**: 6379
- **Elasticsearch**: 9200 (HTTP), 9300 (Transport)
- **Python Service**: 5002

## 访问地址

- **前端界面**: http://192.168.153.111
- **后端API**: http://192.168.153.111:8080/api
- **Elasticsearch**: http://192.168.153.111:9200

## 服务管理

### 查看服务状态

```bash
ssh root@192.168.153.111
cd /opt/deepsearch
docker-compose -f docker-compose-prod.yml ps
```

### 查看日志

```bash
# 查看所有服务日志
docker-compose -f docker-compose-prod.yml logs -f

# 查看特定服务日志
docker logs -f deepsearch-backend-prod
docker logs -f deepsearch-python-service-prod
docker logs -f deepsearch-frontend-prod
docker logs -f deepsearch-mysql-prod
docker logs -f deepsearch-elasticsearch-prod
```

### 重启服务

```bash
# 重启所有服务
docker-compose -f docker-compose-prod.yml restart

# 重启特定服务
docker-compose -f docker-compose-prod.yml restart backend
docker-compose -f docker-compose-prod.yml restart frontend
```

### 停止服务

```bash
# 停止所有服务
docker-compose -f docker-compose-prod.yml down

# 停止但保留数据
docker-compose -f docker-compose-prod.yml stop
```

### 更新服务

```bash
# 在本地执行部署脚本
./deploy.sh

# 或手动更新
ssh root@192.168.153.111
cd /opt/deepsearch
docker-compose -f docker-compose-prod.yml pull
docker-compose -f docker-compose-prod.yml up -d
```

## 数据备份与恢复

### 备份数据

```bash
# 执行备份脚本
./backup-restore.sh backup
```

备份文件保存在 `./backups/YYYYMMDD_HHMMSS/` 目录下:
- `mysql_backup.sql` - MySQL数据库备份
- `es_backup.tar.gz` - Elasticsearch索引备份
- `backup_info.txt` - 备份信息

### 恢复数据

```bash
# 执行恢复脚本
./backup-restore.sh restore

# 根据提示选择要恢复的备份
```

### 手动备份MySQL

```bash
# 在服务器上执行
docker exec deepsearch-mysql-prod mysqldump \
    -u root -pYnet@2024Root mgmt_db > /tmp/backup.sql

# 下载到本地
scp root@192.168.153.111:/tmp/backup.sql ./backup.sql
```

### 手动恢复MySQL

```bash
# 上传备份文件
scp backup.sql root@192.168.153.111:/tmp/

# 在服务器上恢复
ssh root@192.168.153.111
docker exec -i deepsearch-mysql-prod mysql \
    -u root -pYnet@2024Root mgmt_db < /tmp/backup.sql
```

## 监控与维护

### 健康检查

```bash
# MySQL
curl http://192.168.153.111:8080/api/actuator/health

# Elasticsearch
curl http://192.168.153.111:9200/_cluster/health

# Python服务
curl http://192.168.153.111:5002/health

# Frontend
curl -I http://192.168.153.111
```

### 资源使用

```bash
# 查看容器资源使用
docker stats

# 查看磁盘使用
df -h

# 查看Docker磁盘使用
docker system df
```

### 清理Docker资源

```bash
# 清理未使用的镜像
docker image prune -a

# 清理未使用的容器
docker container prune

# 清理未使用的卷
docker volume prune

# 清理所有未使用资源
docker system prune -a
```

## 环境变量

生产环境主要环境变量在 `docker-compose-prod.yml` 中配置:

```yaml
# MySQL
MYSQL_PASSWORD: Ynet@2024
MYSQL_ROOT_PASSWORD: Ynet@2024Root

# Backend
SPRING_PROFILES_ACTIVE: prod
DB_HOST: mysql
DB_PORT: 3306
DB_NAME: mgmt_db
DB_USERNAME: mgmt_user
DB_PASSWORD: Ynet@2024
```

## 故障排查

### 服务无法启动

1. 查看日志: `docker logs <container_name>`
2. 检查端口占用: `netstat -tulpn | grep <port>`
3. 检查磁盘空间: `df -h`
4. 检查内存: `free -h`

### 数据库连接失败

1. 确认MySQL容器运行: `docker ps | grep mysql`
2. 测试连接: `docker exec deepsearch-mysql-prod mysqladmin ping`
3. 检查密码配置是否正确

### Elasticsearch问题

1. 检查集群健康: `curl localhost:9200/_cluster/health`
2. 查看节点信息: `curl localhost:9200/_cat/nodes`
3. 检查内存设置: ES_JAVA_OPTS="-Xms1g -Xmx1g"

### 服务间通信失败

1. 检查Docker网络: `docker network ls`
2. 测试服务间连接: `docker exec <container> ping <other_container>`
3. 检查防火墙规则

## 安全建议

1. **修改默认密码**: 部署后立即修改MySQL等服务的默认密码
2. **配置防火墙**: 只开放必要的端口(80, 8080)
3. **使用HTTPS**: 配置SSL证书,启用HTTPS
4. **定期备份**: 建立自动备份计划
5. **监控日志**: 定期检查系统和应用日志
6. **更新依赖**: 定期更新Docker镜像和系统包

## 性能优化

### 数据库优化

```sql
-- 优化MySQL配置
SET GLOBAL max_connections = 500;
SET GLOBAL query_cache_size = 268435456;
```

### Elasticsearch优化

```bash
# 调整JVM堆内存
ES_JAVA_OPTS="-Xms2g -Xmx2g"

# 调整文件描述符限制
ulimit -n 65536
```

### Redis优化

```bash
# 配置持久化
docker exec deepsearch-redis-prod redis-cli CONFIG SET appendonly yes
docker exec deepsearch-redis-prod redis-cli CONFIG SET maxmemory 2gb
```

## 联系支持

如遇到问题,请查看:
- 项目文档: `/opt/deepsearch/CLAUDE.md`
- 日志文件: `/opt/deepsearch/logs/`
- GitHub Issues: (项目地址)
