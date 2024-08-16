package com.jagex.game.runetek5.vardomain;

import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.cache.node.impl.LongNode;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.varp.VarpbitType;
import com.jagex.game.runetek5.utils.Utils;

/**
 * [J]
 * @author Steven - 22/11/2023
 *
 * [NXT]
	[X]jag::game::PlayerVarDomain::GetVarBitValue(jag::shared_ptr<jag::game::VarBitType> const&)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             .text  00000000000A1B60 00000035 00000008 00000000 R . . . . . .
	[X]jag::game::PlayerVarDomain::GetVarValue(jag::shared_ptr<jag::game::VarType> const&)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   .text  0000000000094D50 00000055                   R . . . . . .
	[X]jag::game::PlayerVarDomain::Reset(void)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               .text  0000000000556080 000000FF 00000028 00000000 R . . . . T .
	[X]jag::game::PlayerVarDomain::Service(void)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             .text  000000000045C8B0 0000049F 000001A8 00000018 R . . . . T .
	[X]jag::game::PlayerVarDomain::SetVarBitValue(jag::shared_ptr<jag::game::VarBitType> const&,int,bool *)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  .text  00000000000AEFD0 00000153 00000098 00000000 R . . . . . .
	[X]jag::game::PlayerVarDomain::SetVarBitValueFromServer(jag::shared_ptr<jag::game::VarBitType> const&,int) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       .text  000000000045D650 000001A6 000000A8 00000000 R . . . B . .
	[X]jag::game::PlayerVarDomain::SetVarValue(jag::shared_ptr<jag::game::VarType> const&,jag::game::VarValue const&)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        .text  000000000045CD50 00000302 00000088 00000000 R . . . . . .
	[X]jag::game::PlayerVarDomain::SetVarValueFromServer(jag::shared_ptr<jag::game::VarType> const&,jag::game::VarValue const&) [clone]   
 */
public class PlayerVarDomain implements VarDomain {

    public static final int[] BIT_MASKS = new int[32];

    static {
        int variable = 2;
        for (int index = 0; index < 32; index++) {
            BIT_MASKS[index] = variable - 1;
            variable += variable;
        }
    }

    public int[] activeVars;
    public IterableNodeMap<LongNode> updates = new IterableNodeMap<LongNode>(128); // TODO: better name?
    public int[] pendingVars;

    public PlayerVarDomain() {
        this.pendingVars = new int[TypeLists.VAR.size];
        this.activeVars = new int[TypeLists.VAR.size];
    }

    /**
     * [J]
     */
    public int service(boolean startHead) {
        long currentTime = Utils.time();
        for (LongNode update = startHead ? this.updates.first() : this.updates.next(); update != null; update = this.updates.next()) {
            if ((update.bitpacked & 0x3fffffffffffffffL) >= currentTime) {
                continue;
            }
            if ((update.bitpacked & 0x4000000000000000L) == 0L) {
                update.unlink();
                continue;
            }
            int configIndex = (int) update.pointer;
            this.activeVars[configIndex] = this.pendingVars[configIndex];
            update.unlink();
            return configIndex;
        }
        return -1;
    }

    /**
     * [J]
     */
    @Override
    public int getVarBitValue(int varbitId) {
        VarpbitType varbitType = TypeLists.VARPBIT.list(varbitId);
        int varId = varbitType.configIndex;
        int start = varbitType.startBit;
        int end = varbitType.endBit;
        int mask = BIT_MASKS[end - start];
        return this.activeVars[varId] >> start & mask;
    }
    
    /**
     * [J]
     */
    @Override
    public int getVarValue(int varpId) {
        return this.activeVars[varpId];
    }
    
    /**
     * [J]
     */
    public void setVarBitValueFromServer(int varbitId, int variable) {
        VarpbitType varbit = TypeLists.VARPBIT.list(varbitId);
        int configIndex = varbit.configIndex;
        int start = varbit.startBit;
        int end = varbit.endBit;
        int bitmask = BIT_MASKS[end - start];
        if (variable < 0 || variable > bitmask) {
            variable = 0;
        }
        bitmask <<= start;
        setVarValueFromServer(configIndex, this.pendingVars[configIndex] & ~bitmask | variable << start & bitmask);
    }
    
    /**
     * [J]
     */
    public void setVarValueFromServer(int configIndex, int varValue) {
        this.pendingVars[configIndex] = varValue;
        LongNode value = this.updates.get(configIndex);
        if (value != null) {
            if (value.bitpacked != 4611686018427387905L) {
                value.bitpacked = Utils.time() + 500L | 0x4000000000000000L;
            }
        } else {
            value = new LongNode(4611686018427387905L);
            this.updates.put(value, configIndex);
        }
    }
    
    /**
     * [J]
     */
    public void setVarBitValue(int varbitId, int variable) {
        VarpbitType varbitType = TypeLists.VARPBIT.list(varbitId);
        int configIndex = varbitType.configIndex;
        int start = varbitType.startBit;
        int end = varbitType.endBit;
        int bitmask = BIT_MASKS[end - start];
        if (variable < 0 || variable > bitmask) {
            variable = 0;
        }
        bitmask <<= start;
        setVarValue(configIndex, this.activeVars[configIndex] & ~bitmask | variable << start & bitmask);
    }
    
    /**
     * [J]
     */
    public void setVarValue(int configIndex, int varValue) {
        this.activeVars[configIndex] = varValue;
        LongNode value = this.updates.get(configIndex);
        if (value != null) {
            value.bitpacked = Utils.time() + 500L;
        } else {
            value = new LongNode(Utils.time() + 500L);
            this.updates.put(value, configIndex);
        }
    }
    
    /**
     * [J]
     */
    public void reset() {
        for (int index = 0; index < TypeLists.VAR.size; index++) {
            this.pendingVars[index] = 0;
            this.activeVars[index] = 0;
        }
        this.updates = new IterableNodeMap<LongNode>(128);
    }
}
