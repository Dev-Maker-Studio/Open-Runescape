package com.jagex.game.runetek5.sound.jagtheora;

import com.jagex.game.runetek5.sound.player.PcmPlayerManager;

public class AudioBufferStream extends AreaSoundStream {

    AudioBufferStream(AudioBuffer buffer, int i_2, int volume, int i_4) {
        this.buffer = buffer;
        this.start = buffer.start;
        this.end = buffer.end;
        this.enableLoop = buffer.enableLoop;
        this.sampleRate = i_2;
        this.volume = volume;
        this.anInt9847 = i_4;
        this.anInt9853 = 0;
        method15332();
    }

    public static int method15729(float[] floats_2, int[] ints_3, int i_4, int i_5, int i_6, int i_7, int i_9, int i_10, AudioBufferStream class282Sub15Sub5Sub111, int i_12, float f_13) {
        int i_11 = 0;
        int i_81 = 0;
        float f_01 = (float) 0.0;
        i_6 *= 128;
        i_7 *= 128;
        if (i_12 == 0 || (i_81 = i_5 + (i_10 + 256 - i_4 + i_12) / i_12) > i_9) {
            i_81 = i_9;
        }

        i_5 <<= 1;

        int i_10001;
        for (i_81 <<= 1; i_5 < i_81; i_4 += i_12) {
            i_11 = i_4 >> 8;
            f_01 = floats_2[i_11 - 1];
            f_01 = f_01 * 256.0F + (floats_2[i_11] - f_01) * (i_4 & 0xff);
            i_10001 = i_5++;
            ints_3[i_10001] += (int) (f_01 * i_6) >> 6;
            i_10001 = i_5++;
            ints_3[i_10001] += (int) (f_01 * i_7) >> 6;
        }

        if (i_12 == 0 || (i_81 = (i_5 >> 1) + (i_10 - i_4 + i_12) / i_12) > i_9) {
            i_81 = i_9;
        }

        for (i_81 <<= 1; i_5 < i_81; i_4 += i_12) {
            f_01 = f_13 * 256.0F + (floats_2[i_4 >> 8] - f_13) * (i_4 & 0xff);
            i_10001 = i_5++;
            ints_3[i_10001] += (int) (f_01 * i_6) >> 6;
            i_10001 = i_5++;
            ints_3[i_10001] += (int) (f_01 * i_7) >> 6;
        }

        class282Sub15Sub5Sub111.anInt9853 = i_4;
        return i_5 >> 1;
    }

    public static int method15731(float[] floats_1, int[] ints_2, int i_3, int i_4, int i_5, int i_6, int i_8, int i_9, AudioBufferStream class282Sub15Sub5Sub110) {
        int i_71 = 0;
        float f_01 = (float) 0.0;
        i_3 >>= 8;
        i_9 >>= 8;
        i_5 <<= 2;
        i_6 <<= 2;
        i_5 *= 128;
        i_6 *= 128;
        if ((i_71 = i_3 + i_4 - (i_9 - 1)) > i_8) {
            i_71 = i_8;
        }

        i_4 <<= 1;
        i_71 <<= 1;

        int i_10001;
        for (i_71 -= 6; i_4 < i_71; ints_2[i_10001] = (int) (ints_2[i_10001] + f_01 * i_6)) {
            f_01 = floats_1[i_3--];
            i_10001 = i_4++;
            ints_2[i_10001] = (int) (ints_2[i_10001] + f_01 * i_5);
            i_10001 = i_4++;
            ints_2[i_10001] = (int) (ints_2[i_10001] + f_01 * i_6);
            f_01 = floats_1[i_3--];
            i_10001 = i_4++;
            ints_2[i_10001] = (int) (ints_2[i_10001] + f_01 * i_5);
            i_10001 = i_4++;
            ints_2[i_10001] = (int) (ints_2[i_10001] + f_01 * i_6);
            f_01 = floats_1[i_3--];
            i_10001 = i_4++;
            ints_2[i_10001] = (int) (ints_2[i_10001] + f_01 * i_5);
            i_10001 = i_4++;
            ints_2[i_10001] = (int) (ints_2[i_10001] + f_01 * i_6);
            f_01 = floats_1[i_3--];
            i_10001 = i_4++;
            ints_2[i_10001] = (int) (ints_2[i_10001] + f_01 * i_5);
            i_10001 = i_4++;
        }

        for (i_71 += 6; i_4 < i_71; ints_2[i_10001] = (int) (ints_2[i_10001] + f_01 * i_6)) {
            f_01 = floats_1[i_3--];
            i_10001 = i_4++;
            ints_2[i_10001] = (int) (ints_2[i_10001] + f_01 * i_5);
            i_10001 = i_4++;
        }

        class282Sub15Sub5Sub110.anInt9853 = i_3 << 8;
        return i_4 >> 1;
    }

    public static int method15734(float[] floats_2, int[] ints_3, int i_4, int i_5, int i_6, int i_8, int i_9, AudioBufferStream class282Sub15Sub5Sub110, int i_11, float f_12) {
        int i_12 = 0;
        int i_71 = 0;
        float f_01 = (float) 0.0;
        i_6 *= 128;
        if (i_11 == 0 || (i_71 = i_5 + (i_11 + (i_9 + 256 - i_4)) / i_11) > i_8) {
            i_71 = i_8;
        }

        int i_10001;
        while (i_5 < i_71) {
            i_12 = i_4 >> 8;
            f_01 = floats_2[i_12 - 1];
            i_10001 = i_5++;
            ints_3[i_10001] += (int) ((f_01 * 256.0F + (floats_2[i_12] - f_01) * (i_4 & 0xff)) * i_6) >> 6;
            i_4 += i_11;
        }

        if (i_11 == 0 || (i_71 = i_5 + (i_11 + (i_9 - i_4)) / i_11) > i_8) {
            i_71 = i_8;
        }

        for (f_01 = f_12; i_5 < i_71; i_4 += i_11) {
            i_10001 = i_5++;
            ints_3[i_10001] += (int) ((f_01 * 256.0F + (floats_2[i_4 >> 8] - f_01) * (i_4 & 0xff)) * i_6) >> 6;
        }

        class282Sub15Sub5Sub110.anInt9853 = i_4;
        return i_5;
    }

