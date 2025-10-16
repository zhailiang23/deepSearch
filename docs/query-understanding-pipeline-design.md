# 查询理解管道（Query Understanding Pipeline）实现方案

> 设计日期：2025-01-16
> 版本：v1.0

## 📋 方案概述

基于对 Elasticsearch、现代 NLP 技术和现有系统架构的深入研究，设计了一个**端到端查询意图管道**，能够智能地解析、丰富和转换用户查询，使搜索结果更符合用户真实意图。

---

## 🏗️ 架构设计

### 总体架构：应用层管道 + 多层处理

```
用户查询 → QueryUnderstandingPipeline → 增强查询 → Elasticsearch → 结果
          ↓                           ↑
    [多个处理器]                   [Python NLP]
          ↓                           ↑
    [QueryContext]               [SiliconFlow LLM]
```

### 五层管道架构

#### **Layer 1: 预处理层（Preprocessing）**
- **TextNormalizationProcessor**: 文本清洗、去除特殊字符、空白标准化
- **PinyinConversionProcessor**: 拼音转换（利用现有拼音能力）
- **TraditionalSimplifiedProcessor**: 繁简转换

#### **Layer 2: 分析层（Analysis）**
- **TypoCorrectionProcessor**: 错别字检测与纠正
  - 使用 Elasticsearch Fuzzy Query + Term Suggester
  - 基于编辑距离的拼写检查
- **PhraseDetectionProcessor**: 短语识别
  - N-gram 分析
  - 基于词典的短语匹配
  - 与热门话题集成
- **NERProcessor**: 命名实体识别
  - 调用 Python NLP 服务或 SiliconFlow LLM
  - 识别人名、地名、机构名、产品名等
- **IntentClassificationProcessor**: 意图分类
  - 使用 SiliconFlow LLM 识别用户意图
  - 意图类型：信息查询、导航、操作指令、比较等

#### **Layer 3: 扩展层（Expansion）**
- **SynonymExpansionProcessor**: 同义词扩展
  - 使用 Elasticsearch Synonym Graph Filter
  - 支持领域特定同义词词典
- **SemanticExpansionProcessor**: 语义相关词扩展
  - 基于语义相似度（SiliconFlow Embedding）
  - 扩展相关术语
- **HotTopicIntegrationProcessor**: 热门话题关联
  - 利用现有热门话题数据

#### **Layer 4: 优化层（Optimization）**
- **QueryRewriteProcessor**: 查询重写
  - 基于识别的意图和实体重写查询
  - 优化查询结构
- **FieldBoostProcessor**: 字段权重调整
  - 根据意图和实体类型动态调整字段 boost
- **DisambiguationProcessor**: 消歧处理
  - 多义词处理
  - 上下文相关性分析

#### **Layer 5: 执行层（Execution）**
- **QueryBuilderProcessor**: 构建最终 ES 查询
  - 生成混合查询（Lexical + Semantic + Fuzzy）
  - 组合多种查询策略
  - 应用权重和过滤条件

---

## 🔧 技术实现

### 后端（Spring Boot）

