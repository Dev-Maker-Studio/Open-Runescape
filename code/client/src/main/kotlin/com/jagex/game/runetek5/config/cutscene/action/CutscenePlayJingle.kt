package com.jagex.game.runetek5.config.cutscene.action

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.cutscene.CutsceneAction
import com.jagex.game.runetek5.sound.player.area.SoundEffectPlayer

/**
 * @author Steven - 23/09/2023
 */
class CutscenePlayJingle(buffer: Packet) : CutsceneAction(buffer) {

    @JvmField
    val jingleId: Int = buffer.readUShort()

    @JvmField
    val volume: Int = buffer.readUByte()

    override fun perform() {
        SoundEffectPlayer.playSoundJingle(jingleId, volume)
    }
}
