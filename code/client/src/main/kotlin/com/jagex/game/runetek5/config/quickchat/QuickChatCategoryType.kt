package com.jagex.game.runetek5.config.quickchat

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.CacheableNode
import com.jagex.game.runetek5.utils.Utils

class QuickChatCategoryType : CacheableNode() {

    @JvmField var name: String? = null
    @JvmField var subCategories: IntArray? = null
    @JvmField var subCategoryHotkeys: CharArray? = null
    @JvmField var messages: IntArray? = null
    @JvmField var messageHotkeys: CharArray? = null

    fun decode(buffer: Packet) {
        while (true) {
            val opcode = buffer.readUByte()
            if (opcode == 0) {
                return
            }

            decodeOpcode(buffer, opcode)
        }
    }

    fun decodeOpcode(buffer: Packet, opcode: Int) {
        when (opcode) {
            1 -> name = buffer.readString()
            2 -> {
                val count = buffer.readUByte()
                subCategories = IntArray(count)
                subCategoryHotkeys = CharArray(count)

                for (index in 0 until count) {
                    subCategories!![index] = buffer.readUShort()
                    val hotkey = buffer.readByte()
                    subCategoryHotkeys!![index] = if (hotkey.toInt() == 0) '\u0000' else Utils.cp1252ToChar(hotkey)
                }
            }
            3 -> {
                val count = buffer.readUByte()
                messages = IntArray(count)
                messageHotkeys = CharArray(count)

                for (index in 0 until count) {
                    messages!![index] = buffer.readUShort()
                    val hotkey = buffer.readByte()
                    messageHotkeys!![index] = if (hotkey.toInt() == 0) '\u0000' else Utils.cp1252ToChar(hotkey)
                }
            }
            4 -> {
            }
        }
    }

    fun subCategoryForHotkey(hotkey: Char): Int {
        if (subCategories != null) {
            for (index in subCategories!!.indices) {
                if (subCategoryHotkeys!![index] == hotkey) {
                    return subCategories!![index]
                }
            }
        }
        return -1
    }

    fun messageForHotkey(hotkey: Char): Int {
        if (messages != null) {
            for (index in messages!!.indices) {
                if (messageHotkeys!![index] == hotkey) {
                    return messages!![index]
                }
            }
        }
        return -1
    }

    fun postDecode() {
        if (messages != null) {
            for (index in messages!!.indices) {
                messages!![index] = messages!![index] or 0x8000
            }
        }

        if (subCategories != null) {
            for (index in subCategories!!.indices) {
                subCategories!![index] = subCategories!![index] or 0x8000
            }
        }
    }
}
