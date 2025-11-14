package com.jagex.game.runetek5.scene

class RegionLoadingStage {
    companion object {
        @JvmField
        val LOADING_MAPS: RegionLoadingStage = RegionLoadingStage()

        @JvmField
        val LOADING_LOCS: RegionLoadingStage = RegionLoadingStage()

        @JvmField
        val LOADING: RegionLoadingStage = RegionLoadingStage()

        @JvmField
        val PREPARE_DISPLAY: RegionLoadingStage = RegionLoadingStage()
    }
}
