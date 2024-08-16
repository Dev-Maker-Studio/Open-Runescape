package com.jagex.game.runetek5.tools;

/**
 * 
 * @author Steven - 29/07/2023
 * Reference: https://gist.github.com/bl00dshooter/995695
 *
 */
public class Isaac {

    public int count;

    public int counter;

    public int lastResult;

    public int accumulator;

    public final int[] memory = new int[256];

    public final int[] results = new int[256];
    
    private static final int RATIO = -1640531527;

    public Isaac(int[] seed) {
        System.arraycopy(seed, 0, this.results, 0, seed.length);
        init();
    }

    public void init() {
        int a, b, c, d, e, f, g, h;
        a = b = c = d = e = f = g = h = RATIO;
        for (int i = 0; i < 4; i++) {
            h ^= g << 11;
            e += h;
            g += f;
            g ^= f >>> 2;
            d += g;
            f += e;
            f ^= e << 8;
            c += f;
            e += d;
            e ^= d >>> 16;
            b += e;
            d += c;
            d ^= c << 10;
            a += d;
            c += b;
            c ^= b >>> 4;
            h += c;
            b += a;
            b ^= a << 8;
            g += b;
            a += h;
            a ^= h >>> 9;
            f += a;
            h += g;
        }
        for (int i = 0; i < 256; i += 8) {
            h += this.results[i];
            g += this.results[i + 1];
            f += this.results[i + 2];
            e += this.results[i + 3];
            d += this.results[i + 4];
            c += this.results[i + 5];
            b += this.results[i + 6];
            a += this.results[i + 7];
            h ^= g << 11;
            e += h;
            g += f;
            g ^= f >>> 2;
            d += g;
            f += e;
            f ^= e << 8;
            c += f;
            e += d;
            e ^= d >>> 16;
            b += e;
            d += c;
            d ^= c << 10;
            a += d;
            c += b;
            c ^= b >>> 4;
            h += c;
            b += a;
            b ^= a << 8;
            g += b;
            a += h;
            a ^= h >>> 9;
            f += a;
            h += g;
            this.memory[i] = h;
            this.memory[i + 1] = g;
            this.memory[i + 2] = f;
            this.memory[i + 3] = e;
            this.memory[i + 4] = d;
            this.memory[i + 5] = c;
            this.memory[i + 6] = b;
            this.memory[i + 7] = a;
        }
        for (int i = 0; i < 256; i += 8) {
            h += this.memory[i];
            g += this.memory[i + 1];
            f += this.memory[i + 2];
            e += this.memory[i + 3];
            d += this.memory[i + 4];
            c += this.memory[i + 5];
            b += this.memory[i + 6];
            a += this.memory[i + 7];
            h ^= g << 11;
            e += h;
            g += f;
            g ^= f >>> 2;
            d += g;
            f += e;
            f ^= e << 8;
            c += f;
            e += d;
            e ^= d >>> 16;
            b += e;
            d += c;
            d ^= c << 10;
            a += d;
            c += b;
            c ^= b >>> 4;
            h += c;
            b += a;
            b ^= a << 8;
            g += b;
            a += h;
            a ^= h >>> 9;
            f += a;
            h += g;
            this.memory[i] = h;
            this.memory[i + 1] = g;
            this.memory[i + 2] = f;
            this.memory[i + 3] = e;
            this.memory[i + 4] = d;
            this.memory[i + 5] = c;
            this.memory[i + 6] = b;
            this.memory[i + 7] = a;
        }
        isaac();
        this.count = 256;
    }

    public int getNextKey() {
        if (this.count == 0) {
            isaac();
            this.count = 256;
        }
        return (results[(count -= 1)]);
    }

    public int nextKey() {
        if (this.count == 0) {
            isaac();
            this.count = 256;
        }
        return (results[count - 1]);
    }

    public void isaac() {
        this.lastResult += ++this.counter;
        for (int i = 0; i < 256; i++) {
            int j = this.memory[i];
            if ((i & 0x2) == 0) {
                if ((i & 0x1) == 0) {
                    this.accumulator ^= this.accumulator << 13;
                } else {
                    this.accumulator ^= this.accumulator >>> 6;
                }
            } else if ((i & 0x1) == 0) {
                this.accumulator ^= this.accumulator << 2;
            } else {
                this.accumulator ^= this.accumulator >>> 16;
            }
            this.accumulator += this.memory[128 + i & 0xff];
            int k;
            this.memory[i] = k = this.memory[(j & 0x3fc) >> 2] + this.lastResult + this.accumulator;
            this.results[i] = this.lastResult = this.memory[(k >> 8 & 0x3fc) >> 2] + j;
        }
    }
}
