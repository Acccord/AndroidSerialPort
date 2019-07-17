package com.vi.vioserial.bean;

import com.google.gson.annotations.SerializedName;

/**
 * @author Vi
 * @date 2019-07-17 17:27
 * @e-mail cfop_f2l@163.com
 */

public class LiftBean {
    private String mtype;
    private int Position;
    @SerializedName("switch")
    private int[] mSwitch;

    public String getMtype() {
        return mtype;
    }

    public void setMtype(String mtype) {
        this.mtype = mtype;
    }

    public int getPosition() {
        return Position;
    }

    public void setPosition(int position) {
        Position = position;
    }

    public int[] getmSwitch() {
        return mSwitch;
    }

    public void setmSwitch(int[] mSwitch) {
        this.mSwitch = mSwitch;
    }
}
