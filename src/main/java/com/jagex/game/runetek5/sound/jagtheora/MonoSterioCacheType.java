package com.jagex.game.runetek5.sound.jagtheora;

public class MonoSterioCacheType {

    public static final MonoSterioCacheType TIMED = new MonoSterioCacheType(1);
    static MonoSterioCacheType COUNTER = new MonoSterioCacheType(0);
    
    /**
     * 0 = Counter
     * 1 = Timer
     */
    public final int type;

    public MonoSterioCacheType(int type) {
        this.type = type;
    }
}
