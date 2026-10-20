package com.jagex.game.runetek5.config.quickchat

interface QuickchatFillerList {
    fun getValueDisplay(value: QuickChatValueType, configs: IntArray, data: Long): String
}
