package com.jagex.game.runetek5.config.cutscene;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.sequence.MovementType;
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity;

/**
 * 
 * @author Brainpower - 06/04/2022
 *
 */
public class CutsceneEntityMovement {

    public final int[] movementTypes;
    public final int[] bitpackedPositions;

    CutsceneEntityMovement(Packet buffer) {
        int stepCount = buffer.readUnsignedSmart();
        this.movementTypes = new int[stepCount];
        this.bitpackedPositions = new int[stepCount];
        for (int index = 0; index < stepCount; index++) {
            int movementType = buffer.readUnsignedByte();
            this.movementTypes[index] = movementType;
            int x = buffer.readUnsignedShort();
            int y = buffer.readUnsignedShort();
            this.bitpackedPositions[index] = y + (x << 16);
        }
    }

    public void move(CutsceneEntity entity, int plane) {
        int bitpackedStartposition = this.bitpackedPositions[0];
        entity.move(plane, bitpackedStartposition >>> 16, bitpackedStartposition & 0xffff);
        PathingEntity pathingEntity = entity.getEntity();
        pathingEntity.movementStepsLeft = 0;
        for (int index = this.movementTypes.length - 1; index >= 0; --index) {
            int type = this.movementTypes[index];
            int position = this.bitpackedPositions[index];
            pathingEntity.movementBaseX[pathingEntity.movementStepsLeft] = position >> 16;
            pathingEntity.movementBaseY[pathingEntity.movementStepsLeft] = position & 0xffff;
            byte moveType = MovementType.WALKING.id;
            if (type == 0) {
                moveType = MovementType.HALF_WALK.id;
            } else if (type == 2) {
                moveType = MovementType.RUNNING.id;
            }
            pathingEntity.movementTypes[pathingEntity.movementStepsLeft] = moveType;
            pathingEntity.movementStepsLeft++;
        }
    }
}
