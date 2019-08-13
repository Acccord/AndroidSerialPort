package com.vi.vioserial;

import android.text.TextUtils;
import com.google.gson.Gson;
import com.vi.vioserial.bean.LiftBean;
import com.vi.vioserial.listener.OnLiftDataListener;
import com.vi.vioserial.listener.OnSerialDataListener;
import com.vi.vioserial.util.Logger;
import com.vi.vioserial.util.SerialDataUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Vi
 * @date 2019-07-17 17:48
 * @e-mail cfop_f2l@163.com
 */

public class LiftSerial {
    private static String TAG = "LiftSerial";

    private volatile static LiftSerial instance;

    private BaseSerial mBaseSerial;
    private List<OnLiftDataListener> mListener;
    private Gson mGson;

    public static LiftSerial instance() {
        if (instance == null) {
            synchronized (LiftSerial.class) {
                if (instance == null) {
                    instance = new LiftSerial();
                }
            }
        }
        return instance;
    }

    private LiftSerial() {
        mGson = new Gson();
    }

    public synchronized int open(String portStr) {
        if (TextUtils.isEmpty(portStr)) {
            throw new IllegalArgumentException("Serial port and baud rate cannot be empty");
        }
        if (this.mBaseSerial != null) {
            close();
        }
        int ibaudRate = 19200;
        mBaseSerial = new BaseSerial(portStr, ibaudRate) {
            @Override
            public void onDataBack(String data) {
                //HEX字符转换为字符串
                String dataStr = SerialDataUtils.hexStringToString(data);
                LiftBean liftBean = mGson.fromJson(dataStr, LiftBean.class);
                //高度
                if (mListener != null && dataStr.length() > 8) {
                    for (int i = mListener.size() - 1; i >= 0; i--) {
                        mListener.get(i).liftDataBack(liftBean);
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
    public void addDataListener(OnLiftDataListener dataListener) {
        if (mListener == null) {
            mListener = new ArrayList<>();
        }
        mListener.add(dataListener);
    }

    /**
     * 移除串口返回数据回调
     * Remove callback
     */
    public void removeDataListener(OnLiftDataListener dataListener) {
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

    public void testCmd(int pwm, int dir) {
        Map<String, Integer> map = new HashMap<>();
        map.put("cmd", 0);
        map.put("pwm", pwm);
        map.put("dir", dir);
        sendCommand(mGson.toJson(map));
    }

    public void highCmd(int high) {
        Map<String, Integer> map = new HashMap<>();
        map.put("cmd", 1);
        map.put("High", high);
        sendCommand(mGson.toJson(map));
    }

    public void toZero() {
        Map<String, Integer> map = new HashMap<>();
        map.put("cmd", 2);
        sendCommand(mGson.toJson(map));
    }

    public void downCmd() {
        Map<String, Integer> map = new HashMap<>();
        map.put("cmd", 0);
        map.put("pwm", 200);
        map.put("dir", 0);
        sendCommand(mGson.toJson(map));
    }

    public void doorCmd(int door, int light) {
        Map<String, Integer> map = new HashMap<>();
        map.put("cmd", 3);
        map.put("door", door);
        map.put("light", light);
        sendCommand(mGson.toJson(map));
    }

    public void getHigh() {
        Map<String, Integer> map = new HashMap<>();
        map.put("cmd", 5);
        sendCommand(mGson.toJson(map));
    }

    private void sendCommand(String data) {
        if (isOpen()) {
            String commandHex = SerialDataUtils.stringToHexString(data);
            mBaseSerial.sendHex(commandHex);
        }
    }
}
