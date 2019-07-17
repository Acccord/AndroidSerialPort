## Android serial communication tool
Ultra-simple serial communication tools, you only need to initialize the serial port data transmission and reception,
you do not have to consider the transmission interval and data subcontracting.
- [中文](https://github.com/Acccord/AndroidSerialPort/blob/master/README.md)

### Simple to use
#### Step 1: Configure
Added in the project's build.gradle
```
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```
Added in module build.gradle
```
dependencies {
    implementation 'xxxxxxxxxxxx'//I haven't released yet...
}
```

#### Step 2: Initialize
```
//ortStr = serial port number; ibaudRate = baud rate;
NormalSerial.instance().init(String portStr, int ibaudRate);

//If you want to know the result of the initialization, such as whether the initialization is successful, you can write
NormalSerial.instance().init(String portStr, int ibaudRate, OnConnectListener connectListener);

```

#### Step 3: Send data to the serial port
```
//Data=The data you want to send, so your data is sent to the serial port.
NormalSerial.instance().sendData(String data)

```

#### Step 4: Data reception returned by the serial port
```
//dataListener is the receive data callback of the serial port. The default receiving type is hex.
//Need other data types, this project provides a SerialDataUtils tool conversion on the line
NormalSerial.instance().addDataListener(OnNormalDataListener dataListener)
```