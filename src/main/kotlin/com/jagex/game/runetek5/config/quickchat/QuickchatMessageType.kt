package com.jagex.game.runetek5.config.quickchat

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.CacheableNode
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.material.texture.ColorImageCache
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Interpreter
import com.jagex.game.runetek5.config.staticelements.StaticElements
import com.jagex.game.runetek5.utils.Utils

class QuickchatMessageType : CacheableNode() {

    @JvmField var responses: IntArray? = null
    @JvmField var searchable = true
    @JvmField var quickchatMessageTypelist: QuickchatMessageTypeList? = null
    @JvmField var message: Array<String>? = null
    @JvmField var valueTypes: IntArray? = null
    @JvmField var configs: Array<IntArray?>? = null

    fun decodeOpcode(buffer: Packet, opcode: Int) {
        when (opcode) {
            1 -> message = Utils.split(buffer.readString(), '<')
            2 -> {
                val count = buffer.readUByte()
                responses = IntArray(count)
                for (index in 0 until count) {
                    responses!![index] = buffer.readUShort()
                }
            }
            3 -> {
                val count = buffer.readUByte()
                valueTypes = IntArray(count)
                configs = arrayOfNulls(count)
                for (index in 0 until count) {
                    val typeId = buffer.readUShort()
                    val type = QuickChatValueType.get(typeId)
                    if (type != null) {
                        valueTypes!![index] = typeId
                        configs!![index] = IntArray(type.parameterCount)
                        for (config in 0 until type.parameterCount) {
                            configs!![index]!![config] = buffer.readUShort()
                        }
                    }
                }
            }
            4 -> searchable = false
        }
    }

    fun encodeValues(buffer: Packet, ints_2: IntArray) {
        if (valueTypes != null) {
            for (typeIndex in valueTypes!!.indices) {
                if (typeIndex >= ints_2.size) break
                val size = getValueType(typeIndex)!!.clientTransmitSize
                if (size > 0) {
                    buffer.writeVariableLengthLong(ints_2[typeIndex].toLong(), size)
                }
            }
        }
    }

    fun fillDynamicValues(buffer: Packet): String {
        val builder = StringBuilder(80)
        if (valueTypes != null) {
            for (valueTypeIndex in valueTypes!!.indices) {
                builder.append(message!![valueTypeIndex])
                val valueType = getValueType(valueTypeIndex)!!
                val serverTransmitSize = QuickChatValueType.get(valueTypes!![valueTypeIndex])!!.serverTransmitSize
                val bitpackedData = buffer.bitpackLong(serverTransmitSize)
                builder.append(quickchatMessageTypelist!!.getValueDisplay(valueType, configs!![valueTypeIndex]!!, bitpackedData))
            }
        }

        builder.append(message!![message!!.size - 1])
        return builder.toString()
    }

    fun getPlaceholderText(): String {
        val builder = StringBuilder(80)
        if (message == null) {
            return ""
        }
        builder.append(message!![0])

        for (messageIndex in 1 until message!!.size) {
            for (i_4 in 0 until 3) {
                builder.append('.')
            }
            builder.append(message!![messageIndex])
        }

        return builder.toString()
    }

    fun getValueConfig(i_1: Int, i_2: Int): Int {
        return if (valueTypes != null && i_1 >= 0 && i_1 <= valueTypes!!.size) {
            if (configs!![i_1] != null && i_2 >= 0 && i_2 <= configs!![i_1]!!.size) configs!![i_1]!![i_2] else -1
        } else -1
    }

    internal fun postDecode() {
        if (responses != null) {
            for (i_2 in responses!!.indices) {
                responses!![i_2] = responses!![i_2] or 0x8000
            }
        }
    }

    internal fun decode(buffer: Packet) {
        while (true) {
            val opcode = buffer.readUByte()
            if (opcode == 0) {
                return
            }

            decodeOpcode(buffer, opcode)
        }
    }

    fun getValueTypeCount(): Int {
        return if (valueTypes == null) 0 else valueTypes!!.size
    }

    fun getValueType(qcTypeIndex: Int): QuickChatValueType? {
        return if (valueTypes != null && qcTypeIndex >= 0 && qcTypeIndex <= valueTypes!!.size) QuickChatValueType.get(valueTypes!![qcTypeIndex]) else null
    }

    companion object {
        @JvmStatic
        fun searchQuickchatMessages(string_0: String, bool1: Boolean) {
            val searchString = string_0.lowercase()
            var shorts_3 = ShortArray(16)
            var i_4 = 0
            val i_5 = if (bool1) 32768 else 0
            val i_6 = (if (bool1) TypeLists.QUICK_CHAT_MESSAGE!!.menuCount else TypeLists.QUICK_CHAT_MESSAGE!!.messageCount) + i_5
            for (i_7 in i_5 until i_6) {
                val quickchatdefinitions12 = TypeLists.QUICK_CHAT_MESSAGE!!.listMessage(i_7)
                if (quickchatdefinitions12.searchable && quickchatdefinitions12.getPlaceholderText().lowercase().indexOf(searchString) != -1) {
                    if (i_4 >= 50) {
                        CS2Interpreter.CS2_QUERY_RESULTS_LEN = -1
                        ColorImageCache.CS2_QUERY_RESULTS = null
                        return
                    }
                    if (i_4 >= shorts_3.size) {
                        val shorts_9 = ShortArray(shorts_3.size * 2)
                        System.arraycopy(shorts_3, 0, shorts_9, 0, i_4)
                        shorts_3 = shorts_9
                    }
                    shorts_3[i_4++] = i_7.toShort()
                }
            }
            ColorImageCache.CS2_QUERY_RESULTS = shorts_3
            StaticElements.CS2_QUERY_RESULT_IDX = 0
            CS2Interpreter.CS2_QUERY_RESULTS_LEN = i_4
            val arr_11 = Array(CS2Interpreter.CS2_QUERY_RESULTS_LEN) { "" }
            for (i_8 in 0 until CS2Interpreter.CS2_QUERY_RESULTS_LEN) {
                arr_11[i_8] = TypeLists.QUICK_CHAT_MESSAGE!!.listMessage(shorts_3[i_8].toInt()).getPlaceholderText()
            }
            Utils.sortAlphabetically(arr_11, ColorImageCache.CS2_QUERY_RESULTS)
        }
    }
}
