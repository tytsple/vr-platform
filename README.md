# VR 管理平台

VR 大空间运营管理平台 — 管理租户、场地、应用、授权和会话。

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Java 17, Spring Boot 3.2, MyBatis, PostgreSQL 16 |
| 前端 | Vue 3, Element Plus, Pinia, Vite |
| 部署 | Docker Compose, Nginx 反向代理 |

## 快速部署（Ubuntu）

```bash
chmod +x *.sh
./deploy.sh
```

首次部署自动完成：Docker 安装、密钥生成、镜像构建、数据库初始化。

部署完成后访问 `http://<服务器IP>`，默认账号 `admin`，密码 `admin123`。

## 目录结构

```
vr-platform/
├── vr-admin/       Spring Boot 启动模块 + REST 控制器
├── vr-common/      共享库（AjaxResult, BaseController, 枚举）
├── vr-framework/   安全层（JWT, 权限, 审计, TenantContext）
├── vr-system/      RBAC（用户/角色/菜单 Mapper + Service）
├── vr-vr/          VR 领域（实体, Mapper, 引擎, WebSocket）
├── vr-quartz/      预留调度模块（空）
├── vr-ui/          Vue 3 前端
├── migrations/     PostgreSQL 迁移脚本
├── nginx/          反向代理配置
├── deploy.sh       一键部署脚本
├── backup.sh       数据库备份
├── restore.sh      数据库恢复
└── docker-compose.yml
```

## 角色

| 角色 | 权限 |
|---|---|
| admin | 全部：租户/场地/应用/授权/用户管理、统计 |
| tenant | 仅看自己租户：场地、授权、统计、会话 |
| operator | 实时监控：活跃会话列表 |

## 运维命令

```bash
./backup.sh                          # 备份数据库
./restore.sh backups/xxx.sql         # 恢复数据库

docker compose logs -f backend       # 查看后端日志
docker compose logs -f frontend      # 查看前端日志
docker compose restart backend       # 重启后端
docker compose up -d --remove-orphans # 更新部署（保留数据）
docker compose down                  # 停止所有服务（保留数据）
docker compose down -v               # ⚠️ 停止并删除数据库
```

## API 路由

| 前缀 | 角色 | 说明 |
|---|---|---|
| `/api/auth/**` | 公开 | 登录 |
| `/api/admin/**` | admin | 管理 CRUD |
| `/api/tenant/**` | tenant | 租户工作台 |
| `/api/operator/**` | operator | 运维监控 |
| `/ws` | 公开 | 场地控制器 WebSocket |
| `/health` | 公开 | 健康检查 |

## 环境变量

| 变量 | 默认值 | 说明 |
|---|---|---|
| `JWT_SECRET` | 自动生成 | JWT 签名密钥 |
| `DB_HOST` | postgres | 数据库主机 |
| `DB_USER` | vr_manager | 数据库用户 |
| `DB_PASS` | 自动生成 | 数据库密码 |
