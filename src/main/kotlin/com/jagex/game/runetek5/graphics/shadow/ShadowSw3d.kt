package com.jagex.game.runetek5.graphics.shadow

import com.jagex.game.runetek5.toolkit.sw3d.FinalizeCacheSw3d
import com.jagex.game.runetek5.toolkit.sw3d.INativeObject

class ShadowSw3d internal constructor() : Shadow(), INativeObject, AutoCloseable {
    @JvmField
    var nativeid: Long = 0

    override external fun destroy(var1: Boolean)

    override fun close() {
        if (nativeid != 0L) {
            FinalizeCacheSw3d.addFinalizedObject(this)
        }
    }
}