    public static int method15735(float[] floats_2, int[] ints_3, int i_4, int i_5, int i_6, int i_7, int i_9, int i_10, AudioBufferStream class282Sub15Sub5Sub111, int i_12, float f_13) {
        int i_11 = 0;
        int i_81 = 0;
        float f_01 = (float) 0.0;
        i_6 *= 128;
        i_7 *= 128;
        class282Sub15Sub5Sub111.anInt9845 -= class282Sub15Sub5Sub111.anInt9852 * i_5;
        class282Sub15Sub5Sub111.anInt9850 -= class282Sub15Sub5Sub111.anInt9851 * i_5;
        if (i_12 == 0 || (i_81 = i_5 + (i_10 - i_4 + i_12 - 257) / i_12) > i_9) {
            i_81 = i_9;
        }

        int i_10001;
        while (i_5 < i_81) {
            i_11 = i_4 >> 8;
            f_01 = floats_2[i_11];
            i_10001 = i_5++;
            ints_3[i_10001] += (int) ((f_01 * 256.0F + (floats_2[i_11 + 1] - f_01) * (i_4 & 0xff)) * i_6) >> 6;
            i_6 += i_7;
            i_4 += i_12;
        }

        if (i_12 == 0 || (i_81 = i_5 + (i_10 - i_4 + i_12 - 1) / i_12) > i_9) {
            i_81 = i_9;
        }

        while (i_5 < i_81) {
            f_01 = floats_2[i_4 >> 8];
            i_10001 = i_5++;
            ints_3[i_10001] += (int) ((f_01 * 256.0F + (f_13 - f_01) * (i_4 & 0xff)) * i_6) >> 6;
            i_6 += i_7;
            i_4 += i_12;
        }

        class282Sub15Sub5Sub111.anInt9845 += class282Sub15Sub5Sub111.anInt9852 * i_5;
        class282Sub15Sub5Sub111.anInt9850 += class282Sub15Sub5Sub111.anInt9851 * i_5;
        class282Sub15Sub5Sub111.anInt9848 = i_6 / 128;
        class282Sub15Sub5Sub111.anInt9853 = i_4;
        return i_5;
    }

    public static int method15737(float[] floats_1, int[] ints_2, int i_3, int i_4, int i_5, int i_6, int i_7, int i_8, int i_10, int i_11, AudioBufferStream class282Sub15Sub5Sub112) {
        int i_91 = 0;
        float f_01 = (float) 0.0;
        i_3 >>= 8;
        i_11 >>= 8;
        i_5 <<= 2;
        i_6 <<= 2;
        i_7 <<= 2;
        i_8 <<= 2;
        i_5 *= 128;
        i_7 *= 128;
        i_6 *= 128;
        i_8 *= 128;
        if ((i_91 = i_11 + i_4 - i_3) > i_10) {
            i_91 = i_10;
        }

        class282Sub15Sub5Sub112.anInt9848 += class282Sub15Sub5Sub112.anInt9849 * (i_91 - i_4);
        i_4 <<= 1;
        i_91 <<= 1;

        int i_10001;
        for (i_91 -= 6; i_4 < i_91; i_6 += i_8) {
            f_01 = floats_1[i_3++];
            i_10001 = i_4++;
            ints_2[i_10001] = (int) (ints_2[i_10001] + f_01 * i_5);
            i_5 += i_7;
            i_10001 = i_4++;
            ints_2[i_10001] = (int) (ints_2[i_10001] + f_01 * i_6);
            i_6 += i_8;
            f_01 = floats_1[i_3++];
            i_10001 = i_4++;
            ints_2[i_10001] = (int) (ints_2[i_10001] + f_01 * i_5);
            i_5 += i_7;
            i_10001 = i_4++;
            ints_2[i_10001] = (int) (ints_2[i_10001] + f_01 * i_6);
            i_6 += i_8;
            f_01 = floats_1[i_3++];
            i_10001 = i_4++;
            ints_2[i_10001] = (int) (ints_2[i_10001] + f_01 * i_5);
            i_5 += i_7;
            i_10001 = i_4++;
            ints_2[i_10001] = (int) (ints_2[i_10001] + f_01 * i_6);
            i_6 += i_8;
            f_01 = floats_1[i_3++];
            i_10001 = i_4++;
            ints_2[i_10001] = (int) (ints_2[i_10001] + f_01 * i_5);
            i_5 += i_7;
            i_10001 = i_4++;
            ints_2[i_10001] = (int) (ints_2[i_10001] + f_01 * i_6);
        }

        for (i_91 += 6; i_4 < i_91; i_6 += i_8) {
            f_01 = floats_1[i_3++];
            i_10001 = i_4++;
            ints_2[i_10001] = (int) (ints_2[i_10001] + f_01 * i_5);
            i_5 += i_7;
            i_10001 = i_4++;
            ints_2[i_10001] = (int) (ints_2[i_10001] + f_01 * i_6);
        }

        class282Sub15Sub5Sub112.anInt9845 = i_5 / 128 >> 2;
        class282Sub15Sub5Sub112.anInt9850 = i_6 / 128 >> 2;
        class282Sub15Sub5Sub112.anInt9853 = i_3 << 8;
        return i_4 >> 1;
    }

