package com.jagex.game.runetek5.graphics.sprite

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface
import com.jagex.game.runetek5.framebuffer.FrameBuffer
import com.jagex.game.runetek5.graphics.shader.program.AbstractSpriteShader
import com.jagex.game.runetek5.graphics.texture.sampler.NativeTextureSampler
import com.jagex.game.runetek5.graphics.texture.sampler.RectangleTextureSampler
import com.jagex.game.runetek5.math.Matrix44
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat
import com.jagex.game.runetek5.unidentified.Class455_Sub1

class SpriteDX_Sub3 : NativeSprite {

    @JvmField
    var aBool9051: Boolean = false
    @JvmField
    var anInt9057: Int = 0
    @JvmField
    var anInt9055: Int = 0
    @JvmField
    var anInt9049: Int = 0
    @JvmField
    var anInt9058: Int = 0
    @JvmField
    val nativeToolkit: AbstractNativeToolkit
    @JvmField
    val anInt9060: Int
    @JvmField
    val anInt9053: Int
    @JvmField
    val anInterface69050: NativeTextureSampler
    @JvmField
    val anInterface99054: FrameBuffer?
    @JvmField
    val aBool9059: Boolean
    @JvmField
    val aBool9056: Boolean
    @JvmField
    val aBool9061: Boolean
    @JvmField
    val aBool9062: Boolean

    constructor(class505Sub21: AbstractNativeToolkit, interface62: NativeTextureSampler) : this(
        class505Sub21, interface62, interface62.getTextureWidth(), interface62.getTextureHeight()
    )

    constructor(
        class505Sub21: AbstractNativeToolkit,
        interface62: NativeTextureSampler,
        i_3: Int,
        i_4: Int
    ) {
        aBool9051 = false
        anInt9057 = 0
        anInt9055 = 0
        anInt9049 = 0
        anInt9058 = 0
        nativeToolkit = class505Sub21
        anInt9060 = i_3
        anInt9053 = i_4
        anInterface69050 = interface62
        anInterface99054 = null
        aBool9059 = anInterface69050.getTextureWidth() != i_3
        aBool9056 = anInterface69050.getTextureHeight() != i_4
        aBool9061 = !aBool9059 && anInterface69050.method46()
        aBool9062 = !aBool9056 && anInterface69050.method46()
        nativeToolkit.getShaderByEffect(12)
    }

    constructor(
        class505Sub21: AbstractNativeToolkit,
        i_2: Int,
        i_3: Int,
        ints_4: IntArray,
        i_5: Int,
        i_6: Int
    ) {
        aBool9051 = false
        anInt9057 = 0
        anInt9055 = 0
        anInt9049 = 0
        anInt9058 = 0
        nativeToolkit = class505Sub21
        anInt9060 = i_2
        anInt9053 = i_3
        anInterface99054 = null
        anInterface69050 = class505Sub21.createTextureSampler(i_2, i_3, false, ints_4, i_5, i_6)
        anInterface69050.setRepeat(true, true)
        aBool9059 = anInterface69050.getTextureWidth() != i_2
        aBool9056 = anInterface69050.getTextureHeight() != i_3
        aBool9061 = !aBool9059 && anInterface69050.method46()
        aBool9062 = !aBool9056 && anInterface69050.method46()
        nativeToolkit.getShaderByEffect(12)
    }

    constructor(
        class505Sub21: AbstractNativeToolkit,
        i_2: Int,
        i_3: Int,
        bool4: Boolean,
        bool5: Boolean
    ) {
        aBool9051 = false
        anInt9057 = 0
        anInt9055 = 0
        anInt9049 = 0
        anInt9058 = 0
        nativeToolkit = class505Sub21
        anInt9060 = i_2
        anInt9053 = i_3
        if (bool5) {
            val interface296: RectangleTextureSampler = class505Sub21.method14064(
                if (bool4) GLFormat.HDR else GLFormat.aClass1501950,
                GLActionFormat.PIXEL_FORMAT,
                i_2,
                i_3
            )
            anInterface99054 = interface296.getFramebuffer()
            anInterface69050 = interface296
        } else {
            anInterface69050 = class505Sub21.method14072(
                if (bool4) GLFormat.HDR else GLFormat.aClass1501950,
                GLActionFormat.PIXEL_FORMAT,
                i_2,
                i_3
            )
            anInterface99054 = null
        }

        anInterface69050.setRepeat(true, true)
        aBool9059 = anInterface69050.getTextureWidth() != i_2
        aBool9056 = anInterface69050.getTextureHeight() != i_3
        aBool9061 = !aBool9059 && anInterface69050.method46()
        aBool9062 = !aBool9056 && anInterface69050.method46()
        nativeToolkit.getShaderByEffect(12)
    }


