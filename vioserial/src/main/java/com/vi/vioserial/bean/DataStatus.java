package com.vi.vioserial.bean;

/**
 * @author Vi
 * @date 2019-07-17 17:22
 * @e-mail cfop_f2l@163.com
 */

public class DataStatus {

    private int channelResult;
    private int lightResult;

    public int getChannelResult() {
        return channelResult;
    }

    public void setChannelResult(int channelResult) {
        this.channelResult = channelResult;
    }

    public int getLightResult() {
        return lightResult;
    }

    public void setLightResult(int lightResult) {
        this.lightResult = lightResult;
    }

    @Override
    public String toString() {
        return "DataStatus{" +
                "channelResult=" + channelResult +
                ", lightResult=" + lightResult +
                '}';
    }
}
