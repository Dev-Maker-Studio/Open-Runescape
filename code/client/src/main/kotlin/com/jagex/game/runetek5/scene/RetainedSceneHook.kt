package com.jagex.game.runetek5.scene

interface RetainedSceneHook {
    fun active(scene: Scene): Boolean

    fun chunkChanged(scene: Scene, plane: Int, tileX: Int, tileY: Int)

    fun sceneReset(scene: Scene)

    fun submitStatics(
        scene: Scene,
        settingBits: Array<Array<ByteArray>>?,
        lowestRenderedPlane: Int,
        roofPreference: Byte
    )
}
