/**
 * 安全头配置模块
 *
 * 提供生产环境的安全头配置，包括：
 * - HTTPS安全配置
 * - 内容安全策略(CSP)
 * - 跨域资源共享(CORS)
 * - 其他安全相关头
 */

/**
 * 基础安全头配置
 */
const SECURITY_HEADERS = {
  // HTTPS传输安全
  'Strict-Transport-Security': 'max-age=31536000; includeSubDomains; preload',

  // 防止点击劫持
  'X-Frame-Options': 'SAMEORIGIN',

  // 防止MIME类型嗅探
  'X-Content-Type-Options': 'nosniff',

  // XSS防护
  'X-XSS-Protection': '1; mode=block',

  // 引用者策略
  'Referrer-Policy': 'strict-origin-when-cross-origin',

  // 权限策略
  'Permissions-Policy': 'geolocation=(), microphone=(), camera=(), payment=(), usb=(), magnetometer=(), gyroscope=()',

  // 跨域嵌入策略
  'Cross-Origin-Embedder-Policy': 'require-corp',

  // 跨域开启策略
  'Cross-Origin-Opener-Policy': 'same-origin',

  // 跨域资源策略
  'Cross-Origin-Resource-Policy': 'same-site'
};

/**
 * 内容安全策略(CSP)配置
 *
 * 这个策略允许：
 * - 同源脚本和内联脚本（开发需要）
 * - 同源样式和内联样式
 * - 图片可以从任何HTTPS源加载
 * - 字体只能从同源加载
 * - 连接只能到同源
 */
const CSP_POLICY = {
  'default-src': "'self'",
  'script-src': "'self' 'unsafe-inline' 'unsafe-eval'",
  'style-src': "'self' 'unsafe-inline'",
  'img-src': "'self' data: https:",
  'font-src': "'self'",
  'connect-src': "'self'",
  'media-src': "'self'",
  'object-src': "'none'",
  'child-src': "'self'",
  'frame-src': "'self'",
  'worker-src': "'self'",
  'frame-ancestors': "'self'",
  'form-action': "'self'",
  'upgrade-insecure-requests': ''
};

/**
 * 生产环境CSP策略（更严格）
 */
const CSP_POLICY_PRODUCTION = {
  'default-src': "'self'",
  'script-src': "'self' 'sha256-HASH_OF_INLINE_SCRIPTS'",
  'style-src': "'self' 'sha256-HASH_OF_INLINE_STYLES'",
  'img-src': "'self' data:",
  'font-src': "'self'",
  'connect-src': "'self'",
  'media-src': "'self'",
  'object-src': "'none'",
  'child-src': "'none'",
  'frame-src': "'none'",
  'worker-src': "'self'",
  'frame-ancestors': "'none'",
  'form-action': "'self'",
  'upgrade-insecure-requests': '',
  'require-trusted-types-for': "'script'"
};

/**
 * CORS配置
 */
const CORS_CONFIG = {
  origin: process.env.ALLOWED_ORIGINS?.split(',') || ['http://localhost:3000'],
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization', 'X-Requested-With'],
  exposedHeaders: ['X-Total-Count'],
  credentials: true,
  maxAge: 86400 // 24小时预检缓存
};

/**
 * 环境特定的安全配置
 */
const ENVIRONMENT_CONFIG = {
  development: {
    csp: CSP_POLICY,
    reportOnly: true,
    httpsRedirect: false
  },
  production: {
    csp: CSP_POLICY_PRODUCTION,
    reportOnly: false,
    httpsRedirect: true
  }
};

/**
 * 生成CSP头字符串
 * @param {Object} policy CSP策略对象
 * @returns {string} CSP头字符串
 */
function generateCSPHeader(policy) {
  return Object.entries(policy)
    .map(([directive, value]) => `${directive} ${value}`)
    .join('; ');
}

/**
 * 获取环境特定的安全头
 * @param {string} environment 环境名称
 * @returns {Object} 安全头对象
 */
