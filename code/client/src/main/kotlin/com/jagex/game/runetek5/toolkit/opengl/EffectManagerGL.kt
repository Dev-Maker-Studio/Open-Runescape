package com.jagex.game.runetek5.toolkit.opengl

import com.jagex.game.runetek5.graphics.texture.OpenGlTexture
import com.jagex.game.runetek5.graphics.texture.water.TextureContainerGL
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.effect.*

class EffectManagerGL(
    @JvmField val openglToolkit: OpenGlToolkit
) {

    @JvmField var param1: Int = 0
    @JvmField var param2: Int = 0
    @JvmField var activeEffect: Int = 0
    @JvmField val textureContainer: TextureContainerGL = TextureContainerGL(openglToolkit)
    @JvmField val materialEffects: Array<MaterialEffectGL?> = arrayOfNulls(16)
    @JvmField val waterEffect: StillWaterEffect

    init {
        materialEffects[1] = CubeMapSpecularEffect(openglToolkit)
        materialEffects[2] = LappingWaterEffect(openglToolkit, textureContainer)
        materialEffects[4] = RipplingWaterEffect(openglToolkit, textureContainer)
        materialEffects[5] = WaterfallEffect(openglToolkit, textureContainer)
        materialEffects[6] = HideLightsEffect(openglToolkit)
        materialEffects[7] = ReflectingTextureEffect(openglToolkit)
        waterEffect = StillWaterEffect(openglToolkit)
        materialEffects[3] = waterEffect
        materialEffects[8] = ReflectiveRipplingWaterEffect(openglToolkit, textureContainer)
        materialEffects[9] = BreakingWaterEffect(openglToolkit, textureContainer)
        if (!materialEffects[8]!!.isInitialized()) {
            materialEffects[8] = materialEffects[4]
        }

        if (!materialEffects[9]!!.isInitialized()) {
            materialEffects[9] = materialEffects[8]
        }
    }

    fun loadEffect(requestedEffect: Int, requestedParam1: Int, requestedParam2: Int, hasLight: Boolean, stillWater: Boolean) {
        var effect = requestedEffect
        var effectParam1 = requestedParam1
        var effectParam2 = requestedParam2
        var useStillWater = stillWater and openglToolkit.stillwaterEffectLoaded()
        if (!useStillWater && (effect == 4 || effect == 8 || effect == 9)) {
            if (effect == 4) {
                effectParam2 = effectParam1
            }

            effect = 2
        }

        if (effect != 0 && hasLight) {
            effect = effect or 0x80000000.toInt()
        }

        if (activeEffect != effect) {
            if (activeEffect != 0) {
                materialEffects[activeEffect and 0x7fffffff]!!.disable()
            }

            if (effect != 0) {
                materialEffects[effect and 0x7fffffff]!!.toggle(hasLight)
                materialEffects[effect and 0x7fffffff]!!.loadCombineMode()
                materialEffects[effect and 0x7fffffff]!!.applyEffectParams(effectParam1, effectParam2)
            }

            activeEffect = effect
            this.param1 = effectParam1
            this.param2 = effectParam2
        } else if (activeEffect != 0) {
            materialEffects[activeEffect and 0x7fffffff]!!.loadCombineMode()
            if (effectParam1 != this.param1 || effectParam2 != this.param2) {
                materialEffects[activeEffect and 0x7fffffff]!!.applyEffectParams(effectParam1, effectParam2)
                this.param1 = effectParam1
                this.param2 = effectParam2
            }
        }
    }

    fun loadCombiner(combiner: OpenGlTexture?, combineMode: Int): Boolean {
        if (activeEffect == 0) {
            return false
        }
        materialEffects[activeEffect and 0x7fffffff]!!.loadCombiner(combiner, combineMode)
        return true
    }

    fun stillwaterEffectLoaded(): Boolean {
        return materialEffects[3]!!.isInitialized()
    }
}
