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

    @JvmField val vorbisInfo = VorbisInfo()
    @JvmField val vorbisComment = VorbisComment()
    @JvmField var granuleTime: Double = 0.0
    @JvmField var totalSamplesRead: Int = 0
    @JvmField var dspState: DSPState? = null
    @JvmField var vorbisBlock: VorbisBlock? = null
    @JvmField var resampler: Resampler? = null
    @JvmField var pcmStream: VorbisPcmStream? = null

    override fun decode(oggPacket: OggPacket) {
        if (packetCount < 3) {
            val result = vorbisInfo.headerIn(vorbisComment, oggPacket)
            if (result < 0) {
                throw IllegalStateException("$result")
            }
            if (packetCount == 2) {
                if (vorbisInfo.channels > 2 || vorbisInfo.channels < 1) {
                    throw RuntimeException("${vorbisInfo.channels}")
                }
                val newDspState = DSPState(vorbisInfo)
                dspState = newDspState
                vorbisBlock = VorbisBlock(newDspState)
                resampler = Resampler(vorbisInfo.rate, PcmPlayerManager.sampleRate)
                pcmStream = VorbisPcmStream(vorbisInfo.channels)
            }
        } else {
            val vorbisBlock = vorbisBlock!!
            if (vorbisBlock.synthesis(oggPacket) == 0) {
                dspState!!.blockIn(vorbisBlock)
            }
            val pcmData = dspState!!.pcmOut(vorbisInfo.channels)!!
            granuleTime = dspState!!.granuleTime()
            if (granuleTime == -1.0) {
                granuleTime = totalSamplesRead.toFloat() / vorbisInfo.rate.toDouble()
            }
            dspState!!.read(pcmData[0].size)
            totalSamplesRead += pcmData[0].size
            val monoStereoData = pcmStream!!.allocateSampleData(pcmData[0].size, granuleTime)
            convertFloatToPcm16(pcmData, monoStereoData.samples)
            for (channel in 0 until vorbisInfo.channels) {
                monoStereoData.samples[channel] = resampler!!.resample(monoStereoData.samples[channel])
            }
            pcmStream!!.enqueueSampleData(monoStereoData)
        }
    }

    fun getPcmStream(): VorbisPcmStream? {
        return pcmStream
    }

    fun getBufferedSampleCount(): Int {
        return pcmStream?.getBufferedCount() ?: 0
    }

    fun getCurrentPlaybackTime(): Double {
        var time = granuleTime
        if (pcmStream != null) {
            time = pcmStream!!.getPlaybackTime()
            if (time < 0.0) {
                time = granuleTime
            }
        }
        return time - (256.0f / PcmPlayerManager.sampleRate)
    }

    override fun reset() {
        if (vorbisBlock != null) {
            vorbisBlock!!.resetPeer()
        }
        if (dspState != null) {
            dspState!!.resetPeer()
        }
        vorbisComment.resetPeer()
        vorbisInfo.resetPeer()
        if (pcmStream != null) {
            pcmStream!!.stop()
        }
    }

    companion object {
        @JvmStatic
        fun convertFloatToPcm16(floatData: Array<FloatArray>, shortData: Array<ShortArray>): Array<ShortArray> {
            for (channel in floatData.indices) {
                for (sample in shortData[channel].indices) {
                    shortData[channel][sample] = (16383.0f * floatData[channel][sample]).toInt().toShort()
                }
            }
            return shortData
        }
    }
}
