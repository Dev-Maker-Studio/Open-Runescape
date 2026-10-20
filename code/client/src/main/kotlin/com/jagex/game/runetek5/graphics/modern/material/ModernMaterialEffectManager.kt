package com.jagex.game.runetek5.graphics.modern.material

import com.jagex.game.runetek5.graphics.modern.core.ShaderProgram
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper

class ModernMaterialEffectManager {

    private var cubeSpecShader: ShaderProgram? = null
    private var lappingWaterShader: ShaderProgram? = null
    private var stillWaterShader: ShaderProgram? = null
    private var ripplingWaterShader: ShaderProgram? = null
    private var waterfallShader: ShaderProgram? = null
    private var reflectingShader: ShaderProgram? = null
    private var hdWaterShader: ShaderProgram? = null

    private var defaultModelShader: ShaderProgram? = null

    private var activeEffect: EffectType = EffectType.NONE
    private var activeShader: ShaderProgram? = null

    private var initialized = false
    private var hideLightsActive = false

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

    fun getShaderForEffect(effectType: EffectType): ShaderProgram? {
        return when (effectType) {
            EffectType.NONE -> null
            EffectType.CUBE_MAP_SPECULAR -> cubeSpecShader
            EffectType.LAPPING_WATER -> lappingWaterShader
            EffectType.STILL_WATER -> stillWaterShader
            EffectType.RIPPLING_WATER -> ripplingWaterShader
            EffectType.WATERFALL -> waterfallShader
            EffectType.HIDE_LIGHTS -> null
            EffectType.REFLECTION_MAP -> reflectingShader
            EffectType.REFLECTIVE_RIPPLING_WATER -> hdWaterShader
            EffectType.HD_WATER -> hdWaterShader
        }
    }

    fun activateEffect(effectType: EffectType, hasLight: Boolean): ShaderProgram? {
        activeEffect = effectType

        if (effectType == EffectType.HIDE_LIGHTS) {
            hideLightsActive = true
            return null
        } else {
            hideLightsActive = false
        }

        activeShader = getShaderForEffect(effectType)
        return activeShader
    }

    fun deactivateEffect() {
        activeShader?.unbind()
        activeEffect = EffectType.NONE
        activeShader = null
        hideLightsActive = false
    }

    fun isHideLightsActive(): Boolean = hideLightsActive

    fun getActiveEffect(): EffectType = activeEffect

    fun isWaterEffect(effectType: EffectType): Boolean {
        return effectType == EffectType.STILL_WATER ||
               effectType == EffectType.LAPPING_WATER ||
               effectType == EffectType.RIPPLING_WATER ||
               effectType == EffectType.WATERFALL ||
               effectType == EffectType.REFLECTIVE_RIPPLING_WATER ||
               effectType == EffectType.HD_WATER
    }

    fun requiresCubeMap(effectType: EffectType): Boolean {
        return effectType == EffectType.CUBE_MAP_SPECULAR ||
               effectType == EffectType.REFLECTION_MAP ||
               effectType == EffectType.REFLECTIVE_RIPPLING_WATER ||
               effectType == EffectType.HD_WATER
    }

    fun requires3DTexture(effectType: EffectType): Boolean {
        return effectType == EffectType.LAPPING_WATER ||
               effectType == EffectType.RIPPLING_WATER ||
               effectType == EffectType.WATERFALL ||
               effectType == EffectType.REFLECTIVE_RIPPLING_WATER ||
               effectType == EffectType.HD_WATER
    }

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
