package com.jagex.game.runetek5.sound.jagtheora

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.cache.node.impl.ObjectNode
import com.jagex.game.runetek5.js5.Js5ResourceProvider

/**
 *
 * @author Brainpower - 28/03/2022
 * Naming from OpenOSRS
 * Reference: https://www.rune-server.ee/runescape-development/rs-503-client-server/help/650307-742-music-midi-decoding.html
 * Reference: https://www.rune-server.ee/runescape-development/rs-503-client-server/snippets/311669-rs-music-file-structure-conversion.html
 * Reference: https://github.com/open-osrs/runelite
 *
 */
class MusicTrack(inputPacket: Packet) : Node<MusicTrack>() {

    @JvmField
    var midi: ByteArray? = null
    @JvmField
    var table: IterableNodeMap<ObjectNode>? = null

    init {
        inputPacket.currentPosition = inputPacket.buffer.size - 3
        val trackCount = inputPacket.readUByte()
        val division = inputPacket.readUShort()
        var midiOutputSize = trackCount * 10 + 14
        inputPacket.currentPosition = 0
        var tempoEventCount = 0
        var controllerChangeCount = 0
        var noteOnCount = 0
        var noteOffCount = 0
        var pitchBendCount = 0
        var channelPressureCount = 0
        var keyPressureCount = 0
        var programChangeCount = 0

        var trackIndex: Int
        var lastEventType: Int
        var eventByte: Int
        trackIndex = 0
        while (trackIndex < trackCount) {
            lastEventType = -1

            while (true) {
                eventByte = inputPacket.readUByte()
                if (eventByte != lastEventType) {
                    ++midiOutputSize
                }

                lastEventType = eventByte and 0xf
                if (eventByte == 7) {
                    break
                }

                if (eventByte == 23) {
                    ++tempoEventCount
                } else if (lastEventType == 0) {
                    ++noteOnCount
                } else if (lastEventType == 1) {
                    ++noteOffCount
                } else if (lastEventType == 2) {
                    ++controllerChangeCount
                } else if (lastEventType == 3) {
                    ++pitchBendCount
                } else if (lastEventType == 4) {
                    ++channelPressureCount
                } else if (lastEventType == 5) {
                    ++keyPressureCount
                } else {
                    if (lastEventType != 6) {
                        throw RuntimeException()
                    }

                    ++programChangeCount
                }
            }
            ++trackIndex
        }

        midiOutputSize += tempoEventCount * 5
        midiOutputSize += (noteOnCount + noteOffCount + controllerChangeCount + pitchBendCount + keyPressureCount) * 2
        midiOutputSize += channelPressureCount + programChangeCount
        trackIndex = inputPacket.currentPosition
        lastEventType = trackCount + tempoEventCount + controllerChangeCount + noteOnCount + noteOffCount + pitchBendCount + channelPressureCount + keyPressureCount + programChangeCount

        for (eventByte in 0 until lastEventType) {
            inputPacket.readVarInt()
        }

        midiOutputSize += inputPacket.currentPosition - trackIndex
        eventByte = inputPacket.currentPosition
        var modWheelMsbCount = 0
        var modWheelLsbCount = 0
        var volumeMsbCount = 0
        var volumeLsbCount = 0
        var panMsbCount = 0
        var panLsbCount = 0
        var nrpnMsbCount = 0
        var nrpnLsbCount = 0
        var rpnMsbCount = 0
        var rpnLsbCount = 0
        var switchControllerCount = 0
        var otherControllerCount = 0
        var controllerNumber = 0

        var controllerIndex = 0
        while (controllerIndex < controllerChangeCount) {
            controllerNumber = controllerNumber + inputPacket.readUByte() and 0x7f
            if (controllerNumber != 0 && controllerNumber != 32) {
                if (controllerNumber == 1) {
                    ++modWheelMsbCount
                } else if (controllerNumber == 33) {
                    ++modWheelLsbCount
                } else if (controllerNumber == 7) {
                    ++volumeMsbCount
                } else if (controllerNumber == 39) {
                    ++volumeLsbCount
                } else if (controllerNumber == 10) {
                    ++panMsbCount
                } else if (controllerNumber == 42) {
                    ++panLsbCount
                } else if (controllerNumber == 99) {
                    ++nrpnMsbCount
                } else if (controllerNumber == 98) {
                    ++nrpnLsbCount
                } else if (controllerNumber == 101) {
                    ++rpnMsbCount
                } else if (controllerNumber == 100) {
                    ++rpnLsbCount
                } else if (controllerNumber != 64 && controllerNumber != 65 && controllerNumber != 120 && controllerNumber != 121 && controllerNumber != 123) {
                    ++otherControllerCount
                } else {
                    ++switchControllerCount
                }
            } else {
                ++programChangeCount
            }
            controllerIndex++
        }

        var switchControllerOffset = inputPacket.currentPosition
        inputPacket.currentPosition += switchControllerCount
        var keyPressureOffset = inputPacket.currentPosition
        inputPacket.currentPosition += keyPressureCount
        var channelPressureOffset = inputPacket.currentPosition
        inputPacket.currentPosition += channelPressureCount
        var pitchBendHighOffset = inputPacket.currentPosition
        inputPacket.currentPosition += pitchBendCount
        var modWheelMsbOffset = inputPacket.currentPosition
        inputPacket.currentPosition += modWheelMsbCount
        var volumeMsbOffset = inputPacket.currentPosition
        inputPacket.currentPosition += volumeMsbCount
        var panMsbOffset = inputPacket.currentPosition
        inputPacket.currentPosition += panMsbCount
        var noteKeyOffset = inputPacket.currentPosition
        inputPacket.currentPosition += noteOnCount + noteOffCount + keyPressureCount
        var noteOnVelocityOffset = inputPacket.currentPosition
        inputPacket.currentPosition += noteOnCount
        var otherControllerOffset = inputPacket.currentPosition
        inputPacket.currentPosition += otherControllerCount
        var noteOffVelocityOffset = inputPacket.currentPosition
        inputPacket.currentPosition += noteOffCount
        var modWheelLsbOffset = inputPacket.currentPosition
        inputPacket.currentPosition += modWheelLsbCount
        var volumeLsbOffset = inputPacket.currentPosition
        inputPacket.currentPosition += volumeLsbCount
        var panLsbOffset = inputPacket.currentPosition
        inputPacket.currentPosition += panLsbCount
        var programChangeOffset = inputPacket.currentPosition
        inputPacket.currentPosition += programChangeCount
        var pitchBendLowOffset = inputPacket.currentPosition
        inputPacket.currentPosition += pitchBendCount
        var nrpnMsbDataOffset = inputPacket.currentPosition
        inputPacket.currentPosition += nrpnMsbCount
        var nrpnLsbDataOffset = inputPacket.currentPosition
        inputPacket.currentPosition += nrpnLsbCount
        var rpnMsbDataOffset = inputPacket.currentPosition
        inputPacket.currentPosition += rpnMsbCount
        var rpnLsbDataOffset = inputPacket.currentPosition
        inputPacket.currentPosition += rpnLsbCount
        var tempoOffset = inputPacket.currentPosition
        inputPacket.currentPosition += tempoEventCount * 3
        this.midi = ByteArray(midiOutputSize)
        val midiOutput = Packet(this.midi!!)
        midiOutput.writeInt(1297377380)
        midiOutput.writeInt(6)
        midiOutput.writeShort(if (trackCount > 1) 1 else 0)
        midiOutput.writeShort(trackCount)
        midiOutput.writeShort(division)
        inputPacket.currentPosition = trackIndex
        var channelXor = 0
        var noteKey = 0
        var noteOnVelocity = 0
        var noteOffVelocity = 0
        var pitchBendAccum = 0
        var channelPressureAccum = 0
        var keyPressureAccum = 0
        var controllerValues = IntArray(128)
        controllerNumber = 0

        var currentTrack = 0
        outerLoop@ while (currentTrack < trackCount) {
            midiOutput.writeInt(1297379947)
            midiOutput.currentPosition += 4
            val trackStartPos = midiOutput.currentPosition
            var previousEventType = -1

            while (true) {
                while (true) {
                    val deltaTime = inputPacket.readVarInt()
                    midiOutput.writeFlags(deltaTime)
                    val encodedEvent = inputPacket.buffer[controllerIndex++].toInt() and 0xff
                    val isNewEventType = encodedEvent != previousEventType
                    previousEventType = encodedEvent and 0xf
                    if (encodedEvent == 7) {
                        if (isNewEventType) {
                            midiOutput.writeByte(255)
                        }

                        midiOutput.writeByte(47)
                        midiOutput.writeByte(0)
                        midiOutput.writeIntLength(midiOutput.currentPosition - trackStartPos)
                        currentTrack++
                        continue@outerLoop
                    }

                    if (encodedEvent == 23) {
                        if (isNewEventType) {
                            midiOutput.writeByte(255)
                        }

                        midiOutput.writeByte(81)
                        midiOutput.writeByte(3)
                        midiOutput.writeByte((inputPacket.buffer[tempoOffset++].toInt() and 0xff))
                        midiOutput.writeByte((inputPacket.buffer[tempoOffset++].toInt() and 0xff))
                        midiOutput.writeByte((inputPacket.buffer[tempoOffset++].toInt() and 0xff))
                    } else {
                        channelXor = channelXor xor (encodedEvent shr 4)
                        if (previousEventType == 0) {
                            if (isNewEventType) {
                                midiOutput.writeByte(channelXor + 144)
                            }

                            noteKey += (inputPacket.buffer[noteKeyOffset++].toInt() and 0xff)
                            noteOnVelocity += (inputPacket.buffer[noteOnVelocityOffset++].toInt() and 0xff)
                            midiOutput.writeByte(noteKey and 0x7f)
                            midiOutput.writeByte(noteOnVelocity and 0x7f)
                        } else if (previousEventType == 1) {
                            if (isNewEventType) {
                                midiOutput.writeByte(channelXor + 128)
                            }

                            noteKey += (inputPacket.buffer[noteKeyOffset++].toInt() and 0xff)
                            noteOffVelocity += (inputPacket.buffer[noteOffVelocityOffset++].toInt() and 0xff)
                            midiOutput.writeByte(noteKey and 0x7f)
                            midiOutput.writeByte(noteOffVelocity and 0x7f)
                        } else if (previousEventType == 2) {
                            if (isNewEventType) {
                                midiOutput.writeByte(channelXor + 176)
                            }

                            controllerNumber = controllerNumber + (inputPacket.buffer[eventByte++].toInt() and 0xff) and 0x7f
                            midiOutput.writeByte(controllerNumber)
                            val controllerValue: Byte
                            if (controllerNumber != 0 && controllerNumber != 32) {
                                controllerValue = when (controllerNumber) {
                                    1 -> inputPacket.buffer[modWheelMsbOffset++]
                                    33 -> inputPacket.buffer[modWheelLsbOffset++]
                                    7 -> inputPacket.buffer[volumeMsbOffset++]
                                    39 -> inputPacket.buffer[volumeLsbOffset++]
                                    10 -> inputPacket.buffer[panMsbOffset++]
                                    42 -> inputPacket.buffer[panLsbOffset++]
                                    99 -> inputPacket.buffer[nrpnMsbDataOffset++]
                                    98 -> inputPacket.buffer[nrpnLsbDataOffset++]
                                    101 -> inputPacket.buffer[rpnMsbDataOffset++]
                                    100 -> inputPacket.buffer[rpnLsbDataOffset++]
                                    else -> if (controllerNumber != 64 && controllerNumber != 65 && controllerNumber != 120 && controllerNumber != 121 && controllerNumber != 123) {
                                        inputPacket.buffer[otherControllerOffset++]
                                    } else {
                                        inputPacket.buffer[switchControllerOffset++]
                                    }
                                }
                            } else {
                                controllerValue = inputPacket.buffer[programChangeOffset++]
                            }

                            val accumulatedValue = controllerValue.toInt() + controllerValues[controllerNumber]
                            controllerValues[controllerNumber] = accumulatedValue
                            midiOutput.writeByte(accumulatedValue and 0x7f)
                        } else if (previousEventType == 3) {
                            if (isNewEventType) {
                                midiOutput.writeByte(channelXor + 224)
                            }

                            pitchBendAccum += (inputPacket.buffer[pitchBendLowOffset++].toInt() and 0xff)
                            pitchBendAccum += (inputPacket.buffer[pitchBendHighOffset++].toInt() and 0xff) shl 7
                            midiOutput.writeByte(pitchBendAccum and 0x7f)
                            midiOutput.writeByte(pitchBendAccum shr 7 and 0x7f)
                        } else if (previousEventType == 4) {
                            if (isNewEventType) {
                                midiOutput.writeByte(channelXor + 208)
                            }

                            channelPressureAccum += (inputPacket.buffer[channelPressureOffset++].toInt() and 0xff)
                            midiOutput.writeByte(channelPressureAccum and 0x7f)
                        } else if (previousEventType == 5) {
                            if (isNewEventType) {
                                midiOutput.writeByte(channelXor + 160)
                            }

                            noteKey += (inputPacket.buffer[noteKeyOffset++].toInt() and 0xff)
                            keyPressureAccum += (inputPacket.buffer[keyPressureOffset++].toInt() and 0xff)
                            midiOutput.writeByte(noteKey and 0x7f)
                            midiOutput.writeByte(keyPressureAccum and 0x7f)
                        } else {
                            if (previousEventType != 6) {
                                throw RuntimeException()
                            }

                            if (isNewEventType) {
                                midiOutput.writeByte(channelXor + 192)
                            }

                            midiOutput.writeByte((inputPacket.buffer[programChangeOffset++].toInt() and 0xff))
                        }
                    }
                }
            }
        }
    }

