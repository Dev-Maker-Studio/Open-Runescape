package com.jagex.game.runetek5.graphics.modern.material

import com.jagex.game.runetek5.config.material.MaterialProvider
import com.jagex.game.runetek5.config.material.MaterialType
import com.jagex.game.runetek5.graphics.modern.state.BlendMode
import com.jagex.game.runetek5.graphics.modern.state.RenderState
import com.jagex.game.runetek5.graphics.modern.texture.FilterMode
import com.jagex.game.runetek5.graphics.modern.texture.TextureManager
import com.jagex.game.runetek5.graphics.modern.texture.TextureOptions
import com.jagex.game.runetek5.graphics.modern.texture.WrapMode
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper

class MaterialAdapter(
    private val materialProvider: MaterialProvider?,
    private val textureManager: TextureManager,
    private val renderState: RenderState
) {
    private val materialTextureCache = mutableMapOf<Int, Int>()

    private var currentMaterialId = -1
    private var currentMaterial: MaterialType? = null

    private var cachedUniforms = MaterialUniforms()

    fun bindMaterial(materialId: Int): MaterialUniforms {
        if (materialId == currentMaterialId && currentMaterial != null) {
            return cachedUniforms
        }

        currentMaterialId = materialId

        if (materialId < 0 || materialProvider == null) {
            currentMaterial = null
            cachedUniforms = MaterialUniforms.DEFAULT
            applyRenderState(cachedUniforms)
            return cachedUniforms
        }

        val material = materialProvider.getMaterial(materialId)
        currentMaterial = material

        cachedUniforms = convertToUniforms(material, materialId)

        applyRenderState(cachedUniforms)

        val textureHandle = getOrCreateTexture(materialId)
        if (textureHandle > 0) {
            textureManager.bind(textureHandle, 0)
        }

        return cachedUniforms
    }

    private fun convertToUniforms(material: MaterialType, materialId: Int): MaterialUniforms {
        val hsl = material.colorHsl.toInt() and 0xFFFF
        val rgb = hslToRgb(hsl)

        val alpha = (material.alpha.toInt() and 0xFF) / 255f

        val brightness = (material.brightness.toInt() and 0xFF) / 128f

        val blendMode = when {
            material.alpha < 0 || alpha < 1.0f -> BlendMode.ALPHA
            material.hdr -> BlendMode.ADDITIVE
            else -> BlendMode.NONE
        }

        val shouldRender = !material.skipTriangles && !material.detailsOnly

        val scrollU = material.textureSpeedU.toFloat() / 64f
        val scrollV = material.textureSpeedV.toFloat() / 64f

        val effectType = EffectType.fromId(material.effectId.toInt())

        return MaterialUniforms(
            colorR = ((rgb shr 16) and 0xFF) / 255f,
            colorG = ((rgb shr 8) and 0xFF) / 255f,
            colorB = (rgb and 0xFF) / 255f,
            alpha = if (alpha == 0f) 1f else alpha,
            brightness = brightness,
            blendMode = blendMode,
            hasTexture = hasTexture(materialId),
            textureScrollU = scrollU,
            textureScrollV = scrollV,
            repeatS = material.repeatS,
            repeatT = material.repeatT,
            effectType = effectType,
            effectParam1 = material.effectParam1.toInt(),
            effectParam2 = material.effectParam2,
            combineMode = CombineMode.fromId(material.combineMode),
            useMipmaps = material.mipmapping > 0,
            isHdr = material.hdr,
            shouldRender = shouldRender
        )
    }

    private fun applyRenderState(uniforms: MaterialUniforms) {
        renderState.setBlendMode(uniforms.blendMode)

        if (uniforms.blendMode != BlendMode.NONE) {
            renderState.setDepthWrite(false)
        } else {
            renderState.setDepthWrite(true)
        }
    }

    private fun getOrCreateTexture(materialId: Int): Int {
        materialTextureCache[materialId]?.let { return it }

        if (materialProvider == null) return 0

        try {
            val width = 128
            val height = 128
            val pixels = materialProvider.getPixelsArgb(materialId, width, height)

            if (pixels.isEmpty()) return 0

            val rgbaBytes = ByteArray(width * height * 4)
            for (i in pixels.indices) {
                val argb = pixels[i]
                val idx = i * 4
                rgbaBytes[idx] = ((argb shr 16) and 0xFF).toByte()
                rgbaBytes[idx + 1] = ((argb shr 8) and 0xFF).toByte()
                rgbaBytes[idx + 2] = (argb and 0xFF).toByte()
                rgbaBytes[idx + 3] = ((argb shr 24) and 0xFF).toByte()
            }

            val material = materialProvider.getMaterial(materialId)

            val options = TextureOptions(
                filtering = if (material.mipmapping > 0) FilterMode.LINEAR else FilterMode.NEAREST,
                wrapMode = if (material.repeatS || material.repeatT) WrapMode.REPEAT else WrapMode.CLAMP,
                generateMipmaps = material.mipmapping > 0
            )

            val handle = textureManager.createTexture(width, height, rgbaBytes, options)
            materialTextureCache[materialId] = handle
            return handle

        } catch (e: Exception) {
            println("[MaterialAdapter] Failed to create texture for material $materialId: ${e.message}")
            return 0
        }
    }

    private fun hasTexture(materialId: Int): Boolean {
        if (materialId < 0 || materialProvider == null) return false
        return materialProvider.isCached(materialId)
    }

    private fun hslToRgb(hsl: Int): Int {
        if (hsl == 0) return 0xFFFFFF

        val hue = ((hsl shr 10) and 0x3F) / 63f
        val saturation = ((hsl shr 3) and 0x7) / 7f
        val lightness = (hsl and 0x7) / 7f

        return hslToRgbFloat(hue, saturation, lightness)
    }

    private fun hslToRgbFloat(h: Float, s: Float, l: Float): Int {
        val c = (1 - kotlin.math.abs(2 * l - 1)) * s
        val x = c * (1 - kotlin.math.abs((h * 6) % 2 - 1))
        val m = l - c / 2

        val (r1, g1, b1) = when ((h * 6).toInt()) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        val r = ((r1 + m) * 255).toInt().coerceIn(0, 255)
        val g = ((g1 + m) * 255).toInt().coerceIn(0, 255)
        val b = ((b1 + m) * 255).toInt().coerceIn(0, 255)

        return (r shl 16) or (g shl 8) or b
    }

    fun unbind() {
        currentMaterialId = -1
        currentMaterial = null
        textureManager.bind(0, 0)
    }

    fun clearCache() {
        for ((_, handle) in materialTextureCache) {
            textureManager.deleteTexture(handle)
        }
        materialTextureCache.clear()
        currentMaterialId = -1
        currentMaterial = null
    }

    fun getCacheSize(): Int = materialTextureCache.size

    fun registerExternalTexture(materialId: Int, glTextureId: Int, width: Int, height: Int) {
        val handle = textureManager.registerExistingTexture(glTextureId, width, height, "material_$materialId")
        materialTextureCache[materialId] = handle
    }
}