    public static int method15738(float[] floats_0, int[] ints_1, int i_2, int i_3, int i_4, int i_5, int i_7, int i_8, AudioBufferStream class282Sub15Sub5Sub19) {
        int i_61 = 0;
        i_2 >>= 8;
        i_8 >>= 8;
        i_4 <<= 2;
        i_5 <<= 2;
        i_4 *= 128;
        i_5 *= 128;
        if ((i_61 = i_3 + i_2 - (i_8 - 1)) > i_7) {
            i_61 = i_7;
        }

        class282Sub15Sub5Sub19.anInt9845 += class282Sub15Sub5Sub19.anInt9852 * (i_61 - i_3);
        class282Sub15Sub5Sub19.anInt9850 += class282Sub15Sub5Sub19.anInt9851 * (i_61 - i_3);

        int i_10001;
        for (i_61 -= 3; i_3 < i_61; i_4 += i_5) {
            i_10001 = i_3++;
            ints_1[i_10001] = (int) (ints_1[i_10001] + floats_0[i_2--] * i_4);
            i_4 += i_5;
            i_10001 = i_3++;
            ints_1[i_10001] = (int) (ints_1[i_10001] + floats_0[i_2--] * i_4);
            i_4 += i_5;
            i_10001 = i_3++;
            ints_1[i_10001] = (int) (ints_1[i_10001] + floats_0[i_2--] * i_4);
            i_4 += i_5;
            i_10001 = i_3++;
            ints_1[i_10001] = (int) (ints_1[i_10001] + floats_0[i_2--] * i_4);
        }

        for (i_61 += 3; i_3 < i_61; i_4 += i_5) {
            i_10001 = i_3++;
            ints_1[i_10001] = (int) (ints_1[i_10001] + floats_0[i_2--] * i_4);
        }

        class282Sub15Sub5Sub19.anInt9848 = i_4 / 128 >> 2;
        class282Sub15Sub5Sub19.anInt9853 = i_2 << 8;
        return i_3;
    }

    public static int method15739(float[] floats_1, int[] ints_2, int i_3, int i_4, int i_5, int i_6, int i_7, int i_8, int i_10, int i_11, AudioBufferStream class282Sub15Sub5Sub112) {
        int i_91 = 0;
        float f_01 = (float) 0.0;
        i_3 >>= 8;
        i_11 >>= 8;
        i_5 <<= 2;
        i_6 <<= 2;
        i_7 <<= 2;
        i_8 <<= 2;
        i_5 *= 128;
        i_7 *= 128;
        i_6 *= 128;
        i_8 *= 128;
        if ((i_91 = i_3 + i_4 - (i_11 - 1)) > i_10) {
            i_91 = i_10;
        }

        class282Sub15Sub5Sub112.anInt9848 += class282Sub15Sub5Sub112.anInt9849 * (i_91 - i_4);
        i_4 <<= 1;
        i_91 <<= 1;

        int i_10001;
        for (i_91 -= 6; i_4 < i_91; i_6 += i_8) {
            f_01 = floats_1[i_3--];
            i_10001 = i_4++;
            ints_2[i_10001] = (int) (ints_2[i_10001] + f_01 * i_5);
            i_5 += i_7;
            i_10001 = i_4++;
            ints_2[i_10001] = (int) (ints_2[i_10001] + f_01 * i_6);
            i_6 += i_8;
            f_01 = floats_1[i_3--];
            i_10001 = i_4++;
            ints_2[i_10001] = (int) (ints_2[i_10001] + f_01 * i_5);
            i_5 += i_7;
            i_10001 = i_4++;
            ints_2[i_10001] = (int) (ints_2[i_10001] + f_01 * i_6);
            i_6 += i_8;
            f_01 = floats_1[i_3--];
            i_10001 = i_4++;
            ints_2[i_10001] = (int) (ints_2[i_10001] + f_01 * i_5);
            i_5 += i_7;
            i_10001 = i_4++;
            ints_2[i_10001] = (int) (ints_2[i_10001] + f_01 * i_6);
            i_6 += i_8;
            f_01 = floats_1[i_3--];
            i_10001 = i_4++;
            ints_2[i_10001] = (int) (ints_2[i_10001] + f_01 * i_5);
            i_5 += i_7;
            i_10001 = i_4++;
            ints_2[i_10001] = (int) (ints_2[i_10001] + f_01 * i_6);
        }

        for (i_91 += 6; i_4 < i_91; i_6 += i_8) {
            f_01 = floats_1[i_3--];
            i_10001 = i_4++;
            ints_2[i_10001] = (int) (ints_2[i_10001] + f_01 * i_5);
            i_5 += i_7;
            i_10001 = i_4++;
            ints_2[i_10001] = (int) (ints_2[i_10001] + f_01 * i_6);
        }

        class282Sub15Sub5Sub112.anInt9845 = i_5 / 128 >> 2;
        class282Sub15Sub5Sub112.anInt9850 = i_6 / 128 >> 2;
        class282Sub15Sub5Sub112.anInt9853 = i_3 << 8;
        return i_4 >> 1;
    }

