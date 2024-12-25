## Android serial communication tool
It was migrated from Google's official serial communication library and expanded on this basis. Provides a packaged API for serial communication in one minute. Stop bits, data bits, parity, and flow control can be set.
- [中文](https://github.com/Acccord/AndroidSerialPort/blob/master/README.md)


## MENU
 - Configuration
 - Proguard
 - Quick use
    - Step 1: Open the serial port
    - Step 2: Send data to the serial port
    - Step 3: Data reception returned by the serial port
 - Custom use
    - Step 1: Create a real class
    - Step 2: Parameter configuration
    - Step 3: Open the serial port
    - Step 4: Send data to the serial port
    - Detailed API
 - GOOGLE serial communication API
    - Set su path
    - View device serial port list
 - Update record


## Configuration
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
    implementation 'com.github.Acccord:AndroidSerialPort:1.5.2'
}
```


## Proguard
```
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class android.serialport.* {*;}

```

## Quick use
For the lack of special serial communication settings, use the default serial port configuration

### Step 1: Open the serial port
``` java
/**
 * Open serial port
 * @param portStr   serial port number
 * @param ibaudRate baud rate
 *
 * @return 0：Open the serial port successfully
 *        -1：Failed to open the serial port: no serial port read/write permission!
 *        -2：Failed to open serial port: unknown error!
 *        -3：Failed to open the serial port: the parameter is wrong!
 */
NormalSerial.instance().open(String portStr, int ibaudRate);
```

#### Step 2: Send data to the serial port
``` java
//data: The data you want to send, so your data is sent to the serial port.
NormalSerial.instance().sendData(String data)

```

#### Step 3: Data reception returned by the serial port
``` java
//dataListener is the receive data callback of the serial port. The default receiving type is hex.
//Need other data types, this project provides a SerialDataUtils tool conversion on the line
NormalSerial.instance().addDataListener(OnNormalDataListener dataListener)
```
Summary: After the fast use of only the required open, you can use sendData to send data to the serial port, and addDataListener to listen to the serial port data return. For other features to use, please refer to the following **Custom Use**.


## Quick use
### Step 1: Create a real class
``` java
/**
 * Open serial port
 * @param portStr   Serial port number
 * @param ibaudRate Baud rate
 */
BaseSerial mBaseSerial = new BaseSerial() {
                           @Override
                           public void onDataBack(String data) {
                               //Here is the serial port data return, the default return type is hex string
                           }
                       };
```

### Step 2: Parameter configuration
``` java
/**
 * Set the serial port
 * @param portStr Serial port number
 */
mBaseSerial.setsPort(String sPort);

/**
 * Set baud rate
 * @param iBaudRate Baud rate
 */
mBaseSerial.setiBaudRate( int iBaudRate);

/**
 * Stop bit [1 or 2]
 * @param mStopBits Stop bit (default 1)
 */
mBaseSerial.setmStopBits( int mStopBits);

/**
 * Data bit [5 ~ 8]
 * @param mDataBits Data bit (default 8)
 */
mBaseSerial.setmDataBits( int mDataBits);

/**
 * Parity【0 None； 1 Odd； 2 Even】
 * @param mParity Parity (default 0)
 */
mBaseSerial.setmParity( int mParity);

/**
 * Flow control [No flow control (NONE), hardware flow control (RTS/CTS), software flow control (XON/XOFF)]
 * @param mFlowCon Flow control is not used by default
 */
mBaseSerial.setmFlowCon(int mFlowCon);
```

### Step 3: Open the serial port
``` java
/**
 * Open serial port
 *
 * @return 0：Open the serial port successfully
 *        -1：Failed to open the serial port: no serial port read/write permission!
 *        -2：Failed to open serial port: unknown error!
 *        -3：Failed to open the serial port: the parameter is wrong!
 */
mBaseSerial.openSerial();
```

### Step 4: Send data to the serial port
``` java
//Send HEX string
mBaseSerial.sendHex(String sHex);

//Send string
mBaseSerial.sendTxt(String sTxt);

//Send byte array
mBaseSerial.sendByteArray(byte[] bOutArray);
```

#### Other methods
Method Name|Return Parameter|Introduction
--|:--:|--:
close()|void|Close the serial port
getBaudRate()|int|Get baud rate
getDataBits()|int|Get data bit
getFlowCon()|int|Get flow control
getParity()|int|Get parity mode
getPort()|String|Get the serial port number
getStopBits()|int|Get stop bit
isOpen()|boolean|Is open
onDataBack(String data)|void|Serial data reception callback, the method is in the main thread
openSerial()|int|Open the serial port; 0=Open the serial port successfully; -1=Unable to open the serial port: No serial port read/write permission; -2=Unable to open serial port: Unknown error; -3=Unable to open serial port: Parameter error!
sendHex(String sHex)|void|Send a HEX string to the serial port
sendTxt(String sTxt)|void|Send a string to the serial port
sendByteArray(byte[] bOutArray)|void|Send a byte array to the serial port
setDelay(int delay)|void|Serial data transmission interval, default 300ms
setBaudRate(int iBaudRate)|void|Set baud rate
setGap(int gap)|void|Serial data read interval, default 30ms
setDataBits(int mDataBits)|void|Set data bit
setFlowCon(int mFlowCon)|void|Set up flow control
setParity(int mParity)|void|Set parity mode
setPort(String sPort)|void|Set the serial port number
setStopBits(int mStopBits)|void|Set stop bit
setSerialDataListener(OnSerialDataListener dataListener)|void|Listening to the sending and receiving of serial data, this method can be used for log printing; note that the method callback is not in the main thread


## GOOGLE serial communication API

### Set su path
``` java
//Need to call before opening the serial port
SerialPort.setSuPath("/system/xbin/su");
```

### View device serial port list
``` java
SerialPortFinder serialPortFinder = new SerialPortFinder();
String[] allDevices = serialPortFinder.getAllDevices();
String[] allDevicesPath = serialPortFinder.getAllDevicesPath();

```

## Update record
- 1.0.0 【2019-07-18】
    - Release version 1.0.0
- 1.1.0 【2019-08-13】
    - minSdkVersion changed to 14
    - Open serial port callback method optimization
- 1.3.0 【2019-09-19】
    - Increase stop bit, data bit, parity, flow control settings
- 1.5.0 【2019-11-26】
    - Release version 1.5.0
- 1.5.2 【2024-12-18】
    - Resolve the issue of failing to depend on version 1.5.0
