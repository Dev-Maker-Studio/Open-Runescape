package com.jagex.game.runetek5.sound.jagtheora;

import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.cache.node.impl.ObjectNode;
import com.jagex.game.runetek5.config.midi.SoundBankCache;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.sound.player.PcmPlayerManager;

public class MidiPcmStream extends PcmStream {

    public int anInt9719;
    public boolean aBool9724;
    public int musicVolume = 256;
    public int volumeMultiplier = 256;
    public final int ticks = 1000000;
    public final int[] anIntArray9702 = new int[16];
    public final int[] volume = new int[16];
    public final int[] pan = new int[16];
    public final int[] expression = new int[16];
    public final int[] anIntArray9698 = new int[16];
    public final int[] programChange = new int[16];
    public final int[] bankSelect = new int[16];
    public final int[] pitchBend = new int[16];
    public final int[] modulation = new int[16];
    public final int[] portamento = new int[16];
    final int[] sustain = new int[16];
    public final int[] nonRegisteredParameter = new int[16];
    public final int[] dataEntry = new int[16];
    final int[] generalPurpose1 = new int[16];
    public final int[] anIntArray9723 = new int[16];
    final int[] anIntArray9712 = new int[16];
    public final MusicPatchNode[][] musicPatchNode1 = new MusicPatchNode[16][128];
    public final MusicPatchNode[][] musicPatchNode2 = new MusicPatchNode[16][128];
    public final MidiFileReader fileReader = new MidiFileReader();
    public final MusicPatchPcmStream patchStream = new MusicPatchPcmStream(this);
    public final IterableNodeMap musicPatches;
    public long aLong9710;
    public MusicTrack aNodeSub79722;
    public int prioritizedTrack;
    public int prioritizedTrackLength;
    public long aLong9693;
    public boolean aBool9716;

    public MidiPcmStream(MidiPcmStream stream) {
        this.musicPatches = stream.musicPatches;
        method15095(-1, 256);
        sendSystemResetMessage(true);
    }

    public MidiPcmStream() {
        this.musicPatches = new IterableNodeMap(128);
        method15095(-1, 256);
        sendSystemResetMessage(true);
    }

    public void setPitchBend(int index, int value) {
        this.pitchBend[index] = value;
    }

    public synchronized void setVolumeMultiplier(int volumeMultiplier) {
        this.volumeMultiplier = volumeMultiplier;
    }

    public synchronized void method15095(int i_1, int i_2) {
        if (i_1 < 0) {
            for (int i_4 = 0; i_4 < 16; i_4++) {
                this.anIntArray9702[i_4] = i_2;
            }
        } else {
            this.anIntArray9702[i_1] = i_2;
        }

    }

    @Override
    public synchronized void fill(int[] ints_1, int i_2, int i_3) {
        if (this.fileReader.dataLoaded()) {
            int i_4 = this.fileReader.division * this.ticks / PcmPlayerManager.sampleRate;

            do {
                long long_5 = (long) i_4 * i_3 + this.aLong9710;
                if (this.aLong9693 - long_5 >= 0L) {
                    this.aLong9710 = long_5;
                    break;
                }

                int i_7 = (int) ((this.aLong9693 - this.aLong9710 + i_4 - 1L) / i_4);
                this.aLong9710 += (long) i_7 * i_4;
                this.patchStream.fill(ints_1, i_2, i_7);
                i_2 += i_7;
                i_3 -= i_7;
                method15127();
            } while (this.fileReader.dataLoaded());
        }

        this.patchStream.fill(ints_1, i_2, i_3);
    }

    public synchronized void method15096() {
        for (MusicPatch class282Sub142 = (MusicPatch) this.musicPatches.first(); class282Sub142 != null; class282Sub142 = (MusicPatch) this.musicPatches.next()) {
            class282Sub142.reset();
        }

    }

    public synchronized void unlinkMusicPatches() {
        for (MusicPatch musicPatch = (MusicPatch) this.musicPatches.first(); musicPatch != null; musicPatch = (MusicPatch) this.musicPatches.next()) {
            musicPatch.unlink();
        }
    }

    public synchronized void setMusicTrack(MusicTrack class282Sub71, boolean bool2) {
        method15131(class282Sub71, bool2, true);
    }

    void method15099(MusicPatchNode patchNode, boolean bool2) {
        int samples = patchNode.audioFile.getTotalSamples();
        int i_5;
        if (bool2 && patchNode.audioFile.enableLoop) {
            int i_6 = samples + samples - patchNode.audioFile.start;
            i_5 = (int) ((long) this.generalPurpose1[patchNode.currentTrack] * i_6 >> 6);
            samples <<= 8;
            if (i_5 >= samples) {
                i_5 = samples + samples - 1 - i_5;
                patchNode.stream.method15313();
            }
        } else {
            i_5 = (int) ((long) this.generalPurpose1[patchNode.currentTrack] * samples >> 6);
        }

        patchNode.stream.method15317(i_5);
    }