    public static int method15740(float[] floats_0, int[] ints_1, int i_2, int i_3, int i_4, int i_6, int i_7, AudioBufferStream class282Sub15Sub5Sub18) {
        int i_51 = 0;
        i_2 >>= 8;
        i_7 >>= 8;
        i_4 <<= 2;
        i_4 *= 128;
        if ((i_51 = i_3 + i_7 - i_2) > i_6) {
            i_51 = i_6;
        }

        int i_10001;
        for (i_51 -= 3; i_3 < i_51; ints_1[i_10001] = (int) (ints_1[i_10001] + floats_0[i_2++] * i_4)) {
            i_10001 = i_3++;
            ints_1[i_10001] = (int) (ints_1[i_10001] + floats_0[i_2++] * i_4);
            i_10001 = i_3++;
            ints_1[i_10001] = (int) (ints_1[i_10001] + floats_0[i_2++] * i_4);
            i_10001 = i_3++;
            ints_1[i_10001] = (int) (ints_1[i_10001] + floats_0[i_2++] * i_4);
            i_10001 = i_3++;
        }

        for (i_51 += 3; i_3 < i_51; ints_1[i_10001] = (int) (ints_1[i_10001] + floats_0[i_2++] * i_4)) {
            i_10001 = i_3++;
        }

        class282Sub15Sub5Sub18.anInt9853 = i_2 << 8;
        return i_3;
    }

    public static int method15741(float[] floats_2, int[] ints_3, int i_4, int i_5, int i_6, int i_7, int i_8, int i_9, int i_11, int i_12, AudioBufferStream class282Sub15Sub5Sub113, int i_14, float f_15) {
        int i_13 = 0;
        int i_101 = 0;
        float f_01 = (float) 0.0;
        i_6 *= 128;
        i_8 *= 128;
        i_7 *= 128;
        i_9 *= 128;
        class282Sub15Sub5Sub113.anInt9848 -= i_5 * class282Sub15Sub5Sub113.anInt9849;
        if (i_14 == 0 || (i_101 = i_5 + (i_12 - i_4 + i_14 - 257) / i_14) > i_11) {
            i_101 = i_11;
        }

        i_5 <<= 1;

        int i_10001;
        for (i_101 <<= 1; i_5 < i_101; i_4 += i_14) {
            i_13 = i_4 >> 8;
            f_01 = floats_2[i_13];
            f_01 = f_01 * 256.0F + (floats_2[i_13 + 1] - f_01) * (i_4 & 0xff);
            i_10001 = i_5++;
            ints_3[i_10001] += (int) (f_01 * i_6) >> 6;
            i_6 += i_8;
            i_10001 = i_5++;
            ints_3[i_10001] += (int) (f_01 * i_7) >> 6;
            i_7 += i_9;
        }

        if (i_14 == 0 || (i_101 = (i_5 >> 1) + (i_12 - i_4 + i_14 - 1) / i_14) > i_11) {
            i_101 = i_11;
        }

        for (i_101 <<= 1; i_5 < i_101; i_4 += i_14) {
            f_01 = floats_2[i_4 >> 8];
            f_01 = f_01 * 256.0F + (f_15 - f_01) * (i_4 & 0xff);
            i_10001 = i_5++;
            ints_3[i_10001] += (int) (f_01 * i_6) >> 6;
            i_6 += i_8;
            i_10001 = i_5++;
            ints_3[i_10001] += (int) (f_01 * i_7) >> 6;
            i_7 += i_9;
        }

        i_5 >>= 1;
        class282Sub15Sub5Sub113.anInt9848 += class282Sub15Sub5Sub113.anInt9849 * i_5;
        class282Sub15Sub5Sub113.anInt9845 = i_6 / 128;
        class282Sub15Sub5Sub113.anInt9850 = i_7 / 128;
        class282Sub15Sub5Sub113.anInt9853 = i_4;
        return i_5;
    }

    public static int method15742(float[] floats_2, int[] ints_3, int i_4, int i_5, int i_6, int i_7, int i_9, int i_10, AudioBufferStream class282Sub15Sub5Sub111, int i_12, float f_13) {
        int i_11 = 0;
        int i_81 = 0;
        float f_01 = (float) 0.0;
        i_6 *= 128;
        i_7 *= 128;
        class282Sub15Sub5Sub111.anInt9845 -= class282Sub15Sub5Sub111.anInt9852 * i_5;
        class282Sub15Sub5Sub111.anInt9850 -= class282Sub15Sub5Sub111.anInt9851 * i_5;
        if (i_12 == 0 || (i_81 = i_5 + (i_10 + 256 - i_4 + i_12) / i_12) > i_9) {
            i_81 = i_9;
        }

        int i_10001;
        while (i_5 < i_81) {
            i_11 = i_4 >> 8;
            f_01 = floats_2[i_11 - 1];
            i_10001 = i_5++;
            ints_3[i_10001] += (int) ((f_01 * 256.0F + (floats_2[i_11] - f_01) * (i_4 & 0xff)) * i_6) >> 6;
            i_6 += i_7;
            i_4 += i_12;
        }

        if (i_12 == 0 || (i_81 = i_5 + (i_10 - i_4 + i_12) / i_12) > i_9) {
            i_81 = i_9;
        }

        for (f_01 = f_13; i_5 < i_81; i_4 += i_12) {
            i_10001 = i_5++;
            ints_3[i_10001] += (int) ((f_01 * 256.0F + (floats_2[i_4 >> 8] - f_01) * (i_4 & 0xff)) * i_6) >> 6;
            i_6 += i_7;
        }

        class282Sub15Sub5Sub111.anInt9845 += class282Sub15Sub5Sub111.anInt9852 * i_5;
        class282Sub15Sub5Sub111.anInt9850 += class282Sub15Sub5Sub111.anInt9851 * i_5;
        class282Sub15Sub5Sub111.anInt9848 = i_6 / 128;
        class282Sub15Sub5Sub111.anInt9853 = i_4;
        return i_5;
    }

