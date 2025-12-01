package com.jagex.game.runetek5.config.cutscene.action

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.cutscene.CutsceneAction
import com.jagex.game.runetek5.config.midi.AreaSound
import com.jagex.game.runetek5.sound.player.area.AreaSoundPlayer

/**
 *
 * @author Steven - 02/10/2023
 *
 */
class CutscenePlayVorbis(buffer: Packet) : CutsceneAction(buffer) {

    @JvmField var vorbisSound: AreaSound? = null

    @JvmField val soundId: Int

    @JvmField val volume: Int

    @JvmField val sampleRate: Int

    @JvmField val timesRepeated: Int

    init {
        this.soundId = buffer.readUShort()
        this.volume = buffer.readUByte()
        this.sampleRate = buffer.readUByte()
        this.timesRepeated = buffer.readUByte()
    }

    override fun perform() {
        this.vorbisSound = AreaSoundPlayer.playSoundVorbis(this.soundId, this.timesRepeated, 0, this.volume, false, this.sampleRate)
    }

    override fun method1593() {
        if (this.vorbisSound != null) {
            AreaSoundPlayer.resetAreaSound(this.vorbisSound!!)
            this.vorbisSound = null
        }
    }
}
