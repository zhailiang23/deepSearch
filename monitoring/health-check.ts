#!/usr/bin/env node

/**
 * 生产环境健康检查脚本
 * 检查各个服务的健康状态并生成报告
 */

import { promisify } from 'util';
import { exec as execCallback } from 'child_process';
import * as https from 'https';
import * as http from 'http';

const exec = promisify(execCallback);

interface ServiceHealth {
  name: string;
  status: 'healthy' | 'unhealthy' | 'unknown';
  responseTime: number;
  details?: string;
  timestamp: Date;
}

interface HealthCheckResult {
  overall: 'healthy' | 'degraded' | 'unhealthy';
  services: ServiceHealth[];
  timestamp: Date;
  environment: string;
}

class HealthChecker {
  private readonly config = {
    timeout: 5000,
    retries: 3,
    services: {
      nginx: { port: 80, path: '/health', protocol: 'http' },
      backend: { port: 8080, path: '/api/actuator/health', protocol: 'http' },
      postgres: { port: 5432 },
      redis: { port: 6379 }
    }
  };

  /**
   * 执行HTTP健康检查
   */
  private async checkHttpService(
    name: string,
    port: number,
    path: string,
    protocol: 'http' | 'https' = 'http'
  ): Promise<ServiceHealth> {
    const startTime = Date.now();

    return new Promise((resolve) => {
      const client = protocol === 'https' ? https : http;

      const req = client.request({
        hostname: 'localhost',
        port,
        path,
        method: 'GET',
        timeout: this.config.timeout
      }, (res) => {
        const responseTime = Date.now() - startTime;

        let data = '';
        res.on('data', (chunk) => {
          data += chunk;
        });

        res.on('end', () => {
          const status = res.statusCode === 200 ? 'healthy' : 'unhealthy';
          resolve({
            name,
            status,
            responseTime,
            details: `HTTP ${res.statusCode} - ${data.slice(0, 100)}`,
            timestamp: new Date()
          });
        });
      });

      req.on('error', (error) => {
        const responseTime = Date.now() - startTime;
        resolve({
          name,
          status: 'unhealthy',
          responseTime,
          details: error.message,
          timestamp: new Date()
        });
      });

      req.on('timeout', () => {
        req.destroy();
        const responseTime = Date.now() - startTime;
        resolve({
          name,
          status: 'unhealthy',
          responseTime,
          details: 'Request timeout',
          timestamp: new Date()
        });
      });

      req.end();
    });
  }

  /**
   * 检查数据库连接
   */
  private async checkDatabase(): Promise<ServiceHealth> {
    const startTime = Date.now();

    try {
      const { stdout } = await exec('docker-compose exec -T postgres pg_isready -U mgmt_user -d mgmt_db');
      const responseTime = Date.now() - startTime;

      return {
        name: 'postgres',
        status: stdout.includes('accepting connections') ? 'healthy' : 'unhealthy',
        responseTime,
        details: stdout.trim(),
        timestamp: new Date()
      };
    } catch (error) {
      const responseTime = Date.now() - startTime;
      return {
        name: 'postgres',
        status: 'unhealthy',
        responseTime,
        details: error instanceof Error ? error.message : 'Unknown error',
        timestamp: new Date()
      };
    }
  }

  /**
   * 检查Redis连接
   */
  private async checkRedis(): Promise<ServiceHealth> {
    const startTime = Date.now();

    try {
      const { stdout } = await exec('docker-compose exec -T redis redis-cli -a "$REDIS_PASSWORD" ping');
      const responseTime = Date.now() - startTime;

      return {
        name: 'redis',
        status: stdout.trim() === 'PONG' ? 'healthy' : 'unhealthy',
        responseTime,
        details: stdout.trim(),
        timestamp: new Date()
      };
    } catch (error) {
      const responseTime = Date.now() - startTime;
      return {
        name: 'redis',
        status: 'unhealthy',
        responseTime,
        details: error instanceof Error ? error.message : 'Unknown error',
        timestamp: new Date()
      };
    }
  }

  /**
   * 检查Docker容器状态
   */
  private async checkContainerHealth(): Promise<ServiceHealth[]> {
    try {
      const { stdout } = await exec('docker-compose ps --format json');
      const containers = stdout.split('\n').filter(line => line.trim()).map(line => JSON.parse(line));

      return containers.map(container => ({
        name: `container-${container.Service}`,
        status: container.State === 'running' ? 'healthy' : 'unhealthy',
        responseTime: 0,
        details: `State: ${container.State}, Status: ${container.Status}`,
        timestamp: new Date()
      }));
    } catch (error) {
      return [{
        name: 'docker-containers',
        status: 'unhealthy',
        responseTime: 0,
        details: error instanceof Error ? error.message : 'Unknown error',
        timestamp: new Date()
      }];
    }
  }

