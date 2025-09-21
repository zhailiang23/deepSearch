---
created: 2025-09-21T07:41:34Z
last_updated: 2025-09-21T07:41:34Z
version: 1.0
author: Claude Code PM System
---

# Project Style Guide

## Development Philosophy

### Core Principles from `.claude/CLAUDE.md`
1. **Think carefully and implement the most concise solution that changes as little code as possible**
2. **NO PARTIAL IMPLEMENTATION** - Complete all features fully
3. **NO SIMPLIFICATION** - No placeholder comments or incomplete implementations
4. **NO CODE DUPLICATION** - Reuse existing functions and constants
5. **NO DEAD CODE** - Either use or delete completely
6. **NO OVER-ENGINEERING** - Prefer working solutions over enterprise patterns

## Code Style and Standards

### File and Directory Naming
- **Configuration Files**: `kebab-case.md` for context and documentation
- **Command Files**: `category/action.md` or `category/action.sh`
- **Agent Files**: `agent-type.md` (e.g., `code-analyzer.md`)
- **Script Files**: `descriptive-name.sh` with clear purpose indication

### Markdown Standards
- **Frontmatter Required**: All context and configuration files must include YAML frontmatter
- **Structure**: Use consistent heading hierarchy (H1 for main title, H2 for sections)
- **Code Blocks**: Use language-specific syntax highlighting
- **Links**: Use relative paths for internal links, absolute for external

### YAML Frontmatter Format
```yaml
---
created: YYYY-MM-DDTHH:MM:SSZ    # Real UTC datetime
last_updated: YYYY-MM-DDTHH:MM:SSZ
version: MAJOR.MINOR
author: Claude Code PM System
---
```

## Agent Development Standards

### Agent File Structure
```markdown
---
agent: agent-name
capabilities: [tool1, tool2, tool3]
specialization: "brief description"
---

# Agent Name

## Purpose
Clear statement of agent responsibility

## Capabilities
- Specific capability 1
- Specific capability 2

## Usage Patterns
When and how to use this agent

## Integration Points
How agent works with other system components
```

### Agent Naming Convention
- Use `kebab-case` for agent names
- Names should reflect primary function: `code-analyzer`, `test-runner`
- Avoid generic names like `helper` or `utility`

## Command Development Standards

### Command File Structure
```markdown
---
allowed-tools: [Read, Write, Bash, Task]
category: command-category
---

# Command Title

## Purpose
Clear statement of command function

## Required Rules
Reference to any applicable rules from `.claude/rules/`

## Preflight Checklist
Validation steps before execution

## Instructions
Step-by-step command implementation

## Error Handling
How to handle common failure scenarios
```

### Command Implementation Guidelines
- **Fail Fast**: Check all prerequisites before starting operations
- **Clear Error Messages**: Provide actionable error messages with solutions
- **Atomic Operations**: Complete fully or not at all
- **Permission Checks**: Verify tool access before attempting operations

## Context Documentation Standards

### Context File Requirements
- **Frontmatter**: Must include creation date, last update, version, author
- **Current Information**: All information must be accurate and up-to-date
- **Structured Content**: Use consistent heading hierarchy and formatting
- **Actionable Content**: Focus on information that enables development decisions

### Content Guidelines
- **Be Specific**: Avoid vague or generic statements
- **Include Examples**: Provide concrete examples where helpful
- **Reference Files**: Include file paths with line numbers when referencing code
- **Update Regularly**: Keep information current as project evolves

## Error Handling Patterns

### Standard Error Response Format
```
❌ [Error Type]: Brief description
💡 Solution: Specific steps to resolve
🔗 Context: Related information or documentation
```

### Error Categories
- **Configuration Errors**: Missing or invalid configuration
- **Permission Errors**: Insufficient tool access or file permissions
- **Dependency Errors**: Missing required tools or dependencies
- **Validation Errors**: Invalid input or precondition failures

## Testing Standards

### Test Implementation Requirements
- **IMPLEMENT TEST FOR EVERY FUNCTION** - No exceptions
- **NO CHEATER TESTS** - Tests must accurately reflect real usage
- **Verbose Tests** - Tests should be designed for debugging
- **Real Testing** - No mock services, test against real implementations

