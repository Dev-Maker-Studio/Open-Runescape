package com.jagex.game.runetek5.sound.jagtheora;

import com.jagex.game.runetek5.config.midi.MixedAudioBuffer;
import com.jagex.game.runetek5.sound.player.PcmPlayerManager;

public class MixedAudioStream extends AreaSoundStream {

    public MixedAudioStream(MixedAudioBuffer buffer, int sampleRate, int volume, int i_4) {
    	this.buffer = buffer;
    	this.start = buffer.start;
    	this.end = buffer.end;
    	this.enableLoop = buffer.enableLoop;
    	this.sampleRate = sampleRate;
        this.volume = volume;
        this.anInt9847 = i_4;
        this.anInt9853 = 0;
        method15332();
    }

    public static int method15991(byte[] bytes_0, int[] ints_1, int i_2, int i_3, int i_4, int i_6, int i_7, MixedAudioStream class282Sub15Sub5Sub28) {
        int i_51 = 0;
        i_2 >>= 8;
        i_7 >>= 8;
        i_4 <<= 2;
        if ((i_51 = i_3 + i_2 - (i_7 - 1)) > i_6) {
            i_51 = i_6;
        }

        int i_10001;
        for (i_51 -= 3; i_3 < i_51; ints_1[i_10001] += bytes_0[i_2--] * i_4) {
            i_10001 = i_3++;
            ints_1[i_10001] += bytes_0[i_2--] * i_4;
            i_10001 = i_3++;
            ints_1[i_10001] += bytes_0[i_2--] * i_4;
            i_10001 = i_3++;
            ints_1[i_10001] += bytes_0[i_2--] * i_4;
            i_10001 = i_3++;
        }

        for (i_51 += 3; i_3 < i_51; ints_1[i_10001] += bytes_0[i_2--] * i_4) {
            i_10001 = i_3++;
        }

        class282Sub15Sub5Sub28.anInt9853 = i_2 << 8;
        return i_3;
    }

    public static int method15992(byte[] bytes_0, int[] ints_1, int i_2, int i_3, int i_4, int i_6, int i_7, MixedAudioStream class282Sub15Sub5Sub28) {
        int i_51 = 0;
        i_2 >>= 8;
        i_7 >>= 8;
        i_4 <<= 2;
        if ((i_51 = i_3 + i_7 - i_2) > i_6) {
            i_51 = i_6;
        }

        int i_10001;
        for (i_51 -= 3; i_3 < i_51; ints_1[i_10001] += bytes_0[i_2++] * i_4) {
            i_10001 = i_3++;
            ints_1[i_10001] += bytes_0[i_2++] * i_4;
            i_10001 = i_3++;
            ints_1[i_10001] += bytes_0[i_2++] * i_4;
            i_10001 = i_3++;
            ints_1[i_10001] += bytes_0[i_2++] * i_4;
            i_10001 = i_3++;
        }

        for (i_51 += 3; i_3 < i_51; ints_1[i_10001] += bytes_0[i_2++] * i_4) {
            i_10001 = i_3++;
        }

        class282Sub15Sub5Sub28.anInt9853 = i_2 << 8;
        return i_3;
    }

    public static int method15995(byte[] bytes_1, int[] ints_2, int i_3, int i_4, int i_5, int i_6, int i_8, int i_9, MixedAudioStream class282Sub15Sub5Sub210) {
        int i_71 = 0;
        i_3 >>= 8;
        i_9 >>= 8;
        i_5 <<= 2;
        i_6 <<= 2;
        if ((i_71 = i_3 + i_4 - (i_9 - 1)) > i_8) {
            i_71 = i_8;
        }

        i_4 <<= 1;
        i_71 <<= 1;

        int i_10001;
        byte b_11;
        for (i_71 -= 6; i_4 < i_71; ints_2[i_10001] += b_11 * i_6) {
            b_11 = bytes_1[i_3--];
            i_10001 = i_4++;
            ints_2[i_10001] += b_11 * i_5;
            i_10001 = i_4++;
            ints_2[i_10001] += b_11 * i_6;
            b_11 = bytes_1[i_3--];
            i_10001 = i_4++;
            ints_2[i_10001] += b_11 * i_5;
            i_10001 = i_4++;
            ints_2[i_10001] += b_11 * i_6;
            b_11 = bytes_1[i_3--];
            i_10001 = i_4++;
            ints_2[i_10001] += b_11 * i_5;
            i_10001 = i_4++;
            ints_2[i_10001] += b_11 * i_6;
            b_11 = bytes_1[i_3--];
            i_10001 = i_4++;
            ints_2[i_10001] += b_11 * i_5;
            i_10001 = i_4++;
        }

        for (i_71 += 6; i_4 < i_71; ints_2[i_10001] += b_11 * i_6) {
            b_11 = bytes_1[i_3--];
            i_10001 = i_4++;
            ints_2[i_10001] += b_11 * i_5;
            i_10001 = i_4++;
        }

        class282Sub15Sub5Sub210.anInt9853 = i_3 << 8;
        return i_4 >> 1;
    }

