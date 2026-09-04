package com.jagex.game.runetek5.interaction.chat

import com.darkan.bot.scripts.ScriptExecutor
import com.darkan.bot.scripts.event.Chat
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.client.ChatMessageType
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor
import com.jagex.game.runetek5.graphics.sprite.NativeSprite
import com.jagex.game.runetek5.graphics.sprite.SpriteType
import com.jagex.game.runetek5.graphics.sprite.StaticMedia
import com.jagex.game.runetek5.network.packet.ClientProt
import com.jagex.game.runetek5.network.packet.PacketEncoder
import com.jagex.game.runetek5.rendering.Renderers

/**
 *
 * @author Brainpower - 18/09/2023
 *
 */
object ChatBox {

    // Chat
    @JvmField val CHAT_LINES = arrayOfNulls<ChatLine>(100)
    @JvmField val CHAT_ICONS = LRUCache(8)
    @JvmField var NUM_CHAT_LINES = 0
    @JvmField var CHATLINE_IDENTIFIER = 0

    // Chat filter
    @JvmField var PUBLIC_FILTER = 0
    @JvmField var PRIVATE_FILTER: ChatFilter? = null
    @JvmField var TRADE_FILTER = 0

    @JvmStatic
    fun writeMessage(type: ChatMessageType, effectFlags: Int, crownedName: String, name: String?, simpleName: String, message: String, clan: String?, quickchatMessageId: Int) {
        ScriptExecutor.pushEvent(Chat(type, simpleName, crownedName, name, message))
        var line = CHAT_LINES[99]
        System.arraycopy(CHAT_LINES, 0, CHAT_LINES, 1, 99)
        if (line == null) {
            line = ChatLine(type, effectFlags, crownedName, name, simpleName, clan, quickchatMessageId, message)
        } else {
            line.set(type, effectFlags, crownedName, name, simpleName, clan, quickchatMessageId, message)
        }
        CHAT_LINES[0] = line
        NUM_CHAT_LINES++
        Client.lastChatMessageTick = Client.serverTick
    }

    @JvmStatic
    fun writeGameMessage(message: String) {
        writeMessage(ChatMessageType.GAME, 0, "", "", "", message)
    }

    @JvmStatic
    fun writeUnfilterableMessage(message: String) {
        writeMessage(ChatMessageType.UNFILTERABLE, 0, "", "", "", message)
    }

    @JvmStatic
    fun writeMessage(type: ChatMessageType, effectFlags: Int, crownedName: String, name: String, nameSimple: String, message: String) {
        writeMessage(type, effectFlags, crownedName, name, nameSimple, message, null, -1)
    }

    @JvmStatic
    fun getLine(index: Int): ChatLine? {
        return if (index >= 0 && index < 100) CHAT_LINES[index] else null
    }

    @JvmStatic
    fun reset() {
        for (index in 0 until 100) {
            CHAT_LINES[index] = null
        }
        NUM_CHAT_LINES = 0
    }

    @JvmStatic
    fun getLineCount(): Int {
        return NUM_CHAT_LINES
    }

    @JvmStatic
    fun getChatlineIdentifier(): Int {
        return ++CHATLINE_IDENTIFIER - 1
    }

    @JvmStatic
    fun addQuestChatIcons(questId: IntArray): String {
        val questStringbuilder = StringBuilder()
        var iconIndex = StaticMedia.totalChatIcons
        for (index in questId.indices) {
            val questType = TypeLists.QUEST!!.list(questId[index])
            if (questType.graphicId != -1) {
                var chatIcon = CHAT_ICONS.get(questType.graphicId.toLong()) as NativeSprite?
                if (chatIcon == null) {
                    val spriteType = SpriteType.listSingle(Resource.SPRITES, questType.graphicId, 0)
                    if (spriteType != null) {
                        chatIcon = Renderers.activeToolkit!!.generateSprite(spriteType, true)
                        CHAT_ICONS.put(chatIcon, questType.graphicId.toLong())
                    }
                }
                if (chatIcon != null) {
                    StaticMedia.chatIcons!![iconIndex] = chatIcon
                    questStringbuilder.append(" <img=").append(iconIndex++).append(">")
                }
            }
        }
        return questStringbuilder.toString()
    }

    @JvmStatic
    fun setFilter(executor: CS2Executor) {
        executor.intStackPtr -= 3
        PUBLIC_FILTER = executor.intStack[executor.intStackPtr]
        PRIVATE_FILTER = ChatFilter.forId(executor.intStack[executor.intStackPtr + 1])
        if (PRIVATE_FILTER == null) {
            PRIVATE_FILTER = ChatFilter.FRIENDS_ONLY
        }
        TRADE_FILTER = executor.intStack[executor.intStackPtr + 2]
        val context = Client.connectionContext
        val packet = PacketEncoder.createPacket(ClientProt.CHAT_SETFILTER, context.isaac)
        packet.buffer.writeByte(PUBLIC_FILTER)
        packet.buffer.writeByte(PRIVATE_FILTER!!.id)
        packet.buffer.writeByte(TRADE_FILTER)
        context.queuePacket(packet)
    }
}
