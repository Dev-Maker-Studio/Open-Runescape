package com.jagex.game.runetek5.sound.jagtheora

import jagtheora.ogg.OggPacket
import jagtheora.ogg.OggPage
import jagtheora.ogg.OggStreamState
import jagtheora.ogg.OggSyncState
import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.utils.Utils
import java.io.IOException

abstract class JagTheoraManager {

    @JvmField
    var isEndOfStream: Boolean = false

    @JvmField
    var theoraDecoder: JagTheoraTheoraNode? = null

    @JvmField
    var vorbisDecoder: JagTheoraVorbisNode? = null

    @JvmField
    var languageAbbreviation: String? = null

    @JvmField
    var kateDecoder: OggKateDecoder? = null

    @JvmField
    var isPaused: Boolean = false

    @JvmField
    var hasBufferedPage: Boolean = false

    @JvmField
    var buffer: ByteArray

    @JvmField
    var syncState: OggSyncState

    @JvmField
    var page: OggPage

    @JvmField
    var packet: OggPacket

    @JvmField
    var cache: IterableNodeMap<OggStreamDecoder>

    @JvmField
    var isShutdown: Boolean = false

    init {
        if (!AbstractToolkit.getNativeLibraryLoader().loadIntoMemory("jagtheora")) {
            throw RuntimeException("")
        } else {
            this.buffer = ByteArray(4096)
            this.syncState = OggSyncState()
            this.page = OggPage()
            this.packet = OggPacket()
            this.cache = IterableNodeMap(8)
        }
    }

    fun shouldDecodeVideoFrame(): Boolean {
        return if (this.vorbisDecoder != null) {
            !this.theoraDecoder!!.isHeaderDecoded() || getPlaybackTime() > this.theoraDecoder!!.granuleTime
        } else {
            val fps = this.theoraDecoder!!.getFps()
            fps == 0.0f || Utils.time() >= this.theoraDecoder!!.packetDecodeTime + 1000.0 / fps
        }
    }

    @Throws(IOException::class)
    fun readNextPage(): OggStreamDecoder? {
        if (this.isShutdown) {
            throw IllegalStateException()
        } else if (this.isEndOfStream) {
            return null
        } else {
            var bytesRead: Int
            while (this.syncState.pageOut(this.page) <= 0) {
                bytesRead = decode(this.buffer)
                if (bytesRead == -1) {
                    this.isEndOfStream = true
                    return null
                }
                if (bytesRead == 0) {
                    return null
                }
                if (!this.syncState.write(this.buffer, bytesRead)) {
                    throw RuntimeException("")
                }
            }
            bytesRead = this.page.getSerialNumber()
            if (!this.page.isBOS()) {
                val decoder = this.cache.get(bytesRead.toLong()) as OggStreamDecoder
                if (!decoder.streamState.pageIn(this.page)) {
                    throw IllegalStateException()
                } else {
                    return decoder
                }
            } else {
                val streamState = OggStreamState(bytesRead)
                if (!streamState.pageIn(this.page)) {
                    throw IllegalStateException()
                } else if (streamState.packetPeek(this.packet) != 1) {
                    throw IllegalStateException()
                } else {
                    val newDecoder: OggStreamDecoder
                    if (this.theoraDecoder == null && this.packet.isTheora()) {
                        this.theoraDecoder = JagTheoraTheoraNode(streamState)
                        newDecoder = this.theoraDecoder!!
                    } else if (this.vorbisDecoder == null && this.packet.isVorbis()) {
                        this.vorbisDecoder = JagTheoraVorbisNode(streamState)
                        newDecoder = this.vorbisDecoder!!
                    } else {
                        val packetData = this.packet.getData()
                        val codecNameBuilder = StringBuilder()
                        var byteIndex = 1
                        while (byteIndex < packetData.size && Character.isLetterOrDigit(packetData[byteIndex].toInt().toChar())) {
                            codecNameBuilder.append(packetData[byteIndex].toInt().toChar())
                            byteIndex++
                        }
                        val codecName = codecNameBuilder.toString()
                        newDecoder = if ("kate" == codecName) {
                            OggKateDecoder(streamState)
                        } else {
                            NullOggStreamDecoder(streamState)
                        }
                    }
                    this.cache.put(newDecoder, bytesRead.toLong())
                    return newDecoder
                }
            }
        }
    }

    @Throws(IOException::class)
    fun decodeTheoraPacket() {
        while (this.theoraDecoder!!.streamState.packetOut(this.packet) != 1) {
            val decoder = readNextPage()
            if (decoder == null) {
                if (this.isEndOfStream) {
                    drainAllStreams()
                }
                return
            }
            if (decoder == this.kateDecoder) {
                processKateSubtitles()
            }
        }
        this.theoraDecoder!!.processPacket(this.packet)
    }

    fun getVorbisDecoder(): JagTheoraVorbisNode? {
        return this.vorbisDecoder
    }

