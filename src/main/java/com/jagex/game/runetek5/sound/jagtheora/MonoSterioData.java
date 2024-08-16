package com.jagex.game.runetek5.sound.jagtheora;

import com.jagex.game.runetek5.cache.node.Node;

public class MonoSterioData extends Node {

    final short[][] aShortArrayArray8003;
    double aDouble8004;

    MonoSterioData(short[][] shorts_1, double d_2) {
        this.aShortArrayArray8003 = shorts_1;
        this.aDouble8004 = d_2;
    }

    long method13294() {
        return this.aShortArrayArray8003.length | this.aShortArrayArray8003[0].length;
    }

}
