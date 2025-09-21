---
created: 2025-09-21T10:31:04Z
last_updated: 2025-09-21T10:31:04Z
version: 1.0
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
│   │   │   ├── service/        # Business logic (to be implemented)
│   │   │   ├── controller/     # REST controllers (to be implemented)
│   │   │   ├── dto/           # Data transfer objects
│   │   │   │   ├── UserStatistics.java
│   │   │   │   └── UserSearchCriteria.java
│   │   │   ├── config/        # Configuration classes
│   │   │   │   └── JpaConfig.java
│   │   │   └── MgmtApplication.java # Main application class
│   │   └── resources/
│   │       └── application.yml  # Application configuration
│   └── test/
│       └── java/com/ynet/mgmt/
│           └── MgmtApplicationTests.java
├── target/                    # Maven build output
├── pom.xml                   # Maven dependencies
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
│   │   ├── ui/             # UI library components
│   │   │   ├── button/     # Button component variants
│   │   │   └── dropdown-menu/ # Dropdown menu components
│   │   ├── layout/         # Layout components
│   │   │   ├── SidebarNav.vue
│   │   │   └── TopNav.vue
│   │   ├── common/         # Common components
│   │   │   ├── UserMenu.vue
│   │   │   ├── Breadcrumb.vue
│   │   │   ├── LanguageSelector.vue
│   │   │   └── ThemeToggle.vue
│   │   └── icons/          # Icon components
│   ├── pages/              # Application pages
│   │   ├── auth/           # Authentication pages
│   │   │   └── Login.vue
│   │   ├── users/          # User management pages
│   │   │   └── UserList.vue
│   │   ├── Dashboard.vue
│   │   └── Settings.vue
│   ├── layouts/            # Page layouts
│   │   └── DefaultLayout.vue
│   ├── stores/             # Pinia state management
│   │   └── auth.ts
│   ├── router/             # Vue Router configuration
│   │   └── index.ts
│   ├── utils/              # Utility functions
│   │   ├── api.ts
│   │   └── http.ts
│   ├── lib/                # Library utilities
│   │   └── utils.ts
│   ├── types/              # TypeScript type definitions
│   │   └── auth.ts
│   ├── locales/            # Internationalization
│   │   ├── index.ts
│   │   ├── zh-CN.json
│   │   └── en-US.json
│   ├── assets/             # Static assets
│   │   ├── styles/
│   │   ├── main.css
│   │   ├── base.css
│   │   └── logo.svg
│   ├── App.vue             # Root component
│   ├── main.ts             # Application entry point
│   └── env.d.ts            # Environment type definitions
├── dist/                   # Build output
├── node_modules/           # Node.js dependencies
├── public/                 # Static public files
├── package.json            # Node.js dependencies and scripts
├── package-lock.json       # Dependency lock file
├── components.json         # UI components configuration
├── tsconfig.json           # TypeScript configuration
├── tsconfig.app.json       # App TypeScript configuration
├── tsconfig.node.json      # Node TypeScript configuration
├── vite.config.ts          # Vite build configuration
├── tailwind.config.js      # TailwindCSS configuration
├── postcss.config.js       # PostCSS configuration
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