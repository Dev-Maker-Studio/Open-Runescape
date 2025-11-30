package com.jagex.game.runetek5.graphics.texture.sampler

import jagdx.IDirect3DDevice
import jagdx.IDirect3DTexture

import java.nio.ByteBuffer

import com.jagex.game.runetek5.toolkit.DirectXNativeToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat
import com.jagex.game.runetek5.utils.MathUtils

open class TextureDirect3D : Direct3DTextureSampler, NativeTextureSampler {

    @JvmField val anInt9154: Int
    @JvmField val anInt9157: Int
    @JvmField var aBool9156: Boolean = false
    @JvmField var aBool9155: Boolean = false

    constructor(class505Sub2Sub21: DirectXNativeToolkit, class1502: GLFormat, i_3: Int, i_4: Int, bool5: Boolean, bytes_6: ByteArray) : super(class505Sub2Sub21, class1502, GLActionFormat.PIXEL_FORMAT, bool5 && class505Sub2Sub21.SUPPORTS_CUBE_MAP) {
        var i_81 = 0
        if (!super.dxToolkit.IS_NO_MIPMAP) {
            this.anInt9154 = MathUtils.nextPowerOfTwo(i_3)
            this.anInt9157 = MathUtils.nextPowerOfTwo(i_4)
        } else {
            this.anInt9154 = i_3
            this.anInt9157 = i_4
        }

        if (bool5) {
            super.handle = IDirect3DDevice.CreateTexture(super.dxToolkit.renderTarget, this.anInt9154, this.anInt9157, 0, 1024, DirectXNativeToolkit.method15652(super.glformat, GLActionFormat.PIXEL_FORMAT), 1)
        } else {
            super.handle = IDirect3DDevice.CreateTexture(super.dxToolkit.renderTarget, this.anInt9154, this.anInt9157, 1, 0, DirectXNativeToolkit.method15652(super.glformat, GLActionFormat.PIXEL_FORMAT), 1)
        }

        i_81 = i_3

        val bytebuffer9 = super.dxToolkit.directBuffer
        bytebuffer9.clear()
        val class15010 = super.glformat
        val class15010000 = super.glformat
        if (class15010 === GLFormat.aClass1501956) {
            bytebuffer9.put(bytes_6, 0, i_3 * i_4 / 2)
            IDirect3DTexture.Upload(super.handle, 0, 0, 0, i_3, i_4 / 4, i_3 / 4 * 8, 0, super.dxToolkit.directBufferAddress)
        } else {
            val class15011 = super.glformat
            if (class15011 === GLFormat.aClass1501957) {
                bytebuffer9.put(bytes_6, 0, i_3 * i_4)
                IDirect3DTexture.Upload(super.handle, 0, 0, 0, i_3, i_4 / 4, i_3 / 4 * 16, 0, super.dxToolkit.directBufferAddress)
            } else {
                bytebuffer9.put(bytes_6, 0, i_81 * i_4)
                IDirect3DTexture.Upload(super.handle, 0, 0, 0, i_3, i_4, super.glformat.bytesPerPixel * i_81, 0, super.dxToolkit.directBufferAddress)
            }
        }

    }

    protected constructor(class505Sub2Sub21: DirectXNativeToolkit, class1502: GLFormat, class763: GLActionFormat, i_4: Int, i_5: Int, i_6: Int, i_7: Int) : super(class505Sub2Sub21, class1502, class763, false) {
        if (!super.dxToolkit.IS_NO_MIPMAP) {
            this.anInt9154 = MathUtils.nextPowerOfTwo(i_4)
            this.anInt9157 = MathUtils.nextPowerOfTwo(i_5)
        } else {
            this.anInt9154 = i_4
            this.anInt9157 = i_5
        }

        super.handle = IDirect3DDevice.CreateTexture(super.dxToolkit.renderTarget, i_4, i_5, 0, i_6, DirectXNativeToolkit.method15652(super.glformat, super.glactionFormat), i_7)
    }

    constructor(class505Sub2Sub21: DirectXNativeToolkit, class1502: GLFormat, i_3: Int, i_4: Int, floats_6: FloatArray) : super(class505Sub2Sub21, class1502, GLActionFormat.PIXEL_FORMAT, class505Sub2Sub21.SUPPORTS_CUBE_MAP) {
        var i_81 = 0
        if (!super.dxToolkit.IS_NO_MIPMAP) {
            this.anInt9154 = MathUtils.nextPowerOfTwo(i_3)
            this.anInt9157 = MathUtils.nextPowerOfTwo(i_4)
        } else {
            this.anInt9154 = i_3
            this.anInt9157 = i_4
        }

        super.handle = IDirect3DDevice.CreateTexture(super.dxToolkit.renderTarget, this.anInt9154, this.anInt9157, 0, 1024, DirectXNativeToolkit.method15652(super.glformat, GLActionFormat.FLOAT_32_ARB), 1)

        i_81 = i_3

        val bytebuffer9 = super.dxToolkit.directBuffer
        bytebuffer9.clear()
        bytebuffer9.asFloatBuffer().put(floats_6, 0, super.glformat.bytesPerPixel * i_81 * i_4)
        IDirect3DTexture.Upload(super.handle, 0, 0, 0, i_3, i_4, super.glformat.bytesPerPixel * i_81 * 4, 0, super.dxToolkit.directBufferAddress)
    }