    public static int method15743(float[] floats_2, int[] ints_3, int i_4, int i_5, int i_6, int i_7, int i_8, int i_9, int i_11, int i_12, AudioBufferStream class282Sub15Sub5Sub113, int i_14, float f_15) {
        int i_13 = 0;
        int i_101 = 0;
        float f_01 = (float) 0.0;
        i_6 *= 128;
        i_8 *= 128;
        i_7 *= 128;
        i_9 *= 128;
        class282Sub15Sub5Sub113.anInt9848 -= i_5 * class282Sub15Sub5Sub113.anInt9849;
        if (i_14 == 0 || (i_101 = i_5 + (i_12 + 256 - i_4 + i_14) / i_14) > i_11) {
            i_101 = i_11;
        }

        i_5 <<= 1;

        int i_10001;
        for (i_101 <<= 1; i_5 < i_101; i_4 += i_14) {
            i_13 = i_4 >> 8;
            f_01 = floats_2[i_13 - 1];
            f_01 = f_01 * 256.0F + (floats_2[i_13] - f_01) * (i_4 & 0xff);
            i_10001 = i_5++;
            ints_3[i_10001] += (int) (f_01 * i_6) >> 6;
            i_6 += i_8;
            i_10001 = i_5++;
            ints_3[i_10001] += (int) (f_01 * i_7) >> 6;
            i_7 += i_9;
        }

        if (i_14 == 0 || (i_101 = (i_5 >> 1) + (i_12 - i_4 + i_14) / i_14) > i_11) {
            i_101 = i_11;
        }

        for (i_101 <<= 1; i_5 < i_101; i_4 += i_14) {
            f_01 = f_15 * 256.0F + (floats_2[i_4 >> 8] - f_15) * (i_4 & 0xff);
            i_10001 = i_5++;
            ints_3[i_10001] += (int) (f_01 * i_6) >> 6;
            i_6 += i_8;
            i_10001 = i_5++;
            ints_3[i_10001] += (int) (f_01 * i_7) >> 6;
            i_7 += i_9;
        }

        i_5 >>= 1;
        class282Sub15Sub5Sub113.anInt9848 += class282Sub15Sub5Sub113.anInt9849 * i_5;
        class282Sub15Sub5Sub113.anInt9845 = i_6 / 128;
        class282Sub15Sub5Sub113.anInt9850 = i_7 / 128;
        class282Sub15Sub5Sub113.anInt9853 = i_4;
        return i_5;
    }

    public static int method15746(float[] floats_2, int[] ints_3, int i_4, int i_5, int i_6, int i_7, int i_9, int i_10, AudioBufferStream class282Sub15Sub5Sub111, int i_12, float f_13) {
        int i_11 = 0;
        int i_81 = 0;
        float f_01 = (float) 0.0;
        i_6 *= 128;
        i_7 *= 128;
        if (i_12 == 0 || (i_81 = i_5 + (i_10 - i_4 + i_12 - 257) / i_12) > i_9) {
            i_81 = i_9;
        }

        i_5 <<= 1;

        int i_10001;
        for (i_81 <<= 1; i_5 < i_81; i_4 += i_12) {
            i_11 = i_4 >> 8;
            f_01 = floats_2[i_11];
            f_01 = f_01 * 256.0F + (floats_2[i_11 + 1] - f_01) * (i_4 & 0xff);
            i_10001 = i_5++;
            ints_3[i_10001] += (int) (f_01 * i_6) >> 6;
            i_10001 = i_5++;
            ints_3[i_10001] += (int) (f_01 * i_7) >> 6;
        }

        if (i_12 == 0 || (i_81 = (i_5 >> 1) + (i_10 - i_4 + i_12 - 1) / i_12) > i_9) {
            i_81 = i_9;
        }

        for (i_81 <<= 1; i_5 < i_81; i_4 += i_12) {
            f_01 = floats_2[i_4 >> 8];
            f_01 = f_01 * 256.0F + (f_13 - f_01) * (i_4 & 0xff);
            i_10001 = i_5++;
            ints_3[i_10001] += (int) (f_01 * i_6) >> 6;
            i_10001 = i_5++;
            ints_3[i_10001] += (int) (f_01 * i_7) >> 6;
        }

        class282Sub15Sub5Sub111.anInt9853 = i_4;
        return i_5 >> 1;
    }

    public static int method15747(float[] floats_2, int[] ints_3, int i_4, int i_5, int i_6, int i_8, int i_9, AudioBufferStream class282Sub15Sub5Sub110, int i_11, float f_12) {
        int i_12 = 0;
        int i_71 = 0;
        float f_01 = (float) 0.0;
        i_6 *= 128;
        if (i_11 == 0 || (i_71 = i_5 + (i_11 + (i_9 - i_4) - 257) / i_11) > i_8) {
            i_71 = i_8;
        }

        int i_10001;
        while (i_5 < i_71) {
            i_12 = i_4 >> 8;
            f_01 = floats_2[i_12];
            i_10001 = i_5++;
            ints_3[i_10001] += (int) ((f_01 * 256.0F + (floats_2[i_12 + 1] - f_01) * (i_4 & 0xff)) * i_6) >> 6;
            i_4 += i_11;
        }

        if (i_11 == 0 || (i_71 = i_5 + (i_11 + (i_9 - i_4) - 1) / i_11) > i_8) {
            i_71 = i_8;
        }

        while (i_5 < i_71) {
            f_01 = floats_2[i_4 >> 8];
            i_10001 = i_5++;
            ints_3[i_10001] += (int) ((f_01 * 256.0F + (f_12 - f_01) * (i_4 & 0xff)) * i_6) >> 6;
            i_4 += i_11;
        }

        class282Sub15Sub5Sub110.anInt9853 = i_4;
        return i_5;
    }

