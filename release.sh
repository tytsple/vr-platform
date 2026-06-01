#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

if [ $# -eq 0 ]; then
    echo "用法: ./release.sh <版本号>"
    echo "示例: ./release.sh v1.0.0"
    echo ""
    echo "已有标签:"
    git tag -l --sort=-v:refname | head -10 || echo "  (无)"
    exit 1
fi

VERSION="$1"
MESSAGE="${2:-Release $VERSION}"

# 确保工作区干净
if [ -n "$(git status --porcelain)" ]; then
    echo "❌ 工作区有未提交的改动，请先提交或暂存"
    exit 1
fi

# 打标签并推送
git tag -a "$VERSION" -m "$MESSAGE"
git push origin "$VERSION"

echo ""
echo "✅ 版本 $VERSION 已发布"
echo "   回退到此版本: ./rollback.sh $VERSION"
