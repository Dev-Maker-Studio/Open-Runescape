package com.jagex.game.runetek5.client.login

import com.jagex.game.runetek5.config.Identifiable

enum class CreateAccountRequest(@JvmField val responseCode: Int) : Identifiable {
    NOT_STARTED(-2),
    AWAITING_RESPONSE(-3),
    SUCCESS(2),
    UNKNOWN_ERROR(3),
    USERNAME_UNAVAILABLE(9),
    INVALID_USERNAME(10),
    INVALID_PASSWORD(20),
    PASSWORD_TOO_SIMILAR(21),
    INVALID_DOB(30),
    EMAIL_ALREADY_IN_USE(31),
    INVALID_EMAIL(32),
    TERMS_NOT_ACCEPTED(33),
    TOO_MANY_ATTEMPTS(34),
    SERVER_UNAVAILABLE(38);

    override fun getValue(): Int = responseCode

    companion object {
        @JvmStatic
        fun getAll(): Array<CreateAccountRequest> {
            return arrayOf(
                INVALID_USERNAME, PASSWORD_TOO_SIMILAR, INVALID_DOB, INVALID_EMAIL,
                NOT_STARTED, UNKNOWN_ERROR, EMAIL_ALREADY_IN_USE, TOO_MANY_ATTEMPTS,
                AWAITING_RESPONSE, USERNAME_UNAVAILABLE, SERVER_UNAVAILABLE, SUCCESS,
                INVALID_PASSWORD, TERMS_NOT_ACCEPTED
            )
        }
    }
}