    public synchronized void method15100(MusicTrack class282Sub71, boolean bool2, long long_4) {
        method15131(class282Sub71, bool2, true);
        method15126(this.fileReader.division * long_4);
    }

    public synchronized void turnOffSound() {
        method15102(true);
    }

    public synchronized void method15102(boolean bool1) {
        this.fileReader.clear();
        this.aNodeSub79722 = null;
        sendSystemResetMessage(bool1);
    }

    public synchronized boolean dataLoaded() {
        return this.fileReader.dataLoaded();
    }

    public void method15104(int i_1, int i_2) {
        this.anIntArray9723[i_1] = i_2;
        this.anIntArray9712[i_1] = (int) (2097152.0D * Math.pow(2.0D, i_2 * 5.4931640625E-4D) + 0.5D);
    }

    public void method15105() {
        this.anIntArray9698[9] = 128;
        this.bankSelect[9] = 128;
        setProgramChange(9, 128);
    }

    public void setNoteOn(int channel, int note, int velocity) {
        setNoteOff(channel, note);
        if ((this.sustain[channel] & 0x2) != 0) {
            for (MusicPatchNode patchNode = (MusicPatchNode) this.patchStream.queue.first(); patchNode != null; patchNode = (MusicPatchNode) this.patchStream.queue.getNext()) {
                if (patchNode.currentTrack == channel && patchNode.anInt7579 < 0) {
                    this.musicPatchNode1[channel][patchNode.currentNotePitch] = null;
                    this.musicPatchNode1[channel][note] = patchNode;
                    int i_9 = (patchNode.anInt7575 * patchNode.anInt7574 >> 12) + patchNode.notePitchAdjustment;
                    patchNode.notePitchAdjustment += note - patchNode.currentNotePitch << 8;
                    patchNode.anInt7574 = i_9 - patchNode.notePitchAdjustment;
                    patchNode.anInt7575 = 4096;
                    patchNode.currentNotePitch = note;
                    return;
                }
            }
        }

        MusicPatch musicPatch = (MusicPatch) this.musicPatches.get(this.programChange[channel]);
        if (musicPatch != null) {
            AbstractAudioFile audioFile = musicPatch.audioFiles[note];
            if (audioFile != null) {
                MusicPatchNode musicPatchNode = new MusicPatchNode();
                musicPatchNode.currentTrack = channel;
                musicPatchNode.patch = musicPatch;
                musicPatchNode.audioFile = audioFile;
                musicPatchNode.musicPatchInfo = musicPatch.musicPatchNodes2[note];
                musicPatchNode.loopVariable = musicPatch.loopMode[note];
                musicPatchNode.currentNotePitch = note;
                musicPatchNode.maxVolumeLevel = velocity * velocity * musicPatch.volumeOffset[note] * musicPatch.baseVelocity + 1024 >> 11;
                musicPatchNode.currentPanValue = musicPatch.panOffset[note] & 0xff;
                musicPatchNode.notePitchAdjustment = (note << 8) - (musicPatch.pitchOffset[note] & 0x7fff);
                musicPatchNode.anInt7576 = 0;
                musicPatchNode.anInt7577 = 0;
                musicPatchNode.anInt7578 = 0;
                musicPatchNode.anInt7579 = -1;
                musicPatchNode.anInt7580 = 0;
                if (this.generalPurpose1[channel] == 0) {
                    musicPatchNode.stream = audioFile.getStream(method15120(musicPatchNode), calculateVolume(musicPatchNode), method15122(musicPatchNode));
                } else {
                    musicPatchNode.stream = audioFile.getStream(method15120(musicPatchNode), 0, method15122(musicPatchNode));
                    method15099(musicPatchNode, musicPatch.pitchOffset[note] < 0);
                }

                if (musicPatch.pitchOffset[note] < 0) {
                    musicPatchNode.stream.setNumLoops(-1);
                }

                if (musicPatchNode.loopVariable >= 0) {
                    MusicPatchNode patchNode = this.musicPatchNode2[channel][musicPatchNode.loopVariable];
                    if (patchNode != null && patchNode.anInt7579 < 0) {
                        this.musicPatchNode1[channel][patchNode.currentNotePitch] = null;
                        patchNode.anInt7579 = 0;
                    }

                    this.musicPatchNode2[channel][musicPatchNode.loopVariable] = musicPatchNode;
                }

                this.patchStream.queue.addFirst(musicPatchNode);
                this.musicPatchNode1[channel][note] = musicPatchNode;
            }
        }

    }

