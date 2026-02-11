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
        var instructionIndex = 0

        while (buffer.currentPosition < end) {
            val opcode = getOpcode(buffer)
            decodeInstruction(buffer, instructionIndex, opcode)
            instructionIndex++
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
        val switchBlockSize = stream.readUShort()
        val headerStart = stream.buffer.size - 2 - switchBlockSize - 16
        stream.currentPosition = headerStart
        val instructionCount = stream.readInt()
        intLocalsCount = stream.readUShort()
        stringLocalsCount = stream.readUShort()
        longLocalsCount = stream.readUShort()
        intArgsCount = stream.readUShort()
        stringArgsCount = stream.readUShort()
        longArgsCount = stream.readUShort()
        val switchTableCount = stream.readUByte()
        if (switchTableCount > 0) {
            switchMaps = arrayOfNulls(switchTableCount)

            for (switchIndex in 0 until switchTableCount) {
                var switchCaseCount = stream.readUShort()
                val switchMap = IterableNodeMap<IntNode>(MathUtils.nextPowerOfTwo(switchCaseCount))
                switchMaps!![switchIndex] = switchMap

                while (switchCaseCount-- > 0) {
                    val caseKey = stream.readInt()
                    val caseTarget = stream.readInt()
                    switchMap.put(IntNode(caseTarget), caseKey.toLong())
                }
            }
        }

        stream.currentPosition = 0
        scriptName = stream.readNullString()
        operations = arrayOfNulls<ClientScriptOpCode>(instructionCount) as Array<ClientScriptOpCode>
        return headerStart
    }

    fun decodeInstruction(buffer: Packet, op: Int, instruction: ClientScriptOpCode) {
        val operationsCount = operations!!.size
        if (instruction == ClientScriptOpCode.PUSH_STRING) {
            if (stringOpValues == null) {
                stringOpValues = arrayOfNulls(operationsCount)
            }

            var stringValue = buffer.readString()
            if (stringValue.lowercase().contains("runescape")) {
                stringValue = stringValue.replace("runescape", "darkan", ignoreCase = false)
                stringValue = stringValue.replace("RuneScape", "Darkan", ignoreCase = false)
                stringValue = stringValue.replace("Runescape", "Darkan", ignoreCase = false)
            }

            stringOpValues!![op] = stringValue.intern()
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
        fun getScript(event: ClientTriggerType, scriptId: Int, alternateId: Int): CS2Script? {
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
            scriptFileId = event.id or ((alternateId + 65536) shl 10)
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
