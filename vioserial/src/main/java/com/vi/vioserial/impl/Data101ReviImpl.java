package com.vi.vioserial.impl;

import android.text.TextUtils;

import com.vi.vioserial.bean.DataStatus;
import com.vi.vioserial.bean.DataVersion;
import com.vi.vioserial.listener.OnSerialDataParse;
import com.vi.vioserial.listener.OnVioDataListener;
import com.vi.vioserial.util.CRC16Util;
import com.vi.vioserial.util.SerialDataUtils;

import java.util.List;

/**
 * @author Vi
 * @date 2019-07-17 17:28
 * @e-mail cfop_f2l@163.com
 */

public class Data101ReviImpl implements OnSerialDataParse {

    @Override
    public void parseData(String data, List<OnVioDataListener> mVioDataListener) {
        if (TextUtils.isEmpty(data) || data.length() < 40) {
            for (int i = mVioDataListener.size() - 1; i >= 0; i--) {
                mVioDataListener.get(i).error("-1");
            }
            return;
        }

        byte[] bytes = SerialDataUtils.HexToByteArr(data.substring(0, 36));
        int crcData = CRC16Util.calcCrc16(bytes);
        if (!CRC16Util.getCrc(crcData).toUpperCase().equals(data.substring(36, 40))) {
            for (int i = mVioDataListener.size() - 1; i >= 0; i--) {
                mVioDataListener.get(i).error("-2");
            }
            return;
        }

        data = data.toUpperCase();

        if (data.startsWith("0001")) {
            for (int i = mVioDataListener.size() - 1; i >= 0; i--) {
                mVioDataListener.get(i).version(parseVersion(data));
            }
        } else if (data.startsWith("0003")) {
            for (int i = mVioDataListener.size() - 1; i >= 0; i--) {
                mVioDataListener.get(i).springResult(parseSpring(data));
            }
        } else if (data.startsWith("0005")) {
            for (int i = mVioDataListener.size() - 1; i >= 0; i--) {
                mVioDataListener.get(i).openResult(parseOpenResult(data));
            }
        } else if (data.startsWith("001B")) {
            for (int i = mVioDataListener.size() - 1; i >= 0; i--) {
                mVioDataListener.get(i).lightResult(parseLight(data));
            }
        }
    }

    private DataVersion parseVersion(String dataStr) {
        dataStr = dataStr.trim();
        dataStr = SerialDataUtils.hexStringToString(dataStr.substring(4, 28));
        DataVersion versionBean = new DataVersion();
        versionBean.setVersion(dataStr);
        return versionBean;
    }

    private DataStatus parseSpring(String dataStr) {
        String Z1 = dataStr.substring(4, 6);
        String Z2 = dataStr.substring(6, 8);
        String Z3 = dataStr.substring(8, 10);
        String Z4_5 = dataStr.substring(10, 14);
        String Z6_7 = dataStr.substring(14, 18);
        String Z8_9 = dataStr.substring(18, 22);
        String Z10 = dataStr.substring(22, 24);

        DataStatus dataStatus = new DataStatus();
        int channel = Integer.parseInt(Z3, 16);
        dataStatus.setChannelResult(channel);
        int time = Integer.parseInt(Z10, 16);
        dataStatus.setLightResult(time);

        return dataStatus;
    }

    private int parseOpenResult(String dataStr) {
        String Z1 = dataStr.substring(4, 6);
        return Integer.parseInt(Z1, 16);
    }

    private int parseLight(String dataStr) {
        String Z1 = dataStr.substring(4, 6);
        return Integer.parseInt(Z1, 16);
    }
}