    public static int method15996(byte[] bytes_2, int[] ints_3, int i_4, int i_5, int i_6, int i_8, int i_9, MixedAudioStream class282Sub15Sub5Sub210, int i_11, int i_12) {
        int i_71 = 0;
        int i_13 = 0;
        if (i_11 == 0 || (i_71 = i_5 + (i_11 + (i_9 - i_4) - 257) / i_11) > i_8) {
            i_71 = i_8;
        }

        byte b_13;
        int i_10001;
        while (i_5 < i_71) {
            i_13 = i_4 >> 8;
            b_13 = bytes_2[i_13];
            i_10001 = i_5++;
            ints_3[i_10001] += ((b_13 << 8) + (bytes_2[i_13 + 1] - b_13) * (i_4 & 0xff)) * i_6 >> 6;
            i_4 += i_11;
        }

        if (i_11 == 0 || (i_71 = i_5 + (i_11 + (i_9 - i_4) - 1) / i_11) > i_8) {
            i_71 = i_8;
        }

        for (i_13 = i_12; i_5 < i_71; i_4 += i_11) {
            b_13 = bytes_2[i_4 >> 8];
            i_10001 = i_5++;
            ints_3[i_10001] += ((b_13 << 8) + (i_13 - b_13) * (i_4 & 0xff)) * i_6 >> 6;
        }

        class282Sub15Sub5Sub210.anInt9853 = i_4;
        return i_5;
    }

    public static int method15998(byte[] bytes_2, int[] ints_3, int i_4, int i_5, int i_6, int i_8, int i_9, MixedAudioStream class282Sub15Sub5Sub210, int i_11, int i_12) {
        int i_71 = 0;
        int i_13 = 0;
        if (i_11 == 0 || (i_71 = i_5 + (i_11 + (i_9 + 256 - i_4)) / i_11) > i_8) {
            i_71 = i_8;
        }

        int i_10001;
        while (i_5 < i_71) {
            i_13 = i_4 >> 8;
            byte b_13 = bytes_2[i_13 - 1];
            i_10001 = i_5++;
            ints_3[i_10001] += ((b_13 << 8) + (bytes_2[i_13] - b_13) * (i_4 & 0xff)) * i_6 >> 6;
            i_4 += i_11;
        }

        if (i_11 == 0 || (i_71 = i_5 + (i_11 + (i_9 - i_4)) / i_11) > i_8) {
            i_71 = i_8;
        }

        int i_01 = i_12;

        for (i_13 = i_11; i_5 < i_71; i_4 += i_13) {
            i_10001 = i_5++;
            ints_3[i_10001] += ((i_01 << 8) + (bytes_2[i_4 >> 8] - i_01) * (i_4 & 0xff)) * i_6 >> 6;
        }

        class282Sub15Sub5Sub210.anInt9853 = i_4;
        return i_5;
    }

    public static int method15999(byte[] bytes_2, int[] ints_3, int i_4, int i_5, int i_6, int i_7, int i_9, int i_10, MixedAudioStream class282Sub15Sub5Sub211, int i_12, int i_13) {
        int i_81 = 0;
        int i_11 = 0;
        int i_01 = 0;
        if (i_12 == 0 || (i_81 = i_5 + (i_10 + 256 - i_4 + i_12) / i_12) > i_9) {
            i_81 = i_9;
        }

        i_5 <<= 1;

        int i_10001;
        for (i_81 <<= 1; i_5 < i_81; i_4 += i_12) {
            i_11 = i_4 >> 8;
            byte b_14 = bytes_2[i_11 - 1];
            i_01 = (bytes_2[i_11] - b_14) * (i_4 & 0xff) + (b_14 << 8);
            i_10001 = i_5++;
            ints_3[i_10001] += i_01 * i_6 >> 6;
            i_10001 = i_5++;
            ints_3[i_10001] += i_01 * i_7 >> 6;
        }

        if (i_12 == 0 || (i_81 = (i_5 >> 1) + (i_10 - i_4 + i_12) / i_12) > i_9) {
            i_81 = i_9;
        }

        i_81 <<= 1;

        for (i_11 = i_13; i_5 < i_81; i_4 += i_12) {
            i_01 = (i_11 << 8) + (i_4 & 0xff) * (bytes_2[i_4 >> 8] - i_11);
            i_10001 = i_5++;
            ints_3[i_10001] += i_01 * i_6 >> 6;
            i_10001 = i_5++;
            ints_3[i_10001] += i_01 * i_7 >> 6;
        }

        class282Sub15Sub5Sub211.anInt9853 = i_4;
        return i_5 >> 1;
    }