    public static int method15770(float[] floats_1, int[] ints_2, int i_3, int i_4, int i_5, int i_6, int i_8, int i_9, AudioBufferStream class282Sub15Sub5Sub110) {
        int i_71 = 0;
        float f_01 = (float) 0.0;
        i_3 >>= 8;
        i_9 >>= 8;
        i_5 <<= 2;
        i_6 <<= 2;
        i_5 *= 128;
        i_6 *= 128;
        if ((i_71 = i_4 + i_9 - i_3) > i_8) {
            i_71 = i_8;
        }

        i_4 <<= 1;
        i_71 <<= 1;

        int i_10001;
        for (i_71 -= 6; i_4 < i_71; ints_2[i_10001] = (int) (ints_2[i_10001] + f_01 * i_6)) {
            f_01 = floats_1[i_3++];
            i_10001 = i_4++;
            ints_2[i_10001] = (int) (ints_2[i_10001] + f_01 * i_5);
            i_10001 = i_4++;
            ints_2[i_10001] = (int) (ints_2[i_10001] + f_01 * i_6);
            f_01 = floats_1[i_3++];
            i_10001 = i_4++;
            ints_2[i_10001] = (int) (ints_2[i_10001] + f_01 * i_5);
            i_10001 = i_4++;
            ints_2[i_10001] = (int) (ints_2[i_10001] + f_01 * i_6);
            f_01 = floats_1[i_3++];
            i_10001 = i_4++;
            ints_2[i_10001] = (int) (ints_2[i_10001] + f_01 * i_5);
            i_10001 = i_4++;
            ints_2[i_10001] = (int) (ints_2[i_10001] + f_01 * i_6);
            f_01 = floats_1[i_3++];
            i_10001 = i_4++;
            ints_2[i_10001] = (int) (ints_2[i_10001] + f_01 * i_5);
            i_10001 = i_4++;
        }

        for (i_71 += 6; i_4 < i_71; ints_2[i_10001] = (int) (ints_2[i_10001] + f_01 * i_6)) {
            f_01 = floats_1[i_3++];
            i_10001 = i_4++;
            ints_2[i_10001] = (int) (ints_2[i_10001] + f_01 * i_5);
            i_10001 = i_4++;
        }

        class282Sub15Sub5Sub110.anInt9853 = i_3 << 8;
        return i_4 >> 1;
    }

    public static int method15774(float[] floats_0, int[] ints_1, int i_2, int i_3, int i_4, int i_6, int i_7, AudioBufferStream class282Sub15Sub5Sub18) {
        int i_51 = 0;
        i_2 >>= 8;
        i_7 >>= 8;
        i_4 <<= 2;
        i_4 *= 128;
        if ((i_51 = i_3 + i_2 - (i_7 - 1)) > i_6) {
            i_51 = i_6;
        }

        int i_10001;
        for (i_51 -= 3; i_3 < i_51; ints_1[i_10001] = (int) (ints_1[i_10001] + floats_0[i_2--] * i_4)) {
            i_10001 = i_3++;
            ints_1[i_10001] = (int) (ints_1[i_10001] + floats_0[i_2--] * i_4);
            i_10001 = i_3++;
            ints_1[i_10001] = (int) (ints_1[i_10001] + floats_0[i_2--] * i_4);
            i_10001 = i_3++;
            ints_1[i_10001] = (int) (ints_1[i_10001] + floats_0[i_2--] * i_4);
            i_10001 = i_3++;
        }

        for (i_51 += 3; i_3 < i_51; ints_1[i_10001] = (int) (ints_1[i_10001] + floats_0[i_2--] * i_4)) {
            i_10001 = i_3++;
        }

        class282Sub15Sub5Sub18.anInt9853 = i_2 << 8;
        return i_3;
    }

    public static int method15777(float[] floats_0, int[] ints_1, int i_2, int i_3, int i_4, int i_5, int i_7, int i_8, AudioBufferStream class282Sub15Sub5Sub19) {
        int i_61 = 0;
        i_2 >>= 8;
        i_8 >>= 8;
        i_4 <<= 2;
        i_5 <<= 2;
        i_4 *= 128;
        i_5 *= 128;
        if ((i_61 = i_3 + i_8 - i_2) > i_7) {
            i_61 = i_7;
        }

        class282Sub15Sub5Sub19.anInt9845 += class282Sub15Sub5Sub19.anInt9852 * (i_61 - i_3);
        class282Sub15Sub5Sub19.anInt9850 += class282Sub15Sub5Sub19.anInt9851 * (i_61 - i_3);

        int i_10001;
        for (i_61 -= 3; i_3 < i_61; i_4 += i_5) {
            i_10001 = i_3++;
            ints_1[i_10001] = (int) (ints_1[i_10001] + floats_0[i_2++] * i_4);
            i_4 += i_5;
            i_10001 = i_3++;
            ints_1[i_10001] = (int) (ints_1[i_10001] + floats_0[i_2++] * i_4);
            i_4 += i_5;
            i_10001 = i_3++;
            ints_1[i_10001] = (int) (ints_1[i_10001] + floats_0[i_2++] * i_4);
            i_4 += i_5;
            i_10001 = i_3++;
            ints_1[i_10001] = (int) (ints_1[i_10001] + floats_0[i_2++] * i_4);
        }

        for (i_61 += 3; i_3 < i_61; i_4 += i_5) {
            i_10001 = i_3++;
            ints_1[i_10001] = (int) (ints_1[i_10001] + floats_0[i_2++] * i_4);
        }

        class282Sub15Sub5Sub19.anInt9848 = i_4 / 128 >> 2;
        class282Sub15Sub5Sub19.anInt9853 = i_2 << 8;
        return i_3;
    }

