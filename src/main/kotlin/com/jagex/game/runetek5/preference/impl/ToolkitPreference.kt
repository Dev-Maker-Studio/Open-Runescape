package com.jagex.game.runetek5.preference.impl

import com.jagex.game.runetek5.preference.GamePreferences
import com.jagex.game.runetek5.preference.Preference

/**
 * Preference for selecting the graphics toolkit: 0 safe mode, 1 legacy OpenGL, 3 modern OpenGL
 * (the id DirectX had; the cache's option bitmasks and scripts already know it as a hardware
 * renderer). Software (2) and DirectX no longer exist in this client.
 *
 * @author Steven - 17/09/2023
 */
class ToolkitPreference : Preference {

    @JvmField var isDefault = false
    @JvmField var verifySettings = true

    constructor(value: Int, gamePreferences: GamePreferences) : super(value, gamePreferences)

    constructor(gamePreferences: GamePreferences) : super(gamePreferences)

    fun ensureValid() {
        if (value !in AVAILABLE) {
            value = getDefaultValue()
        }
    }

    /**
     * Checks if the toolkit preference is visible to the user.
     *
     * @return true if visible
     */
    fun visible(): Boolean {
        return true
    }

    override fun getDefaultValue(): Int {
        isDefault = true
        return LEGACY_OPENGL
    }

    override fun setValue(value: Int) {
        isDefault = false
        this.value = value
    }

    override fun canModify(value: Int): Int {
        return if (value in AVAILABLE) 2 else 3
    }

    override fun getValue(): Int {
        return value
    }

    /**
     * Checks if settings should be verified before applying.
     *
     * @return true if verification is enabled
     */
    fun verifySettings(): Boolean {
        return verifySettings
    }

    /**
     * Sets whether to verify settings before applying.
     *
     * @param verifySettings true to enable verification
     */
    fun setVerifySettings(verifySettings: Boolean) {
        this.verifySettings = verifySettings
    }

    companion object {
        const val SAFE_MODE = 0
        const val LEGACY_OPENGL = 1
        const val MODERN_OPENGL = 3

        @JvmField val AVAILABLE = setOf(SAFE_MODE, LEGACY_OPENGL, MODERN_OPENGL)
    }
}
