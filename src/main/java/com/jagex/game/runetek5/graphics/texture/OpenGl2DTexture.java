package com.jagex.game.runetek5.graphics.texture;

import com.jagex.game.runetek5.framebuffer.FrameBuffer;
import com.jagex.game.runetek5.framebuffer.RenderBuffer;
import com.jagex.game.runetek5.imageproducer.AbstractDisplay;
import com.jagex.game.runetek5.toolkit.OpenGlToolkit;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;
import com.jagex.game.runetek5.toolkit.opengl.GLFormat;
import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;
import jaggl.OpenGL;

/**
 * 
 * @author Steven - 16/09/2023
 *
 */
public class OpenGl2DTexture extends OpenGlTexture {

	public final int width;
	public final int length;

	public OpenGl2DTexture(OpenGlToolkit toolkit, int textureTarget, int width, int height, boolean mipmapping, int[] pixels, int rowLength, int size, boolean flipVertical) {
        super(toolkit, textureTarget, GLFormat.HDR, GLActionFormat.PIXEL_FORMAT, width * height, mipmapping);
        this.width = width;
        this.length = height;
        if (flipVertical) {
            int[] modifiedPixels = new int[pixels.length];

            for (int row = 0; row < height; row++) {
                int dst = width * row;
                int src = width * (height - row - 1);

                for (int col = 0; col < width; col++) {
                    modifiedPixels[dst++] = pixels[src++];
                }
            }

            pixels = modifiedPixels;
        }

        this.activeToolkit.setActiveTexture(this);
        if (this.textureTarget != GL_TEXTURE_RECTANGLE && mipmapping && rowLength == 0 && size == 0) {
            createMipmappedCubeTexture(this.textureTarget, OpenGlToolkit.getInternalTextureFormat(this.aClass1501650, this.aClass761651), this.width, this.length, this.activeToolkit.pixelType, pixels);
            setMipmapping(true);
        } else {
            OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, rowLength);
            OpenGL.glTexImage2Di(this.textureTarget, 0, OpenGlToolkit.getInternalTextureFormat(this.aClass1501650, this.aClass761651), this.width, this.length, 0, GL_BGRA, this.activeToolkit.pixelType, pixels, size * 4);
            OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
            setMipmapping(false);
        }

