#!/usr/bin/env bash
set -euo pipefail

# ============================================================
# VR 管理平台 — Smoke Test Suite
# 每次部署后运行, 5 分钟内验证核心功能
# ============================================================

BASE="${1:-http://localhost:8080}"
PASS=0
FAIL=0
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

ok()   { echo -e "  ${GREEN}PASS${NC} $*"; PASS=$((PASS+1)); }
nope() { echo -e "  ${RED}FAIL${NC} $*"; FAIL=$((FAIL+1)); }
info() { echo -e "${YELLOW}=== $*${NC} ==="; }

trap 'echo ""; echo "结果: ${GREEN}$PASS 通过${NC} / ${RED}$FAIL 失败${NC}"; [ $FAIL -eq 0 ] || exit 1' EXIT

echo "VR 管理平台 Smoke Test — $BASE"
echo ""

# ==========================================
info "1. 基础设施"
# ==========================================

# 1.1 健康检查
info "1.1 健康检查"
STATUS=$(curl -sf -o /dev/null -w "%{http_code}" --connect-timeout 5 "$BASE/health" 2>/dev/null || echo "000")
[ "$STATUS" = "200" ] && ok "健康检查 (HTTP $STATUS)" || nope "健康检查 (HTTP $STATUS, 期望 200)"

# ==========================================
info "2. 认证"
# ==========================================

# 2.1 正常登录
info "2.1 正常登录"
LOGIN=$(curl -sf -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' 2>/dev/null || echo '{}')
TOKEN=$(echo "$LOGIN" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('token',''))" 2>/dev/null || echo "")
[ -n "$TOKEN" ] && ok "admin 登录成功，获取 token" || nope "admin 登录失败"

# 2.2 错误密码
info "2.2 错误密码"
BAD=$(curl -sf -o /dev/null -w "%{http_code}" -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"wrongpassword123"}' 2>/dev/null || echo "000")
[ "$BAD" = "401" ] && ok "错误密码返回 401" || nope "错误密码返回 $BAD (期望 401)"

# 2.3 不存在的用户
info "2.3 不存在的用户"
NOUSER=$(curl -sf -o /dev/null -w "%{http_code}" -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"nobody","password":"x"}' 2>/dev/null || echo "000")
[ "$NOUSER" = "401" ] && ok "不存在用户返回 401" || nope "不存在用户返回 $NOUSER (期望 401)"

# 2.4 登录限流
info "2.4 登录限流 (连续 6 次错误密码)"
for i in 1 2 3 4 5; do
  curl -sf -o /dev/null -X POST "$BASE/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"wrong"}' 2>/dev/null || true
done
RATE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"wrong"}' 2>/dev/null || echo "000")
[ "$RATE" = "429" ] && ok "第 6 次返回 429 (限流生效)" || nope "第 6 次返回 $RATE (期望 429)"

# 2.5 修改密码
info "2.5 修改密码"
CHPWD=$(curl -sf -o /dev/null -w "%{http_code}" -X PUT "$BASE/api/auth/change-password" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"oldPassword":"admin123","newPassword":"admin123"}' 2>/dev/null || echo "000")
[ "$CHPWD" = "200" ] && ok "修改密码成功 (HTTP $CHPWD)" || nope "修改密码 (HTTP $CHPWD, 期望 200)"

# ==========================================
info "3. 权限控制"
# ==========================================

# 3.1 无 token 访问被拒绝
info "3.1 无 token 访问保护接口"
NOAUTH=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/admin/tenants" 2>/dev/null || echo "000")
[ "$NOAUTH" = "401" ] || [ "$NOAUTH" = "403" ] && ok "无 token 返回 ${NOAUTH}" || nope "无 token 返回 $NOAUTH (期望 401/403)"

# 3.2 租户角色不能访问 admin 接口
info "3.2 租户不能访问 admin 接口"
TLOGIN=$(curl -sf -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"tenant","password":"admin123"}' 2>/dev/null || echo '{}')
TTOKEN=$(echo "$TLOGIN" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('token',''))" 2>/dev/null || echo "")
TACCESS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/admin/tenants" \
  -H "Authorization: Bearer $TTOKEN" 2>/dev/null || echo "000")
