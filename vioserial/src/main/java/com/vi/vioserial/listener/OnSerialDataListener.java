package com.vi.vioserial.listener;

/**
 * Serial communication data monitoring
 *
 * @author Vi
 * @date 2019-07-17 17:38
 * @e-mail cfop_f2l@163.com
 */

public interface OnSerialDataListener {

    /**
     * Data sent by serial port
     */
    void onSend(String hexData);

    /**
     * Data received by serial port
     */
    void onReceive(String hexData);

    /**
     * Data received by serial port (return complete hex string)
     */
    void onReceiveFullData(String hexData);

}
