package com.jagex.game.runetek5.graphics.model.buffer

import com.jagex.game.runetek5.toolkit.companion.NativeStream

class ModelBufferDX(
    @JvmField var aBool663: Boolean
) {
    @JvmField
    var streamSource: NativeStream? = null

    @JvmField
    var anInterface4664: NativeStream? = null

    /**
     * Is usable at this time
     */
    @JvmField
    var allocated: Boolean = false

    fun method1264(): Boolean {
        return allocated && !aBool663
    }

    fun deallocate() {
        if (anInterface4664 != null) {
            anInterface4664!!.deallocate()
        }
        allocated = false
    }
}
