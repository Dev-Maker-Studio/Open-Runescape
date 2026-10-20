package com.jagex.game.runetek5.vardomain

interface VarDomain {
    fun getVarValue(varpId: Int): Int
    fun getVarBitValue(varbitId: Int): Int
}
