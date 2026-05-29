---
Status: ready-for-agent
---

## 问题陈述

VR 管理平台后端存在架构债务，导致代码不可测试且脆弱。服务层纯粹是委托调用，没有任何业务逻辑。所有登录用户都被硬编码赋予 admin 权限。租户数据隔离靠人工传递参数且存在 bug — 有的接口查出全量数据在内存里过滤，有的干脆忘了过滤。审计日志只往标准输出打印，不落库。WebSocket 引擎类都是具体类且字段注入，无法脱离运行中的服务器进行测试。配额消耗存在竞态条件 — 进程内锁无法保护数据库的读写-修改-写入周期。前端视图使用了大量后端不存在的字段名，页面数据几乎全部空白。

## 解决方案

十项协同的深化重构，在不改变外部 API 契约（URL 路径、HTTP 方法）和数据库 schema 的前提下，使代码库变得可测试、正确且 AI 可导航。

## 用户故事

1. 作为管理员，我希望登录后只获得 `sys_user_role` 中实际分配的角色，确保租户用户无法访问管理后台接口。
2. 作为租户用户，我希望会话统计在数据库查询层面就按租户过滤，确保无论平台上有多少租户，响应都正确且快速。
3. 作为租户用户，我希望会话列表接口只返回我所属场地的会话，确保看不到其他租户的数据。
4. 作为运维人员，我希望会话追踪和授权配额引擎可以独立测试，确保这些核心循环中的 bug 在部署前就能被发现。
5. 作为审计人员，我希望所有数据变更（增/改/删）和登录尝试都记录到 `sys_oper_log`，确保可以追溯谁在什么时间做了什么操作。
6. 作为开发者，我希望领域术语（租户、场地、应用、授权、会话、配额）在一个地方有明确定义，避免发明相互冲突的命名。
7. 作为开发者，我希望通过注入假 mapper 来测试控制器逻辑，而不是依赖完整的 Spring 容器加 PostgreSQL，确保测试反馈在亚秒级。
8. 作为开发者，我希望租户 ID 从单一来源（TenantContext）获取，避免猜测到底该用 JWT 声明、请求参数还是 PermissionService。
9. 作为管理员，我希望创建用户时同步分配角色和租户，确保新用户能正常登录和使用。
10. 作为开发者，我希望前端视图的表格列名与后端 JSON 字段名完全一致，避免页面空白但零报错。

## 实现决策

### 1. 修复硬编码 admin 角色（权限绕过）

登录接口通过两个新增的 mapper 方法（`selectRoleKeysByUserId`、`selectTenantIdByUserId`）查询 `sys_user_role` + `sys_role`，当用户未分配角色时返回 403 拒绝登录。JWT 携带 `roles` 列表声明而非单个 `role` 字符串。`JwtAuthenticationFilter` 为每个角色授予一个 `ROLE_` 权限。`PermissionService.hasRole()` 改为检查列表包含关系。

用户状态非 `'0'`（正常）时同样拒绝登录。`LoginUser` 中的 `String role` 改为 `List<String> roles`。`SystemController.getInfo()` 直接返回角色列表，前端登录页改用 `roles.includes()` 判断跳转目标。

### 2. 删除透传服务层

`vr-vr` 中所有 `I*Service`/`*ServiceImpl` 对（10 个文件，`com.vr.vr.service` 包）已删除。控制器和引擎类直接注入 mapper。`VenueServiceImpl.regenerateToken()` 中的 SecureRandom 令牌生成逻辑内联到 `VenueController`。`ApplicationMapper.insertVersion` 签名从两个 `@Param` 基础类型参数改为 `AppVersion` 对象，使 `@Options(useGeneratedKeys=true, keyProperty="id")` 能够正确工作。`vr-system` 中的 `SysUserService` 和 `SysMenuService` 保留（有密码编码和菜单树构建的真实逻辑）。

删除范围为 `vr-vr` 模块的 5 对接口+实现，不涉及 `vr-system` 模块。

### 3. TenantContext