#### 1. 核心包结构
```
com.ynet.mgmt.queryunderstanding/
├── pipeline/
│   ├── QueryUnderstandingPipeline.java      // 主管道
│   ├── PipelineConfiguration.java           // 管道配置
│   └── ProcessorChain.java                  // 责任链模式
├── context/
│   ├── QueryContext.java                    // 查询上下文
│   ├── IntentType.java                      // 意图类型枚举
│   └── EntityType.java                      // 实体类型枚举
├── processor/
│   ├── QueryProcessor.java                  // 处理器接口
│   ├── preprocessing/
│   │   ├── TextNormalizationProcessor.java
│   │   ├── PinyinConversionProcessor.java
│   │   └── TraditionalSimplifiedProcessor.java
│   ├── analysis/
│   │   ├── TypoCorrectionProcessor.java
│   │   ├── PhraseDetectionProcessor.java
│   │   ├── NERProcessor.java
│   │   └── IntentClassificationProcessor.java
│   ├── expansion/
│   │   ├── SynonymExpansionProcessor.java
│   │   ├── SemanticExpansionProcessor.java
│   │   └── HotTopicIntegrationProcessor.java
│   ├── optimization/
│   │   ├── QueryRewriteProcessor.java
│   │   ├── FieldBoostProcessor.java
│   │   └── DisambiguationProcessor.java
│   └── execution/
│       └── QueryBuilderProcessor.java
├── service/
│   ├── QueryUnderstandingService.java       // 服务接口
│   └── impl/
│       └── QueryUnderstandingServiceImpl.java
├── client/
│   └── PythonNLPClient.java                 // Python 服务客户端
├── dto/
│   ├── QueryUnderstandingRequest.java
│   ├── QueryUnderstandingResponse.java
│   ├── NERRequest.java
│   ├── NERResponse.java
│   └── IntentRequest.java
├── entity/
│   ├── SynonymConfig.java                   // 同义词配置
│   ├── IntentTemplate.java                  // 意图模板
│   └── PipelineConfig.java                  // 管道配置实体
├── repository/
│   ├── SynonymConfigRepository.java
│   ├── IntentTemplateRepository.java
│   └── PipelineConfigRepository.java
└── controller/
    ├── QueryUnderstandingController.java    // API 控制器
    └── PipelineManagementController.java    // 管道管理
```

#### 2. 核心类设计

**QueryContext.java**
```java
@Data
public class QueryContext {
    // 原始查询
    private String originalQuery;

    // 各阶段处理后的查询
    private String normalizedQuery;
    private String correctedQuery;
    private String expandedQuery;

    // 识别的信息
    private List<Entity> entities;
    private IntentType intent;
    private Double intentConfidence;

    // 扩展信息
    private List<String> synonyms;
    private List<String> relatedTerms;
    private List<String> detectedPhrases;
    private List<String> hotTopics;

    // 查询元数据
    private Map<String, Object> metadata;

    // 最终的 ES 查询
    private Query elasticsearchQuery;

    // 性能追踪
    private Map<String, Long> processorTimings;
}
```

**QueryProcessor.java (接口)**
```java
public interface QueryProcessor {
    /**
     * 处理查询上下文
     */
    void process(QueryContext context);

    /**
     * 处理器名称
     */
    String getName();

    /**
     * 是否启用
     */
    boolean isEnabled();

    /**
     * 处理器优先级
     */
    int getPriority();
}
```

#### 3. 配置（application.yml 扩展）
```yaml
# 查询理解管道配置
query-understanding:
  # 是否启用查询理解管道
  enabled: ${QUERY_UNDERSTANDING_ENABLED:true}

  # 管道超时配置
  timeout:
    total: ${QUERY_UNDERSTANDING_TOTAL_TIMEOUT:5000}  # 总超时 5秒
    processor: ${QUERY_UNDERSTANDING_PROCESSOR_TIMEOUT:1000}  # 单个处理器超时 1秒

  # 缓存配置
  cache:
    enabled: ${QUERY_UNDERSTANDING_CACHE_ENABLED:true}
    ttl: ${QUERY_UNDERSTANDING_CACHE_TTL:PT10M}  # 缓存 10 分钟
    max-size: ${QUERY_UNDERSTANDING_CACHE_MAX_SIZE:10000}

  # 处理器配置
  processors:
    # 预处理层
    text-normalization:
      enabled: true
      priority: 100
    pinyin-conversion:
      enabled: true
      priority: 90
    traditional-simplified:
      enabled: true
      priority: 85

    # 分析层
    typo-correction:
      enabled: true
      priority: 80
      fuzziness: AUTO
      min-similarity: 0.7
    phrase-detection:
      enabled: true
      priority: 75
      min-phrase-length: 2
      max-phrase-length: 5
    ner:
      enabled: true
      priority: 70
      provider: siliconflow  # siliconflow 或 python
    intent-classification:
      enabled: true
      priority: 65
      confidence-threshold: 0.6

    # 扩展层
    synonym-expansion:
      enabled: true
      priority: 60
      max-synonyms: 5
    semantic-expansion:
      enabled: true
      priority: 55
      max-related-terms: 3
      similarity-threshold: 0.7
    hot-topic-integration:
      enabled: true
      priority: 50

    # 优化层
    query-rewrite:
      enabled: true
      priority: 45
    field-boost:
      enabled: true
      priority: 40
    disambiguation:
      enabled: true
      priority: 35

    # 执行层
    query-builder:
      enabled: true
      priority: 10

  # Python NLP 服务配置
  python-nlp:
    service-url: ${PYTHON_NLP_URL:http://localhost:5001}
    timeout: ${PYTHON_NLP_TIMEOUT:3000}
    endpoints:
      ner: /nlp/ner
      intent: /nlp/intent
      expansion: /nlp/expansion

  # 智能跳过策略
  smart-skip:
    enabled: true
    simple-query-threshold: 2  # 少于2个字符跳过复杂处理
    skip-processors-for-simple:
      - ner
      - intent-classification
      - semantic-expansion
```

