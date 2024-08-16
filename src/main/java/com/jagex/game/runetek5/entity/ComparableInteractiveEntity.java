package com.jagex.game.runetek5.entity;

import com.jagex.game.runetek5.entity.gameobject.GameObject;

public class ComparableInteractiveEntity implements ComparableGraphNode {

    @Override
    public boolean matches(AbstractInteractiveEntity interactive) {
        return interactive instanceof GameObject;
    }
}
