package com.jagex.game.runetek5.cache.node.impl;

import com.jagex.game.runetek5.cache.node.Node;

public class ObjectNode extends Node<Object> {

    public final Object value;

    public ObjectNode(Object value) {
        this.value = value;
    }
}