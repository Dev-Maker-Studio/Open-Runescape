package com.jagex.game.runetek5.config.rsinterface.cs2

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.IntNode
import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.cache.node.lru.CacheableNode
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.trigger.ClientTriggerType
import com.jagex.game.runetek5.utils.MathUtils

class CS2Script(buffer: Packet) : CacheableNode() {

    @JvmField var triggerType: ClientTriggerType? = null
    @JvmField var intLocalsCount: Int = 0
    @JvmField var stringLocalsCount: Int = 0
    @JvmField var longLocalsCount: Int = 0
    @JvmField var intArgsCount: Int = 0
    @JvmField var stringArgsCount: Int = 0
    @JvmField var longArgsCount: Int = 0
    @JvmField var switchMaps: Array<IterableNodeMap<IntNode>?>? = null
    @JvmField var scriptName: String? = null
    @JvmField var operations: Array<ClientScriptOpCode>? = null
    @JvmField var stringOpValues: Array<String?>? = null
    @JvmField var intOpValues: IntArray? = null
    @JvmField var longOpValues: LongArray? = null

    init {
        val end = decodeHeader(buffer)
        var i_3 = 0

        while (buffer.currentPosition < end) {
            val cs2opinfo5 = getOpcode(buffer)
            decodeInstruction(buffer, i_3, cs2opinfo5)
            i_3++
        }
    }

    fun getOpcode(buffer: Packet): ClientScriptOpCode {
        val opcodeId = buffer.readUShort()
        val operation = ClientScriptOpCode.getByOpcode(opcodeId)
        if (operation != null) {
            return operation
        }
        throw RuntimeException("")
    }

    fun decodeHeader(stream: Packet): Int {
        stream.currentPosition = stream.buffer.size - 2
        val i_2 = stream.readUShort()
        val i_3 = stream.buffer.size - 2 - i_2 - 16
        stream.currentPosition = i_3
        val i_4 = stream.readInt()
        intLocalsCount = stream.readUShort()
        stringLocalsCount = stream.readUShort()
        longLocalsCount = stream.readUShort()
        intArgsCount = stream.readUShort()
        stringArgsCount = stream.readUShort()
        longArgsCount = stream.readUShort()
        val i_5 = stream.readUByte()
        if (i_5 > 0) {
            switchMaps = arrayOfNulls(i_5)

            for (i_6 in 0 until i_5) {
                var i_7 = stream.readUShort()
                val iterablenodemap8 = IterableNodeMap<IntNode>(MathUtils.nextPowerOfTwo(i_7))
                switchMaps!![i_6] = iterablenodemap8

                while (i_7-- > 0) {
                    val i_9 = stream.readInt()
                    val i_10 = stream.readInt()
                    iterablenodemap8.put(IntNode(i_10), i_9.toLong())
                }
            }
        }

        stream.currentPosition = 0
        scriptName = stream.readNullString()
        operations = arrayOfNulls<ClientScriptOpCode>(i_4) as Array<ClientScriptOpCode>
        return i_3
    }

    fun decodeInstruction(buffer: Packet, op: Int, instruction: ClientScriptOpCode) {
        val operationsCount = operations!!.size
        if (instruction == ClientScriptOpCode.PUSH_STRING) {
            if (stringOpValues == null) {
                stringOpValues = arrayOfNulls(operationsCount)
            }

            var string_5 = buffer.readString()
            if (string_5.lowercase().contains("runescape")) {
                string_5 = string_5.replace("runescape", "darkan", ignoreCase = false)
                string_5 = string_5.replace("RuneScape", "Darkan", ignoreCase = false)
                string_5 = string_5.replace("Runescape", "Darkan", ignoreCase = false)
            }

            stringOpValues!![op] = string_5.intern()
        } else if (instruction == ClientScriptOpCode.PUSH_LONG) {
            if (longOpValues == null) {
                longOpValues = LongArray(operationsCount)
            }

            longOpValues!![op] = buffer.readLong()
        } else {
            if (intOpValues == null) {
                intOpValues = IntArray(operationsCount)
            }

            if (instruction.hasIntConstant) {
                intOpValues!![op] = buffer.readInt()
            } else {
                intOpValues!![op] = buffer.readUByte()
            }
        }

        operations!![op] = instruction
    }

    companion object {
        @JvmStatic
        fun getCs2Script(scriptId: Int): CS2Script? {
            var cs2script = CS2Executor.CS2_CACHE.get(scriptId.toLong()) as CS2Script?
            if (cs2script != null) {
                return cs2script
            }
            val cs2Data = Resource.CS2!!.getFile(scriptId, 0)
            if (cs2Data != null && cs2Data.size > 1) {
                try {
                    cs2script = decodeScript(cs2Data)
                } catch (exception: Exception) {
                    throw RuntimeException(exception.message + " " + scriptId)
                }
                CS2Executor.CS2_CACHE.put(cs2script, scriptId.toLong())
                return cs2script
            }
            return null
        }

        @JvmStatic
        fun decodeScript(scriptData: ByteArray): CS2Script {
            return CS2Script(Packet(scriptData))
        }

        @JvmStatic
        fun getScript(event: ClientTriggerType, scriptId: Int, i_2: Int): CS2Script? {
            var scriptFileId = event.id or (scriptId shl 10)
            var script = CS2Executor.CS2_CACHE.get((scriptFileId.toLong() shl 16)) as CS2Script?
            if (script != null) {
                return script
            }
            var scriptData = Resource.CS2!!.getFile(Resource.CS2!!.getFileId(scriptFileId))
            if (scriptData != null) {
                if (scriptData.size <= 1) {
                    return null
                }
                try {
                    script = decodeScript(scriptData)
                } catch (exception: Exception) {
                    throw RuntimeException(exception.message + " " + scriptFileId)
                }

                script.triggerType = event
                CS2Executor.CS2_CACHE.put(script, scriptFileId.toLong() shl 16)
                return script
            }
            scriptFileId = event.id or ((i_2 + 65536) shl 10)
            script = CS2Executor.CS2_CACHE.get((scriptFileId.toLong() shl 16)) as CS2Script?
            if (script != null) {
                return script
            }
            scriptData = Resource.CS2!!.getFile(Resource.CS2!!.getFileId(scriptFileId))
            if (scriptData != null) {
                if (scriptData.size <= 1) {
                    return null
                }
                try {
                    script = decodeScript(scriptData)
                } catch (exception: Exception) {
                    throw RuntimeException(exception.message + " " + scriptFileId)
                }

                script.triggerType = event
                CS2Executor.CS2_CACHE.put(script, scriptFileId.toLong() shl 16)
                return script
            }
            scriptFileId = event.id or 0x3fffc00
            script = CS2Executor.CS2_CACHE.get((scriptFileId.toLong() shl 16)) as CS2Script?
            if (script != null) {
                return script
            }
            scriptData = Resource.CS2!!.getFile(Resource.CS2!!.getFileId(scriptFileId))
            if (scriptData != null) {
                if (scriptData.size <= 1) {
                    return null
                }
                try {
                    script = decodeScript(scriptData)
                } catch (exception: Exception) {
                    throw RuntimeException(exception.message + " " + scriptFileId)
                }

                script.triggerType = event
                CS2Executor.CS2_CACHE.put(script, scriptFileId.toLong() shl 16)
                return script
            }
            return null
        }
    }
}
