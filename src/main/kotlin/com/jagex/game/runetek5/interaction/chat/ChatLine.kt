package com.jagex.game.runetek5.interaction.chat

import com.jagex.game.runetek5.client.ChatMessageType
import com.jagex.game.runetek5.client.client

/**
 * [J]
 * @author Brainpower - 03/02/2023
 *
 */
class ChatLine(
    type: ChatMessageType,
    effectFlags: Int,
    crownedName: String?,
    name: String?,
    nameSimple: String?,
    clan: String?,
    quickchatMessageId: Int,
    message: String?
) {

    @JvmField var identifier: Int = ChatBox.getChatlineIdentifier()
    @JvmField var time: Int = 0
    @JvmField var type: ChatMessageType? = null
    @JvmField var effectFlags: Int = 0
    @JvmField var crownedName: String? = null
    @JvmField var name: String? = null
    @JvmField var simpleName: String? = null
    @JvmField var clan: String? = null
    @JvmField var quickchatMessageId: Int = 0
    @JvmField var message: String? = null

    init {
        this.time = client.cycles
        this.type = type
        this.effectFlags = effectFlags
        this.crownedName = crownedName
        this.name = name
        this.simpleName = nameSimple
        this.clan = clan
        this.quickchatMessageId = quickchatMessageId
        this.message = message
    }

    fun set(type: ChatMessageType, effectFlags: Int, crownedName: String, name: String, simpleName: String, clan: String, quickchatMessageId: Int, message: String) {
        this.identifier = ChatBox.getChatlineIdentifier()
        this.time = client.cycles
        this.type = type
        this.effectFlags = effectFlags
        this.crownedName = crownedName
        this.name = name
        this.simpleName = simpleName
        this.clan = clan
        this.quickchatMessageId = quickchatMessageId
        this.message = message
    }
}
