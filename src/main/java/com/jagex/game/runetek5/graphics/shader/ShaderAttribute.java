package com.jagex.game.runetek5.graphics.shader;

import com.jagex.game.runetek5.toolkit.opengl.effect.ProgramAttribute;
import com.jagex.game.runetek5.unidentified.Class111;

public abstract class ShaderAttribute extends ProgramAttribute {

    public final Class111 aClass11110058;
    public final String primaryShaderAttributeName;
    public final String secondaryShaderAttributeName;
    public final int anInt10059;
    public final Class111 aClass11110060;

    protected ShaderAttribute(ShaderVariableDecoder class1221) {
        this.aClass11110058 = class1221.aClass1111529;
        this.primaryShaderAttributeName = class1221.primaryShaderAttributeName;
        this.secondaryShaderAttributeName = class1221.secondaryShaderAttributeName;
        this.anInt10059 = class1221.anInt1528;
        this.aClass11110060 = class1221.aClass1111533;
    }

    protected String getPrimaryShaderAttributeName() {
        return this.primaryShaderAttributeName;
    }

    public Class111 method15457() {
        return this.aClass11110058;
    }

    public Class111 method15458() {
        return this.aClass11110060;
    }

    public int method15459() {
        if (this.aClass11110058 != Class111.aClass1111109) {
            throw new IllegalShaderArgumentException();
        } else {
            return this.anInt10059;
        }
    }

    public String toString() {
        String string_1 = this.aClass11110058 + " " + this.primaryShaderAttributeName;
        if (this.secondaryShaderAttributeName != null && !this.secondaryShaderAttributeName.isEmpty()) {
            string_1 = string_1 + " : " + this.secondaryShaderAttributeName;
        }
        return string_1;
    }

    public abstract int getUniformLocation(int slot);

    public abstract boolean loadUniformLocation(int var1);
}
