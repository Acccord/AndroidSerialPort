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
    implementation 'com.github.Acccord:AndroidSerialPort:1.1.1'
}
```

### 更新记录
- 2019-08-12 命令参数改为小写
- 2019-08-13 优化串口打开方式和回调结果
- 2019-08-22 小车板命令更新