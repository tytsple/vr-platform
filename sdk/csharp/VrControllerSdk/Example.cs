using VrControllerSdk;

// ============================================================
// VR 场地控制器 SDK — 使用示例
// ============================================================

var serverUrl = "http://192.168.1.108";
var token = "your-controller-token-here";  // 从管理后台场地管理 → 重置Token 获取

using var client = new VrControllerClient(serverUrl, token);

// 事件绑定
client.OnConnected += () => Console.WriteLine("[SDK] 已连接到平台");
client.OnDisconnected += (reason) => Console.WriteLine($"[SDK] 已断开: {reason}");
client.OnMessage += (msg) => Console.WriteLine($"[SDK] 收到消息: type={msg.Type}");
client.OnError += (ex) => Console.WriteLine($"[SDK] 错误: {ex.Message}");
client.OnStateChanged += (state) => Console.WriteLine($"[SDK] 状态: {state}");

// 连接
await client.ConnectAsync();

// 启动会话 — VR 应用启动时调用
await client.StartSessionAsync(appId: 1, version: "1.0.0");

// 模拟应用运行 30 秒
Console.WriteLine("[SDK] 应用运行中... (Ctrl+C 退出)");
var cts = new CancellationTokenSource();
Console.CancelKeyPress += (_, e) => { e.Cancel = true; cts.Cancel(); };
try { await Task.Delay(-1, cts.Token); } catch (OperationCanceledException) { }

// 结束会话 — VR 应用退出时调用
await client.EndSessionAsync();

// 断开
await client.DisconnectAsync();
