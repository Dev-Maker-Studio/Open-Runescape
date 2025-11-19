package com.jagex.game.runetek5.graphics.model.particle;

import com.jagex.game.runetek5.entity.EntityNode;

public class ParticleNode extends EntityNode {
    public ParticleNode child;
    public ParticleNode next;

    public void unlink() {
        if (next != null) {
            next.child = child;
            child.next = next;
            child = null;
            next = null;
        }
    }
}