    override fun drawColoredCircle(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, colorArgb: Int, blending: Int) {
        nativeToolkit.method13935()
        nativeToolkit.toggleBlending(blending)
        val shader: AbstractSpriteShader = nativeToolkit.spriteShader
        shader.anInterface6443 = anInterface69050
        shader.setColours(i_5, colorArgb)
        var localI3 = i_3
        var localI4 = i_4
        var localI1 = i_1
        var localI2 = i_2
        if (aBool9051) {
            localI3 = localI3 * anInt9060 / getSpriteWidth()
            localI4 = localI4 * anInt9053 / getSpriteHeight()
            localI1 += localI3 * anInt9055 / anInt9060
            localI2 += localI4 * anInt9057 / anInt9053
        }

        val displayWidth = nativeToolkit.getDisplay()!!.getWidth()
        val displayHeight = nativeToolkit.getDisplay()!!.getHeight()
        shader.worldViewProjectionMatrix.method6525(localI3 * 2.0F / displayWidth, localI4 * 2.0F / displayHeight, 1.0F, 1.0F)
        shader.worldViewProjectionMatrix.faceList[12] = (localI1 + nativeToolkit.method13905()) * 2.0F / displayWidth - 1.0F
        shader.worldViewProjectionMatrix.faceList[13] = (localI2 + nativeToolkit.method13905()) * 2.0F / displayHeight - 1.0F
        shader.worldViewProjectionMatrix.faceList[14] = -1.0f
        shader.aClass384440.method6525(anInterface69050.method47(anInt9060.toFloat()).toFloat(), anInterface69050.method62(anInt9053.toFloat()).toFloat(), 1.0F, 1.0F)
        shader.anInterface4445 = nativeToolkit.anInterface48746
        shader.anInt446 = 0
        shader.aClass70447 = nativeToolkit.aClass708786
        shader.loadNormal()
    }

    override fun pixelTransfer() {
        val ints_4 = nativeToolkit.copyPixelRectangle(0, 0, anInt9060, anInt9053)
        val ints_5 = IntArray(anInt9060 * anInt9053)
        anInterface69050.method53(anInt9060, anInt9053, ints_5)
        var i_6: Int
        var i_7: Int
        var i_8: Int
        i_6 = 0
        while (i_6 < anInt9053) {
            i_7 = i_6 * anInt9060

            i_8 = 0
            while (i_8 < anInt9060) {
                ints_5[i_7 + i_8] = ints_5[i_7 + i_8] and 0xffffff or (if (ints_4[i_7 + i_8] != 0) -0x1000000 else 0)
                i_8++
            }
            i_6++
        }

        method14425(0, 0, anInt9060, anInt9053, ints_5, anInt9060)
    }

    override fun method2750(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int) {
        val ints_7 = nativeToolkit.copyPixelRectangle(i_5, i_6, i_3, i_4)
        if (ints_7 != null) {
            for (i_8 in ints_7.indices) {
                ints_7[i_8] = ints_7[i_8] or (-0x1000000)
            }

            method14425(i_1, i_2, i_3, i_4, ints_7, i_3)
        }

    }

    override fun getWidth(): Int {
        return anInt9060
    }

    override fun getHeight(): Int {
        return anInt9053
    }