### Test Naming and Organization
- Test files should clearly indicate what they're testing
- Test names should describe the specific behavior being tested
- Organize tests to match source code structure
- Include both positive and negative test cases

## Code Quality Standards

### Linting and Formatting
- **Python**: Use `ruff` for linting and formatting
- **JavaScript/Node.js**: Follow npm standard patterns
- **Java**: Prepare for standard Java formatting tools
- **Shell Scripts**: Follow bash best practices

### Documentation Requirements
- **API Documentation**: Document all public interfaces
- **Usage Examples**: Include practical examples in documentation
- **Architecture Decisions**: Document significant architectural choices
- **Change Log**: Maintain clear record of changes and rationale

## Git and Version Control Standards

### Commit Message Format
Based on analysis of existing project (single initial commit):
```
Brief description of change

Detailed explanation if needed

No Claude attribution (per user requirements)
```

### Branch Naming
- Use descriptive branch names
- Include issue/feature reference when applicable
- Use `kebab-case` for branch names
- Examples: `feature/context-optimization`, `fix/permission-error`

### File Management
- **Ignore Patterns**: Follow `.gitignore` for Java project patterns
- **Sensitive Information**: Never commit secrets, keys, or personal information
- **Binary Files**: Avoid committing large binary files
- **Generated Files**: Don't commit build artifacts or generated code

## Permission and Security Standards

### Tool Permission Guidelines
- **Whitelist Approach**: Only explicitly allowed tools are permitted
- **Minimal Access**: Grant only necessary permissions for each operation
- **Audit Trail**: Maintain clear record of tool usage
- **Review Regularly**: Periodically review and update permissions

### Security Considerations
- **No Secrets in Code**: Never commit API keys, passwords, or tokens
- **Validate Inputs**: Always validate user inputs before processing
- **Safe Defaults**: Choose secure defaults for all configuration options
- **Access Control**: Implement appropriate access controls for sensitive operations

## Performance and Optimization Standards

### Context Optimization
- **Intelligent Summarization**: Use agents to provide concise summaries
- **Relevant Information**: Include only information necessary for current tasks
- **Structured Data**: Organize information for easy consumption
- **Regular Updates**: Keep context current to maintain relevance

### Resource Management
- **File Handle Management**: Always close files and clean up resources
- **Memory Usage**: Be mindful of memory consumption in long-running operations
- **Network Efficiency**: Minimize unnecessary network requests
- **Caching**: Use intelligent caching for frequently accessed information

## Integration Standards

### Tool Integration Guidelines
- **Standard Interfaces**: Use consistent patterns for tool integration
- **Error Handling**: Implement robust error handling for external tools
- **Version Compatibility**: Ensure compatibility with common tool versions
- **Graceful Degradation**: Handle missing or unavailable tools gracefully

### External Service Integration
- **API Standards**: Follow REST and API best practices
- **Authentication**: Implement secure authentication patterns
- **Rate Limiting**: Respect service rate limits and implement backoff
- **Monitoring**: Include appropriate logging and monitoring

## Maintenance and Evolution

### Code Maintenance Guidelines
- **Regular Refactoring**: Continuously improve code quality
- **Technical Debt Management**: Address technical debt systematically
- **Dependency Updates**: Keep dependencies current and secure
- **Performance Monitoring**: Monitor and optimize system performance

### Documentation Maintenance
- **Regular Updates**: Update documentation as system evolves
- **Accuracy Verification**: Ensure documentation matches implementation
- **Example Updates**: Keep examples current and relevant
- **User Feedback**: Incorporate user feedback into documentation improvements

## Quality Assurance

### Review Process
- **Code Review**: All changes should be reviewed before integration
- **Testing Requirements**: All code must include appropriate tests
- **Documentation Review**: Ensure documentation is complete and accurate
- **Performance Impact**: Consider performance impact of all changes

### Continuous Improvement
- **Metrics Collection**: Collect metrics on system usage and performance
- **User Feedback**: Actively seek and incorporate user feedback
- **Best Practice Evolution**: Continuously evolve best practices based on experience
- **Community Input**: Engage with community for improvement suggestions