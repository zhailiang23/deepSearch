"""
统一的Python微服务
整合嵌入、重排和聚类三个服务
"""
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
import logging
import os
from sentence_transformers import SentenceTransformer, CrossEncoder
import numpy as np
from siliconflow_client import SiliconFlowClient
from clustering import perform_clustering

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# 创建 FastAPI 应用
app = FastAPI(
    title="deepSearch Python 统一服务",
    description="整合嵌入、重排和聚类功能的统一微服务",
    version="1.0.0"
)

# 全局模型实例
_embedding_model: Optional[SentenceTransformer] = None
_embedding_model_name: Optional[str] = None
_rerank_model: Optional[CrossEncoder] = None
_rerank_model_name: Optional[str] = None


# ========== 通用模型 ==========

class HealthResponse(BaseModel):
    """健康检查响应"""
    status: str = Field("healthy", description="服务状态")
    service: str = Field(..., description="服务名称")
    version: str = Field(..., description="版本号")
    modules: Dict[str, bool] = Field(..., description="模块状态")


# ========== 嵌入服务模型 ==========

class EmbeddingRequest(BaseModel):
    """嵌入请求"""
    input: List[str] = Field(..., description="待向量化的文本列表")
    model: str = Field("BAAI/bge-large-zh-v1.5", description="模型名称")
    encoding_format: str = Field("float", description="编码格式(float/base64)")


class EmbeddingData(BaseModel):
    """嵌入数据"""
    object: str = Field("embedding", description="对象类型")
    embedding: List[float] = Field(..., description="向量")
    index: int = Field(..., description="索引")


class EmbeddingResponse(BaseModel):
    """嵌入响应"""
    object: str = Field("list", description="对象类型")
    data: List[EmbeddingData] = Field(..., description="嵌入数据列表")
    model: str = Field(..., description="模型名称")
    usage: dict = Field(..., description="使用量统计")


# ========== 重排服务模型 ==========

class RerankRequest(BaseModel):
    """重排请求"""
    model: str = Field("BAAI/bge-reranker-v2-m3", description="模型名称")
    query: str = Field(..., description="查询文本")
    documents: List[str] = Field(..., description="候选文档列表")
    top_n: Optional[int] = Field(None, description="返回前N个结果")
    return_documents: bool = Field(True, description="是否返回文档内容")


class RerankResult(BaseModel):
    """重排结果"""
    index: int = Field(..., description="原始索引")
    relevance_score: float = Field(..., description="相关性分数")
    document: Optional[str] = Field(None, description="文档内容")


class RerankResponse(BaseModel):
    """重排响应"""
    model: str = Field(..., description="模型名称")
    results: List[RerankResult] = Field(..., description="重排结果列表")
    usage: dict = Field(..., description="使用量统计")


# ========== 聚类服务模型 ==========

class SiliconFlowEmbeddingConfig(BaseModel):
    """硅基流动 Embedding API 配置"""
    api_key: str = Field(..., description="API 密钥")
    api_url: str = Field(..., description="API 地址")
    model: str = Field(..., description="模型名称")
    timeout: int = Field(60000, description="超时时间(毫秒)")


class SiliconFlowLLMConfig(BaseModel):
    """硅基流动 LLM API 配置"""
    api_key: str = Field(..., description="API 密钥")
    api_url: str = Field(..., description="API 地址")
    model: str = Field(..., description="模型名称")
    temperature: float = Field(0.3, description="温度参数")
    max_tokens: int = Field(500, description="最大token数")
    timeout: int = Field(30000, description="超时时间(毫秒)")


class ClusterRequest(BaseModel):
    """聚类请求"""
    texts: List[str] = Field(..., description="待聚类的文本列表")
    eps: float = Field(0.4, ge=0.1, le=1.0, description="DBSCAN eps 参数")
    min_samples: int = Field(3, ge=2, le=10, description="DBSCAN min_samples 参数")
    metric: str = Field("cosine", description="距离度量方式 (cosine/euclidean)")
    siliconflow_embedding: SiliconFlowEmbeddingConfig = Field(..., description="硅基流动 Embedding 配置")
    siliconflow_llm: SiliconFlowLLMConfig = Field(..., description="硅基流动 LLM 配置")


