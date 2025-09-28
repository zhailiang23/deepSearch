# 搜索管理系统 (deepSearch)

一个基于Spring Boot 3和Vue.js 3的企业级搜索管理系统，提供搜索日志分析、热词统计、数据导入和可视化等功能。

## 🚀 功能特性

### 核心功能
- **搜索日志管理**: 完整的搜索行为记录和分析
- **热词统计分析**: 智能提取和分析搜索热词
- **数据可视化**: 丰富的图表和统计报表
- **用户行为分析**: 搜索模式和趋势分析
- **实时监控**: 系统性能和使用情况监控

### 热词统计功能
- **智能分词**: 支持中文分词和词性分析
- **多维筛选**: 按时间、用户、搜索空间等维度分析
- **可视化展示**: 词云图、趋势图、分布图等多种展示方式
- **实时更新**: 热词数据实时计算和更新
- **导出功能**: 支持Excel、PDF等格式导出

### 技术特色
- **高性能**: 优化的数据库查询和缓存策略
- **高可用**: 微服务架构，支持水平扩展
- **安全可靠**: JWT认证，权限控制，数据加密
- **易于扩展**: 模块化设计，插件化架构

## 📋 技术栈

### 后端技术
- **框架**: Spring Boot 3.2.1
- **语言**: Java 17
- **数据库**: PostgreSQL
- **缓存**: Redis
- **安全**: Spring Security + JWT
- **文档**: Springdoc OpenAPI 3

### 前端技术
- **框架**: Vue.js 3.5.18
- **语言**: TypeScript
- **状态管理**: Pinia
- **UI组件**: Reka UI + TailwindCSS
- **图表**: ECharts
- **构建工具**: Vite

### 开发工具
- **容器化**: Docker + Docker Compose
- **API测试**: Swagger UI
- **代码质量**: ESLint, Prettier
- **测试**: JUnit 5, Vitest

## 🛠️ 快速开始

### 环境要求

- Java 17+
- Node.js 18+
- PostgreSQL 13+
- Redis 6+
- Docker & Docker Compose

### 安装部署

#### 1. 克隆项目

```bash
git clone <repository-url>
cd epic-statistics-hot-word
```

#### 2. 环境配置

```bash
# 复制环境配置文件
cp .env.example .env

# 编辑配置文件
vim .env
```

#### 3. 使用Docker启动

```bash
# 启动完整环境
./deploy.sh dev up

# 查看服务状态
./deploy.sh dev logs

# 停止服务
./deploy.sh dev down
```

#### 4. 手动启动（开发模式）

**启动后端服务**
```bash
cd backend
./mvnw clean compile
./mvnw spring-boot:run
```

**启动前端服务**
```bash
cd frontend
npm install
npm run dev
```

### 访问地址

- **前端应用**: http://localhost:3000
- **后端API**: http://localhost:8080
- **API文档**: http://localhost:8080/swagger-ui.html
- **健康检查**: http://localhost:8080/actuator/health

## 📖 文档

### 用户文档
- [热词统计功能使用指南](docs/user-guide/hot-word-features.md)
- [系统操作手册](docs/user-guide/system-manual.md)

### 开发文档
- [热词统计开发指南](docs/developer/hot-word-development.md)
- [API接口文档](docs/api/hot-word-statistics.md)
- [数据库设计文档](docs/database/schema.md)

### 部署文档
- [Docker部署指南](docs/deployment/docker.md)
- [生产环境配置](docs/deployment/production.md)
- [监控和运维](docs/deployment/monitoring.md)

## 🔧 开发指南

### 项目结构

```
epic-statistics-hot-word/
├── backend/                 # Spring Boot后端
│   ├── src/main/java/
│   │   └── com/ynet/mgmt/
│   │       ├── searchlog/   # 搜索日志模块
│   │       ├── config/      # 配置类
│   │       └── common/      # 公共组件
│   └── src/main/resources/
├── frontend/                # Vue.js前端
│   ├── src/
│   │   ├── components/      # 组件
│   │   ├── views/          # 页面
│   │   ├── stores/         # 状态管理
│   │   └── api/            # API调用
├── docs/                   # 文档
├── docker/                 # Docker配置
└── scripts/               # 脚本文件
```

### 开发规范

#### 代码规范
- 后端遵循阿里巴巴Java开发规范
- 前端使用ESLint + Prettier
- 使用TypeScript严格模式
- 编写完整的单元测试

#### Git提交规范
```
feat: 新功能
fix: Bug修复
docs: 文档更新
style: 代码格式
refactor: 重构
test: 测试
chore: 构建过程或辅助工具的变动
```

#### API设计规范
- RESTful风格
- 统一响应格式
- 完整的错误处理
- OpenAPI文档

### 热词统计API示例

```bash
# 获取热词统计
curl -X GET "http://localhost:8080/api/search-logs/hot-words?limit=10&minWordLength=2" \
  -H "Authorization: Bearer <your-token>"

# 获取统计数据
curl -X GET "http://localhost:8080/api/search-logs/statistics?startTime=2024-01-01 00:00:00&endTime=2024-01-07 23:59:59" \
  -H "Authorization: Bearer <your-token>"
```

## 🧪 测试

### 运行后端测试

```bash
cd backend
./mvnw test
```

### 运行前端测试

```bash
cd frontend
npm run test
npm run test:coverage
```

### E2E测试

```bash
npm run test:e2e
```

## 📊 监控和性能

### 性能指标
- 响应时间: < 200ms (P95)
- 吞吐量: > 1000 QPS
- 可用性: > 99.9%
- 内存使用: < 2GB

### 监控工具
- **应用监控**: Spring Boot Actuator
- **日志监控**: Logback + ELK Stack
- **性能监控**: Micrometer + Prometheus
- **错误追踪**: Sentry

## 🔒 安全

### 认证和授权
- JWT Token认证
- 角色权限控制
- API访问限制
- 敏感数据加密

### 安全措施
- HTTPS通信
- CSRF防护
- XSS防护
- SQL注入防护
- 输入验证和过滤

## 🤝 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

### 贡献要求
- 代码符合规范
- 包含单元测试
- 更新相关文档
- 通过所有检查

## 📝 更新日志

### v1.2.0 (2024-01-07)
- ✨ 新增热词统计功能
- ✨ 支持词云图可视化
- ✨ 增加实时数据更新
- 🐛 修复查询性能问题
- 📝 完善API文档

### v1.1.0 (2023-12-15)
- ✨ 新增搜索日志分析
- ✨ 支持数据导出功能
- 🔧 优化数据库性能
- 📝 增加用户使用指南

### v1.0.0 (2023-11-20)
- 🎉 初始版本发布
- ✨ 基础搜索管理功能
- ✨ 用户权限系统
- 📝 基础文档和部署指南

## 📞 支持

### 获取帮助
- **文档**: [查看完整文档](docs/README.md)
- **API文档**: [Swagger UI](http://localhost:8080/swagger-ui.html)
- **问题反馈**: [GitHub Issues](https://github.com/your-org/epic-statistics-hot-word/issues)

### 联系方式
- **邮箱**: support@example.com
- **在线客服**: 工作日 9:00-18:00

## 📄 许可证

本项目基于 [MIT License](LICENSE) 开源协议。

## 🙏 致谢

感谢所有为本项目做出贡献的开发者和用户。

---

**💡 提示**: 如果您是首次使用，建议先阅读[快速开始指南](docs/user-guide/quick-start.md)和[API文档](docs/api/hot-word-statistics.md)。