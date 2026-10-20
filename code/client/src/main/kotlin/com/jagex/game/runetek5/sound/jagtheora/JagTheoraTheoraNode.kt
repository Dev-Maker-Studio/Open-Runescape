package com.jagex.game.runetek5.sound.jagtheora

import com.jagex.game.runetek5.graphics.sprite.INativeSpriteGenerator
import com.jagex.game.runetek5.graphics.sprite.NativeSprite
import com.jagex.game.runetek5.utils.Utils
import jagtheora.ogg.OggPacket
import jagtheora.ogg.OggStreamState
import jagtheora.theora.*

class JagTheoraTheoraNode(streamState: OggStreamState) : OggStreamDecoder(streamState) {

    var unusedBooleanV2: Boolean = false
    var packetDecodeTime: Long = 0
    var headerDecoded: Boolean = false
    var granuleTime: Double = 0.0
    var decoderContext: DecoderContext? = null
    var granulePos: GranulePos? = null
    var frame: Frame? = null
    var maxPostProcessingLevel: Int = 0
    var frameDecoded: Boolean = false
    var unusedBoolean: Boolean = false
    var currentPostProcessingLevel: Int = 0
    var frameSprite: NativeSprite? = null
    val setupInfo: SetupInfo = SetupInfo()
    val info: TheoraInfo = TheoraInfo()
    val comment: TheoraComment = TheoraComment()

    override fun decode(packet: OggPacket) {
        if (!headerDecoded) {
            val header = setupInfo.decodeHeader(info, comment, packet)
            if (header == 0) {
                headerDecoded = true
                if (info.frameWidth > 2048 || info.frameHeight > 1024) {
                    throw IllegalStateException()
                }

                decoderContext = DecoderContext(info, setupInfo)
                granulePos = GranulePos()
                frame = Frame(info.frameWidth, info.frameHeight)
                maxPostProcessingLevel = decoderContext!!.getMaxPostProcessingLevel()
                setPostProcessingLevel(currentPostProcessingLevel)
            } else if (header < 0) {
                throw IllegalStateException("$header")
            }
        } else {
            packetDecodeTime = Utils.time()
            val packetIn = decoderContext!!.decodePacketIn(packet, granulePos!!)
            if (packetIn < 0) {
                throw IllegalStateException("$packetIn")
            }

            decoderContext!!.granuleFrame(granulePos!!)
            granuleTime = decoderContext!!.granuleTime(granulePos!!)
            if (unusedBoolean) {
                val keyframe = packet.isKeyFrame() == 1
                if (!keyframe) {
                    return
                }

                unusedBoolean = false
            }

            if (!unusedBooleanV2 || packet.isKeyFrame() == 1) {
                if (decoderContext!!.decodeFrame(frame!!) != 0) {
                    throw IllegalStateException("$packetIn")
                }

                frameDecoded = true
            }
        }
    }

    fun setPostProcessingLevel(processingLevel: Int) {
        currentPostProcessingLevel = processingLevel
        if (headerDecoded) {
            if (currentPostProcessingLevel > maxPostProcessingLevel) {
                currentPostProcessingLevel = maxPostProcessingLevel
            }

            if (currentPostProcessingLevel < 0) {
                currentPostProcessingLevel = 0
            }

            decoderContext?.setPostProcessingLevel(currentPostProcessingLevel)
        }
    }

    fun getFps(): Float {
        return if (headerDecoded && !info.peerPresent()) {
            info.fpsNumerator.toFloat() / info.fpsDenominator
        } else {
            0.0f
        }
    }

    fun isHeaderDecoded(): Boolean = headerDecoded

    override fun reset() {
        frame?.resetPeer()

        decoderContext?.let {
            it.resetPeer()
            decoderContext = null
        }

        granulePos?.let {
            it.resetPeer()
            granulePos = null
        }

        info.resetPeer()
        comment.resetPeer()
        setupInfo.resetPeer()
    }

    fun getFrameSprite(spriteGenerator: INativeSpriteGenerator): NativeSprite? {
        val currentFrame = frame ?: return null

        return if (!frameDecoded && frameSprite != null) {
            frameSprite
        } else {
            frameSprite = spriteGenerator.createSprite(
                currentFrame.pixels,
                currentFrame.width,
                currentFrame.width,
                currentFrame.height
            )
            frameDecoded = false
            frameSprite
        }
    }
}
