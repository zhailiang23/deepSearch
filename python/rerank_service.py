"""
本地重排服务 - 使用 sentence-transformers 提供文本重排序服务
支持离线运行，替代硅基流动的在线 Rerank API
"""
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from typing import List, Optional, Union
import logging
import os
from sentence_transformers import CrossEncoder
import numpy as np

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# 创建 FastAPI 应用
app = FastAPI(
    title="本地重排服务",
    description="基于 sentence-transformers CrossEncoder 的本地文本重排序服务",
    version="1.0.0"
)

# 全局模型实例
_model: Optional[CrossEncoder] = None
_model_name: Optional[str] = None


# ========== 请求/响应模型 ==========

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


class ModelConfig(BaseModel):
    """模型配置"""
    model_path: str = Field(..., description="模型路径(本地路径或HuggingFace模型ID)")


# ========== 模型管理 ==========

def load_model(model_name: str) -> CrossEncoder:
    """
    加载模型(支持本地路径和HuggingFace模型ID)

    Args:
        model_name: 模型名称或路径

    Returns:
        CrossEncoder: 加载的模型
    """
    global _model, _model_name

    # 如果已加载相同模型,直接返回
    if _model is not None and _model_name == model_name:
        logger.info(f"使用已加载的模型: {model_name}")
        return _model

    logger.info(f"正在加载模型: {model_name}")

    try:
        # 检查是否为本地路径
        if os.path.exists(model_name):
            logger.info(f"从本地路径加载模型: {model_name}")
            # 强制离线模式
            os.environ['TRANSFORMERS_OFFLINE'] = '1'
            os.environ['HF_HUB_OFFLINE'] = '1'

            _model = CrossEncoder(model_name, local_files_only=True)
        else:
            # 从 HuggingFace Hub 下载
            logger.info(f"从 HuggingFace Hub 下载模型: {model_name}")
            _model = CrossEncoder(model_name)

        _model_name = model_name
        logger.info(f"模型加载成功: {model_name}")
        return _model

    except Exception as e:
        logger.error(f"模型加载失败: {str(e)}")
        raise HTTPException(status_code=500, detail=f"模型加载失败: {str(e)}")


# ========== API 端点 ==========

@app.post("/v1/rerank", response_model=RerankResponse, summary="文档重排序")
async def rerank_documents(request: RerankRequest):
    """
    对文档进行重排序 (兼容硅基流动 API 格式)

    Args:
        request: 重排请求

    Returns:
        RerankResponse: 重排响应
    """
    try:
        logger.info(f"收到重排请求 - 文档数量: {len(request.documents)}, 模型: {request.model}")

        # 验证输入
        if not request.documents:
            raise HTTPException(status_code=400, detail="documents 不能为空")

        if not request.query:
            raise HTTPException(status_code=400, detail="query 不能为空")

        # 加载模型
        model = load_model(request.model)

        # 构建查询-文档对
        pairs = [[request.query, doc] for doc in request.documents]

        # 预测相关性分数
        logger.info(f"正在计算 {len(pairs)} 个查询-文档对的相关性分数...")
        scores = model.predict(pairs)

        # 转换为列表(如果是numpy数组)
        if isinstance(scores, np.ndarray):
            scores = scores.tolist()

        # 创建结果列表(索引,分数,文档)
        results_with_index = [
            {
                "index": i,
                "score": float(score),
                "document": doc
            }
            for i, (score, doc) in enumerate(zip(scores, request.documents))
        ]

        # 按分数降序排序
        results_with_index.sort(key=lambda x: x["score"], reverse=True)

        # 如果指定了 top_n,只返回前N个
        if request.top_n is not None and request.top_n > 0:
            results_with_index = results_with_index[:request.top_n]

        # 构建响应
        results = [
            RerankResult(
                index=item["index"],
                relevance_score=item["score"],
                document=item["document"] if request.return_documents else None
            )
            for item in results_with_index
        ]

        # 统计使用量(简化版)
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


@app.post("/api/rerank", response_model=RerankResponse, summary="文档重排序(备用端点)")
async def rerank_documents_api(request: RerankRequest):
    """
    对文档进行重排序 (备用端点,路径为 /api/rerank)

    Args:
        request: 重排请求

    Returns:
        RerankResponse: 重排响应
    """
    return await rerank_documents(request)


@app.post("/config", summary="配置模型")
async def configure_model(config: ModelConfig):
    """
    预加载指定模型

    Args:
        config: 模型配置

    Returns:
        dict: 配置结果
    """
    try:
        model = load_model(config.model_path)
        return {
            "status": "success",
            "model": config.model_path,
            "type": "CrossEncoder"
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"模型配置失败: {str(e)}")


@app.get("/health", summary="健康检查")
async def health_check():
    """
    健康检查端点

    Returns:
        dict: 服务状态
    """
    return {
        "status": "healthy",
        "service": "rerank-service",
        "version": "1.0.0",
        "model_loaded": _model is not None,
        "model_name": _model_name
    }


@app.get("/", summary="服务信息")
async def root():
    """
    服务根路径,返回基本信息

    Returns:
        dict: 服务信息
    """
    return {
        "service": "本地重排服务",
        "version": "1.0.0",
        "description": "基于 sentence-transformers CrossEncoder 的本地文本重排序服务",
        "endpoints": {
            "rerank_v1": "/v1/rerank",
            "rerank_api": "/api/rerank",
            "config": "/config",
            "health": "/health",
            "docs": "/docs"
        }
    }


# ========== 启动说明 ==========
if __name__ == "__main__":
    import uvicorn

    logger.info("正在启动本地重排服务...")
    uvicorn.run(
        "rerank_service:app",
        host="0.0.0.0",
        port=8002,
        log_level="info"
    )
