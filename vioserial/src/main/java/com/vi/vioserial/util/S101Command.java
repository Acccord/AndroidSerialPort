package com.vi.vioserial.util;

/**
 * @author Vi
 * @date 2019-07-17 17:40
 * @e-mail cfop_f2l@163.com
 */

public class S101Command {

    public static final String H01 = "01";

    public static final String H03 = "03";

    public static final String H04 = "04";

    public static final String H05 = "05";

    public static final String H07 = "07";

    public static final String H08 = "08";

    public static final String H09 = "09";

    public static final String H1A = "1A";

    public static final String H1B = "1B";

    public static final String H1C = "1C";

    public static final String FF = "FF";

    public static String getSerialDataHEX(String pcbAdd, String command) {
        return getSerialDataHEX(pcbAdd, command, "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00");
    }

    public static String getSerialDataHEX(String pcbAdd, String command, String dataStr) {
        String fullCommand = "0" + pcbAdd + command + dataStr;
        fullCommand = fullCommand.trim().replaceAll(" ", "");
        fullCommand = fullCommand + addZero(fullCommand);
        fullCommand = checkData(fullCommand).toUpperCase();
        return fullCommand;
    }

    private static String checkData(String dataStr) {
        byte[] bytes = SerialDataUtils.HexToByteArr(dataStr);
        int data = CRC16Util.calcCrc16(bytes);
        return dataStr + CRC16Util.getCrc(data).toUpperCase();
    }

    private static String addZero(String str) {
        int length = 18 * 2 - str.length();
        if (length <= 0) {
            return "";
        }
        StringBuilder tempStr = new StringBuilder();
        for (int i = 0; i < length; i++) {
            tempStr.append("0");
        }
        return tempStr.toString();
    }
}
