import com.jagex.game.runetek5.config.material.MaterialTypeList;
import com.jagex.game.runetek5.graphics.model.Mesh;
import com.jagex.game.runetek5.graphics.model.ModelGL;
import com.jagex.game.runetek5.graphics.model.billboard.Billboard;
import com.jagex.game.runetek5.graphics.model.billboard.BillboardType;
import com.jagex.game.runetek5.graphics.model.billboard.BillboardTypeList;
import com.jagex.game.runetek5.graphics.modern.batch.ModelCache;
import com.jagex.game.runetek5.js5.Js5MasterIndex;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.toolkit.ModernGLToolkit;
import com.jagex.game.runetek5.utils.ColorUtil;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public final class ModelOracle {
    private static final String PINNED = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048";

    private static final Map<Integer, byte[]> models = new HashMap<>();
    private static sun.misc.Unsafe unsafe;
    private static ModernGLToolkit toolkit;
    private static Object scratch;
    private static Method build;

    private static String hash(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }

    private static String digest(byte[] data) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)).substring(0, 12);
    }

    private static void ints(DataOutputStream out, int[] values) throws Exception {
        if (values == null) {
            out.writeByte(0);
            return;
        }
        out.writeByte(1);
        out.writeInt(values.length);
        for (int value : values) out.writeInt(value);
    }

    private static void shorts(DataOutputStream out, short[] values) throws Exception {
        if (values == null) {
            out.writeByte(0);
            return;
        }
        out.writeByte(1);
        out.writeInt(values.length);
        for (short value : values) out.writeShort(value);
    }

    private static void bytes(DataOutputStream out, byte[] values) throws Exception {
        if (values == null) {
            out.writeByte(0);
            return;
        }
        out.writeByte(1);
        out.writeInt(values.length);
        out.write(values);
    }

    private static void floats(DataOutputStream out, float[] values) throws Exception {
        if (values == null) {
            out.writeByte(0);
            return;
        }
        out.writeByte(1);
        out.writeInt(values.length);
        for (float value : values) out.writeInt(Float.floatToIntBits(value));
    }

    private static void groups(DataOutputStream out, int[][] values) throws Exception {
        if (values == null) {
            out.writeByte(0);
            return;
        }
        out.writeByte(1);
        out.writeInt(values.length);
        for (int[] group : values) ints(out, group);
    }

    private static Mesh mesh(String spec) {
        String[] ids = spec.split(",");
        if (ids.length == 1) return new Mesh(models.get(Integer.parseInt(ids[0])));
        Mesh[] parts = new Mesh[ids.length];
        for (int i = 0; i < ids.length; i++) parts[i] = new Mesh(models.get(Integer.parseInt(ids[i])));
        return new Mesh(parts, parts.length);
    }

    private static MaterialTypeList materials(byte[] payload) throws Exception {
        Js5MasterIndex index = (Js5MasterIndex) unsafe.allocateInstance(Js5MasterIndex.class);
        index.fileCounts = new int[] {1};
        Js5ResourceProvider provider = (Js5ResourceProvider) unsafe.allocateInstance(Js5ResourceProvider.class);
        provider.masterIndex = index;
        provider.archiveFiles = new Object[][] {new Object[] {payload}};
        provider.archives = new Object[1];
        return new MaterialTypeList(provider, provider, provider);
    }

    private static void setField(Class<?> type, Object target, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void setupToolkit(MaterialTypeList table) throws Exception {
        toolkit = (ModernGLToolkit) unsafe.allocateInstance(ModernGLToolkit.class);
        setField(AbstractToolkit.class, toolkit, "materialProvider", table);
        setField(ModernGLToolkit.class, toolkit, "legacyCaptureDroppedFaces", new AtomicLong());
        setField(ModernGLToolkit.class, toolkit, "legacyCaptureModelsWithDrops", new AtomicLong());
        setField(ModernGLToolkit.class, toolkit, "legacyCaptureModelsFullyDropped", new AtomicLong());
        setField(ModernGLToolkit.class, toolkit, "EMPTY_VERTEX_DATA", ByteBuffer.allocate(0).order(ByteOrder.nativeOrder()));
        Class<?> scratchClass = Class.forName("com.jagex.game.runetek5.toolkit.ModernGLToolkit$BuildScratch");
        Constructor<?> constructor = scratchClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        scratch = constructor.newInstance();
        build = ModernGLToolkit.class.getDeclaredMethod("buildModelCache", ModelGL.class, boolean.class, ModelCache.CachedModel.class, scratchClass);
        build.setAccessible(true);
    }

    private static String palette(short[] values) {
        if (values == null) return "-";
        java.util.LinkedHashSet<Integer> seen = new java.util.LinkedHashSet<>();
        for (short value : values) {
            seen.add((int) value);
            if (seen.size() == 6) break;
        }
        StringBuilder text = new StringBuilder();
        for (int value : seen) text.append(text.length() == 0 ? "" : ",").append(value);
        return text.length() == 0 ? "-" : text.toString();
    }

    private static boolean uid(int mode, int id) {
        return mode == 1 || (mode == 2 && id % 2 == 0);
    }

    private static void injectBillboards(Mesh mesh, int mode, int material) {
        BillboardTypeList.BILLBOARD_CACHE.reset();
        if (mesh.billboards == null || mode == 0) return;
        for (Billboard billboard : mesh.billboards) {
            BillboardType type = new BillboardType();
            type.hasUid = uid(mode, billboard.billboardId);
            type.materialId = material;
            BillboardTypeList.BILLBOARD_CACHE.put(type, billboard.billboardId);
        }
    }

    private static String state(ModelGL m) throws Exception {
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(raw);
        out.writeInt(m.numVertices);
        out.writeInt(m.maxDepth);
        out.writeInt(m.numNormals);
        out.writeInt(m.numFaces);
        out.writeInt(m.renderedFaceCount);
        ints(out, m.meshFaceIndices);
        ints(out, m.triangles);
        ints(out, m.verticesX);
        ints(out, m.verticesY);
        ints(out, m.verticesZ);
        shorts(out, m.normalsX);
        shorts(out, m.normalsY);
        shorts(out, m.normalsZ);
        bytes(out, m.magnitudes);
        floats(out, m.textureU);
        floats(out, m.textureV);
        shorts(out, m.facesColor);
        bytes(out, m.facesAlpha);
        shorts(out, m.facesA);
        shorts(out, m.facesB);
        shorts(out, m.facesC);
        shorts(out, m.facesMaterial);
        shorts(out, m.facesSubmeshes);
        shorts(out, m.vertexNormalIndices);
        ints(out, m.materialBatchOffsets);
        out.writeInt(m.ambient);
        out.writeInt(m.contrast);
        out.writeByte(m.hasAlphaTransition ? 1 : 0);
        out.writeByte(m.textureAnimating ? 1 : 0);
        groups(out, m.vertexGroups);
        groups(out, m.billboardGroups);
        groups(out, m.faceGroups);
        return digest(raw.toByteArray());
    }

    private static String mergedState(ModelGL m) throws Exception {
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(raw);
        if (m.mergedNormals == null) {
            out.writeByte(0);
        } else {
            out.writeByte(1);
            shorts(out, m.mergedNormals.normalsX);
            shorts(out, m.mergedNormals.normalsY);
            shorts(out, m.mergedNormals.normalsZ);
            bytes(out, m.mergedNormals.magnitude);
        }
        return digest(raw.toByteArray());
    }

    private static String shaded(String[] f) throws Exception {
        ModelGL[] pair = new ModelGL[2];
        for (int i = 0; i < 2; i++) {
            Mesh mesh = mesh(f[1 + i]);
            injectBillboards(mesh, 0, -1);
            pair[i] = new ModelGL(toolkit, mesh, Integer.parseInt(f[3 + i]), Integer.parseInt(f[5]), Integer.parseInt(f[6]), Integer.parseInt(f[7]));
        }
        if (!f[8].equals("-")) {
            for (String op : f[8].split(";")) {
                String[] p = op.split(":");
                switch (p[0]) {
                    case "x" -> pair[0].applyShading(pair[1], Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]), false);
                    case "y" -> pair[1].applyShading(pair[0], Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]), false);
                    case "s" -> pair[0].applyShading(Integer.parseInt(p[1]));
                    case "u" -> pair[1].applyShading(Integer.parseInt(p[1]));
                    default -> throw new IllegalArgumentException(op);
                }
            }
        }
        return state(pair[0]) + "/" + built(pair[0]) + "/" + mergedState(pair[0]) + "|" + state(pair[1]) + "/" + built(pair[1]) + "/" + mergedState(pair[1]);
    }

    private static String built(ModelGL m) throws Exception {
        Object result;
        try {
            result = build.invoke(toolkit, m, true, null, scratch);
        } catch (java.lang.reflect.InvocationTargetException error) {
            return "error";
        }
        if (result == null) return "null";
        ModelCache.CachedModel cached = (ModelCache.CachedModel) result;
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(raw);
        out.writeInt(cached.getVertexCount());
        out.writeInt(cached.getFaceCount());
        out.writeInt(cached.vertexWordCount);
        for (int i = 0; i < cached.vertexWordCount; i++) out.writeInt(Float.isNaN(Float.intBitsToFloat(cached.vertexWords[i])) ? 0x7fc00000 : cached.vertexWords[i]);
        out.writeInt(cached.getMaterialBatches().length);
        for (ModelCache.MaterialBatch batch : cached.getMaterialBatches()) {
            out.writeInt(batch.getMaterialId());
            out.writeInt(batch.getIndexCount());
            for (int i = 0; i < batch.getIndexCount(); i++) out.writeInt(batch.getIndices()[i]);
        }
        return digest(raw.toByteArray());
    }

    private static void apply(ModelGL m, String op) {
        String[] p = op.split(":");
        switch (p[0]) {
            case "r" -> m.recolor((short) Integer.parseInt(p[1]), (short) Integer.parseInt(p[2]));
            case "m" -> m.rematerial((short) Integer.parseInt(p[1]), (short) Integer.parseInt(p[2]));
            case "t" -> m.tint(Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]), Integer.parseInt(p[4]));
            case "a" -> m.setAmbient(Integer.parseInt(p[1]));
            case "c" -> m.setContrast(Integer.parseInt(p[1]));
            case "e" -> m.applyEnvironmentAlpha((byte) Integer.parseInt(p[1]), null);
            case "f" -> {
                byte[] transparency = new byte[m.numFaces];
                int k = Integer.parseInt(p[2]);
                for (int i = 0; i < transparency.length; i++) transparency[i] = (byte) (i * k + 7);
                m.applyEnvironmentAlpha((byte) Integer.parseInt(p[1]), transparency);
            }
            case "s" -> m.applyShading(Integer.parseInt(p[1]));
            default -> throw new IllegalArgumentException(op);
        }
    }

    private static void readBundle(DataInputStream in) throws Exception {
        int count = in.readInt();
        for (int i = 0; i < count; i++) {
            int id = in.readInt();
            byte[] data = new byte[in.readInt()];
            in.readFully(data);
            models.put(id, data);
        }
    }

    public static void main(String[] args) throws Exception {
        Path jar = Path.of(args[0]).toRealPath();
        if (!hash(jar).equals(PINNED)) throw new IllegalStateException("jar differs from the pinned oracle");
        for (Class<?> type : new Class<?>[] {Mesh.class, ModelGL.class, ModernGLToolkit.class, MaterialTypeList.class, BillboardTypeList.class, ColorUtil.class}) {
            Path loaded = Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
            if (!jar.equals(loaded)) throw new IllegalStateException(type.getName() + " code source mismatch");
        }
        try (DataInputStream in = new DataInputStream(new java.io.BufferedInputStream(Files.newInputStream(Path.of(args[1])), 1 << 20))) {
            readBundle(in);
        }
        byte[] payload = Files.readAllBytes(Path.of(args[2]));
        Field unsafeField = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        unsafe = (sun.misc.Unsafe) unsafeField.get(null);
        Class<?> holder = Class.forName(Math.class.getName() + "$RandomNumberGeneratorHolder");
        Field randomField = holder.getDeclaredField("randomNumberGenerator");
        randomField.setAccessible(true);
        ((Random) randomField.get(null)).setSeed(0);
        ColorUtil.calculateHslToRgbTable();
        MaterialTypeList table = materials(payload);
        setupToolkit(table);

        List<String> cases = Files.readAllLines(Path.of(args[3]), StandardCharsets.US_ASCII);
        int total = 0;
        int errors = 0;
        int nulls = 0;
        try (BufferedWriter writer = Files.newBufferedWriter(Path.of(args[4]), StandardCharsets.US_ASCII)) {
            for (String line : cases) {
                if (line.isEmpty()) continue;
                String[] f = line.split("\t");
                String result;
                try {
                    if (f[0].equals("X")) {
                        result = shaded(f);
                        total++;
                        writer.write(line + "\t" + result + "\n");
                        continue;
                    }
                    Mesh mesh = mesh(f[1]);
                    if (f[0].equals("V")) {
                        writer.write(line + "\t" + palette(mesh.facesColor) + "|" + palette(mesh.facesMaterial) + "|"
                            + (mesh.billboards == null ? 0 : 1) + "\n");
                        continue;
                    }
                    injectBillboards(mesh, Integer.parseInt(f[6]), Integer.parseInt(f[7]));
                    ModelGL m = new ModelGL(toolkit, mesh, Integer.parseInt(f[2]), Integer.parseInt(f[3]), Integer.parseInt(f[4]), Integer.parseInt(f[5]));
                    if (!f[8].equals("-")) for (String op : f[8].split(";")) apply(m, op);
                    String s = state(m);
                    String b = built(m);
                    if (b.equals("error")) errors++;
                    if (b.equals("null")) nulls++;
                    result = s + "/" + b;
                } catch (Throwable error) {
                    result = "error";
                    errors++;
                }
                total++;
                writer.write(line + "\t" + result + "\n");
            }
        }
        System.out.println("cases " + total + " errors " + errors + " nullBuilds " + nulls);
        if (!hash(jar).equals(PINNED)) throw new IllegalStateException("jar changed during the run");
    }
}
