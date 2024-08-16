package com.jagex.game.runetek5.graphics.texture.sampler;

import com.jagex.game.runetek5.toolkit.OpenGLNativeToolkit;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;
import com.jagex.game.runetek5.toolkit.opengl.GLFormat;

import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;

import com.jagex.game.runetek5.framebuffer.FrameBuffer;

import jaggl.OpenGL;

/**
 * 
 * @author Steven - 23/08/2023
 *
 */
public class MipmapRectangleTextureSampler extends MipmapTextureSampler implements RectangleTextureSampler {

    public final int width;
    public final int height;

    public MipmapRectangleTextureSampler(OpenGLNativeToolkit toolkit, GLFormat class1502, int width, int height, float[] floats_5) {
        super(toolkit, GL_TEXTURE_RECTANGLE, class1502, GLActionFormat.FLOAT_32_ARB, width * height, false);
        this.width = width;
        this.height = height;
        super.activeToolkit.setTextureSampler(this);
        OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
        OpenGL.glTexImage2Df(super.textureTarget, 0, OpenGLNativeToolkit.texture2dInternalFormat(super.pixelFormat, super.pixelActionFormat),
        		width, height, 0, OpenGLNativeToolkit.texture2dFormat(super.pixelFormat), 5126, floats_5, 0);
        OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
    }

    public MipmapRectangleTextureSampler(OpenGLNativeToolkit toolkit, GLFormat format, GLActionFormat actionFormat, int width, int height) {
        super(toolkit, GL_TEXTURE_RECTANGLE, format, actionFormat, height * width, false);
        this.width = width;
        this.height = height;
        super.activeToolkit.setTextureSampler(this);
        OpenGL.glTexImage2Dub(super.textureTarget, 0, OpenGLNativeToolkit.texture2dInternalFormat(super.pixelFormat, super.pixelActionFormat),
        		width, height, 0, OpenGLNativeToolkit.texture2dFormat(super.pixelFormat), OpenGLNativeToolkit.actionFormatDataType(super.pixelActionFormat), null, 0);
    }

    public MipmapRectangleTextureSampler(OpenGLNativeToolkit toolkit, int width, int height, int[] ints_4, int i_5, int i_6) {
        super(toolkit, GL_TEXTURE_RECTANGLE, GLFormat.HDR, GLActionFormat.PIXEL_FORMAT, height * width, false);
        this.width = width;
        this.height = height;
        super.activeToolkit.setTextureSampler(this);
        OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, i_6);
        OpenGL.glTexImage2Di(super.textureTarget, 0, GL_RGBA, width, height, 0, GL_BGRA, super.activeToolkit.pixelType, ints_4, i_5 * 4);
        OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
    }

    public MipmapRectangleTextureSampler(OpenGLNativeToolkit toolkit, GLFormat format, int width, int height, byte[] data) {
        super(toolkit, GL_TEXTURE_RECTANGLE, format, GLActionFormat.PIXEL_FORMAT, width * height, false);
        this.width = width;
        this.height = height;
        super.activeToolkit.setTextureSampler(this);
        OpenGL.glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
        OpenGL.glTexImage2Dub(super.textureTarget, 0, OpenGLNativeToolkit.texture2dInternalFormat(super.pixelFormat, super.pixelActionFormat),
        		width, height, 0, OpenGLNativeToolkit.texture2dFormat(super.pixelFormat), GL_UNSIGNED_BYTE, data, 0);
        OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
        OpenGL.glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
    }

    @Override
    public float method47(float f_1) {
        return f_1;
    }

    @Override
    public int getTextureWidth() {
        return this.width;
    }

    @Override
    public int getTextureHeight() {
        return this.height;
    }

    @Override
    public float method62(float f_1) {
        return f_1;
    }

    @Override
    public void method48(int i_1, int i_2, int i_3, int i_4, int[] data, int offset, int rows) {
        int rowLength = rows;
        if (rowLength == 0) {
            rowLength = i_3;
        }

        super.activeToolkit.setTextureSampler(this);
        if (i_3 != rowLength) {
            OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, rowLength);
        }

        OpenGL.glTexSubImage2Di(super.textureTarget, 0, i_1, i_2, i_3, i_4, GL_BGRA, super.activeToolkit.pixelType, data, offset);
        if (i_3 != rowLength) {
            OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
        }

    }

    @Override
    public boolean method46() {
        return false;
    }

    @Override
    public void setRepeat(boolean repeatS, boolean repeatT) {
    	// NOOP
    }

    @Override
    public void method52(int offsetX, int offsetY, int width, int height, byte[] data, GLFormat class1506, int offset, int i_8) {
        int i_81 = i_8;
        if (i_81 == 0) {
            i_81 = width;
        }

        super.activeToolkit.setTextureSampler(this);
        OpenGL.glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        if (width != i_81) {
            OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, i_81);
        }

        OpenGL.glTexSubImage2Dub(super.textureTarget, 0, offsetX, offsetY, width, height, OpenGLNativeToolkit.texture2dFormat(class1506), GL_UNSIGNED_BYTE, data, offset);
        if (width != i_81) {
            OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
        }

        OpenGL.glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
    }

    @Override
    public boolean enableMipmapping() {
        return super.enableMipmapping();
    }

    @Override
    public FrameBuffer getFramebuffer() {
        return new FrameBufferRectangleTexture(this, 0);
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
    public void method53(int length, int i_4, int[] dest) {
        int[] src = new int[this.width * this.height];
        super.activeToolkit.setTextureSampler(this);
        OpenGL.glGetTexImagei(super.textureTarget, 0, GL_BGRA, GL_UNSIGNED_BYTE, src, 0);

        for (int i_8 = 0; i_8 < i_4; i_8++) {
        	int srcPos = this.width * ((i_4 - 1) - i_8);
        	int dstPos = length * i_8;
            System.arraycopy(src, srcPos, dest, dstPos, length);
        }

    }

    @Override
    public void bindTexture() {
        super.bindTexture();
    }
}
