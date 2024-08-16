package com.jagex.game.runetek5.config.cutscene.action;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.IntNode;
import com.jagex.game.runetek5.config.cutscene.CutsceneAction;
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList;

/**
 * 
 * @author Brainpower - 01/02/2023
 *
 */
public class CutsceneSetVariable extends CutsceneAction {

    public final long key;

    public final int value;

    public CutsceneSetVariable(Packet buffer, boolean isVarbit) {
        super(buffer);
        int mask = buffer.readUnsignedShort();
        if (isVarbit) {
            this.key = 0x100000000L | mask;
        } else {
            this.key = mask;
        }
        this.value = buffer.readInt();
    }

    @Override
    public void perform() {
        IntNode intnode = CutsceneTypeList.CONFIG_CUTSCENES.get(this.key);
        if (intnode != null) {
            intnode.value = this.value;
        } else {
            CutsceneTypeList.CONFIG_CUTSCENES.put(new IntNode(this.value), this.key);
        }
    }
}
