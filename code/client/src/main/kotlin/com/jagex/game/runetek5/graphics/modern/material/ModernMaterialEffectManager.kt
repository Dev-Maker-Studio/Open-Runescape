package com.jagex.game.runetek5.graphics.modern.material

import com.jagex.game.runetek5.graphics.modern.core.ShaderProgram
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper

/**
 * Modern material effect manager that maps legacy material effect IDs
 * to GLSL 330 core shader programs.
 *
 * This replaces the legacy EffectManagerGL and its MaterialEffectGL subclasses,
 * which used ARB vertex/fragment programs, display lists, fixed-function texture
 * combiners (glTexEnvi), texture generation (glTexGeni), and matrix stack operations.
 *
 * Effect index mapping:
 *   0 = None (standard model/ground shader)
 *   1 = Cube map specular highlights (CubeMapSpecularEffect)
 *   2 = Lapping water with noise perturbation (LappingWaterEffect)
 *   3 = Still water with edge blending (StillWaterEffect)
 *   4 = Rippling white water waves (RipplingWaterEffect)
 *   5 = Waterfall flowing texture (WaterfallEffect)
 *   6 = Hide lights / disable lighting (HideLightsEffect)
 *   7 = Environment map reflections (ReflectingTextureEffect)
 *   8 = HD reflective rippling water (ReflectiveRipplingWaterEffect)
 *   9 = HD water with wave breaking (BreakingWaterEffect)
 *
 * Usage in the modern pipeline:
 *   1. Call initialize() once after GL context is available
 *   2. Before rendering geometry with a material effect, call getShaderForEffect()
 *      to get the appropriate ShaderProgram
 *   3. Bind the shader and set effect-specific uniforms via setEffectUniforms()
 *   4. After rendering, restore the default shader
 */
class ModernMaterialEffectManager {

    // Shader programs for each effect type
    private var cubeSpecShader: ShaderProgram? = null        // Effect 1
    private var lappingWaterShader: ShaderProgram? = null     // Effect 2
    private var stillWaterShader: ShaderProgram? = null       // Effect 3
    private var ripplingWaterShader: ShaderProgram? = null    // Effect 4
    private var waterfallShader: ShaderProgram? = null        // Effect 5
    private var reflectingShader: ShaderProgram? = null       // Effect 7
    private var hdWaterShader: ShaderProgram? = null          // Effects 8 & 9

    // Default shader (model shader, used when no effect or effect 6)
    private var defaultModelShader: ShaderProgram? = null

    // Currently active effect
    private var activeEffect: EffectType = EffectType.NONE
    private var activeShader: ShaderProgram? = null

    // State tracking
    private var initialized = false
    private var hideLightsActive = false

    /**
     * Initializes all material effect shaders.
     * Must be called on the GL thread after context is available.
     */
    fun initialize() {
        if (initialized) return

        try {
            cubeSpecShader = ShaderProgram.createFromResources(
                "/shaders/materialCubeSpec.vert",
                "/shaders/materialCubeSpec.frag"
            )
            println("[ModernMaterialEffects] Cube map specular shader compiled")
        } catch (e: Exception) {
            println("[ModernMaterialEffects] Failed to compile cube spec shader: ${e.message}")
        }

        try {
            lappingWaterShader = ShaderProgram.createFromResources(
                "/shaders/waterLapping.vert",
                "/shaders/waterLapping.frag"
            )
            println("[ModernMaterialEffects] Lapping water shader compiled")
        } catch (e: Exception) {
            println("[ModernMaterialEffects] Failed to compile lapping water shader: ${e.message}")
        }

        try {
            stillWaterShader = ShaderProgram.createFromResources(
                "/shaders/water.vert",
                "/shaders/water.frag"
            )
            println("[ModernMaterialEffects] Still water shader compiled")
        } catch (e: Exception) {
            println("[ModernMaterialEffects] Failed to compile still water shader: ${e.message}")
        }

        try {
            ripplingWaterShader = ShaderProgram.createFromResources(
                "/shaders/waterRippling.vert",
                "/shaders/waterRippling.frag"
            )
            println("[ModernMaterialEffects] Rippling water shader compiled")
        } catch (e: Exception) {
            println("[ModernMaterialEffects] Failed to compile rippling water shader: ${e.message}")
        }

        try {
            waterfallShader = ShaderProgram.createFromResources(
                "/shaders/waterfall.vert",
                "/shaders/waterfall.frag"
            )
            println("[ModernMaterialEffects] Waterfall shader compiled")
        } catch (e: Exception) {
            println("[ModernMaterialEffects] Failed to compile waterfall shader: ${e.message}")
        }

        try {
            reflectingShader = ShaderProgram.createFromResources(
                "/shaders/waterReflecting.vert",
                "/shaders/waterReflecting.frag"
            )
            println("[ModernMaterialEffects] Reflecting texture shader compiled")
        } catch (e: Exception) {
            println("[ModernMaterialEffects] Failed to compile reflecting shader: ${e.message}")
        }

        try {
            hdWaterShader = ShaderProgram.createFromResources(
                "/shaders/waterHD.vert",
                "/shaders/waterHD.frag"
            )
            println("[ModernMaterialEffects] HD water shader compiled")
        } catch (e: Exception) {
            println("[ModernMaterialEffects] Failed to compile HD water shader: ${e.message}")
        }

        initialized = true
        println("[ModernMaterialEffects] Initialized ${getCompiledShaderCount()} / 7 effect shaders")
    }

