import com.jagex.game.runetek5.scene.ground.GroundGL;
import com.jagex.game.runetek5.toolkit.OpenGlToolkit;
import jaclib.memory.Stream;
import jaclib.memory.heap.NativeHeap;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import sun.misc.Unsafe;

public final class GroundLoweringProbe {
    private static final String PINNED = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048";

    private static String quote(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }

    private static String failure(Throwable error) {
        return "{\"type\":" + quote(error.getClass().getSimpleName()) + ",\"message\":" + quote(String.valueOf(error.getMessage())) + "}";
    }

    public static void main(String[] args) throws Exception {
        Path jar = Path.of(args[0]).toRealPath();
        Path output = Path.of(args[1]);
        String before = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar)));
        if (!before.equals(PINNED)) throw new IllegalArgumentException("Unexpected oracle JAR SHA-256");
        Path loaded = Path.of(GroundGL.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
        if (!jar.equals(loaded)) throw new IllegalArgumentException("GroundGL not loaded from private pinned JAR");
        String load = "null";
        try {
            if (args.length == 3) System.load(Path.of(args[2]).toRealPath().toString());
            else System.loadLibrary("jaclib");
        } catch (LinkageError error) {
            load = failure(error);
        }
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Unsafe unsafe = (Unsafe) field.get(null);
        OpenGlToolkit toolkit = (OpenGlToolkit) unsafe.allocateInstance(OpenGlToolkit.class);
        int[][] heights = new int[3][3];
        GroundGL ground = new GroundGL(toolkit, 0, 7, 2, 2, 512, heights, heights);
        if (ground.lightingY[1][1] != -1.0f || ground.lightingY[0][0] != 0.0f) {
            throw new IllegalStateException("Unexpected unchanged GroundGL constructor normals");
        }
        String heap = "null";
        try {
            NativeHeap nativeHeap = new NativeHeap(1048576);
            nativeHeap.createBuffer(64, true);
            nativeHeap.deallocate();
        } catch (LinkageError error) {
            heap = failure(error);
        }
        String stream = "null";
        try {
            Stream.bigEndianessEnabled();
            if (Stream.floatToRawIntBits(1.0f) != Float.floatToRawIntBits(1.0f)) {
                throw new IllegalStateException("Unexpected native float serialization");
            }
        } catch (LinkageError error) {
            stream = failure(error);
        }
        String after = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar)));
        if (!before.equals(after)) throw new IllegalStateException("Oracle JAR changed");
        boolean feasible = heap.equals("null") && stream.equals("null");
        String result = "{\"jarSha256\":" + quote(before)
            + ",\"jarUnchanged\":true,\"actualGroundConstructor\":true,\"nativeLibraryLoadError\":" + load
            + ",\"heapError\":" + heap + ",\"streamError\":" + stream
            + ",\"compilerOracleFeasible\":" + feasible + ",\"compiledMeshParityEstablished\":false}\n";
        Files.writeString(output, result);
        System.out.print(result);
        if (!feasible) System.exit(2);
    }
}
