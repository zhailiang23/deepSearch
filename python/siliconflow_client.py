"""
硅基流动 API 客户端
提供 Embedding 和 LLM 服务
"""
import requests
from typing import List, Dict, Optional
import logging
import json

logger = logging.getLogger(__name__)


class SiliconFlowClient:
    """硅基流动 API 客户端"""

    def __init__(self, embedding_config: dict, llm_config: dict):
        """
        初始化客户端

        Args:
            embedding_config: Embedding API 配置
                - api_key: API密钥
                - api_url: API地址
                - model: 模型名称
                - timeout: 超时时间(毫秒)
            llm_config: LLM API 配置
                - api_key: API密钥
                - api_url: API地址
                - model: 模型名称
                - temperature: 温度参数
                - max_tokens: 最大token数
                - timeout: 超时时间(毫秒)
        """
        self.embedding_config = embedding_config
        self.llm_config = llm_config
        logger.info(f"初始化硅基流动客户端 - Embedding模型: {embedding_config['model']}, LLM模型: {llm_config['model']}")

    def get_embeddings(self, texts: List[str], batch_size: int = 100) -> List[List[float]]:
        """
        调用硅基流动 Embedding API 获取文本向量（支持分批处理）

        Args:
            texts: 文本列表
            batch_size: 每批处理的文本数量，默认100

        Returns:
            向量列表，每个向量是一个浮点数列表

        Raises:
            Exception: API 调用失败时抛出异常
        """
        headers = {
            "Authorization": f"Bearer {self.embedding_config['api_key']}",
            "Content-Type": "application/json"
        }

        all_embeddings = []
        total_texts = len(texts)

        logger.info(f"开始调用硅基流动 Embedding API，文本总数: {total_texts}，批次大小: {batch_size}")

        # 分批处理
        for i in range(0, total_texts, batch_size):
            batch_texts = texts[i:i + batch_size]
            batch_num = i // batch_size + 1
            total_batches = (total_texts + batch_size - 1) // batch_size

            logger.info(f"处理第 {batch_num}/{total_batches} 批，文本数: {len(batch_texts)}")

            payload = {
                "model": self.embedding_config['model'],
                "input": batch_texts,
                "encoding_format": "float"
            }

            try:
                response = requests.post(
                    self.embedding_config['api_url'],
                    json=payload,
                    headers=headers,
                    timeout=self.embedding_config['timeout'] / 1000  # 转换为秒
                )
                response.raise_for_status()

                result = response.json()

                # 提取向量并按索引排序
                batch_embeddings = [
                    item['embedding']
                    for item in sorted(result['data'], key=lambda x: x['index'])
                ]

                all_embeddings.extend(batch_embeddings)
                logger.info(f"第 {batch_num} 批处理成功，已生成 {len(all_embeddings)}/{total_texts} 个向量")

            except requests.exceptions.Timeout:
                logger.error(f"第 {batch_num} 批 Embedding API 调用超时")
                raise Exception(f"向量生成超时（第{batch_num}批），请稍后重试")
            except requests.exceptions.RequestException as e:
                logger.error(f"第 {batch_num} 批 Embedding API 调用失败: {str(e)}")
                raise Exception(f"向量生成失败（第{batch_num}批）: {str(e)}")
            except (KeyError, IndexError) as e:
                logger.error(f"第 {batch_num} 批 Embedding API 响应解析失败: {str(e)}")
                raise Exception(f"向量生成响应格式错误（第{batch_num}批）")

        logger.info(f"所有向量生成完成，总数: {len(all_embeddings)}，维度: {len(all_embeddings[0]) if all_embeddings else 0}")
        return all_embeddings

    def generate_topic(self, cluster_texts: List[str]) -> Optional[Dict]:
        """
        调用硅基流动 LLM API 生成话题描述

        Args:
            cluster_texts: 同一聚类的文本列表

        Returns:
            话题描述字典，包含 topic, examples, tags 字段
            如果生成失败返回 None
        """
        # 限制样本数量，避免 prompt 过长
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

        headers = {
            "Authorization": f"Bearer {self.llm_config['api_key']}",
            "Content-Type": "application/json"
        }

        payload = {
            "model": self.llm_config['model'],
            "messages": [
                {
                    "role": "system",
                    "content": "你是有10年经验的银行业务分析师，擅长从用户反馈中提炼具体问题和诉求"
                },
                {
                    "role": "user",
                    "content": prompt
                }
            ],
            "temperature": self.llm_config['temperature'],
            "max_tokens": self.llm_config['max_tokens']
        }

        try:
            logger.info(f"调用硅基流动 LLM API 生成话题，样本数: {len(sample_texts)}")

            response = requests.post(
                self.llm_config['api_url'],
                json=payload,
                headers=headers,
                timeout=self.llm_config['timeout'] / 1000  # 转换为秒
            )
            response.raise_for_status()

            result = response.json()
            content = result['choices'][0]['message']['content'].strip()

            # 清理可能的 markdown 代码块标记
            if content.startswith('```json'):
                content = content[7:-3].strip()
            elif content.startswith('```'):
                content = content[3:-3].strip()

            topic_info = json.loads(content)

            # 验证必要字段
            if not all(key in topic_info for key in ['topic', 'examples', 'tags']):
                logger.warning("LLM 响应缺少必要字段")
                return None

            logger.info(f"成功生成话题: {topic_info['topic']}")
            return topic_info

        except requests.exceptions.Timeout:
            logger.error("LLM API 调用超时")
            return None
        except requests.exceptions.RequestException as e:
            logger.error(f"LLM API 调用失败: {str(e)}")
            return None
        except (json.JSONDecodeError, KeyError, IndexError) as e:
            logger.error(f"LLM API 响应解析失败: {str(e)}, 响应内容: {content if 'content' in locals() else 'N/A'}")
            return None
