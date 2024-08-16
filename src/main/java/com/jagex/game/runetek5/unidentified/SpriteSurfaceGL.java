package com.jagex.game.runetek5.unidentified;

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface;
import com.jagex.game.runetek5.graphics.texture.OpenGl2DTexture_Sub1;
import com.jagex.game.runetek5.toolkit.OpenGlToolkit;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;
import com.jagex.game.runetek5.toolkit.opengl.GLFormat;

public class SpriteSurfaceGL extends ComponentSurface {

    public final OpenGl2DTexture_Sub1 surface;

    public SpriteSurfaceGL(OpenGlToolkit class505Sub11, int width, int length, byte[] pixels) {
        this.surface = OpenGl2DTexture_Sub1.createSurface(class505Sub11, GLFormat.RGBA, GLActionFormat.PIXEL_FORMAT, width, length, pixels, GLFormat.RGBA);
        this.surface.setRepeat(false, false);
    }

    public static SpriteSurfaceGL create(OpenGlToolkit toolkit, int width, int length, int[] rowStartX, int[] rowWidth) {
        byte[] pixels = new byte[length * width];
        for (int y = 0; y < length; y++) {
            int startX = rowStartX[y] + y * width;
            for (int i = 0; i < rowWidth[y]; i++) {
                pixels[startX++] = -1;
            }
        }
        return new SpriteSurfaceGL(toolkit, width, length, pixels);
    }

}
