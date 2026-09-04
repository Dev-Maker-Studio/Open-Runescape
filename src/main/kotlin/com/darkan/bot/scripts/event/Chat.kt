package com.darkan.bot.scripts.event

import com.jagex.game.runetek5.client.ChatMessageType

class Chat(
    val messageType: ChatMessageType,
    val cleanSenderName: String?,
    val crownedSenderName: String?,
    val formattedSenderName: String?,
    val message: String
) : Event