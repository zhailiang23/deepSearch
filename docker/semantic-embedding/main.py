"""
语义嵌入服务
基于 sentence-transformers 提供文本向量化 API
"""

import os
import time
import psutil
from typing import List, Dict, Any, Optional
from contextlib import asynccontextmanager

import numpy as np
from fastapi import FastAPI, HTTPException, BackgroundTasks
from pydantic import BaseModel, Field
from loguru import logger
from cachetools import TTLCache
from sentence_transformers import SentenceTransformer
import torch


# 配置日志
logger.add(
    "/app/logs/semantic_embedding.log",
    rotation="100 MB",
    retention="30 days",
    level="INFO",
    format="{time:YYYY-MM-DD HH:mm:ss} | {level} | {name}:{function}:{line} | {message}"
)

# 全局变量
model = None
cache = TTLCache(maxsize=1000, ttl=3600)  # 1小时缓存


class EmbeddingRequest(BaseModel):
    """嵌入请求模型"""
    text: str = Field(..., description="要转换为向量的文本", min_length=1)
    model: str = Field(
        default="paraphrase-multilingual-MiniLM-L12-v2",
        description="使用的模型名称"
    )
    normalize: bool = Field(default=True, description="是否标准化向量")


class BatchEmbeddingRequest(BaseModel):
    """批量嵌入请求模型"""
    texts: List[str] = Field(..., description="要转换为向量的文本列表", min_items=1)
    model: str = Field(
        default="paraphrase-multilingual-MiniLM-L12-v2",
        description="使用的模型名称"
    )
    normalize: bool = Field(default=True, description="是否标准化向量")


class SimilarityRequest(BaseModel):
    """相似度请求模型"""
    text1: str = Field(..., description="第一个文本")
    text2: str = Field(..., description="第二个文本")
    model: str = Field(
        default="paraphrase-multilingual-MiniLM-L12-v2",
        description="使用的模型名称"
    )


class EmbeddingResponse(BaseModel):
    """嵌入响应模型"""
    embedding: List[float] = Field(..., description="文本向量")
    model: str = Field(..., description="使用的模型名称")
    dimension: int = Field(..., description="向量维度")
    processing_time: float = Field(..., description="处理耗时（秒）")


class BatchEmbeddingResponse(BaseModel):
    """批量嵌入响应模型"""
    embeddings: List[List[float]] = Field(..., description="文本向量列表")
    model: str = Field(..., description="使用的模型名称")
    dimension: int = Field(..., description="向量维度")
    count: int = Field(..., description="处理的文本数量")
    processing_time: float = Field(..., description="处理耗时（秒）")


class SimilarityResponse(BaseModel):
    """相似度响应模型"""
    similarity: float = Field(..., description="余弦相似度 (-1 到 1)")
    model: str = Field(..., description="使用的模型名称")
    processing_time: float = Field(..., description="处理耗时（秒）")


class HealthResponse(BaseModel):
    """健康检查响应模型"""
    status: str = Field(..., description="服务状态")
    model_loaded: bool = Field(..., description="模型是否已加载")
    model_name: str = Field(..., description="当前加载的模型名称")
    uptime: float = Field(..., description="运行时间（秒）")


class StatusResponse(BaseModel):
    """服务状态响应模型"""
    model_name: str = Field(..., description="当前模型名称")
    model_dimension: int = Field(..., description="模型向量维度")
    cache_size: int = Field(..., description="缓存大小")
    cache_hits: int = Field(..., description="缓存命中次数")
    total_requests: int = Field(..., description="总请求次数")
    uptime: float = Field(..., description="运行时间（秒）")
    memory_usage: Dict[str, Any] = Field(..., description="内存使用情况")


# 统计信息
stats = {
    "cache_hits": 0,
    "total_requests": 0,
    "start_time": time.time()
}


async def load_model():
    """加载语义模型"""
    global model
    try:
        logger.info("开始加载语义模型...")
        model_name = "paraphrase-multilingual-MiniLM-L12-v2"

        # 检查 CUDA 是否可用
        device = "cuda" if torch.cuda.is_available() else "cpu"
        logger.info(f"使用设备: {device}")

        model = SentenceTransformer(model_name, device=device)
        logger.info(f"模型加载成功: {model_name}, 维度: {model.get_sentence_embedding_dimension()}")

    except Exception as e:
        logger.error(f"模型加载失败: {e}")
        raise


