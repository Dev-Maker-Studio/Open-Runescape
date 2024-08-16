package com.jagex.game.runetek5.graphics.shader.program.impl;

import com.jagex.game.runetek5.graphics.shader.Shader;
import com.jagex.game.runetek5.graphics.shader.ShaderAttribute;
import com.jagex.game.runetek5.graphics.shader.ShaderBuilderException;
import com.jagex.game.runetek5.graphics.shader.ShaderMapping;
import com.jagex.game.runetek5.graphics.shader.program.AbstractEnvironmentShader;
import com.jagex.game.runetek5.math.Matrix44;
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit;
import com.jagex.game.runetek5.toolkit.companion.PrimitiveDrawMode;

public class NativeEnvironmentShaderHd extends AbstractEnvironmentShader {

    public int[] activeUniformLocation;
    public final Matrix44 worldViewProjectionMatrix = new Matrix44();
    public final ShaderMapping[] mappings = new ShaderMapping[18];
    public final int[][] uniformLocations = new int[18][17];
    public Shader modelShader;

    public NativeEnvironmentShaderHd(AbstractNativeToolkit nativeToolkit) throws ShaderBuilderException {
        super(nativeToolkit);
        initialize();
    }

    @Override
    public void loadUnlitProgram(boolean ignoreAlpha) {
        if (ignoreAlpha) {
            this.modelShader.loadProgram(this.mappings[1]);
        } else {
            this.modelShader.loadProgram(this.mappings[0]);
        }

        this.activeUniformLocation = this.uniformLocations[this.modelShader.getOccupyingSlot()];
        this.modelShader.setActiveMapping();
        this.modelShader.setDiffuseSampler(this.activeUniformLocation[2], 0, super.textureSampler);
        this.modelShader.setWorldViewProjectionMatrix(this.activeUniformLocation[4], this.worldViewProjectionMatrix);
        this.modelShader.setTextureCoordinateMatrix(this.activeUniformLocation[5], super.textureCoordinateMatrix);
        this.modelShader.load4f(this.activeUniformLocation[7], super.heightFogPlane.x, super.heightFogPlane.y, super.heightFogPlane.z, super.heightFogPlane.w);
        this.modelShader.load3f(this.activeUniformLocation[8], super.heightFogColor.x, super.heightFogColor.y, super.heightFogColor.z);
        this.modelShader.load4f(this.activeUniformLocation[9], super.distanceFogPlane.x, super.distanceFogPlane.y, super.distanceFogPlane.z, super.distanceFogPlane.w);
        this.modelShader.load3f(this.activeUniformLocation[10], super.distanceFogColor.x, super.distanceFogColor.y, super.distanceFogColor.z);
        super.activeToolkit.drawIndexedPrimitive(PrimitiveDrawMode.TRIANGLES, super.anInt467, super.anInt468, super.anInt469, super.anInt470);
    }

    @Override
    public void loadUnderwaterGround() {
        this.modelShader.loadProgram(this.mappings[17]);
        this.activeUniformLocation = this.uniformLocations[this.modelShader.getOccupyingSlot()];
        this.modelShader.setActiveMapping();
        loadUnit(0);
    }
    
    /**
     *             int slot = shader.get_mapping_slot(mappings[index]);
            uniform_locations[index][2] = DiffuseSampler.get_uniform_location(slot);
            uniform_locations[index][3] = EnvironmentSampler.get_uniform_location(slot);
            uniform_locations[index][1] = PointLightsPosAndRadiusSq.get_uniform_location(slot);
            uniform_locations[index][0] = PointLightsDiffuseColour.get_uniform_location(slot);
            uniform_locations[index][4] = WVPMatrix.get_uniform_location(slot);
            uniform_locations[index][5] = TexCoordMatrix.get_uniform_location(slot);
            uniform_locations[index][7] = HeightFogPlane.get_uniform_location(slot);
            uniform_locations[index][8] = HeightFogColour.get_uniform_location(slot);
            uniform_locations[index][9] = DistanceFogPlane.get_uniform_location(slot);
            uniform_locations[index][10] = DistanceFogColour.get_uniform_location(slot);
            uniform_locations[index][11] = SunDir.get_uniform_location(slot);
            uniform_locations[index][12] = SunColour.get_uniform_location(slot);
            uniform_locations[index][13] = AntiSunColour.get_uniform_location(slot);
            uniform_locations[index][14] = AmbientColour.get_uniform_location(slot);
            uniform_locations[index][6] = EyePos.get_uniform_location(slot);
            uniform_locations[index][15] = WorldMatrix.get_uniform_location(slot);
            uniform_locations[index][16] = SpecularExponent.get_uniform_location(slot);
     * @param unit
     */

