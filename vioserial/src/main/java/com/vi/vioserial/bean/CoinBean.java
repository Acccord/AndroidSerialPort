package com.vi.vioserial.bean;

/**
 * @author Vi
 * @date 2019-07-17 17:27
 * @e-mail cfop_f2l@163.com
 */

public class CoinBean {
    private int Coin50;
    private int Coin100;
    private int CoinErr;

    public int getCoin50() {
        return Coin50;
    }

    public void setCoin50(int coin50) {
        Coin50 = coin50;
    }

    public int getCoin100() {
        return Coin100;
    }

    public void setCoin100(int coin100) {
        Coin100 = coin100;
    }

    public int getCoinErr() {
        return CoinErr;
    }

    public void setCoinErr(int coinErr) {
        CoinErr = coinErr;
    }

    @Override
    public String toString() {
        return "CoinStatusBean{" +
                "Coin50=" + Coin50 +
                ", Coin100=" + Coin100 +
                ", CoinErr=" + CoinErr +
                '}';
    }
}
