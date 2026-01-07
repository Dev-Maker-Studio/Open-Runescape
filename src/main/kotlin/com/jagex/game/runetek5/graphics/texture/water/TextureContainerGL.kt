package com.jagex.game.runetek5.graphics.texture.water

import com.jagex.game.runetek5.buffer.DataBuffer
import com.jagex.game.runetek5.graphics.shader.Texture3DShaderGL
import com.jagex.game.runetek5.graphics.texture.OpenGl2DTexture
import com.jagex.game.runetek5.graphics.texture.OpenGl3DTexture
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat

/**
 * Container for OpenGL water textures (2D and 3D).
 */
class TextureContainerGL(openglToolkit: OpenGlToolkit) {

    @JvmField var textures2d: Array<OpenGl2DTexture>? = null
    @JvmField var texture3d: OpenGl3DTexture? = null
    @JvmField var aClass137Sub1Array1634: Array<OpenGl2DTexture>? = null
    @JvmField var aClass137Sub41636: OpenGl3DTexture? = null
    @JvmField var hdrTexture3d: OpenGl3DTexture? = null
    @JvmField val texture3dSupported: Boolean

    init {
        texture3dSupported = openglToolkit.texture3dSupported
        method2348(openglToolkit)
        if (!texture3dSupported) {
            textures2d = Array(16) { index ->
                val bytes = DataBuffer.readDataBuffer(anObject1637, index * 32768)!!
                OpenGl2DTexture(
                    openglToolkit, 3553, GLFormat.aClass1501952,
                    GLActionFormat.PIXEL_FORMAT, 128, 128, true, bytes, GLFormat.aClass1501952, false
                )
            }

            aClass137Sub1Array1634 = Array(16) { index ->
                val bytes = DataBuffer.readDataBuffer(anObject1627, index * 32768)!!
                OpenGl2DTexture(
                    openglToolkit, 3553, GLFormat.aClass1501952,
                    GLActionFormat.PIXEL_FORMAT, 128, 128, true, bytes, GLFormat.aClass1501952, false
                )
            }
        } else {
            var buffer = DataBuffer.unpackDataBuffer(anObject1637, false)!!
            texture3d = OpenGl3DTexture(openglToolkit, GLFormat.aClass1501952, GLActionFormat.PIXEL_FORMAT, buffer, GLFormat.aClass1501952)
            buffer = DataBuffer.unpackDataBuffer(anObject1627, false)!!
            aClass137Sub41636 = OpenGl3DTexture(openglToolkit, GLFormat.aClass1501952, GLActionFormat.PIXEL_FORMAT, buffer, GLFormat.aClass1501952)
            val shader = openglToolkit.aClass1648363
            if (shader?.createShader() == true) {
                buffer = DataBuffer.unpackDataBuffer(databufferHdrTexture, false)!!
                hdrTexture3d = OpenGl3DTexture(openglToolkit, GLFormat.HDR, GLActionFormat.PIXEL_FORMAT)
                val texture3dTemp = OpenGl3DTexture(openglToolkit, GLFormat.aClass1501948, GLActionFormat.PIXEL_FORMAT, buffer, GLFormat.aClass1501948)
                if (!shader.activate(hdrTexture3d!!, texture3dTemp)) {
                    hdrTexture3d!!.dispose()
                    hdrTexture3d = null
                } else {
                    hdrTexture3d!!.generateMipmaps()
                }
                texture3dTemp.dispose()
            }
        }
    }

    companion object {
        @JvmField var anObject1637: Any? = null
        @JvmField var anObject1627: Any? = null
        @JvmField var databufferHdrTexture: Any? = null

        @JvmStatic
        fun method2348(openglToolkit: OpenGlToolkit) {
            if (anObject1637 == null) {
                val generator = Class370_Sub1_Sub2()
                val buffer = generator.method15614()
                anObject1637 = DataBuffer.createDataBuffer(buffer)
            }

            if (anObject1627 == null) {
                val generator = Class370_Sub2_Sub2()
                val buffer = generator.getData()
                anObject1627 = DataBuffer.createDataBuffer(buffer)
            }

            val shader = openglToolkit.aClass1648363
            if (shader?.createShader() == true && databufferHdrTexture == null) {
                val buffer = PerlinWaterTextureGenerator.generatePerlinWaterTexture(PerlinNoiseGenerator())
                databufferHdrTexture = DataBuffer.createDataBuffer(buffer)
            }
        }
    }
}