    public void loadUnit(int unit) {
        this.modelShader.setDiffuseSampler(this.activeUniformLocation[2], 0, super.textureSampler);
        this.modelShader.setWorldViewProjectionMatrix(this.activeUniformLocation[4], this.worldViewProjectionMatrix);
        this.modelShader.setTextureCoordinateMatrix(this.activeUniformLocation[5], super.textureCoordinateMatrix);
        this.modelShader.load4f(this.activeUniformLocation[7], super.heightFogPlane.x, super.heightFogPlane.y, super.heightFogPlane.z, super.heightFogPlane.w);
        this.modelShader.load3f(this.activeUniformLocation[8], super.heightFogColor.x, super.heightFogColor.y, super.heightFogColor.z);
        this.modelShader.load4f(this.activeUniformLocation[9], super.distanceFogPlane.x, super.distanceFogPlane.y, super.distanceFogPlane.z, super.distanceFogPlane.w);
        this.modelShader.load3f(this.activeUniformLocation[10], super.distanceFogColor.x, super.distanceFogColor.y, super.distanceFogColor.z);
        this.modelShader.loadVector3f(this.activeUniformLocation[11], super.sunPosition);
        this.modelShader.loadVector3f(this.activeUniformLocation[12], super.sunColor);
        this.modelShader.loadVector3f(this.activeUniformLocation[13], super.antiSunColor);
        this.modelShader.loadVector3f(this.activeUniformLocation[14], super.lightColor);
        if (unit > 0) {
            this.modelShader.method1673(this.activeUniformLocation[1], super.pointLightsPosAndRadiusSq, unit * 4);
            this.modelShader.method1673(this.activeUniformLocation[0], super.pointLightsDiffuseColor, unit * 4);
        }

        super.activeToolkit.drawIndexedPrimitive(PrimitiveDrawMode.TRIANGLES, super.anInt467, super.anInt468, super.anInt469, super.anInt470);
    }

    @Override
    public void loadStandartPointlight(int unit) {
        this.modelShader.loadProgram(this.mappings[unit + 2]);
        this.activeUniformLocation = this.uniformLocations[this.modelShader.getOccupyingSlot()];
        this.modelShader.setActiveMapping();
        loadUnit(unit);
    }

    @Override
    public void loadSpecularPointlight(int unit) {
        this.modelShader.loadProgram(this.mappings[unit + 7]);
        this.activeUniformLocation = this.uniformLocations[this.modelShader.getOccupyingSlot()];
        this.modelShader.setActiveMapping();
        this.modelShader.load3f(this.activeUniformLocation[6], super.eyePos.x, super.eyePos.y, super.eyePos.z);
        this.modelShader.load4f(this.activeUniformLocation[16], super.aFloat456, super.aFloat453, 0.0F, 0.0F);
        loadUnit(unit);
    }

    @Override
    public void loadPointlightEnvironmentMapping(int unit) {
        this.modelShader.loadProgram(this.mappings[unit + 12]);
        this.activeUniformLocation = this.uniformLocations[this.modelShader.getOccupyingSlot()];
        this.modelShader.setActiveMapping();
        this.modelShader.method1674(this.activeUniformLocation[15], super.aClass384465);
        this.modelShader.load3f(this.activeUniformLocation[6], super.eyePos.x, super.eyePos.y, super.eyePos.z);
        this.modelShader.setDiffuseSampler(this.activeUniformLocation[3], 1, super.anInterface31451);
        loadUnit(unit);
    }

