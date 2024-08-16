package com.jagex.game.runetek5.config.material.texture.operation;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.utils.ArrayUtils;

public class Class149_Sub2 extends Class149 {

    public final int anInt9306;
    public final int anInt9307;
    public final int anInt9305;
    public final int anInt9309;
    public final int anInt9308;
    public final int anInt9311;
    public final int anInt9312;
    public final int anInt9313;

    Class149_Sub2(int i_1, int i_2, int i_3, int i_4, int i_5, int i_6, int i_7, int i_8, int i_9, int i_10) {
        super(-1, i_9, i_10);
        this.anInt9306 = i_1;
        this.anInt9307 = i_2;
        this.anInt9305 = i_3;
        this.anInt9309 = i_4;
        this.anInt9308 = i_5;
        this.anInt9311 = i_6;
        this.anInt9312 = i_7;
        this.anInt9313 = i_8;
    }

    @Override
    public void method2556(int i_1, int i_2) {
    }

    @Override
    public void method2557(int i_1, int i_2) {
    }

    @Override
    public void method2561(int i_1, int i_2) {
        int i_4 = this.anInt9306 * i_1 >> 12;
        int i_5 = i_2 * this.anInt9307 >> 12;
        int i_6 = this.anInt9305 * i_1 >> 12;
        int i_7 = i_2 * this.anInt9309 >> 12;
        int i_8 = this.anInt9308 * i_1 >> 12;
        int i_9 = i_2 * this.anInt9311 >> 12;
        int i_10 = this.anInt9312 * i_1 >> 12;
        int i_11 = i_2 * this.anInt9313 >> 12;
        method12399(i_4, i_5, i_6, i_7, i_8, i_9, i_10, i_11, super.anInt1741);
    }
    
    public static void method11220(int i_0, int i_1, int i_2, int i_3, int i_4) {
	    int i_01 = i_0;
	    int i_21 = i_2;
	    int i_15 = i_1;
	    int i_31 = i_3;
	    int i_6 = i_31 - i_15;
	    int i_7 = i_21 - i_01;
	    if (i_7 == 0) {
	        if (i_6 != 0) {
	            Class149.method13411(i_01, i_15, i_31, i_4);
	        }
	    } else if (i_6 == 0) {
	        ArrayUtils.method568(i_01, i_21, i_15, i_4);
	    } else {
	        if (i_6 < 0) {
	            i_6 = -i_6;
	        }
	        if (i_7 < 0) {
	            i_7 = -i_7;
	        }
	        boolean bool8 = i_6 > i_7;
	        int i_9;
	        int i_10;
	        if (bool8) {
	            i_9 = i_01;
	            i_10 = i_21;
	            i_01 = i_15;
	            i_15 = i_9;
	            i_21 = i_31;
	            i_31 = i_10;
	        }
	        if (i_01 > i_21) {
	            i_9 = i_01;
	            i_10 = i_15;
	            i_01 = i_21;
	            i_21 = i_9;
	            i_15 = i_31;
	            i_31 = i_10;
	        }
	        i_9 = i_15;
	        i_10 = i_21 - i_01;
	        int i_11 = i_31 - i_15;
	        int i_12 = -(i_10 >> 1);
	        int i_13 = i_15 < i_31 ? 1 : -1;
	        if (i_11 < 0) {
	            i_11 = -i_11;
	        }
	        int i_14;
	        if (bool8) {
	            for (i_14 = i_01; i_14 <= i_21; i_14++) {
	                Class149.anIntArrayArray7072[i_14][i_9] = i_4;
	                i_12 += i_11;
	                if (i_12 > 0) {
	                    i_9 += i_13;
	                    i_12 -= i_10;
	                }
	            }
	        } else {
	            for (i_14 = i_01; i_14 <= i_21; i_14++) {
	                Class149.anIntArrayArray7072[i_9][i_14] = i_4;
	                i_12 += i_11;
	                if (i_12 > 0) {
	                    i_9 += i_13;
	                    i_12 -= i_10;
	                }
	            }
	        }
	    }
	}