    public static int method16000(byte[] bytes_0, int[] ints_1, int i_2, int i_3, int i_4, int i_5, int i_7, int i_8, MixedAudioStream class282Sub15Sub5Sub29) {
        int i_61 = 0;
        i_2 >>= 8;
        i_8 >>= 8;
        i_4 <<= 2;
        i_5 <<= 2;
        if ((i_61 = i_3 + i_8 - i_2) > i_7) {
            i_61 = i_7;
        }

        class282Sub15Sub5Sub29.anInt9845 += class282Sub15Sub5Sub29.anInt9852 * (i_61 - i_3);
        class282Sub15Sub5Sub29.anInt9850 += class282Sub15Sub5Sub29.anInt9851 * (i_61 - i_3);

        int i_10001;
        for (i_61 -= 3; i_3 < i_61; i_4 += i_5) {
            i_10001 = i_3++;
            ints_1[i_10001] += bytes_0[i_2++] * i_4;
            i_4 += i_5;
            i_10001 = i_3++;
            ints_1[i_10001] += bytes_0[i_2++] * i_4;
            i_4 += i_5;
            i_10001 = i_3++;
            ints_1[i_10001] += bytes_0[i_2++] * i_4;
            i_4 += i_5;
            i_10001 = i_3++;
            ints_1[i_10001] += bytes_0[i_2++] * i_4;
        }

        for (i_61 += 3; i_3 < i_61; i_4 += i_5) {
            i_10001 = i_3++;
            ints_1[i_10001] += bytes_0[i_2++] * i_4;
        }

        class282Sub15Sub5Sub29.anInt9848 = i_4 >> 2;
        class282Sub15Sub5Sub29.anInt9853 = i_2 << 8;
        return i_3;
    }

    public static int method16001(byte[] bytes_1, int[] ints_2, int i_3, int i_4, int i_5, int i_6, int i_7, int i_8, int i_10, int i_11, MixedAudioStream class282Sub15Sub5Sub212) {
        int i_91 = 0;
        i_3 >>= 8;
        i_11 >>= 8;
        i_5 <<= 2;
        i_6 <<= 2;
        i_7 <<= 2;
        i_8 <<= 2;
        if ((i_91 = i_11 + i_4 - i_3) > i_10) {
            i_91 = i_10;
        }

        class282Sub15Sub5Sub212.anInt9848 += class282Sub15Sub5Sub212.anInt9849 * (i_91 - i_4);
        i_4 <<= 1;
        i_91 <<= 1;

        byte b_13;
        int i_10001;
        for (i_91 -= 6; i_4 < i_91; i_6 += i_8) {
            b_13 = bytes_1[i_3++];
            i_10001 = i_4++;
            ints_2[i_10001] += b_13 * i_5;
            i_5 += i_7;
            i_10001 = i_4++;
            ints_2[i_10001] += b_13 * i_6;
            i_6 += i_8;
            b_13 = bytes_1[i_3++];
            i_10001 = i_4++;
            ints_2[i_10001] += b_13 * i_5;
            i_5 += i_7;
            i_10001 = i_4++;
            ints_2[i_10001] += b_13 * i_6;
            i_6 += i_8;
            b_13 = bytes_1[i_3++];
            i_10001 = i_4++;
            ints_2[i_10001] += b_13 * i_5;
            i_5 += i_7;
            i_10001 = i_4++;
            ints_2[i_10001] += b_13 * i_6;
            i_6 += i_8;
            b_13 = bytes_1[i_3++];
            i_10001 = i_4++;
            ints_2[i_10001] += b_13 * i_5;
            i_5 += i_7;
            i_10001 = i_4++;
            ints_2[i_10001] += b_13 * i_6;
        }

        for (i_91 += 6; i_4 < i_91; i_6 += i_8) {
            b_13 = bytes_1[i_3++];
            i_10001 = i_4++;
            ints_2[i_10001] += b_13 * i_5;
            i_5 += i_7;
            i_10001 = i_4++;
            ints_2[i_10001] += b_13 * i_6;
        }

        class282Sub15Sub5Sub212.anInt9845 = i_5 >> 2;
        class282Sub15Sub5Sub212.anInt9850 = i_6 >> 2;
        class282Sub15Sub5Sub212.anInt9853 = i_3 << 8;
        return i_4 >> 1;
    }

