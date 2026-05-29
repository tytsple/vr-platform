# VR 管理平台 — 领域术语表

## 核心实体

**租户（Tenant）**
VR 空间运营公司。拥有场地，持有应用的授权许可。对应数据库 `tenants` 表，字段：`id`、`name`、`contactInfo`、`createdAt`。
_注意：不要与 SaaS 中的多租户概念混淆 — 这里的租户是客户，不是基础设施分区。_

**场地（Venue）**
属于某个租户的物理 VR 运营点。通过唯一的控制器令牌（controllerToken）以 WebSocket 方式与后端鉴权连接。一个场地同时只有一个活跃的 WebSocket 连接。字段：`id`、`tenantId`、`name`、`address`、`controllerToken`、`createdAt`。

**应用（Application）**
VR 软件产品，可授权给租户使用。拥有版本追踪（AppVersion）。字段：`id`、`name`、`description`、`createdAt`。

**授权（License）**
授予租户使用某个应用的权限。附带可选的配额：`quotaType`（配额类型）、`quotaLimit`（配额上限）、`quotaUsed`（已用配额）。配额消耗发生在会话结束时。授权由 `(tenantId, applicationId)` 唯一确定。字段：`id`、`tenantId`、`applicationId`、`granted`、`quotaType`、`quotaLimit`、`quotaUsed`、`createdAt`。

**会话（Session）**
一次被追踪的 VR 使用事件 — 即某个场地运行某个应用。状态：`active`（进行中）、`normal`（正常结束）、`abnormal`（因超过 30 秒未收到 `session_end` 消息而被过期会话扫描标记为异常结束）。字段：`id`、`venueId`、`applicationId`、`version`、`startedAt`、`endedAt`、`status`、`createdAt`。

**用户（SysUser）**
登录账号，存储在 `sys_user` 表。通过 `sys_user_role` 关联角色，通过 `sys_user_tenant` 关联租户。字段：`userId`、`userName`、`nickName`、`password`、`status`（0=正常, 1=禁用）、`delFlag`（0=活跃, 2=已删除）、`createTime`。

## 角色

**admin / 超级管理员**
拥有全平台权限。管理租户、场地、应用、授权、用户，查看汇总统计数据。路由前缀 `/api/admin/**`。

**tenant / 租户用户**
仅能访问当前登录用户所属租户的数据。只能看到自己租户的场地、授权、统计和会话。路由前缀 `/api/tenant/**`。租户 ID 从 `sys_user_tenant` 获取，登录时写入 JWT 的 `tenant_id` 声明。

**operator / 运维人员**
实时监控。查看活跃会话列表和场地连接状态。路由前缀 `/api/operator/**`。

## 运行时概念

**控制器令牌（Controller Token）**
每个场地生成一个 64 位十六进制字符串（32 字节随机数）。由场地的现场控制器使用，在 WebSocket 连接时通过 `?token=` 查询参数进行鉴权。可通过 `POST /api/admin/venues/{id}/regenerate-token` 重新生成。

**配额（Quota）**
租户使用某个已授权应用的次数上限。两种情况：设置了 `quotaType` → 受限（检查 `quotaUsed < quotaLimit`）；未设置或为空 → 不限（`getQuotaRemaining` 返回 -1）。配额消耗在会话结束时执行原子 SQL 更新：`UPDATE licenses SET quota_used = quota_used + 1 WHERE id = ? AND quota_limit > quota_used`。

**过期会话（Stale Session）**
`startedAt` 超过过期阈值（默认 30 秒）且状态仍为 `active` 的会话。由 Orchestrator 的定时任务（每 15 秒）自动关闭并标记为 `abnormal`。

**WebSocket 集线器（VrWebSocketHandler）**
服务端维护的已连接场地控制器注册表。维护 `venueId → WebSocket 会话` 的一对一映射。重连时替换旧连接。每 5 秒执行一次心跳超时检测（30 秒阈值）。同时实现了 `MessageRouter` 接口，将 WebSocket 消息分发与连接管理解耦。

**编排器（Orchestrator）**
将 WebSocket 消息类型（`session_start`、`session_end`）与 `SessionLifecycle` 和 `LicenseValidator` 引擎连接起来。通过 `MessageRouter.onMessage()` 注册消息处理器。执行定时过期会话扫描。依赖均为构造器注入的接口类型。

**租户上下文（TenantContext）**
`ThreadLocal` 持有当前请求的 `userId`、`roles`、`tenantId`。由 `JwtAuthenticationFilter` 在每个认证请求中设置，filter 链执行后的 `finally` 块中清除。控制器通过 `TenantContext.getCurrentTenantId()` 获取租户 ID，替代手动调用 `PermissionService.getTenantId()`。

## 引擎接口

**MessageRouter** — 消息路由接口。方法：`sendToVenue(venueId, message)`、`onMessage(type, handler)`、`isConnected(venueId)`、`getActiveVenues()`。`VrWebSocketHandler` 为其唯一生产适配器。

**SessionLifecycle** — 会话生命周期管理接口。方法：`startSession(venueId, appId, version)`、`endSession(sessionId)`、`closeStaleSessions(venueId)`。`SessionTracker` 为其唯一生产适配器。

**LicenseValidator** — 授权校验和配额消耗接口。方法：`checkLicense(tenantId, appId)`、`consumeQuota(venueId, appId)`、`getQuotaRemaining(tenantId, appId)`、`pushLicenseUpdate(tenantId, appId, granted)`。`LicenseEngine` 为其唯一生产适配器。

## JSON 命名约定

所有 REST API 的 JSON 字段使用 **camelCase**（与 Java 字段名一致）。领域类已移除 `@JsonProperty` 注解，Jackson 默认策略 `LOWER_CAMEL_CASE` 保证输出与 Java 字段同名。

WebSocket 消息（`Message` 类）仍保留 `@JsonProperty("snake_case")` 注解，因为场地控制器发送的消息遵循原始 Go 端的 snake_case 协议，不应修改。

## 避免使用的术语

- **Service / 服务层** — `I*Service`/`*ServiceImpl` 层已被删除。数据访问请说 **mapper**；业务逻辑请指明具体的引擎（`SessionTracker`、`LicenseEngine`）。
- **User / 用户** — 请明确是指 **admin**（一种角色）、**LoginUser**（安全上下文中的认证主体）还是 **SysUser**（数据库实体）。`sys_user` 表存储所有登录账号，不论角色。
- **role（单数）** — JWT 和 `LoginUser` 中使用复数 `roles`（列表）。单个用户可同时持有多个角色。
