#!/usr/bin/env bash
set -euo pipefail

# ============================================================
# VR 管理平台 — 一键部署脚本 (Ubuntu)
# ============================================================

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_DIR"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC}  $*"; }
err()  { echo -e "${RED}[ERROR]${NC} $*"; }

# ---- 1. 检查 Docker 环境 ----
log "检查 Docker 环境..."

if ! command -v docker &>/dev/null; then
    warn "Docker 未安装，正在安装..."
    sudo apt-get update -qq
    sudo apt-get install -y -qq ca-certificates curl
    sudo install -m 0755 -d /etc/apt/keyrings
    sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
    sudo chmod a+r /etc/apt/keyrings/docker.asc
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
    sudo apt-get update -qq
    sudo apt-get install -y -qq docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
    sudo systemctl enable docker --now
    sudo usermod -aG docker "$USER"
    log "Docker 安装完成。如果这是首次安装，请退出 SSH 重新登录以使 docker 组权限生效。"
fi

if ! docker compose version &>/dev/null; then
    err "需要 Docker Compose v2 (docker compose 命令)，请升级 Docker。"
    exit 1
fi

# ---- 2. 配置 Docker 镜像加速 ----
DOCKER_CONFIG="/etc/docker/daemon.json"
if [ ! -f "$DOCKER_CONFIG" ]; then
    log "配置 Docker 镜像加速（国内源）..."
    sudo mkdir -p /etc/docker
    sudo tee "$DOCKER_CONFIG" > /dev/null <<'JSON'
{
  "registry-mirrors": [
    "https://docker.m.daocloud.io",
    "https://hub-mirror.c.163.com",
    "https://mirror.baidubce.com"
  ],
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  }
}
JSON
    sudo systemctl restart docker
    log "Docker 镜像加速已配置"
else
    log "已存在 $DOCKER_CONFIG，跳过镜像加速配置"
fi

# ---- 3. 生成 .env ----
if [ ! -f .env ]; then
    JWT_SECRET="$(openssl rand -hex 32 2>/dev/null || cat /dev/urandom | tr -dc 'a-zA-Z0-9' | head -c 64)"
    cat > .env <<EOF
# VR 管理平台 — 自动生成
JWT_SECRET=${JWT_SECRET}
DB_PASS=$(openssl rand -hex 16 2>/dev/null || cat /dev/urandom | tr -dc 'a-zA-Z0-9' | head -c 32)
EOF
    log "已生成 .env 文件（JWT_SECRET + DB_PASS 随机生成）"
else
    log "已存在 .env 文件，跳过生成"
fi

source .env

# ---- 4. 检查迁移脚本 ----
if [ ! -f migrations/001_init.sql ]; then
    err "缺少 migrations/001_init.sql，部署将失败。"
    exit 1
fi

# ---- 5. 构建并启动 ----
log "开始构建 Docker 镜像（首次构建约需 5-10 分钟）..."

docker compose build --pull

log "启动所有服务..."
docker compose up -d

# ---- 6. 等待健康检查 ----
log "等待服务就绪..."

MAX_WAIT=120
ELAPSED=0
while [ $ELAPSED -lt $MAX_WAIT ]; do
    HEALTHY=$(docker compose ps --format json 2>/dev/null | grep -c '"Health":"healthy"' || true)
    RUNNING=$(docker compose ps --format json 2>/dev/null | grep -c '"State":"running"' || true)
    if [ "$HEALTHY" -ge 1 ] && [ "$RUNNING" -ge 3 ]; then
        log "所有服务已就绪 (${ELAPSED}s)"
        break
    fi
    sleep 5
    ELAPSED=$((ELAPSED + 5))
done

if [ $ELAPSED -ge $MAX_WAIT ]; then
    warn "部分服务可能未完全就绪，请检查日志: docker compose logs"
fi

# ---- 7. 首次数据库初始化 ----
log "等待数据库初始化完成..."
sleep 3
docker compose exec -T postgres psql -U vr_manager -d vr_manager -c "SELECT COUNT(*) AS table_count FROM information_schema.tables WHERE table_schema='public';" 2>/dev/null || true

# ---- 8. 汇总 ----
SERVER_IP=$(hostname -I 2>/dev/null | awk '{print $1}' || echo "localhost")

echo ""
echo "============================================"
echo "  VR 管理平台部署完成"
echo "============================================"
echo ""
echo "  访问地址:   http://${SERVER_IP}"
echo "  默认账号:   admin"
echo "  默认密码:   admin123"
echo ""
echo "  服务状态:"
docker compose ps --format "table {{.Name}}\t{{.State}}\t{{.Status}}" 2>/dev/null
echo ""
echo "  常用命令:"
echo "    查看日志:    docker compose logs -f [服务名]"
echo "    重启服务:    docker compose restart [服务名]"
echo "    停止所有:    docker compose down"
echo "    完全重置:    docker compose down -v && rm -f .env"
echo "============================================"
