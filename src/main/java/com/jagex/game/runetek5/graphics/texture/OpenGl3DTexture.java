package com.jagex.game.runetek5.graphics.texture;

import com.jagex.game.runetek5.toolkit.OpenGlToolkit;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;
import com.jagex.game.runetek5.toolkit.opengl.GLFormat;
import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;
import jaggl.OpenGL;

public class OpenGl3DTexture extends OpenGlTexture {

    public final int width;
    public final int height;
    public final int depth;

    public OpenGl3DTexture(OpenGlToolkit class505Sub11, GLFormat class1502, GLActionFormat class763) {
        super(class505Sub11, GL_TEXTURE_3D, class1502, class763, 262144, false);
        this.width = 128;
        this.height = 128;
        this.depth = 16;
        this.activeToolkit.setActiveTexture(this);
        OpenGL.glTexImage3Dub(this.textureTarget, 0, OpenGlToolkit.getInternalTextureFormat(this.aClass1501650, this.aClass761651), this.width, this.height, this.depth, 0, OpenGlToolkit.format(this.aClass1501650), GL_UNSIGNED_BYTE, null, 0);
        setLinearTextureFilter(true);
    }

    public OpenGl3DTexture(OpenGlToolkit class505Sub11, GLFormat class1502, GLActionFormat class763, byte[] bytes_7, GLFormat class1508) {
        super(class505Sub11, GL_TEXTURE_3D, class1502, class763, GL_TEXTURE_BIT, false);
        this.width = 128;
        this.height = 128;
        this.depth = 16;
        this.activeToolkit.setActiveTexture(this);
        OpenGL.glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        OpenGL.glTexImage3Dub(this.textureTarget, 0, OpenGlToolkit.getInternalTextureFormat(this.aClass1501650, this.aClass761651), this.width, this.height, this.depth, 0, OpenGlToolkit.format(class1508), GL_UNSIGNED_BYTE, bytes_7, 0);
        OpenGL.glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
        setLinearTextureFilter(true);
    }

    public void copy(int depth, int width, int height) { 
    	this.activeToolkit.setActiveTexture(this);
        OpenGL.glCopyTexSubImage3D(this.textureTarget, 0, 0, 0, depth, 0, 0, width, height);
        OpenGL.glFlush();
    }

}
