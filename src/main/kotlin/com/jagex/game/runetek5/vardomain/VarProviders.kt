package com.jagex.game.runetek5.vardomain

class VarProviders {
    companion object {
        @JvmStatic
        lateinit var PLAYER_VAR_PROVIDER: PlayerVarDomain
        val playerVarsReady: Boolean get() = ::PLAYER_VAR_PROVIDER.isInitialized
        @JvmField
        val CUTSCENE_ACTION_VAR_PROVIDER: VarDomain = CutsceneVarDomain()
    }
}
