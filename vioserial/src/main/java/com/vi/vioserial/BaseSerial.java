package com.vi.vioserial;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;

import com.vi.vioserial.listener.OnConnectListener;
import com.vi.vioserial.util.Logger;
import com.vi.vioserial.util.SerialDataUtils;
import com.vi.vioserial.util.VioSerialHelper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidParameterException;

/**
 * @author Vi
 * @date 2019-06-28 15:30
 * @e-mail cfop_f2l@163.com
 */

public abstract class BaseSerial extends VioSerialHelper {
    private static String TAG = "BaseSerial";

    public BaseSerial(String sPort, int iBaudRate) {
        super(sPort, iBaudRate);
    }

    public abstract void onDataBack(String data);

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String recData = (String) msg.obj;
            onDataBack(recData);
        }
    };

    @Override
    protected void onDataReceived(String ComRecData) {
        Message message = new Message();
        message.obj = ComRecData;
        mHandler.sendMessage(message);
    }

    /**
     * Open serial port
     *
     * @param connectListener Serial connection status monitoring
     */
    public void openSerial(OnConnectListener connectListener) {
        try {
            super.open();
            Logger.getInstace().i(TAG, "Open the serial port successfully");
            if (connectListener != null) {
                connectListener.onSuccess();
            }
        } catch (SecurityException e) {
            Logger.getInstace().e(TAG, "Failed to open the serial port: no serial port read/write permission!");
            if (connectListener != null) {
                connectListener.onError("Failed to open the serial port: no serial port read/write permission!");
            }
        } catch (IOException e) {
            Logger.getInstace().e(TAG, "Failed to open serial port: unknown error!");
            if (connectListener != null) {
                connectListener.onError("Failed to open serial port: unknown error!");
            }
        } catch (InvalidParameterException e) {
            Logger.getInstace().e(TAG, "Failed to open the serial port: the parameter is wrong!");
            if (connectListener != null) {
                connectListener.onError("Failed to open the serial port: the parameter is wrong!");
            }
        }
    }

    /**
     * Send HEX data
     *
     * @param sHex
     */
    public void sendHex(String sHex) {
        byte[] bOutArray = SerialDataUtils.HexToByteArr(sHex);
        Message msg = Message.obtain();
        msg.obj = bOutArray;
        addWaitMessage(msg);
    }

    /**
     * Send string data
     *
     * @param sTxt
     */
    public void sendTxt(String sTxt) {
        byte[] bOutArray = new byte[0];
        try {
            bOutArray = sTxt.getBytes("GB18030");
            Message msg = Message.obtain();
            msg.obj = bOutArray;
            addWaitMessage(msg);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Send byte data
     *
     * @param bOutArray
     */
    public void sendByteArray(byte[] bOutArray) {
        Message msg = Message.obtain();
        msg.obj = bOutArray;
        addWaitMessage(msg);
    }

    /**
     * is show log
     *
     * @param isShowLog
     */
    public void setShowLog(boolean isShowLog) {
        Logger.SHOW_LOG = isShowLog;
    }
}