class ClusterTopic(BaseModel):
    """聚类话题"""
    cluster_id: int = Field(..., description="簇ID")
    topic: str = Field(..., description="话题名称")
    tags: List[str] = Field(default_factory=list, description="业务标签")
    examples: List[str] = Field(default_factory=list, description="代表性问题")
    size: int = Field(..., description="簇大小")


class ScatterPoint(BaseModel):
    """散点数据"""
    x: float = Field(..., description="X坐标")
    y: float = Field(..., description="Y坐标")
    cluster: int = Field(..., description="簇ID (-1表示噪声点)")
    text: str = Field(..., description="文本内容")


class ClusterResponse(BaseModel):
    """聚类响应"""
    clusters: List[ClusterTopic] = Field(..., description="聚类话题列表")
    scatter_data: List[ScatterPoint] = Field(..., description="散点图数据")
    noise_count: int = Field(..., description="噪声点数量")
    total_texts: int = Field(..., description="总文本数")
    valid_clusters: int = Field(..., description="有效簇数量")


# ========== 模型管理 ==========

def load_embedding_model(model_name: str) -> SentenceTransformer:
    """加载嵌入模型"""
    global _embedding_model, _embedding_model_name

    if _embedding_model is not None and _embedding_model_name == model_name:
        logger.info(f"使用已加载的嵌入模型: {model_name}")
        return _embedding_model

    logger.info(f"正在加载嵌入模型: {model_name}")

    try:
        if os.path.exists(model_name):
            logger.info(f"从本地路径加载嵌入模型: {model_name}")
            os.environ['TRANSFORMERS_OFFLINE'] = '1'
            os.environ['HF_HUB_OFFLINE'] = '1'
            _embedding_model = SentenceTransformer(
                model_name,
                local_files_only=True,
                cache_folder=model_name
            )
        else:
            logger.info(f"从 HuggingFace Hub 下载嵌入模型: {model_name}")
            _embedding_model = SentenceTransformer(model_name)

        _embedding_model_name = model_name
        logger.info(f"嵌入模型加载成功,维度: {_embedding_model.get_sentence_embedding_dimension()}")
        return _embedding_model

    except Exception as e:
        logger.error(f"嵌入模型加载失败: {str(e)}")
        raise HTTPException(status_code=500, detail=f"嵌入模型加载失败: {str(e)}")


def load_rerank_model(model_name: str) -> CrossEncoder:
    """加载重排模型"""
    global _rerank_model, _rerank_model_name

    if _rerank_model is not None and _rerank_model_name == model_name:
        logger.info(f"使用已加载的重排模型: {model_name}")
        return _rerank_model

    logger.info(f"正在加载重排模型: {model_name}")

    try:
        if os.path.exists(model_name):
            logger.info(f"从本地路径加载重排模型: {model_name}")
            os.environ['TRANSFORMERS_OFFLINE'] = '1'
            os.environ['HF_HUB_OFFLINE'] = '1'
            _rerank_model = CrossEncoder(model_name, local_files_only=True)
        else:
            logger.info(f"从 HuggingFace Hub 下载重排模型: {model_name}")
            _rerank_model = CrossEncoder(model_name)

        _rerank_model_name = model_name
        logger.info(f"重排模型加载成功: {model_name}")
        return _rerank_model

    except Exception as e:
        logger.error(f"重排模型加载失败: {str(e)}")
        raise HTTPException(status_code=500, detail=f"重排模型加载失败: {str(e)}")


# ========== API 端点 - 服务信息 ==========

@app.get("/", summary="服务信息")
async def root():
    """服务根路径,返回基本信息"""
    return {
        "service": "deepSearch Python 统一服务",
        "version": "1.0.0",
        "description": "整合嵌入、重排和聚类功能的统一微服务",
        "modules": {
            "embedding": "文本向量化服务",
            "rerank": "文档重排序服务",
            "cluster": "文本聚类分析服务"
        },
        "endpoints": {
            "embedding": {
                "v1": "/embedding/v1/embeddings",
                "api": "/embedding/api/embeddings"
            },
            "rerank": {
                "v1": "/rerank/v1/rerank",
                "api": "/rerank/api/rerank"
            },
            "cluster": {
                "api": "/cluster/api/cluster"
            },
            "health": "/health",
            "docs": "/docs"
        }
    }


