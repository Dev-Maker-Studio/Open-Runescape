package com.jagex.game.runetek5.entity.hint;

import com.jagex.game.runetek5.entity.AbstractInteractiveEntity;
import com.jagex.game.runetek5.entity.ComparableGraphNode;

public class HintTrailComparable implements ComparableGraphNode {

    public final HintTrailGraphNode hintTrailGraphNode;

    public HintTrailComparable(HintTrailGraphNode hintTrailGraphNode) {
        this.hintTrailGraphNode = hintTrailGraphNode;
    }

    @Override
    public boolean matches(AbstractInteractiveEntity hintTrailGraphNode) {
        return hintTrailGraphNode == this.hintTrailGraphNode;
    }
}
