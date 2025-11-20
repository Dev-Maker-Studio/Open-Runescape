package com.jagex.game.runetek5.sound.player

/**
 * Manages PCM (Pulse Code Modulation) audio players.
 * Handles initialization and threading for mono/stereo audio playback.
 */
object PcmPlayerManager {

    @JvmField var stereo: Boolean = false
    @JvmField var threadPriority: Int = 0
    @JvmField var sampleRate: Int = 0
    @JvmField var runnable: AbstractMonoSterioPlayerRunnable? = null

    /**
     * Starts a PCM player with the specified configuration
     * @param type Player type (0 or 1)
     * @param sampleRate Sample rate in Hz
     * @return The initialized PcmPlayer, or a dummy player if initialization fails
     */
    @JvmStatic
    fun start(type: Int, sampleRate: Int): PcmPlayer {
        var rate = sampleRate

        if (rate == 0) {
            throw IllegalStateException()
        }

        if (type !in 0..1) {
            throw IllegalArgumentException()
        }

        // Ensure minimum sample rate
        if (rate < 256) {
            rate = 256
        }

        return try {
            val player = MonoSterioPlayer()
            player.bitpackedSoundData = IntArray(256 * if (stereo) 2 else 1)
            player.sampleRate = rate
            player.initialize()

            // Calculate capacity (aligned to 1024) with maximum of 16384
            player.capacity = (rate and -1024) + 1024
            if (player.capacity > 16384) {
                player.capacity = 16384
            }

            player.play(player.capacity)

            // Start background thread if priority is set
            if (threadPriority > 0 && runnable == null) {
                runnable = MonoSterioPlayerRunnable()
                val thread = Thread(runnable)
                thread.isDaemon = true
                thread.start()
                thread.priority = threadPriority
            }

            // Register player with runnable
            runnable?.let {
                if (it.monoSterioPlayers[type] != null) {
                    throw IllegalArgumentException()
                }
                it.monoSterioPlayers[type] = player
            }

            player
        } catch (throwable: Throwable) {
            // Return dummy player on failure
            PcmPlayer()
        }
    }

    /**
     * Configures stereo mode and sample rate
     * @param stereo Whether to use stereo (true) or mono (false)
     */
    @JvmStatic
    fun setStereo(stereo: Boolean) {
        PcmPlayerManager.sampleRate = 22050
        PcmPlayerManager.stereo = stereo
        PcmPlayerManager.threadPriority = 2
    }
}
