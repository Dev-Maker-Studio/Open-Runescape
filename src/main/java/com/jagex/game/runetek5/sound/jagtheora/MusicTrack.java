package com.jagex.game.runetek5.sound.jagtheora;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.cache.node.impl.ObjectNode;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

/**
 * 
 * @author Brainpower - 28/03/2022
 * Naming from OpenOSRS
 * Reference: https://www.rune-server.ee/runescape-development/rs-503-client-server/help/650307-742-music-midi-decoding.html
 * Reference: https://www.rune-server.ee/runescape-development/rs-503-client-server/snippets/311669-rs-music-file-structure-conversion.html
 * Reference: https://github.com/open-osrs/runelite
 *
 */
public class MusicTrack extends Node {

    byte[] midi;
    IterableNodeMap<ObjectNode> table;

    public MusicTrack(Packet rsbytebuffer1) {
        rsbytebuffer1.currentPosition = rsbytebuffer1.buffer.length - 3;
        int i_2 = rsbytebuffer1.readUByte();
        int i_3 = rsbytebuffer1.readUShort();
        int i_4 = i_2 * 10 + 14;
        rsbytebuffer1.currentPosition = 0;
        int i_5 = 0;
        int i_6 = 0;
        int i_7 = 0;
        int i_8 = 0;
        int i_9 = 0;
        int i_10 = 0;
        int i_11 = 0;
        int i_12 = 0;

        int i_13;
        int i_14;
        int i_15;
        for (i_13 = 0; i_13 < i_2; i_13++) {
            i_14 = -1;

            while (true) {
                i_15 = rsbytebuffer1.readUByte();
                if (i_15 != i_14) {
                    ++i_4;
                }

                i_14 = i_15 & 0xf;
                if (i_15 == 7) {
                    break;
                }

                if (i_15 == 23) {
                    ++i_5;
                } else if (i_14 == 0) {
                    ++i_7;
                } else if (i_14 == 1) {
                    ++i_8;
                } else if (i_14 == 2) {
                    ++i_6;
                } else if (i_14 == 3) {
                    ++i_9;
                } else if (i_14 == 4) {
                    ++i_10;
                } else if (i_14 == 5) {
                    ++i_11;
                } else {
                    if (i_14 != 6) {
                        throw new RuntimeException();
                    }

                    ++i_12;
                }
            }
        }

        i_4 += i_5 * 5;
        i_4 += (i_7 + i_8 + i_6 + i_9 + i_11) * 2;
        i_4 += i_10 + i_12;
        i_13 = rsbytebuffer1.currentPosition;
        i_14 = i_2 + i_5 + i_6 + i_7 + i_8 + i_9 + i_10 + i_11 + i_12;

        for (i_15 = 0; i_15 < i_14; i_15++) {
            rsbytebuffer1.readVarInt();
        }

        i_4 += rsbytebuffer1.currentPosition - i_13;
        i_15 = rsbytebuffer1.currentPosition;
        int i_16 = 0;
        int i_17 = 0;
        int i_18 = 0;
        int i_19 = 0;
        int i_20 = 0;
        int i_21 = 0;
        int i_22 = 0;
        int i_23 = 0;
        int i_24 = 0;
        int i_25 = 0;
        int i_26 = 0;
        int i_27 = 0;
        int i_28 = 0;

        int i_29;
        for (i_29 = 0; i_29 < i_6; i_29++) {
            i_28 = i_28 + rsbytebuffer1.readUByte() & 0x7f;
            if (i_28 != 0 && i_28 != 32) {
                if (i_28 == 1) {
                    ++i_16;
                } else if (i_28 == 33) {
                    ++i_17;
                } else if (i_28 == 7) {
                    ++i_18;
                } else if (i_28 == 39) {
                    ++i_19;
                } else if (i_28 == 10) {
                    ++i_20;
                } else if (i_28 == 42) {
                    ++i_21;
                } else if (i_28 == 99) {
                    ++i_22;
                } else if (i_28 == 98) {
                    ++i_23;
                } else if (i_28 == 101) {
                    ++i_24;
                } else if (i_28 == 100) {
                    ++i_25;
                } else if (i_28 != 64 && i_28 != 65 && i_28 != 120 && i_28 != 121 && i_28 != 123) {
                    ++i_27;
                } else {
                    ++i_26;
                }
            } else {
                ++i_12;
            }
        }

        i_29 = 0;
        int i_30 = rsbytebuffer1.currentPosition;
        rsbytebuffer1.currentPosition += i_26;
        int i_31 = rsbytebuffer1.currentPosition;
        rsbytebuffer1.currentPosition += i_11;
        int i_32 = rsbytebuffer1.currentPosition;
        rsbytebuffer1.currentPosition += i_10;
        int i_33 = rsbytebuffer1.currentPosition;
        rsbytebuffer1.currentPosition += i_9;
        int i_34 = rsbytebuffer1.currentPosition;
        rsbytebuffer1.currentPosition += i_16;
        int i_35 = rsbytebuffer1.currentPosition;
        rsbytebuffer1.currentPosition += i_18;
        int i_36 = rsbytebuffer1.currentPosition;
        rsbytebuffer1.currentPosition += i_20;
        int i_37 = rsbytebuffer1.currentPosition;
        rsbytebuffer1.currentPosition += i_7 + i_8 + i_11;
        int i_38 = rsbytebuffer1.currentPosition;
        rsbytebuffer1.currentPosition += i_7;
        int i_39 = rsbytebuffer1.currentPosition;
        rsbytebuffer1.currentPosition += i_27;
        int i_40 = rsbytebuffer1.currentPosition;
        rsbytebuffer1.currentPosition += i_8;
        int i_41 = rsbytebuffer1.currentPosition;
        rsbytebuffer1.currentPosition += i_17;
        int i_42 = rsbytebuffer1.currentPosition;
        rsbytebuffer1.currentPosition += i_19;
        int i_43 = rsbytebuffer1.currentPosition;
        rsbytebuffer1.currentPosition += i_21;
        int i_44 = rsbytebuffer1.currentPosition;
        rsbytebuffer1.currentPosition += i_12;
        int i_45 = rsbytebuffer1.currentPosition;
        rsbytebuffer1.currentPosition += i_9;
        int i_46 = rsbytebuffer1.currentPosition;
        rsbytebuffer1.currentPosition += i_22;
        int i_47 = rsbytebuffer1.currentPosition;
        rsbytebuffer1.currentPosition += i_23;
        int i_48 = rsbytebuffer1.currentPosition;
        rsbytebuffer1.currentPosition += i_24;
        int i_49 = rsbytebuffer1.currentPosition;
        rsbytebuffer1.currentPosition += i_25;
        int i_50 = rsbytebuffer1.currentPosition;
        rsbytebuffer1.currentPosition += i_5 * 3;
        this.midi = new byte[i_4];
        Packet rsbytebuffer51 = new Packet(this.midi);
        rsbytebuffer51.writeInt(1297377380);
        rsbytebuffer51.writeInt(6);
        rsbytebuffer51.writeShort(i_2 > 1 ? 1 : 0);
        rsbytebuffer51.writeShort(i_2);
        rsbytebuffer51.writeShort(i_3);
        rsbytebuffer1.currentPosition = i_13;
        int i_52 = 0;
        int i_53 = 0;
        int i_54 = 0;
        int i_55 = 0;
        int i_56 = 0;
        int i_57 = 0;
        int i_58 = 0;
        int[] ints_59 = new int[128];
        i_28 = 0;

        label228:
        for (int i_60 = 0; i_60 < i_2; i_60++) {
            rsbytebuffer51.writeInt(1297379947);
            rsbytebuffer51.currentPosition += 4;
            int i_61 = rsbytebuffer51.currentPosition;
            int i_62 = -1;

            while (true) {
                while (true) {
                    int i_63 = rsbytebuffer1.readVarInt();
                    rsbytebuffer51.writeFlags(i_63);
                    int i_64 = rsbytebuffer1.buffer[i_29++] & 0xff;
                    boolean bool65 = i_64 != i_62;
                    i_62 = i_64 & 0xf;
                    if (i_64 == 7) {
                        if (bool65) {
                            rsbytebuffer51.writeByte(255);
                        }

                        rsbytebuffer51.writeByte(47);
                        rsbytebuffer51.writeByte(0);
                        rsbytebuffer51.writeIntLength(rsbytebuffer51.currentPosition - i_61);
                        continue label228;
                    }

                    if (i_64 == 23) {
                        if (bool65) {
                            rsbytebuffer51.writeByte(255);
                        }

                        rsbytebuffer51.writeByte(81);
                        rsbytebuffer51.writeByte(3);
                        rsbytebuffer51.writeByte(rsbytebuffer1.buffer[i_50++]);
                        rsbytebuffer51.writeByte(rsbytebuffer1.buffer[i_50++]);
                        rsbytebuffer51.writeByte(rsbytebuffer1.buffer[i_50++]);
                    } else {
                        i_52 ^= i_64 >> 4;
                        if (i_62 == 0) {
                            if (bool65) {
                                rsbytebuffer51.writeByte(i_52 + 144);
                            }

                            i_53 += rsbytebuffer1.buffer[i_37++];
                            i_54 += rsbytebuffer1.buffer[i_38++];
                            rsbytebuffer51.writeByte(i_53 & 0x7f);
                            rsbytebuffer51.writeByte(i_54 & 0x7f);
                        } else if (i_62 == 1) {
                            if (bool65) {
                                rsbytebuffer51.writeByte(i_52 + 128);
                            }

                            i_53 += rsbytebuffer1.buffer[i_37++];
                            i_55 += rsbytebuffer1.buffer[i_40++];
                            rsbytebuffer51.writeByte(i_53 & 0x7f);
                            rsbytebuffer51.writeByte(i_55 & 0x7f);
                        } else if (i_62 == 2) {
                            if (bool65) {
                                rsbytebuffer51.writeByte(i_52 + 176);
                            }

                            i_28 = i_28 + rsbytebuffer1.buffer[i_15++] & 0x7f;
                            rsbytebuffer51.writeByte(i_28);
                            byte b_66;
                            if (i_28 != 0 && i_28 != 32) {
                                if (i_28 == 1) {
                                    b_66 = rsbytebuffer1.buffer[i_34++];
                                } else if (i_28 == 33) {
                                    b_66 = rsbytebuffer1.buffer[i_41++];
                                } else if (i_28 == 7) {
                                    b_66 = rsbytebuffer1.buffer[i_35++];
                                } else if (i_28 == 39) {
                                    b_66 = rsbytebuffer1.buffer[i_42++];
                                } else if (i_28 == 10) {
                                    b_66 = rsbytebuffer1.buffer[i_36++];
                                } else if (i_28 == 42) {
                                    b_66 = rsbytebuffer1.buffer[i_43++];
                                } else if (i_28 == 99) {
                                    b_66 = rsbytebuffer1.buffer[i_46++];
                                } else if (i_28 == 98) {
                                    b_66 = rsbytebuffer1.buffer[i_47++];
                                } else if (i_28 == 101) {
                                    b_66 = rsbytebuffer1.buffer[i_48++];
                                } else if (i_28 == 100) {
                                    b_66 = rsbytebuffer1.buffer[i_49++];
                                } else if (i_28 != 64 && i_28 != 65 && i_28 != 120 && i_28 != 121 && i_28 != 123) {
                                    b_66 = rsbytebuffer1.buffer[i_39++];
                                } else {
                                    b_66 = rsbytebuffer1.buffer[i_30++];
                                }
                            } else {
                                b_66 = rsbytebuffer1.buffer[i_44++];
                            }

                            int i_67 = b_66 + ints_59[i_28];
                            ints_59[i_28] = i_67;
                            rsbytebuffer51.writeByte(i_67 & 0x7f);
                        } else if (i_62 == 3) {
                            if (bool65) {
                                rsbytebuffer51.writeByte(i_52 + 224);
                            }

                            i_56 += rsbytebuffer1.buffer[i_45++];
                            i_56 += rsbytebuffer1.buffer[i_33++] << 7;
                            rsbytebuffer51.writeByte(i_56 & 0x7f);
                            rsbytebuffer51.writeByte(i_56 >> 7 & 0x7f);
                        } else if (i_62 == 4) {
                            if (bool65) {
                                rsbytebuffer51.writeByte(i_52 + 208);
                            }

                            i_57 += rsbytebuffer1.buffer[i_32++];
                            rsbytebuffer51.writeByte(i_57 & 0x7f);
                        } else if (i_62 == 5) {
                            if (bool65) {
                                rsbytebuffer51.writeByte(i_52 + 160);
                            }

                            i_53 += rsbytebuffer1.buffer[i_37++];
                            i_58 += rsbytebuffer1.buffer[i_31++];
                            rsbytebuffer51.writeByte(i_53 & 0x7f);
                            rsbytebuffer51.writeByte(i_58 & 0x7f);
                        } else {
                            if (i_62 != 6) {
                                throw new RuntimeException();
                            }

                            if (bool65) {
                                rsbytebuffer51.writeByte(i_52 + 192);
                            }

                            rsbytebuffer51.writeByte(rsbytebuffer1.buffer[i_44++]);
                        }
                    }
                }
            }
        }

    }

