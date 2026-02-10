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
    var aBool3361: Boolean = false

    @JvmField
    var aNodeSub41Sub33363: JagTheoraTheoraNode? = null

    @JvmField
    var aNodeSub41Sub43366: JagTheoraVorbisNode? = null

    @JvmField
    var languageAbbreviation: String? = null

    @JvmField
    var aNodeSub41Sub23364: OggKateDecoder? = null

    @JvmField
    var aBool3353: Boolean = false

    @JvmField
    var aBool3359: Boolean = false

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
    var aBool3360: Boolean = false

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

    fun method4910(): Boolean {
        return if (this.aNodeSub41Sub43366 != null) {
            !this.aNodeSub41Sub33363!!.isHeaderDecoded() || method4923() > this.aNodeSub41Sub33363!!.granuleTime
        } else {
            val d_2 = this.aNodeSub41Sub33363!!.getFps()
            d_2 == 0.0f || Utils.time() >= this.aNodeSub41Sub33363!!.packetDecodeTime + 1000.0 / d_2
        }
    }

    @Throws(IOException::class)
    fun method4911(): OggStreamDecoder? {
        if (this.aBool3360) {
            throw IllegalStateException()
        } else if (this.aBool3361) {
            return null
        } else {
            var i_2: Int
            while (this.syncState.pageOut(this.page) <= 0) {
                i_2 = decode(this.buffer)
                if (i_2 == -1) {
                    this.aBool3361 = true
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
                if (!class282Sub419.anOggStreamState8032.pageIn(this.page)) {
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
                    if (this.aNodeSub41Sub33363 == null && this.packet.isTheora()) {
                        this.aNodeSub41Sub33363 = JagTheoraTheoraNode(state)
                        obj4 = this.aNodeSub41Sub33363!!
                    } else if (this.aNodeSub41Sub43366 == null && this.packet.isVorbis()) {
                        this.aNodeSub41Sub43366 = JagTheoraVorbisNode(state)
                        obj4 = this.aNodeSub41Sub43366!!
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
    fun method4913() {
        while (this.aNodeSub41Sub33363!!.anOggStreamState8032.packetOut(this.packet) != 1) {
            val class282Sub412 = method4911()
            if (class282Sub412 == null) {
                if (this.aBool3361) {
                    method4916()
                }
                return
            }
            if (class282Sub412 == this.aNodeSub41Sub23364) {
                method4942()
            }
        }
        this.aNodeSub41Sub33363!!.method13361(this.packet)
    }

    fun method4914(): JagTheoraVorbisNode? {
        return this.aNodeSub41Sub43366
    }

    fun setLanguage(languageAbbreviation: String?) {
        this.languageAbbreviation = languageAbbreviation
        if (this.languageAbbreviation == null) {
            this.aNodeSub41Sub23364 = null
        } else {
            if (this.aNodeSub41Sub23364 != null && this.languageAbbreviation != this.aNodeSub41Sub23364!!.getLanguageAbbreviation()) {
                this.aNodeSub41Sub23364 = null
            }
            if (this.aNodeSub41Sub23364 == null) {
                var class282Sub413 = this.cache.first() as OggStreamDecoder?
                while (class282Sub413 != null) {
                    if (class282Sub413 is OggKateDecoder) {
                        val class282Sub41Sub24 = class282Sub413
                        if (this.languageAbbreviation == class282Sub41Sub24.getLanguageAbbreviation()) {
                            this.aNodeSub41Sub23364 = class282Sub41Sub24
                            break
                        }
                    }
                    class282Sub413 = this.cache.next() as OggStreamDecoder?
                }
            }
        }
    }

    fun method4916() {
        var class282Sub412 = this.cache.first() as OggStreamDecoder?
        while (class282Sub412 != null) {
            if (class282Sub412 != this.aNodeSub41Sub33363) {
                while (class282Sub412.anOggStreamState8032.packetOut() == 1) {
                    class282Sub412.method13361(this.packet)
                }
            }
            class282Sub412 = this.cache.next() as OggStreamDecoder?
        }
        if (this.aNodeSub41Sub33363 != null) {
            var i_3 = 0
            while (i_3 < 10 && method4910()) {
                if (this.aNodeSub41Sub33363!!.anOggStreamState8032.packetOut() != 1) {
                    method4947()
                    break
                }
                this.aNodeSub41Sub33363!!.method13361(this.packet)
                i_3++
            }
        }
    }

    fun method4918(): JagTheoraTheoraNode? {
        return this.aNodeSub41Sub33363
    }

    fun method4921(): Boolean {
        return (this.aBool3360 || this.aBool3361) && (this.aNodeSub41Sub43366 == null || this.aNodeSub41Sub43366!!.method14847() <= 0)
    }

    fun method4923(): Double {
        return if (this.aNodeSub41Sub43366 != null) this.aNodeSub41Sub43366!!.method14848() else if (this.aNodeSub41Sub33363 != null) this.aNodeSub41Sub33363!!.granuleTime else 0.0
    }

    fun method4924(bool1: Boolean) {
        if (this.aNodeSub41Sub43366 != null) {
            val class282Sub15Sub13 = this.aNodeSub41Sub43366!!.method14845()
            if (class282Sub15Sub13 != null) {
                class282Sub15Sub13.method14816(bool1)
            }
        }
        this.aBool3353 = !this.aBool3353
    }

    @Throws(IOException::class)
    fun method4932() {
        if (!this.aBool3353) {
            while (!this.aBool3360) {
                val class282Sub412: OggStreamDecoder?
                if (!this.aBool3359) {
                    class282Sub412 = method4911()
                    if (class282Sub412 == null) {
                        if (this.aBool3361) {
                            method4916()
                        }
                        break
                    }
                    this.aBool3359 = true
                } else {
                    class282Sub412 = this.cache.get(this.page.getSerialNumber().toLong()) as OggStreamDecoder?
                }
                if (class282Sub412 == this.aNodeSub41Sub43366) {
                    if (this.aNodeSub41Sub43366!!.method14847() >= 50) {
                        break
                    }
                    while (this.aNodeSub41Sub43366!!.anOggStreamState8032.packetOut(this.packet) == 1) {
                        this.aNodeSub41Sub43366!!.method13361(this.packet)
                        method4942()
                        if (this.aNodeSub41Sub33363 != null) {
                            val d_3 = this.aNodeSub41Sub33363!!.granuleTime
                            var i_5 = 0
                            while (i_5 < 10 && method4910()) {
                                method4913()
                                if (this.aBool3360) {
                                    return
                                }
                                i_5++
                            }
                            if (d_3 < this.aNodeSub41Sub33363!!.granuleTime) {
                                return
                            }
                        }
                        if (this.aNodeSub41Sub43366!!.method14847() >= 50) {
                            return
                        }
                    }
                } else if (class282Sub412 is OggKateDecoder) {
                    method4942()
                } else if (class282Sub412 != this.aNodeSub41Sub33363) {
                    while (class282Sub412!!.anOggStreamState8032.packetOut(this.packet) == 1) {
                        class282Sub412.method13361(this.packet)
                    }
                } else if (this.aNodeSub41Sub43366 == null && !this.aBool3353) {
                    var i_6 = 0
                    while (i_6 < 10 && method4910()) {
                        method4913()
                        if (this.aBool3360) {
                            return
                        }
                        i_6++
                    }
                    return
                }
                this.aBool3359 = false
            }
        }
    }

    fun method4935(): OggKateDecoder? {
        return this.aNodeSub41Sub23364
    }

    fun method4942() {
        var class282Sub412 = this.cache.first() as OggStreamDecoder?
        while (class282Sub412 != null) {
            if (class282Sub412 is OggKateDecoder) {
                val class282Sub41Sub23 = class282Sub412
                while ((class282Sub41Sub23.anInt8031 <= 8 || method4923() > class282Sub41Sub23.method14705()) && class282Sub41Sub23.anOggStreamState8032.packetOut(this.packet) == 1) {
                    class282Sub41Sub23.method13361(this.packet)
                }
            }
            class282Sub412 = this.cache.next() as OggStreamDecoder?
        }
        setLanguage(this.languageAbbreviation)
    }

    fun method4947() {
        if (!this.aBool3360) {
            var class282Sub412 = this.cache.first() as OggStreamDecoder?
            while (class282Sub412 != null) {
                class282Sub412.reset()
                class282Sub412.anOggStreamState8032.resetPeer()
                class282Sub412 = this.cache.next() as OggStreamDecoder?
            }
            this.packet.resetPeer()
            this.page.resetPeer()
            this.syncState.resetPeer()
            this.aBool3360 = true
        }
    }

    abstract fun decode(var1: ByteArray): Int
}