    public int calculateVolume(MusicPatchNode class282Sub131) {
        if (this.anIntArray9702[class282Sub131.currentTrack] == 0) {
            return 0;
        } else {
            MusicPatchNode2 class1103 = class282Sub131.musicPatchInfo;
            int i_4 = this.expression[class282Sub131.currentTrack] * this.volume[class282Sub131.currentTrack] + 4096 >> 13;
            i_4 = i_4 * i_4 + 16384 >> 15;
            i_4 = i_4 * class282Sub131.maxVolumeLevel + 16384 >> 15;
            i_4 = i_4 * this.musicVolume + 128 >> 8;
            i_4 = i_4 * this.volumeMultiplier >> 8;
            i_4 = i_4 * this.anIntArray9702[class282Sub131.currentTrack] + 128 >> 8;
            if (class1103.volumeEnvelopeDecay > 0) {
                i_4 = (int) (i_4 * Math.pow(0.5D, class282Sub131.anInt7576 * 1.953125E-5D * class1103.volumeEnvelopeDecay) + 0.5D);
            }

            int i_5;
            int i_6;
            int i_7;
            int i_8;
            if (class1103.aByteArray1097 != null) {
                i_5 = class282Sub131.anInt7577;
                i_6 = class1103.aByteArray1097[class282Sub131.anInt7578 + 1];
                if (class282Sub131.anInt7578 < class1103.aByteArray1097.length - 2) {
                    i_7 = (class1103.aByteArray1097[class282Sub131.anInt7578] & 0xff) << 8;
                    i_8 = (class1103.aByteArray1097[class282Sub131.anInt7578 + 2] & 0xff) << 8;
                    i_6 += (i_5 - i_7) * (class1103.aByteArray1097[class282Sub131.anInt7578 + 3] - i_6) / (i_8 - i_7);
                }

                i_4 = i_4 * i_6 + 32 >> 6;
            }

            if (class282Sub131.anInt7579 > 0 && class1103.aByteArray1094 != null) {
                i_5 = class282Sub131.anInt7579;
                i_6 = class1103.aByteArray1094[class282Sub131.anInt7580 + 1];
                if (class282Sub131.anInt7580 < class1103.aByteArray1094.length - 2) {
                    i_7 = (class1103.aByteArray1094[class282Sub131.anInt7580] & 0xff) << 8;
                    i_8 = (class1103.aByteArray1094[class282Sub131.anInt7580 + 2] & 0xff) << 8;
                    i_6 += (class1103.aByteArray1094[class282Sub131.anInt7580 + 3] - i_6) * (i_5 - i_7) / (i_8 - i_7);
                }

                i_4 = i_6 * i_4 + 32 >> 6;
            }

            return i_4;
        }
    }

    public void setNoteOff(int channel, int i_2) {
        MusicPatchNode patch1 = this.musicPatchNode1[channel][i_2];
        if (patch1 != null) {
            this.musicPatchNode1[channel][i_2] = null;
            if ((this.sustain[channel] & 0x2) != 0) {
                for (MusicPatchNode streamedPatch = (MusicPatchNode) this.patchStream.queue.getFirst(); streamedPatch != null; streamedPatch = (MusicPatchNode) this.patchStream.queue.next()) {
                    if (patch1.currentTrack == streamedPatch.currentTrack && streamedPatch.anInt7579 < 0 && streamedPatch != patch1) {
                        patch1.anInt7579 = 0;
                        break;
                    }
                }
            } else {
                patch1.anInt7579 = 0;
            }
        }

    }

    public void turnAllSoundOff(int i_1) {
        for (MusicPatchNode class282Sub133 = (MusicPatchNode) this.patchStream.queue.getFirst(); class282Sub133 != null; class282Sub133 = (MusicPatchNode) this.patchStream.queue.next()) {
            if (i_1 < 0 || class282Sub133.currentTrack == i_1) {
                if (class282Sub133.stream != null) {
                    class282Sub133.stream.method15322(PcmPlayerManager.sampleRate / 100);
                    if (class282Sub133.stream.method15311()) {
                        this.patchStream.aNodeSub15Sub49755.addSubStream(class282Sub133.stream);
                    }

                    class282Sub133.clearMusicPatchNode();
                }

                if (class282Sub133.anInt7579 < 0) {
                    this.musicPatchNode1[class282Sub133.currentTrack][class282Sub133.currentNotePitch] = null;
                }

                class282Sub133.unlink();
            }
        }

    }

