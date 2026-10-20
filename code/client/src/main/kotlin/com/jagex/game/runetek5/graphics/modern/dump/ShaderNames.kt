package com.jagex.game.runetek5.graphics.modern.dump

object ShaderNames {
    fun of(vertex: String, fragment: String) = when {
        "aModelMatCol0" in vertex -> "instanced"
        "uModelMatrices" in vertex -> "optimized"
        "uMask" in fragment -> "surfaceMask"
        "gl_VertexID" in vertex -> "fullscreen"
        else -> "source" + vertex.hashCode().toUInt().toString(16)
    }
}