新增 `TenantContext` 类，使用 `ThreadLocal` 持有 `userId`、`roles` 和 `tenantId`。由 `JwtAuthenticationFilter` 在每个认证请求中设置，在 filter 链执行后的 `finally` 块中清除。`TenantUserController` 改用 `TenantContext.getCurrentTenantId()` 替代 `PermissionService.getTenantId()`。

`TenantUserController` 中修复了两个 bug：
- `stats()` 调用 `SessionMapper.selectSessionStatsByTenantId(tenantId)` — 过滤逻辑下推到 SQL：`WHERE v.tenant_id = #{tenantId}`。
- `sessions()` 用租户实际的场地 ID 列表来过滤返回的会话，不再忽略已查询出的 `venueIds`。

### 4. 引擎接口

在 `com.vr.vr.engine` 包中新增三个接口：
- `MessageRouter` — `sendToVenue`、`onMessage`、`isConnected`、`getActiveVenues`
- `SessionLifecycle` — `startSession`、`endSession`、`closeStaleSessions`
- `LicenseValidator` — `checkLicense`、`consumeQuota`、`getQuotaRemaining`、`pushLicenseUpdate`

`VrWebSocketHandler` 实现 `MessageRouter`。`SessionTracker` 实现 `SessionLifecycle`（单构造函数，避免 Spring 注入歧义）。`LicenseEngine` 实现 `LicenseValidator`，依赖 `MessageRouter` 接口而非具体类 `VrWebSocketHandler`。`Orchestrator` 使用构造器注入，类型均为三个接口。

配额消耗改为原子操作：`LicenseMapper.incrementQuotaUsed` 执行 `UPDATE licenses SET quota_used = quota_used + 1 WHERE id = #{id} AND quota_limit > quota_used`。影响行数为 0 表示已被并发请求抢占了配额。

引擎和 WebSocket 包中的所有类均使用构造器注入，零 `@Autowired` 字段。`WebSocketConfig` 同步改为构造器注入。

### 5. 审计追踪

新增 `SysOperLogMapper` 将审计记录持久化到已有的 `sys_oper_log` 表。`LogAspect` 捕获：操作人名称、请求 URL、HTTP 方法、客户端 IP、方法签名、耗时（毫秒）、成功时的响应 JSON 或失败时的错误信息。`sys_oper_log` 表的 `business_type` 和 `operator_type` 列从 INT 改为 VARCHAR(50)，与 Java 枚举的 `.name()` 字符串值匹配。

5 个 CRUD 控制器中的 16 个变更接口均已带有 `@Log` 注解。`AuthController` 记录登录尝试（成功及全部失败原因），含用户名和 IP。

`vr-framework/pom.xml` 新增了对 `vr-system` 的依赖，使切面能够访问 mapper。

### 6. 前端数据模型对齐

所有 Vue 视图的 `el-table-column` prop 和 `row.*` 数据访问改为使用后端实际返回的字段名。领域类（Tenant、Venue、Application、AppVersion、License、Session、TenantStats）删除了 `@JsonProperty("snake_case")` 注解，统一使用 Java 字段名的 camelCase 作为 JSON 输出（与 Jackson 默认 `LOWER_CAMEL_CASE` 策略一致）。`Message` 类（WebSocket 协议）保留 `@JsonProperty` 注解。

`SysUser`、`SysRole`、`SysMenu`、`SysOperLog`（vr-system 模块）本身无 `@JsonProperty` 注解，无需修改。

### 7. 前端路由与响应拦截

路由守卫 `beforeEach` 检查 token 并重定向未登录用户到 `/login`。所有路由（admin 子页面、tenant、operator）统一注册到 `createRouter`，不再区分 `constantRoutes`/`asyncRoutes`，确保侧边栏菜单完整显示。

新增 `vr-ui/nginx.conf` 配置 SPA fallback（`try_files $uri $uri/ /index.html`），避免客户端路由刷新后 404。前端 Dockerfile 将该配置拷贝到容器。

