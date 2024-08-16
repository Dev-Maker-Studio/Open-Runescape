package com.jagex.game.runetek5.config.material.texture.operation;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.utils.ArrayUtils;

public class Class149_Sub1 extends Class149 {

    public final int anInt9274;
    public final int anInt9273;
    public final int anInt9276;
    public final int anInt9275;

    public Class149_Sub1(int i_1, int i_2, int i_3, int i_4, int i_5, int i_6, int i_7) {
        super(i_5, i_6, i_7);
        this.anInt9274 = i_1;
        this.anInt9273 = i_2;
        this.anInt9276 = i_3;
        this.anInt9275 = i_4;
    }

    public static Class149_Sub1 decode(Packet rsbytebuffer0) {
        return new Class149_Sub1(rsbytebuffer0.readShort(), rsbytebuffer0.readShort(), rsbytebuffer0.readShort(), rsbytebuffer0.readShort(), rsbytebuffer0.read24BitUnsignedInteger(), rsbytebuffer0.read24BitUnsignedInteger(), rsbytebuffer0.readUnsignedByte());
    }

    @Override
    public void method2561(int i_1, int i_2) {
    }

    @Override
    public void method2556(int i_1, int i_2) {
        int i_4 = this.anInt9274 * i_1 >> 12;
        int i_5 = this.anInt9276 * i_1 >> 12;
        int i_6 = i_2 * this.anInt9273 >> 12;
        int i_7 = i_2 * this.anInt9275 >> 12;
        ArrayUtils.method14584(i_4, i_6, i_5, i_7, super.anInt1743);
    }

    @Override
    public void method2557(int i_1, int i_2) {
        int i_4 = this.anInt9274 * i_1 >> 12;
        int i_5 = this.anInt9276 * i_1 >> 12;
        int i_6 = i_2 * this.anInt9273 >> 12;
        int i_7 = i_2 * this.anInt9275 >> 12;
        Class149_Sub1.method5316(i_4, i_6, i_5, i_7, super.anInt1743, super.anInt1741, super.anInt1742);
    }