    @Override
    public synchronized void fill(int[] ints_1, int i_2, int i_3) throws IllegalStateException {
        if (super.volume == 0 && super.anInt9858 == 0) {
            skip(i_3);
        } else {
            int i_4 = super.start << 8;
            int i_5 = super.end << 8;
            int i_6 = ((AudioBuffer) super.buffer).getTotalSamples() << 8;
            int i_7 = i_5 - i_4;
            if (i_7 <= 0) {
                super.anInt9854 = 0;
            }

            int i_8 = i_2;
            i_3 += i_2;
            if (super.anInt9853 < 0) {
                if (super.sampleRate <= 0) {
                    method15354();
                    unlink();
                    return;
                }

                super.anInt9853 = 0;
            }

            if (super.anInt9853 >= i_6) {
                if (super.sampleRate >= 0) {
                    method15354();
                    unlink();
                    return;
                }

                super.anInt9853 = i_6 - 1;
            }

            if (super.anInt9854 < 0) {
                if (super.enableLoop) {
                    if (super.sampleRate < 0) {
                        i_8 = method15732(ints_1, i_2, i_4, i_3, ((AudioBuffer) super.buffer).getSamples()[super.start]);
                        if (super.anInt9853 >= i_4) {
                            return;
                        }

                        super.anInt9853 = i_4 + i_4 - 1 - super.anInt9853;
                        super.sampleRate = -(super.sampleRate * 1927994969) * -177305111;
                    }

                    while (true) {
                        i_8 = method15769(ints_1, i_8, i_5, i_3, ((AudioBuffer) super.buffer).getSamples()[super.end - 1]);
                        if (super.anInt9853 < i_5) {
                            break;
                        }

                        super.anInt9853 = i_5 + i_5 - 1 - super.anInt9853;
                        super.sampleRate = -(super.sampleRate * 1927994969) * -177305111;
                        i_8 = method15732(ints_1, i_8, i_4, i_3, ((AudioBuffer) super.buffer).getSamples()[super.start]);
                        if (super.anInt9853 >= i_4) {
                            break;
                        }

                        super.anInt9853 = i_4 + i_4 - 1 - super.anInt9853;
                        super.sampleRate = -(super.sampleRate * 1927994969) * -177305111;
                    }
                } else if (super.sampleRate < 0) {
                    while (true) {
                        i_8 = method15732(ints_1, i_8, i_4, i_3, ((AudioBuffer) super.buffer).getSamples()[super.end - 1]);
                        if (super.anInt9853 >= i_4) {
                            break;
                        }

                        super.anInt9853 = i_5 - 1 - (i_5 - 1 - super.anInt9853) % i_7;
                    }
                } else {
                    while (true) {
                        i_8 = method15769(ints_1, i_8, i_5, i_3, ((AudioBuffer) super.buffer).getSamples()[super.start]);
                        if (super.anInt9853 < i_5) {
                            break;
                        }

                        super.anInt9853 = i_4 + (super.anInt9853 - i_4) % i_7;
                    }
                }
            } else {
                if (super.anInt9854 > 0) {
                    if (super.enableLoop) {
                        label111:
                        {
                            if (super.sampleRate < 0) {
                                i_8 = method15732(ints_1, i_2, i_4, i_3, ((AudioBuffer) super.buffer).getSamples()[super.start]);
                                if (super.anInt9853 >= i_4) {
                                    return;
                                }

                                super.anInt9853 = i_4 + i_4 - 1 - super.anInt9853;
                                super.sampleRate = -(super.sampleRate * 1927994969) * -177305111;
                                if (--super.anInt9854 == 0) {
                                    break label111;
                                }
                            }

                            do {
                                i_8 = method15769(ints_1, i_8, i_5, i_3, ((AudioBuffer) super.buffer).getSamples()[super.end - 1]);
                                if (super.anInt9853 < i_5) {
                                    return;
                                }

                                super.anInt9853 = i_5 + i_5 - 1 - super.anInt9853;
                                super.sampleRate = -(super.sampleRate * 1927994969) * -177305111;
                                if (--super.anInt9854 == 0) {
                                    break;
                                }

                                i_8 = method15732(ints_1, i_8, i_4, i_3, ((AudioBuffer) super.buffer).getSamples()[super.start]);
                                if (super.anInt9853 >= i_4) {
                                    return;
                                }

                                super.anInt9853 = i_4 + i_4 - 1 - super.anInt9853;
                                super.sampleRate = -(super.sampleRate * 1927994969) * -177305111;
                            } while (--super.anInt9854 != 0);
                        }
                    } else {
                        int i_9;
                        if (super.sampleRate < 0) {
                            while (true) {
                                i_8 = method15732(ints_1, i_8, i_4, i_3, ((AudioBuffer) super.buffer).getSamples()[super.end - 1]);
                                if (super.anInt9853 >= i_4) {
                                    return;
                                }

                                i_9 = (i_5 - 1 - super.anInt9853) / i_7;
                                if (i_9 >= super.anInt9854) {
                                    super.anInt9853 += i_7 * super.anInt9854;
                                    super.anInt9854 = 0;
                                    break;
                                }

                                super.anInt9853 += i_7 * i_9;
                                super.anInt9854 -= i_9;
                            }
                        } else {
                            while (true) {
                                i_8 = method15769(ints_1, i_8, i_5, i_3, ((AudioBuffer) super.buffer).getSamples()[super.start]);
                                if (super.anInt9853 < i_5) {
                                    return;
                                }

                                i_9 = (super.anInt9853 - i_4) / i_7;
                                if (i_9 >= super.anInt9854) {
                                    super.anInt9853 -= i_7 * super.anInt9854;
                                    super.anInt9854 = 0;
                                    break;
                                }

                                super.anInt9853 -= i_7 * i_9;
                                super.anInt9854 -= i_9;
                            }
                        }
                    }
                }

                if (super.sampleRate < 0) {
                    method15732(ints_1, i_8, 0, i_3, 0.0F);
                    if (super.anInt9853 < 0) {
                        super.anInt9853 = -1;
                        method15354();
                        unlink();
                    }
                } else {
                    method15769(ints_1, i_8, i_6, i_3, 0.0F);
                    if (super.anInt9853 >= i_6) {
                        super.anInt9853 = i_6;
                        method15354();
                        unlink();
                    }
                }
            }
        }

    }

