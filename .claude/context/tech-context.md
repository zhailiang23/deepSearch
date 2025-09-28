---
created: 2025-09-21T10:31:04Z
last_updated: 2025-09-28T05:05:04Z
version: 1.8
author: Claude Code PM System
---

# Technology Context

## Technology Stack Overview

### Backend Technologies

**Core Framework**
- **Spring Boot:** 3.2.1
- **Java:** 17 (LTS)
- **Maven:** Build and dependency management

**Spring Ecosystem**
- **Spring Boot Starter Web** - REST API development
- **Spring Boot Starter Data JPA** - Database integration
- **Spring Boot Starter Security** - Authentication and authorization
- **Spring Boot Starter Data Redis** - Redis cache integration
- **Spring Boot Starter Validation** - Bean validation
- **Spring Boot Starter Actuator** - Health checks and monitoring
- **Spring Boot DevTools** - Development utilities

**搜索日志管理服务架构** ⭐ **NEW**
- **SearchLogController** - 搜索日志查询和管理控制器
- **SearchLogCleanupController** - 日志清理控制器
- **SearchLogAspect** - AOP切面自动记录搜索行为
- **SearchLogMonitor** - 搜索行为监控和统计
- **搜索日志DTO体系** - 日志查询、统计、详情等数据传输对象

**搜索数据管理服务架构** ⭐
- **ElasticsearchDataController** - 数据CRUD操作控制器
- **ElasticsearchDataService** - 数据服务层实现
- **搜索DTO体系** - 10+个数据传输对象
- **批量操作支持** - BulkOperationRequest/Response
- **文档管理** - DocumentDetail CRUD operations

**Security & Authentication**
- **Spring Security:** JWT-based authentication
- **JJWT (io.jsonwebtoken):** JWT token generation and validation
- **BCrypt:** Password hashing
- **Redis:** Token blacklist and refresh token storage

**Database & Persistence**
- **PostgreSQL:** 15-alpine (Primary database)
- **Redis:** 7-alpine (Caching and token management)
- **Elasticsearch:** (Search and indexing engine)
- **Hibernate:** ORM through Spring Data JPA
- **HikariCP:** Connection pooling
- **H2 Database:** Testing (in-memory)

**Development & Code Enhancement**
- **Lombok:** 自动代码生成 (Getter/Setter, Builder等)
- **javax.annotation-api:** 1.3.2 (注解支持)
- **Maven Wrapper:** `./mvnw` for consistent builds
- **Spring Boot Maven Plugin** - Application packaging

**Async & Task Processing**
- **Spring Boot Async Support** - 异步任务处理
- **AsyncTaskConfig** - 自定义异步配置
- **ThreadPoolTaskExecutor** - 线程池管理

### Frontend Technologies

**Core Framework**
- **Vue.js:** 3.5.18 (Composition API)
- **TypeScript:** 5.8.0 (Strict mode)
- **Vite:** 7.0.6 (Build tool and dev server)

**代码编辑器组件** ⭐ **NEW**
- **CodeMirror:** 6.0.2 (核心编辑器)
- **@codemirror/lang-json:** 6.0.2 (JSON语言支持)
- **@codemirror/autocomplete:** 6.18.7 (自动完成)
- **@codemirror/commands:** 6.8.1 (编辑器命令)
- **@codemirror/search:** 6.5.11 (搜索功能)
- **@codemirror/theme-one-dark:** 6.1.3 (暗色主题)

**搜索数据管理组件系统** ⭐ **NEW**
- **DynamicResultsTable:** 725行响应式表格组件
- **DocumentEditDialog:** 532行文档编辑对话框
- **DeleteConfirmDialog:** 420行删除确认对话框
- **FieldEditor/FieldManager:** 字段编辑器组件系统
- **SearchDataManagePage:** 936行主管理页面

**搜索日志管理组件系统** ⭐⭐ **NEW**
- **SearchLogTable:** 452行搜索日志表格组件
- **ClickLogTable:** 395行点击日志表格组件
- **SearchLogDetailModal:** 570行日志详情对话框
- **SearchLogFilter:** 307行日志过滤器组件
- **StatCard:** 552行统计卡片组件
- **PageHeader:** 265行页面头部组件

