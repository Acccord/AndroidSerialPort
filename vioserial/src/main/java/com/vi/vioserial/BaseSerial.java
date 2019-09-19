package com.vi.vioserial;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;

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

    public BaseSerial() {
        super();
    }

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

    @Override
    public void close() {
        super.close();
        if (mHandler != null) {
            mHandler = null;
        }
    }

    /**
     * 打开串口
     * Open serial port
     *
     * @return 0:success 成功
     * -1：无法打开串口：没有串口读/写权限！
     * -2：无法打开串口：未知错误！
     * -3：无法打开串口：参数错误！
     */
    public int openSerial() {
        try {
            super.open();
            Logger.getInstace().i(TAG, "Open the serial port successfully");
            return 0;
        } catch (SecurityException e) {
            Logger.getInstace().e(TAG, "Failed to open the serial port: no serial port read/write permission!");
            return -1;
        } catch (IOException e) {
            Logger.getInstace().e(TAG, "Failed to open serial port: unknown error!");
            return -2;
        } catch (InvalidParameterException e) {
            Logger.getInstace().e(TAG, "Failed to open the serial port: the parameter is wrong!");
            return -3;
        }
    }

    /**
     * Send HEX data
     *
     * @param sHex hex data
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
     * @param sTxt string data
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
     * @param bOutArray byte data
     */
    public void sendByteArray(byte[] bOutArray) {
        Message msg = Message.obtain();
        msg.obj = bOutArray;
        addWaitMessage(msg);
    }

    /**
     * is show log
     *
     * @param isShowLog true=show
     */
    public void setShowLog(boolean isShowLog) {
        Logger.SHOW_LOG = isShowLog;
    }
}
