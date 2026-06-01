#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

echo "=== VR 管理平台 — 版本回退 ==="
echo ""

if [ $# -ge 1 ]; then
    TARGET="$1"
else
    echo "可用版本:"
    git tag -l --sort=-v:refname | head -20
    echo ""
    echo "用法: ./rollback.sh <版本号>"
    echo "示例: ./rollback.sh v1.0.0"
    exit 1
fi

# 验证标签存在
if ! git rev-parse "$TARGET" >/dev/null 2>&1; then
    echo "❌ 版本 $TARGET 不存在"
    echo "   可用版本: $(git tag -l --sort=-v:refname | tr '\n' ' ')"
    exit 1
fi

# 备份当前数据库
echo "备份数据库..."
./backup.sh 2>/dev/null || true

echo ""
echo "将回退到: $TARGET"
CURRENT=$(git describe --tags --always 2>/dev/null || echo "unknown")
echo "当前版本: $CURRENT"
echo ""

read -p "确认回退？这将重新构建镜像 (y/N): " CONFIRM
if [ "$CONFIRM" != "y" ] && [ "$CONFIRM" != "Y" ]; then
    echo "已取消"
    exit 0
fi

# 切换到目标版本
git checkout "$TARGET"

# 重建并启动
echo ""
echo "构建 $TARGET 版本..."
docker compose build --pull backend frontend
docker compose up -d --remove-orphans

echo ""
echo "✅ 已回退到 $TARGET"
echo "   返回最新版本: git checkout master && ./deploy.sh"
