---
name: search-log-manage
status: backlog
created: 2025-09-27T02:02:44Z
updated: 2025-09-27T02:24:58Z
progress: 0%
prd: .claude/prds/search-log-manage.md
github: https://github.com/zhailiang23/deepSearch/issues/83
---

# Epic: search-log-manage

## Overview

实现基础的搜索日志记录与管理系统，通过Spring AOP切面自动记录ElasticsearchDataController.searchData的所有调用，并提供Vue.js管理界面查看日志和点击行为。重点关注最小化性能影响、数据完整性和简洁易用的管理界面。

## Architecture Decisions

- **日志记录策略**: 使用Spring AOP切面，避免对现有搜索代码的侵入性修改
- **数据存储**: 复用现有MySQL数据库，避免引入新的存储技术
- **异步处理**: 日志记录采用异步方式，确保搜索性能不受影响
- **前端复用**: 基于现有Vue.js技术栈，复用shadcn-vue组件库
- **开放访问**: 所有用户均可访问日志查看功能，简化权限控制
- **数据设计**: 一对多关系支持多次点击记录，使用外键关联确保数据一致性

## Technical Approach

### Frontend Components
- **SearchLogManagePage**: 主管理页面，复用现有页面布局模式
- **SearchLogList**: 日志列表组件，支持分页和筛选
- **SearchLogDetail**: 日志详情对话框，展示JSON数据和格式化视图
- **SearchLogFilters**: 筛选器组件，支持时间、状态、用户、关键词筛选
- **ClickBehaviorDisplay**: 点击行为展示组件，显示多次点击序列

### Backend Services
- **SearchLogAspect**: AOP切面拦截搜索请求，异步记录日志
- **SearchLogService**: 日志CRUD服务，处理查询、筛选和点击记录
- **SearchLogController**: REST API控制器，提供日志管理接口
- **SearchLog/SearchClickLog Entity**: JPA实体，对应数据库表结构
- **日志清理定时任务**: 自动清理过期日志数据

### Infrastructure
- **数据库扩展**: 在现有MySQL中新增两张表，利用现有连接池
- **异步配置**: 复用现有AsyncTaskConfig，添加日志记录专用线程池
- **简化访问**: 移除复杂的权限控制，提供开放的日志查看功能
- **监控集成**: 添加日志记录的健康检查端点

## Implementation Strategy

- **阶段1**: 数据模型和AOP切面，确保日志记录功能基本可用
- **阶段2**: 点击行为追踪，实现前端点击事件集成
- **阶段3**: 管理界面开发，提供完整的查询和筛选功能
- **渐进式部署**: 先在开发环境验证性能影响，再逐步上线
- **性能监控**: 持续监控搜索接口性能，确保影响在可接受范围内

## Task Breakdown Preview

High-level task categories that will be created:
- [ ] 数据库设计与实体创建: 创建search_logs和search_click_logs表及对应JPA实体
- [ ] AOP日志记录实现: 开发SearchLogAspect切面，实现自动日志记录
- [ ] 搜索日志服务层: 实现SearchLogService和SearchLogController
- [ ] 点击行为追踪: 前端点击事件集成和后端记录API
- [ ] 日志管理界面: Vue.js管理页面，包含列表、详情、筛选功能
- [ ] 性能优化与测试: 异步处理优化和性能影响验证
- [ ] 数据清理机制: 定时任务和过期数据自动清理

## Dependencies

### External Service Dependencies
- **MySQL数据库**: 存储日志数据，依赖现有数据库连接
- **Elasticsearch服务**: 搜索功能正常运行，提供性能数据

### Internal Team Dependencies
- **搜索模块团队**: 确认ElasticsearchDataController接口稳定性
- **前端框架**: Vue.js组件库和路由系统

### Prerequisite Work
- 确认现有数据库性能容量
- 评估搜索接口当前性能基线
- 准备测试数据和性能测试环境

## Success Criteria (Technical)

### Performance Benchmarks
- 搜索接口平均响应时间增加 < 5%
- 日志记录异步处理延迟 < 50ms
- 日志查询界面响应时间 < 2秒
- 支持并发100用户查看日志

### Quality Gates
- 日志记录成功率 ≥ 99.9%
- 点击行为记录覆盖率 ≥ 95%
- 所有API接口单元测试覆盖率 ≥ 90%
- 前端组件测试覆盖率 ≥ 80%

### Acceptance Criteria
- 用户能够查看完整搜索日志列表和详情
- 支持多维度筛选（时间、状态、用户、关键词）
- 能够查看用户多次点击行为的完整序列
- 日志记录不影响搜索功能正常使用
- 自动清理过期数据机制正常工作

## Estimated Effort

### Overall Timeline
- **总工期**: 6周
- **开发**: 4-5周
- **测试和优化**: 1-2周

### Resource Requirements
- **后端开发**: 1人 × 4周
- **前端开发**: 1人 × 3周
- **测试和优化**: 1人 × 1周

### Critical Path Items
1. AOP切面实现和性能验证（关键路径）
2. 数据库设计和实体映射
3. 前端管理界面核心功能
4. 点击行为追踪集成
5. 性能测试和优化调整

## Tasks Created
- [ ] 084.md - 数据库表设计与创建 (parallel: true) - GitHub: #84
- [ ] 085.md - JPA实体类实现 (parallel: true) - GitHub: #85
- [ ] 086.md - AOP日志记录切面实现 (parallel: true, depends_on: [085]) - GitHub: #86
- [ ] 087.md - 搜索日志服务层实现 (parallel: true, depends_on: [085]) - GitHub: #87
- [ ] 088.md - 日志管理REST API (parallel: false, depends_on: [087]) - GitHub: #88
- [ ] 089.md - 点击行为追踪前端集成 (parallel: true, depends_on: [088]) - GitHub: #89
- [ ] 090.md - 日志管理界面实现 (parallel: true, depends_on: [088]) - GitHub: #90
- [ ] 091.md - 系统集成与测试 (parallel: false, depends_on: [086, 090]) - GitHub: #91

Total tasks: 8
Parallel tasks: 6
Sequential tasks: 2
Estimated total effort: 16-24 person-days (2-3 days per task)