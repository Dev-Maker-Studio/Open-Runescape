package com.jagex.game.runetek5.config.cutscene;

public class CutsceneActionType {

    public static final CutsceneActionType UNCENTERED_CAMERA_MOVEMENT = new CutsceneActionType(8, 0);
    public static final CutsceneActionType DIRECT_CAMERA_MOVEMENT = new CutsceneActionType(11, 1);
    public static final CutsceneActionType UNUSED3 = new CutsceneActionType(3, 2);
    public static final CutsceneActionType UNUSED7 = new CutsceneActionType(7, 3);
    public static final CutsceneActionType MOVEMENT = new CutsceneActionType(4, 10);
    public static final CutsceneActionType RESET_CUTSCENE_ENTITY = new CutsceneActionType(1, 11);
    public static final CutsceneActionType BASIC_MOVEMENT = new CutsceneActionType(9, 12);
    public static final CutsceneActionType SET_HINT_DETAILS = new CutsceneActionType(12, 13);
    public static final CutsceneActionType ANIMATE_MOVEMENT = new CutsceneActionType(26, 14);
    public static final CutsceneActionType APPLY_HITMARK = new CutsceneActionType(2, 15);
    public static final CutsceneActionType ROTATE_CUTSCENE_ENTITY = new CutsceneActionType(29, 16);
    public static final CutsceneActionType ENTITY_GFX = new CutsceneActionType(25, 17);
    public static final CutsceneActionType REPLACE_OBJECT = new CutsceneActionType(20, 20);
    public static final CutsceneActionType DESTROY_OBJECT = new CutsceneActionType(0, 21);
    public static final CutsceneActionType ANIMATE_OBJECT = new CutsceneActionType(13, 22);
    public static final CutsceneActionType aClass4114940 = new CutsceneActionType(18, 30);
    public static final CutsceneActionType aClass4114941 = new CutsceneActionType(23, 31);
    public static final CutsceneActionType aClass4114950 = new CutsceneActionType(5, 32);
    public static final CutsceneActionType aClass4114943 = new CutsceneActionType(6, 33);
    public static final CutsceneActionType aClass4114954 = new CutsceneActionType(30, 40);
    public static final CutsceneActionType aClass4114947 = new CutsceneActionType(17, 41);
    public static final CutsceneActionType aClass4114946 = new CutsceneActionType(14, 42);
    public static final CutsceneActionType aClass4114938 = new CutsceneActionType(21, 43);
    public static final CutsceneActionType aClass4114948 = new CutsceneActionType(28, 50);
    public static final CutsceneActionType aClass4114949 = new CutsceneActionType(24, 51);
    public static final CutsceneActionType aClass4114945 = new CutsceneActionType(15, 52);
    public static final CutsceneActionType aClass4114932 = new CutsceneActionType(19, 53);
    public static final CutsceneActionType aClass4114952 = new CutsceneActionType(10, 60);
    public static final CutsceneActionType aClass4114925 = new CutsceneActionType(27, 61);
    public static final CutsceneActionType aClass4114931 = new CutsceneActionType(22, 70);
    public static final CutsceneActionType aClass4114955 = new CutsceneActionType(16, 255);

    public final int action;
    public final int id;

    public CutsceneActionType(int action, int id) {
        this.action = action;
        this.id = id;
    }

    public static CutsceneActionType[] values() {
        return new CutsceneActionType[]{UNUSED7, aClass4114952, ANIMATE_OBJECT, aClass4114954, BASIC_MOVEMENT, aClass4114948, aClass4114941, DESTROY_OBJECT, APPLY_HITMARK, DIRECT_CAMERA_MOVEMENT, UNCENTERED_CAMERA_MOVEMENT, RESET_CUTSCENE_ENTITY, ROTATE_CUTSCENE_ENTITY, aClass4114946, UNUSED3, SET_HINT_DETAILS, aClass4114943, aClass4114931, aClass4114949, aClass4114940, ENTITY_GFX, aClass4114947, aClass4114925, MOVEMENT, aClass4114955, aClass4114950, REPLACE_OBJECT, aClass4114932, ANIMATE_MOVEMENT, aClass4114938, aClass4114945};
    }
}