data class MaterialUniforms(
    val colorR: Float = 1f,
    val colorG: Float = 1f,
    val colorB: Float = 1f,
    val alpha: Float = 1f,
    val brightness: Float = 1f,
    val blendMode: BlendMode = BlendMode.NONE,
    val hasTexture: Boolean = false,
    val textureScrollU: Float = 0f,
    val textureScrollV: Float = 0f,
    val repeatS: Boolean = true,
    val repeatT: Boolean = true,
    val effectType: EffectType = EffectType.NONE,
    val effectParam1: Int = 0,
    val effectParam2: Int = 0,
    val combineMode: CombineMode = CombineMode.MODULATE,
    val useMipmaps: Boolean = true,
    val isHdr: Boolean = false,
    val shouldRender: Boolean = true
) {
    companion object {
        val DEFAULT = MaterialUniforms()
    }
}

enum class EffectType(val id: Int) {
    NONE(0),
    CUBE_MAP_SPECULAR(1),
    LAPPING_WATER(2),
    STILL_WATER(3),
    RIPPLING_WATER(4),
    WATERFALL(5),
    HIDE_LIGHTS(6),
    REFLECTION_MAP(7),
    REFLECTIVE_RIPPLING_WATER(8),
    HD_WATER(9);

    companion object {
        private val byId = entries.associateBy { it.id }
        fun fromId(id: Int): EffectType = byId[id] ?: NONE
    }
}

enum class CombineMode(val id: Int) {
    MODULATE(0),
    REPLACE(1),
    ADD(2),
    INTERPOLATE(3);

    companion object {
        private val byId = entries.associateBy { it.id }
        fun fromId(id: Int): CombineMode = byId[id] ?: MODULATE

        fun describeGlsl(mode: CombineMode): String = when (mode) {
            MODULATE -> "texColor * vertexColor"
            REPLACE -> "texColor"
            ADD -> "texColor + vertexColor"
            INTERPOLATE -> "mix(vertexColor, texColor, factor)"
        }
    }
}
