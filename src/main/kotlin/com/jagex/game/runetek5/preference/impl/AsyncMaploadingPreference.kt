package com.jagex.game.runetek5.preference.impl

import com.jagex.game.launch.Loader
import com.jagex.game.runetek5.preference.GamePreferences
import com.jagex.game.runetek5.preference.Preference

/**
 * Preference for enabling asynchronous map loading on multi-core systems.
 * Requires OpenGL toolkit and at least 2 CPU cores.
 *
 * @author Steven - 17/09/2023
 */
class AsyncMaploadingPreference : Preference {

    constructor(value: Int, gamePreferences: GamePreferences) : super(value, gamePreferences)

    constructor(gamePreferences: GamePreferences) : super(gamePreferences)

    /**
     * Ensures the async map loading setting is valid based on system capabilities.
     */
    fun ensureValid() {
        var value = gamePreferences.toolkit!!.getValue()
        if (value != 3 && value != 5) {
            value = 0
        }

        if (gamePreferences.getProcessorSpecs().getProcessorCount() < 2) {
            value = 0
        }

        if (value != 0 && value != 1) {
            value = getDefaultValue()
        }
    }

    override fun getDefaultValue(): Int {
        return 0
    }

    override fun canModify(value: Int): Int {
        if (value == 0) {
            return 1
        }
        if (gamePreferences.getProcessorSpecs().getProcessorCount() < 2) {
            return 3
        }
        val toolkit = gamePreferences.toolkit!!.getValue()
        return if (toolkit != 3 && toolkit != 5) 3 else 1
    }

    override fun setValue(value: Int) {
        this.value = value
    }

    override fun getValue(): Int {
        return if (Loader.MULTI_THREADED_MAPLOADING) 1 else value
    }
}
