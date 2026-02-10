package com.jagex.game.runetek5.config.midi

import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.graphics.sprite.NativeSprite
import com.jagex.game.runetek5.graphics.sprite.NativeSpriteGenerator
import com.jagex.game.runetek5.network.packet.PacketEncoder
import com.jagex.game.runetek5.sound.jagtheora.JagTheoraTheoraNode
import com.jagex.game.runetek5.sound.jagtheora.JagTheoraVorbisNode
import com.jagex.game.runetek5.sound.jagtheora.VorbisPcmStream
import com.jagex.game.runetek5.sound.jagtheora.VorbisFileReference
import com.jagex.game.runetek5.sound.player.area.AmbientSound
import com.jagex.game.runetek5.staticmethods.StaticMethods
import com.jagex.game.runetek5.toolkit.AbstractToolkit

/**
 * Node for managing Vorbis audio files in the game.
 */
class VorbisNode(
    @JvmField val fileId: Int,
    @JvmField val vfReference: VorbisFileReference,
    @JvmField val anInt7997: Int,
    @JvmField val sendPacket: Boolean
) : Node<VorbisNode>() {

    @JvmField var aBool7995: Boolean = false
    @JvmField var aBool7996: Boolean = false
    @JvmField var aBool8000: Boolean = false

    companion object {
        @JvmField val SPRITE_GENERATOR = NativeSpriteGenerator()

        @JvmStatic
        fun method6511() {
            var node = StaticMethods.INDEX36_FILE_CACHE.first()
            while (node != null) {
                if (!node.aBool7996) {
                    StaticMethods.stopVorbisStream(node.fileId)
                } else {
                    node.aBool7996 = false
                }
                node = StaticMethods.INDEX36_FILE_CACHE.next()
            }
        }

        @JvmStatic
        fun method15536() {
            var node = StaticMethods.INDEX36_FILE_CACHE.first()
            while (node != null) {
                if (node.vfReference.isPlaybackComplete()) {
                    StaticMethods.stopVorbisStream(node.fileId)
                } else {
                    node.vfReference.prefetchChunks()

                    try {
                        node.vfReference.processStreams()
                    } catch (exception: Exception) {
                        PacketEncoder.sendErrorToServer("" + node.fileId, exception)
                        StaticMethods.stopVorbisStream(node.fileId)
                    }

                    if (!node.aBool8000 && !node.aBool7995) {
                        val vorbisNode = node.vfReference.getVorbisDecoder()
                        if (vorbisNode != null) {
                            val audioStream = vorbisNode.getPcmStream()
                            if (audioStream != null) {
                                audioStream.setVolume(node.anInt7997)
                                AmbientSound.gameSounds?.addSubStream(audioStream)
                                node.aBool8000 = true
                            }
                        }
                    }
                }
                node = StaticMethods.INDEX36_FILE_CACHE.next()
            }
        }

        @JvmStatic
        fun method8858(toolkit: AbstractToolkit, fileId: Int): NativeSprite? {
            val vorbisNode = StaticMethods.INDEX36_FILE_CACHE.get(fileId.toLong())
            if (vorbisNode != null) {
                val jagTheoraNode = vorbisNode.vfReference.getTheoraDecoder()
                vorbisNode.aBool7996 = true
                if (jagTheoraNode != null) {
                    SPRITE_GENERATOR.toolkit = toolkit
                    return jagTheoraNode.getFrameSprite(SPRITE_GENERATOR)
                }
            }
            return null
        }
    }
}
