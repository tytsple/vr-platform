# VR 管理平台 — 运维手册

## 环境要求

| 依赖 | 版本 |
|---|---|
| 操作系统 | Ubuntu 20.04+ |
| Docker | 24+ (含 Docker Compose v2) |
| Java | 17 (仅开发机) |
| Maven | 3.9+ (仅开发机) |
| Node.js | 20 (仅开发机) |

---

## 一、首次部署

```bash
# 1. 克隆项目
git clone https://github.com/tytsple/vr-platform.git
cd vr-platform

# 2. 赋予脚本执行权限
chmod +x *.sh

# 3. 一键部署
./deploy.sh
```

部署完成后输出：
```
访问地址:   http://192.168.x.x
默认账号:   admin / admin123
```

---

## 二、日常更新

```bash
git pull
docker compose up -d --build
```

数据库数据保存在 `./data/postgres/` 目录下，更新不会丢失数据。

### 重要原则：

| 操作 | 正确命令 | 错误命令 |
|---|---|---|
| 更新代码 | `git pull && docker compose up -d --build` | — |
| 停止服务 | `docker compose down` | — |
| 重启服务 | `docker compose restart` | — |
| 查看日志 | `docker compose logs -f backend` | — |
| 删除数据 | `sudo rm -rf ./data/postgres` | `docker compose down -v` (无效，数据在宿主机) |

---

## 三、测试

### 测试金字塔

```
       ┌─────────┐
       │ Smoke   │  ← 每次部署后跑 (30秒)
       │ 27 用例  │
       ├─────────┤
       │ 单元测试 │  ← 每次 git push 前跑 (10秒)
       │   5 例   │
       └─────────┘
```

### 3.1 Smoke 测试（必须先部署）

```bash
# 本地
./smoke-test.sh http://192.168.1.108

# 远程服务器
./smoke-test.sh http://localhost
```

通过的标志：`结果: 27 通过 / 0 失败`

### 3.2 单元测试（不需要部署，开发机本地跑）

```bash
# 全部测试
mvn test -pl vr-admin

# 单个测试类
mvn test -pl vr-admin -Dtest=AuthControllerTest

# 单个测试方法
mvn test -pl vr-admin -Dtest=AuthControllerTest#loginSuccess
```

---

## 四、版本管理

### 4.1 发布新版本

```bash
# 确认所有测试通过
mvn test -pl vr-admin

# 打版本标签
./release.sh v1.0.0 "修复了权限问题"

# 推送到远程
git push
```

### 4.2 回退版本

```bash
# 列出所有版本
./rollback.sh

# 回退到指定版本（自动备份数据库）
./rollback.sh v1.0.0

# 返回最新版本
git checkout master && docker compose up -d --build
```

---

## 五、数据管理

### 5.1 备份

```bash
./backup.sh
# 输出: 备份完成: ./backups/vr_manager_20260601_120000.sql
```

自动保留最近 7 份，旧的自动删除。

### 5.2 恢复

```bash
./restore.sh
# 列出可用备份，选择要恢复的文件

./restore.sh backups/vr_manager_20260601_120000.sql
```

### 5.3 数据目录

```
./data/postgres/    ← PostgreSQL 数据文件（宿主机目录，不受 docker 删除影响）
./backups/          ← 数据库备份文件
./migrations/       ← 数据库迁移脚本（首次启动时自动执行）
```

---

## 六、故障排查

### 6.1 服务不响应

```bash
# 检查所有容器状态
docker compose ps

# 查看后端日志
docker compose logs backend --tail 50

# 检查端口占用
sudo netstat -tlnp | grep :80
```

### 6.2 数据库连接失败

```bash
# 检查权限
ls -la ./data/postgres

# 修复权限
sudo chown 70:70 ./data/postgres

# 或重新运行部署（自动修复）
./deploy.sh
```

### 6.3 登录失败

```bash
# 检查 .env 是否存在
cat .env | grep JWT_SECRET

# 不存在则重新部署（自动生成）
rm -f .env && ./deploy.sh
```

### 6.4 查看审计日志

```bash
docker compose exec postgres psql -U vr_manager -d vr_manager \
  -c "SELECT oper_name, title, oper_url, oper_ip, oper_time FROM sys_oper_log ORDER BY oper_time DESC LIMIT 20;"
```

---

## 七、账号管理

### 7.1 默认账号

| 用户名 | 密码 | 角色 |
|---|---|---|
| `admin` | admin123 | 超级管理员 |
| `tenant` | admin123 | 租户用户（需先部署种子数据） |
| `operator` | admin123 | 运维人员（需先部署种子数据） |

### 7.2 首次登录后修改密码

右上角 → 修改密码 → 输入原密码和新密码

### 7.3 创建新用户

管理员登录 → 用户管理 → 新增 → 选择角色 → **租户用户必须选择所属租户**

---

## 八、常用命令速查

```bash
# 部署
./deploy.sh

# 测试
./smoke-test.sh http://192.168.1.108

# 备份
./backup.sh

# 恢复
./restore.sh backups/xxx.sql

# 日志
docker compose logs -f backend       # 后端实时日志
docker compose logs -f frontend      # 前端实时日志
docker compose logs -f postgres      # 数据库日志

# 重启
docker compose restart backend       # 重启后端
docker compose restart               # 重启全部

# 完全重建（保留数据）
docker compose down
docker compose up -d --build

# 完全重建（清空数据）
docker compose down
sudo rm -rf ./data/postgres
docker compose up -d

# 查看容器资源
docker stats

# 进入数据库
docker compose exec postgres psql -U vr_manager -d vr_manager
```
