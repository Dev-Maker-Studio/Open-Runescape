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
        @JvmField val PLAY_SONG = CutsceneActionType(18, 30)
        @JvmField val PLAY_SYNTH = CutsceneActionType(23, 31)
        @JvmField val PLAY_JINGLE = CutsceneActionType(5, 32)
        @JvmField val PLAY_VORBIS = CutsceneActionType(6, 33)
        @JvmField val FADE_SCREEN = CutsceneActionType(30, 40)
        @JvmField val TILE_MESSAGE = CutsceneActionType(17, 41)
        @JvmField val POSITIONED_GFX = CutsceneActionType(14, 42)
        @JvmField val UNUSED_43 = CutsceneActionType(21, 43)
        @JvmField val PROJECTILE_HOMING = CutsceneActionType(28, 50)
        @JvmField val PROJECTILE_TO_COORD = CutsceneActionType(24, 51)
        @JvmField val PROJECTILE_FROM_COORD = CutsceneActionType(15, 52)
        @JvmField val PROJECTILE_BETWEEN_COORDS = CutsceneActionType(19, 53)
        @JvmField val SET_VARIABLE = CutsceneActionType(10, 60)
        @JvmField val SET_BIT_VARIABLE = CutsceneActionType(27, 61)
        @JvmField val EXECUTE_SCRIPT = CutsceneActionType(22, 70)
        @JvmField val FINISHED = CutsceneActionType(16, 255)

        @JvmStatic
        fun values(): Array<CutsceneActionType> {
            return arrayOf(
                UNUSED7, SET_VARIABLE, ANIMATE_OBJECT, FADE_SCREEN, BASIC_MOVEMENT, PROJECTILE_HOMING, PLAY_SYNTH,
                DESTROY_OBJECT, APPLY_HITMARK, DIRECT_CAMERA_MOVEMENT, UNCENTERED_CAMERA_MOVEMENT, RESET_CUTSCENE_ENTITY,
                ROTATE_CUTSCENE_ENTITY, POSITIONED_GFX, UNUSED3, SET_HINT_DETAILS, PLAY_VORBIS, EXECUTE_SCRIPT,
                PROJECTILE_TO_COORD, PLAY_SONG, ENTITY_GFX, TILE_MESSAGE, SET_BIT_VARIABLE, MOVEMENT, FINISHED,
                PLAY_JINGLE, REPLACE_OBJECT, PROJECTILE_BETWEEN_COORDS, ANIMATE_MOVEMENT, UNUSED_43, PROJECTILE_FROM_COORD
            )
        }
    }
}
