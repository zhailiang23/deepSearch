# 基础管理系统 Backend

基础管理系统的Spring Boot后端服务，提供用户认证和管理的核心API。

## 技术栈

- **Java 17**
- **Spring Boot 3.2.1**
- **Spring Data JPA**
- **PostgreSQL**
- **HikariCP** (连接池)
- **Maven** (构建工具)

## 项目结构

```
backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/ynet/mgmt/
│   │   │       ├── MgmtApplication.java          # 主启动类
│   │   │       ├── config/                      # 配置类
│   │   │       │   ├── DatabaseConfig.java      # 数据库配置
│   │   │       │   └── JpaConfig.java          # JPA配置
│   │   │       ├── entity/                      # JPA实体
│   │   │       │   └── BaseEntity.java         # 基础实体类
│   │   │       ├── repository/                  # JPA Repository
│   │   │       ├── service/                     # 业务逻辑层
│   │   │       └── controller/                  # REST控制器
│   │   └── resources/
│   │       ├── application.yml                  # 主配置文件
│   │       ├── application-dev.yml              # 开发环境配置
│   │       └── application-prod.yml             # 生产环境配置
│   └── test/
│       └── java/
│           └── com/ynet/mgmt/
│               └── MgmtApplicationTests.java    # 基础测试
├── pom.xml                                     # Maven配置
└── README.md
```

## 环境要求

- Java 17+
- Maven 3.8+
- PostgreSQL 15+

## 本地开发环境设置

### 1. 数据库设置

#### 使用Docker (推荐)

```bash
# 启动PostgreSQL容器
docker run --name mgmt-postgres \
  -e POSTGRES_DB=mgmt_db_dev \
  -e POSTGRES_USER=admin \
  -e POSTGRES_PASSWORD=password \
  -p 5432:5432 \
  -d postgres:15
```

#### 本地安装PostgreSQL

1. 安装PostgreSQL 15+
2. 创建数据库：
```sql
CREATE DATABASE mgmt_db_dev;
CREATE USER admin WITH PASSWORD 'password';
GRANT ALL PRIVILEGES ON DATABASE mgmt_db_dev TO admin;
```

### 2. 环境变量配置

创建环境变量或在IDE中设置：

```bash
export DB_USERNAME=admin
export DB_PASSWORD=password
```

### 3. 编译和运行

```bash
# 编译项目
mvn clean compile

# 运行测试
mvn test

# 启动应用程序（开发环境）
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 或者打包后运行
mvn clean package
java -jar target/basic-mgmt-system-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

## 配置说明

### 数据库配置

- **开发环境**: `application-dev.yml`
- **生产环境**: `application-prod.yml`
- **测试环境**: 使用H2内存数据库

### JPA配置

- **开发环境**: `ddl-auto: create-drop` (自动创建和删除表)
- **生产环境**: `ddl-auto: validate` (仅验证表结构)
- **启用JPA审计**: 自动记录创建时间、更新时间、创建者、更新者

### 连接池配置

使用HikariCP连接池，配置参数：
- 最大连接数: 10 (开发环境) / 20 (生产环境)
- 最小空闲连接: 5 (开发环境) / 10 (生产环境)
- 连接超时: 20秒
- 空闲超时: 5分钟
- 最大生命周期: 20分钟

## API接口

应用程序启动后，可以访问：

- **应用根路径**: http://localhost:8080/api
- **健康检查**: http://localhost:8080/api/actuator/health
- **应用信息**: http://localhost:8080/api/actuator/info

## 测试

### 运行所有测试
```bash
mvn test
```

### 运行特定测试
```bash
mvn test -Dtest=MgmtApplicationTests
```

测试使用H2内存数据库，无需外部数据库依赖。

## 日志配置

### 开发环境
- 应用日志级别: DEBUG
- SQL日志: 显示SQL语句和参数绑定
- Hibernate日志: 显示详细的ORM操作

### 生产环境
- 应用日志级别: INFO
- SQL日志: 关闭
- 根日志级别: WARN

## 下一步开发

1. 创建User实体和Repository (Task 002)
2. 实现JWT认证系统 (Task 003)
3. 开发用户管理API (Task 004)

## 故障排除

### 常见问题

1. **数据库连接失败**
   - 检查PostgreSQL服务是否启动
   - 验证数据库连接参数
   - 确认用户权限设置正确

2. **应用启动失败**
   - 检查Java版本是否为17+
   - 验证Maven依赖是否正确下载
   - 查看详细错误日志

3. **测试失败**
   - 确认H2数据库依赖已添加
   - 检查测试配置文件是否正确
   - 验证测试环境配置

### 查看日志

```bash
# 查看应用日志
tail -f logs/application.log

# 在开发环境中，日志会输出到控制台
```