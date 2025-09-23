---
created: 2025-09-21T10:31:04Z
last_updated: 2025-09-21T10:31:04Z
version: 1.0
author: Claude Code PM System
---

# Project Style Guide

## Code Style Standards

### Java/Spring Boot Backend

**Naming Conventions**
```java
// Classes: PascalCase
public class UserRepository {}
public class MgmtApplication {}

// Methods and Variables: camelCase
public void updateLastLogin() {}
private String emailAddress;

// Constants: UPPER_SNAKE_CASE
public static final int MAX_LOGIN_ATTEMPTS = 5;
public static final String DEFAULT_ROLE = "USER";

// Packages: lowercase with dots
package com.ynet.mgmt.entity;
package com.ynet.mgmt.repository;
```

**Class Organization Pattern**
```java
public class User extends BaseEntity {
    // 1. Static constants
    public static final int MAX_USERNAME_LENGTH = 50;

    // 2. Instance fields (with annotations)
    @Column(name = "username", unique = true, nullable = false)
    private String username;

    // 3. Constructors
    public User() {}
    public User(String username, String email) {}

    // 4. Business methods
    public boolean isActive() {}
    public void lockAccount() {}

    // 5. Getters and setters
    public String getUsername() {}
    public void setUsername(String username) {}

    // 6. Object methods (equals, hashCode, toString)
    @Override
    public boolean equals(Object o) {}
}
```

**Documentation Standards**
```java
/**
 * 用户实体类
 * 管理系统用户的基本信息、状态和安全相关数据
 *
 * @author system
 * @since 1.0.0
 */
@Entity
public class User extends BaseEntity {

    /**
     * 检查用户是否处于激活状态
     * @return true if 用户状态为ACTIVE
     */
    public boolean isActive() {
        return UserStatus.ACTIVE.equals(this.status);
    }
}
```

**Database Conventions**
```java
// Table names: snake_case
@Table(name = "users")

// Column names: snake_case
@Column(name = "full_name")
@Column(name = "created_at")

// Index names: descriptive with prefix
@Index(name = "idx_username", columnList = "username")
@Index(name = "idx_email", columnList = "email")
```

### Vue.js/TypeScript Frontend

**Component Naming**
```typescript
// Components: PascalCase (single file)
UserMenu.vue
LanguageSelector.vue
DefaultLayout.vue

// Component directories: kebab-case
dropdown-menu/
ui/button/
layout/common/
```

**File Structure Pattern**
```vue
<!-- Template first -->
<template>
  <div class="user-menu">
    <button @click="toggleMenu">
      {{ user.name }}
    </button>
  </div>
</template>

<!-- Script second -->
<script setup lang="ts">
import { ref, computed } from 'vue'
import type { User } from '@/types/auth'

// Props and emits
interface Props {
  user: User
}
const props = defineProps<Props>()
const emit = defineEmits<{
  logout: []
}>()

// Reactive state
const isMenuOpen = ref(false)

// Computed properties
const displayName = computed(() =>
  props.user.fullName || props.user.username
)

// Methods
function toggleMenu() {
  isMenuOpen.value = !isMenuOpen.value
}
</script>

<!-- Styles last -->
<style scoped>
.user-menu {
  @apply relative inline-block;
}
</style>
```

**TypeScript Conventions**
```typescript
// Interfaces: PascalCase with descriptive names
interface User {
  id: number
  username: string
  email: string
}

interface UserSearchCriteria {
  keyword?: string
  status?: UserStatus
  role?: UserRole
}

// Enums: PascalCase
enum UserStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  LOCKED = 'LOCKED'
}

// Functions: camelCase
function validateUser(user: User): boolean {}
function formatDisplayName(user: User): string {}
```

**Composable Pattern**
```typescript
// Composables: use prefix + descriptive name
export function useAuth() {
  const user = ref<User | null>(null)
  const isLoggedIn = computed(() => !!user.value)

  async function login(credentials: LoginCredentials) {
    // Implementation
  }

  function logout() {
    user.value = null
  }

  return {
    user: readonly(user),
    isLoggedIn,
    login,
    logout
  }
}
```

## File Organization Patterns

### Backend Directory Structure
```
src/main/java/com/ynet/mgmt/
├── config/          # Configuration classes
├── controller/      # REST controllers
├── dto/            # Data transfer objects
├── entity/         # JPA entities
├── exception/      # Custom exceptions
├── repository/     # Data access layer
├── service/        # Business logic
└── util/           # Utility classes
```

### Frontend Directory Structure
```
src/
├── components/
│   ├── ui/         # Reusable UI components
│   ├── layout/     # Layout components
│   ├── common/     # Common business components
│   └── icons/      # Icon components
├── composables/    # Vue composables
├── layouts/        # Page layouts
├── pages/          # Route components
├── stores/         # Pinia stores
├── types/          # TypeScript definitions
├── utils/          # Utility functions
└── assets/         # Static assets
```

