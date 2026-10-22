import com.jagex.game.runetek5.client.Client;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.*;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.interaction.chat.ChatBox;
import com.jagex.game.runetek5.movement.CoordGrid;
import com.jagex.game.runetek5.movement.CoordinateSpace;
import com.jagex.game.runetek5.scene.MapRegion;
import com.jagex.game.runetek5.tools.Language;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class MessageFixtures {
    static final long SEED = 0x3e55a9e1L;
    static final String[] LANGS = {"null", "EN", "DE", "FR", "PT", "NL", "ES"};
    static final String[] WORDS = {
        "", "a", "A", "b", "B", "abc", "ABC", "Abc", "abd", "ab", "abcd", "z", "Z", "0", "9", "10", "1", " ", "  ", "a b", "A B",
        "café", "CAFÉ", "cafe", "CAFE", "é", "É", "e", "E", "è", "ê", "ë", "ñ", "Ñ", "n", "N", "niño",
        "nino", "straße", "STRASSE", "strasse", "ß", "ss", "s", "æ", "Æ", "ae", "AE", "Ae", "œ", "Œ", "oe", "OE",
        "Ÿ", "ÿ", "y", "Y", "ý", "Ý", "àáâãäå", "ÀÁÂ", "Ç", "ç", "c", "C",
        "ìíîï", "Ì", "i", "I", "ı", "İ", "òóôõö", "Ò", "o", "O", "ùúûü",
        "Ù", "u", "U", "×", "÷", "Ø", "ø", "Þ", "þ", "Ð", "ð", "Ǆ", "ǅ", "ǆ", "ᾈ",
        "Α", "α", "А", "а", "中", "Ａ", "zebra", "Zebra", "apple", "Apple", "APPLE", "Zezima", "zezima", "Mod Mark",
        "the quick brown fox", "The Quick Brown Fox", "x".repeat(60), "x".repeat(61), "X".repeat(60), "aaab", "aaaa", "aæ", "ae", "æa", "ea",
        "ææ", "eeee", "ßa", "sa", "ssa", "aßb", "asb", "assb"
    };
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

    static Language language(String name) {
        return switch (name) {
            case "EN" -> Language.ENGLISH;
            case "DE" -> Language.GERMAN;
            case "FR" -> Language.FRENCH;
            case "PT" -> Language.PORTUGESE;
            case "NL" -> Language.DUTCH;
            case "ES" -> Language.SPANISH;
            default -> null;
        };
    }

    static String hex(String value) {
        if (value == null) return "~";
        var sb = new StringBuilder("=");
        for (int i = 0; i < value.length(); i++) sb.append(String.format("%04x", (int) value.charAt(i)));
        return sb.toString();
    }

    static String ints(int[] values, int pointer) {
        if (pointer <= 0) return "-";
        var sb = new StringBuilder();
        for (int i = 0; i < pointer; i++) {
            if (i > 0) sb.append(',');
            sb.append(values[i]);
        }
        return sb.toString();
    }

    static String run(String op, CS2Executor e, String[] strings, int[] ints) {
        e.intOpValues = new int[] {0};
        e.instrPtr = 0;
        Arrays.fill(e.intStack, 0x5a5a5a5a);
        e.intStackPtr = 0;
        e.stringStackPtr = 0;
        try {
            for (var s : strings) e.stringStack[e.stringStackPtr++] = s;
            for (var v : ints) e.intStack[e.intStackPtr++] = v;
            switch (op) {
                case "COMPARE" -> MathStringOps.INSTANCE.dispatch(ClientScriptOpCode.COMPARE, e);
                case "MES" -> InterfaceQueryOps.INSTANCE.dispatch(ClientScriptOpCode.MES, e);
                case "MES_TYPED" -> DialogOps.INSTANCE.dispatch(ClientScriptOpCode.MES_TYPED, e);
                default -> PlayerWorldOps.INSTANCE.dispatch(ClientScriptOpCode.COORD, e);
            }
            return "ok";
        } catch (Throwable error) {
            return "error:" + error.getClass().getSimpleName();
        }
    }

    static void line(StringBuilder out, String op, String lang, String args, String result, CS2Executor e, String message) {
        out.append("T\t").append(op).append('\t').append(lang).append('\t').append(args).append('\t').append(result).append('\t')
            .append(ints(e.intStack, e.intStackPtr)).append('\t').append(message).append('\n');
    }

    static String compareArgs(String a, String b) {
        return hex(a) + "|" + hex(b);
    }

    static int total;
    static final TreeMap<String, int[]> stats = new TreeMap<>();

    static void count(String op, String result) {
        stats.computeIfAbsent(op, k -> new int[2])[result.equals("ok") ? 0 : 1]++;
        total++;
    }

    static String randomWord(Random random) {
        var sb = new StringBuilder();
        int parts = random.nextInt(3) + 1;
        for (int i = 0; i < parts; i++) sb.append(WORDS[random.nextInt(WORDS.length)]);
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        var theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        unsafe = (sun.misc.Unsafe) theUnsafe.get(null);
        var out = new StringBuilder();
        var random = new Random(SEED);
        for (var lang : LANGS) {
            staticSet(Client.class, "CURRENT_LANGUAGE", language(lang));
            for (var a : WORDS) {
                for (var b : WORDS) {
                    if (random.nextInt(lang.equals("EN") ? 3 : lang.equals("null") ? 60 : 8) != 0) continue;
                    var e = new CS2Executor();
                    var result = run("COMPARE", e, new String[] {a, b}, new int[0]);
                    line(out, "COMPARE", lang, compareArgs(a, b), result, e, "-");
                    count("COMPARE", result);
                }
            }
            for (int i = 0; i < (lang.equals("null") ? 5 : 300); i++) {
                var a = randomWord(random);
                var b = random.nextInt(4) == 0 ? a.toUpperCase() : random.nextInt(4) == 0 ? a + randomWord(random) : randomWord(random);
                var e = new CS2Executor();
                var result = run("COMPARE", e, new String[] {a, b}, new int[0]);
                line(out, "COMPARE", lang, compareArgs(a, b), result, e, "-");
                count("COMPARE", result);
            }
            for (var pair : new String[][] {{null, "a"}, {"a", null}, {null, null}}) {
                var e = new CS2Executor();
                var result = run("COMPARE", e, pair, new int[0]);
                line(out, "COMPARE", lang, compareArgs(pair[0], pair[1]), result, e, "-");
                count("COMPARE", result);
            }
        }
        staticSet(Client.class, "CURRENT_LANGUAGE", Language.ENGLISH);
        for (int unit = 0; unit < 0x10000; unit++) {
            if (unit >= 0x2200 && unit < 0xff00) continue;
            if (unit >= 0xd800 && unit < 0xe000) continue;
            for (var lang : new String[] {"EN", "FR", "ES"}) {
                if (!lang.equals("EN") && unit > 0x300) continue;
                staticSet(Client.class, "CURRENT_LANGUAGE", language(lang));
                for (var other : new String[] {"a", "A", "é", "n"}) {
                    if (!lang.equals("EN") && !other.equals("a")) continue;
                    if (lang.equals("EN") && unit > 0x300 && !other.equals("a")) continue;
                    var e = new CS2Executor();
                    var text = String.valueOf((char) unit);
                    var result = run("COMPARE", e, new String[] {text, other}, new int[0]);
                    line(out, "COMPARE", lang, compareArgs(text, other), result, e, "-");
                    count("COMPARE", result);
                }
            }
        }
        staticSet(Client.class, "CURRENT_LANGUAGE", Language.ENGLISH);
        var bodies = new ArrayList<String>(Arrays.asList(WORDS));
        bodies.add("line\nbreak");
        bodies.add("<col=ff0000>colour</col>");
        bodies.add("x".repeat(500));
        bodies.add(null);
        for (var body : bodies) {
            int before = ChatBox.NUM_CHAT_LINES;
            var e = new CS2Executor();
            var result = run("MES", e, new String[] {body}, new int[0]);
            int added = ChatBox.NUM_CHAT_LINES - before;
            var message = added == 0 ? "-" : added + ":" + ChatBox.CHAT_LINES[0].type.getValue() + ":" + ChatBox.CHAT_LINES[0].effectFlags + ":" + hex(ChatBox.CHAT_LINES[0].message);
            line(out, "MES", "EN", hex(body), result, e, message);
            count("MES", result);
        }
        int[] types = new int[130 + 6];
        for (int i = 0; i < 130; i++) types[i] = i;
        types[130] = -1;
        types[131] = -100;
        types[132] = 200;
        types[133] = 65536;
        types[134] = Integer.MAX_VALUE;
        types[135] = Integer.MIN_VALUE;
        for (int type : types) {
            if (type == 98 || type == 99) continue;
            for (int k = 0; k < 4; k++) {
                int flags = k == 0 ? 0 : k == 1 ? 1 : k == 2 ? random.nextInt() : random.nextInt(256);
                var body = bodies.get(random.nextInt(bodies.size()));
                int before = ChatBox.NUM_CHAT_LINES;
                var e = new CS2Executor();
                var result = run("MES_TYPED", e, new String[] {body}, new int[] {type, flags});
                int added = ChatBox.NUM_CHAT_LINES - before;
                var message = added == 0 ? "-" : added + ":" + ChatBox.CHAT_LINES[0].type.getValue() + ":" + ChatBox.CHAT_LINES[0].effectFlags + ":" + hex(ChatBox.CHAT_LINES[0].message);
                line(out, "MES_TYPED", "EN", hex(body) + "|" + type + "|" + flags, result, e, message);
                count("MES_TYPED", result);
            }
        }
        int[][] planes = {{0}, {1}, {2}, {3}, {-1}, {127}, {-128}};
        for (int c = 0; c < 400; c++) {
            int mode = c % 10;
            String coordArgs;
            if (mode == 0) {
                PlayerUpdate.MY_PLAYER = null;
                coordArgs = "M";
            } else if (mode == 1) {
                var region = (MapRegion) unsafe.allocateInstance(MapRegion.class);
                region.baseCoordinates = new CoordGrid();
                Resource.mapRegion = null;
                var me = (PlayerEntity) unsafe.allocateInstance(PlayerEntity.class);
                set(me, "basePosition", new CoordinateSpace());
                set(me, "_absolutePosition", new CoordinateSpace());
                me.calculateAbsolutePosition = true;
                PlayerUpdate.MY_PLAYER = me;
                coordArgs = "R";
            } else {
                int plane = planes[random.nextInt(planes.length)][0];
                int fx = random.nextInt(20) == 0 ? random.nextInt(400000) - 200000 : random.nextInt(104 * 512);
                int fz = random.nextInt(20) == 0 ? random.nextInt(400000) - 200000 : random.nextInt(104 * 512);
                int bx = random.nextInt(20) == 0 ? random.nextInt(30000) : random.nextInt(100) * 8 + 6400;
                int by = random.nextInt(20) == 0 ? random.nextInt(30000) : random.nextInt(100) * 8 + 6400;
                var region = (MapRegion) unsafe.allocateInstance(MapRegion.class);
                var grid = new CoordGrid();
                grid.x = bx;
                grid.y = by;
                region.baseCoordinates = grid;
                Resource.mapRegion = region;
                var me = (PlayerEntity) unsafe.allocateInstance(PlayerEntity.class);
                var base = new CoordinateSpace();
                base.coords.x = fx + (fx >= 0 ? 0.5f : -0.5f);
                base.coords.z = fz + (fz >= 0 ? 0.5f : -0.5f);
                set(me, "basePosition", base);
                set(me, "_absolutePosition", new CoordinateSpace());
                me.calculateAbsolutePosition = true;
                me.plane = (byte) plane;
                PlayerUpdate.MY_PLAYER = me;
                coordArgs = "P:" + (int) (byte) plane + ":" + fx + ":" + fz + ":" + bx + ":" + by;
            }
            var e = new CS2Executor();
            var result = run("COORD", e, new String[0], new int[0]);
            line(out, "COORD", "EN", coordArgs, result, e, "-");
            count("COORD", result);
        }
        out.append("Z\t").append(total).append('\n');
        Files.writeString(output.resolve("message.native.tsv"), out.toString(), StandardCharsets.UTF_8);
        var summary = new StringBuilder();
        for (var entry : stats.entrySet()) summary.append(entry.getKey()).append('\t').append(entry.getValue()[0]).append('\t').append(entry.getValue()[1]).append('\n');
        Files.writeString(output.resolve("message.stats.tsv"), summary.toString(), StandardCharsets.UTF_8);
        System.exit(0);
    }
}
