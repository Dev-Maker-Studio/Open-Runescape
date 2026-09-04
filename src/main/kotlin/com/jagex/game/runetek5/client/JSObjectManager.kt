package com.jagex.game.runetek5.client

class JSObjectManager(@JvmField val type: Int) {
    fun getCallName(): String = "_$type"

    companion object {
        @JvmField val PERIODIC = JSObjectManager(9)
        @JvmField val ENTERING_GAME = JSObjectManager(3)
        @JvmField val CS2_INS6416 = JSObjectManager(7)
        @JvmField val CS2_INS6410 = JSObjectManager(8)
        @JvmField val WINDOW_ADJUSTED = JSObjectManager(10)
        @JvmField val CS2_INS6838 = JSObjectManager(4)
        @JvmField val CS2_INS6680 = JSObjectManager(15)
        @JvmField val CS2_INS6476 = JSObjectManager(1)
        @JvmField val CS2_INS6867 = JSObjectManager(5)
        @JvmField val LOGIN_PROMPT_BECOME_MEMBER = JSObjectManager(11)
        @JvmField val SERVER_PACKET_URL = JSObjectManager(13)
        @JvmField val SERVER_WINDOW_CALL = JSObjectManager(6)
        @JvmField val CS2_INS6868 = JSObjectManager(2)
        @JvmField val CS2_INS6869 = JSObjectManager(12)
        @JvmField val CS2_INS6058 = JSObjectManager(14)

        @JvmField var highlightFlashCycles: Int = 0
    }
}
