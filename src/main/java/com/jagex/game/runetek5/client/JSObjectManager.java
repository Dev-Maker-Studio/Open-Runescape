package com.jagex.game.runetek5.client;

public class JSObjectManager {

    public static final JSObjectManager PERIODIC = new JSObjectManager(9);
    public static final JSObjectManager ENTERING_GAME = new JSObjectManager(3);
    public static final JSObjectManager CS2_INS6416 = new JSObjectManager(7);
    public static final JSObjectManager CS2_INS6410 = new JSObjectManager(8);
    public static final JSObjectManager WINDOW_ADJUSTED = new JSObjectManager(10);
    public static final JSObjectManager CS2_INS6838 = new JSObjectManager(4);
    public static final JSObjectManager CS2_INS6680 = new JSObjectManager(15);
    public static final JSObjectManager CS2_INS6476 = new JSObjectManager(1);
    public static final JSObjectManager CS2_INS6867 = new JSObjectManager(5);
    public static final JSObjectManager LOGIN_PROMPT_BECOME_MEMBER = new JSObjectManager(11);
    public static final JSObjectManager SERVER_PACKET_URL = new JSObjectManager(13);
    public static final JSObjectManager SERVER_WINDOW_CALL = new JSObjectManager(6);
    public static final JSObjectManager CS2_INS6868 = new JSObjectManager(2);
    public static final JSObjectManager CS2_INS6869 = new JSObjectManager(12);
    public static final JSObjectManager CS2_INS6058 = new JSObjectManager(14);
    public static int anInt4185;
    public final int type;

    public JSObjectManager(int type) {
        this.type = type;
    }

    public String getCallName() {
        return "_" + this.type;
    }
}
