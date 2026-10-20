package com.jagex.game.runetek5.sound.player

object PcmPlayerManager {

    @JvmField var stereo: Boolean = false
    @JvmField var threadPriority: Int = 0
    @JvmField var sampleRate: Int = 0
    @JvmField var runnable: AbstractMonoSterioPlayerRunnable? = null

    @JvmStatic
    fun start(type: Int, sampleRate: Int): PcmPlayer {
        var rate = sampleRate

        if (rate == 0) {
            throw IllegalStateException()
        }

        if (type !in 0..1) {
            throw IllegalArgumentException()
        }

        if (rate < 256) {
            rate = 256
        }

        return try {
            val player = MonoSterioPlayer()
            player.bitpackedSoundData = IntArray(256 * if (stereo) 2 else 1)
            player.sampleRate = rate
            player.initialize()

            player.capacity = (rate and -1024) + 1024
            if (player.capacity > 16384) {
                player.capacity = 16384
            }

            player.play(player.capacity)

            if (threadPriority > 0 && runnable == null) {
                runnable = MonoSterioPlayerRunnable()
                val thread = Thread(runnable)
                thread.isDaemon = true
                thread.start()
                thread.priority = threadPriority
            }

            runnable?.let {
                if (it.monoSterioPlayers[type] != null) {
                    throw IllegalArgumentException()
                }
                it.monoSterioPlayers[type] = player
            }

            player
        } catch (throwable: Throwable) {
            PcmPlayer()
        }
    }

    @JvmStatic
    fun setStereo(stereo: Boolean) {
        PcmPlayerManager.sampleRate = 22050
        PcmPlayerManager.stereo = stereo
        PcmPlayerManager.threadPriority = 2
    }
}
