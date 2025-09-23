# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> Think carefully and implement the most concise solution that changes as little code as possible.

## Project Overview

deepSearch is a basic management system built with **Spring Boot 3.2.1** (Java 17) backend and **Vue.js 3.5.18** (TypeScript) frontend, using PostgreSQL and Redis for data persistence.

## Architecture

### Backend (Spring Boot)
- **Package Structure**: `com.ynet.mgmt.*`
- **Layers**: Controller → Service → Repository → Entity
- **Database**: PostgreSQL with JPA/Hibernate
- **Port**: 8080 (API context path: `/api`)

### Frontend (Vue.js)
- **Framework**: Vue 3 + TypeScript + Vite
- **State Management**: Pinia
- **UI**: Reka UI + TailwindCSS
- **i18n**: Vue I18n (Chinese/English)
- **Port**: 3000

### Infrastructure
- **Containerization**: Docker + Docker Compose
- **Services**: backend, frontend, postgres, redis
- **Health Checks**: Built-in for all services

## Development Commands

### Environment Management
```bash
# Start development environment
./deploy.sh dev up

# Stop environment
./deploy.sh dev down

# View logs
./deploy.sh dev logs

# Health check
./health-check.sh dev
```

### Backend Development
```bash
cd backend
./mvnw compile          # Compile
./mvnw test            # Run tests
./mvnw package         # Build JAR
./mvnw spring-boot:run # Local development
```

### Frontend Development
```bash
cd frontend
npm run dev            # Development server
npm run type-check     # TypeScript validation
npm run build          # Production build
```

## Code Style & Conventions

### Java/Spring Boot
- **Naming**: PascalCase classes, camelCase methods/variables
- **Entities**: Extend `BaseEntity`, use JPA annotations
- **Validation**: Bean Validation annotations (`@NotBlank`, `@Email`)
- **Database**: snake_case table/column names
- **Documentation**: JavaDoc with Chinese comments

### Vue.js/TypeScript
- **Components**: PascalCase (e.g., `UserMenu.vue`)
- **Composition API**: Use `ref`/`reactive` for reactivity
- **Folders**: kebab-case
- **Types**: Strict TypeScript mode
- **Styles**: TailwindCSS utility classes

### API Design
- RESTful endpoints
- Unified error responses
- Request validation
- Proper HTTP status codes

## Required Checks Before Completion

### Backend
1. `./mvnw compile` - Must pass
2. `./mvnw test` - All tests pass
3. Verify JPA entity design patterns
4. Check JavaDoc documentation

### Frontend
1. `npm run type-check` - No TypeScript errors
2. `npm run build` - Successful production build
3. Verify component naming conventions
4. Test responsive design

### Integration
1. `./deploy.sh dev up` - Full stack deployment
2. `./health-check.sh dev` - All services healthy
3. Test API endpoints: `http://localhost:8080/api/actuator/health`
4. Test frontend: `http://localhost:3000`

## Database Operations
```bash
# Connect to PostgreSQL
docker-compose exec postgres psql -U mgmt_user -d mgmt_db

# View container status
docker-compose ps
```

## Entity Design Patterns

Follow existing `User` entity patterns:
- Use `@Entity` with `@Table` and `@Index` annotations
- Extend `BaseEntity` for audit fields
- Implement business logic methods in entity (e.g., `isActive()`, `isLocked()`)
- Use `@Version` for optimistic locking
- Proper validation with Bean Validation

## Sub-Agent Usage

- Use **file-analyzer** for reading verbose files/logs
- Use **code-analyzer** for code analysis and bug hunting
- Use **test-runner** for executing and analyzing tests

## Absolute Rules

- 这个系统不需要国际化(i18n)
- 前端整体采用淡绿色主题
- 只使用 3000 和 8080 端口启动前后端服务，端口被占用时杀死占用的进程
- NO PARTIAL IMPLEMENTATION
- NO CODE DUPLICATION - reuse existing functions
- NO DEAD CODE - delete unused code
- IMPLEMENT TESTS FOR ALL FUNCTIONS
- NO INCONSISTENT NAMING - follow existing patterns
- NO MIXED CONCERNS - proper layer separation
- NO RESOURCE LEAKS - close connections properly