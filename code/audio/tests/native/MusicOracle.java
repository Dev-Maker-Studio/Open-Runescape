import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.cache.node.impl.ObjectNode;
import com.jagex.game.runetek5.sound.jagtheora.MidiFileReader;
import com.jagex.game.runetek5.sound.jagtheora.MusicPatch;
import com.jagex.game.runetek5.sound.jagtheora.MusicPatchInfo;
import com.jagex.game.runetek5.sound.jagtheora.MusicTrack;
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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class MusicOracle {
    private static final String PINNED = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048";
    private static final int PATCH_BASE = 1000000;
    private static final int EVENT_CAP = 300000;

    private static String hash(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }

    private static String digest(byte[] data) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)).substring(0, 16);
    }

    private static String digest(String text) throws Exception {
        return digest(text.getBytes(StandardCharsets.US_ASCII));
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

    private static String ints(int[] values) {
        StringBuilder out = new StringBuilder();
        for (int value : values) {
            out.append(value).append(',');
        }
        return out.toString();
    }

    private static String bytes(byte[] values) {
        if (values == null) {
            return "-";
        }
        StringBuilder out = new StringBuilder();
        for (byte value : values) {
            out.append(value).append(',');
        }
        return out.toString();
    }

    private static String shorts(short[] values) {
        StringBuilder out = new StringBuilder();
        for (short value : values) {
            out.append(value).append(',');
        }
        return out.toString();
    }

    private static String trackRow(String id, byte[] data) throws Exception {
        MusicTrack track = new MusicTrack(new Packet(data));
        track.buildPatchTable();
        StringBuilder table = new StringBuilder();
        IterableNodeMap<ObjectNode> map = track.table;
        for (ObjectNode node = map.first(); node != null; node = map.next()) {
            table.append(node.pointer).append(':').append(bytes((byte[]) node.value)).append(';');
        }
        MidiFileReader reader = new MidiFileReader(track.midi);
        StringBuilder events = new StringBuilder();
        int count = reader.getTrackCount();
        for (int t = 0; t < count; t++) {
            reader.getGotoTrack(t);
            reader.getTrackLength(t);
            reader.markTracePosition(t);
        }
        int emitted = 0;
        outer:
        do {
            while (true) {
                int t = reader.getPrioritizedTrack();
                int length = reader.trackLengths[t];
                while (length == reader.trackLengths[t]) {
                    reader.getGotoTrack(t);
                    int message = reader.readMessage(t);
                    events.append(t).append(',').append(message).append(';');
                    if (++emitted > EVENT_CAP) {
                        break outer;
                    }
                    if (message == 1) {
                        reader.setTrackDone();
                        reader.markTracePosition(t);
                        continue outer;
                    }
                    reader.getTrackLength(t);
                    reader.markTracePosition(t);
                }
            }
        } while (!reader.isDone());
        events.append(reader.microsecondsPerBeat).append(',').append(reader.tempoTimeOffset).append(',')
            .append(reader.getTickTimestamp(1000)).append(';');
        reader.resetTracksFromTimestamp(12345L);
        events.append(ints(reader.trackLengths)).append(ints(reader.startPositionTrackBuffer))
            .append(reader.tempoTimeOffset);
        return "T\t" + id + "\t" + digest(track.midi) + "\t" + track.midi.length + "\t" + reader.division + "\t"
            + count + "\t" + digest(ints(reader.trackStarts)) + "\t" + digest(table.toString()) + "\t" + emitted
            + "\t" + digest(events.toString());
    }

    private static String patchRow(String id, byte[] data) throws Exception {
        MusicPatch patch = new MusicPatch(data);
        IdentityHashMap<MusicPatchInfo, Integer> order = new IdentityHashMap<>();
        StringBuilder keys = new StringBuilder();
        StringBuilder infos = new StringBuilder();
        for (int key = 0; key < 128; key++) {
            MusicPatchInfo info = patch.musicPatchNodes2[key];
            if (info == null) {
                keys.append("-1,");
                continue;
            }
            Integer index = order.get(info);
            if (index == null) {
                index = order.size();
                order.put(info, index);
                infos.append(bytes(info.releaseEnvelopeData)).append('/').append(bytes(info.volumeEnvelopeData))
                    .append('/').append(info.volumeEnvelopeKeyScaling).append('/').append(info.volumeEnvelopeDecay)
                    .append('/').append(info.releaseEnvelopeKeyScaling).append('/').append(info.decayKeyScaling)
                    .append('/').append(info.vibratoLfoPitch).append('/').append(info.volumeEnvelopeRelease)
                    .append('/').append(info.vibratoDelay).append(';');
            }
            keys.append(index).append(',');
        }
        String all = ints(patch.sampleOffsets) + "|" + shorts(patch.pitchOffset) + "|" + bytes(patch.volumeOffset)
            + "|" + bytes(patch.panOffset) + "|" + bytes(patch.loopMode) + "|" + patch.baseVelocity + "|" + keys
            + "|" + infos;
        return "P\t" + id + "\t" + digest(all) + "\t" + all.length() + "\t" + patch.baseVelocity + "\t"
            + order.size();
    }

    private static String row(String id, int base, byte[] data) {
        try {
            return base >= PATCH_BASE ? patchRow(id, data) : trackRow(id, data);
        } catch (Throwable error) {
            return (base >= PATCH_BASE ? "P" : "T") + "\t" + id + "\terror";
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
                String result = row(f[1], base, data);
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
