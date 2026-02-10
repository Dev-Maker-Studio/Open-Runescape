package com.jagex.game.runetek5.sound.player

import com.jagex.game.runetek5.cache.node.impl.AudioNode
import com.jagex.game.runetek5.sound.jagtheora.PcmStream
import com.jagex.game.runetek5.tools.TimeTools
import com.jagex.game.runetek5.utils.Utils
import java.util.*

open class PcmPlayer {
    var currentPcmStream: PcmStream? = null
        @Synchronized set(value) {
            field = value
        }
    var bitpackedSoundData: IntArray? = null
    var sampleRate: Int = 0
    var capacity: Int = 0
    var timeLastCycle: Long = Utils.time()
    var nextBufferSwapTime: Long = 0
    var enabled: Boolean = true
    var dataConsumed: Int = 0
    var currentOffset: Int = 0
    var dataLeftLastCycle: Int = 0
    var nextUpdate: Long = 0
    var lastDataConsumed: Int = 0
    var shutdown: Boolean = false
    var anInt3117: Int = 0
    val anInt3123: Int = 32
    val activeStreams: Array<PcmStream?> = arrayOfNulls(8)
    val inactiveStreams: Array<PcmStream?> = arrayOfNulls(8)

    @Synchronized
    fun cycle() {
        if (!this.shutdown) {
            var currentTime = Utils.time()

            try {
                if (currentTime > this.timeLastCycle + 6000L) {
                    this.timeLastCycle = currentTime - 6000L
                }

                while (currentTime > this.timeLastCycle + 5000L) {
                    method4373()
                    this.timeLastCycle += 256000 / PcmPlayerManager.sampleRate
                    currentTime = Utils.time()
                }
            } catch (ex: Exception) {
                this.timeLastCycle = currentTime
            }

            if (this.bitpackedSoundData != null) {
                try {
                    if (this.nextBufferSwapTime != 0L) {
                        if (currentTime < this.nextBufferSwapTime) {
                            return
                        }

                        play(this.capacity)
                        this.nextBufferSwapTime = 0L
                        this.enabled = true
                    }

                    var dataLeft = dataLeft()
                    if (this.dataLeftLastCycle - dataLeft > this.dataConsumed) {
                        this.dataConsumed = this.dataLeftLastCycle - dataLeft
                    }

                    var offset = this.sampleRate + this.currentOffset
                    if (offset + 256 > 16384) {
                        offset = 16128
                    }

                    if (offset + 256 > this.capacity) {
                        this.capacity += 1024
                        if (this.capacity > 16384) {
                            this.capacity = 16384
                        }

                        close()
                        play(this.capacity)
                        dataLeft = 0
                        this.enabled = true
                        if (offset + 256 > this.capacity) {
                            offset = this.capacity - 256
                            this.currentOffset = offset - this.sampleRate
                        }
                    }

                    while (dataLeft < offset) {
                        method4337(this.bitpackedSoundData!!)
                        writeToBuffer()
                        dataLeft += 256
                    }

                    if (currentTime > this.nextUpdate) {
                        if (!this.enabled) {
                            if (this.dataConsumed == 0 && this.lastDataConsumed == 0) {
                                close()
                                this.nextBufferSwapTime = currentTime + 2000L
                                return
                            }

                            this.currentOffset = Math.min(this.lastDataConsumed, this.dataConsumed)
                            this.lastDataConsumed = this.dataConsumed
                        } else {
                            this.enabled = false
                        }

                        this.dataConsumed = 0
                        this.nextUpdate = currentTime + 2000L
                    }

                    this.dataLeftLastCycle = dataLeft
                } catch (exception: Exception) {
                    close()
                    this.nextBufferSwapTime = currentTime + 2000L
                }
            }
        }
    }

    @Synchronized
    fun start() {
        this.enabled = true

        try {
            open()
        } catch (exception: Exception) {
            close()
            this.nextBufferSwapTime = Utils.time() + 2000L
        }
    }

    @Synchronized
    fun shutdown() {
        if (PcmPlayerManager.runnable != null) {
            var readyToExit = true

            for (index in 0 until 2) {
                if (this == PcmPlayerManager.runnable!!.monoSterioPlayers[index]) {
                    PcmPlayerManager.runnable!!.monoSterioPlayers[index] = null
                }

                if (PcmPlayerManager.runnable!!.monoSterioPlayers[index] != null) {
                    readyToExit = false
                }
            }

            if (readyToExit) {
                PcmPlayerManager.runnable!!.exit = true

                while (PcmPlayerManager.runnable!!.running) {
                    TimeTools.sleep(50L)
                }

                PcmPlayerManager.runnable = null
            }
        }

        close()
        this.bitpackedSoundData = null
        this.shutdown = true
    }

