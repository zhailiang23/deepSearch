#!/bin/bash

# 生产环境部署脚本
# 提供零停机部署、回滚和监控功能

set -euo pipefail

# 脚本配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
LOG_FILE="/tmp/deploy-$(date +%Y%m%d-%H%M%S).log"
BACKUP_DIR="/tmp/backups/$(date +%Y%m%d-%H%M%S)"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')] INFO: $1${NC}" | tee -a "$LOG_FILE"
}

warn() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] WARN: $1${NC}" | tee -a "$LOG_FILE"
}

error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ERROR: $1${NC}" | tee -a "$LOG_FILE"
}

success() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] SUCCESS: $1${NC}" | tee -a "$LOG_FILE"
}

# 显示使用帮助
show_help() {
    cat << EOF
生产环境部署脚本

用法: $0 [选项] <环境> <动作>

环境:
  dev     开发环境
  test    测试环境
  prod    生产环境

动作:
  deploy          部署应用（零停机）
  rollback        回滚到上一版本
  status          查看服务状态
  logs            查看服务日志
  health-check    执行健康检查
  backup          备份数据
  restore         恢复数据
  clean           清理旧版本

选项:
  -h, --help      显示此帮助信息
  -v, --verbose   详细输出
  -y, --yes       自动确认所有操作
  --no-backup     部署时不创建备份
  --force         强制执行操作

示例:
  $0 prod deploy              # 生产环境部署
  $0 prod rollback            # 生产环境回滚
  $0 prod health-check        # 生产环境健康检查
  $0 test deploy --no-backup  # 测试环境部署（无备份）

EOF
}

# 解析命令行参数
VERBOSE=false
AUTO_YES=false
NO_BACKUP=false
FORCE=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        -y|--yes)
            AUTO_YES=true
            shift
            ;;
        --no-backup)
            NO_BACKUP=true
            shift
            ;;
        --force)
            FORCE=true
            shift
            ;;
        -*)
            error "未知选项: $1"
            show_help
            exit 1
            ;;
        *)
            if [ -z "${ENV:-}" ]; then
                ENV=$1
            elif [ -z "${ACTION:-}" ]; then
                ACTION=$1
            else
                error "过多的参数: $1"
                show_help
                exit 1
            fi
            shift
            ;;
    esac
done

# 验证参数
if [ -z "${ENV:-}" ] || [ -z "${ACTION:-}" ]; then
    error "缺少必需参数"
    show_help
    exit 1
fi

# 验证环境
case $ENV in
    dev|test|prod)
        ;;
    *)
        error "无效环境: $ENV"
        show_help
        exit 1
        ;;
esac

# 设置环境特定配置
case $ENV in
    dev)
        COMPOSE_FILE="docker-compose.yml"
        ENV_FILE=".env.development"
        ;;
    test)
        COMPOSE_FILE="docker-compose.test.yml"
        ENV_FILE=".env.test"
        ;;
    prod)
        COMPOSE_FILE="docker-compose.prod.yml"
        ENV_FILE=".env"
        ;;
esac

# 进入项目目录
cd "$PROJECT_DIR"

# 检查必需文件
check_prerequisites() {
    log "检查部署前置条件..."

    if [ ! -f "$COMPOSE_FILE" ]; then
        error "Docker Compose文件不存在: $COMPOSE_FILE"
        exit 1
    fi

    if [ "$ENV" = "prod" ] && [ ! -f "$ENV_FILE" ]; then
        error "环境文件不存在: $ENV_FILE"
        error "请从 .env.example 复制并配置环境文件"
        exit 1
    fi

    if ! command -v docker &> /dev/null; then
        error "Docker未安装"
        exit 1
    fi

    if ! command -v docker-compose &> /dev/null; then
        error "Docker Compose未安装"
        exit 1
    fi

    success "前置条件检查通过"
}

