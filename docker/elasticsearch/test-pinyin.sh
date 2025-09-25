#!/bin/bash

# Elasticsearch拼音插件测试脚本
# 等待Elasticsearch启动
echo "等待Elasticsearch启动..."
until curl -f http://localhost:9200/_cluster/health >/dev/null 2>&1; do
    echo "Elasticsearch还未就绪，等待5秒..."
    sleep 5
done

echo "Elasticsearch已就绪，开始测试拼音插件..."

# 1. 检查插件是否安装
echo "1. 检查已安装的插件："
curl -s http://localhost:9200/_cat/plugins?v

echo -e "\n2. 创建测试索引并配置拼音分析器："
curl -X PUT "http://localhost:9200/test_pinyin" \
  -H 'Content-Type: application/json' \
  -d @pinyin-analyzer.json

echo -e "\n3. 添加测试文档："
curl -X POST "http://localhost:9200/test_pinyin/_doc/1" \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "北京大学",
    "content": "北京大学是中国著名的高等学府",
    "description": "位于北京市海淀区"
  }'

curl -X POST "http://localhost:9200/test_pinyin/_doc/2" \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "清华大学",
    "content": "清华大学也是中国顶尖的大学",
    "description": "同样位于北京市海淀区"
  }'

curl -X POST "http://localhost:9200/test_pinyin/_doc/3" \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "上海交通大学",
    "content": "上海交通大学是上海的知名学府",
    "description": "位于上海市闵行区"
  }'

echo -e "\n4. 刷新索引："
curl -X POST "http://localhost:9200/test_pinyin/_refresh"

echo -e "\n5. 测试拼音搜索："

echo -e "\n5.1 使用全拼搜索 'beijing'："
curl -X GET "http://localhost:9200/test_pinyin/_search" \
  -H 'Content-Type: application/json' \
  -d '{
    "query": {
      "match": {
        "title": "beijing"
      }
    }
  }' | python3 -m json.tool

echo -e "\n5.2 使用首字母搜索 'bjdx'："
curl -X GET "http://localhost:9200/test_pinyin/_search" \
  -H 'Content-Type: application/json' \
  -d '{
    "query": {
      "match": {
        "title.first_letter": "bjdx"
      }
    }
  }' | python3 -m json.tool

echo -e "\n5.3 使用混合拼音搜索 'qinghua'："
curl -X GET "http://localhost:9200/test_pinyin/_search" \
  -H 'Content-Type: application/json' \
  -d '{
    "query": {
      "match": {
        "title": "qinghua"
      }
    }
  }' | python3 -m json.tool

echo -e "\n6. 测试拼音分析器："
echo -e "\n6.1 分析 '北京大学'："
curl -X POST "http://localhost:9200/test_pinyin/_analyze" \
  -H 'Content-Type: application/json' \
  -d '{
    "analyzer": "pinyin_analyzer",
    "text": "北京大学"
  }' | python3 -m json.tool

echo -e "\n6.2 分析首字母 '北京大学'："
curl -X POST "http://localhost:9200/test_pinyin/_analyze" \
  -H 'Content-Type: application/json' \
  -d '{
    "analyzer": "pinyin_first_letter_analyzer",
    "text": "北京大学"
  }' | python3 -m json.tool

echo -e "\n测试完成!"