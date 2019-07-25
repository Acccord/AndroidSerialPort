## LOCKER API
- [中文](https://github.com/Acccord/AndroidSerialPort/blob/master/doc/LockerApi.md)

### Step 1: [Configuration](https://github.com/Acccord/AndroidSerialPort/blob/master/README-en.md)（Ignore the article after it has been configured.）

### Step 2: Initialize
``` java
/**
 * Initialize
 * @param portStr       serial port number
 * @param ibaudRate     baud rate
 */
LockerSerial.instance().init(String portStr, int ibaudRate);

/**
 * If you want to know the result of the initialization, such as whether the initialization is successful, you can write
 *
 * @param connectListener   Serial port open result callback
 */
LockerSerial.instance().init(String portStr, int ibaudRate, OnConnectListener connectListener);
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
