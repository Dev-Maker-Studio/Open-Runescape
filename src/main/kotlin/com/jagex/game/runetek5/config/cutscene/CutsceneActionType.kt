package com.jagex.game.runetek5.config.cutscene

class CutsceneActionType(
    @JvmField val action: Int,
    @JvmField val id: Int
) {

    companion object {
        @JvmField val UNCENTERED_CAMERA_MOVEMENT = CutsceneActionType(8, 0)
        @JvmField val DIRECT_CAMERA_MOVEMENT = CutsceneActionType(11, 1)
        @JvmField val UNUSED3 = CutsceneActionType(3, 2)
        @JvmField val UNUSED7 = CutsceneActionType(7, 3)
        @JvmField val MOVEMENT = CutsceneActionType(4, 10)
        @JvmField val RESET_CUTSCENE_ENTITY = CutsceneActionType(1, 11)
        @JvmField val BASIC_MOVEMENT = CutsceneActionType(9, 12)
        @JvmField val SET_HINT_DETAILS = CutsceneActionType(12, 13)
        @JvmField val ANIMATE_MOVEMENT = CutsceneActionType(26, 14)
        @JvmField val APPLY_HITMARK = CutsceneActionType(2, 15)
        @JvmField val ROTATE_CUTSCENE_ENTITY = CutsceneActionType(29, 16)
        @JvmField val ENTITY_GFX = CutsceneActionType(25, 17)
        @JvmField val REPLACE_OBJECT = CutsceneActionType(20, 20)
        @JvmField val DESTROY_OBJECT = CutsceneActionType(0, 21)
        @JvmField val ANIMATE_OBJECT = CutsceneActionType(13, 22)
        @JvmField val aClass4114940 = CutsceneActionType(18, 30)
        @JvmField val aClass4114941 = CutsceneActionType(23, 31)
        @JvmField val aClass4114950 = CutsceneActionType(5, 32)
        @JvmField val aClass4114943 = CutsceneActionType(6, 33)
        @JvmField val aClass4114954 = CutsceneActionType(30, 40)
        @JvmField val aClass4114947 = CutsceneActionType(17, 41)
        @JvmField val aClass4114946 = CutsceneActionType(14, 42)
        @JvmField val aClass4114938 = CutsceneActionType(21, 43)
        @JvmField val aClass4114948 = CutsceneActionType(28, 50)
        @JvmField val aClass4114949 = CutsceneActionType(24, 51)
        @JvmField val aClass4114945 = CutsceneActionType(15, 52)
        @JvmField val aClass4114932 = CutsceneActionType(19, 53)
        @JvmField val aClass4114952 = CutsceneActionType(10, 60)
        @JvmField val aClass4114925 = CutsceneActionType(27, 61)
        @JvmField val aClass4114931 = CutsceneActionType(22, 70)
        @JvmField val aClass4114955 = CutsceneActionType(16, 255)

        @JvmStatic
        fun values(): Array<CutsceneActionType> {
            return arrayOf(
                UNUSED7, aClass4114952, ANIMATE_OBJECT, aClass4114954, BASIC_MOVEMENT, aClass4114948, aClass4114941,
                DESTROY_OBJECT, APPLY_HITMARK, DIRECT_CAMERA_MOVEMENT, UNCENTERED_CAMERA_MOVEMENT, RESET_CUTSCENE_ENTITY,
                ROTATE_CUTSCENE_ENTITY, aClass4114946, UNUSED3, SET_HINT_DETAILS, aClass4114943, aClass4114931,
                aClass4114949, aClass4114940, ENTITY_GFX, aClass4114947, aClass4114925, MOVEMENT, aClass4114955,
                aClass4114950, REPLACE_OBJECT, aClass4114932, ANIMATE_MOVEMENT, aClass4114938, aClass4114945
            )
        }
    }
}
