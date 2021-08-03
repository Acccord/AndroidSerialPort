## 安卓串口通信工具
由 Google 官方串口通信库迁移而来，并在此基础上做了扩展。提供封装后的API，可在一分钟搞定串口通信。可设置停止位、数据位、奇偶校验、流控。也不用考虑分包拆包，拿来即用。
- [English](https://github.com/Acccord/AndroidSerialPort/blob/master/README-en.md)


## 目录
 - [配置](#配置)
 - [混淆](#混淆)
 - [快速使用](#快速使用)
    - 第1步：打开串口
    - 第2步：往串口发数据
    - 第3步：串口返回的数据接收
 - [打开多个串口](#打开多个串口)
    - 第1步：打开串口
    - 第2步：往串口发数据
    - 第3步：串口返回的数据接收
 - [自定义使用](#自定义使用)
    - 第1步：创建实类
    - 第2步：参数配置
    - 第3步：打开串口
    - 第4步：向串口发送数据
    - 详细API
 - [GOOGLE串口通信API](#GOOGLE串口通信API)
    - 设置su路径
    - 查看设备串口列表
 - [更新记录](#更新记录)


## 配置
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
    implementation 'com.github.Acccord:AndroidSerialPort:1.5.0'
}
```


## 混淆
```
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class android.serialport.* {*;}

```

## 快速使用
针对没有特殊串口通信设置需求，使用默认串口配置

### 第1步：打开串口
``` java
/**
 * 打开串口
 * @param portStr   串口号
 * @param ibaudRate 波特率
 *
 * @return 0：打开串口成功
 *        -1：无法打开串口：没有串口读/写权限！
 *        -2：无法打开串口：未知错误！
 *        -3：无法打开串口：参数错误！
 */
NormalSerial.instance().open(String portStr, int ibaudRate);

//例
int openStatus = NormalSerial.instance().open("/dev/ttyS1", 9600);
```

### 第2步：往串口发数据
``` java
/**
 * 注意发送的数据类型为hex，字符串需要转成hex在发送
 * 转换方法：SerialDataUtils.stringToHexString(String s)
 * @param hexData 发送的数据
 */
NormalSerial.instance().sendHex(String hexData)

//例
NormalSerial.instance().sendHex("AA033C0000E9")
```

### 第3步：串口返回的数据接收
``` java
/**
 * OnNormalDataListener 为串口的接收数据回调，建议在当前类去实现这个接口
 * 不用时记得释放NormalSerial.instance().removeDataListener(this);
 */
NormalSerial.instance().addDataListener(new OnNormalDataListener() {
    @Override
    public void normalDataBack(String hexData) {
        //注意，默认接收的类型为hex
        //需要转换为字符串可调用SerialDataUtils.hexStringToString(hexData)
    }
});
```
总结：快速使用只需要的open成功后，就可以调用sendHex往串口发送数据，同时addDataListener来监听串口数据返回。如需使用其他功能使用，可参考下面的**自定义使用**。

## 打开多个串口
同时打开多个串口，可以针对每一个串口进行数据收发

### 第1步：打开串口
``` java
/**
 * 打开串口，该方法可以多次调用，用来打开多个串口
 * @param portStr   串口号
 * @param ibaudRate 波特率
 *
 * @return 0：打开串口成功
 *        -1：无法打开串口：没有串口读/写权限！
 *        -2：无法打开串口：未知错误！
 *        -3：无法打开串口：参数错误！
 */
COMSerial.instance().addCOM(String portStr, int ibaudRate);
//也可以使用该方法添加串口，自定义串口参数
COMSerial.instance().addCOM(String portStr, int ibaudRate, int mStopBits, int mDataBits, int mParity, int mFlowCon);

//例
COMSerial.instance().addCOM("/dev/ttyS1", 9600);
COMSerial.instance().addCOM("/dev/ttyS2", 115200);
COMSerial.instance().addCOM("/dev/ttyS3", 9600, 1, 8, 0, 0);
```

### 第2步：往串口发数据
``` java
/**
 * 注意发送的数据类型为hex，字符串需要转成hex在发送
 * 转换方法：SerialDataUtils.stringToHexString(String s)
 * @param portStr 串口号（即需要往哪个串口发送数据）
 * @param hexData 发送的数据
 */
COMSerial.instance().sendHex(String portStr, String hexData)

//例
NormalSerial.instance().sendHex("/dev/ttyS1", "AA033C0000E9")
```

### 第3步：串口返回的数据接收
``` java
/**
 * OnComDataListener 为串口的接收数据回调，建议在当前类去实现这个接口
 * 不用时记得释放 COMSerial.instance().removeDataListener(this);
 *
 * 打开的所有串口有数据接收就会触发该回调，回调中会区分数据来自哪个串口，默认接收的类型为hex。
 */
COMSerial.instance().addDataListener(new OnComDataListener() {
    @Override
    public void comDataBack(String com, String hexData) {
        //com 为串口号
        //hexData 为接收到的数据
    }
});
```
总结：多个串口使用只需要多次调用addCOM方法，就可以同时打开多个串口。调用sendHex往不同的串口发送数据，同时addDataListener来监听串口数据返回。如需使用其他功能使用，可参考下面的**自定义使用**。

## 自定义使用

### 第1步：创建实类
``` java
/**
 * 打开串口
 * @param portStr   串口号
 * @param ibaudRate 波特率
 */
BaseSerial mBaseSerial = new BaseSerial() {
                           @Override
                           public void onDataBack(String data) {
                               //这里是串口的数据返回，默认返回类型为16进制字符串
                           }
                       };
```

### 第2步：参数配置
``` java
/**
 * 设置串口
 * @param portStr 串口号
 */
mBaseSerial.setsPort(String sPort);

/**
 * 设置波特率
 * @param iBaudRate 波特率
 */
mBaseSerial.setiBaudRate( int iBaudRate);

/**
 * 停止位 【1 或 2】
 * @param mStopBits 停止位（默认 1）
 */
mBaseSerial.setmStopBits( int mStopBits);

/**
 * 数据位【5 ~ 8】
 * @param mDataBits 数据位（默认 8）
 */
mBaseSerial.setmDataBits( int mDataBits);

/**
 * 奇偶校验【0 None； 1 Odd； 2 Even】
 * @param mParity 奇偶校验（默认 0）
 */
mBaseSerial.setmParity( int mParity);

/**
 * 流控 【不使用流控(NONE), 硬件流控(RTS/CTS), 软件流控(XON/XOFF)】
 * @param mFlowCon 默认不使用流控
 */
mBaseSerial.setmFlowCon(int mFlowCon);
```

### 第3步：打开串口
``` java
/**
 * 打开串口
 *
 * @return 0：打开串口成功
 *        -1：无法打开串口：没有串口读/写权限！
 *        -2：无法打开串口：未知错误！
 *        -3：无法打开串口：参数错误！
 */
mBaseSerial.openSerial();
```

### 第4步：向串口发送数据
``` java
//发送HEX字符串
mBaseSerial.sendHex(String sHex);

//发送字符串
mBaseSerial.sendTxt(String sTxt);

//发送字节数组
mBaseSerial.sendByteArray(byte[] bOutArray);
```

### 详细API
方法名|返回参数|介绍
--|:--:|--:
close()|void|关闭串口
getBaudRate()|int|获取连接串口的波特率
getDataBits()|int|获取数据位
getFlowCon()|int|获取流控
getParity()|int|获取奇偶校验方式
getPort()|String|获取连接串口的串口号
getStopBits()|int|获取停止位
isOpen()|boolean|串口是否打开
onDataBack(String data)|void|串口数据接收回调，该方法在主线程
openSerial()|int|打开串口；0=打开串口成功; -1=无法打开串口：没有串口读/写权限; -2=无法打开串口：未知错误; -3=无法打开串口：参数错误！
sendHex(String sHex)|void|向串口发送HEX字符串
sendTxt(String sTxt)|void|向串口发送字符串
sendByteArray(byte[] bOutArray)|void|向串口发送字节数组
setBaudRate(int iBaudRate)|void|设置波特率
setDelay(int delay)|void|串口数据的发送间隔，默认300ms
setGap(int gap)|void|串口数据的读取间隔，默认30ms
setDataBits(int mDataBits)|void|设置数据位
setFlowCon(int mFlowCon)|void|设置流控
setParity(int mParity)|void|设置奇偶校验方式
setPort(String sPort)|void|设置串口号
setStopBits(int mStopBits)|void|设置停止位
setSerialDataListener(OnSerialDataListener dataListener)|void|监听串口数据的发送和接收，该方法可用于log打印；注意该方法回调不是在主线程


## GOOGLE串口通信API

### 设置su路径
``` java
//需要在打开串口前调用
SerialPort.setSuPath("/system/xbin/su");
```

### 查看设备串口列表
``` java
SerialPortFinder serialPortFinder = new SerialPortFinder();
String[] allDevices = serialPortFinder.getAllDevices();
String[] allDevicesPath = serialPortFinder.getAllDevicesPath();

```

## 更新记录
- 1.5.0 【2019-11-26】
    - 发布1.5.0版本
- 1.3.0 【2019-09-19】
    - 增加停止位、数据位、奇偶校验、流控设置
- 1.1.0 【2019-08-13】
    - minSdkVersion改为14
    - 打开串口回调方法优化
- 1.0.0 【2019-07-18】
    - 发布1.0.0版本
