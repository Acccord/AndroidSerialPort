package com.vi.vioserial.listener;

/**
 * @author Vi
 * @date 2019-07-17 17:52
 * @e-mail cfop_f2l@163.com
 */

public interface OnSerialDataSend {

    String OnReadVersion();

    String OnOpenChanel(String pcbAdd, String channel, int motorType, int lightType);

    String OnReadSpring(int lightType);

    String OnOpenLock(int type);

    String OnLightOpen();

    String OnLightRead();

    String OnLightClose();

    String OnRestartSerial();

    String OnAppStart();

    String OnReturnCoin(int coin);

    String OnReturnBill(int count);

    String OnChangeCoin(int coinStatus);

    String OnChangeBill(int billStatus);

    String OnClearMoney();

    String OnReadCoin();

    String OnReadBill();

    String OnChangeTempBill(int status);

    String OnReadMoney();

}
