package com.jagex.game.runetek5.systeminfo;

public class ProcessorSpecs {

    public final int maxMemory;
    public final int processorCount;
    public final boolean isArm;

    public ProcessorSpecs(int maxMemory, int processorCount, boolean isArm) {
        this.maxMemory = maxMemory;
        this.processorCount = processorCount;
        this.isArm = isArm;
    }

    public boolean isARM() {
        return this.isArm;
    }

    public int getProcessorCount() {
        return this.processorCount;
    }

    public int getMaxMemory() {
        return this.maxMemory;
    }
}
