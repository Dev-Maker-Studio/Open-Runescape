import com.jagex.game.runetek5.cache.node.NodeCollection;
import com.jagex.game.runetek5.client.JSObjectManager;
import com.jagex.game.runetek5.client.ServerEnvironment;
import com.jagex.game.runetek5.config.mec.MECType;
import com.jagex.game.runetek5.config.mec.MECTypeList;
import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.*;
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMap;
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapCategory;
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapDisplay;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.vardomain.VarDomain;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class WorldMapEffectFixtures {
    static final long SEED = 0x727c5228L ^ 0x5eedL;
    static final int MEC_IDS = 6;
    static final int VARPS = 8;
    static final int[][] VARBITS = {{0, 0, 31}, {1, 0, 7}, {2, 8, 15}, {3, 4, 4}};
    static final int[] ZOOMS = {37, 50, 75, 100, 200, 0, 1, 36, 201, -1, 300};
    static final int[] RANGE = {0, 1, 2, 3, 5, 7, 255, -1, 100, Integer.MIN_VALUE, Integer.MAX_VALUE};
    static final String[] SINGLE = {
        "WORLDMAP_SETZOOM", "WORLDMAP_SETMAP", "WORLDMAP_SETMAP_COORD", "WORLDMAP_SETMAP_COORD_OVERRIDE",
        "WORLDMAP_JUMPTODISPLAYCOORD", "WORLDMAP_JUMPTOSOURCECOORD", "WORLDMAP_FLASHELEMENT", "WORLDMAP_FLASHELEMENTCATEGORY",
        "WORLDMAP_CLOSEMAP"
    };
    static final int SINGLE_CASES = 160;
    static final int LIST_CASES = 260;
    static int[] varps = new int[VARPS];

    static final VarDomain DOMAIN = new VarDomain() {
        public int getVarValue(int id) {
            return varps[id];
        }

        public int getVarBitValue(int id) {
            int[] bit = VARBITS[id];
            int width = bit[2] - bit[1];
            int mask = width == 31 ? -1 : (1 << (width + 1)) - 1;
            return (varps[bit[0]] >> bit[1]) & mask;
        }
    };

    static int pick(Random random, int[] values) {
        return values[random.nextInt(values.length)];
    }

    static String csv(int[] values) {
        if (values.length == 0) return "-";
        var text = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) text.append(',');
            text.append(values[i]);
        }
        return text.toString();
    }

    static String state() {
        return csv(new int[] {
            WorldMapDisplay.scrollTargetX, WorldMapDisplay.scrollTargetY, WorldMapDisplay.requestedScrollX,
            WorldMapDisplay.requestedScrollY, WorldMapDisplay.useTargetPosition ? 1 : 0, WorldMapDisplay.someMecId,
            WorldMapDisplay.highlightedCategoryId, JSObjectManager.highlightFlashCycles, ServerEnvironment.flashCycleCounter,
            (int) WorldMap.requestedMapZoom,
            WorldMap.staticElement == null ? -1 : WorldMap.staticElement.elementId
        });
    }

    static int[] mecType(Random random, int id, StringBuilder out, MECTypeList list) throws Exception {
        var type = new MECType();
        type.mectypeId = id;
        int[] row = {id, 1, -1, -1, 0, 0, -1, -1, 0, 0};
        if (random.nextInt(4) != 0) {
            row[1] = random.nextInt(5) == 0 ? 0 : 1;
            row[2] = random.nextInt(3) == 0 ? -1 : random.nextInt(VARPS);
            row[3] = random.nextInt(3) == 0 ? -1 : random.nextInt(VARBITS.length);
            row[4] = pick(random, RANGE);
            row[5] = random.nextInt(2) == 0 ? row[4] + random.nextInt(3) : pick(random, RANGE);
            row[6] = random.nextInt(3) == 0 ? -1 : random.nextInt(VARPS);
            row[7] = random.nextInt(3) == 0 ? -1 : random.nextInt(VARBITS.length);
            row[8] = pick(random, RANGE);
            row[9] = random.nextInt(2) == 0 ? row[8] + random.nextInt(3) : pick(random, RANGE);
            type.visible = row[1] == 1;
            type.primaryVarp = row[2];
            type.primaryVarpbit = row[3];
            type.primaryVariableMinValue = row[4];
            type.primaryVariableMaxValue = row[5];
            type.secondaryVarp = row[6];
            type.secondaryVarpbit = row[7];
            type.secondaryVariableMinValue = row[8];
            type.secondaryVariableMaxValue = row[9];
        }
        type.postDecode();
        synchronized (list.definitionCache) {
            list.definitionCache.put(type, (long) id);
        }
        return row;
    }

    static void emit(StringBuilder out, int set, int staticId, int squareX, int squareY, String categories, String ops, String args,
                     String pre, String result, String stack, String post, String effects) {
        out.append("C\t").append(set).append('\t').append(staticId).append('\t').append(squareX).append('\t').append(squareY).append('\t')
            .append(categories).append('\t').append(csv(varps)).append('\t').append(ops).append('\t').append(args).append('\t')
            .append(pre).append('\t').append(result).append('\t').append(stack).append('\t').append(post).append('\t').append(effects).append('\n');
    }

    static void prepare(Random random, WorldMapFixtures.Loaded loaded) throws Exception {
        WorldMapDisplay.scrollTargetX = random.nextInt(3) == 0 ? -1 : random.nextInt(500);
        WorldMapDisplay.scrollTargetY = random.nextInt(3) == 0 ? -1 : random.nextInt(500);
        WorldMapDisplay.requestedScrollX = random.nextInt(3) == 0 ? -1 : random.nextInt(500);
        WorldMapDisplay.requestedScrollY = random.nextInt(3) == 0 ? -1 : random.nextInt(500);
        WorldMapDisplay.useTargetPosition = random.nextBoolean();
        WorldMapDisplay.someMecId = random.nextInt(3) == 0 ? -1 : random.nextInt(50);
        WorldMapDisplay.highlightedCategoryId = random.nextInt(3) == 0 ? -1 : random.nextInt(50);
        JSObjectManager.highlightFlashCycles = random.nextInt(5);
        ServerEnvironment.flashCycleCounter = random.nextInt(120);
        WorldMap.requestedMapZoom = pick(random, new int[] {0, 3, 4, 6, 8, 16, 5});
    }

    static void chooseStatic(Random random, WorldMapFixtures.Loaded loaded) {
        WorldMap.staticElement = null;
        int staticId = -1;
        if (random.nextInt(10) < 8 && !loaded.elements.isEmpty()) {
            staticId = loaded.elements.get(random.nextInt(loaded.elements.size())).elementId;
        } else if (random.nextInt(2) == 0) {
            staticId = WorldMapFixtures.IDS[random.nextInt(WorldMapFixtures.IDS.length)];
        }
        if (staticId >= 0) WorldMap.staticElement = WorldMap.poll(staticId);
    }

    static int[] arguments(String op, Random random, WorldMapFixtures.Loaded loaded) {
        return switch (op) {
            case "WORLDMAP_SETZOOM" -> new int[] {pick(random, ZOOMS)};
            case "WORLDMAP_SETMAP" -> new int[] {WorldMapFixtures.elementId(random, loaded)};
            case "WORLDMAP_SETMAP_COORD", "WORLDMAP_SETMAP_COORD_OVERRIDE" ->
                new int[] {WorldMapFixtures.elementId(random, loaded), random.nextInt(4) == 0 ? random.nextInt() : WorldMapFixtures.gameCoord(random, loaded)};
            case "WORLDMAP_JUMPTODISPLAYCOORD" -> new int[] {random.nextInt(4) == 0 ? random.nextInt() : WorldMapFixtures.mapCoord(random, loaded)};
            case "WORLDMAP_JUMPTOSOURCECOORD" -> new int[] {WorldMapFixtures.gameCoord(random, loaded)};
            case "WORLDMAP_FLASHELEMENT", "WORLDMAP_FLASHELEMENTCATEGORY" -> new int[] {random.nextInt(3) == 0 ? WorldMapFixtures.general(random) : random.nextInt(60)};
            default -> new int[0];
        };
    }

    static String run(List<String> ops, int[] given, int[] stackOut) {
        var e = new CS2Executor();
        Arrays.fill(e.intStack, 0x5a5a5a5a);
        e.intStackPtr = 0;
        e.stringStackPtr = 0;
        e.intOpValues = new int[] {0};
        e.instrPtr = 0;
        for (int v : given) e.intStack[e.intStackPtr++] = v;
        String result = "ok";
        try {
            for (var name : ops) QuickChatDisplayOps.dispatch(ClientScriptOpCode.valueOf(name), e);
        } catch (Throwable error) {
            result = "error";
            if (System.getProperty("fixture.debug") != null) error.printStackTrace();
        }
        int pointer = Math.max(0, e.intStackPtr);
        stackOut[0] = pointer;
        System.arraycopy(e.intStack, 0, stackOut, 1, Math.min(pointer, stackOut.length - 1));
        return result;
    }

    static String stackText(int[] holder) {
        int pointer = holder[0];
        return WorldMapFixtures.stack(Arrays.copyOfRange(holder, 1, holder.length), pointer);
    }

    static void drive(Random random, StringBuilder out, int set, WorldMapFixtures.Loaded loaded, int[] total) throws Exception {
        var listClass = MECTypeList.class;
        var list = (MECTypeList) WorldMapFixtures.unsafe.allocateInstance(listClass);
        WorldMapFixtures.putObj(list, "definitionCache", new LRUCache(128));
        WorldMap.MECTYPE_LIST = list;
        WorldMap.PLAYER_VAR_PROVIDER = DOMAIN;
        for (var name : SINGLE) {
            for (int n = 0; n < SINGLE_CASES; n++) {
                chooseStatic(random, loaded);
                prepare(random, loaded);
                int staticId = WorldMap.staticElement == null ? -1 : WorldMap.staticElement.elementId;
                int squareX = random.nextInt(4) == 0 ? WorldMapFixtures.BIG[random.nextInt(WorldMapFixtures.BIG.length)] : 2944 + random.nextInt(64) * 8 - 100;
                int squareY = random.nextInt(4) == 0 ? WorldMapFixtures.BIG[random.nextInt(WorldMapFixtures.BIG.length)] : 3000 + random.nextInt(64) * 8 - 100;
                WorldMap.mapSquareAbsX = squareX;
                WorldMap.mapSquareAbsY = squareY;
                var given = arguments(name, random, loaded);
                if (given.length > 0 && random.nextInt(25) == 0) given = Arrays.copyOf(given, random.nextInt(given.length));
                var pre = state();
                WorldMapEffectHooks.LOG.clear();
                var holder = new int[65];
                var result = run(List.of(name), given, holder);
                var effects = WorldMapEffectHooks.LOG.isEmpty() ? "-" : String.join(",", WorldMapEffectHooks.LOG);
                emit(out, set, staticId, squareX, squareY, "-", name, csv(given), pre, result,
                    result.equals("ok") ? stackText(holder) : "-", state(), effects);
                total[0]++;
            }
        }
        for (int n = 0; n < LIST_CASES; n++) {
            chooseStatic(random, loaded);
            prepare(random, loaded);
            int staticId = WorldMap.staticElement == null ? -1 : WorldMap.staticElement.elementId;
            int squareX = random.nextInt(4) == 0 ? WorldMapFixtures.BIG[random.nextInt(WorldMapFixtures.BIG.length)] : 2944 + random.nextInt(64) * 8 - 100;
            int squareY = random.nextInt(4) == 0 ? WorldMapFixtures.BIG[random.nextInt(WorldMapFixtures.BIG.length)] : 3000 + random.nextInt(64) * 8 - 100;
            WorldMap.mapSquareAbsX = squareX;
            WorldMap.mapSquareAbsY = squareY;
            for (int i = 0; i < VARPS; i++) varps[i] = random.nextInt(3) == 0 ? pick(random, RANGE) : random.nextInt(8);
            synchronized (list.definitionCache) {
                list.definitionCache.reset();
            }
            var typeRows = new StringBuilder();
            for (int id = 0; id < MEC_IDS; id++) {
                var row = mecType(random, id, typeRows, list);
                if (id > 0) typeRows.append(';');
                typeRows.append(csv(row));
            }
            while (WorldMap.worldMapCategories.popHead() != null) {
            }
            var categories = new ArrayList<int[]>();
            int count = random.nextInt(4) == 0 ? 0 : random.nextInt(10);
            var categoryText = new StringBuilder();
            for (int i = 0; i < count; i++) {
                int mec = random.nextInt(MEC_IDS);
                int x = random.nextInt(5) == 0 ? WorldMapFixtures.BIG[random.nextInt(WorldMapFixtures.BIG.length)] : random.nextInt(400) - 50;
                int y = random.nextInt(5) == 0 ? WorldMapFixtures.BIG[random.nextInt(WorldMapFixtures.BIG.length)] : random.nextInt(400) - 50;
                var category = new WorldMapCategory(mec);
                category.absX = x;
                category.absY = y;
                WorldMap.worldMapCategories.addFirst(category);
                if (i > 0) categoryText.append(';');
                categoryText.append(mec).append(':').append(x).append(':').append(y);
            }
            var sequence = new ArrayList<String>();
            int steps = 1 + random.nextInt(count + 3);
            boolean start = random.nextInt(8) != 0;
            for (int i = 0; i < steps; i++) {
                sequence.add(i == 0 && start || (i > 0 && random.nextInt(12) == 0) ? "WORLDMAP_LISTELEMENT_START" : "WORLDMAP_LISTELEMENT_NEXT");
            }
            WorldMapDisplay.categoryIterator.reinitialize(new NodeCollection());
            var pre = state();
            WorldMapEffectHooks.LOG.clear();
            var holder = new int[129];
            var result = run(sequence, new int[0], holder);
            var catalog = typeRows.toString();
            emit(out, set, staticId, squareX, squareY, (count == 0 ? "-" : categoryText) + "@" + catalog, String.join(",", sequence), "-", pre,
                result, result.equals("ok") ? stackText(holder) : "-", state(), "-");
            total[0]++;
        }
        WorldMap.staticElement = null;
        WorldMap.worldMapCategories.popHead();
        WorldMap.WORLDMAP_DEFS.clear();
    }

    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        var input = Path.of(args[1]);
        Files.createDirectories(output);
        var theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        WorldMapFixtures.unsafe = (sun.misc.Unsafe) theUnsafe.get(null);
        var random = new Random(SEED);
        var real = new ArrayList<Integer>();
        for (var line : Files.readAllLines(input.resolve("real-elements.tsv"))) {
            var columns = line.split("\t");
            WorldMapFixtures.SOURCES.add(new WorldMapFixtures.Source(Integer.parseInt(columns[0]), WorldMapFixtures.unhex(columns[1]), "real"));
            real.add(WorldMapFixtures.SOURCES.size() - 1);
        }
        var out = new StringBuilder();
        for (var line : Files.readAllLines(input.resolve("real-elements.tsv"))) {
            var columns = line.split("\t");
            out.append("E\t").append(columns[0]).append('\t').append(columns[1]).append('\n');
        }
        for (int i = 0; i < VARBITS.length; i++) {
            out.append("B\t").append(i).append('\t').append(VARBITS[i][0]).append('\t').append(VARBITS[i][1]).append('\t').append(VARBITS[i][2]).append('\n');
        }
        var total = new int[1];
        drive(random, out, 0, WorldMapFixtures.load(real), total);
        drive(random, out, 1, WorldMapFixtures.load(new ArrayList<>()), total);
        out.append("Z\t").append(total[0]).append('\n');
        Files.writeString(output.resolve("world-map-effects.native.tsv"), out.toString(), StandardCharsets.UTF_8);
        System.exit(0);
    }
}
