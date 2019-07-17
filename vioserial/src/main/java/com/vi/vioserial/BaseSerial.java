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
    private static String TAG = "VioSerial";

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
        //把数据抛到主线程
        Message message = new Message();
        message.obj = ComRecData;
        mHandler.sendMessage(message);
    }

    /**
     * 打开串口
     *
     * @param connectListener 串口链接监听
     */
    public void openSerial(OnConnectListener connectListener) {
        try {
            super.open();
            Logger.getInstace().i(TAG, "【单片机】打开串口成功");
            if (connectListener != null) {
                connectListener.onSuccess();
            }
        } catch (SecurityException e) {
            Logger.getInstace().e(TAG, "【单片机】打开串口失败:没有串口读/写权限!");
            if (connectListener != null) {
                connectListener.onError("打开串口失败:没有串口读/写权限!");
            }
        } catch (IOException e) {
            Logger.getInstace().e(TAG, "【单片机】打开串口失败:未知错误!");
            if (connectListener != null) {
                connectListener.onError("打开串口失败:未知错误!");
            }
        } catch (InvalidParameterException e) {
            Logger.getInstace().e(TAG, "【单片机】打开串口失败:参数错误!");
            if (connectListener != null) {
                connectListener.onError("打开串口失败:参数错误!");
            }
        }
    }

    /**
     * 发送HEX数据
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
     * 发送字符串数据
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
     * 发送字节数据
     *
     * @param bOutArray
     */
    public void sendByteArray(byte[] bOutArray) {
        Message msg = Message.obtain();
        msg.obj = bOutArray;
        addWaitMessage(msg);
    }

    /**
     * 是否显示log
     *
     * @param isShowLog
     */
    public void setShowLog(boolean isShowLog) {
        Logger.SHOW_LOG = isShowLog;
    }
}
