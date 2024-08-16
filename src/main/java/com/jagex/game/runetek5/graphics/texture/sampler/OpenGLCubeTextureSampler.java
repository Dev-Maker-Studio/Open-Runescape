package com.jagex.game.runetek5.graphics.texture.sampler;

import com.jagex.game.runetek5.toolkit.OpenGLNativeToolkit;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;
import com.jagex.game.runetek5.toolkit.opengl.GLFormat;

import jaggl.OpenGL;
import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;

public class OpenGLCubeTextureSampler extends MipmapTextureSampler implements CubeTextureSampler {

    public OpenGLCubeTextureSampler(OpenGLNativeToolkit toolkit, int sizeXy, boolean mipmapsGenerated, int[][] materialPixels) {
        super(toolkit, GL_TEXTURE_CUBE_MAP, GLFormat.HDR, GLActionFormat.PIXEL_FORMAT, sizeXy * sizeXy * 6, mipmapsGenerated);
        super.activeToolkit.setTextureSampler(this);
        if (mipmapsGenerated) {
            for (int index = 0; index < 6; index++) {
                generateMipmaps(index + GL_TEXTURE_CUBE_MAP_POSITIVE_X, sizeXy, sizeXy, materialPixels[index]);
            }
        } else {
            for (int index = 0; index < 6; index++) {
                OpenGL.glTexImage2Di(index + GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, OpenGLNativeToolkit.texture2dInternalFormat(super.pixelFormat, super.pixelActionFormat),
                        sizeXy, sizeXy, 0, OpenGLNativeToolkit.texture2dFormat(super.pixelFormat), super.activeToolkit.pixelType, materialPixels[index], 0);
            }
        }
    }

    @Override
    public void bindTexture() {
        super.bindTexture();
    }

    @Override
    public void setSamplerMode(SampleMode sampleMode) {
        super.setSamplerMode(sampleMode);
    }

    @Override
    public void deallocate() {
        super.deallocate();
    }
}
