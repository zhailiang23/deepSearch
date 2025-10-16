#!/bin/bash

# 初始化热门话题拼音数据脚本
# 为已存在的hot_topics记录生成pinyin和pinyin_first_letter字段

# 数据库连接配置（从环境变量或默认值获取）
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-mgmt_db}"
DB_USER="${DB_USER:-mgmt_user}"
DB_PASSWORD="${DB_PASSWORD:-mgmt_password}"

echo "========================================="
echo "热门话题拼音数据初始化"
echo "========================================="
echo "数据库: $DB_HOST:$DB_PORT/$DB_NAME"
echo ""

# 检查MySQL客户端是否安装
if ! command -v mysql &> /dev/null; then
    echo "错误: 未找到mysql命令，请先安装MySQL客户端"
    exit 1
fi

# 测试数据库连接
echo "测试数据库连接..."
if ! mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" -e "SELECT 1" &> /dev/null; then
    echo "错误: 无法连接到数据库"
    exit 1
fi
echo "数据库连接成功"
echo ""

# 执行SQL更新
echo "开始初始化拼音数据..."
echo ""

# 注意：这里的拼音转换需要在应用层完成
# 此脚本仅作为示例，实际使用时建议通过Java应用程序调用PinyinUtils进行批量更新

cat << 'EOF' | mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" "$DB_NAME"
-- 查询需要更新的记录数
SELECT COUNT(*) as total_records
FROM hot_topics
WHERE pinyin IS NULL OR pinyin_first_letter IS NULL;

-- 注意：实际的拼音转换需要通过Java应用程序完成
-- 以下是示例数据，实际使用时需要根据具体数据调整

-- 示例：更新一些常见的话题拼音
-- UPDATE hot_topics SET pinyin = 'quxian', pinyin_first_letter = 'qx' WHERE name = '取现';
-- UPDATE hot_topics SET pinyin = 'zhuanzhang', pinyin_first_letter = 'zz' WHERE name = '转账';
-- UPDATE hot_topics SET pinyin = 'yue', pinyin_first_letter = 'y' WHERE name = '余额';

EOF

echo ""
echo "========================================="
echo "注意事项:"
echo "========================================="
echo "1. 此脚本仅添加了数据库字段"
echo "2. 实际的拼音转换需要通过Java应用程序完成"
echo "3. 请在应用启动后调用HotTopicService的拼音初始化方法"
echo "4. 或者通过管理接口手动触发拼音数据生成"
echo ""
echo "完成！"
