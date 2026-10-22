import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.identitykit.IdkType;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class IdkOracle {
    private static final String PINNED = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048";

    private static String hash(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }

    private static String ints(int[] values) {
        if (values == null) return "null";
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) out.append(',');
            out.append(values[i]);
        }
        return out.append(']').toString();
    }

    private static String shorts(short[] values) {
        if (values == null) return "null";
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) out.append(',');
            out.append(values[i]);
        }
        return out.append(']').toString();
    }

    public static void main(String[] args) throws Exception {
        Path jar = Path.of(args[0]).toRealPath();
        Path bundle = Path.of(args[1]);
        Path output = Path.of(args[2]);
        if (!hash(jar).equals(PINNED)) throw new IllegalArgumentException("Unexpected oracle JAR");
        Path loaded = Path.of(IdkType.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
        if (!jar.equals(loaded)) throw new IllegalArgumentException("code source mismatch");
        Method decode = null;
        for (Method method : IdkType.class.getDeclaredMethods()) {
            if (method.getName().startsWith("decode") && method.getParameterCount() == 1) decode = method;
        }
        decode.setAccessible(true);
        int errors = 0;
        int count;
        try (DataInputStream in = new DataInputStream(new java.io.BufferedInputStream(Files.newInputStream(bundle), 1 << 20));
             PrintStream out = new PrintStream(new BufferedOutputStream(Files.newOutputStream(output), 1 << 20), false, StandardCharsets.US_ASCII)) {
            count = in.readInt();
            for (int i = 0; i < count; i++) {
                int id = in.readInt();
                byte[] data = new byte[in.readInt()];
                in.readFully(data);
                String line;
                try {
                    IdkType type = new IdkType();
                    decode.invoke(type, new Packet(data));
                    line = "body=" + ints(type.bodyModels) + "\trecolorSrc=" + shorts(type.recolorSrc) + "\trecolorDst=" + shorts(type.recolorDst)
                        + "\tretexSrc=" + shorts(type.retexSrc) + "\tretexDst=" + shorts(type.retexDst) + "\thead=" + ints(type.headModels);
                } catch (Throwable error) {
                    errors++;
                    line = "error";
                }
                out.println(id + "\t" + line);
            }
        }
        if (!hash(jar).equals(PINNED)) throw new IllegalStateException("Oracle JAR changed");
        System.out.println("IDK_ORACLE ok records=" + count + " errors=" + errors);
    }
}
