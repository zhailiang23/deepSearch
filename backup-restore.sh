#!/bin/bash

# deepSearch 数据备份和恢复脚本

set -e

# 配置
SERVER_IP="192.168.153.111"
SERVER_USER="root"
SERVER_PASSWORD="Ynet@2024"
REMOTE_DIR="/opt/deepsearch"
BACKUP_DIR="./backups"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

show_usage() {
    echo "用法: $0 [backup|restore]"
    echo ""
    echo "命令:"
    echo "  backup   - 从服务器备份MySQL和Elasticsearch数据"
    echo "  restore  - 将本地备份恢复到服务器"
    echo ""
    echo "示例:"
    echo "  $0 backup   # 备份数据"
    echo "  $0 restore  # 恢复数据"
}

backup_data() {
    echo -e "${GREEN}开始备份数据...${NC}"

    # 创建备份目录
    TIMESTAMP=$(date +%Y%m%d_%H%M%S)
    BACKUP_PATH="${BACKUP_DIR}/${TIMESTAMP}"
    mkdir -p "${BACKUP_PATH}"

    # 备份MySQL
    echo -e "${YELLOW}备份MySQL数据...${NC}"
    sshpass -p "${SERVER_PASSWORD}" ssh -o StrictHostKeyChecking=no ${SERVER_USER}@${SERVER_IP} \
        "docker exec deepsearch-mysql-prod mysqldump -u root -pYnet@2024Root mgmt_db > /tmp/mysql_backup.sql"

    sshpass -p "${SERVER_PASSWORD}" scp -o StrictHostKeyChecking=no \
        ${SERVER_USER}@${SERVER_IP}:/tmp/mysql_backup.sql "${BACKUP_PATH}/"

    sshpass -p "${SERVER_PASSWORD}" ssh -o StrictHostKeyChecking=no ${SERVER_USER}@${SERVER_IP} \
        "rm /tmp/mysql_backup.sql"

    echo -e "${GREEN}✓ MySQL备份完成: ${BACKUP_PATH}/mysql_backup.sql${NC}"

    # 备份Elasticsearch数据
    echo -e "${YELLOW}备份Elasticsearch数据...${NC}"
    sshpass -p "${SERVER_PASSWORD}" ssh -o StrictHostKeyChecking=no ${SERVER_USER}@${SERVER_IP} << ENDSSH
cd ${REMOTE_DIR}

# 创建ES快照仓库
curl -X PUT "localhost:9200/_snapshot/backup_repo" -H 'Content-Type: application/json' -d'
{
  "type": "fs",
  "settings": {
    "location": "/tmp/es_backup"
  }
}'

# 创建快照
curl -X PUT "localhost:9200/_snapshot/backup_repo/snapshot_${TIMESTAMP}?wait_for_completion=true"

# 打包快照数据
docker exec deepsearch-elasticsearch-prod tar czf /tmp/es_backup.tar.gz -C /tmp es_backup
ENDSSH

    sshpass -p "${SERVER_PASSWORD}" scp -o StrictHostKeyChecking=no \
        ${SERVER_USER}@${SERVER_IP}:/tmp/es_backup.tar.gz "${BACKUP_PATH}/"

    sshpass -p "${SERVER_PASSWORD}" ssh -o StrictHostKeyChecking=no ${SERVER_USER}@${SERVER_IP} \
        "rm /tmp/es_backup.tar.gz"

    echo -e "${GREEN}✓ Elasticsearch备份完成: ${BACKUP_PATH}/es_backup.tar.gz${NC}"

    # 创建备份信息文件
    cat > "${BACKUP_PATH}/backup_info.txt" << EOF
备份时间: $(date)
服务器: ${SERVER_IP}
MySQL备份: mysql_backup.sql
ES备份: es_backup.tar.gz
EOF

    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}备份完成!${NC}"
    echo -e "${GREEN}备份位置: ${BACKUP_PATH}${NC}"
    echo -e "${GREEN}========================================${NC}"
}

restore_data() {
    echo -e "${YELLOW}请选择要恢复的备份:${NC}"

    if [ ! -d "${BACKUP_DIR}" ] || [ -z "$(ls -A ${BACKUP_DIR})" ]; then
        echo -e "${RED}未找到备份文件${NC}"
        exit 1
    fi

    # 列出可用备份
    backups=($(ls -d ${BACKUP_DIR}/*/ 2>/dev/null | xargs -n 1 basename))

    if [ ${#backups[@]} -eq 0 ]; then
        echo -e "${RED}未找到备份${NC}"
        exit 1
    fi

    echo "可用备份:"
    for i in "${!backups[@]}"; do
        echo "  $((i+1)). ${backups[$i]}"
    done

    read -p "请选择备份编号 (1-${#backups[@]}): " choice

    if ! [[ "$choice" =~ ^[0-9]+$ ]] || [ "$choice" -lt 1 ] || [ "$choice" -gt ${#backups[@]} ]; then
        echo -e "${RED}无效的选择${NC}"
        exit 1
    fi

    SELECTED_BACKUP="${backups[$((choice-1))]}"
    RESTORE_PATH="${BACKUP_DIR}/${SELECTED_BACKUP}"

    echo -e "${YELLOW}将恢复备份: ${SELECTED_BACKUP}${NC}"
    read -p "确认恢复? (yes/no): " confirm

    if [ "$confirm" != "yes" ]; then
        echo "取消恢复"
        exit 0
    fi

    echo -e "${GREEN}开始恢复数据...${NC}"

    # 上传备份文件到服务器
    echo -e "${YELLOW}上传备份文件...${NC}"
    sshpass -p "${SERVER_PASSWORD}" scp -o StrictHostKeyChecking=no \
        "${RESTORE_PATH}/mysql_backup.sql" \
        ${SERVER_USER}@${SERVER_IP}:/tmp/

    sshpass -p "${SERVER_PASSWORD}" scp -o StrictHostKeyChecking=no \
        "${RESTORE_PATH}/es_backup.tar.gz" \
        ${SERVER_USER}@${SERVER_IP}:/tmp/

    # 恢复MySQL
    echo -e "${YELLOW}恢复MySQL数据...${NC}"
    sshpass -p "${SERVER_PASSWORD}" ssh -o StrictHostKeyChecking=no ${SERVER_USER}@${SERVER_IP} \
        "docker exec -i deepsearch-mysql-prod mysql -u root -pYnet@2024Root mgmt_db < /tmp/mysql_backup.sql"

    echo -e "${GREEN}✓ MySQL恢复完成${NC}"

    # 恢复Elasticsearch
    echo -e "${YELLOW}恢复Elasticsearch数据...${NC}"
    sshpass -p "${SERVER_PASSWORD}" ssh -o StrictHostKeyChecking=no ${SERVER_USER}@${SERVER_IP} << ENDSSH
# 解压ES备份
docker exec deepsearch-elasticsearch-prod tar xzf /tmp/es_backup.tar.gz -C /tmp

# 恢复快照
curl -X POST "localhost:9200/_snapshot/backup_repo/snapshot_*/_restore?wait_for_completion=true"

# 清理
rm /tmp/mysql_backup.sql /tmp/es_backup.tar.gz
ENDSSH

    echo -e "${GREEN}✓ Elasticsearch恢复完成${NC}"

    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}数据恢复完成!${NC}"
    echo -e "${GREEN}========================================${NC}"
}

# 主逻辑
if [ $# -eq 0 ]; then
    show_usage
    exit 1
fi

case "$1" in
    backup)
        backup_data
        ;;
    restore)
        restore_data
        ;;
    *)
        show_usage
        exit 1
        ;;
esac
