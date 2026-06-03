#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

echo "=== VR 管理平台 — 种子数据初始化 ==="

# 运行种子数据迁移（幂等，已存在则跳过）
docker compose exec -T postgres psql -U vr_manager -d vr_manager \
  -f /docker-entrypoint-initdb.d/004_seed_test_data.sql

echo ""
echo "种子数据已检查。测试账号:"
echo "  admin/admin123    (管理员)"
echo "  tenant/admin123   (租户用户)"
echo "  operator/admin123 (运维人员)"
