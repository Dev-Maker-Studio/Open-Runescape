package com.jagex.game.runetek5.graphics.texture.water;

import com.jagex.game.runetek5.buffer.DataBuffer;
import com.jagex.game.runetek5.graphics.shader.Texture3DShaderGL;
import com.jagex.game.runetek5.graphics.texture.OpenGl2DTexture;
import com.jagex.game.runetek5.graphics.texture.OpenGl3DTexture;
import com.jagex.game.runetek5.toolkit.OpenGlToolkit;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;
import com.jagex.game.runetek5.toolkit.opengl.GLFormat;

public class TextureContainerGL {

    public static Object anObject1637;
    public static Object anObject1627;
    public static Object databufferHdrTexture;
    public OpenGl2DTexture[] textures2d;
    public OpenGl3DTexture texture3d;
    public OpenGl2DTexture[] aClass137Sub1Array1634;
    public OpenGl3DTexture aClass137Sub41636;
    public OpenGl3DTexture hdrTexture3d;
    public final boolean texture3dSupported;

    public TextureContainerGL(OpenGlToolkit openglToolkit) {
        this.texture3dSupported = openglToolkit.texture3dSupported;
        method2348(openglToolkit);
        if (!this.texture3dSupported) {
            this.textures2d = new OpenGl2DTexture[16];

            byte[] bytes_3;
            for (int index = 0; index < 16; index++) {
                bytes_3 = DataBuffer.readDataBuffer(anObject1637, index * 32768);
                this.textures2d[index] = new OpenGl2DTexture(openglToolkit, 3553, GLFormat.aClass1501952,
                		GLActionFormat.PIXEL_FORMAT, 128, 128, true, bytes_3, GLFormat.aClass1501952, false);
            }

            this.aClass137Sub1Array1634 = new OpenGl2DTexture[16];

            for (int index = 0; index < 16; index++) {
                bytes_3 = DataBuffer.readDataBuffer(anObject1627, index * 32768);
                this.aClass137Sub1Array1634[index] = new OpenGl2DTexture(openglToolkit, 3553, GLFormat.aClass1501952,
                		GLActionFormat.PIXEL_FORMAT, 128, 128, true, bytes_3, GLFormat.aClass1501952, false);
            }
        } else {
            byte[] buffer = DataBuffer.unpackDataBuffer(anObject1637, false);
            this.texture3d = new OpenGl3DTexture(openglToolkit, GLFormat.aClass1501952, GLActionFormat.PIXEL_FORMAT, buffer, GLFormat.aClass1501952);
            buffer = DataBuffer.unpackDataBuffer(anObject1627, false);
            this.aClass137Sub41636 = new OpenGl3DTexture(openglToolkit, GLFormat.aClass1501952, GLActionFormat.PIXEL_FORMAT, buffer, GLFormat.aClass1501952);
            Texture3DShaderGL class1646 = openglToolkit.aClass1648363;
            if (class1646.createShader()) {
                buffer = DataBuffer.unpackDataBuffer(databufferHdrTexture, false);
                this.hdrTexture3d = new OpenGl3DTexture(openglToolkit, GLFormat.HDR, GLActionFormat.PIXEL_FORMAT);
                OpenGl3DTexture texture3d = new OpenGl3DTexture(openglToolkit, GLFormat.aClass1501948, GLActionFormat.PIXEL_FORMAT, buffer, GLFormat.aClass1501948);
                if (!class1646.activate(this.hdrTexture3d, texture3d)) {
                    this.hdrTexture3d.dispose();
                    this.hdrTexture3d = null;
                } else {
                    this.hdrTexture3d.generateMipmaps();
                }

                texture3d.dispose();
            }
        }
    }

    public static void method2348(OpenGlToolkit openglToolkit) {
        byte[] buffer;
        if (anObject1637 == null) {
            Class370_Sub1_Sub2 class370Sub1Sub21 = new Class370_Sub1_Sub2();
            buffer = class370Sub1Sub21.method15614();
            anObject1637 = DataBuffer.createDataBuffer(buffer);
        }

        if (anObject1627 == null) {
            Class370_Sub2_Sub2 class370Sub2Sub23 = new Class370_Sub2_Sub2();
            buffer = class370Sub2Sub23.getData();
            anObject1627 = DataBuffer.createDataBuffer(buffer);
        }

        Texture3DShaderGL class1644 = openglToolkit.aClass1648363;
        if (class1644.createShader() && databufferHdrTexture == null) {
            buffer = Class264.method4782(new Class261_Sub1());
            databufferHdrTexture = DataBuffer.createDataBuffer(buffer);
        }
    }
}
