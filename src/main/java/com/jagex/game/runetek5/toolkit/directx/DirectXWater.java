package com.jagex.game.runetek5.toolkit.directx;

import jaclib.memory.Stream;
import jaclib.memory.heap.NativeHeapBuffer;

import java.nio.ByteBuffer;

import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.config.material.MaterialType;
import com.jagex.game.runetek5.scene.ground.GroundDX;
import com.jagex.game.runetek5.scene.tile.WaterTile;
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit;
import com.jagex.game.runetek5.toolkit.companion.NativeStream;

public class DirectXWater extends Node {

	public int anInt7508;
	public int anInt7514;
	public int anInt7519;
	public int anInt7517;
    public final GroundDX directxGround;
    public final AbstractNativeToolkit nativeToolkit;
    public final int textureId;
    public final float waterScale;
    public final WaterTile hdWaterTile;
    public final int[] anIntArray7515;
    public NativeStream directxWaterBuffer;
    public NativeHeapBuffer nativeHeap;
    public Stream buffer;

    public DirectXWater(GroundDX dxground, int textureId, int waterScale, WaterTile waterTile) {
        this.directxGround = dxground;
        this.nativeToolkit = this.directxGround.activeToolkit;
        this.textureId = textureId;
        this.waterScale = waterScale;
        this.hdWaterTile = waterTile;
        this.anIntArray7515 = new int[this.directxGround.width * this.directxGround.length];
        byte effect = 10;
        if (textureId >= 0) {
            MaterialType material = this.nativeToolkit.materialProvider.getMaterial(textureId);
            if (material.effectId > 0) {
                effect = material.effectId;
            }
        }

        this.nativeToolkit.getShaderByEffect(effect);
        this.nativeToolkit.getShaderByEffect(3);
    }

    public void method12143(int i_1) {
        this.buffer.method2919(i_1 * 4 + 3);
        this.buffer.putByte(-1);
    }

    public void method12145(int i_1, int colorRgb, int intensity, float transparency) {
        if (this.textureId != -1) {
            MaterialType material = this.nativeToolkit.materialProvider.getMaterial(this.textureId);
            int alpha = material.alpha & 0xff;
            int baseColor;
            if (alpha != 0 && material.effectId != 4) {
                if (intensity < 0) {
                    baseColor = 0;
                } else if (intensity > 127) {
                    baseColor = 16777215;
                } else {
                    baseColor = intensity * 131586;
                }

                int i_9 = 256 - alpha;
                colorRgb = ((baseColor & 0xff00ff) * alpha + i_9 * (colorRgb & 0xff00ff) & -16711936) + (i_9 * (colorRgb & 0xff00) + alpha * (baseColor & 0xff00) & 0xff0000) >> 8;
            }

            int brightness = material.brightness & 0xff;
            if (brightness != 0) {
                brightness += 256;
                int red = ((colorRgb & 0xff0000) >> 16) * brightness;
                if (red > 65535) {
                    red = 65535;
                }

                int green = ((colorRgb & 0xff00) >> 8) * brightness;
                if (green > 65535) {
                    green = 65535;
                }

                int blue = brightness * (colorRgb & 0xff);
                if (blue > 65535) {
                    blue = 65535;
                }

                colorRgb = (blue >> 8) + (green & 0xff00) + (red << 8 & 0xff0000);
            }
        }

        if (transparency != 1.0F) {
            int red = colorRgb >> 16 & 0xff;
            int green = colorRgb >> 8 & 0xff;
            int blue = colorRgb & 0xff;
            red = (int) (red * transparency);
            if (red < 0) {
                red = 0;
            } else if (red > 255) {
                red = 255;
            }

            green = (int) (green * transparency);
            if (green < 0) {
                green = 0;
            } else if (green > 255) {
                green = 255;
            }

            blue = (int) (blue * transparency);
            if (blue < 0) {
                blue = 0;
            } else if (blue > 255) {
                blue = 255;
            }

            colorRgb = red << 16 | green << 8 | blue;
        }

        this.buffer.method2919(i_1 * 4);
        if (this.nativeToolkit.endiannessType == 0) {
            this.buffer.putByte((byte) colorRgb);
            this.buffer.putByte((byte) (colorRgb >> 8));
            this.buffer.putByte((byte) (colorRgb >> 16));
        } else {
            this.buffer.putByte((byte) (colorRgb >> 16));
            this.buffer.putByte((byte) (colorRgb >> 8));
            this.buffer.putByte((byte) colorRgb);
        }

    }

    public void method12146(int i_1) {
        this.buffer.method2925();
        this.directxWaterBuffer = this.nativeToolkit.createStream(false);
        this.directxWaterBuffer.createFromSource(i_1 * 4, 4, this.nativeHeap);
        this.nativeHeap = null;
        this.buffer = null;
    }

    public void method12147(int[] ints_1, int i_2) {
        this.anInt7514 = 0;
        this.anInt7519 = 32767;
        this.anInt7517 = -32768;
        ByteBuffer bytebuffer3 = this.nativeToolkit.directBuffer;

        label49:
        for (int i_4 = 0; i_4 < i_2; i_4++) {
            int i_5 = ints_1[i_4];
            short[] shorts_6 = this.directxGround.tileVertices[i_5];
            int i_7 = this.anIntArray7515[i_5];
            if (i_7 != 0 && shorts_6 != null) {
                int i_8 = 0;
                int i_9 = 0;

                while (true) {
                    while (true) {
                        if (i_9 >= shorts_6.length) {
                            continue label49;
                        }

                        if ((i_7 & 1 << i_8++) != 0) {
                            for (int i_10 = 0; i_10 < 3; i_10++) {
                                int i_11 = shorts_6[i_9++] & 0xffff;
                                if (i_11 > this.anInt7517) {
                                    this.anInt7517 = i_11;
                                }

                                if (i_11 < this.anInt7519) {
                                    this.anInt7519 = i_11;
                                }

                                bytebuffer3.putShort((short) i_11);
                            }

                            this.anInt7514 += 3;
                        } else {
                            i_9 += 3;
                        }
                    }
                }
            }
        }

    }

    public void method12150(int i_1) {
        this.nativeHeap = this.nativeToolkit.generateNativeHeapBuffer(i_1 * 4, true);
        this.buffer = new Stream(this.nativeHeap, 0, i_1 * 4);
    }

    public void method12152(int i_1, int i_2, int i_3) {
        this.anIntArray7515[i_2 * this.directxGround.width + i_1] |= 1 << i_3;
        ++this.anInt7508;
    }

}