        setLinearTextureFilter(true);
    }

	public OpenGl2DTexture(OpenGlToolkit toolkit, int tt, GLFormat class1503, GLActionFormat class764, int width, int height, boolean mipmapping, byte[] pixels, GLFormat class1509, boolean flipVertical) {
        super(toolkit, tt, class1503, class764, width * height, mipmapping);
        this.width = width;
        this.length = height;
        if (flipVertical) {
            byte[] modifiedPixels = new byte[pixels.length];
            for (int row = 0; row < height; row++) {
                int src = width * row;
                int dst = width * (height - row - 1);
                for (int col = 0; col < width; col++) {
                    modifiedPixels[src++] = pixels[dst++];
                }
            }
            pixels = modifiedPixels;
        }
        this.activeToolkit.setActiveTexture(this);
        OpenGL.glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        if (mipmapping && this.textureTarget != GL_TEXTURE_RECTANGLE) {
            method2359(tt, OpenGlToolkit.getInternalTextureFormat(this.aClass1501650, this.aClass761651), width, height, class1509, pixels);
            setMipmapping(true);
        } else {
            OpenGL.glTexImage2Dub(this.textureTarget, 0, OpenGlToolkit.getInternalTextureFormat(this.aClass1501650, this.aClass761651), this.width, this.length, 0, OpenGlToolkit.format(class1509), GL_UNSIGNED_BYTE, pixels, 0);
            setMipmapping(false);
        }

        OpenGL.glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
        setLinearTextureFilter(true);
    }

	public OpenGl2DTexture(OpenGlToolkit toolkit, int tt, int x, int y, int width, int length) {
        super(toolkit, tt, GLFormat.aClass1501950, GLActionFormat.PIXEL_FORMAT, width * length, false);
        this.width = width;
        this.length = length;
        this.activeToolkit.setActiveTexture(this);
        AbstractDisplay display = this.activeToolkit.getDisplay();
        if (display != null) {
            int adjustedY = display.getHeight() - (y + length);
            int internalFormat = OpenGlToolkit.getInternalTextureFormat(this.aClass1501650, this.aClass761651);
            OpenGL.glCopyTexImage2D(super.textureTarget, 0, internalFormat, x, adjustedY, width, length, 0);
        }

        setLinearTextureFilter(true);
    }

	public OpenGl2DTexture(OpenGlToolkit toolkit, GLFormat class1503, GLActionFormat class764, int width, int height, boolean mipmapping, float[] pixels, GLFormat class1509) {
        super(toolkit, GL_TEXTURE_2D, class1503, class764, width * height, mipmapping);
        this.width = width;
        this.length = height;
        this.activeToolkit.setActiveTexture(this);
        if (mipmapping && super.textureTarget != GL_TEXTURE_RECTANGLE) {
            generateMipmaps(OpenGlToolkit.getInternalTextureFormat(this.aClass1501650, this.aClass761651), width, height, class1509, pixels);
            setMipmapping(true);
        } else {
            OpenGL.glTexImage2Df(this.textureTarget, 0, OpenGlToolkit.getInternalTextureFormat(this.aClass1501650, this.aClass761651), this.width, this.length, 0, OpenGlToolkit.format(class1509), 5126, pixels, 0);
            setMipmapping(false);
        }
        setLinearTextureFilter(true);
    }

    public OpenGl2DTexture(OpenGlToolkit toolkit, int textureTarget, GLFormat format, GLActionFormat action, int width, int length) {
        super(toolkit, textureTarget, format, action, width * length, false);
        this.width = width;
        this.length = length;
        this.activeToolkit.setActiveTexture(this);
        OpenGL.glTexImage2Dub(this.textureTarget, 0, OpenGlToolkit.getInternalTextureFormat(this.aClass1501650, this.aClass761651), width, length, 0, OpenGlToolkit.format(this.aClass1501650), GL_UNSIGNED_BYTE, null, 0);
        setLinearTextureFilter(true);
    }

    public void copyTextureSubImage(int offsetX, int offsetY, int width, int height, int x, int y) {
        AbstractDisplay display = this.activeToolkit.getDisplay();
        if (display != null) {
            int adjustedY = display.getHeight() - (height + y);
            this.activeToolkit.setActiveTexture(this);
            OpenGL.glCopyTexSubImage2D(this.textureTarget, 0, offsetX, this.length - (offsetY + height), x, adjustedY, width, height);
            OpenGL.glFlush();
        }
    }

    public RenderBuffer createRenderBuffer() {
        return new FramebufferTexture2DEXT(this);
    }

    public FrameBuffer createFrameBuffer() {
        return new FramebufferTexture2DEXT(this);
    }

    public void setRepeat(boolean repeatS, boolean repeatT) {
        if (this.textureTarget == GL_TEXTURE_2D) {
        	this.activeToolkit.setActiveTexture(this);
            OpenGL.glTexParameteri(super.textureTarget, GL_TEXTURE_WRAP_S, repeatS ? GL_REPEAT : GL_CLAMP_TO_EDGE);
            OpenGL.glTexParameteri(super.textureTarget, GL_TEXTURE_WRAP_T, repeatT ? GL_REPEAT : GL_CLAMP_TO_EDGE);
        }
    }

    public void updateTextureRegion(int x, int y, int width, int height, int[] originalPixels, int rowLength) {
        int effectiveRowLength = rowLength;
        if (effectiveRowLength == 0) {
            effectiveRowLength = width;
        }
        int[] modifiedPixels = new int[width * height];
        for (int row = 0; row < height; row++) {
            int dst = width * row;
            int src = (height - row - 1) * effectiveRowLength;
            for (int col = 0; col < width; col++) {
                modifiedPixels[dst++] = originalPixels[src++];
            }
        }
        this.activeToolkit.setActiveTexture(this);
        if (width != effectiveRowLength) {
            OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, effectiveRowLength);
        }
        OpenGL.glTexSubImage2Di(this.textureTarget, 0, x, this.length - y - height, width, height, GL_BGRA, this.activeToolkit.pixelType, modifiedPixels, 0);
        if (width != effectiveRowLength) {
            OpenGL.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
        }
    }

    public void uploadTextureData(int offsetX, int offsetY, int width, byte[] data, GLFormat format, boolean flipVertical) {
        if (flipVertical) {
            int bytesPerPixel = format.bytesPerPixel;
            int rowSize = offsetY * bytesPerPixel;
            int flippedRowSize = bytesPerPixel * offsetY;
            byte[] modifiedData = new byte[rowSize * width];
            for (int row = 0; row < width; row++) {
                int originalRowIndex = row * rowSize;
                int flippedRowIndex = flippedRowSize * (width - row - 1);
                for (int col = 0; col < rowSize; col++) {
                    modifiedData[originalRowIndex++] = data[flippedRowIndex++];
                }
            }
            data = modifiedData;
        }
        this.activeToolkit.setActiveTexture(this);
        OpenGL.glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        OpenGL.glTexSubImage2Dub(this.textureTarget, 0, 0, this.length - offsetX - width, offsetY, width, OpenGlToolkit.format(format), GL_UNSIGNED_BYTE, data, 0);
        OpenGL.glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
    }

}
