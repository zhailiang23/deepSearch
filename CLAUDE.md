# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 提供在此代码库工作时的指导。

> 仔细思考并实现最简洁的解决方案,尽可能少地修改代码。

## 项目概述

deepSearch 是一个搜索分析和管理系统,采用 **Spring Boot 3.2.1** (Java 17) 后端和 **Vue.js 3.5.18** (TypeScript) 前端构建,具有搜索日志分析、热词统计、AI聚类和查询理解管道等功能。

## 架构

### 后端 (Spring Boot)
- **包结构**: `com.ynet.mgmt.*`
- **核心模块**:
  - `searchlog` - 搜索日志管理和分析
  - `searchdata` - Elasticsearch数据检索和搜索召回演示
  - `statistics` - 统计分析和报表
  - `clustering` - AI聚类分析(集成Python统一服务)
  - `queryunderstanding` - LLM驱动的查询理解管道(带缓存)
  - `hotTopic` - 热点话题分析和趋势检测
  - `sensitiveWord` - 敏感词过滤和管理
  - `imagerecognition` - 图像识别AI(用于基于图像的搜索查询)
  - `jsonimport` - 批量数据导入(异步处理)
  - `searchspace`, `channel` - 搜索空间和渠道管理
  - `user`, `auth`, `role` - 用户认证授权(JWT)
  - `monitor` - 系统监控和性能跟踪
  - `common` - 共享工具和基础实体(BaseEntity)
- **分层**: Controller → Service → Repository → Entity (继承 `BaseEntity`)
- **数据库**: MySQL 8.0 with JPA/Hibernate (snake_case命名)
- **缓存**: Redis 7 + Caffeine (多级缓存)
- **搜索引擎**: Elasticsearch 8.11.0 with 自定义拼音分析器
- **端口**: 8080 (API上下文路径: `/api`)

### 前端 (Vue.js)
- **框架**: Vue 3.5.18 + TypeScript + Vite
- **状态管理**: Pinia (演示配置持久化到localStorage)
- **UI组件**: Reka UI + TailwindCSS + Element Plus
- **图表**: ECharts 数据可视化
- **代码编辑器**: CodeMirror 6 (用于JSON编辑)
- **测试**: Playwright (E2E), Vitest (单元测试)
- **端口**: 3000
- **关键页面**:
  - 搜索日志分析(带热词和统计)
  - 聚类分析(带散点图和话题可视化)
  - 搜索召回演示(移动端优化,可配置搜索参数)
  - 数据导入(JSON批量导入,进度跟踪)

### AI服务 (Python统一服务)
- **统一服务** (端口 5002) - FastAPI服务器 (`python/main_service.py`)
  - **Embedding服务**: 使用sentence transformers进行文本向量化
  - **Rerank服务**: 使用cross-encoders进行文档重排序
  - **Cluster服务**: DBSCAN聚类 + UMAP降维
  - 集成SiliconFlow API进行嵌入和LLM调用
  - 健康检查和完善的错误处理
- **注意**: 端口从5000改为5002以避免与macOS ControlCenter冲突

### 基础设施
- **容器化**: Docker + Docker Compose
- **服务**: backend, frontend, mysql, redis, elasticsearch, python-service
- **数据库**: MySQL 8.0 (不是PostgreSQL)
- **健康检查**: 所有服务都内置健康检查

## 开发命令

### 后端开发
```bash
cd backend

# 重要: 编译前必须先clean (项目规则)
./mvnw clean compile

# 运行测试
./mvnw test

# 构建JAR
./mvnw package

# 本地开发 (端口8080)
./mvnw spring-boot:run
```

### 前端开发
```bash
cd frontend

# 安装依赖
npm install

# 开发服务器 (端口3000)
npm run dev

# TypeScript验证 (完成前必须通过)
npm run type-check

# 生产构建 (完成前必须成功)
npm run build

# E2E测试
npm run test:e2e
npm run test:e2e:headed    # 带浏览器UI
npm run test:e2e:debug     # 调试模式

# 性能测试
npm run test:performance
npm run test:k6            # 使用k6进行负载测试
```

### Python服务
```bash
# 重要: 使用uv进行Python环境管理
cd python

# 创建虚拟环境
uv venv

# 安装依赖
uv pip install -r requirements.txt

# 运行统一服务 (端口5002 - 从5000改为5002)
uv run uvicorn main_service:app --host 0.0.0.0 --port 5002

# 服务端点:
# - Embedding: POST /embedding/v1/embeddings
# - Rerank: POST /rerank/v1/rerank
# - Cluster: POST /cluster/api/cluster
# - Health: GET /health
# - Docs: GET /docs
```

