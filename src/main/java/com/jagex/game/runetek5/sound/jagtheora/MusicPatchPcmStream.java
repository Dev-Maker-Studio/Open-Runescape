package com.jagex.game.runetek5.sound.jagtheora;

import com.jagex.game.runetek5.cache.node.NodeCollection;
import com.jagex.game.runetek5.sound.player.PcmPlayerManager;

public class MusicPatchPcmStream extends PcmStream {

    final NodeCollection queue = new NodeCollection();
    final Node_Sub15_Sub4 aNodeSub15Sub49755 = new Node_Sub15_Sub4();
    public final MidiPcmStream aNodeSub15Sub29753;

    MusicPatchPcmStream(MidiPcmStream class282Sub15Sub21) {
        this.aNodeSub15Sub29753 = class282Sub15Sub21;
    }

    @Override
    public PcmStream getFirstStream() {
        MusicPatchNode musicPatch = (MusicPatchNode) this.queue.getFirst();
        return musicPatch == null ? null : (musicPatch.stream != null ? musicPatch.stream : getNextStream());
    }

    @Override
    public void fill(int[] ints_1, int i_2, int i_3) {
        this.aNodeSub15Sub49755.fill(ints_1, i_2, i_3);

        for (MusicPatchNode class282Sub134 = (MusicPatchNode) this.queue.getFirst(); class282Sub134 != null; class282Sub134 = (MusicPatchNode) this.queue.next()) {
            if (!this.aNodeSub15Sub29753.method15115(class282Sub134)) {
                int i_5 = i_2;
                int i_6 = i_3;

                do {
                    if (i_6 <= class282Sub134.anInt7584) {
                        method15234(class282Sub134, ints_1, i_5, i_6, i_6 + i_5);
                        class282Sub134.anInt7584 -= i_6;
                        break;
                    }

                    method15234(class282Sub134, ints_1, i_5, class282Sub134.anInt7584, i_5 + i_6);
                    i_5 += class282Sub134.anInt7584;
                    i_6 -= class282Sub134.anInt7584;
                } while (!this.aNodeSub15Sub29753.method15128(class282Sub134, ints_1, i_5, i_6));
            }
        }

    }

    public void method15233(MusicPatchNode class282Sub131, int i_2) {
        int i_21 = i_2;
        if ((this.aNodeSub15Sub29753.sustain[class282Sub131.currentTrack] & 0x4) != 0 && class282Sub131.anInt7579 < 0) {
            int i_4 = this.aNodeSub15Sub29753.anIntArray9712[class282Sub131.currentTrack] / PcmPlayerManager.sampleRate;
            int i_5 = (i_4 + 1048575 - class282Sub131.anInt7583) / i_4;
            class282Sub131.anInt7583 = i_4 * i_21 + class282Sub131.anInt7583 & 0xfffff;
            if (i_5 <= i_21) {
                AreaSoundStream class282Sub15Sub56 = class282Sub131.stream;
                if (this.aNodeSub15Sub29753.generalPurpose1[class282Sub131.currentTrack] == 0) {
                    class282Sub131.stream = class282Sub131.audioFile.getStream(class282Sub15Sub56.method15323(), class282Sub15Sub56.method15315(), class282Sub15Sub56.method15316());
                } else {
                    class282Sub131.stream = class282Sub131.audioFile.getStream(class282Sub15Sub56.method15323(), 0, class282Sub15Sub56.method15316());
                    this.aNodeSub15Sub29753.method15099(class282Sub131, class282Sub131.patch.pitchOffset[class282Sub131.currentNotePitch] < 0);
                }

                if (class282Sub131.patch.pitchOffset[class282Sub131.currentNotePitch] < 0) {
                    class282Sub131.stream.setNumLoops(-1);
                }

                i_21 = class282Sub131.anInt7583 / i_4;
            }
        }

        class282Sub131.stream.skip(i_21);
    }

    public void method15234(MusicPatchNode class282Sub131, int[] ints_2, int i_3, int i_4, int i_5) {
        if ((this.aNodeSub15Sub29753.sustain[class282Sub131.currentTrack] & 0x4) != 0 && class282Sub131.anInt7579 < 0) {
            int i_7 = this.aNodeSub15Sub29753.anIntArray9712[class282Sub131.currentTrack] / PcmPlayerManager.sampleRate;

            while (true) {
                int i_8 = (i_7 + 1048575 - class282Sub131.anInt7583) / i_7;
                if (i_8 > i_4) {
                    class282Sub131.anInt7583 += i_4 * i_7;
                    break;
                }

                class282Sub131.stream.fill(ints_2, i_3, i_8);
                i_3 += i_8;
                i_4 -= i_8;
                class282Sub131.anInt7583 += i_7 * i_8 - 1048576;
                int i_9 = PcmPlayerManager.sampleRate / 100;
                int i_10 = 262144 / i_7;
                if (i_10 < i_9) {
                    i_9 = i_10;
                }

                AreaSoundStream class282Sub15Sub511 = class282Sub131.stream;
                if (this.aNodeSub15Sub29753.generalPurpose1[class282Sub131.currentTrack] == 0) {
                    class282Sub131.stream = class282Sub131.audioFile.getStream(class282Sub15Sub511.method15323(), class282Sub15Sub511.method15315(), class282Sub15Sub511.method15316());
                } else {
                    class282Sub131.stream = class282Sub131.audioFile.getStream(class282Sub15Sub511.method15323(), 0, class282Sub15Sub511.method15316());
                    this.aNodeSub15Sub29753.method15099(class282Sub131, class282Sub131.patch.pitchOffset[class282Sub131.currentNotePitch] < 0);
                    class282Sub131.stream.method15320(i_9, class282Sub15Sub511.method15315());
                }

                if (class282Sub131.patch.pitchOffset[class282Sub131.currentNotePitch] < 0) {
                    class282Sub131.stream.setNumLoops(-1);
                }

                class282Sub15Sub511.method15322(i_9);
                class282Sub15Sub511.fill(ints_2, i_3, i_5 - i_3);
                if (class282Sub15Sub511.method15311()) {
                    this.aNodeSub15Sub49755.addSubStream(class282Sub15Sub511);
                }
            }
        }

        class282Sub131.stream.fill(ints_2, i_3, i_4);
    }

    @Override
    public PcmStream getNextStream() {
        MusicPatchNode musicPatch;
        do {
            musicPatch = (MusicPatchNode) this.queue.next();
            if (musicPatch == null) {
                return null;
            }
        } while (musicPatch.stream == null);

        return musicPatch.stream;
    }

    @Override
    public int method12228() {
        return 0;
    }

    @Override
    public void skip(int i_1) {
        this.aNodeSub15Sub49755.skip(i_1);

        for (MusicPatchNode class282Sub132 = (MusicPatchNode) this.queue.getFirst(); class282Sub132 != null; class282Sub132 = (MusicPatchNode) this.queue.next()) {
            if (!this.aNodeSub15Sub29753.method15115(class282Sub132)) {
                int i_3 = i_1;

                do {
                    if (i_3 <= class282Sub132.anInt7584) {
                        method15233(class282Sub132, i_3);
                        class282Sub132.anInt7584 -= i_3;
                        break;
                    }

                    method15233(class282Sub132, class282Sub132.anInt7584);
                    i_3 -= class282Sub132.anInt7584;
                } while (!this.aNodeSub15Sub29753.method15128(class282Sub132, null, 0, i_3));
            }
        }

    }
}
