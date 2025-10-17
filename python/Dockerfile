# 使用官方 Python 轻量级镜像
FROM python:3.11-slim

# 设置工作目录
WORKDIR /app

# 设置环境变量
ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1 \
    TRANSFORMERS_CACHE=/app/models \
    HF_HOME=/app/models

# 复制依赖文件
COPY requirements.txt .

# 配置pip使用清华大学镜像源加速下载
RUN pip config set global.index-url https://pypi.tuna.tsinghua.edu.cn/simple

# 安装 Python 依赖 - 关闭进度条但显示详细信息
RUN pip install --no-cache-dir --progress-bar off -v -r requirements.txt

# 复制所有应用代码
COPY main_service.py \
     clustering.py \
     siliconflow_client.py \
     embedding_service.py \
     rerank_service.py \
     ./

# 创建模型缓存目录
RUN mkdir -p /app/models

# 暴露端口(统一服务端口 - 改为5002避免与macOS ControlCenter冲突)
EXPOSE 5002

# 健康检查 - 使用Python而不是curl
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:5002/health', timeout=5)" || exit 1

# 启动统一服务
CMD ["uvicorn", "main_service:app", "--host", "0.0.0.0", "--port", "5002", "--log-level", "info"]
