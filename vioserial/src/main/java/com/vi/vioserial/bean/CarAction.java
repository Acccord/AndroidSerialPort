package com.vi.vioserial.bean;

/**
 * @author Vi
 * @date 2019-07-16 16:09
 * @e-mail cfop_f2l@163.com
 */

public class CarAction {
    private String mtype;
    private int add;

    public String getMtype() {
        return mtype;
    }

    public void setMtype(String mtype) {
        this.mtype = mtype;
    }

    public int getAdd() {
        return add;
    }

    public void setAdd(int add) {
        this.add = add;
    }

    @Override
    public String toString() {
        return "CarStatus{" +
                "mtype='" + mtype + '\'' +
                ", add=" + add +
                '}';
    }
}