    override fun method2764(f_1: Float, f_2: Float, f_3: Float, f_4: Float, f_5: Float, f_6: Float, i_7: Int, i_8: Int, i_9: Int) {
        nativeToolkit.method13935()
        nativeToolkit.toggleBlending(i_9)
        var localF1 = f_1
        var localF2 = f_2
        var localF3 = f_3
        var localF4 = f_4
        var localF5 = f_5
        var localF6 = f_6
        var f_12: Float
        var f_13: Float
        if (aBool9051) {
            val f_11 = getSpriteWidth().toFloat()
            f_12 = getSpriteHeight().toFloat()
            f_13 = (localF3 - localF1) / f_11
            val f_14 = (localF4 - localF2) / f_11
            val f_15 = (localF5 - localF1) / f_12
            val f_16 = (localF6 - localF2) / f_12
            val f_17 = f_15 * anInt9057
            val f_18 = f_16 * anInt9057
            val f_19 = f_13 * anInt9055
            val f_20 = f_14 * anInt9055
            val f_21 = -f_13 * anInt9058
            val f_22 = -f_14 * anInt9058
            val f_23 = -f_15 * anInt9049
            val f_24 = -f_16 * anInt9049
            localF1 = localF1 + f_19 + f_17
            localF2 = localF2 + f_20 + f_18
            localF3 = localF3 + f_21 + f_17
            localF4 = localF4 + f_22 + f_18
            localF5 = localF5 + f_19 + f_23
            localF6 = localF6 + f_20 + f_24
        }

        val class4725: AbstractSpriteShader = nativeToolkit.spriteShader
        class4725.anInterface6443 = anInterface69050
        class4725.setColours(i_7, i_8)
        f_12 = nativeToolkit.getDisplay()!!.getWidth().toFloat()
        f_13 = nativeToolkit.getDisplay()!!.getHeight().toFloat()
        class4725.worldViewProjectionMatrix.identity()
        class4725.worldViewProjectionMatrix.faceList[0] = (localF3 - localF1) * 2.0F / f_12
        class4725.worldViewProjectionMatrix.faceList[1] = (localF4 - localF2) * 2.0F / f_13
        class4725.worldViewProjectionMatrix.faceList[4] = (localF5 - localF1) * 2.0F / f_12
        class4725.worldViewProjectionMatrix.faceList[5] = (localF6 - localF2) * 2.0F / f_13
        class4725.worldViewProjectionMatrix.faceList[12] = (localF1 + nativeToolkit.method13905()) * 2.0F / f_12 - 1.0F
        class4725.worldViewProjectionMatrix.faceList[13] = (localF2 + nativeToolkit.method13905()) * 2.0F / f_13 - 1.0F
        class4725.worldViewProjectionMatrix.faceList[14] = -1.0f
        class4725.aClass384440.method6525(anInterface69050.method47(anInt9060.toFloat()), anInterface69050.method62(anInt9053.toFloat()), 1.0F, 1.0F)
        class4725.anInterface4445 = nativeToolkit.anInterface48746
        class4725.anInt446 = 0
        class4725.aClass70447 = nativeToolkit.aClass708786
        class4725.loadNormal()
    }

    override fun getFramebuffer(): FrameBuffer? {
        return anInterface99054
    }

    override fun method2773(i_1: Int, i_2: Int, class4553: ComponentSurface, i_4: Int, i_5: Int) {
        nativeToolkit.method13935()
        val class476: AbstractSpriteShader = nativeToolkit.spriteShader
        class476.anInterface6443 = anInterface69050
        class476.setColours(1, -1)
        var localI1 = i_1
        var localI2 = i_2
        localI1 += anInt9055
        localI2 += anInt9057
        val f_7 = nativeToolkit.getDisplay()!!.getWidth()
        val f_8 = nativeToolkit.getDisplay()!!.getHeight()
        class476.worldViewProjectionMatrix.method6525(anInt9060 * 2.0F / f_7, anInt9053 * 2.0F / f_8, 1.0F, 1.0F)
        class476.worldViewProjectionMatrix.faceList[12] = (localI1 + nativeToolkit.method13905()) * 2.0F / f_7 - 1.0F
        class476.worldViewProjectionMatrix.faceList[13] = (localI2 + nativeToolkit.method13905()) * 2.0F / f_8 - 1.0F
        class476.worldViewProjectionMatrix.faceList[14] = -1.0f
        class476.aClass384440.method6525(anInterface69050.method47(anInt9060.toFloat()), anInterface69050.method62(anInt9053.toFloat()), 1.0F, 1.0F)
        class476.anInterface4445 = nativeToolkit.anInterface48746
        class476.anInt446 = 0
        class476.aClass70447 = nativeToolkit.aClass708786
        val interface69: NativeTextureSampler = (class4553 as Class455_Sub1).nativeTextureSampler
        class476.anInterface6441 = interface69
        class476.aClass384444.method6525(interface69.method47(anInt9060.toFloat()), interface69.method62(anInt9053.toFloat()), 1.0F, 1.0F)
        class476.aClass384444.faceList[12] = interface69.method47((localI1 - i_4).toFloat())
        class476.aClass384444.faceList[13] = interface69.method62((localI2 - i_5).toFloat())
        class476.loadMasked()
    }


