import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.client.Client;
import com.jagex.game.runetek5.client.input.camera.Camera;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.*;
import com.jagex.game.runetek5.graphics.map.minimap.ComponentMinimap;
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMap;
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapDisplay;
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapElement;
import com.jagex.game.runetek5.movement.CoordGrid;
import com.jagex.game.runetek5.network.PingRequester;
import com.jagex.game.runetek5.scene.MapRegion;
import com.jagex.game.runetek5.staticmethods.StaticMethods;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class WorldFixtures {
    static final long SEED = 0x727c5228L;
    static final int PER_OP = 600;
    static final int[] POOL = {0, 1, 2, 3, 5, 7, 8, 100, -1, 65535, Integer.MAX_VALUE, Integer.MIN_VALUE};
    static final int[] GENERAL = {0, 1, -1, 2, 3, 4, 5, 6, 7, 8, 12, 24, 100, 255, 256, 65535, 65536, 0x7fffffff, Integer.MIN_VALUE, -2, 1000, 12345678, -98765, 0x5aa5f00f, 32767, 32768, -32768, 70000};
    static final int[] COORDS = {0, -1, 3200 << 14 | 3200, 3300 << 14 | 3250, 16383 << 14 | 16383, 1 << 14, 1, (3 << 28) | (3200 << 14) | 3200, 100 << 14 | 200, 0x7fffffff, Integer.MIN_VALUE, 5000 << 14 | 40, 0x3fff, 3100 << 14 | 3500};
    static final int[] PATHS = {-2, -1, 0, 1, 0, 1, 2, 3, 100, Integer.MAX_VALUE, Integer.MIN_VALUE};
    static final int[] KEYFRAMES = {-3, -2, -1, 0, 1, 2, 3, 4, 5, 6, 10, 0x7fffffff, Integer.MIN_VALUE, 0x40000000, 0x3fffffff};
    static final int[] COUNTS = {-1, -5, 0, 1, 2, 3, 4, 6, 8, 16, 40, 64, Integer.MIN_VALUE, 0x40000000, 0xc0000000};
    static final int[] BASES = {0, 3200, 3300, 16383, -1, 5000, 100, 2944, 12000};
    static final int[] SIZES = {0, 104, 64, 128, 40, -5, 13, 1, 255};
    static final float[] FLOATS = {0f, 12f, -12f, 24f, -24f, 3.5f, 100f, -7.9f, 7.9f, 1000.5f, 1e10f, -1e10f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 8f, 16f, 5f};
    static final float[] ZOOMS = {0f, 3f, 4f, 6f, 8f, 5f, 3.0000002f, -0f, Float.NaN, 2f, 9f, 6.5f, 4.0000005f};
    static final short[] SHORTS = {0, 1, -1, 256, 205, 320, 32767, -32768, 100, 5, 2, 1000, 12345};
    static final String[] QUICK = {
        "CAM_MOVETO", "CAM_MOVEALONG", "CAM_RESET", "CAM_SMOOTHRESET", "CAM_GETANGLE_XA", "CAM_GETANGLE_YA", "CAM_INC_X", "CAM_DEC_X",
        "CAM_INC_Y", "CAM_DEC_Y", "CAM_SETFOLLOWHEIGHT", "CAM_GETFOLLOWHEIGHT", "CAM_FOLLOWCOORD", "COORD_CLAMPTOSCENE_ORDEFAULT",
        "SPLINE_NEW", "SPLINE_ADDPOINT", "SPLINE_LENGTH", "WORLDMAP_GETDISPLAYPOSITION", "WORLDMAP_GETSIZE", "WORLDMAP_ISLOADED",
        "WORLDMAP_GETCURRENTMAP", "WORLDMAP_GETZOOM", "WORLDMAP_DISABLEELEMENTS", "WORLDMAP_GETDISABLEELEMENTS", "WORLDMAP_DISABLEELEMENT",
        "WORLDMAP_GETDISABLEELEMENT", "WORLDMAP_DISABLEELEMENTCATEGORY", "WORLDMAP_GETDISABLEELEMENTCATEGORY"
    };
    static final String[] CAMERA = {"CAM2_ISENABLED", "VIEWPORT_SETFOV", "VIEWPORT_SETZOOM", "VIEWPORT_CLAMPFOV", "VIEWPORT_GETFOV", "VIEWPORT_GETZOOM"};
    static sun.misc.Unsafe unsafe;
    static final Map<String, Field> FIELDS = new HashMap<>();

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

    static Object base(Field f) {
        return unsafe.staticFieldBase(f);
    }

    static long off(Field f) {
        return unsafe.staticFieldOffset(f);
    }

    static void setInt(Class<?> t, String n, int v) throws Exception {
        var f = field(t, n);
        unsafe.putInt(base(f), off(f), v);
    }

    static int getInt(Class<?> t, String n) throws Exception {
        var f = field(t, n);
        return unsafe.getInt(base(f), off(f));
    }

    static void setFloat(Class<?> t, String n, float v) throws Exception {
        var f = field(t, n);
        unsafe.putFloat(base(f), off(f), v);
    }

    static float getFloat(Class<?> t, String n) throws Exception {
        var f = field(t, n);
        return unsafe.getFloat(base(f), off(f));
    }

    static void setBool(Class<?> t, String n, boolean v) throws Exception {
        var f = field(t, n);
        unsafe.putBoolean(base(f), off(f), v);
    }

    static boolean getBool(Class<?> t, String n) throws Exception {
        var f = field(t, n);
        return unsafe.getBoolean(base(f), off(f));
    }

    static void setShort(Class<?> t, String n, short v) throws Exception {
        var f = field(t, n);
        unsafe.putShort(base(f), off(f), v);
    }

    static short getShort(Class<?> t, String n) throws Exception {
        var f = field(t, n);
        return unsafe.getShort(base(f), off(f));
    }

    static Object getObj(Class<?> t, String n) throws Exception {
        var f = field(t, n);
        return unsafe.getObject(base(f), off(f));
    }

    static void setObj(Class<?> t, String n, Object v) throws Exception {
        var f = field(t, n);
        unsafe.putObject(base(f), off(f), v);
    }

    static void putInt(Object target, String name, int value) throws Exception {
        var f = field(target.getClass(), name);
        unsafe.putInt(target, unsafe.objectFieldOffset(f), value);
    }

    static int readInt(Object target, String name) throws Exception {
        var f = field(target.getClass(), name);
        return unsafe.getInt(target, unsafe.objectFieldOffset(f));
    }

    static int pickFrom(Random random, int[] values) {
        return values[random.nextInt(values.length)];
    }

    static int general(Random random) {
        return random.nextInt(4) == 0 ? random.nextInt(2000) - 1000 : pickFrom(random, GENERAL);
    }

    static int small(Random random) {
        return random.nextInt(40) - 10;
    }

    static int[][] randomSpline(Random random) {
        int length = random.nextInt(7) * 2;
        var rows = new int[length][];
        for (int i = 0; i < length; i++) {
            rows[i] = new int[(i & 1) == 0 ? 4 : 3];
            for (int j = 0; j < rows[i].length; j++) rows[i][j] = general(random);
        }
        return rows;
    }

    static void install(Random random) throws Exception {
        var c = Camera.class;
        Camera.cameraCalculationType = random.nextInt(7);
        Camera.cameraYaw = general(random);
        Camera.cameraRoll = general(random);
        Camera.cameraPitch = general(random);
        Camera.moveCameraSpeed = general(random);
        Camera.moveCameraAngle = general(random);
        Camera.cameraMovementLocalX = general(random);
        Camera.cameraMovementLocalY = general(random);
        Camera.cameraMovementZ = general(random);
        Camera.cameraConstantMoveSpeed = general(random);
        Camera.cameraVariableMoveSpeed = general(random);
        Camera.cameraPositionAbsoluteX = general(random);
        Camera.cameraPositionAbsoluteY = general(random);
        Camera.cameraPositionAbsoluteZ = general(random);
        Camera.transitionStartX = general(random);
        Camera.transitionStartY = general(random);
        Camera.transitionStartZ = general(random);
        Camera.transitionStartYaw = general(random);
        Camera.transitionStartRoll = general(random);
        Camera.camAngleX = FLOATS[random.nextInt(FLOATS.length)];
        Camera.camAngleY = FLOATS[random.nextInt(FLOATS.length)];
        var cl = Client.class;
        setInt(cl, "cameraTargetAbsX", general(random));
        setInt(cl, "cameraTargetAbsY", general(random));
        setInt(cl, "cameraMaxZoom", general(random));
        setInt(cl, "cameraPositionPathIndex", pickFrom(random, PATHS));
        setInt(cl, "cameraLookAtPathIndex", pickFrom(random, PATHS));
        setInt(cl, "cameraPositionKeyframe", general(random));
        setInt(cl, "cameraLookAtKeyframe", general(random));
        setInt(cl, "cameraSplineProgress", general(random));
        setInt(cl, "cameraSplineSpeedStart", general(random));
        setInt(cl, "cameraSplineSpeedEnd", general(random));
        setFloat(cl, "cameraPitchVelocity", FLOATS[random.nextInt(FLOATS.length)]);
        setFloat(cl, "cameraYawVelocity", FLOATS[random.nextInt(FLOATS.length)]);
        setBool(cl, "cameraPitchVelocityApplied", random.nextBoolean());
        setBool(cl, "cameraYawVelocityApplied", random.nextBoolean());
        setBool(cl, "sendCameraAngleToServer", random.nextBoolean());
        setInt(cl, "lastCameraSmoothCycle", general(random));
        setInt(cl, "cycles", general(random));
        var shaking = (boolean[]) getObj(cl, "CAM_SHAKING");
        for (int i = 0; i < 5; i++) shaking[i] = random.nextBoolean();
        setShort(cl, "viewportFovMin", SHORTS[random.nextInt(SHORTS.length)]);
        setShort(cl, "viewportFovMax", SHORTS[random.nextInt(SHORTS.length)]);
        setShort(cl, "cameraZoomMin", SHORTS[random.nextInt(SHORTS.length)]);
        setShort(cl, "cameraZoomMax", SHORTS[random.nextInt(SHORTS.length)]);
        setShort(cl, "fovScaleMin", SHORTS[random.nextInt(SHORTS.length)]);
        setShort(cl, "fovScaleMax", SHORTS[random.nextInt(SHORTS.length)]);
        setShort(cl, "aspectFovMin", SHORTS[random.nextInt(SHORTS.length)]);
        setShort(cl, "aspectFovMax", SHORTS[random.nextInt(SHORTS.length)]);
        var paths = (Object[]) getObj(cl, "cameraSplinePaths");
        for (int i = 0; i < 2; i++) paths[i] = random.nextInt(6) == 0 ? null : randomSpline(random);
        setInt(PingRequester.class, "cameraTargetAbsX", general(random));
        StaticMethods.cameraTargetAbsY = general(random);
        StaticMethods.worldMapViewWidth = general(random);
        setInt(ComponentMinimap.class, "mapViewportScale", general(random));
        setInt(ComponentMinimap.class, "mapAssetLoadingPercentage", random.nextInt(3) == 0 ? 100 : general(random));
        setFloat(WorldMap.class, "requestedMapZoom", ZOOMS[random.nextInt(ZOOMS.length)]);
        setInt(WorldMap.class, "mapSquareAbsX", general(random));
        setInt(WorldMap.class, "mapSquareAbsY", general(random));
        if (random.nextInt(3) == 0) {
            setObj(WorldMap.class, "staticElement", null);
        } else {
            var element = (WorldMapElement) unsafe.allocateInstance(WorldMapElement.class);
            putInt(element, "elementId", general(random));
            setObj(WorldMap.class, "staticElement", element);
        }
        setInt(WorldMapDisplay.class, "mapCenterX", general(random));
        setInt(WorldMapDisplay.class, "mapCenterY", general(random));
        setBool(WorldMapDisplay.class, "hideIcons", random.nextBoolean());
        var elements = (IterableNodeMap<Node<Object>>) getObj(WorldMapDisplay.class, "hiddenMecIds");
        var categories = (IterableNodeMap<Node<Object>>) getObj(WorldMapDisplay.class, "hiddenCategoryIds");
        elements.clear();
        categories.clear();
        for (int id : POOL) {
            if (random.nextInt(3) == 0) elements.put(new Node<Object>() {}, (long) id);
            if (random.nextInt(3) == 0) categories.put(new Node<Object>() {}, (long) id);
        }
        var region = (MapRegion) unsafe.allocateInstance(MapRegion.class);
        var coords = new CoordGrid();
        coords.x = pickFrom(random, BASES);
        coords.y = pickFrom(random, BASES);
        region.baseCoordinates = coords;
        putInt(region, "sceneSizeX", pickFrom(random, SIZES));
        putInt(region, "sceneSizeY", pickFrom(random, SIZES));
        Resource.mapRegion = region;
    }

    static void ints(StringBuilder sb, int... values) {
        for (int v : values) sb.append(v).append(',');
    }

    static String splines(Object[] paths) {
        var sb = new StringBuilder();
        for (int p = 0; p < 2; p++) {
            if (p > 0) sb.append('|');
            var path = (int[][]) paths[p];
            if (path == null) {
                sb.append('n');
                continue;
            }
            sb.append('a');
            for (int i = 0; i < path.length; i++) {
                if (i > 0) sb.append(';');
                var row = path[i];
                for (int j = 0; j < row.length; j++) {
                    if (j > 0) sb.append(':');
                    sb.append(row[j]);
                }
            }
        }
        return sb.toString();
    }

    static String hidden(IterableNodeMap<Node<Object>> map) {
        var sb = new StringBuilder();
        for (int id : POOL) {
            if (map.get((long) id) != null) {
                if (sb.length() > 0) sb.append(';');
                sb.append(id);
            }
        }
        return sb.length() == 0 ? "-" : sb.toString();
    }

    static String digest() throws Exception {
        var cl = Client.class;
        var sb = new StringBuilder();
        ints(sb, Camera.cameraCalculationType, Camera.cameraYaw, Camera.cameraRoll, Camera.cameraPitch, Camera.moveCameraSpeed, Camera.moveCameraAngle,
            Camera.cameraMovementLocalX, Camera.cameraMovementLocalY, Camera.cameraMovementZ, Camera.cameraConstantMoveSpeed, Camera.cameraVariableMoveSpeed,
            Camera.cameraPositionAbsoluteX, Camera.cameraPositionAbsoluteY, Camera.cameraPositionAbsoluteZ, Camera.transitionStartX, Camera.transitionStartY,
            Camera.transitionStartZ, Camera.transitionStartYaw, Camera.transitionStartRoll, Float.floatToRawIntBits(Camera.camAngleX), Float.floatToRawIntBits(Camera.camAngleY),
            getInt(cl, "cameraTargetAbsX"), getInt(cl, "cameraTargetAbsY"), getInt(PingRequester.class, "cameraTargetAbsX"), StaticMethods.cameraTargetAbsY,
            getInt(cl, "cameraMaxZoom"), getInt(cl, "cameraBaseZoom"), getInt(cl, "cameraPositionPathIndex"), getInt(cl, "cameraLookAtPathIndex"),
            getInt(cl, "cameraPositionKeyframe"), getInt(cl, "cameraLookAtKeyframe"), getInt(cl, "cameraSplineProgress"), getInt(cl, "cameraSplineSpeedStart"),
            getInt(cl, "cameraSplineSpeedEnd"), Float.floatToRawIntBits(getFloat(cl, "cameraPitchVelocity")), Float.floatToRawIntBits(getFloat(cl, "cameraYawVelocity")),
            getBool(cl, "cameraPitchVelocityApplied") ? 1 : 0, getBool(cl, "cameraYawVelocityApplied") ? 1 : 0, getBool(cl, "sendCameraAngleToServer") ? 1 : 0,
            getInt(cl, "lastCameraSmoothCycle"), getInt(cl, "cycles"));
        var shaking = (boolean[]) getObj(cl, "CAM_SHAKING");
        for (boolean b : shaking) sb.append(b ? 1 : 0).append(',');
        ints(sb, getShort(cl, "viewportFovMin"), getShort(cl, "viewportFovMax"), getShort(cl, "cameraZoomMin"), getShort(cl, "cameraZoomMax"),
            getShort(cl, "fovScaleMin"), getShort(cl, "fovScaleMax"), getShort(cl, "aspectFovMin"), getShort(cl, "aspectFovMax"),
            Resource.mapRegion.baseCoordinates.x, Resource.mapRegion.baseCoordinates.y, readInt(Resource.mapRegion, "sceneSizeX"), readInt(Resource.mapRegion, "sceneSizeY"));
        sb.setLength(sb.length() - 1);
        sb.append('|');
        var element = (WorldMapElement) getObj(WorldMap.class, "staticElement");
        ints(sb, getBool(WorldMapDisplay.class, "hideIcons") ? 1 : 0, Float.floatToRawIntBits(getFloat(WorldMap.class, "requestedMapZoom")),
            getInt(WorldMap.class, "mapSquareAbsX"), getInt(WorldMap.class, "mapSquareAbsY"), getInt(WorldMapDisplay.class, "mapCenterX"), getInt(WorldMapDisplay.class, "mapCenterY"),
            getInt(ComponentMinimap.class, "mapAssetLoadingPercentage"), StaticMethods.worldMapViewWidth, getInt(ComponentMinimap.class, "mapViewportScale"));
        sb.append(element == null ? "n" : Integer.toString(element.elementId)).append('|');
        sb.append(hidden((IterableNodeMap<Node<Object>>) getObj(WorldMapDisplay.class, "hiddenMecIds"))).append('|');
        sb.append(hidden((IterableNodeMap<Node<Object>>) getObj(WorldMapDisplay.class, "hiddenCategoryIds"))).append('|');
        sb.append(splines((Object[]) getObj(cl, "cameraSplinePaths")));
        return sb.toString();
    }

    static int argCount(String op) {
        return switch (op) {
            case "CAM_MOVETO" -> 4;
            case "CAM_MOVEALONG" -> 6;
            case "CAM_SETFOLLOWHEIGHT", "CAM_FOLLOWCOORD", "COORD_CLAMPTOSCENE_ORDEFAULT", "SPLINE_LENGTH", "WORLDMAP_DISABLEELEMENTS",
                "WORLDMAP_GETDISABLEELEMENT", "WORLDMAP_GETDISABLEELEMENTCATEGORY" -> 1;
            case "SPLINE_NEW", "WORLDMAP_DISABLEELEMENT", "WORLDMAP_DISABLEELEMENTCATEGORY", "VIEWPORT_SETFOV", "VIEWPORT_SETZOOM" -> 2;
            case "SPLINE_ADDPOINT" -> 7;
            case "VIEWPORT_CLAMPFOV" -> 4;
            default -> 0;
        };
    }

    static int[] arguments(String op, Random random) {
        int n = argCount(op);
        var a = new int[n];
        for (int i = 0; i < n; i++) a[i] = general(random);
        switch (op) {
            case "CAM_MOVETO" -> {
                a[0] = random.nextBoolean() ? pickFrom(random, COORDS) : (random.nextInt(0x4000) << 14) | random.nextInt(0x4000);
                a[1] = random.nextInt(3) == 0 ? general(random) : random.nextInt(600) - 100;
            }
            case "CAM_MOVEALONG" -> {
                a[0] = pickFrom(random, PATHS);
                a[1] = pickFrom(random, KEYFRAMES);
                a[4] = pickFrom(random, PATHS);
                a[5] = pickFrom(random, KEYFRAMES);
                if (random.nextInt(10) < 7) {
                    a[0] = random.nextInt(2);
                    a[1] = random.nextInt(5) - 1;
                    a[4] = random.nextInt(2);
                    a[5] = random.nextInt(5) - 1;
                }
            }
            case "CAM_FOLLOWCOORD", "COORD_CLAMPTOSCENE_ORDEFAULT" -> a[0] = random.nextBoolean() ? pickFrom(random, COORDS) : (random.nextInt(0x4000) << 14) | random.nextInt(0x4000);
            case "SPLINE_LENGTH" -> a[0] = random.nextInt(10) < 6 ? random.nextInt(2) : pickFrom(random, PATHS);
            case "SPLINE_NEW" -> {
                a[0] = pickFrom(random, PATHS);
                a[1] = pickFrom(random, COUNTS);
            }
            case "SPLINE_ADDPOINT" -> {
                a[0] = pickFrom(random, PATHS);
                a[1] = pickFrom(random, KEYFRAMES);
                a[2] = random.nextBoolean() ? pickFrom(random, COORDS) : (random.nextInt(0x4000) << 14) | random.nextInt(0x4000);
                a[4] = random.nextBoolean() ? pickFrom(random, COORDS) : (random.nextInt(0x4000) << 14) | random.nextInt(0x4000);
            }
            case "WORLDMAP_DISABLEELEMENTS" -> a[0] = random.nextInt(4) - 1;
            case "WORLDMAP_DISABLEELEMENT", "WORLDMAP_DISABLEELEMENTCATEGORY" -> {
                a[0] = random.nextInt(4) == 0 ? general(random) : pickFrom(random, POOL);
                a[1] = random.nextInt(4) - 1;
            }
            case "WORLDMAP_GETDISABLEELEMENT", "WORLDMAP_GETDISABLEELEMENTCATEGORY" -> a[0] = random.nextInt(4) == 0 ? general(random) : pickFrom(random, POOL);
            default -> {
            }
        }
        return a;
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

    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        var theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        unsafe = (sun.misc.Unsafe) theUnsafe.get(null);
        var random = new Random(SEED);
        var out = new StringBuilder();
        var stats = new TreeMap<String, int[]>();
        var names = new ArrayList<String>();
        names.addAll(Arrays.asList(QUICK));
        names.addAll(Arrays.asList(CAMERA));
        int total = 0;
        for (var name : names) {
            var op = ClientScriptOpCode.valueOf(name);
            boolean quick = Arrays.asList(QUICK).contains(name);
            for (int n = 0; n < PER_OP; n++) {
                install(random);
                var given = arguments(name, random);
                if (given.length > 0 && random.nextInt(12) == 0) given = Arrays.copyOf(given, random.nextInt(given.length));
                var e = new CS2Executor();
                Arrays.fill(e.intStack, 0x5a5a5a5a);
                e.intStackPtr = 0;
                e.intOpValues = new int[] {0};
                e.instrPtr = 0;
                for (int v : given) e.intStack[e.intStackPtr++] = v;
                int initial = e.intStackPtr;
                var before = digest();
                String result = "ok";
                try {
                    if (quick) QuickChatDisplayOps.dispatch(op, e);
                    else CameraLoginPrefOps.dispatch(op, e);
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
                    .append(result.equals("ok") ? stack(e.intStack, e.intStackPtr) : "-").append('\t').append(digest()).append('\n');
                stats.computeIfAbsent(name, k -> new int[2])[result.equals("ok") ? 0 : 1]++;
                total++;
            }
        }
        out.append("Z\t").append(total).append('\n');
        Files.writeString(output.resolve("world.native.tsv"), out.toString(), StandardCharsets.UTF_8);
        var summary = new StringBuilder();
        for (var entry : stats.entrySet()) summary.append(entry.getKey()).append('\t').append(entry.getValue()[0]).append('\t').append(entry.getValue()[1]).append('\n');
        Files.writeString(output.resolve("world.stats.tsv"), summary.toString(), StandardCharsets.UTF_8);
        System.exit(0);
    }
}
