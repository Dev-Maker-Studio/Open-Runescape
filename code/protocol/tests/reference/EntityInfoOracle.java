import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.client.Client;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.defaults.EntityDefaults;
import com.jagex.game.runetek5.config.defaults.EquipmentDefaultsType;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.scene.MapRegion;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class EntityInfoOracle {
    static final int[] ORDER = {
        0x20000, 0x400000, 0x200, 0x20, 0x800000, 0x10000, 0x100000, 0x4000, 0x80000,
        0x40000, 0x8000, 0x40, 0x1, 0x10, 0x2, 0x1000, 0x2000, 0x4, 0x200000, 0x800, 0x80
    };
    static final int[] SAFE = {
        0x20000, 0x400000, 0x200, 0x20, 0x800000, 0x10000, 0x100000, 0x4000, 0x80000,
        0x40000, 0x8000, 0x40, 0x2, 0x1000, 0x2000, 0x4, 0x200000, 0x800, 0x80
    };
    static final int[] TERMINAL = {0x200, 0x100000, 0x40000, 0x1, 0x10, 0x2000, 0x200000, 0x80};
    static final int[] CUSTOMIZABLE = {0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    static sun.misc.Unsafe unsafe;

    static String hex(byte[] data) {
        StringBuilder out = new StringBuilder();
        for (byte b : data) {
            out.append(String.format("%02x", b & 0xff));
        }
        return out.toString();
    }

    static long fold(long h, long v) {
        for (int i = 0; i < 8; i++) {
            h ^= (v >>> (8 * i)) & 0xff;
            h *= 0x100000001b3L;
        }
        return h;
    }

    static final long BASIS = 0xcbf29ce484222325L;

    static class Bits {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        int cur = 0;
        int used = 0;

        void bits(int n, long v) {
            for (int i = n - 1; i >= 0; i--) {
                cur = (cur << 1) | (int) ((v >>> i) & 1);
                used++;
                if (used == 8) {
                    out.write(cur);
                    cur = 0;
                    used = 0;
                }
            }
        }

        void align() {
            while (used != 0) {
                bits(1, 0);
            }
        }
    }

    static class Mask {
        int index;
        int flags;
        byte[] bytes;
    }

    static class Scenario {
        int myIndex;
        Random rnd;
        List<Mask> masks = new ArrayList<>();
    }

    static Random rnd;
    static int cycles;

    static int rawFor(String kind, int wanted) {
        for (int raw = 0; raw < 256; raw++) {
            Packet p = new Packet(new byte[]{(byte) raw});
            int v;
            switch (kind) {
                case "ubsub": v = p.readUByteSubtract(); break;
                case "ubadd": v = p.readUByteAdd(); break;
                case "ubinv": v = p.readUByteInverse(); break;
                default: throw new IllegalStateException(kind);
            }
            if (v == wanted) {
                return raw;
            }
        }
        throw new IllegalStateException(kind + " " + wanted);
    }

    static byte[] rawShort(String kind, int wanted) {
        for (int b0 = 0; b0 < 256; b0++) {
            for (int b1 = 0; b1 < 256; b1++) {
                Packet p = new Packet(new byte[]{(byte) b0, (byte) b1});
                int v;
                switch (kind) {
                    case "addlittle": v = p.readUShortAddLittle(); break;
                    case "ushort": v = p.readUShort(); break;
                    case "little": v = p.readUShortLittle(); break;
                    default: throw new IllegalStateException(kind);
                }
                if (v == wanted) {
                    return new byte[]{(byte) b0, (byte) b1};
                }
            }
        }
        throw new IllegalStateException(kind + " " + wanted);
    }

    static void rbytes(ByteArrayOutputStream o, int n) {
        for (int i = 0; i < n; i++) {
            o.write(rnd.nextInt(256));
        }
    }

    static void usmart(ByteArrayOutputStream o, int v) {
        if (v < 128) {
            o.write(v);
        } else {
            int w = v + 32768;
            o.write(w >> 8);
            o.write(w & 0xff);
        }
    }

    static void bigsmart(ByteArrayOutputStream o, int v) {
        if (v < 32767) {
            o.write(v >> 8);
            o.write(v & 0xff);
        } else {
            int w = v | 0x80000000;
            o.write(w >>> 24);
            o.write((w >> 16) & 0xff);
            o.write((w >> 8) & 0xff);
            o.write(w & 0xff);
        }
    }

    static int randomBigSmartValue() {
        switch (rnd.nextInt(4)) {
            case 0: return 32767;
            case 1: return rnd.nextInt(32767);
            case 2: return 32768 + rnd.nextInt(100000);
            default: return rnd.nextInt(200);
        }
    }

    static void string(ByteArrayOutputStream o, int max) {
        int n = rnd.nextInt(max + 1);
        for (int i = 0; i < n; i++) {
            o.write(1 + rnd.nextInt(255));
        }
        o.write(0);
    }

    static void spot(ByteArrayOutputStream o, String kind, boolean safe, boolean shortFirst) {
        int gfx = safe ? 65535 : rnd.nextInt(65535);
        byte[] raw;
        if (kind.equals("be")) {
            raw = rawShort("ushort", gfx);
        } else if (kind.equals("addlittle")) {
            raw = rawShort("addlittle", gfx);
        } else {
            raw = rawShort("little", gfx);
        }
        o.write(raw[0]);
        o.write(raw[1]);
        rbytes(o, 4);
        rbytes(o, 1);
    }

    static byte[] block(int flag, boolean safe, int[] keysOut) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        switch (flag) {
            case 0x20000:
                rbytes(o, 8);
                break;
            case 0x400000:
            case 0x800000: {
                int n = rnd.nextInt(4);
                o.write(n);
                for (int i = 0; i < n; i++) {
                    o.write(rnd.nextInt(16));
                    o.write(0);
                    rbytes(o, 4);
                }
                break;
            }
            case 0x200:
                spot(o, "be", safe, true);
                break;
            case 0x20:
                rbytes(o, 2);
                break;
            case 0x10000:
                rbytes(o, 1);
                break;
            case 0x100000: {
                int n = safe ? 0 : 1 + rnd.nextInt(3);
                o.write(rawFor("ubsub", n));
                for (int i = 0; i < n; i++) {
                    byte[] t = new byte[2];
                    rnd.nextBytes(t);
                    if (rnd.nextBoolean()) {
                        t[0] = (byte) (t[0] | 0xc0);
                    }
                    o.write(t[0]);
                    o.write(t[1]);
                    int target = new Packet(t).readUShortAdd();
                    if ((target & 0xc000) == 49152) {
                        rbytes(o, 2);
                    }
                    rbytes(o, 2);
                }
                break;
            }
            case 0x4000:
                string(o, 8);
                break;
            case 0x80000:
                string(o, 8);
                o.write(rawFor("ubadd", 2 * rnd.nextInt(127)));
                break;
            case 0x40000:
                spot(o, "addlittle", safe, false);
                break;
            case 0x8000:
                rbytes(o, 1);
                break;
            case 0x40:
                o.write(rawFor("ubsub", 0));
                o.write(0);
                break;
            case 0x1: {
                int n = rnd.nextInt(60);
                o.write(rawFor("ubinv", n));
                rbytes(o, n);
                break;
            }
            case 0x10:
                for (int i = 0; i < 4; i++) {
                    bigsmart(o, randomBigSmartValue());
                }
                rbytes(o, 1);
                break;
            case 0x2:
                rbytes(o, 2);
                break;
            case 0x1000:
                rbytes(o, 1);
                break;
            case 0x2000: {
                int n = safe ? 0 : 1 + rnd.nextInt(3);
                o.write(rawFor("ubsub", n));
                for (int i = 0; i < n; i++) {
                    bigsmart(o, randomBigSmartValue());
                    rbytes(o, 3);
                }
                break;
            }
            case 0x4:
                rbytes(o, 1);
                break;
            case 0x200000:
                spot(o, "little", safe, false);
                break;
            case 0x800:
                rbytes(o, 10);
                break;
            case 0x80:
                spot(o, "little", safe, false);
                break;
            default:
                throw new IllegalStateException();
        }
        return o.toByteArray();
    }

    static byte[] maskBytes(int flags, boolean safe) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        int first = flags & 0xff;
        int second = (flags >> 8) & 0xff;
        int third = (flags >> 16) & 0xff;
        boolean two = second != 0 || third != 0;
        if (two) {
            first |= 0x8;
        }
        o.write(first);
        if (two) {
            if (third != 0) {
                second |= 1;
            }
            o.write(second);
            if (third != 0) {
                o.write(third);
            }
        }
        for (int flag : ORDER) {
            if ((flags & flag) != 0) {
                byte[] b = block(flag, safe, null);
                o.write(b, 0, b.length);
            }
        }
        return o.toByteArray();
    }

    static void writeSkip(Bits w, int c) {
        List<Integer> types = new ArrayList<>();
        if (c == 0) {
            types.add(0);
        }
        if (c < 32) {
            types.add(1);
        }
        if (c < 256) {
            types.add(2);
        }
        types.add(3);
        int type = types.get(rnd.nextInt(types.size()));
        w.bits(2, type);
        if (type == 1) {
            w.bits(5, c);
        } else if (type == 2) {
            w.bits(8, c);
        } else if (type == 3) {
            w.bits(11, c);
        }
    }

    static void writeMovement(Bits w, boolean allowEnterRecursion, int depth, boolean ownMask, List<int[]> enters, int index) {
        int type = rnd.nextInt(4);
        w.bits(2, type);
        if (type == 0) {
            if (allowEnterRecursion && depth == 0 && rnd.nextInt(5) == 0) {
                w.bits(1, 1);
                writeNonEnter(w);
            } else {
                w.bits(1, 0);
            }
            w.bits(6, rnd.nextInt(64));
            w.bits(6, rnd.nextInt(64));
            int further = rnd.nextInt(3) == 0 ? 1 : 0;
            w.bits(1, further);
            enters.add(new int[]{index, further});
        } else {
            writeNonEnterBody(w, type);
        }
    }

    static void writeNonEnter(Bits w) {
        int type = 1 + rnd.nextInt(3);
        w.bits(2, type);
        writeNonEnterBody(w, type);
    }

    static void writeNonEnterBody(Bits w, int type) {
        if (type == 1) {
            w.bits(2, rnd.nextInt(4));
        } else if (type == 2) {
            w.bits(5, rnd.nextInt(32));
        } else {
            w.bits(18, rnd.nextInt(1 << 18));
        }
    }

    static byte[] ownPacket(int myIndex, Random r) {
        Bits w = new Bits();
        int plane = r.nextInt(4);
        w.bits(30, ((long) plane << 28) | ((long) r.nextInt(16384) << 14) | r.nextInt(16384));
        for (int i = 1; i < 2048; i++) {
            if (i != myIndex) {
                w.bits(18, r.nextInt(1 << 18));
            }
        }
        w.align();
        return w.out.toByteArray();
    }

    static byte[] stepPacket(int my, boolean terminal, int terminalFlag, List<Mask> masksOut) {
        byte[] state = PlayerUpdate.OUT_PLAYER_UPDATE_STATE;
        List<Integer> active = new ArrayList<>();
        for (int i = 0; i < PlayerUpdate.NUM_OUT_PLAYER_INDICES; i++) {
            active.add(PlayerUpdate.OUT_PLAYER_INDICES[i]);
        }
        List<Integer> inactive = new ArrayList<>();
        for (int i = 0; i < PlayerUpdate.numInactivePlayerIndices; i++) {
            inactive.add(PlayerUpdate.INACTIVE_PLAYER_INDICES[i]);
        }
        int target = -1;
        if (terminal) {
            target = active.get(rnd.nextInt(active.size()));
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<int[]> maskAdds = new ArrayList<>();
        for (int pass = 1; pass <= 4; pass++) {
            List<Integer> source = pass <= 2 ? active : inactive;
            List<Integer> entries = new ArrayList<>();
            for (int e : source) {
                boolean bit = (state[e] & 1) != 0;
                boolean wantBit = pass == 2 || pass == 3;
                if (bit == wantBit) {
                    entries.add(e);
                }
            }
            Bits w = new Bits();
            int i = 0;
            double updateProb = pass <= 2 ? 0.5 : 0.01;
            while (i < entries.size()) {
                int e = entries.get(i);
                boolean update;
                if (terminal) {
                    update = e == target;
                } else {
                    update = rnd.nextDouble() < updateProb;
                }
                if (!update) {
                    int remaining = entries.size() - i - 1;
                    int limit = remaining;
                    if (terminal) {
                        for (int j = i + 1; j < entries.size(); j++) {
                            if (entries.get(j) == target) {
                                limit = j - i - 1;
                                break;
                            }
                        }
                    }
                    int c = 0;
                    while (c < limit && rnd.nextInt(40) != 0) {
                        c++;
                    }
                    if (rnd.nextInt(3) == 0) {
                        c = limit;
                    }
                    w.bits(1, 0);
                    writeSkip(w, c);
                    i += 1 + c;
                    continue;
                }
                w.bits(1, 1);
                if (pass <= 2) {
                    if (terminal) {
                        w.bits(1, 1);
                        w.bits(2, 0);
                        maskAdds.add(new int[]{e, 1});
                    } else {
                        int hasMask = rnd.nextInt(2);
                        int size = rnd.nextInt(4);
                        if (size == 0 && hasMask == 0 && e == my) {
                            hasMask = 1;
                        }
                        w.bits(1, hasMask);
                        w.bits(2, size);
                        if (hasMask == 1) {
                            maskAdds.add(new int[]{e, 1});
                        }
                        if (size == 0) {
                            if (hasMask == 0) {
                                if (rnd.nextBoolean()) {
                                    w.bits(1, 1);
                                    writeMovement(w, false, 1, false, maskAdds, e);
                                } else {
                                    w.bits(1, 0);
                                }
                            }
                        } else if (size == 1) {
                            w.bits(3, rnd.nextInt(8));
                        } else if (size == 2) {
                            w.bits(4, rnd.nextInt(16));
                        } else {
                            int kind = rnd.nextInt(2);
                            w.bits(1, kind);
                            if (kind == 0) {
                                w.bits(12, rnd.nextInt(1 << 12));
                            } else {
                                w.bits(30, ((long) rnd.nextInt(4) << 28) | ((long) rnd.nextInt(16384) << 14) | rnd.nextInt(16384));
                            }
                        }
                    }
                } else {
                    writeMovement(w, true, 0, false, maskAdds, e);
                }
                i++;
            }
            w.align();
            byte[] b = w.out.toByteArray();
            out.write(b, 0, b.length);
        }
        for (int[] add : maskAdds) {
            if (add[1] == 0) {
                continue;
            }
        }
        List<Integer> order = new ArrayList<>();
        for (int[] add : maskAdds) {
            if (add[1] == 1) {
                order.add(add[0]);
            }
        }
        for (int k = 0; k < order.size(); k++) {
            Mask m = new Mask();
            m.index = order.get(k);
            int flags = 0;
            boolean safe = true;
            if (terminal) {
                flags = terminalFlag;
                safe = false;
                for (int f : SAFE) {
                    boolean before = indexOf(ORDER, f) < indexOf(ORDER, terminalFlag);
                    if (before && rnd.nextInt(3) == 0 && !contains(TERMINAL, f)) {
                        flags |= f;
                    }
                }
            } else {
                for (int f : SAFE) {
                    if (rnd.nextInt(4) == 0) {
                        flags |= f;
                    }
                }
            }
            m.flags = flags;
            m.bytes = maskBytes(flags, safe);
            if (terminal) {
                m.bytes = terminalMask(flags, terminalFlag);
            }
            masksOut.add(m);
            out.write(m.bytes, 0, m.bytes.length);
        }
        return out.toByteArray();
    }

    static byte[] terminalMask(int flags, int terminalFlag) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        int first = flags & 0xff;
        int second = (flags >> 8) & 0xff;
        int third = (flags >> 16) & 0xff;
        boolean two = second != 0 || third != 0;
        if (two) {
            first |= 0x8;
        }
        o.write(first);
        if (two) {
            if (third != 0) {
                second |= 1;
            }
            o.write(second);
            if (third != 0) {
                o.write(third);
            }
        }
        for (int flag : ORDER) {
            if ((flags & flag) != 0) {
                byte[] b = block(flag, flag != terminalFlag, null);
                o.write(b, 0, b.length);
            }
        }
        return o.toByteArray();
    }

    static int indexOf(int[] a, int v) {
        for (int i = 0; i < a.length; i++) {
            if (a[i] == v) {
                return i;
            }
        }
        return -1;
    }

    static boolean contains(int[] a, int v) {
        return indexOf(a, v) >= 0;
    }

    static String dumpState() {
        long hp = BASIS;
        int count = 0;
        for (int i = 1; i < 2048; i++) {
            PlayerEntity p = PlayerUpdate.players[i];
            if (p != null) {
                count++;
                hp = fold(hp, i);
                hp = fold(hp, p.movementBaseX[0]);
                hp = fold(hp, p.movementBaseY[0]);
                hp = fold(hp, p.plane);
                hp = fold(hp, p.faceEntity);
                hp = fold(hp, p.pMapdot ? 1 : 0);
                hp = fold(hp, p.reduceAttackPriority ? 1 : 0);
            }
        }
        long hs = BASIS;
        for (int i = 0; i < 2048; i++) {
            if (PlayerUpdate.PLAYER_UPDATES[i] != null) {
                hs = fold(hs, i);
                hs = fold(hs, PlayerUpdate.PLAYER_UPDATES[i].packedCoordinates);
                hs = fold(hs, PlayerUpdate.PLAYER_UPDATES[i].faceEntity);
                hs = fold(hs, PlayerUpdate.PLAYER_UPDATES[i].pMapdot ? 1 : 0);
                hs = fold(hs, PlayerUpdate.PLAYER_UPDATES[i].reduceAttackPriority ? 1 : 0);
            }
        }
        long hst = BASIS;
        long hm = BASIS;
        for (int i = 1; i < 2048; i++) {
            hst = fold(hst, PlayerUpdate.OUT_PLAYER_UPDATE_STATE[i]);
            hm = fold(hm, PlayerUpdate.PLAYER_MOVEMENT_TYPES[i]);
        }
        long ha = BASIS;
        for (int i = 0; i < PlayerUpdate.NUM_OUT_PLAYER_INDICES; i++) {
            ha = fold(ha, PlayerUpdate.OUT_PLAYER_INDICES[i]);
        }
        long hi = BASIS;
        for (int i = 0; i < PlayerUpdate.numInactivePlayerIndices; i++) {
            hi = fold(hi, PlayerUpdate.INACTIVE_PLAYER_INDICES[i]);
        }
        return PlayerUpdate.MY_PLAYER_PLANE + " " + count + " " + Long.toHexString(hp) + " " + Long.toHexString(hs)
            + " " + Long.toHexString(hst) + " " + Long.toHexString(hm) + " " + Long.toHexString(ha) + " "
            + Long.toHexString(hi);
    }

    static String blockTokens(Mask m) {
        PlayerEntity p = PlayerUpdate.players[m.index];
        StringBuilder s = new StringBuilder();
        int f = m.flags;
        if ((f & 0x20000) != 0) {
            s.append(" T ").append(p.hue).append(' ').append(p.saturation).append(' ').append(p.lightness)
                .append(' ').append(p.opacity & 0xff).append(' ').append(p.tintStartCycle - Client.cycles)
                .append(' ').append(p.tintEndCycle - Client.cycles);
        }
        if ((f & (0x400000 | 0x800000)) != 0) {
            s.append(" C ");
            for (int k = 0; k < 16; k++) {
                var map = p.playerConfigs.varpValues;
                var node = map == null ? null : map.get(k);
                s.append(k == 0 ? "" : ",").append(node == null ? "_" : String.valueOf(node.value));
            }
        }
        if ((f & 0x10000) != 0) {
            s.append(" R ").append(p.reduceAttackPriority ? 1 : 0);
        }
        if ((f & 0x8000) != 0) {
            s.append(" D ").append(p.pMapdot ? 1 : 0);
        }
        if ((f & 0x2) != 0) {
            s.append(" F ").append(p.faceEntity);
        }
        if ((f & 0x4) != 0) {
            s.append(" Y ").append(PlayerUpdate.PLAYER_MOVEMENT_TYPES[m.index]);
        }
        if ((f & 0x800) != 0) {
            s.append(" V ").append(p.forceMovementT1XOff - p.movementBaseX[0])
                .append(' ').append(p.forceMovementT1YOff - p.movementBaseY[0])
                .append(' ').append(p.forceMovementT2XOff - p.movementBaseX[0])
                .append(' ').append(p.forceMovementT2YOff - p.movementBaseY[0])
                .append(' ').append(p.forceMovementT1Delay - Client.cycles)
                .append(' ').append(p.forceMovementT2Delay - Client.cycles)
                .append(' ').append(p.forceMoveDirection);
        }
        return s.toString();
    }

    static void staticSetup() throws Exception {
        Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        unsafe = (sun.misc.Unsafe) f.get(null);
        EntityDefaults ed = (EntityDefaults) unsafe.allocateInstance(EntityDefaults.class);
        ed.maximumHits = 4;
        Field ef = Client.class.getDeclaredField("ENTITY_DEFAULTS");
        ef.setAccessible(true);
        ef.set(null, ed);
        EquipmentDefaultsType eq = (EquipmentDefaultsType) unsafe.allocateInstance(EquipmentDefaultsType.class);
        eq.customizableObjSlots = CUSTOMIZABLE;
        TypeLists.EQUIPMENT_DEFAULTS = eq;
        Resource.mapRegion = new MapRegion(false);
        com.jagex.game.runetek5.config.bas.BasTypeList bas =
            (com.jagex.game.runetek5.config.bas.BasTypeList) unsafe.allocateInstance(com.jagex.game.runetek5.config.bas.BasTypeList.class);
        Field cf = com.jagex.game.runetek5.config.bas.BasTypeList.class.getDeclaredField("basCache");
        cf.setAccessible(true);
        com.jagex.game.runetek5.cache.node.lru.LRUCache cache = new com.jagex.game.runetek5.cache.node.lru.LRUCache(64);
        cache.put(new com.jagex.game.runetek5.config.bas.BasType(), 0L);
        cf.set(bas, cache);
        TypeLists.BAS = bas;
        Object prefs = unsafe.allocateInstance(Class.forName("com.jagex.game.runetek5.preference.GamePreferences"));
        Object tex = unsafe.allocateInstance(Class.forName("com.jagex.game.runetek5.preference.impl.TexturesPreference"));
        Field tf = prefs.getClass().getDeclaredField("textures");
        tf.setAccessible(true);
        tf.set(prefs, tex);
        Field pf = com.jagex.game.runetek5.client.PreferenceManager.class.getDeclaredField("preferences");
        pf.setAccessible(true);
        pf.set(null, prefs);
        com.jagex.game.runetek5.config.npc.NpcTypeList nl = (com.jagex.game.runetek5.config.npc.NpcTypeList)
            unsafe.allocateInstance(com.jagex.game.runetek5.config.npc.NpcTypeList.class);
        com.jagex.game.runetek5.cache.node.lru.LRUCache ncache = new com.jagex.game.runetek5.cache.node.lru.LRUCache(40000);
        Field tcf = com.jagex.game.runetek5.config.npc.NpcTypeList.class.getDeclaredField("typeCache");
        tcf.setAccessible(true);
        unsafe.putObject(nl, unsafe.objectFieldOffset(tcf), ncache);
        for (int i = 0; i < 32768; i++) {
            com.jagex.game.runetek5.config.npc.NPCType t = new com.jagex.game.runetek5.config.npc.NPCType();
            t.type = i;
            t.modelIds = new int[0];
            t.npcTypeList = nl;
            ncache.put(t, i);
        }
        TypeLists.NPC = nl;
        com.jagex.game.runetek5.config.hitsplat.HitSplatTypeList hs = (com.jagex.game.runetek5.config.hitsplat.HitSplatTypeList)
            unsafe.allocateInstance(com.jagex.game.runetek5.config.hitsplat.HitSplatTypeList.class);
        com.jagex.game.runetek5.cache.node.lru.LRUCache hcache = new com.jagex.game.runetek5.cache.node.lru.LRUCache(2048);
        Field hcf = com.jagex.game.runetek5.config.hitsplat.HitSplatTypeList.class.getDeclaredField("hitsplatTypeCache");
        hcf.setAccessible(true);
        unsafe.putObject(hs, unsafe.objectFieldOffset(hcf), hcache);
        for (int i = 0; i < 1024; i++) {
            hcache.put(new com.jagex.game.runetek5.config.hitsplat.HitSplatType(), i);
        }
        TypeLists.HITSPLAT = hs;
        com.jagex.game.runetek5.config.hitbar.HitbarTypeList hb = (com.jagex.game.runetek5.config.hitbar.HitbarTypeList)
            unsafe.allocateInstance(com.jagex.game.runetek5.config.hitbar.HitbarTypeList.class);
        com.jagex.game.runetek5.cache.node.lru.LRUCache bcache = new com.jagex.game.runetek5.cache.node.lru.LRUCache(2048);
        Field bcf = com.jagex.game.runetek5.config.hitbar.HitbarTypeList.class.getDeclaredField("hitbarCache");
        bcf.setAccessible(true);
        unsafe.putObject(hb, unsafe.objectFieldOffset(bcf), bcache);
        for (int i = 0; i < 1024; i++) {
            bcache.put(new com.jagex.game.runetek5.config.hitbar.HitbarType(), i);
        }
        TypeLists.HITBAR = hb;
    }

    static void resetPlayers(int my) {
        PlayerUpdate.reset();
        for (int i = 0; i < 2048; i++) {
            PlayerUpdate.players[i] = null;
            PlayerUpdate.OUT_PLAYER_UPDATE_STATE[i] = 0;
        }
        PlayerUpdate.numUpdateMaskPlayers = 0;
        PlayerUpdate.numInactivePlayerIndices = 0;
        PlayerUpdate.NUM_OUT_PLAYER_INDICES = 0;
        PlayerUpdate.myPlayerIndex = my;
        PlayerUpdate.MY_PLAYER_PLANE = 0;
    }

    static Packet.Bit packet(byte[] data) {
        Packet.Bit b = new Packet.Bit(data.length + 8);
        System.arraycopy(data, 0, b.buffer, 0, data.length);
        b.currentPosition = 0;
        return b;
    }

    static boolean terminalException(Throwable t, int flag) {
        String needle;
        switch (flag) {
            case 0x1: needle = "decodeAppearance"; break;
            case 0x10: case 0x2000: needle = "animate"; break;
            case 0x100000: needle = "setEquipmentOverrides"; break;
            default: needle = "playSpotAnimation"; break;
        }
        for (StackTraceElement e : t.getStackTrace()) {
            if (e.getMethodName().contains(needle)) {
                return true;
            }
        }
        return false;
    }

    static void playerScenario(PrintWriter out, int id, long seed, int steps, String mode) {
        rnd = new Random(seed);
        int my = 1 + rnd.nextInt(2047);
        resetPlayers(my);
        out.println("BEGIN " + id + " " + my);
        byte[] own = ownPacket(my, rnd);
        out.println("OWN " + hex(own));
        PlayerUpdate.decodeOwnPlayer(packet(own));
        PlayerUpdate.MY_PLAYER = new PlayerEntity(null);
        out.println("DUMP ok " + dumpState());
        byte[] last = null;
        for (int s = 0; s < steps; s++) {
            boolean terminal = mode.equals("terminal") && s == steps - 1;
            int tflag = terminal ? TERMINAL[rnd.nextInt(TERMINAL.length)] : 0;
            List<Mask> masks = new ArrayList<>();
            byte[] data = stepPacket(my, terminal, tflag, masks);
            boolean error = mode.equals("error") && s == steps - 1;
            if (error) {
                if (rnd.nextBoolean() && data.length > 0) {
                    data = java.util.Arrays.copyOf(data, data.length - 1 - rnd.nextInt(Math.min(data.length, 6)));
                } else {
                    byte[] longer = java.util.Arrays.copyOf(data, data.length + 1 + rnd.nextInt(3));
                    data = longer;
                }
            }
            out.println("STEP " + hex(data));
            Packet.Bit b = packet(data);
            try {
                PlayerUpdate.decode(b, data.length);
                if (error) {
                    out.println("DUMP ok " + dumpState());
                    break;
                }
                if (terminal && tflag != 0x100000) {
                    throw new IllegalStateException("terminal step succeeded " + Integer.toHexString(tflag));
                }
            } catch (Throwable t) {
                if (error) {
                    out.println("DUMP err");
                    break;
                }
                if (terminal && terminalException(t, tflag) && b.currentPosition == data.length) {
                    out.println("TERM " + Integer.toHexString(tflag));
                    break;
                }
                System.err.println("scenario " + id + " step " + s + " flag " + Integer.toHexString(tflag)
                    + " pos " + b.currentPosition + "/" + data.length);
                t.printStackTrace();
                throw new IllegalStateException("generation failure");
            }
            out.println("DUMP ok " + dumpState());
            for (Mask m : masks) {
                out.println("BLK " + m.index + blockTokens(m));
            }
        }
        out.println("END");
    }

    static final int[] NPC_ORDER = {
        0x10, 0x100, 0x400, 0x1000, 0x200000, 0x1000000, 0x80, 0x8000, 0x800, 0x1, 0x8, 0x2000000,
        0x80000, 0x40000, 0x10000, 0x400000, 0x4, 0x20000, 0x800000, 0x2, 0x4000, 0x20
    };
    static final int[] NPC_TERMINAL = {0x10, 0x1000000, 0x800, 0x2000000, 0x20};
    static final int[] NPC_SAFE = {
        0x100, 0x400, 0x1000, 0x200000, 0x80, 0x8000, 0x1, 0x8, 0x80000, 0x40000, 0x10000, 0x400000, 0x4,
        0x20000, 0x800000, 0x2, 0x4000
    };

    static Class<?> CLIENT = Client.class;

    static Object clientGet(String name) throws Exception {
        Field f = CLIENT.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(null);
    }

    static void clientSet(String name, int value) throws Exception {
        Field f = CLIENT.getDeclaredField(name);
        f.setAccessible(true);
        f.setInt(null, value);
    }

    static int clientInt(String name) throws Exception {
        Field f = CLIENT.getDeclaredField(name);
        f.setAccessible(true);
        return f.getInt(null);
    }

    static com.jagex.game.runetek5.cache.node.IterableNodeMap<com.jagex.game.runetek5.cache.node.impl.ObjectNode> npcMap() throws Exception {
        @SuppressWarnings("unchecked")
        var m = (com.jagex.game.runetek5.cache.node.IterableNodeMap<com.jagex.game.runetek5.cache.node.impl.ObjectNode>) clientGet("NPC_MAP");
        return m;
    }

    static com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity npcAt(int id) throws Exception {
        var node = npcMap().get(id);
        return node == null ? null : (com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity) node.value;
    }

    static void resetNpcs(int bx, int by) throws Exception {
        npcMap().clear();
        Object[] arr = (Object[]) clientGet("NPC_ARRAY");
        java.util.Arrays.fill(arr, null);
        clientSet("NPC_UPDATE_INDEX", 0);
        clientSet("npcCount", 0);
        clientSet("npcListSize", 0);
        clientSet("localNpcCount", 0);
        clientSet("npcUpdateCycle", 0);
        Client.cycles = 0;
        PlayerEntity me = new PlayerEntity(null);
        me.movementBaseX[0] = bx;
        me.movementBaseY[0] = by;
        PlayerUpdate.MY_PLAYER = me;
    }

    static void hitBlock(ByteArrayOutputStream o) {
        int n = rnd.nextInt(3);
        o.write(rawFor("ubadd", n));
        for (int i = 0; i < n; i++) {
            switch (rnd.nextInt(5)) {
                case 0:
                    usmart(o, 32767);
                    for (int k = 0; k < 4; k++) {
                        usmart(o, rnd.nextInt(300));
                    }
                    break;
                case 1:
                    usmart(o, 32766);
                    rbytes(o, 1);
                    break;
                case 2:
                    usmart(o, 32764);
                    usmart(o, rnd.nextInt(300));
                    string(o, 5);
                    usmart(o, rnd.nextInt(300));
                    string(o, 5);
                    break;
                case 3:
                    usmart(o, 32765);
                    usmart(o, rnd.nextInt(300));
                    string(o, 5);
                    break;
                default:
                    usmart(o, rnd.nextInt(300));
                    usmart(o, rnd.nextInt(300));
                    break;
            }
            usmart(o, rnd.nextInt(300));
        }
        int m = rnd.nextInt(3);
        o.write(rawFor("ubadd", m));
        for (int i = 0; i < m; i++) {
            usmart(o, rnd.nextInt(200));
            int life = rnd.nextInt(4) == 0 ? 32767 : (rnd.nextBoolean() ? 0 : 1 + rnd.nextInt(60));
            usmart(o, life);
            if (life != 32767) {
                usmart(o, rnd.nextInt(300));
                rbytes(o, 1);
                if (life > 0) {
                    rbytes(o, 1);
                }
            }
        }
    }

    static void npcBlock(int flag, ByteArrayOutputStream o) {
        switch (flag) {
            case 0x10:
                for (int i = 0; i < 4; i++) {
                    bigsmart(o, randomBigSmartValue());
                }
                rbytes(o, 1);
                break;
            case 0x100: rbytes(o, 1); break;
            case 0x400: rbytes(o, 10); break;
            case 0x1000: rbytes(o, 2); break;
            case 0x200000:
            case 0x400000: {
                int n = rnd.nextInt(4);
                o.write(n);
                for (int i = 0; i < n; i++) {
                    rbytes(o, 6);
                }
                break;
            }
            case 0x1000000: rbytes(o, 7); break;
            case 0x80: rbytes(o, 2); break;
            case 0x8000: {
                int n = rnd.nextInt(4);
                o.write(rawFor("ubinv", n));
                for (int i = 0; i < n; i++) {
                    if (rnd.nextBoolean()) {
                        o.write(0x7f);
                        o.write(0xff);
                        rbytes(o, 3);
                    } else {
                        bigsmart(o, randomBigSmartValue());
                        rbytes(o, 1);
                        o.write(0);
                        o.write(0);
                    }
                }
                break;
            }
            case 0x800: rbytes(o, 7); break;
            case 0x1: hitBlock(o); break;
            case 0x8: bigsmart(o, rnd.nextInt(8) == 0 ? 32767 : rnd.nextInt(32767)); break;
            case 0x2000000: rbytes(o, 7); break;
            case 0x80000: {
                int n = rnd.nextInt(4);
                o.write(n);
                for (int i = 0; i < n; i++) {
                    o.write(rawFor("ubadd", rnd.nextInt(6)));
                    rbytes(o, 7);
                }
                break;
            }
            case 0x40000: rbytes(o, 1); break;
            case 0x10000: rbytes(o, 2); break;
            case 0x4: rbytes(o, 4); break;
            case 0x20000: rbytes(o, 8); break;
            case 0x800000:
            case 0x2:
                string(o, 8);
                break;
            case 0x4000: {
                int n = rnd.nextInt(4);
                o.write(rawFor("ubsub", n));
                for (int i = 0; i < n; i++) {
                    int value = rnd.nextBoolean() ? 0xc000 | rnd.nextInt(0x4000) : rnd.nextInt(0xc000);
                    byte[] raw = rawShort("addlittle", value);
                    o.write(raw[0]);
                    o.write(raw[1]);
                    if ((value & 0xc000) == 0xc000) {
                        rbytes(o, 2);
                    }
                    rbytes(o, 2);
                }
                break;
            }
            case 0x20: rbytes(o, 7); break;
            default: throw new IllegalStateException("flag " + flag);
        }
    }

    static int npcFlagsFor(int[] pool, boolean empty) {
        int f = 0;
        if (!empty) {
            for (int bit : pool) {
                if (rnd.nextInt(5) == 0) {
                    f |= bit;
                }
            }
        }
        return f;
    }

    static byte[] npcMaskBytes(int blocks) {
        int f = blocks;
        if ((f & ~0xffff) != 0) {
            f |= 0x2000 | 0x40;
        }
        if ((f & ~0xffffff) != 0) {
            f |= 0x100000 | 0x2000 | 0x40;
        }
        if ((f & ~0xff) != 0) {
            f |= 0x40;
        }
        if (rnd.nextInt(8) == 0) {
            f |= 0x40;
        }
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        o.write(f & 0xff);
        if ((f & 0x40) != 0) {
            o.write((f >> 8) & 0xff);
        }
        if ((f & 0x2000) != 0) {
            o.write((f >> 16) & 0xff);
        }
        if ((f & 0x100000) != 0) {
            o.write((f >> 24) & 0xff);
        }
        for (int flag : NPC_ORDER) {
            if ((blocks & flag) != 0) {
                npcBlock(flag, o);
            }
        }
        return o.toByteArray();
    }

    static class NpcPacket {
        byte[] data;
        boolean large;
        List<int[]> masks = new ArrayList<>();
    }

    static int dirBits() {
        return rnd.nextInt(8);
    }

    static NpcPacket npcStep(List<Integer> cur, boolean terminal, int tflag) {
        NpcPacket p = new NpcPacket();
        p.large = rnd.nextBoolean();
        Bits w = new Bits();
        int size = cur.size();
        if (size > 0 && rnd.nextInt(5) == 0) {
            size = rnd.nextInt(size + 1);
        }
        w.bits(8, size);
        List<Integer> maskIds = new ArrayList<>();
        java.util.Set<Integer> taken = new java.util.HashSet<>();
        for (int i = 0; i < size; i++) {
            int id = cur.get(i);
            if (rnd.nextInt(100) < 35) {
                w.bits(1, 0);
                taken.add(id);
                continue;
            }
            w.bits(1, 1);
            int speed = rnd.nextInt(4);
            w.bits(2, speed);
            switch (speed) {
                case 0:
                    taken.add(id);
                    maskIds.add(id);
                    break;
                case 1:
                    taken.add(id);
                    w.bits(3, dirBits());
                    if (rnd.nextBoolean()) {
                        w.bits(1, 1);
                        maskIds.add(id);
                    } else {
                        w.bits(1, 0);
                    }
                    break;
                case 2:
                    taken.add(id);
                    if (rnd.nextBoolean()) {
                        w.bits(1, 1);
                        w.bits(3, dirBits());
                        w.bits(3, dirBits());
                    } else {
                        w.bits(1, 0);
                        w.bits(3, dirBits());
                    }
                    if (rnd.nextBoolean()) {
                        w.bits(1, 1);
                        maskIds.add(id);
                    } else {
                        w.bits(1, 0);
                    }
                    break;
                default:
                    break;
            }
        }
        int entries = rnd.nextInt(4);
        if (terminal && maskIds.isEmpty() && entries == 0) {
            entries = 1;
        }
        int offBits = p.large ? 8 : 5;
        for (int e = 0; e < entries; e++) {
            int id;
            do {
                int pick = rnd.nextInt(6);
                if (pick == 0 && !cur.isEmpty()) {
                    id = cur.get(rnd.nextInt(cur.size()));
                } else {
                    id = 1 + rnd.nextInt(32766);
                }
            } while (taken.contains(id));
            taken.add(id);
            w.bits(15, id);
            boolean mask = rnd.nextBoolean() || (terminal && e == entries - 1 && maskIds.isEmpty());
            w.bits(1, mask ? 1 : 0);
            if (mask) {
                maskIds.add(id);
            }
            w.bits(offBits, rnd.nextInt(1 << offBits));
            w.bits(3, rnd.nextInt(8));
            w.bits(15, rnd.nextInt(32768));
            w.bits(offBits, rnd.nextInt(1 << offBits));
            w.bits(1, rnd.nextInt(2));
            w.bits(2, rnd.nextInt(4));
        }
        ByteArrayOutputStream masks = new ByteArrayOutputStream();
        for (int i = 0; i < maskIds.size(); i++) {
            int flags;
            if (terminal && i == maskIds.size() - 1) {
                flags = tflag;
            } else {
                flags = npcFlagsFor(NPC_SAFE, rnd.nextInt(6) == 0);
            }
            p.masks.add(new int[]{maskIds.get(i), flags});
            byte[] mb = npcMaskBytes(flags);
            masks.write(mb, 0, mb.length);
        }
        int pad = (8 - w.used) % 8;
        if (pad + masks.size() * 8 >= 15) {
            w.bits(15, 32767);
        }
        w.align();
        ByteArrayOutputStream all = new ByteArrayOutputStream();
        byte[] head = w.out.toByteArray();
        all.write(head, 0, head.length);
        byte[] mb = masks.toByteArray();
        all.write(mb, 0, mb.length);
        p.data = all.toByteArray();
        return p;
    }

    static String npcDump() throws Exception {
        long hn = BASIS;
        int count = 0;
        for (int id = 0; id < 32768; id++) {
            var npc = npcAt(id);
            if (npc != null) {
                count++;
                hn = fold(hn, id);
                hn = fold(hn, npc.movementBaseX[0]);
                hn = fold(hn, npc.movementBaseY[0]);
                hn = fold(hn, npc.plane);
            }
        }
        long hi = BASIS;
        int n = clientInt("NPC_UPDATE_INDEX");
        int[] idx = (int[]) clientGet("NPC_UPDATE_INDICES");
        for (int i = 0; i < n; i++) {
            hi = fold(hi, idx[i]);
        }
        return count + " " + n + " " + Long.toHexString(hn) + " " + Long.toHexString(hi);
    }

    static String hexChars(String s) {
        if (s == null) {
            return "-";
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (i > 0) {
                b.append('.');
            }
            b.append(Integer.toHexString(s.charAt(i)));
        }
        return b.toString();
    }

    static String npcTokens(int id, int f) throws Exception {
        var npc = npcAt(id);
        StringBuilder s = new StringBuilder();
        if ((f & 0x400) != 0) {
            s.append(" V ").append(npc.forceMovementT1XOff - npc.movementBaseX[0])
                .append(' ').append(npc.forceMovementT1YOff - npc.movementBaseY[0])
                .append(' ').append(npc.forceMovementT2XOff - npc.movementBaseX[0])
                .append(' ').append(npc.forceMovementT2YOff - npc.movementBaseY[0])
                .append(' ').append(npc.forceMovementT1Delay - Client.cycles)
                .append(' ').append(npc.forceMovementT2Delay - Client.cycles)
                .append(' ').append(npc.forceMoveDirection);
        }
        if ((f & 0x1000) != 0) {
            s.append(" R ").append(npc.currRenderAnim);
        }
        if ((f & 0x80) != 0) {
            s.append(" F ").append(npc.faceEntity);
        }
        if ((f & 0x10000) != 0) {
            s.append(" L ").append(npc.modifiedCbLvl);
        }
        if ((f & 0x4) != 0) {
            s.append(" X ").append(npc.faceX).append(' ').append(npc.faceY);
        }
        if ((f & 0x20000) != 0) {
            s.append(" T ").append(npc.hue).append(' ').append(npc.saturation).append(' ').append(npc.lightness)
                .append(' ').append(npc.opacity & 0xff).append(' ').append(npc.tintStartCycle - Client.cycles)
                .append(' ').append(npc.tintEndCycle - Client.cycles);
        }
        if ((f & 0x800000) != 0) {
            s.append(" N ").append(hexChars(npc.modifiedName));
        }
        return s.toString();
    }

    static boolean npcTerminalException(Throwable t) {
        if (t instanceof IllegalArgumentException) {
            return false;
        }
        for (StackTraceElement e : t.getStackTrace()) {
            if (e.getMethodName().equals("decodeMasks")) {
                return true;
            }
        }
        return false;
    }

    static void setReceived(byte[] data) {
        var ctx = Client.GAME_CONNECTION_CONTEXT;
        ctx.receivedBuffer.buffer = java.util.Arrays.copyOf(data, data.length + 8);
        ctx.receivedBuffer.currentPosition = 0;
        ctx.currentPacketSize = data.length;
    }

    static void npcScenario(PrintWriter out, int id, long seed, int steps, String mode) throws Exception {
        rnd = new Random(seed);
        int bx = 2000 + rnd.nextInt(2000);
        int by = 2000 + rnd.nextInt(2000);
        resetNpcs(bx, by);
        out.println("BEGIN " + id + " " + bx + " " + by);
        List<Integer> cur = new ArrayList<>();
        for (int s = 0; s < steps; s++) {
            boolean terminal = mode.equals("terminal") && s == steps - 1;
            int tflag = terminal ? NPC_TERMINAL[rnd.nextInt(NPC_TERMINAL.length)] : 0;
            NpcPacket p = npcStep(cur, terminal, tflag);
            byte[] data = p.data;
            boolean error = mode.equals("error") && s == steps - 1;
            if (error) {
                if (rnd.nextBoolean() && data.length > 0) {
                    data = java.util.Arrays.copyOf(data, data.length - 1 - rnd.nextInt(Math.min(data.length, 6)));
                } else {
                    data = java.util.Arrays.copyOf(data, data.length + 1 + rnd.nextInt(3));
                }
            }
            out.println("STEP " + (p.large ? 1 : 0) + " " + hex(data));
            setReceived(data);
            try {
                com.jagex.game.runetek5.entity.pathingentity.npc.NPCUpdate.decode(p.large);
                if (error) {
                    out.println("DUMP ok " + npcDump());
                    break;
                }
                if (terminal) {
                    throw new IllegalStateException("terminal step succeeded " + Integer.toHexString(tflag));
                }
            } catch (Throwable t) {
                if (error) {
                    out.println("DUMP err");
                    break;
                }
                if (terminal && npcTerminalException(t)
                    && Client.GAME_CONNECTION_CONTEXT.receivedBuffer.currentPosition == data.length) {
                    out.println("TERM " + Integer.toHexString(tflag));
                    break;
                }
                System.err.println("npc scenario " + id + " step " + s + " flag " + Integer.toHexString(tflag));
                t.printStackTrace();
                throw new IllegalStateException("generation failure");
            }
            out.println("DUMP ok " + npcDump());
            for (int[] m : p.masks) {
                out.println("BLK " + m[0] + npcTokens(m[0], m[1]));
            }
            cur.clear();
            int n = clientInt("NPC_UPDATE_INDEX");
            int[] idx = (int[]) clientGet("NPC_UPDATE_INDICES");
            for (int i = 0; i < n; i++) {
                cur.add(idx[i]);
            }
        }
        out.println("END");
    }

    static void npcProbe() throws Exception {
        for (int flag : NPC_ORDER) {
            rnd = new Random(77 + flag);
            resetNpcs(3000, 3000);
            Bits w = new Bits();
            w.bits(8, 0);
            w.bits(15, 5);
            w.bits(1, 1);
            w.bits(8, 3);
            w.bits(3, 1);
            w.bits(15, 9);
            w.bits(8, 4);
            w.bits(1, 0);
            w.bits(2, 0);
            ByteArrayOutputStream masks = new ByteArrayOutputStream();
            byte[] mb = npcMaskBytes(flag);
            masks.write(mb, 0, mb.length);
            w.bits(15, 32767);
            w.align();
            ByteArrayOutputStream all = new ByteArrayOutputStream();
            byte[] head = w.out.toByteArray();
            all.write(head, 0, head.length);
            all.write(masks.toByteArray(), 0, masks.size());
            byte[] data = all.toByteArray();
            setReceived(data);
            try {
                com.jagex.game.runetek5.entity.pathingentity.npc.NPCUpdate.decode(true);
                System.out.println("PROBE " + Integer.toHexString(flag) + " ok");
            } catch (Throwable t) {
                StackTraceElement[] st = t.getStackTrace();
                System.out.println("PROBE " + Integer.toHexString(flag) + " " + t.getClass().getSimpleName() + " "
                    + (st.length > 0 ? st[0] : "") + " pos " + Client.GAME_CONNECTION_CONTEXT.receivedBuffer.currentPosition
                    + "/" + data.length + " " + t.getMessage());
            }
        }
    }

    public static void main(String[] args) throws Exception {
        try {
            run(args);
        } catch (Throwable t) {
            t.printStackTrace();
            System.err.flush();
            Runtime.getRuntime().halt(1);
        }
    }

    static void run(String[] args) throws Exception {
        staticSetup();
        String dir = args[0];
        if (args.length > 1 && args[1].equals("probe")) {
            npcProbe();
            System.out.flush();
            Runtime.getRuntime().halt(0);
        }
        try (PrintWriter out = new PrintWriter(dir + "/entity-player.tsv", StandardCharsets.UTF_8)) {
            int id = 0;
            for (int i = 0; i < 120; i++) {
                playerScenario(out, id++, 1000 + i, 8 + i % 7, "plain");
            }
            for (int i = 0; i < 60; i++) {
                playerScenario(out, id++, 2000 + i, 3 + i % 4, "terminal");
            }
            for (int i = 0; i < 30; i++) {
                playerScenario(out, id++, 3000 + i, 3, "error");
            }
        }
        try (PrintWriter out = new PrintWriter(dir + "/entity-npc.tsv", StandardCharsets.UTF_8)) {
            int id = 0;
            for (int i = 0; i < 120; i++) {
                npcScenario(out, id++, 5000 + i, 8 + i % 7, "plain");
            }
            for (int i = 0; i < 60; i++) {
                npcScenario(out, id++, 6000 + i, 3 + i % 4, "terminal");
            }
            for (int i = 0; i < 30; i++) {
                npcScenario(out, id++, 7000 + i, 3, "error");
            }
        }
        System.out.flush();
        Runtime.getRuntime().halt(0);
    }
}