    override fun method2772(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int, i_7: Int) {
        nativeToolkit.method13935()
        nativeToolkit.toggleBlending(i_7)
        val class478: AbstractSpriteShader = nativeToolkit.spriteShader
        class478.anInterface6443 = anInterface69050
        class478.setColours(i_5, i_6)
        val f_9 = nativeToolkit.getDisplay()!!.getWidth()
        val f_10 = nativeToolkit.getDisplay()!!.getHeight()
        class478.anInterface4445 = nativeToolkit.anInterface48746
        class478.anInt446 = 0
        class478.aClass70447 = nativeToolkit.aClass708786
        val bool11 = aBool9062 && anInt9057 == 0 && anInt9049 == 0
        val bool12 = aBool9061 && anInt9055 == 0 && anInt9058 == 0
        if (bool12 and bool11) {
            class478.worldViewProjectionMatrix.method6525(i_3 * 2.0F / f_9, i_4 * 2.0F / f_10, 1.0F, 1.0F)
            class478.worldViewProjectionMatrix.faceList[12] = (i_1 + nativeToolkit.method13905()) * 2.0F / f_9 - 1.0F
            class478.worldViewProjectionMatrix.faceList[13] = (i_2 + nativeToolkit.method13905()) * 2.0F / f_10 - 1.0F
            class478.worldViewProjectionMatrix.faceList[14] = -1.0f
            class478.aClass384440.method6525(anInterface69050.method47(i_3.toFloat()), anInterface69050.method62(i_4.toFloat()), 1.0F, 1.0F)
            class478.loadNormal()
        } else {
            var i_13: Int
            var i_14: Int
            var i_15: Int
            var i_16: Int
            if (bool12) {
                i_13 = i_2 + i_4
                i_14 = getSpriteHeight()
                class478.aClass384440.method6525(anInterface69050.method47(i_3.toFloat()), anInterface69050.method62(anInt9053.toFloat()), 1.0F, 1.0F)
                i_15 = i_2 + anInt9057

                i_16 = i_15 + anInt9053
                while (i_16 <= i_13) {
                    class478.worldViewProjectionMatrix.method6525(i_3 * 2.0F / f_9, anInt9053 * 2.0F / f_10, 1.0F, 1.0F)
                    class478.worldViewProjectionMatrix.faceList[12] = (i_1 + nativeToolkit.method13905()) * 2.0F / f_9 - 1.0F
                    class478.worldViewProjectionMatrix.faceList[13] = (i_15 + nativeToolkit.method13905()) * 2.0F / f_10 - 1.0F
                    class478.worldViewProjectionMatrix.faceList[14] = -1.0f
                    class478.loadNormal()
                    i_15 += i_14
                    i_16 = i_15 + anInt9053
                }

                if (i_15 < i_13) {
                    i_16 = i_13 - i_15
                    class478.aClass384440.method6525(anInterface69050.method47(i_3.toFloat()), anInterface69050.method62(i_16.toFloat()), 1.0F, 1.0F)
                    class478.worldViewProjectionMatrix.method6525(i_3 * 2.0F / f_9, i_16 * 2.0F / f_10, 1.0F, 1.0F)
                    class478.worldViewProjectionMatrix.faceList[12] = (i_1 + nativeToolkit.method13905()) * 2.0F / f_9 - 1.0F
                    class478.worldViewProjectionMatrix.faceList[13] = (i_15 + nativeToolkit.method13905()) * 2.0F / f_10 - 1.0F
                    class478.worldViewProjectionMatrix.faceList[14] = -1.0f
                    class478.loadNormal()
                }
            } else if (bool11) {
                i_13 = i_3 + i_1
                i_14 = getSpriteWidth()
                class478.aClass384440.method6525(anInterface69050.method47(anInt9060.toFloat()), anInterface69050.method62(i_4.toFloat()), 1.0F, 1.0F)
                i_15 = anInt9055 + i_1

                i_16 = i_15 + anInt9060
                while (i_16 <= i_13) {
                    class478.worldViewProjectionMatrix.method6525(anInt9060 * 2.0F / f_9, i_4 * 2.0F / f_10, 1.0F, 1.0F)
                    class478.worldViewProjectionMatrix.faceList[12] = (i_15 + nativeToolkit.method13905()) * 2.0F / f_9 - 1.0F
                    class478.worldViewProjectionMatrix.faceList[13] = (i_2 + nativeToolkit.method13905()) * 2.0F / f_10 - 1.0F
                    class478.worldViewProjectionMatrix.faceList[14] = -1.0f
                    class478.loadNormal()
                    i_15 += i_14
                    i_16 = i_15 + anInt9060
                }

                if (i_15 < i_13) {
                    i_16 = i_13 - i_15
                    class478.aClass384440.method6525(anInterface69050.method47(i_16.toFloat()), anInterface69050.method62(i_4.toFloat()), 1.0F, 1.0F)
                    class478.worldViewProjectionMatrix.method6525(i_16 * 2.0F / f_9, i_4 * 2.0F / f_10, 1.0F, 1.0F)
                    class478.worldViewProjectionMatrix.faceList[12] = (i_15 + nativeToolkit.method13905()) * 2.0F / f_9 - 1.0F
                    class478.worldViewProjectionMatrix.faceList[13] = (i_2 + nativeToolkit.method13905()) * 2.0F / f_10 - 1.0F
                    class478.worldViewProjectionMatrix.faceList[14] = -1.0f
                    class478.loadNormal()
                }
            } else {
                i_13 = i_2 + i_4
                i_14 = i_3 + i_1
                i_15 = getSpriteWidth()
                i_16 = getSpriteHeight()
                var i_17 = i_2 + anInt9057

                var i_18: Int
                var i_19: Int
                var i_20: Int
                i_18 = i_17 + anInt9053
                while (i_18 <= i_13) {
                    class478.aClass384440.method6525(anInterface69050.method47(anInt9060.toFloat()), anInterface69050.method62(anInt9053.toFloat()), 1.0F, 1.0F)
                    i_19 = anInt9055 + i_1

                    i_20 = i_19 + anInt9060
                    while (i_20 <= i_14) {
                        class478.worldViewProjectionMatrix.method6525(anInt9060 * 2.0F / f_9, anInt9053 * 2.0F / f_10, 1.0F, 1.0F)
                        class478.worldViewProjectionMatrix.faceList[12] = (i_19 + nativeToolkit.method13905()) * 2.0F / f_9 - 1.0F
                        class478.worldViewProjectionMatrix.faceList[13] = (i_17 + nativeToolkit.method13905()) * 2.0F / f_10 - 1.0F
                        class478.worldViewProjectionMatrix.faceList[14] = -1.0f
                        class478.loadNormal()
                        i_19 += i_15
                        i_20 = i_19 + anInt9060
                    }

                    if (i_19 < i_14) {
                        i_20 = i_14 - i_19
                        class478.aClass384440.method6525(anInterface69050.method47(i_20.toFloat()), anInterface69050.method62(anInt9053.toFloat()), 1.0F, 1.0F)
                        class478.worldViewProjectionMatrix.method6525(i_20 * 2.0F / f_9, anInt9053 * 2.0F / f_10, 1.0F, 1.0F)
                        class478.worldViewProjectionMatrix.faceList[12] = (i_19 + nativeToolkit.method13905()) * 2.0F / f_9 - 1.0F
                        class478.worldViewProjectionMatrix.faceList[13] = (i_17 + nativeToolkit.method13905()) * 2.0F / f_10 - 1.0F
                        class478.worldViewProjectionMatrix.faceList[14] = -1.0f
                        class478.loadNormal()
                    }

                    i_17 += i_16
                    i_18 = i_17 + anInt9053
                }

                if (i_17 < i_13) {
                    i_18 = i_13 - i_17
                    class478.aClass384440.method6525(anInterface69050.method47(anInt9060.toFloat()), anInterface69050.method62(i_18.toFloat()), 1.0F, 1.0F)
                    i_19 = anInt9055 + i_1

                    i_20 = i_19 + anInt9060
                    while (i_20 <= i_14) {
                        class478.worldViewProjectionMatrix.method6525(anInt9060 * 2.0F / f_9, i_18 * 2.0F / f_10, 1.0F, 1.0F)
                        class478.worldViewProjectionMatrix.faceList[12] = (i_19 + nativeToolkit.method13905()) * 2.0F / f_9 - 1.0F
                        class478.worldViewProjectionMatrix.faceList[13] = (i_17 + nativeToolkit.method13905()) * 2.0F / f_10 - 1.0F
                        class478.worldViewProjectionMatrix.faceList[14] = -1.0f
                        class478.loadNormal()
                        i_19 += i_15
                        i_20 = i_19 + anInt9060
                    }

                    if (i_19 < i_14) {
                        i_20 = i_14 - i_19
                        class478.aClass384440.method6525(anInterface69050.method47(i_20.toFloat()), anInterface69050.method62(i_18.toFloat()), 1.0F, 1.0F)
                        class478.worldViewProjectionMatrix.method6525(i_20 * 2.0F / f_9, i_18 * 2.0F / f_10, 1.0F, 1.0F)
                        class478.worldViewProjectionMatrix.faceList[12] = (i_19 + nativeToolkit.method13905()) * 2.0F / f_9 - 1.0F
                        class478.worldViewProjectionMatrix.faceList[13] = (i_17 + nativeToolkit.method13905()) * 2.0F / f_10 - 1.0F
                        class478.worldViewProjectionMatrix.faceList[14] = -1.0f
                        class478.loadNormal()
                    }
                }
            }
        }

    }


