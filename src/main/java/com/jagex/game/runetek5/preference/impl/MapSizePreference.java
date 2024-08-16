package com.jagex.game.runetek5.preference.impl;

import com.jagex.game.runetek5.preference.GamePreferences;
import com.jagex.game.runetek5.preference.Preference;
import com.jagex.game.runetek5.scene.MapSize;

/**
 * 
 * @author Steven - 17/09/2023
 *
 */
public class MapSizePreference extends Preference {

    public MapSizePreference(GamePreferences gamePreferences) {
        super(gamePreferences);
    }

    public MapSizePreference(int value, GamePreferences gamePreferences) {
        super(value, gamePreferences);
    }

    @Override
    public void setValue(int value) {
        this.value = value;
    }

    @Override
    public int getDefaultValue() {
        return MapSize.MAPSIZE_104_DEFAULT.id;
    }

    public boolean allowSelection() {
        int maxMemory = super.gamePreferences.getProcessorSpecs().getMaxMemory();
        return maxMemory >= 245;
    }

    @Override
    public int canModify(int mapSize) {
        int maxMemory = super.gamePreferences.getProcessorSpecs().getMaxMemory();
        return maxMemory < 245 ? 3 : (mapSize == MapSize.MAPSIZE_168_EXT3.id && maxMemory < 500 ? 3 : 1);
    }

    public int getValue() {
    	return super.value;
    }

    public void ensureValid() {
        int maxMemory = super.gamePreferences.getProcessorSpecs().getMaxMemory();
        if (maxMemory < 245) {
            super.value = MapSize.MAPSIZE_104_DEFAULT.id;
        }

        if (super.value == MapSize.MAPSIZE_168_EXT3.id && maxMemory < 500) {
            super.value = MapSize.MAPSIZE_136_EXT2.id;
        }

        if (super.value < MapSize.MAPSIZE_104_DEFAULT.id || super.value > MapSize.MAPSIZE_72_MINI_CS.id) {
            super.value = getDefaultValue();
        }
    }
}
