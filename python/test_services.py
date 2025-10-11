"""
测试嵌入和重排服务
"""
import requests
import json

# 服务配置
EMBEDDING_URL = "http://localhost:8001"
RERANK_URL = "http://localhost:8002"


def test_embedding_service():
    """测试嵌入服务"""
    print("=" * 50)
    print("测试嵌入服务")
    print("=" * 50)

    # 测试健康检查
    print("\n1. 测试健康检查...")
    response = requests.get(f"{EMBEDDING_URL}/health")
    print(f"状态: {response.status_code}")
    print(f"响应: {json.dumps(response.json(), indent=2, ensure_ascii=False)}")

    # 测试生成向量
    print("\n2. 测试生成向量...")
    payload = {
        "input": ["你好", "世界", "Python微服务"],
        "model": "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"
    }
    response = requests.post(f"{EMBEDDING_URL}/v1/embeddings", json=payload)
    print(f"状态: {response.status_code}")

    if response.status_code == 200:
        result = response.json()
        print(f"模型: {result['model']}")
        print(f"生成向量数: {len(result['data'])}")
        print(f"向量维度: {len(result['data'][0]['embedding'])}")
        print(f"使用token数: {result['usage']['total_tokens']}")
        print("✅ 嵌入服务测试通过")
    else:
        print(f"❌ 嵌入服务测试失败: {response.text}")


def test_rerank_service():
    """测试重排服务"""
    print("\n" + "=" * 50)
    print("测试重排服务")
    print("=" * 50)

    # 测试健康检查
    print("\n1. 测试健康检查...")
    response = requests.get(f"{RERANK_URL}/health")
    print(f"状态: {response.status_code}")
    print(f"响应: {json.dumps(response.json(), indent=2, ensure_ascii=False)}")

    # 测试文档重排
    print("\n2. 测试文档重排...")
    payload = {
        "model": "cross-encoder/ms-marco-MiniLM-L-6-v2",
        "query": "Python微服务部署",
        "documents": [
            "Docker容器化部署指南",
            "Python编程基础教程",
            "Kubernetes集群管理",
            "FastAPI微服务开发",
            "数据库设计原则"
        ],
        "top_n": 3,
        "return_documents": True
    }
    response = requests.post(f"{RERANK_URL}/v1/rerank", json=payload)
    print(f"状态: {response.status_code}")

    if response.status_code == 200:
        result = response.json()
        print(f"模型: {result['model']}")
        print(f"返回结果数: {len(result['results'])}")
        print("\n重排结果:")
        for i, item in enumerate(result['results'], 1):
            print(f"  {i}. [{item['index']}] {item['document']} (分数: {item['relevance_score']:.4f})")
        print("✅ 重排服务测试通过")
    else:
        print(f"❌ 重排服务测试失败: {response.text}")


def test_integration():
    """测试服务集成"""
    print("\n" + "=" * 50)
    print("测试服务集成")
    print("=" * 50)

    # 1. 生成查询向量
    print("\n1. 生成查询向量...")
    query = "如何使用Docker部署Python服务"
    embedding_payload = {
        "input": [query],
        "model": "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"
    }
    response = requests.post(f"{EMBEDDING_URL}/v1/embeddings", json=embedding_payload)
    if response.status_code != 200:
        print(f"❌ 向量生成失败: {response.text}")
        return

    query_embedding = response.json()['data'][0]['embedding']
    print(f"✅ 查询向量生成成功,维度: {len(query_embedding)}")

    # 2. 生成文档向量
    print("\n2. 生成文档向量...")
    documents = [
        "Docker Compose编排多容器应用",
        "使用Dockerfile构建镜像",
        "Kubernetes Pod配置",
        "Python虚拟环境管理"
    ]
    docs_payload = {
        "input": documents,
        "model": "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"
    }
    response = requests.post(f"{EMBEDDING_URL}/v1/embeddings", json=docs_payload)
    if response.status_code != 200:
        print(f"❌ 文档向量生成失败: {response.text}")
        return

    print(f"✅ 文档向量生成成功,数量: {len(documents)}")

    # 3. 重排文档
    print("\n3. 重排文档...")
    rerank_payload = {
        "model": "cross-encoder/ms-marco-MiniLM-L-6-v2",
        "query": query,
        "documents": documents,
        "top_n": 2,
        "return_documents": True
    }
    response = requests.post(f"{RERANK_URL}/v1/rerank", json=rerank_payload)
    if response.status_code != 200:
        print(f"❌ 文档重排失败: {response.text}")
        return

    result = response.json()
    print("✅ 文档重排成功")
    print("\n最相关的文档:")
    for i, item in enumerate(result['results'], 1):
        print(f"  {i}. {item['document']} (分数: {item['relevance_score']:.4f})")

    print("\n✅ 服务集成测试通过")


if __name__ == "__main__":
    try:
        # 测试嵌入服务
        test_embedding_service()

        # 测试重排服务
        test_rerank_service()

        # 测试服务集成
        test_integration()

        print("\n" + "=" * 50)
        print("所有测试完成!")
        print("=" * 50)

    except requests.exceptions.ConnectionError as e:
        print(f"\n❌ 服务连接失败: {e}")
        print("请确保服务已启动:")
        print(f"  - 嵌入服务: {EMBEDDING_URL}")
        print(f"  - 重排服务: {RERANK_URL}")
    except Exception as e:
        print(f"\n❌ 测试失败: {e}")
