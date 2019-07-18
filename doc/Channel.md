## 主板API

### 第1步：[配置](https://github.com/Acccord/AndroidSerialPort/blob/master/README.md)（已配置请忽略）

### 第2步：初始化
``` java
/**
 * 初始化
 * @param serialType    主板类型（VioSerial.NO_101/VioSerial.NO_427）
 * @param portStr       串口号
 * @param ibaudRate     波特率
 */
VioSerial.instance().init(String serialType, String portStr, int ibaudRate);

/**
 * 如果你想知道初始化的结果，比如是否初始化成功，你可以这样写
 * @param connectListener   串口打开结果回调
 */
VioSerial.instance().init(String serialType, String portStr, int ibaudRate, OnConnectListener connectListener);
```

### 第3步：数据接收
``` java
/**
 * 添加串口数据回调
 * @param dataListener   串口返回数据回调
 */
VioSerial.instance().addDataListener(OnVioDataListener dataListener);

//在不使用的时候移除回调
VioSerial.instance().removeDataListener(OnVioDataListener dataListener);
```

### 功能命令
- 1，获取下位机版本号
``` java
//发送获取下位机版本号指令
VioSerial.instance().readVersion();

//版本号返回（OnVioDataListener以下方法中会返回版本号信息）。
void version(DataVersion dataVersion);
```

- 2，货道出货
``` java
/**
 * 货道出货
 *
 * @param channel   货道号码 三位数表示，详细规则见下面**货道号规范**
 * @param lightType 光幕类型 0-未使用光幕 （427主板：1-简易光幕 2-盒装光幕）（101主板：1-电机转一圈 2-电机转动直到检测到结果为止）
 */
VioSerial.instance().openChannel(String channel, int lightType);

//该命令发送后，正常情况下电机会进行转动，出货结果请在下面"出货结果"中读取，一般情况下一秒读取一次。
```

- 3，格子机出货
``` java
/**
  * 打开格子机器箱格
  *
  * @param channel      货道号码 三位数表示，详细规则见下面**货道号规范**
  * @param lightType    光幕设置 0-未使用光幕 （427主板：1-简易光幕 2-盒装光幕）（101主板：1-电机转一圈 2-电机转动直到检测到结果为止）
  */
VioSerial.instance().openCell(String channel, int lightType);

//格子机出货结果会直接在OnVioDataListener以下方法中会返回出货结果
//result-> 0=已启动；1=无效的电机索引号；2=另一台电机在运行；
void cellResult(int result);
```

- 4，获取货道出货结果
``` java
/**
  * 发送获取出货结果指令，一般情况下一秒读取一次出货结果
  *
  * @param lightType 光幕类型 0-未使用光幕 （427主板：1-简易光幕 2-盒装光幕）（101主板：1-电机转一圈 2-电机转动直到检测到结果为止）
  */
VioSerial.instance().readSpring(int lightType);

//数据返回（OnVioDataListener以下方法中会返回出货结果）。
/**
 * DataStatus参数
 * @param channelResult 货道出货结果
 * 0 = 成功。
 * 1 = 过流。负载过重,或者卡货
 * 2 = 欠流。一般是电线断开,或者负载未安装
 * 3 = 超时。指超过7秒仍未检测到电机到位信号。（一般是由于负载过重、卡货或开关电源干扰引起）
 * 4 = 光幕自检失败，未启动电机。
 * 5 = 有反馈电磁铁门未开
 * 6 = 出货失败（427板子）
 * 9 = 出货中（427板子）
 *
 * ----------
 *
 * @param lightResult 货物经过光幕的时间，
 * 0 = 无掉货
 * 1~200 = 表示货物经过光栅的时间（单位ms）
 */
void springResult(DataStatus dataStatus);

//注意：如果"货道出货"指令中lightType=0，则channelResult为最终出货结果；
//如果"货道出货"指令中lightType!=0，则根据lightResult作为最终出货结果，0表示无掉货，大于0表示有货物掉落；
```

- 5，开门锁
``` java
//该方法通过继电器开门
VioSerial.instance().openLock();
```

- 6，机器重启
``` java
//目前只支持427主板，其他主板重启方法请根据联系我
VioSerial.instance().restartSerial();
```

## 其他说明
### 专用名词介绍
- 主板
> 机器的主要控制板；目前分为427和101两种型号，主要功能是控制货道相关操作。
- 串口/波特率
> 串口通信的必要属性，该值由硬件决定。
- 光幕
> 检测物品是否掉落的硬件设备。带有光幕的机器出货结果由这个决定。

### 货道号规范
``` text
所有货道号按照3位数来制定。
**110**
第一位数字 -> 机器编号：主机为1，从机为2、3、4、5、6、7、8。具体数字由从机数量决定，没从机的话默认主机。
第二位数字 -> 层数：第一层为1，第二层为2 ...依次类推。
第三位数字 -> 货道顺序：第一个货道为0，第二个货道为1 ...依次类推。注意货道顺序从0开始，最多到9，也就是每层最多十个货道。
例：第一层第一个货道为110；第二层第二个货道为121；第五层第七个货道为156 ...
```