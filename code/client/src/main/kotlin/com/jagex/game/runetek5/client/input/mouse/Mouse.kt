package com.jagex.game.runetek5.client.input.mouse

object Mouse {
    @JvmStatic
    fun isClicktypeObj(clickType: Int): Boolean {
        return clickType == 18 || clickType == 19 || clickType == 20 || clickType == 21 || clickType == 22 || clickType == 1004 || clickType == 17
    }

    @JvmStatic
    fun isClicktypeNpc(clickType: Int): Boolean {
        return clickType == 9 || clickType == 10 || clickType == 11 || clickType == 12 || clickType == 13 || clickType == 1003 || clickType == 8
    }

    @JvmStatic
    fun isClicktypeLoc(clickType: Int): Boolean {
        return clickType == 3 || clickType == 4 || clickType == 5 || clickType == 6 || clickType == 1001 || clickType == 1002 || clickType == 2
    }

    @JvmStatic
    fun isClicktypeComponent(clickType: Int): Boolean {
        return clickType == 57 || clickType == 58 || clickType == 1007 || clickType == 25 || clickType == 30
    }
}
