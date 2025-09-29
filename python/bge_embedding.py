from sentence_transformers import SentenceTransformer
import os
import warnings

# 忽略无关警告
warnings.filterwarnings("ignore", module="huggingface_hub")

_model = None

def load_bge_model(model_path=r"D:\易诚互动\恒丰银行\话题聚类\算法试行\huggingface\hub\models--BAAI--bge-large-zh-v1.5\snapshots\79e7739b6ab944e86d6171e44d24c997fc1e0116"):
    global _model
    if _model is not None:
        return _model  # ✅ 如果已加载，直接返回

    print("🔄 正在加载 BGE 模型...")

    # ✅ 第一步：检查路径是否存在
    if not os.path.exists(model_path):
        raise FileNotFoundError(
            f"❌ 模型路径不存在: {model_path}\n"
            "请确认路径正确，且包含完整的模型文件。"
        )

    # 检查关键文件是否存在
    required_files = ["config.json", "tokenizer.json", "vocab.txt"]
    missing_files = [f for f in required_files if not os.path.exists(os.path.join(model_path, f))]
    
    # 检查模型权重文件（可能有不同的命名方式）
    weight_files = ["pytorch_model.bin", "model.safetensors"]
    has_weight = any(os.path.exists(os.path.join(model_path, f)) for f in weight_files)
    
    if missing_files or not has_weight:
        error_msg = "❌ 模型文件不完整\n"
        if missing_files:
            error_msg += f"缺少必要文件: {', '.join(missing_files)}\n"
        if not has_weight:
            error_msg += f"未找到模型权重文件（需包含以下之一: {', '.join(weight_files)}）\n"
        raise FileNotFoundError(error_msg)

    # ✅ 第二步：强制离线模式
    os.environ['TRANSFORMERS_OFFLINE'] = '1'
    os.environ['HF_HUB_OFFLINE'] = '1'

    try:
        # ✅ 第三步：加载本地模型
        _model = SentenceTransformer(
            model_path,
            local_files_only=True,
            cache_folder=model_path
        )
        print(f"✅ 成功加载本地 BGE 模型: {model_path}")
    except Exception as e:
        raise RuntimeError(f"❌ 加载模型失败: {str(e)}\n"
                          "可能是模型文件损坏或不完整，请重新下载。")

    return _model

def get_embeddings(texts):
    """输入文本列表，返回对应的向量"""
    model = load_bge_model()  # 使用默认路径，无需重复指定
    print(f"🔍 正在为 {len(texts)} 条文本生成向量...")
    embeddings = model.encode(texts, normalize_embeddings=True)
    print("✅ 向量生成完成")
    return embeddings
