package com.jagex.game.runetek5.graphics.model.particle;

public class ParticleBinaryTree {

    public final ParticleNode current = new ParticleNode();
    public ParticleNode aParticleNode;

    public ParticleBinaryTree() {
        this.current.child = this.current;
        this.current.next = this.current;
    }

    public ParticleNode getNext() {
        ParticleNode child = this.current.child;
        if (child == this.current) {
            this.aParticleNode = null;
            return null;
        } else {
            this.aParticleNode = child.child;
            return child;
        }
    }

    public ParticleNode method7734() {
        ParticleNode node = this.aParticleNode;
        if (node == this.current) {
            this.aParticleNode = null;
            return null;
        } else {
            this.aParticleNode = node.child;
            return node;
        }
    }

    public void insert(ParticleNode node) {
        if (node.next != null) {
            node.unlink();
        }
        node.next = this.current.next;
        node.child = this.current;
        node.next.child = node;
        node.child.next = node;
    }

    public int getNodeCount() {
        int count = 0;
        for (ParticleNode class275Sub13 = this.current.child; class275Sub13 != this.current; class275Sub13 = class275Sub13.child) {
            count++;
        }
        return count;
    }

    public void unlinkAll() {
        while (true) {
            ParticleNode particleNode = this.current.child;
            if (particleNode == this.current) {
                this.aParticleNode = null;
                return;
            }
            particleNode.unlink();
        }
    }
}
