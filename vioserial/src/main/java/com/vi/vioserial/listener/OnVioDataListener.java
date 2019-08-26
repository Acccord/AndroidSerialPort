package com.vi.vioserial.listener;

import com.vi.vioserial.bean.BillBean;
import com.vi.vioserial.bean.CoinBean;
import com.vi.vioserial.bean.DataStatus;
import com.vi.vioserial.bean.DataVersion;
import com.vi.vioserial.bean.MoneyBean;
import com.vi.vioserial.bean.RestBean;

/**
 * @author Vi
 * @date 2019-07-17 17:52
 * @e-mail cfop_f2l@163.com
 */

public interface OnVioDataListener {

    void version(DataVersion dataVersion);

    void springResult(DataStatus dataStatus);

    void openResult(int result);

    void lightResult(int result);

    void coin(CoinBean coinBean);

    void bill(BillBean billBean);

    void money(MoneyBean moneyBean);

    void returnCoin(RestBean returnCoinBean);

    void error(String code);

}
