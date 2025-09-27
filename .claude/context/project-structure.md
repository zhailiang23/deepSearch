---
created: 2025-09-21T10:31:04Z
last_updated: 2025-09-27T01:30:13Z
version: 1.5
author: Claude Code PM System
---

# Project Structure

## Root Directory Layout

```
deepSearch/
├── backend/                 # Spring Boot backend application
├── frontend/               # Vue.js frontend application  
├── docker/                 # Docker configuration files
├── .claude/                # Claude Code PM system
│   ├── context/            # Project context documentation
│   ├── epics/              # Epic documentation
│   │   ├── .archived/      # **NEW: 归档的已完成Epics**
│   │   │   ├── loginAndLogout/         # 已完成的登录登出Epic
│   │   │   ├── search-data-manage/     # 已完成的搜索数据管理Epic
│   │   │   └── search-space-json-import/ # 已完成的JSON导入Epic
│   │   └── pinyin-search/  # **NEW: 拼音搜索Epic (进行中)**
│   └── prds/               # Product Requirement Documents
├── test/                   # Test data and documentation
│   ├── extended_search_test_data.json
│   ├── search_test_cases.md
│   └── search_test_data.json
├── .env.example           # Environment variables template
├── .gitignore             # Git ignore patterns
├── CLAUDE.md              # Claude Code project instructions
├── README.md              # Project documentation
├── COMMANDS.md            # Available commands reference
├── AGENTS.md              # Agent documentation
├── DOCKER_README.md       # Docker setup instructions
├── LICENSE                # Project license
├── deploy.sh              # Deployment script
├── health-check.sh        # Health check script
├── docker-compose.yml     # Development environment
├── docker-compose.prod.yml # Production environment
└── docker-compose.test.yml # Test environment
```