    fun buildPatchTable() {
        if (this.table == null) {
            this.table = IterableNodeMap(16)
            val bankSelects = IntArray(16)
            val patchPrograms = IntArray(16)
            patchPrograms[9] = 128
            bankSelects[9] = 128
            val reader = MidiFileReader(this.midi!!)
            val trackCount = reader.getTrackCount()

            var track: Int
            for (track in 0 until trackCount) {
                reader.getGotoTrack(track)
                reader.getTrackLength(track)
                reader.markTracePosition(track)
            }

            outerLoop@ do {
                while (true) {
                    track = reader.getPrioritizedTrack()
                    val trackLength = reader.trackLengths!![track]

                    while (trackLength == reader.trackLengths!![track]) {
                        reader.getGotoTrack(track)
                        val message = reader.readMessage(track)
                        if (message == 1) {
                            reader.setTrackDone()
                            reader.markTracePosition(track)
                            continue@outerLoop
                        }

                        val status = message and 0xf0
                        var channel: Int
                        var data1: Int
                        var data2: Int
                        if (status == 176) {
                            channel = message and 0xf
                            data1 = message shr 8 and 0x7f
                            data2 = message shr 16 and 0x7f
                            if (data1 == 0) {
                                bankSelects[channel] = (data2 shl 14) + (bankSelects[channel] and -2080769)
                            }

                            if (data1 == 32) {
                                bankSelects[channel] = (bankSelects[channel] and -16257) + (data2 shl 7)
                            }
                        }

                        if (status == 192) {
                            channel = message and 0xf
                            data1 = message shr 8 and 0x7f
                            patchPrograms[channel] = data1 + bankSelects[channel]
                        }

                        if (status == 144) {
                            channel = message and 0xf
                            data1 = message shr 8 and 0x7f
                            data2 = message shr 16 and 0x7f
                            if (data2 > 0) {
                                val patchId = patchPrograms[channel]
                                var patchNode = this.table!!.get(patchId.toLong())
                                if (patchNode == null) {
                                    patchNode = ObjectNode(ByteArray(128))
                                    this.table!!.put(patchNode, patchId.toLong())
                                }

                                (patchNode.value as ByteArray)[data1] = 1
                            }
                        }

                        reader.getTrackLength(track)
                        reader.markTracePosition(track)
                    }
                }
            } while (!reader.isDone())
        }
    }

    fun clear() {
        this.table = null
    }

    companion object {
        @JvmStatic
        fun loadMusictrack(resource: Js5ResourceProvider, archiveId: Int, fileId: Int): MusicTrack? {
            val data = resource.getFile(archiveId, fileId)
            return if (data == null) null else MusicTrack(Packet(data))
        }
    }
}
