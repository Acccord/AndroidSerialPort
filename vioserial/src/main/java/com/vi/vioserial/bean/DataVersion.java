package com.vi.vioserial.bean;

/**
 * @author Vi
 * @date 2019-07-17 17:22
 * @e-mail cfop_f2l@163.com
 */

public class DataVersion {
    private String Version;
    private int Testmode;

    public String getVersion() {
        return Version;
    }

    public void setVersion(String version) {
        Version = version;
    }

    public int getTestmode() {
        return Testmode;
    }

    public void setTestmode(int testmode) {
        Testmode = testmode;
    }

    @Override
    public String toString() {
        return "VersionBean{" +
                "Version='" + Version + '\'' +
                ", Testmode=" + Testmode +
                '}';
    }
}
