package com.vi.vioserial.bean;

/**
 * @author Vi
 * @date 2019-07-17 17:22
 * @e-mail cfop_f2l@163.com
 */

public class BillBean {
    private int Billcapacity;
    private int MDBReturnN;
    private int Billfull;
    private int billErr;

    public int getBillcapacity() {
        return Billcapacity;
    }

    public void setBillcapacity(int billcapacity) {
        Billcapacity = billcapacity;
    }

    public int getMDBReturnN() {
        return MDBReturnN;
    }

    public void setMDBReturnN(int MDBReturnN) {
        this.MDBReturnN = MDBReturnN;
    }

    public int getBillfull() {
        return Billfull;
    }

    public void setBillfull(int billfull) {
        Billfull = billfull;
    }

    public int getBillErr() {
        return billErr;
    }

    public void setBillErr(int billErr) {
        this.billErr = billErr;
    }

    @Override
    public String toString() {
        return "BillStatusBean{" +
                "Billcapacity=" + Billcapacity +
                ", MDBReturnN=" + MDBReturnN +
                ", Billfull=" + Billfull +
                ", billErr=" + billErr +
                '}';
    }
}
