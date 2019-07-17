package com.vi.vioserial.bean;

/**
 * @author Vi
 * @date 2019-07-16 16:09
 * @e-mail cfop_f2l@163.com
 */

public class CarVersion {
    private String mtype;
    private int add;
    private String ver;
    private String readme;

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

    public String getVer() {
        return ver;
    }

    public void setVer(String ver) {
        this.ver = ver;
    }

    public String getReadme() {
        return readme;
    }

    public void setReadme(String readme) {
        this.readme = readme;
    }

    @Override
    public String toString() {
        return "CarVersion{" +
                "mtype='" + mtype + '\'' +
                ", add=" + add +
                ", ver='" + ver + '\'' +
                ", readme='" + readme + '\'' +
                '}';
    }
}
