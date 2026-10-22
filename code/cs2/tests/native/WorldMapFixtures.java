import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.*;
import com.jagex.game.runetek5.graphics.map.minimap.ComponentMinimap;
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMap;
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapCategory;
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapDisplay;
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapElement;
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapPlacement;
import com.jagex.game.runetek5.js5.Js5MasterIndex;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class WorldMapFixtures {
    static final long SEED = 0x727c5228L;
    static final int SYNTHETIC_ELEMENTS = 260;
    static final int SYNTHETIC_SETS = 14;
    static final int REAL_CASES_PER_OP = 220;
    static final int SET_CASES_PER_OP = 90;
    static final int[] IDS = {0, 1, 2, 3, 15, 16, 17, 18, 31, 33, 49, 65, 65536, 65537, 100000, 12345, 255, 4096};
    static final int[] LOADING = {0, 50, 99, 100, 101, -1, 255, 100, 100, 100};
    static final int[] BIG = {0, 1, -1, 16383, 16384, 0x7fffffff, Integer.MIN_VALUE, 46341, -46341, 65535, 1 << 28, 12800};
    static final String[] OPS = {
        "WORLDMAP_GETMAP", "WORLDMAP_GETMAPNAME", "WORLDMAP_GETCONFIGORIGIN", "WORLDMAP_GETCONFIGSIZE", "WORLDMAP_GETCONFIGZOOM",
        "WORLDMAP_COORDINMAP", "WORLDMAP_GETSOURCECOORD", "WORLDMAP_GETDISPLAYCOORD", "WORLDMAP_GETSOURCEPOSITION", "WORLDMAP_FINDNEARESTELEMENT"
    };
    static sun.misc.Unsafe unsafe;
    static final Map<String, Field> FIELDS = new HashMap<>();

    static final class Source {
        final int id;
        final byte[] bytes;
        final String kind;

        Source(int id, byte[] bytes, String kind) {
            this.id = id;
            this.bytes = bytes;
            this.kind = kind;
        }
    }

    static final List<Source> SOURCES = new ArrayList<>();
    static final List<Integer> VALID = new ArrayList<>();

    static Field field(Class<?> type, String name) throws Exception {
        var key = type.getName() + "#" + name;
        var cached = FIELDS.get(key);
        if (cached != null) return cached;
        Class.forName(type.getName(), true, type.getClassLoader());
        for (var c = type; c != null; c = c.getSuperclass()) {
            try {
                var f = c.getDeclaredField(name);
                FIELDS.put(key, f);
                return f;
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(key);
    }

    static void setStaticInt(Class<?> t, String n, int v) throws Exception {
        var f = field(t, n);
        unsafe.putInt(unsafe.staticFieldBase(f), unsafe.staticFieldOffset(f), v);
    }

    static void setStaticObj(Class<?> t, String n, Object v) throws Exception {
        var f = field(t, n);
        unsafe.putObject(unsafe.staticFieldBase(f), unsafe.staticFieldOffset(f), v);
    }

    static void putInt(Object target, String name, int value) throws Exception {
        unsafe.putInt(target, unsafe.objectFieldOffset(field(target.getClass(), name)), value);
    }

    static void putObj(Object target, String name, Object value) throws Exception {
        unsafe.putObject(target, unsafe.objectFieldOffset(field(target.getClass(), name)), value);
    }

    static String hex(byte[] bytes) {
        var text = new StringBuilder();
        for (byte b : bytes) text.append(String.format("%02x", b & 0xff));
        return bytes.length == 0 ? "-" : text.toString();
    }

    static byte[] unhex(String text) {
        if (text.equals("-")) return new byte[0];
        var bytes = new byte[text.length() / 2];
        for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) Integer.parseInt(text.substring(i * 2, i * 2 + 2), 16);
        return bytes;
    }

    static String units(String text) {
        if (text.isEmpty()) return "-";
        var out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (i > 0) out.append('.');
            out.append(Integer.toHexString(text.charAt(i)));
        }
        return out.toString();
    }

    static WorldMapElement decode(int id, byte[] bytes) throws Exception {
        var provider = (Js5ResourceProvider) unsafe.allocateInstance(Js5ResourceProvider.class);
        var master = (Js5MasterIndex) unsafe.allocateInstance(Js5MasterIndex.class);
        master.fileCounts = new int[] {id + 1};
        provider.masterIndex = master;
        provider.archives = new Object[1];
        var files = new Object[1][];
        files[0] = new Object[id + 1];
        files[0][id] = bytes;
        provider.archiveFiles = files;
        provider.loadType = 0;
        return WorldMapElement.list(provider, 0, id);
    }

    static String digest(WorldMapElement el) {
        var out = new StringBuilder();
        out.append(el.elementId).append('|').append(units(el.filenamePrefix)).append('|').append(units(el.displayName)).append('|')
            .append(el.bitpackedPlacement).append('|').append(el.color).append('|').append(el.shouldRender ? 1 : 0).append('|')
            .append(el.defaultZoomLevel).append('|').append(el.boundsMinX).append(',').append(el.boundsMaxX).append(',')
            .append(el.boundsMinY).append(',').append(el.boundsMaxY).append('|');
        boolean first = true;
        Node<?> node = el.worldMapPlacements.getFirst();
        while (node != null) {
            var p = (WorldMapPlacement) node;
            if (!first) out.append(';');
            first = false;
            out.append(p.plane).append(',').append(p.startX).append(',').append(p.startY).append(',').append(p.endX).append(',').append(p.endY)
                .append(',').append(p.mapMinX).append(',').append(p.mapMinY).append(',').append(p.mapMaxX).append(',').append(p.mapMaxY);
            node = el.worldMapPlacements.next();
        }
        return out.toString();
    }

    static void putString(ByteArrayBuilder out, Random random, boolean terminate) {
        int length = random.nextInt(5) == 0 ? 0 : random.nextInt(14);
        for (int i = 0; i < length; i++) {
            int roll = random.nextInt(10);
            int b = roll < 5 ? 97 + random.nextInt(26) : roll < 7 ? 128 + random.nextInt(32) : roll < 8 ? 160 + random.nextInt(96) : roll < 9 ? 1 + random.nextInt(31) : 32 + random.nextInt(95);
            out.put(b);
        }
        if (terminate) out.put(0);
    }

    static final class ByteArrayBuilder {
        byte[] data = new byte[64];
        int length;

        void put(int value) {
            if (length == data.length) data = Arrays.copyOf(data, data.length * 2);
            data[length++] = (byte) value;
        }

        void putShort(int value) {
            put(value >> 8);
            put(value);
        }

        void putInt(int value) {
            putShort(value >>> 16);
            putShort(value);
        }

        byte[] bytes() {
            return Arrays.copyOf(data, length);
        }
    }

    static byte[] randomValid(Random random) {
        var out = new ByteArrayBuilder();
        putString(out, random, true);
        putString(out, random, true);
        out.putInt(random.nextInt(3) == 0 ? random.nextInt() : (((2400 + random.nextInt(900)) << 14) | (2800 + random.nextInt(600))));
        out.putInt(random.nextInt(3) == 0 ? random.nextInt() : random.nextInt(0xffffff));
        int[] renders = {1, 1, 1, 1, 0, 2, 255};
        out.put(renders[random.nextInt(renders.length)]);
        int[] zooms = {0, 1, 2, 3, 4, 5, 6, 7, 8, 100, 255, 128, 200};
        out.put(zooms[random.nextInt(zooms.length)]);
        out.put(random.nextInt(256));
        int[] counts = {0, 1, 1, 1, 2, 2, 3, 4, 6, 20, 255};
        int count = counts[random.nextInt(counts.length)];
        out.put(count);
        int baseX = 3000 + random.nextInt(40);
        int baseY = 3000 + random.nextInt(40);
        for (int i = 0; i < count; i++) {
            out.put(random.nextInt(8) == 0 ? random.nextInt(256) : random.nextInt(4));
            int startX = random.nextInt(6) == 0 ? random.nextInt(65536) : baseX + random.nextInt(40);
            int startY = random.nextInt(6) == 0 ? random.nextInt(65536) : baseY + random.nextInt(40);
            int endX = random.nextInt(10) == 0 ? startX - random.nextInt(5) : startX + random.nextInt(30);
            int endY = random.nextInt(10) == 0 ? startY - random.nextInt(5) : startY + random.nextInt(30);
            int minX = random.nextInt(6) == 0 ? random.nextInt(65536) : 100 + random.nextInt(300);
            int minY = random.nextInt(6) == 0 ? random.nextInt(65536) : 100 + random.nextInt(300);
            int maxX = random.nextInt(10) == 0 ? minX - random.nextInt(5) : minX + random.nextInt(35);
            int maxY = random.nextInt(10) == 0 ? minY - random.nextInt(5) : minY + random.nextInt(35);
            for (int v : new int[] {startX, startY, endX, endY, minX, minY, maxX, maxY}) out.putShort(Math.floorMod(v, 65536));
        }
        return out.bytes();
    }

    static byte[] unterminated(Random random) {
        var out = new ByteArrayBuilder();
        int length = 1 + random.nextInt(12);
        for (int i = 0; i < length; i++) out.put(1 + random.nextInt(255));
        return out.bytes();
    }

    static String stack(int[] values, int pointer) {
        if (pointer <= 0) return "-";
        var out = new StringBuilder();
        for (int i = 0; i < pointer; i++) {
            if (i > 0) out.append(',');
            out.append(values[i]);
        }
        return out.toString();
    }

    static int packed(int x, int y) {
        return ((x & 0x3fff) << 14) | (y & 0x3fff);
    }

    static int general(Random random) {
        return random.nextInt(4) == 0 ? BIG[random.nextInt(BIG.length)] : random.nextInt(60000) - 100;
    }

    static final class Loaded {
        final List<Integer> indexes = new ArrayList<>();
        final List<WorldMapElement> elements = new ArrayList<>();
        final IdentityHashMap<WorldMapElement, Integer> owners = new IdentityHashMap<>();
        final List<int[]> placements = new ArrayList<>();
    }

    static Loaded load(List<Integer> members) throws Exception {
        var loaded = new Loaded();
        WorldMap.WORLDMAP_DEFS.clear();
        for (int index : members) {
            var source = SOURCES.get(index);
            var element = decode(source.id, source.bytes);
            WorldMap.WORLDMAP_DEFS.put(element, (long) element.elementId);
            loaded.indexes.add(index);
            loaded.elements.add(element);
            loaded.owners.put(element, index);
            Node<?> node = element.worldMapPlacements.getFirst();
            while (node != null) {
                var p = (WorldMapPlacement) node;
                loaded.placements.add(new int[] {p.plane, p.startX, p.startY, p.endX, p.endY, p.mapMinX, p.mapMinY, p.mapMaxX, p.mapMaxY});
                node = element.worldMapPlacements.next();
            }
        }
        return loaded;
    }

    static int near(Random random, int low, int high) {
        int roll = random.nextInt(10);
        if (roll == 0) return low - 1;
        if (roll == 1) return high + 1;
        if (roll == 2) return low;
        if (roll == 3) return high;
        if (high < low) return low + random.nextInt(3) - 1;
        return low + random.nextInt(high - low + 1);
    }

    static int gameCoord(Random random, Loaded loaded) {
        if (loaded.placements.isEmpty() || random.nextInt(5) == 0) return packed(general(random) & 0x3fff, general(random) & 0x3fff) | (random.nextInt(4) << 28);
        var p = loaded.placements.get(random.nextInt(loaded.placements.size()));
        return packed(near(random, p[1], p[3]), near(random, p[2], p[4])) | (random.nextInt(3) == 0 ? random.nextInt(4) << 28 : p[0] << 28);
    }

    static int mapCoord(Random random, Loaded loaded) {
        if (loaded.placements.isEmpty() || random.nextInt(5) == 0) return packed(general(random) & 0x3fff, general(random) & 0x3fff);
        var p = loaded.placements.get(random.nextInt(loaded.placements.size()));
        return packed(near(random, p[5], p[7]), near(random, p[6], p[8]));
    }

    static int elementId(Random random, Loaded loaded) {
        int roll = random.nextInt(10);
        if (roll < 7 && !loaded.elements.isEmpty()) return loaded.elements.get(random.nextInt(loaded.elements.size())).elementId;
        if (roll < 9) return IDS[random.nextInt(IDS.length)];
        return general(random);
    }

    static int expected(String op) {
        return switch (op) {
            case "WORLDMAP_COORDINMAP", "WORLDMAP_FINDNEARESTELEMENT" -> 2;
            case "WORLDMAP_GETSOURCEPOSITION" -> 0;
            default -> 1;
        };
    }

    static int[] arguments(String op, Random random, Loaded loaded) {
        return switch (op) {
            case "WORLDMAP_GETMAP" -> new int[] {gameCoord(random, loaded)};
            case "WORLDMAP_GETSOURCECOORD" -> new int[] {mapCoord(random, loaded)};
            case "WORLDMAP_GETDISPLAYCOORD" -> new int[] {gameCoord(random, loaded)};
            case "WORLDMAP_COORDINMAP" -> new int[] {gameCoord(random, loaded), elementId(random, loaded)};
            case "WORLDMAP_FINDNEARESTELEMENT" -> new int[] {random.nextInt(4), random.nextInt(3) == 0 ? general(random) : packed(2900 + random.nextInt(300), 3000 + random.nextInt(300))};
            case "WORLDMAP_GETSOURCEPOSITION" -> new int[0];
            default -> new int[] {elementId(random, loaded)};
        };
    }

    static String categories(Random random, List<int[]> list) {
        int count = random.nextInt(4) == 0 ? 0 : random.nextInt(9);
        var text = new StringBuilder();
        for (int i = 0; i < count; i++) {
            int mec = random.nextInt(4);
            int x = random.nextInt(5) == 0 ? BIG[random.nextInt(BIG.length)] : random.nextInt(400) - 50;
            int y = random.nextInt(5) == 0 ? BIG[random.nextInt(BIG.length)] : random.nextInt(400) - 50;
            list.add(new int[] {mec, x, y});
            if (i > 0) text.append(';');
            text.append(mec).append(':').append(x).append(':').append(y);
        }
        return count == 0 ? "-" : text.toString();
    }

    static void runSet(Random random, StringBuilder out, int setId, List<Integer> members, int perOp, int[] total) throws Exception {
        var loaded = load(members);
        var memberText = new StringBuilder();
        for (int i = 0; i < members.size(); i++) {
            if (i > 0) memberText.append(',');
            memberText.append(members.get(i));
        }
        out.append("S\t").append(setId).append('\t').append(members.isEmpty() ? "-" : memberText).append('\n');
        var order = new StringBuilder();
        var element = WorldMap.WORLDMAP_DEFS.first();
        while (element != null) {
            if (order.length() > 0) order.append(',');
            order.append(loaded.owners.get(element));
            element = WorldMap.WORLDMAP_DEFS.next();
        }
        out.append("O\t").append(setId).append('\t').append(order.length() == 0 ? "-" : order).append('\n');
        for (var name : OPS) {
            var op = ClientScriptOpCode.valueOf(name);
            for (int n = 0; n < perOp; n++) {
                int staticId = -1;
                WorldMap.staticElement = null;
                if (random.nextInt(10) < 8 && !loaded.elements.isEmpty()) {
                    staticId = loaded.elements.get(random.nextInt(loaded.elements.size())).elementId;
                } else if (random.nextInt(2) == 0) {
                    staticId = IDS[random.nextInt(IDS.length)];
                }
                if (staticId >= 0) WorldMap.staticElement = WorldMap.poll(staticId);
                if (WorldMap.staticElement == null) staticId = -1;
                int squareX = random.nextInt(4) == 0 ? BIG[random.nextInt(BIG.length)] : 2944 + random.nextInt(64) * 8 - 100;
                int squareY = random.nextInt(4) == 0 ? BIG[random.nextInt(BIG.length)] : 3000 + random.nextInt(64) * 8 - 100;
                int centerX = random.nextInt(4) == 0 ? BIG[random.nextInt(BIG.length)] : random.nextInt(400);
                int centerY = random.nextInt(4) == 0 ? BIG[random.nextInt(BIG.length)] : random.nextInt(400);
                int loading = LOADING[random.nextInt(LOADING.length)];
                WorldMap.mapSquareAbsX = squareX;
                WorldMap.mapSquareAbsY = squareY;
                WorldMapDisplay.mapCenterX = centerX;
                WorldMapDisplay.mapCenterY = centerY;
                ComponentMinimap.mapAssetLoadingPercentage = loading;
                while (WorldMap.worldMapCategories.popHead() != null) {
                }
                var list = new ArrayList<int[]>();
                var categoryText = "-";
                if (name.equals("WORLDMAP_FINDNEARESTELEMENT")) {
                    categoryText = categories(random, list);
                    for (var c : list) {
                        var category = new WorldMapCategory(c[0]);
                        category.absX = c[1];
                        category.absY = c[2];
                        WorldMap.worldMapCategories.addFirst(category);
                    }
                }
                var given = arguments(name, random, loaded);
                if (given.length > 0 && random.nextInt(25) == 0) given = Arrays.copyOf(given, random.nextInt(given.length));
                var e = new CS2Executor();
                Arrays.fill(e.intStack, 0x5a5a5a5a);
                e.intStackPtr = 0;
                e.stringStackPtr = 0;
                e.intOpValues = new int[] {0};
                e.instrPtr = 0;
                for (int v : given) e.intStack[e.intStackPtr++] = v;
                String result = "ok";
                try {
                    QuickChatDisplayOps.dispatch(op, e);
                } catch (Throwable error) {
                    result = "error:" + error.getClass().getSimpleName();
                }
                var argText = new StringBuilder();
                for (int i = 0; i < given.length; i++) {
                    if (i > 0) argText.append(',');
                    argText.append(given[i]);
                }
                if (given.length == 0) argText.append('-');
                String text = "-";
                if (result.equals("ok") && e.stringStackPtr > 0) {
                    var joined = new StringBuilder();
                    for (int i = 0; i < e.stringStackPtr; i++) {
                        if (i > 0) joined.append('|');
                        joined.append(units((String) e.stringStack[i]));
                    }
                    text = joined.toString();
                }
                out.append("C\t").append(setId).append('\t').append(staticId).append('\t').append(squareX).append('\t').append(squareY).append('\t')
                    .append(centerX).append('\t').append(centerY).append('\t').append(loading).append('\t').append(categoryText).append('\t')
                    .append(name).append('\t').append(argText).append('\t').append(result).append('\t')
                    .append(result.equals("ok") ? stack(e.intStack, e.intStackPtr) : "-").append('\t').append(text).append('\n');
                total[0]++;
            }
        }
        WorldMap.staticElement = null;
        WorldMap.WORLDMAP_DEFS.clear();
    }

    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        var input = Path.of(args[1]);
        Files.createDirectories(output);
        var theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        unsafe = (sun.misc.Unsafe) theUnsafe.get(null);
        var random = new Random(SEED);
        var real = new ArrayList<Integer>();
        for (var line : Files.readAllLines(input.resolve("real-elements.tsv"))) {
            var columns = line.split("\t");
            SOURCES.add(new Source(Integer.parseInt(columns[0]), unhex(columns[1]), "real"));
            real.add(SOURCES.size() - 1);
        }
        for (int n = 0; n < SYNTHETIC_ELEMENTS; n++) {
            int id = random.nextInt(3) == 0 ? IDS[random.nextInt(IDS.length)] : random.nextInt(200);
            var bytes = randomValid(random);
            int roll = random.nextInt(10);
            String kind = "valid";
            if (roll == 0 && bytes.length > 0) {
                bytes = Arrays.copyOf(bytes, random.nextInt(bytes.length));
                kind = "truncated";
            } else if (roll == 1) {
                bytes = unterminated(random);
                kind = "unterminated";
            } else if (roll == 2) {
                var extra = new byte[1 + random.nextInt(12)];
                random.nextBytes(extra);
                var joined = Arrays.copyOf(bytes, bytes.length + extra.length);
                System.arraycopy(extra, 0, joined, bytes.length, extra.length);
                bytes = joined;
                kind = "trailing";
            }
            SOURCES.add(new Source(id, bytes, kind));
        }
        var out = new StringBuilder();
        var stats = new TreeMap<String, int[]>();
        for (int index = 0; index < SOURCES.size(); index++) {
            var source = SOURCES.get(index);
            String result;
            try {
                result = digest(decode(source.id, source.bytes));
                VALID.add(index);
            } catch (Throwable error) {
                result = "error:" + error.getClass().getSimpleName();
            }
            out.append("E\t").append(index).append('\t').append(source.id).append('\t').append(hex(source.bytes)).append('\t').append(result).append('\n');
            stats.computeIfAbsent("element\t" + source.kind, k -> new int[2])[result.startsWith("error:") ? 1 : 0]++;
        }
        var total = new int[1];
        runSet(random, out, 0, real, REAL_CASES_PER_OP, total);
        var synthetic = new ArrayList<Integer>();
        for (int index : VALID) if (!real.contains(index)) synthetic.add(index);
        int setId = 1;
        var collide = new ArrayList<Integer>();
        for (int index : synthetic) if (SOURCES.get(index).id % 16 == 1) collide.add(index);
        runSet(random, out, setId++, collide.subList(0, Math.min(8, collide.size())), SET_CASES_PER_OP, total);
        var duplicated = new ArrayList<Integer>();
        if (!synthetic.isEmpty()) {
            duplicated.add(synthetic.get(0));
            duplicated.add(synthetic.get(synthetic.size() / 2));
            duplicated.add(synthetic.get(0));
        }
        runSet(random, out, setId++, duplicated, SET_CASES_PER_OP, total);
        runSet(random, out, setId++, new ArrayList<>(), SET_CASES_PER_OP, total);
        while (setId < SYNTHETIC_SETS) {
            var members = new ArrayList<Integer>();
            int size = 1 + random.nextInt(14);
            for (int i = 0; i < size; i++) members.add(synthetic.get(random.nextInt(synthetic.size())));
            if (random.nextInt(4) == 0) members.addAll(real.subList(0, 1 + random.nextInt(6)));
            Collections.shuffle(members, random);
            runSet(random, out, setId++, members, SET_CASES_PER_OP, total);
        }
        out.append("Z\t").append(total[0]).append('\t').append(SOURCES.size()).append('\n');
        Files.writeString(output.resolve("world-map.native.tsv"), out.toString(), StandardCharsets.UTF_8);
        var summary = new StringBuilder();
        for (var entry : stats.entrySet()) summary.append(entry.getKey()).append('\t').append(entry.getValue()[0]).append('\t').append(entry.getValue()[1]).append('\n');
        Files.writeString(output.resolve("world-map.stats.tsv"), summary.toString(), StandardCharsets.UTF_8);
        System.exit(0);
    }
}