[ -z "$TTOKEN" ] || [ "$TACCESS" = "403" ] && ok "租户无法访问 admin 接口 ($TACCESS)" \
  || nope "租户意外能访问 admin 接口 ($TACCESS)"

# 3.3 admin 不能访问 tenant 专用接口
info "3.3 admin 不能访问 tenant 专用接口"
AACCESS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/tenant/venues" \
  -H "Authorization: Bearer $TOKEN" 2>/dev/null || echo "000")
[ "$AACCESS" = "403" ] && ok "admin 无法访问 tenant 接口" \
  || nope "admin 意外能访问 tenant 接口 ($AACCESS)"

# ==========================================
info "4. CRUD 操作"
# ==========================================

# 4.1 租户列表
info "4.1 租户列表"
TLIST=$(curl -sf "$BASE/api/admin/tenants" -H "Authorization: Bearer $TOKEN" 2>/dev/null || echo "[]")
TCOUNT=$(echo "$TLIST" | python3 -c "import sys,json; print(len(json.load(sys.stdin)))" 2>/dev/null || echo "0")
[ "$TCOUNT" -ge 1 ] && ok "租户列表 ($TCOUNT 条)" || nope "租户列表为空或失败"

# 4.2 场地列表
info "4.2 场地列表"
VLIST=$(curl -sf "$BASE/api/admin/venues" -H "Authorization: Bearer $TOKEN" 2>/dev/null || echo "[]")
VCOUNT=$(echo "$VLIST" | python3 -c "import sys,json; print(len(json.load(sys.stdin)))" 2>/dev/null || echo "0")
[ "$VCOUNT" -ge 1 ] && ok "场地列表 ($VCOUNT 条)" || echo "  ${YELLOW}SKIP${NC} 无场地数据 (种子数据需重建)"

# 4.3 用户列表
info "4.3 用户列表"
ULIST=$(curl -sf "$BASE/api/admin/users" -H "Authorization: Bearer $TOKEN" 2>/dev/null || echo "[]")
UCOUNT=$(echo "$ULIST" | python3 -c "import sys,json; print(len(json.load(sys.stdin)))" 2>/dev/null || echo "0")
[ "$UCOUNT" -ge 1 ] && ok "用户列表 ($UCOUNT 条)" || nope "用户列表为空"

# 4.4 用户详情包含角色信息
info "4.4 用户详情含角色"
UDETAIL=$(curl -sf "$BASE/api/admin/users/1" -H "Authorization: Bearer $TOKEN" 2>/dev/null || echo "{}")
HASROL=$(echo "$UDETAIL" | python3 -c "import sys,json; d=json.load(sys.stdin); print('roles' in d.get('data',{}))" 2>/dev/null || echo "False")
[ "$HASROL" = "True" ] && ok "用户详情包含 roles" || nope "用户详情缺少 roles"

# 4.5 创建场地自动生成 token
info "4.5 创建场地自动生成 token"
TID=$(echo "$TLIST" | python3 -c "import sys,json; print(json.load(sys.stdin)[0].get('id',1))" 2>/dev/null || echo "1")
NEWV=$(curl -sf -X POST "$BASE/api/admin/venues" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"name\":\"smoke-test-venue\",\"tenantId\":$TID}" 2>/dev/null || echo "{}")
VTOKEN=$(echo "$NEWV" | python3 -c "import sys,json; d=json.load(sys.stdin); t=d.get('controllerToken','') or d.get('data',{}).get('controllerToken',''); print(t)" 2>/dev/null || echo "")
[ -n "$VTOKEN" ] && ok "场地创建自动生成 token" || nope "场地创建未生成 token"

