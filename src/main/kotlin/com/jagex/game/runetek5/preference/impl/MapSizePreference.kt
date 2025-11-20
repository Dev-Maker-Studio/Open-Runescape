package com.jagex.game.runetek5.preference.impl

import com.jagex.game.runetek5.preference.GamePreferences
import com.jagex.game.runetek5.preference.Preference
import com.jagex.game.runetek5.scene.MapSize

/**
 * Preference for controlling the visible map size based on available memory.
 *
 * @author Steven - 17/09/2023
 */
class MapSizePreference : Preference {

    constructor(gamePreferences: GamePreferences) : super(gamePreferences)

    constructor(value: Int, gamePreferences: GamePreferences) : super(value, gamePreferences)

    override fun setValue(value: Int) {
        this.value = value
    }

    override fun getDefaultValue(): Int {
        return MapSize.MAPSIZE_104_DEFAULT.id
    }

    /**
     * Checks if the user is allowed to modify map size based on memory.
     *
     * @return true if the user has enough memory (>= 245 MB)
     */
    fun allowSelection(): Boolean {
        val maxMemory = gamePreferences.getProcessorSpecs().getMaxMemory()
        return maxMemory >= 245
    }

    override fun canModify(value: Int): Int {
        val maxMemory = gamePreferences.getProcessorSpecs().getMaxMemory()
        return if (maxMemory < 245) {
            3
        } else if (value == MapSize.MAPSIZE_168_EXT3.id && maxMemory < 500) {
            3
        } else {
            1
        }
    }

    override fun getValue(): Int {
        return value
    }

    /**
     * Ensures the map size value is valid based on available memory constraints.
     * Downgrades the map size if insufficient memory is available.
     */
    fun ensureValid() {
        val maxMemory = gamePreferences.getProcessorSpecs().getMaxMemory()
        if (maxMemory < 245) {
            value = MapSize.MAPSIZE_104_DEFAULT.id
        }

        if (value == MapSize.MAPSIZE_168_EXT3.id && maxMemory < 500) {
            value = MapSize.MAPSIZE_136_EXT2.id
        }

        if (value < MapSize.MAPSIZE_104_DEFAULT.id || value > MapSize.MAPSIZE_72_MINI_CS.id) {
            value = getDefaultValue()
        }
    }
}
