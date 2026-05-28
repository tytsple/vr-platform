# VR 管理平台 — 领域术语表

## 核心实体

**租户（Tenant）**
VR 空间运营公司。拥有场地，持有应用的授权许可。
_注意：不要与 SaaS 中的多租户概念混淆 — 这里的租户是客户，不是基础设施分区。_

**场地（Venue）**
属于某个租户的物理 VR 运营点。通过唯一的控制器令牌（controller token）以 WebSocket 方式与后端鉴权连接。一个场地同时只有一个活跃的 WebSocket 连接。

**应用（Application）**
VR 软件产品，可授权给租户使用。拥有版本追踪（`AppVersion`）。

**授权（License）**
授予租户使用某个应用的权限。附带可选的配额：`quotaType`（配额类型）、`quotaLimit`（配额上限）、`quotaUsed`（已用配额）。配额消耗发生在会话结束时。授权由 `(租户, 应用)` 唯一确定。

**会话（Session）**
一次被追踪的 VR 使用事件 — 即某个场地运行某个应用。状态：`active`（进行中）、`normal`（正常结束）、`abnormal`（因超过 30 秒未收到 `session_end` 消息而被过期会话扫描标记为异常结束）。

## 角色

**admin / 超级管理员**
拥有全平台权限。管理租户、场地、应用、授权、用户，查看汇总统计数据。

**tenant / 租户用户**
仅能访问当前登录用户所属租户的数据。只能看到自己租户的场地、授权、统计和会话。

**operator / 运维人员**
实时监控。查看活跃会话列表和场地连接状态。

## 运行时概念

**控制器令牌（Controller Token）**
每个场地生成一个 64 位十六进制字符串（32 字节随机数）。由场地的现场控制器使用，在 WebSocket 连接时进行鉴权。可通过管理后台 API 重新生成。

**配额（Quota）**
租户使用某个已授权应用的次数上限。两种情况：设置了 `quotaType` → 受限（检查 `quotaUsed < quotaLimit`）；未设置或为空 → 不限（剩余量返回 -1）。

**过期会话（Stale Session）**
`startedAt` 超过过期阈值（默认 30 秒）且状态仍为 `active` 的会话。由定时任务（每 15 秒）自动关闭并标记为 `abnormal`。

**WebSocket 集线器（WebSocket Hub）**
服务端维护的已连接场地控制器注册表。维护 `venueId → WebSocket 会话` 的一对一映射。重连时替换旧连接。每 5 秒执行一次心跳超时检测（阈值 30 秒）。

**编排器（Orchestrator）**
将 WebSocket 消息类型（`session_start`、`session_end`）与会话生命周期引擎和授权配额引擎连接起来。执行定时过期会话扫描。

## 避免使用的术语

- **Service / 服务层** — `I*Service`/`*ServiceImpl` 层已被删除。数据访问请说 **mapper**；业务逻辑请指明具体的引擎（`SessionTracker`、`LicenseEngine`）。
- **User / 用户** — 请明确是指 **admin**（一种角色）还是 **LoginUser**（安全上下文中的认证主体）。`sys_user` 表存储所有登录账号，不论角色。
