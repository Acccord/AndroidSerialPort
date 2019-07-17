package com.vi.vioserial.util;

/**
 * @author Vi
 * @date 2019-07-17 17:41
 * @e-mail cfop_f2l@163.com
 */

public class S427Command {

    private static String TAG = "S427Command";

    public static final String PCBADD = "PCBAdd";

    public static final String READ_VERSION = "Readversion";

    public static final String READ_TEMP = "ReadTemp";

    public static final String READ_SWITCH1 = "Readswitch1";

    public static final String READ_SWITCH2 = "Readswitch2";

    public static final String READ_LIGHT_SENSOR = "ReadLightSensor";

    public static final String READ_CHANNEL_OPENSTATUS = "ChannelopenStatus";

    public static final String READ_RELAY1 = "Relay1";

    public static final String READ_RELAY2 = "Relay2";

    public static final String READ_RELAY3 = "Relay3";

    public static final String READ_CHANNEL_OPEN = "Channelopen";

    public static final String MDB_COIN_STATUS = "MDBCoinstatus";

    public static final String MDB_BILL_STATUS = "MDBbillstatus";

    public static final String MDB_COIN = "MDBcoin";

    public static final String MDB_BILL = "MDBbill";

    public static final String MDB_COIN_RETURN = "MDBCoinReturn";

    public static final String MDB_BILL_RETURN = "MDBBillReturn";

    public static final String MDB_COIN_RETURN_READ = "MDBCoinReturnRead";

    public static final String READ_MONEY = "Readmoney";

    public static final String MDB_MONEY_CLEAR = "MDBmoneyClear";

    public static final String AYTO_RST = "AutoRst";

    public static final String MDBmoneyEscrow = "MDBmoneyEscrow";

    public static String getSerialDataHEX(String pcbAdd, String command, String type) {
        String fullCommand = PCBADD + "=" + pcbAdd + "," + command + "=" + type;
        fullCommand = fullCommand.replaceAll(" ", "");
        String dataStr = SerialDataUtils.stringToHexString(fullCommand);
        dataStr = addHeadAddFoot(dataStr);
        return dataStr;
    }

    private static String addHeadAddFoot(String dataStr) {
        String head = "232A";
        String foot = "0D0A";
        return head + dataStr + foot;
    }
}
