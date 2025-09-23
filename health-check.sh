#!/bin/bash

# 增强的健康检查脚本
# 支持详细的服务检查和监控

set -e

ENV=${1:-dev}
VERBOSE=${2:-false}

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')] $1${NC}"
}

success() {
    echo -e "${GREEN}✓ $1${NC}"
}

warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

error() {
    echo -e "${RED}✗ $1${NC}"
}

echo "=== 深搜管理系统健康检查 ==="
echo "环境: $ENV"
echo "时间: $(date)"
echo "============================================"

case $ENV in
    dev)
        COMPOSE_FILE="docker-compose.yml"
        BASE_URL="http://localhost"
        API_PORT="8080"
        FRONTEND_PORT="3000"
        ;;
    test)
        COMPOSE_FILE="docker-compose.test.yml"
        BASE_URL="http://localhost"
        API_PORT="8080"
        FRONTEND_PORT="3000"
        ;;
    prod)
        COMPOSE_FILE="docker-compose.prod.yml"
        BASE_URL="http://localhost"
        API_PORT="80"
        FRONTEND_PORT="80"
        ;;
    *)
        echo "用法: $0 {dev|test|prod} [verbose]"
        exit 1
        ;;
esac

# 检查Docker环境
log "检查Docker环境..."
if ! command -v docker &> /dev/null; then
    error "Docker未安装"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    error "Docker Compose未安装"
    exit 1
fi

success "Docker环境检查通过"

# 检查容器状态
log "检查容器状态..."
echo ""
docker-compose -f $COMPOSE_FILE ps
echo ""

# 检查各个服务
OVERALL_STATUS=0

# 检查 PostgreSQL
log "检查 PostgreSQL..."
if docker-compose -f $COMPOSE_FILE exec -T postgres pg_isready -U mgmt_user -d mgmt_db 2>/dev/null; then
    success "PostgreSQL 正在运行并接受连接"

    # 详细检查
    if [ "$VERBOSE" = "true" ]; then
        DB_VERSION=$(docker-compose -f $COMPOSE_FILE exec -T postgres psql -U mgmt_user -d mgmt_db -t -c "SELECT version();" 2>/dev/null | head -1 | xargs)
        echo "  版本: $DB_VERSION"

        CONNECTIONS=$(docker-compose -f $COMPOSE_FILE exec -T postgres psql -U mgmt_user -d mgmt_db -t -c "SELECT count(*) FROM pg_stat_activity;" 2>/dev/null | xargs)
        echo "  当前连接数: $CONNECTIONS"
    fi
else
    error "PostgreSQL 未就绪或无法连接"
    OVERALL_STATUS=1
fi

# 检查 Redis
log "检查 Redis..."
if docker-compose -f $COMPOSE_FILE exec -T redis redis-cli ping 2>/dev/null | grep -q PONG; then
    success "Redis 正在运行"

    if [ "$VERBOSE" = "true" ]; then
        REDIS_VERSION=$(docker-compose -f $COMPOSE_FILE exec -T redis redis-cli info server | grep "redis_version" | cut -d: -f2 | tr -d '\r')
        echo "  版本: $REDIS_VERSION"

        MEMORY_USAGE=$(docker-compose -f $COMPOSE_FILE exec -T redis redis-cli info memory | grep "used_memory_human" | cut -d: -f2 | tr -d '\r')
        echo "  内存使用: $MEMORY_USAGE"
    fi
else
    error "Redis 未就绪或无法连接"
    OVERALL_STATUS=1
fi

# 检查后端服务
log "检查后端API..."
BACKEND_URL="$BASE_URL:$API_PORT/api/actuator/health"

