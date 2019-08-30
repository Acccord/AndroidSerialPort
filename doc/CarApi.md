## 小推车 API

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

### 更新记录
- 2019-08-12 命令参数改为小写
- 2019-08-13 优化串口打开方式和回调结果
- 2019-08-22 小车板命令更新

### 快捷导航
- [主板API](https://github.com/Acccord/AndroidSerialPort/blob/master/doc/Channel.md)
- [温控板API](https://github.com/Acccord/AndroidSerialPort/blob/master/doc/TempApi.md)
- [升降板API](https://github.com/Acccord/AndroidSerialPort/blob/master/doc/LiftApi.md)
