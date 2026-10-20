package com.jagex.game.runetek5.preference.impl

import com.jagex.game.runetek5.preference.GamePreferences
import com.jagex.game.runetek5.preference.Preference

class ToolkitPreference : Preference {

    @JvmField var isDefault = false
    @JvmField var verifySettings = true

    constructor(value: Int, gamePreferences: GamePreferences) : super(MODERN_OPENGL, gamePreferences)

    constructor(gamePreferences: GamePreferences) : super(gamePreferences)

    fun ensureValid() {
        value = MODERN_OPENGL
    }

    fun visible(): Boolean = false

    override fun getDefaultValue(): Int {
        isDefault = true
        return MODERN_OPENGL
    }

    override fun setValue(value: Int) {
        isDefault = false
        this.value = MODERN_OPENGL
    }

    override fun canModify(value: Int): Int = if (value == MODERN_OPENGL) 2 else 3

    override fun getValue(): Int = MODERN_OPENGL

    fun verifySettings(): Boolean = verifySettings

    fun setVerifySettings(verifySettings: Boolean) {
        this.verifySettings = verifySettings
    }

    companion object {
        const val MODERN_OPENGL = 3
    }
}
