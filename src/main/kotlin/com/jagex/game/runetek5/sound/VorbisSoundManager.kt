package com.jagex.game.runetek5.sound

import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.midi.VorbisNode
import com.jagex.game.runetek5.sound.jagtheora.VorbisFileReference
import com.jagex.game.runetek5.staticmethods.StaticMethods
import com.jagex.game.runetek5.trigger.sound.SoundTrigger

object VorbisSoundManager {
    @JvmStatic
    fun preloadVorbisSound(fileId: Int, i_1: Int, bool2: Boolean) {
        if (StaticMethods.INDEX36_FILE_CACHE[fileId.toLong()] == null) {
            if (!Client.jagtheoraLoaded) {
                SoundTrigger.playVorbis(fileId, bool2)
            } else {
                val vorbisnode = VorbisNode(fileId, VorbisFileReference(Resource.VORBIS!!, fileId), i_1, bool2)
                vorbisnode.vfReference.setLanguage(Client.CURRENT_LANGUAGE!!.lowercaseAbbreviation)
                StaticMethods.INDEX36_FILE_CACHE.put(vorbisnode, fileId.toLong())
            }
        }
    }
}
