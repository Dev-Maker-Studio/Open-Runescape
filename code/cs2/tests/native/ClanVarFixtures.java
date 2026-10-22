import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.cache.node.IntNode;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.cache.node.impl.LongNode;
import com.jagex.game.runetek5.cache.node.impl.ObjectNode;
import com.jagex.game.runetek5.client.Client;
import com.jagex.game.runetek5.client.Game;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.clanvar.ClanVarTypeList;
import com.jagex.game.runetek5.config.clanvarsettings.ClanVarSettingsTypeList;
import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.*;
import com.jagex.game.runetek5.interaction.chat.clanchat.settings.ClanSettings;
import com.jagex.game.runetek5.js5.Js5MasterIndex;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class ClanVarFixtures {
    static final long SEED = 0xc1a2b3L;
    static final int VAR_ARCHIVE = 47;
    static final int SETTINGS_ARCHIVE = 54;
    static final int FILES = 10;
    static final String[] TEXTS = {"", "a", "Zezima", "Straße", "x y", "9", "ünï", "long text value"};
    static final int[] WIDE = {0, 1, -1, 7, 255, 256, 1000, Integer.MIN_VALUE, Integer.MAX_VALUE, 0x12345678, -559038737, 65535};
    static final long[] LONGS = {0L, 1L, -1L, Long.MIN_VALUE, Long.MAX_VALUE, 0x123456789abcdefL, 4294967296L};
    static final int[] BITS = {0, 1, 4, 7, 8, 15, 16, 30, 31, 32, 33, 255};
    static final int[] TYPE_CHARS = {1, 105, 49, 115, 108, 2, 0x85, 0x9f, 0xff, 100};
    static final int[] GAMES = {0, 1, 2, 5, 255, 32768, -1};
    static final String[] GLOBAL_OPS = {"PUSH_VARCLAN", "PUSH_VARCLANBIT", "LOAD_CLAN_VAR_LONG", "LOAD_CLAN_VAR_STRING"};
    static final String[] SETTING_OPS = {"PUSH_VARCLANSETTING", "PUSH_VARCLANSETTINGBIT", "PUSH_VARCLANSETTING_LONG", "PUSH_VARCLANSETTING_STRING"};
    static final String[] FIND = {"ACTIVECLANSETTINGS_FIND_LISTENED", "ACTIVECLANSETTINGS_FIND_AFFINED"};
    static sun.misc.Unsafe unsafe;

    static Field field(Class<?> type, String name) throws Exception {
        for (var c = type; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(name);
    }

    static void set(Object target, String name, Object value) throws Exception {
        var f = field(target.getClass(), name);
        f.setAccessible(true);
        f.set(target, value);
    }

    static void staticSet(Class<?> type, String name, Object value) throws Exception {
        var f = field(type, name);
        f.setAccessible(true);
        f.set(null, value);
    }

    static String hex(byte[] bytes) {
        var sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }

    static byte[] file(Random random, boolean settings, int index) {
        var out = new java.io.ByteArrayOutputStream();
        int kind = random.nextInt(14);
        int typeChar = TYPE_CHARS[random.nextInt(TYPE_CHARS.length)];
        int base = random.nextInt(FILES + 2);
        int start = BITS[random.nextInt(BITS.length)];
        int end = BITS[random.nextInt(BITS.length)];
        int bitsOpcode = settings ? 2 : 3;
        if (kind == 0) {
            out.write(0);
        } else if (kind == 1) {
            out.write(1);
            out.write(0);
            out.write(0);
        } else if (kind == 2) {
            out.write(bitsOpcode);
            out.write(0);
            out.write(base);
        } else if (kind == 3) {
            out.write(1);
            out.write(typeChar);
        } else if (kind == 4) {
            out.write(7);
            out.write(1);
            out.write(typeChar);
            out.write(0);
        } else {
            var parts = new ArrayList<byte[]>();
            parts.add(new byte[] {1, (byte) typeChar});
            parts.add(new byte[] {(byte) bitsOpcode, (byte) (base >> 8), (byte) base, (byte) start, (byte) end});
            if (random.nextBoolean()) Collections.reverse(parts);
            if (kind == 5) parts.remove(random.nextInt(parts.size()));
            for (var part : parts) out.write(part, 0, part.length);
            out.write(0);
        }
        return out.toByteArray();
    }

    static Js5ResourceProvider provider(byte[][] vars, byte[][] settings) throws Exception {
        var provider = (Js5ResourceProvider) unsafe.allocateInstance(Js5ResourceProvider.class);
        var index = (Js5MasterIndex) unsafe.allocateInstance(Js5MasterIndex.class);
        var counts = new int[10000];
        var archiveFiles = new Object[10000][];
        counts[VAR_ARCHIVE] = vars.length;
        counts[SETTINGS_ARCHIVE] = settings.length;
        archiveFiles[VAR_ARCHIVE] = new Object[vars.length];
        archiveFiles[SETTINGS_ARCHIVE] = new Object[settings.length];
        for (int i = 0; i < vars.length; i++) archiveFiles[VAR_ARCHIVE][i] = vars[i];
        for (int i = 0; i < settings.length; i++) archiveFiles[SETTINGS_ARCHIVE][i] = settings[i];
        index.fileCounts = counts;
        provider.masterIndex = index;
        provider.archiveFiles = archiveFiles;
        return provider;
    }

    static Object value(Random random, int kindPick) {
        return switch (kindPick) {
            case 0 -> WIDE[random.nextInt(WIDE.length)];
            case 1 -> LONGS[random.nextInt(LONGS.length)];
            default -> TEXTS[random.nextInt(TEXTS.length)];
        };
    }

    static String kind(Object value) {
        return value instanceof Integer ? "i" : value instanceof Long ? "l" : "s";
    }

    static String show(Object value) {
        return kind(value) + value;
    }

    static ClanSettings settings(StringBuilder out, int state, int slot, int game, Random random) throws Exception {
        var settings = (ClanSettings) unsafe.allocateInstance(ClanSettings.class);
        boolean nullMap = random.nextInt(6) == 0;
        out.append("S\t").append(state).append('\t').append(slot).append('\t').append(nullMap ? 1 : 0).append('\n');
        if (nullMap) {
            set(settings, "variables", null);
            return settings;
        }
        var map = new IterableNodeMap<Node<?>>(4);
        var seen = new HashSet<Integer>();
        for (int i = 0; i < 14; i++) {
            int id = random.nextInt(FILES + 2);
            int owner = random.nextInt(8) == 0 ? game + 1 : game;
            int key = (owner << 16) | id;
            if (!seen.add(key)) continue;
            Object value = value(random, random.nextInt(3));
            Node<?> node = value instanceof Integer ? new IntNode((Integer) value) : value instanceof Long ? new LongNode((Long) value) : new ObjectNode(value);
            map.put(node, (long) key);
            out.append("w\t").append(state).append('\t').append(slot).append('\t').append(key).append('\t').append(show(value)).append('\n');
        }
        set(settings, "variables", map);
        return settings;
    }

    static ClanSettings fresh(ClanSettings source) throws Exception {
        var copy = (ClanSettings) unsafe.allocateInstance(ClanSettings.class);
        set(copy, "variables", field(ClanSettings.class, "variables").get(source));
        return copy;
    }

    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        var theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        unsafe = (sun.misc.Unsafe) theUnsafe.get(null);
        var out = new StringBuilder();
        var random = new Random(SEED);
        var varFiles = new byte[FILES][];
        var settingFiles = new byte[FILES][];
        for (int i = 0; i < FILES; i++) {
            varFiles[i] = file(random, false, i);
            settingFiles[i] = file(random, true, i);
            out.append("D\tv\t").append(i).append('\t').append(hex(varFiles[i])).append('\n');
            out.append("D\ts\t").append(i).append('\t').append(hex(settingFiles[i])).append('\n');
        }
        var provider = provider(varFiles, settingFiles);
        TypeLists.CLAN_VAR = new ClanVarTypeList(provider);
        TypeLists.CLAN_VAR_SETTINGS = new ClanVarSettingsTypeList(provider);
        int stateCount = 28;
        var applies = new ArrayList<Runnable>();
        for (int s = 0; s < stateCount; s++) {
            int game = GAMES[s % GAMES.length];
            boolean nullVars = s % 9 == 8;
            int length = nullVars ? -1 : random.nextInt(FILES + 3);
            var vars = nullVars ? null : new Object[length];
            out.append("G\t").append(s).append('\t').append(game).append('\t').append(length).append('\n');
            for (int i = 0; i < length; i++) {
                if (random.nextInt(4) == 0) continue;
                vars[i] = value(random, random.nextInt(5) < 2 ? random.nextInt(3) : 0);
                out.append("v\t").append(s).append('\t').append(i).append('\t').append(show(vars[i])).append('\n');
            }
            var pool = new ClanSettings[2];
            for (int i = 0; i < 2; i++) pool[i] = settings(out, s, i, game, random);
            int main = s % 7 == 6 ? -1 : random.nextInt(2);
            int guest = s % 5 == 4 ? -1 : random.nextInt(2);
            out.append("H\t").append(s).append('\t').append(main).append('\t').append(guest).append('\n');
            applies.add(() -> {
                try {
                    staticSet(Client.class, "CURRENT_GAME", new Game("g", "G", game));
                    staticSet(ClanVarTypeList.class, "CLAN_VARS", vars == null ? null : vars.clone());
                    staticSet(Client.class, "CLAN_SETTINGS", main < 0 ? null : fresh(pool[main]));
                    staticSet(Client.class, "GUEST_CLAN_SETTINGS", guest < 0 ? null : fresh(pool[guest]));
                } catch (Exception error) {
                    throw new RuntimeException(error);
                }
            });
        }
        var cases = new ArrayList<Object[]>();
        String[][] preludes = {{}, {"SA"}, {"SL"}, {"SL", "SA"}};
        for (int s = 0; s < stateCount; s++) {
            for (var op : GLOBAL_OPS) {
                for (int id = -1; id <= FILES + 3; id++) cases.add(new Object[] {op, s, new String[0], id});
                cases.add(new Object[] {op, s, new String[0], Integer.MAX_VALUE});
                cases.add(new Object[] {op, s, new String[0], Integer.MIN_VALUE});
            }
            for (var op : SETTING_OPS) {
                for (var prelude : preludes) {
                    for (int id = -1; id <= FILES + 1; id++) cases.add(new Object[] {op, s, prelude, id});
                    cases.add(new Object[] {op, s, prelude, 65536});
                }
            }
        }
        Collections.shuffle(cases, random);
        int total = 0;
        var stats = new TreeMap<String, int[]>();
        staticSet(Client.class, "gameState", com.jagex.game.runetek5.client.GameState.UNK_4);
        for (var item : cases) {
            String op = (String) item[0];
            int state = (Integer) item[1];
            String[] prelude = (String[]) item[2];
            int id = (Integer) item[3];
            applies.get(state).run();
            var e = new CS2Executor();
            e.intOpValues = new int[] {0};
            e.instrPtr = 0;
            Arrays.fill(e.intStack, 0x5a5a5a5a);
            e.intStackPtr = 0;
            e.stringStackPtr = 0;
            e.longStackPtr = 0;
            String result = "ok";
            try {
                for (var p : prelude) {
                    e.intOpValues = new int[] {0};
                    ClanStockOps.INSTANCE.dispatch(ClientScriptOpCode.valueOf(p.equals("SA") ? FIND[1] : FIND[0]), e);
                }
                e.intOpValues = new int[] {id};
                CoreVmOps.INSTANCE.dispatch(ClientScriptOpCode.valueOf(op), e);
            } catch (Throwable error) {
                result = "error:" + error.getClass().getSimpleName();
            }
            var ints = new StringBuilder();
            for (int i = 0; i < e.intStackPtr; i++) ints.append(i > 0 ? "," : "").append(e.intStack[i]);
            var strings = new StringBuilder();
            for (int i = 0; i < e.stringStackPtr; i++) strings.append(i > 0 ? "|" : "").append(e.stringStack[i] == null ? "~" : "\"" + e.stringStack[i] + "\"");
            var longs = new StringBuilder();
            for (int i = 0; i < e.longStackPtr; i++) longs.append(i > 0 ? "," : "").append(e.longStack[i]);
            out.append("T\t").append(op).append('\t').append(state).append('\t').append(prelude.length == 0 ? "-" : String.join(",", prelude)).append('\t').append(id)
                .append('\t').append(result).append('\t').append(ints.length() == 0 ? "-" : ints).append('\t').append(strings.length() == 0 ? "-" : strings)
                .append('\t').append(longs.length() == 0 ? "-" : longs).append('\n');
            stats.computeIfAbsent(op, k -> new int[2])[result.equals("ok") ? 0 : 1]++;
            total++;
        }
        out.append("Z\t").append(total).append('\n');
        Files.writeString(output.resolve("clan-var.native.tsv"), out.toString(), StandardCharsets.UTF_8);
        var summary = new StringBuilder();
        for (var entry : stats.entrySet()) summary.append(entry.getKey()).append('\t').append(entry.getValue()[0]).append('\t').append(entry.getValue()[1]).append('\n');
        Files.writeString(output.resolve("clan-var.stats.tsv"), summary.toString(), StandardCharsets.UTF_8);
        System.exit(0);
    }
}
