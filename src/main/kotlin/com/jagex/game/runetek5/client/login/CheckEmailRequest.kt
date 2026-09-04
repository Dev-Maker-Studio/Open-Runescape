package com.jagex.game.runetek5.client.login

import com.jagex.game.runetek5.config.Identifiable

enum class CheckEmailRequest(
    @JvmField val value: Int
) : Identifiable {
    DEFAULT(-2),
    SENT_REQUEST(-3),
    UNUSED_2(2),
    RECEIVED_RESPONSE(3),
    UNUSED_21(21),
    UNUSED_20(20);

    override fun getValue(): Int {
        return value
    }

    companion object {
        @JvmStatic
        fun getValues(): Array<CheckEmailRequest> {
            return arrayOf(SENT_REQUEST, UNUSED_21, UNUSED_20, RECEIVED_RESPONSE, DEFAULT, UNUSED_2)
        }
    }
}
