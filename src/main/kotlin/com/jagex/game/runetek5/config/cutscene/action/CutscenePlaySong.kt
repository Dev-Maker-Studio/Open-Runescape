package com.jagex.game.runetek5.config.cutscene.action

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.cutscene.CutsceneAction
import com.jagex.game.runetek5.config.midi.SongReference
import com.jagex.game.runetek5.sound.player.song.SongPlayer

/**
 *
 * @author Brainpower - 09/04/2022
 *
 */
class CutscenePlaySong(buffer: Packet) : CutsceneAction(buffer) {

    @JvmField val musicId: Int = buffer.readUShort()
    @JvmField val volume: Int = buffer.readUByte()
    @JvmField var songReference: SongReference? = null

    override fun isFullyCached(): Boolean {
        if (this.songReference == null) {
            this.songReference = SongReference(Resource.MUSIC, this.musicId)
        }
        return this.songReference!!.initialize()
    }

    override fun perform() {
        SongPlayer.play(this.songReference, this.volume)
    }
}
