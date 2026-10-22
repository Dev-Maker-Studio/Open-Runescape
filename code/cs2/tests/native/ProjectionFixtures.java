import com.jagex.game.runetek5.client.Client;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.*;
import com.jagex.game.runetek5.entity.gameobject.StationaryInteractiveObject;
import com.jagex.game.runetek5.entity.gameobject.StationaryWall;
import com.jagex.game.runetek5.entity.grounditem.GroundItem;
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity;
import com.jagex.game.runetek5.graphics.hint.HintRenderer;
import com.jagex.game.runetek5.math.Matrix43;
import com.jagex.game.runetek5.math.Matrix44;
import com.jagex.game.runetek5.movement.CoordinateSpace;
import com.jagex.game.runetek5.rendering.Renderers;
import com.jagex.game.runetek5.toolkit.ModernGLToolkit;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class ProjectionFixtures {
    static final long SEED = 0x727c5228L;
    static final int PER_OP = 2500;
    static final String[] NAMES43 = {"xA", "xB", "xC", "xD", "yA", "yB", "yC", "yD", "zA", "zB", "zC", "zD"};
    static final short[] SHORTS = {0, 1, -1, 100, 200, 256, 300, 334, 400, 512, 640, 768, 1000, 1024, 1500, 2000, 3000, 32767, -32768};
    static final int[] SIZES = {765, 503, 1920, 1080, 1, 0, -5, 334, 100, 333, 800, 600, 3840, 2160, 335, 434, 435, 1 << 23, 1 << 24, Integer.MAX_VALUE, Integer.MIN_VALUE, 7, 20000, 5000};
    static final int[] OFFSETS = {0, 5, -5, 12, 100, -100, 512, 1000, 0x7fffffff, Integer.MIN_VALUE, -1, 24, 48};
    static final int[] PLANES = {0, 0, 0, 1, 1, 2, 3, -1, 4, 7, 127, -128, 100};
    static final float[] SPECIAL = {0f, -0f, 1f, -1f, 0.5f, 1e10f, -1e10f, 3e38f, -3e38f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 1e-30f, Float.MIN_VALUE, 512f, 4096f};
    static final String[] OPS = {"VIEWPORT_GETEFFECTIVESIZE", "GET_ENTITY_SCREEN_POSITION", "GET_LOC_SCREEN_POSITION", "GET_OBJ_SCREEN_POSITION"};
    static sun.misc.Unsafe unsafe;
    static String anchorText = "0,0,0,0,0,0";

    static int fbits(float value) {
        return Float.isNaN(value) ? 0x7fc00000 : Float.floatToRawIntBits(value);
    }

    static void putFloat(Object target, String name, float value) throws Exception {
        unsafe.putFloat(target, unsafe.objectFieldOffset(SceneFixtures.field(target.getClass(), name)), value);
    }

    static float getFloat(Object target, String name) throws Exception {
        return unsafe.getFloat(target, unsafe.objectFieldOffset(SceneFixtures.field(target.getClass(), name)));
    }

    static void putShort(Object target, String name, short value) throws Exception {
        unsafe.putShort(target, unsafe.objectFieldOffset(SceneFixtures.field(target.getClass(), name)), value);
    }

    static void putByte(Object target, String name, byte value) throws Exception {
        unsafe.putByte(target, unsafe.objectFieldOffset(SceneFixtures.field(target.getClass(), name)), value);
    }

    static void putBool(Object target, String name, boolean value) throws Exception {
        unsafe.putBoolean(target, unsafe.objectFieldOffset(SceneFixtures.field(target.getClass(), name)), value);
    }

    static void setStaticShort(Class<?> type, String name, short value) throws Exception {
        var f = SceneFixtures.field(type, name);
        unsafe.putShort(unsafe.staticFieldBase(f), unsafe.staticFieldOffset(f), value);
    }

    static short getStaticShort(Class<?> type, String name) throws Exception {
        var f = SceneFixtures.field(type, name);
        return unsafe.getShort(unsafe.staticFieldBase(f), unsafe.staticFieldOffset(f));
    }

    static void setStaticObj(Class<?> type, String name, Object value) throws Exception {
        var f = SceneFixtures.field(type, name);
        unsafe.putObject(unsafe.staticFieldBase(f), unsafe.staticFieldOffset(f), value);
    }

    static Object getStaticObj(Class<?> type, String name) throws Exception {
        var f = SceneFixtures.field(type, name);
        return unsafe.getObject(unsafe.staticFieldBase(f), unsafe.staticFieldOffset(f));
    }

    static float randomFloat(Random random) {
        return switch (random.nextInt(6)) {
            case 0 -> SPECIAL[random.nextInt(SPECIAL.length)];
            case 1 -> (float) (random.nextGaussian() * 1000);
            case 2 -> random.nextInt(8192) - 4096;
            case 3 -> Float.intBitsToFloat(random.nextInt());
            case 4 -> (float) (random.nextGaussian() * 2);
            default -> random.nextFloat();
        };
    }

    static float[] randomView(Random random) {
        var out = new float[12];
        int mode = random.nextInt(10);
        if (mode < 6) {
            double yaw = random.nextDouble() * 6.283185307179586;
            double pitch = random.nextDouble() * 1.5;
            float c = (float) Math.cos(yaw), s = (float) Math.sin(yaw), cp = (float) Math.cos(pitch), sp = (float) Math.sin(pitch);
            out[0] = c;
            out[1] = 0f;
            out[2] = -s;
            out[4] = s * sp;
            out[5] = cp;
            out[6] = c * sp;
            out[8] = s * cp;
            out[9] = -sp;
            out[10] = c * cp;
            out[3] = random.nextInt(8000) - 4000;
            out[7] = random.nextInt(8000) - 4000;
            out[11] = random.nextInt(8000) - 1000;
        } else if (mode < 7) {
            out[0] = 1f;
            out[5] = 1f;
            out[10] = 1f;
            out[3] = randomFloat(random);
            out[7] = randomFloat(random);
            out[11] = randomFloat(random);
        } else {
            for (int i = 0; i < 12; i++) out[i] = randomFloat(random);
        }
        return out;
    }

    static float[] randomModel(Random random) {
        var out = new float[16];
        int mode = random.nextInt(10);
        if (mode < 7) {
            out[0] = 1f;
            out[5] = 1f;
            out[10] = 1f;
            out[15] = 1f;
        } else if (mode < 8) {
            out[0] = 1f;
            out[5] = 1f;
            out[10] = 1f;
            out[15] = 1f;
            out[12] = randomFloat(random);
            out[13] = randomFloat(random);
            out[14] = randomFloat(random);
        } else {
            for (int i = 0; i < 16; i++) out[i] = randomFloat(random);
        }
        return out;
    }

    static void setM43(Matrix43 target, float[] values) throws Exception {
        for (int i = 0; i < 12; i++) putFloat(target, NAMES43[i], values[i]);
    }

    static void m43Bits(Matrix43 source, List<Integer> out) throws Exception {
        for (int i = 0; i < 12; i++) out.add(fbits(getFloat(source, NAMES43[i])));
    }

    static void m44Bits(Matrix44 source, List<Integer> out) {
        for (int i = 0; i < 16; i++) out.add(fbits(source.getElements()[i]));
    }

    static ModernGLToolkit toolkit() throws Exception {
        var toolkit = (ModernGLToolkit) unsafe.allocateInstance(ModernGLToolkit.class);
        putObj(toolkit, "viewMatrix43", new Matrix43());
        putObj(toolkit, "viewMatrix44", new Matrix44());
        putObj(toolkit, "inverseSunMatrix44", new Matrix44());
        putObj(toolkit, "currentModelMatrix", new Matrix44());
        putObj(toolkit, "viewProjectionMatrix", new Matrix44());
        putObj(toolkit, "inverseSunMatrix43", new Matrix43());
        putObj(toolkit, "frustumPlanes", new float[6][4]);
        SceneFixtures.putInt(toolkit, "depthClippingMode", 1);
        return toolkit;
    }

    static void putObj(Object target, String name, Object value) throws Exception {
        SceneFixtures.putObj(target, name, value);
    }

    static String describeAnchor(int kind, Object entity) throws Exception {
        return anchorText;
    }

    static void install(Random random, ModernGLToolkit toolkit, int sceneId, String name, Object[] holder) throws Exception {
        SceneFixtures.install(random, sceneId);
        for (var field : new String[] {"viewportFovMin", "viewportFovMax", "fovScaleMin", "fovScaleMax", "aspectFovMin", "aspectFovMax"}) {
            short value = random.nextInt(3) == 0 ? (short) (random.nextInt(3200) - 100) : SHORTS[random.nextInt(SHORTS.length)];
            setStaticShort(Client.class, field, value);
        }
        if (random.nextInt(4) == 0) {
            setStaticShort(Client.class, "viewportFovMin", (short) (100 + random.nextInt(500)));
            setStaticShort(Client.class, "viewportFovMax", (short) (600 + random.nextInt(1400)));
            setStaticShort(Client.class, "aspectFovMin", (short) (200 + random.nextInt(400)));
            setStaticShort(Client.class, "aspectFovMax", (short) (700 + random.nextInt(1000)));
        }
        SceneFixtures.setStaticInt(Client.class, "hintArrowOffsetX", SceneFixtures.pickFrom(random, OFFSETS));
        SceneFixtures.setStaticInt(Client.class, "hintArrowOffsetY", SceneFixtures.pickFrom(random, OFFSETS));
        SceneFixtures.setStaticInt(Client.class, "gameAreaWidth", SceneFixtures.general(random));
        SceneFixtures.setStaticInt(Client.class, "gameAreaHeight", SceneFixtures.general(random));
        SceneFixtures.setStaticInt(Client.class, "viewportScaledFov", SceneFixtures.general(random));
        SceneFixtures.setStaticBool(Client.class, "smoothEntityMovement", false);
        var screen = SceneFixtures.field(Client.class, "GAME_SCREEN_INTERFACE");
        if (random.nextInt(12) == 0) {
            setStaticObj(Client.class, "GAME_SCREEN_INTERFACE", null);
        } else {
            var component = unsafe.allocateInstance(screen.getType());
            SceneFixtures.putInt(component, "width", random.nextInt(3) == 0 ? SceneFixtures.pickFrom(random, SIZES) : 100 + random.nextInt(2000));
            SceneFixtures.putInt(component, "height", random.nextInt(3) == 0 ? SceneFixtures.pickFrom(random, SIZES) : 100 + random.nextInt(1200));
            setStaticObj(Client.class, "GAME_SCREEN_INTERFACE", component);
        }
        var hint = (float[]) getStaticObj(Client.class, "HINT_POSITION");
        for (int i = 0; i < 3; i++) hint[i] = randomFloat(random);
        var model = toolkit.currentModelMatrix;
        var modelValues = randomModel(random);
        System.arraycopy(modelValues, 0, model.getElements(), 0, 16);
        float[] origin = {randomFloat(random), randomFloat(random), random.nextBoolean() ? 382.5f : randomFloat(random), random.nextBoolean() ? 251.5f : randomFloat(random)};
        putFloat(toolkit, "viewportOriginX", origin[0]);
        putFloat(toolkit, "viewportOriginY", origin[1]);
        putFloat(toolkit, "imageProducerCenterX", origin[2]);
        putFloat(toolkit, "imageProducerCenterY", origin[3]);
        var view = new Matrix43();
        setM43(view, randomView(random));
        toolkit.setViewMatrix(view);
        if (random.nextInt(5) == 0) {
            for (int i = 0; i < 16; i++) {
                toolkit.viewMatrix44.getElements()[i] = randomFloat(random);
                toolkit.viewProjectionMatrix.getElements()[i] = randomFloat(random);
            }
        }
        var sun = new Matrix43();
        setM43(sun, randomView(random));
        ((Matrix43) getStaticObj(Client.class, "SUN_MATRIX")).copyFrom(sun);
        Renderers.activeToolkit = toolkit;
        anchorText = "0,0,0,0,0,0";
        var exec = (CS2Executor) holder[0];
        exec.currentEntity = null;
        exec.currentLocation = null;
        exec.currentGroundItem = null;
        if (name.startsWith("VIEWPORT") || random.nextInt(25) == 0) return;
        switch (name) {
            case "GET_ENTITY_SCREEN_POSITION" -> {
                var npc = (NpcEntity) unsafe.allocateInstance(NpcEntity.class);
                float x = pointCoordinate(random, SceneFixtures.SCENE_DATA, sceneId, true);
                float z = pointCoordinate(random, SceneFixtures.SCENE_DATA, sceneId, false);
                var matrix = new Matrix43();
                putFloat(matrix, "xD", x);
                putFloat(matrix, "zD", z);
                putObj(npc, "_projectionMatrix", matrix);
                putBool(npc, "projectionUpdate", false);
                byte plane = planeByte(random);
                putByte(npc, "plane", plane);
                exec.currentEntity = npc;
                anchorText = "1," + fbits(x) + "," + fbits(z) + "," + plane + ",0,0";
            }
            case "GET_LOC_SCREEN_POSITION" -> {
                byte plane = planeByte(random);
                if (random.nextBoolean()) {
                    var object = (StationaryInteractiveObject) unsafe.allocateInstance(StationaryInteractiveObject.class);
                    var d = (SceneFixtures.Definition) SceneFixtures.SCENE_DATA.get(sceneId)[0];
                    short startX = footprint(random, d.sizeX);
                    short startZ = footprint(random, d.sizeY);
                    short endX = random.nextInt(8) == 0 ? footprint(random, d.sizeX) : (short) (startX + random.nextInt(4));
                    short endZ = random.nextInt(8) == 0 ? footprint(random, d.sizeY) : (short) (startZ + random.nextInt(4));
                    putShort(object, "startX", startX);
                    putShort(object, "endX", endX);
                    putShort(object, "startZ", startZ);
                    putShort(object, "endZ", endZ);
                    putByte(object, "plane", plane);
                    exec.currentLocation = object;
                    anchorText = "2," + startX + "," + endX + "," + startZ + "," + endZ + "," + plane;
                } else {
                    var wall = (StationaryWall) unsafe.allocateInstance(StationaryWall.class);
                    anchorText = absolute(random, wall, sceneId, plane);
                    exec.currentLocation = wall;
                }
            }
            default -> {
                var item = (GroundItem) unsafe.allocateInstance(GroundItem.class);
                anchorText = absolute(random, item, sceneId, planeByte(random));
                exec.currentGroundItem = item;
            }
        }
    }

    static String absolute(Random random, Object node, int sceneId, byte plane) throws Exception {
        float x = pointCoordinate(random, SceneFixtures.SCENE_DATA, sceneId, true);
        float z = pointCoordinate(random, SceneFixtures.SCENE_DATA, sceneId, false);
        var space = new CoordinateSpace();
        space.coords.x = x;
        space.coords.z = z;
        putObj(node, "_absolutePosition", space);
        putBool(node, "calculateAbsolutePosition", false);
        putByte(node, "plane", plane);
        return "1," + fbits(x) + "," + fbits(z) + "," + plane + ",0,0";
    }

    static short footprint(Random random, int size) {
        if (random.nextInt(10) == 0) return SHORTS[random.nextInt(SHORTS.length)];
        return (short) (random.nextInt(size + 4) - 2);
    }

    static byte planeByte(Random random) {
        int roll = random.nextInt(8);
        return (byte) (roll == 0 ? random.nextInt(256) : SceneFixtures.pickFrom(random, PLANES));
    }

    static float pointCoordinate(Random random, List<Object[]> data, int sceneId, boolean horizontal) {
        var d = (SceneFixtures.Definition) data.get(sceneId)[0];
        int size = horizontal ? d.sizeX : d.sizeY;
        int roll = random.nextInt(10);
        if (roll < 6) return random.nextInt(size * 512);
        if (roll < 7) return random.nextInt(size * 512) + random.nextFloat();
        if (roll < 8) return random.nextInt(size * 512 + 4096) - 2048;
        return randomFloat(random);
    }

    static String digest(int sceneId, ModernGLToolkit toolkit) throws Exception {
        var out = new ArrayList<Integer>();
        for (var field : new String[] {"viewportFovMin", "viewportFovMax", "fovScaleMin", "fovScaleMax", "aspectFovMin", "aspectFovMax"}) {
            out.add((int) getStaticShort(Client.class, field));
        }
        for (var field : new String[] {"hintArrowOffsetX", "hintArrowOffsetY", "gameAreaWidth", "gameAreaHeight", "viewportScaledFov"}) {
            out.add(SceneFixtures.getStaticInt(Client.class, field));
        }
        var screen = getStaticObj(Client.class, "GAME_SCREEN_INTERFACE");
        if (screen == null) {
            out.add(0);
            out.add(0);
            out.add(0);
        } else {
            out.add(1);
            out.add(unsafe.getInt(screen, unsafe.objectFieldOffset(SceneFixtures.field(screen.getClass(), "width"))));
            out.add(unsafe.getInt(screen, unsafe.objectFieldOffset(SceneFixtures.field(screen.getClass(), "height"))));
        }
        out.add(fbits(getFloat(toolkit, "viewportOriginX")));
        out.add(fbits(getFloat(toolkit, "viewportOriginY")));
        out.add(fbits(getFloat(toolkit, "imageProducerCenterX")));
        out.add(fbits(getFloat(toolkit, "imageProducerCenterY")));
        for (float value : (float[]) getStaticObj(Client.class, "HINT_POSITION")) out.add(fbits(value));
        m43Bits(toolkit.viewMatrix43, out);
        m44Bits(toolkit.viewMatrix44, out);
        m44Bits(toolkit.currentModelMatrix, out);
        m44Bits(toolkit.viewProjectionMatrix, out);
        m43Bits((Matrix43) getStaticObj(Client.class, "SUN_MATRIX"), out);
        var sb = new StringBuilder().append(sceneId).append('|');
        for (int i = 0; i < out.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(out.get(i));
        }
        return sb.append(',').append(anchorText).toString();
    }

    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        var theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        unsafe = (sun.misc.Unsafe) theUnsafe.get(null);
        SceneFixtures.unsafe = unsafe;
        var random = new Random(SEED);
        var out = new StringBuilder();
        for (int id = 0; id < 10; id++) {
            var d = SceneFixtures.define(random, id);
            SceneFixtures.SCENE_DATA.add(new Object[] {d});
            out.append(SceneFixtures.sceneLine(id, d)).append('\n');
        }
        int total = 0;
        var stats = new TreeMap<String, int[]>();
        var toolkit = toolkit();
        for (var name : OPS) {
            var op = ClientScriptOpCode.valueOf(name);
            for (int n = 0; n < PER_OP; n++) {
                int sceneId = random.nextInt(10);
                if (random.nextInt(3) != 0 && sceneId > 1) sceneId = random.nextInt(2);
                var e = new CS2Executor();
                Arrays.fill(e.intStack, 0x5a5a5a5a);
                e.intStackPtr = 0;
                e.intOpValues = new int[] {0};
                e.instrPtr = 0;
                install(random, toolkit, sceneId, name, new Object[] {e});
                int[] given = name.startsWith("VIEWPORT") ? new int[0] : new int[] {random.nextInt(3) == 0 ? SceneFixtures.general(random) : random.nextInt(400) - 100};
                if (!name.startsWith("VIEWPORT") && random.nextInt(14) == 0) given = new int[0];
                for (int v : given) e.intStack[e.intStackPtr++] = v;
                var before = digest(sceneId, toolkit);
                String result = "ok";
                try {
                    if (name.startsWith("VIEWPORT")) CameraLoginPrefOps.dispatch(op, e);
                    else DetailCapabilityOps.dispatch(op, e);
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
                    .append(result.equals("ok") ? SceneFixtures.stack(e.intStack, e.intStackPtr) : "-").append('\t').append(digest(sceneId, toolkit)).append('\n');
                stats.computeIfAbsent(name + "\t" + sceneId, k -> new int[2])[result.equals("ok") ? 0 : 1]++;
                total++;
            }
        }
        out.append("Z\t").append(total).append('\n');
        Files.writeString(output.resolve("projection.native.tsv"), out.toString(), StandardCharsets.UTF_8);
        var summary = new StringBuilder();
        for (var entry : stats.entrySet()) summary.append(entry.getKey()).append('\t').append(entry.getValue()[0]).append('\t').append(entry.getValue()[1]).append('\n');
        Files.writeString(output.resolve("projection.stats.tsv"), summary.toString(), StandardCharsets.UTF_8);
        System.exit(0);
    }
}
