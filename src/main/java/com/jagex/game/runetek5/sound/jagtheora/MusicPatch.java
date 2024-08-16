package com.jagex.game.runetek5.sound.jagtheora;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.config.midi.SoundBankCache;

public class MusicPatch extends Node {

    public int[] sampleOffsets = new int[128];
    final AbstractAudioFile[] audioFiles = new AbstractAudioFile[128];
    final short[] pitchOffset = new short[128];
    final byte[] volumeOffset = new byte[128];
    final byte[] panOffset = new byte[128];
    final MusicPatchNode2[] musicPatchNodes2 = new MusicPatchNode2[128];
    final byte[] loopMode = new byte[128];
    final int baseVelocity;

    public MusicPatch(byte[] fileData) {
        Packet patchBuffer = new Packet(fileData);

        int i_3;
        for (i_3 = 0; patchBuffer.buffer[i_3 + patchBuffer.currentPosition] != 0; i_3++) {
        }

        byte[] bytes_4 = new byte[i_3];

        int i_5;
        for (i_5 = 0; i_5 < i_3; i_5++) {
            bytes_4[i_5] = patchBuffer.readByte();
        }

        ++patchBuffer.currentPosition;
        ++i_3;
        i_5 = patchBuffer.currentPosition;
        patchBuffer.currentPosition += i_3;

        int i_6;
        for (i_6 = 0; patchBuffer.buffer[i_6 + patchBuffer.currentPosition] != 0; i_6++) {
        }

        byte[] bytes_7 = new byte[i_6];

        int i_8;
        for (i_8 = 0; i_8 < i_6; i_8++) {
            bytes_7[i_8] = patchBuffer.readByte();
        }

        ++patchBuffer.currentPosition;
        ++i_6;
        i_8 = patchBuffer.currentPosition;
        patchBuffer.currentPosition += i_6;

        int i_9;
        for (i_9 = 0; patchBuffer.buffer[i_9 + patchBuffer.currentPosition] != 0; i_9++) {
        }

        byte[] bytes_10 = new byte[i_9];

        for (int i_11 = 0; i_11 < i_9; i_11++) {
            bytes_10[i_11] = patchBuffer.readByte();
        }

        ++patchBuffer.currentPosition;
        ++i_9;
        byte[] bytes_36 = new byte[i_9];
        int i_12;
        int i_14;
        if (i_9 > 1) {
            bytes_36[1] = 1;
            int i_13 = 1;
            i_12 = 2;

            for (i_14 = 2; i_14 < i_9; i_14++) {
                int i_41 = patchBuffer.readUnsignedByte();
                if (i_41 == 0) {
                    i_13 = i_12++;
                } else {
                    if (i_41 <= i_13) {
                        --i_41;
                    }

                    i_13 = i_41;
                }

                bytes_36[i_14] = (byte) i_13;
            }
        } else {
            i_12 = i_9;
        }

        MusicPatchNode2[] arr_37 = new MusicPatchNode2[i_12];

        for (i_14 = 0; i_14 < arr_37.length; i_14++) {
            MusicPatchNode2 class11015 = arr_37[i_14] = new MusicPatchNode2();
            int i_40 = patchBuffer.readUnsignedByte();
            if (i_40 > 0) {
                class11015.aByteArray1097 = new byte[i_40 * 2];
            }

            i_40 = patchBuffer.readUnsignedByte();
            if (i_40 > 0) {
                class11015.aByteArray1094 = new byte[i_40 * 2 + 2];
                class11015.aByteArray1094[1] = 64;
            }
        }

        i_14 = patchBuffer.readUnsignedByte();
        byte[] bytes_43 = i_14 > 0 ? new byte[i_14 * 2] : null;
        i_14 = patchBuffer.readUnsignedByte();
        byte[] bytes_16 = i_14 > 0 ? new byte[i_14 * 2] : null;

        int i_17;
        for (i_17 = 0; patchBuffer.buffer[i_17 + patchBuffer.currentPosition] != 0; i_17++) {
        }

        byte[] bytes_18 = new byte[i_17];

        int i_19;
        for (i_19 = 0; i_19 < i_17; i_19++) {
            bytes_18[i_19] = patchBuffer.readByte();
        }

        ++patchBuffer.currentPosition;
        ++i_17;
        i_19 = 0;

        int i_20;
        for (i_20 = 0; i_20 < 128; i_20++) {
            i_19 += patchBuffer.readUnsignedByte();
            this.pitchOffset[i_20] = (short) i_19;
        }

        i_19 = 0;

        for (i_20 = 0; i_20 < 128; i_20++) {
            i_19 += patchBuffer.readUnsignedByte();
            this.pitchOffset[i_20] += (i_19 << 8);
        }

        i_20 = 0;
        int i_21 = 0;
        int i_22 = 0;

        int i_23;
        for (i_23 = 0; i_23 < 128; i_23++) {
            if (i_20 == 0) {
                if (i_21 < bytes_18.length) {
                    i_20 = bytes_18[i_21++];
                } else {
                    i_20 = -1;
                }

                i_22 = patchBuffer.method13099();
            }

            this.pitchOffset[i_23] += ((i_22 - 1 & 0x2) << 14);
            this.sampleOffsets[i_23] = i_22;
            --i_20;
        }

        i_20 = 0;
        i_21 = 0;
        i_23 = 0;

        int i_24;
        for (i_24 = 0; i_24 < 128; i_24++) {
            if (this.sampleOffsets[i_24] != 0) {
                if (i_20 == 0) {
                    if (i_21 < bytes_4.length) {
                        i_20 = bytes_4[i_21++];
                    } else {
                        i_20 = -1;
                    }

                    i_23 = patchBuffer.buffer[i_5++] - 1;
                }

                this.loopMode[i_24] = (byte) i_23;
                --i_20;
            }
        }

        i_20 = 0;
        i_21 = 0;
        i_24 = 0;

        for (int i_25 = 0; i_25 < 128; i_25++) {
            if (this.sampleOffsets[i_25] != 0) {
                if (i_20 == 0) {
                    if (i_21 < bytes_7.length) {
                        i_20 = bytes_7[i_21++];
                    } else {
                        i_20 = -1;
                    }

                    i_24 = patchBuffer.buffer[i_8++] + 16 << 2;
                }

                this.panOffset[i_25] = (byte) i_24;
                --i_20;
            }
        }

        i_20 = 0;
        i_21 = 0;
        MusicPatchNode2 class11038 = null;

        int i_26;
        for (i_26 = 0; i_26 < 128; i_26++) {
            if (this.sampleOffsets[i_26] != 0) {
                if (i_20 == 0) {
                    class11038 = arr_37[bytes_36[i_21]];
                    if (i_21 < bytes_10.length) {
                        i_20 = bytes_10[i_21++];
                    } else {
                        i_20 = -1;
                    }
                }

                this.musicPatchNodes2[i_26] = class11038;
                --i_20;
            }
        }

        i_20 = 0;
        i_21 = 0;
        i_26 = 0;

        int i_27;
        for (i_27 = 0; i_27 < 128; i_27++) {
            if (i_20 == 0) {
                if (i_21 < bytes_18.length) {
                    i_20 = bytes_18[i_21++];
                } else {
                    i_20 = -1;
                }

                if (this.sampleOffsets[i_27] > 0) {
                    i_26 = patchBuffer.readUnsignedByte() + 1;
                }
            }

            this.volumeOffset[i_27] = (byte) i_26;
            --i_20;
        }

        this.baseVelocity = patchBuffer.readUnsignedByte() + 1;

        int i_29;
        MusicPatchNode2 class11042;
        for (i_27 = 0; i_27 < i_12; i_27++) {
            class11042 = arr_37[i_27];
            if (class11042.aByteArray1097 != null) {
                for (i_29 = 1; i_29 < class11042.aByteArray1097.length; i_29 += 2) {
                    class11042.aByteArray1097[i_29] = patchBuffer.readByte();
                }
            }

            if (class11042.aByteArray1094 != null) {
                for (i_29 = 3; i_29 < class11042.aByteArray1094.length - 2; i_29 += 2) {
                    class11042.aByteArray1094[i_29] = patchBuffer.readByte();
                }
            }
        }

        if (bytes_43 != null) {
            for (i_27 = 1; i_27 < bytes_43.length; i_27 += 2) {
                bytes_43[i_27] = patchBuffer.readByte();
            }
        }

        if (bytes_16 != null) {
            for (i_27 = 1; i_27 < bytes_16.length; i_27 += 2) {
                bytes_16[i_27] = patchBuffer.readByte();
            }
        }

        for (i_27 = 0; i_27 < i_12; i_27++) {
            class11042 = arr_37[i_27];
            if (class11042.aByteArray1094 != null) {
                i_19 = 0;

                for (i_29 = 2; i_29 < class11042.aByteArray1094.length; i_29 += 2) {
                    i_19 = 1 + i_19 + patchBuffer.readUnsignedByte();
                    class11042.aByteArray1094[i_29] = (byte) i_19;
                }
            }
        }

        for (i_27 = 0; i_27 < i_12; i_27++) {
            class11042 = arr_37[i_27];
            if (class11042.aByteArray1097 != null) {
                i_19 = 0;

                for (i_29 = 2; i_29 < class11042.aByteArray1097.length; i_29 += 2) {
                    i_19 += 1 + patchBuffer.readUnsignedByte();
                    class11042.aByteArray1097[i_29] = (byte) i_19;
                }
            }
        }

        byte b_30;
        int i_32;
        int i_33;
        int i_34;
        Object obj39;
        byte b_47;
        if (bytes_43 != null) {
            i_19 = patchBuffer.readUnsignedByte();
            bytes_43[0] = (byte) i_19;

            for (i_27 = 2; i_27 < bytes_43.length; i_27 += 2) {
                i_19 = 1 + i_19 + patchBuffer.readUnsignedByte();
                bytes_43[i_27] = (byte) i_19;
            }

            b_47 = bytes_43[0];
            byte b_28 = bytes_43[1];

            for (i_29 = 0; i_29 < b_47; i_29++) {
                this.volumeOffset[i_29] = (byte) (b_28 * this.volumeOffset[i_29] + 32 >> 6);
            }

            for (i_29 = 2; i_29 < bytes_43.length; i_29 += 2) {
                b_30 = bytes_43[i_29];
                byte b_31 = bytes_43[i_29 + 1];
                i_32 = b_28 * (b_30 - b_47) + (b_30 - b_47) / 2;

                for (i_33 = b_47; i_33 < b_30; i_33++) {
                    i_34 = MusicPatch.method6349(i_32, b_30 - b_47);
                    this.volumeOffset[i_33] = (byte) (i_34 * this.volumeOffset[i_33] + 32 >> 6);
                    i_32 += b_31 - b_28;
                }

                b_47 = b_30;
                b_28 = b_31;
            }

            for (i_29 = b_47; i_29 < 128; i_29++) {
                this.volumeOffset[i_29] = (byte) (b_28 * this.volumeOffset[i_29] + 32 >> 6);
            }

            obj39 = null;
        }

        if (bytes_16 != null) {
            i_19 = patchBuffer.readUnsignedByte();
            bytes_16[0] = (byte) i_19;

            for (i_27 = 2; i_27 < bytes_16.length; i_27 += 2) {
                i_19 = 1 + i_19 + patchBuffer.readUnsignedByte();
                bytes_16[i_27] = (byte) i_19;
            }

            b_47 = bytes_16[0];
            int i_44 = bytes_16[1] << 1;

            int i_45;
            for (i_29 = 0; i_29 < b_47; i_29++) {
                i_45 = i_44 + (this.panOffset[i_29] & 0xff);
                if (i_45 < 0) {
                    i_45 = 0;
                }

                if (i_45 > 128) {
                    i_45 = 128;
                }

                this.panOffset[i_29] = (byte) i_45;
            }

            for (i_29 = 2; i_29 < bytes_16.length; i_29 += 2) {
                b_30 = bytes_16[i_29];
                int i_46 = bytes_16[i_29 + 1] << 1;
                i_32 = i_44 * (b_30 - b_47) + (b_30 - b_47) / 2;

                for (i_33 = b_47; i_33 < b_30; i_33++) {
                    i_34 = MusicPatch.method6349(i_32, b_30 - b_47);
                    int i_35 = i_34 + (this.panOffset[i_33] & 0xff);
                    if (i_35 < 0) {
                        i_35 = 0;
                    }

                    if (i_35 > 128) {
                        i_35 = 128;
                    }

                    this.panOffset[i_33] = (byte) i_35;
                    i_32 += i_46 - i_44;
                }

                b_47 = b_30;
                i_44 = i_46;
            }

            for (i_29 = b_47; i_29 < 128; i_29++) {
                i_45 = i_44 + (this.panOffset[i_29] & 0xff);
                if (i_45 < 0) {
                    i_45 = 0;
                }

                if (i_45 > 128) {
                    i_45 = 128;
                }

                this.panOffset[i_29] = (byte) i_45;
            }

            obj39 = null;
        }

        for (i_27 = 0; i_27 < i_12; i_27++) {
            arr_37[i_27].volumeEnvelopeDecay = patchBuffer.readUnsignedByte();
        }

        for (i_27 = 0; i_27 < i_12; i_27++) {
            class11042 = arr_37[i_27];
            if (class11042.aByteArray1097 != null) {
                class11042.volumeEnvelopeSustain = patchBuffer.readUnsignedByte();
            }

            if (class11042.aByteArray1094 != null) {
                class11042.vibratoLfoDelay = patchBuffer.readUnsignedByte();
            }

            if (class11042.volumeEnvelopeDecay > 0) {
                class11042.vibratoLfoFrequency = patchBuffer.readUnsignedByte();
            }
        }

        for (i_27 = 0; i_27 < i_12; i_27++) {
            arr_37[i_27].volumeEnvelopeRelease = patchBuffer.readUnsignedByte();
        }

        for (i_27 = 0; i_27 < i_12; i_27++) {
            class11042 = arr_37[i_27];
            if (class11042.volumeEnvelopeRelease > 0) {
                class11042.vibratoLfoPitch = patchBuffer.readUnsignedByte();
            }
        }

        for (i_27 = 0; i_27 < i_12; i_27++) {
            class11042 = arr_37[i_27];
            if (class11042.vibratoLfoPitch > 0) {
                class11042.anInt1102 = patchBuffer.readUnsignedByte();
            }
        }

    }

    boolean ready(SoundBankCache cache, byte[] data, int[] sampleRates) {
        boolean ready = true;
        int file = 0;
        AbstractAudioFile audioFile = null;

        for (int index = 0; index < 128; index++) {
            if (data == null || data[index] != 0) {
                int sampleOffset = this.sampleOffsets[index];
                if (sampleOffset != 0) {
                    if (sampleOffset != file) {
                        file = sampleOffset--;
                        if ((sampleOffset & 0x1) == 0) {
                            audioFile = cache.listEffect(sampleOffset >> 2, sampleRates);
                        } else {
                            audioFile = cache.listMusic(sampleOffset >> 2, sampleRates);
                        }

                        if (audioFile == null) {
                            ready = false;
                        }
                    }

                    if (audioFile != null) {
                        this.audioFiles[index] = audioFile;
                        this.sampleOffsets[index] = 0;
                    }
                }
            }
        }

        return ready;
    }

    void reset() {
        this.sampleOffsets = null;
    }

	public static int method6349(int i_0, int i_1) {
	    int i_3 = i_0 >>> 31;
	    return (i_0 + i_3) / i_1 - i_3;
	}

}
