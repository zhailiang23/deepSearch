"""
聚类分析 FastAPI 服务
提供基于硅基流动 API 的文本聚类分析服务
"""
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from typing import List, Optional
import numpy as np
from siliconflow_client import SiliconFlowClient
from clustering import perform_clustering
import logging

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# 创建 FastAPI 应用
app = FastAPI(
    title="聚类分析服务",
    description="基于硅基流动 API 的文本聚类分析服务",
    version="1.0.0"
)


# ========== 请求/响应模型 ==========

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


# ========== API 端点 ==========

@app.post("/api/cluster", response_model=ClusterResponse, summary="执行聚类分析")
async def cluster_analysis(request: ClusterRequest):
    """
    执行文本聚类分析

    流程：
    1. 调用硅基流动 Embedding API 生成向量
    2. 执行 DBSCAN 聚类
    3. 使用 UMAP 降维到 2D
    4. 调用硅基流动 LLM API 生成话题描述

    Returns:
        ClusterResponse: 聚类分析结果
    """
    try:
        logger.info(f"收到聚类请求 - 文本数量: {len(request.texts)}, eps: {request.eps}, min_samples: {request.min_samples}")

        # 验证输入
        if not request.texts:
            raise HTTPException(status_code=400, detail="文本列表不能为空")

        if len(request.texts) < request.min_samples:
            raise HTTPException(
                status_code=400,
                detail=f"文本数量({len(request.texts)})少于 min_samples({request.min_samples})"
            )

        # 1. 初始化硅基流动客户端
        client = SiliconFlowClient(
            embedding_config=request.siliconflow_embedding.dict(),
            llm_config=request.siliconflow_llm.dict()
        )

        # 2. 调用硅基流动获取向量
        logger.info("步骤 1/4: 调用硅基流动 Embedding API...")
        embeddings = client.get_embeddings(request.texts)
        embeddings_array = np.array(embeddings)
        logger.info(f"向量生成完成 - 形状: {embeddings_array.shape}")

        # 3. 执行聚类和降维
        logger.info("步骤 2/4: 执行 DBSCAN 聚类和 UMAP 降维...")
        labels, umap_coords = perform_clustering(
            embeddings=embeddings_array,
            texts=request.texts,
            eps=request.eps,
            min_samples=request.min_samples,
            metric=request.metric
        )
        logger.info(f"聚类完成 - 标签数量: {len(labels)}")

        # 4. 分组聚类文本
        logger.info("步骤 3/4: 分组聚类文本...")
        clusters_map = {}
        for i, label in enumerate(labels):
            if label == -1:  # 跳过噪声点
                continue
            if label not in clusters_map:
                clusters_map[label] = []
            clusters_map[label].append(request.texts[i])

        logger.info(f"发现 {len(clusters_map)} 个有效簇")

        # 5. 生成话题描述
        logger.info("步骤 4/4: 调用硅基流动 LLM API 生成话题描述...")
        cluster_results = []
        for cluster_id, cluster_texts in sorted(clusters_map.items()):
            topic_info = client.generate_topic(cluster_texts)

            # 如果 LLM 生成失败，使用默认值
            if topic_info is None:
                logger.warning(f"簇 {cluster_id} 的话题生成失败，使用默认值")
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

        # 6. 构建散点数据
        scatter_data = [
            ScatterPoint(
                x=float(umap_coords[i][0]),
                y=float(umap_coords[i][1]),
                cluster=int(labels[i]),
                text=request.texts[i]
            )
            for i in range(len(request.texts))
        ]

        # 7. 统计信息
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


@app.get("/health", summary="健康检查")
async def health_check():
    """
    健康检查端点

    Returns:
        dict: 服务状态
    """
    return {
        "status": "healthy",
        "service": "cluster-analysis",
        "version": "1.0.0"
    }


@app.get("/", summary="服务信息")
async def root():
    """
    服务根路径，返回基本信息

    Returns:
        dict: 服务信息
    """
    return {
        "service": "聚类分析服务",
        "version": "1.0.0",
        "description": "基于硅基流动 API 的文本聚类分析服务",
        "endpoints": {
            "cluster": "/api/cluster",
            "health": "/health",
            "docs": "/docs"
        }
    }


# ========== 启动说明 ==========
if __name__ == "__main__":
    import uvicorn

    logger.info("正在启动聚类分析服务...")
    uvicorn.run(
        "cluster_api:app",
        host="0.0.0.0",
        port=5001,
        log_level="info"
    )
