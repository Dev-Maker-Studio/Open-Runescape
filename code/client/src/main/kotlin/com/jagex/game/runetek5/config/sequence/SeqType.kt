package com.jagex.game.runetek5.config.sequence

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.IntNode
import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.cache.node.impl.ObjectNode
import com.jagex.game.runetek5.utils.MathUtils

class SeqType {

    @JvmField var id: Int = 0
    @JvmField var durations: IntArray? = null
    @JvmField var replayInterval: Int = -1
    @JvmField var interLeaveOrder: BooleanArray? = null
    @JvmField var frames: IntArray? = null
    @JvmField var priority: Int = 5
    @JvmField var replacementShield: Int = -1
    @JvmField var replacementWeapon: Int = -1
    @JvmField var replayCount: Int = 99
    @JvmField var interruptType: Int = -1
    @JvmField var moveType: Int = -1
    @JvmField var loopType: Int = 2
    @JvmField var interfaceFrames: IntArray? = null
    @JvmField var frameSounds: Array<IntArray?>? = null
    @JvmField var lights: Boolean = false
    @JvmField var tweened: Boolean = false
    @JvmField var vorbis: Boolean = false
    @JvmField var frameSoundVolume: IntArray? = null
    @JvmField var soundMaxDelay: IntArray? = null
    @JvmField var soundMinDelay: IntArray? = null
    @JvmField var seqtypelist: SeqTypeList? = null
    @JvmField var parameters: IterableNodeMap<Node<*>>? = null

    fun decode(stream: Packet) {
        while (true) {
            val opcode = stream.readUByte()
            if (opcode == 0) {
                return
            }
            decode(stream, opcode)
        }
    }

    fun decode(stream: Packet, opcode: Int) {
        if (opcode == 1) {
            val count = stream.readUShort()
            durations = IntArray(count)
            for (index in 0 until count) {
                durations!![index] = stream.readUShort()
            }
            frames = IntArray(count)
            for (index in 0 until count) {
                frames!![index] = stream.readUShort()
            }
            for (index in 0 until count) {
                frames!![index] += stream.readUShort() shl 16
            }
        } else if (opcode == 2) {
            replayInterval = stream.readUShort()
        } else if (opcode == 3) {
            interLeaveOrder = BooleanArray(256)
            val count = stream.readUByte()
            for (index in 0 until count) {
                interLeaveOrder!![stream.readUByte()] = true
            }
        } else if (opcode == 5) {
            priority = stream.readUByte()
        } else if (opcode == 6) {
            replacementShield = stream.readUShort()
        } else if (opcode == 7) {
            replacementWeapon = stream.readUShort()
        } else if (opcode == 8) {
            replayCount = stream.readUByte()
        } else if (opcode == 9) {
            interruptType = stream.readUByte()
        } else if (opcode == 10) {
            moveType = stream.readUByte()
        } else if (opcode == 11) {
            loopType = stream.readUByte()
        } else if (opcode == 12) {
            val count = stream.readUByte()
            interfaceFrames = IntArray(count)
            for (index in 0 until count) {
                interfaceFrames!![index] = stream.readUShort()
            }
            for (index in 0 until count) {
                interfaceFrames!![index] += stream.readUShort() shl 16
            }
        } else if (opcode == 13) {
            val frameCount = stream.readUShort()
            frameSounds = arrayOfNulls(frameCount)
            for (frame in 0 until frameCount) {
                val soundCount = stream.readUByte()
                if (soundCount > 0) {
                    frameSounds!![frame] = IntArray(soundCount)
                    frameSounds!![frame]!![0] = stream.readUMedium()
                    for (alternativeSound in 1 until soundCount) {
                        frameSounds!![frame]!![alternativeSound] = stream.readUShort()
                    }
                }
            }
        } else if (opcode == 14) {
            lights = true
        } else if (opcode == 15) {
            tweened = true
        } else if (opcode == 18) {
            vorbis = true
        } else if (opcode == 19) {
            if (frameSoundVolume == null) {
                frameSoundVolume = IntArray(frameSounds!!.size)
                for (count in frameSounds!!.indices) {
                    frameSoundVolume!![count] = 255
                }
            }
            frameSoundVolume!![stream.readUByte()] = stream.readUByte()
        } else if (opcode == 20) {
            if (soundMaxDelay == null || soundMinDelay == null) {
                soundMaxDelay = IntArray(frameSounds!!.size)
                soundMinDelay = IntArray(frameSounds!!.size)
                for (count in frameSounds!!.indices) {
                    soundMaxDelay!![count] = 256
                    soundMinDelay!![count] = 256
                }
            }
            val count = stream.readUByte()
            soundMaxDelay!![count] = stream.readUShort()
            soundMinDelay!![count] = stream.readUShort()
        } else if (opcode == 249) {
            var count = stream.readUByte()
            if (parameters == null) {
                val index = MathUtils.nextPowerOfTwo(count)
                parameters = IterableNodeMap(index)
            }
            for (index in 0 until count) {
                val isString = stream.readUByte() == 1
                val key = stream.readUMedium()
                val value: Node<*>
                if (isString) {
                    value = ObjectNode(stream.readString())
                } else {
                    value = IntNode(stream.readInt())
                }
                parameters!!.put(value, key.toLong())
            }
        }
    }

    fun postDecode() {
        if (interruptType == -1) {
            if (interLeaveOrder != null) {
                interruptType = 2
            } else {
                interruptType = 0
            }
        }
        if (moveType == -1) {
            if (interLeaveOrder != null) {
                moveType = 2
            } else {
                moveType = 0
            }
        }
    }

    fun ready(): Boolean {
        if (frames == null) {
            return true
        }
        var fullyCached = true
        for (index in frames!!.indices) {
            val frame = frames!![index]
            if (seqtypelist!!.listFrameset(frame ushr 16) == null) {
                fullyCached = false
            }
        }
        return fullyCached
    }

    fun getCs2Integer(key: Int, defaultValue: Int): Int {
        if (parameters == null) {
            return defaultValue
        }
        val cs2Entry = parameters!!.get(key.toLong()) as IntNode?
        return cs2Entry?.value ?: defaultValue
    }

    fun getCs2Object(key: Int, defaultResult: String): String {
        if (parameters == null) {
            return defaultResult
        }
        val objectNode = parameters!!.get(key.toLong()) as ObjectNode?
        return (objectNode?.value as? String) ?: defaultResult
    }
}
