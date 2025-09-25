# 基于官方Elasticsearch 8.11.0镜像
FROM docker.elastic.co/elasticsearch/elasticsearch:8.11.0

# 使用root用户安装插件
USER root

# 安装拼音分析插件
# 先尝试从INFINI Labs下载8.11.0版本，如果不存在则尝试8.4.1版本
RUN bin/elasticsearch-plugin install --batch https://get.infini.cloud/elasticsearch/analysis-pinyin/8.11.0 || \
    bin/elasticsearch-plugin install --batch https://get.infini.cloud/elasticsearch/analysis-pinyin/8.4.1

# 创建配置目录
RUN mkdir -p /usr/share/elasticsearch/config/analysis

# 切换回elasticsearch用户
USER elasticsearch

# 验证插件安装
RUN bin/elasticsearch-plugin list