import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.midi.MixedAudioBuffer;
import com.jagex.game.runetek5.config.midi.SoundEffect;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

public final class SynthOracle {
    private static final String PINNED = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048";

    private static String hash(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }

    private static String digest(byte[] data) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)).substring(0, 16);
    }

    private static byte[] mutate(byte[] data, int kind, int pos, int value) {
        if (kind == 1) {
            return Arrays.copyOf(data, Math.min(pos, data.length));
        }
        if (kind == 2) {
            byte[] copy = data.clone();
            if (pos < copy.length) {
                copy[pos] = (byte) value;
            }
            return copy;
        }
        return data;
    }

    private static String row(String id, byte[] data) {
        try {
            SoundEffect first = new SoundEffect(new Packet(data));
            MixedAudioBuffer a = first.getMixedAudio();
            SoundEffect second = new SoundEffect(new Packet(data));
            int delay = second.getDelay();
            MixedAudioBuffer b = second.getMixedAudio();
            return "S\t" + id + "\t" + digest(a.samples) + "\t" + a.samples.length + "\t" + first.loopBegin + "\t"
                + first.loopEnd + "\t" + a.start + "\t" + a.end + "\t" + delay + "\t" + second.loopBegin
                + "\t" + second.loopEnd + "\t" + digest(b.samples) + "\t" + b.samples.length;
        } catch (Throwable error) {
            return "S\t" + id + "\terror";
        }
    }

    public static void main(String[] args) throws Exception {
        Path jar = Path.of(args[0]);
        if (!hash(jar).equals(PINNED)) {
            throw new IllegalStateException("jar does not match the pinned oracle sha256");
        }
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(Files.readAllBytes(Path.of(args[1]))));
        Map<Integer, byte[]> blobs = new HashMap<>();
        int count = in.readInt();
        for (int i = 0; i < count; i++) {
            int id = in.readInt();
            byte[] data = new byte[in.readInt()];
            in.readFully(data);
            blobs.put(id, data);
        }
        List<String> cases = Files.readAllLines(Path.of(args[2]), StandardCharsets.US_ASCII);
        int errors = 0;
        int total = 0;
        try (BufferedWriter out = Files.newBufferedWriter(Path.of(args[3]), StandardCharsets.US_ASCII)) {
            for (String line : cases) {
                if (line.isEmpty()) {
                    continue;
                }
                String[] f = line.split("\t");
                int base = Integer.parseInt(f[2]);
                byte[] data = mutate(blobs.get(base), Integer.parseInt(f[3]), Integer.parseInt(f[4]), Integer.parseInt(f[5]));
                String result = row(f[1], data);
                if (result.endsWith("\terror")) {
                    errors++;
                }
                total++;
                out.write(result);
                out.write("\n");
            }
        }
        System.out.println("rows=" + total + " errors=" + errors);
    }
}