## Backend Structure (Spring Boot)

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/ynet/mgmt/
│   │   │   ├── entity/         # JPA entities
│   │   │   │   ├── User.java
│   │   │   │   ├── UserRole.java
│   │   │   │   ├── UserStatus.java
│   │   │   │   └── BaseEntity.java
│   │   │   ├── repository/     # Data access layer
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── UserRepositoryCustom.java
│   │   │   │   └── UserRepositoryImpl.java
│   │   │   ├── service/        # Business logic services
│   │   │   │   ├── AuthService.java
│   │   │   │   ├── UserService.java
│   │   │   │   └── UserServiceImpl.java
│   │   │   ├── controller/     # REST controllers
│   │   │   │   ├── AuthController.java
│   │   │   │   └── UserController.java
│   │   │   ├── dto/           # Data transfer objects
│   │   │   │   ├── auth/      # Authentication DTOs
│   │   │   │   │   ├── LoginRequest.java
│   │   │   │   │   ├── LoginResponse.java
│   │   │   │   │   ├── RefreshTokenRequest.java
│   │   │   │   │   └── RefreshTokenResponse.java
│   │   │   │   ├── UserStatistics.java
│   │   │   │   └── UserSearchCriteria.java
│   │   │   ├── searchdata/    # **NEW: 搜索数据管理模块** ⭐
│   │   │   │   ├── controller/
│   │   │   │   │   └── ElasticsearchDataController.java
│   │   │   │   ├── dto/       # 搜索数据DTO
│   │   │   │   │   ├── BulkOperationRequest.java
│   │   │   │   │   ├── BulkOperationResponse.java
│   │   │   │   │   ├── DeleteDocumentRequest.java
│   │   │   │   │   ├── DeleteDocumentResponse.java
│   │   │   │   │   ├── DocumentDetailResponse.java
│   │   │   │   │   ├── IndexMappingResponse.java
│   │   │   │   │   ├── SearchDataRequest.java
│   │   │   │   │   ├── SearchDataResponse.java
│   │   │   │   │   ├── UpdateDocumentRequest.java
│   │   │   │   │   └── UpdateDocumentResponse.java
│   │   │   │   └── service/   # 搜索数据服务
│   │   │   │       └── ElasticsearchDataService.java
│   │   │   ├── jsonimport/    # JSON导入功能模块
│   │   │   │   ├── dto/       # 导入相关DTO
│   │   │   │   │   ├── ImportExecuteRequest.java
│   │   │   │   │   └── ImportTaskStatus.java
│   │   │   │   ├── enums/     # 导入相关枚举
│   │   │   │   │   ├── FieldType.java
│   │   │   │   │   └── ImportTaskState.java
│   │   │   │   ├── model/     # 导入数据模型
│   │   │   │   │   ├── FieldAnalysisResult.java
│   │   │   │   │   ├── FieldStatistics.java
│   │   │   │   │   ├── IndexMappingConfig.java
│   │   │   │   │   └── JsonSchemaAnalysis.java
│   │   │   │   ├── service/   # 导入服务
│   │   │   │   │   ├── DataImportService.java
│   │   │   │   │   ├── IndexConfigService.java
│   │   │   │   │   └── JsonAnalysisService.java
│   │   │   │   └── util/      # 导入工具类
│   │   │   │       ├── FieldTypeInferrer.java
│   │   │   │       └── StatisticsCalculator.java
│   │   │   ├── searchspace/   # 搜索空间管理模块
│   │   │   │   ├── controller/
│   │   │   │   │   ├── SearchSpaceController.java (扩展)
│   │   │   │   │   └── SearchSpaceExceptionHandler.java (新增)
│   │   │   │   ├── dto/       # 搜索空间DTO
│   │   │   │   │   ├── FileUploadResponse.java
│   │   │   │   │   ├── FileValidationResult.java
│   │   │   │   │   ├── ImportStatistics.java
│   │   │   │   │   ├── ImportSyncResponse.java
│   │   │   │   │   └── SearchSpaceDTO.java (扩展)
│   │   │   │   ├── entity/    # 实体扩展
│   │   │   │   │   └── SearchSpace.java (扩展)
│   │   │   │   ├── mapper/    # 映射器
│   │   │   │   │   └── SearchSpaceMapper.java
│   │   │   │   ├── repository/
│   │   │   │   │   └── SearchSpaceRepository.java (扩展)
│   │   │   │   └── service/   # 服务扩展
│   │   │   │       ├── ElasticsearchManager.java (扩展)
│   │   │   │       ├── FileStorageService.java
│   │   │   │       ├── FileValidationService.java
│   │   │   │       ├── SearchSpaceService.java
│   │   │   │       └── impl/  # 服务实现
│   │   │   │           ├── FileStorageServiceImpl.java
│   │   │   │           ├── FileValidationServiceImpl.java
│   │   │   │           └── SearchSpaceServiceImpl.java (扩展)
│   │   │   ├── config/        # Configuration classes
│   │   │   │   ├── JpaConfig.java
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── RedisConfig.java
│   │   │   │   ├── DataInitializer.java
│   │   │   │   └── AsyncTaskConfig.java  # 重构的异步配置
│   │   │   ├── security/      # Security components
│   │   │   │   ├── JwtService.java
│   │   │   │   ├── RefreshTokenService.java
│   │   │   │   └── TokenBlacklistService.java
│   │   │   └── MgmtApplication.java # Main application class
│   │   └── resources/
│   │       ├── application.yml      # Base configuration (扩展)
│   │       └── application-dev.yml  # Development configuration
│   └── test/
│       └── java/com/ynet/mgmt/
│           ├── integration/        # Integration tests
│           ├── e2e/               # End-to-end tests
│           ├── jsonimport/        # JSON导入测试
│           │   ├── service/       # 服务测试
│           │   │   ├── DataImportServiceTest.java
│           │   │   ├── IndexConfigServiceTest.java
│           │   │   └── JsonAnalysisServiceTest.java
│           │   └── util/          # 工具类测试
│           │       ├── FieldTypeInferrerTest.java
│           │       └── StatisticsCalculatorTest.java
│           ├── searchspace/       # 搜索空间测试扩展
│           │   ├── controller/    # 控制器测试
│           │   │   ├── SearchSpaceControllerImportIntegrationTest.java
│           │   │   └── SearchSpaceControllerImportTest.java
│           │   ├── dto/           # DTO测试
│           │   │   └── ImportStatisticsTest.java
│           │   ├── entity/        # 实体测试
│           │   │   └── SearchSpaceTest.java
│           │   ├── mapper/        # 映射器测试
│           │   │   └── SearchSpaceMapperTest.java
│           │   ├── repository/    # 仓库测试
│           │   │   └── SearchSpaceRepositoryTest.java
│           │   └── service/       # 服务测试
│           │       ├── FileStorageServiceTest.java
│           │       └── FileValidationServiceTest.java
│           └── MgmtApplicationTests.java
├── target/                    # Maven build output
├── pom.xml                   # Maven dependencies (新增Lombok等依赖)
├── Dockerfile               # Docker image definition
├── Dockerfile.prod         # Production Docker image
├── mvnw                    # Maven wrapper (Unix)
├── mvnw.cmd               # Maven wrapper (Windows)
└── README.md              # Backend documentation
```

## Frontend Structure (Vue.js)

```
frontend/
├── src/
│   ├── main.ts              # Application entry point
│   ├── App.vue              # Root component
│   ├── router/              # Vue Router configuration
│   │   └── index.ts
│   ├── stores/              # Pinia state management
│   │   ├── auth.ts          # Authentication store
│   │   ├── searchSpace.ts   # Search space store
│   │   └── mobileSearchDemo.ts # **NEW: 571行移动搜索演示状态管理** ⭐
│   ├── composables/         # **NEW: Composition API utilities** ⭐
│   │   ├── useMediaQuery.ts # 318行媒体查询工具 (响应式设计)
│   │   ├── useMobileSearchDemo.ts # **NEW: 813行移动搜索演示逻辑** ⭐
│   │   ├── useParameterSync.ts    # **NEW: 769行参数同步管理** ⭐
│   │   ├── useSearchCache.ts      # **NEW: 554行搜索缓存管理** ⭐
│   │   ├── useSearchHistory.ts    # **NEW: 530行搜索历史管理** ⭐
│   │   └── useSearchPerformance.ts # **NEW: 1022行搜索性能监控** ⭐
│   ├── utils/               # **NEW: Utility functions** ⭐
│   │   ├── performance.ts   # 422行性能优化工具
│   │   └── searchOptimization.ts # **NEW: 515行搜索优化工具** ⭐
│   ├── types/               # TypeScript type definitions
│   │   ├── auth.ts          # Authentication types
│   │   ├── searchSpace.ts   # Search space types
│   │   ├── tableData.ts     # **NEW: 110行表格数据类型定义** ⭐
│   │   └── demo.ts         # **NEW: 409行演示数据类型定义** ⭐
│   ├── services/            # API service layer
│   │   ├── api.ts           # Base API configuration
│   │   ├── authService.ts   # Authentication API
│   │   ├── searchSpaceApi.ts # Search space API
│   │   ├── searchDataService.ts # **NEW: 129行搜索数据API服务** ⭐
│   │   └── searchData.ts    # **NEW: 157行搜索数据API** ⭐
│   ├── components/          # Reusable components
│   │   ├── layout/          # Layout components
│   │   │   ├── TopNav.vue
│   │   │   ├── SidebarNav.vue
│   │   │   ├── ResponsiveLayout.vue
│   │   │   └── DualPaneLayout.vue # **NEW: 253行双面板布局** ⭐
│   │   ├── ui/              # Base UI components
│   │   │   ├── Button.vue
│   │   │   ├── Dropdown.vue
│   │   │   ├── UserMenu.vue
│   │   │   ├── VirtualList.vue  # **NEW: 249行虚拟化列表组件** ⭐
│   │   │   ├── select/      # **NEW: 选择器组件系统** ⭐
│   │   │   │   ├── Select.vue        # 19行主选择器
│   │   │   │   ├── SelectContent.vue # 43行选择器内容
│   │   │   │   ├── SelectItem.vue    # 36行选择器项目
│   │   │   │   ├── SelectTrigger.vue # 31行选择器触发器
│   │   │   │   ├── SelectValue.vue   # 21行选择器值
│   │   │   │   └── index.ts         # 5行导出文件
│   │   │   └── switch/      # **NEW: 开关组件系统** ⭐
│   │   │       ├── Switch.vue       # 41行开关组件
│   │   │       └── index.ts        # 1行导出文件
│   │   ├── searchSpace/     # Search space components
│   │   │   ├── SearchSpaceList.vue
│   │   │   ├── SearchSpaceForm.vue
│   │   │   ├── JsonImportDialog.vue (1015行)
│   │   │   └── SpaceSelectionDialog.vue
│   │   └── searchData/      # **NEW: 搜索数据管理组件系统** ⭐
│   │       ├── DeleteConfirmDialog.vue     # 420行删除确认对话框
│   │       ├── DocumentEditDialog.vue      # 532行文档编辑对话框
│   │       ├── DynamicResultsTable.vue     # 725行动态结果表格
│   │       ├── FieldEditor.vue             # 482行字段编辑器
│   │       ├── FieldManager.vue            # 409行字段管理器
│   │       ├── PaginationControl.vue       # 379行分页控制器
│   │       ├── TypeBadge.vue               # 142行类型标记组件
│   │       └── table/                      # 表格子组件系统
│   │           ├── CellRenderer.vue        # 428行单元格渲染器
│   │           ├── TableRowCard.vue        # 500行移动端表格行
│   │           └── TableRowDesktop.vue     # 383行桌面端表格行
│   │   ├── mobile/           # **NEW: 移动端组件系统** ⭐⭐⭐
│   │   │   ├── DeviceFrame.vue           # 251行设备框架组件
│   │   │   ├── EmptyState.vue            # 487行空状态组件
│   │   │   ├── ErrorState.vue            # 615行错误状态组件
│   │   │   ├── LoadingState.vue          # 456行加载状态组件
│   │   │   ├── MobileSearchApp.vue       # 326行移动搜索应用
│   │   │   ├── MobileSearchInterface.vue # 1278行移动搜索界面
│   │   │   ├── PhoneSimulator.vue        # 248行手机模拟器
│   │   │   ├── SearchInput.vue           # 462行搜索输入框
│   │   │   ├── SearchResultItem.vue      # 472行搜索结果项
│   │   │   ├── SearchResults.vue         # 345行搜索结果
│   │   │   ├── StatusBar.vue             # 78行状态栏
│   │   │   ├── __tests__/                # 移动端组件测试
│   │   │   │   ├── DeviceFrame.spec.ts   # 132行设备框架测试
│   │   │   │   ├── PhoneSimulator.spec.ts # 133行手机模拟器测试
│   │   │   │   └── StatusBar.spec.ts     # 154行状态栏测试
│   │   │   ├── README.md                # 138行移动端组件文档
│   │   │   └── index.ts                 # 23行导出文件
│   │   └── demo/             # **NEW: 演示组件系统** ⭐⭐
│   │       ├── ConfigManager.vue         # 457行配置管理器
│   │       ├── DemoContainer.vue         # 296行演示容器
│   │       ├── PagingConfig.vue          # 319行分页配置
│   │       ├── ParameterPanel.vue        # 350行参数面板
│   │       ├── PinyinSearchConfig.vue    # 174行拼音搜索配置
│   │       └── SearchSpaceSelector.vue   # 196行搜索空间选择器
│   ├── pages/               # **NEW: 页面组件** ⭐
│   │   ├── DashboardSimple.vue      # **NEW: 16行简化仪表板** ⭐
│   │   └── MobileSearchDemo.vue     # **NEW: 154行移动搜索演示页面** ⭐
│   ├── views/               # Page components
│   │   ├── LoginPage.vue
│   │   ├── DashboardPage.vue
│   │   ├── UserListPage.vue
│   │   ├── UserSettingsPage.vue
│   │   ├── SearchSpaceListPage.vue
│   │   ├── SearchDataManagePage.vue # **NEW: 936行搜索数据主管理页面** ⭐
│   │   └── PhoneSimulatorTest.vue   # **NEW: 198行手机模拟器测试页面** ⭐
│   ├── assets/              # Static assets
│   │   ├── styles/
│   │   │   └── globals.css
│   │   └── images/
│   └── test/                # **NEW: 前端测试组件** ⭐
│       └── composables/     # Composition API测试
│           └── __tests__/
│               └── useMobileSearchDemo.test.ts # **NEW: 388行移动搜索演示测试** ⭐
├── public/                  # Public static files
├── components.json          # shadcn-vue component registry
├── index.html              # HTML template
├── tailwind.config.js      # TailwindCSS configuration
├── tsconfig.json           # TypeScript configuration
├── tsconfig.app.json       # App TypeScript configuration
├── tsconfig.node.json      # Node TypeScript configuration  
├── vite.config.ts          # Vite build configuration
├── package.json            # Dependencies and scripts
└── package-lock.json       # Dependency lock file
```

## Docker Infrastructure

```
docker/
├── postgres/               # PostgreSQL configuration
│   ├── init/              # Database initialization scripts
│   └── postgresql.conf    # PostgreSQL configuration
└── redis/                 # Redis configuration
    └── redis.conf         # Redis configuration
