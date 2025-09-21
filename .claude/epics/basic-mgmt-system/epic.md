---
name: basic-mgmt-system
status: backlog
created: 2025-09-21T08:01:28Z
progress: 0%
prd: .claude/prds/basic-mgmt-system.md
github: https://github.com/zhailiang23/deepSearch/issues/1
---

# Epic: basic-mgmt-system

## Overview

基础管理系统是一个现代化的前后端分离Web应用，实现用户认证和管理的核心功能。采用Spring Boot 3 + Vue 3技术栈，提供可扩展的技术基础架构。系统以极简设计理念，专注于用户管理核心功能，为后续业务扩展奠定坚实基础。

**技术特点**：
- 前后端完全分离，独立开发和部署
- JWT无状态认证，支持横向扩展
- 基于角色的简单权限控制
- 响应式UI设计，移动端友好
- Docker容器化部署，开发环境一致性

## Architecture Decisions

### 核心技术选择
- **后端框架**: Spring Boot 3.2+
  - *理由*: 成熟稳定，生态丰富，与项目Java技术栈一致
- **持久层**: Spring Data JPA
  - *理由*: 简化数据访问层开发，提供丰富的查询方法，支持审计和分页
- **安全框架**: Spring Security 6
  - *理由*: 与Spring Boot深度集成，JWT支持完善
- **数据库**: PostgreSQL
  - *理由*: 开源、性能优秀、扩展性好，JPA支持完善
- **缓存**: Redis (可选)
  - *理由*: 用于Token黑名单和会话管理

### 前端技术选择
- **框架**: Vue 3 + Composition API
  - *理由*: 现代化、性能好、生态成熟
- **UI库**: shadcn-vue + Tailwind CSS
  - *理由*: 现代化设计、组件丰富、可定制性强
- **状态管理**: Pinia
  - *理由*: Vue 3官方推荐，TypeScript支持好
- **HTTP客户端**: Axios
  - *理由*: 功能完善，拦截器支持JWT自动刷新

### 架构模式
- **后端**: 分层架构 (Controller -> Service -> JPA Repository)
- **数据访问**: Spring Data JPA + 自定义Repository接口
- **前端**: 组件化 + Composables模式
- **API设计**: RESTful风格，统一响应格式
- **认证方式**: JWT Token + 刷新Token机制

## Technical Approach

### Frontend Components

#### 核心页面组件
1. **LoginPage**: 用户登录页面
   - 用户名/邮箱登录支持
   - 密码输入和验证
   - "记住我"功能
   - 错误提示和加载状态

2. **DashboardLayout**: 主布局组件
   - 顶部导航栏（用户信息、退出）
   - 侧边栏导航（可收缩）
   - 主内容区域

3. **UserManagement**: 用户管理页面
   - 用户列表展示（表格）
   - 搜索和筛选功能
   - 创建/编辑用户弹窗
   - 批量操作支持

4. **UserProfile**: 个人资料页面
   - 用户信息展示和编辑
   - 密码修改
   - 登录历史查看

#### 通用组件
- **DataTable**: 可复用数据表格组件
- **FormModal**: 通用表单弹窗组件
- **ConfirmDialog**: 确认对话框组件
- **LoadingSpinner**: 加载状态组件

#### 状态管理
```javascript
// Pinia Store结构
userStore: {
  currentUser,
  isAuthenticated,
  login(),
  logout(),
  refreshToken()
}

userManagementStore: {
  users,
  loading,
  pagination,
  fetchUsers(),
  createUser(),
  updateUser(),
  deleteUser()
}
```

### Backend Services

#### 核心API端点
1. **AuthController**: 认证相关
   - `POST /api/auth/login` - 用户登录
   - `POST /api/auth/logout` - 用户登出
   - `POST /api/auth/refresh` - Token刷新
   - `GET /api/auth/profile` - 获取当前用户信息
   - `PUT /api/auth/profile` - 更新用户信息

