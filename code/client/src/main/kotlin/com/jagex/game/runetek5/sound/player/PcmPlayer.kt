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
    var streamUpdateCountdown: Int = 0
    val maxActiveVoices: Int = 32
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
                    skipAudioSamples()
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

                    var targetOffset = this.sampleRate + this.currentOffset
                    if (targetOffset + 256 > 16384) {
                        targetOffset = 16128
                    }

                    if (targetOffset + 256 > this.capacity) {
                        this.capacity += 1024
                        if (this.capacity > 16384) {
                            this.capacity = 16384
                        }

                        close()
                        play(this.capacity)
                        dataLeft = 0
                        this.enabled = true
                        if (targetOffset + 256 > this.capacity) {
                            targetOffset = this.capacity - 256
                            this.currentOffset = targetOffset - this.sampleRate
                        }
                    }

                    while (dataLeft < targetOffset) {
                        fillSoundBuffer(this.bitpackedSoundData!!)
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

    fun fillSoundBuffer(soundData: IntArray) {
        var bufferLength = 256
        if (PcmPlayerManager.stereo) {
            bufferLength = 512
        }

        Arrays.fill(soundData, 0, bufferLength, 0)
        this.streamUpdateCountdown -= 256
        if (this.currentPcmStream != null && this.streamUpdateCountdown <= 0) {
            this.streamUpdateCountdown += PcmPlayerManager.sampleRate shr 4
            resetStreamFillState(this.currentPcmStream!!)
            enqueueStreamByPriority(this.currentPcmStream!!, this.currentPcmStream!!.getPriority())
            var activeVoiceCount = 0
            var priorityBitmask = 255

            var priorityLevel = 7
            var previousStream: PcmStream?
            label107@ while (priorityBitmask != 0) {
                var bucketIndex: Int
                var voiceThreshold: Int
                if (priorityLevel < 0) {
                    bucketIndex = priorityLevel and 0x3
                    voiceThreshold = -(priorityLevel shr 2)
                } else {
                    bucketIndex = priorityLevel
                    voiceThreshold = 0
                }

                var bitmask = priorityBitmask ushr bucketIndex and 0x11111111
                while (bitmask != 0) {
                    if ((bitmask and 0x1) != 0) {
                        priorityBitmask = priorityBitmask and (1 shl bucketIndex).inv()
                        previousStream = null
                        var activeStream = this.activeStreams[bucketIndex]

                        label101@ while (true) {
                            while (true) {
                                if (activeStream == null) {
                                    break@label101
                                }

                                val audioNode = activeStream.buffer
                                if (audioNode != null && audioNode.activeVoiceCount > voiceThreshold) {
                                    priorityBitmask = priorityBitmask or (1 shl bucketIndex)
                                    previousStream = activeStream
                                    activeStream = activeStream.nextStream
                                } else {
                                    activeStream.fill = true
                                    val voiceCount = activeStream.getActiveVoiceCount()
                                    activeVoiceCount += voiceCount
                                    if (audioNode != null) {
                                        audioNode.activeVoiceCount += voiceCount
                                    }

                                    if (activeVoiceCount >= this.maxActiveVoices) {
                                        break@label107
                                    }

                                    var childStream: PcmStream? = activeStream.getFirstStream()
                                    if (childStream != null) {
                                        val parentPriority = activeStream.streamPriority
                                        while (childStream != null) {
                                            enqueueStreamByPriority(childStream, parentPriority * childStream.getPriority() shr 8)
                                            childStream = activeStream.getNextStream()
                                        }
                                    }

                                    val nextActive = activeStream.nextStream
                                    activeStream.nextStream = null
                                    if (previousStream == null) {
                                        this.activeStreams[bucketIndex] = nextActive
                                    } else {
                                        previousStream.nextStream = nextActive
                                    }

                                    if (nextActive == null) {
                                        this.inactiveStreams[bucketIndex] = previousStream
                                    }

                                    activeStream = nextActive
                                }
                            }
                        }
                    }

                    bucketIndex += 4
                    voiceThreshold++
                    bitmask = bitmask ushr 4
                }
                priorityLevel--
            }

            for (index in 0 until 8) {
                val headStream = this.activeStreams[index]
                val streamArray = this.activeStreams
                this.inactiveStreams[index] = null

                var current = headStream
                streamArray[index] = null
                while (current != null) {
                    previousStream = current.nextStream
                    current.nextStream = null
                    current = previousStream
                }
            }
        }

        if (this.streamUpdateCountdown < 0) {
            this.streamUpdateCountdown = 0
        }

        if (this.currentPcmStream != null) {
            this.currentPcmStream!!.fill(soundData, 0, 256)
        }

        this.timeLastCycle = Utils.time()
    }

    fun enqueueStreamByPriority(stream: PcmStream, priority: Int) {
        val bucketIndex = priority shr 5
        val tail = this.inactiveStreams[bucketIndex]
        if (tail == null) {
            this.activeStreams[bucketIndex] = stream
        } else {
            tail.nextStream = stream
        }

        this.inactiveStreams[bucketIndex] = stream
        stream.streamPriority = priority
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
    open fun play(bufferSize: Int) {
        // NOOP
    }

    open fun initialize() {
        // NOOP
    }

    fun skipAudioSamples() {
        this.streamUpdateCountdown -= 256
        if (this.streamUpdateCountdown < 0) {
            this.streamUpdateCountdown = 0
        }

        if (this.currentPcmStream != null) {
            this.currentPcmStream!!.skip(256)
        }
    }

    companion object {
        @JvmStatic
        fun resetStreamFillState(stream: PcmStream) {
            stream.fill = false
            if (stream.buffer != null) {
                stream.buffer!!.activeVoiceCount = 0
            }

            var childStream: PcmStream? = stream.getFirstStream()
            while (childStream != null) {
                resetStreamFillState(childStream)
                childStream = stream.getNextStream()
            }
        }
    }
}
