package com.jagex.game.runetek5.config.material.texture.operation;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.staticmethods.StaticMethods;
import com.jagex.game.runetek5.utils.ArrayUtils;

public class Class149_Sub4 extends Class149 {

    public final int anInt9391;

    public final int anInt9393;

    public final int anInt9392;

    public final int anInt9394;

    public Class149_Sub4(int i_1, int i_2, int i_3, int i_4, int i_5, int i_6, int i_7) {
        super(i_5, i_6, i_7);
        this.anInt9391 = i_1;
        this.anInt9393 = i_2;
        this.anInt9392 = i_3;
        this.anInt9394 = i_4;
    }

    public static Class149_Sub4 decode(Packet rsbytebuffer0) {
        return new Class149_Sub4(rsbytebuffer0.readShort(), rsbytebuffer0.readShort(), rsbytebuffer0.readShort(), rsbytebuffer0.readShort(), rsbytebuffer0.read24BitUnsignedInteger(), rsbytebuffer0.read24BitUnsignedInteger(), rsbytebuffer0.readUnsignedByte());
    }

    @Override
    public void method2561(int i_1, int i_2) {
        int i_4 = this.anInt9391 * i_1 >> 12;
        int i_5 = this.anInt9392 * i_1 >> 12;
        int i_6 = i_2 * this.anInt9393 >> 12;
        int i_7 = i_2 * this.anInt9394 >> 12;
        Class149_Sub4.method1805(i_4, i_5, i_6, i_7, super.anInt1741, super.anInt1742);
    }

    @Override
    public void method2556(int i_1, int i_2) {
        int i_4 = this.anInt9391 * i_1 >> 12;
        int i_5 = this.anInt9392 * i_1 >> 12;
        int i_6 = i_2 * this.anInt9393 >> 12;
        int i_7 = i_2 * this.anInt9394 >> 12;
        StaticMethods.method6772(i_4, i_5, i_6, i_7, super.anInt1743);
    }

    @Override
    public void method2557(int i_1, int i_2) {
        int i_4 = this.anInt9391 * i_1 >> 12;
        int i_5 = this.anInt9392 * i_1 >> 12;
        int i_6 = i_2 * this.anInt9393 >> 12;
        int i_7 = i_2 * this.anInt9394 >> 12;
        StaticMethods.method6731(i_4, i_5, i_6, i_7, super.anInt1743, super.anInt1741, super.anInt1742);
    }

	public static void method4561(int i_0, int i_1, int i_2, int i_3, int i_4, int i_5) {
	    int i_7 = i_5 + i_2;
	    int i_8 = i_3 - i_5;
	    int i_9;
	    for (i_9 = i_2; i_9 < i_7; i_9++) {
	        ArrayUtils.setArrayValuesTo(Class149.anIntArrayArray7072[i_9], i_0, i_1, i_4);
	    }
	    for (i_9 = i_3; i_9 > i_8; --i_9) {
	        ArrayUtils.setArrayValuesTo(Class149.anIntArrayArray7072[i_9], i_0, i_1, i_4);
	    }
	    i_9 = i_5 + i_0;
	    int i_10 = i_1 - i_5;
	    for (int i_11 = i_7; i_11 <= i_8; i_11++) {
	        int[] ints_12 = Class149.anIntArrayArray7072[i_11];
	        ArrayUtils.setArrayValuesTo(ints_12, i_0, i_9, i_4);
	        ArrayUtils.setArrayValuesTo(ints_12, i_10, i_1, i_4);
	    }
	}

	public static void method1388(int i_0, int i_1, int i_2, int i_3, int i_4) {
	    int i_01 = i_0;
	    int i_11 = i_1;
	    int i_21 = i_2;
	    int i_31 = i_3;
	    if (i_21 <= TextureOpRasterizer.anInt7068 && i_31 >= TextureOpRasterizer.anInt7070) {
	        boolean bool6;
	        if (i_01 < TextureOpRasterizer.anInt7071) {
	            i_01 = TextureOpRasterizer.anInt7071;
	            bool6 = false;
	        } else if (i_01 > TextureOpRasterizer.anInt7069) {
	            i_01 = TextureOpRasterizer.anInt7069;
	            bool6 = false;
	        } else {
	            bool6 = true;
	        }
	        boolean bool7;
	        if (i_11 < TextureOpRasterizer.anInt7071) {
	            i_11 = TextureOpRasterizer.anInt7071;
	            bool7 = false;
	        } else if (i_11 > TextureOpRasterizer.anInt7069) {
	            i_11 = TextureOpRasterizer.anInt7069;
	            bool7 = false;
	        } else {
	            bool7 = true;
	        }
	        if (i_21 >= TextureOpRasterizer.anInt7070) {
	            ArrayUtils.setArrayValuesTo(Class149.anIntArrayArray7072[i_21++], i_01, i_11, i_4);
	        } else {
	            i_21 = TextureOpRasterizer.anInt7070;
	        }
	        if (i_31 <= TextureOpRasterizer.anInt7068) {
	            ArrayUtils.setArrayValuesTo(Class149.anIntArrayArray7072[i_31--], i_01, i_11, i_4);
	        } else {
	            i_31 = TextureOpRasterizer.anInt7068;
	        }
	        int i_8;
	        if (bool6 && bool7) {
	            for (i_8 = i_21; i_8 <= i_31; i_8++) {
	                int[] ints_9 = Class149.anIntArrayArray7072[i_8];
	                ints_9[i_01] = ints_9[i_11] = i_4;
	            }
	        } else if (bool6) {
	            for (i_8 = i_21; i_8 <= i_31; i_8++) {
	                Class149.anIntArrayArray7072[i_8][i_01] = i_4;
	            }
	        } else if (bool7) {
	            for (i_8 = i_21; i_8 <= i_31; i_8++) {
	                Class149.anIntArrayArray7072[i_8][i_11] = i_4;
	            }
	        }
	    }
	}

	public static void method1805(int i_0, int i_1, int i_2, int i_3, int i_4, int i_5) {
	    if (i_0 >= TextureOpRasterizer.anInt7071 && i_1 <= TextureOpRasterizer.anInt7069 && i_2 >= TextureOpRasterizer.anInt7070 && i_3 <= TextureOpRasterizer.anInt7068) {
	        if (i_5 == 1) {
	            StaticMethods.method14572(i_0, i_1, i_2, i_3, i_4);
	        } else {
	            Class149_Sub4.method4561(i_0, i_1, i_2, i_3, i_4, i_5);
	        }
	    } else if (i_5 == 1) {
	        Class149_Sub4.method1388(i_0, i_1, i_2, i_3, i_4);
	    } else {
	        ArrayUtils.method744(i_0, i_1, i_2, i_3, i_4, i_5);
	    }
	}

}
