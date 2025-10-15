#!/bin/zsh

# 为已有的 Elasticsearch 索引添加审核状态字段 (audit_status)
# 并将所有文档的审核状态值设置为 "available"(可用)

# Elasticsearch 连接配置
ES_HOST="${ES_HOST:-localhost}"
ES_PORT="${ES_PORT:-9200}"
ES_URL="http://${ES_HOST}:${ES_PORT}"

# 需要更新的索引列表
INDICES=("function" "information" "product" "activity")

# 审核状态字段名称
STATUS_FIELD="audit_status"

# 审核状态字段的默认值
DEFAULT_STATUS="available"

echo "=========================================="
echo "为 Elasticsearch 索引添加审核状态字段"
echo "ES URL: ${ES_URL}"
echo "索引列表: ${INDICES[@]}"
echo "字段名称: ${STATUS_FIELD}"
echo "默认状态: ${DEFAULT_STATUS}"
echo "=========================================="
echo

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查 Elasticsearch 是否可访问
check_elasticsearch() {
    echo -n "检查 Elasticsearch 连接状态... "
    if curl -s -f "${ES_URL}/_cluster/health" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ 连接成功${NC}"
        return 0
    else
        echo -e "${RED}✗ 连接失败${NC}"
        echo "错误: 无法连接到 Elasticsearch (${ES_URL})"
        echo "请确保:"
        echo "1. Elasticsearch 正在运行"
        echo "2. 端口 ${ES_PORT} 未被占用"
        echo "3. Docker 容器正常运行 (如使用 Docker)"
        return 1
    fi
}

# 检查索引是否存在
check_index_exists() {
    local index_name=$1
    if curl -s -f "${ES_URL}/${index_name}" > /dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

# 获取索引的文档数量
get_document_count() {
    local index_name=$1
    local count=$(curl -s "${ES_URL}/${index_name}/_count" | grep -o '"count":[0-9]*' | grep -o '[0-9]*')
    echo $count
}

# 为索引添加审核状态字段映射
add_status_field_mapping() {
    local index_name=$1

    echo -n "  为索引 ${index_name} 添加审核状态字段映射... "

    # 使用 PUT mapping API 添加新字段
    local response=$(curl -s -w "\n%{http_code}" -X PUT "${ES_URL}/${index_name}/_mapping" \
        -H 'Content-Type: application/json' \
        -d "{
            \"properties\": {
                \"${STATUS_FIELD}\": {
                    \"type\": \"keyword\",
                    \"index\": true,
                    \"doc_values\": true
                }
            }
        }")

    local http_code=$(echo "$response" | tail -n 1)
    local response_body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "200" ]; then
        echo -e "${GREEN}✓ 成功${NC}"
        return 0
    else
        echo -e "${RED}✗ 失败${NC}"
        echo "  HTTP Code: $http_code"
        echo "  Response: $response_body"
        return 1
    fi
}

# 更新所有文档的审核状态字段值
update_all_documents() {
    local index_name=$1
    local status_value=$2

    echo -n "  更新索引 ${index_name} 的所有文档审核状态为 '${status_value}'... "

    # 使用 update_by_query API 更新所有文档
    local response=$(curl -s -w "\n%{http_code}" -X POST "${ES_URL}/${index_name}/_update_by_query?conflicts=proceed" \
        -H 'Content-Type: application/json' \
        -d "{
            \"script\": {
                \"source\": \"ctx._source.${STATUS_FIELD} = params.status\",
                \"lang\": \"painless\",
                \"params\": {
                    \"status\": \"${status_value}\"
                }
            },
            \"query\": {
                \"match_all\": {}
            }
        }")

    local http_code=$(echo "$response" | tail -n 1)
    local response_body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "200" ]; then
        # 提取更新的文档数量
        local updated=$(echo "$response_body" | grep -o '"updated":[0-9]*' | grep -o '[0-9]*')
        echo -e "${GREEN}✓ 成功 (更新了 ${updated} 个文档)${NC}"
        return 0
    else
        echo -e "${RED}✗ 失败${NC}"
        echo "  HTTP Code: $http_code"
        echo "  Response: $response_body"
        return 1
    fi
}

# 验证更新结果
verify_update() {
    local index_name=$1
    local expected_status=$2

    echo -n "  验证索引 ${index_name} 的更新结果... "

    # 查询带有审核状态字段的文档数量
    local response=$(curl -s "${ES_URL}/${index_name}/_search" \
        -H 'Content-Type: application/json' \
        -d "{
            \"size\": 0,
            \"query\": {
                \"term\": {
                    \"${STATUS_FIELD}\": \"${expected_status}\"
                }
            }
        }")

    local count=$(echo "$response" | grep -o '"total":{"value":[0-9]*' | grep -o '[0-9]*' | head -1)
    local total_docs=$(get_document_count "$index_name")

    if [ "$count" = "$total_docs" ]; then
        echo -e "${GREEN}✓ 验证通过 (${count}/${total_docs} 个文档状态正确)${NC}"
        return 0
    else
        echo -e "${YELLOW}⚠ 部分成功 (${count}/${total_docs} 个文档状态正确)${NC}"
        return 1
    fi
}

# 主流程
main() {
    # 检查 Elasticsearch 连接
    if ! check_elasticsearch; then
        exit 1
    fi

    echo

    # 统计变量
    local success_count=0
    local fail_count=0
    local skip_count=0

    # 处理每个索引
    for index_name in "${INDICES[@]}"; do
        echo "处理索引: ${index_name}"
        echo "----------------------------------------"

        # 检查索引是否存在
        if ! check_index_exists "$index_name"; then
            echo -e "  ${YELLOW}⚠ 索引不存在，跳过${NC}"
            ((skip_count++))
            echo
            continue
        fi

        # 获取文档数量
        local doc_count=$(get_document_count "$index_name")
        echo "  文档数量: ${doc_count}"

        # 添加状态字段映射
        if ! add_status_field_mapping "$index_name"; then
            echo -e "  ${RED}✗ 添加字段映射失败，跳过此索引${NC}"
            ((fail_count++))
            echo
            continue
        fi

        # 如果索引有文档，则更新所有文档
        if [ "$doc_count" -gt 0 ]; then
            if ! update_all_documents "$index_name" "$DEFAULT_STATUS"; then
                echo -e "  ${RED}✗ 更新文档失败${NC}"
                ((fail_count++))
                echo
                continue
            fi

            # 等待索引刷新
            sleep 1

            # 验证更新结果
            verify_update "$index_name" "$DEFAULT_STATUS"
        else
            echo -e "  ${YELLOW}⚠ 索引为空，无需更新文档${NC}"
        fi

        ((success_count++))
        echo -e "  ${GREEN}✓ 索引 ${index_name} 处理完成${NC}"
        echo
    done

    # 输出汇总信息
    echo "=========================================="
    echo "处理完成"
    echo "=========================================="
    echo -e "${GREEN}成功: ${success_count}${NC}"
    echo -e "${RED}失败: ${fail_count}${NC}"
    echo -e "${YELLOW}跳过: ${skip_count}${NC}"
    echo "总计: $((success_count + fail_count + skip_count))"
    echo

    if [ $fail_count -eq 0 ]; then
        echo -e "${GREEN}所有索引已成功更新！${NC}"
        return 0
    else
        echo -e "${RED}部分索引更新失败，请检查日志${NC}"
        return 1
    fi
}

# 执行主流程
main
exit $?
