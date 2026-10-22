import com.jagex.game.runetek5.client.Client;
import com.jagex.game.runetek5.client.input.camera.Camera;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.*;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.movement.CoordGrid;
import com.jagex.game.runetek5.network.PingRequester;
import com.jagex.game.runetek5.scene.MapRegion;
import com.jagex.game.runetek5.scene.Scene;
import com.jagex.game.runetek5.scene.SceneGraph;
import com.jagex.game.runetek5.scene.SettingsBits;
import com.jagex.game.runetek5.scene.ground.Ground;
import com.jagex.game.runetek5.scene.ground.GroundSM;
import com.jagex.game.runetek5.staticmethods.StaticMethods;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class SceneFixtures {
    static final long SEED = 0x727c5228L;
    static final int PER_OP = 1500;
    static final int ATAN_PAIRS = 40000;
    static final int[] GENERAL = {0, 1, -1, 2, 3, 4, 5, 6, 7, 8, 12, 24, 100, 255, 256, 65535, 65536, 0x7fffffff, Integer.MIN_VALUE, -2, 1000, 12345678, -98765, 0x5aa5f00f, 32767, 32768, -32768, 70000};
    static final int[] BASES = {0, 3200, 3300, 16383, -1, 5000, 100, 2944, 12000};
    static final int[] PLANES = {0, 0, 0, 1, 1, 2, 3, -1, 4, 7};
    static final float[] FLOATS = {0f, 12f, -12f, 3.5f, 100f, 1e10f, -1e10f, Float.NaN, 8f};
    static sun.misc.Unsafe unsafe;
    static final Map<String, Field> FIELDS = new HashMap<>();
    static final List<Scene> SCENES = new ArrayList<>();
    static final List<int[]> SIZES = new ArrayList<>();
    static final List<Object[]> SCENE_DATA = new ArrayList<>();

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

    static int getStaticInt(Class<?> t, String n) throws Exception {
        var f = field(t, n);
        return unsafe.getInt(unsafe.staticFieldBase(f), unsafe.staticFieldOffset(f));
    }

    static void setStaticBool(Class<?> t, String n, boolean v) throws Exception {
        var f = field(t, n);
        unsafe.putBoolean(unsafe.staticFieldBase(f), unsafe.staticFieldOffset(f), v);
    }

    static boolean getStaticBool(Class<?> t, String n) throws Exception {
        var f = field(t, n);
        return unsafe.getBoolean(unsafe.staticFieldBase(f), unsafe.staticFieldOffset(f));
    }

    static void putInt(Object target, String name, int value) throws Exception {
        unsafe.putInt(target, unsafe.objectFieldOffset(field(target.getClass(), name)), value);
    }

    static void putObj(Object target, String name, Object value) throws Exception {
        unsafe.putObject(target, unsafe.objectFieldOffset(field(target.getClass(), name)), value);
    }

    static int pickFrom(Random random, int[] values) {
        return values[random.nextInt(values.length)];
    }

    static int general(Random random) {
        return random.nextInt(4) == 0 ? random.nextInt(2000) - 1000 : pickFrom(random, GENERAL);
    }

    static final class Definition {
        boolean loaded = true;
        boolean graph = true;
        int sizeX;
        int sizeY;
        int[][][][] heights = new int[4][][][];
        int[] widths = new int[4];
        int[] lengths = new int[4];
        boolean[] nullPlane = new boolean[4];
        byte[][] masks;
        byte[][][][] lighting;
        int heightKind;
    }

    static int height(Random random, int kind, int x, int y) {
        return switch (kind) {
            case 0 -> random.nextInt(6000) - 3000;
            case 1 -> random.nextInt(2) == 0 ? (random.nextInt(1 << 30) - (1 << 29)) : (random.nextBoolean() ? Integer.MAX_VALUE - random.nextInt(8) : Integer.MIN_VALUE + random.nextInt(8));
            default -> -(x * 7 + y * 5) * 3 + random.nextInt(40);
        };
    }

    static int[][] grid(Random random, int kind, int w, int l) {
        var rows = new int[w + 1][l + 1];
        for (int x = 0; x <= w; x++) for (int y = 0; y <= l; y++) rows[x][y] = height(random, kind, x, y);
        return rows;
    }

    static byte[][] bytes(Random random, int w, int l, int percentRoof) {
        var rows = new byte[w][l];
        for (int x = 0; x < w; x++) for (int y = 0; y < l; y++) {
            int roll = random.nextInt(100);
            rows[x][y] = (byte) (roll < percentRoof ? (random.nextInt(256) | 2) : (random.nextInt(256) & ~2));
        }
        return rows;
    }

    static Definition define(Random random, int variant) {
        var d = new Definition();
        int sx = switch (variant) {
            case 0 -> 30;
            case 1 -> 40;
            case 2 -> 30;
            case 3 -> 20;
            case 4 -> 6;
            case 5 -> 30;
            case 6 -> 24;
            case 7 -> 64;
            case 8 -> 18;
            default -> 22;
        };
        int sy = variant == 0 ? 26 : variant == 8 ? 12 : sx;
        d.sizeX = sx;
        d.sizeY = sy;
        d.loaded = variant != 2;
        d.graph = variant != 1;
        d.heightKind = variant == 1 ? 1 : variant == 7 ? 2 : 0;
        for (int p = 0; p < 4; p++) {
            int w = sx;
            int l = sy;
            if (variant == 8) {
                w = sx - 5;
                l = sy - 3;
            }
            d.widths[p] = w;
            d.lengths[p] = l;
            d.heights[p] = new int[][][] {grid(random, d.heightKind, w, l)};
            if (variant == 9 && p == 1) {
                var g = d.heights[p][0];
                g[3] = Arrays.copyOf(g[3], 4);
                d.heights[p][0] = Arrays.copyOf(g, 12);
            }
        }
        d.nullPlane[2] = variant == 3;
        d.masks = variant == 5 ? bytes(random, 10, 10, 40) : bytes(random, sx, sy, variant == 4 ? 80 : 30);
        d.lighting = new byte[4][][][];
        for (int p = 0; p < 4; p++) {
            var plane = new byte[sx][sy];
            for (var row : plane) random.nextBytes(row);
            d.lighting[p] = new byte[][][] {plane};
        }
        if (variant == 6) {
            d.lighting[1] = null;
            d.lighting[2][0] = Arrays.copyOf(d.lighting[2][0], 10);
        }
        return d;
    }

    static String rowsText(int[][] rows) {
        var sb = new StringBuilder();
        for (int i = 0; i < rows.length; i++) {
            if (i > 0) sb.append(';');
            for (int j = 0; j < rows[i].length; j++) {
                if (j > 0) sb.append(',');
                sb.append(rows[i][j]);
            }
        }
        return sb.toString();
    }

    static String bytesText(byte[][] rows) {
        var sb = new StringBuilder();
        for (int i = 0; i < rows.length; i++) {
            if (i > 0) sb.append(';');
            for (int j = 0; j < rows[i].length; j++) {
                if (j > 0) sb.append(',');
                sb.append(rows[i][j]);
            }
        }
        return sb.toString();
    }

    static String sceneLine(int id, Definition d) {
        var sb = new StringBuilder("S\t").append(id).append('\t').append(d.loaded ? 1 : 0).append('\t').append(d.sizeX).append('\t').append(d.sizeY).append('\t');
        for (int p = 0; p < 4; p++) {
            if (p > 0) sb.append('|');
            if (d.nullPlane[p]) {
                sb.append('n');
            } else {
                sb.append(d.widths[p]).append(':').append(d.lengths[p]).append('/').append(rowsText(d.heights[p][0]));
            }
        }
        sb.append('\t').append(bytesText(d.masks)).append('\t');
        if (!d.graph) {
            sb.append('n');
        } else {
            for (int p = 0; p < 4; p++) {
                if (p > 0) sb.append('|');
                sb.append(d.lighting[p] == null ? "n" : "a" + bytesText(d.lighting[p][0]));
            }
        }
        return sb.toString();
    }

    static void installScene(Definition d, int baseX, int baseY) throws Exception {
        var region = (MapRegion) unsafe.allocateInstance(MapRegion.class);
        var coords = new CoordGrid();
        coords.x = baseX;
        coords.y = baseY;
        region.baseCoordinates = coords;
        putInt(region, "sceneSizeX", d.sizeX);
        putInt(region, "sceneSizeY", d.sizeY);
        var bits = (SettingsBits) unsafe.allocateInstance(SettingsBits.class);
        var masks = new byte[4][][];
        for (int i = 0; i < 4; i++) masks[i] = new byte[0][];
        masks[1] = d.masks;
        putObj(bits, "tileFunctionMasks", masks);
        putObj(region, "tileSettingsBits", bits);
        if (d.loaded) {
            var scene = (Scene) unsafe.allocateInstance(Scene.class);
            var grounds = new Ground[4];
            for (int p = 0; p < 4; p++) {
                if (d.nullPlane[p]) continue;
                var ground = (Ground) unsafe.allocateInstance(GroundSM.class);
                putInt(ground, "width", d.widths[p]);
                putInt(ground, "length", d.lengths[p]);
                putObj(ground, "tileHeights", d.heights[p][0]);
                putInt(ground, "tileUnits", 512);
                putInt(ground, "tileScale", 9);
                grounds[p] = ground;
            }
            putObj(scene, "localHeightmap", grounds);
            region.scene = scene;
        } else {
            region.scene = null;
        }
        if (d.graph) {
            var graph = (SceneGraph) unsafe.allocateInstance(SceneGraph.class);
            var lighting = new byte[4][][];
            for (int p = 0; p < 4; p++) lighting[p] = d.lighting[p] == null ? null : d.lighting[p][0];
            putObj(graph, "lighting", lighting);
            putObj(region, "surfaceSceneGraph", graph);
        } else {
            putObj(region, "surfaceSceneGraph", null);
        }
        Resource.mapRegion = region;
    }

    static int baseTile(Random random, int size) {
        int roll = random.nextInt(10);
        if (roll < 7) return (random.nextInt(size * 512)) + 0;
        if (roll < 8) return random.nextInt(size * 512 + 4096) - 2048;
        return general(random);
    }

    static void install(Random random, int sceneId) throws Exception {
        var d = (Definition) SCENE_DATA.get(sceneId)[0];
        installScene(d, pickFrom(random, BASES), pickFrom(random, BASES));
        Camera.cameraCalculationType = random.nextInt(7);
        Camera.cameraYaw = general(random);
        Camera.cameraRoll = general(random);
        Camera.cameraPitch = general(random);
        Camera.moveCameraSpeed = general(random);
        Camera.moveCameraAngle = general(random);
        Camera.cameraPositionAbsoluteX = random.nextInt(3) == 0 ? general(random) : random.nextInt(d.sizeX * 512);
        Camera.cameraPositionAbsoluteY = random.nextInt(3) == 0 ? general(random) : random.nextInt(d.sizeY * 512);
        Camera.cameraPositionAbsoluteZ = random.nextInt(3) == 0 ? general(random) : random.nextInt(8000) - 4000;
        Camera.camAngleX = FLOATS[random.nextInt(FLOATS.length)];
        Camera.camAngleY = FLOATS[random.nextInt(FLOATS.length)];
        Camera.lookX = general(random);
        Camera.lookY = general(random);
        Camera.lookAtHeightOffset = general(random);
        Camera.cameraBaseX = baseTile(random, d.sizeX);
        Camera.cameraBaseY = baseTile(random, d.sizeY);
        Camera.regularLowestYaw = random.nextBoolean() ? 262144 + random.nextInt(600000) : general(random);
        setStaticBool(Client.class, "sendCameraAngleToServer", random.nextBoolean());
        setStaticInt(PingRequester.class, "cameraTargetAbsX", general(random));
        StaticMethods.cameraTargetAbsY = general(random);
        PlayerUpdate.MY_PLAYER_PLANE = pickFrom(random, PLANES);
    }

    static String digest(int sceneId) throws Exception {
        var sb = new StringBuilder();
        sb.append(sceneId).append('|');
        var base = Resource.mapRegion.baseCoordinates;
        int[] values = {Camera.cameraCalculationType, Camera.cameraYaw, Camera.cameraRoll, Camera.cameraPitch, Camera.moveCameraSpeed, Camera.moveCameraAngle,
            Camera.cameraPositionAbsoluteX, Camera.cameraPositionAbsoluteY, Camera.cameraPositionAbsoluteZ, Float.floatToRawIntBits(Camera.camAngleX),
            Float.floatToRawIntBits(Camera.camAngleY), getStaticBool(Client.class, "sendCameraAngleToServer") ? 1 : 0, getStaticInt(PingRequester.class, "cameraTargetAbsX"),
            StaticMethods.cameraTargetAbsY, Camera.lookX, Camera.lookY, Camera.lookAtHeightOffset, Camera.cameraBaseX, Camera.cameraBaseY, Camera.regularLowestYaw,
            PlayerUpdate.MY_PLAYER_PLANE, base.x, base.y};
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(values[i]);
        }
        return sb.toString();
    }

    static int coordFor(Random random, Definition d) {
        var base = Resource.mapRegion.baseCoordinates;
        int roll = random.nextInt(10);
        if (roll < 7) {
            int x = base.x + random.nextInt(d.sizeX + 6) - 3;
            int y = base.y + random.nextInt(d.sizeY + 6) - 3;
            return ((x & 0x3fff) << 14) | (y & 0x3fff);
        }
        if (roll < 9) return (random.nextInt(0x4000) << 14) | random.nextInt(0x4000);
        return general(random);
    }

    static int[] arguments(String op, Random random, Definition d) {
        if (op.equals("CAM_LOOKAT")) {
            int angle = random.nextInt(4) == 0 ? random.nextInt(130) : 100 + random.nextInt(60);
            int height = random.nextInt(3) == 0 ? general(random) : random.nextInt(900) - 200;
            return new int[] {coordFor(random, d), height, general(random), angle};
        }
        if (random.nextInt(4) == 0) return new int[] {general(random), general(random)};
        return new int[] {random.nextInt(2400), random.nextInt(2400) - 200};
    }

    static String stack(int[] values, int pointer) {
        if (pointer <= 0) return "-";
        var sb = new StringBuilder();
        for (int i = 0; i < pointer; i++) {
            if (i > 0) sb.append(',');
            sb.append(values[i]);
        }
        return sb.toString();
    }

    static double randomDouble(Random random) {
        return switch (random.nextInt(8)) {
            case 0 -> random.nextInt(40000) - 20000;
            case 1 -> random.nextInt(70) - 35;
            case 2 -> Double.longBitsToDouble(random.nextLong());
            case 3 -> random.nextGaussian() * 1000;
            case 4 -> new double[] {0.0, -0.0, 1.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.MIN_VALUE, -Double.MIN_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, 1e-300, 1e300, 0.4375, 0.6875, 1.1875, 2.4375}[random.nextInt(17)];
            case 5 -> random.nextInt(2000) - 1000 + 0.0;
            case 6 -> Math.pow(2, random.nextInt(140) - 70) * (random.nextBoolean() ? 1 : -1);
            default -> random.nextDouble() * 4 - 2;
        };
    }

    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        var theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        unsafe = (sun.misc.Unsafe) theUnsafe.get(null);
        var random = new Random(SEED);
        var out = new StringBuilder();
        for (int id = 0; id < 10; id++) {
            var d = define(random, id);
            SCENE_DATA.add(new Object[] {d});
            out.append(sceneLine(id, d)).append('\n');
        }
        int total = 0;
        var stats = new TreeMap<String, int[]>();
        for (var name : new String[] {"CAM_LOOKAT", "CAM_FORCEANGLE"}) {
            var op = ClientScriptOpCode.valueOf(name);
            for (int n = 0; n < PER_OP; n++) {
                int sceneId = random.nextInt(10);
                if (random.nextInt(3) != 0 && sceneId > 1) sceneId = random.nextInt(2);
                var d = (Definition) SCENE_DATA.get(sceneId)[0];
                install(random, sceneId);
                var given = arguments(name, random, d);
                if (random.nextInt(14) == 0) given = Arrays.copyOf(given, random.nextInt(given.length));
                var e = new CS2Executor();
                Arrays.fill(e.intStack, 0x5a5a5a5a);
                e.intStackPtr = 0;
                e.intOpValues = new int[] {0};
                e.instrPtr = 0;
                for (int v : given) e.intStack[e.intStackPtr++] = v;
                var before = digest(sceneId);
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
                out.append("C\t").append(name).append('\t').append(argText).append('\t').append(before).append('\t').append(result).append('\t')
                    .append(result.equals("ok") ? stack(e.intStack, e.intStackPtr) : "-").append('\t').append(digest(sceneId)).append('\n');
                stats.computeIfAbsent(name + "\t" + sceneId, k -> new int[2])[result.equals("ok") ? 0 : 1]++;
                total++;
            }
        }
        for (int n = 0; n < ATAN_PAIRS; n++) {
            double y = randomDouble(random);
            double x = randomDouble(random);
            out.append("A\t").append(Long.toHexString(Double.doubleToRawLongBits(y))).append('\t').append(Long.toHexString(Double.doubleToRawLongBits(x))).append('\t')
                .append(Long.toHexString(Double.doubleToRawLongBits(Math.atan2(y, x)))).append('\n');
        }
        out.append("Z\t").append(total).append('\t').append(ATAN_PAIRS).append('\n');
        Files.writeString(output.resolve("scene.native.tsv"), out.toString(), StandardCharsets.UTF_8);
        var summary = new StringBuilder();
        for (var entry : stats.entrySet()) summary.append(entry.getKey()).append('\t').append(entry.getValue()[0]).append('\t').append(entry.getValue()[1]).append('\n');
        Files.writeString(output.resolve("scene.stats.tsv"), summary.toString(), StandardCharsets.UTF_8);
        System.exit(0);
    }
}
