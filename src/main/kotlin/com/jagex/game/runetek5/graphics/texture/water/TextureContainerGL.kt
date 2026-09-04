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
    @JvmField var billowTextures2d: Array<OpenGl2DTexture>? = null
    @JvmField var billowTexture3d: OpenGl3DTexture? = null
    @JvmField var hdrTexture3d: OpenGl3DTexture? = null
    @JvmField val texture3dSupported: Boolean

    init {
        texture3dSupported = openglToolkit.texture3dSupported
        initializeTextureData(openglToolkit)
        if (!texture3dSupported) {
            textures2d = Array(16) { index ->
                val bytes = DataBuffer.readDataBuffer(waveTextureDataBuffer, index * 32768)!!
                OpenGl2DTexture(
                    openglToolkit, 3553, GLFormat.LUMINANCE_ALPHA,
                    GLActionFormat.PIXEL_FORMAT, 128, 128, true, bytes, GLFormat.LUMINANCE_ALPHA, false
                )
            }

            billowTextures2d = Array(16) { index ->
                val bytes = DataBuffer.readDataBuffer(billowTextureDataBuffer, index * 32768)!!
                OpenGl2DTexture(
                    openglToolkit, 3553, GLFormat.LUMINANCE_ALPHA,
                    GLActionFormat.PIXEL_FORMAT, 128, 128, true, bytes, GLFormat.LUMINANCE_ALPHA, false
                )
            }
        } else {
            var buffer = DataBuffer.unpackDataBuffer(waveTextureDataBuffer, false)!!
            texture3d = OpenGl3DTexture(openglToolkit, GLFormat.LUMINANCE_ALPHA, GLActionFormat.PIXEL_FORMAT, buffer, GLFormat.LUMINANCE_ALPHA)
            buffer = DataBuffer.unpackDataBuffer(billowTextureDataBuffer, false)!!
            billowTexture3d = OpenGl3DTexture(openglToolkit, GLFormat.LUMINANCE_ALPHA, GLActionFormat.PIXEL_FORMAT, buffer, GLFormat.LUMINANCE_ALPHA)
            val shader = openglToolkit.texture3dShader
            if (shader?.createShader() == true) {
                buffer = DataBuffer.unpackDataBuffer(databufferHdrTexture, false)!!
                hdrTexture3d = OpenGl3DTexture(openglToolkit, GLFormat.HDR, GLActionFormat.PIXEL_FORMAT)
                val texture3dTemp = OpenGl3DTexture(openglToolkit, GLFormat.LUMINANCE, GLActionFormat.PIXEL_FORMAT, buffer, GLFormat.LUMINANCE)
                if (!shader.activate(hdrTexture3d!!, texture3dTemp, buffer)) {
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
        @JvmField var waveTextureDataBuffer: Any? = null
        @JvmField var billowTextureDataBuffer: Any? = null
        @JvmField var databufferHdrTexture: Any? = null

        @JvmStatic
        fun initializeTextureData(openglToolkit: OpenGlToolkit) {
            if (waveTextureDataBuffer == null) {
                val generator = GLWaveTextureData()
                val buffer = generator.getData()
                waveTextureDataBuffer = DataBuffer.createDataBuffer(buffer)
            }

            if (billowTextureDataBuffer == null) {
                val generator = GLBillowTextureData()
                val buffer = generator.getData()
                billowTextureDataBuffer = DataBuffer.createDataBuffer(buffer)
            }

            val shader = openglToolkit.texture3dShader
            if (shader?.createShader() == true && databufferHdrTexture == null) {
                val buffer = PerlinWaterTextureGenerator.generatePerlinWaterTexture(PerlinNoiseGenerator())
                databufferHdrTexture = DataBuffer.createDataBuffer(buffer)
            }
        }
    }
}
