package com.vi.vioserial.listener;

/**
 * 串口通信数据监听
 *
 * @author Vi
 * @date 2019-07-17 17:38
 * @e-mail cfop_f2l@163.com
 */

public interface OnSerialDataListener {

    /**
     * 串口发送数据
     */
    void onSend(String hexData);

    /**
     * 串口接收数据
     */
    void onReceive(String hexData);

    /**
     * 串口接收数据(返回完整hex字符串)
     */
    void onReceiveFullData(String hexData);

}
