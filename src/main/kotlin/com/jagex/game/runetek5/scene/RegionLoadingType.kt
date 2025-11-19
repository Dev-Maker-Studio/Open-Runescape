package com.jagex.game.runetek5.scene

class RegionLoadingType(
    @JvmField val constructedMap: Boolean,
    @JvmField val ignoreUpdatezoneSizeLimitations: Boolean
) {

    fun ignoreUpdateZoneSizeLimitations(): Boolean {
        return this.ignoreUpdatezoneSizeLimitations
    }

    /**
     * Map build from dynamic map packet,
     * Example: player owned houses
     */
    fun constructedMap(): Boolean {
        return this.constructedMap
    }

    companion object {
        /**
         * The position of the camera reached the edge of the map
         * on the animated login screen
         */
        @JvmField val ANIMATED_LOGIN_SCREEN = RegionLoadingType(false, false)

        /**
         * Cutscene loading is requested
         */
        @JvmField val CUTSCENE = RegionLoadingType(true, false)

        @JvmField val STANDARD = RegionLoadingType(false, false)
        @JvmField val INSTANCE = RegionLoadingType(true, false)
        @JvmField val INSTANCE_NO_ENTITY_RESET = RegionLoadingType(true, false)
        @JvmField val INSTANCE_LARGE = RegionLoadingType(true, true)
        @JvmField val INSTANCE_LARGE_NO_RELOAD_UPDATEZONES = RegionLoadingType(true, true)

        /**
         * Reload requested by debug opcode
         */
        @JvmField val RELOAD = RegionLoadingType(false, false)
    }
}
