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

    override fun method1102() {
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
        interface65: NativeTextureSampler
    ) {
        val f_6 = nativeToolkit.method13905()
        val displayWidth = display.getWidth().toFloat()
        val displayHeight = display.getHeight().toFloat()
        var sampler = compositeSampler
        val floats_10 = floatArrayOf(-1.0f, 1.0f, 0.0f, 0.0f, -1.0f, -3.0f, 0.0f, 2.0f, 3.0f, 1.0f, 2.0f, 0.0f)
        var effectWidth = displayWidth.toInt()
        var effectHeight = displayHeight.toInt()
        var i_13 = effectWidth
        var i_14 = effectHeight

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
                i_13 = effectWidth
                i_14 = effectHeight
                filterBloomShader!!.loadProgram(blur!!)
                filterBloomShader!!.setActiveMapping()
                filterBloomShader!!.unform2f(sampleSize!!, baseBlur / displayWidth, 0.0f)
            }
            2 -> {
                effectWidth = 256
                effectHeight = 256
                i_13 = effectWidth
                i_14 = effectHeight
                filterBloomShader!!.loadProgram(blur!!)
                filterBloomShader!!.setActiveMapping()
                filterBloomShader!!.unform2f(sampleSize!!, 0.0f, baseBlur / displayHeight)
            }
            3 -> {
                filterBloomShader!!.loadProgram(composite!!)
                sampler = interface65
                filterBloomShader!!.loadTextureSampler(bloomTex1!!, 1, compositeSampler)
                filterBloomShader!!.setActiveMapping()
            }
        }

        val f_15 = effectWidth / displayWidth
        val f_16 = effectHeight / displayHeight
        val f_17 = i_13 / displayWidth
        val f_18 = i_14 / displayHeight
        floats_10[8] = (floats_10[8] + 1.0f) * f_15 - 1.0f
        floats_10[5] = (floats_10[5] - 1.0f) * f_16 + 1.0f
        floats_10[10] *= f_17
        floats_10[7] *= f_18
        filterBloomShader!!.method1688(posAndTexCoords!!, floats_10)
        filterBloomShader!!.loadTextureSampler(sceneTex!!, 0, sampler)
        filterBloomShader!!.loadRgbaColorAttribute(params!!, red, green, blue, 0.0f)
        filterBloomShader!!.loadRgbaColorAttribute(
            pixelOffsetAndBloomScale!!,
            f_6 / displayWidth,
            f_6 / displayHeight,
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

    override fun method1097(): Boolean {
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
                } catch (exceptionSub2Sub22: ShaderAttributeLoadingException) {
                    return false
                } catch (exceptionSub2Sub13: MappedShaderCreationException) {
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
