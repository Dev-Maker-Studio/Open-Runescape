package com.jagex.game.runetek5.config.cutscene.action

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.cutscene.CutsceneAction
import com.jagex.game.runetek5.config.midi.AreadSound
import com.jagex.game.runetek5.sound.player.area.AreaSoundPlayer

class CutscenePlaySynth(rsbytebuffer1: Packet) : CutsceneAction(rsbytebuffer1) {

    @JvmField var aClass2689367: AreadSound? = null
    @JvmField val anInt9363: Int = rsbytebuffer1.readUShort()
    @JvmField val anInt9365: Int = rsbytebuffer1.readUByte()
    @JvmField val anInt9364: Int = rsbytebuffer1.readUByte()
    @JvmField val anInt9366: Int = rsbytebuffer1.readUByte()

    override fun perform() {
        this.aClass2689367 = AreaSoundPlayer.playSoundSynth(this.anInt9363, this.anInt9366, 0, this.anInt9365, this.anInt9364)
    }

    override fun method1593() {
        if (this.aClass2689367 != null) {
            AreaSoundPlayer.resetAreaSound(this.aClass2689367)
            this.aClass2689367 = null
        }
    }
}