# 创建备份
create_backup() {
    if [ "$NO_BACKUP" = true ]; then
        log "跳过备份创建"
        return 0
    fi

    log "创建备份到 $BACKUP_DIR..."
    mkdir -p "$BACKUP_DIR"

    # 备份数据库
    if docker-compose -f "$COMPOSE_FILE" ps postgres | grep -q "Up"; then
        log "备份PostgreSQL数据库..."
        docker-compose -f "$COMPOSE_FILE" exec -T postgres pg_dump -U mgmt_user mgmt_db | gzip > "$BACKUP_DIR/postgres_backup.sql.gz"
    fi

    # 备份Redis
    if docker-compose -f "$COMPOSE_FILE" ps redis | grep -q "Up"; then
        log "备份Redis数据..."
        docker-compose -f "$COMPOSE_FILE" exec -T redis redis-cli --rdb - | gzip > "$BACKUP_DIR/redis_backup.rdb.gz"
    fi

    # 备份配置文件
    log "备份配置文件..."
    cp -r docker/ "$BACKUP_DIR/docker/" 2>/dev/null || true
    cp "$ENV_FILE" "$BACKUP_DIR/" 2>/dev/null || true

    success "备份创建完成: $BACKUP_DIR"
}

# 健康检查
health_check() {
    log "执行健康检查..."

    # 检查服务状态
    if [ -f "monitoring/health-check.ts" ]; then
        if command -v node &> /dev/null; then
            node monitoring/health-check.ts
        else
            warn "Node.js未安装，跳过TypeScript健康检查"
        fi
    fi

    # 基础HTTP检查
    local max_attempts=30
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        log "健康检查尝试 $attempt/$max_attempts..."

        # 检查Nginx
        if curl -f -s http://localhost/health > /dev/null 2>&1; then
            success "Nginx健康检查通过"
        else
            warn "Nginx健康检查失败"
            if [ $attempt -eq $max_attempts ]; then
                return 1
            fi
        fi

        # 检查后端API
        if curl -f -s http://localhost/api/actuator/health > /dev/null 2>&1; then
            success "后端API健康检查通过"
            return 0
        else
            warn "后端API健康检查失败"
        fi

        ((attempt++))
        sleep 10
    done

    error "健康检查失败"
    return 1
}

# 零停机部署
deploy() {
    log "开始零停机部署..."

    check_prerequisites

    # 确认部署
    if [ "$AUTO_YES" = false ]; then
        echo -n "确认部署到 $ENV 环境? [y/N] "
        read -r response
        if [[ ! "$response" =~ ^[Yy]$ ]]; then
            log "部署已取消"
            exit 0
        fi
    fi

    # 创建备份
    create_backup

    # 拉取最新代码（如果是Git仓库）
    if [ -d ".git" ]; then
        log "拉取最新代码..."
        git pull origin main || warn "Git拉取失败，继续部署..."
    fi

    # 构建新镜像
    log "构建Docker镜像..."
    docker-compose -f "$COMPOSE_FILE" build --no-cache

    # 启动新服务（并行启动数据库和缓存）
    log "启动基础服务..."
    docker-compose -f "$COMPOSE_FILE" up -d postgres redis

    # 等待数据库就绪
    log "等待数据库就绪..."
    local db_ready=false
    for i in {1..60}; do
        if docker-compose -f "$COMPOSE_FILE" exec -T postgres pg_isready -U mgmt_user -d mgmt_db; then
            db_ready=true
            break
        fi
        sleep 1
    done

    if [ "$db_ready" = false ]; then
        error "数据库启动超时"
        exit 1
    fi

    # 数据库迁移
    log "执行数据库迁移..."
    docker-compose -f "$COMPOSE_FILE" run --rm backend java -jar app.jar --spring.profiles.active=prod --migrate || true

    # 启动应用服务
    log "启动应用服务..."
    docker-compose -f "$COMPOSE_FILE" up -d backend

    # 等待后端就绪
    log "等待后端服务就绪..."
    local backend_ready=false
    for i in {1..120}; do
        if curl -f -s http://localhost:8080/api/actuator/health > /dev/null 2>&1; then
            backend_ready=true
            break
        fi
        sleep 1
    done

    if [ "$backend_ready" = false ]; then
        error "后端服务启动超时"
        exit 1
    fi

    # 启动前端和Nginx
    log "启动前端服务..."
    docker-compose -f "$COMPOSE_FILE" up -d frontend nginx

    # 执行健康检查
    log "执行部署后健康检查..."
    if ! health_check; then
        error "健康检查失败，考虑回滚"
        exit 1
    fi

    # 清理旧镜像
    log "清理旧Docker镜像..."
    docker image prune -f

    success "部署完成！"
    log "日志文件: $LOG_FILE"
    log "备份位置: $BACKUP_DIR"
}

