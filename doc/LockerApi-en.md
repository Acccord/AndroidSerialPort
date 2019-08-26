## LOCKER API
- [中文](https://github.com/Acccord/AndroidSerialPort/blob/master/doc/LockerApi.md)

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

### Step 2: Open serial port
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
LockerSerial.instance().open(String portStr, int ibaudRate);
```

### Step 3: Data reception
``` java
/**
 * Add serial data callback
 * @param dataListener   Serial port returns data callback
 */
LockerSerial.instance().addDataListener(OnLockerDataListener dataListener);

//Remove callbacks when not in use
LockerSerial.instance().removeDataListener(OnLockerDataListener dataListener);
```

### Function command
- 1，Open the box
``` java
/**
 * Open the box
 *
 * @param pcb       Locker number
 * @param boxNumber Box number
 */
LockerSerial.instance().openBox(int pcb,int boxNumber);
```

- 2，Read box status
``` java
/**
 * Read box status
 *
 * @param pcb   Locker number
 */
LockerSerial.instance().readBox();

//The returned box status information is returned in the following method of OnLockerDataListener
/**
 * Return box status information
 *
 * @param jsonObject
 */
void boxStatus(JsonObject jsonObject);
```

### update record
- 2019-08-13 Optimize serial port open mode and callback result