    override fun method2766(
        f_1: Float,
        f_2: Float,
        f_3: Float,
        f_4: Float,
        f_5: Float,
        f_6: Float,
        i_7: Int,
        class4558: ComponentSurface,
        i_9: Int,
        i_10: Int
    ) {
        nativeToolkit.method13935()
        val class4711: AbstractSpriteShader = nativeToolkit.spriteShader
        class4711.anInterface6443 = anInterface69050
        class4711.setColours(1, -1)
        val f_12 = nativeToolkit.getDisplay()!!.getWidth()
        val f_13 = nativeToolkit.getDisplay()!!.getHeight()
        class4711.worldViewProjectionMatrix.identity()
        if (aBool9051) {
            val f_14 = anInt9060.toFloat() / getSpriteWidth()
            val f_15 = anInt9053.toFloat() / getSpriteHeight()
            class4711.worldViewProjectionMatrix.faceList[0] = (f_3 - f_1) * f_14
            class4711.worldViewProjectionMatrix.faceList[1] = (f_4 - f_2) * f_14
            class4711.worldViewProjectionMatrix.faceList[4] = (f_5 - f_1) * f_15
            class4711.worldViewProjectionMatrix.faceList[5] = (f_6 - f_2) * f_15
            class4711.worldViewProjectionMatrix.faceList[12] = (f_1 + anInt9055) * f_14 + nativeToolkit.method13905()
            class4711.worldViewProjectionMatrix.faceList[13] = (f_2 + anInt9057) * f_15 + nativeToolkit.method13905()
        } else {
            class4711.worldViewProjectionMatrix.faceList[0] = f_3 - f_1
            class4711.worldViewProjectionMatrix.faceList[1] = f_4 - f_2
            class4711.worldViewProjectionMatrix.faceList[4] = f_5 - f_1
            class4711.worldViewProjectionMatrix.faceList[5] = f_6 - f_2
            class4711.worldViewProjectionMatrix.faceList[12] = f_1 + nativeToolkit.method13905()
            class4711.worldViewProjectionMatrix.faceList[13] = f_2 + nativeToolkit.method13905()
        }

        val matrix4416: Matrix44 = nativeToolkit.aClass3848683
        matrix4416.identity()
        matrix4416.faceList[0] = 2.0F / f_12
        matrix4416.faceList[5] = 2.0F / f_13
        matrix4416.faceList[12] = -1.0f
        matrix4416.faceList[13] = -1.0f
        matrix4416.faceList[14] = -1.0f
        class4711.worldViewProjectionMatrix.multiplyBy(matrix4416)
        class4711.aClass384440.method6525(anInterface69050.method47(anInt9060.toFloat()), anInterface69050.method62(anInt9053.toFloat()), 1.0F, 1.0F)
        class4711.anInterface4445 = nativeToolkit.anInterface48746
        class4711.anInt446 = 0
        class4711.aClass70447 = nativeToolkit.aClass708786
        val interface617: NativeTextureSampler = (class4558 as Class455_Sub1).nativeTextureSampler
        class4711.anInterface6441 = interface617
        class4711.aClass384444.identity()
        class4711.aClass384444.faceList[0] = (f_3 - f_1) * interface617.method47(1.0F)
        class4711.aClass384444.faceList[1] = (f_4 - f_2) * interface617.method47(1.0F)
        class4711.aClass384444.faceList[4] = (f_5 - f_1) * interface617.method62(1.0F)
        class4711.aClass384444.faceList[5] = (f_6 - f_2) * interface617.method62(1.0F)
        class4711.aClass384444.faceList[12] = (f_1 - i_9) * interface617.method47(1.0F)
        class4711.aClass384444.faceList[13] = (f_2 - i_10) * interface617.method62(1.0F)
        class4711.loadMasked()
    }

