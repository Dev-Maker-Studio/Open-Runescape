package com.jagex.game.runetek5.scene

class RegionLoadingType(
    @JvmField val constructedMap: Boolean,
    @JvmField val ignoreUpdatezoneSizeLimitations: Boolean
) {

    fun ignoreUpdateZoneSizeLimitations(): Boolean {
        return this.ignoreUpdatezoneSizeLimitations
    }

    fun constructedMap(): Boolean {
        return this.constructedMap
    }

    companion object {
        @JvmField val ANIMATED_LOGIN_SCREEN = RegionLoadingType(false, false)

        @JvmField val CUTSCENE = RegionLoadingType(true, false)

        @JvmField val STANDARD = RegionLoadingType(false, false)
        @JvmField val INSTANCE = RegionLoadingType(true, false)
        @JvmField val INSTANCE_NO_ENTITY_RESET = RegionLoadingType(true, false)
        @JvmField val INSTANCE_LARGE = RegionLoadingType(true, true)
        @JvmField val INSTANCE_LARGE_NO_RELOAD_UPDATEZONES = RegionLoadingType(true, true)

        @JvmField val RELOAD = RegionLoadingType(false, false)
    }
}