    public void resetAllControllers(int i_1) {
        if (i_1 < 0) {
            for (i_1 = 0; i_1 < 16; i_1++) {
                resetAllControllers(i_1);
            }
        } else {
            this.volume[i_1] = 12800;
            this.pan[i_1] = 8192;
            this.expression[i_1] = 16383;
            this.pitchBend[i_1] = 8192;
            this.modulation[i_1] = 0;
            this.portamento[i_1] = 8192;
            resetSustainPedal(i_1);
            resetEffectSwitch(i_1, (byte) 41);
            this.sustain[i_1] = 0;
            this.nonRegisteredParameter[i_1] = 32767;
            this.dataEntry[i_1] = 256;
            this.generalPurpose1[i_1] = 0;
            method15104(i_1, 8192);
        }
    }

    boolean method15115(MusicPatchNode class282Sub131) {
        if (class282Sub131.stream == null) {
            if (class282Sub131.anInt7579 >= 0) {
                class282Sub131.unlink();
                if (class282Sub131.loopVariable > 0 && class282Sub131 == this.musicPatchNode2[class282Sub131.currentTrack][class282Sub131.loopVariable]) {
                    this.musicPatchNode2[class282Sub131.currentTrack][class282Sub131.loopVariable] = null;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    public void sendSystemResetMessage(boolean allSoundOff) {
        if (allSoundOff) {
            turnAllSoundOff(-1);
        } else {
            turnAllNotesOff(-1);
        }

        resetAllControllers(-1);

        System.arraycopy(this.anIntArray9698, 0, this.programChange, 0, 16);

        for (int index = 0; index < 16; index++) {
            this.bankSelect[index] = this.anIntArray9698[index] & -128;
        }
    }

    public void resetSustainPedal(int index) {
        if ((this.sustain[index] & 0x2) != 0) {
            for (MusicPatchNode musicPatch = (MusicPatchNode) this.patchStream.queue.getFirst(); musicPatch != null; musicPatch = (MusicPatchNode) this.patchStream.queue.next()) {
                if (musicPatch.currentTrack == index && this.musicPatchNode1[index][musicPatch.currentNotePitch] == null && musicPatch.anInt7579 < 0) {
                    musicPatch.anInt7579 = 0;
                }
            }
        }

    }

    public void resetEffectSwitch(int index, byte b_2) {
        if ((this.sustain[index] & 0x4) != 0 && b_2 > 1) {
            for (MusicPatchNode musicPatch = (MusicPatchNode) this.patchStream.queue.getFirst(); musicPatch != null; musicPatch = (MusicPatchNode) this.patchStream.queue.next()) {
                if (musicPatch.currentTrack == index) {

                    musicPatch.anInt7583 = 0;
                }
            }
        }

    }

    public void parseMessage(int bitpackedMidiMessage) {
        int message = bitpackedMidiMessage & 0xf0;
        int channel;
        int data1;
        int data2;
        if (message == 128) {
            channel = bitpackedMidiMessage & 0xf;
            data1 = bitpackedMidiMessage >> 8 & 0x7f;
            data2 = bitpackedMidiMessage >> 16 & 0x7f;
            setNoteOff(channel, data1);
        } else if (message == 144) {
            channel = bitpackedMidiMessage & 0xf;
            data1 = bitpackedMidiMessage >> 8 & 0x7f;
            data2 = bitpackedMidiMessage >> 16 & 0x7f;
            if (data2 > 0) {
                setNoteOn(channel, data1, data2);
            } else {
                setNoteOff(channel, data1);
            }
        } else if (message == 160) {
            channel = bitpackedMidiMessage & 0xf;
            data1 = bitpackedMidiMessage >> 8 & 0x7f;
            data2 = bitpackedMidiMessage >> 16 & 0x7f;
            method15188();
        } else if (message == 176) {
            channel = bitpackedMidiMessage & 0xf;
            data1 = bitpackedMidiMessage >> 8 & 0x7f;
            data2 = bitpackedMidiMessage >> 16 & 0x7f;
            if (data1 == 0) {
                this.bankSelect[channel] = (data2 << 14) + (this.bankSelect[channel] & -2080769);
            }

            if (data1 == 32) {
                this.bankSelect[channel] = (data2 << 7) + (this.bankSelect[channel] & -16257);
            }

            if (data1 == 1) {
                this.modulation[channel] = (data2 << 7) + (this.modulation[channel] & -16257);
            }

            if (data1 == 33) {
                this.modulation[channel] = data2 + (this.modulation[channel] & -128);
            }

            if (data1 == 5) {
                this.portamento[channel] = (data2 << 7) + (this.portamento[channel] & -16257);
            }

            if (data1 == 37) {
                this.portamento[channel] = data2 + (this.portamento[channel] & -128);
            }

            if (data1 == 7) {
                this.volume[channel] = (data2 << 7) + (this.volume[channel] & -16257);
            }

            if (data1 == 39) {
                this.volume[channel] = data2 + (this.volume[channel] & -128);
            }

            if (data1 == 10) {
                this.pan[channel] = (data2 << 7) + (this.pan[channel] & -16257);
            }

            if (data1 == 42) {
                this.pan[channel] = data2 + (this.pan[channel] & -128);
            }

            if (data1 == 11) {
                this.expression[channel] = (data2 << 7) + (this.expression[channel] & -16257);
            }

            if (data1 == 43) {
                this.expression[channel] = data2 + (this.expression[channel] & -128);
            }

            if (data1 == 64) {
                if (data2 >= 64) {
                    this.sustain[channel] |= 0x1;
                } else {
                    this.sustain[channel] &= -2;
                }
            }

            if (data1 == 65) {
                if (data2 >= 64) {
                    this.sustain[channel] |= 0x2;
                } else {
                    resetSustainPedal(channel);
                    this.sustain[channel] &= -3;
                }
            }

            if (data1 == 99) {
                this.nonRegisteredParameter[channel] = (data2 << 7) + (this.nonRegisteredParameter[channel] & 0x7f);
            }

            if (data1 == 98) {
                this.nonRegisteredParameter[channel] = (this.nonRegisteredParameter[channel] & 0x3f80) + data2;
            }

            if (data1 == 101) {
                this.nonRegisteredParameter[channel] = (data2 << 7) + (this.nonRegisteredParameter[channel] & 0x7f) + 16384;
            }

            if (data1 == 100) {
                this.nonRegisteredParameter[channel] = (this.nonRegisteredParameter[channel] & 0x3f80) + data2 + 16384;
            }

            if (data1 == 120) {
                turnAllSoundOff(channel);
            }

            if (data1 == 121) {
                resetAllControllers(channel);
            }

            if (data1 == 123) {
                turnAllNotesOff(channel);
            }

            int i_7;
            if (data1 == 6) {
                i_7 = this.nonRegisteredParameter[channel];
                if (i_7 == 16384) {
                    this.dataEntry[channel] = (data2 << 7) + (this.dataEntry[channel] & -16257);
                }
            }

            if (data1 == 38) {
                i_7 = this.nonRegisteredParameter[channel];
                if (i_7 == 16384) {
                    this.dataEntry[channel] = data2 + (this.dataEntry[channel] & -128);
                }
            }

            if (data1 == 16) {
                this.generalPurpose1[channel] = (data2 << 7) + (this.generalPurpose1[channel] & -16257);
            }

            if (data1 == 48) {
                this.generalPurpose1[channel] = data2 + (this.generalPurpose1[channel] & -128);
            }

            if (data1 == 81) {
                if (data2 >= 64) {
                    this.sustain[channel] |= 0x4;
                } else {
                    resetEffectSwitch(channel, (byte) 97);
                    this.sustain[channel] &= -5;
                }
            }

            if (data1 == 17) {
                method15104(channel, (data2 << 7) + (this.anIntArray9723[channel] & -16257));
            }

            if (data1 == 49) {
                method15104(channel, data2 + (this.anIntArray9723[channel] & -128));
            }
        } else if (message == 192) {
            channel = bitpackedMidiMessage & 0xf;
            data1 = bitpackedMidiMessage >> 8 & 0x7f;
            setProgramChange(channel, data1 + this.bankSelect[channel]);
        } else if (message == 208) {
            channel = bitpackedMidiMessage & 0xf;
            data1 = bitpackedMidiMessage >> 8 & 0x7f;
            setAfterTouch();
        } else if (message == 224) {
            channel = bitpackedMidiMessage & 0xf;
            data1 = (bitpackedMidiMessage >> 8 & 0x7f) + (bitpackedMidiMessage >> 9 & 0x3f80);
            setPitchBend(channel, data1);
        } else {
            message = bitpackedMidiMessage & 0xff;
            if (message == 255) {
                sendSystemResetMessage(true);
            }
        }

    }

    public int method15120(MusicPatchNode class282Sub131) {
        int i_3 = (class282Sub131.anInt7574 * class282Sub131.anInt7575 >> 12) + class282Sub131.notePitchAdjustment;
        i_3 += (this.pitchBend[class282Sub131.currentTrack] - 8192) * this.dataEntry[class282Sub131.currentTrack] >> 12;
        MusicPatchNode2 class1104 = class282Sub131.musicPatchInfo;
        int i_5;
        if (class1104.volumeEnvelopeRelease > 0 && (class1104.vibratoLfoPitch > 0 || this.modulation[class282Sub131.currentTrack] > 0)) {
            i_5 = class1104.vibratoLfoPitch << 2;
            int i_6 = class1104.anInt1102 << 1;
            if (class282Sub131.anInt7581 < i_6) {
                i_5 = i_5 * class282Sub131.anInt7581 / i_6;
            }

            i_5 += this.modulation[class282Sub131.currentTrack] >> 7;
            double d_7 = Math.sin(0.01227184630308513D * (class282Sub131.anInt7571 & 0x1ff));
            i_3 += (int) (d_7 * i_5);
        }

        i_5 = (int) ((class282Sub131.audioFile.sampleRate * 256) * Math.pow(2.0D, 3.255208333333333E-4D * i_3) / PcmPlayerManager.sampleRate + 0.5D);
        return Math.max(1, i_5);
    }

    public void setAfterTouch() {
    }

    public int method15122(MusicPatchNode class282Sub131) {
        int i_3 = this.pan[class282Sub131.currentTrack];
        return i_3 < 8192 ? i_3 * class282Sub131.currentPanValue + 32 >> 6 : 16384 - ((128 - class282Sub131.currentPanValue) * (16384 - i_3) + 32 >> 6);
    }

    @Override
    public synchronized PcmStream getFirstStream() {
        return this.patchStream;
    }

    public int getMusicVolume() {
        return this.musicVolume;
    }

    @Override
    public synchronized void skip(int i_1) {
        if (this.fileReader.dataLoaded()) {
            int i_2 = this.fileReader.division * this.ticks / PcmPlayerManager.sampleRate;

            do {
                long long_3 = (long) i_1 * i_2 + this.aLong9710;
                if (this.aLong9693 - long_3 >= 0L) {
                    this.aLong9710 = long_3;
                    break;
                }

                int i_5 = (int) ((this.aLong9693 - this.aLong9710 + i_2 - 1L) / i_2);
                this.aLong9710 += (long) i_2 * i_5;
                this.patchStream.skip(i_5);
                i_1 -= i_5;
                method15127();
            } while (this.fileReader.dataLoaded());
        }

        this.patchStream.skip(i_1);
    }

    public void method15126(long long_1) {
        while (this.aLong9693 <= long_1) {
            int track = this.prioritizedTrack;
            int length = this.prioritizedTrackLength;

            long long_3;
            for (long_3 = this.aLong9693; length == this.prioritizedTrackLength; long_3 = this.fileReader.method1403(length)) {
                while (length == this.fileReader.trackLengths[track]) {
                    this.fileReader.getGotoTrack(track);
                    int flags = this.fileReader.readMessage(track);
                    if (flags == 1) {
                        this.fileReader.setTrackDone();
                        this.fileReader.markTracePosition(track);
                        if (this.fileReader.isDone()) {
                            if (!this.aBool9716 || length == 0) {
                                sendSystemResetMessage(true);
                                this.fileReader.clear();
                                return;
                            }

                            this.fileReader.method1406(long_3);
                        }
                        break;
                    }

                    if ((flags & 0x80) != 0 && (flags & 0xf0) != 144) {
                        parseMessage(flags);
                    }

                    this.fileReader.getTrackLength(track);
                    this.fileReader.markTracePosition(track);
                }

                this.aLong9710 = long_3;
                track = this.fileReader.getPrioritizedTrack();
                length = this.fileReader.trackLengths[track];
            }

            this.prioritizedTrack = track;
            this.prioritizedTrackLength = length;
            this.aLong9693 = long_3;
        }

    }

    public void method15127() {
        int i_2 = this.prioritizedTrack;
        int i_3 = this.prioritizedTrackLength;
        long long_4 = this.aLong9693;
        if (this.aNodeSub79722 != null && i_3 == this.anInt9719) {
            method15131(this.aNodeSub79722, this.aBool9716, this.aBool9724);
            method15127();
        } else {
            while (true) {
                if (i_3 != this.prioritizedTrackLength) {
                    this.prioritizedTrack = i_2;
                    this.prioritizedTrackLength = i_3;
                    this.aLong9693 = long_4;
                    if (this.aNodeSub79722 != null && this.anInt9719 < i_3) {
                        this.prioritizedTrack = -1;
                        this.prioritizedTrackLength = this.anInt9719;
                        this.aLong9693 = this.fileReader.method1403(this.prioritizedTrackLength);
                    }
                    break;
                }

                while (i_3 == this.fileReader.trackLengths[i_2]) {
                    this.fileReader.getGotoTrack(i_2);
                    int i_6 = this.fileReader.readMessage(i_2);
                    if (i_6 == 1) {
                        this.fileReader.setTrackDone();
                        this.fileReader.markTracePosition(i_2);
                        if (this.fileReader.isDone()) {
                            if (this.aNodeSub79722 != null) {
                                setMusicTrack(this.aNodeSub79722, this.aBool9716);
                                method15127();
                                return;
                            }

                            if (!this.aBool9716 || i_3 == 0) {
                                sendSystemResetMessage(true);
                                this.fileReader.clear();
                                return;
                            }

                            this.fileReader.method1406(long_4);
                        }
                        break;
                    }

                    if ((i_6 & 0x80) != 0) {
                        parseMessage(i_6);
                    }

                    this.fileReader.getTrackLength(i_2);
                    this.fileReader.markTracePosition(i_2);
                }

                i_2 = this.fileReader.getPrioritizedTrack();
                i_3 = this.fileReader.trackLengths[i_2];
                long_4 = this.fileReader.method1403(i_3);
            }
        }

    }

    @Override
    public synchronized int method12228() {
        return 0;
    }

    boolean method15128(MusicPatchNode class282Sub131, int[] ints_2, int i_3, int i_4) {
        class282Sub131.anInt7584 = PcmPlayerManager.sampleRate / 100;
        if (class282Sub131.anInt7579 < 0 || class282Sub131.stream != null && !class282Sub131.stream.method15319()) {
            int i_6 = class282Sub131.anInt7575;
            if (i_6 > 0) {
                i_6 -= (int) (16.0D * Math.pow(2.0D, this.portamento[class282Sub131.currentTrack] * 4.921259842519685E-4D) + 0.5D);
                if (i_6 < 0) {
                    i_6 = 0;
                }

                class282Sub131.anInt7575 = i_6;
            }

            class282Sub131.stream.method15308(method15120(class282Sub131));
            MusicPatchNode2 class1107 = class282Sub131.musicPatchInfo;
            boolean bool8 = false;
            ++class282Sub131.anInt7581;
            class282Sub131.anInt7571 += class1107.volumeEnvelopeRelease;
            double d_9 = 5.086263020833333E-6D * ((class282Sub131.currentNotePitch - 60 << 8) + (class282Sub131.anInt7574 * class282Sub131.anInt7575 >> 12));
            if (class1107.volumeEnvelopeDecay > 0) {
                if (class1107.vibratoLfoFrequency > 0) {
                    class282Sub131.anInt7576 += (int) (128.0D * Math.pow(2.0D, d_9 * class1107.vibratoLfoFrequency) + 0.5D);
                } else {
                    class282Sub131.anInt7576 += 128;
                }

                if (class1107.volumeEnvelopeDecay * class282Sub131.anInt7576 >= 819200) {
                    bool8 = true;
                }
            }

            if (class1107.aByteArray1097 != null) {
                if (class1107.volumeEnvelopeSustain > 0) {
                    class282Sub131.anInt7577 += (int) (128.0D * Math.pow(2.0D, d_9 * class1107.volumeEnvelopeSustain) + 0.5D);
                } else {
                    class282Sub131.anInt7577 += 128;
                }

                while (class282Sub131.anInt7578 < class1107.aByteArray1097.length - 2 && class282Sub131.anInt7577 > (class1107.aByteArray1097[class282Sub131.anInt7578 + 2] & 0xff) << 8) {
                    class282Sub131.anInt7578 += 2;
                }

                if (class1107.aByteArray1097.length - 2 == class282Sub131.anInt7578 && class1107.aByteArray1097[class282Sub131.anInt7578 + 1] == 0) {
                    bool8 = true;
                }
            }

            if (class282Sub131.anInt7579 >= 0 && class1107.aByteArray1094 != null && (this.sustain[class282Sub131.currentTrack] & 0x1) == 0 && (class282Sub131.loopVariable < 0 || class282Sub131 != this.musicPatchNode2[class282Sub131.currentTrack][class282Sub131.loopVariable])) {
                if (class1107.vibratoLfoDelay > 0) {
                    class282Sub131.anInt7579 += (int) (128.0D * Math.pow(2.0D, d_9 * class1107.vibratoLfoDelay) + 0.5D);
                } else {
                    class282Sub131.anInt7579 += 128;
                }

                while (class282Sub131.anInt7580 < class1107.aByteArray1094.length - 2 && class282Sub131.anInt7579 > (class1107.aByteArray1094[class282Sub131.anInt7580 + 2] & 0xff) << 8) {
                    class282Sub131.anInt7580 += 2;
                }

                if (class1107.aByteArray1094.length - 2 == class282Sub131.anInt7580) {
                    bool8 = true;
                }
            }

            if (bool8) {
                class282Sub131.stream.method15322(class282Sub131.anInt7584);
                if (ints_2 != null) {
                    class282Sub131.stream.fill(ints_2, i_3, i_4);
                } else {
                    class282Sub131.stream.skip(i_4);
                }

                if (class282Sub131.stream.method15311()) {
                    this.patchStream.aNodeSub15Sub49755.addSubStream(class282Sub131.stream);
                }

                class282Sub131.clearMusicPatchNode();
                if (class282Sub131.anInt7579 >= 0) {
                    class282Sub131.unlink();
                    if (class282Sub131.loopVariable > 0 && class282Sub131 == this.musicPatchNode2[class282Sub131.currentTrack][class282Sub131.loopVariable]) {
                        this.musicPatchNode2[class282Sub131.currentTrack][class282Sub131.loopVariable] = null;
                    }
                }

                return true;
            } else {
                class282Sub131.stream.method15321(class282Sub131.anInt7584, calculateVolume(class282Sub131), method15122(class282Sub131));
                return false;
            }
        } else {
            class282Sub131.clearMusicPatchNode();
            class282Sub131.unlink();
            if (class282Sub131.loopVariable > 0 && class282Sub131 == this.musicPatchNode2[class282Sub131.currentTrack][class282Sub131.loopVariable]) {
                this.musicPatchNode2[class282Sub131.currentTrack][class282Sub131.loopVariable] = null;
            }

            return true;
        }
    }

    public synchronized void method15131(MusicTrack musicTrack, boolean bool2, boolean bool3) {
        method15102(bool3);
        this.fileReader.decode(musicTrack.midi);
        this.aBool9716 = bool2;
        this.aLong9710 = 0L;
        int trackCount = this.fileReader.getTrackCount();

        for (int track = 0; track < trackCount; track++) {
            this.fileReader.getGotoTrack(track);
            this.fileReader.getTrackLength(track);
            this.fileReader.markTracePosition(track);
        }

        this.prioritizedTrack = this.fileReader.getPrioritizedTrack();
        this.prioritizedTrackLength = this.fileReader.trackLengths[this.prioritizedTrack];
        this.aLong9693 = this.fileReader.method1403(this.prioritizedTrackLength);
    }

    public synchronized void load() {
        method15105();
    }

    public synchronized void setMusicVolume(int volume) {
        this.musicVolume = volume;
    }

    @Override
    public synchronized PcmStream getNextStream() {
        return null;
    }

    public synchronized boolean ready(MusicTrack track, Js5ResourceProvider resource, SoundBankCache cache) {
        track.method12165();
        boolean ready = true;
        int[] ints_7 = new int[]{22050};

        for (ObjectNode class282Sub478 = track.table.first(); class282Sub478 != null; class282Sub478 = track.table.next()) {
            int key = (int) class282Sub478.pointer;
            MusicPatch class282Sub1410 = (MusicPatch) this.musicPatches.get(key);
            if (class282Sub1410 == null) {
                class282Sub1410 = MidiPcmStream.getMusicPatch(resource, key);
                if (class282Sub1410 == null) {
                    ready = false;
                    continue;
                }

                this.musicPatches.put(class282Sub1410, key);
            }

            if (!class282Sub1410.ready(cache, (byte[]) class282Sub478.value, ints_7)) {
                ready = false;
            }
        }

        if (ready) {
            track.clear();
        }

        return ready;
    }

    public void method15188() {
    }

    public void setProgramChange(int channel, int data1) {
        if (data1 != this.programChange[channel]) {
            this.programChange[channel] = data1;

            for (int noteIndex = 0; noteIndex < 128; noteIndex++) {
                this.musicPatchNode2[channel][noteIndex] = null;
            }
        }
    }

    public void turnAllNotesOff(int i_1) {
        for (MusicPatchNode patchNode = (MusicPatchNode) this.patchStream.queue.getFirst(); patchNode != null; patchNode = (MusicPatchNode) this.patchStream.queue.next()) {
            if ((i_1 < 0 || patchNode.currentTrack == i_1) && patchNode.anInt7579 < 0) {
                this.musicPatchNode1[patchNode.currentTrack][patchNode.currentNotePitch] = null;
                patchNode.anInt7579 = 0;
            }
        }
    }

	public static MusicPatch getMusicPatch(Js5ResourceProvider resource, int fileId) {
	    byte[] fileData = resource.getFile(fileId);
	    return fileData == null ? null : new MusicPatch(fileData);
	}
}