    override fun draw(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int) {
        nativeToolkit.method13935()
        nativeToolkit.toggleBlending(i_5)
        val class476: AbstractSpriteShader = nativeToolkit.spriteShader
        class476.anInterface6443 = anInterface69050
        class476.setColours(i_3, i_4)
        var localI1 = i_1
        var localI2 = i_2
        localI1 += anInt9055
        localI2 += anInt9057
        val f_7 = nativeToolkit.getDisplay()!!.getWidth()
        val f_8 = nativeToolkit.getDisplay()!!.getHeight()
        class476.worldViewProjectionMatrix.method6525(anInt9060 * 2.0F / f_7, anInt9053 * 2.0F / f_8, 1.0F, 1.0F)
        class476.worldViewProjectionMatrix.faceList[12] = (localI1 + nativeToolkit.method13905()) * 2.0F / f_7 - 1.0F
        class476.worldViewProjectionMatrix.faceList[13] = (localI2 + nativeToolkit.method13905()) * 2.0F / f_8 - 1.0F
        class476.worldViewProjectionMatrix.faceList[14] = -1.0f
        class476.aClass384440.method6525(anInterface69050.method47(anInt9060.toFloat()), anInterface69050.method62(anInt9053.toFloat()), 1.0F, 1.0F)
        class476.anInterface4445 = nativeToolkit.anInterface48746
        class476.anInt446 = 0
        class476.aClass70447 = nativeToolkit.aClass708786
        class476.loadNormal()
    }


    fun method14425(i_1: Int, i_2: Int, i_3: Int, i_4: Int, ints_5: IntArray, i_7: Int) {
        anInterface69050.method48(i_1, i_2, i_3, i_4, ints_5, 0, i_7)
    }


    override fun setSize(i_1: Int, i_2: Int, i_3: Int, i_4: Int) {
        anInt9055 = i_1
        anInt9057 = i_2
        anInt9058 = i_3
        anInt9049 = i_4
        aBool9051 = anInt9055 != 0 || anInt9057 != 0 || anInt9058 != 0 || anInt9049 != 0
    }


    override fun getSpriteHeight(): Int {
        return anInt9053 + anInt9057 + anInt9049
    }

    override fun cacheSizeInto(ints_1: IntArray) {
        ints_1[0] = anInt9055
        ints_1[1] = anInt9057
        ints_1[2] = anInt9058
        ints_1[3] = anInt9049
    }

    override fun getSpriteWidth(): Int {
        return anInt9060 + anInt9055 + anInt9058
    }

}
