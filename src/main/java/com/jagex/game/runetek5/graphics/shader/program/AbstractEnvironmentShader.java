package com.jagex.game.runetek5.graphics.shader.program;

import com.jagex.game.runetek5.config.material.MaterialType;
import com.jagex.game.runetek5.graphics.texture.sampler.CubeTextureSampler;
import com.jagex.game.runetek5.graphics.texture.sampler.NativeTextureSampler;
import com.jagex.game.runetek5.math.Matrix44;
import com.jagex.game.runetek5.math.Vector3;
import com.jagex.game.runetek5.math.Vector4;
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit;

public abstract class AbstractEnvironmentShader {

    public CubeTextureSampler anInterface31451;
    public NativeTextureSampler textureSampler;
    public int anInt467;
    public int anInt468;
    public int anInt469;
    public int anInt470;
    public final float[] pointLightsDiffuseColor = new float[16];
    public final float[] pointLightsPosAndRadiusSq = new float[16];
    public final Matrix44 textureCoordinateMatrix = new Matrix44();
    public final Vector3 eyePos = new Vector3();
    public final Vector4 heightFogPlane = new Vector4();
    public final Vector3 heightFogColor = new Vector3();
    public final Vector4 distanceFogPlane = new Vector4();
    public final Vector3 distanceFogColor = new Vector3();
    public final Vector3 sunPosition = new Vector3();
    public final Vector3 sunColor = new Vector3();
    public final Vector3 antiSunColor = new Vector3();
    public final Vector3 lightColor = new Vector3();
    public final Matrix44 aClass384465 = new Matrix44();
    protected float aFloat456;
    protected float aFloat453;
    protected final AbstractNativeToolkit activeToolkit;

    protected AbstractEnvironmentShader(AbstractNativeToolkit toolkit) {
        this.activeToolkit = toolkit;
    }

    public void indentifyMaterialEffect(MaterialType material) {
        switch (material.effectParam1) {
            case 1:
                this.aFloat456 = 32.0F;
                this.aFloat453 = 0.5F;
                break;
            case 2:
                this.aFloat456 = 4.0F;
                this.aFloat453 = 0.65F;
                break;
            case 3:
                this.aFloat456 = 2.0F;
                this.aFloat453 = 0.8F;
        }

    }

    public abstract void loadUnlitProgram(boolean var1);

    public abstract void loadSpecularPointlight(int var1);

    public abstract void loadPointlightEnvironmentMapping(int var1);

    public abstract void loadUnderwaterGround();

    public abstract void loadWorldViewProjection(Matrix44 var1);

    public abstract void loadStandartPointlight(int var1);

}