2. **UserController**: 用户管理
   - `GET /api/users` - 获取用户列表（分页、搜索）
   - `POST /api/users` - 创建用户
   - `GET /api/users/{id}` - 获取用户详情
   - `PUT /api/users/{id}` - 更新用户信息
   - `DELETE /api/users/{id}` - 删除用户（软删除）
   - `PUT /api/users/{id}/status` - 更新用户状态

#### JPA实体设计
```java
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 枚举定义
    public enum UserRole { ADMIN, USER }
    public enum UserStatus { ACTIVE, DISABLED, LOCKED }
}
```

#### Repository接口设计
```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Page<User> findByStatusAndUsernameContainingIgnoreCase(
        UserStatus status, String username, Pageable pageable);
    Page<User> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
        String username, String email, Pageable pageable);
    long countByStatus(UserStatus status);
}
```

#### 业务逻辑组件
- **UserService**: 用户业务逻辑，使用UserRepository
- **AuthService**: 认证和授权逻辑
- **JwtTokenProvider**: JWT Token生成和验证
- **PasswordEncoder**: 密码加密和验证（BCrypt）

### Infrastructure

#### 部署架构
```yaml
# docker-compose.yml 示例
services:
  backend:
    build: ./backend
    ports: ["8080:8080"]
    environment:
      - DB_URL=jdbc:postgresql://db:5432/mgmt
    depends_on: [db]

  frontend:
    build: ./frontend
    ports: ["3000:80"]
    depends_on: [backend]

  db:
    image: postgres:15
    environment:
      - POSTGRES_DB=mgmt
      - POSTGRES_USER=admin
    volumes: ["db_data:/var/lib/postgresql/data"]
```

#### 监控和日志
- **应用日志**: Logback配置，结构化日志输出
- **访问日志**: 记录API访问和响应时间
- **错误监控**: 全局异常处理和错误报告
- **健康检查**: Spring Boot Actuator端点

## Implementation Strategy

### 开发优先级
1. **MVP功能优先**: 先实现登录和基础用户管理
2. **安全性优先**: JWT认证和基础权限控制
3. **可用性优先**: 响应式设计和错误处理
4. **扩展性考虑**: 模块化设计，便于后续功能添加

### 风险缓解策略
- **技术风险**: 提前进行shadcn-vue组件库调研和原型验证
- **集成风险**: 定义详细的API接口规范，前后端并行开发
- **性能风险**: 数据库索引优化，分页查询，前端虚拟滚动
- **安全风险**: 代码审查，安全测试，OWASP最佳实践

### 测试策略
- **后端**: 单元测试(JUnit) + 集成测试(TestContainers)
- **前端**: 组件测试(Vitest) + E2E测试(Playwright)
- **API测试**: Postman/Newman自动化测试
- **安全测试**: OWASP ZAP扫描

## Task Breakdown Preview

基于简化原则，将任务优化为以下8个核心任务：

- [ ] **项目初始化**: 搭建Spring Boot项目，集成Spring Data JPA和PostgreSQL
- [ ] **JPA实体和Repository**: 设计User实体，创建UserRepository接口
- [ ] **认证系统**: 实现JWT认证、Spring Security配置、权限控制
- [ ] **用户管理API**: 基于JPA Repository实现用户CRUD操作接口
- [ ] **前端基础**: 搭建Vue项目，集成shadcn-vue，实现布局组件
- [ ] **登录页面**: 实现用户登录界面和认证流程
- [ ] **用户管理页面**: 实现用户列表、创建、编辑、删除功能
- [ ] **部署配置**: Docker容器化，包含PostgreSQL数据库

## Dependencies

### 外部服务依赖
- **PostgreSQL 15+**: 主数据库
- **Redis 7+**: 可选，用于Token黑名单
- **Docker & Docker Compose**: 容器化部署

### 开发工具依赖
- **Java 17+**: 后端开发环境
- **Node.js 18+**: 前端开发环境
- **Maven 3.8+**: 后端构建工具
- **Vite**: 前端构建工具

### 核心依赖库
**后端 (Spring Boot)**:
- `spring-boot-starter-data-jpa`: JPA数据访问支持
- `spring-boot-starter-security`: Spring Security集成
- `spring-boot-starter-web`: Web MVC支持
- `postgresql`: PostgreSQL驱动
- `spring-boot-starter-validation`: 数据验证
- `jjwt`: JWT Token处理

