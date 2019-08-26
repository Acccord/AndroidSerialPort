package com.vi.vioserial.impl;

import com.vi.vioserial.listener.OnSerialDataSend;
import com.vi.vioserial.util.S101Command;

/**
 * @author Vi
 * @date 2019-07-17 17:30
 * @e-mail cfop_f2l@163.com
 */

public class Data101SendImpl implements OnSerialDataSend {

    @Override
    public String OnReadVersion() {
        return S101Command.getSerialDataHEX("1", S101Command.H01);
    }

    @Override
    public String OnOpenChanel(String pcbAdd, String channel, int motorType, int lightType) {
        int num = Integer.parseInt(channel);
        num = num - 110;
        if (num < 0) {
            num = 0;
        }
        channel = Integer.toHexString(num);
        if (channel.length() == 1) {
            channel = "0" + channel;
        }

        String Y2 = "0" + motorType;
        String Y3 = "0" + lightType;
        String dataStr = channel + Y2 + Y3;
        return S101Command.getSerialDataHEX("1", S101Command.H05, dataStr);
    }

    @Override
    public String OnReadSpring(int lightType) {
        return S101Command.getSerialDataHEX("1", S101Command.H03);
    }

    @Override
    public String OnOpenLock(int type) {
        String Y1 = "05";
        String Y2 = "0" + type;
        String dataStr = Y1 + Y2;
        return S101Command.getSerialDataHEX("1", S101Command.H08, dataStr);
    }

    @Override
    public String OnLightOpen() {
        return S101Command.getSerialDataHEX("1", S101Command.H1A);
    }

    @Override
    public String OnLightRead() {
        return S101Command.getSerialDataHEX("1", S101Command.H1B);
    }

    @Override
    public String OnLightClose() {
        return S101Command.getSerialDataHEX("1", S101Command.H1C);
    }

    //*********************** 427 **********************/

    @Override
    public String OnRestartSerial() {
        return "OnRestartSerial";
    }

    @Override
    public String OnAppStart() {
        return "OnAppStart";
    }

    @Override
    public String OnReturnCoin(int coin) {
        return "OnReturnCoin";
    }

    @Override
    public String OnReturnBill(int count) {
        return "OnReturnBill";
    }

    @Override
    public String OnChangeCoin(int coinStatus) {
        return "OnChangeCoin";
    }

    @Override
    public String OnChangeBill(int billStatus) {
        return "OnChangeBill";
    }

    @Override
    public String OnClearMoney() {
        return "OnClearMoney";
    }

    @Override
    public String OnReadCoin() {
        return "OnReadCoin";
    }

    @Override
    public String OnReadBill() {
        return "OnReadBill";
    }

    @Override
    public String OnChangeTempBill(int status) {
        return "OnChangeTempBill";
    }

    @Override
    public String OnReadMoney() {
        return "OnReadMoney";
    }

}
