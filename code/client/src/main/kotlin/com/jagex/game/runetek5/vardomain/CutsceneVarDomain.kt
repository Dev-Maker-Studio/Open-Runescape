package com.jagex.game.runetek5.vardomain

import com.jagex.game.runetek5.cache.node.IntNode
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList

class CutsceneVarDomain : VarDomain {

    override fun getVarBitValue(varbitId: Int): Int {
        val integerNode = CutsceneTypeList.CONFIG_CUTSCENES.get(varbitId.toLong() or 0x100000000L)
        return integerNode?.value ?: VarProviders.PLAYER_VAR_PROVIDER.getVarBitValue(varbitId)
    }

    override fun getVarValue(varpId: Int): Int {
        val integerNode = CutsceneTypeList.CONFIG_CUTSCENES.get(varpId.toLong())
        return integerNode?.value ?: VarProviders.PLAYER_VAR_PROVIDER.getVarValue(varpId)
    }
}
