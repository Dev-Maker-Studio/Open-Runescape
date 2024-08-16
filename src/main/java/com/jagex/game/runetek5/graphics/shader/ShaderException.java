package com.jagex.game.runetek5.graphics.shader;

@SuppressWarnings("serial")
public class ShaderException extends Exception {

    public final ShaderLoader shaderLoader;

    public ShaderException(ShaderLoader shaderLoader) {
        super("");
        this.shaderLoader = shaderLoader;
    }

}