    fun method4337(bitpackedSoundData: IntArray) {
        var bufferLength = 256
        if (PcmPlayerManager.stereo) {
            bufferLength = 512
        }

        Arrays.fill(bitpackedSoundData, 0, bufferLength, 0)
        this.anInt3117 -= 256
        if (this.currentPcmStream != null && this.anInt3117 <= 0) {
            this.anInt3117 += PcmPlayerManager.sampleRate shr 4
            method4338(this.currentPcmStream!!)
            method4339(this.currentPcmStream!!, this.currentPcmStream!!.getPriority())
            var i_4 = 0
            var i_5 = 255

            var i_6: Int
            var inactive: PcmStream?
            label107@ for (i_6 in 7 downTo 0) {
                if (i_5 == 0) break

                val i_16: Int
                val i_17: Int
                if (i_6 < 0) {
                    i_16 = i_6 and 0x3
                    i_17 = -(i_6 shr 2)
                } else {
                    i_16 = i_6
                    i_17 = 0
                }

                var i_9 = i_5 ushr i_16 and 0x11111111
                while (i_9 != 0) {
                    if ((i_9 and 0x1) != 0) {
                        i_5 = i_5 and (1 shl i_16).inv()
                        inactive = null
                        var activeStream = this.activeStreams[i_16]

                        label101@ while (true) {
                            while (true) {
                                if (activeStream == null) {
                                    break@label101
                                }

                                val class282Sub2612 = activeStream.buffer
                                if (class282Sub2612 != null && class282Sub2612.anInt7690 > i_17) {
                                    i_5 = i_5 or (1 shl i_16)
                                    inactive = activeStream
                                    activeStream = activeStream.nextStream
                                } else {
                                    activeStream.fill = true
                                    val i_13 = activeStream.getActiveVoiceCount()
                                    i_4 += i_13
                                    if (class282Sub2612 != null) {
                                        class282Sub2612.anInt7690 += i_13
                                    }

                                    if (i_4 >= this.anInt3123) {
                                        break@label107
                                    }

                                    var class282Sub1514: PcmStream? = activeStream.getFirstStream()
                                    if (class282Sub1514 != null) {
                                        val i_15 = activeStream.streamPriority
                                        while (class282Sub1514 != null) {
                                            method4339(class282Sub1514, i_15 * class282Sub1514.getPriority() shr 8)
                                            class282Sub1514 = activeStream.getNextStream()
                                        }
                                    }

                                    val active = activeStream.nextStream
                                    activeStream.nextStream = null
                                    if (inactive == null) {
                                        this.activeStreams[i_16] = active
                                    } else {
                                        inactive.nextStream = active
                                    }

                                    if (active == null) {
                                        this.inactiveStreams[i_16] = inactive
                                    }

                                    activeStream = active
                                }
                            }
                        }
                    }

                    i_9 = i_9 ushr 4
                }
            }

            for (index in 0 until 8) {
                val class282Sub157 = this.activeStreams[index]
                val arr_8 = this.activeStreams
                this.inactiveStreams[index] = null

                var current = class282Sub157
                arr_8[index] = null
                while (current != null) {
                    inactive = current.nextStream
                    current.nextStream = null
                    current = inactive
                }
            }
        }

        if (this.anInt3117 < 0) {
            this.anInt3117 = 0
        }

        if (this.currentPcmStream != null) {
            this.currentPcmStream!!.fill(bitpackedSoundData, 0, 256)
        }

        this.timeLastCycle = Utils.time()
    }

    fun method4339(next: PcmStream, priority: Int) {
        val i_3 = priority shr 5
        val inactive = this.inactiveStreams[i_3]
        if (inactive == null) {
            this.activeStreams[i_3] = next
        } else {
            inactive.nextStream = next
        }

        this.inactiveStreams[i_3] = next
        next.streamPriority = priority
    }

    open fun dataLeft(): Int {
        return this.capacity
    }

    open fun writeToBuffer() {
        // NOOP
    }

    open fun close() {
        // NOOP
    }

    @Throws(Exception::class)
    open fun open() {
        // NOOP
    }

    @Throws(Exception::class)
    open fun play(i_1: Int) {
        // NOOP
    }

    open fun initialize() {
        // NOOP
    }

    fun method4373() {
        this.anInt3117 -= 256
        if (this.anInt3117 < 0) {
            this.anInt3117 = 0
        }

        if (this.currentPcmStream != null) {
            this.currentPcmStream!!.skip(256)
        }
    }

    companion object {
        @JvmStatic
        fun method4338(stream: PcmStream) {
            stream.fill = false
            if (stream.buffer != null) {
                stream.buffer!!.anInt7690 = 0
            }

            var patchStream: PcmStream? = stream.getFirstStream()
            while (patchStream != null) {
                method4338(patchStream)
                patchStream = stream.getNextStream()
            }
        }
    }
}
