import com.jagex.game.runetek5.client.Client;
import com.jagex.game.runetek5.client.input.camera.Camera;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.movement.CoordinateSpace;
import com.jagex.game.runetek5.scene.MapRegion;
import com.jagex.game.runetek5.scene.Scene;
import com.jagex.game.runetek5.scene.SceneGraph;
import com.jagex.game.runetek5.scene.SettingsBits;
import com.jagex.game.runetek5.scene.ground.Ground;
import com.jagex.game.runetek5.scene.ground.GroundSM;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class CameraOrbitOracle {
    static final long SEED = 0x0c4a3e72L;
    static final int CASES = 60;
    static final int FRAMES = 90;
    static final int SCRIPTS = 4;
    static final int SCRIPT_FRAMES = 24;
    static final int SIZE = 104;
    static final int[] AREA_HEIGHTS = {0, 200, 334, 380, 434, 503, 720, 1080};
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
        throw new NoSuchFieldException(type.getName() + "." + name);
    }

    static void putInt(Object target, String name, int value) throws Exception {
        unsafe.putInt(target, unsafe.objectFieldOffset(field(target.getClass(), name)), value);
    }

    static void putObj(Object target, String name, Object value) throws Exception {
        unsafe.putObject(target, unsafe.objectFieldOffset(field(target.getClass(), name)), value);
    }

    static void putBool(Object target, String name, boolean value) throws Exception {
        unsafe.putBoolean(target, unsafe.objectFieldOffset(field(target.getClass(), name)), value);
    }

    static void putShort(Object target, String name, short value) throws Exception {
        unsafe.putShort(target, unsafe.objectFieldOffset(field(target.getClass(), name)), value);
    }

    static void setStaticInt(Class<?> type, String name, int value) throws Exception {
        var f = field(type, name);
        unsafe.putInt(unsafe.staticFieldBase(f), unsafe.staticFieldOffset(f), value);
    }

    static void setStaticFloat(Class<?> type, String name, float value) throws Exception {
        var f = field(type, name);
        unsafe.putFloat(unsafe.staticFieldBase(f), unsafe.staticFieldOffset(f), value);
    }

    static void setStaticBool(Class<?> type, String name, boolean value) throws Exception {
        var f = field(type, name);
        unsafe.putBoolean(unsafe.staticFieldBase(f), unsafe.staticFieldOffset(f), value);
    }

    static void setStaticShort(Class<?> type, String name, short value) throws Exception {
        var f = field(type, name);
        unsafe.putShort(unsafe.staticFieldBase(f), unsafe.staticFieldOffset(f), value);
    }

    static void setStaticObj(Class<?> type, String name, Object value) throws Exception {
        var f = field(type, name);
        unsafe.putObject(unsafe.staticFieldBase(f), unsafe.staticFieldOffset(f), value);
    }

    static int getStaticInt(Class<?> type, String name) throws Exception {
        var f = field(type, name);
        return unsafe.getInt(unsafe.staticFieldBase(f), unsafe.staticFieldOffset(f));
    }

    static float getStaticFloat(Class<?> type, String name) throws Exception {
        var f = field(type, name);
        return unsafe.getFloat(unsafe.staticFieldBase(f), unsafe.staticFieldOffset(f));
    }

    static int mix(int plane, int x, int y) {
        int v = (x * 73856093) ^ (y * 19349663) ^ ((plane + 1) * 83492791);
        v ^= v >>> 13;
        v *= 0x5bd1e995;
        v ^= v >>> 15;
        return v;
    }

    static int heightAt(int plane, int x, int y) {
        int v = mix(plane, x, y);
        if ((v >>> 28) == 0) return ((v & 0x7fffffff) % 3000) - 2400;
        return (v & 0xff) - 128;
    }

    static byte lightingAt(int plane, int x, int y) {
        int v = mix(plane + 7, x, y);
        return ((v >>> 20) & 3) == 0 ? (byte) ((v >>> 8) & 0x3f) : 0;
    }

    static boolean bridgeAt(int x, int y) {
        return (mix(11, x, y) >>> 5) % 9 == 0;
    }

    static void install() throws Exception {
        setStaticShort(Client.class, "cameraZoomMin", (short) 256);
        setStaticShort(Client.class, "cameraZoomMax", (short) 320);
        for (var type : new Class<?>[] {Camera.class, PlayerUpdate.class, Resource.class, MapRegion.class}) {
            Class.forName(type.getName(), true, type.getClassLoader());
        }
        var heightmap = new Ground[4];
        var lighting = new byte[4][][];
        for (int plane = 0; plane < 4; plane++) {
            var heights = new int[SIZE + 1][SIZE + 1];
            for (int x = 0; x <= SIZE; x++) {
                for (int y = 0; y <= SIZE; y++) heights[x][y] = heightAt(plane, x, y);
            }
            var ground = (GroundSM) unsafe.allocateInstance(GroundSM.class);
            putInt(ground, "width", SIZE);
            putInt(ground, "length", SIZE);
            putObj(ground, "tileHeights", heights);
            putInt(ground, "tileUnits", 512);
            putInt(ground, "tileScale", 9);
            heightmap[plane] = ground;
            lighting[plane] = new byte[SIZE + 1][SIZE + 1];
            for (int x = 0; x <= SIZE; x++) {
                for (int y = 0; y <= SIZE; y++) lighting[plane][x][y] = lightingAt(plane, x, y);
            }
        }
        var bits = new SettingsBits(SIZE, SIZE);
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) bits.tileFunctionMasks[1][x][y] = (byte) (bridgeAt(x, y) ? 2 : 0);
        }
        var scene = (Scene) unsafe.allocateInstance(Scene.class);
        putObj(scene, "localHeightmap", heightmap);
        putInt(scene, "sceneDimensionX", SIZE);
        putInt(scene, "sceneDimensionY", SIZE);
        var graph = (SceneGraph) unsafe.allocateInstance(SceneGraph.class);
        putObj(graph, "lighting", lighting);
        var region = (MapRegion) unsafe.allocateInstance(MapRegion.class);
        putObj(region, "scene", scene);
        putInt(region, "sceneSizeX", SIZE);
        putInt(region, "sceneSizeY", SIZE);
        putObj(region, "tileSettingsBits", bits);
        putObj(region, "surfaceSceneGraph", graph);
        setStaticObj(Resource.class, "mapRegion", region);
    }

    static Method pitchOp(String name) throws Exception {
        for (var m : Class.forName("com.jagex.game.runetek5.staticmethods.StaticMethods").getDeclaredMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == 0) return m;
        }
        throw new NoSuchMethodException(name);
    }

    public static void main(String[] args) throws Exception {
        var unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        unsafe = (sun.misc.Unsafe) unsafeField.get(null);
        Path out = Paths.get(args[0]);
        Files.createDirectories(out);
        install();
        var player = (PlayerEntity) unsafe.allocateInstance(PlayerEntity.class);
        var space = new CoordinateSpace();
        putObj(player, "_absolutePosition", space);
        putBool(player, "calculateAbsolutePosition", false);
        setStaticObj(PlayerUpdate.class, "MY_PLAYER", player);
        var staticMethodsClass = Class.forName("com.jagex.game.runetek5.staticmethods.StaticMethods");
        Object staticMethods = null;
        try {
            staticMethods = staticMethodsClass.getField("INSTANCE").get(null);
        } catch (NoSuchFieldException ignored) {
        }
        var pitchUp = pitchOp("applyCameraPitchUp");
        var pitchDown = pitchOp("applyCameraPitchDown");
        pitchUp.setAccessible(true);
        pitchDown.setAccessible(true);
        var random = new Random(SEED);
        var lines = new ArrayList<String>();
        int count = 0;
        for (int c = 0; c < CASES + SCRIPTS; c++) {
            final boolean scripted = c >= CASES;
            final int script = c - CASES;
            int plane = random.nextInt(3);
            int area = AREA_HEIGHTS[random.nextInt(AREA_HEIGHTS.length)];
            int playerX = (8 + random.nextInt(SIZE - 16)) * 512 + random.nextInt(512);
            int playerY = (8 + random.nextInt(SIZE - 16)) * 512 + random.nextInt(512);
            int jh = random.nextInt(100) - 50;
            int jv = random.nextInt(110) - 55;
            int ja = random.nextInt(80) - 40;
            int angleY0 = random.nextInt(16384);
            int angleX0 = random.nextInt(4) == 0 ? random.nextInt(4096) : 1052 + random.nextInt(600);
            int zoom0 = random.nextInt(4) == 0 ? random.nextInt(1900) - 100 : 600;
            int lowest0 = random.nextInt(4) == 0 ? random.nextInt(900000) : 262144 + random.nextInt(500000);
            int baseX0 = playerX + jh + (random.nextInt(4) == 0 ? random.nextInt(6001) - 3000 : 0);
            int baseY0 = playerY + jv + (random.nextInt(4) == 0 ? random.nextInt(6001) - 3000 : 0);
            int cycleCounter = random.nextInt(4) == 0 ? 500 + random.nextInt(9) : random.nextInt(508);
            final int startCycle = cycleCounter;
            setStaticInt(PlayerUpdate.class, "MY_PLAYER_PLANE", plane);
            setStaticInt(Camera.class, "cameraBaseX", baseX0);
            setStaticInt(Camera.class, "cameraBaseY", baseY0);
            setStaticFloat(Camera.class, "camAngleX", (float) angleX0);
            setStaticFloat(Camera.class, "camAngleY", (float) angleY0);
            setStaticInt(Camera.class, "regularLowestYaw", lowest0);
            setStaticInt(Camera.class, "cameraZoom", zoom0);
            setStaticInt(Camera.class, "cameraRandomisationH", jh);
            setStaticInt(Camera.class, "cameraRandomisationV", jv);
            setStaticInt(Camera.class, "cameraRandomisationA", ja);
            setStaticInt(Camera.class, "cameraStepH", 2);
            setStaticInt(Camera.class, "cameraStepV", 2);
            setStaticInt(Camera.class, "cameraStepA", 1);
            setStaticFloat(Client.class, "cameraYawVelocity", 0f);
            setStaticFloat(Client.class, "cameraPitchVelocity", 0f);
            setStaticBool(Client.class, "cameraYawVelocityApplied", false);
            setStaticBool(Client.class, "cameraPitchVelocityApplied", false);
            int yawDir = 0;
            int pitchDir = 0;
            for (int f = 0; f < (scripted ? SCRIPT_FRAMES : FRAMES); f++) {
                if (random.nextInt(8) == 0) yawDir = random.nextInt(3) - 1;
                if (random.nextInt(8) == 0) pitchDir = random.nextInt(3) - 1;
                int elapsed = random.nextInt(10) == 0 ? random.nextInt(5) : 1 + random.nextInt(60);
                int cycles = random.nextInt(3) == 0 ? 0 : (random.nextInt(12) == 0 ? 20 + random.nextInt(30) : 1 + random.nextInt(3));
                int bits = random.nextInt(8);
                int wheel = random.nextInt(5) == 0 ? (random.nextBoolean() ? 1 : -1) : 0;
                boolean nudge = random.nextInt(6) == 0;
                if (scripted) {
                    int phase = f / 6;
                    int held = phase == 0 ? 1 : phase == 1 ? -1 : 0;
                    yawDir = script == 0 || script == 3 ? held : 0;
                    pitchDir = script == 1 || script == 3 ? (phase == 0 ? -1 : phase == 1 ? 1 : 0) : 0;
                    elapsed = 20;
                    cycles = script == 2 ? 1 : 5;
                    bits = 0;
                    wheel = script == 2 ? (f < 8 ? 1 : f < 16 ? -1 : 0) : 0;
                }
                if (nudge && !scripted) {
                    playerX = Math.max(0, Math.min(SIZE * 512 - 1, playerX + random.nextInt(1537) - 768));
                    playerY = Math.max(0, Math.min(SIZE * 512 - 1, playerY + random.nextInt(1537) - 768));
                }
                boolean teleport = random.nextInt(60) == 0;
                if (teleport && !scripted) {
                    playerX = (8 + random.nextInt(SIZE - 16)) * 512 + random.nextInt(512);
                    playerY = (8 + random.nextInt(SIZE - 16)) * 512 + random.nextInt(512);
                }
                space.coords.x = (float) playerX;
                space.coords.z = (float) playerY;
                for (int k = 0; k < cycles; k++) {
                    if (yawDir > 0) Client.applyCameraYawRight();
                    if (yawDir < 0) {
                        var m = staticMethodsClass.getMethod("applyCameraYawLeft");
                        m.invoke(staticMethods);
                    }
                    if (pitchDir > 0) pitchUp.invoke(staticMethods);
                    if (pitchDir < 0) pitchDown.invoke(staticMethods);
                    if (getBool("cameraPitchVelocityApplied")) {
                        setStaticBool(Client.class, "cameraPitchVelocityApplied", false);
                    } else {
                        setStaticFloat(Client.class, "cameraPitchVelocity", getStaticFloat(Client.class, "cameraPitchVelocity") / 2.0f);
                    }
                    if (getBool("cameraYawVelocityApplied")) {
                        setStaticBool(Client.class, "cameraYawVelocityApplied", false);
                    } else {
                        setStaticFloat(Client.class, "cameraYawVelocity", getStaticFloat(Client.class, "cameraYawVelocity") / 2.0f);
                    }
                    cycleCounter++;
                    if (cycleCounter > 508) {
                        cycleCounter = 0;
                        if ((bits & 1) == 1) setStaticInt(Camera.class, "cameraRandomisationH", getStaticInt(Camera.class, "cameraRandomisationH") + getStaticInt(Camera.class, "cameraStepH"));
                        if ((bits & 2) == 2) setStaticInt(Camera.class, "cameraRandomisationV", getStaticInt(Camera.class, "cameraRandomisationV") + getStaticInt(Camera.class, "cameraStepV"));
                        if ((bits & 4) == 4) setStaticInt(Camera.class, "cameraRandomisationA", getStaticInt(Camera.class, "cameraRandomisationA") + getStaticInt(Camera.class, "cameraStepA"));
                    }
                    if (getStaticInt(Camera.class, "cameraRandomisationH") < -51) setStaticInt(Camera.class, "cameraStepH", 2);
                    if (getStaticInt(Camera.class, "cameraRandomisationH") > 56) setStaticInt(Camera.class, "cameraStepH", -2);
                    if (getStaticInt(Camera.class, "cameraRandomisationV") < -63) setStaticInt(Camera.class, "cameraStepV", 2);
                    if (getStaticInt(Camera.class, "cameraRandomisationV") > 56) setStaticInt(Camera.class, "cameraStepV", -2);
                    if (getStaticInt(Camera.class, "cameraRandomisationA") < -44) setStaticInt(Camera.class, "cameraStepA", 1);
                    if (getStaticInt(Camera.class, "cameraRandomisationA") > 40) setStaticInt(Camera.class, "cameraStepA", -1);
                }
                int zoom = getStaticInt(Camera.class, "cameraZoom");
                if (wheel == -1 && zoom >= -50) zoom -= 30;
                if (wheel == 1 && zoom < 1700) zoom += 30;
                setStaticInt(Camera.class, "cameraZoom", zoom);
                Camera.updateFollowPlayerCamera((long) elapsed);
                int yaw = (int) getStaticFloat(Camera.class, "camAngleX");
                int lowest = getStaticInt(Camera.class, "regularLowestYaw");
                if (lowest >> 8 > yaw) yaw = lowest >> 8;
                int roll = ((int) getStaticFloat(Camera.class, "camAngleY") + getStaticInt(Camera.class, "cameraRandomisationA")) & 0x3fff;
                Camera.setCameraPosition(
                    getStaticInt(Camera.class, "cameraBaseX"),
                    MapRegion.tileheightForAbsolutePosition(playerX, playerY, plane) - 200,
                    getStaticInt(Camera.class, "cameraBaseY"),
                    yaw, roll, (((yaw >> 3) * 3 + zoom) << 2), area);
                int cx = Math.min(Math.max(getStaticInt(Camera.class, "cameraPositionAbsoluteX"), 0), (SIZE << 9) - 1);
                int cy = Math.min(Math.max(getStaticInt(Camera.class, "cameraPositionAbsoluteY"), 0), (SIZE << 9) - 1);
                int cz = getStaticInt(Camera.class, "cameraPositionAbsoluteZ");
                var row = new StringBuilder("C\torbit");
                for (int v : new int[] {c, f, plane, area, playerX, playerY, elapsed, cycles, yawDir, pitchDir, bits, wheel,
                        jh, jv, ja, angleY0, angleX0, zoom0, lowest0, baseX0, baseY0, startCycle}) {
                    row.append('\t').append(v);
                }
                row.append("\t|");
                for (int v : new int[] {cx, cz, cy, getStaticInt(Camera.class, "cameraYaw"), getStaticInt(Camera.class, "cameraRoll"),
                        getStaticInt(Camera.class, "cameraBaseX"), getStaticInt(Camera.class, "cameraBaseY"), lowest,
                        Float.floatToRawIntBits(getStaticFloat(Camera.class, "camAngleX")),
                        Float.floatToRawIntBits(getStaticFloat(Camera.class, "camAngleY")),
                        zoom,
                        Float.floatToRawIntBits(getStaticFloat(Client.class, "cameraYawVelocity")),
                        Float.floatToRawIntBits(getStaticFloat(Client.class, "cameraPitchVelocity")),
                        getStaticInt(Camera.class, "cameraRandomisationH"), getStaticInt(Camera.class, "cameraRandomisationV"),
                        getStaticInt(Camera.class, "cameraRandomisationA"),
                        MapRegion.tileheightForAbsolutePosition(playerX, playerY, plane)}) {
                    row.append('\t').append(v);
                }
                lines.add(row.toString());
                count++;
            }
        }
        lines.add("T\t" + count);
        Files.write(out.resolve("camera-orbit.native.tsv"), (String.join("\n", lines) + "\n").getBytes(StandardCharsets.UTF_8));
        System.exit(0);
    }

    static boolean getBool(String name) throws Exception {
        var f = field(Client.class, name);
        return unsafe.getBoolean(unsafe.staticFieldBase(f), unsafe.staticFieldOffset(f));
    }
}
