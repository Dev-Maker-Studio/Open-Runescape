package com.jagex.game.runetek5.graphics.model.billboard

class Billboard(
    @JvmField val billboardId: Int,
    @JvmField val appliesOnFace: Int,
    @JvmField val depth: Int,
    @JvmField val distance: Int
) {
    fun translateToFace(face: Int): Billboard {
        return Billboard(billboardId, face, depth, distance)
    }
}
