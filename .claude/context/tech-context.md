---
created: 2025-09-21T10:31:04Z
last_updated: 2025-09-23T04:02:26Z
version: 1.2
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

**Security & Authentication**
- **Spring Security:** JWT-based authentication
- **JJWT (io.jsonwebtoken):** JWT token generation and validation
- **BCrypt:** Password hashing
- **Redis:** Token blacklist and refresh token storage

**Database & Persistence**
- **PostgreSQL:** 15-alpine (Primary database)
- **Redis:** 7-alpine (Caching and token management)
- **Hibernate:** ORM through Spring Data JPA
- **HikariCP:** Connection pooling
- **H2 Database:** Testing (in-memory)

**Development Tools**
- **Maven Wrapper:** `./mvnw` for consistent builds
- **Spring Boot Maven Plugin** - Application packaging

### Frontend Technologies

**Core Framework**
- **Vue.js:** 3.5.18 (Composition API)
- **TypeScript:** 5.8.0 (Strict mode)
- **Vite:** 7.0.6 (Build tool and dev server)

**State Management & Routing**
- **Pinia:** 3.0.3 (State management)
- **Vue Router:** 4.5.1 (SPA routing)
- **Vue I18n:** 9.14.5 (Internationalization)

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

### Infrastructure & DevOps

**Containerization**
- **Docker:** Container platform
- **Docker Compose:** Multi-service orchestration
- **Alpine Linux:** Lightweight base images

**Services Configuration**
- **PostgreSQL:** 15-alpine with custom configuration
- **Redis:** 7-alpine for caching/sessions
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

## Dependency Versions

### Backend Dependencies (pom.xml)

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

## Configuration Files

### Backend Configuration

**application.yml:**
- Database connection (PostgreSQL)
- JPA/Hibernate settings
- Actuator endpoints
- Logging configuration
- Server configuration (port 8080, context-path /api)

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
- TailwindCSS configuration
- PostCSS processing
- Component styling patterns

## Development Tools

**Backend Development:**
- Maven Wrapper for consistent builds
- Spring Boot DevTools for hot reload
- Actuator for health monitoring
- H2 for testing without external database

**Frontend Development:**
- Vite dev server with HMR
- Vue DevTools browser extension
- TypeScript language server
- Component library (Reka UI)

**Database Tools:**
- PostgreSQL command line tools
- Docker exec for database access
- JPA/Hibernate DDL generation
- Database migration support

## External Services

**Development Services:**
- PostgreSQL database server
- Redis cache server
- Docker network for service communication

**Monitoring & Health:**
- Spring Actuator health endpoints
- Docker container health checks
- Application logging
- Build information generation

## Version Compatibility

**Minimum Requirements:**
- Java 17 (LTS)
- Node.js 20.19.0+
- Docker with Compose support
- Git 2.x

**Browser Support:**
- Modern browsers with ES2020 support
- TypeScript strict mode compatibility
- Vue 3 Composition API support

## Update History
- 2025-09-23T04:02:26Z: Added Spring Security with JWT authentication, Redis integration, shadcn-vue component library, Playwright testing framework, and enhanced development tools