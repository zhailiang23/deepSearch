---
created: 2025-09-21T10:31:04Z
last_updated: 2025-09-24T10:20:29Z
version: 1.2
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
│   │   └── search-space-json-import/  # JSON导入Epic (11个任务)
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
│   │   │   ├── jsonimport/    # **NEW: JSON导入功能模块**
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
│   │   │   ├── searchspace/   # **EXPANDED: 搜索空间管理模块**
│   │   │   │   ├── controller/
│   │   │   │   │   ├── SearchSpaceController.java (扩展)
│   │   │   │   │   └── SearchSpaceExceptionHandler.java (新增)
│   │   │   │   ├── dto/       # 搜索空间DTO
│   │   │   │   │   ├── FileUploadResponse.java
│   │   │   │   │   ├── FileValidationResult.java
│   │   │   │   │   ├── ImportStatistics.java
│   │   │   │   │   ├── ImportSyncResponse.java
│   │   │   │   │   └── SearchSpaceDTO.java
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
│   │   │   │   └── AsyncTaskConfig.java  # **UPDATED: 重构的异步配置**
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
│           ├── jsonimport/        # **NEW: JSON导入测试**
│           │   ├── service/       # 服务测试
│           │   │   ├── DataImportServiceTest.java
│           │   │   ├── IndexConfigServiceTest.java
│           │   │   └── JsonAnalysisServiceTest.java
│           │   └── util/          # 工具类测试
│           │       ├── FieldTypeInferrerTest.java
│           │       └── StatisticsCalculatorTest.java
│           ├── searchspace/       # **EXPANDED: 搜索空间测试扩展**
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
│   ├── components/          # Reusable Vue components
│   │   ├── ui/             # shadcn-vue UI library components
│   │   │   ├── button/     # Button component variants
│   │   │   ├── card/       # Card components
│   │   │   ├── collapsible/ # Collapsible components
│   │   │   ├── dropdown-menu/ # Dropdown menu components
│   │   │   ├── input/      # Input components
│   │   │   ├── label/      # Label components
│   │   │   ├── separator/  # Separator components
│   │   │   ├── sheet/      # Sheet components
│   │   │   ├── sidebar/    # Sidebar components
│   │   │   ├── skeleton/   # Skeleton components
│   │   │   ├── table/      # Table components
│   │   │   └── tooltip/    # Tooltip components
│   │   ├── layout/         # Layout components
│   │   │   ├── ResponsiveLayout.vue (移除i18n引用)
│   │   │   ├── SidebarNav.vue
│   │   │   └── TopNav.vue
│   │   ├── common/         # Common components
│   │   │   ├── UserMenu.vue
│   │   │   ├── Breadcrumb.vue
│   │   │   ├── LanguageSelector.vue
│   │   │   └── ThemeToggle.vue
│   │   ├── searchSpace/    # **NEW: 搜索空间组件**
│   │   │   ├── JsonImportDialog.vue  # **NEW: JSON导入对话框 (1015行)**
│   │   │   └── SearchSpaceList.vue   # **UPDATED: 集成导入功能**
│   │   ├── LoginForm.vue   # Login form component
│   │   └── icons/          # Icon components
│   ├── pages/              # Application pages
│   │   ├── auth/           # Authentication pages
│   │   │   ├── Login.vue
│   │   │   └── LoginSimple.vue
│   │   ├── users/          # User management pages
│   │   │   └── UserList.vue
│   │   ├── searchSpace/    # **NEW: 搜索空间页面**
│   │   │   └── SearchSpaceContent.vue  # **UPDATED: 添加导入功能**
│   │   ├── Dashboard.vue
│   │   ├── DashboardSimple.vue
│   │   └── Settings.vue
│   ├── layouts/            # Page layouts
│   │   └── DefaultLayout.vue
│   ├── stores/             # Pinia state management
│   │   ├── auth.ts
│   │   └── searchSpace.ts  # **UPDATED: 扩展状态管理**
│   ├── router/             # Vue Router configuration
│   │   └── index.ts
│   ├── services/           # **NEW: 服务层**
│   │   └── importService.ts  # **NEW: 导入服务 (335行)**
│   ├── utils/              # Utility functions
│   │   ├── api.ts
│   │   └── http.ts
│   ├── lib/                # Library utilities
│   │   └── utils.ts
│   ├── types/              # TypeScript type definitions
│   │   └── auth.ts
│   ├── locales/            # Internationalization (系统不需要但保留)
│   │   ├── index.ts
│   │   ├── zh-CN.json
│   │   └── en-US.json
│   ├── assets/             # Static assets
│   │   ├── styles/
│   │   │   └── globals.css
│   │   ├── main.css
│   │   ├── base.css
│   │   └── logo.svg
│   ├── App.vue             # Root component
│   ├── main.ts             # Application entry point
│   └── env.d.ts            # Environment type definitions
├── tests/                  # Test files
│   └── e2e/               # End-to-end tests
│       ├── config/        # Test configuration
│       └── auth-integration.spec.ts
├── dist/                   # Build output
├── node_modules/           # Node.js dependencies
├── public/                 # Static public files
├── test-import-dialog.html # **NEW: 导入对话框测试页面**
├── .env.development        # Development environment variables
├── package.json            # Node.js dependencies and scripts
├── package-lock.json       # Dependency lock file
├── components.json         # shadcn-vue components configuration
├── tsconfig.json           # TypeScript configuration
├── tsconfig.app.json       # App TypeScript configuration
├── tsconfig.node.json      # Node TypeScript configuration
├── vite.config.ts          # Vite build configuration
├── tailwind.config.js      # TailwindCSS configuration
├── postcss.config.js       # PostCSS configuration
├── playwright.config.ts    # Playwright test configuration
├── Dockerfile              # Docker image definition
├── Dockerfile.prod         # Production Docker image
├── index.html              # HTML template
└── README.md               # Frontend documentation
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