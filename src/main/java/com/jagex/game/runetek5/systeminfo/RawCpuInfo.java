package com.jagex.game.runetek5.systeminfo;

public class RawCpuInfo {

    public final int cpuIdLeafNum;

    public final int resultEAX;

    public final int resultEBX;

    public final int resultECX;

    public final int resultEDX;

    public RawCpuInfo(int i_1, int i_2, int i_3, int i_4, int i_5) {
        this.cpuIdLeafNum = i_1;
        this.resultEAX = i_2;
        this.resultEBX = i_3;
        this.resultECX = i_4;
        this.resultEDX = i_5;
    }

    public int hashCode() {
        return this.cpuIdLeafNum;
    }
}
