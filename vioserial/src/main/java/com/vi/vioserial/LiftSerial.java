package com.vi.vioserial;

import android.text.TextUtils;
import com.google.gson.Gson;
import com.vi.vioserial.bean.LiftBean;
import com.vi.vioserial.listener.OnConnectListener;
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

    public synchronized void init(String portStr, int ibaudRate) {
        init(portStr, ibaudRate, null);
    }

    public synchronized void init(String portStr, int ibaudRate, OnConnectListener connectListener) {
        if (TextUtils.isEmpty(portStr) || ibaudRate == 0) {
            throw new IllegalArgumentException("Serial port and baud rate cannot be empty");
        }
        if (this.mBaseSerial == null) {
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
            mBaseSerial.openSerial(connectListener);
        } else {
            Logger.getInstace().i(TAG, "Serial port has been initialized");
        }
    }

    /**
     * Add callback
     */
    public void addDataListener(OnLiftDataListener dataListener) {
        if (mListener == null) {
            mListener = new ArrayList<>();
        }
        mListener.add(dataListener);
    }

    /**
     * Remove callback
     */
    public void removeDataListener(OnLiftDataListener dataListener) {
        if (mListener != null) {
            mListener.remove(dataListener);
        }
    }

    /**
     * Remove all
     */
    public void clearAllDataListener() {
        if (mListener != null) {
            mListener.clear();
        }
    }

    /**
     * Listening to serial data
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
     * Serial port status (open/close)
     *
     * @return
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
