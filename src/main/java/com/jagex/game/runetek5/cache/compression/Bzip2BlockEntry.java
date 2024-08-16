package com.jagex.game.runetek5.cache.compression;

public class Bzip2BlockEntry {

    int anInt5496;
    int anInt5499;
    byte[] compressed;
    int anInt5505;
    int decompressedLength;
    int anInt5508;
    byte aByte5509;
    int anInt5511;
    int anInt5513;
    int anInt5514;
    int anInt5515;
    int anInt5516;
    int anInt5518;
    int anInt5520;
    byte[] decompressed;
    int anInt5532;
    int anInt5498 = 6;

    int anInt5500 = 50;

    int anInt5501 = 18002;

    int anInt5512 = 258;

    int anInt5521 = 16;

    int anInt5522 = 4096;

    int startOffset;

    int anInt5506;

    final int[] unzftab;

    final int[] anIntArray5519;

    final boolean[] inUse;

    final boolean[] inUse16;

    final byte[] seqToUnseq;

    final byte[] yy;

    final int[] anIntArray5525;

    final byte[] selector;

    final byte[] selectorMtf;

    final byte[][] len;

    final int[][] limit;

    final int[][] base;

    final int[][] perm;

    final int[] minLens;

    Bzip2BlockEntry() {
        this.startOffset = 0;
        this.anInt5506 = 0;
        this.unzftab = new int[256];
        this.anIntArray5519 = new int[257];
        this.inUse = new boolean[256];
        this.inUse16 = new boolean[16];
        this.seqToUnseq = new byte[256];
        this.yy = new byte[4096];
        this.anIntArray5525 = new int[16];
        this.selector = new byte[18002];
        this.selectorMtf = new byte[18002];
        this.len = new byte[6][258];
        this.limit = new int[6][258];
        this.base = new int[6][258];
        this.perm = new int[6][258];
        this.minLens = new int[6];
    }

}
