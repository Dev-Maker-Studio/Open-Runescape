package com.jagex.game.runetek5.buffer

import com.jagex.game.runetek5.toolkit.directx.DirectXBuffer

class NormalsBufferDX(
    @JvmField var aBool406: Boolean
) {
    @JvmField
    var anInterface32407: DirectXBuffer? = null

    @JvmField
    var anInterface32409: DirectXBuffer? = null

    @JvmField
    var allocated: Boolean = false

    fun method861(): Boolean {
        return allocated && !aBool406
    }

    fun deallocate() {
        if (anInterface32409 != null) {
            anInterface32409!!.deallocate()
        }
        allocated = false
    }
}
