#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

echo "=== VR 管理平台 — 数据库恢复 ==="

BACKUP_FILE="$1"

if [ -z "$BACKUP_FILE" ]; then
    # 列出可用备份
    echo "用法: ./restore.sh <备份文件路径>"
    echo ""
    echo "可用备份："
    ls -1t ./backups/*.sql 2>/dev/null || echo "  (无可用备份)"
    exit 1
fi

if [ ! -f "$BACKUP_FILE" ]; then
    echo "错误: 文件 $BACKUP_FILE 不存在"
    exit 1
fi

echo "将恢复: $BACKUP_FILE"
read -p "确认恢复？这将覆盖现有数据 (y/N): " CONFIRM

if [ "$CONFIRM" != "y" ] && [ "$CONFIRM" != "Y" ]; then
    echo "已取消"
    exit 0
fi

docker compose exec -T postgres psql -U vr_manager -d vr_manager < "$BACKUP_FILE"
echo "恢复完成"
