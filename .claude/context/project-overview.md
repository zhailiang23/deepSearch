---
created: 2025-09-21T07:41:34Z
last_updated: 2025-09-21T08:16:00Z
version: 1.1
author: Claude Code PM System
---

# Project Overview

## High-Level Summary

deepSearch is an AI-driven project management framework that transforms software development through intelligent agent coordination and context optimization. Built specifically for Claude AI integration, it provides developers and teams with structured workflows, specialized AI agents, and persistent context management to enhance productivity and code quality.

## Core Features

### 🤖 Intelligent Agent System
**Specialized AI Agents for Development Tasks**
- **code-analyzer**: Deep code analysis, bug detection, vulnerability scanning, logic tracing
- **file-analyzer**: Intelligent file summarization, log analysis, content extraction
- **test-runner**: Test execution with AI-powered result analysis and failure diagnosis
- **parallel-worker**: Coordination of multiple concurrent AI tasks and workflows

### 📋 Command Framework
**Structured Automation for Development Workflows**
- **Project Management** (`/pm:*`): Issue tracking, status reporting, PRD management
- **Context Operations** (`/context:*`): Context creation, updating, and session loading
- **Testing Integration** (`/testing:*`): Test configuration and intelligent execution
- **Utility Commands**: Complex prompt handling, system reinitialization, code review processing

### 🧠 Context Intelligence
**Persistent Project Knowledge Management**
- **Structured Documentation**: Comprehensive project context in 9 specialized files
- **Session Continuity**: Preserved context across AI conversation sessions
- **Intelligent Updates**: Automatic context refreshing based on project changes
- **Optimized Loading**: Fast context priming for immediate productivity

### 🔐 Permission System
**Granular Control Over AI Tool Access**
- **Whitelist-based Security**: Explicit permissions for tool usage
- **Tool-specific Controls**: Fine-grained access control for bash, git, npm, python, etc.
- **Domain Restrictions**: Limited web access to trusted domains
- **Audit Trail**: Clear tracking of permitted operations

## Current Capabilities

### Development Workflow Enhancement
- **Fast Project Onboarding**: New developers can understand project context quickly
- **Context-Aware Development**: AI maintains project understanding across sessions
- **Intelligent Code Review**: AI-powered analysis with specialized expertise
- **Parallel Task Execution**: Multiple AI agents working on different aspects simultaneously

### AI Collaboration Patterns
- **Structured AI Interaction**: Consistent patterns for AI-assisted development
- **Context Optimization**: Intelligent summarization to preserve conversation context
- **Specialized Expertise**: Task-specific AI agents with domain knowledge
- **Workflow Automation**: Repeatable patterns for common development tasks

### Project Management Integration
- **GitHub Integration**: Native support for repository operations via `gh` CLI
- **Issue Workflow**: Structured approach to issue creation, tracking, and resolution
- **PRD Management**: Product Requirements Document creation and maintenance
- **Status Tracking**: Comprehensive project status reporting and monitoring

## Technical Architecture

### Framework Components
```
deepSearch Framework
├── Agent System           # Specialized AI agents
├── Command Framework      # Executable workflow commands
├── Context Management     # Project knowledge preservation
├── Permission System      # Security and access control
└── Integration Layer      # Git, GitHub, development tools
```

### Data Flow
```
User Request → Command System → Agent Selection → Specialized Processing → Context Update → User Response
```

### Key Technologies
- **Core Language**: Java (configured, not yet implemented)
- **AI Platform**: Claude AI with agent-based architecture
- **Version Control**: Git with GitHub integration
- **Automation**: Bash scripting with permission controls
- **Documentation**: Markdown-based configuration and context

## Current Project State

### ✅ Completed Framework Elements
- **Agent Definitions**: Four specialized agents with clear capabilities
- **Command System**: 12+ commands across project management, context, and testing
- **Context Structure**: 9 comprehensive context files
- **Permission Configuration**: Granular tool access control
- **GitHub Integration**: Repository connection and CLI access
- **Documentation**: Comprehensive guides and references

### 🔄 Current Status
- **Phase**: Development Planning Complete - Ready for Implementation
- **State**: "basic-mgmt-system" 项目已完全规划，包含完整PRD、Epic和8个开发任务
- **Technology Stack**: Spring Boot 3 + Spring Data JPA + Vue 3 + PostgreSQL 确定
- **Git Status**: All planning artifacts created, ready for initial commit
- **Next Step**: 开始Task 001 - 项目初始化

### 📋 Development Ready Operations
- **Current Focus**: basic-mgmt-system 极简管理系统开发
- **Task Management**: 8个具体开发任务已创建，可开始实施
- **Architecture**: 前后端分离，JWT认证，基于角色的权限控制
- **Timeline**: 预估4-5周完成，支持并行开发模式

## Integration Points

