package com.jagex.game.runetek5.config.enumtype

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.utils.Utils
import java.io.Serializable
import java.util.*

class EnumType {

    @JvmField
    var keyType: Char = 0.toChar()

    @JvmField
    var valueType: Char = 0.toChar()

    @JvmField
    var defaultStringValue: String = "null"

    @JvmField
    var defaultIntValue: Int = 0

    @JvmField
    var size: Int = 0

    @JvmField
    var parameters: MutableMap<Int, Any>? = null

    @JvmField
    var values: Array<Any?>? = null

    @JvmField
    var valueReferences: HashMap<Any, IntArray>? = null

    fun decode(buffer: Packet) {
        while (true) {
            val opcode = buffer.readUByte()
            if (opcode == 0) {
                return
            }
            decode(buffer, opcode)
        }
    }

    fun decode(stream: Packet, opcode: Int) {
        when (opcode) {
            1 -> {
                keyType = Utils.cp1252ToChar(stream.readByte())
            }
            2 -> {
                valueType = Utils.cp1252ToChar(stream.readByte())
            }
            3 -> {
                defaultStringValue = stream.readString()
            }
            4 -> {
                defaultIntValue = stream.readInt()
            }
            5, 6 -> {
                size = stream.readUShort()
                parameters = HashMap(size)

                for (index in 0 until size) {
                    val key = stream.readInt()
                    val value: Any = if (opcode == 5) {
                        stream.readString()
                    } else {
                        stream.readInt()
                    }
                    parameters!![key] = value
                }
            }
            7, 8 -> {
                val numValues = stream.readUShort()
                size = stream.readUShort()
                values = arrayOfNulls(numValues)

                for (index in 0 until size) {
                    val valueToSet = stream.readUShort()
                    values!![valueToSet] = if (opcode == 7) {
                        stream.readString()
                    } else {
                        stream.readInt()
                    }
                }
            }
        }
    }

    fun buildValueMap() {
        val hashmap = HashMap<Any, LinkedList<Int>>()
        if (values != null) {
            for (index in values!!.indices) {
                if (values!![index] != null) {
                    val key = values!![index]!!
                    var valueIndices = hashmap[key]
                    if (valueIndices == null) {
                        valueIndices = LinkedList()
                        hashmap[key] = valueIndices
                    }
                    valueIndices.add(index)
                }
            }
        } else {
            if (parameters == null) {
                throw IllegalStateException()
            }

            val parameterIterator = parameters!!.entries.iterator()
            while (parameterIterator.hasNext()) {
                val entry = parameterIterator.next()
                val key = entry.value
                var valueIndices = hashmap[key]
                if (valueIndices == null) {
                    valueIndices = LinkedList()
                    hashmap[key] = valueIndices
                }
                valueIndices.add(entry.key)
            }
        }

        valueReferences = HashMap()
        val hashmapIterator = hashmap.entries.iterator()

        while (hashmapIterator.hasNext()) {
            val entry = hashmapIterator.next()
            val referenceList = entry.value
            val references = IntArray(referenceList.size)
            var index = 0
            val intIt = referenceList.iterator()

            while (intIt.hasNext()) {
                val reference = intIt.next()
                references[index++] = reference
            }

            if (values == null) {
                Arrays.sort(references)
            }
            valueReferences!![entry.key] = references
        }
    }

    fun getValue(key: Int): Any? {
        return if (values != null) {
            if (key >= 0 && key < values!!.size) values!![key] else null
        } else {
            parameters?.get(key)
        }
    }

    fun getStringValue(key: Int): String {
        val obj = getValue(key)
        return obj?.toString() ?: defaultStringValue
    }

    fun getIntValue(key: Int): Int {
        val value = getValue(key)
        return if (value == null) defaultIntValue else (value as Int)
    }

    fun getValueReferences(serializable: Serializable): IntArray? {
        if (size == 0) {
            return null
        }
        if (valueReferences == null) {
            buildValueMap()
        }
        return valueReferences!![serializable]
    }

    fun containsKey(serializable: Serializable): Boolean {
        if (size == 0) {
            return false
        }
        if (valueReferences == null) {
            buildValueMap()
        }
        return valueReferences!!.containsKey(serializable)
    }

    fun getSize(): Int {
        return size
    }
}
