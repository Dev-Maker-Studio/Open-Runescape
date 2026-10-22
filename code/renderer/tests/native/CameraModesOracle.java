import com.jagex.game.runetek5.client.Client;
import com.jagex.game.runetek5.client.input.camera.Camera;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.movement.CoordGrid;
import com.jagex.game.runetek5.movement.CoordinateSpace;
import com.jagex.game.runetek5.network.PingRequester;
import com.jagex.game.runetek5.staticmethods.StaticMethods;
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

public final class CameraModesOracle {
    static final long SEED = 0x5ce7a9d1L;
    static final int CASES = 48;
    static final int FRAMES = 70;
    static final int KEYS = 6;
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
        putObj(region, "baseCoordinates", new CoordGrid());
        putInt(region, "sceneSizeX", SIZE);
        putInt(region, "sceneSizeY", SIZE);
        putObj(region, "tileSettingsBits", bits);
        putObj(region, "surfaceSceneGraph", graph);
        setStaticObj(Resource.class, "mapRegion", region);
    }

    static Object getStaticObj(Class<?> type, String name) throws Exception {
        var f = field(type, name);
        return unsafe.getObject(unsafe.staticFieldBase(f), unsafe.staticFieldOffset(f));
    }

    static boolean getStaticBool(Class<?> type, String name) throws Exception {
        var f = field(type, name);
        return unsafe.getBoolean(unsafe.staticFieldBase(f), unsafe.staticFieldOffset(f));
    }

    static int camInt(String name) throws Exception {
        return getStaticInt(Camera.class, name);
    }

    static void setCam(String name, int value) throws Exception {
        setStaticInt(Camera.class, name, value);
    }

    static int splineValue(int seed, int path, int point, int axis, int baseX, int baseY) {
        int v = mix(seed + path * 31 + axis * 7, point, path * 5 + axis);
        int span = (v >>> 4) & 0xffff;
        if (axis == 0) return baseX * 512 + (span % (SIZE * 512));
        if (axis == 2) return baseY * 512 + (span % (SIZE * 512));
        if (axis == 1) return (span % 3000) - 1500;
        return (span % 2200) - 400;
    }

    static void fillSplines(int seed, int baseX, int baseY) throws Exception {
        var paths = (int[][][]) getStaticObj(Client.class, "cameraSplinePaths");
        for (int p = 0; p < 2; p++) {
            var path = new int[KEYS * 2][4];
            for (int i = 0; i < KEYS * 2; i++) {
                for (int a = 0; a < 4; a++) path[i][a] = splineValue(seed, p, i, a, baseX, baseY);
            }
            paths[p] = path;
        }
    }

    static String joinInts(int[] values) {
        var sb = new StringBuilder();
        for (int v : values) sb.append('\t').append(v);
        return sb.toString();
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
        var region = Resource.mapRegion;
        var base = (CoordGrid) field(MapRegion.class, "baseCoordinates").get(region);
        var random = new Random(SEED);
        var lines = new ArrayList<String>();
        int count = 0;
        var shaking = (boolean[]) getStaticObj(Client.class, "CAM_SHAKING");
        var jitterArr = (int[]) getStaticObj(Client.class, "CAM_SHAKE_JITTER");
        var ampArr = (int[]) getStaticObj(Client.class, "CUSTOM_LOWEST_YAW");
        var freqArr = (int[]) getStaticObj(Client.class, "CAM_SHAKE_FREQUENCY");
        var phaseArr = (int[]) getStaticObj(Client.class, "CAM_SHAKE_PHASE");
        for (int c = 0; c < CASES; c++) {
            int plane = random.nextInt(3);
            int area = AREA_HEIGHTS[random.nextInt(AREA_HEIGHTS.length)];
            int playerX = (8 + random.nextInt(SIZE - 16)) * 512 + random.nextInt(512);
            int playerY = (8 + random.nextInt(SIZE - 16)) * 512 + random.nextInt(512);
            int baseTX = random.nextInt(4) == 0 ? 3200 + random.nextInt(900) : 0;
            int baseTY = random.nextInt(4) == 0 ? 3200 + random.nextInt(900) : 0;
            int jh = random.nextInt(100) - 50;
            int jv = random.nextInt(110) - 55;
            int ja = random.nextInt(80) - 40;
            int angleY0 = random.nextInt(16384);
            int angleX0 = random.nextInt(4) == 0 ? random.nextInt(4096) : 1052 + random.nextInt(600);
            int zoom0 = random.nextInt(4) == 0 ? random.nextInt(1900) - 100 : 600;
            int lowest0 = 262144 + random.nextInt(500000);
            int baseX0 = playerX + jh;
            int baseY0 = playerY + jv;
            int cycles0 = random.nextInt(1000);
            int splineSeed = random.nextInt(1 << 20);
            base.x = baseTX;
            base.y = baseTY;
            fillSplines(splineSeed, baseTX, baseTY);
            for (int i = 0; i < 5; i++) {
                shaking[i] = false;
                jitterArr[i] = 0;
                ampArr[i] = 0;
                freqArr[i] = 0;
                phaseArr[i] = 0;
            }
            var transitionCycle = new int[] {-1, -1, -1, -1, -1};
            var transition = new int[5];
            setStaticInt(PlayerUpdate.class, "MY_PLAYER_PLANE", plane);
            setCam("cameraBaseX", baseX0);
            setCam("cameraBaseY", baseY0);
            setStaticFloat(Camera.class, "camAngleX", (float) angleX0);
            setStaticFloat(Camera.class, "camAngleY", (float) angleY0);
            setCam("regularLowestYaw", lowest0);
            setCam("cameraZoom", zoom0);
            setCam("cameraRandomisationH", jh);
            setCam("cameraRandomisationV", jv);
            setCam("cameraRandomisationA", ja);
            setCam("cameraCalculationType", 2);
            for (var name : new String[] {"cameraPositionAbsoluteX", "cameraPositionAbsoluteY", "cameraPositionAbsoluteZ", "cameraYaw", "cameraRoll", "cameraPitch",
                    "transitionStartX", "transitionStartY", "transitionStartZ", "transitionStartYaw", "transitionStartRoll", "lookX", "lookY", "lookAtHeightOffset",
                    "cameraMovementLocalX", "cameraMovementLocalY", "cameraMovementZ", "cameraConstantMoveSpeed", "cameraVariableMoveSpeed", "moveCameraAngle", "moveCameraSpeed"}) {
                setCam(name, 0);
            }
            setStaticFloat(Client.class, "cameraYawVelocity", 0f);
            setStaticFloat(Client.class, "cameraPitchVelocity", 0f);
            setStaticInt(Client.class, "cycles", cycles0);
            setStaticInt(Client.class, "lastCameraSmoothCycle", 0);
            setStaticInt(Client.class, "cameraTargetAbsX", 0);
            setStaticInt(Client.class, "cameraTargetAbsY", 0);
            setStaticInt(Client.class, "cameraPositionPathIndex", -1);
            setStaticInt(Client.class, "cameraLookAtPathIndex", -1);
            setStaticInt(Client.class, "cameraPositionKeyframe", 0);
            setStaticInt(Client.class, "cameraLookAtKeyframe", 0);
            setStaticInt(Client.class, "cameraSplineProgress", 0);
            setStaticInt(Client.class, "cameraSplineSpeedStart", 0);
            setStaticInt(Client.class, "cameraSplineSpeedEnd", 0);
            setStaticBool(Client.class, "sendCameraAngleToServer", false);
            setStaticBool(Client.class, "cutsceneCameraCompleted", false);
            setStaticBool(Client.class, "cutsceneCameraPreviouslyCompleted", false);
            setStaticInt(PingRequester.class, "cameraTargetAbsX", -1);
            setStaticInt(StaticMethods.class, "cameraTargetAbsY", -1);
            for (int f = 0; f < FRAMES; f++) {
                int op = random.nextInt(100) < 45 ? 0 : 1 + random.nextInt(9);
                if (f == 0 && c % 3 == 0) op = 9;
                int[] a = new int[6];
                switch (op) {
                    case 1 -> {
                        a[0] = 5 + random.nextInt(SIZE - 10);
                        a[1] = 5 + random.nextInt(SIZE - 10);
                        a[2] = random.nextInt(3200);
                        a[3] = random.nextInt(25);
                        a[4] = new int[] {0, 5, 20, 60, 99, 100, 255}[random.nextInt(7)];
                    }
                    case 2 -> {
                        a[0] = random.nextInt(420);
                        a[1] = random.nextInt(2048);
                        a[2] = random.nextInt(100);
                    }
                    case 3 -> {
                        a[0] = 5 + random.nextInt(SIZE - 10);
                        a[1] = 5 + random.nextInt(SIZE - 10);
                        a[2] = random.nextInt(800) - 200;
                        a[3] = random.nextInt(12);
                        a[4] = new int[] {0, 10, 50, 99, 100, 255}[random.nextInt(6)];
                    }
                    case 6 -> {
                        a[0] = random.nextInt(5);
                        a[1] = random.nextInt(60);
                        a[2] = random.nextInt(65536);
                        a[3] = random.nextInt(14);
                        a[4] = random.nextInt(256);
                    }
                    case 7, 8 -> {
                        if (op == 7 && random.nextInt(4) == 0) {
                            a[0] = -1;
                        } else {
                            int tx = baseTX + random.nextInt(SIZE + 20) - 10;
                            int ty = baseTY + random.nextInt(SIZE + 20) - 10;
                            a[0] = ((random.nextInt(4) << 28) | ((tx & 0x3fff) << 14) | (ty & 0x3fff));
                        }
                    }
                    case 9 -> {
                        a[0] = random.nextInt(2);
                        a[1] = random.nextInt(KEYS - 1);
                        a[2] = 300 + random.nextInt(2500);
                        a[3] = 300 + random.nextInt(2500);
                        a[4] = random.nextInt(2);
                        a[5] = random.nextInt(KEYS - 1);
                    }
                    default -> {
                    }
                }
                int elapsed = 1 + random.nextInt(60);
                int cycles = random.nextInt(3) == 0 ? 0 : 1 + random.nextInt(4);
                if (random.nextInt(20) == 0) cycles = 30 + random.nextInt(40);
                var draws = new long[5];
                for (int i = 0; i < 5; i++) draws[i] = Double.doubleToRawLongBits(random.nextDouble());
                if (random.nextInt(10) == 0) {
                    playerX = (8 + random.nextInt(SIZE - 16)) * 512 + random.nextInt(512);
                    playerY = (8 + random.nextInt(SIZE - 16)) * 512 + random.nextInt(512);
                }
                space.coords.x = (float) playerX;
                space.coords.z = (float) playerY;
                int sizeX = SIZE;
                switch (op) {
                    case 1 -> Camera.moveCamera(a[0], a[1], a[2], a[3], a[4], true);
                    case 2 -> Camera.setAngle(a[0], a[1], a[2]);
                    case 3 -> Camera.lookat(a[0], a[1], a[2], a[3], a[4]);
                    case 4 -> Camera.smoothReset();
                    case 5 -> Camera.startSmoothTransition();
                    case 6 -> {
                        int slot = a[0];
                        shaking[slot] = true;
                        jitterArr[slot] = a[3];
                        ampArr[slot] = a[1];
                        freqArr[slot] = a[4];
                        phaseArr[slot] = a[2];
                    }
                    case 7 -> {
                        if (a[0] == -1) {
                            setStaticInt(PingRequester.class, "cameraTargetAbsX", -1);
                            setStaticInt(StaticMethods.class, "cameraTargetAbsY", -1);
                        } else {
                            int lx = ((a[0] >> 14) & 0x3fff) - base.x;
                            if (lx < 0) lx = 0; else if (lx >= sizeX) lx = sizeX;
                            int ly = (a[0] & 0x3fff) - base.y;
                            if (ly < 0) ly = 0; else if (ly >= sizeX) ly = sizeX;
                            setStaticInt(PingRequester.class, "cameraTargetAbsX", (lx << 9) + 256);
                            setStaticInt(StaticMethods.class, "cameraTargetAbsY", (ly << 9) + 256);
                        }
                    }
                    case 8 -> {
                        int lx = ((a[0] >> 14) & 0x3fff) - base.x;
                        if (lx < 0) lx = 0; else if (lx >= sizeX) lx = sizeX;
                        int ly = (a[0] & 0x3fff) - base.y;
                        if (ly < 0) ly = 0; else if (ly >= sizeX) ly = sizeX;
                        setStaticInt(Client.class, "cameraTargetAbsX", (lx << 9) + 256);
                        setStaticInt(Client.class, "cameraTargetAbsY", (ly << 9) + 256);
                        setCam("cameraCalculationType", 4);
                        setStaticInt(StaticMethods.class, "cameraTargetAbsY", -1);
                        setStaticInt(PingRequester.class, "cameraTargetAbsX", -1);
                    }
                    case 9 -> {
                        setStaticInt(Client.class, "cameraPositionPathIndex", a[0]);
                        setStaticInt(Client.class, "cameraPositionKeyframe", a[1]);
                        setStaticInt(Client.class, "cameraSplineProgress", 0);
                        setStaticInt(Client.class, "cameraSplineSpeedStart", a[2]);
                        setStaticInt(Client.class, "cameraSplineSpeedEnd", a[3]);
                        setStaticInt(Client.class, "cameraLookAtPathIndex", a[4]);
                        setStaticInt(Client.class, "cameraLookAtKeyframe", a[5]);
                        setCam("cameraCalculationType", 3);
                        setStaticInt(StaticMethods.class, "cameraTargetAbsY", -1);
                        setStaticInt(PingRequester.class, "cameraTargetAbsX", -1);
                    }
                    default -> {
                    }
                }
                for (int k = 0; k < cycles; k++) {
                    if (camInt("cameraCalculationType") == 5) {
                        Camera.calculateCameraPosition();
                    } else if (camInt("cameraCalculationType") == 3) {
                        Camera.calculateCutsceneCameraPosition();
                    }
                    for (int i = 0; i < 5; i++) phaseArr[i]++;
                    setStaticInt(Client.class, "cycles", getStaticInt(Client.class, "cycles") + 1);
                }
                int type = camInt("cameraCalculationType");
                if (type == 2 || type == 1) {
                    Camera.updateFollowPlayerCamera((long) elapsed);
                } else if (type == 4) {
                    Camera.updateOrbitTargetCamera((long) elapsed);
                }
                type = camInt("cameraCalculationType");
                int h = area;
                if (type == 2) {
                    int yaw = (int) getStaticFloat(Camera.class, "camAngleX");
                    int lowest = camInt("regularLowestYaw");
                    if (lowest >> 8 > yaw) yaw = lowest >> 8;
                    if (shaking[4] && ampArr[4] + 128 > yaw) yaw = ampArr[4] + 128;
                    int roll = ((int) getStaticFloat(Camera.class, "camAngleY") + camInt("cameraRandomisationA")) & 0x3fff;
                    Camera.setCameraPosition(camInt("cameraBaseX"), MapRegion.tileheightForAbsolutePosition(playerX, playerY, plane) - 200, camInt("cameraBaseY"), yaw, roll, (yaw >> 3) * 3 + camInt("cameraZoom") << 2, h);
                } else if (type == 4) {
                    int yaw = (int) getStaticFloat(Camera.class, "camAngleX");
                    int lowest = camInt("regularLowestYaw");
                    if (lowest >> 8 > yaw) yaw = lowest >> 8;
                    if (shaking[4] && ampArr[4] + 128 > yaw) yaw = ampArr[4] + 128;
                    int roll = ((int) getStaticFloat(Camera.class, "camAngleY")) & 0x3fff;
                    Camera.setCameraPosition(camInt("cameraBaseX"), MapRegion.tileheightForAbsolutePosition(getStaticInt(Client.class, "cameraTargetAbsX"), getStaticInt(Client.class, "cameraTargetAbsY"), plane) - 200, camInt("cameraBaseY"), yaw, roll, (yaw >> 3) * 3 + camInt("cameraZoom") << 2, h);
                } else if (type == 1) {
                    Camera.updateSmoothTransitionCamera(h);
                }
                int savedX = camInt("cameraPositionAbsoluteX");
                int savedZ = camInt("cameraPositionAbsoluteZ");
                int savedY = camInt("cameraPositionAbsoluteY");
                int savedYaw = camInt("cameraYaw");
                int savedRoll = camInt("cameraRoll");
                int cyc = getStaticInt(Client.class, "cycles");
                int drawIndex = 0;
                for (int slot = 0; slot < 5; slot++) {
                    if (!shaking[slot]) continue;
                    if (transitionCycle[slot] != cyc) {
                        transitionCycle[slot] = cyc;
                        transition[slot] = (int) (Double.longBitsToDouble(draws[drawIndex++]) * (jitterArr[slot] * 2 + 1) - jitterArr[slot] + Math.sin(phaseArr[slot] * (freqArr[slot] / 100.0)) * ampArr[slot]);
                    }
                    int t = transition[slot];
                    if (slot == 0) setCam("cameraPositionAbsoluteX", camInt("cameraPositionAbsoluteX") + (t << 2));
                    if (slot == 1) setCam("cameraPositionAbsoluteZ", camInt("cameraPositionAbsoluteZ") + (t << 2));
                    if (slot == 2) setCam("cameraPositionAbsoluteY", camInt("cameraPositionAbsoluteY") + (t << 2));
                    if (slot == 3) setCam("cameraRoll", (t + camInt("cameraRoll")) & 0x3fff);
                    if (slot == 4) {
                        int y2 = camInt("cameraYaw") + t;
                        if (y2 < 1024) y2 = 1024; else if (y2 > 3072) y2 = 3072;
                        setCam("cameraYaw", y2);
                    }
                }
                int px = Math.min(Math.max(camInt("cameraPositionAbsoluteX"), 0), (SIZE << 9) - 1);
                int py = Math.min(Math.max(camInt("cameraPositionAbsoluteY"), 0), (SIZE << 9) - 1);
                int pz = camInt("cameraPositionAbsoluteZ");
                int pyaw = camInt("cameraYaw");
                int proll = camInt("cameraRoll");
                int ppitch = camInt("cameraPitch");
                setCam("cameraPositionAbsoluteX", savedX);
                setCam("cameraPositionAbsoluteZ", savedZ);
                setCam("cameraPositionAbsoluteY", savedY);
                setCam("cameraYaw", savedYaw);
                setCam("cameraRoll", savedRoll);
                var row = new StringBuilder("C\tmodes");
                row.append(joinInts(new int[] {c, f, plane, area, playerX, playerY, baseTX, baseTY, elapsed, cycles, op, a[0], a[1], a[2], a[3], a[4], a[5]}));
                for (long d : draws) row.append('\t').append(d);
                row.append(joinInts(new int[] {jh, jv, ja, angleY0, angleX0, zoom0, lowest0, baseX0, baseY0, cycles0, splineSeed}));
                row.append("\t|");
                row.append(joinInts(new int[] {px, pz, py, pyaw, proll, ppitch, camInt("cameraCalculationType"), camInt("cameraBaseX"), camInt("cameraBaseY"), camInt("regularLowestYaw"),
                        Float.floatToRawIntBits(getStaticFloat(Camera.class, "camAngleX")), Float.floatToRawIntBits(getStaticFloat(Camera.class, "camAngleY")),
                        savedX, savedZ, savedY, savedYaw, savedRoll, camInt("cameraPitch"),
                        getStaticInt(PingRequester.class, "cameraTargetAbsX"), getStaticInt(StaticMethods.class, "cameraTargetAbsY"),
                        getStaticInt(Client.class, "cameraTargetAbsX"), getStaticInt(Client.class, "cameraTargetAbsY"),
                        getStaticInt(Client.class, "cameraSplineProgress"), getStaticBool(Client.class, "cutsceneCameraCompleted") ? 1 : 0,
                        getStaticBool(Client.class, "cutsceneCameraPreviouslyCompleted") ? 1 : 0,
                        getStaticInt(Client.class, "cameraPositionPathIndex"), getStaticInt(Client.class, "cameraLookAtPathIndex"),
                        camInt("moveCameraSpeed"), camInt("moveCameraAngle"), camInt("transitionStartX"), camInt("transitionStartYaw"),
                        getStaticInt(Client.class, "cycles") - getStaticInt(Client.class, "lastCameraSmoothCycle"),
                        getStaticBool(Client.class, "sendCameraAngleToServer") ? 1 : 0}));
                lines.add(row.toString());
                count++;
            }
        }
        lines.add("T\t" + count);
        Files.write(out.resolve("camera-modes.native.tsv"), (String.join("\n", lines) + "\n").getBytes(StandardCharsets.UTF_8));
        System.exit(0);
    }
}