---

### Python NLP 服务扩展

#### 1. 新增文件
```
python/
├── nlp_service.py          // NLP 服务主文件（新增）
├── ner_handler.py          // NER 处理器（新增）
├── intent_handler.py       // 意图分类处理器（新增）
├── expansion_handler.py    // 查询扩展处理器（新增）
└── requirements.txt        // 更新依赖
```

#### 2. 新增依赖
```txt
# NLP 库
spacy>=3.7.0
spacy-transformers>=1.3.0
jieba>=0.42.1

# 可选：使用 transformers 进行高级 NER
transformers>=4.35.0
```

#### 3. API 端点
```python
# nlp_service.py
from fastapi import FastAPI

app = FastAPI()

@app.post("/nlp/ner")
async def extract_entities(request: NERRequest):
    """命名实体识别"""
    # 使用 spaCy 或调用 SiliconFlow LLM
    pass

@app.post("/nlp/intent")
async def classify_intent(request: IntentRequest):
    """意图分类"""
    # 使用 LLM 进行意图识别
    pass

@app.post("/nlp/expansion")
async def expand_query(request: ExpansionRequest):
    """查询扩展"""
    # 基于语义相似度扩展
    pass
```

---

### 前端管理界面

#### 1. 新增页面
```
frontend/src/
├── views/
│   └── QueryUnderstandingPage.vue    // 查询理解测试页面
├── components/
│   └── queryUnderstanding/
│       ├── PipelineDebugger.vue      // 管道调试器
│       ├── ProcessorConfig.vue       // 处理器配置
│       ├── SynonymManager.vue        // 同义词管理
│       ├── IntentTemplateEditor.vue  // 意图模板编辑
│       └── PerformanceMonitor.vue    // 性能监控
└── api/
    └── queryUnderstanding.ts         // API 调用
```

#### 2. 核心功能
- **查询测试工具**: 输入查询，实时查看各阶段处理结果
- **管道配置**: 启用/禁用处理器、调整参数
- **同义词管理**: CRUD 同义词配置
- **性能监控**: 各处理器耗时、缓存命中率等
- **A/B 测试**: 对比智能搜索 vs 普通搜索效果

---

## 📊 数据流示例

### 示例查询："生活缴费"

