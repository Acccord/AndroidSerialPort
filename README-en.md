## Android serial communication tool
Ultra-simple serial communication tools, you only need to initialize the serial port data transmission and reception,
you do not have to consider the transmission interval and data subcontracting.
- [中文](https://github.com/Acccord/AndroidSerialPort/blob/master/README.md)

### Quick use
#### Step 1: Configure
Add it in your root build.gradle at the end of repositories:
```
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```
Add the dependency
```
dependencies {
    implementation 'com.github.Acccord:AndroidSerialPort:1.2.0'
}
```

#### Step 2: Open serial port
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

#### Step 3: Send data to the serial port
``` java
//data: The data you want to send, so your data is sent to the serial port.
NormalSerial.instance().sendData(String data)

```

#### Step 4: Data reception returned by the serial port
``` java
//dataListener is the receive data callback of the serial port. The default receiving type is hex.
//Need other data types, this project provides a SerialDataUtils tool conversion on the line
NormalSerial.instance().addDataListener(OnNormalDataListener dataListener)
```
Summary: After the fast use of only the required init, you can use sendData to send data to the serial port, and addDataListener to listen to the serial port data return. For other features to use, please refer to the following **Custom Use**.


### Custom use
#### Step 1: Configuration (ibid.)

#### Step 2: Create a class
``` java
//portStr = serial port number; ibaudRate = baud rate;
BaseSerial mBaseSerial = new BaseSerial(String portStr, int ibaudRate) {
                           @Override
                           public void onDataBack(String data) {
                               //Here is the serial port data return, the default return type is hex string
                           }
                       };
```

#### Step 3: Open the serial port
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

#### Step 4：Send data to the serial port
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
getBaudRate()|int|Get the baud rate of the serial port
getPort()|String|Get the serial port number
isOpen()|boolean|is open
onDataBack(String data)|void|Serial data reception callback, the method is in the main thread
openSerial()|int|0：Open the serial port successfully; -1：Failed to open the serial port: no serial port read/write permission; -2：Failed to open serial port: unknown error; -3：Failed to open the serial port: the parameter is wrong!
sendHex(String sHex)|void|Send a HEX string to the serial port
sendTxt(String sTxt)|void|Send a string to the serial port
sendByteArray(byte[] bOutArray)|void|Send a byte array to the serial port
setDelay(int delay)|void|Serial data transmission interval, default 300ms
setSerialDataListener(OnSerialDataListener dataListener)|void|Listening to the sending and receiving of serial data, this method can be used for log printing; note that the method callback is not in the main thread

### update record
- 1.0.0 [2019-07-18]
    - Release version 1.0.0
- 1.1.0 [2019-08-13]
    - minSdkVersion changed to 14
    - Open serial port callback method optimization
    - Internal api update
- 1.1.1 [2019-08-22]
    - Internal api update
- 1.2.0 [2019-08-26]
    - Internal api update