package com.vi.vioserial.impl;

import com.vi.vioserial.listener.OnSerialDataSend;
import com.vi.vioserial.util.S427Command;

/**
 * @author Vi
 * @date 2019-07-17 17:33
 * @e-mail cfop_f2l@163.com
 */

public class Data427SendImpl implements OnSerialDataSend {

    @Override
    public String OnReadVersion() {
        return S427Command.getSerialDataHEX("1", S427Command.READ_VERSION, "1");
    }

    @Override
    public String OnOpenChanel(String pcbAdd, String channel, int motorType, int lightType) {
        return S427Command.getSerialDataHEX(pcbAdd, S427Command.READ_CHANNEL_OPEN, channel);
    }

    @Override
    public String OnReadSpring(int lightType) {
        if (lightType == 0) {
            return S427Command.getSerialDataHEX("1", S427Command.READ_CHANNEL_OPENSTATUS, "1");
        } else {
            return S427Command.getSerialDataHEX("1", S427Command.READ_LIGHT_SENSOR, lightType + "");
        }
    }

    @Override
    public String OnOpenLock(int type) {
        if (type == 0) {
            type = 2;
        }
        return S427Command.getSerialDataHEX("1", S427Command.READ_RELAY1, type + "");
    }

    @Override
    public String OnRestartSerial() {
        return S427Command.getSerialDataHEX("1", S427Command.READ_RELAY3, "1");
    }

    @Override
    public String OnAppStart() {
        return S427Command.getSerialDataHEX("1", S427Command.AYTO_RST, "1");
    }

    @Override
    public String OnReturnCoin(int coin) {
        return S427Command.getSerialDataHEX("1", S427Command.MDB_COIN_RETURN, coin + "");
    }

    @Override
    public String OnReturnBill(int count) {
        return S427Command.getSerialDataHEX("1", S427Command.MDB_BILL_RETURN, count + "");
    }

    @Override
    public String OnChangeCoin(int coinStatus) {
        return S427Command.getSerialDataHEX("1", S427Command.MDB_COIN, coinStatus + "");
    }

    @Override
    public String OnChangeBill(int billStatus) {
        return S427Command.getSerialDataHEX("1", S427Command.MDB_BILL, billStatus + "");
    }

    @Override
    public String OnClearMoney() {
        return S427Command.getSerialDataHEX("1", S427Command.MDB_MONEY_CLEAR, "1");
    }

    @Override
    public String OnReadCoin() {
        return S427Command.getSerialDataHEX("1", S427Command.MDB_COIN_STATUS, "1");
    }

    @Override
    public String OnReadBill() {
        return S427Command.getSerialDataHEX("1", S427Command.MDB_BILL_STATUS, "1");
    }

    @Override
    public String OnChangeTempBill(int status) {
        return S427Command.getSerialDataHEX("1", S427Command.MDBmoneyEscrow, status + "");
    }

    @Override
    public String OnReadMoney() {
        return S427Command.getSerialDataHEX("1", S427Command.READ_MONEY, "1");
    }

}