	public static void method12117(int i_0, int i_1, int i_2, int i_3, int i_4, int i_5, int i_6, int i_7, int i_8) {
	    if (i_0 == i_2 && i_3 == i_1 && i_4 == i_6 && i_5 == i_7) {
	        Class149_Sub2.method11220(i_0, i_1, i_6, i_7, i_8);
	    } else {
	        int i_10 = i_0;
	        int i_11 = i_1;
	        int i_12 = i_0 * 3;
	        int i_13 = i_1 * 3;
	        int i_14 = i_2 * 3;
	        int i_15 = i_3 * 3;
	        int i_16 = i_4 * 3;
	        int i_17 = i_5 * 3;
	        int i_18 = i_6 - i_16 + i_14 - i_0;
	        int i_19 = i_15 + (i_7 - i_17) - i_1;
	        int i_20 = i_12 + (i_16 - i_14 - i_14);
	        int i_21 = i_13 + (i_17 - i_15 - i_15);
	        int i_22 = i_14 - i_12;
	        int i_23 = i_15 - i_13;
	        for (int i_24 = 128; i_24 <= 4096; i_24 += 128) {
	            int i_25 = i_24 * i_24 >> 12;
	            int i_26 = i_25 * i_24 >> 12;
	            int i_27 = i_18 * i_26;
	            int i_28 = i_26 * i_19;
	            int i_29 = i_20 * i_25;
	            int i_30 = i_25 * i_21;
	            int i_31 = i_24 * i_22;
	            int i_32 = i_24 * i_23;
	            int i_33 = i_0 + (i_29 + i_27 + i_31 >> 12);
	            int i_34 = (i_28 + i_30 + i_32 >> 12) + i_1;
	            Class149_Sub2.method11220(i_10, i_11, i_33, i_34, i_8);
	            i_10 = i_33;
	            i_11 = i_34;
	        }
	    }
	}

	public static void method4779(int i_0, int i_1, int i_2, int i_3, int i_4, int i_5, int i_6, int i_7, int i_8) {
	    if (i_0 == i_2 && i_3 == i_1 && i_4 == i_6 && i_5 == i_7) {
	        Class149.method6159(i_0, i_1, i_6, i_7, i_8);
	    } else {
	        int i_10 = i_0;
	        int i_11 = i_1;
	        int i_12 = i_0 * 3;
	        int i_13 = i_1 * 3;
	        int i_14 = i_2 * 3;
	        int i_15 = i_3 * 3;
	        int i_16 = i_4 * 3;
	        int i_17 = i_5 * 3;
	        int i_18 = i_6 - i_16 + i_14 - i_0;
	        int i_19 = i_15 + (i_7 - i_17) - i_1;
	        int i_20 = i_12 + (i_16 - i_14 - i_14);
	        int i_21 = i_13 + (i_17 - i_15 - i_15);
	        int i_22 = i_14 - i_12;
	        int i_23 = i_15 - i_13;
	        for (int i_24 = 128; i_24 <= 4096; i_24 += 128) {
	            int i_25 = i_24 * i_24 >> 12;
	            int i_26 = i_24 * i_25 >> 12;
	            int i_27 = i_18 * i_26;
	            int i_28 = i_19 * i_26;
	            int i_29 = i_25 * i_20;
	            int i_30 = i_21 * i_25;
	            int i_31 = i_22 * i_24;
	            int i_32 = i_24 * i_23;
	            int i_33 = i_0 + (i_29 + i_27 + i_31 >> 12);
	            int i_34 = (i_30 + i_28 + i_32 >> 12) + i_1;
	            Class149.method6159(i_10, i_11, i_33, i_34, i_8);
	            i_10 = i_33;
	            i_11 = i_34;
	        }
	    }
	}

	public static Class149_Sub2 decode(Packet rsbytebuffer0) {
	    return new Class149_Sub2(rsbytebuffer0.readShort(), rsbytebuffer0.readShort(), rsbytebuffer0.readShort(), rsbytebuffer0.readShort(), rsbytebuffer0.readShort(), rsbytebuffer0.readShort(), rsbytebuffer0.readShort(), rsbytebuffer0.readShort(), rsbytebuffer0.read24BitUnsignedInteger(), rsbytebuffer0.readUnsignedByte());
	}

	public static void method12399(int i_0, int i_1, int i_2, int i_3, int i_4, int i_5, int i_6, int i_7, int i_8) {
        if (i_0 >= TextureOpRasterizer.anInt7071 && i_0 <= TextureOpRasterizer.anInt7069 && i_2 >= TextureOpRasterizer.anInt7071 && i_2 <= TextureOpRasterizer.anInt7069 && i_4 >= TextureOpRasterizer.anInt7071 && i_4 <= TextureOpRasterizer.anInt7069 && i_6 >= TextureOpRasterizer.anInt7071 && i_6 <= TextureOpRasterizer.anInt7069 && i_1 >= TextureOpRasterizer.anInt7070 && i_1 <= TextureOpRasterizer.anInt7068 && i_3 >= TextureOpRasterizer.anInt7070 && i_3 <= TextureOpRasterizer.anInt7068 && i_5 >= TextureOpRasterizer.anInt7070 && i_5 <= TextureOpRasterizer.anInt7068 && i_7 >= TextureOpRasterizer.anInt7070 && i_7 <= TextureOpRasterizer.anInt7068) {
            Class149_Sub2.method12117(i_0, i_1, i_2, i_3, i_4, i_5, i_6, i_7, i_8);
        } else {
            Class149_Sub2.method4779(i_0, i_1, i_2, i_3, i_4, i_5, i_6, i_7, i_8);
        }
    }

}
