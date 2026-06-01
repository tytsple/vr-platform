#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="./backups"
mkdir -p "$BACKUP_DIR"

echo "=== VR 管理平台 — 数据库备份 ==="

# 备份 PostgreSQL
docker compose exec -T postgres pg_dump -U vr_manager vr_manager > "$BACKUP_DIR/vr_manager_${TIMESTAMP}.sql"

echo "备份完成: $BACKUP_DIR/vr_manager_${TIMESTAMP}.sql"

# 保留最近 7 份备份
ls -t "$BACKUP_DIR"/*.sql 2>/dev/null | tail -n +8 | xargs -r rm

echo "已清理旧备份（保留最新 7 份）"
