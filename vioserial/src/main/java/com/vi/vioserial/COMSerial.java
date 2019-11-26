package com.vi.vioserial;

import android.text.TextUtils;

import com.vi.vioserial.listener.OnComDataListener;
import com.vi.vioserial.listener.OnSerialDataListener;
import com.vi.vioserial.util.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Vi
 * @date 2019-11-26 11:43
 * @e-mail cfop_f2l@163.com
 */

public class COMSerial {
    private static String TAG = "COMSerial";

    private volatile static COMSerial instance;

    private Map<String, BaseSerial> mBaseSerials = new HashMap<String, BaseSerial>();
    private List<OnComDataListener> mListener;

    public static COMSerial instance() {
        if (instance == null) {
            synchronized (COMSerial.class) {
                if (instance == null) {
                    instance = new COMSerial();
                }
            }
        }
        return instance;
    }

    public synchronized int addCOM(String portStr, int ibaudRate) {
        return addCOM(portStr, ibaudRate, 1, 8, 0, 0);
    }

    public synchronized int addCOM(final String portStr, int ibaudRate, int mStopBits, int mDataBits, int mParity, int mFlowCon) {
        if (TextUtils.isEmpty(portStr) || ibaudRate == 0) {
            throw new IllegalArgumentException("Serial port and baud rate cannot be empty");
        }

        BaseSerial baseSerials = mBaseSerials.get(portStr);
        if (baseSerials != null && baseSerials.isOpen()) {
            return 1;
        }

        BaseSerial mBaseSerial = new BaseSerial(portStr, ibaudRate) {
            @Override
            public void onDataBack(String data) {
                //温度
                if (mListener != null) {
                    for (int i = mListener.size() - 1; i >= 0; i--) {
                        mListener.get(i).comDataBack(portStr, data);
                    }
                }
            }
        };
        mBaseSerial.setmStopBits(mStopBits);
        mBaseSerial.setmDataBits(mDataBits);
        mBaseSerial.setmParity(mParity);
        mBaseSerial.setmFlowCon(mFlowCon);
        int openStatus = mBaseSerial.openSerial();
        if (openStatus != 0) {
            mBaseSerial.close();
        } else {
            mBaseSerials.put(portStr, mBaseSerial);
        }
        return openStatus;
    }

    /**
     * 串口是否已经打开
     * Serial port status (open/close)
     */
    public boolean isOpenSerial(String portStr) {
        BaseSerial baseSerials = mBaseSerials.get(portStr);
        return baseSerials != null && baseSerials.isOpen();
    }

    /**
     * 添加串口返回数据回调
     * Add callback
     */
    public void addDataListener(OnComDataListener dataListener) {
        if (mListener == null) {
            mListener = new ArrayList<>();
        }
        mListener.add(dataListener);
    }

    /**
     * 移除串口返回数据回调
     * Remove callback
     */
    public void removeDataListener(OnComDataListener dataListener) {
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
    public void setSerialDataListener(String portStr, OnSerialDataListener dataListener) {
        BaseSerial baseSerial = mBaseSerials.get(portStr);
        if (baseSerial != null) {
            baseSerial.setSerialDataListener(dataListener);
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
    public boolean isOpen(String portStr) {
        BaseSerial baseSerial = mBaseSerials.get(portStr);
        if (baseSerial != null) {
            return baseSerial.isOpen();
        } else {
            Logger.getInstace().e(TAG, "The serial port is closed or not initialized");
            //throw new IllegalArgumentException("The serial port is closed or not initialized");
            return false;
        }
    }

    /**
     * Close the serial port
     */
    public void close(String portStr) {
        BaseSerial baseSerial = mBaseSerials.get(portStr);
        if (baseSerial != null) {
            baseSerial.close();
        } else {
            Logger.getInstace().e(TAG, "The serial port is closed or not initialized");
            //throw new IllegalArgumentException("The serial port is closed or not initialized");
        }
    }

    /**
     * send data
     *
     * @param portStr
     * @param hexData
     */
    public void sendHex(String portStr, String hexData) {
        if (TextUtils.isEmpty(portStr)) {
            Logger.getInstace().e(TAG, "The serial port is empty");
            return;
        }

        BaseSerial baseSerial = mBaseSerials.get(portStr);
        if (baseSerial != null && baseSerial.isOpen()) {
            String dateTrim = hexData.trim().replace(" ", "");
            baseSerial.sendHex(dateTrim);
        } else {
            Logger.getInstace().e(TAG, "The serial port is closed or not initialized");
        }
    }

}