### Docker操作
```bash
# 启动所有服务
docker-compose up -d

# 查看日志
docker-compose logs -f [service-name]

# 停止所有服务
docker-compose down

# 重建特定服务
docker-compose build [service-name]
docker-compose up -d [service-name]

# 连接MySQL
docker-compose exec mysql mysql -u mgmt_user -p mgmt_db

# 连接Redis
docker-compose exec redis redis-cli
```

## 代码风格和规范

### Java/Spring Boot
- **命名**: PascalCase类名, camelCase方法/变量
- **实体**: 必须继承 `BaseEntity` (位于 `com.ynet.mgmt.common`)
- **实体模式**:
  - 使用 `@Entity` 配合 `@Table` 和 `@Index` 注解
  - 在实体中实现业务逻辑方法(如 `isActive()`, `isLocked()`)
  - 使用 `@Version` 进行乐观锁
  - 应用Bean Validation注解(`@NotBlank`, `@Email`等)
- **数据库**: snake_case表名/列名
- **文档**: JavaDoc使用中文注释
- **配置**: 使用 `@ConfigurationProperties` 进行外部配置
- **错误处理**: 创建继承 `RuntimeException` 的自定义异常
- **特殊库**:
  - Jieba 中文分词
  - Pinyin4j 拼音转换
  - Caffeine 本地缓存

### Vue.js/TypeScript
- **组件**: PascalCase (如 `UserMenu.vue`, `ClusterScatterChart.vue`)
- **组合式API**: 使用 `ref`/`reactive` 实现响应式
- **文件夹**: kebab-case (如 `cluster-analysis/`, `hot-topic/`)
- **类型**: 启用严格TypeScript模式
- **样式**: TailwindCSS工具类(浅绿色主题)
- **API调用**: 集中在 `src/api/` 目录
- **状态管理**: 使用 `src/stores/` 中的Pinia stores
  - 移动端演示stores将配置持久化到localStorage
  - 搜索历史支持可配置保留期

### Python (AI服务)
- **环境**: 始终使用 `uv` 进行包管理
- **API框架**: FastAPI用于Web服务
- **类型提示**: 使用Python类型注解
- **错误处理**: 适当的HTTP状态码和错误消息
- **模型管理**: 延迟加载和全局实例

### API设计
- `/api` 上下文路径下的RESTful端点
- 使用 `ApiResponse<T>` 统一响应格式
- 使用Bean Validation进行请求验证
- 适当的HTTP状态码(200, 400, 500等)

## 完成前必须检查

### 后端
1. **必须运行**: `./mvnw clean compile` (编译前必须先clean)
2. **必须通过**: `./mvnw test`
3. 验证实体继承 `BaseEntity`
4. 检查JavaDoc中文文档
5. 验证正确的异常处理

### 前端
1. **必须通过**: `npm run type-check` (无TypeScript错误)
2. **必须通过**: `npm run build` (生产构建成功)
3. 验证组件命名(PascalCase)
4. 验证文件夹命名(kebab-case)
5. 检查浅绿色主题一致性

### Python服务
1. **必须使用**: `uv` 进行所有Python操作
2. 验证 `requirements.txt` 中的所有依赖
3. 独立测试服务端点
4. 验证统一服务运行在端口5002

### 集成
1. 所有服务通过Docker Compose成功启动
2. 所有服务健康检查通过
3. 测试API: `http://localhost:8080/api/actuator/health`
4. 测试前端: `http://localhost:3000`
5. 测试Python服务: `http://localhost:5002/health`
6. **端口冲突**: 必要时杀死占用3000、8080和5002端口的进程

## 端口管理

**关键端口**:
- **3000**: 前端开发服务器
- **8080**: 后端API服务器
- **5002**: Python统一服务(因macOS ControlCenter从5000改为5002)

```bash
# 检查端口占用
lsof -i :3000
lsof -i :8080
lsof -i :5002

# 如果端口被占用则杀死进程(自动批准)
lsof -i :3000 | grep LISTEN | awk '{print $2}' | xargs kill -9
lsof -i :8080 | grep LISTEN | awk '{print $2}' | xargs kill -9
lsof -i :5002 | grep LISTEN | awk '{print $2}' | xargs kill -9
```

## 数据库操作
```bash
# 连接MySQL (不是PostgreSQL)
docker-compose exec mysql mysql -u mgmt_user -pmgmt_password mgmt_db

# 查看容器状态
docker-compose ps

# 查看MySQL日志
docker-compose logs mysql
```

