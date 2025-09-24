# GitHub Issue Mapping

This document records the mapping between local task files and GitHub issues for the search-data-manage epic.

**Epic Sync Date**: 2025-09-24T19:20:00Z

## Epic Information
- **Epic File**: `.claude/epics/search-data-manage/epic.md`
- **GitHub Epic Issue**: [#35 - Epic: search-data-manage](https://github.com/zhailiang/deepSearch/issues/35)

## Task File Mappings

| Original File | GitHub Issue | New File | Task Name |
|---------------|--------------|----------|-----------|
| 001.md | #36 | 36.md | 导航和路由设置 |
| 002.md | #37 | 37.md | 搜索空间选择组件 |
| 003.md | #38 | 38.md | Elasticsearch全文检索功能 |
| 004.md | #39 | 39.md | 动态结果表格组件 |
| 005.md | #40 | 40.md | 数据记录编辑功能 |
| 006.md | #41 | 41.md | 数据记录删除功能 |
| 007.md | #42 | 42.md | 后端API实现和ES集成 |

## Dependency Updates

The following dependency references were updated in task files:

- **Task 37** (002.md → 37.md): `depends_on: [001]` → `depends_on: [36]`
- **Task 38** (003.md → 38.md): `depends_on: [002, 007]` → `depends_on: [37, 42]`
- **Task 39** (004.md → 39.md): `depends_on: [003]` → `depends_on: [38]`
- **Task 40** (005.md → 40.md): `depends_on: [004]` → `depends_on: [39]`
- **Task 41** (006.md → 41.md): `depends_on: [004]` → `depends_on: [39]`

## Summary

- **Total Tasks**: 7
- **Parallel Tasks**: 7
- **Sequential Tasks**: 0
- **Estimated Effort**: 86 hours
- **GitHub Issues Created**: #35 (epic) + #36-#42 (tasks)

All task files have been successfully renamed to match their GitHub issue numbers and all internal references have been updated accordingly.