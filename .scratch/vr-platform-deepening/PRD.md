---
Status: ready-for-agent
---

## 问题陈述

VR 管理平台后端存在架构债务，导致代码不可测试且脆弱。服务层纯粹是委托调用，没有任何业务逻辑。所有登录用户都被硬编码赋予 admin 权限。租户数据隔离靠人工传递参数且存在 bug — 有的接口查出全量数据在内存里过滤，有的干脆忘了过滤。审计日志只往标准输出打印，不落库。WebSocket 引擎类都是具体类且字段注入，无法脱离运行中的服务器进行测试。配额消耗存在竞态条件 — 进程内锁无法保护数据库的读写-修改-写入周期。

## 解决方案

五个协同的深化重构，在不改变任何对外 API 契约和数据库 schema 的前提下，使代码库变得可测试、正确且 AI 可导航。

## 用户故事

1. 作为管理员，我希望登录后只获得 `sys_user_role` 中实际分配的角色，确保租户用户无法访问管理后台接口。
2. 作为租户用户，我希望会话统计在数据库查询层面就按租户过滤，确保无论平台上有多少租户，响应都正确且快速。
3. 作为租户用户，我希望会话列表接口只返回我所属场地的会话，确保看不到其他租户的数据。
4. 作为运维人员，我希望会话追踪和授权配额引擎可以独立测试，确保这些核心循环中的 bug 在部署前就能被发现。
5. 作为审计人员，我希望所有数据变更（增/改/删）和登录尝试都记录到 `sys_oper_log`，确保可以追溯谁在什么时间做了什么操作。
6. 作为开发者，我希望领域术语（租户、场地、应用、授权、会话、配额）在一个地方有明确定义，避免发明相互冲突的命名。
7. 作为开发者，我希望通过注入假 mapper 来测试控制器逻辑，而不是依赖完整的 Spring 容器加 PostgreSQL，确保测试反馈在亚秒级。
8. 作为开发者，我希望租户 ID 从单一来源（TenantContext）获取，避免猜测到底该用 JWT 声明、请求参数还是 PermissionService。

## 实现决策

### 1. 修复硬编码 admin 角色（权限绕过）

登录接口现在通过两个新增的 mapper 方法（`selectRoleKeysByUserId`、`selectTenantIdByUserId`）查询 `sys_user_role` + `sys_role`，当用户未分配角色时返回 403 拒绝登录。JWT 携带 `roles` 列表声明而非单个 `role` 字符串。`JwtAuthenticationFilter` 为每个角色授予一个 `ROLE_` 权限。`PermissionService.hasRole()` 改为检查列表包含关系。

用户状态非活跃（`'0'`）时同样拒绝登录。

### 2. 删除透传服务层

`vr-vr` 中所有 `I*Service`/`*ServiceImpl` 对（10 个文件，`com.vr.vr.service` 包）已删除。控制器和引擎类直接注入 mapper。原本在 `VenueServiceImpl.regenerateToken()` 中的非平凡逻辑（SecureRandom 十六进制生成）被内联到 `VenueController` 中。`ApplicationMapper.insertVersion` 的签名从两个 `@Param` 基础类型参数改为 `AppVersion` 对象，使 `@Options(useGeneratedKeys=true, keyProperty="id")` 能够正确工作。

### 3. TenantContext

新增 `TenantContext` 类，使用 `ThreadLocal` 持有 `userId`、`roles` 和 `tenantId`。由 `JwtAuthenticationFilter` 在每个认证请求中设置，在 filter 链执行后的 `finally` 块中清除。`TenantUserController` 改用 `TenantContext.getCurrentTenantId()` 替代 `PermissionService.getTenantId()`。

`TenantUserController` 中修复了两个 bug：
- `stats()` 现在调用 `SessionMapper.selectSessionStatsByTenantId(tenantId)` — 过滤逻辑下推到 SQL：`WHERE v.tenant_id = #{tenantId}`。
- `sessions()` 现在用租户实际的场地 ID 列表来过滤返回的会话，不再忽略已查询出的 `venueIds`。

