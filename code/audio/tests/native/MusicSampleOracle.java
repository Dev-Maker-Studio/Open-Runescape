import com.jagex.game.runetek5.config.midi.instrument.MusicSample;
import com.jagex.game.runetek5.config.midi.instrument.MusicSampleCodebook;
import com.jagex.game.runetek5.config.midi.instrument.MusicSampleFloor;
import com.jagex.game.runetek5.config.midi.instrument.MusicSamepleMapping;
import com.jagex.game.runetek5.config.midi.instrument.MusicSampleResidue;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

public final class MusicSampleOracle {
    private static final String PINNED = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048";
    private static final int ITERATION_CAP = 100000;
    private static final long WATCHDOG_MS = 30000;
    private static volatile String currentCase = "";
    private static volatile long caseStart = 0;
    private static Path hangFile;

    private static String hash(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }

    private static String digest(byte[] data) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)).substring(0, 16);
    }

    private static String digest(String text) throws Exception {
        return digest(text.getBytes(StandardCharsets.US_ASCII));
    }

    private static int bits(float value) {
        return Float.floatToIntBits(value);
    }

    private static String floatDigest(float[] values) throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(values.length * 4);
        for (float value : values) {
            buffer.putInt(bits(value));
        }
        return digest(buffer.array());
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

    private static void ints(StringBuilder out, int[] values) {
        out.append('[');
        for (int value : values) {
            out.append(value).append(',');
        }
        out.append(']');
    }

    private static void floats(StringBuilder out, float[] values) {
        out.append('[');
        for (float value : values) {
            out.append(bits(value)).append(',');
        }
        out.append(']');
    }

    private static String setupSummary() throws Exception {
        StringBuilder out = new StringBuilder();
        out.append(MusicSample.getBlockSize0()).append(',').append(MusicSample.getBlockSize1()).append('|');
        for (MusicSampleCodebook book : MusicSample.getCodebooks()) {
            out.append("cb:").append(book.dimensions).append(',').append(book.entries);
            ints(out, book.lengths);
            if (book.multiplicands == null) {
                out.append('-');
            } else {
                for (float[] row : book.multiplicands) {
                    floats(out, row);
                }
            }
            ints(out, book.huffmanTree);
            out.append(';');
        }
        for (MusicSampleFloor floor : MusicSample.getFloors()) {
            out.append("fl:");
            ints(out, floor.getPartitionClassList());
            ints(out, floor.getClassDimensions());
            ints(out, floor.getClassSubclasses());
            ints(out, floor.getClassMasterbooks());
            for (int[] books : floor.getSubclassBooks()) {
                ints(out, books);
            }
            out.append(floor.getFloor1Multiplier());
            ints(out, floor.getXList());
            out.append(';');
        }
        for (MusicSampleResidue residue : MusicSample.getResidues()) {
            out.append("rs:").append(residue.residueType).append(',').append(residue.residueBegin).append(',')
                .append(residue.residueEnd).append(',').append(residue.partitionSize).append(',')
                .append(residue.classifications).append(',').append(residue.classbook);
            ints(out, residue.residueBooks);
            out.append(';');
        }
        for (MusicSamepleMapping mapping : MusicSample.getMappings()) {
            out.append("mp:").append(mapping.submaps).append(',').append(mapping.mux);
            ints(out, mapping.submapFloors);
            ints(out, mapping.submapResidues);
            out.append(';');
        }
        out.append("md:");
        for (boolean flag : MusicSample.getNoResidues()) {
            out.append(flag ? '1' : '0');
        }
        ints(out, MusicSample.getModes());
        floats(out, MusicSample.getCdR());
        floats(out, MusicSample.getCdP());
        floats(out, MusicSample.getCdV());
        ints(out, MusicSample.getCdAv());
        floats(out, MusicSample.getCdAg());
        floats(out, MusicSample.getCdAq());
        floats(out, MusicSample.getCdAj());
        ints(out, MusicSample.getCdAr());
        return digest(out.toString());
    }

    private static String decode(String id, byte[] data) throws Exception {
        MusicSample sample = new MusicSample(data);
        int guard = 0;
        while (sample.hasMoreSamples() && sample.soundIndices < sample.packets.length && guard++ < ITERATION_CAP) {
            sample.calculateAudioBufferLength(sample.sampleLength + sample.sampleRate / 8);
        }
        String header = sample.sampleRate + "," + sample.sampleCount + "," + sample.start + "," + sample.end + ","
            + (sample.loopConsistency ? 1 : 0) + "," + sample.packets.length;
        String state = sample.sampleLength + "," + sample.soundIndices + "," + (sample.hasMoreSamples() ? 1 : 0) + ","
            + sample.audioBufferLength;
        String samples = sample.samples == null ? "-" : floatDigest(sample.samples);
        return header + "\t" + state + "\t" + samples + "\t" + floatDigest(MusicSample.getWindow());
    }

    private static String sampleRow(String id, byte[] data) {
        try {
            return "S\t" + id + "\t" + decode(id, data);
        } catch (Throwable error) {
            return "S\t" + id + "\terror";
        }
    }

    private static String setupRow(String id, byte[] data, List<byte[]> probes) {
        try {
            MusicSample.initializeData(data);
            String summary = setupSummary();
            StringBuilder sub = new StringBuilder();
            for (byte[] probe : probes) {
                sub.append(sampleRow("p", probe)).append('\n');
            }
            return "U\t" + id + "\t" + summary + "\t" + digest(sub.toString());
        } catch (Throwable error) {
            return "U\t" + id + "\terror";
        }
    }

    private static String initRow(String id, byte[] data) {
        try {
            MusicSample.initializeData(data);
            return "I\t" + id + "\t" + setupSummary();
        } catch (Throwable error) {
            return "I\t" + id + "\terror";
        }
    }

    public static void main(String[] args) throws Exception {
        Path jar = Path.of(args[0]);
        if (!hash(jar).equals(PINNED)) {
            throw new IllegalStateException("jar does not match the pinned oracle sha256");
        }
        hangFile = Path.of(args[3] + ".hang");
        Files.deleteIfExists(hangFile);
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(Files.readAllBytes(Path.of(args[1]))));
        Map<Integer, byte[]> blobs = new HashMap<>();
        int count = in.readInt();
        for (int i = 0; i < count; i++) {
            int id = in.readInt();
            byte[] data = new byte[in.readInt()];
            in.readFully(data);
            blobs.put(id, data);
        }
        List<Integer> order = new ArrayList<>();
        for (int id : blobs.keySet()) {
            if (id >= 1 && blobs.get(id).length >= 2000) {
                order.add(id);
            }
        }
        order.sort((a, b) -> blobs.get(a).length != blobs.get(b).length
            ? Integer.compare(blobs.get(a).length, blobs.get(b).length) : Integer.compare(a, b));
        List<byte[]> probes = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            probes.add(blobs.get(order.get(i)));
        }
        Thread watchdog = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(500);
                    if (caseStart != 0 && System.currentTimeMillis() - caseStart > WATCHDOG_MS) {
                        Files.writeString(hangFile, currentCase);
                        Runtime.getRuntime().halt(3);
                    }
                } catch (Exception error) {
                    Runtime.getRuntime().halt(4);
                }
            }
        });
        watchdog.setDaemon(true);
        watchdog.start();
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
                int kind = Integer.parseInt(f[3]);
                currentCase = f[1];
                caseStart = System.currentTimeMillis();
                byte[] data = mutate(blobs.get(base), kind, Integer.parseInt(f[4]), Integer.parseInt(f[5]));
                String result;
                if (base == 0 && kind == 0) {
                    result = initRow(f[1], data);
                } else if (base == 0) {
                    result = setupRow(f[1], data, probes);
                } else {
                    result = sampleRow(f[1], data);
                }
                caseStart = 0;
                if (result.endsWith("\terror")) {
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
