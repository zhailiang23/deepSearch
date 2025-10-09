# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> Think carefully and implement the most concise solution that changes as little code as possible.

## Project Overview

deepSearch is a search analytics and management system built with **Spring Boot 3.2.1** (Java 17) backend and **Vue.js 3.5.18** (TypeScript) frontend, featuring search log analysis, hot word statistics, and AI-powered clustering.

## Architecture

### Backend (Spring Boot)
- **Package Structure**: `com.ynet.mgmt.*`
- **Core Modules**:
  - `searchlog` - Search log management
  - `statistics` - Statistical analysis
  - `clustering` - AI clustering analysis (integrates with Python service)
  - `hotTopic` - Hot topic analysis
  - `sensitiveWord` - Sensitive word filtering
  - `user`, `auth`, `role` - User authentication & authorization
  - `searchspace`, `channel` - Search space and channel management
  - `common` - Shared utilities and base entities
- **Layers**: Controller → Service → Repository → Entity (extending `BaseEntity`)
- **Database**: MySQL 8.0 with JPA/Hibernate
- **Cache**: Redis 7
- **Search Engine**: Elasticsearch with custom pinyin analyzer
- **Port**: 8080 (API context path: `/api`)

### Frontend (Vue.js)
- **Framework**: Vue 3.5.18 + TypeScript + Vite
- **State Management**: Pinia
- **UI Components**: Reka UI + TailwindCSS + Element Plus
- **Charts**: ECharts for data visualization
- **Code Editor**: CodeMirror 6
- **Testing**: Playwright (E2E), Vitest (Unit)
- **Port**: 3000

### AI Services
- **Python Clustering Service** (Port 5001)
  - FastAPI server (`python/cluster_api.py`)
  - DBSCAN clustering algorithm
  - UMAP dimensionality reduction
  - Integration with SiliconFlow API for embeddings and LLM
- **Semantic Embedding Service**
  - Text embedding generation

### Infrastructure
- **Containerization**: Docker + Docker Compose
- **Services**: backend, frontend, mysql, redis, elasticsearch, semantic-embedding, python-cluster
- **Database**: MySQL (not PostgreSQL as initially stated)
- **Health Checks**: Built-in for all services

## Development Commands

### Backend Development
```bash
cd backend

# IMPORTANT: Always clean before compile (per project rules)
./mvnw clean compile

# Run tests
./mvnw test

# Build JAR
./mvnw package

# Local development (port 8080)
./mvnw spring-boot:run
```

### Frontend Development
```bash
cd frontend

# Install dependencies
npm install

# Development server (port 3000)
npm run dev

# TypeScript validation (MUST pass before completion)
npm run type-check

# Production build (MUST succeed before completion)
npm run build

# E2E tests
npm run test:e2e
npm run test:e2e:headed    # With browser UI
npm run test:e2e:debug     # Debug mode

# Performance tests
npm run test:performance
npm run test:k6            # Load testing with k6
```

### Python Services
```bash
# IMPORTANT: Use uv for Python environment management
cd python

# Create virtual environment
uv venv

# Install dependencies
uv pip install -r requirements.txt

# Run clustering service (port 5001)
uv run uvicorn cluster_api:app --host 0.0.0.0 --port 5001
```

### Docker Operations
```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f [service-name]

# Stop all services
docker-compose down

# Rebuild specific service
docker-compose build [service-name]
docker-compose up -d [service-name]

# Connect to MySQL
docker-compose exec mysql mysql -u mgmt_user -p mgmt_db

# Connect to Redis
docker-compose exec redis redis-cli
```

## Code Style & Conventions

### Java/Spring Boot
- **Naming**: PascalCase classes, camelCase methods/variables
- **Entities**: Must extend `BaseEntity` (located in `com.ynet.mgmt.common`)
- **Entity Patterns**:
  - Use `@Entity` with `@Table` and `@Index` annotations
  - Implement business logic methods in entity (e.g., `isActive()`, `isLocked()`)
  - Use `@Version` for optimistic locking
  - Apply Bean Validation annotations (`@NotBlank`, `@Email`, etc.)
- **Database**: snake_case table/column names
- **Documentation**: JavaDoc with Chinese comments
- **Configuration**: Use `@ConfigurationProperties` for external configuration
- **Error Handling**: Create custom exceptions extending `RuntimeException`

### Vue.js/TypeScript
- **Components**: PascalCase (e.g., `UserMenu.vue`, `ClusterScatterChart.vue`)
- **Composition API**: Use `ref`/`reactive` for reactivity
- **Folders**: kebab-case (e.g., `cluster-analysis/`, `hot-topic/`)
- **Types**: Strict TypeScript mode enabled
- **Styles**: TailwindCSS utility classes (light green theme)
- **API Calls**: Centralized in `src/api/` directory
- **State Management**: Use Pinia stores in `src/stores/`

### Python (AI Services)
- **Environment**: Always use `uv` for package management
- **API Framework**: FastAPI for web services
- **Type Hints**: Use Python type annotations
- **Error Handling**: Proper HTTP status codes and error messages

