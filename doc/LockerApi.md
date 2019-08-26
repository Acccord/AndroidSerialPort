## LOCKER API
- [English](https://github.com/Acccord/AndroidSerialPort/blob/master/doc/LockerApi-en.md)

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
  * @param ibaudRate 波特率
  *
  * @return 0：打开串口成功
  *        -1：无法打开串口：没有串口读/写权限！
  *        -2：无法打开串口：未知错误！
  *        -3：无法打开串口：参数错误！
  */
LockerSerial.instance().open(String portStr, int ibaudRate);
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

### 更新记录
- 2019-08-13 优化串口打开方式和回调结果
