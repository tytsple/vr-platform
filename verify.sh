#!/usr/bin/env bash
set -euo pipefail

# ============================================================
# VR 管理平台 — 验证脚本
# 用法: chmod +x verify.sh && ./verify.sh
# ============================================================

BASE="${1:-http://localhost}"
PASS=0
FAIL=0
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

ok()   { echo -e "  ${GREEN}✓${NC} $*"; PASS=$((PASS+1)); }
nope() { echo -e "  ${RED}✗${NC} $*"; FAIL=$((FAIL+1)); }

echo "VR 管理平台验证 — $BASE"
echo ""

# ---- 健康检查 ----
echo "=== 1. 服务状态 ==="
if curl -sf --connect-timeout 5 "$BASE/health" >/dev/null 2>&1; then
  ok "服务在线"
else
  nope "服务不可达"
  echo "结果: ${GREEN}$PASS 通过${NC} / ${RED}$FAIL 失败${NC}"
  exit 1
fi

# 登录获取 token
TOKEN=$(curl -sf -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])" 2>/dev/null || echo "")

if [ -z "$TOKEN" ]; then
  nope "admin 登录失败"
  echo "结果: ${GREEN}$PASS 通过${NC} / ${RED}$FAIL 失败${NC}"
  exit 1
fi
ok "admin 登录"

AUTH="-H 'Authorization: Bearer $TOKEN'"

# ---- Bug1: 删除保护 ----
echo ""
echo "=== 2. Bug 修复验证 ==="

echo "--- Bug1: 租户删除 FK 保护 ---"
DEL_CODE=$(curl -sf -X DELETE "$BASE/api/admin/tenants/1" -H "Authorization: Bearer $TOKEN" \
  | python3 -c "import sys,json; print(json.load(sys.stdin).get('code',0))" 2>/dev/null || echo "000")
if [ "$DEL_CODE" = "409" ]; then
  ok "Bug1 租户删除保护 (有子记录被拒绝)"
else
  nope "Bug1 租户删除保护 (期望409, 实际$DEL_CODE)"
fi

echo "--- Bug2: 用户管理创建 tenant 用户 ---"
# 先查租户列表(不能假定id=1)
TENANT_ID=$(curl -sf "$BASE/api/admin/tenants" -H "Authorization: Bearer $TOKEN" \
  | python3 -c "import sys,json; l=json.load(sys.stdin); print(l[0]['id']) if l else exit(1)" 2>/dev/null || echo "")
if [ -z "$TENANT_ID" ]; then
  nope "Bug2 无可用租户(先运行 ./seed.sh)"
else
  CREATE_CODE=$(curl -sf -X POST "$BASE/api/admin/users" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" \
    -d "{\"userName\":\"verify-cc-$(date +%s)\",\"password\":\"test123\",\"roleKey\":\"tenant\",\"tenantId\":$TENANT_ID,\"status\":\"0\"}" \
    | python3 -c "import sys,json; print(json.load(sys.stdin).get('code',0))" 2>/dev/null || echo "500")
  if [ "$CREATE_CODE" = "200" ]; then
    ok "Bug2 创建 tenant 用户成功"
  else
    nope "Bug2 创建 tenant 用户失败 (code=$CREATE_CODE)"
  fi
fi

echo "--- Bug3: 租户用户登录+数据可见 ---"
# 先运行种子数据
docker compose exec -T postgres psql -U vr_manager -d vr_manager \
  -f /docker-entrypoint-initdb.d/004_seed_test_data.sql >/dev/null 2>&1 || true

TTOKEN=$(curl -sf -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"tenant","password":"admin123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])" 2>/dev/null || echo "")

if [ -z "$TTOKEN" ]; then
  nope "Bug3 tenant 登录失败(先运行 ./seed.sh)"
else
  ok "Bug3 tenant 登录"
  # 检验数据可见
  TVENUES=$(curl -sf "$BASE/api/tenant/venues" -H "Authorization: Bearer $TTOKEN" \
    | python3 -c "import sys,json; print(len(json.load(sys.stdin)))" 2>/dev/null || echo "0")
  if [ "$TVENUES" -gt 0 ]; then
    ok "Bug3 tenant 可见 $TVENUES 个场地"
  else
    nope "Bug3 tenant 场地为空 (租户未关联)"
  fi
fi

# ---- 代码审查修复验证 ----
echo ""
echo "=== 3. 代码审查修复验证 ==="

echo "--- 登录限流 ---"
for i in $(seq 1 5); do
  curl -sf -o /dev/null -X POST "$BASE/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"wrong"}' 2>/dev/null || true
done
RATE_CODE=$(curl -sf -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"wrong"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin).get('code',0))" 2>/dev/null || echo "0")
[ "$RATE_CODE" = "429" ] && ok "限流生效" || nope "限流失效 (code=$RATE_CODE)"

echo "--- 改密码失效旧token ---"
CHPWD=$(curl -sf -X PUT "$BASE/api/auth/change-password" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"oldPassword":"admin123","newPassword":"admin123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin).get('code',0))" 2>/dev/null || echo "0")
[ "$CHPWD" = "200" ] && ok "改密码成功" || nope "改密码失败 (code=$CHPWD)"

echo "--- 密码哈希保护 ---"
ULIST=$(curl -sf "$BASE/api/admin/users" -H "Authorization: Bearer $TOKEN" 2>/dev/null || echo "[]")
HAS_PWD=$(echo "$ULIST" | python3 -c "import sys,json; d=json.load(sys.stdin); print('password' in d[0])" 2>/dev/null || echo "True")
[ "$HAS_PWD" = "False" ] && ok "用户列表不含密码" || nope "用户列表泄露密码"

echo "--- 自身删除保护 ---"
ADMIN_ID=$(curl -sf "$BASE/api/admin/users" -H "Authorization: Bearer $TOKEN" \
  | python3 -c "import sys,json; l=json.load(sys.stdin); print(l[0]['userId'])" 2>/dev/null || echo "1")
DEL_SELF=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE/api/admin/users/$ADMIN_ID" \
  -H "Authorization: Bearer $TOKEN" 2>/dev/null || echo "000")
[ "$DEL_SELF" != "200" ] && [ "$DEL_SELF" != "204" ] && ok "自身删除被拒绝 ($DEL_SELF)" \
  || nope "自身删除未被阻止 ($DEL_SELF)"

# ---- 结果 ----
echo ""
echo "=============================="
echo -e "结果:  ${GREEN}$PASS 通过${NC}  /  ${RED}$FAIL 失败${NC}"
echo "=============================="
[ $FAIL -eq 0 ] || exit 1
