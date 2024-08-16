package com.jagex.game.runetek5.config.midi.instrument;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.client.GameShell;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.sound.jagtheora.AudioBuffer;
import com.jagex.game.runetek5.utils.BufferUtils;

/**
 * 
 * @author Brainpower - 19/04/2022
 * @autor lequietriot - 13/08/2021 
 * Reference: https://github.com/lequietriot/RuneScape-MIDI-Player-Suite/blob/master/src/main/MusicSample.java
 *
 */
public class MusicSample extends Node<MusicSample> {

    public static boolean fullyCached;
    public static int blockSize0;
    public static int blockSize1;
    public static float[] window;
    public static MusicSampleCodebook[] codebooks;
    public static MusicSampleFloor[] floors;
    public static MusicSampleResidue[] residues;
    public static MusicSamepleMapping[] mappings;
    public static boolean[] noResidues;
    public static int[] modes;
    public static float[] CdR;
    public static float[] CdP;
    public static float[] CdV;
    public static int[] CdAv;
    public static float[] CdAg;
    public static float[] CdAq;
    public static float[] CdAj;
    public static int[] CdAr;
    public static byte[] instrumentData;
    public static int byteIndex;
    public static int root;
    public int sampleRate;
    public int sampleCount;
    public int start;
    public int end;
    public boolean loopConsistency;
    public byte[][] packets;
    public float[] T;
    public int Y;
    public int H;
    public boolean blockFlag;
    public float[] samples;
    public int sampleLength;
    public int soundIndices;
    public int audioBufferLength;
	public static final LinkedList<MusicSample> MUSIC_SAMPLES = new LinkedList<MusicSample>();

    public MusicSample(byte[] data) throws IOException {
        decode(data);
    }

    public static void initializeData(byte[] data) {
        setData(data);
        blockSize0 = 1 << getBits(4);
        blockSize1 = 1 << getBits(4);
        window = new float[blockSize1];

        for (int blockType = 0; blockType < 2; blockType++) {
            int blockSize = blockType != 0 ? blockSize1 : blockSize0;
            int halfBlock = blockSize >> 1;
        	int quarterBlock = blockSize >> 2;
        	int eightBlock = blockSize >> 3;
            float[] trig1 = new float[halfBlock];
            for (int mapping = 0; mapping < quarterBlock; mapping++) {
                trig1[mapping * 2] = (float) Math.cos((mapping * 4) * Math.PI / blockSize);
                trig1[mapping * 2 + 1] = -((float) Math.sin((mapping * 4) * Math.PI / blockSize));
            }
            float[] trig2 = new float[halfBlock];
            for (int mapping = 0; mapping < quarterBlock; mapping++) {
                trig2[mapping * 2] = (float) Math.cos((mapping * 2 + 1) * Math.PI / (blockSize * 2));
                trig2[mapping * 2 + 1] = (float) Math.sin((mapping * 2 + 1) * Math.PI / (blockSize * 2));
            }
            float[] trig3 = new float[quarterBlock];
            for (int time = 0; time < eightBlock; time++) {
                trig3[time * 2] = (float) Math.cos((time * 4 + 2) * Math.PI / blockSize);
                trig3[time * 2 + 1] = -((float) Math.sin((time * 4 + 2) * Math.PI / blockSize));
            }
            int[] ints_15 = new int[eightBlock];
            int i_10 = BufferUtils.ilog(eightBlock - 1);
            for (int i_11 = 0; i_11 < eightBlock; i_11++) {
                ints_15[i_11] = method285(i_11, i_10);
            }
            if (blockType != 0) {
                CdAg = trig1;
                CdAq = trig2;
                CdAj = trig3;
                CdAr = ints_15;
            } else {
                CdR = trig1;
                CdP = trig2;
                CdV = trig3;
                CdAv = ints_15;
            }
        }
        int codeBookCount = getBits(8) + 1;
        codebooks = new MusicSampleCodebook[codeBookCount];
        for (int index = 0; index < codeBookCount; index++) {
            codebooks[index] = new MusicSampleCodebook();
        }
        int skip = getBits(6) + 1;
        for (int residueCount = 0; residueCount < skip; residueCount++) {
            getBits(16);
        }
        int floorCount = getBits(6) + 1;
        floors = new MusicSampleFloor[floorCount];
        for (int residueCount = 0; residueCount < floorCount; residueCount++) {
            floors[residueCount] = new MusicSampleFloor();
        }
        int residueCount = getBits(6) + 1;
        residues = new MusicSampleResidue[residueCount];
        for (int mappingCount = 0; mappingCount < residueCount; mappingCount++) {
            residues[mappingCount] = new MusicSampleResidue();
        }
        int mappingCount = getBits(6) + 1;
        mappings = new MusicSamepleMapping[mappingCount];
        for (int mapping = 0; mapping < mappingCount; mapping++) {
            mappings[mapping] = new MusicSamepleMapping();
        }
        int noResidueCount = getBits(6) + 1;
        noResidues = new boolean[noResidueCount];
        modes = new int[noResidueCount];
        for (int index = 0; index < noResidueCount; index++) {
            noResidues[index] = getBit() != 0;
            getBits(16);
            getBits(16);
            modes[index] = getBits(8);
        }
        fullyCached = true;
    }

