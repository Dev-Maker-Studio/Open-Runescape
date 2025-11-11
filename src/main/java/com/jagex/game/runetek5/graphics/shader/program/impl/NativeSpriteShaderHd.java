package com.jagex.game.runetek5.graphics.shader.program.impl;

import com.jagex.game.runetek5.graphics.shader.Shader;
import com.jagex.game.runetek5.graphics.shader.ShaderAttribute;
import com.jagex.game.runetek5.graphics.shader.ShaderBuilderException;
import com.jagex.game.runetek5.graphics.shader.ShaderMapping;
import com.jagex.game.runetek5.graphics.shader.program.AbstractSpriteShader;
import com.jagex.game.runetek5.math.Vector4;
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit;
import com.jagex.game.runetek5.toolkit.companion.PrimitiveDrawMode;

public class NativeSpriteShaderHd extends AbstractSpriteShader {

    public int[] activeUniformLocations;

    public Shader spriteShader;

    public final ShaderMapping[] shaderMappings = new ShaderMapping[3];

    public final int[][] uniformLocations = new int[3][7];

    public final Vector4 mulColour = new Vector4(1.0F, 1.0F, 1.0F, 1.0F);

    public final Vector4 addColour = new Vector4(0.0F, 0.0F, 0.0F, 0.0F);

    public NativeSpriteShaderHd(AbstractNativeToolkit nativeToolkit) throws ShaderBuilderException {
        super(nativeToolkit);
        initialize();
    }

    @Override
    public void loadNormal() {
        this.spriteShader.loadProgram(this.shaderMappings[0]);
        this.activeUniformLocations = this.uniformLocations[this.spriteShader.getOccupyingSlot()];
        load();
    }

    public boolean initialize() throws ShaderBuilderException {
        this.spriteShader = super.activeToolkit.loadShader("Sprite");
        ShaderAttribute WVPMatrix = this.spriteShader.getAttribute("WVPMatrix");
        ShaderAttribute SpriteSampler = this.spriteShader.getAttribute("SpriteSampler");
        ShaderAttribute MaskSampler = this.spriteShader.getAttribute("MaskSampler");
        ShaderAttribute MulColour = this.spriteShader.getAttribute("MulColour");
        ShaderAttribute AddColour = this.spriteShader.getAttribute("AddColour");
        ShaderAttribute SpriteTexCoordMatrix = this.spriteShader.getAttribute("SpriteTexCoordMatrix");
        ShaderAttribute MaskTexCoordMatrix = this.spriteShader.getAttribute("MaskTexCoordMatrix");
        this.shaderMappings[0] = this.spriteShader.getShaderMapping("Normal");
        this.shaderMappings[1] = this.spriteShader.getShaderMapping("Masked");
        this.shaderMappings[2] = this.spriteShader.getShaderMapping("AlphaTex");
        for (int type = 0; type < 3; type++) {
            int slot = this.spriteShader.getMappingSlot(this.shaderMappings[type]);
            this.uniformLocations[type][0] = WVPMatrix.getUniformLocation(slot);
            this.uniformLocations[type][1] = SpriteSampler.getUniformLocation(slot);
            this.uniformLocations[type][2] = MaskSampler.getUniformLocation(slot);
            this.uniformLocations[type][5] = MulColour.getUniformLocation(slot);
            this.uniformLocations[type][6] = AddColour.getUniformLocation(slot);
            this.uniformLocations[type][3] = SpriteTexCoordMatrix.getUniformLocation(slot);
            this.uniformLocations[type][4] = MaskTexCoordMatrix.getUniformLocation(slot);
        }
        this.spriteShader.loadProgram(this.shaderMappings[0]);
        return true;
    }

    @Override
    public void setColours(int type, int value) {
        switch (type) {
            case 0:
            case 2:
                this.mulColour.unpack(value);
                this.addColour.unpack(0);
                break;
            case 1:
                this.mulColour.unpack(-1);
                this.addColour.unpack(0);
                break;
            case 3:
                this.mulColour.unpack(0xffffff | value & ~0xffffff);
                this.addColour.unpack(value & 0xffffff);
                break;
            case 4:
                this.mulColour.unpack(-1);
                this.addColour.unpack(value);
                this.addColour.negate();
        }
    }

    public void load() {
        this.spriteShader.setActiveMapping();
        this.spriteShader.setDiffuseSampler(this.activeUniformLocations[1], 0, super.anInterface6443);
        super.activeToolkit.loadWorldViewProjectionMatrix(super.worldViewProjectionMatrix);
        this.spriteShader.setWorldViewProjectionMatrix(this.activeUniformLocations[0], super.worldViewProjectionMatrix);
        this.spriteShader.setTextureCoordinateMatrix(this.activeUniformLocations[3], super.aClass384440);
        this.spriteShader.load4f(this.activeUniformLocations[5], this.mulColour.x, this.mulColour.y, this.mulColour.z, this.mulColour.w);
        this.spriteShader.load4f(this.activeUniformLocations[6], this.addColour.x, this.addColour.y, this.addColour.z, this.addColour.w);
        super.activeToolkit.setStreamSource(0, super.anInterface4445);
        super.activeToolkit.setVertexDeclaration(super.aClass70447);
        super.activeToolkit.drawPrimitive(PrimitiveDrawMode.TRIANGLE_FAN, super.anInt446, 2);
    }

    @Override
    public void loadMasked() {
        this.spriteShader.loadProgram(this.shaderMappings[1]);
        this.activeUniformLocations = this.uniformLocations[this.spriteShader.getOccupyingSlot()];
        this.spriteShader.setDiffuseSampler(this.activeUniformLocations[2], 1, super.anInterface6441);
        this.spriteShader.setTextureCoordinateMatrix(this.activeUniformLocations[4], super.aClass384444);
        load();
    }
}