## 关键集成点

### 后端-Python集成
- 后端调用Python统一服务的配置URL (默认: `http://localhost:5002`)
- Python服务需要SiliconFlow API凭证进行聚类
- `application.yml` 中的配置:
  - `cluster.python.service-url` - 聚类API端点
  - `semantic.embedding.siliconflow.*` - Embedding服务配置
  - `rerank.siliconflow.*` - Rerank服务配置
- Docker环境: `PYTHON_CLUSTER_URL=http://python-service:5002`

### 前端-后端集成
- 通过集中在 `src/api/` 模块的API调用
- 基础URL来自环境配置
- 使用Axios进行HTTP请求
- 移动端演示stores管理搜索配置并持久化到localStorage

### 查询理解管道
- LLM驱动的查询增强,带缓存
- 组件: QueryExpander, IntentDetector, ContextBuilder
- Redis缓存LLM结果(1小时TTL)
- 可配置超时和智能跳过策略
- 移动端搜索演示UI中的开关控制

## 子代理使用

- **file-analyzer**: 读取冗长文件/日志(特别是 `backend/logs/application.log`)
- **code-analyzer**: 代码分析、bug查找、逻辑追踪
- **test-runner**: 执行测试并分析结果
- **parallel-worker**: 并行任务执行(多步骤任务首选)

## 测试理念

### 通用原则
- **始终使用test-runner代理**执行测试
- **不使用mock服务** - 集成测试使用真实服务
- **顺序执行测试** - 完成当前测试再进行下一个
- **测试结构验证** - 在指责代码前先检查测试正确性
- **详细测试** - 设计便于调试的清晰测试

### 测试类型
- **后端**: 使用真实database/Redis的JUnit 5测试
- **前端**: Vitest单元测试, Playwright E2E测试
- **性能**: k6负载测试, Playwright性能测试

## 绝对规则

### 开发工作流
- ✅ **始终使用context7** 在编写代码前搜索相关信息
- ✅ **编译前始终clean**: `./mvnw clean compile`
- ✅ **Python使用uv**: 所有Python操作必须使用 `uv` 包管理器
- ✅ **优先使用parallel-worker**: 用于多步并行任务
- ✅ **使用zsh**: 终端命令应使用zsh(不是bash)

### 代码质量
- ❌ **禁止部分实现** - 完整实现所有功能
- ❌ **禁止简化** - 不要有"暂时简化"的注释
- ❌ **禁止代码重复** - 重用现有函数和常量
- ❌ **禁止死代码** - 完全删除未使用的代码
- ❌ **禁止命名不一致** - 遵循现有代码库模式
- ❌ **禁止过度工程** - 避免不必要的抽象
- ❌ **禁止关注点混合** - 正确的层分离(Controller/Service/Repository)
- ❌ **禁止资源泄漏** - 关闭连接、清除超时、移除监听器

### 测试要求
- ✅ **为每个函数实现测试**
- ✅ **禁止作弊测试** - 准确反映真实使用的测试
- ✅ **设计以暴露缺陷** - 测试必须能抓住bug

### UI/UX
- 🎨 **浅绿色主题** 前端(不需要国际化)
- 🚪 **仅使用3000、8080、5002端口** - 自动杀死冲突进程

### Git和部署
- 🔧 **代理处理**: Git命令必须忽略代理(在 `~/.zshrc` 中设置)
- 📝 **无Claude品牌**: Git提交不应包含Claude署名
- ⏸️ **不自动推送**: 等待用户明确请求再推送代码
- 🔐 **服务器密钥**: 192.168.155.54的密钥在 `/tmp/ssh_key` (来自 `~/Documents/code/tmp_prikey.ppk`)

### 基础设施
- 🐳 **Docker用于中间件**: 使用Docker部署MySQL、Redis、Elasticsearch等
- 📋 **日志位置**: 后端日志在 `backend/logs/application.log`

## 近期重要变更

### Python服务整合 (2025-10-17)
- 将聚类服务合并到端口5002的统一服务
- 删除冗余的 `cluster_api.py` (功能已在 `main_service.py` 中)
- 更新所有配置使用5002端口而不是5000/5001
- 统一服务提供: embedding、rerank和cluster API
- Docker和后端配置相应更新

### 查询理解管道
- 添加带Redis缓存的LLM驱动查询理解
- 集成到搜索召回演示,带开关控制
- 可配置超时和智能跳过策略
- UI中分离显示原始查询和增强查询