**移动端组件系统** ⭐⭐⭐
- **MobileSearchInterface:** 1278行移动端搜索界面核心组件
- **PhoneSimulator:** 248行手机模拟器组件
- **DeviceFrame:** 251行设备框架组件
- **移动端状态组件:** EmptyState(487行), ErrorState(615行), LoadingState(456行)
- **搜索组件系统:** SearchInput(462行), SearchResults(345行), SearchResultItem(472行)
- **演示系统:** ConfigManager(457行), DemoContainer(296行), ParameterPanel(350行)

**搜索日志Composition API** ⭐⭐ **NEW**
- **useClickTracking:** 424行点击追踪逻辑
- **搜索日志工具函数:** date(133行), export(481行), message(131行)
- **追踪配置:** tracking.ts(114行)配置文件

**移动端Composition API** ⭐⭐
- **useMobileSearchDemo:** 813行移动搜索演示逻辑
- **useParameterSync:** 769行参数同步管理
- **useSearchCache:** 554行搜索缓存管理
- **useSearchHistory:** 530行搜索历史管理
- **useSearchPerformance:** 1022行搜索性能监控

**性能优化组件** ⭐ **NEW**
- **VirtualList:** 249行虚拟化列表组件
- **useMediaQuery:** 318行媒体查询Composable
- **performance utils:** 422行性能优化工具类
- **searchOptimization:** 515行搜索优化工具
- **CellRenderer:** 动态单元格渲染组件
- **TableRowCard/Desktop:** 响应式表格行组件

**数据导出和文件处理** ⭐ **NEW**
- **xlsx:** 0.18.5 (Excel文件生成和处理)
- **@types/xlsx:** 0.0.35 (xlsx类型定义)

**State Management & Routing**
- **Pinia:** 3.0.3 (State management)
- **mobileSearchDemo Store:** 571行移动搜索演示状态管理 ⭐ **NEW**
- **Vue Router:** 4.5.1 (SPA routing)
- **Vue I18n:** 9.14.5 (Internationalization - 系统不需要但保留)
- **vuedraggable:** 2.24.3 (拖拽交互组件)

**UI & Styling**
- **shadcn-vue:** Modern component library (replaces Reka UI)
- **Reka UI:** 2.5.0 (Base component primitives)
- **TailwindCSS:** 3.4.17 (Utility-first CSS)
- **Lucide Vue Next:** Icon library
- **clsx:** Conditional className utility
- **tailwind-merge:** Tailwind class merging
- **Lucide Vue Next:** 0.544.0 (Icon library)
- **Class Variance Authority:** 0.7.1 (Component variants)
- **TailwindCSS Merge:** 3.3.1 (Class merging)

**Development Dependencies**
- **Vue TSC:** 3.0.4 (TypeScript checking)
- **Vue DevTools:** 8.0.0 (Development tools)
- **PostCSS:** 8.5.6 (CSS processing)
- **Autoprefixer:** 10.4.21 (CSS vendor prefixes)

### 搜索日志管理服务层 ⭐ **NEW**

**TypeScript服务**
- **searchLog.ts:** 199行搜索日志API服务
  - 搜索日志查询和过滤
  - 日志统计和分析
  - 数据导出功能
  - 清理任务管理

### 搜索数据管理服务层 ⭐

**TypeScript服务**
- **searchDataService.ts:** 129行Elasticsearch数据服务
  - 搜索和查询API封装
  - 文档CRUD操作
  - 批量操作支持
  - 错误处理和验证
- **searchData.ts:** 157行搜索数据API
  - 演示数据API封装
  - 银行数据搜索服务
  - 拼音搜索支持

**类型定义系统**
- **searchLog.ts:** 166行搜索日志类型定义 ⭐ **NEW**
  - 搜索日志实体类型
  - 点击日志类型
  - 统计数据类型
  - 查询请求类型
- **tableData.ts:** 110行表格数据类型定义
  - SearchData类型接口
- **demo.ts:** 409行演示数据类型定义
  - 移动端演示组件类型
  - 搜索配置类型
  - 性能监控类型
  - 字段类型定义
  - 表格配置类型
  - API响应类型

