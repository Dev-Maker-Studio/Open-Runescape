import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.sequence.frameset.AnimBase;
import com.jagex.game.runetek5.config.sequence.frameset.AnimFrame;
import com.jagex.game.runetek5.config.sequence.frameset.FrameSet;
import com.jagex.game.runetek5.graphics.model.Mesh;
import com.jagex.game.runetek5.graphics.model.ModelGL;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class PoseOracle {
    private static final String PINNED = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048";

    private static final Map<Integer, byte[]> models = new HashMap<>();
    private static final Map<Integer, byte[]> baseData = new HashMap<>();
    private static final Map<Integer, Map<Integer, byte[]>> setData = new HashMap<>();
    private static final Map<Integer, AnimBase> bases = new HashMap<>();
    private static final Map<Integer, FrameSet> sets = new HashMap<>();
    private static sun.misc.Unsafe unsafe;

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

    private static int[] orEmpty(int[] values) {
        return values == null ? new int[0] : values;
    }

    private static AnimBase base(int id) {
        AnimBase b = bases.get(id);
        if (b == null) {
            byte[] data = baseData.get(id);
            if (data == null) throw new IllegalStateException();
            b = new AnimBase(id, data);
            bases.put(id, b);
        }
        return b;
    }

    private static FrameSet set(int group) throws Exception {
        FrameSet existing = sets.get(group);
        if (existing != null) return existing;
        Map<Integer, byte[]> files = setData.get(group);
        int max = -1;
        for (int id : files.keySet()) max = Math.max(max, id);
        AnimFrame[] frames = new AnimFrame[max + 1];
        for (Map.Entry<Integer, byte[]> file : new TreeMap<>(files).entrySet()) {
            Packet buffer = new Packet(file.getValue());
            buffer.currentPosition = 1;
            AnimBase b = base(buffer.readUShort());
            frames[file.getKey()] = new AnimFrame(file.getValue(), b);
        }
        FrameSet made = new FrameSet(group);
        Field field = FrameSet.class.getDeclaredField("frames");
        field.setAccessible(true);
        field.set(made, frames);
        sets.put(group, made);
        return made;
    }

    private static Mesh mesh(String spec) {
        String[] ids = spec.split(",");
        if (ids.length == 1) return new Mesh(models.get(Integer.parseInt(ids[0])));
        Mesh[] parts = new Mesh[ids.length];
        for (int i = 0; i < ids.length; i++) parts[i] = new Mesh(models.get(Integer.parseInt(ids[i])));
        return new Mesh(parts, parts.length);
    }

    private static int[][] groups(Object value) {
        return (int[][]) value;
    }

    private static ModelGL model(Mesh mesh) throws Exception {
        ModelGL m = (ModelGL) unsafe.allocateInstance(ModelGL.class);
        m.numVertices = mesh.numVertices;
        m.verticesX = mesh.verticesX == null ? null : mesh.verticesX.clone();
        m.verticesY = mesh.verticesY == null ? null : mesh.verticesY.clone();
        m.verticesZ = mesh.verticesZ == null ? null : mesh.verticesZ.clone();
        if (mesh.vertexGroups != null) m.vertexGroups = groups(mesh.createVertexGroups(false));
        if (mesh.faceGroups != null) m.faceGroups = groups(mesh.createFaceGroups());
        m.facesAlpha = mesh.facesAlpha == null ? new byte[mesh.numFaces] : mesh.facesAlpha.clone();
        m.facesColor = mesh.facesColor == null ? new short[mesh.numFaces] : mesh.facesColor.clone();
        return m;
    }

    private static String pose(ModelGL m) throws Exception {
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(raw);
        out.writeInt(m.numVertices);
        ints(out, orEmpty(m.verticesX));
        ints(out, orEmpty(m.verticesY));
        ints(out, orEmpty(m.verticesZ));
        bytes(out, m.facesAlpha);
        shorts(out, m.facesColor);
        return digest(raw.toByteArray());
    }

    private static FrameSet frameset(String group) throws Exception {
        return group.equals("-1") ? null : set(Integer.parseInt(group));
    }

    private static boolean[] mask(String hex) {
        if (hex.equals("-")) return null;
        byte[] raw = HexFormat.of().parseHex(hex);
        boolean[] values = new boolean[raw.length * 8];
        for (int i = 0; i < values.length; i++) values[i] = (raw[i >> 3] >> (i & 7) & 1) != 0;
        return values;
    }

    private static void readBundle(DataInputStream in) throws Exception {
        int count = in.readInt();
        for (int i = 0; i < count; i++) {
            int id = in.readInt();
            byte[] data = new byte[in.readInt()];
            in.readFully(data);
            models.put(id, data);
        }
        count = in.readInt();
        for (int i = 0; i < count; i++) {
            int id = in.readInt();
            byte[] data = new byte[in.readInt()];
            in.readFully(data);
            baseData.put(id, data);
        }
        count = in.readInt();
        for (int i = 0; i < count; i++) {
            int group = in.readInt();
            int files = in.readInt();
            Map<Integer, byte[]> map = new HashMap<>();
            for (int f = 0; f < files; f++) {
                int id = in.readInt();
                byte[] data = new byte[in.readInt()];
                in.readFully(data);
                map.put(id, data);
            }
            setData.put(group, map);
        }
    }

    public static void main(String[] args) throws Exception {
        Path jar = Path.of(args[0]).toRealPath();
        if (!hash(jar).equals(PINNED)) throw new IllegalStateException("jar differs from the pinned oracle");
        Path loaded = Path.of(Mesh.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
        if (!jar.equals(loaded)) throw new IllegalStateException("code source mismatch");
        try (DataInputStream in = new DataInputStream(new java.io.BufferedInputStream(Files.newInputStream(Path.of(args[1])), 1 << 20))) {
            readBundle(in);
        }
        Field unsafeField = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        unsafe = (sun.misc.Unsafe) unsafeField.get(null);

        List<String> cases = Files.readAllLines(Path.of(args[2]), StandardCharsets.US_ASCII);
        int[] counts = new int[3];
        int[] errors = new int[3];
        try (BufferedWriter writer = Files.newBufferedWriter(Path.of(args[3]), StandardCharsets.US_ASCII)) {
            for (String line : cases) {
                if (line.isEmpty()) continue;
                String[] f = line.split("\t");
                int kind = "VOD".indexOf(f[0]);
                String result;
                try {
                    if (f[0].equals("V")) {
                        result = mesh(f[1]).vertexGroups != null ? "1" : "0";
                    } else {
                        ModelGL m = model(mesh(f[1]));
                        FrameSet first = frameset(f[2]);
                        int firstFrame = Integer.parseInt(f[3]);
                        FrameSet next = frameset(f[4]);
                        int nextFrame = Integer.parseInt(f[5]);
                        int cycle = Integer.parseInt(f[6]);
                        int length = Integer.parseInt(f[7]);
                        if (f[0].equals("O")) {
                            m.animateModelFrame(first, firstFrame, next, nextFrame, cycle, length, Integer.parseInt(f[8]), false);
                        } else {
                            m.doubleAnimation(first, firstFrame, next, nextFrame, cycle, length,
                                frameset(f[8]), Integer.parseInt(f[9]), frameset(f[10]), Integer.parseInt(f[11]),
                                Integer.parseInt(f[12]), Integer.parseInt(f[13]), mask(f[14]), false);
                        }
                        result = pose(m);
                    }
                } catch (Throwable error) {
                    result = "error";
                    errors[kind]++;
                }
                counts[kind]++;
                writer.write(line + "\t" + result + "\n");
            }
        }
        System.out.println("visits " + counts[0] + " single " + counts[1] + " (" + errors[1] + " errors) double " + counts[2]
            + " (" + errors[2] + " errors)");
        if (!hash(jar).equals(PINNED)) throw new IllegalStateException("jar changed during the run");
    }
}
