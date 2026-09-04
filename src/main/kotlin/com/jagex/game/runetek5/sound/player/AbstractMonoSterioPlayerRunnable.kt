package com.jagex.game.runetek5.sound.player

abstract class AbstractMonoSterioPlayerRunnable : Runnable {
    @JvmField
    val monoSterioPlayers: Array<PcmPlayer?> = arrayOfNulls(2)

    @Volatile
    @JvmField
    var exit: Boolean = false

    @Volatile
    @JvmField
    var running: Boolean = false
}
