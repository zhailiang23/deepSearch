---
created: 2025-09-21T07:41:34Z
last_updated: 2025-09-21T09:09:21Z
version: 1.2
author: Claude Code PM System
---

# Project Structure

## Root Directory Layout

```
deepSearch/
├── README.md                    # Basic project description
├── CLAUDE.md                    # Updated Claude Code guidance
├── AGENTS.md                    # Agent system documentation
├── COMMANDS.md                  # Command reference guide
├── LICENSE                      # Project license
├── .gitignore                   # Java project ignore patterns
├── screenshot.webp              # Project screenshot
├── backend/                     # Spring Boot 3 后端应用
├── frontend/                    # Vue 3 前端应用
├── docker/                      # Docker配置文件
├── docker-compose.yml           # 开发环境容器编排
├── docker-compose.test.yml      # 测试环境容器编排
├── docker-compose.prod.yml      # 生产环境容器编排
├── deploy.sh                    # 自动化部署脚本
├── health-check.sh              # 健康检查脚本
├── .env.example                 # 环境变量配置模板
├── DOCKER_README.md             # Docker部署文档
└── .claude/                     # Claude AI configuration
```

## Claude Configuration Structure

```
.claude/
├── CLAUDE.md                    # Main development rules and guidance
├── settings.local.json          # Tool permissions and access control
├── agents/                      # Specialized agent definitions
│   ├── code-analyzer.md         # Code analysis and bug detection
│   ├── file-analyzer.md         # File content analysis and summaries
│   ├── parallel-worker.md       # Parallel task coordination
│   └── test-runner.md           # Test execution and result analysis
├── commands/                    # Command system implementations
│   ├── pm/                      # Project management commands
│   │   ├── init.sh              # PM system initialization
│   │   ├── status.md            # Project status reporting
│   │   ├── issue-start.md       # Issue workflow initiation
│   │   └── prd-new.md           # PRD creation workflow
│   ├── context/                 # Context management commands
│   │   ├── create.md            # Initial context creation
│   │   ├── update.md            # Context refresh and updates
│   │   └── prime.md             # Context loading for sessions
│   └── testing/                 # Testing framework commands
│       ├── prime.md             # Test configuration setup
│       └── run.md               # Test execution with analysis
├── scripts/                     # Shell automation scripts
│   ├── pm/                      # Project management scripts
│   │   └── init.sh              # PM initialization script
│   └── test-and-log.sh          # Test execution logging
├── rules/                       # System rules and constraints
│   └── datetime.md              # Date/time handling rules
├── prds/                        # Product Requirements Documents
│   └── basic-mgmt-system.md     # Basic management system PRD
├── epics/                       # Epic implementation plans
│   └── basic-mgmt-system/       # Basic management system epic
│       ├── epic.md              # Main epic document
│       ├── execution-status.md  # Epic execution tracking
│       ├── updates/             # Task progress updates
│       ├── 001.md               # Task: 项目初始化 ✅
│       ├── 002.md               # Task: JPA实体和Repository
│       ├── 003.md               # Task: 认证系统
│       ├── 004.md               # Task: 用户管理API
│       ├── 005.md               # Task: 前端基础 ✅
│       ├── 006.md               # Task: 登录页面
│       ├── 007.md               # Task: 用户管理页面
│       └── 008.md               # Task: 部署配置 ✅
└── context/                     # Project context documentation
    ├── progress.md              # Current status and progress
    ├── project-structure.md     # This file - directory organization
    ├── tech-context.md          # Dependencies and technologies
    ├── system-patterns.md       # Architectural patterns
    ├── product-context.md       # Product requirements and users
    ├── project-brief.md         # Scope and objectives
    ├── project-overview.md      # Features and capabilities
    ├── project-vision.md        # Long-term vision
    └── project-style-guide.md   # Coding standards
```

## Key Directory Purposes

### `.claude/agents/`
- **Purpose**: Specialized AI agent definitions
- **Pattern**: Each agent has dedicated markdown file with capabilities
- **Usage**: Agents handle specific tasks to optimize context usage

### `.claude/commands/`
- **Purpose**: Command system implementations
- **Pattern**: Organized by functional category (pm/, context/, testing/)
- **Usage**: Markdown files interpreted as executable instructions

### `.claude/scripts/`
- **Purpose**: Shell automation scripts
- **Pattern**: Bash scripts for complex operations
- **Usage**: Called by commands for system-level tasks

### `.claude/context/`
- **Purpose**: Project knowledge and documentation
- **Pattern**: Structured markdown files with frontmatter
- **Usage**: Loaded into conversations for project awareness

### `.claude/rules/`
- **Purpose**: System constraints and guidelines
- **Pattern**: Rule definitions in markdown format
- **Usage**: Referenced by commands for consistent behavior

### `.claude/prds/`
- **Purpose**: Product Requirements Documents
- **Pattern**: One PRD file per feature/product
- **Usage**: Comprehensive product specifications and business requirements