  /**
   * 检查系统资源
   */
  private async checkSystemResources(): Promise<ServiceHealth[]> {
    const checks = [];

    try {
      // 检查磁盘空间
      const { stdout: dfOutput } = await exec('df -h /');
      const diskUsage = dfOutput.split('\n')[1].split(/\s+/)[4];
      const diskUsagePercent = parseInt(diskUsage.replace('%', ''));

      checks.push({
        name: 'disk-space',
        status: diskUsagePercent < 85 ? 'healthy' : diskUsagePercent < 95 ? 'degraded' : 'unhealthy',
        responseTime: 0,
        details: `Disk usage: ${diskUsage}`,
        timestamp: new Date()
      });
    } catch (error) {
      checks.push({
        name: 'disk-space',
        status: 'unknown' as const,
        responseTime: 0,
        details: 'Failed to check disk space',
        timestamp: new Date()
      });
    }

    try {
      // 检查内存使用
      const { stdout: memOutput } = await exec('free -m');
      const memLines = memOutput.split('\n');
      const memInfo = memLines[1].split(/\s+/);
      const totalMem = parseInt(memInfo[1]);
      const usedMem = parseInt(memInfo[2]);
      const memUsagePercent = Math.round((usedMem / totalMem) * 100);

      checks.push({
        name: 'memory',
        status: memUsagePercent < 80 ? 'healthy' : memUsagePercent < 90 ? 'degraded' : 'unhealthy',
        responseTime: 0,
        details: `Memory usage: ${memUsagePercent}% (${usedMem}MB / ${totalMem}MB)`,
        timestamp: new Date()
      });
    } catch (error) {
      checks.push({
        name: 'memory',
        status: 'unknown' as const,
        responseTime: 0,
        details: 'Failed to check memory',
        timestamp: new Date()
      });
    }

    return checks;
  }

  /**
   * 执行完整健康检查
   */
  public async performHealthCheck(): Promise<HealthCheckResult> {
    console.log('🏥 Starting health check...');

    const allChecks: ServiceHealth[] = [];

    // HTTP服务检查
    const httpChecks = await Promise.all([
      this.checkHttpService('nginx', 80, '/health'),
      this.checkHttpService('backend', 8080, '/api/actuator/health')
    ]);
    allChecks.push(...httpChecks);

    // 数据库和缓存检查
    const [dbCheck, redisCheck] = await Promise.all([
      this.checkDatabase(),
      this.checkRedis()
    ]);
    allChecks.push(dbCheck, redisCheck);

    // 容器状态检查
    const containerChecks = await this.checkContainerHealth();
    allChecks.push(...containerChecks);

    // 系统资源检查
    const systemChecks = await this.checkSystemResources();
    allChecks.push(...systemChecks);

    // 计算整体健康状态
    const healthyCount = allChecks.filter(check => check.status === 'healthy').length;
    const unhealthyCount = allChecks.filter(check => check.status === 'unhealthy').length;
    const totalCount = allChecks.length;

    let overall: 'healthy' | 'degraded' | 'unhealthy';
    if (unhealthyCount === 0) {
      overall = 'healthy';
    } else if (healthyCount >= totalCount * 0.7) {
      overall = 'degraded';
    } else {
      overall = 'unhealthy';
    }

    return {
      overall,
      services: allChecks,
      timestamp: new Date(),
      environment: process.env.NODE_ENV || 'production'
    };
  }

  /**
   * 生成健康检查报告
   */
  public generateReport(result: HealthCheckResult): string {
    const { overall, services, timestamp } = result;

    let report = `\n=== Health Check Report ===\n`;
    report += `Timestamp: ${timestamp.toISOString()}\n`;
    report += `Overall Status: ${overall.toUpperCase()}\n`;
    report += `Environment: ${result.environment}\n\n`;

    // 按状态分组
    const statusGroups = {
      healthy: services.filter(s => s.status === 'healthy'),
      degraded: services.filter(s => s.status === 'degraded'),
      unhealthy: services.filter(s => s.status === 'unhealthy'),
      unknown: services.filter(s => s.status === 'unknown')
    };

    Object.entries(statusGroups).forEach(([status, serviceList]) => {
      if (serviceList.length > 0) {
        const statusIcon = status === 'healthy' ? '✅' :
                          status === 'degraded' ? '⚠️' :
                          status === 'unhealthy' ? '❌' : '❓';

        report += `${statusIcon} ${status.toUpperCase()} (${serviceList.length})\n`;

        serviceList.forEach(service => {
          report += `  - ${service.name}: ${service.details} (${service.responseTime}ms)\n`;
        });
        report += '\n';
      }
    });

    return report;
  }
}

// CLI执行
async function main() {
  const checker = new HealthChecker();

  try {
    const result = await checker.performHealthCheck();
    const report = checker.generateReport(result);

    console.log(report);

    // 根据健康状态设置退出码
    const exitCode = result.overall === 'healthy' ? 0 :
                     result.overall === 'degraded' ? 1 : 2;

    process.exit(exitCode);
  } catch (error) {
    console.error('❌ Health check failed:', error);
    process.exit(3);
  }
}

// 如果作为主模块运行
if (require.main === module) {
  main();
}

export { HealthChecker, type ServiceHealth, type HealthCheckResult };