package com.jagex.game.runetek5.toolkit.opengl;

import com.jagex.game.runetek5.graphics.texture.OpenGlTexture;
import com.jagex.game.runetek5.graphics.texture.water.TextureContainerGL;
import com.jagex.game.runetek5.toolkit.OpenGlToolkit;
import com.jagex.game.runetek5.toolkit.opengl.effect.LappingWaterEffect;
import com.jagex.game.runetek5.toolkit.opengl.effect.MaterialEffect1GL;
import com.jagex.game.runetek5.toolkit.opengl.effect.RipplingWaterEffect;
import com.jagex.game.runetek5.toolkit.opengl.effect.WaterfallEffect;
import com.jagex.game.runetek5.toolkit.opengl.effect.HideLightsEffect;
import com.jagex.game.runetek5.toolkit.opengl.effect.ReflectingTextureEffect;
import com.jagex.game.runetek5.toolkit.opengl.effect.ReflectiveRipplingWaterEffect;
import com.jagex.game.runetek5.toolkit.opengl.effect.MaterialEffect9GL;
import com.jagex.game.runetek5.toolkit.opengl.effect.MaterialEffectGL;
import com.jagex.game.runetek5.toolkit.opengl.effect.StillWaterEffect;

public class EffectManagerGL {

    public int param1;
    public int param2;
    public int activeEffect;
    public final OpenGlToolkit openglToolkit;
    public final TextureContainerGL textureContainer;
    public final MaterialEffectGL[] materialEffects;
    public final StillWaterEffect waterEffect;

    public EffectManagerGL(OpenGlToolkit openglToolkit) {
        this.openglToolkit = openglToolkit;
        this.textureContainer = new TextureContainerGL(openglToolkit);
        this.materialEffects = new MaterialEffectGL[16];
        this.materialEffects[1] = new MaterialEffect1GL(openglToolkit);
        this.materialEffects[2] = new LappingWaterEffect(openglToolkit, this.textureContainer);
        this.materialEffects[4] = new RipplingWaterEffect(openglToolkit, this.textureContainer);
        this.materialEffects[5] = new WaterfallEffect(openglToolkit, this.textureContainer);
        this.materialEffects[6] = new HideLightsEffect(openglToolkit);
        this.materialEffects[7] = new ReflectingTextureEffect(openglToolkit);
        this.materialEffects[3] = this.waterEffect = new StillWaterEffect(openglToolkit);
        this.materialEffects[8] = new ReflectiveRipplingWaterEffect(openglToolkit, this.textureContainer);
        this.materialEffects[9] = new MaterialEffect9GL(openglToolkit, this.textureContainer);
        if (!this.materialEffects[8].isInitialized()) {
        	this.materialEffects[8] = this.materialEffects[4];
        }

        if (!this.materialEffects[9].isInitialized()) {
        	this.materialEffects[9] = this.materialEffects[8];
        }
    }

    public void loadEffect(int requestedEffect, int effectParam1, int effectParam2, boolean hasLight, boolean stillWater) {
        stillWater &= this.openglToolkit.stillwaterEffectLoaded();
        if (!stillWater && (requestedEffect == 4 || requestedEffect == 8 || requestedEffect == 9)) {
            if (requestedEffect == 4) {
                effectParam2 = effectParam1;
            }

            requestedEffect = 2;
        }

        if (requestedEffect != 0 && hasLight) {
            requestedEffect |= 0x80000000;
        }

        if (this.activeEffect != requestedEffect) {
            if (this.activeEffect != 0) {
            	this.materialEffects[this.activeEffect & 0x7fffffff].disable();
            }

            if (requestedEffect != 0) {
            	this.materialEffects[requestedEffect & 0x7fffffff].toggle(hasLight);
            	this.materialEffects[requestedEffect & 0x7fffffff].loadCombineMode();
            	this.materialEffects[requestedEffect & 0x7fffffff].applyEffectParams(effectParam1, effectParam2);
            }

            this.activeEffect = requestedEffect;
            this.param1 = effectParam1;
            this.param2 = effectParam2;
        } else if (this.activeEffect != 0) {
        	this.materialEffects[this.activeEffect & 0x7fffffff].loadCombineMode();
            if (effectParam1 != this.param1 || effectParam2 != this.param2) {
            	this.materialEffects[this.activeEffect & 0x7fffffff].applyEffectParams(effectParam1, effectParam2);
            	this.param1 = effectParam1;
            	this.param2 = effectParam2;
            }
        }
    }

    public boolean loadCombiner(OpenGlTexture combiner, int i_2) {
        if (this.activeEffect == 0) {
            return false;
        } 
        this.materialEffects[this.activeEffect & 0x7fffffff].loadCombiner(combiner, i_2);
        return true;       
    }

    public boolean stillwaterEffectLoaded() {
        return this.materialEffects[3].isInitialized();
    }
}
