import com.jagex.game.runetek5.config.material.MaterialTypeList;
import com.jagex.game.runetek5.graphics.model.Mesh;
import com.jagex.game.runetek5.graphics.model.billboard.Billboard;
import com.jagex.game.runetek5.graphics.model.billboard.BillboardType;
import com.jagex.game.runetek5.graphics.model.billboard.BillboardTypeList;
import com.jagex.game.runetek5.graphics.modern.util.ModernModelBuilder;
import com.jagex.game.runetek5.js5.Js5MasterIndex;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.utils.ColorUtil;
import java.io.DataInputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.zip.CRC32;
import sun.misc.Unsafe;

public final class MeshOracle {
    private static final String PINNED = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048";
    private static final long BASIS = 0xcbf29ce484222325L;
    private static final long PRIME = 0x100000001b3L;
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

    private static Map<String, Object> object(Object... entries) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) result.put((String) entries[i], entries[i + 1]);
        return result;
    }

    private static String json(Object value) {
        if (value == null) return "null";
        if (value instanceof String text) return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        StringBuilder result = new StringBuilder();
        if (value instanceof Map<?, ?> map) {
            result.append('{');
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (result.length() > 1) result.append(',');
                result.append(json(entry.getKey())).append(':').append(json(entry.getValue()));
            }
            return result.append('}').toString();
        }
        result.append('[');
        if (value instanceof Iterable<?> list) {
            for (Object entry : list) {
                if (result.length() > 1) result.append(',');
                result.append(json(entry));
            }
        } else if (value.getClass().isArray()) {
            for (int i = 0; i < Array.getLength(value); i++) {
                if (i != 0) result.append(',');
                result.append(json(Array.get(value, i)));
            }
        } else throw new IllegalArgumentException(value.getClass().getSimpleName());
        return result.append(']').toString();
    }

    private static long hashInt(long hash, int value) {
        for (int shift = 0; shift < 32; shift += 8) hash = (hash ^ ((value >>> shift) & 255)) * PRIME;
        return hash;
    }

    private static long hashBytes(long hash, byte[] bytes) {
        for (byte value : bytes) hash = (hash ^ (value & 255)) * PRIME;
        return hash;
    }

    private static long hashArray(long hash, Object array) {
        if (array == null) return hashInt(hash, -1);
        int length = Array.getLength(array);
        hash = hashInt(hash, length);
        for (int i = 0; i < length; i++) hash = hashInt(hash, ((Number) Array.get(array, i)).intValue());
        return hash;
    }

    private static long meshHash(Mesh mesh) {
        long h = BASIS;
        h = hashInt(h, mesh.version);
        h = hashInt(h, mesh.numVertices);
        h = hashInt(h, mesh.maxDepth);
        h = hashInt(h, mesh.numFaces);
        h = hashInt(h, mesh.priority);
        h = hashInt(h, mesh.numTextures);
        Object[] arrays = {mesh.texturesMapping, mesh.verticesX, mesh.verticesY, mesh.verticesZ, mesh.facesA, mesh.facesB, mesh.facesC,
            mesh.vertexGroups, mesh.faceRenderType, mesh.facesLayer, mesh.facesAlpha, mesh.faceGroups, mesh.facesMaterial, mesh.facesColor,
            mesh.facesTexture, mesh.textureFaceX, mesh.textureFaceY, mesh.textureFaceZ, mesh.texturesScaleX, mesh.texturesScaleY,
            mesh.texturesScaleZ, mesh.texturesRotation, mesh.texturesDirection, mesh.texturesSpeed, mesh.texturesUtrans, mesh.texturesVtrans};
        for (Object array : arrays) h = hashArray(h, array);
        if (mesh.billboards == null) h = hashInt(h, -1);
        else {
            h = hashInt(h, mesh.billboards.length);
            for (Billboard b : mesh.billboards) {
                h = hashInt(h, b.billboardId);
                h = hashInt(h, b.appliesOnFace);
                h = hashInt(h, b.depth);
                h = hashInt(h, b.distance);
            }
        }
        h = hashInt(h, mesh.particleEmitters == null ? -1 : mesh.particleEmitters.length);
        h = hashInt(h, mesh.particleEffectors == null ? -1 : mesh.particleEffectors.length);
        return h;
    }

    private static MaterialTypeList materials(byte[] payload) throws Exception {
        Js5MasterIndex index = (Js5MasterIndex) UNSAFE.allocateInstance(Js5MasterIndex.class);
        index.fileCounts = new int[] {1};
        Js5ResourceProvider provider = (Js5ResourceProvider) UNSAFE.allocateInstance(Js5ResourceProvider.class);
        provider.masterIndex = index;
        provider.archiveFiles = new Object[][] {new Object[] {payload}};
        provider.archives = new Object[1];
        return new MaterialTypeList(provider, provider, provider);
    }

    private static Map<Integer, byte[]> bundle(Path path) throws Exception {
        Map<Integer, byte[]> models = new HashMap<>();
        try (DataInputStream in = new DataInputStream(Files.newInputStream(path))) {
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                int id = in.readInt();
                byte[] data = new byte[in.readInt()];
                in.readFully(data);
                models.put(id, data);
            }
        }
        return models;
    }

    private static boolean uid(int mode, int id) {
        return mode == 1 || (mode == 2 && id % 2 == 0);
    }

    private static void injectBillboards(Mesh mesh, int mode) {
        BillboardTypeList.BILLBOARD_CACHE.reset();
        if (mesh.billboards == null) return;
        for (Billboard billboard : mesh.billboards) {
            BillboardType type = new BillboardType();
            type.hasUid = uid(mode, billboard.billboardId);
            BillboardTypeList.BILLBOARD_CACHE.put(type, billboard.billboardId);
        }
    }

    private static Map<String, Object> run(String[] f, Map<Integer, byte[]> models, MaterialTypeList table, Map<String, Integer> tally, int[] counters) {
        String name = f[1];
        String[] ids = f[2].split(",");
        boolean strip = f[3].equals("1");
        int ambient = Integer.parseInt(f[4]);
        int contrast = Integer.parseInt(f[5]);
        int uidMode = Integer.parseInt(f[6]);
        boolean withMaterials = f[7].equals("1");
        Map<String, Object> result = object("name", name);
        Mesh mesh;
        try {
            Mesh[] parts = new Mesh[ids.length];
            for (int i = 0; i < ids.length; i++) {
                parts[i] = new Mesh(models.get(Integer.parseInt(ids[i])));
                if (parts[i].version < 13) parts[i].upscale();
            }
            mesh = parts.length == 1 ? parts[0] : new Mesh(parts, parts.length);
        } catch (Throwable error) {
            result.put("outcome", "error");
            result.put("stage", "mesh");
            result.put("error", error.getClass().getSimpleName());
            counters[0]++;
            return result;
        }
        result.put("meshHash", meshHash(mesh));
        if (mesh.billboards != null) tally.merge("billboardMeshes", 1, Integer::sum);
        if (mesh.texturesMapping != null) for (byte m : mesh.texturesMapping) tally.merge("mapping" + m, 1, Integer::sum);
        try {
            injectBillboards(mesh, uidMode);
            ModernModelBuilder.BuiltModel built = ModernModelBuilder.build(mesh, ambient, withMaterials ? table : null, strip, contrast);
            if (built == null) {
                result.put("outcome", "null");
                counters[1]++;
                return result;
            }
            CRC32 crc = new CRC32();
            crc.update(built.getVertexData());
            List<Object> batches = new ArrayList<>();
            for (ModernModelBuilder.BuiltModel.TextureBatch batch : built.getBatches()) {
                batches.add(object("materialId", batch.getMaterialId(), "count", batch.getIndices().length, "hash", hashArray(BASIS, batch.getIndices())));
            }
            result.put("outcome", "ok");
            result.put("id", built.getId());
            result.put("faceCount", built.getFaceCount());
            result.put("vertexCount", built.getVertexCount());
            result.put("vertexFnv", hashBytes(BASIS, built.getVertexData()));
            result.put("vertexCrc", crc.getValue());
            result.put("vertexBytes", built.getVertexData().length);
            result.put("batches", batches);
            result.put("skipBillboardHidden", built.getSkipBillboardHidden());
            result.put("skipRenderType", built.getSkipRenderType());
            result.put("skipInvalidVertex", built.getSkipInvalidVertex());
            result.put("skipTransparent", built.getSkipTransparent());
            result.put("sourceFaceCount", built.getSourceFaceCount());
            result.put("billboardsSeen", built.getBillboardsSeen());
            result.put("billboardsWithUid", built.getBillboardsWithUid());
            result.put("billboardsOutOfRange", built.getBillboardsOutOfRange());
            result.put("skipMaterialFiltered", built.getSkipMaterialFiltered());
            result.put("acceptedFaces", hashArray(BASIS, built.getAcceptedFaces()));
            result.put("acceptedFacesLength", built.getAcceptedFaces().length);
            result.put("acceptedMaterialIds", hashArray(BASIS, built.getAcceptedMaterialIds()));
            counters[2]++;
            return result;
        } catch (Throwable error) {
            result.put("outcome", "error");
            result.put("stage", "build");
            result.put("error", error.getClass().getSimpleName());
            counters[0]++;
            return result;
        }
    }

    public static void main(String[] args) throws Exception {
        Path jar = Path.of(args[0]).toRealPath();
        Path library = Path.of(args[1]).toRealPath();
        String jarBefore = hash(jar), libBefore = hash(library);
        if (!jarBefore.equals(PINNED)) throw new IllegalArgumentException("Unexpected oracle JAR");
        for (Class<?> type : new Class<?>[] {Mesh.class, ModernModelBuilder.class, MaterialTypeList.class, BillboardTypeList.class, ColorUtil.class}) {
            Path loaded = Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
            if (!jar.equals(loaded)) throw new IllegalArgumentException(type.getName() + " code source mismatch");
        }
        System.load(library.toString());
        long seed = 0;
        Class<?> holder = Class.forName(Math.class.getName() + "$RandomNumberGeneratorHolder");
        Field randomField = holder.getDeclaredField("randomNumberGenerator");
        randomField.setAccessible(true);
        ((Random) randomField.get(null)).setSeed(seed);
        double randomUnit = new Random(seed).nextDouble();
        ColorUtil.calculateHslToRgbTable();
        Map<Integer, byte[]> models = bundle(Path.of(args[2]));
        byte[] payload = Files.readAllBytes(Path.of(args[3]));
        MaterialTypeList table = materials(payload);
        Map<String, Integer> tally = new LinkedHashMap<>();
        int[] counters = new int[3];
        List<Object> cases = new ArrayList<>();
        for (String line : Files.readAllLines(Path.of(args[4]))) {
            if (line.isBlank()) continue;
            String[] fields = line.split("\\t");
            if (!fields[0].equals("case")) throw new IllegalArgumentException("unknown input line");
            cases.add(run(fields, models, table, tally, counters));
        }
        if (!jarBefore.equals(hash(jar)) || !libBefore.equals(hash(library))) throw new IllegalStateException("Oracle inputs changed");
        Files.writeString(Path.of(args[5]), json(object("jarSha256", jarBefore, "nativeLibrarySha256", libBefore,
            "jarUnchanged", true, "nativeLibraryUnchanged", true, "randomUnit", randomUnit,
            "materialPayloadSha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload)),
            "materialCount", table.getSize(), "tally", tally, "cases", cases)) + "\n");
        System.out.println("MESH_ORACLE cases=" + cases.size() + " ok=" + counters[2] + " null=" + counters[1] + " error=" + counters[0]);
    }

    private static String hash(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }
}
