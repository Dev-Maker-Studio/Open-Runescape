package com.jagex.game.runetek5.graphics.shader;

import com.jagex.game.runetek5.utils.Utils;

public class ProgramAttributeHandler implements IProgramAttributeHandler {

    @Override
    public long getHandle(String toHash) {
        return Utils.hashAsLong(toHash);
    }
}