### API Design
- RESTful endpoints under `/api` context path
- Unified response format using `ApiResponse<T>`
- Request validation with Bean Validation
- Proper HTTP status codes (200, 400, 500, etc.)

## Required Checks Before Completion

### Backend
1. **MUST run**: `./mvnw clean compile` (always clean before compile)
2. **MUST pass**: `./mvnw test`
3. Verify entity extends `BaseEntity`
4. Check JavaDoc documentation in Chinese
5. Verify proper exception handling

### Frontend
1. **MUST pass**: `npm run type-check` (no TypeScript errors)
2. **MUST pass**: `npm run build` (successful production build)
3. Verify component naming (PascalCase)
4. Verify folder naming (kebab-case)
5. Check light green theme consistency

### Python Services
1. **MUST use**: `uv` for all Python operations
2. Verify all dependencies in `requirements.txt`
3. Test service endpoints independently

### Integration
1. All services start successfully with Docker Compose
2. Health checks pass for all services
3. Test API: `http://localhost:8080/api/actuator/health`
4. Test frontend: `http://localhost:3000`
5. **Port conflicts**: Kill processes on ports 3000 and 8080 if needed

## Port Management

**CRITICAL**: Only use ports 3000 (frontend) and 8080 (backend)

```bash
# Check port usage
lsof -i :3000
lsof -i :8080

# Kill processes if ports are occupied (auto-approved)
lsof -i :3000 | grep LISTEN | awk '{print $2}' | xargs kill -9
lsof -i :8080 | grep LISTEN | awk '{print $2}' | xargs kill -9
```

## Database Operations
```bash
# Connect to MySQL (NOT PostgreSQL)
docker-compose exec mysql mysql -u mgmt_user -pmgmt_password mgmt_db

# View container status
docker-compose ps

# View MySQL logs
docker-compose logs mysql
```

## Key Integration Points

### Backend-Python Integration
- Backend calls Python service at configured URL (default: `http://localhost:5001`)
- Python service requires SiliconFlow API credentials
- Configuration in `application.yml` under `cluster.*`

### Frontend-Backend Integration
- API calls through centralized `src/api/` modules
- Base URL from environment configuration
- Axios for HTTP requests

## Sub-Agent Usage

- **file-analyzer**: Reading verbose files/logs (especially `backend/logs/application.log`)
- **code-analyzer**: Code analysis, bug hunting, logic tracing
- **test-runner**: Executing tests and analyzing results
- **parallel-worker**: Parallel task execution (preferred for multi-step tasks)

## Testing Philosophy

### General Principles
- **Always use test-runner agent** to execute tests
- **No mock services** - use real services for integration tests
- **Sequential test execution** - complete current test before moving to next
- **Test structure validation** - check test correctness before blaming code
- **Verbose tests** - design for debugging clarity

### Test Types
- **Backend**: JUnit 5 tests with real database/Redis
- **Frontend**: Vitest for unit, Playwright for E2E
- **Performance**: k6 load tests, Playwright performance tests

## Absolute Rules

### Development Workflow
- ✅ **ALWAYS use context7** before writing code to search for relevant information
- ✅ **ALWAYS clean before compile**: `./mvnw clean compile`
- ✅ **Use uv for Python**: All Python operations must use `uv` package manager
- ✅ **Prioritize parallel-worker**: Use for multi-step parallel tasks
- ✅ **Use zsh**: Terminal commands should use zsh (not bash)

### Code Quality
- ❌ **NO PARTIAL IMPLEMENTATION** - complete all features fully
- ❌ **NO SIMPLIFICATION** - no "simplified for now" comments
- ❌ **NO CODE DUPLICATION** - reuse existing functions and constants
- ❌ **NO DEAD CODE** - delete unused code completely
- ❌ **NO INCONSISTENT NAMING** - follow existing codebase patterns
- ❌ **NO OVER-ENGINEERING** - avoid unnecessary abstractions
- ❌ **NO MIXED CONCERNS** - proper layer separation (Controller/Service/Repository)
- ❌ **NO RESOURCE LEAKS** - close connections, clear timeouts, remove listeners

### Testing Requirements
- ✅ **IMPLEMENT TESTS FOR EVERY FUNCTION**
- ✅ **NO CHEATER TESTS** - accurate tests reflecting real usage
- ✅ **DESIGN TO REVEAL FLAWS** - tests must catch bugs

### UI/UX
- 🎨 **Light green theme** for frontend (no i18n needed)
- 🚪 **Ports 3000 & 8080 only** - kill conflicting processes automatically

### Git & Deployment
- 🔧 **Proxy handling**: Git commands must ignore proxy (set in `~/.zshrc`)
- 📝 **No Claude branding**: Git commits should not include Claude attribution
- ⏸️ **No auto-push**: Wait for explicit user request before pushing code
- 🔐 **Server key**: 192.168.155.54 key at `/tmp/ssh_key` (from `~/Documents/code/tmp_prikey.ppk`)

### Infrastructure
- 🐳 **Docker for middleware**: Use Docker for MySQL, Redis, Elasticsearch, etc.
- 📋 **Log location**: Backend logs at `backend/logs/application.log`