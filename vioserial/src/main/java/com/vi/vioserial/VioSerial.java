package com.vi.vioserial;

import android.text.TextUtils;
import com.vi.vioserial.impl.Data101ReviImpl;
import com.vi.vioserial.impl.Data101SendImpl;
import com.vi.vioserial.impl.Data427ReviImpl;
import com.vi.vioserial.impl.Data427SendImpl;
import com.vi.vioserial.listener.OnSerialDataListener;
import com.vi.vioserial.listener.OnSerialDataParse;
import com.vi.vioserial.listener.OnSerialDataSend;
import com.vi.vioserial.listener.OnVioDataListener;
import com.vi.vioserial.util.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Vi
 * @date 2019-07-17 17:50
 * @e-mail cfop_f2l@163.com
 */

public class VioSerial {
    private static String TAG = "VioSerial";

    public static final String SERIAL_101 = "NO_101";
    public static final String SERIAL_427 = "NO_427";

    private volatile static VioSerial instance;

    private BaseSerial mBaseSerial;
    private List<OnVioDataListener> mVioDataListener;
    private OnSerialDataParse mSerialDataParse;
    private OnSerialDataSend mSerialDataSend;
    private String mDefaultSerialType;

    public static VioSerial instance() {
        if (instance == null) {
            synchronized (VioSerial.class) {
                if (instance == null) {
                    instance = new VioSerial();
                }
            }
        }
        return instance;
    }

    public synchronized int open(String serialType, String portStr) {
        if (TextUtils.isEmpty(portStr)) {
            throw new IllegalArgumentException("Serial port and baud rate cannot be empty");
        }
        if (this.mBaseSerial != null) {
            close();
        }
        int ibaudRate;
        switch (serialType) {
            case SERIAL_101:
                ibaudRate = 9600;
                this.mSerialDataParse = new Data101ReviImpl();
                this.mSerialDataSend = new Data101SendImpl();
                break;
            case SERIAL_427:
                ibaudRate = 19200;
                this.mSerialDataParse = new Data427ReviImpl();
                this.mSerialDataSend = new Data427SendImpl();
                break;
            default:
                throw new IllegalArgumentException("serial type error");
        }
        mDefaultSerialType = serialType;

        mBaseSerial = new BaseSerial(portStr, ibaudRate) {
            @Override
            public void onDataBack(String data) {
                if (mVioDataListener == null || mVioDataListener.size() == 0 || mSerialDataParse == null) {
                    return;
                }
                mSerialDataParse.parseData(data, mVioDataListener);
            }
        };
        int openStatus = mBaseSerial.openSerial();
        if (openStatus != 0) {
            close();
        }
        return openStatus;
    }

    /**
     * 添加串口返回数据回调
     * Add callback
     */
    public void addDataListener(OnVioDataListener dataListener) {
        if (mVioDataListener == null) {
            mVioDataListener = new ArrayList<>();
        }
        mVioDataListener.add(dataListener);
    }

    /**
     * 移除串口返回数据回调
     * Remove callback
     */
    public void removeDataListener(OnVioDataListener dataListener) {
        if (mVioDataListener != null) {
            mVioDataListener.remove(dataListener);
        }
    }

    /**
     * 移除全部回调
     * Remove all
     */
    public void clearAllDataListener() {
        if (mVioDataListener != null) {
            mVioDataListener.clear();
        }
    }

    /**
     * 监听串口数据
     * Listening to serial data
     * 该方法必须在串口打开成功后调用
     * This method must be called after the serial port is successfully opened.
     */
    public void setSerialDataListener(OnSerialDataListener dataListener) {
        if (mBaseSerial != null) {
            mBaseSerial.setSerialDataListener(dataListener);
        } else {
            Logger.getInstace().e(TAG, "The serial port is closed or not initialized");
            //throw new IllegalArgumentException("The serial port is closed or not initialized");
        }
    }

    /**
     * 串口是否打开
     * Serial port status (open/close)
     *
     * @return true/false
     */
    public boolean isOpen() {
        if (mBaseSerial != null) {
            return mBaseSerial.isOpen();
        } else {
            Logger.getInstace().e(TAG, "The serial port is closed or not initialized");
            //throw new IllegalArgumentException("The serial port is closed or not initialized");
            return false;
        }
    }

    /**
     * 关闭串口
     * Close the serial port
     */
    public void close() {
        if (mBaseSerial != null) {
            mBaseSerial.close();
            mBaseSerial = null;
        } else {
            Logger.getInstace().e(TAG, "The serial port is closed or not initialized");
            //throw new IllegalArgumentException("The serial port is closed or not initialized");
        }
    }

    /**
     * send data
     *
     * @param data data
     */
    private void sendData(String data) {
        if (isOpen()) {
            mBaseSerial.sendHex(data);
        }
    }

    public void setShowLog(boolean isShowLog) {
        Logger.SHOW_LOG = isShowLog;
    }

    public String getDefaultSerialType() {
        return mDefaultSerialType;
    }

    public void readVersion() {
        sendData(mSerialDataSend.OnReadVersion());
    }

    public void openChannel(String channel, int lightType) {
        openChannel("1", channel, 3, lightType);
    }

    public void openCell(String channel, int lightType) {
        openChannel("1", channel, 0, lightType);
    }

    private void openChannel(String pcbAdd, String channel, int motorType, int lightType) {
        String dataStr = mSerialDataSend.OnOpenChanel(pcbAdd, channel, motorType, lightType);
        sendData(dataStr);
    }

    public void readSpring(int lightType) {
        String dataStr = mSerialDataSend.OnReadSpring(lightType);
        sendData(dataStr);
    }

    public void openLock() {
        sendData(mSerialDataSend.OnOpenLock(1));
        sendData(mSerialDataSend.OnOpenLock(0));
        sendData(mSerialDataSend.OnOpenLock(1));
        sendData(mSerialDataSend.OnOpenLock(0));
        sendData(mSerialDataSend.OnOpenLock(1));
        sendData(mSerialDataSend.OnOpenLock(0));
    }

    //*********************** 427 **********************/

    @Deprecated
    public void restartSerial() {
        String dataStr = mSerialDataSend.OnRestartSerial();
        sendData(dataStr);
    }

    @Deprecated
    public void appStart() {
        String dataStr = mSerialDataSend.OnAppStart();
        sendData(dataStr);
    }

    @Deprecated
    public void retunrCoin(int coin) {
        String dataStr = mSerialDataSend.OnReturnCoin(coin);
        sendData(dataStr);
    }

    @Deprecated
    public void retunrBill(int count) {
        String dataStr = mSerialDataSend.OnReturnBill(count);
        sendData(dataStr);
    }

    @Deprecated
    public void changeCoin(int coinStatus) {
        sendData(mSerialDataSend.OnChangeCoin(coinStatus));
    }

    @Deprecated
    public void changeBill(int billStatus) {
        sendData(mSerialDataSend.OnChangeBill(billStatus));
    }

    @Deprecated
    public void clearMoney() {
        sendData(mSerialDataSend.OnClearMoney());
    }

    @Deprecated
    public void readCoin() {
        sendData(mSerialDataSend.OnReadCoin());
    }

    @Deprecated
    public void readBill() {
        sendData(mSerialDataSend.OnReadBill());
    }

    @Deprecated
    public void changeTempBill(int status) {
        sendData(mSerialDataSend.OnChangeTempBill(status));
    }

    @Deprecated
    public void readMoney() {
        sendData(mSerialDataSend.OnReadMoney());
    }

}
