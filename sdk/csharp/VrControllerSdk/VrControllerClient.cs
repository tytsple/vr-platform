using System.Net.WebSockets;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace VrControllerSdk;

/// <summary>
/// VR 场地控制器 SDK — 与 VR 管理平台通信。
/// </summary>
public class VrControllerClient : IDisposable
{
    private readonly string _serverUrl;
    private readonly string _token;
    private readonly HttpClient _http;
    private ClientWebSocket? _ws;
    private CancellationTokenSource? _cts;
    private long _sessionId;
    private bool _autoReconnect = true;

    private static readonly TimeSpan HeartbeatInterval = TimeSpan.FromSeconds(10);
    private static readonly TimeSpan ReconnectDelay = TimeSpan.FromSeconds(5);
    private static readonly int MaxReconnectAttempts = 10;

    // ==================== 事件 ====================

    /// <summary>连接已建立并鉴权成功。</summary>
    public event Action? OnConnected;

    /// <summary>连接断开。</summary>
    public event Action<string>? OnDisconnected;

    /// <summary>收到服务端下发的消息（quota_update, license_update 等）。</summary>
    public event Action<ServerMessage>? OnMessage;

    /// <summary>发生错误。</summary>
    public event Action<Exception>? OnError;

    /// <summary>连接状态变更。</summary>
    public event Action<ConnectionState>? OnStateChanged;

    // ==================== 属性 ====================

    public ConnectionState State { get; private set; } = ConnectionState.Disconnected;

    public VrControllerClient(string serverUrl, string token)
    {
        _serverUrl = serverUrl.TrimEnd('/');
        _token = token;
        _http = new HttpClient { Timeout = TimeSpan.FromSeconds(10) };
    }

    // ==================== 连接管理 ====================

    /// <summary>建立 WebSocket 连接并开始心跳。</summary>
    public async Task ConnectAsync(CancellationToken ct = default)
    {
        if (State == ConnectionState.Connected) return;

        SetState(ConnectionState.Connecting);
        _cts = CancellationTokenSource.CreateLinkedTokenSource(ct);

        try
        {
            _ws = new ClientWebSocket();
            var uri = new Uri($"{_serverUrl}/ws?token={_token}");
            await _ws.ConnectAsync(uri, _cts.Token);

            SetState(ConnectionState.Connected);
            OnConnected?.Invoke();

            // 启动心跳 + 接收循环
            _ = Task.Run(() => HeartbeatLoop(_cts.Token));
            _ = Task.Run(() => ReceiveLoop(_cts.Token));
        }
        catch (Exception ex)
        {
            SetState(ConnectionState.Disconnected);
            OnError?.Invoke(ex);
        }
    }

    /// <summary>断开连接。</summary>
    public async Task DisconnectAsync()
    {
        _autoReconnect = false;
        _cts?.Cancel();
        if (_ws?.State == WebSocketState.Open)
        {
            try { await _ws.CloseAsync(WebSocketCloseStatus.NormalClosure, "", CancellationToken.None); }
            catch { /* 忽略关闭时的错误 */ }
        }
        SetState(ConnectionState.Disconnected);
    }

    private async Task ReconnectLoop()
    {
        int attempts = 0;
        while (_autoReconnect && !_cts!.Token.IsCancellationRequested)
        {
            try
            {
                await Task.Delay(ReconnectDelay, _cts.Token);
                attempts++;
                SetState(ConnectionState.Connecting);
                _ws?.Dispose();
                _ws = new ClientWebSocket();
                var uri = new Uri($"{_serverUrl}/ws?token={_token}");
                await _ws.ConnectAsync(uri, _cts.Token);
                SetState(ConnectionState.Connected);
                OnConnected?.Invoke();
                _ = Task.Run(() => HeartbeatLoop(_cts.Token));
                _ = Task.Run(() => ReceiveLoop(_cts.Token));
                return;
            }
            catch (OperationCanceledException) { break; }
            catch (Exception ex)
            {
                OnError?.Invoke(new Exception($"重连失败 ({attempts}/{MaxReconnectAttempts})", ex));
                if (attempts >= MaxReconnectAttempts)
                {
                    OnDisconnected?.Invoke($"已达最大重连次数 ({MaxReconnectAttempts})");
                    SetState(ConnectionState.Disconnected);
                    break;
                }
            }
        }
    }

    // ==================== 业务方法 ====================

    /// <summary>开始 VR 会话。应用启动时调用。</summary>
    public async Task StartSessionAsync(long appId, string version)
    {
        await SendAsync(new
        {
            type = "session_start",
            app_id = appId,
            version
        });
    }

    /// <summary>结束 VR 会话。应用关闭时调用。</summary>
    public async Task EndSessionAsync()
    {
        if (_sessionId <= 0) return;
        await SendAsync(new
        {
            type = "session_end",
            session_id = _sessionId
        });
    }