```

## Claude Code PM System

```
.claude/
├── context/               # Project context documentation
├── scripts/               # PM system scripts
│   └── pm/               # Project management utilities
├── CLAUDE.md             # Internal PM rules and guidelines
└── (various PM system files)
```

## File Naming Conventions

### Backend (Java)
- **Classes:** PascalCase (e.g., `UserRepository.java`)
- **Packages:** lowercase with dots (e.g., `com.ynet.mgmt.entity`)
- **Configuration:** kebab-case (e.g., `application.yml`)

### Frontend (Vue.js/TypeScript)
- **Components:** PascalCase (e.g., `UserMenu.vue`)
- **Directories:** kebab-case (e.g., `dropdown-menu/`)
- **Scripts:** camelCase (e.g., `api.ts`)
- **Styles:** kebab-case (e.g., `main.css`)

### General
- **Documentation:** UPPERCASE (e.g., `README.md`)
- **Configuration:** lowercase (e.g., `docker-compose.yml`)
- **Scripts:** kebab-case (e.g., `health-check.sh`)

## Module Organization

### Backend Layers
1. **Entity Layer** - JPA entities with business logic
2. **Repository Layer** - Data access with custom queries
3. **Service Layer** - Business logic and validation
4. **Controller Layer** - REST API endpoints
5. **DTO Layer** - Data transfer objects
6. **Config Layer** - Application configuration

### Frontend Architecture
1. **Component Layer** - Reusable UI components
2. **Page Layer** - Route-specific components
3. **Store Layer** - State management (Pinia)
4. **Router Layer** - Navigation configuration
5. **Utility Layer** - Helper functions
6. **Type Layer** - TypeScript definitions

## New Module Details

### JSON Import Module (jsonimport)
**Purpose:** 处理JSON文件上传、分析、配置和数据导入

**Core Components:**
- **Service Layer:** 数据分析、索引配置、异步导入处理
- **DTO Layer:** 请求响应对象和状态管理
- **Model Layer:** 字段分析、统计信息、配置模型
- **Util Layer:** 类型推断、统计计算等工具

### Search Space Extensions
**Purpose:** 扩展搜索空间管理功能，集成JSON导入

**Enhanced Components:**
- **Controller:** 新增导入相关API端点
- **Service:** 文件存储、验证、Elasticsearch集成
- **DTO:** 导入统计、响应对象等
- **Frontend:** 完整的导入界面组件

### Test Coverage Enhancement
**New Test Files:** 11个测试文件，覆盖所有新增功能
**Test Types:** 单元测试、集成测试、控制器测试
**Test Data:** 扩展的搜索测试数据集

## File Statistics
- **Total Files Added:** 70个新文件
- **Core Java Classes:** 25个
- **Test Classes:** 11个
- **Frontend Components:** 主要是JsonImportDialog (1015行)
- **Documentation:** Epic文档11个，PRD文档1个

## Update History
- 2025-09-24T10:20:29Z: 新增JSON导入功能模块结构，包括70个新文件的完整组织结构
- 2025-09-24T23:02:23Z: 新增完整的搜索数据管理模块结构，包括后端searchdata包(12个文件)、前端searchData组件系统(15个组件)、性能优化工具、类型定义等30+个新文件