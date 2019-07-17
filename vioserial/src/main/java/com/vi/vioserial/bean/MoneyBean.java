package com.vi.vioserial.bean;

import java.io.Serializable;

/**
 * @author Vi
 * @date 2019-07-17 17:27
 * @e-mail cfop_f2l@163.com
 */

public class MoneyBean implements Serializable {
    private int MDBcoin;
    private int MDBbill;
    private int Coinmoney;
    private int billmoney;
    private int BillEscrow;

    public int getMDBcoin() {
        return MDBcoin;
    }

    public void setMDBcoin(int MDBcoin) {
        this.MDBcoin = MDBcoin;
    }

    public int getMDBbill() {
        return MDBbill;
    }

    public void setMDBbill(int MDBbill) {
        this.MDBbill = MDBbill;
    }

    public int getCoinmoney() {
        return Coinmoney;
    }

    public void setCoinmoney(int coinmoney) {
        Coinmoney = coinmoney;
    }

    public int getBillmoney() {
        return billmoney;
    }

    public void setBillmoney(int billmoney) {
        this.billmoney = billmoney;
    }

    public int getBillEscrow() {
        return BillEscrow;
    }

    public void setBillEscrow(int billEscrow) {
        BillEscrow = billEscrow;
    }

    @Override
    public String toString() {
        return "ReadMoneyBean{" +
                "MDBcoin=" + MDBcoin +
                ", MDBbill=" + MDBbill +
                ", Coinmoney=" + Coinmoney +
                ", billmoney=" + billmoney +
                '}';
    }
}
