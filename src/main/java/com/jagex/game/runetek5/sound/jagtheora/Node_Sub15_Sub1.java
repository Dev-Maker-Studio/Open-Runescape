package com.jagex.game.runetek5.sound.jagtheora;

import com.jagex.game.runetek5.cache.node.NodeCollection;
import com.jagex.game.runetek5.sound.player.PcmPlayerManager;

public class Node_Sub15_Sub1 extends PcmStream {

    public static final MonoSterioAudioCache TIMED_CACHE;

    static {
        TIMED_CACHE = new MonoSterioAudioCache(MonoSterioCacheType.TIMED);
    }

    public boolean aBool9571;
    public int anInt9570;
    public int capacityUsed;
    public final NodeCollection nodeCollection = new NodeCollection();
    public int anInt9565 = 256;
    public int anInt9572 = 256;
    public int anInt9567;
    public boolean aBool9574;

    Node_Sub15_Sub1(int i_1) {
        this.anInt9567 = i_1;
    }

    synchronized void method14808() {
        this.aBool9571 = true;
    }

    @Override
	public
    synchronized void fill(int[] ints_1, int i_2, int i_3) {
        if (!this.aBool9574) {
            if (getFirst() == null) {
                if (this.aBool9571) {
                    unlink();
                    TIMED_CACHE.clear();
                }
            } else {
                int i_4 = i_3 + i_2;
                if (PcmPlayerManager.stereo) {
                    i_4 <<= 1;
                }
                byte b_5 = 0;
                byte b_6 = 0;
                if (this.anInt9567 == 2) {
                    b_6 = 1;
                }
                while (i_2 < i_4) {
                    MonoSterioData class282Sub397 = getFirst();
                    if (class282Sub397 == null) {
                        break;
                    }
                    short[][] shorts_8;
                    for (shorts_8 = class282Sub397.aShortArrayArray8003; i_2 < i_4 && this.anInt9570 < shorts_8[0].length; this.anInt9570++) {
                        if (PcmPlayerManager.stereo) {
                            ints_1[i_2++] = shorts_8[b_5][this.anInt9570] * this.anInt9565;
                            ints_1[i_2++] = shorts_8[b_6][this.anInt9570] * this.anInt9572;
                        } else {
                            int i_10001 = i_2++;
                            ints_1[i_10001] += shorts_8[b_6][this.anInt9570] * this.anInt9572 + shorts_8[b_5][this.anInt9570] * this.anInt9565;
                        }
                    }
                    if (this.anInt9570 >= shorts_8[0].length) {
                        method14813();
                    }
                }
            }
        }
    }

    synchronized int method14811() {
        return this.capacityUsed;
    }

    public synchronized MonoSterioData getFirst() {
        return (MonoSterioData) this.nodeCollection.getFirst();
    }

    public synchronized void method14813() {
        MonoSterioData class282Sub392 = getFirst();
        if (class282Sub392 != null) {
            class282Sub392.unlink();
            this.anInt9570 = 0;
            this.capacityUsed--;
            TIMED_CACHE.put(Long.valueOf(class282Sub392.method13294()), class282Sub392);
        }
    }

    MonoSterioData method14814(int i_1, double d_2) {
        long long_4 = this.anInt9567 | i_1;
        MonoSterioData class282Sub396 = TIMED_CACHE.get(Long.valueOf(long_4));
        if (class282Sub396 != null) {
            class282Sub396.aDouble8004 = d_2;
            TIMED_CACHE.pop(Long.valueOf(long_4));
        } else {
            class282Sub396 = new MonoSterioData(new short[this.anInt9567][i_1], d_2);
        }
        return class282Sub396;
    }

    synchronized void method14815(MonoSterioData class282Sub391) {
        while (this.capacityUsed >= 100) {
            this.nodeCollection.popHead();
            this.capacityUsed--;
        }
        this.nodeCollection.addFirst(class282Sub391);
        this.capacityUsed++;
    }

    public synchronized void method14816(boolean bool1) {
        this.aBool9574 = bool1;
    }

    @Override
    public int method12228() {
        return 1;
    }

    @Override
    public synchronized void skip(int i_1) {
        if (!this.aBool9574) {
            while (true) {
                MonoSterioData class282Sub392 = getFirst();
                if (class282Sub392 == null) {
                    if (this.aBool9571) {
                        unlink();
                        TIMED_CACHE.clear();
                    }
                    break;
                }
                if (class282Sub392.aShortArrayArray8003[0].length - this.anInt9570 > i_1) {
                    this.anInt9570 += i_1;
                    break;
                }
                i_1 -= class282Sub392.aShortArrayArray8003[0].length - this.anInt9570;
                method14813();
            }
        }
    }

    synchronized double method14819() {
        if (this.capacityUsed < 1) {
            return -1.0;
        } 
        MonoSterioData class282Sub392 = (MonoSterioData) this.nodeCollection.getFirst();
        return class282Sub392 == null ? -1.0 : class282Sub392.aDouble8004 - ((float) class282Sub392.aShortArrayArray8003[0].length / PcmPlayerManager.sampleRate);
    }

    public void method14820(int i_1) {
        this.anInt9565 = i_1;
        this.anInt9572 = i_1;
    }

    @Override
    public PcmStream getNextStream() {
        return null;
    }

    @Override
    public PcmStream getFirstStream() {
        return null;
    }
}
