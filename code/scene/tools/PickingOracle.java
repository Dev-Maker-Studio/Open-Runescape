import com.jagex.game.runetek5.client.menu.MiniMenu;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.entity.BoundingBoxPolygon;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.math.Matrix43;
import com.jagex.game.runetek5.math.Matrix44;
import com.jagex.game.runetek5.scene.MapRegion;
import com.jagex.game.runetek5.scene.Scene;
import com.jagex.game.runetek5.scene.SettingsBits;
import com.jagex.game.runetek5.scene.ground.Ground;
import com.jagex.game.runetek5.scene.ground.GroundSM;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Random;
import sun.misc.Unsafe;

public final class PickingOracle {
    private static final Unsafe UNSAFE;

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSAFE = (Unsafe) field.get(null);
        } catch (ReflectiveOperationException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    private static String hash(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }

    private static void checkSource(Path jar, Class<?> type) throws Exception {
        Path loaded = Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
        if (!jar.equals(loaded)) throw new IllegalArgumentException(type.getName() + " code source mismatch");
    }

    private static Field find(Class<?> type, String name) throws Exception {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static void putObj(Object o, String name, Object value) throws Exception {
        UNSAFE.putObject(o, UNSAFE.objectFieldOffset(find(o.getClass(), name)), value);
    }

    private static void putInt(Object o, String name, int value) throws Exception {
        UNSAFE.putInt(o, UNSAFE.objectFieldOffset(find(o.getClass(), name)), value);
    }

    private static void putByte(Object o, String name, byte value) throws Exception {
        UNSAFE.putByte(o, UNSAFE.objectFieldOffset(find(o.getClass(), name)), value);
    }

    private static void putStatic(Class<?> type, String name, Object value) throws Exception {
        Field field = find(type, name);
        UNSAFE.putObject(UNSAFE.staticFieldBase(field), UNSAFE.staticFieldOffset(field), value);
    }

    private static int bits(float value) {
        return Float.floatToRawIntBits(value);
    }

    private static float fl(int bits) {
        return Float.intBitsToFloat(bits);
    }

    private static void floats(StringBuilder out, float[] values) {
        for (float v : values) out.append('\t').append(bits(v));
    }

    private static Matrix44 matrix(float[] values) {
        Matrix44 m = new Matrix44();
        System.arraycopy(values, 0, m.getElements(), 0, 16);
        return m;
    }

    private static float[] randomMatrix(Random random, float span) {
        float[] values = new float[16];
        for (int i = 0; i < 16; i++) values[i] = (random.nextFloat() * 2f - 1f) * span;
        for (int i = 0; i < 16; i += 5) values[i] += span * 2f * (random.nextBoolean() ? 1 : -1);
        return values;
    }

    private static String matrixCases(Random random) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < 400; i++) {
            float span = i % 4 == 0 ? 0.01f : i % 4 == 1 ? 1f : i % 4 == 2 ? 300f : 40000f;
            float[] a = randomMatrix(random, span);
            float[] b = randomMatrix(random, i % 3 == 0 ? 3f : span);
            Matrix44 product = matrix(a);
            product.multiplyBy(matrix(b));
            Matrix44 inverse = matrix(a);
            inverse.invert();
            float det = matrix(a).determinant();
            float[] scaled = new float[4];
            float x = (random.nextFloat() * 2f - 1f);
            float y = (random.nextFloat() * 2f - 1f);
            float z = random.nextBoolean() ? -1f : 1f;
            matrix(a).scale(x, y, z, scaled);
            out.append(i);
            floats(out, a);
            floats(out, b);
            floats(out, product.getElements());
            floats(out, inverse.getElements());
            floats(out, new float[] {det, x, y, z});
            floats(out, scaled);
            out.append('\n');
        }
        return out.toString();
    }

    private static Ground ground(int width, int length, int scaling, int[][] heights) throws Exception {
        Ground ground = (Ground) UNSAFE.allocateInstance(GroundSM.class);
        int scale = 0;
        for (int s = scaling; s > 1; s >>= 1) scale++;
        putInt(ground, "width", width);
        putInt(ground, "length", length);
        putInt(ground, "tileScale", scale);
        putInt(ground, "tileUnits", 1 << scale);
        putObj(ground, "tileHeights", heights);
        return ground;
    }

    private static int[][] randomHeights(Random random, int width, int length, int base, int spread) {
        int[][] heights = new int[width + 1][length + 1];
        for (int x = 0; x <= width; x++) {
            for (int y = 0; y <= length; y++) {
                heights[x][y] = base + random.nextInt(2 * spread + 1) - spread;
            }
        }
        return heights;
    }

    private static String heightCases(Random random) throws Exception {
        StringBuilder out = new StringBuilder();
        int[][] dims = {{4, 4, 512}, {7, 5, 512}, {9, 9, 512}, {3, 6, 256}, {5, 5, 128}, {6, 4, 1024}};
        for (int id = 0; id < dims.length; id++) {
            int width = dims[id][0];
            int length = dims[id][1];
            int scaling = dims[id][2];
            int[][] heights = randomHeights(random, width, length, id % 2 == 0 ? 0 : -200, id < 4 ? 600 : 90000);
            Ground ground = ground(width, length, scaling, heights);
            StringBuilder flat = new StringBuilder();
            for (int x = 0; x <= width; x++) {
                for (int y = 0; y <= length; y++) {
                    if (flat.length() > 0) flat.append(',');
                    flat.append(heights[x][y]);
                }
            }
            out.append("G\t").append(id).append('\t').append(width).append('\t').append(length).append('\t').append(scaling).append('\t').append(flat).append('\n');
            int range = (Math.max(width, length) + 2) * scaling;
            for (int q = 0; q < 300; q++) {
                int x = random.nextInt(range + scaling) - scaling / 2;
                int y = random.nextInt(range + scaling) - scaling / 2;
                if (q % 25 == 0) {
                    x = (random.nextInt(width + 1)) * scaling + (q % 50 == 0 ? 0 : -1);
                    y = (random.nextInt(length + 1)) * scaling;
                }
                out.append("Q\t").append(id).append('\t').append(x).append('\t').append(y).append('\t').append(ground.getAverageHeight(x, y)).append('\n');
            }
        }
        return out.toString();
    }

    private static MapRegion region(int sizeX, int sizeY, Ground[] planes, byte[][] masksLevel1) throws Exception {
        Scene scene = (Scene) UNSAFE.allocateInstance(Scene.class);
        putObj(scene, "surfaceHeightmap", planes);
        MapRegion region = (MapRegion) UNSAFE.allocateInstance(MapRegion.class);
        putObj(region, "scene", scene);
        putInt(region, "sceneSizeX", sizeX);
        putInt(region, "sceneSizeY", sizeY);
        SettingsBits settings = new SettingsBits(sizeX, sizeY);
        for (int x = 0; x < sizeX; x++) System.arraycopy(masksLevel1[x], 0, settings.tileFunctionMasks[1][x], 0, sizeY);
        putObj(region, "tileSettingsBits", settings);
        return region;
    }

    private static void installPlayer(int plane, int size) throws Exception {
        PlayerEntity player = (PlayerEntity) UNSAFE.allocateInstance(PlayerEntity.class);
        putByte(player, "plane", (byte) plane);
        putInt(player, "sizeInSquares", size);
        PlayerUpdate.MY_PLAYER = player;
    }

    private static String rayCases(Random random) throws Exception {
        StringBuilder out = new StringBuilder();
        int[][] sizes = {{16, 16}, {24, 20}, {40, 40}};
        for (int id = 0; id < sizes.length; id++) {
            int sx = sizes[id][0];
            int sy = sizes[id][1];
            Ground[] planes = new Ground[4];
            int[][][] all = new int[4][][];
            for (int p = 0; p < 4; p++) {
                all[p] = randomHeights(random, sx, sy, -p * 240, 300 + id * 200);
                planes[p] = ground(sx, sy, 512, all[p]);
            }
            byte[][] masks = new byte[sx][sy];
            for (int x = 0; x < sx; x++) for (int y = 0; y < sy; y++) masks[x][y] = (byte) (random.nextInt(4) == 0 ? 2 : random.nextInt(5));
            Resource.mapRegion = (region(sx, sy, planes, masks));
            StringBuilder planeRows = new StringBuilder();
            out.append("S\t").append(id).append('\t').append(sx).append('\t').append(sy);
            out.append('\n');
            for (int p = 0; p < 4; p++) {
                out.append("P\t").append(id).append('\t').append(p).append('\t');
                for (int x = 0; x <= sx; x++) for (int y = 0; y <= sy; y++) out.append(all[p][x][y]).append(x == sx && y == sy ? "" : ",");
                out.append('\n');
            }
            out.append("K\t").append(id).append('\t');
            for (int x = 0; x < sx; x++) for (int y = 0; y < sy; y++) out.append(masks[x][y]).append(x == sx - 1 && y == sy - 1 ? "" : ",");
            out.append('\n');
            for (int q = 0; q < 220; q++) {
                int plane = random.nextInt(4);
                installPlayer(plane, 1);
                int depth = 1 + random.nextInt(5);
                float[] d = new float[3];
                float[] e = new float[3];
                d[0] = random.nextFloat() * sx * 512f;
                d[2] = random.nextFloat() * sy * 512f;
                d[1] = -2000f - random.nextFloat() * 4000f;
                e[0] = d[0] + (random.nextFloat() - 0.5f) * 12000f;
                e[2] = d[2] + (random.nextFloat() - 0.5f) * 12000f;
                e[1] = 2000f + random.nextFloat() * 4000f;
                if (q % 11 == 0) {
                    d[0] = -3000f + random.nextFloat() * 2000f;
                }
                float t = MiniMenu.raycastTerrainIntersection(d[0], d[1], d[2], e[0], e[1], e[2], depth);
                out.append("R\t").append(id).append('\t').append(plane).append('\t').append(depth);
                floats(out, d);
                floats(out, e);
                floats(out, new float[] {t});
                out.append('\n');
            }
        }
        return out.toString();
    }

    private static String tileCases(Random random) throws Exception {
        StringBuilder out = new StringBuilder();
        int sx = 32;
        int sy = 32;
        Ground[] planes = new Ground[4];
        int[][][] all = new int[4][][];
        for (int p = 0; p < 4; p++) {
            all[p] = randomHeights(random, sx, sy, -p * 240, 250);
            planes[p] = ground(sx, sy, 512, all[p]);
        }
        byte[][] masks = new byte[sx][sy];
        for (int x = 0; x < sx; x++) for (int y = 0; y < sy; y++) masks[x][y] = (byte) (random.nextInt(6) == 0 ? 2 : 0);
        Resource.mapRegion = (region(sx, sy, planes, masks));
        out.append("S\t").append(sx).append('\t').append(sy).append('\n');
        for (int p = 0; p < 4; p++) {
            out.append("P\t").append(p).append('\t');
            for (int x = 0; x <= sx; x++) for (int y = 0; y <= sy; y++) out.append(all[p][x][y]).append(x == sx && y == sy ? "" : ",");
            out.append('\n');
        }
        out.append("K\t");
        for (int x = 0; x < sx; x++) for (int y = 0; y < sy; y++) out.append(masks[x][y]).append(x == sx - 1 && y == sy - 1 ? "" : ",");
        out.append('\n');
        for (int q = 0; q < 400; q++) {
            int plane = random.nextInt(4);
            int size = 1 + random.nextInt(4);
            installPlayer(plane, size);
            float[] w = new float[16];
            w[0] = 2500f + random.nextFloat() * 4000f;
            w[1] = (random.nextFloat() - 0.5f) * 40f;
            w[2] = (random.nextFloat() - 0.5f) * 40f;
            w[3] = (random.nextFloat() - 0.5f) * 0.0004f;
            w[4] = (random.nextFloat() - 0.5f) * 40f;
            w[5] = (random.nextFloat() - 0.5f) * 40f;
            w[6] = 2500f + random.nextFloat() * 4000f;
            w[7] = (random.nextFloat() - 0.5f) * 0.0004f;
            w[8] = (random.nextFloat() - 0.5f) * 40f;
            w[9] = 3000f + random.nextFloat() * 3000f;
            w[10] = (random.nextFloat() - 0.5f) * 40f;
            w[11] = (random.nextFloat() - 0.5f) * 0.0004f;
            w[12] = sx * 256f + (random.nextFloat() - 0.5f) * 3000f;
            w[13] = (random.nextFloat() - 0.5f) * 800f;
            w[14] = sy * 256f + (random.nextFloat() - 0.5f) * 3000f;
            w[15] = 1f;
            Matrix44 wInverse = matrix(w);
            wInverse.invert();
            Matrix43 sun43 = new Matrix43();
            UNSAFE.putFloat(sun43, UNSAFE.objectFieldOffset(find(Matrix43.class, "xA")), 0.8f + random.nextFloat() * 0.4f);
            UNSAFE.putFloat(sun43, UNSAFE.objectFieldOffset(find(Matrix43.class, "yB")), 0.8f + random.nextFloat() * 0.4f);
            UNSAFE.putFloat(sun43, UNSAFE.objectFieldOffset(find(Matrix43.class, "zC")), 0.8f + random.nextFloat() * 0.4f);
            UNSAFE.putFloat(sun43, UNSAFE.objectFieldOffset(find(Matrix43.class, "yA")), (random.nextFloat() - 0.5f) * 0.1f);
            UNSAFE.putFloat(sun43, UNSAFE.objectFieldOffset(find(Matrix43.class, "xD")), (random.nextFloat() - 0.5f) * 40f);
            UNSAFE.putFloat(sun43, UNSAFE.objectFieldOffset(find(Matrix43.class, "yD")), (random.nextFloat() - 0.5f) * 40f);
            Matrix44 sun = new Matrix44();
            sun.fromMatrix(sun43);
            Matrix44 camera = new Matrix44(sun);
            camera.invert();
            camera.multiplyBy(wInverse);
            int vx = random.nextInt(300);
            int vy = random.nextInt(200);
            int gw = 200 + random.nextInt(1200);
            int gh = 150 + random.nextInt(800);
            int mx = vx + random.nextInt(gw);
            int my = vy + random.nextInt(gh);
            if (q % 17 == 0) mx = -1 - random.nextInt(3);
            int tx = -1;
            int ty = -1;
            if (mx >= 0 && my >= 0) {
                Matrix44 inverse = new Matrix44(sun);
                inverse.multiplyBy(camera);
                inverse.invert();
                int vmx = mx - vx;
                int vmy = my - vy;
                float ndcX = 2.0f * vmx / gw - 1.0f;
                float ndcY = 2.0f * vmy / gh - 1.0f;
                float[] buffer = new float[4];
                inverse.scale(ndcX, ndcY, -1.0f, buffer);
                float nearX = buffer[0] / buffer[3];
                float nearY = buffer[1] / buffer[3];
                float nearZ = buffer[2] / buffer[3];
                inverse.scale(ndcX, ndcY, 1.0f, buffer);
                float farX = buffer[0] / buffer[3];
                float farY = buffer[1] / buffer[3];
                float farZ = buffer[2] / buffer[3];
                float rayT = MiniMenu.raycastTerrainIntersection(nearX, nearY, nearZ, farX, farY, farZ, 4);
                if (rayT > 0.0f) {
                    float rayDx = farX - nearX;
                    float rayDz = farZ - nearZ;
                    int posX = (int) (rayDx * rayT + nearX);
                    int posY = (int) (rayT * rayDz + nearZ);
                    tx = (posX + ((size - 1) << 8)) >> 9;
                    ty = (posY + ((size - 1) << 8)) >> 9;
                }
            }
            out.append("T\t").append(plane).append('\t').append(size).append('\t').append(vx).append('\t').append(vy).append('\t').append(gw).append('\t').append(gh).append('\t').append(mx).append('\t').append(my);
            floats(out, sun.getElements());
            floats(out, camera.getElements());
            out.append('\t').append(tx).append('\t').append(ty).append('\n');
        }
        return out.toString();
    }

    private static String polygonCases(Random random) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < 3000; i++) {
            BoundingBoxPolygon polygon = new BoundingBoxPolygon();
            int range = i % 3 == 0 ? 40 : i % 3 == 1 ? 600 : 5000;
            polygon.left = random.nextInt(2 * range + 1) - range;
            polygon.right = random.nextInt(2 * range + 1) - range;
            polygon.top = random.nextInt(2 * range + 1) - range;
            polygon.bottom = random.nextInt(2 * range + 1) - range;
            if (i % 13 == 0) {
                polygon.right = polygon.left;
                polygon.top = polygon.bottom;
            }
            polygon.size = random.nextInt(i % 2 == 0 ? 60 : 900);
            polygon.positionCalculated = random.nextInt(12) != 0;
            int mx = random.nextInt(2 * range + 1) - range;
            int my = random.nextInt(2 * range + 1) - range;
            int shift = random.nextInt(3);
            polygon.size <<= shift;
            boolean hit = polygon.hovered(mx, my);
            polygon.size >>= shift;
            out.append(polygon.top).append('\t').append(polygon.left).append('\t').append(polygon.bottom).append('\t').append(polygon.right).append('\t').append(polygon.size).append('\t').append(polygon.positionCalculated ? 1 : 0).append('\t').append(shift).append('\t').append(mx).append('\t').append(my).append('\t').append(hit ? 1 : 0).append('\n');
        }
        return out.toString();
    }

    public static void main(String[] args) throws Exception {
        Path jar = Path.of(args[0]).toRealPath();
        Path work = Path.of(args[1]);
        String pinned = args[2];
        String before = hash(jar);
        if (!before.equals(pinned)) throw new IllegalArgumentException("Unexpected oracle JAR");
        checkSource(jar, MiniMenu.class);
        checkSource(jar, Matrix44.class);
        checkSource(jar, BoundingBoxPolygon.class);
        Random random = new Random(0x71C4C0DEL);
        Files.writeString(work.resolve("picking-matrix.tsv"), matrixCases(new Random(11)));
        Files.writeString(work.resolve("picking-height.tsv"), heightCases(new Random(12)));
        Files.writeString(work.resolve("picking-ray.tsv"), rayCases(new Random(13)));
        Files.writeString(work.resolve("picking-tile.tsv"), tileCases(new Random(14)));
        Files.writeString(work.resolve("picking-polygon.tsv"), polygonCases(new Random(15)));
        System.out.println("ok");
    }
}
