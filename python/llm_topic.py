import requests
import json
import logging
from typing import List, Dict, Optional
from sklearn.feature_extraction.text import TfidfVectorizer
import jieba

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('cluster_analysis.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

def generate_cluster_description(cluster_texts: List[str], api_key: str) -> Optional[Dict]:
    """调用QWen2-72B模型生成簇描述，重点突出用户具体问题"""
    if not cluster_texts:
        logger.warning("聚类文本为空，无法生成描述")
        return None
        
    # 限制输入文本数量，避免过长
    sample_texts = cluster_texts[:10]
    prompt = f"""作为银行客户反馈分析专家，请精准提炼用户的核心问题：
    
    【输入数据】
    以下是同一类别的用户反馈：
    {chr(10).join([f'- {text}' for text in sample_texts])}

    【输出要求】
    1. 生成1个简洁的类别名称（10-15字），必须包含：
       - 涉及的业务领域（如转账、信用卡、APP等）
       - 用户的核心诉求或问题（如额度不足、无法使用、流程复杂等）
       示例："转账限额不足，希望提高"、"信用卡审批被拒，询问原因"
    
    2. 列出3个最具代表性的原始问题（保留用户表述特点）
    
    3. 提取2-3个业务标签（如"转账限额"、"信用卡审批"、"APP故障"）
    
    请用JSON格式回复，不要包含任何额外文本：
    {{
        "topic": "类别名称",
        "examples": ["问题1", "问题2", "问题3"],
        "tags": ["标签1", "标签2"]
    }}"""
    
    try:
        headers = {
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json"
        }
        data = {
            "model": "QWen2-72B-Instruct",
            "messages": [
                {"role": "system", "content": "你是有10年经验的银行业务分析师，擅长从用户反馈中提炼具体问题和诉求"},
                {"role": "user", "content": prompt}
            ],
            "temperature": 0.3,
            "max_tokens": 500
        }
        
        logger.info(f"调用大模型处理聚类文本，样本数量: {len(sample_texts)}")
        response = requests.post(
            "http://192.168.156.142:9902/QWen2-72B-Instruct/v1/chat/completions",
            headers=headers,
            json=data,
            timeout=30
        )
        
        response.raise_for_status()
        result = response.json()
        
        if 'choices' in result and len(result['choices']) > 0:
            content = result['choices'][0]['message']['content'].strip()
            if content.startswith('```json'):
                content = content[7:-3].strip()
            return json.loads(content)
        else:
            logger.error("API返回格式不符合预期，缺少choices字段")
            return None
            
    except Exception as e:
        logger.error(f"API调用异常: {str(e)}")
        return None


def tfidf_fallback(cluster_texts: List[str], cluster_id: int) -> Dict:
    """TF-IDF降级方案，当LLM调用失败时使用"""
    try:
        tfidf = TfidfVectorizer(
            tokenizer=jieba.lcut,
            max_features=10,
            ngram_range=(1, 2),
            stop_words=['的', '了', '在', '是', '我', '有']
        )
        tfidf.fit_transform(cluster_texts)
        keywords = tfidf.get_feature_names_out()
        
        # 尝试从关键词中组合出包含问题的话题名称
        if len(keywords) >= 2:
            topic = f"{keywords[0]} {keywords[1]} 问题"
        else:
            topic = "、".join(keywords) + "相关问题"
            
        return {
            "topic": topic[:15],  # 控制长度
            "examples": cluster_texts[:3],
            "tags": keywords[:3].tolist() if len(keywords)>=3 else keywords.tolist()
        }
    except Exception as e:
        logger.error(f"TF-IDF降级方案失败: {str(e)}")
        return {
            "topic": f"簇{cluster_id}的用户问题",
            "examples": cluster_texts[:3] if cluster_texts else [],
            "tags": ["用户问题"]
        }


def generate_all_cluster_descriptions(df_all, api_key: str) -> Dict:
    """为所有有效聚类生成描述"""
    cluster_results = {}
    
    try:
        valid_clusters = sorted([cid for cid in df_all['cluster'].unique() if cid != -1])
    except KeyError:
        logger.error("DataFrame中缺少'cluster'列")
        return cluster_results

    if len(valid_clusters) == 0:
        logger.warning("未发现有效聚类（所有点均为噪声）")
        return cluster_results
        
    logger.info(f"开始为 {len(valid_clusters)} 个热点簇生成描述：{valid_clusters}")

    for cluster_id in valid_clusters:
        try:
            cluster_texts = df_all[df_all['cluster'] == cluster_id]['text'].tolist()
        except KeyError:
            logger.error("DataFrame中缺少'text'列")
            continue
            
        n_texts = len(cluster_texts)
        logger.info(f"处理簇 {cluster_id}（共 {n_texts} 条反馈）")
        
        if n_texts == 0:
            logger.warning(f"簇 {cluster_id} 无文本数据")
            continue

        result = generate_cluster_description(cluster_texts, api_key)
        
        if result and all(key in result for key in ['topic', 'examples', 'tags']):
            cluster_results[cluster_id] = result
        else:
            logger.info(f"簇 {cluster_id} 使用TF-IDF降级生成描述")
            cluster_results[cluster_id] = tfidf_fallback(cluster_texts, cluster_id)
    
    return cluster_results


def print_analysis_report(cluster_results: Dict):
    """打印格式化的聚类分析报告"""
    print("\n" + "🏦" + " " + "银行用户反馈聚类分析报告".center(46) + " " + "🏦")
    print("="*60)
    
    if not cluster_results:
        print("❌ 未能生成任何聚类描述，请检查 API 连接或数据。")
    else:
        for cluster_id, desc in sorted(cluster_results.items()):
            print(f"\n■ 热点簇 {cluster_id}: {desc['topic']}")
            print(f"  ├─ 🔖 业务标签: {', '.join(desc['tags'])}")
            print(f"  └─ 💬 典型问题:")
            
            examples = desc.get('examples', [])
            for example in examples[:3]:
                if isinstance(example, str):
                    display_text = example.strip()
                    if len(display_text) > 60:
                        display_text = display_text[:57] + "..."
                    print(f"      ▪ {display_text}")

    print("\n" + "💡 建议：以上热点簇可优先安排产品优化或客服话术更新。")
    print("="*60)


if __name__ == "__main__":
    api_key = "yk-4a72dfebe1a4455fb04e4e07c82c3d18"
    
    import pandas as pd
    df_all = pd.DataFrame({
        'cluster': [0, 0, 1, 1, -1],
        'text': [
            "我的银行卡被冻结了，不知道为什么",
            "银行卡突然不能用了，显示冻结状态",
            "手机银行转账限额太低，不够用",
            "希望能提高手机银行的转账额度",
            "这个银行的服务真差"
        ]
    })
    
    cluster_results = generate_all_cluster_descriptions(df_all, api_key)
    print_analysis_report(cluster_results)
    