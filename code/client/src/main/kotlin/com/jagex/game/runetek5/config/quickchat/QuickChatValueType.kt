package com.jagex.game.runetek5.config.quickchat

/**
 *
 * @author Brainpower - 22/04/2022
 *
 */
enum class QuickChatValueType(
    @JvmField val id: Int,
    @JvmField val clientTransmitSize: Int,
    @JvmField val serverTransmitSize: Int,
    @JvmField val parameterCount: Int
) {
    LIST_DIALOG(0, 2, 2, 1),
    OBJ_DIALOG(1, 2, 2, 0),
    COUNT_DIALOG(2, 4, 4, 0),
    STAT_BASE(4, 1, 1, 1),
    ENUM_STRING(6, 0, 4, 2),
    ENUM_STRING_CLAN(7, 0, 1, 1),
    TOSTRING_VARP(8, 0, 4, 1),
    TOSTRING_VARBIT(9, 0, 4, 1),
    OBJ_TRADE_DIALOG(10, 2, 2, 0),
    ENUM_STRING_STATBASE(11, 0, 1, 2),
    ACC_GETCOUNT_WORLD(12, 0, 1, 0),
    ACC_GET_MEAN_COMBAT_LEVEL(13, 0, 1, 0),
    TOSTRING_SHARED(14, 0, 4, 1),
    ACTIVE_COMBAT_LEVEL(15, 0, 1, 0);

    companion object {
        @JvmStatic
        fun get(id: Int): QuickChatValueType? {
            val valueTypes = values()
            for (index in valueTypes.indices) {
                if (id == valueTypes[index].id) {
                    return valueTypes[index]
                }
            }
            return null
        }
    }
}
