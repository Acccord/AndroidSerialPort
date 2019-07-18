## LOCKER API
- [English](https://github.com/Acccord/AndroidSerialPort/blob/master/doc/LockerApi-en.md)

### 第1步：[配置](https://github.com/Acccord/AndroidSerialPort/blob/master/README.md)（已配置请忽略）

### 第2步：初始化
``` java
/**
 * 初始化
 * @param portStr       串口号
 * @param ibaudRate     波特率
 */
LockerSerial.instance().init(String portStr, int ibaudRate);

/**
 * 如果你想知道初始化的结果，比如是否初始化成功，你可以这样写
 * @param connectListener   串口打开结果回调
 */
LockerSerial.instance().init(String portStr, int ibaudRate, OnConnectListener connectListener);
```

### 第3步：数据接收
``` java
/**
 * 添加串口数据回调
 * @param dataListener   串口返回数据回调
 */
LockerSerial.instance().addDataListener(OnLockerDataListener dataListener);

//在不使用的时候移除回调
LockerSerial.instance().removeDataListener(OnLockerDataListener dataListener);
```

### 功能命令
- 1，打开箱子
``` java
/**
 * 打开箱子
 *
 * @param pcb       柜子号码
 * @param boxNumber 箱格号码
 */
LockerSerial.instance().openBox(int pcb,int boxNumber);
```

- 2，读取箱子状态
``` java
/**
 * 读取箱子状态
 *
 * @param pcb   柜子号码
 */
LockerSerial.instance().readBox();

//返回的箱子状态信息在OnLockerDataListener以下方法中返回
/**
 * 返回箱子状态信息
 *
 * @param jsonObject
 */
void boxStatus(JsonObject jsonObject);
```
