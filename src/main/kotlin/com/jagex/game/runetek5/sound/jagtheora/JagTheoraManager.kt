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
            val d_2 = this.theoraDecoder!!.getFps()
            d_2 == 0.0f || Utils.time() >= this.theoraDecoder!!.packetDecodeTime + 1000.0 / d_2
        }
    }

    @Throws(IOException::class)
    fun readNextPage(): OggStreamDecoder? {
        if (this.isShutdown) {
            throw IllegalStateException()
        } else if (this.isEndOfStream) {
            return null
        } else {
            var i_2: Int
            while (this.syncState.pageOut(this.page) <= 0) {
                i_2 = decode(this.buffer)
                if (i_2 == -1) {
                    this.isEndOfStream = true
                    return null
                }
                if (i_2 == 0) {
                    return null
                }
                if (!this.syncState.write(this.buffer, i_2)) {
                    throw RuntimeException("")
                }
            }
            i_2 = this.page.getSerialNumber()
            if (!this.page.isBOS()) {
                val class282Sub419 = this.cache.get(i_2.toLong()) as OggStreamDecoder
                if (!class282Sub419.streamState.pageIn(this.page)) {
                    throw IllegalStateException()
                } else {
                    return class282Sub419
                }
            } else {
                val state = OggStreamState(i_2)
                if (!state.pageIn(this.page)) {
                    throw IllegalStateException()
                } else if (state.packetPeek(this.packet) != 1) {
                    throw IllegalStateException()
                } else {
                    val obj4: OggStreamDecoder
                    if (this.theoraDecoder == null && this.packet.isTheora()) {
                        this.theoraDecoder = JagTheoraTheoraNode(state)
                        obj4 = this.theoraDecoder!!
                    } else if (this.vorbisDecoder == null && this.packet.isVorbis()) {
                        this.vorbisDecoder = JagTheoraVorbisNode(state)
                        obj4 = this.vorbisDecoder!!
                    } else {
                        val bytes_5 = this.packet.getData()
                        val stringbuilder6 = StringBuilder()
                        var i_7 = 1
                        while (i_7 < bytes_5.size && Character.isLetterOrDigit(bytes_5[i_7].toInt().toChar())) {
                            stringbuilder6.append(bytes_5[i_7].toInt().toChar())
                            i_7++
                        }
                        val string_8 = stringbuilder6.toString()
                        obj4 = if ("kate" == string_8) {
                            OggKateDecoder(state)
                        } else {
                            NullOggStreamDecoder(state)
                        }
                    }
                    this.cache.put(obj4, i_2.toLong())
                    return obj4
                }
            }
        }
    }

    @Throws(IOException::class)
    fun decodeTheoraPacket() {
        while (this.theoraDecoder!!.streamState.packetOut(this.packet) != 1) {
            val class282Sub412 = readNextPage()
            if (class282Sub412 == null) {
                if (this.isEndOfStream) {
                    drainAllStreams()
                }
                return
            }
            if (class282Sub412 == this.kateDecoder) {
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
                var class282Sub413 = this.cache.first() as OggStreamDecoder?
                while (class282Sub413 != null) {
                    if (class282Sub413 is OggKateDecoder) {
                        val class282Sub41Sub24 = class282Sub413
                        if (this.languageAbbreviation == class282Sub41Sub24.getLanguageAbbreviation()) {
                            this.kateDecoder = class282Sub41Sub24
                            break
                        }
                    }
                    class282Sub413 = this.cache.next() as OggStreamDecoder?
                }
            }
        }
    }

    fun drainAllStreams() {
        var class282Sub412 = this.cache.first() as OggStreamDecoder?
        while (class282Sub412 != null) {
            if (class282Sub412 != this.theoraDecoder) {
                while (class282Sub412.streamState.packetOut() == 1) {
                    class282Sub412.processPacket(this.packet)
                }
            }
            class282Sub412 = this.cache.next() as OggStreamDecoder?
        }
        if (this.theoraDecoder != null) {
            var i_3 = 0
            while (i_3 < 10 && shouldDecodeVideoFrame()) {
                if (this.theoraDecoder!!.streamState.packetOut() != 1) {
                    shutdown()
                    break
                }
                this.theoraDecoder!!.processPacket(this.packet)
                i_3++
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

    fun togglePause(bool1: Boolean) {
        if (this.vorbisDecoder != null) {
            val class282Sub15Sub13 = this.vorbisDecoder!!.getPcmStream()
            if (class282Sub15Sub13 != null) {
                class282Sub15Sub13.setMuted(bool1)
            }
        }
        this.isPaused = !this.isPaused
    }

    @Throws(IOException::class)
    fun processStreams() {
        if (!this.isPaused) {
            while (!this.isShutdown) {
                val class282Sub412: OggStreamDecoder?
                if (!this.hasBufferedPage) {
                    class282Sub412 = readNextPage()
                    if (class282Sub412 == null) {
                        if (this.isEndOfStream) {
                            drainAllStreams()
                        }
                        break
                    }
                    this.hasBufferedPage = true
                } else {
                    class282Sub412 = this.cache.get(this.page.getSerialNumber().toLong()) as OggStreamDecoder?
                }
                if (class282Sub412 == this.vorbisDecoder) {
                    if (this.vorbisDecoder!!.getBufferedSampleCount() >= 50) {
                        break
                    }
                    while (this.vorbisDecoder!!.streamState.packetOut(this.packet) == 1) {
                        this.vorbisDecoder!!.processPacket(this.packet)
                        processKateSubtitles()
                        if (this.theoraDecoder != null) {
                            val d_3 = this.theoraDecoder!!.granuleTime
                            var i_5 = 0
                            while (i_5 < 10 && shouldDecodeVideoFrame()) {
                                decodeTheoraPacket()
                                if (this.isShutdown) {
                                    return
                                }
                                i_5++
                            }
                            if (d_3 < this.theoraDecoder!!.granuleTime) {
                                return
                            }
                        }
                        if (this.vorbisDecoder!!.getBufferedSampleCount() >= 50) {
                            return
                        }
                    }
                } else if (class282Sub412 is OggKateDecoder) {
                    processKateSubtitles()
                } else if (class282Sub412 != this.theoraDecoder) {
                    while (class282Sub412!!.streamState.packetOut(this.packet) == 1) {
                        class282Sub412.processPacket(this.packet)
                    }
                } else if (this.vorbisDecoder == null && !this.isPaused) {
                    var i_6 = 0
                    while (i_6 < 10 && shouldDecodeVideoFrame()) {
                        decodeTheoraPacket()
                        if (this.isShutdown) {
                            return
                        }
                        i_6++
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
        var class282Sub412 = this.cache.first() as OggStreamDecoder?
        while (class282Sub412 != null) {
            if (class282Sub412 is OggKateDecoder) {
                val class282Sub41Sub23 = class282Sub412
                while ((class282Sub41Sub23.packetCount <= 8 || getPlaybackTime() > class282Sub41Sub23.getEndTime()) && class282Sub41Sub23.streamState.packetOut(this.packet) == 1) {
                    class282Sub41Sub23.processPacket(this.packet)
                }
            }
            class282Sub412 = this.cache.next() as OggStreamDecoder?
        }
        setLanguage(this.languageAbbreviation)
    }

    fun shutdown() {
        if (!this.isShutdown) {
            var class282Sub412 = this.cache.first() as OggStreamDecoder?
            while (class282Sub412 != null) {
                class282Sub412.reset()
                class282Sub412.streamState.resetPeer()
                class282Sub412 = this.cache.next() as OggStreamDecoder?
            }
            this.packet.resetPeer()
            this.page.resetPeer()
            this.syncState.resetPeer()
            this.isShutdown = true
        }
    }

    abstract fun decode(var1: ByteArray): Int
}
