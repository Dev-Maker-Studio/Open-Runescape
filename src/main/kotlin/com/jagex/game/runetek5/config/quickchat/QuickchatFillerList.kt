package com.jagex.game.runetek5.config.quickchat

/**
 *
 * @author Brainpower - 22/04/2022
 *
 */
interface QuickchatFillerList {
    fun getValueDisplay(value: QuickChatValueType, configs: IntArray, data: Long): String
}
