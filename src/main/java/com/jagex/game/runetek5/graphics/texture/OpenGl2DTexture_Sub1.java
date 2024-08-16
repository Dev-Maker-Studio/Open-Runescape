package com.jagex.game.runetek5.graphics.texture;

import com.jagex.game.runetek5.toolkit.OpenGlToolkit;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;
import com.jagex.game.runetek5.toolkit.opengl.GLFormat;
import com.jagex.game.runetek5.utils.MathUtils;
import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;

public class OpenGl2DTexture_Sub1 extends OpenGl2DTexture {

	public final int singleWidth;
	public final int singleLength;
	public final float t;
	public final float s;
	public final boolean tiled;
	public final boolean mipmapNearest;

	public OpenGl2DTexture_Sub1(OpenGlToolkit toolkit, GLFormat class1502, GLActionFormat class763, int i_4,
                                int i_5, int width, int legth) {
		super(toolkit, GL_TEXTURE_2D, class1502, class763, width, legth);
		this.singleWidth = i_4;
		this.singleLength = i_5;
		this.t = (float) i_5 / legth;
		this.s = (float) i_4 / width;
		this.tiled = false;
		this.mipmapNearest = true;
		setRepeat(false, false);
	}

	public OpenGl2DTexture_Sub1(OpenGlToolkit toolkit, GLFormat class1502, GLActionFormat class763, int i_4,
                                int i_5, int i_6, int i_7, byte[] bytes_8, GLFormat class1509) {
		super(toolkit, GL_TEXTURE_2D, class1502, class763, i_6, i_7);
		this.singleWidth = i_4;
		this.singleLength = i_5;
		uploadTextureData(i_7 - i_5, i_4, i_5, bytes_8, class1509, true);
		this.t = (float) i_5 / i_7;
		this.s = (float) i_4 / i_6;
		this.tiled = false;
		this.mipmapNearest = true;
		setRepeat(false, false);
	}

	public OpenGl2DTexture_Sub1(OpenGlToolkit toolkit, int textureTarget, GLFormat format, GLActionFormat actionFormat,
                                int width, int length) {
		super(toolkit, textureTarget, format, actionFormat, width, length);
		this.singleWidth = width;
		this.singleLength = length;
		if (textureTarget == GL_TEXTURE_RECTANGLE) {
			this.t = length;
			this.s = width;
			this.tiled = false;
		} else {
			this.t = 1.0F;
			this.s = 1.0F;
			this.tiled = true;
		}

		this.mipmapNearest = false;
	}

	public OpenGl2DTexture_Sub1(OpenGlToolkit toolkit, int target, int x, int y, int width, int length) {
		super(toolkit, target, x, y, width, length);
		this.singleWidth = width;
		this.singleLength = length;
		if (super.textureTarget == GL_TEXTURE_RECTANGLE) {
			this.t = length;
			this.s = width;
			this.tiled = false;
		} else {
			this.t = 1.0F;
			this.s = 1.0F;
			this.tiled = true;
		}

		this.mipmapNearest = false;
	}

	public OpenGl2DTexture_Sub1(OpenGlToolkit toolkit, int width, int length, int totalWidth, int totalLength, int[] ints_6) {
		super(toolkit, GL_TEXTURE_2D, GLFormat.HDR, GLActionFormat.PIXEL_FORMAT, totalWidth, totalLength);
		this.singleWidth = width;
		this.singleLength = length;
		updateTextureRegion(0, totalLength - length, width, length, ints_6, 0);
		this.t = (float) length / totalLength;
		this.s = (float) width / totalWidth;
		this.tiled = false;
		this.mipmapNearest = true;
		setRepeat(false, false);
	}

	public OpenGl2DTexture_Sub1(OpenGlToolkit toolkit, int x, int y, int i_4, int i_5, int width, int length) {
		super(toolkit, GL_TEXTURE_2D, x, y, width, length);
		this.singleWidth = i_4;
		this.singleLength = i_5;
		this.t = (float) i_5 / length;
		this.s = (float) i_4 / width;
		this.tiled = false;
		this.mipmapNearest = true;
		setRepeat(false, false);
	}

	public OpenGl2DTexture_Sub1(OpenGlToolkit toolkit, int i_2, int i_3, int i_4, int[] ints_6, int i_7, int i_8) {
		super(toolkit, i_2, i_3, i_4, false, ints_6, i_7, i_8, true);
		this.singleWidth = i_3;
		this.singleLength = i_4;
		if (super.textureTarget == GL_TEXTURE_RECTANGLE) {
			this.t = i_4;
			this.s = i_3;
			this.tiled = false;
		} else {
			this.t = 1.0F;
			this.s = 1.0F;
			this.tiled = true;
		}

		this.mipmapNearest = false;
	}

