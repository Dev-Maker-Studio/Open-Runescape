package com.jagex.game.runetek5.entity;

import com.jagex.game.runetek5.config.sequence.EntitySequence;
import com.jagex.game.runetek5.config.sequence.Sequence;
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity;

public class PathingEntitySpotAnimation {

    public int adjustmentType;

    public int height;

    public int rotation;

    public int spotAnimationId = -1;

    public final Sequence sequence;

    public PathingEntitySpotAnimation(PathingEntity entity) {
        this.sequence = new EntitySequence(entity);
    }
}
