#!/usr/bin/env python3
"""
优化功能描述脚本
使用硅基流动的 DeepSeek-R1 模型优化 functions_export.json 中的描述字段
"""
import json
import os
import sys
import requests
from typing import Dict, List, Optional
import time


class DescriptionOptimizer:
    """功能描述优化器"""

    def __init__(self, api_key: str):
        """
        初始化优化器

        Args:
            api_key: 硅基流动 API 密钥
        """
        self.api_key = api_key
        self.api_url = "https://api.siliconflow.cn/v1/chat/completions"
        self.model = "Pro/deepseek-ai/DeepSeek-R1"

        # 描述优化模板
        self.description_template = {
            "structure": [
                "痛点场景引入（1-2句）",
                "核心功能详解（3-4句）",
                "使用场景举例（2-3个具体例子）",
                "特色优势说明（2-3个独特卖点）",
                "操作便利性描述（1-2句）",
                "安全保障措施（1-2句）",
                "适用人群推荐（1-2句）"
            ],
            "examples": {
                "转账类": "以具体转账场景开头，强调便捷安全和实时性",
                "存款类": "从资金增值需求切入，突出利率优势和灵活性",
                "贷款类": "以资金短缺困境开始，展现审批快速和用途广泛",
                "查询类": "从对账需求出发，体现数据全面和操作简单"
            }
        }

    def _build_prompt(self, function_name: str, original_description: str, category: str) -> str:
        """
        构建优化描述的提示词

        Args:
            function_name: 功能名称
            original_description: 原始描述
            category: 功能类别

        Returns:
            优化提示词
        """
        return f"""你是一位资深的银行产品文案专家，擅长将功能描述改写得更加口语化、场景化、贴近用户思维，最大化语义密度。

【任务】
请根据以下功能信息，优化功能描述，使其更加详细、生动、贴近用户实际使用场景。

【功能信息】
- 功能名称：{function_name}
- 功能类别：{category}
- 原始描述：{original_description}

【优化要求】
1. 描述结构应包含以下要素（根据实际情况灵活组合，不必严格按顺序）：
   - 痛点场景引入（1-2句）：以用户实际遇到的问题或需求开头
   - 核心功能详解（3-4句）：清晰说明功能能做什么
   - 使用场景举例（2-3个具体例子）：列举典型应用场景
   - 特色优势说明（2-3个独特卖点）：突出与其他功能的差异化优势
   - 操作便利性描述（1-2句）：强调使用的简单便捷
   - 安全保障措施（1-2句）：如适用，说明安全性
   - 适用人群推荐（1-2句）：适合什么样的用户使用

2. 根据功能类别采用不同的切入点：
   - 转账类：以具体转账场景开头，强调便捷安全和实时性
   - 存款类：从资金增值需求切入，突出利率优势和灵活性
   - 贷款类：以资金短缺困境开始，展现审批快速和用途广泛
   - 查询类：从对账需求出发，体现数据全面和操作简单
   - 其他类别：根据实际功能特点灵活处理

3. 语言风格：
   - 口语化、亲切自然
   - 避免官方套话
   - 多用问句引入场景
   - 使用具体数字和细节增强可信度

【参考示例】
功能名称：附近网点
原描述：定位并显示您附近的银行网点。方便客户前往银行网点办理业务。
优化后：需要提现或办理现金业务?帮您找到附近的ATM机和银行网点。无论是从自动取款机支取资金,还是到柜台办理存取款,都能快速定位最近的服务地点。提供详细地址、营业时间、地图导航,让您方便地找到可以提取现金的ATM自助设备和银行营业厅,满足您的资金周转需求。

【输出格式】
只需输出优化后的描述文本，不要包含任何其他内容、标题或说明。直接给出可以替换原描述的优化文本。
"""

    def optimize_description(self, function_name: str, original_description: str,
                           category: str) -> Optional[str]:
        """
        调用 DeepSeek-R1 优化单个功能描述

        Args:
            function_name: 功能名称
            original_description: 原始描述
            category: 功能类别

        Returns:
            优化后的描述，失败返回 None
        """
        prompt = self._build_prompt(function_name, original_description, category)

        headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json"
        }

        payload = {
            "model": self.model,
            "messages": [
                {
                    "role": "system",
                    "content": "你是一位资深的银行产品文案专家，擅长将功能描述改写得更加口语化、场景化、贴近用户思维。"
                },
                {
                    "role": "user",
                    "content": prompt
                }
            ],
            "temperature": 0.7,
            "max_tokens": 2048
        }

        try:
            print(f"  正在优化功能: {function_name}...")
            response = requests.post(
                self.api_url,
                json=payload,
                headers=headers,
                timeout=60
            )
            response.raise_for_status()

            result = response.json()
            optimized_description = result['choices'][0]['message']['content'].strip()

            # 移除可能的引号包裹
            if optimized_description.startswith('"') and optimized_description.endswith('"'):
                optimized_description = optimized_description[1:-1]

            print(f"  ✓ 优化成功")
            return optimized_description

        except requests.exceptions.Timeout:
            print(f"  ✗ API 调用超时")
            return None
        except requests.exceptions.RequestException as e:
            print(f"  ✗ API 调用失败: {str(e)}")
            return None
        except (KeyError, IndexError) as e:
            print(f"  ✗ 响应解析失败: {str(e)}")
            return None

    def optimize_functions_file(self, input_file: str, output_file: str,
                               start_index: int = 0, limit: Optional[int] = None):
        """
        优化整个功能文件

        Args:
            input_file: 输入 JSON 文件路径
            output_file: 输出 JSON 文件路径
            start_index: 从第几个功能开始处理（用于断点续传）
            limit: 最多处理多少个功能（None 表示全部处理）
        """
        # 读取原始数据
        print(f"正在读取文件: {input_file}")
        with open(input_file, 'r', encoding='utf-8') as f:
            functions = json.load(f)

        print(f"共找到 {len(functions)} 个功能")

        # 确定处理范围
        end_index = len(functions) if limit is None else min(start_index + limit, len(functions))
        print(f"将处理第 {start_index + 1} 到第 {end_index} 个功能")

        # 优化每个功能描述
        processed_count = 0
        success_count = 0

        for i in range(start_index, end_index):
            func = functions[i]
            print(f"\n[{i + 1}/{len(functions)}] 处理功能: {func['name']}")
            print(f"  原描述: {func['descript'][:50]}...")

            optimized = self.optimize_description(
                func['name'],
                func['descript'],
                func.get('category', '其他')
            )

            if optimized:
                func['descript'] = optimized
                success_count += 1
                print(f"  新描述: {optimized[:50]}...")
            else:
                print(f"  保留原描述")

            processed_count += 1

            # 每处理 5 个功能保存一次，避免数据丢失
            if (i + 1) % 5 == 0:
                print(f"\n保存中间结果到: {output_file}")
                with open(output_file, 'w', encoding='utf-8') as f:
                    json.dump(functions, f, ensure_ascii=False, indent=2)

            # 避免请求过快，稍作延迟
            time.sleep(1)

        # 保存最终结果
        print(f"\n保存最终结果到: {output_file}")
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(functions, f, ensure_ascii=False, indent=2)

        print(f"\n优化完成!")
        print(f"  处理总数: {processed_count}")
        print(f"  成功优化: {success_count}")
        print(f"  失败/跳过: {processed_count - success_count}")


def main():
    """主函数"""
    # 从环境变量或配置获取 API Key
    api_key = os.environ.get('SILICONFLOW_API_KEY', 'sk-iiqrscvxpnomsjmissalpxylgwbmoszzoawkgtowyuqirejb')

    # 文件路径
    input_file = 'functions_export.json'
    output_file = 'functions_export_optimized.json'

    # 解析命令行参数
    start_index = 0
    limit = None

    if len(sys.argv) > 1:
        start_index = int(sys.argv[1])
    if len(sys.argv) > 2:
        limit = int(sys.argv[2])

    # 创建优化器并执行
    optimizer = DescriptionOptimizer(api_key)
    optimizer.optimize_functions_file(input_file, output_file, start_index, limit)


if __name__ == '__main__':
    main()
