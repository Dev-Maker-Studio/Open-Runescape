package com.jagex.game.runetek5.graphics.model.particle;

import com.jagex.game.runetek5.entity.EntityNode;

public class ParticleNode extends EntityNode {

    public ParticleNode child;
    public ParticleNode next;

    public void unlink() {
        if (this.next != null) {
            this.next.child = this.child;
            this.child.next = this.next;
            this.child = null;
            this.next = null;
        }

    }
}