    public int method15732(int[] ints_1, int i_2, int i_3, int i_4, float f_5) {
        do {
            if (super.anInt9858 <= 0) {
                if (super.sampleRate == -256 && (super.anInt9853 & 0xff) == 0) {
                    if (PcmPlayerManager.stereo) {
                        return method15731(((AudioBuffer) super.buffer).getSamples(), ints_1, super.anInt9853, i_2, super.anInt9845, super.anInt9850, i_4, i_3, this);
                    }

                    return method15774(((AudioBuffer) super.buffer).getSamples(), ints_1, super.anInt9853, i_2, super.anInt9848, i_4, i_3, this);
                }

                if (PcmPlayerManager.stereo) {
                    return method15729(((AudioBuffer) super.buffer).getSamples(), ints_1, super.anInt9853, i_2, super.anInt9845, super.anInt9850, i_4, i_3, this, super.sampleRate, f_5);
                }

                return method15734(((AudioBuffer) super.buffer).getSamples(), ints_1, super.anInt9853, i_2, super.anInt9848, i_4, i_3, this, super.sampleRate, f_5);
            }

            int i_6 = i_2 + super.anInt9858;
            if (i_6 > i_4) {
                i_6 = i_4;
            }

            super.anInt9858 += i_2;
            if (super.sampleRate == -256 && (super.anInt9853 & 0xff) == 0) {
                if (PcmPlayerManager.stereo) {
                    i_2 = method15739(((AudioBuffer) super.buffer).getSamples(), ints_1, super.anInt9853, i_2, super.anInt9845, super.anInt9850, super.anInt9852, super.anInt9851, i_6, i_3, this);
                } else {
                    i_2 = method15738(((AudioBuffer) super.buffer).getSamples(), ints_1, super.anInt9853, i_2, super.anInt9848, super.anInt9849, i_6, i_3, this);
                }
            } else if (PcmPlayerManager.stereo) {
                i_2 = method15743(((AudioBuffer) super.buffer).getSamples(), ints_1, super.anInt9853, i_2, super.anInt9845, super.anInt9850, super.anInt9852, super.anInt9851, i_6, i_3, this, super.sampleRate, f_5);
            } else {
                i_2 = method15742(((AudioBuffer) super.buffer).getSamples(), ints_1, super.anInt9853, i_2, super.anInt9848, super.anInt9849, i_6, i_3, this, super.sampleRate, f_5);
            }

            super.anInt9858 -= i_2;
            if (super.anInt9858 != 0) {
                return i_2;
            }
        } while (!method15324());

        return i_4;
    }


    public int method15769(int[] ints_1, int i_2, int i_3, int i_4, float f_5) {
        do {
            if (super.anInt9858 <= 0) {
                if (super.sampleRate == 256 && (super.anInt9853 & 0xff) == 0) {
                    if (PcmPlayerManager.stereo) {
                        return method15770(((AudioBuffer) super.buffer).getSamples(), ints_1, super.anInt9853, i_2, super.anInt9845, super.anInt9850, i_4, i_3, this);
                    }

                    return method15740(((AudioBuffer) super.buffer).getSamples(), ints_1, super.anInt9853, i_2, super.anInt9848, i_4, i_3, this);
                }

                if (PcmPlayerManager.stereo) {
                    return method15746(((AudioBuffer) super.buffer).getSamples(), ints_1, super.anInt9853, i_2, super.anInt9845, super.anInt9850, i_4, i_3, this, super.sampleRate, f_5);
                }

                return method15747(((AudioBuffer) super.buffer).getSamples(), ints_1, super.anInt9853, i_2, super.anInt9848, i_4, i_3, this, super.sampleRate, f_5);
            }

            int i_6 = i_2 + super.anInt9858;
            if (i_6 > i_4) {
                i_6 = i_4;
            }

            super.anInt9858 += i_2;
            if (super.sampleRate == 256 && (super.anInt9853 & 0xff) == 0) {
                if (PcmPlayerManager.stereo) {
                    i_2 = method15737(((AudioBuffer) super.buffer).getSamples(), ints_1, super.anInt9853, i_2, super.anInt9845, super.anInt9850, super.anInt9852, super.anInt9851, i_6, i_3, this);
                } else {
                    i_2 = method15777(((AudioBuffer) super.buffer).getSamples(), ints_1, super.anInt9853, i_2, super.anInt9848, super.anInt9849, i_6, i_3, this);
                }
            } else if (PcmPlayerManager.stereo) {
                i_2 = method15741(((AudioBuffer) super.buffer).getSamples(), ints_1, super.anInt9853, i_2, super.anInt9845, super.anInt9850, super.anInt9852, super.anInt9851, i_6, i_3, this, super.sampleRate, f_5);
            } else {
                i_2 = method15735(((AudioBuffer) super.buffer).getSamples(), ints_1, super.anInt9853, i_2, super.anInt9848, super.anInt9849, i_6, i_3, this, super.sampleRate, f_5);
            }

            super.anInt9858 -= i_2;
            if (super.anInt9858 != 0) {
                return i_2;
            }
        } while (!method15324());

        return i_4;
    }

}