	public static void method6824(int i_0, int i_1, int i_2, int i_3, int i_4, int i_5, int i_6) {
	    int i_8 = 0;
	    int i_9 = i_3;
	    int i_10 = 0;
	    int i_11 = i_2 - i_6;
	    int i_12 = i_3 - i_6;
	    int i_13 = i_2 * i_2;
	    int i_14 = i_3 * i_3;
	    int i_15 = i_11 * i_11;
	    int i_16 = i_12 * i_12;
	    int i_17 = i_14 << 1;
	    int i_18 = i_13 << 1;
	    int i_19 = i_16 << 1;
	    int i_20 = i_15 << 1;
	    int i_21 = i_3 << 1;
	    int i_22 = i_12 << 1;
	    int i_23 = i_17 + (1 - i_21) * i_13;
	    int i_24 = i_14 - i_18 * (i_21 - 1);
	    int i_25 = i_19 + (1 - i_22) * i_15;
	    int i_26 = i_16 - i_20 * (i_22 - 1);
	    int i_27 = i_13 << 2;
	    int i_28 = i_14 << 2;
	    int i_29 = i_15 << 2;
	    int i_30 = i_16 << 2;
	    int i_31 = i_17 * 3;
	    int i_32 = i_18 * (i_21 - 3);
	    int i_33 = i_19 * 3;
	    int i_34 = i_20 * (i_22 - 3);
	    int i_35 = i_28;
	    int i_36 = (i_3 - 1) * i_27;
	    int i_37 = i_30;
	    int i_38 = i_29 * (i_12 - 1);
	    int i_40;
	    int i_41;
	    int i_42;
	    int i_43;
	    if (i_1 >= TextureOpRasterizer.anInt7070 && i_1 <= TextureOpRasterizer.anInt7068) {
	        int[] ints_39 = Class149.anIntArrayArray7072[i_1];
	        i_40 = ArrayUtils.method4890(i_0 - i_2, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	        i_41 = ArrayUtils.method4890(i_0 + i_2, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	        i_42 = ArrayUtils.method4890(i_0 - i_11, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	        i_43 = ArrayUtils.method4890(i_0 + i_11, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	        ArrayUtils.setArrayValuesTo(ints_39, i_40, i_42, i_5);
	        ArrayUtils.setArrayValuesTo(ints_39, i_42, i_43, i_4);
	        ArrayUtils.setArrayValuesTo(ints_39, i_43, i_41, i_5);
	    }
	
	    while (i_9 > 0) {
	        boolean bool47 = i_9 <= i_12;
	        if (bool47) {
	            if (i_25 < 0) {
	                while (i_25 < 0) {
	                    i_25 += i_33;
	                    i_26 += i_37;
	                    i_33 += i_30;
	                    i_37 += i_30;
	                    ++i_10;
	                }
	            }
	
	            if (i_26 < 0) {
	                i_25 += i_33;
	                i_26 += i_37;
	                i_33 += i_30;
	                i_37 += i_30;
	                ++i_10;
	            }
	
	            i_25 += -i_38;
	            i_26 += -i_34;
	            i_34 -= i_29;
	            i_38 -= i_29;
	        }
	
	        if (i_23 < 0) {
	            while (i_23 < 0) {
	                i_23 += i_31;
	                i_24 += i_35;
	                i_31 += i_28;
	                i_35 += i_28;
	                ++i_8;
	            }
	        }
	
	        if (i_24 < 0) {
	            i_23 += i_31;
	            i_24 += i_35;
	            i_31 += i_28;
	            i_35 += i_28;
	            ++i_8;
	        }
	
	        i_23 += -i_36;
	        i_24 += -i_32;
	        i_32 -= i_27;
	        i_36 -= i_27;
	        --i_9;
	        i_40 = i_1 - i_9;
	        i_41 = i_9 + i_1;
	        if (i_41 >= TextureOpRasterizer.anInt7070 && i_40 <= TextureOpRasterizer.anInt7068) {
	            i_42 = ArrayUtils.method4890(i_0 + i_8, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	            i_43 = ArrayUtils.method4890(i_0 - i_8, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	            if (bool47) {
	                int i_44 = ArrayUtils.method4890(i_0 + i_10, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	                int i_45 = ArrayUtils.method4890(i_0 - i_10, TextureOpRasterizer.anInt7071, TextureOpRasterizer.anInt7069);
	                int[] ints_46;
	                if (i_40 >= TextureOpRasterizer.anInt7070) {
	                    ints_46 = Class149.anIntArrayArray7072[i_40];
	                    ArrayUtils.setArrayValuesTo(ints_46, i_43, i_45, i_5);
	                    ArrayUtils.setArrayValuesTo(ints_46, i_45, i_44, i_4);
	                    ArrayUtils.setArrayValuesTo(ints_46, i_44, i_42, i_5);
	                }
	
	                if (i_41 <= TextureOpRasterizer.anInt7068) {
	                    ints_46 = Class149.anIntArrayArray7072[i_41];
	                    ArrayUtils.setArrayValuesTo(ints_46, i_43, i_45, i_5);
	                    ArrayUtils.setArrayValuesTo(ints_46, i_45, i_44, i_4);
	                    ArrayUtils.setArrayValuesTo(ints_46, i_44, i_42, i_5);
	                }
	            } else {
	                if (i_40 >= TextureOpRasterizer.anInt7070) {
	                    ArrayUtils.setArrayValuesTo(Class149.anIntArrayArray7072[i_40], i_43, i_42, i_5);
	                }
	
	                if (i_41 <= TextureOpRasterizer.anInt7068) {
	                    ArrayUtils.setArrayValuesTo(Class149.anIntArrayArray7072[i_41], i_43, i_42, i_5);
	                }
	            }
	        }
	    }
	
	}

	public static void method15241(int i_0, int i_1, int i_2, int i_3, int i_4, int i_5, int i_6) {
	    int i_8 = 0;
	    int i_9 = i_3;
	    int i_10 = 0;
	    int i_11 = i_2 - i_6;
	    int i_12 = i_3 - i_6;
	    int i_13 = i_2 * i_2;
	    int i_14 = i_3 * i_3;
	    int i_15 = i_11 * i_11;
	    int i_16 = i_12 * i_12;
	    int i_17 = i_14 << 1;
	    int i_18 = i_13 << 1;
	    int i_19 = i_16 << 1;
	    int i_20 = i_15 << 1;
	    int i_21 = i_3 << 1;
	    int i_22 = i_12 << 1;
	    int i_23 = i_17 + (1 - i_21) * i_13;
	    int i_24 = i_14 - i_18 * (i_21 - 1);
	    int i_25 = i_19 + (1 - i_22) * i_15;
	    int i_26 = i_16 - i_20 * (i_22 - 1);
	    int i_27 = i_13 << 2;
	    int i_28 = i_14 << 2;
	    int i_29 = i_15 << 2;
	    int i_30 = i_16 << 2;
	    int i_31 = i_17 * 3;
	    int i_32 = i_18 * (i_21 - 3);
	    int i_33 = i_19 * 3;
	    int i_34 = i_20 * (i_22 - 3);
	    int i_35 = i_28;
	    int i_36 = (i_3 - 1) * i_27;
	    int i_37 = i_30;
	    int i_38 = i_29 * (i_12 - 1);
	    int[] ints_39 = Class149.anIntArrayArray7072[i_1];
	    ArrayUtils.setArrayValuesTo(ints_39, i_0 - i_2, i_0 - i_11, i_5);
	    ArrayUtils.setArrayValuesTo(ints_39, i_0 - i_11, i_0 + i_11, i_4);
	    ArrayUtils.setArrayValuesTo(ints_39, i_0 + i_11, i_0 + i_2, i_5);
	
	    while (i_9 > 0) {
	        boolean bool40 = i_9 <= i_12;
	        if (bool40) {
	            if (i_25 < 0) {
	                while (i_25 < 0) {
	                    i_25 += i_33;
	                    i_26 += i_37;
	                    i_33 += i_30;
	                    i_37 += i_30;
	                    ++i_10;
	                }
	            }
	
	            if (i_26 < 0) {
	                i_25 += i_33;
	                i_26 += i_37;
	                i_33 += i_30;
	                i_37 += i_30;
	                ++i_10;
	            }
	
	            i_25 += -i_38;
	            i_26 += -i_34;
	            i_34 -= i_29;
	            i_38 -= i_29;
	        }
	
	        if (i_23 < 0) {
	            while (i_23 < 0) {
	                i_23 += i_31;
	                i_24 += i_35;
	                i_31 += i_28;
	                i_35 += i_28;
	                ++i_8;
	            }
	        }
	
	        if (i_24 < 0) {
	            i_23 += i_31;
	            i_24 += i_35;
	            i_31 += i_28;
	            i_35 += i_28;
	            ++i_8;
	        }
	
	        i_23 += -i_36;
	        i_24 += -i_32;
	        i_32 -= i_27;
	        i_36 -= i_27;
	        --i_9;
	        int i_41 = i_1 - i_9;
	        int i_42 = i_9 + i_1;
	        int i_43 = i_0 + i_8;
	        int i_44 = i_0 - i_8;
	        if (bool40) {
	            int i_45 = i_0 + i_10;
	            int i_46 = i_0 - i_10;
	            ArrayUtils.setArrayValuesTo(Class149.anIntArrayArray7072[i_41], i_44, i_46, i_5);
	            ArrayUtils.setArrayValuesTo(Class149.anIntArrayArray7072[i_41], i_46, i_45, i_4);
	            ArrayUtils.setArrayValuesTo(Class149.anIntArrayArray7072[i_41], i_45, i_43, i_5);
	            ArrayUtils.setArrayValuesTo(Class149.anIntArrayArray7072[i_42], i_44, i_46, i_5);
	            ArrayUtils.setArrayValuesTo(Class149.anIntArrayArray7072[i_42], i_46, i_45, i_4);
	            ArrayUtils.setArrayValuesTo(Class149.anIntArrayArray7072[i_42], i_45, i_43, i_5);
	        } else {
	            ArrayUtils.setArrayValuesTo(Class149.anIntArrayArray7072[i_41], i_44, i_43, i_5);
	            ArrayUtils.setArrayValuesTo(Class149.anIntArrayArray7072[i_42], i_44, i_43, i_5);
	        }
	    }
	
	}

	public static void method12838(int i_0, int i_1, int i_2, int i_3, int i_4, int i_5) {
	    if (i_0 - i_2 >= TextureOpRasterizer.anInt7071 && i_0 + i_2 <= TextureOpRasterizer.anInt7069 && i_1 - i_2 >= TextureOpRasterizer.anInt7070 && i_2 + i_1 <= TextureOpRasterizer.anInt7068) {
	        ArrayUtils.method2637(i_0, i_1, i_2, i_3, i_4, i_5);
	    } else {
	        ArrayUtils.method1174(i_0, i_1, i_2, i_3, i_4, i_5);
	    }
	}

	public static void method5316(int i_0, int i_1, int i_2, int i_3, int i_4, int i_5, int i_6) {
	    if (i_3 == i_2) {
	        Class149_Sub1.method12838(i_0, i_1, i_2, i_4, i_5, i_6);
	    } else if (i_0 - i_2 >= TextureOpRasterizer.anInt7071 && i_0 + i_2 <= TextureOpRasterizer.anInt7069 && i_1 - i_3 >= TextureOpRasterizer.anInt7070 && i_3 + i_1 <= TextureOpRasterizer.anInt7068) {
	        Class149_Sub1.method15241(i_0, i_1, i_2, i_3, i_4, i_5, i_6);
	    } else {
	        Class149_Sub1.method6824(i_0, i_1, i_2, i_3, i_4, i_5, i_6);
	    }
	}

}
