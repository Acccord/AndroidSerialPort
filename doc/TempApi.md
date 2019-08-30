## 温控板API

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
 * @param portStr   串口号
 *
 * @return 0：打开串口成功
 *        -1：无法打开串口：没有串口读/写权限！
 *        -2：无法打开串口：未知错误！
 *        -3：无法打开串口：参数错误！
 */
TempSerial.instance().open(String portStr);
```

### 第3步：数据接收
``` java
/**
 * 添加串口数据回调
 * @param dataListener   串口返回数据回调
 */
TempSerial.instance().addDataListener(OnTempDataListener dataListener);

//在不使用的时候移除回调
TempSerial.instance().removeDataListener(OnTempDataListener dataListener);
```

### 功能命令
- 1，设置温度
``` java
/**
 * 设置温度
 *
 * @param isCold    是否开启制冷
 * @param Upper1    制冷温度上限
 * @param Lower1    制冷温度下限
 * @param isDefrost 是否开启除霜
 * @param Upper2    除霜温度上限
 * @param Lower2    除霜温度下限
 * @param isHot     是否开启加热
 * @param Upper3    加热温度上限
 * @param Lower3    加热温度下限
 */
TempSerial.instance().setTemp(boolean isCold, int Upper1, int Lower1,
                              boolean isDefrost, int Upper2, int Lower2,
                              boolean isHot, int Upper3, int Lower3);
```

- 2，清空温度设置
``` java
TempSerial.instance().clearTempSet();
```

- 3，读取当前温度
``` java
TempSerial.instance().readTemp();

//返回的当前温度信息在OnTempDataListener以下方法中返回
/**
 * TempBean参数
 * @param tmp1 设备内温度
 * @param tmp2 冷凝器温度
 * @param tmp3 加热器温度
 * @param tmp1SetH 设置的制冷温度上限
 * @param tmp1SetL 设置的制冷温度下限
 * @param tmp2SetH 设置的除霜温度上限
 * @param tmp2SetL 设置的除霜温度下限
 * @param tmp3SetH 设置的加热温度上限
 * @param tmp3SetL 设置的加热温度下限
 * @param version 版本号
 */
void tempDataBack(TempBean tempBean);
```

## 其他说明
该温控版可设置功能分别是制冷、除霜、加热，具体开启哪些功能由上面setTemp参数决定。温控版上装了三个温度探头，对应的值为TempBean中的tmp1，tmp2，tmp3
探头1的位置一般是机器仓内，所以一般用temp1为当前机器内温度；temp2一般对应的为冷凝器的温度；temp3对应的一般为加热器的温度。

### 更新记录
- 2019-08-13 优化串口打开方式和回调结果

### 快捷导航
- [主板API](https://github.com/Acccord/AndroidSerialPort/blob/master/doc/Channel.md)
- [升降板API](https://github.com/Acccord/AndroidSerialPort/blob/master/doc/LiftApi.md)
- [小推车API](https://github.com/Acccord/AndroidSerialPort/blob/master/doc/CarApi.md)
