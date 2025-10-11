"""
本地嵌入服务 - 使用 sentence-transformers 提供文本向量化服务
支持离线运行，替代硅基流动的在线 Embedding API
"""
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from typing import List, Optional
import logging
import os
from sentence_transformers import SentenceTransformer
import numpy as np

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# 创建 FastAPI 应用
app = FastAPI(
    title="本地嵌入服务",
    description="基于 sentence-transformers 的本地文本向量化服务",
    version="1.0.0"
)

# 全局模型实例
_model: Optional[SentenceTransformer] = None
_model_name: Optional[str] = None


# ========== 请求/响应模型 ==========

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


class ModelConfig(BaseModel):
    """模型配置"""
    model_path: str = Field(..., description="模型路径(本地路径或HuggingFace模型ID)")


# ========== 模型管理 ==========

def load_model(model_name: str) -> SentenceTransformer:
    """
    加载模型(支持本地路径和HuggingFace模型ID)

    Args:
        model_name: 模型名称或路径

    Returns:
        SentenceTransformer: 加载的模型
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

            _model = SentenceTransformer(
                model_name,
                local_files_only=True,
                cache_folder=model_name
            )
        else:
            # 从 HuggingFace Hub 下载
            logger.info(f"从 HuggingFace Hub 下载模型: {model_name}")
            _model = SentenceTransformer(model_name)

        _model_name = model_name
        logger.info(f"模型加载成功,维度: {_model.get_sentence_embedding_dimension()}")
        return _model

    except Exception as e:
        logger.error(f"模型加载失败: {str(e)}")
        raise HTTPException(status_code=500, detail=f"模型加载失败: {str(e)}")


# ========== API 端点 ==========

@app.post("/v1/embeddings", response_model=EmbeddingResponse, summary="生成文本向量")
async def create_embeddings(request: EmbeddingRequest):
    """
    生成文本向量 (兼容 OpenAI API 格式)

    Args:
        request: 嵌入请求

    Returns:
        EmbeddingResponse: 嵌入响应
    """
    try:
        logger.info(f"收到嵌入请求 - 文本数量: {len(request.input)}, 模型: {request.model}")

        # 验证输入
        if not request.input:
            raise HTTPException(status_code=400, detail="input 不能为空")

        # 加载模型
        model = load_model(request.model)

        # 生成向量
        logger.info(f"正在为 {len(request.input)} 条文本生成向量...")
        embeddings = model.encode(
            request.input,
            normalize_embeddings=True,
            show_progress_bar=False
        )

        # 转换为列表格式
        embeddings_list = embeddings.tolist() if isinstance(embeddings, np.ndarray) else embeddings

        # 构建响应
        data = [
            EmbeddingData(
                object="embedding",
                embedding=embedding,
                index=i
            )
            for i, embedding in enumerate(embeddings_list)
        ]

        # 统计使用量(简化版,实际应该计算token数)
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


@app.post("/api/embeddings", response_model=EmbeddingResponse, summary="生成文本向量(备用端点)")
async def create_embeddings_api(request: EmbeddingRequest):
    """
    生成文本向量 (备用端点,路径为 /api/embeddings)

    Args:
        request: 嵌入请求

    Returns:
        EmbeddingResponse: 嵌入响应
    """
    return await create_embeddings(request)


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
            "dimension": model.get_sentence_embedding_dimension()
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
        "service": "embedding-service",
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
        "service": "本地嵌入服务",
        "version": "1.0.0",
        "description": "基于 sentence-transformers 的本地文本向量化服务",
        "endpoints": {
            "embeddings_v1": "/v1/embeddings",
            "embeddings_api": "/api/embeddings",
            "config": "/config",
            "health": "/health",
            "docs": "/docs"
        }
    }


# ========== 启动说明 ==========
if __name__ == "__main__":
    import uvicorn

    logger.info("正在启动本地嵌入服务...")
    uvicorn.run(
        "embedding_service:app",
        host="0.0.0.0",
        port=8001,
        log_level="info"
    )
