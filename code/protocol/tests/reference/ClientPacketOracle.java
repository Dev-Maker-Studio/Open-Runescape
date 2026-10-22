import com.jagex.game.runetek5.client.*;
import com.jagex.game.runetek5.client.input.mouse.*;
import com.jagex.game.runetek5.client.input.keyboard.*;
import com.jagex.game.runetek5.client.input.camera.Camera;
import com.jagex.game.runetek5.config.quickchat.QuickChatValueType;
import com.jagex.game.runetek5.encoding.Huffman;
import com.jagex.game.runetek5.encoding.HuffmanList;
import com.jagex.game.runetek5.network.packet.*;
import com.jagex.game.runetek5.tools.Isaac;
import com.jagex.game.runetek5.cache.node.NodeCollection;
import com.jagex.game.runetek5.staticmethods.StaticMethods;
import com.jagex.game.runetek5.utils.TextUtils;
import com.jagex.game.runetek5.utils.Utils;
import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.client.login.AccountCreationStage;
import java.awt.Canvas;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import com.jagex.game.runetek5.client.input.mouse.MouseActionServer;

public final class ClientPacketOracle {
    interface Body {
        void write(Packet p) throws Exception;
    }

    static final int[] SEED = {1, -2, 0x12345678, Integer.MIN_VALUE};
    static Isaac stream = new Isaac(SEED);
    static Isaac scratch = new Isaac(new int[] {9, 8, 7, 6});
    static StringBuilder out = new StringBuilder();
    static int cases = 0;

