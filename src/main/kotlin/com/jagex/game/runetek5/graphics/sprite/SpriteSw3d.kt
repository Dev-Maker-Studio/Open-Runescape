package com.jagex.game.runetek5.graphics.sprite

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface
import com.jagex.game.runetek5.framebuffer.FrameBuffer
import com.jagex.game.runetek5.toolkit.SoftwareToolkit
import com.jagex.game.runetek5.toolkit.sw3d.FinalizeCacheSw3d
import com.jagex.game.runetek5.toolkit.sw3d.INativeObject
import com.jagex.game.runetek5.toolkit.sw3d.wa

class SpriteSw3d : NativeSprite, INativeObject, FrameBuffer, AutoCloseable {

    @JvmField var nativeid: Long = 0L
    @JvmField var aBool7477: Boolean = false

    constructor(ja1: SoftwareToolkit, ints_2: IntArray, i_3: Int, i_4: Int, i_5: Int, i_6: Int) {
        RA(ja1, ints_2, i_3, i_4, i_5, i_6, false)
    }

    constructor(ja1: SoftwareToolkit, i_2: Int, i_3: Int, i_4: Int, i_5: Int, bool6: Boolean) {
        K(ja1, i_2, i_3, i_4, i_5, bool6)
    }

    constructor(ja1: SoftwareToolkit, ints_2: IntArray, bytes_3: ByteArray, bytes_4: ByteArray, i_6: Int, i_7: Int, i_8: Int) {
        EA(ja1, ints_2, bytes_3, bytes_4, 0, i_6, i_7, i_8)
    }

    constructor(ja1: SoftwareToolkit, i_2: Int, i_3: Int, bool4: Boolean) {
        aBool7477 = bool4
        UA(ja1, i_2, i_3)
    }

    private external fun e(var1: Long, var3: Float, var4: Float, var5: Float, var6: Float, var7: Float, var8: Float, var9: Int, var10: Int, var11: Int, var12: Int)

    override fun method2750(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int) {
        ea(nativeid, i_1, i_2, i_3, i_4, i_5, i_6)
    }

    private external fun K(var1: SoftwareToolkit, var2: Int, var3: Int, var4: Int, var5: Int, var6: Boolean)

    override fun close() {
        if (nativeid != 0L) {
            FinalizeCacheSw3d.addFinalizedObject(this)
        }
    }

    override fun destroy(bool1: Boolean) {
        M(nativeid, bool1)
    }

    override fun setSize(i_1: Int, i_2: Int, i_3: Int, i_4: Int) {
        U(nativeid, i_1, i_2, i_3, i_4)
    }

    private external fun RA(var1: SoftwareToolkit, var2: IntArray, var3: Int, var4: Int, var5: Int, var6: Int, var7: Boolean)

    override fun getUseWidth(): Int {
        return da(nativeid)
    }

    override fun getWidth(): Int {
        return da(nativeid)
    }

    private external fun da(var1: Long): Int

    override fun getSpriteWidth(): Int {
        return WA(nativeid)
    }

    private external fun WA(var1: Long): Int

    private external fun GA(var1: Long): Int

    private external fun ba(var1: Long): Int

    override fun getFramebuffer(): FrameBuffer? {
        return if (aBool7477) this else null
    }

    private external fun ea(var1: Long, var3: Int, var4: Int, var5: Int, var6: Int, var7: Int, var8: Int)

    override fun pixelTransfer() {
        j(nativeid, 0, 0, 3)
    }

    private external fun j(var1: Long, var3: Int, var4: Int, var5: Int)

    override fun draw(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int) {
        Y(nativeid, i_1, i_2, i_3, i_4, i_5)
    }

    private external fun Y(var1: Long, var3: Int, var4: Int, var5: Int, var6: Int, var7: Int)

    override fun method2773(i_1: Int, i_2: Int, class4553: ComponentSurface, i_4: Int, i_5: Int) {
        o(nativeid, i_1, i_2, (class4553 as wa).nativeid, i_4, i_5)
    }

    private external fun o(var1: Long, var3: Int, var4: Int, var5: Long, var7: Int, var8: Int)

    override fun drawColoredCircle(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int, i_7: Int) {
        ha(nativeid, i_1, i_2, i_3, i_4, i_5, i_6, i_7, 1)
    }

    override fun method2772(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int, i_7: Int) {
        aa(nativeid, i_1, i_2, i_3, i_4, i_5, i_6, i_7)
    }

    private external fun aa(var1: Long, var3: Int, var4: Int, var5: Int, var6: Int, var7: Int, var8: Int, var9: Int)

    override fun method2766(f_1: Float, f_2: Float, f_3: Float, f_4: Float, f_5: Float, f_6: Float, i_7: Int, class4558: ComponentSurface, i_9: Int, i_10: Int) {
        FA(nativeid, f_1, f_2, f_3, f_4, f_5, f_6, i_7, (class4558 as wa).nativeid, i_9, i_10, aBool7477)
    }

    private external fun FA(var1: Long, var3: Float, var4: Float, var5: Float, var6: Float, var7: Float, var8: Float, var9: Int, var10: Long, var12: Int, var13: Int, var14: Boolean)

    override fun deallocate() {
    }

    private external fun M(var1: Long, var3: Boolean)

    override fun getHeight(): Int {
        return GA(nativeid)
    }

    private external fun F(var1: Long, var3: IntArray)

    override fun method2764(f_1: Float, f_2: Float, f_3: Float, f_4: Float, f_5: Float, f_6: Float, i_7: Int, i_8: Int, i_9: Int) {
        e(nativeid, f_1, f_2, f_3, f_4, f_5, f_6, i_7, i_8, i_9, 1)
    }

    private external fun EA(var1: SoftwareToolkit, var2: IntArray, var3: ByteArray, var4: ByteArray, var5: Int, var6: Int, var7: Int, var8: Int)

    private external fun U(var1: Long, var3: Int, var4: Int, var5: Int, var6: Int)

    private external fun ha(var1: Long, var3: Int, var4: Int, var5: Int, var6: Int, var7: Int, var8: Int, var9: Int, var10: Int)

    override fun getUseHeight(): Int {
        return GA(nativeid)
    }

    override fun getSpriteHeight(): Int {
        return ba(nativeid)
    }

    private external fun UA(var1: SoftwareToolkit, var2: Int, var3: Int)

    override fun cacheSizeInto(ints_1: IntArray) {
        F(nativeid, ints_1)
    }
}