    public static int method16002(byte[] bytes_0, int[] ints_1, int i_2, int i_3, int i_4, int i_5, int i_7, int i_8, MixedAudioStream class282Sub15Sub5Sub29) {
        int i_61 = 0;
        i_2 >>= 8;
        i_8 >>= 8;
        i_4 <<= 2;
        i_5 <<= 2;
        if ((i_61 = i_3 + i_2 - (i_8 - 1)) > i_7) {
            i_61 = i_7;
        }

        class282Sub15Sub5Sub29.anInt9845 += class282Sub15Sub5Sub29.anInt9852 * (i_61 - i_3);
        class282Sub15Sub5Sub29.anInt9850 += class282Sub15Sub5Sub29.anInt9851 * (i_61 - i_3);

        int i_10001;
        for (i_61 -= 3; i_3 < i_61; i_4 += i_5) {
            i_10001 = i_3++;
            ints_1[i_10001] += bytes_0[i_2--] * i_4;
            i_4 += i_5;
            i_10001 = i_3++;
            ints_1[i_10001] += bytes_0[i_2--] * i_4;
            i_4 += i_5;
            i_10001 = i_3++;
            ints_1[i_10001] += bytes_0[i_2--] * i_4;
            i_4 += i_5;
            i_10001 = i_3++;
            ints_1[i_10001] += bytes_0[i_2--] * i_4;
        }

        for (i_61 += 3; i_3 < i_61; i_4 += i_5) {
            i_10001 = i_3++;
            ints_1[i_10001] += bytes_0[i_2--] * i_4;
        }

        class282Sub15Sub5Sub29.anInt9848 = i_4 >> 2;
        class282Sub15Sub5Sub29.anInt9853 = i_2 << 8;
        return i_3;
    }

    public static int method16003(byte[] bytes_1, int[] ints_2, int i_3, int i_4, int i_5, int i_6, int i_7, int i_8, int i_10, int i_11, MixedAudioStream class282Sub15Sub5Sub212) {
        int i_91 = 0;
        i_3 >>= 8;
        i_11 >>= 8;
        i_5 <<= 2;
        i_6 <<= 2;
        i_7 <<= 2;
        i_8 <<= 2;
        if ((i_91 = i_3 + i_4 - (i_11 - 1)) > i_10) {
            i_91 = i_10;
        }

        class282Sub15Sub5Sub212.anInt9848 += class282Sub15Sub5Sub212.anInt9849 * (i_91 - i_4);
        i_4 <<= 1;
        i_91 <<= 1;

        byte b_13;
        int i_10001;
        for (i_91 -= 6; i_4 < i_91; i_6 += i_8) {
            b_13 = bytes_1[i_3--];
            i_10001 = i_4++;
            ints_2[i_10001] += b_13 * i_5;
            i_5 += i_7;
            i_10001 = i_4++;
            ints_2[i_10001] += b_13 * i_6;
            i_6 += i_8;
            b_13 = bytes_1[i_3--];
            i_10001 = i_4++;
            ints_2[i_10001] += b_13 * i_5;
            i_5 += i_7;
            i_10001 = i_4++;
            ints_2[i_10001] += b_13 * i_6;
            i_6 += i_8;
            b_13 = bytes_1[i_3--];
            i_10001 = i_4++;
            ints_2[i_10001] += b_13 * i_5;
            i_5 += i_7;
            i_10001 = i_4++;
            ints_2[i_10001] += b_13 * i_6;
            i_6 += i_8;
            b_13 = bytes_1[i_3--];
            i_10001 = i_4++;
            ints_2[i_10001] += b_13 * i_5;
            i_5 += i_7;
            i_10001 = i_4++;
            ints_2[i_10001] += b_13 * i_6;
        }

        for (i_91 += 6; i_4 < i_91; i_6 += i_8) {
            b_13 = bytes_1[i_3--];
            i_10001 = i_4++;
            ints_2[i_10001] += b_13 * i_5;
            i_5 += i_7;
            i_10001 = i_4++;
            ints_2[i_10001] += b_13 * i_6;
        }

        class282Sub15Sub5Sub212.anInt9845 = i_5 >> 2;
        class282Sub15Sub5Sub212.anInt9850 = i_6 >> 2;
        class282Sub15Sub5Sub212.anInt9853 = i_3 << 8;
        return i_4 >> 1;
    }

    public static int method16004(byte[] bytes_2, int[] ints_3, int i_4, int i_5, int i_6, int i_7, int i_9, int i_10, MixedAudioStream class282Sub15Sub5Sub211, int i_12, int i_13) {
        int i_81 = 0;
        int i_11 = 0;
        class282Sub15Sub5Sub211.anInt9845 -= class282Sub15Sub5Sub211.anInt9852 * i_5;
        class282Sub15Sub5Sub211.anInt9850 -= class282Sub15Sub5Sub211.anInt9851 * i_5;
        if (i_12 == 0 || (i_81 = i_5 + (i_10 - i_4 + i_12 - 257) / i_12) > i_9) {
            i_81 = i_9;
        }

        byte b_14;
        int i_10001;
        while (i_5 < i_81) {
            i_11 = i_4 >> 8;
            b_14 = bytes_2[i_11];
            i_10001 = i_5++;
            ints_3[i_10001] += ((b_14 << 8) + (bytes_2[i_11 + 1] - b_14) * (i_4 & 0xff)) * i_6 >> 6;
            i_6 += i_7;
            i_4 += i_12;
        }

        if (i_12 == 0 || (i_81 = i_5 + (i_10 - i_4 + i_12 - 1) / i_12) > i_9) {
            i_81 = i_9;
        }

        for (i_11 = i_13; i_5 < i_81; i_4 += i_12) {
            b_14 = bytes_2[i_4 >> 8];
            i_10001 = i_5++;
            ints_3[i_10001] += ((b_14 << 8) + (i_11 - b_14) * (i_4 & 0xff)) * i_6 >> 6;
            i_6 += i_7;
        }

        class282Sub15Sub5Sub211.anInt9845 += class282Sub15Sub5Sub211.anInt9852 * i_5;
        class282Sub15Sub5Sub211.anInt9850 += class282Sub15Sub5Sub211.anInt9851 * i_5;
        class282Sub15Sub5Sub211.anInt9848 = i_6;
        class282Sub15Sub5Sub211.anInt9853 = i_4;
        return i_5;
    }

