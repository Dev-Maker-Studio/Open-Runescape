import com.jagex.game.runetek5.scene.ground.GroundGL;
import com.jagex.game.runetek5.scene.tile.WaterTile;
import com.jagex.game.runetek5.toolkit.OpenGlToolkit;
import com.jagex.game.runetek5.toolkit.opengl.GroundMaterialGL;
import com.jagex.game.runetek5.utils.ColorUtil;
import jaclib.memory.heap.NativeHeap;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import sun.misc.Unsafe;

public final class GroundLoweringOracle {
    private static final String PINNED = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048";
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

    private static int[] ints(String[] fields, int start) {
        int[] result = new int[fields.length - start];
        for (int i = start; i < fields.length; i++) result[i - start] = Integer.parseInt(fields[i]);
        return result;
    }

    private static int[][] grid(int[] values, int width, int height) {
        if (values.length != (width + 1) * (height + 1)) throw new IllegalArgumentException("grid length");
        int[][] result = new int[width + 1][height + 1];
        for (int z = 0; z <= height; z++) for (int x = 0; x <= width; x++) result[x][z] = values[z * (width + 1) + x];
        return result;
    }

    private static byte[] bytes(long address, int count) {
        if (count > 0 && address == 0) throw new IllegalArgumentException("null native address");
        byte[] result = new byte[count];
        for (int i = 0; i < count; i++) result[i] = UNSAFE.getByte(address + i);
        return result;
    }

    private static final class TileInput {
        final int x;
        final int z;
        final int emitted;
        final boolean shadows;
        final boolean hasBlend;
        final List<int[]> vertices = new ArrayList<>();
        TileInput(int[] fields) {
            x = fields[0]; z = fields[1]; emitted = fields[2]; shadows = fields[3] != 0; hasBlend = fields.length < 5 || fields[4] != 0;
        }
    }

    private static final class CaseInput {
        final String name;
        final int width;
        final int height;
        final int scale;
        int[] position;
        int[] normals;
        int[] shadows;
        final List<TileInput> tiles = new ArrayList<>();
        CaseInput(String[] fields) {
            name = fields[1]; width = Integer.parseInt(fields[2]); height = Integer.parseInt(fields[3]); scale = Integer.parseInt(fields[4]);
        }

        Map<String, Object> compile() throws Exception {
            OpenGlToolkit toolkit = (OpenGlToolkit) UNSAFE.allocateInstance(OpenGlToolkit.class);
            toolkit.nativeHeap = new NativeHeap(16777216);
            toolkit.vertexbufferSupported = false;
            toolkit.bigEndianess = false;
            GroundGL ground = new GroundGL(toolkit, 0, 7, width, height, 512, grid(position, width, height), grid(normals, width, height));
            for (int z = 0; z <= height; z++) for (int x = 0; x <= width; x++) ground.setTileShadow(x, z, shadows[z * (width + 1) + x]);
            int allocated = 0;
            for (TileInput tile : tiles) {
                int n = tile.vertices.size();
                int[] ox = new int[n], oz = new int[n], main = new int[n], blend = new int[n], texture = new int[n], scales = new int[n];
                for (int i = 0; i < n; i++) {
                    int[] v = tile.vertices.get(i);
                    ox[i] = v[0]; oz[i] = v[1]; main[i] = v[2]; blend[i] = v[3]; texture[i] = v[4]; scales[i] = v[5];
                }
                allocated += n;
                ground.addBlendedTile(tile.x, tile.z, ox, null, oz, null, main, tile.hasBlend ? blend : null, texture, scales, new WaterTile(), tile.shadows);
            }
            ground.resetLight();
            int count = ground.totalMaterializedTiles;
            String vertexBytes = count == 0 ? "" : HexFormat.of().formatHex(bytes(ground.dataArrayBuffer.getBufferAddress(), count * 36));
            String materialBytes = "";
            int[] masks = new int[width * height];
            int materialCount = ground.goundMaterialNodes == null ? 0 : ground.goundMaterialNodes.length;
            if (materialCount > 1) throw new IllegalStateException("not single material");
            if (materialCount == 1) {
                GroundMaterialGL material = (GroundMaterialGL) ground.goundMaterialNodes[0];
                if (material.materialId != -1 || material.textureScale != (float) scale) throw new IllegalStateException("material mismatch");
                materialBytes = HexFormat.of().formatHex(bytes(material.arrayBufferPointer.buffer.getBufferAddress(), count * 4));
                masks = material.groundFunctionMask;
            }
            List<Object> indices = new ArrayList<>();
            for (short[] tile : ground.materialIndices) {
                if (tile == null) indices.add(null);
                else {
                    int[] unsigned = new int[tile.length];
                    for (int i = 0; i < tile.length; i++) unsigned[i] = tile[i] & 65535;
                    indices.add(unsigned);
                }
            }
            Map<String, Object> result = object("name", name, "allocatedVertices", allocated, "vertexCount", count, "stride", 36,
                "vertexBytes", vertexBytes, "materialBytes", materialBytes, "materialCount", materialCount, "indices", indices, "masks", masks);
            toolkit.nativeHeap.deallocate();
            return result;
        }
    }

    public static void main(String[] args) throws Exception {
        Path jar = Path.of(args[0]).toRealPath();
        Path library = Path.of(args[1]).toRealPath();
        String jarBefore = hash(jar), libBefore = hash(library);
        if (!jarBefore.equals(PINNED)) throw new IllegalArgumentException("Unexpected oracle JAR");
        Path loaded = Path.of(GroundGL.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
        if (!jar.equals(loaded)) throw new IllegalArgumentException("GroundGL code source mismatch");
        System.load(library.toString());
        long seed = 0;
        Class<?> holder = Class.forName(Math.class.getName() + "$RandomNumberGeneratorHolder");
        Field randomField = holder.getDeclaredField("randomNumberGenerator");
        randomField.setAccessible(true);
        ((Random) randomField.get(null)).setSeed(seed);
        double randomUnit = new Random(seed).nextDouble();
        ColorUtil.calculateHslToRgbTable();
        List<Object> cases = new ArrayList<>();
        CaseInput current = null;
        TileInput tile = null;
        for (String line : Files.readAllLines(Path.of(args[2]))) {
            if (line.isBlank()) continue;
            String[] fields = line.split("\\t");
            switch (fields[0]) {
                case "case" -> { current = new CaseInput(fields); tile = null; }
                case "position" -> current.position = ints(fields, 1);
                case "normal" -> current.normals = ints(fields, 1);
                case "shadow" -> current.shadows = ints(fields, 1);
                case "tile" -> { tile = new TileInput(ints(fields, 1)); current.tiles.add(tile); }
                case "vertex" -> tile.vertices.add(ints(fields, 1));
                case "end" -> { cases.add(current.compile()); current = null; tile = null; }
                default -> throw new IllegalArgumentException("unknown input line");
            }
        }
        if (current != null) throw new IllegalArgumentException("unterminated case");
        if (!jarBefore.equals(hash(jar)) || !libBefore.equals(hash(library))) throw new IllegalStateException("Oracle inputs changed");
        Files.writeString(Path.of(args[3]), json(object("jarSha256", jarBefore, "nativeLibrarySha256", libBefore,
            "jarUnchanged", true, "nativeLibraryUnchanged", true, "actualGroundConstructor", true, "actualResetLight", true,
            "glDrawExecuted", false, "randomUnit", randomUnit, "cases", cases)) + "\n");
        System.out.println("UNCHANGED_GROUNDGL_COMPILED " + cases.size());
    }

    private static String hash(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }
}
