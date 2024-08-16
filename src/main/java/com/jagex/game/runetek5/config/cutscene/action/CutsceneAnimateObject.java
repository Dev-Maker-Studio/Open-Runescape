package com.jagex.game.runetek5.config.cutscene.action;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.cutscene.CutsceneAction;
import com.jagex.game.runetek5.config.cutscene.CutsceneObject;
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList;
import com.jagex.game.runetek5.config.sequence.SeqType;
import com.jagex.game.runetek5.scene.SceneGraph;

/**
 * 
 * @author Steven - 24/09/2023
 *
 */
public class CutsceneAnimateObject extends CutsceneAction {

    public final int objectIndex;

    public final int sequenceId;

    public CutsceneAnimateObject(Packet buffer) {
        super(buffer);
        this.objectIndex = buffer.readUnsignedShort();
        this.sequenceId = buffer.readBigSmart();
    }

    @Override
    public void perform() {
        CutsceneObject object = CutsceneTypeList.CUTSCENE_OBJECTS[this.objectIndex];
        SceneGraph.animateObject(object.plane, object.x, object.y, object.locShape.group, object.locShape.shape, object.rotation, this.sequenceId);
    }

    @Override
    public boolean isFullyCached() {
        SeqType seqType = TypeLists.SEQ.list(this.sequenceId);
        return seqType.ready();
    }
}