function getSecurityHeaders(environment = 'production') {
  const config = ENVIRONMENT_CONFIG[environment] || ENVIRONMENT_CONFIG.production;
  const headers = { ...SECURITY_HEADERS };

  // 添加CSP头
  const cspHeaderName = config.reportOnly ? 'Content-Security-Policy-Report-Only' : 'Content-Security-Policy';
  headers[cspHeaderName] = generateCSPHeader(config.csp);

  return headers;
}

/**
 * Express中间件：应用安全头
 * @param {string} environment 环境名称
 * @returns {Function} Express中间件函数
 */
function securityHeadersMiddleware(environment = 'production') {
  const headers = getSecurityHeaders(environment);

  return (req, res, next) => {
    // 应用安全头
    Object.entries(headers).forEach(([name, value]) => {
      res.setHeader(name, value);
    });

    // 移除暴露服务器信息的头
    res.removeHeader('X-Powered-By');

    next();
  };
}

/**
 * Nginx配置生成器
 * @param {string} environment 环境名称
 * @returns {string} Nginx安全头配置
 */
function generateNginxSecurityHeaders(environment = 'production') {
  const headers = getSecurityHeaders(environment);

  return Object.entries(headers)
    .map(([name, value]) => `add_header ${name} "${value}" always;`)
    .join('\n    ');
}

/**
 * 验证CSP策略
 * @param {Object} policy CSP策略对象
 * @returns {Array} 验证错误数组
 */
function validateCSPPolicy(policy) {
  const errors = [];
  const requiredDirectives = ['default-src', 'script-src', 'style-src'];

  requiredDirectives.forEach(directive => {
    if (!policy[directive]) {
      errors.push(`Missing required directive: ${directive}`);
    }
  });

  // 检查不安全的配置
  if (policy['script-src']?.includes('unsafe-eval')) {
    errors.push('Warning: unsafe-eval in script-src can be dangerous');
  }

  if (policy['object-src'] !== "'none'") {
    errors.push('Recommendation: Set object-src to "none" for better security');
  }

  return errors;
}

/**
 * 安全头测试工具
 * @param {Object} headers HTTP头对象
 * @returns {Object} 测试结果
 */
function testSecurityHeaders(headers) {
  const results = {
    score: 0,
    maxScore: 10,
    tests: []
  };

  const tests = [
    { name: 'HSTS', header: 'Strict-Transport-Security', weight: 2 },
    { name: 'CSP', header: 'Content-Security-Policy', weight: 3 },
    { name: 'X-Frame-Options', header: 'X-Frame-Options', weight: 1 },
    { name: 'X-Content-Type-Options', header: 'X-Content-Type-Options', weight: 1 },
    { name: 'X-XSS-Protection', header: 'X-XSS-Protection', weight: 1 },
    { name: 'Referrer-Policy', header: 'Referrer-Policy', weight: 1 },
    { name: 'Permissions-Policy', header: 'Permissions-Policy', weight: 1 }
  ];

  tests.forEach(test => {
    const hasHeader = !!headers[test.header];
    results.tests.push({
      name: test.name,
      passed: hasHeader,
      header: test.header,
      value: headers[test.header] || null
    });

    if (hasHeader) {
      results.score += test.weight;
    }
  });

  results.grade = results.score >= 9 ? 'A' :
                  results.score >= 7 ? 'B' :
                  results.score >= 5 ? 'C' :
                  results.score >= 3 ? 'D' : 'F';

  return results;
}

module.exports = {
  SECURITY_HEADERS,
  CSP_POLICY,
  CSP_POLICY_PRODUCTION,
  CORS_CONFIG,
  ENVIRONMENT_CONFIG,
  getSecurityHeaders,
  securityHeadersMiddleware,
  generateNginxSecurityHeaders,
  generateCSPHeader,
  validateCSPPolicy,
  testSecurityHeaders
};

// 如果作为脚本直接运行，输出Nginx配置
if (require.main === module) {
  console.log('# Nginx Security Headers Configuration');
  console.log(generateNginxSecurityHeaders('production'));
}