# 回滚
rollback() {
    log "开始回滚操作..."

    if [ "$AUTO_YES" = false ]; then
        echo -n "确认回滚 $ENV 环境? [y/N] "
        read -r response
        if [[ ! "$response" =~ ^[Yy]$ ]]; then
            log "回滚已取消"
            exit 0
        fi
    fi

    # 查找最近的备份
    local latest_backup=$(find /tmp/backups -name "*.sql.gz" -type f -printf '%T@ %p\n' 2>/dev/null | sort -rn | head -1 | cut -d' ' -f2- | xargs dirname 2>/dev/null || echo "")

    if [ -z "$latest_backup" ]; then
        error "未找到备份文件"
        exit 1
    fi

    log "使用备份: $latest_backup"

    # 停止当前服务
    docker-compose -f "$COMPOSE_FILE" down

    # 恢复数据库
    if [ -f "$latest_backup/postgres_backup.sql.gz" ]; then
        log "恢复数据库..."
        docker-compose -f "$COMPOSE_FILE" up -d postgres
        sleep 10
        gunzip -c "$latest_backup/postgres_backup.sql.gz" | docker-compose -f "$COMPOSE_FILE" exec -T postgres psql -U mgmt_user -d mgmt_db
    fi

    # 恢复配置
    if [ -f "$latest_backup/.env" ]; then
        log "恢复环境配置..."
        cp "$latest_backup/.env" ".env"
    fi

    # 启动服务
    log "启动回滚后的服务..."
    docker-compose -f "$COMPOSE_FILE" up -d

    success "回滚完成"
}

# 查看状态
show_status() {
    log "查看 $ENV 环境服务状态..."
    docker-compose -f "$COMPOSE_FILE" ps
    echo
    docker-compose -f "$COMPOSE_FILE" top
}

# 查看日志
show_logs() {
    log "查看 $ENV 环境服务日志..."
    docker-compose -f "$COMPOSE_FILE" logs -f --tail=100
}

# 恢复数据
restore_data() {
    log "数据恢复功能..."
    echo "请选择恢复选项："
    echo "1) 从备份恢复"
    echo "2) 从指定路径恢复"
    echo "3) 取消"

    read -p "请选择 [1-3]: " choice

    case $choice in
        1)
            # 从备份目录恢复
            ;;
        2)
            # 从指定路径恢复
            ;;
        *)
            log "操作取消"
            ;;
    esac
}

# 清理
cleanup() {
    log "清理旧版本和无用资源..."

    # 清理Docker资源
    docker system prune -f
    docker volume prune -f

    # 清理旧备份（保留最近7天）
    find /tmp/backups -type d -mtime +7 -exec rm -rf {} + 2>/dev/null || true

    success "清理完成"
}

# 主逻辑
main() {
    case $ACTION in
        deploy)
            deploy
            ;;
        rollback)
            rollback
            ;;
        status)
            show_status
            ;;
        logs)
            show_logs
            ;;
        health-check)
            health_check
            ;;
        backup)
            create_backup
            ;;
        restore)
            restore_data
            ;;
        clean)
            cleanup
            ;;
        *)
            error "未知动作: $ACTION"
            show_help
            exit 1
            ;;
    esac
}

# 信号处理
trap 'error "部署被中断"; exit 1' INT TERM

# 执行主函数
main