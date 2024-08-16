package com.jagex.game.runetek5.graphics.texture;

public class TextureState {

    public static final TextureState INITIALIZE = new TextureState(0);
    public static final TextureState PARTIAL_COPY_LAST = new TextureState(1);
    static TextureState UNUSED_T2 = new TextureState(2);
    static TextureState UNUSED_T3 = new TextureState(3);
    static TextureState UNUSED_T5 = new TextureState(5);
    static TextureState UNUSED_T4 = new TextureState(4);
    public final int type;

    public TextureState(int type) {
        this.type = type;
    }

}