```
1. 预处理层
   原始: "生活缴费"
   标准化: "生活缴费"
   拼音: "shenghuo jiaofei"

2. 分析层
   错别字检测: 无错误
   短语识别: ["生活缴费"]
   实体识别: [Entity(text="生活缴费", type="SERVICE")]
   意图分类: IntentType.INFORMATION_QUERY (confidence=0.95)

3. 扩展层
   同义词: ["生活缴费", "缴费服务", "生活服务"]
   相关词: ["水电费", "燃气费", "物业费"]
   热门话题: ["便民服务"]

4. 优化层
   查询重写: "生活缴费 OR 缴费服务 OR 生活服务"
   字段权重: {title: 2.0, name: 1.8, content: 1.0}
   消歧: 无歧义

5. 执行层
   最终查询: Bool Query {
     should: [
       match(title, "生活缴费", boost=2.0),
       match(name, "缴费服务", boost=1.8),
       semantic(embedding, vector, boost=1.5)
     ]
   }
```

---

## 🚀 实施路线图

### **Phase 1: 基础框架（MVP）** - 预计 2 周
✅ **目标**: 建立可工作的基础管道

**后端任务**:
1. 创建 `queryunderstanding` 包结构
2. 实现 QueryContext 和 QueryProcessor 接口
3. 实现 QueryUnderstandingPipeline 主管道（责任链模式）
4. 实现 3 个基础处理器：
   - TextNormalizationProcessor
   - TypoCorrectionProcessor（ES Fuzzy Query）
   - SynonymExpansionProcessor（ES Synonym）
5. 创建 QueryUnderstandingService 和 Controller
6. 集成到 ElasticsearchDataService
7. 添加配置支持（application.yml）

**测试**:
- 单元测试（各处理器）
- 集成测试（完整管道）
- API 测试

**交付物**:
- 可工作的基础管道
- API 文档
- 配置文档

---

### **Phase 2: NLP 能力** - ✅ 已完成
✅ **目标**: 添加智能分析能力

**已完成任务**:
1. ✅ 实现 QueryUnderstandingLlmService（LLM服务抽象层）
2. ✅ 实现 QueryUnderstandingLlmServiceImpl（SiliconFlow API集成）
3. ✅ 实现 IntentRecognitionProcessor（意图识别处理器）
4. ✅ 实现 EntityExtractionProcessor（实体抽取处理器）
5. ✅ 实现 QueryRewriteProcessor（查询重写处理器）
6. ✅ 增强 TypoCorrectionProcessor（ES Term Suggester框架）
7. ✅ 扩展 IntentType 枚举（新增 QUERY/COMMAND/QUESTION）
8. ✅ 扩展 QueryContext（新增 rewrittenQuery 字段）
9. ✅ 添加完整的 LLM 配置（application.yml）
10. ✅ 创建标准化 LLM 通信 DTOs（LlmApiRequest/Response）

**技术实现**:
- **LLM集成**: 使用 SiliconFlow Qwen2.5-7B-Instruct 模型
- **意图识别**: 三分类（QUERY/COMMAND/QUESTION），带置信度计算
- **实体抽取**: 支持8种实体类型（PERSON/LOCATION/ORGANIZATION/DATE/TIME/MONEY/PRODUCT/EVENT）
- **查询重写**: 基于意图和实体的智能查询优化
- **错别字纠正**: ES Term Suggester集成框架（Phase 2基础）

**配置选项**:
```yaml
query-understanding:
  llm:
    enabled: false  # 默认关闭（避免成本）
    siliconflow:
      api-key: xxx
      api-url: https://api.siliconflow.cn/v1/chat/completions
      model: Qwen/Qwen2.5-7B-Instruct
      temperature: 0.3
      max-tokens: 500
      timeout: 10000
```

**性能和容错**:
- 所有LLM调用带超时保护（10秒）
- 服务不可用时自动跳过
- 失败自动降级（使用默认值）
- 不影响现有搜索功能

**交付物**:
- ✅ 完整的LLM服务层
- ✅ 3个新增NLP处理器（意图识别、实体抽取、查询重写）
- ✅ 增强的错别字纠正框架
- ✅ 标准化配置管理
- ✅ 编译通过（271个源文件）
- ✅ 完整的错误处理和日志记录

---

