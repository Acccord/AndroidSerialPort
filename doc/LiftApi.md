## 升降板API

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
### 快捷导航
- [主板API](https://github.com/Acccord/AndroidSerialPort/blob/master/doc/Channel.md)
- [温控板API](https://github.com/Acccord/AndroidSerialPort/blob/master/doc/TempApi.md)
- [小推车API](https://github.com/Acccord/AndroidSerialPort/blob/master/doc/CarApi.md)
