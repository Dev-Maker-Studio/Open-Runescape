package com.jagex.game.runetek5.rendering;

public class RenderInfo {

    public final int vendor;
    public final String name;
    public final int version;
    public final String device;
    public final long driverVersion;

    public RenderInfo(int vendor, String name, int version, String description, long driverVersion) {
        this.vendor = vendor;
        this.name = name;
        this.version = version;
        this.device = description;
        this.driverVersion = driverVersion;
    }
}
