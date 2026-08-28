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

/**
 * Bridges the legacy RuneTek5 material system with modern shader uniforms.
 *
 * Converts MaterialType properties into shader-compatible formats and
 * manages texture binding for the modern pipeline.
 */
class MaterialAdapter(
    private val materialProvider: MaterialProvider?,
    private val textureManager: TextureManager,
    private val renderState: RenderState
) {
    // Material texture cache (legacy material ID -> modern texture handle)
    private val materialTextureCache = mutableMapOf<Int, Int>()

    // Currently bound material
    private var currentMaterialId = -1
    private var currentMaterial: MaterialType? = null

    // Cached uniform data to avoid repeated conversions
    private var cachedUniforms = MaterialUniforms()

    /**
     * Binds a material by its ID and returns the uniform data for shaders.
     *
     * @param materialId The legacy material ID
     * @return MaterialUniforms containing shader-ready values
     */
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

        // Convert material properties to shader uniforms
        cachedUniforms = convertToUniforms(material, materialId)

        // Apply render state changes
        applyRenderState(cachedUniforms)

        // Bind texture if available
        val textureHandle = getOrCreateTexture(materialId)
        if (textureHandle > 0) {
            textureManager.bind(textureHandle, 0)
        }

        return cachedUniforms
    }

    /**
     * Converts a MaterialType to shader-compatible uniform values.
     */
    private fun convertToUniforms(material: MaterialType, materialId: Int): MaterialUniforms {
        // Convert HSL color to RGB
        val hsl = material.colorHsl.toInt() and 0xFFFF
        val rgb = hslToRgb(hsl)

        // Calculate alpha (0-255 range, byte can be negative in Java/Kotlin)
        val alpha = (material.alpha.toInt() and 0xFF) / 255f

        // Calculate brightness modifier
        val brightness = (material.brightness.toInt() and 0xFF) / 128f

        // Determine blend mode from alpha and material flags
        val blendMode = when {
            material.alpha < 0 || alpha < 1.0f -> BlendMode.ALPHA
            material.hdr -> BlendMode.ADDITIVE
            else -> BlendMode.NONE
        }

        // Determine if material should skip rendering
        val shouldRender = !material.skipTriangles && !material.detailsOnly

        // Calculate texture scroll speeds (convert from byte to normalized)
        val scrollU = material.textureSpeedU.toFloat() / 64f
        val scrollV = material.textureSpeedV.toFloat() / 64f

        // Determine effect type
        val effectType = EffectType.fromId(material.effectId.toInt())

        return MaterialUniforms(
            colorR = ((rgb shr 16) and 0xFF) / 255f,
            colorG = ((rgb shr 8) and 0xFF) / 255f,
            colorB = (rgb and 0xFF) / 255f,
            alpha = if (alpha == 0f) 1f else alpha,  // Default alpha to 1 if 0
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

    /**
     * Applies render state based on material uniforms.
     */
    private fun applyRenderState(uniforms: MaterialUniforms) {
        // Set blend mode
        renderState.setBlendMode(uniforms.blendMode)

        // For transparent materials, disable depth writing but keep testing
        if (uniforms.blendMode != BlendMode.NONE) {
            renderState.setDepthWrite(false)
        } else {
            renderState.setDepthWrite(true)
        }
    }

    /**
     * Gets or creates a texture for a material.
     */
    private fun getOrCreateTexture(materialId: Int): Int {
        // Check cache first
        materialTextureCache[materialId]?.let { return it }

        // Try to get texture from material provider
        if (materialProvider == null) return 0

        try {
            // Get ARGB pixel data from material provider
            val width = 128  // Standard texture size
            val height = 128
            val pixels = materialProvider.getPixelsArgb(materialId, width, height)

            if (pixels.isEmpty()) return 0

            // Convert ARGB int array to RGBA byte array for modern pipeline
            val rgbaBytes = ByteArray(width * height * 4)
            for (i in pixels.indices) {
                val argb = pixels[i]
                val idx = i * 4
                rgbaBytes[idx] = ((argb shr 16) and 0xFF).toByte()  // R
                rgbaBytes[idx + 1] = ((argb shr 8) and 0xFF).toByte()   // G
                rgbaBytes[idx + 2] = (argb and 0xFF).toByte()           // B
                rgbaBytes[idx + 3] = ((argb shr 24) and 0xFF).toByte()  // A
            }

            val material = materialProvider.getMaterial(materialId)

            // Create texture with appropriate options
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

    /**
     * Checks if a material has an associated texture.
     */
    private fun hasTexture(materialId: Int): Boolean {
        if (materialId < 0 || materialProvider == null) return false
        return materialProvider.isCached(materialId)
    }

    /**
     * Converts HSL color (RuneScape format) to RGB.
     *
     * RuneScape HSL format:
     * - Bits 10-15: Hue (0-63)
     * - Bits 3-9: Saturation (0-7)
     * - Bits 0-2: Lightness (0-7)
     */
    private fun hslToRgb(hsl: Int): Int {
        if (hsl == 0) return 0xFFFFFF  // White for default

        val hue = ((hsl shr 10) and 0x3F) / 63f
        val saturation = ((hsl shr 3) and 0x7) / 7f
        val lightness = (hsl and 0x7) / 7f

        return hslToRgbFloat(hue, saturation, lightness)
    }

    /**
     * Converts HSL floats (0-1 range) to packed RGB int.
     */
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

    /**
     * Unbinds the current material.
     */
    fun unbind() {
        currentMaterialId = -1
        currentMaterial = null
        textureManager.bind(0, 0)
    }

    /**
     * Clears the texture cache and releases resources.
     */
    fun clearCache() {
        for ((_, handle) in materialTextureCache) {
            textureManager.deleteTexture(handle)
        }
        materialTextureCache.clear()
        currentMaterialId = -1
        currentMaterial = null
    }

    /**
     * Gets the number of cached material textures.
     */
    fun getCacheSize(): Int = materialTextureCache.size

    /**
     * Registers an externally created texture for a material ID.
     * Useful for integrating with legacy texture loading code.
     */
    fun registerExternalTexture(materialId: Int, glTextureId: Int, width: Int, height: Int) {
        val handle = textureManager.registerExistingTexture(glTextureId, width, height, "material_$materialId")
        materialTextureCache[materialId] = handle
    }
}

/**
 * Shader-ready uniform values converted from MaterialType.
 */
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

/**
 * Material effect types matching RuneScape's effect system.
 *
 * These IDs correspond to the indices in EffectManagerGL.materialEffects[]:
 *   0 = none
 *   1 = CubeMapSpecularEffect    (cube map specular highlights via normal maps)
 *   2 = LappingWaterEffect   (animated lapping water with Perlin noise)
 *   3 = StillWaterEffect     (still water with edge blending)
 *   4 = RipplingWaterEffect  (white rippling waves)
 *   5 = WaterfallEffect      (flowing waterfall texture)
 *   6 = HideLightsEffect     (disables lighting for bright surfaces)
 *   7 = ReflectingTextureEffect (environment map reflections)
 *   8 = ReflectiveRipplingWaterEffect (HD reflective water)
 *   9 = BreakingWaterEffect    (HD water with wave breaking and specular)
 */
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

/**
 * Texture combine modes for multi-texturing.
 *
 * These map to the legacy GL_COMBINE texture environment modes:
 *   GL_MODULATE    = 8448  -> base * texture
 *   GL_REPLACE     = 7681  -> replace base with texture
 *   GL_ADD         = 260   -> base + texture
 *   GL_ADD_SIGNED  = 34164 -> base + texture - 0.5
 *   GL_INTERPOLATE = 34165 -> mix(base, texture, factor)
 *   GL_DECAL       = 8449  -> texture over base using texture alpha
 *
 * The combineMode byte from MaterialType is an engine-specific index (0..3),
 * not a raw GL constant. See OpenGlToolkit.loadTextureCombineMode() for mapping.
 */
enum class CombineMode(val id: Int) {
    MODULATE(0),      // Base * texture (default)
    REPLACE(1),       // Replace base with texture
    ADD(2),           // Base + texture
    INTERPOLATE(3);   // mix(base, texture, factor)

    companion object {
        private val byId = entries.associateBy { it.id }
        fun fromId(id: Int): CombineMode = byId[id] ?: MODULATE

        /**
         * Converts a combine mode to the fragment shader operation.
         * Returns the GLSL expression string for documentation purposes.
         */
        fun describeGlsl(mode: CombineMode): String = when (mode) {
            MODULATE -> "texColor * vertexColor"
            REPLACE -> "texColor"
            ADD -> "texColor + vertexColor"
            INTERPOLATE -> "mix(vertexColor, texColor, factor)"
        }
    }
}
