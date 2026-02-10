package com.jagex.game.runetek5.sound.jagtheora

import com.jagex.game.runetek5.sound.Resampler
import com.jagex.game.runetek5.sound.player.PcmPlayerManager
import jagtheora.ogg.OggPacket
import jagtheora.ogg.OggStreamState
import jagtheora.vorbis.DSPState
import jagtheora.vorbis.VorbisBlock
import jagtheora.vorbis.VorbisComment
import jagtheora.vorbis.VorbisInfo

class JagTheoraVorbisNode(oggStreamState: OggStreamState) : OggStreamDecoder(oggStreamState) {

    @JvmField val aVorbisInfo9605 = VorbisInfo()
    @JvmField val aVorbisComment9599 = VorbisComment()
    @JvmField var aDouble9604: Double = 0.0
    @JvmField var anInt9602: Int = 0
    @JvmField var aDSPState9600: DSPState? = null
    @JvmField var aVorbisBlock9601: VorbisBlock? = null
    @JvmField var aClass3449598: Resampler? = null
    @JvmField var aNodeSub15Sub19603: VorbisPcmStream? = null

    override fun decode(oggPacket: OggPacket) {
        if (anInt8031 < 3) {
            val result = aVorbisInfo9605.headerIn(aVorbisComment9599, oggPacket)
            if (result < 0) {
                throw IllegalStateException("$result")
            }
            if (anInt8031 == 2) {
                if (aVorbisInfo9605.channels > 2 || aVorbisInfo9605.channels < 1) {
                    throw RuntimeException("${aVorbisInfo9605.channels}")
                }
                val dspState = DSPState(aVorbisInfo9605)
                aDSPState9600 = dspState
                aVorbisBlock9601 = VorbisBlock(dspState)
                aClass3449598 = Resampler(aVorbisInfo9605.rate, PcmPlayerManager.sampleRate)
                aNodeSub15Sub19603 = VorbisPcmStream(aVorbisInfo9605.channels)
            }
        } else {
            val vorbisBlock = aVorbisBlock9601!!
            if (vorbisBlock.synthesis(oggPacket) == 0) {
                aDSPState9600!!.blockIn(vorbisBlock)
            }
            val pcmData = aDSPState9600!!.pcmOut(aVorbisInfo9605.channels)!!
            aDouble9604 = aDSPState9600!!.granuleTime()
            if (aDouble9604 == -1.0) {
                aDouble9604 = anInt9602.toFloat() / aVorbisInfo9605.rate.toDouble()
            }
            aDSPState9600!!.read(pcmData[0].size)
            anInt9602 += pcmData[0].size
            val monoStereoData = aNodeSub15Sub19603!!.method14814(pcmData[0].size, aDouble9604)
            method12805(pcmData, monoStereoData.aShortArrayArray8003)
            for (channel in 0 until aVorbisInfo9605.channels) {
                monoStereoData.aShortArrayArray8003[channel] = aClass3449598!!.resample(monoStereoData.aShortArrayArray8003[channel])
            }
            aNodeSub15Sub19603!!.method14815(monoStereoData)
        }
    }

    fun method14845(): VorbisPcmStream? {
        return aNodeSub15Sub19603
    }

    fun method14847(): Int {
        return aNodeSub15Sub19603?.method14811() ?: 0
    }

    fun method14848(): Double {
        var time = aDouble9604
        if (aNodeSub15Sub19603 != null) {
            time = aNodeSub15Sub19603!!.method14819()
            if (time < 0.0) {
                time = aDouble9604
            }
        }
        return time - (256.0f / PcmPlayerManager.sampleRate)
    }

    override fun reset() {
        if (aVorbisBlock9601 != null) {
            aVorbisBlock9601!!.resetPeer()
        }
        if (aDSPState9600 != null) {
            aDSPState9600!!.resetPeer()
        }
        aVorbisComment9599.resetPeer()
        aVorbisInfo9605.resetPeer()
        if (aNodeSub15Sub19603 != null) {
            aNodeSub15Sub19603!!.method14808()
        }
    }

    companion object {
        @JvmStatic
        fun method12805(floatData: Array<FloatArray>, shortData: Array<ShortArray>): Array<ShortArray> {
            for (channel in floatData.indices) {
                for (sample in shortData[channel].indices) {
                    shortData[channel][sample] = (16383.0f * floatData[channel][sample]).toInt().toShort()
                }
            }
            return shortData
        }
    }
}