    public static int method16005(byte[] bytes_2, int[] ints_3, int i_4, int i_5, int i_6, int i_7, int i_8, int i_9, int i_11, int i_12, MixedAudioStream class282Sub15Sub5Sub213, int i_14, int i_15) {
        int i_101 = 0;
        int i_13 = 0;
        int i_01 = 0;
        class282Sub15Sub5Sub213.anInt9848 -= i_5 * class282Sub15Sub5Sub213.anInt9849;
        if (i_14 == 0 || (i_101 = i_5 + (i_12 - i_4 + i_14 - 257) / i_14) > i_11) {
            i_101 = i_11;
        }

        i_5 <<= 1;

        byte b_16;
        int i_10001;
        for (i_101 <<= 1; i_5 < i_101; i_4 += i_14) {
            i_13 = i_4 >> 8;
            b_16 = bytes_2[i_13];
            i_01 = (b_16 << 8) + (i_4 & 0xff) * (bytes_2[i_13 + 1] - b_16);
            i_10001 = i_5++;
            ints_3[i_10001] += i_01 * i_6 >> 6;
            i_6 += i_8;
            i_10001 = i_5++;
            ints_3[i_10001] += i_01 * i_7 >> 6;
            i_7 += i_9;
        }

        if (i_14 == 0 || (i_101 = (i_5 >> 1) + (i_12 - i_4 + i_14 - 1) / i_14) > i_11) {
            i_101 = i_11;
        }

        i_101 <<= 1;

        for (i_13 = i_15; i_5 < i_101; i_4 += i_14) {
            b_16 = bytes_2[i_4 >> 8];
            i_01 = (b_16 << 8) + (i_13 - b_16) * (i_4 & 0xff);
            i_10001 = i_5++;
            ints_3[i_10001] += i_01 * i_6 >> 6;
            i_6 += i_8;
            i_10001 = i_5++;
            ints_3[i_10001] += i_01 * i_7 >> 6;
            i_7 += i_9;
        }

        i_5 >>= 1;
        class282Sub15Sub5Sub213.anInt9848 += class282Sub15Sub5Sub213.anInt9849 * i_5;
        class282Sub15Sub5Sub213.anInt9845 = i_6;
        class282Sub15Sub5Sub213.anInt9850 = i_7;
        class282Sub15Sub5Sub213.anInt9853 = i_4;
        return i_5;
    }

    public static int method16006(byte[] bytes_2, int[] ints_3, int i_4, int i_5, int i_6, int i_7, int i_9, int i_10, MixedAudioStream class282Sub15Sub5Sub211, int i_12, int i_13) {
        int i_81 = 0;
        int i_11 = 0;
        class282Sub15Sub5Sub211.anInt9845 -= class282Sub15Sub5Sub211.anInt9852 * i_5;
        class282Sub15Sub5Sub211.anInt9850 -= class282Sub15Sub5Sub211.anInt9851 * i_5;
        if (i_12 == 0 || (i_81 = i_5 + (i_10 + 256 - i_4 + i_12) / i_12) > i_9) {
            i_81 = i_9;
        }

        int i_10001;
        while (i_5 < i_81) {
            i_11 = i_4 >> 8;
            byte b_14 = bytes_2[i_11 - 1];
            i_10001 = i_5++;
            ints_3[i_10001] += ((b_14 << 8) + (bytes_2[i_11] - b_14) * (i_4 & 0xff)) * i_6 >> 6;
            i_6 += i_7;
            i_4 += i_12;
        }

        if (i_12 == 0 || (i_81 = i_5 + (i_10 - i_4 + i_12) / i_12) > i_9) {
            i_81 = i_9;
        }

        int i_01 = i_13;

        for (i_11 = i_12; i_5 < i_81; i_4 += i_11) {
            i_10001 = i_5++;
            ints_3[i_10001] += ((i_01 << 8) + (bytes_2[i_4 >> 8] - i_01) * (i_4 & 0xff)) * i_6 >> 6;
            i_6 += i_7;
        }

        class282Sub15Sub5Sub211.anInt9845 += class282Sub15Sub5Sub211.anInt9852 * i_5;
        class282Sub15Sub5Sub211.anInt9850 += class282Sub15Sub5Sub211.anInt9851 * i_5;
        class282Sub15Sub5Sub211.anInt9848 = i_6;
        class282Sub15Sub5Sub211.anInt9853 = i_4;
        return i_5;
    }

