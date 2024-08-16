package com.jagex.game.runetek5.graphics.texture;

import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.toolkit.OpenGlToolkit;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;

public abstract class Node_Sub5 extends Node {

    public boolean aBool7506;
    final OpenGlToolkit activeToolkit;

    Node_Sub5(OpenGlToolkit class505Sub11) {
        this.activeToolkit = class505Sub11;
    }

    public abstract void draw(OpenGl2DTexture var2);

    public abstract void destroy();

    public abstract void resetTexture();

    public abstract void calculateMipmaps(int s, int t);

    public int getTexturedFaces() {
        return 1;
    }

    public boolean loadTexture() {
        return false;
    }

    public GLActionFormat getFormat() {
        return GLActionFormat.PIXEL_FORMAT;
    }

    public boolean method12129() {
        return this.aBool7506;
    }

    public abstract boolean createShaders();

    public abstract boolean displayPresent();

}
