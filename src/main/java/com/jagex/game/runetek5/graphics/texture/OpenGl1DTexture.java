package com.jagex.game.runetek5.graphics.texture;

import com.jagex.game.runetek5.toolkit.OpenGlToolkit;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;
import com.jagex.game.runetek5.toolkit.opengl.GLFormat;
import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;
import jaggl.OpenGL;

public class OpenGl1DTexture extends OpenGlTexture {

    public final int width;

    public OpenGl1DTexture(OpenGlToolkit toolkit, GLFormat controllerA, GLActionFormat format, byte[] data, GLFormat controllerB) {
        super(toolkit, GL_TEXTURE_1D, controllerA, format, 2, false);
        this.width = 2;
        super.activeToolkit.setActiveTexture(this);
        OpenGL.glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        OpenGL.glTexImage1Dub(super.textureTarget, 0, OpenGlToolkit.getInternalTextureFormat(super.aClass1501650, super.aClass761651), this.width, 0, OpenGlToolkit.format(controllerB), GL_UNSIGNED_BYTE, data, 0);
        OpenGL.glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
        setLinearTextureFilter(true);
    }

    public void setActive() {
        super.activeToolkit.setActiveTexture(this);
        OpenGL.glTexParameteri(super.textureTarget, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    }
}
