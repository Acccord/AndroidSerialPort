package com.vi.vioserial.bean;

import java.util.Arrays;

/**
 * @author Vi
 * @date 2019-07-16 16:09
 * @e-mail cfop_f2l@163.com
 */

public class CarStatus {
    private String mtype;
    private int add;
    private int[] swich;

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

    public int[] getSwich() {
        return swich;
    }

    public void setSwich(int[] swich) {
        this.swich = swich;
    }

    @Override
    public String toString() {
        return "CarStatus{" +
                "mtype='" + mtype + '\'' +
                ", add=" + add +
                ", swich=" + Arrays.toString(swich) +
                '}';
    }
}