    /**
     * Gets the shader program for a given effect type.
     * Returns null if the effect should use the default model shader,
     * or if the shader failed to compile.
     */
    fun getShaderForEffect(effectType: EffectType): ShaderProgram? {
        return when (effectType) {
            EffectType.NONE -> null  // Use default model shader
            EffectType.CUBE_MAP_SPECULAR -> cubeSpecShader
            EffectType.LAPPING_WATER -> lappingWaterShader
            EffectType.STILL_WATER -> stillWaterShader
            EffectType.RIPPLING_WATER -> ripplingWaterShader
            EffectType.WATERFALL -> waterfallShader
            EffectType.HIDE_LIGHTS -> null  // Uses default shader with lighting disabled
            EffectType.REFLECTION_MAP -> reflectingShader
            EffectType.REFLECTIVE_RIPPLING_WATER -> hdWaterShader
            EffectType.HD_WATER -> hdWaterShader  // Same shader as effect 8
        }
    }

    /**
     * Activates a material effect. This selects the appropriate shader
     * and updates the hideLights flag.
     *
     * @param effectType The effect to activate
     * @param hasLight Whether the geometry has lighting enabled
     * @return The ShaderProgram to use, or null for the default model shader
     */
    fun activateEffect(effectType: EffectType, hasLight: Boolean): ShaderProgram? {
        activeEffect = effectType

        // Handle the hide-lights effect (just a flag, no shader change)
        if (effectType == EffectType.HIDE_LIGHTS) {
            hideLightsActive = true
            return null  // Use default shader with lighting disabled
        } else {
            hideLightsActive = false
        }

        activeShader = getShaderForEffect(effectType)
        return activeShader
    }

    /**
     * Deactivates the current material effect.
     */
    fun deactivateEffect() {
        activeShader?.unbind()
        activeEffect = EffectType.NONE
        activeShader = null
        hideLightsActive = false
    }

    /**
     * Whether the hide-lights effect is currently active.
     * When true, the rendering pipeline should disable lighting calculations.
     */
    fun isHideLightsActive(): Boolean = hideLightsActive

    /**
     * Gets the currently active effect type.
     */
    fun getActiveEffect(): EffectType = activeEffect

    /**
     * Whether the given effect type is a water effect.
     * Water effects share common fog/plane calculation patterns.
     */
    fun isWaterEffect(effectType: EffectType): Boolean {
        return effectType == EffectType.STILL_WATER ||
               effectType == EffectType.LAPPING_WATER ||
               effectType == EffectType.RIPPLING_WATER ||
               effectType == EffectType.WATERFALL ||
               effectType == EffectType.REFLECTIVE_RIPPLING_WATER ||
               effectType == EffectType.HD_WATER
    }

    /**
     * Whether the given effect type requires a cube map texture.
     */
    fun requiresCubeMap(effectType: EffectType): Boolean {
        return effectType == EffectType.CUBE_MAP_SPECULAR ||
               effectType == EffectType.REFLECTION_MAP ||
               effectType == EffectType.REFLECTIVE_RIPPLING_WATER ||
               effectType == EffectType.HD_WATER
    }

    /**
     * Whether the given effect type requires a 3D (volume) texture.
     */
    fun requires3DTexture(effectType: EffectType): Boolean {
        return effectType == EffectType.LAPPING_WATER ||
               effectType == EffectType.RIPPLING_WATER ||
               effectType == EffectType.WATERFALL ||
               effectType == EffectType.REFLECTIVE_RIPPLING_WATER ||
               effectType == EffectType.HD_WATER
    }

    /**
     * Gets the number of successfully compiled effect shaders.
     */
    fun getCompiledShaderCount(): Int {
        var count = 0
        if (cubeSpecShader != null) count++
        if (lappingWaterShader != null) count++
        if (stillWaterShader != null) count++
        if (ripplingWaterShader != null) count++
        if (waterfallShader != null) count++
        if (reflectingShader != null) count++
        if (hdWaterShader != null) count++
        return count
    }

    /**
     * Gets a debug status string listing all shader compilation results.
     */
    fun getDebugStatus(): String {
        val sb = StringBuilder()
        sb.appendLine("ModernMaterialEffectManager:")
        sb.appendLine("  initialized: $initialized")
        sb.appendLine("  activeEffect: $activeEffect")
        sb.appendLine("  hideLightsActive: $hideLightsActive")
        sb.appendLine("  Shaders:")
        sb.appendLine("    cubeMapSpecular (1): ${if (cubeSpecShader != null) "OK" else "FAILED"}")
        sb.appendLine("    lappingWater (2): ${if (lappingWaterShader != null) "OK" else "FAILED"}")
        sb.appendLine("    stillWater (3): ${if (stillWaterShader != null) "OK" else "FAILED"}")
        sb.appendLine("    ripplingWater (4): ${if (ripplingWaterShader != null) "OK" else "FAILED"}")
        sb.appendLine("    waterfall (5): ${if (waterfallShader != null) "OK" else "FAILED"}")
        sb.appendLine("    reflecting (7): ${if (reflectingShader != null) "OK" else "FAILED"}")
        sb.appendLine("    hdWater (8/9): ${if (hdWaterShader != null) "OK" else "FAILED"}")
        return sb.toString()
    }

    /**
     * Releases all GPU resources.
     */
    fun destroy() {
        cubeSpecShader?.destroy()
        lappingWaterShader?.destroy()
        stillWaterShader?.destroy()
        ripplingWaterShader?.destroy()
        waterfallShader?.destroy()
        reflectingShader?.destroy()
        hdWaterShader?.destroy()

        cubeSpecShader = null
        lappingWaterShader = null
        stillWaterShader = null
        ripplingWaterShader = null
        waterfallShader = null
        reflectingShader = null
        hdWaterShader = null

        initialized = false
        activeEffect = EffectType.NONE
        activeShader = null
        hideLightsActive = false
    }
}
