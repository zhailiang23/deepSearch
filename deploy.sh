#!/bin/bash

# deepSearch 生产环境部署脚本
# 服务器: 192.168.153.111 (x86架构)

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 配置
SERVER_IP="192.168.153.111"
SERVER_USER="root"
SERVER_PASSWORD="Ynet@2024"
REMOTE_DIR="/opt/deepsearch"
PROJECT_NAME="deepSearch"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}deepSearch 生产环境部署${NC}"
echo -e "${GREEN}========================================${NC}"

# 检查是否安装了sshpass
if ! command -v sshpass &> /dev/null; then
    echo -e "${YELLOW}正在安装 sshpass...${NC}"
    brew install hudochenkov/sshpass/sshpass 2>/dev/null || {
        echo -e "${RED}请手动安装 sshpass: brew install hudochenkov/sshpass/sshpass${NC}"
        exit 1
    }
fi

# 1. 打包前端
echo -e "${YELLOW}步骤 1/7: 构建前端...${NC}"
cd frontend
npm run build
cd ..
echo -e "${GREEN}✓ 前端构建完成${NC}"

# 2. 打包后端
echo -e "${YELLOW}步骤 2/7: 构建后端...${NC}"
cd backend
./mvnw clean package -DskipTests
cd ..
echo -e "${GREEN}✓ 后端构建完成${NC}"

# 3. 准备部署文件
echo -e "${YELLOW}步骤 3/7: 准备部署文件...${NC}"
mkdir -p deploy-temp
cp -r backend deploy-temp/
cp -r frontend deploy-temp/
cp -r python deploy-temp/
cp -r docker deploy-temp/
cp docker-compose-prod.yml deploy-temp/
cp .dockerignore deploy-temp/ 2>/dev/null || true

# 创建日志目录
mkdir -p deploy-temp/logs/backend

echo -e "${GREEN}✓ 部署文件准备完成${NC}"

# 4. 上传文件到服务器
echo -e "${YELLOW}步骤 4/7: 上传文件到服务器 ${SERVER_IP}...${NC}"

# 创建远程目录
sshpass -p "${SERVER_PASSWORD}" ssh -o StrictHostKeyChecking=no ${SERVER_USER}@${SERVER_IP} "mkdir -p ${REMOTE_DIR}"

# 使用rsync上传文件
sshpass -p "${SERVER_PASSWORD}" rsync -avz --progress \
    -e "ssh -o StrictHostKeyChecking=no" \
    --exclude 'node_modules' \
    --exclude '.git' \
    --exclude 'target' \
    --exclude '__pycache__' \
    --exclude '.venv' \
    --exclude 'dist' \
    --exclude '.DS_Store' \
    deploy-temp/ ${SERVER_USER}@${SERVER_IP}:${REMOTE_DIR}/

echo -e "${GREEN}✓ 文件上传完成${NC}"

# 5. 在服务器上构建镜像
echo -e "${YELLOW}步骤 5/7: 在服务器上构建Docker镜像...${NC}"

sshpass -p "${SERVER_PASSWORD}" ssh -o StrictHostKeyChecking=no ${SERVER_USER}@${SERVER_IP} << 'ENDSSH'
cd /opt/deepsearch

echo "开始构建Docker镜像..."

# 构建镜像
docker-compose -f docker-compose-prod.yml build --no-cache

echo "✓ Docker镜像构建完成"
ENDSSH

echo -e "${GREEN}✓ 镜像构建完成${NC}"

# 6. 停止旧服务并启动新服务
echo -e "${YELLOW}步骤 6/7: 部署服务...${NC}"

sshpass -p "${SERVER_PASSWORD}" ssh -o StrictHostKeyChecking=no ${SERVER_USER}@${SERVER_IP} << 'ENDSSH'
cd /opt/deepsearch

echo "停止旧服务..."
docker-compose -f docker-compose-prod.yml down 2>/dev/null || true

echo "启动新服务..."
docker-compose -f docker-compose-prod.yml up -d

echo "等待服务启动..."
sleep 30

echo "检查服务状态..."
docker-compose -f docker-compose-prod.yml ps

ENDSSH

echo -e "${GREEN}✓ 服务部署完成${NC}"

# 7. 健康检查
echo -e "${YELLOW}步骤 7/7: 健康检查...${NC}"

sleep 10

# 检查各服务健康状态
echo "检查MySQL..."
sshpass -p "${SERVER_PASSWORD}" ssh -o StrictHostKeyChecking=no ${SERVER_USER}@${SERVER_IP} \
    "docker exec deepsearch-mysql-prod mysqladmin ping -h localhost -u mgmt_user -pYnet@2024 || echo 'MySQL未就绪'"

echo "检查Redis..."
sshpass -p "${SERVER_PASSWORD}" ssh -o StrictHostKeyChecking=no ${SERVER_USER}@${SERVER_IP} \
    "docker exec deepsearch-redis-prod redis-cli ping || echo 'Redis未就绪'"

echo "检查Elasticsearch..."
curl -s http://${SERVER_IP}:9200/_cluster/health || echo "Elasticsearch未就绪"

echo "检查Python服务..."
curl -s http://${SERVER_IP}:5002/health || echo "Python服务未就绪"

echo "检查Backend服务..."
curl -s http://${SERVER_IP}:8080/api/actuator/health || echo "Backend服务未就绪"

echo "检查Frontend..."
curl -s -o /dev/null -w "%{http_code}" http://${SERVER_IP}/ || echo "Frontend未就绪"

# 清理临时文件
echo -e "${YELLOW}清理临时文件...${NC}"
rm -rf deploy-temp

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}部署完成!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "访问地址: ${GREEN}http://${SERVER_IP}${NC}"
echo -e "后端API: ${GREEN}http://${SERVER_IP}:8080/api${NC}"
echo -e "Elasticsearch: ${GREEN}http://${SERVER_IP}:9200${NC}"
echo ""
echo "查看日志:"
echo "  docker logs -f deepsearch-backend-prod"
echo "  docker logs -f deepsearch-python-service-prod"
echo "  docker logs -f deepsearch-frontend-prod"
echo ""
echo "服务管理:"
echo "  停止: ssh ${SERVER_USER}@${SERVER_IP} 'cd ${REMOTE_DIR} && docker-compose -f docker-compose-prod.yml down'"
echo "  重启: ssh ${SERVER_USER}@${SERVER_IP} 'cd ${REMOTE_DIR} && docker-compose -f docker-compose-prod.yml restart'"
