import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.cache.node.NodeCollection;
import com.jagex.game.runetek5.cache.node.impl.ObjectNode;
import com.jagex.game.runetek5.config.midi.MixedAudioBuffer;
import com.jagex.game.runetek5.sound.jagtheora.AbstractAudioFile;
import com.jagex.game.runetek5.sound.jagtheora.AudioBuffer;
import com.jagex.game.runetek5.sound.jagtheora.MidiPcmStream;
import com.jagex.game.runetek5.sound.jagtheora.MusicPatch;
import com.jagex.game.runetek5.sound.jagtheora.MusicTrack;
import com.jagex.game.runetek5.sound.player.PcmPlayerManager;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

public final class MidiPcmOracle {
    private static final String PINNED = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048";
    private static final int PATCH_BASE = 1000000;

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

    private static final class Lcg {
        long state;

        Lcg(long seed) {
            state = seed;
        }

        int next(int bound) {
            state = state * 6364136223846793005L + 1442695040888963407L;
            return (int) ((state >>> 33) % bound);
        }
    }

    private static AbstractAudioFile synthetic(int id, int kind) {
        Lcg rng = new Lcg(id * 2L + kind + 1);
        int length = 200 + rng.next(1800);
        int start = rng.next(length / 2);
        int end = start + 1 + rng.next(length - start - 4);
        boolean loop = rng.next(2) == 1;
        if (kind == 0) {
            byte[] samples = new byte[length];
            for (int i = 0; i < length; i++) {
                samples[i] = (byte) (rng.next(256) - 128);
            }
            MixedAudioBuffer buffer = new MixedAudioBuffer(samples, start, end);
            buffer.enableLoop = loop;
            return buffer;
        }
        int[] rates = {11025, 22050, 32000, 44100};
        int rate = rates[rng.next(4)];
        float[] samples = new float[length];
        for (int i = 0; i < length; i++) {
            samples[i] = (rng.next(2001) - 1000) / 1000.0f;
        }
        return new AudioBuffer(rate, samples, start, end, loop);
    }