    public static int method16007(byte[] bytes_2, int[] ints_3, int i_4, int i_5, int i_6, int i_7, int i_8, int i_9, int i_11, int i_12, MixedAudioStream class282Sub15Sub5Sub213, int i_14, int i_15) {
        int i_101 = 0;
        int i_13 = 0;
        int i_01 = 0;
        class282Sub15Sub5Sub213.anInt9848 -= i_5 * class282Sub15Sub5Sub213.anInt9849;
        if (i_14 == 0 || (i_101 = i_5 + (i_12 + 256 - i_4 + i_14) / i_14) > i_11) {
            i_101 = i_11;
        }

        i_5 <<= 1;

        int i_10001;
        for (i_101 <<= 1; i_5 < i_101; i_4 += i_14) {
            i_13 = i_4 >> 8;
            byte b_16 = bytes_2[i_13 - 1];
            i_01 = (bytes_2[i_13] - b_16) * (i_4 & 0xff) + (b_16 << 8);
            i_10001 = i_5++;
            ints_3[i_10001] += i_01 * i_6 >> 6;
            i_6 += i_8;
            i_10001 = i_5++;
            ints_3[i_10001] += i_01 * i_7 >> 6;
            i_7 += i_9;
        }

        if (i_14 == 0 || (i_101 = (i_5 >> 1) + (i_12 - i_4 + i_14) / i_14) > i_11) {
            i_101 = i_11;
        }

        i_101 <<= 1;

        for (i_13 = i_15; i_5 < i_101; i_4 += i_14) {
            i_01 = (i_13 << 8) + (i_4 & 0xff) * (bytes_2[i_4 >> 8] - i_13);
            i_10001 = i_5++;
            ints_3[i_10001] += i_01 * i_6 >> 6;
            i_6 += i_8;
            i_10001 = i_5++;
            ints_3[i_10001] += i_01 * i_7 >> 6;
            i_7 += i_9;
        }

        i_5 >>= 1;
        class282Sub15Sub5Sub213.anInt9848 += class282Sub15Sub5Sub213.anInt9849 * i_5;
        class282Sub15Sub5Sub213.anInt9845 = i_6;
        class282Sub15Sub5Sub213.anInt9850 = i_7;
        class282Sub15Sub5Sub213.anInt9853 = i_4;
        return i_5;
    }

    public static int method16019(byte[] bytes_2, int[] ints_3, int i_4, int i_5, int i_6, int i_7, int i_9, int i_10, MixedAudioStream class282Sub15Sub5Sub211, int i_12, int i_13) {
        int i_81 = 0;
        int i_11 = 0;
        int i_01 = 0;
        if (i_12 == 0 || (i_81 = i_5 + (i_10 - i_4 + i_12 - 257) / i_12) > i_9) {
            i_81 = i_9;
        }

        i_5 <<= 1;

        byte b_14;
        int i_10001;
        for (i_81 <<= 1; i_5 < i_81; i_4 += i_12) {
            i_11 = i_4 >> 8;
            b_14 = bytes_2[i_11];
            i_01 = (b_14 << 8) + (i_4 & 0xff) * (bytes_2[i_11 + 1] - b_14);
            i_10001 = i_5++;
            ints_3[i_10001] += i_01 * i_6 >> 6;
            i_10001 = i_5++;
            ints_3[i_10001] += i_01 * i_7 >> 6;
        }

        if (i_12 == 0 || (i_81 = (i_5 >> 1) + (i_10 - i_4 + i_12 - 1) / i_12) > i_9) {
            i_81 = i_9;
        }

        i_81 <<= 1;

        for (i_11 = i_13; i_5 < i_81; i_4 += i_12) {
            b_14 = bytes_2[i_4 >> 8];
            i_01 = (b_14 << 8) + (i_11 - b_14) * (i_4 & 0xff);
            i_10001 = i_5++;
            ints_3[i_10001] += i_01 * i_6 >> 6;
            i_10001 = i_5++;
            ints_3[i_10001] += i_01 * i_7 >> 6;
        }

        class282Sub15Sub5Sub211.anInt9853 = i_4;
        return i_5 >> 1;
    }

