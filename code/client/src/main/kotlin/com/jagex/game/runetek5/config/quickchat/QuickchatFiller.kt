package com.jagex.game.runetek5.config.quickchat

import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.quickchat.QuickChatValueType.*

class QuickchatFiller : QuickchatFillerList {
    override fun getValueDisplay(value: QuickChatValueType, configs: IntArray, data: Long): String {
        if (value == LIST_DIALOG) {
            val enumType = TypeLists.ENUM!!.list(configs[0])
            return enumType.getStringValue(data.toInt())
        }
        if (value == OBJ_DIALOG || value == OBJ_TRADE_DIALOG) {
            val objType = TypeLists.OBJ!!.list(data.toInt())
            return objType.name
        }
        if (value == ENUM_STRING || value == ENUM_STRING_CLAN || value == ENUM_STRING_STATBASE) {
            return TypeLists.ENUM!!.list(configs[0]).getStringValue(data.toInt())
        }
        return ""
    }
}
