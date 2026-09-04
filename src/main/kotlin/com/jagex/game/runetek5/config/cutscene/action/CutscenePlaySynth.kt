package com.jagex.game.runetek5.config.cutscene.action

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.cutscene.CutsceneAction
import com.jagex.game.runetek5.config.midi.AreaSound
import com.jagex.game.runetek5.sound.player.area.AreaSoundPlayer

class CutscenePlaySynth(rsbytebuffer1: Packet) : CutsceneAction(rsbytebuffer1) {

    @JvmField var areaSound: AreaSound? = null
    @JvmField val soundId: Int = rsbytebuffer1.readUShort()
    @JvmField val volume: Int = rsbytebuffer1.readUByte()
    @JvmField val sampleRate: Int = rsbytebuffer1.readUByte()
    @JvmField val timesRepeated: Int = rsbytebuffer1.readUByte()

    override fun perform() {
        this.areaSound = AreaSoundPlayer.playSoundSynth(this.soundId, this.timesRepeated, 0, this.volume, this.sampleRate)
    }

    override fun stopAction() {
        if (this.areaSound != null) {
            AreaSoundPlayer.resetAreaSound(this.areaSound!!)
            this.areaSound = null
        }
    }
}
