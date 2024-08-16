package com.jagex.game.runetek5.graphics.shader.program.impl;

import com.jagex.game.runetek5.graphics.shader.MappedShaderCreationException;
import com.jagex.game.runetek5.graphics.shader.Shader;
import com.jagex.game.runetek5.graphics.shader.ShaderAttribute;
import com.jagex.game.runetek5.graphics.shader.ShaderAttributeLoadingException;
import com.jagex.game.runetek5.graphics.shader.ShaderMapping;
import com.jagex.game.runetek5.graphics.shader.program.AbstractBloomShader;
import com.jagex.game.runetek5.graphics.texture.sampler.NativeTextureSampler;
import com.jagex.game.runetek5.imageproducer.DirectDisplay;
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit;

public class NativeFilterBloomShader extends AbstractBloomShader {

    public static float green = 0.25F;
    public static float blue = 1.0F;
    public static float red = 1.0F;
    public static float baseBlur = 1.0F;
    public Shader filterBloomShader;
    public ShaderAttribute sceneTex;
    public ShaderAttribute bloomTex1;
    public ShaderAttribute params;
    public ShaderAttribute sampleSize;
    public ShaderAttribute pixelOffsetAndBloomScale;
    public ShaderAttribute posAndTexCoords;
    public ShaderMapping techFullscreenTri;
    public ShaderMapping brightpass;
    public ShaderMapping blur;
    public ShaderMapping composite;
    public ShaderMapping techDefault;
    public boolean loaded;

    public NativeFilterBloomShader(AbstractNativeToolkit nativeToolkit) {
        super(nativeToolkit);
    }

    @Override
    public void reset() {
        this.filterBloomShader.reset();
    }

    @Override
    public boolean isLoaded() {
        return this.loaded;
    }

    @Override
    public void method1102() {
    }

    @Override
    public void destroy() {
    }

    @Override
    public int getIterations() {
        return 4;
    }

    @Override
    public void initialize(int type, DirectDisplay display, NativeTextureSampler compositeSampler, NativeTextureSampler interface65) {
        float f_6 = super.nativeToolkit.method13905();
        float displayWidth = display.getWidth();
        float displayHeight = display.getHeight();
        NativeTextureSampler sampler = compositeSampler;
        float[] floats_10 = {-1.0f, 1.0F, 0.0F, 0.0F, -1.0f, -3.0f, 0.0F, 2.0F, 3.0F, 1.0F, 2.0F, 0.0F};
        int effectWidth = (int) displayWidth;
        int effectHeight = (int) displayHeight;
        int i_13 = effectWidth;
        int i_14 = effectHeight;
        if (type == 0) {
            effectWidth = 256;
            effectHeight = 256;
            this.filterBloomShader.loadProgram(this.brightpass);
            this.filterBloomShader.setActiveMapping();
        } else if (type == 1) {
            effectWidth = 256;
            effectHeight = 256;
            i_13 = effectWidth;
            i_14 = effectHeight;
            this.filterBloomShader.loadProgram(this.blur);
            this.filterBloomShader.setActiveMapping();
            this.filterBloomShader.unform2f(this.sampleSize, baseBlur / displayWidth, 0.0F);
        } else if (type == 2) {
            effectWidth = 256;
            effectHeight = 256;
            i_13 = effectWidth;
            i_14 = effectHeight;
            this.filterBloomShader.loadProgram(this.blur);
            this.filterBloomShader.setActiveMapping();
            this.filterBloomShader.unform2f(this.sampleSize, 0.0F, baseBlur / displayHeight);
        } else if (type == 3) {
            this.filterBloomShader.loadProgram(this.composite);
            sampler = interface65;
            this.filterBloomShader.loadTextureSampler(this.bloomTex1, 1, compositeSampler);
            this.filterBloomShader.setActiveMapping();
        }

        float f_15 = effectWidth / displayWidth;
        float f_16 = effectHeight / displayHeight;
        float f_17 = i_13 / displayWidth;
        float f_18 = i_14 / displayHeight;
        floats_10[8] = (floats_10[8] + 1.0F) * f_15 - 1.0F;
        floats_10[5] = (floats_10[5] - 1.0F) * f_16 + 1.0F;
        floats_10[10] *= f_17;
        floats_10[7] *= f_18;
        this.filterBloomShader.method1688(this.posAndTexCoords, floats_10);
        this.filterBloomShader.loadTextureSampler(this.sceneTex, 0, sampler);
        this.filterBloomShader.loadRgbaColorAttribute(this.params, red, green, blue, 0.0F);
        this.filterBloomShader.loadRgbaColorAttribute(this.pixelOffsetAndBloomScale, f_6 / displayWidth, f_6 / displayHeight, 256.0F / displayWidth, 256.0F / displayHeight);
        super.nativeToolkit.setDrawableArea(0, 0, effectWidth, effectHeight);
        super.nativeToolkit.setClipping(0, 0, effectWidth, effectHeight);
    }

    @Override
    public int getFormatType() {
        return 1;
    }

    public boolean isShaderSupported() {
        return super.nativeToolkit.GLEXTFramebufferObject && super.nativeToolkit.supportsArbFragmentShader();
    }

    @Override
    public boolean method1097() {
        if (isShaderSupported()) {
            this.filterBloomShader = super.nativeToolkit.loadShader("FilterBloom");
            if (this.filterBloomShader == null) {
                return false;
            } else {
                try {
                    this.sceneTex = this.filterBloomShader.getAttribute("sceneTex");
                    this.bloomTex1 = this.filterBloomShader.getAttribute("bloomTex1");
                    this.params = this.filterBloomShader.getAttribute("params");
                    this.sampleSize = this.filterBloomShader.getAttribute("sampleSize");
                    this.pixelOffsetAndBloomScale = this.filterBloomShader.getAttribute("pixelOffsetAndBloomScale");
                    this.posAndTexCoords = this.filterBloomShader.getAttribute("PosAndTexCoords");
                    this.filterBloomShader.getShaderMapping("test");
                    this.techFullscreenTri = this.filterBloomShader.getShaderMapping("techFullscreenTri");
                    this.brightpass = this.filterBloomShader.getShaderMapping("brightpass");
                    this.blur = this.filterBloomShader.getShaderMapping("blur");
                    this.composite = this.filterBloomShader.getShaderMapping("composite");
                    this.techDefault = this.filterBloomShader.getShaderMapping("techDefault");
                } catch (ShaderAttributeLoadingException exceptionSub2Sub22) {
                    return false;
                } catch (MappedShaderCreationException exceptionSub2Sub13) {
                    return false;
                }

                if (!this.techFullscreenTri.createShaders()) {
                    return false;
                } else if (!this.brightpass.createShaders()) {
                    return false;
                } else if (!this.blur.createShaders()) {
                    return false;
                } else if (!this.composite.createShaders()) {
                    return false;
                } else if (!this.techDefault.createShaders()) {
                    return false;
                } else {
                    this.loaded = true;
                    return true;
                }
            }
        } else {
            return false;
        }
    }
}
