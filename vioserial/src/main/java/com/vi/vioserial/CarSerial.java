package com.vi.vioserial;

import android.text.TextUtils;
import com.google.gson.Gson;
import com.vi.vioserial.bean.CarAction;
import com.vi.vioserial.bean.CarStatus;
import com.vi.vioserial.bean.CarVersion;
import com.vi.vioserial.listener.OnCarDataListener;
import com.vi.vioserial.listener.OnConnectListener;
import com.vi.vioserial.listener.OnSerialDataListener;
import com.vi.vioserial.util.Logger;
import com.vi.vioserial.util.SerialDataUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Vi
 * @date 2019-07-17 17:46
 * @e-mail cfop_f2l@163.com
 */

public class CarSerial {
    private static String TAG = "CarSerial";

    private volatile static CarSerial instance;

    private BaseSerial mBaseSerial;
    private List<OnCarDataListener> mListener;
    private Gson mGson;

    public static CarSerial instance() {
        if (instance == null) {
            synchronized (CarSerial.class) {
                if (instance == null) {
                    instance = new CarSerial();
                }
            }
        }
        return instance;
    }

    private CarSerial() {
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
                    String dataStr = SerialDataUtils.hexStringToString(data);
                    if (mListener != null) {
                        for (int i = mListener.size() - 1; i >= 0; i--) {
                            if (dataStr.contains("ver")) {
                                CarVersion carVersion = mGson.fromJson(dataStr, CarVersion.class);
                                mListener.get(i).carVersion(carVersion);
                            } else if (dataStr.contains("switch")) {
                                CarStatus carStatus = mGson.fromJson(dataStr, CarStatus.class);
                                mListener.get(i).carStatus(carStatus);
                            } else if (dataStr.contains("action")) {
                                CarAction carAction = mGson.fromJson(dataStr, CarAction.class);
                                mListener.get(i).carAction(carAction);
                            }
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
    public void addDataListener(OnCarDataListener dataListener) {
        if (mListener == null) {
            mListener = new ArrayList<>();
        }
        mListener.add(dataListener);
    }

    /**
     * Remove callback
     */
    public void removeDataListener(OnCarDataListener dataListener) {
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

    public void readVersion(int add) {
        Map<String, Object> map = new HashMap<>();
        map.put("ADD", add);
        map.put("mType", "ver");
        sendData(mGson.toJson(map));
    }

    public void readStatus(int add) {
        Map<String, Object> map = new HashMap<>();
        map.put("mType", "status");
        map.put("add", add);
        sendData(new Gson().toJson(map));
    }

    public void sendAction(int add, int m1, int action) {
        Map<String, Object> map = new HashMap<>();
        map.put("Mtype", "action");
        map.put("ADD", add);
        map.put("MOTOR1", m1);
        map.put("MOTOR2", action);
        sendData(new Gson().toJson(map));
    }

    public void cloceDoor(int add) {
        Map<String, Object> map = new HashMap<>();
        map.put("mType", "close");
        map.put("ADD", add);
        sendData(new Gson().toJson(map));
    }

    private void sendData(String data) {
        if (isOpen()) {
            String commandHex = SerialDataUtils.stringToHexString(data);
            mBaseSerial.sendHex(commandHex);
        }
    }

}