    static String hex(TcpPacket t) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < t.buffer.currentPosition; i++) {
            sb.append(String.format("%02x", t.buffer.buffer[i] & 255));
        }
        return sb.toString();
    }

    static String frame(ClientProt prot, Body body) {
        try {
            TcpPacket dry = PacketEncoder.createPacket(prot, scratch);
            body.write(dry.buffer);
        } catch (Throwable e) {
            return "err";
        }
        TcpPacket real = PacketEncoder.createPacket(prot, stream);
        try {
            body.write(real.buffer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return hex(real);
    }

    static void emit(String name, String args, String hex) {
        out.append(name).append('\t').append(args).append('\t').append(hex).append('\n');
        cases++;
    }

    static void single(String name, String args, Body body) {
        emit(name, args, frame(ClientProt.valueOf(name), body));
    }

    static String t(String s) {
        if (s.isEmpty()) {
            return "~";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            sb.append(String.format("%04x", (int) s.charAt(i)));
        }
        return sb.toString();
    }

    static String rep(char c, int n) {
        return String.valueOf(c).repeat(n);
    }

    static final int[] INTS = {0, 1, -1, 127, 128, 255, 256, 32767, 32768, 65535, 65536, 0x7fffffff,
        Integer.MIN_VALUE, 0x12345678, -0x12345678};
    static final String[] STR_ALL = {"", "a", "Zezima", "hello world", rep('x', 80), rep('y', 200),
        "é€™ü", "日本", "ŒœŸ", "a\u0000b"};
    static final String[] STR_SHORT = {"", "a", "Zezima", "hello world", rep('x', 80), "é€™ü",
        "日本", "ŒœŸ", "a\u0000b"};
    static final String[] MSG_ALL = {"", "a", "hello world", "The quick brown fox jumps over the lazy dog.",
        rep('e', 80), rep('q', 200), "é€", "A"};

    static final byte[] TABLE_A = new byte[256];
    static final byte[] TABLE_B = new byte[256];

    static {
        for (int i = 0; i < 256; i++) {
            TABLE_A[i] = (byte) (i < 64 ? 7 : i < 128 ? 8 : 9);
        }
        String l3 = " e";
        String l4 = "taoi";
        String l5 = "nshrdlcu";
        String l6 = "mwfgypbv";
        String l7 = "kjxqz0123456789.";
        for (char c : l3.toCharArray()) TABLE_B[c] = 3;
        for (char c : l4.toCharArray()) TABLE_B[c] = 4;
        for (char c : l5.toCharArray()) TABLE_B[c] = 5;
        for (char c : l6.toCharArray()) TABLE_B[c] = 6;
        for (char c : l7.toCharArray()) TABLE_B[c] = 7;
    }

    static boolean nul(String... ss) {
        for (String s : ss) {
            if (s.indexOf('\0') >= 0) {
                return true;
            }
        }
        return false;
    }

    static void simple() {
        for (int v : INTS) {
            for (String n : new String[] {"SONG_LOADED", "SOUND_EFFECT_MUSIC_ENDED", "TRANSMITVAR_VERIFYID",
                "REQUEST_WORLD_LIST", "RESUME_COUNTDIALOG"}) {
                single(n, "" + v, p -> p.writeInt(v));
            }
            for (String n : new String[] {"PLAY_VORBIS", "RESUME_HSLDIALOG", "GRAND_EXCHANGE_ITEM_SELECT",
                "WRITE_PING"}) {
                single(n, "" + v, p -> p.writeShort(v));
            }
            for (String n : new String[] {"CHAT_TYPE", "ACCOUNT_CREATION_STAGE"}) {
                single(n, "" + v, p -> p.writeByte(v));
            }
        }
        for (int v = 0; v < 2; v++) {
            int f = v;
            single("CUTSCENE_FINISHED", "" + v, p -> p.writeByte(f));
        }
        single("CLOSE_INTERFACE", "-", p -> { });
        single("UNK_82", "-", p -> p.writeInt(1057001181));
        int[] fps = {0, 1, 50, 255, 256, 1000, -5};
        for (int i = 0; i < INTS.length; i++) {
            int key = INTS[i];
            int flags = INTS[(i + 5) % INTS.length];
            for (int f : fps) {
                single("SEND_FPS", key + " " + flags + " " + f, p -> {
                    p.writeIntInverseMiddle(key);
                    p.writeIntMiddle(flags);
                    p.writeByteSubtract(Math.min(f, 255));
                });
            }
        }
        int[] bytes = {0, 1, 2, 255};
        for (int a : bytes) {
            for (int b : bytes) {
                for (int c : bytes) {
                    single("CHAT_SETFILTER", a + " " + b + " " + c, p -> {
                        p.writeByte(a);
                        p.writeByte(b);
                        p.writeByte(c);
                    });
                }
            }
        }
        int[] wm = {0, 1, 2, 3};
        int[] w = {0, 765, 1920, 65535};
        int[] h = {0, 503, 1080, 65535, -1};
        int[] aa = {0, 1, 4, 255};
        for (int a : wm) {
            for (int b : w) {
                for (int c : h) {
                    for (int d : aa) {
                        single("SCREEN_SIZE", a + " " + b + " " + c + " " + d, p -> {
                            p.writeByte(a);
                            p.writeShort(b);
                            p.writeShort(c * -969250379);
                            p.writeByte(d);
                        });
                    }
                }
            }
        }
        int[] zs = {0, 1, 3};
        int[] xy = {0, 1, 3200, 16383};
        for (int z : zs) {
            for (int x : xy) {
                for (int y : xy) {
                    single("WORLD_MAP_CLICK", z + " " + x + " " + y, p -> p.writeIntLittle(z << 28 | x << 14 | y));
                }
            }
        }
    }

    static void strings() {
        for (String s : STR_ALL) {
            for (String n : new String[] {"RESUME_TEXTDIALOG", "RESUME_NAMEDIALOG", "RESUME_CLANFORUMQFCDIALOG",
                "EMAIL_VALIDATION_SUBMIT_CODE", "ADD_FRIEND", "REMOVE_FRIEND", "REMOVE_IGNORE", "FC_KICK",
                "FC_JOIN"}) {
                boolean cl = n.equals("RESUME_TEXTDIALOG") || n.equals("RESUME_NAMEDIALOG") || n.equals("RESUME_CLANFORUMQFCDIALOG");
                single(n, t(s), p -> {
                    p.writeByte(cl ? s.length() + 1 : TextUtils.getLength(s));
                    p.writeString(s);
                });
            }
            single("COMMAND", "1 0 " + t(s), p -> {
                p.writeByte(s.length() + 3);
                p.writeByte(1);
                p.writeByte(0);
                p.writeString(s);
            });
            single("COMMAND", "0 1 " + t(s), p -> {
                p.writeByte(s.length() + 3);
                p.writeByte(0);
                p.writeByte(1);
                p.writeString(s);
            });
        }
        single("FC_JOIN", "leave", p -> p.writeByte(0));
        int[] ranks = {0, 1, 5, 127, -1, 255};
        for (String s : STR_ALL) {
            for (int r : ranks) {
                single("FC_SET_RANK", r + " " + t(s), p -> {
                    p.writeByte(1 + TextUtils.getLength(s));
                    p.writeByteSubtract(r);
                    p.writeString(s);
                });
            }
            for (int f = 0; f < 2; f++) {
                int ff = f;
                single("ADD_IGNORE", t(s) + " " + f, p -> {
                    p.writeByte(TextUtils.getLength(s) + 1);
                    p.writeString(s);
                    p.writeByte(ff);
                });
            }
        }
        for (String name : STR_SHORT) {
            for (String comment : STR_SHORT) {
                for (int rule : new int[] {1, 2, 10, 255}) {
                    for (int mute : new int[] {0, 1}) {
                        String c80 = comment.length() > 80 ? comment.substring(0, 80) : comment;
                        single("REPORT_ABUSE", t(name) + " " + rule + " " + mute + " " + t(comment), p -> {
                            p.writeByte(TextUtils.getLength(name) + 2 + TextUtils.getLength(c80));
                            p.writeString(name);
                            p.writeByte(rule - 1);
                            p.writeByte(mute);
                            p.writeString(c80);
                        });
                    }
                }
            }
        }
        int[] idx = {0, 1, 255, 256, 32767, 65535, -1};
        for (String s : STR_ALL) {
            for (int i : idx) {
                single("UNUSED_CLAN_OP", i + " " + t(s), p -> {
                    p.writeByte(2 + TextUtils.getLength(s));
                    p.writeShort(i);
                    p.writeString(s);
                });
                for (int m = 0; m < 2; m++) {
                    int mm = m;
                    single("CLANCHANNEL_KICKUSER", m + " " + i + " " + t(s), p -> {
                        p.writeByte(3 + TextUtils.getLength(s));
                        p.writeByte(mm);
                        p.writeShort(i);
                        p.writeString(s);
                    });
                }
            }
        }
        for (String a : STR_SHORT) {
            for (String b : STR_SHORT) {
                for (String c : new String[] {"", "http://x", "é"}) {
                    for (int fl = 0; fl < 4; fl++) {
                        boolean nw = (fl & 1) != 0;
                        boolean ex = (fl & 2) != 0;
                        single("LOBBY_HYPERLINK", t(a) + " " + t(b) + " " + t(c) + " " + (nw ? 1 : 0) + " " + (ex ? 1 : 0), p -> {
                            p.writeShort(TextUtils.getLength(a) + TextUtils.getLength(b) + TextUtils.getLength(c) + 1);
                            p.writeString(a);
                            p.writeString(b);
                            p.writeString(c);
                            int flags = 0;
                            if (nw) flags |= 1;
                            if (ex) flags |= 2;
                            p.writeByte(flags);
                        });
                    }
                }
            }
        }
        for (String title : STR_SHORT) {
            for (String desc : STR_ALL) {
                for (int type : new int[] {0, 1, 7, 255}) {
                    single("BUG_REPORT", type + " " + t(title) + " " + t(desc), p -> {
                        p.writeShort(1 + Utils.stringLengthPlus2(title) + Utils.stringLengthPlus2(desc));
                        p.writeByte(type);
                        p.writeJagString(desc);
                        p.writeJagString(title);
                    });
                }
            }
        }
        for (String s : STR_ALL) {
            for (int fl = 0; fl < 8; fl++) {
                int f = fl;
                single("EMAIL_VALIDATION_ADD_NEW_ADDRESS", t(s) + " " + (f & 1) + " " + ((f >> 1) & 1) + " " + ((f >> 2) & 1), p -> {
                    p.writeShort(TextUtils.getLength(s) + 1);
                    p.writeString(s);
                    p.writeByte(f);
                });
            }
        }
        for (String a : STR_SHORT) {
            for (String b : STR_SHORT) {
                single("EMAIL_VALIDATION_CHANGE_ADDRESS", t(a) + " " + t(b), p -> {
                    p.writeShort(TextUtils.getLength(a) + TextUtils.getLength(b));
                    p.writeString(a);
                    p.writeString(b);
                });
            }
        }
    }

    static void useTable(String id) {
        HuffmanList.HUFFMAN = new Huffman(id.equals("A") ? TABLE_A : TABLE_B);
    }

    static void encodeValues(Packet p, int[] types, int[] values) {
        for (int i = 0; i < types.length; i++) {
            if (i >= values.length) {
                break;
            }
            int size = QuickChatValueType.Companion.get(types[i]).clientTransmitSize;
            if (size > 0) {
                p.writeVariableLengthLong((long) values[i], size);
            }
        }
    }

    static String ints(int[] a) {
        if (a.length == 0) {
            return "~";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < a.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(a[i]);
        }
        return sb.toString();
    }

    static void chat() {
        for (String table : new String[] {"A", "B"}) {
            useTable(table);
            for (String m : MSG_ALL) {
                for (int color : new int[] {0, 5, 255}) {
                    for (int effect : new int[] {0, 3, 255}) {
                        single("CHAT", table + " " + color + " " + effect + " " + t(m), p -> {
                            p.writeByte(0);
                            int start = p.currentPosition;
                            p.writeByte(color);
                            p.writeByte(effect);
                            PacketEncoder.chatMessageToBuffer(p, m);
                            p.writeIndex(p.currentPosition - start);
                        });
                    }
                }
                for (String rec : new String[] {"Zezima", "a", "é€"}) {
                    single("SEND_PRIVATE_MESSAGE", table + " " + t(rec) + " " + t(m), p -> {
                        p.writeShort(0);
                        int start = p.currentPosition;
                        p.writeString(rec);
                        PacketEncoder.chatMessageToBuffer(p, m);
                        p.writeShortLength(p.currentPosition - start);
                    });
                }
            }
        }
        int[][] typeLists = {{}, {0}, {1, 2}, {4, 10}, {6}, {0, 1, 2, 4, 10}, {2, 2}, {7, 8, 9}, {11, 12, 13, 14, 15},
            {3}, {5}, {99}, {-1}, {0, 3}};
        int[][] valueLists = {{}, {0}, {1}, {255}, {65535}, {-1}, {0x12345678, 7}, {5, 65536, 3, 9, 0x7fffffff},
            {Integer.MIN_VALUE, -2, 300}, {7, 8, 9, 10, 11, 12}};
        int[] ids = {0, 1, 255, 256, 32767, 65535, -1};
        for (int[] types : typeLists) {
            for (int[] values : valueLists) {
                for (int id : ids) {
                    for (int mode : new int[] {0, 2, 255}) {
                        single("QUICKCHAT_PUBLIC", mode + " " + id + " " + ints(types) + " " + ints(values), p -> {
                            p.writeByte(0);
                            int start = p.currentPosition;
                            p.writeByte(mode);
                            p.writeShort(id);
                            encodeValues(p, types, values);
                            p.writeIndex(p.currentPosition - start);
                        });
                    }
                    for (String rec : new String[] {"Zezima", "", "é€", "a\u0000b"}) {
                        single("QUICKCHAT_PRIVATE", t(rec) + " " + id + " " + ints(types) + " " + ints(values), p -> {
                            p.writeByte(0);
                            int start = p.currentPosition;
                            p.writeString(rec);
                            p.writeShort(id);
                            encodeValues(p, types, values);
                            p.writeIndex(p.currentPosition - start);
                        });
                    }
                }
            }
        }
    }

    static final int[][] CTX = {
        {0, 3200, 3200, 0x2d0003, 4151, 7, 12, 1024, 3, 4, 3222, 3218, 995, 0x1d0005, 9, 561},
        {1, 0, 0, -1, 65535, 65535, 2047, 8191, 255, 255, 65535, 65535, 65535, -2, 65535, 65535},
        {1, 6400, 12800, 0x7fffffff, 0, 0, 0, 0, 0, 0, 0, 0, 0, Integer.MIN_VALUE, 0, 0}};
    static final long[] UIDS = {0, 1, 2, 5, 10, 70000, 0x0000123400000007L, 0x7fffffff00000003L, -2L};
    static final int[][] PARAMS = {{0, 0}, {52, 60}, {65535, 40000}};

    static ClientProt prot(String base, int n) {
        return ClientProt.valueOf(base + n);
    }

    static String menu(int clickIn, long ent, int a1, int a2, int[] c) {
        int ct = clickIn >= 2000 ? clickIn - 2000 : clickIn;
        int uid = (int) ent;
        int run = c[0];
        int bx = c[1];
        int by = c[2];
        int selHash = c[3];
        int selItem = c[4];
        int selSlot = c[5];
        int own = c[6];
        int yaw = c[7];
        int mmRot = c[8];
        int mmZoom = c[9];
        int absX = c[10];
        int absZ = c[11];
        int container = c[12];
        int tHash = c[13];
        int tSlot = c[14];
        int tItem = c[15];
        int objHash = (int) (ent >>> 32) & 0x7fffffff;
        if (ct >= 44 && ct <= 53) {
            return frame(prot("PLAYER_OP", ct - 43), p -> {
                p.writeShort(uid);
                p.writeByteSubtract(run);
            });
        }
        if (ct == 16 || ct == 15) {
            int who = ct == 16 ? own : uid;
            return frame(ClientProt.IF_ON_PLAYER, p -> {
                p.writeShort(selSlot);
                p.writeShortLittle(who);
                p.writeByteSubtract(run);
                p.writeIntInverseMiddle(selHash);
                p.writeShortLittle(selItem);
            });
        }
        if (ct == 2) {
            return frame(ClientProt.IF_ON_OBJECT, p -> {
                p.writeShortAddLittle(a1 + bx);
                p.writeByteSubtract(run);
                p.writeIntMiddle(objHash);
                p.writeInt(selHash);
                p.writeShortLittle(selItem);
                p.writeShortAdd(selSlot);
                p.writeShortLittle(a2 + by);
            });
        }
        if ((ct >= 9 && ct <= 13) || ct == 1003) {
            ClientProt pr = ct == 1003 ? ClientProt.NPC_EXAMINE : prot("NPC_OP", ct - 8);
            return frame(pr, p -> {
                p.writeShort(uid);
                p.writeByte(run);
            });
        }
        if (ct == 60) {
            return frame(ClientProt.UNK_63, p -> {
                p.writeShortAdd(a1 + bx);
                p.writeShort(a2 + by);
            });
        }
        if (ct == 23) {
            if (uid != 0 && uid != 1) {
                return "-";
            }
            return frame(uid == 0 ? ClientProt.WALK : ClientProt.MINI_WALK, p -> {
                p.writeByte(run);
                p.writeShort(a1 + bx);
                p.writeShortLittle(a2 + by);
                if (uid == 1) {
                    p.writeByte(-1);
                    p.writeByte(-1);
                    p.writeShort(yaw);
                    p.writeByte(57);
                    p.writeByte(mmRot);
                    p.writeByte(mmZoom);
                    p.writeByte(89);
                    p.writeShort(absX);
                    p.writeShort(absZ);
                    p.writeByte(63);
                }
            });
        }
        if (ct == 17) {
            return frame(ClientProt.IF_ON_GROUND_ITEM, p -> {
                p.writeShortAdd(selItem);
                p.writeIntInverseMiddle(selHash);
                p.writeShort(uid);
                p.writeByteSubtract(run);
                p.writeShortAddLittle(selSlot);
                p.writeShortAddLittle(a2 + by);
                p.writeShortLittle(a1 + bx);
            });
        }
        if (ct == 59) {
            return frame(ClientProt.IF_ON_TILE, p -> {
                p.writeShortLittle(selItem);
                p.writeShortLittle(a2 + by);
                p.writeIntMiddle(selHash);
                p.writeShortAdd(selSlot);
                p.writeShortLittle(a1 + bx);
            });
        }
        if (ct == 30) {
            return frame(ClientProt.IF_CONTINUE, p -> {
                p.writeIntMiddle(a2);
                p.writeShortAddLittle(a1);
            });
        }
        if (ct == 58) {
            return frame(ClientProt.IF_ON_IF, p -> {
                p.writeShortAddLittle(tSlot);
                p.writeShortLittle(selSlot);
                p.writeShortAddLittle(tItem);
                p.writeIntLittle(tHash);
                p.writeIntInverseMiddle(selHash);
                p.writeShortLittle(selItem);
            });
        }
        if ((ct >= 18 && ct <= 22) || ct == 1004) {
            ClientProt pr = ct == 1004 ? ClientProt.GROUND_ITEM_EXAMINE : prot("GROUND_ITEM_OP", ct - 17);
            return frame(pr, p -> {
                p.writeShortAddLittle(uid);
                p.writeByteInverse(run);
                p.writeShort(a2 + by);
                p.writeShortAdd(a1 + bx);
            });
        }
        if (ct == 8) {
            return frame(ClientProt.IF_ON_NPC, p -> {
                p.writeIntInverseMiddle(selHash);
                p.writeShortAddLittle(uid);
                p.writeByteSubtract(run);
                p.writeShortAddLittle(selItem);
                p.writeShortAdd(selSlot);
            });
        }
        if ((ct >= 3 && ct <= 6) || ct == 1001 || ct == 1002) {
            ClientProt pr = ct == 1001 ? ClientProt.OBJECT_OP5 : ct == 1002 ? ClientProt.OBJECT_EXAMINE : prot("OBJECT_OP", ct - 2);
            return frame(pr, p -> {
                p.writeShort(a2 + by);
                p.writeShort(a1 + bx);
                p.writeInt(objHash);
                p.writeByteAdd(run);
            });
        }
        if (ct == 57 || ct == 1007) {
            if (uid < 1 || uid > 10) {
                return "-";
            }
            return frame(prot("IF_OP", uid), p -> {
                p.writeIntLittle(a2);
                p.writeShort(container);
                p.writeShortAdd(a1);
            });
        }
        return "-";
    }

    static void menus() {
        List<Integer> types = new ArrayList<>();
        for (int i = 44; i <= 53; i++) types.add(i);
        for (int i : new int[] {16, 15, 2, 1003, 60, 23, 17, 59, 30, 58, 1004, 8, 1001, 1002, 57, 1007, 25, 1008, 1012,
            0, -5, 2044, 2023, 3000}) types.add(i);
        for (int i = 9; i <= 13; i++) types.add(i);
        for (int i = 18; i <= 22; i++) types.add(i);
        for (int i = 3; i <= 6; i++) types.add(i);
        for (int ct : types) {
            for (long uid : UIDS) {
                for (int[] pr : PARAMS) {
                    for (int[] c : CTX) {
                        String args = ct + " " + uid + " " + pr[0] + " " + pr[1] + " " + ints(c);
                        emit("MENU", args, menu(ct, uid, pr[0], pr[1], c));
                    }
                }
            }
        }
    }

    static void directs() {
        for (int[] c : CTX) {
            for (int sSlot : new int[] {0, 7, 65535}) {
                for (int sItem : new int[] {0, 4151, 65535}) {
                    for (int sHash : new int[] {0, 0x2d0003, -1, Integer.MIN_VALUE}) {
                        emit("IF_DRAG_ONTO_IF", sSlot + " " + sItem + " " + sHash + " " + ints(c), frame(ClientProt.IF_DRAG_ONTO_IF, p -> {
                            p.writeShortAddLittle(c[14]);
                            p.writeShortLittle(sSlot);
                            p.writeShort(c[15]);
                            p.writeShortAddLittle(sItem);
                            p.writeIntMiddle(sHash);
                            p.writeIntLittle(c[13]);
                        }));
                    }
                }
            }
            for (int idx : new int[] {0, 1, 2047, 65535, -1}) {
                emit("IF_ON_PLAYER_DIRECT", idx + " " + ints(c), frame(ClientProt.IF_ON_PLAYER, p -> {
                    p.writeShort(c[5]);
                    p.writeShortLittle(idx);
                    p.writeByteSubtract(0);
                    p.writeIntInverseMiddle(c[3]);
                    p.writeShortLittle(c[4]);
                }));
            }
        }
        for (int op = -1; op <= 11; op++) {
            for (int idx : new int[] {0, 1, 2047, 65535}) {
                ClientProt pr = null;
                switch (op) {
                    case 1: pr = ClientProt.PLAYER_OP1; break;
                    case 4: pr = ClientProt.PLAYER_OP4; break;
                    case 5: pr = ClientProt.PLAYER_OP5; break;
                    case 6: pr = ClientProt.PLAYER_OP6; break;
                    case 7: pr = ClientProt.PLAYER_OP7; break;
                    case 9: pr = ClientProt.PLAYER_OP9; break;
                    default: break;
                }
                String h = "-";
                if (pr != null) {
                    h = frame(pr, p -> {
                        p.writeShort(idx);
                        p.writeByteSubtract(0);
                    });
                }
                emit("PLAYER_OPERATION", op + " " + idx, h);
            }
        }
        for (int[] c : CTX) {
            for (int hash : INTS) {
                for (int slot : new int[] {0, 1, 65535, -1}) {
                    emit("IF_CONTINUE", hash + " " + slot, frame(ClientProt.IF_CONTINUE, p -> {
                        p.writeIntMiddle(hash);
                        p.writeShortAddLittle(slot);
                    }));
                }
            }
            break;
        }
    }


    static String capture() {
        NodeCollection q = Client.GAME_CONNECTION_CONTEXT.queuedPackets;
        StringBuilder sb = new StringBuilder();
        TcpPacket p;
        boolean first = true;
        while ((p = (TcpPacket) q.popHead()) != null) {
            if (!first) sb.append('|');
            first = false;
            for (int i = 0; i < p.capacity; i++) {
                sb.append(String.format("%02x", p.buffer.buffer[i] & 255));
            }
        }
        return first ? "-" : sb.toString();
    }

    static MouseActionServer rec(String tok, long base) {
        String[] f = tok.split(",");
        MouseActionServer r = MouseActionServer.createMouseRecord(Integer.parseInt(f[0]), Integer.parseInt(f[1]),
            Integer.parseInt(f[2]), Integer.parseInt(f[3]));
        r.time = base + Long.parseLong(f[4]);
        return r;
    }

    static String runMouse(String spec) {
        String[] tok = spec.split(" ");
        boolean fresh = tok[0].equals("F");
        long base = fresh ? Utils.time() + 100000000L : 1000L;
        MouseActionServer.initialize();
        MouseActionServer.reset();
        MouseActionServer.CLICK_PACKET.records.clear();
        Client.Companion.getMOUSE_MOVEMENTS().remove_all();
        StaticMethods.cameraUpdateCooldown = 0;
        Client.Companion.setSendCameraAngleToServer(false);
        GameShell.appletHasFocus = true;
        StringBuilder result = new StringBuilder();
        int keys = 0;
        List<String> ops = new ArrayList<>();
        for (int i = 1; i <= tok.length; i++) {
            if (i < tok.length && !tok[i].equals(";")) {
                ops.add(tok[i]);
                continue;
            }
            Client.Companion.setMaximumHeldKeys(0);
            for (String op : ops) {
                String kind = op.substring(0, op.indexOf(':'));
                String arg = op.substring(op.indexOf(':') + 1);
                switch (kind) {
                    case "m": MouseActionServer.MOVE_PACKET.append(rec(arg, base)); break;
                    case "c": MouseActionServer.CLICK_PACKET.append(rec(arg, base)); break;
                    case "q": MouseActionServer.CLICK_PACKET.addToQueue(rec(arg, base)); break;
                    case "h": Client.Companion.getMOUSE_MOVEMENTS().addFirst(rec(arg, base)); break;
                    case "k": {
                        String[] f = arg.split(",");
                        int code = Integer.parseInt(f[0]);
                        long time = base + Long.parseLong(f[1]);
                        int slot = Client.Companion.getMaximumHeldKeys();
                        Client.Companion.setMaximumHeldKeys(slot + 1);
                        Client.Companion.getKEY_RECORDS()[slot] = new KeyRecord() {
                            public char getCharacter() { return 0; }
                            public int getType() { return 0; }
                            public int getCode() { return code; }
                            public long getTimeSubmitted() { return time; }
                            public int getFlags() { return 0; }
                        };
                        break;
                    }
                    case "cam": {
                        String[] f = arg.split(",");
                        Camera.camAngleX = Float.parseFloat(f[0]);
                        Camera.camAngleY = Float.parseFloat(f[1]);
                        Client.Companion.setSendCameraAngleToServer(true);
                        break;
                    }
                    case "foc": GameShell.appletHasFocus = arg.equals("1"); break;
                    default: throw new IllegalStateException(kind);
                }
            }
            ops.clear();
            PacketEncoder.submitQueuedPackets();
            if (result.length() > 0) result.append('/');
            result.append(capture());
            Client.Companion.getMOUSE_MOVEMENTS().remove_all();
        }
        return result.toString();
    }

    static final int[] METAS = {160, 161, 162, 164, 165, 167, 168, 170, 512, 513, 515, 999};
    static final long[] GAPS = {0, 5, 19, 20, 25, 60, 150, 300, 639, 640, 1000, 163840, 200000};
    static final int[] EDGE = {-1, 0, 1, 65534, 65535, 70000, -5, 32768, 255, 256};

    static String rrec(Random rnd, int[] pos, long[] time, boolean monotone) {
        int mode = rnd.nextInt(6);
        int x = pos[0];
        int y = pos[1];
        if (mode == 0) {
            x += rnd.nextInt(81) - 40;
            y += rnd.nextInt(81) - 40;
        } else if (mode == 1) {
            x += rnd.nextInt(401) - 200;
            y += rnd.nextInt(401) - 200;
        } else if (mode == 2) {
            x = rnd.nextInt(800);
            y = rnd.nextInt(600);
        } else if (mode == 3) {
            x = EDGE[rnd.nextInt(EDGE.length)];
            y = EDGE[rnd.nextInt(EDGE.length)];
        } else if (mode == 4) {
        } else {
            x += rnd.nextInt(7) - 3;
            y += rnd.nextInt(7) - 3;
        }
        pos[0] = x;
        pos[1] = y;
        long gap = GAPS[rnd.nextInt(GAPS.length)];
        time[0] += monotone ? gap : gap - 100;
        return x + "," + y + "," + METAS[rnd.nextInt(METAS.length)] + "," + rnd.nextInt(256) + "," + time[0];
    }

    static void mouseScenario(String spec) {
        emit("MOUSE", spec, runMouse(spec));
    }

    static void mouse() {
        Random rnd = new Random(20260101L);
        for (int n = 0; n < 700; n++) {
            boolean fresh = rnd.nextInt(3) != 0;
            boolean monotone = rnd.nextInt(8) != 0;
            StringBuilder sb = new StringBuilder(fresh ? "F" : "S");
            int cycles = 1 + rnd.nextInt(4);
            int[] pos = {rnd.nextInt(800), rnd.nextInt(600)};
            long[] time = {rnd.nextInt(5000)};
            for (int c = 0; c < cycles; c++) {
                if (c > 0) sb.append(" ;");
                int big = rnd.nextInt(10) == 0 ? 40 : 0;
                int m = rnd.nextInt(13) + big;
                int cl = rnd.nextInt(9) + big;
                int q = rnd.nextInt(14);
                for (int i = 0; i < m; i++) sb.append(" m:").append(rrec(rnd, pos, time, monotone));
                for (int i = 0; i < cl; i++) sb.append(" c:").append(rrec(rnd, pos, time, monotone));
                for (int i = 0; i < q; i++) sb.append(" q:").append(rrec(rnd, pos, time, monotone));
                if (rnd.nextInt(2) == 0) sb.append(" h:").append(rrec(rnd, pos, time, monotone));
                int k = rnd.nextInt(5);
                for (int i = 0; i < k; i++) {
                    sb.append(" k:").append(rnd.nextInt(112)).append(',').append(time[0] + rnd.nextInt(200000) - 50);
                }
                if (rnd.nextInt(3) == 0) {
                    sb.append(" cam:").append(rnd.nextInt(16384)).append(',').append(rnd.nextInt(2048) - 100);
                }
                if (rnd.nextInt(6) == 0) sb.append(" foc:").append(rnd.nextInt(2));
            }
            mouseScenario(sb.toString());
        }
        for (int n = 1; n <= 75; n += 37) {
            StringBuilder sb = new StringBuilder("F");
            for (int i = 0; i < n; i++) sb.append(" k:").append(i % 112).append(',').append(i * 7);
            mouseScenario(sb.toString());
        }
        StringBuilder cool = new StringBuilder("F");
        for (int c = 0; c < 45; c++) {
            if (c > 0) cool.append(" ;");
            cool.append(" cam:").append(1000 + c * 8).append(',').append(2000 + c * 16);
        }
        mouseScenario(cool.toString());
        StringBuilder foc = new StringBuilder("F foc:0 ; ; foc:1 ; foc:1 ; foc:0 ; foc:0");
        mouseScenario(foc.toString());
        StringBuilder bigm = new StringBuilder("F");
        long tt = 0;
        for (int i = 0; i < 70; i++) {
            tt += 25;
            bigm.append(" m:").append(i * 300).append(',').append(i * 300).append(",161,0,").append(tt);
        }
        bigm.append(" ; ; ; ;");
        mouseScenario(bigm.toString());
        StringBuilder bigc = new StringBuilder("F");
        tt = 0;
        for (int i = 0; i < 70; i++) {
            tt += 25;
            bigc.append(" c:").append(i * 300).append(',').append(i * 300).append(",161,").append(i).append(',').append(tt);
        }
        bigc.append(" q:5,5,161,1,10 q:6,6,164,0,20 ; ; ; ;");
        mouseScenario(bigc.toString());
        StringBuilder queue = new StringBuilder("F");
        for (int i = 0; i < 14; i++) queue.append(" q:").append(i).append(',').append(i + 1).append(",161,").append(i % 2).append(',').append(i * 30);
        queue.append(" ; ; ; ; ; ; ; ; ; ; ; ; ;");
        mouseScenario(queue.toString());
        mouseScenario("S m:100,200,161,0,0 m:110,205,160,3,30 q:50,60,161,0,100 cam:1000,2000");
    }

    static String runKeys(String spec) {
        CanvasKeyRecorder k = new CanvasKeyRecorder(new Canvas());
        Canvas c = new Canvas();
        StringBuilder sb = new StringBuilder();
        for (String t : spec.split(" ")) {
            if (t.equals("F")) {
                k.focusLost(new FocusEvent(c, FocusEvent.FOCUS_LOST));
            } else if (t.startsWith("P:")) {
                k.keyPressed(new KeyEvent(c, KeyEvent.KEY_PRESSED, 0, 0, Integer.parseInt(t.substring(2)), KeyEvent.CHAR_UNDEFINED));
            } else if (t.startsWith("R:")) {
                k.keyReleased(new KeyEvent(c, KeyEvent.KEY_RELEASED, 0, 0, Integer.parseInt(t.substring(2)), KeyEvent.CHAR_UNDEFINED));
            } else if (t.startsWith("T:")) {
                k.keyTyped(new KeyEvent(c, KeyEvent.KEY_TYPED, 0, 0, KeyEvent.VK_UNDEFINED, (char) Integer.parseInt(t.substring(2))));
            } else if (t.equals("W")) {
                k.prepareAwaitingKeys();
                KeyRecord kr;
                while ((kr = k.popHead()) != null) {
                    if (sb.length() > 0) sb.append(',');
                    sb.append(kr.getType()).append('/').append((int) kr.getCharacter()).append('/').append(kr.getCode()).append('/').append(kr.getFlags());
                }
                sb.append(';');
            }
        }
        return sb.length() == 0 ? "~" : sb.toString();
    }

    static void keyScenario(String spec) {
        emit("KEYS", spec, runKeys(spec));
    }

    static void keys() {
        for (int vk = 0; vk <= 600; vk++) {
            keyScenario("P:" + vk + " R:" + vk + " W");
            keyScenario("P:" + vk + " P:" + vk + " W R:" + vk + " W");
        }
        for (int u = 0; u <= 300; u++) {
            keyScenario("T:" + u + " W");
        }
        for (int u : new int[] {0x20ac, 0x201a, 0x0152, 0x0153, 0x0178, 0x2122, 0xfffe, 0x7f, 0x80, 0x9f, 0xa0, 0xff, 0x100, 0x2013}) {
            keyScenario("T:" + u + " W");
        }
        String[] seqs = {
            "P:16 P:65 T:65 R:65 R:16 W",
            "P:17 P:67 T:3 R:17 R:67 W",
            "P:18 P:65 F W",
            "P:16 W P:65 W R:16 W R:65 W",
            "P:16 P:17 P:18 P:70 W F W",
            "P:65 T:97 T:98 R:65 P:66 W",
            "F W P:112 W",
            "P:112 P:113 P:114 P:115 R:113 F W",
            "P:37 P:38 P:39 P:40 W R:37 R:38 R:39 R:40 W",
            "P:10 T:10 R:10 W",
            "P:8 R:8 P:127 R:127 W"
        };
        for (String s : seqs) keyScenario(s);
    }

    static final String[] PREF_FIELDS = {"antiAliasing", "bloom", "brightness", "mapSize", "buildArea",
        "flickeringEffects", "fog", "goundBlending", "groundDecoration", "idleAnimations", "lightDetail",
        "sceneryShadows", "defaultToolkit", "particles", "removeRoofs", "maxScreenSize", "skyboxes",
        "characterShadows", "textures", "water", "fixedScreen", "customCursor", "graphics", "cpu",
        "gametipStartStagePreference", "safeMode", "soundEffectVolume", "ambientSoundVolume",
        "voiceOverVolume", "musicVolume", "soundLevel", "monoStereo"};

    static sun.misc.Unsafe unsafe() throws Exception {
        java.lang.reflect.Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return (sun.misc.Unsafe) f.get(null);
    }

    static String drainOne(NodeCollection q) {
        StringBuilder sb = new StringBuilder();
        TcpPacket p;
        int n = 0;
        while ((p = (TcpPacket) q.popHead()) != null) {
            n++;
            for (int i = 0; i < p.capacity; i++) {
                sb.append(String.format("%02x", p.buffer.buffer[i] & 255));
            }
        }
        if (n > 1) {
            throw new IllegalStateException("more than one packet queued");
        }
        return n == 0 ? "-" : sb.toString();
    }

    static void prefCase(int[] v) throws Exception {
        sun.misc.Unsafe u = unsafe();
        com.jagex.game.runetek5.preference.GamePreferences gp =
            (com.jagex.game.runetek5.preference.GamePreferences) u.allocateInstance(
                com.jagex.game.runetek5.preference.GamePreferences.class);
        java.lang.reflect.Field owner =
            com.jagex.game.runetek5.preference.Preference.class.getDeclaredField("gamePreferences");
        owner.setAccessible(true);
        String[] names = new String[PREF_FIELDS.length + 1];
        System.arraycopy(PREF_FIELDS, 0, names, 0, PREF_FIELDS.length);
        names[PREF_FIELDS.length] = "toolkit";
        for (int i = 0; i < names.length; i++) {
            java.lang.reflect.Field f = com.jagex.game.runetek5.preference.GamePreferences.class.getField(names[i]);
            com.jagex.game.runetek5.preference.Preference pr =
                (com.jagex.game.runetek5.preference.Preference) u.allocateInstance(f.getType());
            owner.set(pr, gp);
            pr.value = i < v.length ? v[i] : 0;
            f.set(gp, pr);
        }
        com.jagex.game.runetek5.client.PreferenceManager.preferences = gp;
        drainOne(Client.GAME_CONNECTION_CONTEXT.queuedPackets);
        Client.preferencesSavedOnServer = false;
        PacketEncoder.submitQueuedPackets();
        String hex = drainOne(Client.GAME_CONNECTION_CONTEXT.queuedPackets);
        StringBuilder args = new StringBuilder();
        for (int i = 0; i < v.length; i++) {
            if (i > 0) args.append(' ');
            args.append(v[i]);
        }
        emit("SEND_PREFERENCES", args.toString(), hex);
    }

    static void prefs() throws Exception {
        int n = PREF_FIELDS.length;
        int[] z = new int[n];
        prefCase(z);
        int[] seq = new int[n];
        for (int i = 0; i < n; i++) seq[i] = i;
        prefCase(seq);
        for (int fill : new int[] {1, 2, 3, 127, 128, 200, 255, 256, 300, -1, -128, -129, 65535, Integer.MAX_VALUE,
            Integer.MIN_VALUE}) {
            int[] a = new int[n];
            java.util.Arrays.fill(a, fill);
            prefCase(a);
        }
        for (int i = 0; i < n; i++) {
            int[] a = new int[n];
            a[i] = 200 + i;
            prefCase(a);
        }
        Random rnd = new Random(7271);
        for (int k = 0; k < 120; k++) {
            int[] a = new int[n];
            for (int i = 0; i < n; i++) {
                int mode = rnd.nextInt(4);
                a[i] = mode == 0 ? rnd.nextInt(4) : mode == 1 ? rnd.nextInt(256) : mode == 2 ? rnd.nextInt(2000) - 1000
                    : rnd.nextInt();
            }
            prefCase(a);
        }
    }

    static final int[][] KEY_SETS = {
        {0, 0, 0, 0}, {1, 2, 3, 4}, {-1, -2, -3, -4}, {0x12345678, 0x7fffffff, Integer.MIN_VALUE, 0x0badf00d},
        {305419896, -1412567295, 1985229328, -19088743}};

    static String keys4(int[] k) {
        return k[0] + " " + k[1] + " " + k[2] + " " + k[3];
    }

    static final String[] ACCOUNT_TEXT = {"", "a", "a@b.c", "tester@test.test", "é€@x.y", rep('q', 7),
        rep('q', 8), rep('q', 9), rep('r', 40), rep('s', 120), "中文", "café au lait"};

    static void accountCases() throws Exception {
        GameState outside = GameState.UNK_0;
        for (int ki = 0; ki < KEY_SETS.length; ki++) {
            int[] k = KEY_SETS[ki];
            AccountCreationStage.LOGIN_XTEAS = k.clone();
            for (int ei = 0; ei < ACCOUNT_TEXT.length; ei++) {
                if (ki > 1 && ei % 3 != ki % 3) continue;
                Client.gameState = GameState.IN_ACCOUNT_CREATION;
                drainOne(Client.LOBBY_CONNECTION_CONTEXT.queuedPackets);
                PacketEncoder.sendCheckEmailPacket(ACCOUNT_TEXT[ei]);
                emit("CHECK_EMAIL_VALIDITY", "1 " + keys4(k) + " " + t(ACCOUNT_TEXT[ei]),
                    drainOne(Client.LOBBY_CONNECTION_CONTEXT.queuedPackets));
                for (int pi = 0; pi < ACCOUNT_TEXT.length; pi += 1 + (ei + ki) % 3) {
                    int age = (ei * 7 + pi * 3 + ki) % 120;
                    boolean agreed = (ei + pi) % 2 == 0;
                    PacketEncoder.sendSignUpForm(ACCOUNT_TEXT[ei], ACCOUNT_TEXT[pi], age, agreed);
                    emit("SEND_SIGN_UP_FORM", "1 " + keys4(k) + " " + t(ACCOUNT_TEXT[ei]) + " " + t(ACCOUNT_TEXT[pi])
                        + " " + age + " " + (agreed ? 1 : 0), drainOne(Client.LOBBY_CONNECTION_CONTEXT.queuedPackets));
                }
            }
        }
        AccountCreationStage.LOGIN_XTEAS = KEY_SETS[1].clone();
        for (int age : new int[] {0, 12, 13, 18, 127, 128, 255, 256, -1}) {
            Client.gameState = GameState.IN_ACCOUNT_CREATION;
            PacketEncoder.sendSignUpForm("a@b.c", "w", age, true);
            emit("SEND_SIGN_UP_FORM", "1 " + keys4(KEY_SETS[1]) + " " + t("a@b.c") + " " + t("w") + " " + age + " 1",
                drainOne(Client.LOBBY_CONNECTION_CONTEXT.queuedPackets));
        }
        Client.gameState = outside;
        PacketEncoder.sendCheckEmailPacket("a@b.c");
        emit("CHECK_EMAIL_VALIDITY", "0 " + keys4(KEY_SETS[1]) + " " + t("a@b.c"),
            drainOne(Client.LOBBY_CONNECTION_CONTEXT.queuedPackets));
        PacketEncoder.sendSignUpForm("a@b.c", "w", 20, true);
        emit("SEND_SIGN_UP_FORM", "0 " + keys4(KEY_SETS[1]) + " " + t("a@b.c") + " " + t("w") + " 20 1",
            drainOne(Client.LOBBY_CONNECTION_CONTEXT.queuedPackets));
        Client.gameState = GameState.IN_ACCOUNT_CREATION;
    }

    static final class PB {
        final java.io.ByteArrayOutputStream o = new java.io.ByteArrayOutputStream();

        PB ub(int v) {
            o.write(v);
            return this;
        }

        PB i(int v) {
            o.write(v >>> 24);
            o.write(v >>> 16);
            o.write(v >>> 8);
            o.write(v);
            return this;
        }

        PB s(String s) {
            for (int k = 0; k < s.length(); k++) o.write(s.charAt(k));
            o.write(0);
            return this;
        }

        PB raw(byte[] b) {
            o.write(b, 0, b.length);
            return this;
        }

        byte[] get() {
            return o.toByteArray();
        }
    }

    static String hexBytes(byte[] b) {
        if (b.length == 0) return "~";
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x & 255));
        return sb.toString();
    }

    static byte[] collectBody() {
        Packet.Bit bit = new Packet.Bit(4000);
        com.jagex.game.runetek5.botdetection.ReflectionCheckManager.collectResult(bit);
        return java.util.Arrays.copyOf(bit.buffer, bit.currentPosition);
    }

    static String reflectionFrame(byte[] body) {
        if (body.length > 255) return null;
        return frame(ClientProt.REFLECTION_CHECK, p -> {
            p.writeByte(0);
            int pos = p.currentPosition;
            p.writeBytes(body, 0, body.length);
            p.writeIndex(p.currentPosition - pos);
        });
    }

    static void request(byte[] payload) {
        com.jagex.game.runetek5.botdetection.ReflectionCheckManager.setupPendingChecks();
        String result;
        try {
            com.jagex.game.runetek5.botdetection.ReflectionCheckManager.requestReflectionCheck(new Packet(payload));
        } catch (Throwable e) {
            emit("REFLECTION_REQUEST", hexBytes(payload), "-");
            com.jagex.game.runetek5.botdetection.ReflectionCheckManager.setupPendingChecks();
            return;
        }
        byte[] body = collectBody();
        result = reflectionFrame(body);
        if (result != null) emit("REFLECTION_REQUEST", hexBytes(payload), result);
    }

    static final String[] RCLASS = {"x.Missing", "I", "J", "void", "", "B", "Q", "Z", "S", "F", "D", "C", "x.A"};
    static final String[] RPARAM = {"I", "B", "x.A", "Q", "J", "void", "x.Missing", ""};

    static void randomOp(Random rnd, PB pb) {
        int[] ops = {0, 1, 2, 3, 4, 5, 9, 200, 255};
        int op = ops[rnd.nextInt(ops.length)];
        pb.ub(op);
        if (op <= 2) {
            pb.s(RCLASS[rnd.nextInt(RCLASS.length)]).s("f" + rnd.nextInt(3));
            if (op == 1) pb.i(rnd.nextInt());
        } else if (op <= 4) {
            pb.s(RCLASS[rnd.nextInt(RCLASS.length)]).s("m");
            int count = rnd.nextInt(4);
            pb.ub(count);
            for (int k = 0; k < count; k++) pb.s(RPARAM[rnd.nextInt(RPARAM.length)]);
            pb.s(RPARAM[rnd.nextInt(RPARAM.length)]);
            if (op == 3) {
                for (int k = 0; k < count; k++) {
                    int mode = rnd.nextInt(5);
                    int len = mode == 0 ? -1 - rnd.nextInt(5) : mode == 1 ? rnd.nextInt(3) : mode == 2 ? 40000 : 0;
                    pb.i(len);
                    for (int j = 0; j < len && j < 2 && len > 0; j++) pb.ub(rnd.nextInt(256));
                }
            }
        }
    }

    static void requests() {
        request(new byte[0]);
        request(new byte[] {0});
        request(new byte[] {0, 0, 0, 0});
        request(new PB().ub(0).i(0).get());
        request(new PB().ub(0).i(-1).get());
        request(new PB().ub(0).i(0x7a5b3c1d).get());
        request(new PB().ub(1).i(5).get());
        request(new PB().ub(1).i(5).ub(0).get());
        request(new PB().ub(1).i(5).ub(0).s("x.Missing").get());
        request(new PB().ub(1).i(5).ub(0).s("x.Missing").s("f").get());
        request(new PB().ub(1).i(5).ub(0).s("x.Missing").s("f").ub(7).get());
        for (int op = 0; op <= 2; op++) {
            for (String cls : new String[] {"x.Missing", "I", "void", "", "J", "Q"}) {
                PB pb = new PB().ub(1).i(op * 1000 + 17).ub(op).s(cls).s("field");
                if (op == 1) pb.i(99);
                request(pb.get());
                PB cut = new PB().ub(1).i(op).ub(op).s(cls);
                request(cut.get());
            }
        }
        request(new PB().ub(1).i(3).ub(1).s("x.Missing").s("f").get());
        request(new PB().ub(1).i(3).ub(1).s("x.Missing").s("f").ub(0).ub(0).ub(0).get());
        for (int op = 3; op <= 4; op++) {
            for (String cls : new String[] {"x.Missing", "I", "void", "", "x.A"}) {
                for (String ret : new String[] {"I", "void", "x.Missing", "Q"}) {
                    PB pb = new PB().ub(1).i(op).ub(op).s(cls).s("m").ub(0).s(ret);
                    request(pb.get());
                    PB one = new PB().ub(1).i(op).ub(op).s(cls).s("m").ub(1).s("I").s(ret);
                    if (op == 3) one.i(0);
                    request(one.get());
                    PB miss = new PB().ub(1).i(op).ub(op).s(cls).s("m").ub(2).s("I").s("x.Missing").s(ret);
                    if (op == 3) miss.i(0).i(0);
                    request(miss.get());
                }
            }
        }
        request(new PB().ub(1).i(8).ub(3).s("I").s("m").ub(1).s("I").s("I").i(-1).get());
        request(new PB().ub(1).i(8).ub(3).s("I").s("m").ub(1).s("I").s("I").i(-2147483648).get());
        request(new PB().ub(1).i(8).ub(3).s("I").s("m").ub(1).s("I").s("I").i(5).ub(1).ub(2).get());
        request(new PB().ub(1).i(8).ub(3).s("I").s("m").ub(1).s("I").s("I").i(60000).get());
        request(new PB().ub(1).i(8).ub(3).s("I").s("m").ub(1).s("I").s("I").i(2).ub(1).ub(2).get());
        request(new PB().ub(1).i(8).ub(3).s("I").s("m").ub(2).s("I").s("J").s("I").i(0).get());
        request(new PB().ub(1).i(8).ub(3).s("I").s("m").ub(2).s("I").s("J").s("I").i(0).i(0).get());
        request(new PB().ub(1).i(8).ub(3).s("x.Missing").s("m").ub(1).s("I").s("I").i(-1).get());
        request(new PB().ub(1).i(8).ub(4).s("I").s("m").ub(1).s("I").s("I").get());
        for (int unknown : new int[] {5, 6, 9, 100, 255}) {
            request(new PB().ub(1).i(4).ub(unknown).get());
            request(new PB().ub(2).i(4).ub(unknown).ub(unknown).get());
            request(new PB().ub(3).i(4).ub(unknown).ub(0).s("x.Missing").s("f").ub(unknown).get());
        }
        PB many = new PB().ub(200).i(77);
        for (int k = 0; k < 200; k++) many.ub(9);
        request(many.get());
        PB manyMixed = new PB().ub(60).i(78);
        for (int k = 0; k < 60; k++) {
            manyMixed.ub(k % 3).s(k % 2 == 0 ? "x.Missing" : "I").s("f");
            if (k % 3 == 1) manyMixed.i(k);
        }
        request(manyMixed.get());
        request(new PB().ub(10).i(1).ub(0).s("x.Missing").s("f").get());
        request(new PB().ub(255).i(1).get());
        Random rnd = new Random(36);
        for (int k = 0; k < 300; k++) {
            PB pb = new PB();
            int count = rnd.nextInt(7);
            pb.ub(count).i(rnd.nextInt());
            for (int j = 0; j < count; j++) randomOp(rnd, pb);
            byte[] b = pb.get();
            if (rnd.nextInt(5) == 0 && b.length > 1) b = java.util.Arrays.copyOf(b, rnd.nextInt(b.length));
            request(b);
        }
        for (int k = 0; k < 60; k++) {
            byte[] b = new byte[rnd.nextInt(40)];
            for (int j = 0; j < b.length; j++) {
                int c = rnd.nextInt(256);
                if (c == '[' || c == 'L') c = 'x';
                b[j] = (byte) c;
            }
            if (b.length > 0) b[0] = (byte) rnd.nextInt(4);
            request(b);
        }
    }

    public static final class Probe {
        public static int F = 77;
        public static int G = 5;
        private static int P = 3;
        public static final int K = 9;
        public static long L = 1L << 40;
        public static boolean Z = true;
        public static short S = -3;
        public static byte B = -7;
        public static char C = 'A';
        public int inst = 1;
        protected static volatile int V = 11;

        public static int takesInt(int a) {
            return a * 2;
        }

        public static String takesString(String s) {
            return s;
        }

        public static int add(int a, int b) {
            return a + b;
        }

        public static Object getNull() {
            return null;
        }

        public static Object getObj() {
            return new Object();
        }

        public static long big() {
            return Long.MIN_VALUE;
        }

        public static double dbl() {
            return 2.75;
        }

        public static boolean flag() {
            return true;
        }

        public static String withNul() {
            return "a\u0000b";
        }

        public static String uni() {
            return "é€中\u0001z";
        }

        public static void nothing() {
        }

        public static int boom() {
            throw new IllegalStateException();
        }

        private static int hidden() {
            return 1;
        }

        static int packaged() {
            return 4;
        }
    }

    public static final class Boom implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        int kind;

        Boom(int kind) {
            this.kind = kind;
        }

        @SuppressWarnings("unchecked")
        static <T extends Throwable> void sneaky(Throwable t) throws T {
            throw (T) t;
        }

        private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
            in.defaultReadObject();
            switch (kind) {
                case 0: throw new ClassNotFoundException("x");
                case 1: throw new java.io.InvalidClassException("x");
                case 2: throw new java.io.StreamCorruptedException("x");
                case 4: Boom.<RuntimeException>sneaky(new IllegalAccessException("x")); break;
                case 5: throw new IllegalArgumentException("x");
                case 6: Boom.<RuntimeException>sneaky(new java.lang.reflect.InvocationTargetException(null)); break;
                case 7: throw new SecurityException("x");
                case 8: throw new java.io.IOException("x");
                case 9: throw new NullPointerException("x");
                case 10: throw new RuntimeException("x");
                case 11: throw new AssertionError("x");
                case 12: Boom.<RuntimeException>sneaky(new Exception("x")); break;
                default: break;
            }
        }
    }

    static byte[] ser(Object o) throws Exception {
        java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
        java.io.ObjectOutputStream oo = new java.io.ObjectOutputStream(bo);
        oo.writeObject(o);
        oo.close();
        return bo.toByteArray();
    }

    static byte[] blockOnly() throws Exception {
        java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
        java.io.ObjectOutputStream oo = new java.io.ObjectOutputStream(bo);
        oo.writeInt(7);
        oo.close();
        return bo.toByteArray();
    }

    static byte[] missingClass() throws Exception {
        byte[] b = ser(new Boom(3));
        String name = "ClientPacketOracle$Boom";
        byte[] nb = name.getBytes(StandardCharsets.ISO_8859_1);
        for (int i = 0; i + nb.length <= b.length; i++) {
            boolean match = true;
            for (int j = 0; j < nb.length; j++) {
                if (b[i + j] != nb[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                b[i + nb.length - 1] = (byte) 'x';
                return b;
            }
        }
        throw new IllegalStateException("class name not found");
    }

    static final class Op {
        int kind;
        int exc;
        java.lang.reflect.Field field;
        java.lang.reflect.Method method;
        byte[][] args = new byte[0][];
        int input;
        String token;
    }

    static Op fieldOp(int kind, String name, int input, String token) throws Exception {
        Op op = new Op();
        op.kind = kind;
        op.input = input;
        op.token = token;
        op.field = name == null ? null : Probe.class.getDeclaredField(name);
        return op;
    }

    static Op instField(String token) throws Exception {
        Op op = fieldOp(0, "inst", 0, token);
        return op;
    }

    static Op methodOp(int kind, String name, Class<?>[] types, byte[][] args, String token) throws Exception {
        Op op = new Op();
        op.kind = kind;
        op.token = token;
        op.method = name == null ? null : Probe.class.getDeclaredMethod(name, types);
        op.args = args;
        return op;
    }

    static Op noMethod(int kind, String token) {
        Op op = new Op();
        op.kind = kind;
        op.token = token;
        return op;
    }

    static Op failed(int code) {
        Op op = new Op();
        op.exc = code;
        op.token = "x:" + code;
        return op;
    }

    static final Class<?>[] NONE = new Class<?>[0];

    static void inject(int header, Op... ops) {
        com.jagex.game.runetek5.botdetection.ReflectionCheckManager.setupPendingChecks();
        com.jagex.game.runetek5.botdetection.ReflectionCheck rc = new com.jagex.game.runetek5.botdetection.ReflectionCheck();
        int n = ops.length;
        rc.operationCount = n;
        rc.header = header;
        rc.operations = new int[n];
        rc.exceptionsEncountered = new int[n];
        rc.fieldsToCheck = new java.lang.reflect.Field[n];
        rc.fieldInput = new int[n];
        rc.methodsToCheck = new java.lang.reflect.Method[n];
        rc.argumentBuffers = new byte[n][][];
        StringBuilder args = new StringBuilder().append(header);
        for (int i = 0; i < n; i++) {
            Op o = ops[i];
            rc.operations[i] = o.kind;
            rc.exceptionsEncountered[i] = o.exc;
            rc.fieldsToCheck[i] = o.field;
            rc.fieldInput[i] = o.input;
            rc.methodsToCheck[i] = o.method;
            rc.argumentBuffers[i] = o.args;
            args.append(' ').append(o.token);
        }
        com.jagex.game.runetek5.botdetection.ReflectionCheckManager.PENDING_REFLECTION_CHECKS.addFirst(rc);
        String result = reflectionFrame(collectBody());
        if (result == null) throw new IllegalStateException("injected case too large");
        emit("REFLECTION_RESULT", args.toString(), result);
    }

    static byte[][] a(Object... values) throws Exception {
        byte[][] r = new byte[values.length][];
        for (int i = 0; i < values.length; i++) r[i] = ser(values[i]);
        return r;
    }

    static void injected() throws Exception {
        Class<?>[] I = {int.class};
        Class<?>[] S1 = {String.class};
        inject(0);
        inject(-1);
        inject(0x12345678);
        inject(5, failed(-1), failed(-2), failed(-3), failed(-4), failed(-5));
        inject(6, failed(-10), failed(-17), failed(-21));
        inject(7, fieldOp(0, "F", 0, "f:77"));
        inject(8, fieldOp(2, "F", 0, "fm:9"), fieldOp(2, "K", 0, "fm:25"), fieldOp(2, "P", 0, "fm:10"),
            fieldOp(2, "V", 0, "fm:76"), fieldOp(2, "inst", 0, "fm:1"));
        inject(9, fieldOp(1, "G", 123, "s"), fieldOp(0, "G", 0, "f:123"), fieldOp(1, "G", -9, "s"),
            fieldOp(0, "G", 0, "f:-9"), fieldOp(1, "G", Integer.MIN_VALUE, "s"),
            fieldOp(0, "G", 0, "f:" + Integer.MIN_VALUE));
        inject(10, fieldOp(0, "P", 0, "x:-14"), fieldOp(1, "K", 4, "x:-14"), fieldOp(0, "K", 0, "f:9"),
            fieldOp(0, "L", 0, "x:-15"), fieldOp(0, "Z", 0, "x:-15"), fieldOp(0, "S", 0, "f:-3"),
            fieldOp(0, "B", 0, "f:-7"), fieldOp(0, "C", 0, "f:65"), instField("x:-19"),
            fieldOp(0, null, 0, "x:-19"), fieldOp(1, null, 3, "x:-19"), fieldOp(2, null, 0, "x:-19"));
        inject(11, fieldOp(1, "L", 3, "s"), fieldOp(1, "Z", 3, "x:-15"), fieldOp(1, "S", 70000, "x:-15"),
            fieldOp(1, "inst", 3, "x:-19"), fieldOp(1, "V", 12, "x:-14"), fieldOp(0, "V", 0, "x:-14"));
        inject(12, methodOp(3, "takesInt", I, a(21), "ml:42"), methodOp(3, "takesInt", I, a(-4), "ml:-8"),
            methodOp(3, "add", new Class<?>[] {int.class, int.class}, a(3, 4), "ml:7"),
            methodOp(3, "big", NONE, a(), "ml:" + Long.MIN_VALUE), methodOp(3, "dbl", NONE, a(), "ml:2"),
            methodOp(3, "flag", NONE, a(), "mo"), methodOp(3, "getObj", NONE, a(), "mo"),
            methodOp(3, "getNull", NONE, a(), "mn"), methodOp(3, "nothing", NONE, a(), "mn"));
        inject(13, methodOp(3, "takesString", S1, a("hello"), "ms:" + t("hello")),
            methodOp(3, "takesString", S1, a(""), "ms:~"), methodOp(3, "takesString", S1, a((Object) null), "mn"),
            methodOp(3, "uni", NONE, a(), "ms:" + t("é€中\u0001z")),
            methodOp(3, "withNul", NONE, a(), "ms:" + t("a\u0000b")),
            methodOp(3, "takesString", S1, a("—‘’x"), "ms:" + t("—‘’x")));
        inject(14, methodOp(3, "boom", NONE, a(), "x:-16"), methodOp(3, "hidden", NONE, a(), "x:-14"),
            methodOp(3, "packaged", NONE, a(), "x:-14"),
            methodOp(3, "takesInt", I, a("text"), "x:-15"), methodOp(3, "takesInt", I, a((Object) null), "x:-15"),
            methodOp(3, "add", new Class<?>[] {int.class, int.class}, a(1), "x:-15"),
            methodOp(3, "takesInt", I, a(1, 2), "x:-15"),
            noMethod(3, "x:-19"), noMethod(4, "x:-19"));
        inject(15, methodOp(4, "takesInt", I, a(), "mm:9"), methodOp(4, "hidden", NONE, a(), "mm:10"),
            methodOp(4, "packaged", NONE, a(), "mm:8"), methodOp(4, "boom", NONE, a(), "mm:9"));
        byte[][] corrupt = {new byte[0]};
        byte[][] garbage = {new byte[] {1, 2, 3, 4, 5}};
        byte[][] block = {blockOnly()};
        byte[][] missing = {missingClass()};
        inject(16, methodOp(3, "takesString", S1, corrupt, "x:-18"), methodOp(3, "takesString", S1, garbage, "x:-12"),
            methodOp(3, "takesString", S1, block, "x:-13"), methodOp(3, "takesString", S1, missing, "x:-10"));
        int[] boomCodes = {-10, -11, -12, -13, -18, -15, -18, -17, -18, -19, -20, -21, -18};
        for (int kind = 0; kind <= 12; kind++) {
            if (kind == 3) continue;
            inject(100 + kind, methodOp(3, "takesString", S1, a(new Boom(kind)), "x:" + boomCodes[kind]));
        }
        inject(17, methodOp(3, "takesString", S1, a(new Boom(13)), "x:-15"));
        Random rnd = new Random(2012);
        String[] words = {"", "a", "café", "x y", "€"};
        for (int k = 0; k < 80; k++) {
            int count = rnd.nextInt(12);
            Op[] ops = new Op[count];
            for (int j = 0; j < count; j++) {
                int pick = rnd.nextInt(20);
                switch (pick) {
                    case 0: ops[j] = fieldOp(0, "F", 0, "f:77"); break;
                    case 1: ops[j] = fieldOp(2, "F", 0, "fm:9"); break;
                    case 2: ops[j] = fieldOp(0, "K", 0, "f:9"); break;
                    case 3: ops[j] = fieldOp(0, "P", 0, "x:-14"); break;
                    case 4: ops[j] = fieldOp(0, "L", 0, "x:-15"); break;
                    case 5: ops[j] = failed(-1 - rnd.nextInt(5)); break;
                    case 6: ops[j] = methodOp(3, "boom", NONE, a(), "x:-16"); break;
                    case 7: {
                        int v = rnd.nextInt() / 4;
                        ops[j] = methodOp(3, "takesInt", I, a(v), "ml:" + (long) (v * 2));
                        break;
                    }
                    case 8: {
                        String w = words[rnd.nextInt(words.length)];
                        ops[j] = methodOp(3, "takesString", S1, a(w), "ms:" + t(w));
                        break;
                    }
                    case 9: ops[j] = methodOp(3, "dbl", NONE, a(), "ml:2"); break;
                    case 10: ops[j] = methodOp(3, "getNull", NONE, a(), "mn"); break;
                    case 11: ops[j] = methodOp(3, "flag", NONE, a(), "mo"); break;
                    case 12: ops[j] = methodOp(4, "takesInt", I, a(), "mm:9"); break;
                    case 13: ops[j] = methodOp(3, "takesString", S1, garbage, "x:-12"); break;
                    case 14: ops[j] = methodOp(3, "hidden", NONE, a(), "x:-14"); break;
                    case 15: ops[j] = noMethod(3, "x:-19"); break;
                    case 16: ops[j] = fieldOp(0, "S", 0, "f:-3"); break;
                    case 17: ops[j] = methodOp(3, "big", NONE, a(), "ml:" + Long.MIN_VALUE); break;
                    case 18: ops[j] = methodOp(3, "takesString", S1, block, "x:-13"); break;
                    default: ops[j] = failed(-19 + rnd.nextInt(3)); break;
                }
            }
            inject(rnd.nextInt(), ops);
        }
    }

    static void reflection() throws Exception {
        requests();
        injected();
    }

    public static void main(String[] args) throws Exception {
        try {
            Client.GAME_CONNECTION_CONTEXT.isaac = stream;
            Client.LOBBY_CONNECTION_CONTEXT.isaac = stream;
            GameShell.gameCanvas = new Canvas();
            useTable("A");
            simple();
            strings();
            chat();
            menus();
            directs();
            mouse();
            keys();
            prefs();
            accountCases();
            reflection();
            Path dir = Path.of(args[0]);
            Files.createDirectories(dir);
            Files.write(dir.resolve("client-packets.tsv"), out.toString().getBytes(StandardCharsets.UTF_8));
            System.err.println("cases " + cases);
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }
}