    public static void setData(byte[] bytes_0) {
        instrumentData = bytes_0;
        byteIndex = 0;
        root = 0;
    }

    public static int getBit() {
        int bit = instrumentData[byteIndex] >> root & 0x1;
        ++root;
        byteIndex += root >> 3;
        root &= 0x7;
        return bit;
    }

    public static int getBits(int bits) {
        int res = 0;

        int index;
        int bitIndex;
        for (index = 0; bits >= 8 - root; bits -= bitIndex) {
            bitIndex = 8 - root;
            int mask = (1 << bitIndex) - 1;
            res += (instrumentData[byteIndex] >> root & mask) << index;
            root = 0;
            byteIndex++;
            index += bitIndex;
        }

        if (bits > 0) {
            bitIndex = (1 << bits) - 1;
            res += (instrumentData[byteIndex] >> root & bitIndex) << index;
            root += bits;
        }

        return res;
    }

    public static boolean isLoaded(Js5ResourceProvider resource) {
        if (!fullyCached) {
            byte[] data = resource.getFile(0, 0);
            if (data == null) {
                return false;
            }
            initializeData(data);
        }
        return true;
    }

    public static MusicSample list(Js5ResourceProvider resource, int fileId) {
        if (!isLoaded(resource)) {
            resource.isFileCached(fileId);
            return null;
        } 
        byte[] data = resource.getFile(fileId);
        if (data == null) {
            return null;
        } 
        MusicSample instrument = null;
        try {
            instrument = new MusicSample(data);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return instrument;
    }

    public static MusicSample list(Js5ResourceProvider resource, int archiveId, int fileId) {
        if (!isLoaded(resource)) {
            resource.isCached(archiveId, fileId);
            return null;
        }
        byte[] data = resource.getFile(archiveId, fileId);
        if (data == null) {
            return null;
        } 
        MusicSample musicSample = null;
        try {
            musicSample = new MusicSample(data);
        } catch (IOException ioexception) {
            ioexception.printStackTrace();
        }
        return musicSample;
    }

    /**
     * https://www.cs.cmu.edu/afs/club/usr/cort/code/img2ktx/third_party/stb/stb_vorbis.c
     */
    public static float float32Unpack(int input) {
        int mantissa = input & 0x1fffff;
        int sign = input & -2147483648;
        int exponent = (input & 0x7fe00000) >> 21;
        if (sign != 0) {
            mantissa = -mantissa;
        }

        return (float) (mantissa * Math.pow(2.0D, exponent - 788));
    }

    public static void method7042() {
        int numSamples = MusicSample.MUSIC_SAMPLES.size();
        Iterator<MusicSample> sampleIt = MusicSample.MUSIC_SAMPLES.iterator();
        while (sampleIt.hasNext()) {
            MusicSample sample = sampleIt.next();
            sample.calculateAudioBufferLength(sample.getSampleLength() + sample.getSampleRate() / (2 * numSamples));
            if (!sample.method12276()) {
                sampleIt.remove();
            }
        }
    }

    public static int method285(int i_0, int i_1) {
        int i_3;
        for (i_3 = 0; i_1 > 0; --i_1) {
            i_3 = i_3 << 1 | i_0 & 0x1;
            i_0 >>>= 1;
        }
        return i_3;
    }

    public static void addIfAbsent(MusicSample musicSample) {
        if (!MusicSample.MUSIC_SAMPLES.contains(musicSample)) {
            MusicSample.MUSIC_SAMPLES.add(musicSample);
        }
    }

    public void decode(byte[] data) throws IOException {
        Packet stream = new Packet(data);
        this.sampleRate = stream.readInt();
        this.sampleCount = stream.readInt();
        this.start = stream.readInt();
        this.end = stream.readInt();
        if (this.end < 0) {
            this.end = ~this.end;
            this.loopConsistency = true;
        }
        int packetCount = stream.readInt();
        if (packetCount < 0) {
            throw new IOException();
        } 
        this.packets = new byte[packetCount][];
        for (int packet = 0; packet < packetCount; packet++) {
            int size = 0;
            int offset;
            do {
                offset = stream.readUnsignedByte();
                size += offset;
            } while (offset >= 255);
            byte[] packetData = new byte[size];
            stream.readBytes(packetData, 0, size);
            this.packets[packet] = packetData;
        }
    }

    public float[] mdctFloatCompute(int packetIndex) {
        setData(this.packets[packetIndex]);
        getBit();
        int modeNumber = getBits(BufferUtils.ilog(modes.length - 1));
        boolean blockSizeFlag = noResidues[modeNumber];
        int blocksize = blockSizeFlag ? blockSize1 : blockSize0;
        boolean previousWindowFlag = false;
        boolean nextWindowFlag = false;
        if (blockSizeFlag) {
            previousWindowFlag = getBit() != 0;
            nextWindowFlag = getBit() != 0;
        }

        int windowCenter = blocksize >> 1;
        int leftWindowStart;
        int leftWindowEnd;
        int leftN;
        if (blockSizeFlag && !previousWindowFlag) {
            leftWindowStart = (blocksize >> 2) - (blockSize0 >> 2);
            leftWindowEnd = (blockSize0 >> 2) + (blocksize >> 2);
            leftN = blockSize0 >> 1;
        } else {
            leftWindowStart = 0;
            leftWindowEnd = windowCenter;
            leftN = blocksize >> 1;
        }

        int rightWindowStart;
        int rightWindowEnd;
        int rightN;
        if (blockSizeFlag && !nextWindowFlag) {
            rightWindowStart = blocksize - (blocksize >> 2) - (blockSize0 >> 2);
            rightWindowEnd = (blockSize0 >> 2) + (blocksize - (blocksize >> 2));
            rightN = blockSize0 >> 1;
        } else {
            rightWindowStart = windowCenter;
            rightWindowEnd = blocksize;
            rightN = blocksize >> 1;
        }

        MusicSamepleMapping mapping = mappings[modes[modeNumber]];
        int submapNumber = mapping.mux;
        int floorNumber = mapping.submapFloors[submapNumber];
        boolean allFloorsEmpty = !floors[floorNumber].decodeFloor();

        for (int submap = 0; submap < mapping.submaps; submap++) {
            MusicSampleResidue residue = residues[mapping.submapResidues[submap]];
            float[] pcm = window;
            residue.decodeResidue(pcm, blocksize >> 1, allFloorsEmpty);
        }

        if (!allFloorsEmpty) {
            int floor = mapping.mux;
            int floorIndex = mapping.submapFloors[floor];
            floors[floorIndex].computeFloor(window, blocksize >> 1);
        }

        if (allFloorsEmpty) {
            for (int w = blocksize >> 1; w < blocksize; w++) {
                window[w] = 0.0F;
            }
        } else {
            int halfBlocksize = blocksize >> 1;
            int quarterBlocksize = blocksize >> 2;
            int eightBlocksize = blocksize >> 3;
            float[] pcm = window;

            for (int i = 0; i < halfBlocksize; i++) {
                pcm[i] *= 0.5F;
            }

            for (int i = halfBlocksize; i < blocksize; i++) {
                pcm[i] = -pcm[blocksize - i - 1];
            }

            float[] floats_44 = blockSizeFlag ? CdAg : CdR;
            float[] floats_23 = blockSizeFlag ? CdAq : CdP;
            float[] floats_24 = blockSizeFlag ? CdAj : CdV;
            int[] ints_25 = blockSizeFlag ? CdAr : CdAv;

            for (int i = 0; i < quarterBlocksize; i++) {
            	float f_27 = pcm[i * 4] - pcm[blocksize - i * 4 - 1];
            	float f_28 = pcm[i * 4 + 2] - pcm[blocksize - i * 4 - 3];
            	float f_29 = floats_44[i * 2];
            	float f_30 = floats_44[i * 2 + 1];
                pcm[blocksize - i * 4 - 1] = f_27 * f_29 - f_28 * f_30;
                pcm[blocksize - i * 4 - 3] = f_27 * f_30 + f_28 * f_29;
            }


            for (int i = 0; i < eightBlocksize; i++) {
            	float f_27 = pcm[halfBlocksize + i * 4 + 3];
            	float f_28 = pcm[halfBlocksize + i * 4 + 1];
            	float f_29 = pcm[i * 4 + 3];
            	float f_30 = pcm[i * 4 + 1];
                pcm[halfBlocksize + i * 4 + 3] = f_27 + f_29;
                pcm[halfBlocksize + i * 4 + 1] = f_28 + f_30;
                float f_31 = floats_44[halfBlocksize - 4 - i * 4];
                float f_32 = floats_44[halfBlocksize - 3 - i * 4];
                pcm[i * 4 + 3] = (f_27 - f_29) * f_31 - (f_28 - f_30) * f_32;
                pcm[i * 4 + 1] = (f_28 - f_30) * f_31 + (f_27 - f_29) * f_32;
            }

            int i_26 = BufferUtils.ilog(blocksize - 1);

            for (int i_45 = 0; i_45 < i_26 - 3; i_45++) {
            	int i_46 = blocksize >> i_45 + 2;
            	int i_47 = 8 << i_45;

                for (int i_48 = 0; i_48 < 2 << i_45; i_48++) {
                    int i_49 = blocksize - i_46 * i_48 * 2;
                    int i_50 = blocksize - i_46 * (i_48 * 2 + 1);

                    for (int i_33 = 0; i_33 < blocksize >> i_45 + 4; i_33++) {
                        int i_34 = i_33 * 4;
                        float f_35 = pcm[i_49 - 1 - i_34];
                        float f_36 = pcm[i_49 - 3 - i_34];
                        float f_37 = pcm[i_50 - 1 - i_34];
                        float f_38 = pcm[i_50 - 3 - i_34];
                        pcm[i_49 - 1 - i_34] = f_35 + f_37;
                        pcm[i_49 - 3 - i_34] = f_36 + f_38;
                        float f_39 = floats_44[i_33 * i_47];
                        float f_40 = floats_44[i_33 * i_47 + 1];
                        pcm[i_50 - 1 - i_34] = (f_35 - f_37) * f_39 - (f_36 - f_38) * f_40;
                        pcm[i_50 - 3 - i_34] = (f_36 - f_38) * f_39 + (f_35 - f_37) * f_40;
                    }
                }
            }

            for (int i_45 = 1; i_45 < eightBlocksize - 1; i_45++) {
            	int i_46 = ints_25[i_45];
                if (i_45 < i_46) {
                	int i_47 = i_45 * 8;
                	int i_48 = i_46 * 8;
                    float tmp = pcm[i_47 + 1];
                    pcm[i_47 + 1] = pcm[i_48 + 1];
                    pcm[i_48 + 1] = tmp;
                    tmp = pcm[i_47 + 3];
                    pcm[i_47 + 3] = pcm[i_48 + 3];
                    pcm[i_48 + 3] = tmp;
                    tmp = pcm[i_47 + 5];
                    pcm[i_47 + 5] = pcm[i_48 + 5];
                    pcm[i_48 + 5] = tmp;
                    tmp = pcm[i_47 + 7];
                    pcm[i_47 + 7] = pcm[i_48 + 7];
                    pcm[i_48 + 7] = tmp;
                }
            }

            for (int i_45 = 0; i_45 < halfBlocksize; i_45++) {
                pcm[i_45] = pcm[i_45 * 2 + 1];
            }

            for (int i_45 = 0; i_45 < eightBlocksize; i_45++) {
                pcm[blocksize - 1 - i_45 * 2] = pcm[i_45 * 4];
                pcm[blocksize - 2 - i_45 * 2] = pcm[i_45 * 4 + 1];
                pcm[blocksize - quarterBlocksize - 1 - i_45 * 2] = pcm[i_45 * 4 + 2];
                pcm[blocksize - quarterBlocksize - 2 - i_45 * 2] = pcm[i_45 * 4 + 3];
            }

            for (int i_45 = 0; i_45 < eightBlocksize; i_45++) {
            	float f_28 = floats_24[i_45 * 2];
            	float f_29 = floats_24[i_45 * 2 + 1];
            	float f_30 = pcm[halfBlocksize + i_45 * 2];
            	float f_31 = pcm[halfBlocksize + i_45 * 2 + 1];
            	float f_32 = pcm[blocksize - 2 - i_45 * 2];
                float f_51 = pcm[blocksize - 1 - i_45 * 2];
                float f_52 = f_29 * (f_30 - f_32) + f_28 * (f_31 + f_51);
                pcm[halfBlocksize + i_45 * 2] = (f_30 + f_32 + f_52) * 0.5F;
                pcm[blocksize - 2 - i_45 * 2] = (f_30 + f_32 - f_52) * 0.5F;
                f_52 = f_29 * (f_31 + f_51) - f_28 * (f_30 - f_32);
                pcm[halfBlocksize + i_45 * 2 + 1] = (f_31 - f_51 + f_52) * 0.5F;
                pcm[blocksize - 1 - i_45 * 2] = (-f_31 + f_51 + f_52) * 0.5F;
            }

            for (int pos = 0; pos < quarterBlocksize; pos++) {
                pcm[pos] = pcm[halfBlocksize + pos * 2] * floats_23[pos * 2] + pcm[halfBlocksize + pos * 2 + 1] * floats_23[pos * 2 + 1];
                pcm[halfBlocksize - 1 - pos] = pcm[halfBlocksize + pos * 2] * floats_23[pos * 2 + 1] - pcm[halfBlocksize + pos * 2 + 1] * floats_23[pos * 2];
            }

            for (int pos = 0; pos < quarterBlocksize; pos++) {
                pcm[pos + (blocksize - quarterBlocksize)] = -pcm[pos];
            }

            for (int pos = 0; pos < quarterBlocksize; pos++) {
                pcm[pos] = pcm[quarterBlocksize + pos];
            }

            for (int pos = 0; pos < quarterBlocksize; pos++) {
                pcm[quarterBlocksize + pos] = -pcm[quarterBlocksize - pos - 1];
            }

            for (int pos = 0; pos < quarterBlocksize; pos++) {
                pcm[halfBlocksize + pos] = pcm[blocksize - pos - 1];
            }

            for (int index = leftWindowStart; index < leftWindowEnd; index++) {
            	float f_28 = (float) Math.sin(((index - leftWindowStart) + 0.5D) / leftN * 0.5D * Math.PI);
                window[index] *= (float) Math.sin(1.5707963267948966D * f_28 * f_28);
            }

            for (int index = rightWindowStart; index < rightWindowEnd; index++) {
            	float f_28 = (float) Math.sin(((index - rightWindowStart) + 0.5D) / rightN * 0.5D * Math.PI + 1.5707963267948966D);
                window[index] *= (float) Math.sin(1.5707963267948966D * f_28 * f_28);
            }
        }

        float[] floats_53 = null;
        if (this.Y > 0) {
            int i_20 = blocksize + this.Y >> 2;
            floats_53 = new float[i_20];
            int i_21;
            if (!this.blockFlag) {
                for (i_21 = 0; i_21 < this.H; i_21++) {
                    int i_22 = i_21 + (this.Y >> 1);
                    floats_53[i_21] += this.T[i_22];
                }
            }

            if (!allFloorsEmpty) {
                for (i_21 = leftWindowStart; i_21 < blocksize >> 1; i_21++) {
                    int i_22 = floats_53.length - (blocksize >> 1) + i_21;
                    floats_53[i_22] += window[i_21];
                }
            }
        }

        float[] tmp = this.T;
        this.T = window;
        window = tmp;
        this.Y = blocksize;
        this.H = rightWindowEnd - (blocksize >> 1);
        this.blockFlag = allFloorsEmpty;
        return floats_53;
    }

    public AudioBuffer getAudioBuffer() {
        addIfAbsent(this);
        return !method12276() || this.sampleLength > this.sampleRate && this.audioBufferLength > this.sampleRate / GameShell.gameCyclesPerSecond()
        		? new AudioBuffer(this.sampleRate, this.samples, this.start, this.end, this.loopConsistency) : null;
    }

    public int getAudiobufferLength(int len) {
        int length = 0;
        if (this.samples == null) {
            this.Y = 0;
            this.T = new float[blockSize1];
            this.samples = new float[this.sampleCount];
            this.sampleLength = 0;
            this.soundIndices = 0;
        }

        for (; len > this.sampleLength && this.soundIndices < this.packets.length; this.soundIndices++) {
            float[] computed = mdctFloatCompute(this.soundIndices);
            if (computed != null) {
                int count = this.sampleLength;
                int total = computed.length;
                if (total > this.sampleCount - count) {
                    total = this.sampleCount - count;
                }

                for (int src = 0; src < total; src++) {
                    this.samples[count++] = computed[src];
                }

                length += count - this.sampleLength;
                this.sampleLength = count;
            }
        }
        return length;
    }

    public int getSampleLength() {
        return this.sampleLength;
    }

    public boolean method12276() {
        return this.sampleLength < this.sampleCount - 1;
    }

    public int getSampleRate() {
        return this.sampleRate;
    }

    public void calculateAudioBufferLength(int len) {
        if (len > this.sampleCount - 1) {
            len = this.sampleCount - 1;
        }

        if (len > this.sampleLength) {
            this.audioBufferLength = getAudiobufferLength(len);
        } else {
            this.audioBufferLength = 0;
        }

        if (!method12276()) {
            this.T = null;
        }
    }
}
