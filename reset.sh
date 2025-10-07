#!/bin/bash
echo "🔄 重置MoviesAPI环境..."

# 停止并删除所有数据
docker-compose down -v

# 重新构建并启动
docker-compose up --build -d

# 等待服务启动
echo "⏳ 等待服务启动..."
sleep 30

# 验证服务状态
echo "✅ 检查服务状态:"
docker ps
echo "🎉 重置完成！"
