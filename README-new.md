## Android 串口通信库

Android 串口通信库，面向长期稳定运行的工程场景（工控、售货机、智能柜等）。

本库以**“快速使用 / 自定义使用”**为主线：  
**快速使用**用于极速完成串口收发；**自定义使用**覆盖协议、重连、监控与高端场景。

---

## 快速使用

针对没有特殊串口通信设置需求，使用默认串口配置。

默认能力包括：
- 8N1（8 数据位、无校验、1 停止位）
- 默认主线程回调（EasySerial）
- 默认帧解码：Idle-Gap（处理半包/粘包，默认空闲间隔 50ms、最大帧长 2048）
- 默认自动重连（指数退避策略，仅运行期进入 ERROR 才触发；手动 close 不重连；权限错误不重连）
- 默认支持多串口
提示：同一端口被其他进程占用时会打开失败，需先释放占用或更换端口。

使用提示（建议顺序）：
- 先注册 `onDataReceived` / `onError`，再 `open`，避免首包或首个异常被错过

### 第1步：打开串口

```java
EasySerial.open("/dev/ttyS1", 9600);
EasySerial.open("/dev/ttyS2", 9600);  // 可同时打开多个串口
```

返回值说明（打开阶段的即时结果）：
- `OPEN_OK (0)`：打开成功
- `OPEN_NO_PERMISSION (-1)`：无权限访问设备节点
- `OPEN_UNKNOWN_ERROR (-2)`：未知错误（设备异常/系统返回异常等）
- `OPEN_INVALID_PARAM (-3)`：参数非法（端口为空、波特率 <= 0）

### 第2步：往串口发数据

```java
EasySerial.send("/dev/ttyS1", HexCodec.decode("AA033C0000E9"));
```
提示：只有在对应端口 `open` 成功后再发送；`port` 必须与 `open` 时一致。

### 第3步：串口返回的数据接收

```java
EasySerial.onDataReceived(new EasySerial.OnDataReceivedListener() {
    @Override
    public void onDataReceived(String port, byte[] data, int length) {
        String hex = HexCodec.encode(data, 0, length);
        System.out.println("[" + port + "] RX: " + hex);
    }
});
```

### 第4步：异常监听

```java
EasySerial.onError(new EasySerial.OnErrorListener() {
    @Override
    public void onError(String port, SerialError error, String message, Throwable throwable) {
        System.err.println("[" + port + "] Error: " + message);
        if (throwable != null) throwable.printStackTrace();
    }
});
```

说明：运行过程中的断线、读写异常等属于异步错误，需要通过 `onError` 监听获取。
常见处理：权限不足请确认设备节点权限；设备路径不存在或被占用会导致 `OPEN_FAILED`/`CLOSED`。
建议：应用退出或不再使用时调用 `close` / `closeAll`，避免资源泄漏。

`SerialError` 枚举说明：
- `INVALID_PARAMETER`：参数错误
- `PERMISSION_DENIED`：权限不足
- `OPEN_FAILED`：打开串口失败
- `IO_ERROR`：读写异常
- `CLOSED`：串口已关闭或未打开

常见问题（简要排查）：
- `PERMISSION_DENIED`：确认设备节点权限或授予设备访问权限
- `OPEN_FAILED`：检查设备路径是否存在、是否被其他进程占用
- `CLOSED`：先 `open` 再 `send`，并确保端口名一致

**总结：** 快速使用只需要 `open` 成功后，就可以调用 `send` 往串口发送数据，同时 `onDataReceived` 来监听串口数据返回。如需使用其他功能，可参考下面的**自定义使用**。

---

## 自定义使用（协议 / 多串口精细管理 / 高端场景）

### 2.1 入口选择

- `SerialConnection`：单连接、可控性高
- `SerialManager`：多串口精细管理

### 2.1.1 回调线程提示

- `SerialConnection` / `SerialManager` 默认回调线程为 IO 线程（`Dispatchers.direct()`），请避免在回调里执行耗时操作。
- `EasySerial` 默认回调在主线程（见“快速使用”）。
- 如需切换到主线程或自定义线程池，请使用 `callbackDispatcher(...)` 配置。

### 2.2 `SerialConnection` + 协议帧（CRLF）

```java
SerialConfig config = new SerialConfig.Builder()
    .port("/dev/ttyS1")
    .baudRate(9600)
    .build();

SerialConnection conn = SerialConnection.builder(config)
    .frameDecoder(SerialFraming.crlf())
    .callbackDispatcher(Dispatchers.mainThread())
    .listener(new SerialListenerAdapter() {
        @Override
        public void onFrame(byte[] frame, int length) {
            String hex = HexCodec.encode(frame, 0, length);
            System.out.println("Frame: " + hex);
        }
    })
    .build();

conn.open();
```
提示：`SerialConnection` 默认不自动重连，只有配置 `reconnectPolicy(...)` 才会启用。
提示：重连仅在运行期进入 `ERROR` 状态触发，不会覆盖手动 `close` 的行为。