响应拦截器区分 `AjaxResult`（含 `code` 字段）和原始数组/对象两种返回值，原始响应返回完整的 axios Response 对象，使调用方统一通过 `res.data` 访问数据。此改动修复了所有列表 API 和详情 API 因 `res.data` 为 `undefined` 而返回空数据的系统性 bug。

### 8. 用户管理增强

`UserController` 的创建/编辑接口改为接收 `Map<String, Object>` 格式，除基础字段外支持 `roleKey`（角色）和 `tenantId`（租户关联）。创建用户时同步写入 `sys_user_role` 和 `sys_user_tenant`，删除用户时先清理两张关联表。`SysUserMapper` 新增 `insertUserRole`、`deleteUserRoles`、`upsertUserTenant`、`deleteUserTenant` 方法。

`SysUserService.insertUser` 增加密码非空校验，`SysUserMapper.updateUser` 使用 MyBatis 动态 SQL（`<if test='password != null'>`）仅在传入新密码时更新 password 字段，防止编辑用户时误清空密码。`insertUser` 的 SQL 中 `status` 改用参数 `#{status}` 而非硬编码 `'0'`。

前端用户管理页面新增角色选择器和租户选择器，始终在提交负载中包含 `tenantId`（为 null 时清除关联）。

### 9. 依赖与构建修复

- `vr-common/pom.xml` 补充 `pagehelper-spring-boot-starter` 依赖（`BaseController` 引用该库）
- `vr-system/pom.xml` 补充 `spring-boot-starter-security`（`SysUserService` 使用 `PasswordEncoder`）
- `vr-framework/pom.xml` 新增 `vr-system` 依赖（`LogAspect` 使用 `SysOperLog`）
- 父 POM 中 `spring-boot-maven-plugin` 移到 `pluginManagement`，仅 `vr-admin` 激活
- `VRApplication` 新增 `@MapperScan("com.vr")` 确保 `vr-vr` 和 `vr-system` 中的 mapper 被 MyBatis 扫描
- `pagehelper` 版本从不存在的 `2.1.5` 修正为 `1.4.7`
- `Dockerfile` 中 JAR 拷贝使用精确前缀 `vr-admin-*.jar` 而非通配符 `*.jar`
- 新增 `maven-settings.xml`（阿里云 + 华为云 Maven 仓库回退策略）

### 10. 部署支持

- 补齐 `migrations/001_init.sql`（6 张 VR 领域表的 DDL）
- `sys_role.role_key` 增加 UNIQUE 约束（使 `ON CONFLICT DO NOTHING` 正确工作）
- `deploy.sh` 一键部署脚本：自动安装 Docker、配置镜像加速、生成 `.env`、构建并启动
- Docker 镜像加速使用实测可用的 `docker.m.daocloud.io` + `docker.1ms.run`
- 前端 Dockerfile 中 npm 使用 `registry.npmmirror.com` 镜像
- 新增 `.dockerignore` 和 `vr-ui/.dockerignore`
- `docker-compose.yml` 移除废弃的 `version` 字段

## 模块依赖关系

```
vr-common → vr-framework → vr-admin
           → vr-system    → vr-quartz（空模块）
           → vr-vr
```

`vr-framework → vr-system` 为新增依赖（审计日志），无循环依赖。

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
| `UserController` | 构造参数 | `SysUserService`、`SysUserMapper` |
| 任意 CRUD 控制器 | 注入依赖的字段/方法 | 对应的 `*Mapper` |

## 范围之外

- WebSocket 消息处理的异步分发（需要 ThreadPoolTaskExecutor、协调关闭机制和错误传播策略）
- 登录频率限制与 JWT 密钥轮换
- 动态菜单从后端加载（`getRouters` 已实现但前端未调用）
- `vr-quartz` 模块实现（当前为空模块）
- 数据库级别的多租户隔离（行级安全策略）
- 密码复杂度策略

## 补充说明

- `migrations/001_init.sql` 为本次补齐的缺失文件
- 部署脚本 `deploy.sh` 支持 Ubuntu 一键部署，包含 Docker 安装、镜像加速、自动构建
- `CONTEXT.md` 定义了完整的领域术语表，后续 AI 辅助开发技能均读取该文件
