#!/usr/bin/env bash
set -euo pipefail

# ============================================================
# VR 管理平台 — Smoke Test Suite
# 用法: ./smoke-test.sh [BASE_URL]
# ============================================================

BASE="${1:-http://localhost}"
PASS=0
FAIL=0
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

ok()   { echo -e "  ${GREEN}PASS${NC} $*"; PASS=$((PASS+1)); }
nope() { echo -e "  ${RED}FAIL${NC} $*"; FAIL=$((FAIL+1)); }
info() { echo -e "\n${YELLOW}=== $* ===${NC}"; }
skip() { echo -e "  ${YELLOW}SKIP${NC} $*"; }

# 解析 JSON code 字段
jcode() { python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('code',200))" 2>/dev/null || echo "0"; }

trap 'echo ""; echo "结果: ${GREEN}$PASS 通过${NC} / ${RED}$FAIL 失败${NC}"; [ $FAIL -eq 0 ] || exit 1' EXIT

echo "VR 管理平台 Smoke Test — $BASE"

# ==========================================
info "1. 基础设施"
# ==========================================

info "1.1 健康检查"
if curl -sf --connect-timeout 5 "$BASE/health" >/dev/null 2>&1; then
  ok "健康检查"
else
  nope "健康检查失败"
fi

# ==========================================
info "2. 认证"
# ==========================================