    public static MusicTrack loadMusictrack(Js5ResourceProvider resource, int archiveId, int fileId) {
        byte[] data = resource.getFile(archiveId, fileId);
        return data == null ? null : new MusicTrack(new Packet(data));
    }

    void method12165() {
        if (this.table == null) {
            this.table = new IterableNodeMap<>(16);
            int[] ints_1 = new int[16];
            int[] ints_2 = new int[16];
            ints_2[9] = 128;
            ints_1[9] = 128;
            MidiFileReader class804 = new MidiFileReader(this.midi);
            int i_5 = class804.getTrackCount();

            int i_6;
            for (i_6 = 0; i_6 < i_5; i_6++) {
                class804.getGotoTrack(i_6);
                class804.getTrackLength(i_6);
                class804.markTracePosition(i_6);
            }

            label56:
            do {
                while (true) {
                    i_6 = class804.getPrioritizedTrack();
                    int i_7 = class804.trackLengths[i_6];

                    while (i_7 == class804.trackLengths[i_6]) {
                        class804.getGotoTrack(i_6);
                        int i_8 = class804.readMessage(i_6);
                        if (i_8 == 1) {
                            class804.setTrackDone();
                            class804.markTracePosition(i_6);
                            continue label56;
                        }

                        int i_9 = i_8 & 0xf0;
                        int i_10;
                        int i_11;
                        int i_12;
                        if (i_9 == 176) {
                            i_10 = i_8 & 0xf;
                            i_11 = i_8 >> 8 & 0x7f;
                            i_12 = i_8 >> 16 & 0x7f;
                            if (i_11 == 0) {
                                ints_1[i_10] = (i_12 << 14) + (ints_1[i_10] & -2080769);
                            }

                            if (i_11 == 32) {
                                ints_1[i_10] = (ints_1[i_10] & -16257) + (i_12 << 7);
                            }
                        }

                        if (i_9 == 192) {
                            i_10 = i_8 & 0xf;
                            i_11 = i_8 >> 8 & 0x7f;
                            ints_2[i_10] = i_11 + ints_1[i_10];
                        }

                        if (i_9 == 144) {
                            i_10 = i_8 & 0xf;
                            i_11 = i_8 >> 8 & 0x7f;
                            i_12 = i_8 >> 16 & 0x7f;
                            if (i_12 > 0) {
                                int i_13 = ints_2[i_10];
                                ObjectNode class282Sub4714 = this.table.get(i_13);
                                if (class282Sub4714 == null) {
                                    class282Sub4714 = new ObjectNode(new byte[128]);
                                    this.table.put(class282Sub4714, i_13);
                                }

                                ((byte[]) class282Sub4714.value)[i_11] = 1;
                            }
                        }

                        class804.getTrackLength(i_6);
                        class804.markTracePosition(i_6);
                    }
                }
            } while (!class804.isDone());
        }

    }

    void clear() {
        this.table = null;
    }
}
