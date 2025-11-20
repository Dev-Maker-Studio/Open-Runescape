package com.jagex.game.runetek5.graphics.texture.water;

import com.jagex.game.runetek5.buffer.DataBuffer;
import com.jagex.game.runetek5.graphics.texture.sampler.NativeTextureSampler;
import com.jagex.game.runetek5.graphics.texture.sampler.Texture3DSampler;
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;
import com.jagex.game.runetek5.toolkit.opengl.GLFormat;

public class NativeWaterTexture {

    public static Object anObject482;
    public static Object anObject477;
    public static Object hdrDatabuffer;
    public Texture3DSampler normalSampler;
    public NativeTextureSampler[] textureSamplers;
    public Texture3DSampler billowSampler;
    public boolean hasDepth;
    public NativeTextureSampler[] waterTextureSamplers;
    public Texture3DSampler waterSampler3d;
    public final AbstractNativeToolkit activeToolkit;

    public NativeWaterTexture(AbstractNativeToolkit toolkit) {
        this.activeToolkit = toolkit;
        this.hasDepth = this.activeToolkit.SUPPORTS_TEXTURE3D;
        if (this.hasDepth && !this.activeToolkit.method13955(GLFormat.aClass1501952, GLActionFormat.PIXEL_FORMAT)) {
            this.hasDepth = false;
        }

        if (this.hasDepth || this.activeToolkit.method13954(GLFormat.aClass1501952, GLActionFormat.PIXEL_FORMAT)) {
            method968();
            if (!this.hasDepth) {
                this.waterTextureSamplers = new NativeTextureSampler[16];

                byte[] data;
                for (int index = 0; index < 16; index++) {
                    data = DataBuffer.readDataBuffer(anObject482, index * 32768);
                    this.waterTextureSamplers[index] = this.activeToolkit.createTextureSampler(GLFormat.aClass1501952, 128, 128, true, data);
                }

                this.textureSamplers = new NativeTextureSampler[16];

                for (int index = 0; index < 16; index++) {
                    data = DataBuffer.readDataBuffer(anObject477, index * 32768);
                    this.textureSamplers[index] = this.activeToolkit.createTextureSampler(GLFormat.aClass1501952, 128, 128, true, data);
                }
            } else {
                byte[] bytes_4 = DataBuffer.unpackDataBuffer(anObject482, false);
                this.waterSampler3d = this.activeToolkit.method13962(GLFormat.aClass1501952, bytes_4);
                bytes_4 = DataBuffer.unpackDataBuffer(anObject477, false);
                this.billowSampler = this.activeToolkit.method13962(GLFormat.aClass1501952, bytes_4);
            }
        }

    }

    public static void method968() {
        byte[] DATA;
        if (anObject482 == null) {
            Class370_Sub1_Sub1 class370Sub1Sub10 = new Class370_Sub1_Sub1();
            DATA = class370Sub1Sub10.getData();
            anObject482 = DataBuffer.createDataBuffer(DATA);
        }

        if (anObject477 == null) {
            Class370_Sub2_Sub1 class370Sub2Sub12 = new Class370_Sub2_Sub1();
            DATA = class370Sub2Sub12.getData();
            anObject477 = DataBuffer.createDataBuffer(DATA);
        }

    }

