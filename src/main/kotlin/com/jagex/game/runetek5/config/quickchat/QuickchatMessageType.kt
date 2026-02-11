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

    fun encodeValues(buffer: Packet, responseValues: IntArray) {
        if (valueTypes != null) {
            for (typeIndex in valueTypes!!.indices) {
                if (typeIndex >= responseValues.size) break
                val size = getValueType(typeIndex)!!.clientTransmitSize
                if (size > 0) {
                    buffer.writeVariableLengthLong(responseValues[typeIndex].toLong(), size)
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
            for (dotIndex in 0 until 3) {
                builder.append('.')
            }
            builder.append(message!![messageIndex])
        }

        return builder.toString()
    }

    fun getValueConfig(valueTypeIndex: Int, configIndex: Int): Int {
        return if (valueTypes != null && valueTypeIndex >= 0 && valueTypeIndex <= valueTypes!!.size) {
            if (configs!![valueTypeIndex] != null && configIndex >= 0 && configIndex <= configs!![valueTypeIndex]!!.size) configs!![valueTypeIndex]!![configIndex] else -1
        } else -1
    }

    internal fun postDecode() {
        if (responses != null) {
            for (responseIndex in responses!!.indices) {
                responses!![responseIndex] = responses!![responseIndex] or 0x8000
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
        fun searchQuickchatMessages(query: String, isMenuSearch: Boolean) {
            val searchString = query.lowercase()
            var matchingIds = ShortArray(16)
            var matchCount = 0
            val startIndex = if (isMenuSearch) 32768 else 0
            val endIndex = (if (isMenuSearch) TypeLists.QUICK_CHAT_MESSAGE!!.menuCount else TypeLists.QUICK_CHAT_MESSAGE!!.messageCount) + startIndex
            for (messageId in startIndex until endIndex) {
                val messageType = TypeLists.QUICK_CHAT_MESSAGE!!.listMessage(messageId)
                if (messageType.searchable && messageType.getPlaceholderText().lowercase().indexOf(searchString) != -1) {
                    if (matchCount >= 50) {
                        CS2Interpreter.CS2_QUERY_RESULTS_LEN = -1
                        ColorImageCache.CS2_QUERY_RESULTS = null
                        return
                    }
                    if (matchCount >= matchingIds.size) {
                        val expandedIds = ShortArray(matchingIds.size * 2)
                        System.arraycopy(matchingIds, 0, expandedIds, 0, matchCount)
                        matchingIds = expandedIds
                    }
                    matchingIds[matchCount++] = messageId.toShort()
                }
            }
            ColorImageCache.CS2_QUERY_RESULTS = matchingIds
            StaticElements.CS2_QUERY_RESULT_IDX = 0
            CS2Interpreter.CS2_QUERY_RESULTS_LEN = matchCount
            val displayTexts = Array(CS2Interpreter.CS2_QUERY_RESULTS_LEN) { "" }
            for (resultIndex in 0 until CS2Interpreter.CS2_QUERY_RESULTS_LEN) {
                displayTexts[resultIndex] = TypeLists.QUICK_CHAT_MESSAGE!!.listMessage(matchingIds[resultIndex].toInt()).getPlaceholderText()
            }
            Utils.sortAlphabetically(displayTexts, ColorImageCache.CS2_QUERY_RESULTS)
        }
    }
}
