package com.vi.vioserial;

import android.text.TextUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vi.vioserial.listener.OnLockerDataListener;
import com.vi.vioserial.listener.OnSerialDataListener;
import com.vi.vioserial.util.Logger;
import com.vi.vioserial.util.SerialDataUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Vi
 * @date 2019-07-18 18:14
 * @e-mail cfop_f2l@163.com
 */

public class LockerSerial {
    private static String TAG = "LockerSerial";

    private volatile static LockerSerial instance;

    private BaseSerial mBaseSerial;
    private List<OnLockerDataListener> mListener;
    private Gson mGson;

    public static LockerSerial instance() {
        if (instance == null) {
            synchronized (LockerSerial.class) {
                if (instance == null) {
                    instance = new LockerSerial();
                }
            }
        }
        return instance;
    }

    private LockerSerial() {
        mGson = new Gson();
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
                String dataStr = SerialDataUtils.hexStringToString(data);
                if (dataStr.contains("read")) {
                    JsonObject returnData = new JsonParser().parse(dataStr).getAsJsonObject();
                    if (mListener != null) {
                        for (int i = mListener.size() - 1; i >= 0; i--) {
                            mListener.get(i).boxStatus(returnData);
                        }
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
     * Add callback
     */
    public void addDataListener(OnLockerDataListener dataListener) {
        if (mListener == null) {
            mListener = new ArrayList<>();
        }
        mListener.add(dataListener);
    }

    /**
     * Remove callback
     */
    public void removeDataListener(OnLockerDataListener dataListener) {
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

    /**
     * open locker
     *
     * @param pcb       locker index
     * @param boxNumber Box number
     */
    public void openBox(int pcb, int boxNumber) {
        Map<String, Object> map = new HashMap<>();
        map.put("cmd", "open");
        map.put("SubPCB", pcb);
        map.put("box", boxNumber);
        map.put("opentime", 100);
        String gsonStr = new Gson().toJson(map);
        sendData(gsonStr);
    }

    /**
     * read locker status
     *
     * @param pcb locker index
     */
    public void readBox(int pcb) {
        Map<String, Object> map = new HashMap<>();
        map.put("cmd", "Read");
        map.put("SubPCB", pcb);
        String gsonStr = new Gson().toJson(map);
        sendData(gsonStr);
    }

    private void sendData(String data) {
        if (isOpen()) {
            String commandHex = SerialDataUtils.stringToHexString(data);
            mBaseSerial.sendHex(commandHex);
        }
    }

}