@app.get("/health", response_model=HealthResponse, summary="健康检查")
async def health_check():
    """健康检查端点"""
    return HealthResponse(
        status="healthy",
        service="deepsearch-python-service",
        version="1.0.0",
        modules={
            "embedding": _embedding_model is not None,
            "rerank": _rerank_model is not None,
            "cluster": True
        }
    )


# ========== API 端点 - 嵌入服务 ==========

@app.post("/embedding/v1/embeddings", response_model=EmbeddingResponse, summary="生成文本向量")
async def create_embeddings(request: EmbeddingRequest):
    """生成文本向量 (兼容 OpenAI API 格式)"""
    try:
        logger.info(f"收到嵌入请求 - 文本数量: {len(request.input)}, 模型: {request.model}")

        if not request.input:
            raise HTTPException(status_code=400, detail="input 不能为空")

        model = load_embedding_model(request.model)

        logger.info(f"正在为 {len(request.input)} 条文本生成向量...")
        embeddings = model.encode(
            request.input,
            normalize_embeddings=True,
            show_progress_bar=False
        )

        embeddings_list = embeddings.tolist() if isinstance(embeddings, np.ndarray) else embeddings

        data = [
            EmbeddingData(
                object="embedding",
                embedding=embedding,
                index=i
            )
            for i, embedding in enumerate(embeddings_list)
        ]

        total_tokens = sum(len(text.split()) for text in request.input)

        logger.info(f"向量生成完成,总数: {len(data)}, 维度: {len(data[0].embedding)}")

        return EmbeddingResponse(
            object="list",
            data=data,
            model=request.model,
            usage={
                "prompt_tokens": total_tokens,
                "total_tokens": total_tokens
            }
        )

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"生成向量失败: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"生成向量失败: {str(e)}")


@app.post("/embedding/api/embeddings", response_model=EmbeddingResponse, summary="生成文本向量(备用端点)")
async def create_embeddings_api(request: EmbeddingRequest):
    """生成文本向量 (备用端点)"""
    return await create_embeddings(request)


# ========== API 端点 - 重排服务 ==========

@app.post("/rerank/v1/rerank", response_model=RerankResponse, summary="文档重排序")
async def rerank_documents(request: RerankRequest):
    """对文档进行重排序 (兼容硅基流动 API 格式)"""
    try:
        logger.info(f"收到重排请求 - 文档数量: {len(request.documents)}, 模型: {request.model}")

        if not request.documents:
            raise HTTPException(status_code=400, detail="documents 不能为空")

        if not request.query:
            raise HTTPException(status_code=400, detail="query 不能为空")

        model = load_rerank_model(request.model)

        pairs = [[request.query, doc] for doc in request.documents]

        logger.info(f"正在计算 {len(pairs)} 个查询-文档对的相关性分数...")
        scores = model.predict(pairs)

        if isinstance(scores, np.ndarray):
            scores = scores.tolist()

        results_with_index = [
            {
                "index": i,
                "score": float(score),
                "document": doc
            }
            for i, (score, doc) in enumerate(zip(scores, request.documents))
        ]

        results_with_index.sort(key=lambda x: x["score"], reverse=True)

        if request.top_n is not None and request.top_n > 0:
            results_with_index = results_with_index[:request.top_n]

        results = [
            RerankResult(
                index=item["index"],
                relevance_score=item["score"],
                document=item["document"] if request.return_documents else None
            )
            for item in results_with_index
        ]

        total_tokens = len(request.query.split()) + sum(len(doc.split()) for doc in request.documents)

        logger.info(f"重排完成,返回结果数: {len(results)}")

        return RerankResponse(
            model=request.model,
            results=results,
            usage={
                "total_tokens": total_tokens
            }
        )

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"重排失败: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"重排失败: {str(e)}")


@app.post("/rerank/api/rerank", response_model=RerankResponse, summary="文档重排序(备用端点)")
async def rerank_documents_api(request: RerankRequest):
    """对文档进行重排序 (备用端点)"""
    return await rerank_documents(request)


