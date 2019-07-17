package com.vi.vioserial.listener;

/**
 * 串口链接状态监听
 *
 * @author Vi
 * @date 2019/4/4 6:46 PM
 * @e-mail cfop_f2l@163.com
 */

public interface OnConnectListener {

    void onSuccess();

    void onError(String errorData);

}
