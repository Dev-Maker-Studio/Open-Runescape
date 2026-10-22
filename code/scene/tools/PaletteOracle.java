import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.config.flu.FluType;
import com.jagex.game.runetek5.config.flu.FluTypeList;
import com.jagex.game.runetek5.js5.Js5MasterIndex;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.scene.MapSize;
import com.jagex.game.runetek5.scene.Scene;
import com.jagex.game.runetek5.scene.ground.Ground;
import com.jagex.game.runetek5.scene.ground.GroundSM;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.toolkit.SafeModeToolkit;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.jetbrains.org.objectweb.asm.ClassReader;
import org.jetbrains.org.objectweb.asm.ClassVisitor;
import org.jetbrains.org.objectweb.asm.ClassWriter;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Opcodes;
import sun.misc.Unsafe;

public final class PaletteOracle {
    static final String LOADER = "com.jagex.game.runetek5.scene.MapLoader";
    static final String LOADER_PATH = LOADER.replace('.', '/') + ".class";
    static final String CAPTURE_DESCRIPTOR = "(Lcom/jagex/game/runetek5/scene/ground/Ground;I[[ILcom/jagex/game/runetek5/scene/ground/Ground;Lcom/jagex/game/runetek5/scene/ground/Ground;)V";
    static final String PALETTE_DESCRIPTOR = "(Lcom/jagex/game/runetek5/toolkit/AbstractToolkit;Lcom/jagex/game/runetek5/scene/ground/Ground;Lcom/jagex/game/runetek5/scene/ground/Ground;Z)V";
    static final int[] RGB = {0x123456, 0xff0000, 0x00ff00, 0x0000ff, 0xff00ff, 0xffff00, 0x00ffff, 0x000000, 0xffffff, 0xb3b3b3, 0xc0c0c0, 0xc1c1c1, 0xd9d9d9, 0xdadada, 0xf3f3f3, 0xf4f4f4};
    static final String[] PATTERNS = {"empty", "uniform", "checker", "sparse", "default", "all-default", "carry", "overflow", "divide", "edge", "bytexff", "thresholds"};
    static final int[][] STRESS = {
        {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 1},
        {Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, 3},
        {0x40000001, -100000003, 200000009, 0x40000003},
        {-0x40000003, 100000007, -200000011, 0x40000007},
        {-123456789, -257, 243, 5},
        {123456791, 259, 244, 7}
    };
    static final Unsafe UNSAFE;
    static int[][][] captured;
    static boolean[] capturedPlanes;

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSAFE = (Unsafe) field.get(null);
        } catch (ReflectiveOperationException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    public static void capture(int plane, int[][] palette) {
        if (capturedPlanes[plane]) {
            throw new IllegalStateException("native submitted a plane twice");
        }
        capturedPlanes[plane] = true;
        captured[plane] = new int[palette.length][];
        for (int x = 0; x < palette.length; x++) {
            captured[plane][x] = palette[x].clone();
        }
    }

    static String digest(byte[] data) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
    }

    static byte[] classBytes(String path) throws Exception {
        try (InputStream input = PaletteOracle.class.getClassLoader().getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException("missing oracle class " + path);
            }
            return input.readAllBytes();
        }
    }

    static byte[] methodBytes(byte[] original, String name, String descriptor) {
        ClassWriter writer = new ClassWriter(0);
        new ClassReader(original).accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String methodName, String methodDescriptor, String signature, String[] exceptions) {
                if (methodName.equals(name) && methodDescriptor.equals(descriptor)) {
                    return super.visitMethod(access, methodName, methodDescriptor, signature, exceptions);
                }
                return null;
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return writer.toByteArray();
    }

    static byte[] instrument(byte[] original) throws Exception {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        int[] replaced = {0};
        new ClassReader(original).accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor method = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!name.equals("addRegularTile") || !descriptor.equals(CAPTURE_DESCRIPTOR)) {
                    return method;
                }
                replaced[0]++;
                method.visitCode();
                method.visitVarInsn(Opcodes.ILOAD, 2);
                method.visitVarInsn(Opcodes.ALOAD, 3);
                method.visitMethodInsn(Opcodes.INVOKESTATIC, "PaletteOracle", "capture", "(I[[I)V", false);
                method.visitInsn(Opcodes.RETURN);
                method.visitMaxs(0, 0);
                method.visitEnd();
                return null;
            }
        }, 0);
        if (replaced[0] != 1) {
            throw new IllegalStateException("expected one addRegularTile capture seam");
        }
        byte[] transformed = writer.toByteArray();
        if (!MessageDigest.isEqual(methodBytes(original, "calculateUnderlayPalette", PALETTE_DESCRIPTOR), methodBytes(transformed, "calculateUnderlayPalette", PALETTE_DESCRIPTOR))) {
            throw new IllegalStateException("palette method changed during downstream instrumentation");
        }
        return transformed;
    }

    static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    static FluType custom(int[] fields) {
        FluType result = new FluType();
        result.hue = fields[0];
        result.saturation = fields[1];
        result.lightness = fields[2];
        result.hueMultiplier = fields[3];
        return result;
    }

    static FluTypeList definitions(StringBuilder rows) throws Exception {
        Js5MasterIndex master = (Js5MasterIndex) UNSAFE.allocateInstance(Js5MasterIndex.class);
        master.fileCounts = new int[0];
        Js5ResourceProvider resource = (Js5ResourceProvider) UNSAFE.allocateInstance(Js5ResourceProvider.class);
        resource.masterIndex = master;
        FluTypeList list = new FluTypeList(resource);
        for (int id = 0; id < RGB.length; id++) {
            FluType underlay = new FluType();
            underlay.calculateHsl16(RGB[id]);
            list.cache.put(underlay, id);
            definitionRow(rows, id, "rgb", String.format("%06x", RGB[id]), underlay);
        }
        FluType missing = list.list(16);
        if (missing.hue != 0 || missing.saturation != 0 || missing.lightness != 0 || missing.hueMultiplier != 0) {
            throw new IllegalStateException("native absent FluType is not the constructor default");
        }
        definitionRow(rows, 16, "absent", "-", missing);
        for (int offset = 0; offset < STRESS.length; offset++) {
            FluType underlay = custom(STRESS[offset]);
            list.cache.put(underlay, 20 + offset);
            definitionRow(rows, 20 + offset, "custom", "-", underlay);
        }
        for (int offset = 0; offset < 8; offset++) {
            FluType underlay = custom(new int[]{123, 255, new int[]{179, 180, 192, 193, 217, 218, 243, 244}[offset], 5});
            list.cache.put(underlay, 30 + offset);
            definitionRow(rows, 30 + offset, "custom", "-", underlay);
        }
        FluType signed = custom(new int[]{-7, -13, 27, 11});
        list.cache.put(signed, 40);
        definitionRow(rows, 40, "custom", "-", signed);
        FluType high = new FluType();
        high.calculateHsl16(0xff00ff);
        list.cache.put(high, 254);
        definitionRow(rows, 254, "rgb", "ff00ff", high);
        return list;
    }

    static void definitionRow(StringBuilder rows, int id, String kind, String payload, FluType value) {
        rows.append("definition\t").append(id).append('\t').append(kind).append('\t').append(payload).append('\t').append(value.hue).append('\t').append(value.saturation).append('\t').append(value.lightness).append('\t').append(value.hueMultiplier).append('\n');
    }

    static int tileId(String pattern, int plane, int x, int y, int size) {
        return switch (pattern) {
            case "empty" -> 0;
            case "uniform" -> 1;
            case "checker" -> {
                int value = (plane * 53 + x * 17 + y * 31) % 19;
                yield value == 18 ? 255 : value;
            }
            case "sparse" -> {
                boolean point = x == y && (x == 0 || x == 4 || x == 5 || x == 9 || x == 10 || x == 31 || x == 63 || x == 64 || x == size - 1) || x == 0 && y == size - 1 || x == size - 1 && y == 0;
                yield point && plane < 3 ? 1 + (plane * 3 + x + y) % 16 : 0;
            }
            case "default" -> (x + y + plane) % 2 == 0 ? 1 : 17;
            case "all-default" -> 17;
            case "carry" -> plane == 0 ? 1 + (x * 7 + y * 11) % 16 : plane == 1 ? 0 : plane == 2 ? 17 : x < size / 2 && y < size / 2 ? 5 : 0;
            case "overflow" -> (plane + x + y) % 13 == 0 ? 0 : 21 + (plane * 7 + x * 11 + y * 3) % 6;
            case "divide" -> (x + y + plane) % 3 == 0 ? 0 : (x + 2 * y) % 2 == 0 ? 41 : 1;
            case "edge" -> x == 20 + plane * 5 && y == 20 ? 2 : x == 20 && y == 20 + plane * 5 ? 5 : 0;
            case "bytexff" -> 255;
            case "thresholds" -> 31 + (plane + x / 12 + y / 12) % 8;
            default -> throw new IllegalArgumentException(pattern);
        };
    }

    static long paletteHash(int[][] palette) {
        long hash = 0xcbf29ce484222325L;
        for (int[] column : palette) {
            for (int value : column) {
                for (int b = 0; b < 4; b++) {
                    hash = (hash ^ ((value >>> (b * 8)) & 255)) * 0x100000001b3L;
                }
            }
        }
        return hash;
    }

    static void appendCase(StringBuilder rows, Class<?> loaderClass, FluTypeList definitions, int caseId, int size, String pattern) throws Exception {
        Object loader = UNSAFE.allocateInstance(loaderClass);
        set(loader, "regionSizeZ", 4);
        set(loader, "regionSizeX", size);
        set(loader, "regionSizeY", size);
        set(loader, "underlayTypeList", definitions);
        set(loader, "overlayHidden", true);
        set(loader, "blendFloor", false);
        Scene scene = (Scene) UNSAFE.allocateInstance(Scene.class);
        Ground ground = (Ground) UNSAFE.allocateInstance(GroundSM.class);
        scene.localHeightmap = new Ground[]{ground, ground, ground, ground};
        set(loader, "scene", scene);
        byte[][][] ids = new byte[4][size][size];
        for (int p = 0; p < 4; p++) {
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    ids[p][x][y] = (byte) tileId(pattern, p, x, y, size);
                }
            }
        }
        set(loader, "underlayIds", ids);
        for (String name : new String[]{"overlayIds", "overlayPathShapes", "overlayRotations"}) {
            set(loader, name, new byte[4][size][size]);
        }
        captured = new int[4][][];
        capturedPlanes = new boolean[4];
        AbstractToolkit toolkit = (AbstractToolkit) UNSAFE.allocateInstance(SafeModeToolkit.class);
        Method calculate = loaderClass.getMethod("calculateUnderlayPalette", AbstractToolkit.class, Ground.class, Ground.class, boolean.class);
        calculate.invoke(loader, toolkit, null, null, true);
        rows.append("case\t").append(caseId).append('\t').append(size).append('\t').append(pattern).append('\n');
        int[] coordinates = {0, 1, 4, 5, 9, 10, 14, 15, 16, 19, 20, 21, 24, 25, 26, 29, 30, 31, 32, 34, 35, 36, 39, 40, 41, 63, 64, 65, size - 6, size - 5, size - 4, size - 1};
        for (int p = 0; p < 4; p++) {
            if (!capturedPlanes[p]) {
                throw new IllegalStateException("native did not submit plane " + p);
            }
            rows.append("plane-hash\t").append(caseId).append('\t').append(p).append('\t').append(HexFormat.of().toHexDigits(paletteHash(captured[p]))).append('\n');
            for (int i = 0; i < coordinates.length; i++) {
                int x = coordinates[i];
                int y = coordinates[(i * 7 + 3) % coordinates.length];
                rows.append("sample\t").append(caseId).append('\t').append(p).append('\t').append(x).append('\t').append(y).append('\t').append(captured[p][x][y]).append('\n');
            }
            for (int coordinate : coordinates) {
                rows.append("sample\t").append(caseId).append('\t').append(p).append('\t').append(coordinate).append("\t20\t").append(captured[p][coordinate][20]).append('\n');
                rows.append("sample\t").append(caseId).append('\t').append(p).append("\t20\t").append(coordinate).append('\t').append(captured[p][20][coordinate]).append('\n');
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Path jar = Path.of(args[0]).toRealPath();
        StringBuilder rows = new StringBuilder();
        rows.append("jar-sha256\t").append(digest(Files.readAllBytes(jar))).append('\n');
        byte[] original = classBytes(LOADER_PATH);
        rows.append("class-sha256\t").append(LOADER).append('\t').append(digest(original)).append('\n');
        rows.append("palette-method-sha256\t").append(digest(methodBytes(original, "calculateUnderlayPalette", PALETTE_DESCRIPTOR))).append('\n');
        for (Class<?> dependency : new Class<?>[]{FluType.class, FluTypeList.class, com.jagex.game.runetek5.utils.ColorUtil.class}) {
            rows.append("class-sha256\t").append(dependency.getName()).append('\t').append(digest(classBytes(dependency.getName().replace('.', '/') + ".class"))).append('\n');
        }
        rows.append("capture-seam\taddRegularTile\t").append(CAPTURE_DESCRIPTOR).append('\n');
        byte[] transformed = instrument(original);
        try (URLClassLoader isolated = new URLClassLoader(new URL[]{jar.toUri().toURL()}, PaletteOracle.class.getClassLoader()) {
            @Override
            public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (!name.equals(LOADER)) {
                    return super.loadClass(name, resolve);
                }
                synchronized (getClassLoadingLock(name)) {
                    Class<?> loaded = findLoadedClass(name);
                    if (loaded == null) {
                        loaded = defineClass(name, transformed, 0, transformed.length);
                    }
                    if (resolve) {
                        resolveClass(loaded);
                    }
                    return loaded;
                }
            }
        }) {
            Class<?> loaderClass = isolated.loadClass(LOADER);
            FluTypeList list = definitions(rows);
            int caseId = 0;
            for (int id = 0; id < 5; id++) {
                int size = MapSize.get(id).size;
                rows.append("map-size\t").append(id).append('\t').append(size).append('\n');
                for (String pattern : PATTERNS) {
                    appendCase(rows, loaderClass, list, caseId++, size, pattern);
                }
            }
        }
        Files.writeString(Path.of(args[1]), rows);
        System.out.println("NATIVE_PALETTE_ORACLE_OK rows=" + rows.toString().lines().count());
    }
}
