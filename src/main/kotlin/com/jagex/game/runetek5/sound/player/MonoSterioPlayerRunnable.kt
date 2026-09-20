package com.jagex.game.runetek5.sound.player

import com.jagex.game.runetek5.client.GameShell
import com.jagex.game.runetek5.network.packet.PacketEncoder
import com.jagex.game.runetek5.tools.TimeTools

class MonoSterioPlayerRunnable : AbstractMonoSterioPlayerRunnable() {

    override fun run() {
        super.running = true

        try {
            try {
                while (!super.exit) {
                    for (index in 0 until 2) {
                        val player = super.monoSterioPlayers[index]
                        if (player != null) {
                            player.cycle()
                        }
                    }

                    TimeTools.sleep(10L)
                    GameShell.performDummyActionevent(null)
                }
            } catch (innerException: Exception) {
                PacketEncoder.sendErrorToServer(null, innerException)
                super.running = false
                return
            }

            super.running = false
        } catch (outerException: Exception) {
            super.running = false
        }
    }
}
