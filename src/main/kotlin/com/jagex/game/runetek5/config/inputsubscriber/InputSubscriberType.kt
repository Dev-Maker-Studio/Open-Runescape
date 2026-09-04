package com.jagex.game.runetek5.config.inputsubscriber

enum class InputSubscriberType(
    @JvmField val value: Int
) {
    COMBINED(0),
    KEY_PRESS(1),
    KEY_HOLD(2);

    companion object {
        @JvmStatic
        fun forId(valueToFind: Int): InputSubscriberType? {
            val values = entries
            for (index in values.indices) {
                val inputSubscriberType = values[index]
                if (valueToFind == inputSubscriberType.value) {
                    return inputSubscriberType
                }
            }
            return null
        }
    }
}