### 4. 引擎接口

在 `com.vr.vr.engine` 包中新增三个接口：
- `MessageRouter` — `sendToVenue`、`onMessage`、`isConnected`、`getActiveVenues`
- `SessionLifecycle` — `startSession`、`endSession`、`closeStaleSessions`
- `LicenseValidator` — `checkLicense`、`consumeQuota`、`getQuotaRemaining`、`pushLicenseUpdate`

`VrWebSocketHandler` 实现 `MessageRouter`。`SessionTracker` 实现 `SessionLifecycle`，`staleTimeoutSeconds` 改为可配置的构造参数（默认 30 秒）。`LicenseEngine` 实现 `LicenseValidator`，依赖 `MessageRouter` 接口而非具体类 `VrWebSocketHandler`。`Orchestrator` 使用构造器注入，类型均为三个接口。

配额消耗改为原子操作：`LicenseMapper.incrementQuotaUsed` 执行 `UPDATE licenses SET quota_used = quota_used + 1 WHERE id = #{id} AND quota_limit > quota_used`。如果影响行数为 0，说明被并发请求抢占了配额。

引擎和 WebSocket 包中的所有类均使用构造器注入，无任何 `@Autowired` 字段残留。

### 5. 审计追踪

新增 `SysOperLogMapper` 将审计记录持久化到已有的 `sys_oper_log` 表。`LogAspect` 捕获：操作人名称、请求 URL、HTTP 方法、客户端 IP、方法签名、耗时（毫秒）、成功时的响应 JSON 或失败时的错误信息。5 个 CRUD 控制器中的 16 个变更接口均已带有 `@Log` 注解。`AuthController` 记录登录尝试（成功及全部 3 种失败原因），含用户名和 IP。

`vr-framework/pom.xml` 新增了对 `vr-system` 的依赖，使切面能够访问 mapper。

### 模块依赖关系（未改变）

```
vr-common → vr-framework → vr-admin
                         → vr-system（新增：framework → system 用于审计）
                         → vr-vr
                         → vr-quartz
```

### CONTEXT.md

项目根目录新增 `CONTEXT.md`，定义了领域术语表：租户、场地、应用、授权、会话、配额、控制器令牌、过期会话、WebSocket 集线器、编排器 — 以及三个角色（admin、tenant、operator）。

## 测试决策

### 何为好的测试

测试应与生产代码调用方走同一个接缝（seam）。每个测试通过注入接口背后的假实现（fake）来断言可观测行为，而非内部状态。任何测试都不应依赖运行中的数据库、WebSocket 服务器或 Spring 应用容器。

### 现可测试的模块

| 模块 | 接缝 | 需要 fake 的依赖 |
|---|---|---|
| `AuthController` | 注入依赖的方法调用 | `SysUserService`、`SysOperLogMapper`（`PasswordEncoder` 可用真实实现或 fake） |
| `Orchestrator` | 构造参数 | `MessageRouter`、`SessionLifecycle`、`LicenseValidator` |
| `SessionTracker` | 构造参数 | `SessionMapper` |
| `LicenseEngine` | 构造参数 | `LicenseMapper`、`VenueMapper`、`MessageRouter` |
| `VrWebSocketHandler` | 构造参数 | `VenueMapper` |
| 任意 CRUD 控制器 | 注入依赖的字段/方法 | 对应的 `*Mapper` |

### 已有参考

代码库中暂无已有测试。上述接缝是首个测试套件的搭建基础。

## 范围之外

- WebSocket 消息处理的异步分发（需要 ThreadPoolTaskExecutor、协调关闭机制和错误传播策略 — 值得作为独立的专项改动）
- 登录频率限制
- JWT 密钥轮换或密钥 ID
- JWT 使用 httpOnly cookie 存储（需要前端配合改动）
- 数据库级别的多租户隔离（行级安全策略）
- 密码策略（复杂度、过期）

## 补充说明

`migrations/001_init.sql` 文件也在本次工作中补齐 — 该文件此前在仓库中缺失，包含了后续迁移脚本引用的 6 张 VR 领域表的 DDL。
