package com.jagex.game.runetek5.unidentified;

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface;
import com.jagex.game.runetek5.graphics.texture.sampler.NativeTextureSampler;
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;
import com.jagex.game.runetek5.toolkit.opengl.GLFormat;

public class Class455_Sub1 extends ComponentSurface {

    public final NativeTextureSampler nativeTextureSampler;

    public Class455_Sub1(AbstractNativeToolkit nativeToolkit, int i_2, int i_3, byte[] bytes_4) {
        this.nativeTextureSampler = nativeToolkit.createTextureSampler(GLFormat.RGBA, i_2, i_3, false, bytes_4);
        this.nativeTextureSampler.setRepeat(false, false);
    }

    public Class455_Sub1(AbstractNativeToolkit nativeToolkit, int width, int length, int[] pixels) {
        this.nativeTextureSampler = nativeToolkit.createTexture(width, length, false, pixels);
        this.nativeTextureSampler.setRepeat(false, false);
    }

    public static Class455_Sub1 method13769(AbstractNativeToolkit nativeToolkit, int width, int length, int[] ints_3, int[] ints_4) {
        if (nativeToolkit.method13954(GLFormat.RGBA, GLActionFormat.PIXEL_FORMAT)) {
            byte[] bytes_9 = new byte[length * width];

            for (int i_6 = 0; i_6 < length; i_6++) {
                int i_7 = ints_3[i_6] + i_6 * width;

                for (int i_8 = 0; i_8 < ints_4[i_6]; i_8++) {
                    bytes_9[i_7++] = -1;
                }
            }

            return new Class455_Sub1(nativeToolkit, width, length, bytes_9);
        } else {
            int[] pixels = new int[length * width];

            for (int index = 0; index < length; index++) {
            	int pixelPtr = ints_3[index] + index * width;

                for (int i_8 = 0; i_8 < ints_4[index]; i_8++) {
                    pixels[pixelPtr++] = ~0xffffff;
                }
            }

            return new Class455_Sub1(nativeToolkit, width, length, pixels);
        }
    }

}
