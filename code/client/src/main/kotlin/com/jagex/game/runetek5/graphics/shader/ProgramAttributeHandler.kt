package com.jagex.game.runetek5.graphics.shader

import com.jagex.game.runetek5.utils.Utils

class ProgramAttributeHandler : IProgramAttributeHandler {
    override fun getHandle(name: String): Long {
        return Utils.hashAsLong(name)
    }
}