    public boolean loadSampledTexture() {
        if (!this.activeToolkit.SUPPORTS_TEXTURE3D) {
            return false;
        } 
        if (this.normalSampler == null) {
            byte[] bytes_1;
            if (hdrDatabuffer == null) {
                bytes_1 = PerlinWaterTextureGenerator.method4782(new PerlinNoiseGenerator());
                hdrDatabuffer = DataBuffer.createDataBuffer(bytes_1);
            }

            bytes_1 = DataBuffer.unpackDataBuffer(hdrDatabuffer, false);
            byte[] bytes_2 = new byte[bytes_1.length * 4];
            int i_3 = 0;

            for (int i_4 = 0; i_4 < 16; i_4++) {
                int i_5 = i_4 * 16384;
                int i_6 = i_5;

                for (int i_7 = 0; i_7 < 128; i_7++) {
                    int i_8 = i_6 + i_7 * 128;
                    int i_9 = i_6 + (i_7 - 1 & 0x7f) * 128;
                    int i_10 = (i_7 + 1 & 0x7f) * 128 + i_6;

                    for (int i_11 = 0; i_11 < 128; i_11++) {
                        float f_12 = ((bytes_1[i_9 + i_11] & 0xff) - (bytes_1[i_10 + i_11] & 0xff));
                        float f_13 = ((bytes_1[i_8 + (i_11 - 1 & 0x7f)] & 0xff) - (bytes_1[(i_11 + 1 & 0x7f) + i_8] & 0xff));
                        float f_14 = (float) (128.0D / Math.sqrt(f_13 * f_13 + 16384.0F + f_12 * f_12));
                        bytes_2[i_3++] = (byte) ((int) (f_13 * f_14 + 127.0F));
                        bytes_2[i_3++] = (byte) ((int) (128.0F * f_14 + 127.0F));
                        bytes_2[i_3++] = (byte) ((int) (f_12 * f_14 + 127.0F));
                        bytes_2[i_3++] = bytes_1[i_5++];
                    }
                }
            }

            this.normalSampler = this.activeToolkit.method13962(GLFormat.HDR, bytes_2);
        }

        return this.normalSampler != null;
    }
    
	/*public boolean method969() {
        if (!aGraphicalRenderer_Sub2_481.aBool8827) {
            return false;
		if (anInterface1_480 == null) {
			if (anObject483 == null) {
				byte[] is = PerlinWaterTextureGenerator.method4782(128, 128, 16, 8, new PerlinNoiseGenerator(419684), 4.0F, 4.0F, 16.0F, 0.5F, 0.6F);
				anObject483 = DataBuffer.create_data_buffer(is, false, (byte) 40);
			}
			byte[] is = Class346.method6154(anObject483, false, (byte) 1);
			byte[] is_0_ = new byte[is.length * 4];
			int i = 0;
			for (int i_1_ = 0; i_1_ < 16; i_1_++) {
				int i_2_ = i_1_ * 16384;
				int i_3_ = i_2_;
				for (int i_4_ = 0; i_4_ < 128; i_4_++) {
					int i_5_ = i_3_ + i_4_ * 128;
					int i_6_ = i_3_ + (i_4_ - 1 & 0x7f) * 128;
					int i_7_ = i_3_ + (i_4_ + 1 & 0x7f) * 128;
					for (int i_8_ = 0; i_8_ < 128; i_8_++) {
						float f = (float) ((is[i_6_ + i_8_] & 0xff) - (is[i_7_ + i_8_] & 0xff));
						float f_9_ = (float) ((is[i_5_ + (i_8_ - 1 & 0x7f)] & 0xff) - (is[i_5_ + (i_8_ + 1 & 0x7f)] & 0xff));
						float f_10_ = (float) (128.0 / Math.sqrt((double) (f_9_ * f_9_ + 16384.0F + f * f)));
						is_0_[i++] = (byte) (int) (f_9_ * f_10_ + 127.0F);
						is_0_[i++] = (byte) (int) (128.0F * f_10_ + 127.0F);
						is_0_[i++] = (byte) (int) (f * f_10_ + 127.0F);
						is_0_[i++] = is[i_2_++];
					}
				}
			}
			anInterface1_480 = aGraphicalRenderer_Sub2_481.method13962(GLFormat.HDR, 128, 128, 16, true, is_0_);
		}
		return anInterface1_480 != null;
	}*/

    public boolean readyToSample() {
        return this.hasDepth ? this.waterSampler3d != null : this.waterTextureSamplers != null;
    }

}