    public static int method16031(byte[] bytes_1, int[] ints_2, int i_3, int i_4, int i_5, int i_6, int i_8, int i_9, MixedAudioStream class282Sub15Sub5Sub210) {
        int i_71 = 0;
        i_3 >>= 8;
        i_9 >>= 8;
        i_5 <<= 2;
        i_6 <<= 2;
        if ((i_71 = i_4 + i_9 - i_3) > i_8) {
            i_71 = i_8;
        }

        i_4 <<= 1;
        i_71 <<= 1;

        int i_10001;
        byte b_11;
        for (i_71 -= 6; i_4 < i_71; ints_2[i_10001] += b_11 * i_6) {
            b_11 = bytes_1[i_3++];
            i_10001 = i_4++;
            ints_2[i_10001] += b_11 * i_5;
            i_10001 = i_4++;
            ints_2[i_10001] += b_11 * i_6;
            b_11 = bytes_1[i_3++];
            i_10001 = i_4++;
            ints_2[i_10001] += b_11 * i_5;
            i_10001 = i_4++;
            ints_2[i_10001] += b_11 * i_6;
            b_11 = bytes_1[i_3++];
            i_10001 = i_4++;
            ints_2[i_10001] += b_11 * i_5;
            i_10001 = i_4++;
            ints_2[i_10001] += b_11 * i_6;
            b_11 = bytes_1[i_3++];
            i_10001 = i_4++;
            ints_2[i_10001] += b_11 * i_5;
            i_10001 = i_4++;
        }

        for (i_71 += 6; i_4 < i_71; ints_2[i_10001] += b_11 * i_6) {
            b_11 = bytes_1[i_3++];
            i_10001 = i_4++;
            ints_2[i_10001] += b_11 * i_5;
            i_10001 = i_4++;
        }

        class282Sub15Sub5Sub210.anInt9853 = i_3 << 8;
        return i_4 >> 1;
    }