### Version Control Integration
- **Git Operations**: Full git command support with proper permissions
- **GitHub CLI**: Issue management, PR creation, repository operations
- **Branch Management**: Context-aware branch operations and status tracking
- **Commit Integration**: Structured commit processes with AI assistance

### Development Tool Integration
- **Multi-language Support**: Python (pytest), Node.js (npm), Java (future)
- **Testing Frameworks**: Framework-agnostic test execution and analysis
- **Code Quality**: Linting support (ruff for Python, extensible for other languages)
- **Build Systems**: Prepared for Maven, Gradle, npm, etc.

### AI Service Integration
- **Claude AI**: Core AI capabilities through specialized agents
- **Context Optimization**: Intelligent summarization to maximize AI effectiveness
- **Parallel Processing**: Concurrent AI operations for complex tasks
- **Specialized Expertise**: Domain-specific AI knowledge through agent system

## Value Propositions

### For Individual Developers
- **Reduced Context Switching**: AI maintains project awareness across sessions
- **Enhanced Code Quality**: Specialized AI analysis identifies issues and improvements
- **Workflow Acceleration**: Structured patterns for common development tasks
- **Knowledge Preservation**: Project understanding persists beyond individual memory

### For Development Teams
- **Standardized AI Usage**: Consistent patterns for AI-enhanced development
- **Improved Collaboration**: Shared context and workflow understanding
- **Quality Assurance**: Systematic AI-assisted code review processes
- **Knowledge Management**: Team-wide project context preservation

### For Project Success
- **Faster Onboarding**: New team members productive quickly
- **Consistent Quality**: AI-assisted analysis maintains code standards
- **Better Decisions**: AI-powered insights inform architectural choices
- **Technical Debt Reduction**: Systematic identification and resolution of issues

## Extensibility and Customization

### Agent System Extensions
- **Custom Agents**: Framework supports adding new specialized agents
- **Domain Expertise**: Agents can be configured for specific technologies or domains
- **Integration Patterns**: Consistent patterns for agent development and integration

### Command System Expansion
- **Custom Commands**: Easy addition of project-specific commands
- **Workflow Automation**: Commands can be composed for complex operations
- **Permission Integration**: New commands inherit security and access control

### Context Customization
- **Project-Specific Context**: Additional context files for specialized needs
- **Dynamic Updates**: Context system adapts to changing project requirements
- **External Integration**: Context can incorporate external tool outputs

## Performance Characteristics

### Context Efficiency
- **Optimized Loading**: Fast context priming for immediate productivity
- **Intelligent Summarization**: AI agents provide concise, actionable summaries
- **Memory Management**: Context preservation without conversation overload
- **Selective Updates**: Only relevant context changes are propagated

### Execution Performance
- **Parallel Processing**: Multiple agents work simultaneously on complex tasks
- **Caching Strategies**: Intelligent caching of AI responses and context
- **Resource Management**: Controlled resource usage through permission system
- **Scalable Architecture**: Framework scales with project complexity

## Current Development Project: basic-mgmt-system

### 📋 Project Artifacts Created
- **PRD Document**: `.claude/prds/basic-mgmt-system.md` - 完整产品需求文档
- **Epic Plan**: `.claude/epics/basic-mgmt-system/epic.md` - 技术实施方案
- **Development Tasks**: 8个具体任务文件 (001.md through 008.md)
- **Technology Stack**: Spring Boot 3 + JPA + Vue 3 + PostgreSQL

### 🏗️ Implementation Architecture
- **Backend**: Spring Boot 3.2+ + Spring Data JPA + Spring Security 6
- **Frontend**: Vue 3 + shadcn-vue + Tailwind CSS + Pinia状态管理
- **Database**: PostgreSQL with JPA实体设计
- **Authentication**: JWT Token + 刷新Token机制
- **Deployment**: Docker + Docker Compose容器化

### 📈 Development Timeline
- **Total Tasks**: 8个 (项目初始化 → JPA实体 → 认证 → API → 前端 → 登录 → 用户管理 → 部署)
- **Parallel Tasks**: 前端基础开发和部署配置可并行进行
- **Estimated Duration**: 4-5周 (18-21天开发时间)
- **Current Status**: 规划完成，准备开始实施

## Future-Ready Design

### Planned Enhancements
- **Enterprise Features**: Governance, compliance, and scale capabilities
- **Integration Ecosystem**: Plugins and extensions for additional tools
- **AI Model Flexibility**: Support for multiple AI providers and models
- **Distributed Development**: Enhanced support for remote and distributed teams

### Evolution Pathways
- **Business Application**: basic-mgmt-system 提供实际的用户管理功能
- **Platform Expansion**: Evolution into comprehensive AI development platform
- **Industry Specialization**: Vertical-specific AI development frameworks
- **Open Source Ecosystem**: Community-driven extensions and improvements