if curl -f -s --connect-timeout 10 --max-time 30 "$BACKEND_URL" > /dev/null 2>&1; then
    success "后端API健康检查通过"

    if [ "$VERBOSE" = "true" ]; then
        HEALTH_RESPONSE=$(curl -s "$BACKEND_URL" 2>/dev/null)
        echo "  健康状态: $HEALTH_RESPONSE"

        # 检查特定端点
        log "检查API端点..."

        # 检查登录端点
        if curl -f -s --connect-timeout 5 "$BASE_URL:$API_PORT/api/auth/login" -X POST -H "Content-Type: application/json" -d '{}' > /dev/null 2>&1; then
            success "  登录端点可访问"
        else
            warning "  登录端点响应异常"
        fi
    fi
else
    error "后端API健康检查失败"
    OVERALL_STATUS=1
fi

# 检查前端服务
log "检查前端服务..."
FRONTEND_URL="$BASE_URL:$FRONTEND_PORT"

if curl -f -s --connect-timeout 10 --max-time 30 "$FRONTEND_URL" > /dev/null 2>&1; then
    success "前端服务可访问"

    if [ "$VERBOSE" = "true" ]; then
        HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$FRONTEND_URL")
        echo "  HTTP状态码: $HTTP_STATUS"

        RESPONSE_TIME=$(curl -s -o /dev/null -w "%{time_total}" "$FRONTEND_URL")
        echo "  响应时间: ${RESPONSE_TIME}s"
    fi
else
    error "前端服务不可访问"
    OVERALL_STATUS=1
fi

# 检查Nginx（生产环境）
if [ "$ENV" = "prod" ]; then
    log "检查Nginx..."

    if curl -f -s --connect-timeout 5 "$BASE_URL/health" > /dev/null 2>&1; then
        success "Nginx健康检查通过"
    else
        error "Nginx健康检查失败"
        OVERALL_STATUS=1
    fi
fi

# 系统资源检查
log "检查系统资源..."

# 磁盘空间
DISK_USAGE=$(df -h / | awk 'NR==2 {print $5}' | sed 's/%//')
if [ "$DISK_USAGE" -lt 85 ]; then
    success "磁盘空间充足 (${DISK_USAGE}% 已使用)"
elif [ "$DISK_USAGE" -lt 95 ]; then
    warning "磁盘空间不足 (${DISK_USAGE}% 已使用)"
else
    error "磁盘空间严重不足 (${DISK_USAGE}% 已使用)"
    OVERALL_STATUS=1
fi

# 内存使用
if command -v free &> /dev/null; then
    MEMORY_USAGE=$(free | grep Mem | awk '{printf "%.0f", $3/$2 * 100.0}')
    if [ "$MEMORY_USAGE" -lt 80 ]; then
        success "内存使用正常 (${MEMORY_USAGE}% 已使用)"
    elif [ "$MEMORY_USAGE" -lt 90 ]; then
        warning "内存使用偏高 (${MEMORY_USAGE}% 已使用)"
    else
        error "内存使用过高 (${MEMORY_USAGE}% 已使用)"
        OVERALL_STATUS=1
    fi
fi

# Docker资源检查
log "检查Docker资源..."
if [ "$VERBOSE" = "true" ]; then
    echo "容器资源使用:"
    docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}"
fi

# 网络连通性检查
log "检查网络连通性..."
if [ "$ENV" = "prod" ]; then
    # 检查外网连通性
    if ping -c 1 8.8.8.8 > /dev/null 2>&1; then
        success "外网连通性正常"
    else
        warning "外网连通性异常"
    fi
fi

# 运行TypeScript健康检查（如果存在）
if [ -f "monitoring/health-check.ts" ] && command -v node &> /dev/null; then
    log "执行详细健康检查..."
    if node monitoring/health-check.ts; then
        success "详细健康检查通过"
    else
        warning "详细健康检查发现问题"
    fi
fi

echo ""
echo "============================================"

if [ $OVERALL_STATUS -eq 0 ]; then
    success "所有服务健康检查通过!"
    echo "系统运行正常，可以提供服务。"
else
    error "发现服务异常!"
    echo "请检查上述错误并进行修复。"
fi

echo "健康检查完成于: $(date)"
exit $OVERALL_STATUS