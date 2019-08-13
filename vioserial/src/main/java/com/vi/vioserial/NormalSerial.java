package com.vi.vioserial;

import android.text.TextUtils;
import com.vi.vioserial.listener.OnNormalDataListener;
import com.vi.vioserial.listener.OnSerialDataListener;
import com.vi.vioserial.util.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Vi
 * @date 2019-07-17 17:49
 * @e-mail cfop_f2l@163.com
 */

public class NormalSerial {
    private static String TAG = "NormalSerial";

    private volatile static NormalSerial instance;

    private BaseSerial mBaseSerial;
    private List<OnNormalDataListener> mListener;

    public static NormalSerial instance() {
        if (instance == null) {
            synchronized (NormalSerial.class) {
                if (instance == null) {
                    instance = new NormalSerial();
                }
            }
        }
        return instance;
    }

    public synchronized int open(String portStr, int ibaudRate) {
        if (TextUtils.isEmpty(portStr) || ibaudRate == 0) {
            throw new IllegalArgumentException("Serial port and baud rate cannot be empty");
        }
        if (this.mBaseSerial != null) {
            close();
        }
        mBaseSerial = new BaseSerial(portStr, ibaudRate) {
            @Override
            public void onDataBack(String data) {
                //温度
                if (mListener != null) {
                    for (int i = mListener.size() - 1; i >= 0; i--) {
                        mListener.get(i).normalDataBack(data);
                    }
                }
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
    public void addDataListener(OnNormalDataListener dataListener) {
        if (mListener == null) {
            mListener = new ArrayList<>();
        }
        mListener.add(dataListener);
    }

    /**
     * 移除串口返回数据回调
     * Remove callback
     */
    public void removeDataListener(OnNormalDataListener dataListener) {
        if (mListener != null) {
            mListener.remove(dataListener);
        }
    }

    /**
     * 移除全部回调
     * Remove all
     */
    public void clearAllDataListener() {
        if (mListener != null) {
            mListener.clear();
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
     * @param data
     */
    public void sendData(String data) {
        if (isOpen()) {
            mBaseSerial.sendHex(data);
        }
    }

}
