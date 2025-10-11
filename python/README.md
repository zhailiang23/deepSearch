# Python 统一微服务

本目录包含一个统一的 Python 微服务,整合了三个功能模块:

1. **嵌入服务 (Embedding Service)** - 文本向量化服务
2. **重排服务 (Rerank Service)** - 文档重排序服务
3. **聚类服务 (Cluster Service)** - 文本聚类分析服务

## 架构说明

所有服务已整合到 `main_service.py` 中,通过不同的路径前缀访问不同的功能:
- `/embedding/*` - 嵌入服务相关接口
- `/rerank/*` - 重排服务相关接口
- `/cluster/*` - 聚类服务相关接口

**统一端口**: 5001 (本地开发) 或 5000 (Docker)

## 服务说明

### 1. 嵌入服务 (embedding_service.py)

**功能**: 使用 sentence-transformers 提供本地文本向量化服务,替代硅基流动的在线 Embedding API。

**端口**: 8001

**API 端点**:
- `POST /v1/embeddings` - 生成文本向量 (兼容 OpenAI API 格式)
- `POST /api/embeddings` - 生成文本向量 (备用端点)
- `POST /config` - 预加载指定模型
- `GET /health` - 健康检查
- `GET /` - 服务信息
- `GET /docs` - API 文档

**请求示例**:
```bash
curl -X POST http://localhost:8001/v1/embeddings \
  -H "Content-Type: application/json" \
  -d '{
    "input": ["你好", "世界"],
    "model": "BAAI/bge-large-zh-v1.5"
  }'
```

**支持的模型**:
- `BAAI/bge-large-zh-v1.5` (中文,1024维)
- `sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2` (多语言,384维)
- 任何 HuggingFace 上的 sentence-transformers 模型

### 2. 重排服务 (rerank_service.py)

**功能**: 使用 sentence-transformers CrossEncoder 提供本地文本重排序服务,替代硅基流动的在线 Rerank API。

**端口**: 8002

**API 端点**:
- `POST /v1/rerank` - 文档重排序 (兼容硅基流动 API 格式)
- `POST /api/rerank` - 文档重排序 (备用端点)
- `POST /config` - 预加载指定模型
- `GET /health` - 健康检查
- `GET /` - 服务信息
- `GET /docs` - API 文档

**请求示例**:
```bash
curl -X POST http://localhost:8002/v1/rerank \
  -H "Content-Type: application/json" \
  -d '{
    "model": "BAAI/bge-reranker-v2-m3",
    "query": "如何转账",
    "documents": ["转账流程说明", "信用卡申请", "余额查询"],
    "top_n": 2,
    "return_documents": true
  }'
```

**支持的模型**:
- `BAAI/bge-reranker-v2-m3` (中文/多语言)
- `cross-encoder/ms-marco-MiniLM-L-6-v2` (英文)
- 任何 HuggingFace 上的 CrossEncoder 模型

### 3. 聚类服务 (cluster_api.py)

**功能**: 提供基于硅基流动 API 的文本聚类分析服务(可配置使用本地服务)。

**端口**: 5001

**API 端点**:
- `POST /api/cluster` - 执行聚类分析
- `GET /health` - 健康检查
- `GET /` - 服务信息

## 本地开发

### 环境准备

使用 uv 管理 Python 环境:

```bash
cd python

# 创建虚拟环境
uv venv

# 激活虚拟环境
source .venv/bin/activate

# 安装依赖
uv pip install -r requirements.txt
```

### 启动服务

**统一服务**(推荐):
```bash
uv run uvicorn main_service:app --host 0.0.0.0 --port 5001
```

**单独服务**(仅用于开发调试):
```bash
# 嵌入服务
uv run uvicorn embedding_service:app --host 0.0.0.0 --port 8001

# 重排服务
uv run uvicorn rerank_service:app --host 0.0.0.0 --port 8002

# 聚类服务
uv run uvicorn cluster_api:app --host 0.0.0.0 --port 5002
```

## Docker 部署

### 构建镜像

**统一服务**:
```bash
cd python
docker build -t python-service:latest .
```

### 运行容器

**统一服务**:
```bash
docker run -d \
  -p 5000:5000 \
  -v python_models:/app/models \
  --name python-service \
  python-service:latest
```

### 使用 Docker Compose

在项目根目录运行:

```bash
# 启动所有服务(包括Python统一服务)
docker-compose up -d

# 只启动Python服务
docker-compose up -d python-service

# 查看日志
docker-compose logs -f python-service

# 停止服务
docker-compose down
```