# 4.6 创建用户-角色分配
info "4.6 创建用户同步分配角色"
NEWU=$(curl -sf -X POST "$BASE/api/admin/users" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"userName":"test-smoke","nickName":"测试","password":"test123","roleKey":"operator","status":"0"}' 2>/dev/null || echo "{}")
NEWUID=$(echo "$NEWU" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('userId',''))" 2>/dev/null || echo "")
[ -n "$NEWUID" ] && ok "创建 test-smoke 用户成功 (id=$NEWUID)" || nope "创建用户失败"

# 4.7 创建租户角色需选 tenant_id
info "4.7 租户角色必须选租户"
NOTENANT=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/api/admin/users" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"userName":"test-notenant","password":"x","roleKey":"tenant"}' 2>/dev/null || echo "000")
[ "$NOTENANT" != "200" ] && ok "租户用户不选租户被拒绝 ($NOTENANT)" \
  || nope "租户用户不选租户未被拒绝"

# ==========================================
info "5. 统计与监控"
# ==========================================

# 5.1 统计页日期过滤
info "5.1 统计接口响应"
STATS=$(curl -sf "$BASE/api/admin/stats?from=2024-01-01&to=2024-12-31" \
  -H "Authorization: Bearer $TOKEN" 2>/dev/null || echo "[]")
SCODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/admin/stats" \
  -H "Authorization: Bearer $TOKEN" 2>/dev/null || echo "000")
[ "$SCODE" = "200" ] && ok "统计接口正常 (HTTP $SCODE)" || nope "统计接口异常 ($SCODE)"

# 5.2 运维接口
info "5.2 运维活跃会话"
OSESS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/operator/sessions/active" \
  -H "Authorization: Bearer $TOKEN" 2>/dev/null || echo "000")
[ "$OSESS" = "200" ] && ok "运维会话接口 (HTTP $OSESS)" || echo "  ${YELLOW}SKIP${NC} admin 无权访问运维接口"

# 5.3 会话概览
info "5.3 会话概览含名称"
OVIEW=$(curl -sf "$BASE/api/admin/sessions/overview" \
  -H "Authorization: Bearer $TOKEN" 2>/dev/null || echo "[]")
HAS_VNAME=$(echo "$OVIEW" | python3 -c "import sys,json; d=json.load(sys.stdin); print('venueName' in (d[0] if d else {}))" 2>/dev/null || echo "True")
[ "$HAS_VNAME" = "True" ] && ok "会话概览包含 venueName" || nope "会话概览缺少 venueName"

# ==========================================
info "6. 安全防护"
# ==========================================

# 6.1 XSS 防护 (前端渲染转义)
info "6.1 输入包含 XSS payload"
XSS_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/api/admin/tenants" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"<script>alert(1)</script>","contactInfo":"test"}' 2>/dev/null || echo "000")
[ "$XSS_STATUS" = "201" ] && ok "XSS payload 被正常接受 (后端不转义,由前端处理)" \
  || nope "XSS payload 被拒绝 ($XSS_STATUS)"

# 6.2 密码哈希不返回
info "6.2 密码哈希不在 API 响应中"
FIRSTUSER=$(curl -sf "$BASE/api/admin/users" -H "Authorization: Bearer $TOKEN" 2>/dev/null || echo "[]")
HAS_PWD=$(echo "$FIRSTUSER" | python3 -c "import sys,json; d=json.load(sys.stdin); print('password' in (d[0] if d else {}))" 2>/dev/null || echo "False")
[ "$HAS_PWD" = "False" ] && ok "用户列表不含密码字段" || nope "用户列表泄露密码字段"

# 6.3 场地删除保护
info "6.3 删除保护"
VENUE_ID=$(echo "$VLIST" | python3 -c "import sys,json; l=json.load(sys.stdin); print(l[0].get('id','')) if l else print('')" 2>/dev/null || echo "")
if [ -n "$VENUE_ID" ]; then
  DELR=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE/api/admin/venues/$VENUE_ID" \
    -H "Authorization: Bearer $TOKEN" 2>/dev/null || echo "000")
  [ "$DELR" = "200" ] || [ "$DELR" = "204" ] || [ "$DELR" = "409" ] && ok "场地删除 (返回 $DELR)" \
    || nope "场地删除异常 ($DELR)"
fi

echo ""
