---
created: 2025-09-21T07:41:34Z
last_updated: 2025-09-21T09:09:21Z
version: 1.1
author: Claude Code PM System
---

# Technology Context

## Current Technology Stack

### Core Technologies

#### Backend Stack (Implemented)
- **Primary Language**: Java 17+
- **Framework**: Spring Boot 3.2.1
- **Data Access**: Spring Data JPA
- **Database**: PostgreSQL 15+
- **Security**: Spring Security 6 (for upcoming auth features)
- **Build Tool**: Maven
- **Connection Pool**: HikariCP

#### Frontend Stack (Implemented)
- **Framework**: Vue 3 + Composition API
- **Language**: TypeScript
- **Build Tool**: Vite
- **UI Library**: shadcn-vue + Tailwind CSS
- **State Management**: Pinia
- **HTTP Client**: Axios
- **Routing**: Vue Router 4
- **Internationalization**: Vue I18n

#### Infrastructure & DevOps (Implemented)
- **Containerization**: Docker + Docker Compose
- **Web Server**: Nginx (reverse proxy)
- **Cache**: Redis
- **Environment Management**: Multi-stage configurations
- **Deployment**: Automated scripts (deploy.sh)
- **Version Control**: Git + GitHub
- **AI Framework**: Claude AI with agent-based architecture

### Development Tools

#### Allowed Development Tools (from `.claude/settings.local.json`)
- **Package Managers**:
  - npm, npx, pnpm (Node.js ecosystem)
  - composer (PHP)
- **Programming Languages**:
  - Python (with pytest for testing)
  - Node.js/JavaScript
- **Testing Frameworks**:
  - pytest (Python)
  - Custom test runner via `.claude/scripts/test-and-log.sh`
- **Code Quality**:
  - ruff (Python linting)
- **System Tools**:
  - bash, find, grep, sed
  - git, gh (GitHub CLI)
  - Standard Unix utilities (ls, mv, rm, touch, tree)

### AI Agent Technology

#### Specialized Agents
1. **code-analyzer** - Code analysis and vulnerability detection
2. **file-analyzer** - File content analysis and summarization
3. **test-runner** - Test execution and result analysis
4. **parallel-worker** - Parallel task coordination

#### Agent Capabilities
- Context optimization through intelligent summarization
- Parallel execution for complex workflows
- Specialized knowledge for different domains
- Integration with development tools

## Dependencies and Frameworks

### Current Dependencies
**Status**: No package management files detected
- No `package.json` (Node.js)
- No `requirements.txt` or `pyproject.toml` (Python)
- No `Cargo.toml` (Rust)
- No `go.mod` (Go)
- No Maven/Gradle files (Java)

### Expected Dependencies (Future)
Based on Java project setup and agent system:
- **Java Build Tool**: Maven or Gradle
- **Testing Framework**: JUnit or TestNG
- **Logging**: SLF4J, Logback
- **JSON Processing**: Jackson or Gson
- **HTTP Client**: Java 11+ HttpClient or OkHttp

### AI Integration Dependencies
- **Claude AI API**: For agent communication
- **GitHub CLI**: For repository integration (`gh` tool)
- **Shell Scripting**: Bash for automation

## Development Environment

### Required Environment
- **Java**: JDK (version TBD)
- **Git**: Version control
- **GitHub CLI**: Repository operations
- **Shell**: Bash for script execution
- **Claude Code**: AI development assistant

### Optional Tools
- **Python**: For testing and automation (pytest available)
- **Node.js**: For JavaScript tooling (npm, npx, pnpm available)
- **ruff**: Python code formatting and linting

## Configuration Management

### Environment Configuration
- **Git Configuration**: Required for commits and pushes
- **GitHub Authentication**: Required for `gh` commands
- **Shell Environment**: Bash with PATH for development tools

### Project Configuration
- **`.claude/settings.local.json`**: Tool permissions and access control
- **`.gitignore`**: Java project ignore patterns
- **`.claude/CLAUDE.md`**: Development rules and practices

