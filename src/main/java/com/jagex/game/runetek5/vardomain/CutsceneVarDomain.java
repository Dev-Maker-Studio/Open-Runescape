package com.jagex.game.runetek5.vardomain;

import com.jagex.game.runetek5.cache.node.IntNode;
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList;

public class CutsceneVarDomain implements VarDomain {

    @Override
    public int getVarBitValue(int varbitId) {
        IntNode integerNode = CutsceneTypeList.CONFIG_CUTSCENES.get(varbitId | 0x100000000L);
        return integerNode == null ? VarProviders.PLAYER_VAR_PROVIDER.getVarBitValue(varbitId) : integerNode.value;
    }

    @Override
    public int getVarValue(int varpId) {
        IntNode integerNode = CutsceneTypeList.CONFIG_CUTSCENES.get(varpId);
        return integerNode == null ? VarProviders.PLAYER_VAR_PROVIDER.getVarValue(varpId) : integerNode.value;
    }

}
