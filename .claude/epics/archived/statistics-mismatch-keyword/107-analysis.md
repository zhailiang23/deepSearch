---
issue: 107
title: API接口开发
analyzed: 2025-09-28T12:01:53Z
estimated_hours: 12-16
parallelization_factor: 2.5
---

# Parallel Work Analysis: Issue #107

## Overview
为关键词不匹配度排行榜功能设计和实现RESTful API接口。该任务涉及控制器层、DTO层、验证层和测试层的开发，具有较好的并行开发潜力。

## Parallel Streams

### Stream A: 数据传输对象设计与实现
**Scope**: 设计并实现API相关的DTO和枚举类
**Files**:
- `src/main/java/com/ynet/mgmt/statistics/dto/RankingQueryRequest.java`
- `src/main/java/com/ynet/mgmt/statistics/dto/RankingQueryResponse.java`
- `src/main/java/com/ynet/mgmt/statistics/dto/MismatchKeywordRankingDTO.java`
- `src/main/java/com/ynet/mgmt/statistics/enums/TimeRangeEnum.java`
**Agent Type**: backend-specialist
**Can Start**: immediately
**Estimated Hours**: 4-5
**Dependencies**: none

### Stream B: 控制器层设计与实现
**Scope**: 实现RESTful API控制器和请求处理
**Files**:
- `src/main/java/com/ynet/mgmt/statistics/controller/MismatchKeywordController.java`
- `src/main/java/com/ynet/mgmt/config/WebMvcConfig.java` (CORS配置)
**Agent Type**: backend-specialist
**Can Start**: immediately (可与Stream A并行开始)
**Estimated Hours**: 4-5
**Dependencies**: none (控制器框架可以先搭建)

### Stream C: 测试层实现
**Scope**: 编写单元测试和集成测试
**Files**:
- `src/test/java/com/ynet/mgmt/statistics/controller/MismatchKeywordControllerTest.java`
- `src/test/java/com/ynet/mgmt/statistics/dto/RankingQueryRequestTest.java`
- `src/test/java/integration/MismatchKeywordIntegrationTest.java`
**Agent Type**: backend-specialist
**Can Start**: after Streams A & B have basic structure
**Estimated Hours**: 4-6
**Dependencies**: Stream A (需要DTO), Stream B (需要Controller)

## Coordination Points

### Shared Files
以下文件可能需要多个流协调:
- `src/main/java/com/ynet/mgmt/config/SwaggerConfig.java` - API文档配置 (Stream B更新)
- `pom.xml` - 如需新增依赖 (Stream A可能需要validation依赖)

### Sequential Requirements
必须按顺序完成的工作:
1. DTO基础结构 → Controller参数绑定
2. Controller基础框架 → 集成测试
3. 基本功能实现 → 错误处理完善

## Conflict Risk Assessment
- **Low Risk**: 不同的streams工作在不同的类文件上
- **Medium Risk**: 可能在配置文件上有轻微重叠
- **High Risk**: 无高风险冲突点

## Parallelization Strategy

**Recommended Approach**: hybrid

启动策略:
1. **并行启动**: Stream A (DTO设计) 和 Stream B (Controller框架) 同时开始
2. **依次启动**: Stream C (测试) 在A、B有基础结构后开始
3. **协调点**: A和B完成基础部分后，进行一次同步确保接口匹配

## Expected Timeline

With parallel execution:
- Wall time: 6-8 hours (最长Stream的时间)
- Total work: 12-16 hours
- Efficiency gain: 50-60%

Without parallel execution:
- Wall time: 12-16 hours

## Stream-Specific Implementation Plan

### Stream A Details:
1. 创建TimeRangeEnum枚举类 (支持7d/30d/90d)
2. 设计RankingQueryRequest (时间范围、分页参数)
3. 设计MismatchKeywordRankingDTO (关键词、不匹配率等)
4. 设计RankingQueryResponse (包装响应数据)
5. 添加Bean Validation注解

### Stream B Details:
1. 创建MismatchKeywordController类
2. 实现GET /api/statistics/mismatch-keywords/ranking端点
3. 参数验证和类型转换
4. 调用服务层(先mock)
5. 统一响应格式封装
6. 错误处理和异常响应

### Stream C Details:
1. DTO单元测试 (验证validation注解)
2. Controller单元测试 (参数验证、响应格式)
3. 集成测试 (完整API调用流程)
4. 边界条件测试 (无效参数、空数据等)

## Notes
- 该任务具有良好的并行性，DTO和Controller可以独立开发
- 建议先完成基础结构，再进行详细实现
- 测试层可以采用TDD方式，驱动接口设计的完善
- 注意保持API设计的一致性和Spring Boot最佳实践