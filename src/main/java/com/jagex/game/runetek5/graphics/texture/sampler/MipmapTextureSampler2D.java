package com.jagex.game.runetek5.graphics.texture.sampler;

import com.jagex.game.runetek5.toolkit.OpenGLNativeToolkit;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;
import com.jagex.game.runetek5.toolkit.opengl.GLFormat;

import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;

import com.jagex.game.runetek5.framebuffer.FrameBuffer;
import com.jagex.game.runetek5.graphics.texture.FrameBufferTexture2D;

import jaggl.OpenGL;

public class MipmapTextureSampler2D extends MipmapTextureSampler implements RectangleTextureSampler {

    public final int width;
    public final int height;

    public MipmapTextureSampler2D(OpenGLNativeToolkit toolkit, int width, int height, boolean mipmapsGenerated, int[] ints_5, int i_6, int i_7) {
        super(toolkit, GL_TEXTURE_2D, GLFormat.HDR, GLActionFormat.PIXEL_FORMAT, height * width, mipmapsGenerated);
        this.width = width;
        this.height = height;
        this.activeToolkit.setTextureSampler(this);
        if (mipmapsGenerated && i_7 == 0 && i_6 == 0) {
            generateMipmaps(this.textureTarget, width, height, ints_5);
        } else {
            OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, i_7);
            OpenGL.glTexImage2Di(this.textureTarget, 0, GL_RGBA, this.width, this.height, 0, GL_BGRA, this.activeToolkit.pixelType, ints_5, i_6 * 4);
            OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
        }

    }

    public MipmapTextureSampler2D(OpenGLNativeToolkit toolkit, GLFormat class1502, int width, int height, boolean bool5, byte[] pixels) {
        super(toolkit, GL_TEXTURE_2D, class1502, GLActionFormat.PIXEL_FORMAT, width * height, bool5);
        this.width = width;
        this.height = height;
        this.activeToolkit.setTextureSampler(this);
        OpenGL.glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        if (bool5) {
            generateMipmaps(super.textureTarget, width, height, pixels);
        } else {
            OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
            GLFormat class1509 = super.pixelFormat;
            if (class1509 == GLFormat.aClass1501956) {
                OpenGL.glCompressedTexImage2Dub(super.textureTarget, 0, GL_COMPRESSED_RGBA_S3TC_DXT1_EXT, width, height, 0, width * height / 2, pixels, 0);
            } else {
                GLFormat class15010 = super.pixelFormat;
                if (class15010 == GLFormat.aClass1501957) {
                    OpenGL.glCompressedTexImage2Dub(super.textureTarget, 0, GL_COMPRESSED_RGBA_S3TC_DXT5_EXT, width, height, 0, width * height, pixels, 0);
                } else {
                    OpenGL.glTexImage2Dub(super.textureTarget, 0, OpenGLNativeToolkit.texture2dInternalFormat(super.pixelFormat, super.pixelActionFormat), width, height, 0, OpenGLNativeToolkit.texture2dFormat(super.pixelFormat), GL_UNSIGNED_BYTE, pixels, 0);
                }
            }

            OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
        }

        OpenGL.glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
    }

    @SuppressWarnings("unused")
	public MipmapTextureSampler2D(OpenGLNativeToolkit class505Sub2Sub11, GLFormat class1502, int i_3, int i_4, float[] floats_6) {
        super(class505Sub2Sub11, GL_TEXTURE_2D, class1502, GLActionFormat.FLOAT_32_ARB, i_3 * i_4, true);
        this.width = i_3;
        this.height = i_4;
        super.activeToolkit.setTextureSampler(this);
        if (true) {
            generateMipmaps(super.textureTarget, i_3, i_4, floats_6);
        } else {
            OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
            OpenGL.glTexImage2Df(super.textureTarget, 0, OpenGLNativeToolkit.texture2dInternalFormat(super.pixelFormat, super.pixelActionFormat),
            		i_3, i_4, 0, OpenGLNativeToolkit.texture2dFormat(super.pixelFormat), 5126, floats_6, 0);
            OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
        }

    }

    public MipmapTextureSampler2D(OpenGLNativeToolkit toolkit, GLFormat class1502, GLActionFormat class763, int width, int height) {
        super(toolkit, GL_TEXTURE_2D, class1502, class763, height * width, false);
        this.width = width;
        this.height = height;
        super.activeToolkit.setTextureSampler(this);
        OpenGL.glTexImage2Dub(super.textureTarget, 0, OpenGLNativeToolkit.texture2dInternalFormat(super.pixelFormat, super.pixelActionFormat),
        		width, height, 0, OpenGLNativeToolkit.texture2dFormat(super.pixelFormat), OpenGLNativeToolkit.actionFormatDataType(super.pixelActionFormat), null, 0);
    }

    @Override
    public void bindTexture() {
        super.bindTexture();
    }

    @Override
    public int getTextureWidth() {
        return this.width;
    }

    @Override
    public int getTextureHeight() {
        return this.height;
    }

    public void method14494(int i_3, int i_4, float[] floats_5, GLFormat class1506) {
        super.activeToolkit.setTextureSampler(this);
        OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
        OpenGL.glTexSubImage2Df(super.textureTarget, 0, 0, 0, i_3, i_4, OpenGLNativeToolkit.texture2dFormat(class1506), GL_UNSIGNED_BYTE, floats_5, 0);
        OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
    }

    @Override
    public boolean method46() {
        return true;
    }

    @Override
    public void setRepeat(boolean repeatS, boolean repeatT) {
        super.activeToolkit.setTextureSampler(this);
        OpenGL.glTexParameteri(super.textureTarget, GL_TEXTURE_WRAP_S, repeatS ? GL_REPEAT : GL_CLAMP_TO_EDGE);
        OpenGL.glTexParameteri(super.textureTarget, GL_TEXTURE_WRAP_T, repeatT ? GL_REPEAT : GL_CLAMP_TO_EDGE);
    }
    
    @Override
    public void method52(int i_1, int i_2, int i_3, int i_4, byte[] bytes_5, GLFormat class1506, int i_7, int i_8) {
        super.activeToolkit.setTextureSampler(this);
        OpenGL.glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, i_8);
        OpenGL.glTexSubImage2Dub(super.textureTarget, 0, i_1, i_2, i_3, i_4, OpenGLNativeToolkit.texture2dFormat(class1506), GL_UNSIGNED_BYTE, bytes_5, i_7);
        OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
        OpenGL.glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
    }

    @Override
    public float method47(float f_1) {
        return f_1 / this.width;
    }

    @Override
    public FrameBuffer getFramebuffer() {
        return new FrameBufferTexture2D(this, 0);
    }

    @Override
    public void setSamplerMode(SampleMode mode) {
        super.setSamplerMode(mode);
    }

    @Override
    public void deallocate() {
        super.deallocate();
    }

    @Override
    public void method48(int i_1, int i_2, int i_3, int i_4, int[] ints_5, int i_6, int i_7) {
        super.activeToolkit.setTextureSampler(this);
        OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, i_7);
        OpenGL.glTexSubImage2Di(super.textureTarget, 0, i_1, i_2, i_3, i_4, GL_BGRA, super.activeToolkit.pixelType, ints_5, i_6);
        OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
    }

    @Override
    public float method62(float f_1) {
        return f_1 / this.height;
    }

    @Override
    public void method53(int i_3, int i_4, int[] ints_5) {
        int[] ints_7 = new int[this.width * this.height];
        super.activeToolkit.setTextureSampler(this);
        OpenGL.glGetTexImagei(super.textureTarget, 0, GL_BGRA, GL_UNSIGNED_BYTE, ints_7, 0);

        for (int i_8 = 0; i_8 < i_4; i_8++) {
            System.arraycopy(ints_7, this.width * ((i_4 - 1) - i_8), ints_5, i_3 * i_8, i_3);
        }

    }

    @Override
    public boolean enableMipmapping() {
        return super.enableMipmapping();
    }
}
