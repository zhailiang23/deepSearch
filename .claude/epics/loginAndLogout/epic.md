---
name: loginAndLogout
status: in_progress
created: 2025-09-21T11:00:25Z
updated: 2025-09-21T12:06:31Z
progress: 22%
prd: .claude/prds/loginAndLogout.md
github: https://github.com/zhailiang23/deepSearch/issues/10
---

# Epic: loginAndLogout

## Overview

实现基于现有 User 实体的简洁 JWT 认证系统，充分利用已有的失败登录跟踪和账户锁定机制。采用无状态 JWT 令牌，无需额外的会话存储依赖。前端使用 Vue 3 + Pinia 状态管理，后端扩展现有 Spring Boot 架构。

## Architecture Decisions

### 认证策略
- **JWT Token**: 完全无状态令牌，包含所有必要用户信息
- **本地存储**: 前端管理令牌存储，无需服务器端会话
- **Spring Security**: 标准安全框架，与现有架构集成
- **BCrypt 密码哈希**: 已在 User 实体中使用，保持一致性
- **简化架构**: 无需 Redis 或其他外部会话存储

### 安全设计
- **利用现有 User 实体**: `failedLoginAttempts`、`accountLockedUntil` 字段
- **IP 跟踪**: 扩展 `lastLoginIp` 字段功能
- **审计日志**: 基于现有日志框架
- **HTTPS 强制**: 生产环境必须启用

### 前端架构
- **Pinia Store**: 集中管理认证状态
- **Axios 拦截器**: 自动处理 JWT 令牌
- **Vue Router Guards**: 路由级别的权限控制
- **组件复用**: 利用现有 UI 组件库

## Technical Approach

### Frontend Components
- **LoginForm.vue**: 主登录表单组件
  - 用户名/邮箱切换输入
  - 密码输入与显示切换
  - 实时表单验证

- **AuthGuard**: 路由守卫中间件
  - 检查 JWT 令牌有效性
  - 自动重定向到登录页
  - 处理令牌过期

- **UserMenu 增强**: 扩展现有组件
  - 添加退出功能
  - 显示用户登录状态

### Backend Services
- **AuthController**: 认证 REST API
  - `POST /api/auth/login` - 用户登录
  - `POST /api/auth/logout` - 用户退出（客户端清除令牌）
  - `GET /api/auth/me` - 获取当前用户信息

- **AuthService**: 业务逻辑层
  - 用户名/邮箱认证逻辑
  - 失败登录次数管理（利用现有字段）
  - 账户锁定检查和处理
  - JWT 令牌生成和验证

- **JwtTokenProvider**: 令牌工具类
  - JWT 生成和解析
  - 令牌有效性验证
  - 安全密钥管理

- **SecurityConfig**: Spring Security 配置
  - JWT 过滤器链
  - 密码编码器配置
  - CORS 和 CSRF 配置

### Infrastructure
- **简化部署**:
  - 无需额外中间件依赖
  - 仅依赖现有 PostgreSQL 数据库
  - JWT 密钥配置

- **数据库**:
  - 利用现有 User 表和索引
  - 可能添加 `login_history` 表用于审计
  - 无需会话存储表

- **容器化**:
  - 在现有 Docker Compose 中配置
  - 环境变量管理
  - 健康检查集成

## Implementation Strategy

### 开发阶段
1. **Phase 1 (核心认证)**: 基础登录/退出 + JWT 实现
2. **Phase 2 (安全增强)**: 失败跟踪 + 账户锁定集成
3. **Phase 3 (用户体验)**: UI 优化 + 错误处理完善

### 风险缓解
- **渐进式开发**: 每个阶段可独立测试和部署
- **现有功能复用**: 最大化利用已有 User 实体功能
- **向后兼容**: 不破坏现有用户数据结构

### 测试策略
- **单元测试**: AuthService、JwtTokenProvider
- **集成测试**: 完整登录流程
- **安全测试**: 防暴力攻击、会话安全
- **E2E 测试**: 用户登录退出完整流程

## Task Breakdown Preview

高级任务类别（限制在 8 个以内）:

- [ ] **Backend Security Infrastructure**: Spring Security + JWT 配置和核心认证服务
- [ ] **Authentication API**: 登录/退出 REST 端点实现
- [ ] **Frontend Authentication System**: Vue 3 登录组件 + Pinia 状态管理
- [ ] **JWT Token Management**: 纯前端令牌管理
- [ ] **Security Enhancements**: 失败登录跟踪 + 账户锁定集成
- [ ] **User Experience**: 错误处理 + 表单验证 + 多语言支持
- [ ] **Integration & Testing**: API 集成 + 路由守卫 + 端到端测试
- [ ] **Production Readiness**: 性能优化 + 监控 + 部署配置

## Dependencies

### External Dependencies
- **Spring Security Starter**: 认证框架
- **JWT Library**: `io.jsonwebtoken:jjwt-api`
- **BCrypt**: 密码哈希（已存在）

### Internal Dependencies
- **User Entity**: 已完成，需要充分利用现有字段
- **PostgreSQL Database**: 现有数据库服务
- **Frontend UI Components**: 现有组件库
- **Database Indices**: 现有用户表索引

### Team Dependencies
- **无阻塞依赖**: 可以独立开发，不依赖其他团队
- **测试环境**: 需要完整的容器化环境

## Success Criteria (Technical)

### 性能基准
- 登录 API 响应时间 < 500ms (95th percentile)
- JWT 验证时间 < 50ms
- 并发登录支持 > 1000 用户
- 无会话存储查询延迟

### 质量门禁
- 代码覆盖率 > 90%
- 安全扫描零高危漏洞
- 负载测试通过（1000 并发用户）
- 浏览器兼容性测试通过

### 验收标准
- 用户可以使用用户名或邮箱登录
- 失败 5 次后账户锁定 15 分钟
- JWT 令牌在有效期内保持登录状态
- 退出功能清除客户端令牌
- 响应式设计在移动设备正常显示

## Estimated Effort

### 总体时间线: 2-3 周
- **Week 1**: Backend 安全基础设施 + 核心 API（2-3 个任务）
- **Week 2**: Frontend 认证系统 + JWT 管理（2-3 个任务）
- **Week 3**: 安全增强 + 用户体验 + 生产准备（2 个任务）

### 资源需求
- **1-2 名全栈开发者**: 可以并行开发前后端
- **现有基础设施**: 无需额外资源
- **测试环境**: 使用现有 Docker 环境

### 关键路径
1. JWT 和 Spring Security 集成（基础）
2. 前端 Pinia 状态管理（核心）
3. JWT 令牌生命周期管理（支撑）
4. 安全功能与现有 User 实体集成（增强）

### 风险缓解时间
- **预留 20% 缓冲时间**: 处理意外技术难题
- **每周迭代评估**: 及时调整进度和优先级

## Tasks Created
- [ ] 001.md - Backend Security Infrastructure (parallel: false)
- [ ] 002.md - JWT Token Management (parallel: false)
- [ ] 003.md - Authentication API (parallel: false)
- [ ] 004.md - Security Enhancements (parallel: true)
- [ ] 005.md - Frontend Authentication System (parallel: true)
- [ ] 006.md - User Experience (parallel: true)
- [ ] 007.md - Integration & Testing (parallel: false)

Total tasks: 7
Parallel tasks: 3
Sequential tasks: 4
Estimated total effort: 52 hours