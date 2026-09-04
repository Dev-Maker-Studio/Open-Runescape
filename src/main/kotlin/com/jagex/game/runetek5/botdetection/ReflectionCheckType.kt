package com.jagex.game.runetek5.botdetection

class ReflectionCheckType {
    companion object {
        const val GET_FIELD_VALUE: Int = 0
        const val SET_FIELD_VALUE: Int = 1
        const val GET_FIELD_MODIFIERS: Int = 2
        const val GET_METHOD_RESULT: Int = 3
        const val GET_METHOD_MODIFIERS: Int = 4
    }
}
