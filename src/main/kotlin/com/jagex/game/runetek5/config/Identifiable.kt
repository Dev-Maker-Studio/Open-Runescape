package com.jagex.game.runetek5.config

interface Identifiable {
    fun getValue(): Int

    companion object {
        @JvmStatic
        fun identify(identifiables: Array<out Identifiable>, value: Int): Identifiable? {
            for (identifiable in identifiables) {
                if (value == identifiable.getValue()) {
                    return identifiable
                }
            }
            return null
        }
    }
}