## Build and Deployment

### Current State
**Status**: No build system configured
- No build scripts or configuration files
- No deployment configuration
- Framework project without business logic

### Expected Build System (Future)
For Java project:
- **Build Tool**: Maven (`pom.xml`) or Gradle (`build.gradle`)
- **Compilation**: `javac` or build tool
- **Testing**: JUnit with build tool integration
- **Packaging**: JAR or WAR files
- **Dependencies**: Maven Central or similar repository

### Testing Strategy
- **Test Runner Agent**: Intelligent test execution with analysis
- **Framework Agnostic**: Supports pytest, npm test, Java testing
- **Logging**: Custom test-and-log script for output capture
- **Analysis**: AI-powered test result interpretation

## Integration Points

### GitHub Integration
- **Repository**: git@github.com:zhailiang23/deepSearch.git
- **CLI Access**: `gh` tool for issues, PRs, releases
- **Authentication**: SSH key or token-based

### Claude AI Integration
- **Agent System**: Specialized AI agents for different tasks
- **Command System**: Markdown-based command execution
- **Context Management**: Intelligent context preservation
- **Permission System**: Granular tool access control

## Technology Decisions

### Chosen Patterns
- **Configuration as Code**: Markdown files for system configuration
- **Agent-Based Architecture**: Specialized AI agents for different concerns
- **Permission-Based Security**: Explicit tool access control
- **Context Optimization**: AI agents provide summaries to preserve context

### Technology Constraints
- **Shell Access**: Bash commands allowed with specific tool permissions
- **File Operations**: Full file system access for project files
- **Web Access**: Limited to GitHub domain and general web search
- **Language Support**: Multi-language support (Java, Python, Node.js)

## Future Technology Considerations

### Potential Additions
- **Java Framework**: Spring Boot, Micronaut, or Quarkus
- **Database**: PostgreSQL, MySQL, or embedded H2
- **Caching**: Redis or in-memory caching
- **API Framework**: JAX-RS, Spring MVC, or similar
- **Containerization**: Docker for deployment
- **CI/CD**: GitHub Actions or similar

### Monitoring and Observability
- **Logging Framework**: SLF4J with Logback
- **Metrics**: Micrometer for application metrics
- **Health Checks**: Framework-provided health endpoints
- **Documentation**: OpenAPI/Swagger for API documentation

## Implementation Status Update (2025-09-21)

### ✅ Major Technology Implementations Completed

#### Backend Stack Implemented (Task 001)
- **Spring Boot 3.2.1**: Production-ready application with health checks
- **Spring Data JPA**: Entity management and repository layer
- **PostgreSQL Integration**: Database connectivity and configuration
- **Multi-environment Support**: dev/test/prod configurations
- **Maven Build System**: Dependency management and build automation

#### Frontend Stack Implemented (Task 005)
- **Vue 3 + TypeScript**: Modern reactive frontend framework
- **shadcn-vue + Tailwind**: Professional UI component system
- **Pinia**: State management for authentication and app state
- **Vue Router**: Client-side routing with guards
- **Internationalization**: Chinese/English language support
- **Theme System**: Dark/light mode switching

#### DevOps Infrastructure Implemented (Task 008)
- **Docker Containerization**: Multi-stage builds for both applications
- **Container Orchestration**: Development, test, and production environments
- **Database Services**: PostgreSQL and Redis containers
- **Reverse Proxy**: Nginx configuration for production
- **Deployment Automation**: Shell scripts for environment management

### 📊 Technology Readiness: 100% Infrastructure Complete

The project now has a complete, working technology stack with:
- ✅ **Full-stack Development Environment**: Both frontend and backend running
- ✅ **Production-Ready Infrastructure**: Docker containerization complete
- ✅ **Modern Development Workflow**: TypeScript, hot reloading, debugging
- ✅ **Scalable Architecture**: Microservices-ready container setup

**Next Phase**: Business logic implementation starting with JPA entities and authentication systems.
