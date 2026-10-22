import com.jagex.game.runetek5.math.Matrix43;
import com.jagex.game.runetek5.math.Matrix44;
import com.jagex.game.runetek5.rendering.Renderers;
import com.jagex.game.runetek5.scene.MapRegion;
import com.jagex.game.runetek5.toolkit.ModernGLToolkit;
import com.jagex.game.runetek5.utils.MathUtils;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class CameraFixtures {
    static final long SEED = 0x727c5228L;
    static final int PER_KIND = 4000;
    static final String[] NAMES43 = {"xA", "xB", "xC", "xD", "yA", "yB", "yC", "yD", "zA", "zB", "zC", "zD"};
    static final int[] SPECIAL_INTS = {0, 1, -1, 0x3fff, 0x4000, -0x4000, 16383, 8192, 4096, 12288, 1024, 2048, 3072, 1960, Integer.MAX_VALUE, Integer.MIN_VALUE, 65535, -65536};
    static final int[] SIZES = {765, 503, 1920, 1080, 1, 0, -5, 334, 640, 800, 600, 3840, 2160, 1280, 720, 1 << 23, Integer.MAX_VALUE, 7};
    static final int[] FOVS = {0, 1, -1, 256, 400, 512, 600, 768, 1000, 1500, 2000, 100000, Integer.MIN_VALUE, Integer.MAX_VALUE, 1 << 30};
    static final float[] SPECIAL = {0f, -0f, 1f, -1f, 0.5f, 1e10f, -1e10f, 3e38f, -3e38f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 1e-30f, Float.MIN_VALUE, 512f, 4096f};
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

    static int fbits(float value) {
        return Float.isNaN(value) ? 0x7fc00000 : Float.floatToRawIntBits(value);
    }

    static void putFloat(Object target, String name, float value) throws Exception {
        unsafe.putFloat(target, unsafe.objectFieldOffset(field(target.getClass(), name)), value);
    }

    static float getFloat(Object target, String name) throws Exception {
        return unsafe.getFloat(target, unsafe.objectFieldOffset(field(target.getClass(), name)));
    }

    static void putInt(Object target, String name, int value) throws Exception {
        unsafe.putInt(target, unsafe.objectFieldOffset(field(target.getClass(), name)), value);
    }

    static void putBool(Object target, String name, boolean value) throws Exception {
        unsafe.putBoolean(target, unsafe.objectFieldOffset(field(target.getClass(), name)), value);
    }

    static void putObj(Object target, String name, Object value) throws Exception {
        unsafe.putObject(target, unsafe.objectFieldOffset(field(target.getClass(), name)), value);
    }

    static void setStaticInt(Class<?> type, String name, int value) throws Exception {
        var f = field(type, name);
        unsafe.putInt(unsafe.staticFieldBase(f), unsafe.staticFieldOffset(f), value);
    }

    static void setStaticObj(Class<?> type, String name, Object value) throws Exception {
        var f = field(type, name);
        unsafe.putObject(unsafe.staticFieldBase(f), unsafe.staticFieldOffset(f), value);
    }

    static Object getStaticObj(Class<?> type, String name) throws Exception {
        var f = field(type, name);
        return unsafe.getObject(unsafe.staticFieldBase(f), unsafe.staticFieldOffset(f));
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
        putFloat(toolkit, "densityFactor", 1.0f);
        putFloat(toolkit, "fogMin", 0.0f);
        putInt(toolkit, "depthClippingMode", 1);
        return toolkit;
    }

    static int randomInt(Random random) {
        return switch (random.nextInt(4)) {
            case 0 -> SPECIAL_INTS[random.nextInt(SPECIAL_INTS.length)];
            case 1 -> random.nextInt();
            case 2 -> random.nextInt(16384);
            default -> random.nextInt(20000) - 10000;
        };
    }

    static int pick(Random random, int[] values) {
        return values[random.nextInt(values.length)];
    }

    static float randomFloat(Random random) {
        return switch (random.nextInt(5)) {
            case 0 -> SPECIAL[random.nextInt(SPECIAL.length)];
            case 1 -> (float) (random.nextGaussian() * 1000);
            case 2 -> random.nextInt(8192) - 4096;
            case 3 -> Float.intBitsToFloat(random.nextInt());
            default -> random.nextFloat();
        };
    }

    static int coordinate(Random random) {
        return random.nextInt(4) == 0 ? randomInt(random) : random.nextInt(104 << 9);
    }

    static int height(Random random) {
        return random.nextInt(4) == 0 ? randomInt(random) : random.nextInt(8000) - 4000;
    }

    static int size(Random random) {
        return random.nextInt(3) == 0 ? pick(random, SIZES) : 100 + random.nextInt(3000);
    }

    static int fov(Random random) {
        return random.nextInt(3) == 0 ? pick(random, FOVS) : 200 + random.nextInt(1000);
    }

    static int viewStart(Random random) {
        return random.nextInt(3) == 0 ? random.nextInt(2000) - 500 : 200;
    }

    static int viewEnd(Random random) {
        return random.nextInt(3) == 0 ? random.nextInt(60000) - 1000 : 40000 + random.nextInt(30000);
    }

    static Matrix43 view(int x, int y, int z, int yaw, int roll, int pitch) {
        var m = new Matrix43();
        m.setViewPosition(-(float) x, -(float) z, -(float) y);
        m.rotate(0.0f, -1.0f, 0.0f, MathUtils.getTheta(-roll & 0x3fff));
        m.rotate(-1.0f, 0.0f, 0.0f, MathUtils.getTheta(-yaw & 0x3fff));
        m.rotate(0.0f, 0.0f, -1.0f, MathUtils.getTheta(-pitch & 0x3fff));
        return m;
    }

    static Matrix44 projection(int w, int h, int scaledFov, int start, int end) {
        var m = new Matrix44();
        float focal = (float) (scaledFov << 1);
        m.setPerspective((float) (w / 2), (float) (h / 2), focal, focal, (float) start, (float) end, (float) w, (float) h);
        return m;
    }

    static void bits43(StringBuilder out, Matrix43 m) throws Exception {
        for (var n : NAMES43) out.append('\t').append(fbits(getFloat(m, n)));
    }

    static void bits(StringBuilder out, float[] values) {
        for (var v : values) out.append('\t').append(fbits(v));
    }

    static void ints(StringBuilder out, int... values) {
        for (var v : values) out.append('\t').append(v);
    }

    static void emit(List<String> lines, String kind, StringBuilder in, StringBuilder out) {
        lines.add("C\t" + kind + "\t" + in.substring(1) + "\t|" + out);
    }

    public static void main(String[] args) throws Exception {
        var out = Path.of(args[0]);
        Files.createDirectories(out);
        var f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        unsafe = (sun.misc.Unsafe) f.get(null);
        var random = new Random(SEED);
        var lines = new ArrayList<String>();
        var counts = new LinkedHashMap<String, Integer>();

        for (int i = 0; i < PER_KIND; i++) {
            int rot = randomInt(random);
            var in = new StringBuilder();
            ints(in, rot);
            var o = new StringBuilder();
            bits(o, new float[] {MathUtils.getTheta(rot)});
            emit(lines, "theta", in, o);
        }
        counts.put("theta", PER_KIND);

        for (int i = 0; i < PER_KIND; i++) {
            int x = coordinate(random), y = coordinate(random), z = height(random);
            int yaw = randomInt(random), roll = randomInt(random), pitch = randomInt(random);
            var in = new StringBuilder();
            ints(in, x, y, z, yaw, roll, pitch);
            var o = new StringBuilder();
            bits43(o, view(x, y, z, yaw, roll, pitch));
            emit(lines, "pose", in, o);
        }
        counts.put("pose", PER_KIND);

        for (int i = 0; i < PER_KIND; i++) {
            int w = size(random), h = size(random), fv = fov(random), start = viewStart(random), end = viewEnd(random);
            var m = projection(w, h, fv, start, end);
            var in = new StringBuilder();
            ints(in, w, h, fv, start, end);
            var o = new StringBuilder();
            bits(o, m.getElements());
            bits(o, new float[] {m.getNearClipDistance(), m.getDepthPerspectiveCD()});
            emit(lines, "proj", in, o);
        }
        counts.put("proj", PER_KIND);

        for (int i = 0; i < PER_KIND; i++) {
            int x = coordinate(random), y = coordinate(random), z = height(random);
            int yaw = random.nextInt(4) == 0 ? randomInt(random) : 1024 + random.nextInt(2048);
            int roll = random.nextInt(4) == 0 ? randomInt(random) : random.nextInt(16384);
            int pitch = random.nextInt(4) == 0 ? randomInt(random) : random.nextInt(32) - 16;
            int w = size(random), h = size(random), fv = fov(random), start = viewStart(random), end = viewEnd(random);
            var toolkit = toolkit();
            var proj = projection(w, h, fv, start, end);
            toolkit.currentModelMatrix.copyFrom(proj);
            toolkit.viewMatrix43.copyFrom(view(x, y, z, yaw, roll, pitch));
            toolkit.viewMatrix44.fromMatrix(toolkit.viewMatrix43);
            toolkit.updateViewProjectionMatrix();
            var in = new StringBuilder();
            ints(in, x, y, z, yaw, roll, pitch, w, h, fv, start, end);
            var o = new StringBuilder();
            bits(o, toolkit.viewMatrix44.getElements());
            var vp = toolkit.viewProjectionMatrix.getElements().clone();
            bits(o, vp);
            vp[1] = -vp[1];
            vp[5] = -vp[5];
            vp[9] = -vp[9];
            vp[13] = -vp[13];
            bits(o, vp);
            emit(lines, "vp", in, o);
        }
        counts.put("vp", PER_KIND);

        for (int i = 0; i < PER_KIND; i++) {
            var toolkit = toolkit();
            float[] model;
            var in = new StringBuilder();
            if (random.nextInt(5) == 0) {
                model = new float[16];
                for (int k = 0; k < 16; k++) model[k] = randomFloat(random);
            } else {
                int w = size(random), h = size(random), fv = fov(random), start = viewStart(random), end = viewEnd(random);
                model = projection(w, h, fv, start, end).getElements().clone();
            }
            int density = random.nextInt(3) == 0 ? randomInt(random) : (random.nextInt(2) == 0 ? -1 : (random.nextInt(2048) + 256) << 2);
            int offset = random.nextInt(3) == 0 ? randomInt(random) : 0;
            int color = random.nextInt(3) == 0 ? random.nextInt() : (random.nextBoolean() ? 13156520 : random.nextInt(1 << 24));
            boolean enabled = random.nextBoolean();
            System.arraycopy(model, 0, toolkit.currentModelMatrix.getElements(), 0, 16);
            toolkit.fogColor = color;
            toolkit.fogDensity = density;
            toolkit.fogOffset = offset;
            putInt(toolkit, "depthClippingMode", 0);
            toolkit.recalculateFogAndProjection();
            bits(in, model);
            ints(in, enabled ? 1 : 0, color, density, offset);
            var o = new StringBuilder();
            bits(o, new float[] {toolkit.fogStartPosition, toolkit.fogEndPosition});
            o.append('\t').append(enabled && density >= 0 ? 1 : 0);
            bits(o, new float[] {((color >> 16) & 0xff) / 255f, ((color >> 8) & 0xff) / 255f, (color & 0xff) / 255f});
            emit(lines, "fog", in, o);
        }
        counts.put("fog", PER_KIND);

        var region = (MapRegion) unsafe.allocateInstance(MapRegion.class);
        var activeToolkit = toolkit();
        setStaticObj(Renderers.class, "activeToolkit", activeToolkit);
        Class<?> loader = Class.forName("com.jagex.game.launch.Loader");
        for (int i = 0; i < PER_KIND; i++) {
            int sizeX = random.nextInt(3) == 0 ? randomInt(random) : pick(random, new int[] {104, 120, 136, 168, 72, 88});
            int mult = random.nextInt(3) == 0 ? randomInt(random) : 10;
            putInt(region, "sceneSizeX", sizeX);
            setStaticInt(loader, "FOG_DISTANCE_MULTIPLIER", mult);
            region.calculateViewRange();
            var in = new StringBuilder();
            ints(in, sizeX, mult);
            var o = new StringBuilder();
            ints(o, region.getViewRangeStart(), region.getViewRangeEnd());
            emit(lines, "range", in, o);
        }
        counts.put("range", PER_KIND);

        for (int i = 0; i < PER_KIND; i++) {
            int depth = random.nextInt(3) == 0 ? randomInt(random) : random.nextInt(2048) - 256;
            boolean on = random.nextBoolean();
            int density = on ? (depth + 256 << 2) : -1;
            var in = new StringBuilder();
            ints(in, depth, on ? 1 : 0);
            var o = new StringBuilder();
            ints(o, density);
            emit(lines, "density", in, o);
        }
        counts.put("density", PER_KIND);

        for (int i = 0; i < PER_KIND; i++) {
            int argb = random.nextInt(3) == 0 ? pick(random, SPECIAL_INTS) : random.nextInt();
            var in = new StringBuilder();
            ints(in, argb);
            var o = new StringBuilder();
            bits(o, new float[] {(argb & 0xff0000) / 1.671168E7f, (argb & 0xff00) / 65280.0f, (argb & 0xff) / 255.0f, (argb >>> 24) / 255.0f});
            emit(lines, "clear", in, o);
        }
        counts.put("clear", PER_KIND);

        lines.add("T\t" + lines.size());
        Files.write(out.resolve("camera.native.tsv"), (String.join("\n", lines) + "\n").getBytes(StandardCharsets.UTF_8));
        var stats = new StringBuilder();
        counts.forEach((k, v) -> stats.append(k).append('\t').append(v).append('\n'));
        Files.write(out.resolve("camera.stats.tsv"), stats.toString().getBytes(StandardCharsets.UTF_8));
    }
}