    fun setLanguage(languageAbbreviation: String?) {
        this.languageAbbreviation = languageAbbreviation
        if (this.languageAbbreviation == null) {
            this.kateDecoder = null
        } else {
            if (this.kateDecoder != null && this.languageAbbreviation != this.kateDecoder!!.getLanguageAbbreviation()) {
                this.kateDecoder = null
            }
            if (this.kateDecoder == null) {
                var decoder = this.cache.first() as OggStreamDecoder?
                while (decoder != null) {
                    if (decoder is OggKateDecoder) {
                        val kateDecoder = decoder
                        if (this.languageAbbreviation == kateDecoder.getLanguageAbbreviation()) {
                            this.kateDecoder = kateDecoder
                            break
                        }
                    }
                    decoder = this.cache.next() as OggStreamDecoder?
                }
            }
        }
    }

    fun drainAllStreams() {
        var decoder = this.cache.first() as OggStreamDecoder?
        while (decoder != null) {
            if (decoder != this.theoraDecoder) {
                while (decoder.streamState.packetOut() == 1) {
                    decoder.processPacket(this.packet)
                }
            }
            decoder = this.cache.next() as OggStreamDecoder?
        }
        if (this.theoraDecoder != null) {
            var frameCount = 0
            while (frameCount < 10 && shouldDecodeVideoFrame()) {
                if (this.theoraDecoder!!.streamState.packetOut() != 1) {
                    shutdown()
                    break
                }
                this.theoraDecoder!!.processPacket(this.packet)
                frameCount++
            }
        }
    }

    fun getTheoraDecoder(): JagTheoraTheoraNode? {
        return this.theoraDecoder
    }

    fun isPlaybackComplete(): Boolean {
        return (this.isShutdown || this.isEndOfStream) && (this.vorbisDecoder == null || this.vorbisDecoder!!.getBufferedSampleCount() <= 0)
    }

    fun getPlaybackTime(): Double {
        return if (this.vorbisDecoder != null) this.vorbisDecoder!!.getCurrentPlaybackTime() else if (this.theoraDecoder != null) this.theoraDecoder!!.granuleTime else 0.0
    }

    fun togglePause(paused: Boolean) {
        if (this.vorbisDecoder != null) {
            val pcmStream = this.vorbisDecoder!!.getPcmStream()
            if (pcmStream != null) {
                pcmStream.setMuted(paused)
            }
        }
        this.isPaused = !this.isPaused
    }

    @Throws(IOException::class)
    fun processStreams() {
        if (!this.isPaused) {
            while (!this.isShutdown) {
                val decoder: OggStreamDecoder?
                if (!this.hasBufferedPage) {
                    decoder = readNextPage()
                    if (decoder == null) {
                        if (this.isEndOfStream) {
                            drainAllStreams()
                        }
                        break
                    }
                    this.hasBufferedPage = true
                } else {
                    decoder = this.cache.get(this.page.getSerialNumber().toLong()) as OggStreamDecoder?
                }
                if (decoder == this.vorbisDecoder) {
                    if (this.vorbisDecoder!!.getBufferedSampleCount() >= 50) {
                        break
                    }
                    while (this.vorbisDecoder!!.streamState.packetOut(this.packet) == 1) {
                        this.vorbisDecoder!!.processPacket(this.packet)
                        processKateSubtitles()
                        if (this.theoraDecoder != null) {
                            val previousGranuleTime = this.theoraDecoder!!.granuleTime
                            var frameCount = 0
                            while (frameCount < 10 && shouldDecodeVideoFrame()) {
                                decodeTheoraPacket()
                                if (this.isShutdown) {
                                    return
                                }
                                frameCount++
                            }
                            if (previousGranuleTime < this.theoraDecoder!!.granuleTime) {
                                return
                            }
                        }
                        if (this.vorbisDecoder!!.getBufferedSampleCount() >= 50) {
                            return
                        }
                    }
                } else if (decoder is OggKateDecoder) {
                    processKateSubtitles()
                } else if (decoder != this.theoraDecoder) {
                    while (decoder!!.streamState.packetOut(this.packet) == 1) {
                        decoder.processPacket(this.packet)
                    }
                } else if (this.vorbisDecoder == null && !this.isPaused) {
                    var frameCount = 0
                    while (frameCount < 10 && shouldDecodeVideoFrame()) {
                        decodeTheoraPacket()
                        if (this.isShutdown) {
                            return
                        }
                        frameCount++
                    }
                    return
                }
                this.hasBufferedPage = false
            }
        }
    }

    fun getKateDecoder(): OggKateDecoder? {
        return this.kateDecoder
    }

    fun processKateSubtitles() {
        var decoder = this.cache.first() as OggStreamDecoder?
        while (decoder != null) {
            if (decoder is OggKateDecoder) {
                val kateDecoder = decoder
                while ((kateDecoder.packetCount <= 8 || getPlaybackTime() > kateDecoder.getEndTime()) && kateDecoder.streamState.packetOut(this.packet) == 1) {
                    kateDecoder.processPacket(this.packet)
                }
            }
            decoder = this.cache.next() as OggStreamDecoder?
        }
        setLanguage(this.languageAbbreviation)
    }

    fun shutdown() {
        if (!this.isShutdown) {
            var decoder = this.cache.first() as OggStreamDecoder?
            while (decoder != null) {
                decoder.reset()
                decoder.streamState.resetPeer()
                decoder = this.cache.next() as OggStreamDecoder?
            }
            this.packet.resetPeer()
            this.page.resetPeer()
            this.syncState.resetPeer()
            this.isShutdown = true
        }
    }

    abstract fun decode(buffer: ByteArray): Int
}
