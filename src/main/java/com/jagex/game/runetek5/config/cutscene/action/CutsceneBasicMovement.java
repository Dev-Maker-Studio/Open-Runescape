package com.jagex.game.runetek5.config.cutscene.action;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.cutscene.CutsceneAction;
import com.jagex.game.runetek5.config.cutscene.CutsceneEntity;
import com.jagex.game.runetek5.config.cutscene.CutsceneEntityMovement;
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList;

public class CutsceneBasicMovement extends CutsceneAction {

    public static int anInt9440;

    public final int entityIndex;

    public final int movementIndex;

    public final int plane;

    public CutsceneBasicMovement(Packet buffer) {
        super(buffer);
        this.entityIndex = buffer.readUShort();
        this.movementIndex = buffer.readUShort();
        this.plane = buffer.readUByte();
    }

    @Override
    public void perform() {
        CutsceneEntity entity = CutsceneTypeList.CUTSCENE_ENTITIES[this.entityIndex];
        CutsceneEntityMovement movement = CutsceneTypeList.CUTSCENE_ENTITY_MOVEMENTS[this.movementIndex];
        movement.move(entity, this.plane);
    }
}