### 2.3 `SerialManager` 管理多串口

```java
SerialManager manager = new SerialManager();
manager.setCallbackDispatcher(Dispatchers.mainThread());

try {
    manager.open("/dev/ttyS1", 9600);
    manager.addDataListener("/dev/ttyS1", new SerialManager.OnHexDataListener() {
        @Override
        public void onData(String hex) {
            System.out.println("Received: " + hex);
        }
    });
    manager.sendHex("/dev/ttyS1", "AA033C0000E9");
} catch (SerialException e) {
    e.printStackTrace();
}
```
提示：`sendHex/sendBytes` 在端口未打开时会抛出 `CLOSED`；多端口场景请使用 `port` 区分回调来源。
提示：同一端口重复 `open` 会关闭旧连接并创建新连接，请避免并发重复打开。

### 2.4 高端场景（NIO + 监控 + 重连）

```java
SerialConfig config = new SerialConfig.Builder()
    .port("/dev/ttyS1")
    .baudRate(500000)
    .readTimeoutMs(50)
    .deviceCheckIntervalMs(1000)
    .useNioMode(true)
    .readBufferSize(8192)
    .build();

SerialConnection connection = SerialConnection.builder(config)
    .frameDecoder(SerialFraming.crlf())
    .logger(new AndroidSerialLogger(true))
    .reconnectPolicy(new ReconnectPolicy.ExponentialBackoff(10, 100, 2.0, 5000))
    .build();

connection.open();
SerialStatistics stats = connection.getStatistics();
System.out.println("Throughput: " + stats.getReceiveThroughputBps() + " B/s");
System.out.println("Error rate: " + stats.getErrorRate() + " errors/s");
```
提示：`readTimeoutMs` 越小 CPU 轮询越频繁；`deviceCheckIntervalMs` 越小越敏感但更耗电；`readBufferSize` 过小易增大分帧/拷贝开销。

### 2.5 自定义帧解码

```java
FrameDecoder fixed = SerialFraming.fixedLength(64);
FrameDecoder length = SerialFraming.lengthFieldBuilder()
    .lengthFieldOffset(0)
    .lengthFieldLength(2)
    .lengthAdjustment(0)
    .initialBytesToStrip(2)
    .build();
FrameDecoder idle = SerialFraming.idleGap(100, 1024);
```
说明：`lengthFieldBuilder()` 默认大端、`maxFrameLength=4096`；`initialBytesToStrip` 表示丢弃前 N 个字节。
提示：长度字段是否包含头部/校验，需与协议定义一致，否则会导致错帧或丢弃。

**丢弃计数说明：**  
当帧解码器检测到异常数据流（超长/非法长度）会丢弃缓冲区。可通过解码器实例读取丢弃次数与字节数：

```java
FrameDecoder decoder = SerialFraming.crlf();
if (decoder instanceof DelimiterFrameDecoder) {
    long dropped = ((DelimiterFrameDecoder) decoder).getDroppedCount();
    long droppedBytes = ((DelimiterFrameDecoder) decoder).getDroppedBytes();
}
```
提示：丢弃计数持续增长通常意味着帧边界或长度配置不匹配，请检查协议定义与解码器参数。

### 2.6 配置说明（`SerialConfig`）

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `port` | String | - | 串口路径（必需） |
| `baudRate` | int | - | 波特率（必需） |
| `stopBits` | int | 1 | 停止位（1或2） |
| `dataBits` | int | 8 | 数据位（5-8） |
| `parity` | int | 0 | 奇偶校验（0=None, 1=Odd, 2=Even） |
| `flowCon` | int | 0 | 流控（0=None, 1=Hard, 2=Soft） |
| `readTimeoutMs` | int | 1000 | 读超时（0=阻塞，>0=超时毫秒） |
| `sendIntervalMs` | int | 0 | 发送间隔（毫秒） |
| `deviceCheckIntervalMs` | int | 5000 | 设备在线检查间隔（毫秒，0=禁用） |
| `useNioMode` | boolean | false | 启用 NIO Selector（毫秒级超时精度） |
| `readBufferSize` | int | 1024 | 读缓冲区（字节，0=默认=1024） |
| `permissionStrategy` | PermissionStrategy | - | 设备权限策略（空表示不做自动权限处理） |

---