	public OpenGl2DTexture_Sub1(OpenGlToolkit toolkit, int target, GLFormat class1503, GLActionFormat class764,
                                int t, int s, byte[] pixels, GLFormat class1509) {
		super(toolkit, target, class1503, class764, t, s, false, pixels, class1509, true);
		this.singleWidth = t;
		this.singleLength = s;
		if (this.textureTarget == GL_TEXTURE_RECTANGLE) {
			this.t = s;
			this.s = t;
			this.tiled = false;
		} else {
			this.t = 1.0F;
			this.s = 1.0F;
			this.tiled = true;
		}

		this.mipmapNearest = false;
	}

	public static OpenGl2DTexture_Sub1 method15537(OpenGlToolkit toolkit, int i_1, int i_2, int[] ints_4, int i_5, int i_6) {
        if (!toolkit.aBool8498 && (!OpenGlTexture.isPowerOfTwo(i_1) || !OpenGlTexture.isPowerOfTwo(i_2))) {
            if (toolkit.arbRectangleTexturesSupported)
                return new OpenGl2DTexture_Sub1(toolkit, GL_TEXTURE_RECTANGLE, i_1, i_2, ints_4, i_5, i_6);
            return new OpenGl2DTexture_Sub1(toolkit, i_1, i_2, MathUtils.nextPowerOfTwo(i_1),
                    MathUtils.nextPowerOfTwo(i_2), ints_4);
        }
        return new OpenGl2DTexture_Sub1(toolkit, GL_TEXTURE_2D, i_1, i_2, ints_4, i_5, i_6);
    }

	public static OpenGl2DTexture_Sub1 method15538(OpenGlToolkit toolkit, int x, int y, int width, int length) {
        if (!toolkit.aBool8498 && (!OpenGlTexture.isPowerOfTwo(width) || !OpenGlTexture.isPowerOfTwo(length))) {
            if (toolkit.arbRectangleTexturesSupported) {
                return new OpenGl2DTexture_Sub1(toolkit, GL_TEXTURE_RECTANGLE, x, y, width, length);
            }
            return new OpenGl2DTexture_Sub1(toolkit, x, y, width, length, MathUtils.nextPowerOfTwo(width), MathUtils.nextPowerOfTwo(length));
        }
        return new OpenGl2DTexture_Sub1(toolkit, GL_TEXTURE_2D, x, y, width, length);
    }

	public static OpenGl2DTexture_Sub1 createSurface(OpenGlToolkit toolkit, GLFormat class1501,
                                                     GLActionFormat class762, int i_3, int i_4, byte[] bytes_6, GLFormat class1507) {
        if (!toolkit.aBool8498
                && (!OpenGlTexture.isPowerOfTwo(i_3) || !OpenGlTexture.isPowerOfTwo(i_4))) {
            if (toolkit.arbRectangleTexturesSupported)
                return new OpenGl2DTexture_Sub1(toolkit, GL_TEXTURE_RECTANGLE, class1501, class762, i_3, i_4,
                        bytes_6, class1507);
            return new OpenGl2DTexture_Sub1(toolkit, class1501, class762, i_3, i_4,
                    MathUtils.nextPowerOfTwo(i_3), MathUtils.nextPowerOfTwo(i_4), bytes_6, class1507);
        }
        return new OpenGl2DTexture_Sub1(toolkit, GL_TEXTURE_2D, class1501, class762, i_3, i_4,
                bytes_6, class1507);
    }

	public static OpenGl2DTexture_Sub1 method15547(OpenGlToolkit toolkit, GLFormat class1501,
                                                   GLActionFormat class762, int i_3, int i_4) {
        if (!toolkit.aBool8498
                && (!OpenGlTexture.isPowerOfTwo(i_3) || !OpenGlTexture.isPowerOfTwo(i_4))) {
            if (toolkit.arbRectangleTexturesSupported)
                return new OpenGl2DTexture_Sub1(toolkit, GL_TEXTURE_RECTANGLE, class1501, class762, i_3, i_4);
            return new OpenGl2DTexture_Sub1(toolkit, class1501, class762, i_3, i_4,
                    MathUtils.nextPowerOfTwo(i_3), MathUtils.nextPowerOfTwo(i_4));
        }
        return new OpenGl2DTexture_Sub1(toolkit, GL_TEXTURE_2D, class1501, class762, i_3, i_4);
    }

	@Override
	public void setLinearTextureFilter(boolean linear) {
		super.setLinearTextureFilter(linear && !this.mipmapNearest);
	}

}