# ========== API 端点 - 聚类服务 ==========

@app.post("/cluster/api/cluster", response_model=ClusterResponse, summary="执行聚类分析")
async def cluster_analysis(request: ClusterRequest):
    """执行文本聚类分析"""
    try:
        logger.info(f"收到聚类请求 - 文本数量: {len(request.texts)}, eps: {request.eps}, min_samples: {request.min_samples}")

        if not request.texts:
            raise HTTPException(status_code=400, detail="文本列表不能为空")

        if len(request.texts) < request.min_samples:
            raise HTTPException(
                status_code=400,
                detail=f"文本数量({len(request.texts)})少于 min_samples({request.min_samples})"
            )

        # 初始化硅基流动客户端
        client = SiliconFlowClient(
            embedding_config=request.siliconflow_embedding.dict(),
            llm_config=request.siliconflow_llm.dict()
        )

        # 调用硅基流动获取向量
        logger.info("步骤 1/4: 调用硅基流动 Embedding API...")
        embeddings = client.get_embeddings(request.texts)
        embeddings_array = np.array(embeddings)
        logger.info(f"向量生成完成 - 形状: {embeddings_array.shape}")

        # 执行聚类和降维
        logger.info("步骤 2/4: 执行 DBSCAN 聚类和 UMAP 降维...")
        labels, umap_coords = perform_clustering(
            embeddings=embeddings_array,
            texts=request.texts,
            eps=request.eps,
            min_samples=request.min_samples,
            metric=request.metric
        )
        logger.info(f"聚类完成 - 标签数量: {len(labels)}")

        # 分组聚类文本
        logger.info("步骤 3/4: 分组聚类文本...")
        clusters_map = {}
        for i, label in enumerate(labels):
            if label == -1:
                continue
            if label not in clusters_map:
                clusters_map[label] = []
            clusters_map[label].append(request.texts[i])

        logger.info(f"发现 {len(clusters_map)} 个有效簇")

        # 生成话题描述
        logger.info("步骤 4/4: 调用硅基流动 LLM API 生成话题描述...")
        cluster_results = []
        for cluster_id, cluster_texts in sorted(clusters_map.items()):
            topic_info = client.generate_topic(cluster_texts)

            if topic_info is None:
                logger.warning(f"簇 {cluster_id} 的话题生成失败,使用默认值")
                topic_info = {
                    "topic": f"用户反馈簇 {cluster_id}",
                    "tags": ["待分析"],
                    "examples": cluster_texts[:3]
                }

            cluster_results.append(ClusterTopic(
                cluster_id=int(cluster_id),
                topic=topic_info.get("topic", f"簇{cluster_id}"),
                tags=topic_info.get("tags", []),
                examples=topic_info.get("examples", cluster_texts[:3]),
                size=len(cluster_texts)
            ))

        logger.info(f"成功生成 {len(cluster_results)} 个话题描述")

        # 构建散点数据
        scatter_data = [
            ScatterPoint(
                x=float(umap_coords[i][0]),
                y=float(umap_coords[i][1]),
                cluster=int(labels[i]),
                text=request.texts[i]
            )
            for i in range(len(request.texts))
        ]

        noise_count = int(np.sum(labels == -1))
        valid_clusters = len(clusters_map)

        logger.info(f"聚类完成 - 有效簇: {valid_clusters}, 噪声点: {noise_count}")

        return ClusterResponse(
            clusters=cluster_results,
            scatter_data=scatter_data,
            noise_count=noise_count,
            total_texts=len(request.texts),
            valid_clusters=valid_clusters
        )

    except HTTPException:
        raise
    except ValueError as e:
        logger.error(f"参数验证失败: {str(e)}")
        raise HTTPException(status_code=400, detail=f"参数错误: {str(e)}")
    except Exception as e:
        logger.error(f"聚类分析失败: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"聚类分析失败: {str(e)}")


# ========== 启动说明 ==========
if __name__ == "__main__":
    import uvicorn

    logger.info("正在启动 deepSearch Python 统一服务...")
    uvicorn.run(
        "main_service:app",
        host="0.0.0.0",
        port=5000,
        log_level="info"
    )