**Composables**
- **useMediaQuery.ts:** 318行媒体查询钩子
  - 响应式设计适配
  - 断点检测
  - 设备类型判断

### Infrastructure & DevOps

**Containerization**
- **Docker:** Container platform
- **Docker Compose:** Multi-service orchestration
- **Alpine Linux:** Lightweight base images

**Services Configuration**
- **PostgreSQL:** 15-alpine with custom configuration
- **Redis:** 7-alpine for caching/sessions
- **Elasticsearch:** Search and indexing service
- **Nginx:** (Implicit in frontend container)

**Database Configuration**
- **Database Name:** mgmt_db
- **Username:** mgmt_user
- **Connection Pool:** HikariCP (max 10 connections)
- **Dialect:** PostgreSQL

### Development Environment

**Node.js Requirements**
- **Node.js:** ^20.19.0 || >=22.12.0
- **Package Manager:** npm (with lock file)

**Build Tools**
- **Frontend Build:** Vite with TypeScript
- **Backend Build:** Maven 3.x
- **Docker Images:** Multi-stage builds

**Port Configuration**
- **Frontend:** 3000 (development), 80 (production)
- **Backend:** 8080 (API context: /api)
- **PostgreSQL:** 5432
- **Redis:** 6379
- **Elasticsearch:** 9200 (默认端口)

## Dependency Versions

### Backend Dependencies (pom.xml) - **UPDATED**

```xml
<properties>
    <java.version>17</java.version>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>
```

**Production Dependencies:**
- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Spring Boot Starter Validation
- Spring Boot Starter Actuator
- PostgreSQL Driver
- Spring Boot DevTools (runtime)
- **Lombok** (optional=true) - **NEW**
- **javax.annotation-api** 1.3.2 - **NEW**

**Test Dependencies:**
- Spring Boot Starter Test
- H2 Database (test scope)

### Frontend Dependencies (package.json)

**Production Dependencies:**
- vue: ^3.5.18
- pinia: ^3.0.3
- vue-router: ^4.5.1
- vue-i18n: ^9.14.5
- reka-ui: ^2.5.0
- tailwindcss: ^3.4.17
- axios: ^1.12.2
- @vueuse/core: ^13.9.0

**Development Dependencies:**
- typescript: ~5.8.0
- @vitejs/plugin-vue: ^6.0.1
- vite: ^7.0.6
- vue-tsc: ^3.0.4

## New Technology Integration

### 搜索数据管理系统 ⭐ **NEW**
**Purpose:** 完整的Elasticsearch数据管理和CRUD操作系统

**核心技术栈:**
- **Elasticsearch集成:** 高性能搜索和数据检索
- **响应式UI:** Vue 3 + TypeScript + TailwindCSS
- **虚拟化渲染:** 大数据量表格性能优化
- **动态表单:** 实时编辑和验证系统
- **批量操作:** 高效的批量数据处理

**后端架构:**
- **Controller Layer:** ElasticsearchDataController
- **Service Layer:** ElasticsearchDataService  
- **DTO Layer:** 10+个数据传输对象
- **Validation:** Bean Validation集成
- **Exception Handling:** 统一异常处理机制

**前端架构:**
- **组件化设计:** 30+个专用组件
- **状态管理:** Pinia集成的响应式状态
- **性能优化:** 虚拟滚动和懒加载
- **响应式设计:** 桌面和移动端适配

### JSON Import System
**Purpose:** 处理大规模JSON文件上传、分析和数据导入

**Core Technologies:**
- **File Processing:** Java NIO for file handling
- **JSON Parsing:** Jackson ObjectMapper (Spring Boot 内置)
- **Type Inference:** 自定义FieldTypeInferrer算法
- **Statistical Analysis:** 自定义StatisticsCalculator工具
- **Async Processing:** Spring Boot异步任务支持

**Elasticsearch Integration:**
- **Index Management:** 动态索引创建和配置
- **Mapping Configuration:** 自动字段映射生成
- **Bulk Import:** 高效批量数据导入
- **Search Optimization:** 索引优化策略