## Comment Style

### Java Comments
```java
/**
 * JavaDoc for public APIs
 * Include @param, @return, @throws as needed
 */
public void updateUser(User user) {
    // Single line comments for implementation details
    validateUserData(user);

    /* Multi-line comments for complex logic
     * that requires detailed explanation
     */
    if (user.getStatus() == UserStatus.LOCKED) {
        // Check if lock period has expired
        checkLockExpiration(user);
    }
}
```

### TypeScript Comments
```typescript
/**
 * JSDoc for public functions and interfaces
 * @param user - The user to validate
 * @returns True if user is valid
 */
function validateUser(user: User): boolean {
  // Inline comments for business logic
  if (!user.email || !user.email.includes('@')) {
    return false
  }

  // TODO: Add more validation rules
  return true
}
```

### Vue Template Comments
```vue
<template>
  <!-- Component description comments -->
  <div class="user-profile">
    <!-- Conditional rendering explanation -->
    <div v-if="user.isVerified" class="verified-badge">
      Verified
    </div>

    <!-- Complex template logic explanation -->
    <UserActions
      :user="user"
      @edit="handleEdit"
      @delete="handleDelete"
    />
  </div>
</template>
```

## Configuration File Patterns

### Backend Configuration (application.yml)
```yaml
# Environment-specific configuration
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}

  # Database configuration
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:mgmt_db}
    username: ${DB_USERNAME:mgmt_user}
    password: ${DB_PASSWORD:mgmt_password}

  # JPA configuration
  jpa:
    hibernate:
      ddl-auto: ${DDL_AUTO:create-drop}
    show-sql: ${SHOW_SQL:true}
```

### Frontend Configuration (package.json)
```json
{
  "scripts": {
    "dev": "vite",
    "build": "run-p type-check \"build-only {@}\" --",
    "preview": "vite preview",
    "type-check": "vue-tsc --build"
  },
  "dependencies": {
    "@vueuse/core": "^13.9.0",
    "vue": "^3.5.18"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^6.0.1",
    "typescript": "~5.8.0"
  }
}
```

## Error Handling Patterns

### Backend Error Handling
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            ValidationException ex) {
        return ResponseEntity
            .badRequest()
            .body(new ErrorResponse("VALIDATION_ERROR", ex.getMessage()));
    }
}
```

### Frontend Error Handling
```typescript
// Store error handling
export const useErrorStore = defineStore('error', () => {
  const errors = ref<string[]>([])

  function addError(message: string) {
    errors.value.push(message)
  }

  function clearErrors() {
    errors.value = []
  }

  return { errors, addError, clearErrors }
})

// Component error handling
try {
  await userService.updateUser(user)
  showSuccess('User updated successfully')
} catch (error) {
  showError('Failed to update user')
  console.error('User update error:', error)
}
```

## Testing Conventions

### Backend Testing
```java
@SpringBootTest
class UserServiceTest {

    @Test
    @DisplayName("Should create user with valid data")
    void shouldCreateUserWithValidData() {
        // Given
        User user = new User("testuser", "test@example.com");

        // When
        User result = userService.createUser(user);

        // Then
        assertThat(result.getId()).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
    }
}
```

### Frontend Testing (Future)
```typescript
describe('UserMenu', () => {
  it('should display user name', () => {
    const user = { id: 1, username: 'testuser', email: 'test@example.com' }
    const wrapper = mount(UserMenu, { props: { user } })

    expect(wrapper.text()).toContain('testuser')
  })
})
```

## Git Commit Conventions

### Commit Message Format
```
type(scope): subject

body (optional)

footer (optional)
```

### Examples
```bash
# Feature commits
feat(user): 添加用户登录功能
feat(api): 实现用户管理REST API

# Bug fixes
fix(auth): 修复登录失败次数计算错误
fix(ui): 解决用户列表分页显示问题

# Documentation
docs(readme): 更新安装说明
docs(api): 添加用户API文档

# Refactoring
refactor(service): 重构用户服务层结构
refactor(component): 优化用户菜单组件

# Testing
test(user): 添加用户实体单元测试
test(integration): 添加用户API集成测试
```

## Code Quality Standards

### General Principles
1. **DRY (Don't Repeat Yourself)** - 避免代码重复
2. **SOLID Principles** - 遵循面向对象设计原则
3. **Clean Code** - 编写清晰、可读的代码
4. **Performance** - 考虑性能影响
5. **Security** - 遵循安全最佳实践

### Code Review Checklist
- [ ] 代码符合命名约定
- [ ] 包含适当的注释和文档
- [ ] 错误处理完整
- [ ] 测试覆盖充分
- [ ] 性能考虑合理
- [ ] 安全实践正确
- [ ] 遵循项目架构模式