## 主板API
> 提示：对接过程中所需要的参数（如主板类型、串口号、波特率...等）跟硬件相关，需要在对接过程中联系相关人员。

#### 第1步：配置
在项目的build.gradle添加
```
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```
在模块的build.gradle添加
```
dependencies {
    implementation 'com.github.Acccord:AndroidSerialPort:1.2.0'
}
```

### 第2步：打开串口
``` java
 /**
  * 打开串口
  * @param serialType   主板类型（VioSerial.SERIAL_101/VioSerial.SERIAL_427）
  * @param portStr      串口号
  *
  * @return 0：打开串口成功
  *        -1：无法打开串口：没有串口读/写权限！
  *        -2：无法打开串口：未知错误！
  *        -3：无法打开串口：参数错误！
  */
VioSerial.instance().open(String serialType, String portStr);
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
OnVioDataListener回调方法参数详细解释

方法|释义|回调参数详细|主板类型
--|:--:|:--|--:
void version(DataVersion dataVersion)|版本号，readVersion方法返回|Version: 版本号<br>Testmode:1物理按钮被点击 0默认值|通用
void springResult(DataStatus dataStatus)|出货结果，readSpring方法返回|channelResult: 货道出货结果<br>&nbsp;&nbsp;0 = 成功<br>&nbsp;&nbsp;1 = 过流。负载过重,或者卡货<br>&nbsp;&nbsp;2 = 欠流。一般是电线断开,或者负载未安装<br>&nbsp;&nbsp;3 = 超时。指超过7秒仍未检测到电机到位信号。（一般是由于负载过重、卡货或开关电源干扰引起）<br>&nbsp;&nbsp;4 = 光幕自检失败，未启动电机。<br>&nbsp;&nbsp;5 = 有反馈电磁铁门未开<br>&nbsp;&nbsp;6 = 427板子 出货失败<br>&nbsp;&nbsp;9 = 427板子 出货中<br><br>lightResult: 货物经过光幕的时间<br>&nbsp;&nbsp;0 = 无掉货<br>&nbsp;&nbsp;1~200 = 表示货物经过光栅的时间（单位ms）|通用
void openResult(int result)| 电机转动结果，openChannel方法返回|result: 电机转动结果<br>&nbsp;&nbsp;0=已启动；<br>&nbsp;&nbsp;1=无效的电机索引号；<br>&nbsp;&nbsp;2=另一台电机在运行；|通用
void lightResult(int result)|光幕读取结果，lightRead方法返回|result: 光幕读取结果<br>&nbsp;&nbsp;0=遮挡；<br>&nbsp;&nbsp;1=未遮挡|101
void coin(CoinBean coinBean)|硬币器状态||427
void bill(BillBean billBean)|纸币器状态||427
void money(MoneyBean moneyBean)|读取纸硬币金额||427
void returnCoin(RestBean returnCoinBean)|找零状态||427
void coin(CoinBean coinBean)|硬币器状态||427
void error(String code)|SDK报错|code: 错误码<br>&nbsp;&nbsp;-1=串口数据读取不完整，请检查是否有其他程序占用该串口！<br>&nbsp;&nbsp;-2=数据验证失败，请检查是否有其他程序占用该串口！|通用

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

//电机转动结果返回，一般情况下该命令发送后，正常情况下电机会进行转动，出货结果请在下面【获取货道出货结果】中读取，一般情况下一秒读取一次，读取到结果后停止读取。
void openResult(int result)
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

//格子机出货结果，直接用电机转动结果返回作为出货结果
//result-> 0=已启动；1=无效的电机索引号；2=另一台电机在运行；
void openResult(int result)
```

- 4，获取货道出货结果
``` java
/**
  * 发送获取出货结果指令，一般情况下一秒读取一次出货结果，读取到结果后停止读取，最多8秒
  *
  * @param lightType 光幕类型 0-未使用光幕 （427主板：1-简易光幕 2-盒装光幕）（101主板：1-电机转一圈 2-电机转动直到检测到结果为止）
  */
VioSerial.instance().readSpring(int lightType);

//结果返回（OnVioDataListener以下方法中会返回出货结果）。
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
//目前只支持427主板，其他主板重启方法请联系相关人员。
VioSerial.instance().restartSerial();
```

- 7，打开光幕
``` java
//目前只支持101主板
VioSerial.instance().lightOpen();
```

- 8，读取光幕
``` java
//目前只支持101主板
VioSerial.instance().lightRead();

//光幕读取结果（OnVioDataListener以下方法中会返回光幕读取结果）。
//result 0=遮挡;1=未遮挡
void lightResult(int result);
```

- 9，关闭光幕
``` java
//目前只支持101主板
VioSerial.instance().lightClose();
```

### 业务流程
- 出货流程
    - 1，调用上面【2，货道出货】，根据结果判断电机是否转动成功，电机转动成功进行下一步
    - 2，电机转动成功后8秒内，每一秒读取一次出货结果【4，获取货道出货结果】
    - 3，读取到出货结果后流程结束，停止读取，最多读取8秒
- 光幕自检流程
    - 1，调用【7，打开光幕】
    - 2，一秒后调用【8，读取光幕】获取检测结果
    - 3，检测到结果后调用【9，关闭光幕】


## 其他说明
### 更新记录
- 2019-08-13 【1.1.1】
    - 优化串口打开方式和回调结果
- 2019-08-26 【1.2.0】
    - 加入光幕检测功能
    - 优化电机转动结果回调

### 专用名词介绍
- 主板
> 机器的主要控制板；目前分为427和101两种型号，主要功能是控制货道相关操作。
- 串口
> 串口通信的必要属性，该值由主板上的插线决定。
- 波特率
> 串口通信的必要属性，该值由嵌入式开发人员决定。
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

### 快捷导航
- [温控板API](https://github.com/Acccord/AndroidSerialPort/blob/master/doc/TempApi.md)
- [升降板API](https://github.com/Acccord/AndroidSerialPort/blob/master/doc/LiftApi.md)
- [小推车API](https://github.com/Acccord/AndroidSerialPort/blob/master/doc/CarApi.md)
