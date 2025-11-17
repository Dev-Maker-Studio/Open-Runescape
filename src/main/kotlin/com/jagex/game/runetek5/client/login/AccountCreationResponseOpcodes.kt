package com.jagex.game.runetek5.client.login

import com.jagex.game.runetek5.config.Identifiable

enum class AccountCreationResponseOpcodes(@JvmField val opcode: Int) : Identifiable {
    aClass4835737(-1),
    aClass4835730(-2),
    aClass4835729(-3),
    aClass4835732(-4),
    aClass4835731(-5),
    CONTINUE(2),
    aClass4835735(3),
    aClass4835736(7),
    aClass4835733(9),
    CLIENT_HAS_BEEN_UPDATED(37);

    override fun getValue(): Int = opcode

    companion object {
        @JvmStatic
        fun method1489(): AccountCreationResponseOpcodes {
            return if (AccountCreationStage.RECEIVED_RESPONSE == null) aClass4835730 else AccountCreationStage.RECEIVED_RESPONSE!!
        }

        @JvmStatic
        fun getValues(): Array<AccountCreationResponseOpcodes> {
            return arrayOf(
                aClass4835729, CLIENT_HAS_BEEN_UPDATED, CONTINUE, aClass4835733,
                aClass4835730, aClass4835735, aClass4835732, aClass4835736,
                aClass4835737, aClass4835731
            )
        }
    }
}
