package com.jagex.game.runetek5.config.cutscene

class CutsceneLoadingStage {
    companion object {
        @JvmField
        val LOADING_DEFINITIONS: CutsceneLoadingStage = CutsceneLoadingStage()

        @JvmField
        val CACHING_ASSETS: CutsceneLoadingStage = CutsceneLoadingStage()

        @JvmField
        val FINISHED: CutsceneLoadingStage = CutsceneLoadingStage()
    }
}