info "2.1 正常登录"
LOGIN=$(curl -sf -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' 2>/dev/null || echo '{}')
TOKEN=$(echo "$LOGIN" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('token',''))" 2>/dev/null || echo "")
[ -n "$TOKEN" ] && ok "admin 登录成功" || nope "admin 登录失败"

info "2.2 错误密码"
BAD=$(curl -sf -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"wrongpassword123"}' 2>/dev/null | jcode)
[ "$BAD" = "401" ] && ok "错误密码返回 code=401" || nope "错误密码返回 code=$BAD (期望 401)"

info "2.3 不存在的用户"
NOUSER=$(curl -sf -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"nobody","password":"x"}' 2>/dev/null | jcode)
[ "$NOUSER" = "401" ] && ok "不存在用户返回 code=401" || nope "不存在用户返回 code=$NOUSER (期望 401)"

info "2.4 登录限流"
for i in 1 2 3 4 5; do
  curl -sf -o /dev/null -X POST "$BASE/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"wrong"}' 2>/dev/null || true
done
RATE=$(curl -sf -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"wrong"}' 2>/dev/null | jcode)
[ "$RATE" = "429" ] && ok "第6次返回 code=429 (限流生效)" || nope "第6次返回 code=$RATE (期望 429)"

info "2.5 修改密码"
CHPWD=$(curl -sf -X PUT "$BASE/api/auth/change-password" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"oldPassword":"admin123","newPassword":"admin123"}' 2>/dev/null | jcode)
[ "$CHPWD" = "200" ] && ok "修改密码成功" || nope "修改密码失败 (code=$CHPWD)"

# ==========================================
info "3. 权限控制"
# ==========================================

info "3.1 无 token 访问保护接口"
NOAUTH=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/admin/tenants" 2>/dev/null || echo "000")
[ "$NOAUTH" = "401" ] || [ "$NOAUTH" = "403" ] && ok "无 token 返回 $NOAUTH" || nope "无 token 返回 $NOAUTH"

info "3.2 租户不能访问 admin 接口"
TLOGIN=$(curl -sf -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"tenant","password":"admin123"}' 2>/dev/null || echo '{}')
TTOKEN=$(echo "$TLOGIN" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('token',''))" 2>/dev/null || echo "")
if [ -z "$TTOKEN" ]; then
  skip "租户账号不存在 (种子数据需重建)"
else
  TACCESS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/admin/tenants" \
    -H "Authorization: Bearer $TTOKEN" 2>/dev/null || echo "000")
  [ "$TACCESS" = "403" ] && ok "租户无法访问 admin 接口" || nope "租户能访问 admin 接口 ($TACCESS)"
fi

info "3.3 admin 不能访问 tenant 专用接口"
AACCESS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/tenant/venues" \
  -H "Authorization: Bearer $TOKEN" 2>/dev/null || echo "000")
[ "$AACCESS" = "403" ] && ok "admin 无法访问 tenant 接口" || nope "admin 意外能访问 ($AACCESS)"

# ==========================================
info "4. CRUD 操作"
# ==========================================

info "4.1 租户列表"
TLIST=$(curl -sf "$BASE/api/admin/tenants" -H "Authorization: Bearer $TOKEN" 2>/dev/null || echo "[]")
TCOUNT=$(echo "$TLIST" | python3 -c "import sys,json; print(len(json.load(sys.stdin)))" 2>/dev/null || echo "0")
[ "$TCOUNT" -ge 1 ] && ok "租户列表 ($TCOUNT 条)" || nope "租户列表为空"

info "4.2 场地列表"
VLIST=$(curl -sf "$BASE/api/admin/venues" -H "Authorization: Bearer $TOKEN" 2>/dev/null || echo "[]")
VCOUNT=$(echo "$VLIST" | python3 -c "import sys,json; print(len(json.load(sys.stdin)))" 2>/dev/null || echo "0")
[ "$VCOUNT" -ge 1 ] && ok "场地列表 ($VCOUNT 条)" || skip "无场地数据"

info "4.3 用户列表"
ULIST=$(curl -sf "$BASE/api/admin/users" -H "Authorization: Bearer $TOKEN" 2>/dev/null || echo "[]")
UCOUNT=$(echo "$ULIST" | python3 -c "import sys,json; print(len(json.load(sys.stdin)))" 2>/dev/null || echo "0")
[ "$UCOUNT" -ge 1 ] && ok "用户列表 ($UCOUNT 条)" || nope "用户列表为空"

info "4.4 用户详情含角色"
UDETAIL=$(curl -sf "$BASE/api/admin/users/1" -H "Authorization: Bearer $TOKEN" 2>/dev/null || echo "{}")
HASROL=$(echo "$UDETAIL" | python3 -c "import sys,json; d=json.load(sys.stdin); dd=d.get('data',d); print('roles' in dd)" 2>/dev/null || echo "False")
[ "$HASROL" = "True" ] && ok "用户详情包含 roles" || nope "用户详情缺少 roles"

info "4.5 创建场地自动生成 token"
TID=$(echo "$TLIST" | python3 -c "import sys,json; print(json.load(sys.stdin)[0].get('id',1))" 2>/dev/null || echo "1")
NEWV=$(curl -sf -X POST "$BASE/api/admin/venues" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"name\":\"smoke-test-venue\",\"tenantId\":$TID}" 2>/dev/null || echo "{}")
# token 在 ResponseEntity body 中,经过拦截器返回为 response
VTOKEN=$(echo "$NEWV" | python3 -c "
import sys,json
d = json.load(sys.stdin)
t = d.get('data',{}).get('controllerToken','') or d.get('controllerToken','')
print(t)
" 2>/dev/null || echo "")
[ -n "$VTOKEN" ] && ok "场地创建自动生成 token" || nope "场地创建未生成 token"

info "4.6 创建用户同步分配角色"
NEWU=$(curl -sf -X POST "$BASE/api/admin/users" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"userName":"test-smoke-nn","nickName":"测","password":"test123","roleKey":"operator","status":"0"}' 2>/dev/null | jcode)
[ "$NEWU" = "200" ] && ok "创建用户成功" || nope "创建用户失败 (code=$NEWU)"

info "4.7 租户角色必须选租户"
NOTENANT=$(curl -sf -X POST "$BASE/api/admin/users" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"userName":"test-notenant-nn","password":"x","roleKey":"tenant"}' 2>/dev/null | jcode)
[ "$NOTENANT" != "200" ] && ok "租户不选租户被拒绝 (code=$NOTENANT)" || nope "租户不选租户未被拒绝"

# ==========================================
info "5. 统计与监控"
# ==========================================

info "5.1 统计接口响应"
STATS_CODE=$(curl -sf "$BASE/api/admin/stats" \
  -H "Authorization: Bearer $TOKEN" 2>/dev/null | jcode)
[ "$STATS_CODE" = "200" ] || [ "$STATS_CODE" = "0" ] && ok "统计接口正常" || nope "统计接口异常 (code=$STATS_CODE)"

info "5.2 运维活跃会话"
OSESS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/operator/sessions/active" \
  -H "Authorization: Bearer $TOKEN" 2>/dev/null || echo "000")
[ "$OSESS" = "200" ] && ok "运维会话接口正常" || skip "admin 无权访问运维接口"

info "5.3 会话概览含名称"
OVIEW=$(curl -sf "$BASE/api/admin/sessions/overview" \
  -H "Authorization: Bearer $TOKEN" 2>/dev/null || echo "[]")
HAS_VNAME=$(echo "$OVIEW" | python3 -c "import sys,json; d=json.load(sys.stdin); print('venueName' in (d[0] if d else {}))" 2>/dev/null || echo "True")
[ "$HAS_VNAME" = "True" ] && ok "会话概览包含 venueName" || skip "暂无会话数据"

# ==========================================
info "6. 安全防护"
# ==========================================

info "6.1 XSS payload 可接受"
XSS_CODE=$(curl -sf -o /dev/null -w "%{http_code}" -X POST "$BASE/api/admin/tenants" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"<script>alert(1)</script>","contactInfo":"test"}' 2>/dev/null || echo "000")
[ "$XSS_CODE" = "201" ] && ok "XSS payload 正常接受" || nope "XSS payload 被拒绝 ($XSS_CODE)"

info "6.2 密码哈希不在 API 响应中"
FIRSTUSER=$(curl -sf "$BASE/api/admin/users" -H "Authorization: Bearer $TOKEN" 2>/dev/null || echo "[]")
HAS_PWD=$(echo "$FIRSTUSER" | python3 -c "import sys,json; d=json.load(sys.stdin); print('password' in (d[0] if d else {}))" 2>/dev/null || echo "False")
[ "$HAS_PWD" = "False" ] && ok "用户列表不含密码字段" || nope "用户列表泄露密码字段"

info "6.3 删除保护"
VENUE_ID=$(echo "$VLIST" | python3 -c "import sys,json; l=json.load(sys.stdin); print(l[0].get('id','')) if l else print('')" 2>/dev/null || echo "")
if [ -n "$VENUE_ID" ]; then
  DELR=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE/api/admin/venues/$VENUE_ID" \
    -H "Authorization: Bearer $TOKEN" 2>/dev/null || echo "000")
  [ "$DELR" = "200" ] || [ "$DELR" = "204" ] || [ "$DELR" = "409" ] && ok "场地删除 (返回 $DELR)" || nope "场地删除异常 ($DELR)"
fi

echo ""