**前端 (Vue)**:
- `vue`: Vue 3框架
- `@shadcn/vue`: UI组件库
- `tailwindcss`: CSS框架
- `pinia`: 状态管理
- `axios`: HTTP客户端

### 团队依赖
- **全栈开发工程师**: 1-2名，熟悉Spring Boot、JPA和Vue
- **UI/UX设计**: 可选，shadcn-vue提供基础设计系统
- **DevOps支持**: 部署和运维支持

## Success Criteria (Technical)

### 功能完整性
- [ ] 用户登录/登出功能正常
- [ ] 用户管理CRUD操作完整
- [ ] 权限控制有效（管理员vs普通用户）
- [ ] 响应式设计适配移动端

### 性能基准
- [ ] 登录响应时间 < 1秒
- [ ] 用户列表查询响应时间 < 500ms
- [ ] 页面首次加载时间 < 2秒
- [ ] 支持100个并发用户

### 质量标准
- [ ] 后端单元测试覆盖率 > 80%
- [ ] 前端组件测试覆盖率 > 70%
- [ ] 安全扫描无高危漏洞
- [ ] API文档完整且准确

### 可维护性
- [ ] 代码规范检查通过（ESLint + SonarQube）
- [ ] 项目文档完整（README + API文档）
- [ ] Docker部署配置正确
- [ ] 日志和监控配置完善

## Estimated Effort

### 总体时间估算
- **总工期**: 4-5周（按1名全栈工程师计算）
- **后端开发**: 2周
- **前端开发**: 2周
- **集成测试**: 0.5周
- **部署配置**: 0.5周

### 关键路径
1. **Week 1**: 项目初始化 + 数据库设计 + 认证系统
2. **Week 2**: 用户管理API + 前端基础组件
3. **Week 3**: 登录页面 + 用户管理页面
4. **Week 4**: 功能完善 + 测试 + 部署配置

### 资源需求
- **开发工程师**: 1名全栈（或前后端各1名）
- **测试环境**: 1套完整环境（数据库+应用）
- **部署环境**: Docker支持的生产环境

### 关键里程碑
- **Week 1 End**: 认证系统可用，用户可以登录
- **Week 2 End**: 基础用户管理功能完成
- **Week 3 End**: 前端界面完整，功能联调完成
- **Week 4 End**: 测试通过，部署就绪

---

## Tasks Created
- [ ] 001.md - 项目初始化：搭建Spring Boot项目，集成Spring Data JPA和PostgreSQL (GitHub: #2, parallel: false, 2天)
- [ ] 002.md - JPA实体和Repository：设计User实体，创建UserRepository接口 (GitHub: #3, parallel: false, 2天)
- [ ] 003.md - 认证系统：实现JWT认证、Spring Security配置、权限控制 (GitHub: #4, parallel: false, 2-3天)
- [ ] 004.md - 用户管理API：基于JPA Repository实现用户CRUD操作接口 (GitHub: #5, parallel: false, 2天)
- [ ] 005.md - 前端基础：搭建Vue项目，集成shadcn-vue，实现布局组件 (GitHub: #6, parallel: true, 2-3天)
- [ ] 006.md - 登录页面：实现用户登录界面和认证流程 (GitHub: #7, parallel: false, 2天)
- [ ] 007.md - 用户管理页面：实现用户列表、创建、编辑、删除功能 (GitHub: #8, parallel: false, 3天)
- [ ] 008.md - 部署配置：Docker容器化，包含PostgreSQL数据库 (GitHub: #9, parallel: true, 3天)

**任务统计**:
- 总任务数: 8个
- 并行任务: 2个 (005, 008)
- 串行任务: 6个 (001→002→003→004→006→007)
- 预估总工期: 18-21天 (考虑并行执行约4-5周)

**关键路径**: 001 → 002 → 003 → 004 → 006 → 007

---

**Epic状态**: 已分解为具体任务，准备开始开发
**复杂度**: 中等（适合作为技术基础项目）
**风险等级**: 低（技术栈成熟，需求明确）