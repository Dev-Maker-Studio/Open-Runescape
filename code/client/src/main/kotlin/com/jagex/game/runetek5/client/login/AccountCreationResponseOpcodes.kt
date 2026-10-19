package com.jagex.game.runetek5.client.login

import com.jagex.game.runetek5.config.Identifiable

enum class AccountCreationResponseOpcodes(@JvmField val opcode: Int) : Identifiable {
    NONE(-1),
    AWAITING_RESPONSE(-2),
    NOT_STARTED(-3),
    CONNECTION_ERROR(-4),
    CONNECTION_FAILED(-5),
    CONTINUE(2),
    NAME_UNAVAILABLE(3),
    ACCOUNT_CREATION_DISABLED(7),
    ALREADY_LOGGED_IN(9),
    CLIENT_HAS_BEEN_UPDATED(37);

    override fun getValue(): Int = opcode

    companion object {
        @JvmStatic
        fun getReceivedResponse(): AccountCreationResponseOpcodes {
            return if (AccountCreationStage.RECEIVED_RESPONSE == null) AWAITING_RESPONSE else AccountCreationStage.RECEIVED_RESPONSE!!
        }

        @JvmStatic
        fun getValues(): Array<AccountCreationResponseOpcodes> {
            return arrayOf(
                NOT_STARTED, CLIENT_HAS_BEEN_UPDATED, CONTINUE, ALREADY_LOGGED_IN,
                AWAITING_RESPONSE, NAME_UNAVAILABLE, CONNECTION_ERROR, ACCOUNT_CREATION_DISABLED,
                NONE, CONNECTION_FAILED
            )
        }
    }
}
