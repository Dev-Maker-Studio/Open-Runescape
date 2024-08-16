package com.jagex.game.runetek5.sound.player.area;

public class AreaSoundRange {

    public final int x;

    public final int z;

    public final int near;

    public final int far;

    public final int range;

    public AreaSoundRange(int x, int z, int near, int far) {
        this.x = x;
        this.z = z;
        this.near = near;
        this.far = far;
        this.range = far - near;
    }

}