@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用生命周期管理"""
    # 启动时加载模型
    await load_model()
    yield
    # 关闭时清理资源
    logger.info("服务正在关闭...")


# 创建 FastAPI 应用
app = FastAPI(
    title="语义嵌入服务",
    description="基于 sentence-transformers 的文本向量化服务",
    version="1.0.0",
    lifespan=lifespan
)


def get_cache_key(text: str, model_name: str) -> str:
    """生成缓存键"""
    return f"{model_name}:{hash(text)}"


def get_memory_usage() -> Dict[str, Any]:
    """获取内存使用情况"""
    process = psutil.Process()
    memory_info = process.memory_info()
    return {
        "rss_mb": round(memory_info.rss / 1024 / 1024, 2),
        "vms_mb": round(memory_info.vms / 1024 / 1024, 2),
        "percent": round(process.memory_percent(), 2)
    }


@app.get("/health", response_model=HealthResponse)
async def health_check():
    """健康检查端点"""
    uptime = time.time() - stats["start_time"]
    return HealthResponse(
        status="healthy" if model is not None else "unhealthy",
        model_loaded=model is not None,
        model_name="paraphrase-multilingual-MiniLM-L12-v2" if model else "",
        uptime=uptime
    )


@app.get("/status", response_model=StatusResponse)
async def get_status():
    """获取服务状态"""
    if model is None:
        raise HTTPException(status_code=503, detail="模型尚未加载")

    uptime = time.time() - stats["start_time"]
    return StatusResponse(
        model_name="paraphrase-multilingual-MiniLM-L12-v2",
        model_dimension=model.get_sentence_embedding_dimension(),
        cache_size=len(cache),
        cache_hits=stats["cache_hits"],
        total_requests=stats["total_requests"],
        uptime=uptime,
        memory_usage=get_memory_usage()
    )


@app.post("/embeddings", response_model=EmbeddingResponse)
async def create_embedding(request: EmbeddingRequest):
    """生成文本向量"""
    if model is None:
        raise HTTPException(status_code=503, detail="模型尚未加载")

    stats["total_requests"] += 1
    start_time = time.time()

    try:
        # 检查缓存
        cache_key = get_cache_key(request.text, request.model)
        if cache_key in cache:
            stats["cache_hits"] += 1
            embedding = cache[cache_key]
            logger.debug(f"从缓存获取向量: {request.text[:50]}...")
        else:
            # 生成向量
            embedding = model.encode(
                request.text,
                normalize_embeddings=request.normalize,
                convert_to_numpy=True
            )

            # 转换为列表并缓存
            embedding = embedding.tolist()
            cache[cache_key] = embedding
            logger.debug(f"生成新向量: {request.text[:50]}...")

        processing_time = time.time() - start_time

        return EmbeddingResponse(
            embedding=embedding,
            model=request.model,
            dimension=len(embedding),
            processing_time=processing_time
        )

    except Exception as e:
        logger.error(f"生成向量失败: {e}")
        raise HTTPException(status_code=500, detail=f"向量生成失败: {str(e)}")


@app.post("/embeddings/batch", response_model=BatchEmbeddingResponse)
async def create_batch_embeddings(request: BatchEmbeddingRequest):
    """批量生成文本向量"""
    if model is None:
        raise HTTPException(status_code=503, detail="模型尚未加载")

    stats["total_requests"] += 1
    start_time = time.time()

    try:
        # 批量生成向量
        embeddings = model.encode(
            request.texts,
            normalize_embeddings=request.normalize,
            convert_to_numpy=True
        )

        # 转换为列表
        embeddings_list = [emb.tolist() for emb in embeddings]

        # 缓存结果
        for text, embedding in zip(request.texts, embeddings_list):
            cache_key = get_cache_key(text, request.model)
            cache[cache_key] = embedding

        processing_time = time.time() - start_time
        dimension = len(embeddings_list[0]) if embeddings_list else 0

        logger.info(f"批量生成向量完成: {len(request.texts)} 个文本，耗时 {processing_time:.3f}s")

        return BatchEmbeddingResponse(
            embeddings=embeddings_list,
            model=request.model,
            dimension=dimension,
            count=len(embeddings_list),
            processing_time=processing_time
        )

    except Exception as e:
        logger.error(f"批量生成向量失败: {e}")
        raise HTTPException(status_code=500, detail=f"批量向量生成失败: {str(e)}")


@app.post("/similarity", response_model=SimilarityResponse)
async def calculate_similarity(request: SimilarityRequest):
    """计算文本相似度"""
    if model is None:
        raise HTTPException(status_code=503, detail="模型尚未加载")

    stats["total_requests"] += 1
    start_time = time.time()

    try:
        # 生成两个文本的向量
        embeddings = model.encode([request.text1, request.text2], convert_to_numpy=True)

        # 计算余弦相似度
        similarity = np.dot(embeddings[0], embeddings[1]) / (
            np.linalg.norm(embeddings[0]) * np.linalg.norm(embeddings[1])
        )

        processing_time = time.time() - start_time

        return SimilarityResponse(
            similarity=float(similarity),
            model=request.model,
            processing_time=processing_time
        )

    except Exception as e:
        logger.error(f"计算相似度失败: {e}")
        raise HTTPException(status_code=500, detail=f"相似度计算失败: {str(e)}")


@app.get("/")
async def root():
    """根端点"""
    return {
        "message": "语义嵌入服务",
        "version": "1.0.0",
        "status": "running" if model else "loading",
        "docs": "/docs"
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)