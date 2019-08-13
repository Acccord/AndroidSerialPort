package com.vi.vioserial.util;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.serialport.SerialPort;
import android.text.TextUtils;
import com.vi.vioserial.listener.OnSerialDataListener;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;

/**
 * Serial port auxiliary tool class
 *
 * @author Vi
 * @date 2019-07-17 17:43
 * @e-mail cfop_f2l@163.com
 */

public abstract class VioSerialHelper {
    private static String TAG = "VioSerial";
    private static String TAG_END = "0D0A";
    private SerialPort mSerialPort;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private ReadThread mReadThread;
    private String sPort;
    private int iBaudRate;
    private boolean _isOpen = false;
    private int iDelay = 300;

    private Handler mWorkHandler;
    private HandlerThread mHandlerThread;

    private OnSerialDataListener mSerialDataListener = null;

    public VioSerialHelper(String sPort, int iBaudRate) {
        this.sPort = sPort;
        this.iBaudRate = iBaudRate;
    }

    public void setSerialDataListener(OnSerialDataListener serialDataListene) {
        this.mSerialDataListener = serialDataListene;
    }

    public void open() throws SecurityException, IOException, InvalidParameterException {
        mSerialPort = new SerialPort(new File(sPort), iBaudRate, 0);
        mOutputStream = mSerialPort.getOutputStream();
        mInputStream = mSerialPort.getInputStream();
        mReadThread = new ReadThread();
        mReadThread.start();

        mHandlerThread = new HandlerThread("handlerThread");
        mHandlerThread.start();
        mWorkHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                byte[] sendData = (byte[]) msg.obj;
                send(sendData);

                try {
                    Thread.sleep(iDelay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        _isOpen = true;
    }

    public void close() {
        try {
            if(mInputStream!=null){
                mInputStream.close();
            }
            if(mOutputStream!=null){
                mOutputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (mReadThread != null) {
            mReadThread.interrupt();
        }
        if (mSerialPort != null) {
            mSerialPort.close();
            mSerialPort = null;
        }
        if (mHandlerThread != null) {
            mHandlerThread.quit();
            mHandlerThread = null;
        }
        _isOpen = false;
    }

    protected void addWaitMessage(Message msg) {
        mWorkHandler.sendMessage(msg);
    }

    private void send(byte[] bOutArray) {
        if (mWorkHandler == null) {
            return;
        }
        if (!_isOpen) {
            return;
        }

        try {
            mOutputStream.write(bOutArray);

            if (mSerialDataListener != null) {
                String hexString = SerialDataUtils.ByteArrToHex(bOutArray).trim();
                mSerialDataListener.onSend(hexString);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ReadThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                try {
                    if (mInputStream == null) return;
                    byte[] buffer = new byte[5];
                    int im = mInputStream.available();
                    if (im > 0) {
                        buffer = new byte[im];
                        int size = mInputStream.read(buffer);
                        String hexString = SerialDataUtils.ByteArrToHex(buffer).trim();

                        if (mSerialDataListener != null) {
                            mSerialDataListener.onReceive(hexString);
                        }
                        parseData(hexString);
                    } else {
                        parseData(TAG_END);
                    }

                    try {
                        Thread.sleep(30);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }

    private String mFullData = "";

    private void parseData(String tempData) {
        if (tempData.equals(TAG_END) && TextUtils.isEmpty(mFullData)) {
            return;
        }

        if (tempData.equals(TAG_END) && !TextUtils.isEmpty(mFullData)) {
            String trimData = mFullData.trim().replaceAll(" ", "");
            if (mSerialDataListener != null) {
                mSerialDataListener.onReceiveFullData(trimData);
            }
            onDataReceived(trimData);
            mFullData = "";
            return;
        }
        mFullData += tempData;
    }

    public int getBaudRate() {
        return iBaudRate;
    }

    public String getPort() {
        return sPort;
    }

    public boolean isOpen() {
        return _isOpen;
    }

    public void setDelay(int delay) {
        iDelay = delay;
    }

    protected abstract void onDataReceived(String ComRecData);
}