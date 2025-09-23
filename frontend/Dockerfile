# 构建阶段
FROM node:20-alpine as builder

WORKDIR /app

# 复制package文件
COPY package*.json ./

# 安装依赖
RUN npm ci --only=production && npm cache clean --force

# 复制源代码
COPY . .

# 构建应用
RUN npm run build

# 生产阶段
FROM nginx:alpine

# 复制nginx配置
COPY docker/nginx/default.conf /etc/nginx/conf.d/

# 复制构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

# 安装curl用于健康检查
RUN apk add --no-cache curl

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD curl -f http://localhost || exit 1

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]