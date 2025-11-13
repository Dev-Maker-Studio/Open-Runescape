package com.jagex.game.runetek5.vardomain

/**
 *
 * @author Steven - 22/11/2023
 *
 */
interface VarDomain {
    fun getVarValue(varpId: Int): Int
    fun getVarBitValue(varbitId: Int): Int
}
