package com.jagex.game.runetek5.graphics.shader

class ShaderAttributeDX(
    @JvmField val aClass101Sub210305: ShaderDX,
    class1222: ShaderVariableDecoder
) : ShaderAttribute(class1222) {

    @JvmField val anIntArray10306: IntArray = class1222.anIntArray1531!!
    @JvmField val anIntArray10307: IntArray = class1222.anIntArray1534!!

    override fun loadUniformLocation(i_1: Int): Boolean {
        return true
    }

    fun method15782(): Int {
        return this.anIntArray10306[this.aClass101Sub210305.getOccupyingSlot()]
    }

    fun method15783(): Int {
        return this.anIntArray10307[this.aClass101Sub210305.getOccupyingSlot()]
    }

    override fun getUniformLocation(i_1: Int): Int {
        val i_2 = this.anIntArray10306[i_1]
        val i_3 = this.anIntArray10307[i_1]
        return if (i_2 != -1) i_2 else (if (i_3 != -1) i_3 or 0x10000 else -1)
    }
}