    public boolean initialize() throws ShaderBuilderException {
        this.modelShader = super.activeToolkit.loadShader("Model");
        ShaderAttribute DiffuseSampler = this.modelShader.getAttribute("DiffuseSampler");
        ShaderAttribute EnvironmentSampler = this.modelShader.getAttribute("EnvironmentSampler");
        ShaderAttribute PointLightsPosAndRadiusSq = this.modelShader.getAttribute("PointLightsPosAndRadiusSq");
        ShaderAttribute PointLightsDiffuseColour = this.modelShader.getAttribute("PointLightsDiffuseColour");
        ShaderAttribute WVPMatrix = this.modelShader.getAttribute("WVPMatrix");
        ShaderAttribute TexCoordMatrix = this.modelShader.getAttribute("TexCoordMatrix");
        ShaderAttribute HeightFogPlane = this.modelShader.getAttribute("HeightFogPlane");
        ShaderAttribute HeightFogColour = this.modelShader.getAttribute("HeightFogColour");
        ShaderAttribute DistanceFogPlane = this.modelShader.getAttribute("DistanceFogPlane");
        ShaderAttribute DistanceFogColour = this.modelShader.getAttribute("DistanceFogColour");
        ShaderAttribute SunDir = this.modelShader.getAttribute("SunDir");
        ShaderAttribute SunColour = this.modelShader.getAttribute("SunColour");
        ShaderAttribute AntiSunColour = this.modelShader.getAttribute("AntiSunColour");
        ShaderAttribute AmbientColour = this.modelShader.getAttribute("AmbientColour");
        ShaderAttribute EyePos = this.modelShader.getAttribute("EyePos");
        ShaderAttribute SpecularExponent = this.modelShader.getAttribute("SpecularExponent");
        ShaderAttribute WorldMatrix = this.modelShader.getAttribute("WorldMatrix");
        this.mappings[0] = this.modelShader.getShaderMapping("Unlit");
        this.mappings[1] = this.modelShader.getShaderMapping("Unlit_IgnoreAlpha");
        this.mappings[17] = this.modelShader.getShaderMapping("UnderwaterGround");
        for (int index = 0; index <= 4; index++) {
            this.mappings[index + 2] = this.modelShader.getShaderMapping("Standard_" + index + "PointLights");
            this.mappings[index + 7] = this.modelShader.getShaderMapping("Specular_" + index + "PointLights");
            this.mappings[index + 12] = this.modelShader.getShaderMapping("EnvironmentalMapping_" + index + "PointLights");
        }
        for (int index = 0; index < 18; index++) {
            int slot = this.modelShader.getMappingSlot(this.mappings[index]);
            this.uniformLocations[index][2] = DiffuseSampler.getUniformLocation(slot);
            this.uniformLocations[index][3] = EnvironmentSampler.getUniformLocation(slot);
            this.uniformLocations[index][1] = PointLightsPosAndRadiusSq.getUniformLocation(slot);
            this.uniformLocations[index][0] = PointLightsDiffuseColour.getUniformLocation(slot);
            this.uniformLocations[index][4] = WVPMatrix.getUniformLocation(slot);
            this.uniformLocations[index][5] = TexCoordMatrix.getUniformLocation(slot);
            this.uniformLocations[index][7] = HeightFogPlane.getUniformLocation(slot);
            this.uniformLocations[index][8] = HeightFogColour.getUniformLocation(slot);
            this.uniformLocations[index][9] = DistanceFogPlane.getUniformLocation(slot);
            this.uniformLocations[index][10] = DistanceFogColour.getUniformLocation(slot);
            this.uniformLocations[index][11] = SunDir.getUniformLocation(slot);
            this.uniformLocations[index][12] = SunColour.getUniformLocation(slot);
            this.uniformLocations[index][13] = AntiSunColour.getUniformLocation(slot);
            this.uniformLocations[index][14] = AmbientColour.getUniformLocation(slot);
            this.uniformLocations[index][6] = EyePos.getUniformLocation(slot);
            this.uniformLocations[index][15] = WorldMatrix.getUniformLocation(slot);
            this.uniformLocations[index][16] = SpecularExponent.getUniformLocation(slot);
        }
        this.modelShader.loadProgram(this.mappings[2]);
        return true;
    }

    @Override
    public void loadWorldViewProjection(Matrix44 wvpMatrix) {
        this.worldViewProjectionMatrix.copyFrom(wvpMatrix);
        this.worldViewProjectionMatrix.multiplyBy(super.activeToolkit.aClass3848729);
    }
}