### 访问服务

- 统一服务: http://localhost:5000
- API文档: http://localhost:5000/docs
- 健康检查: http://localhost:5000/health

## 离线部署

### 模型预下载

在有网络的环境中预下载模型:

```python
from sentence_transformers import SentenceTransformer, CrossEncoder

# 下载嵌入模型
embedding_model = SentenceTransformer("BAAI/bge-large-zh-v1.5")
embedding_model.save("/path/to/models/bge-large-zh-v1.5")

# 下载重排模型
rerank_model = CrossEncoder("BAAI/bge-reranker-v2-m3")
rerank_model.save("/path/to/models/bge-reranker-v2-m3")
```

### 离线使用

1. 将下载的模型目录复制到离线环境
2. 启动服务时使用本地模型路径:

```bash
# 使用环境变量指定模型路径
export TRANSFORMERS_OFFLINE=1
export HF_HUB_OFFLINE=1

# 启动服务
uv run uvicorn embedding_service:app --host 0.0.0.0 --port 8001
```

3. 或在 Docker 中挂载模型目录:

```bash
docker run -d \
  -p 8001:8001 \
  -v /path/to/models:/app/models \
  -e TRANSFORMERS_OFFLINE=1 \
  -e HF_HUB_OFFLINE=1 \
  embedding-service:latest
```

## 配置后端使用本地服务

修改 `backend/src/main/resources/application.yml`:

### 嵌入服务配置

```yaml
semantic:
  embedding:
    # 使用本地服务
    provider: local
    enabled: true
    timeout: 60000

    # 本地服务配置
    local:
      service:
        url: http://localhost:5001/embedding  # 或 http://python-service:5000/embedding (Docker)
      model: BAAI/bge-large-zh-v1.5
      dimension: 1024
```

### 重排服务配置

```yaml
rerank:
  # 使用本地服务
  provider: local
  enabled: true

  # 本地服务配置
  local:
    service:
      url: http://localhost:5001/rerank  # 或 http://python-service:5000/rerank (Docker)
    model: BAAI/bge-reranker-v2-m3
    timeout: 5000
    default-top-n: 50
```

### 聚类服务配置

```yaml
cluster:
  # Python聚类服务URL
  python-service-url: http://localhost:5001/cluster  # 或 http://python-service:5000/cluster (Docker)
```

## 性能优化

### 模型选择建议

**嵌入模型**:
- 生产环境: `BAAI/bge-large-zh-v1.5` (高精度,1024维)
- 测试环境: `sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2` (快速,384维)

**重排模型**:
- 中文环境: `BAAI/bge-reranker-v2-m3`
- 英文环境: `cross-encoder/ms-marco-MiniLM-L-6-v2`

### 资源配置

建议 Docker 容器资源配置:

- **统一Python服务**: 4GB 内存 (包含嵌入、重排和聚类功能)
- 如果只使用单一功能,可以降低到 2GB

### GPU 加速

如果有 GPU 可用,模型会自动使用 GPU 加速。确保:
1. 安装 CUDA 和 cuDNN
2. 安装支持 GPU 的 PyTorch: `pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu118`

## 故障排查

### 常见问题

1. **模型加载失败**
   - 检查网络连接(首次下载需要访问 HuggingFace)
   - 检查磁盘空间(模型文件较大)
   - 查看日志: `docker-compose logs -f semantic-embedding`

2. **内存不足**
   - 增加 Docker 容器内存限制
   - 使用更小的模型
   - 减少批处理大小

3. **服务启动慢**
   - 首次启动需要下载模型,耐心等待
   - 使用预下载的模型
   - 检查健康检查超时设置

### 日志查看

```bash
# Docker 日志
docker-compose logs -f python-service

# 本地日志(如果配置了日志文件)
tail -f logs/python_service.log
```

## API 文档

服务启动后访问:
- 统一服务API文档: http://localhost:5001/docs (本地) 或 http://localhost:5000/docs (Docker)

## 依赖项

主要依赖:
- `fastapi` - Web 框架
- `uvicorn` - ASGI 服务器
- `sentence-transformers` - 文本嵌入和重排
- `torch` - 深度学习框架
- `numpy` - 数值计算
- `scikit-learn` - 机器学习(聚类服务)
- `umap-learn` - 降维(聚类服务)

完整依赖列表见 `requirements.txt`。
