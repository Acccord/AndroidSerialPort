## 安卓串口通信工具
简单的串口通信工具，只需要初始化之后就可以开始串口数据收发，完全不用考虑发送间隔和数据分包问题。
- [English](https://github.com/Acccord/AndroidSerialPort/blob/master/README-en.md)

### 快速使用
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
    implementation 'com.github.Acccord:AndroidSerialPort:1.0.0'
}
```

#### 第2步：初始化
``` java
//portStr=串口号；ibaudRate=波特率；
NormalSerial.instance().init(String portStr, int ibaudRate);

//如果想知道初始化的结果，比如是否初始化成功，可以添加初始化回调监听
NormalSerial.instance().init(String portStr, int ibaudRate, OnConnectListener connectListener);

```

#### 第3步：往串口发数据
``` java
//data=准备发送的数据，就这样数据就发到串口上了
NormalSerial.instance().sendData(String data)

```

#### 第4步：串口返回的数据接收
``` java
//dataListener为串口的接收数据回调，默认接收的类型为hex
//需要其他数据类型的，本项目提供了一个SerialDataUtils工具转换就行了
NormalSerial.instance().addDataListener(OnNormalDataListener dataListener)
```
总结：快速使用只需要的init成功后，就可以调用sendData往串口发送数据，同时addDataListener来监听串口数据返回。如需使用其他功能使用，可参考下面的**自定义使用**。

### 自定义使用
#### 第1步：配置（同上）

#### 第2步：创建实类
``` java
//portStr=串口号；ibaudRate=波特率；
BaseSerial mBaseSerial = new BaseSerial(String portStr, int ibaudRate) {
                           @Override
                           public void onDataBack(String data) {
                               //这里是串口的数据返回，默认返回类型为16进制字符串
                           }
                       };
```

#### 第3步：打开串口
``` java
//connectListener:监听串口是否正常打开
mBaseSerial.openSerial(OnConnectListener connectListener);
```

#### 第4步：向串口发送数据
``` java
//发送HEX字符串
mBaseSerial.sendHex(String sHex);

//发送字符串
mBaseSerial.sendTxt(String sTxt);

//发送字节数组
mBaseSerial.sendByteArray(byte[] bOutArray);
```

#### 其他方法介绍
方法名|返回参数|介绍
--|:--:|--:
close()|void|关闭串口
getBaudRate()|int|获取连接串口的波特率
getPort()|String|获取连接串口的串口号
isOpen()|boolean|串口是否打开
onDataBack(String data)|void|串口数据接收回调，该方法在主线程
openSerial(OnConnectListener connectListener)|void|打开串口；OnConnectListener为串口打开状态回调结果
sendHex(String sHex)|void|向串口发送HEX字符串
sendTxt(String sTxt)|void|向串口发送字符串
sendByteArray(byte[] bOutArray)|void|向串口发送字节数组
setDelay(int delay)|void|串口数据的发送间隔，默认300ms
setSerialDataListener(OnSerialDataListener dataListener)|void|监听串口数据的发送和接收，该方法可用于log打印；注意该方法回调不是在主线程

### 更新记录
- 1.0.0 【2019-07-18】
    - 发布1.0.0版本
- 暂未发布 【2019-08-12】
    - minSdkVersion改为14
    - 小推车API优化

### 其他API
以下内容需配合特定硬件使用，仅限特定开发人士参考。
- [主板API](https://github.com/Acccord/AndroidSerialPort/blob/master/doc/Channel.md)
- [温控板API](https://github.com/Acccord/AndroidSerialPort/blob/master/doc/TempApi.md)
- [升降板API](https://github.com/Acccord/AndroidSerialPort/blob/master/doc/LiftApi.md)
- [小推车API](https://github.com/Acccord/AndroidSerialPort/blob/master/doc/CarApi.md)
- [Locker API](https://github.com/Acccord/AndroidSerialPort/blob/master/doc/LockerApi.md)
