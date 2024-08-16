package com.jagex.game.runetek5.graphics.shader.program;

import com.jagex.game.runetek5.graphics.texture.sampler.NativeTextureSampler;
import com.jagex.game.runetek5.imageproducer.DirectDisplay;
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit;

public abstract class AbstractBloomShader {

    protected final AbstractNativeToolkit nativeToolkit;
    public boolean hasLinkedVector;

    protected AbstractBloomShader(AbstractNativeToolkit class505Sub21) {
        this.nativeToolkit = class505Sub21;
    }

    public abstract boolean method1097();

    public abstract boolean isLoaded();

    public boolean method1099() {
        return method1097();
    }

    public int getIterations() {
        return 1;
    }

    public abstract void method1102();

    public abstract void initialize(int type, DirectDisplay display, NativeTextureSampler compositeSampler, NativeTextureSampler var5);

    public abstract void reset();

    public void method1105() {
    }

    public int getFormatType() {
        return 0;
    }

    public boolean method1107() {
        return this.hasLinkedVector;
    }

    public abstract void destroy();

}
