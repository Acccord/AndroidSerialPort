## 安卓串口通信工具
超简易的串口通信工具，只需要初始化之后就可以开始串口数据收发，你完全不用考虑发送间隔和数据分包问题。

### 简单使用
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
    implementation 'xxxxxxxxxxxx'//我还没发布...先占个坑
}
```

#### 第2步：初始化
```
//portStr=串口号；ibaudRate=波特率；
NormalSerial.instance().init(String portStr, int ibaudRate);

//如果你想知道初始化的结果，比如是否初始化成功，你可以这样写
NormalSerial.instance().init(String portStr, int ibaudRate, OnConnectListener connectListener);

```

#### 第3步：往串口发数据
```
//data=你要发送的数据，就这样的你的数据就发到串口上了
NormalSerial.instance().sendData(String data)

```

#### 第4步：串口返回的数据接收
```
//dataListener为串口的接收数据回调，默认接收的类型为hex，需要String用SerialDataUtils工具转换一下
 NormalSerial.instance().addDataListener(OnNormalDataListener dataListener)
```