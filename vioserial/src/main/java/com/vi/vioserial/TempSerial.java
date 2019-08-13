package com.vi.vioserial;

import android.text.TextUtils;
import com.google.gson.Gson;
import com.vi.vioserial.bean.TempBean;
import com.vi.vioserial.listener.OnSerialDataListener;
import com.vi.vioserial.listener.OnTempDataListener;
import com.vi.vioserial.util.Logger;
import com.vi.vioserial.util.SerialDataUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Vi
 * @date 2019-07-17 17:49
 * @e-mail cfop_f2l@163.com
 */

public class TempSerial {
    private static String TAG = "TempSerial";

    private volatile static TempSerial instance;

    private BaseSerial mBaseSerial;
    private List<OnTempDataListener> mListener;
    private Gson mGson;

    public static TempSerial instance() {
        if (instance == null) {
            synchronized (TempSerial.class) {
                if (instance == null) {
                    instance = new TempSerial();
                }
            }
        }
        return instance;
    }

    private TempSerial() {
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
                String dataStr = SerialDataUtils.hexStringToString(data);

                if (mListener != null) {
                    for (int i = mListener.size() - 1; i >= 0; i--) {
                        TempBean tempBean = mGson.fromJson(dataStr, TempBean.class);
                        mListener.get(i).tempDataBack(tempBean);
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
    public void addDataListener(OnTempDataListener dataListener) {
        if (mListener == null) {
            mListener = new ArrayList<>();
        }
        mListener.add(dataListener);
    }

    /**
     * Remove callback
     */
    public void removeDataListener(OnTempDataListener dataListener) {
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
     * Setting temperature
     *
     * @param isCold    Whether to turn on refrigeration
     * @param Upper1    Cooling temperature upper limit
     * @param Lower1    Cooling temperature lower limit
     * @param isDefrost Whether to open the defrost
     * @param Upper2    Defrost temperature upper limit
     * @param Lower2    Defrost temperature lower limit
     * @param isHot     Whether to turn on heating
     * @param Upper3    Heating temperature upper limit
     * @param Lower3    Heating temperature lower limit
     */
    public void setTemp(boolean isCold, int Upper1, int Lower1,
                        boolean isDefrost, int Upper2, int Lower2,
                        boolean isHot, int Upper3, int Lower3) {
        Map<String, Integer> map = new HashMap<>();
        map.put("wkcmd", 1);
        //Refrigeration
        if (isCold) {
            map.put("Upper1", Upper1);
            map.put("Lower1", Lower1);
        }
        //Defrost
        if (isDefrost) {
            map.put("Upper2", Upper2);
            map.put("Lower2", Lower2);
        }
        //heating
        if (isHot) {
            map.put("Upper3", Upper3);
            map.put("Lower3", Lower3);
        }
        sendData(mGson.toJson(map));
    }

    /**
     * Empty temperature setting
     */
    public void clearTempSet() {
        Map<String, Integer> map = new HashMap<>();
        map.put("wkcmd", 1);
        sendData(mGson.toJson(map));
    }

    public void readTemp() {
        Map<String, Integer> map = new HashMap<>();
        map.put("wkcmd", 2);
        sendData(mGson.toJson(map));
    }

    private void sendData(String data) {
        if (isOpen()) {
            String commandHex = SerialDataUtils.stringToHexString(data);
            mBaseSerial.sendHex(commandHex);
        }
    }

}
