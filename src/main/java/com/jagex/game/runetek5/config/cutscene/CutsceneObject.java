package com.jagex.game.runetek5.config.cutscene;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.Identifiable;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.object.ObjectShapes;
import com.jagex.game.runetek5.config.object.ObjectType;
import com.jagex.game.runetek5.scene.LocAction;

/**
 * 
 * @author Brainpower - 01/02/2023
 *
 */
public class CutsceneObject {

    public final int locId;

    public final ObjectShapes locShape;

    public int plane;

    public int x;

    public int y;

    public int rotation;

    public CutsceneObject(Packet buffer) {
        this.locId = buffer.readBigSmart();
        this.locShape = (ObjectShapes) Identifiable.identify(ObjectShapes.values(), buffer.readUnsignedByte());
    }


    boolean isFullyCached() {
        if (this.locId < 0) {
            return true;
        } 
        ObjectType locType = TypeLists.LOC.list(this.locId);
        boolean cached = locType.isTypeCached(this.locShape.shape);
        int[] seqIds = locType.getAnimations();
        if (seqIds != null) {
            for (int index = 0; index < seqIds.length; index++) {
                int seqId = seqIds[index];
                cached &= TypeLists.SEQ.list(seqId).ready();
            }
        }
        return cached;
    }

    public void destroy() {
        LocAction.destroyObject(this.plane, this.x, this.y, this.locShape.group, -1, this.locShape.shape, this.rotation);
    }

    public void replace(int plane, int x, int y, int rotation) {
        LocAction.destroyObject(plane, x, y, this.locShape.group, this.locId, this.locShape.shape, rotation);
        this.plane = plane;
        this.x = x;
        this.y = y;
        this.rotation = rotation;
    }
}