    /// <summary>检查平台 API 是否可达。</summary>
    public async Task<bool> HealthCheckAsync()
    {
        try
        {
            var resp = await _http.GetAsync($"{_serverUrl}/health");
            return resp.IsSuccessStatusCode;
        }
        catch { return false; }
    }

    /// <summary>获取授权状态。</summary>
    public async Task<LicenseInfo?> GetLicenseAsync(long appId)
    {
        // 通过 REST API 查询授权（需要场地控制器有 HTTP 访问权限）
        // 实际场景中授权信息通过 WebSocket 主动推送
        try
        {
            var resp = await _http.GetAsync($"{_serverUrl}/api/admin/licenses?tenant_id=0");
            return null; // 管理端 API，控制器不应直接调
        }
        catch { return null; }
    }

    // ==================== 内部方法 ====================

    private async Task SendAsync(object message)
    {
        if (_ws?.State != WebSocketState.Open) return;
        var json = JsonSerializer.Serialize(message, JsonOptions);
        var bytes = Encoding.UTF8.GetBytes(json);
        await _ws.SendAsync(new ArraySegment<byte>(bytes), WebSocketMessageType.Text, true, _cts?.Token ?? CancellationToken.None);
    }

    private async Task HeartbeatLoop(CancellationToken ct)
    {
        while (!ct.IsCancellationRequested && _ws?.State == WebSocketState.Open)
        {
            try
            {
                await Task.Delay(HeartbeatInterval, ct);
                await SendAsync(new { type = "heartbeat" });
            }
            catch (OperationCanceledException) { break; }
            catch (Exception ex)
            {
                OnError?.Invoke(ex);
                break;
            }
        }
    }

    private async Task ReceiveLoop(CancellationToken ct)
    {
        var buffer = new byte[4096];
        var messageBuffer = new StringBuilder();

        while (!ct.IsCancellationRequested && _ws?.State == WebSocketState.Open)
        {
            try
            {
                WebSocketReceiveResult result;
                messageBuffer.Clear();

                do
                {
                    result = await _ws.ReceiveAsync(new ArraySegment<byte>(buffer), ct);
                    messageBuffer.Append(Encoding.UTF8.GetString(buffer, 0, result.Count));
                } while (!result.EndOfMessage);

                if (result.MessageType == WebSocketMessageType.Close)
                {
                    SetState(ConnectionState.Disconnected);
                    OnDisconnected?.Invoke("服务端关闭连接");
                    if (_autoReconnect) _ = Task.Run(() => ReconnectLoop());
                    break;
                }

                var json = messageBuffer.ToString();

                // 心跳 ACK 内部处理
                if (json.Contains("\"heartbeat_ack\"")) continue;

                // 解析 session_start 响应获取 session_id
                if (json.Contains("\"session_id\""))
                {
                    try
                    {
                        var doc = JsonDocument.Parse(json);
                        if (doc.RootElement.TryGetProperty("session_id", out var sid))
                            _sessionId = sid.GetInt64();
                    }
                    catch { }
                }

                var msg = JsonSerializer.Deserialize<ServerMessage>(json, JsonOptions);
                if (msg != null) OnMessage?.Invoke(msg);
            }
            catch (OperationCanceledException) { break; }
            catch (WebSocketException)
            {
                SetState(ConnectionState.Disconnected);
                OnDisconnected?.Invoke("WebSocket 连接异常断开");
                if (_autoReconnect) _ = Task.Run(() => ReconnectLoop());
                break;
            }
            catch (Exception ex)
            {
                OnError?.Invoke(ex);
            }
        }
    }

    private void SetState(ConnectionState state)
    {
        State = state;
        OnStateChanged?.Invoke(state);
    }

    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.SnakeCaseLower,
        DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull
    };

    public void Dispose()
    {
        _cts?.Cancel();
        _ws?.Dispose();
        _http.Dispose();
    }
}

// ==================== 类型定义 ====================

public enum ConnectionState
{
    Disconnected,
    Connecting,
    Connected
}

/// <summary>服务端下发的消息。</summary>
public class ServerMessage
{
    [JsonPropertyName("type")]
    public string Type { get; set; } = "";

    [JsonPropertyName("app_id")]
    public long? AppId { get; set; }

    [JsonPropertyName("quota_remaining")]
    public long? QuotaRemaining { get; set; }

    [JsonPropertyName("granted")]
    public bool? Granted { get; set; }
}

/// <summary>授权信息（预留）。</summary>
public class LicenseInfo
{
    public long AppId { get; set; }
    public bool Granted { get; set; }
    public long QuotaRemaining { get; set; }
}
