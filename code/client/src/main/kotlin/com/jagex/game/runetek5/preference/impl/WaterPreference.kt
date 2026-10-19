package com.jagex.game.runetek5.preference.impl

import com.jagex.game.runetek5.preference.GamePreferences
import com.jagex.game.runetek5.preference.Preference

/**
 *
 * @author Steven - 17/09/2023
 *
 */
class WaterPreference : Preference {

    constructor(value: Int, gamePreferences: GamePreferences) : super(value, gamePreferences)

    constructor(gamePreferences: GamePreferences) : super(gamePreferences)

    fun ensureValid() {
    }

    override fun getDefaultValue(): Int {
        return 1
    }

    override fun getValue(): Int {
        return super.value
    }

    override fun setValue(value: Int) {
        this.value = value
    }

    override fun canModify(value: Int): Int {
        if (value == ULTRA && !modernRenderer()) return 3
        return if (value != 0 && super.gamePreferences.goundBlending!!.getValue() != 1) 2 else 1
    }

    fun display(): Boolean {
        return true
    }

    fun highDetail(): Boolean = value >= HIGH

    fun waterShaders(): Boolean = value == ULTRA && modernRenderer()

    fun menuLevel(): Int = when {
        waterShaders() -> MENU_ULTRA
        highDetail() -> MENU_HIGH
        else -> MENU_LOW
    }

    private fun modernRenderer(): Boolean =
        super.gamePreferences.activeToolkit?.getValue() == ToolkitPreference.MODERN_OPENGL

    companion object {
        const val LOW = 0
        const val HIGH = 2
        const val ULTRA = 3
        const val MENU_LOW = 0
        const val MENU_HIGH = 1
        const val MENU_ULTRA = 2

        fun fromMenuLevel(level: Int): Int = when (level) {
            MENU_ULTRA -> ULTRA
            MENU_HIGH -> HIGH
            else -> LOW
        }
    }
}