### **Phase 3: 智能扩展** - 预计 1.5 周
✅ **目标**: 增强查询扩展能力

**后端任务**:
1. 实现 SemanticExpansionProcessor
2. 实现 HotTopicIntegrationProcessor
3. 实现 QueryRewriteProcessor
4. 实现 DisambiguationProcessor
5. 优化 QueryBuilderProcessor

**Python 任务**:
1. 实现查询扩展端点（语义相似度）

**测试**:
- 扩展效果测试
- 查询质量评估

**交付物**:
- 完整的查询扩展能力
- 高级查询重写
- 消歧处理

---

### **Phase 4: 优化和可视化** - 预计 2 周
✅ **目标**: 性能优化和管理界面

**后端任务**:
1. 实现多层缓存（Redis）
2. 实现异步处理（CompletableFuture）
3. 实现智能跳过策略
4. 添加性能监控和指标
5. 实现 A/B 测试框架

**前端任务**:
1. 创建 QueryUnderstandingPage.vue
2. 实现 PipelineDebugger 组件（查询测试工具）
3. 实现 ProcessorConfig 组件（管道配置）
4. 实现 SynonymManager 组件（同义词管理）
5. 实现 PerformanceMonitor 组件（性能监控）

**测试**:
- 性能压测
- 前端 E2E 测试
- 用户体验测试

**交付物**:
- 完整的前端管理界面
- 性能优化版本
- A/B 测试框架
- 完整文档

---

## 🎯 技术亮点

1. **模块化设计**: 责任链模式，每个处理器独立可配置
2. **充分利用现有能力**:
   - Elasticsearch（Fuzzy、Synonym、Semantic）
   - SiliconFlow（LLM、Embedding）
   - 现有拼音、热门话题功能
3. **性能优化**:
   - 多层缓存
   - 异步处理
   - 智能跳过
   - 超时保护
4. **可观测性**:
   - 各阶段耗时追踪
   - 调试工具
   - 性能监控
5. **向后兼容**:
   - 保留直接搜索模式
   - 通过配置或参数选择模式
6. **灵活扩展**:
   - 易于添加新处理器
   - 可配置处理器顺序和参数

---

## 💡 关键决策

1. **为什么选择应用层管道而非 ES Ingest Pipeline？**
   - ES Ingest Pipeline 主要用于索引时处理
   - 应用层管道更灵活，可以集成 LLM、Python NLP 等
   - 更容易调试和监控

2. **为什么使用 SiliconFlow 而非本地 NLP 模型？**
   - SiliconFlow 提供高质量的中文 LLM
   - 无需维护模型
   - 系统已有集成

3. **为什么分 4 个 Phase？**
   - 每个阶段都能交付可用功能
   - 降低风险，可以逐步迭代
   - 便于测试和验证

---

## 📈 预期效果

1. **搜索准确率提升 20-30%**（通过错别字纠正和同义词扩展）
2. **用户意图理解准确率 >80%**（通过 LLM 意图分类）
3. **平均查询响应时间 <500ms**（通过缓存和优化）
4. **缓存命中率 >60%**（相似查询复用结果）

---

## ⚠️ 风险和缓解

1. **延迟增加风险**:
   - 缓解：多层缓存、异步处理、智能跳过
2. **成本增加（SiliconFlow API 调用）**:
   - 缓解：缓存 NLP 结果、批处理、按需启用
3. **复杂度增加**:
   - 缓解：充分的文档、调试工具、分阶段实施

---

## 📝 总结

这是一个**完整、可实施、分阶段交付**的查询理解管道方案，它：
- ✅ 满足所有需求（短语识别、错别字检测、同义词、意图分类、术语扩展、消歧）
- ✅ 充分利用现有系统能力
- ✅ 技术栈成熟可靠
- ✅ 性能和成本可控
- ✅ 可观测、可调试、可配置

建议从 **Phase 1（基础框架）** 开始实施，验证架构可行性后逐步推进。
