package com.jagex.game.runetek5.graphics.shader.program.impl

import com.jagex.game.runetek5.graphics.shader.MappedShaderCreationException
import com.jagex.game.runetek5.graphics.shader.Shader
import com.jagex.game.runetek5.graphics.shader.ShaderAttribute
import com.jagex.game.runetek5.graphics.shader.ShaderAttributeLoadingException
import com.jagex.game.runetek5.graphics.shader.ShaderMapping
import com.jagex.game.runetek5.graphics.shader.program.AbstractBloomShader
import com.jagex.game.runetek5.graphics.texture.sampler.NativeTextureSampler
import com.jagex.game.runetek5.imageproducer.DirectDisplay
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit

class NativeFilterBloomShader(
    nativeToolkit: AbstractNativeToolkit
) : AbstractBloomShader(nativeToolkit) {

    @JvmField var filterBloomShader: Shader? = null
    @JvmField var sceneTex: ShaderAttribute? = null
    @JvmField var bloomTex1: ShaderAttribute? = null
    @JvmField var params: ShaderAttribute? = null
    @JvmField var sampleSize: ShaderAttribute? = null
    @JvmField var pixelOffsetAndBloomScale: ShaderAttribute? = null
    @JvmField var posAndTexCoords: ShaderAttribute? = null
    @JvmField var techFullscreenTri: ShaderMapping? = null
    @JvmField var brightpass: ShaderMapping? = null
    @JvmField var blur: ShaderMapping? = null
    @JvmField var composite: ShaderMapping? = null
    @JvmField var techDefault: ShaderMapping? = null
    @JvmField var loaded: Boolean = false

    override fun reset() {
        filterBloomShader!!.reset()
    }

    override fun isLoaded(): Boolean {
        return loaded
    }

    override fun onRendererInitialized() {
    }

    override fun destroy() {
    }

    override fun getIterations(): Int {
        return 4
    }

    override fun initialize(
        type: Int,
        display: DirectDisplay,
        compositeSampler: NativeTextureSampler,
        sceneSampler: NativeTextureSampler
    ) {
        val pixelOffset = nativeToolkit.getPixelOffset()
        val displayWidth = display.getWidth().toFloat()
        val displayHeight = display.getHeight().toFloat()
        var sampler = compositeSampler
        val posAndTexCoordData = floatArrayOf(-1.0f, 1.0f, 0.0f, 0.0f, -1.0f, -3.0f, 0.0f, 2.0f, 3.0f, 1.0f, 2.0f, 0.0f)
        var effectWidth = displayWidth.toInt()
        var effectHeight = displayHeight.toInt()
        var srcWidth = effectWidth
        var srcHeight = effectHeight

        when (type) {
            0 -> {
                effectWidth = 256
                effectHeight = 256
                filterBloomShader!!.loadProgram(brightpass!!)
                filterBloomShader!!.setActiveMapping()
            }
            1 -> {
                effectWidth = 256
                effectHeight = 256
                srcWidth = effectWidth
                srcHeight = effectHeight
                filterBloomShader!!.loadProgram(blur!!)
                filterBloomShader!!.setActiveMapping()
                filterBloomShader!!.unform2f(sampleSize!!, baseBlur / displayWidth, 0.0f)
            }
            2 -> {
                effectWidth = 256
                effectHeight = 256
                srcWidth = effectWidth
                srcHeight = effectHeight
                filterBloomShader!!.loadProgram(blur!!)
                filterBloomShader!!.setActiveMapping()
                filterBloomShader!!.unform2f(sampleSize!!, 0.0f, baseBlur / displayHeight)
            }
            3 -> {
                filterBloomShader!!.loadProgram(composite!!)
                sampler = sceneSampler
                filterBloomShader!!.loadTextureSampler(bloomTex1!!, 1, compositeSampler)
                filterBloomShader!!.setActiveMapping()
            }
        }

        val posScaleX = effectWidth / displayWidth
        val posScaleY = effectHeight / displayHeight
        val texScaleU = srcWidth / displayWidth
        val texScaleV = srcHeight / displayHeight
        posAndTexCoordData[8] = (posAndTexCoordData[8] + 1.0f) * posScaleX - 1.0f
        posAndTexCoordData[5] = (posAndTexCoordData[5] - 1.0f) * posScaleY + 1.0f
        posAndTexCoordData[10] *= texScaleU
        posAndTexCoordData[7] *= texScaleV
        filterBloomShader!!.uploadUniform4fv(posAndTexCoords!!, posAndTexCoordData)
        filterBloomShader!!.loadTextureSampler(sceneTex!!, 0, sampler)
        filterBloomShader!!.loadRgbaColorAttribute(params!!, red, green, blue, 0.0f)
        filterBloomShader!!.loadRgbaColorAttribute(
            pixelOffsetAndBloomScale!!,
            pixelOffset / displayWidth,
            pixelOffset / displayHeight,
            256.0f / displayWidth,
            256.0f / displayHeight
        )
        nativeToolkit.setDrawableArea(0, 0, effectWidth, effectHeight)
        nativeToolkit.setClipping(0, 0, effectWidth, effectHeight)
    }

    override fun getFormatType(): Int {
        return 1
    }

    fun isShaderSupported(): Boolean {
        return nativeToolkit.GLEXTFramebufferObject && nativeToolkit.supportsArbFragmentShader()
    }

    override fun loadShader(): Boolean {
        if (isShaderSupported()) {
            filterBloomShader = nativeToolkit.loadShader("FilterBloom")
            if (filterBloomShader == null) {
                return false
            } else {
                try {
                    sceneTex = filterBloomShader!!.getAttribute("sceneTex")
                    bloomTex1 = filterBloomShader!!.getAttribute("bloomTex1")
                    params = filterBloomShader!!.getAttribute("params")
                    sampleSize = filterBloomShader!!.getAttribute("sampleSize")
                    pixelOffsetAndBloomScale = filterBloomShader!!.getAttribute("pixelOffsetAndBloomScale")
                    posAndTexCoords = filterBloomShader!!.getAttribute("PosAndTexCoords")
                    filterBloomShader!!.getShaderMapping("test")
                    techFullscreenTri = filterBloomShader!!.getShaderMapping("techFullscreenTri")
                    brightpass = filterBloomShader!!.getShaderMapping("brightpass")
                    blur = filterBloomShader!!.getShaderMapping("blur")
                    composite = filterBloomShader!!.getShaderMapping("composite")
                    techDefault = filterBloomShader!!.getShaderMapping("techDefault")
                } catch (e: ShaderAttributeLoadingException) {
                    return false
                } catch (e: MappedShaderCreationException) {
                    return false
                }

                if (!techFullscreenTri!!.createShaders()) {
                    return false
                } else if (!brightpass!!.createShaders()) {
                    return false
                } else if (!blur!!.createShaders()) {
                    return false
                } else if (!composite!!.createShaders()) {
                    return false
                } else if (!techDefault!!.createShaders()) {
                    return false
                } else {
                    loaded = true
                    return true
                }
            }
        } else {
            return false
        }
    }

    companion object {
        @JvmField var green: Float = 0.25f
        @JvmField var blue: Float = 1.0f
        @JvmField var red: Float = 1.0f
        @JvmField var baseBlur: Float = 1.0f
    }
}