### `.claude/epics/`
- **Purpose**: Technical implementation plans
- **Pattern**: Each epic has dedicated directory with epic.md + numbered task files
- **Usage**: Bridge between PRDs and actual development tasks

## File Naming Patterns

### Context Files
- **Pattern**: `kebab-case.md`
- **Structure**: YAML frontmatter + markdown content
- **Examples**: `project-overview.md`, `tech-context.md`

### Command Files
- **Pattern**: `category/action.md` or `category/action.sh`
- **Structure**: YAML frontmatter + instructions/script
- **Examples**: `pm/status.md`, `testing/run.md`

### Agent Files
- **Pattern**: `agent-type.md`
- **Structure**: Agent definition with capabilities
- **Examples**: `code-analyzer.md`, `test-runner.md`

### PRD Files
- **Pattern**: `feature-name.md`
- **Structure**: YAML frontmatter + comprehensive requirements
- **Examples**: `basic-mgmt-system.md`

### Epic Files
- **Pattern**: `epic.md` + `001.md`, `002.md`, etc.
- **Structure**: Epic overview + numbered task files
- **Examples**: `basic-mgmt-system/epic.md`, `basic-mgmt-system/001.md`

## Configuration Files

### `.claude/settings.local.json`
- **Purpose**: Tool access permissions
- **Structure**: JSON with allowed tool patterns
- **Security**: Controls what tools agents can use

### `.claude/CLAUDE.md`
- **Purpose**: Development rules and patterns
- **Structure**: Markdown with project-specific guidance
- **Usage**: Read by all Claude instances working on project

## Application Structure

### Backend Application (Spring Boot 3)

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/ynet/mgmt/
│   │   │   ├── MgmtApplication.java      # 主应用类
│   │   │   ├── config/
│   │   │   │   └── JpaConfig.java        # JPA配置
│   │   │   ├── entity/
│   │   │   │   └── BaseEntity.java       # 基础实体类
│   │   │   ├── repository/               # JPA Repository
│   │   │   ├── service/                  # 业务逻辑层
│   │   │   └── controller/               # REST控制器
│   │   └── resources/
│   │       ├── application.yml           # 主配置文件
│   │       ├── application-dev.yml       # 开发环境配置
│   │       └── application-prod.yml      # 生产环境配置
│   └── test/
│       ├── java/com/ynet/mgmt/
│       │   └── MgmtApplicationTests.java # 基础测试
│       └── resources/
│           └── application-test.yml      # 测试配置
├── pom.xml                              # Maven配置
├── Dockerfile                           # 开发环境Docker镜像
├── Dockerfile.prod                      # 生产环境Docker镜像
└── README.md
```

### Frontend Application (Vue 3)

```
frontend/
├── src/
│   ├── components/
│   │   ├── ui/                          # shadcn-vue组件
│   │   ├── common/                      # 通用组件
│   │   └── layout/                      # 布局组件
│   ├── layouts/                         # 页面布局
│   ├── pages/                           # 页面组件
│   ├── router/                          # 路由配置
│   ├── stores/                          # Pinia状态管理
│   ├── utils/                           # 工具函数和API
│   ├── types/                           # TypeScript类型
│   ├── locales/                         # 国际化配置
│   └── assets/styles/                   # 全局样式
├── public/
├── package.json
├── vite.config.ts
├── tailwind.config.js
├── tsconfig.json
├── Dockerfile                           # 开发环境Docker镜像
├── Dockerfile.prod                      # 生产环境Docker镜像
└── nginx.conf                           # Nginx配置
```

### Docker Configuration

```
docker/
├── postgres/
│   ├── init.sql                         # 数据库初始化脚本
│   └── postgresql.conf                  # PostgreSQL配置
├── redis/
│   └── redis.conf                       # Redis配置
└── nginx/
    ├── nginx.conf                       # Nginx主配置
    └── default.conf                     # 默认站点配置
```

## Architecture Notes

- **Configuration-driven**: Heavy use of markdown for configuration
- **Agent-based**: Specialized agents handle different concerns
- **Context-optimized**: Structure designed to minimize token usage
- **Permission-based**: Granular control over tool access
- **Modular**: Clear separation between different functional areas

## Growth Patterns

As the project evolves, expect:
1. **Source directories**: `src/`, `lib/`, `test/` when coding begins
2. **Build configuration**: Build tool configs (Maven, Gradle, etc.)
3. **Multiple PRDs**: Additional PRD files for new features
4. **More epics**: Additional epic directories for different features
5. **Extended context**: More specialized context files
6. **Custom commands**: Project-specific command implementations

## Update History
- 2025-09-21: Added `.claude/prds/` and `.claude/epics/` directories with basic-mgmt-system project artifacts
- 2025-09-21: Added complete application structure - backend/, frontend/, docker/ with full implementation