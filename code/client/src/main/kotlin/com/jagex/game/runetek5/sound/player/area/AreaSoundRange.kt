package com.jagex.game.runetek5.sound.player.area

class AreaSoundRange(
    @JvmField val x: Int,
    @JvmField val z: Int,
    @JvmField val near: Int,
    @JvmField val far: Int
) {
    @JvmField
    val range: Int = far - near
}