    @Override
    public synchronized void fill(int[] ints_1, int i_2, int i_3) {
        if (super.volume == 0 && super.anInt9858 == 0) {
            skip(i_3);
        } else {
            int i_4 = super.start << 8;
            int i_5 = super.end << 8;
            int i_6 = ((MixedAudioBuffer) super.buffer).samples.length << 8;
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
                        i_8 = method16018(ints_1, i_2, i_4, i_3, ((MixedAudioBuffer) super.buffer).samples[super.start]);
                        if (super.anInt9853 >= i_4) {
                            return;
                        }

                        super.anInt9853 = i_4 + i_4 - 1 - super.anInt9853;
                        super.sampleRate = -(super.sampleRate * 1927994969) * -177305111;
                    }

                    while (true) {
                        i_8 = method15990(ints_1, i_8, i_5, i_3, ((MixedAudioBuffer) super.buffer).samples[super.end - 1]);
                        if (super.anInt9853 < i_5) {
                            break;
                        }

                        super.anInt9853 = i_5 + i_5 - 1 - super.anInt9853;
                        super.sampleRate = -(super.sampleRate * 1927994969) * -177305111;
                        i_8 = method16018(ints_1, i_8, i_4, i_3, ((MixedAudioBuffer) super.buffer).samples[super.start]);
                        if (super.anInt9853 >= i_4) {
                            break;
                        }

                        super.anInt9853 = i_4 + i_4 - 1 - super.anInt9853;
                        super.sampleRate = -(super.sampleRate * 1927994969) * -177305111;
                    }
                } else if (super.sampleRate < 0) {
                    while (true) {
                        i_8 = method16018(ints_1, i_8, i_4, i_3, ((MixedAudioBuffer) super.buffer).samples[super.end - 1]);
                        if (super.anInt9853 >= i_4) {
                            break;
                        }

                        super.anInt9853 = i_5 - 1 - (i_5 - 1 - super.anInt9853) % i_7;
                    }
                } else {
                    while (true) {
                        i_8 = method15990(ints_1, i_8, i_5, i_3, ((MixedAudioBuffer) super.buffer).samples[super.start]);
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
                                i_8 = method16018(ints_1, i_2, i_4, i_3, ((MixedAudioBuffer) super.buffer).samples[super.start]);
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
                                i_8 = method15990(ints_1, i_8, i_5, i_3, ((MixedAudioBuffer) super.buffer).samples[super.end - 1]);
                                if (super.anInt9853 < i_5) {
                                    return;
                                }

                                super.anInt9853 = i_5 + i_5 - 1 - super.anInt9853;
                                super.sampleRate = -(super.sampleRate * 1927994969) * -177305111;
                                if (--super.anInt9854 == 0) {
                                    break;
                                }

                                i_8 = method16018(ints_1, i_8, i_4, i_3, ((MixedAudioBuffer) super.buffer).samples[super.start]);
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
                                i_8 = method16018(ints_1, i_8, i_4, i_3, ((MixedAudioBuffer) super.buffer).samples[super.end - 1]);
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
                                i_8 = method15990(ints_1, i_8, i_5, i_3, ((MixedAudioBuffer) super.buffer).samples[super.start]);
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
                    method16018(ints_1, i_8, 0, i_3, 0);
                    if (super.anInt9853 < 0) {
                        super.anInt9853 = -1;
                        method15354();
                        unlink();
                    }
                } else {
                    method15990(ints_1, i_8, i_6, i_3, 0);
                    if (super.anInt9853 >= i_6) {
                        super.anInt9853 = i_6;
                        method15354();
                        unlink();
                    }
                }
            }
        }

    }

    public int method15990(int[] ints_1, int i_2, int i_3, int i_4, int i_5) {
        do {
            if (super.anInt9858 <= 0) {
                if (super.sampleRate == 256 && (super.anInt9853 & 0xff) == 0) {
                    if (PcmPlayerManager.sterio) {
                        return method16031(((MixedAudioBuffer) super.buffer).samples, ints_1, super.anInt9853, i_2, super.anInt9845, super.anInt9850, i_4, i_3, this);
                    }

                    return method15992(((MixedAudioBuffer) super.buffer).samples, ints_1, super.anInt9853, i_2, super.anInt9848, i_4, i_3, this);
                }

                if (PcmPlayerManager.sterio) {
                    return method16019(((MixedAudioBuffer) super.buffer).samples, ints_1, super.anInt9853, i_2, super.anInt9845, super.anInt9850, i_4, i_3, this, super.sampleRate, i_5);
                }

                return method15996(((MixedAudioBuffer) super.buffer).samples, ints_1, super.anInt9853, i_2, super.anInt9848, i_4, i_3, this, super.sampleRate, i_5);
            }

            int i_6 = i_2 + super.anInt9858;
            if (i_6 > i_4) {
                i_6 = i_4;
            }

            super.anInt9858 += i_2;
            if (super.sampleRate == 256 && (super.anInt9853 & 0xff) == 0) {
                if (PcmPlayerManager.sterio) {
                    i_2 = method16001(((MixedAudioBuffer) super.buffer).samples, ints_1, super.anInt9853, i_2, super.anInt9845, super.anInt9850, super.anInt9852, super.anInt9851, i_6, i_3, this);
                } else {
                    i_2 = method16000(((MixedAudioBuffer) super.buffer).samples, ints_1, super.anInt9853, i_2, super.anInt9848, super.anInt9849, i_6, i_3, this);
                }
            } else if (PcmPlayerManager.sterio) {
                i_2 = method16005(((MixedAudioBuffer) super.buffer).samples, ints_1, super.anInt9853, i_2, super.anInt9845, super.anInt9850, super.anInt9852, super.anInt9851, i_6, i_3, this, super.sampleRate, i_5);
            } else {
                i_2 = method16004(((MixedAudioBuffer) super.buffer).samples, ints_1, super.anInt9853, i_2, super.anInt9848, super.anInt9849, i_6, i_3, this, super.sampleRate, i_5);
            }

            super.anInt9858 -= i_2;
            if (super.anInt9858 != 0) {
                return i_2;
            }
        } while (!method15324());

        return i_4;
    }

    public int method16018(int[] ints_1, int i_2, int i_3, int i_4, int i_5) {
        do {
            if (super.anInt9858 <= 0) {
                if (super.sampleRate == -256 && (super.anInt9853 & 0xff) == 0) {
                    if (PcmPlayerManager.sterio) {
                        return method15995(((MixedAudioBuffer) super.buffer).samples, ints_1, super.anInt9853, i_2, super.anInt9845, super.anInt9850, i_4, i_3, this);
                    }

                    return method15991(((MixedAudioBuffer) super.buffer).samples, ints_1, super.anInt9853, i_2, super.anInt9848, i_4, i_3, this);
                }

                if (PcmPlayerManager.sterio) {
                    return method15999(((MixedAudioBuffer) super.buffer).samples, ints_1, super.anInt9853, i_2, super.anInt9845, super.anInt9850, i_4, i_3, this, super.sampleRate, i_5);
                }

                return method15998(((MixedAudioBuffer) super.buffer).samples, ints_1, super.anInt9853, i_2, super.anInt9848, i_4, i_3, this, super.sampleRate, i_5);
            }

            int i_6 = i_2 + super.anInt9858;
            if (i_6 > i_4) {
                i_6 = i_4;
            }

            super.anInt9858 += i_2;
            if (super.sampleRate == -256 && (super.anInt9853 & 0xff) == 0) {
                if (PcmPlayerManager.sterio) {
                    i_2 = method16003(((MixedAudioBuffer) super.buffer).samples, ints_1, super.anInt9853, i_2, super.anInt9845, super.anInt9850, super.anInt9852, super.anInt9851, i_6, i_3, this);
                } else {
                    i_2 = method16002(((MixedAudioBuffer) super.buffer).samples, ints_1, super.anInt9853, i_2, super.anInt9848, super.anInt9849, i_6, i_3, this);
                }
            } else if (PcmPlayerManager.sterio) {
                i_2 = method16007(((MixedAudioBuffer) super.buffer).samples, ints_1, super.anInt9853, i_2, super.anInt9845, super.anInt9850, super.anInt9852, super.anInt9851, i_6, i_3, this, super.sampleRate, i_5);
            } else {
                i_2 = method16006(((MixedAudioBuffer) super.buffer).samples, ints_1, super.anInt9853, i_2, super.anInt9848, super.anInt9849, i_6, i_3, this, super.sampleRate, i_5);
            }

            super.anInt9858 -= i_2;
            if (super.anInt9858 != 0) {
                return i_2;
            }
        } while (!method15324());

        return i_4;
    }

}