    private static String digestInts(int[] values) throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(values.length * 4);
        for (int value : values) {
            buffer.putInt(value);
        }
        return digest(buffer.array());
    }

    private static int count(NodeCollection collection) {
        int n = 0;
        for (Node<?> node = collection.getFirst(); node != null; node = collection.next()) {
            n++;
        }
        return n;
    }

    private static String state(MidiPcmStream s) {
        StringBuilder out = new StringBuilder();
        out.append(s.currentTickTime).append(',').append(s.nextEventTickTime).append(',').append(s.prioritizedTrack)
            .append(',').append(s.prioritizedTrackLength).append(',').append(s.dataLoaded() ? 1 : 0).append(',')
            .append(count(s.patchStream.queue)).append(',').append(count(s.patchStream.fadeOutScheduler.getSubStreams()));
        for (int c = 0; c < 16; c++) {
            out.append(',').append(s.programChange[c]).append(',').append(s.pitchBend[c]).append(',')
                .append(s.sustain[c]).append(',').append(s.effectSpeedScaled[c]);
        }
        return out.toString();
    }

    private static int[] parse(String text) {
        if (text.isEmpty()) {
            return new int[0];
        }
        String[] parts = text.split(",");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            out[i] = Integer.parseInt(parts[i]);
        }
        return out;
    }

    private static String render(String[] f, Map<Integer, byte[]> blobs) throws Exception {
        String id = f[1];
        int base = Integer.parseInt(f[2]);
        byte[] data = mutate(blobs.get(base), Integer.parseInt(f[3]), Integer.parseInt(f[4]), Integer.parseInt(f[5]));
        PcmPlayerManager.sampleRate = Integer.parseInt(f[6]);
        PcmPlayerManager.stereo = f[7].equals("1");
        boolean looping = f[8].equals("1");
        int channels = PcmPlayerManager.stereo ? 2 : 1;
        StringBuilder all = new StringBuilder();
        int opIndex = -1;
        try {
            MusicTrack track = new MusicTrack(new Packet(data));
            MidiPcmStream stream = new MidiPcmStream();
            stream.load();
            track.buildPatchTable();
            IterableNodeMap<ObjectNode> table = track.table;
            for (ObjectNode node = table.first(); node != null; node = table.next()) {
                int key = (int) node.pointer;
                MusicPatch patch = stream.musicPatches.get((long) key);
                if (patch == null) {
                    byte[] patchData = blobs.get(PATCH_BASE + key);
                    if (patchData == null) {
                        continue;
                    }
                    patch = new MusicPatch(patchData);
                    stream.musicPatches.put(patch, (long) key);
                }
                byte[] flags = (byte[]) node.value;
                int file = 0;
                AbstractAudioFile audioFile = null;
                for (int index = 0; index < 128; index++) {
                    if (flags == null || flags[index] != 0) {
                        int offset = patch.sampleOffsets[index];
                        if (offset != 0) {
                            if (offset != file) {
                                int variable = offset - 1;
                                file = offset;
                                audioFile = synthetic(variable >> 2, variable & 1);
                            }
                            patch.audioFiles[index] = audioFile;
                            patch.sampleOffsets[index] = 0;
                        }
                    }
                }
            }
            track.clear();
            stream.setMusicTrack(track, looping);
            all.append(state(stream)).append('|');
            String[] ops = f[9].isEmpty() ? new String[0] : f[9].split(";");
            for (int index = 0; index < ops.length; index++) {
                opIndex = index;
                char code = ops[index].charAt(0);
                int[] a = parse(ops[index].substring(1));
                switch (code) {
                    case 'F': {
                        int[] mix = new int[a[0] * channels];
                        stream.fill(mix, 0, a[0]);
                        all.append(digestInts(mix)).append(',');
                        break;
                    }
                    case 'K':
                        stream.skip(a[0]);
                        break;
                    case 'V':
                        stream.setMusicVolume(a[0]);
                        break;
                    case 'W':
                        stream.setVolumeMultiplier(a[0]);
                        break;
                    case 'C':
                        stream.setChannelVolume(a[0], a[1]);
                        break;
                    case 'B':
                        stream.setPitchBend(a[0], a[1]);
                        break;
                    case 'R':
                        stream.resetMusicPatches();
                        break;
                    case 'T':
                        stream.setMusicTrackAtPosition(track, looping, a[0]);
                        break;
                    case 'S':
                        stream.setMusicTrack(track, looping);
                        break;
                    case 'O':
                        stream.turnOffSound();
                        break;
                    default:
                        throw new IllegalStateException("op");
                }
                all.append(state(stream)).append('|');
            }
        } catch (Throwable error) {
            return "M\t" + id + "\terror\t" + opIndex;
        }
        return "M\t" + id + "\t" + digest(all.toString().getBytes(StandardCharsets.US_ASCII)) + "\t" + all.length();
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
                String[] fields = line.split("\t", -1);
                String[] holder = new String[1];
                Thread worker = new Thread(null, () -> {
                    try {
                        holder[0] = render(fields, blobs);
                    } catch (Throwable error) {
                        holder[0] = "M\t" + fields[1] + "\terror\t-2";
                    }
                }, "render", 64L << 20);
                worker.setDaemon(true);
                worker.start();
                worker.join(30000);
                String result = null;
                if (holder[0] == null) {
                    out.flush();
                    System.out.println("timeout " + fields[1]);
                    System.out.flush();
                    Runtime.getRuntime().halt(3);
                    return;
                } else {
                    result = holder[0];
                }
                if (result.contains("\terror")) {
                    errors++;
                }
                total++;
                out.write(result);
                out.write("\n");
                out.flush();
            }
        }
        System.out.println("rows=" + total + " errors=" + errors);
    }
}
