package com.jagex.game.runetek5.preference.impl

import com.jagex.game.runetek5.preference.GamePreferences
import com.jagex.game.runetek5.preference.Preference
import com.jagex.game.runetek5.toolkit.AbstractToolkit

/**
 * Preference for selecting the graphics toolkit (Software, OpenGL, DirectX).
 *
 * @author Steven - 17/09/2023
 */
class ToolkitPreference : Preference {

    @JvmField var isDefault = false
    @JvmField var verifySettings = true

    constructor(value: Int, gamePreferences: GamePreferences) : super(value, gamePreferences)

    constructor(gamePreferences: GamePreferences) : super(gamePreferences)

    /**
     * Ensures the toolkit value is within valid range (0-5).
     */
    fun ensureValid() {
        if (value < 0 || value > 5) {
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
        return 2
    }

    override fun setValue(value: Int) {
        isDefault = false
        this.value = value
    }

    override fun canModify(value: Int): Int {
        return if (value == 3 && !AbstractToolkit.getNativeLibraryLoader().isJagdxLoaded()) {
            3
        } else {
            2
        }
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
}