    constructor(class505Sub2Sub21: DirectXNativeToolkit, class1502: GLFormat, class763: GLActionFormat, i_4: Int, i_5: Int) : this(class505Sub2Sub21, class1502, class763, i_4, i_5, 0, 1)

    constructor(class505Sub2Sub21: DirectXNativeToolkit, i_2: Int, i_3: Int, bool4: Boolean, ints_5: IntArray, i_6: Int, i_7: Int) : super(class505Sub2Sub21, GLFormat.HDR, GLActionFormat.PIXEL_FORMAT, bool4 && class505Sub2Sub21.SUPPORTS_CUBE_MAP) {
        var i_71 = i_7
        if (!super.dxToolkit.IS_NO_MIPMAP) {
            this.anInt9154 = MathUtils.nextPowerOfTwo(i_2)
            this.anInt9157 = MathUtils.nextPowerOfTwo(i_3)
        } else {
            this.anInt9154 = i_2
            this.anInt9157 = i_3
        }

        if (bool4) {
            super.handle = IDirect3DDevice.CreateTexture(super.dxToolkit.renderTarget, this.anInt9154, this.anInt9157, 0, 1024, 21, 1)
        } else {
            super.handle = IDirect3DDevice.CreateTexture(super.dxToolkit.renderTarget, this.anInt9154, this.anInt9157, 1, 0, 21, 1)
        }

        if (i_71 == 0) {
            i_71 = i_2
        }

        val bytebuffer8 = super.dxToolkit.directBuffer
        bytebuffer8.clear()
        bytebuffer8.asIntBuffer().put(ints_5, i_6, i_3 * i_71)
        IDirect3DTexture.Upload(super.handle, 0, 0, 0, i_2, i_3, super.glformat.bytesPerPixel * i_71, 0, super.dxToolkit.directBufferAddress)
    }

    override fun method46(): Boolean {
        return true
    }

    override fun method48(i_1: Int, i_2: Int, i_3: Int, i_4: Int, src: IntArray, offset: Int, i_7: Int) {
        var i_71 = i_7
        if (super.glformat === GLFormat.HDR && super.glactionFormat === GLActionFormat.PIXEL_FORMAT) {
            if (i_71 == 0) {
                i_71 = i_3
            }

            val buffer = super.dxToolkit.directBuffer
            buffer.clear()
            buffer.asIntBuffer().put(src, offset, i_4 * i_71)
            IDirect3DTexture.Upload(super.handle, 0, i_1, i_2, i_3, i_4, super.glformat.bytesPerPixel * i_71, 0, super.dxToolkit.directBufferAddress)
        } else {
            throw RuntimeException()
        }
    }

    override fun getHandle(): Long {
        return super.handle
    }

    override fun method62(f_1: Float): Float {
        return f_1 / this.anInt9157
    }

    override fun method52(i_1: Int, i_2: Int, i_3: Int, i_4: Int, bytes_5: ByteArray, class1506: GLFormat, i_7: Int, i_8: Int) {
        var i_81 = i_8
        if (class1506 === super.glformat && super.glactionFormat === GLActionFormat.PIXEL_FORMAT) {
            if (i_81 == 0) {
                i_81 = i_3
            }

            val bytebuffer9 = super.dxToolkit.directBuffer
            bytebuffer9.clear()
            bytebuffer9.put(bytes_5, i_7, i_81 * i_4)
            IDirect3DTexture.Upload(super.handle, 0, i_1, i_2, i_3, i_4, super.glformat.bytesPerPixel * i_81, 0, super.dxToolkit.directBufferAddress)
        } else {
            throw RuntimeException()
        }
    }

    override fun setRepeat(bool1: Boolean, bool2: Boolean) {
        this.aBool9156 = bool1
        this.aBool9155 = bool2
    }

    override fun method53(i_3: Int, i_4: Int, ints_5: IntArray) {
        if (super.glformat === GLFormat.HDR && super.glactionFormat === GLActionFormat.PIXEL_FORMAT) {
            val bytebuffer7 = super.dxToolkit.directBuffer
            bytebuffer7.clear()
            IDirect3DTexture.Download(super.handle, 0, 0, 0, i_3, i_4, i_3 * 4, 0, super.dxToolkit.directBufferAddress)
            bytebuffer7.asIntBuffer().get(ints_5, 0, i_3 * i_4)
        } else {
            throw RuntimeException()
        }
    }

    override fun bindTexture() {
        super.dxToolkit.setTextureSamplerState(this)
    }

    override fun method47(f_1: Float): Float {
        return f_1 / this.anInt9154
    }

    override fun deallocate() {
        super.deallocate()
    }

    override fun getTextureHeight(): Int {
        return this.anInt9157
    }

    override fun enableMipmapping(): Boolean {
        return false
    }

    override fun getTextureWidth(): Int {
        return this.anInt9154
    }

    override fun setSamplerMode(class351: SampleMode) {
        super.setSamplerMode(class351)
    }
}