### Frontend Enhancement
**Component Architecture:**
- **JsonImportDialog:** 1015行复杂组件 (文件上传、配置、进度监控)
- **Reactive State:** Pinia状态管理集成
- **Progress Tracking:** 实时导入进度显示
- **Error Handling:** 完善的错误处理机制

## Configuration Files

### Backend Configuration - **UPDATED**

**application.yml:**
- Database connection (PostgreSQL)
- JPA/Hibernate settings
- Actuator endpoints
- Logging configuration
- Server configuration (port 8080, context-path /api)
- **File upload configuration** - **EXISTING**
- **Async task configuration** - **EXISTING**
- **Elasticsearch connection settings** - **ENHANCED** ⭐

**搜索数据管理配置:** - **NEW**
- ElasticsearchDataController路由配置
- 批量操作限制和超时设置
- 文档大小和字段限制配置
- 性能优化参数

**AsyncTaskConfig.java:** - **NEW**
- Thread pool configuration
- Task execution settings
- Error handling strategies

**Docker Configuration:**
- Multi-stage Dockerfile
- Production optimizations
- Health check endpoints

### Frontend Configuration

**TypeScript:**
- Strict mode enabled
- Multiple tsconfig files (app, node)
- Vue 3 type support

**Build Configuration:**
- Vite for development and build
- Vue plugin integration
- TypeScript compilation
- Asset optimization

**Styling:**
- TailwindCSS configuration (淡绿色主题)
- PostCSS processing
- Component styling patterns

## Development Tools

**Backend Development:**
- Maven Wrapper for consistent builds
- Spring Boot DevTools for hot reload
- Actuator for health monitoring
- H2 for testing without external database
- **Lombok plugin support** - **NEW**

**Frontend Development:**
- Vite dev server with HMR
- Vue DevTools browser extension
- TypeScript language server
- Component library (shadcn-vue)

**Database Tools:**
- PostgreSQL command line tools
- Docker exec for database access
- JPA/Hibernate DDL generation
- Database migration support

## External Services

**Development Services:**
- PostgreSQL database server
- Redis cache server
- Elasticsearch cluster
- Docker network for service communication

**Monitoring & Health:**
- Spring Actuator health endpoints
- Docker container health checks
- Application logging
- Build information generation

## Performance & Scalability

### 搜索数据管理优化 ⭐ **NEW**
- **虚拟化渲染:** VirtualList组件减少DOM开销
- **响应式查询:** 智能媒体查询适配
- **批量操作:** 高效的批量数据处理
- **懒加载:** 按需加载表格数据
- **缓存策略:** 搜索结果和组件状态缓存

### JSON Import Optimization
- **Memory Management:** 流式处理大文件
- **Batch Processing:** 批量操作减少I/O开销
- **Thread Pool:** 异步任务并行处理
- **Progress Tracking:** 实时进度反馈

### Database Optimization
- **Connection Pooling:** HikariCP优化配置
- **Index Strategy:** Elasticsearch索引优化
- **Query Performance:** JPA查询优化

## Version Compatibility

**Minimum Requirements:**
- Java 17 (LTS)
- Node.js 20.19.0+
- Docker with Compose support
- Git 2.x
- **Elasticsearch 8.x+** - **NEW**

**Browser Support:**
- Modern browsers with ES2020 support
- TypeScript strict mode compatibility
- Vue 3 Composition API support

## Update History
- 2025-09-28T05:05:04Z: 新增搜索日志管理系统完整技术栈，包括SearchLogAspect AOP切面、搜索监控模块、日志管理组件系统、点击追踪API、xlsx数据导出功能、E2E测试覆盖
- 2025-09-24T23:02:23Z: 新增搜索数据管理完整技术栈，包括30+个前端组件、ElasticsearchDataController/Service、虚拟化性能优化、响应式设计系统
- 2025-09-24T10:20:29Z: 添加Lombok和javax.annotation依赖，集成Elasticsearch搜索引擎，新增JSON导入系统架构，AsyncTaskConfig异步配置重构
- 2025-09-23T04:02:26Z: Added Spring Security with JWT authentication, Redis integration, shadcn-vue component library, Playwright testing framework, and enhanced development tools