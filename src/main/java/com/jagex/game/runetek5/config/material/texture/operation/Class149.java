package com.jagex.game.runetek5.config.material.texture.operation;

import com.jagex.game.runetek5.utils.ArrayUtils;

public abstract class Class149 {

    public final int anInt1743;

    public final int anInt1741;

    final int anInt1742;

	public static int[][] anIntArrayArray7072;

    Class149(int i_1, int i_2, int i_3) {
        this.anInt1743 = i_1;
        this.anInt1741 = i_2;
        this.anInt1742 = i_3;
    }

    public abstract void method2556(int var1, int var2);

    public abstract void method2557(int var1, int var2);

    public abstract void method2561(int var1, int var2);

	public static void method13411(int i_0, int i_1, int i_2, int i_3) {
	    int i_5;
	    if (i_1 > i_2) {
	        for (i_5 = i_2; i_5 < i_1; i_5++) {
	            Class149.anIntArrayArray7072[i_5][i_0] = i_3;
	        }
	    } else {
	        for (i_5 = i_1; i_5 < i_2; i_5++) {
	            Class149.anIntArrayArray7072[i_5][i_0] = i_3;
	        }
	    }
	}

	public static void method12746(int i_0, int i_1, int i_2, int i_3) {
	    if (i_0 >= TextureOpRasterizer.anInt7071 && i_0 <= TextureOpRasterizer.anInt7069) {
	        i_1 = ArrayUtils.method4890(i_1, TextureOpRasterizer.anInt7070, TextureOpRasterizer.anInt7068);
	        i_2 = ArrayUtils.method4890(i_2, TextureOpRasterizer.anInt7070, TextureOpRasterizer.anInt7068);
	        Class149.method13411(i_0, i_1, i_2, i_3);
	    }
	
	}

	public static void method6159(int i_0, int i_1, int i_2, int i_3, int i_4) {
	    int i_6 = i_2 - i_0;
	    int i_7 = i_3 - i_1;
	    if (i_6 == 0) {
	        if (i_7 != 0) {
	            Class149.method12746(i_0, i_1, i_3, i_4);
	        }
	    } else if (i_7 == 0) {
	        ArrayUtils.method11250(i_0, i_2, i_1, i_4);
	    } else {
	        int i_8 = (i_7 << 12) / i_6;
	        int i_9 = i_1 - (i_0 * i_8 >> 12);
	        int i_10;
	        int i_11;
	        if (i_0 < TextureOpRasterizer.anInt7071) {
	            i_10 = (i_8 * TextureOpRasterizer.anInt7071 >> 12) + i_9;
	            i_11 = TextureOpRasterizer.anInt7071;
	        } else if (i_0 > TextureOpRasterizer.anInt7069) {
	            i_10 = (i_8 * TextureOpRasterizer.anInt7069 >> 12) + i_9;
	            i_11 = TextureOpRasterizer.anInt7069;
	        } else {
	            i_10 = i_1;
	            i_11 = i_0;
	        }
	        int i_12;
	        int i_13;
	        if (i_2 < TextureOpRasterizer.anInt7071) {
	            i_12 = (i_8 * TextureOpRasterizer.anInt7071 >> 12) + i_9;
	            i_13 = TextureOpRasterizer.anInt7071;
	        } else if (i_2 > TextureOpRasterizer.anInt7069) {
	            i_12 = (i_8 * TextureOpRasterizer.anInt7069 >> 12) + i_9;
	            i_13 = TextureOpRasterizer.anInt7069;
	        } else {
	            i_12 = i_3;
	            i_13 = i_2;
	        }
	        if (i_10 < TextureOpRasterizer.anInt7070) {
	            i_10 = TextureOpRasterizer.anInt7070;
	            i_11 = (TextureOpRasterizer.anInt7070 - i_9 << 12) / i_8;
	        } else if (i_10 > TextureOpRasterizer.anInt7068) {
	            i_10 = TextureOpRasterizer.anInt7068;
	            i_11 = (TextureOpRasterizer.anInt7068 - i_9 << 12) / i_8;
	        }
	        if (i_12 < TextureOpRasterizer.anInt7070) {
	            i_12 = TextureOpRasterizer.anInt7070;
	            i_13 = (TextureOpRasterizer.anInt7070 - i_9 << 12) / i_8;
	        } else if (i_12 > TextureOpRasterizer.anInt7068) {
	            i_12 = TextureOpRasterizer.anInt7068;
	            i_13 = (TextureOpRasterizer.anInt7068 - i_9 << 12) / i_8;
	        }
	        Class149_Sub2.method11220(i_11, i_10, i_13, i_12, i_4);
	    }
	}

}
