import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.cache.node.impl.ObjectNode;
import com.jagex.game.runetek5.cache.node.IntNode;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.config.sequence.SeqType;
import com.jagex.game.runetek5.config.sequence.SeqTypeList;
import com.jagex.game.runetek5.config.sequence.Sequence;
import com.jagex.game.runetek5.config.sequence.SequenceMask;
import com.jagex.game.runetek5.config.sequence.frameset.AnimBase;
import com.jagex.game.runetek5.config.sequence.frameset.AnimFrame;
import com.jagex.game.runetek5.config.sequence.frameset.FrameSet;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public final class AnimOracle {
    private static final String PINNED = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048";

    private static final class Seq extends Sequence {
        Seq(boolean tween) {
            super(tween);
        }
    }

    private static final Map<Integer, byte[]> baseData = new HashMap<>();
    private static final Map<Integer, Map<Integer, byte[]>> setData = new HashMap<>();
    private static final Map<Integer, byte[]> seqData = new HashMap<>();
    private static final Map<Integer, SeqType> seqs = new HashMap<>();
    private static final Map<Integer, FrameSet> sets = new HashMap<>();
    private static LRUCache seqCache;
    private static LRUCache setCache;

    private static String hash(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }

    private static String digest(byte[] data) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)).substring(0, 12);
    }

    private static void iv(DataOutputStream out, int[] values) throws Exception {
        out.writeInt(values.length);
        for (int value : values) out.writeInt(value);
    }

    private static void ints(DataOutputStream out, int[] values) throws Exception {
        if (values == null) {
            out.writeByte(0);
            return;
        }
        out.writeByte(1);
        iv(out, values);
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

    private static void bools(DataOutputStream out, boolean[] values) throws Exception {
        if (values == null) {
            out.writeByte(0);
            return;
        }
        out.writeByte(1);
        out.writeInt(values.length);
        for (boolean value : values) out.writeByte(value ? 1 : 0);
    }

    private static void base(DataOutputStream out, AnimBase b) throws Exception {
        out.writeInt(b.id);
        out.writeInt(b.numTransformation);
        iv(out, b.transformationType);
        out.writeInt(b.labels.length);
        for (int[] label : b.labels) iv(out, label);
        bools(out, b.shadowed);
        iv(out, b.submeshes);
    }

    private static void frame(DataOutputStream out, AnimFrame f) throws Exception {
        out.writeInt(f.base.id);
        out.writeInt(f.numTransformations);
        out.writeByte(f.modifiesAlpha ? 1 : 0);
        out.writeByte(f.modifiesColor ? 1 : 0);
        out.writeByte(f.modifiesBillboard ? 1 : 0);
        shorts(out, f.baseIndices);
        shorts(out, f.transformationX);
        shorts(out, f.transformationY);
        shorts(out, f.transformationZ);
        shorts(out, f.labels);
        bytes(out, f.tweeningProperties);
    }

    private static void seqType(DataOutputStream out, SeqType s) throws Exception {
        out.writeInt(s.id);
        ints(out, s.durations);
        out.writeInt(s.replayInterval);
        bools(out, s.interLeaveOrder);
        ints(out, s.frames);
        out.writeInt(s.priority);
        out.writeInt(s.replacementShield);
        out.writeInt(s.replacementWeapon);
        out.writeInt(s.replayCount);
        out.writeInt(s.interruptType);
        out.writeInt(s.moveType);
        out.writeInt(s.loopType);
        ints(out, s.interfaceFrames);
        if (s.frameSounds == null) {
            out.writeByte(0);
        } else {
            out.writeByte(1);
            out.writeInt(s.frameSounds.length);
            for (int[] sound : s.frameSounds) ints(out, sound);
        }
        out.writeByte(s.lights ? 1 : 0);
        out.writeByte(s.tweened ? 1 : 0);
        out.writeByte(s.vorbis ? 1 : 0);
        ints(out, s.frameSoundVolume);
        ints(out, s.soundMaxDelay);
        ints(out, s.soundMinDelay);
        TreeSet<Long> keys = new TreeSet<>();
        if (s.parameters != null) {
            for (Node<?> node : (IterableNodeMap<Node<?>>) s.parameters) keys.add(node.pointer);
        }
        out.writeInt(keys.size());
        for (long key : keys) {
            out.writeInt((int) key);
            Node<?> node = s.parameters.get(key);
            if (node instanceof IntNode) {
                out.writeByte(0);
                out.writeInt(((IntNode) node).value);
            } else {
                out.writeByte(1);
                String text = (String) ((ObjectNode) node).value;
                out.writeInt(text.length());
                for (int i = 0; i < text.length(); i++) out.writeByte(text.charAt(i));
            }
            try {
                out.writeInt(s.getCs2Integer((int) key, -77));
            } catch (Throwable error) {
                out.writeInt(-78);
            }
        }
    }

    private static AnimBase makeBase(int id) {
        byte[] data = baseData.get(id);
        if (data == null) throw new IllegalStateException();
        return new AnimBase(id, data);
    }

    private static FrameSet makeSet(int group) throws Exception {
        Map<Integer, byte[]> files = setData.get(group);
        int max = -1;
        for (int id : files.keySet()) max = Math.max(max, id);
        AnimFrame[] frames = new AnimFrame[max + 1];
        Map<Integer, AnimBase> bases = new HashMap<>();
        for (Map.Entry<Integer, byte[]> file : new java.util.TreeMap<>(files).entrySet()) {
            Packet buffer = new Packet(file.getValue());
            buffer.currentPosition = 1;
            int baseId = buffer.readUShort();
            AnimBase b = bases.get(baseId);
            if (b == null) {
                b = makeBase(baseId);
                bases.put(baseId, b);
            }
            frames[file.getKey()] = new AnimFrame(file.getValue(), b);
        }
        FrameSet set = new FrameSet(group);
        Field field = FrameSet.class.getDeclaredField("frames");
        field.setAccessible(true);
        field.set(set, frames);
        return set;
    }

    private static void state(DataOutputStream out, Sequence s) throws Exception {
        out.writeInt(s.getId());
        out.writeByte(s.resetAnimation ? 1 : 0);
        out.writeByte(s.tweening ? 1 : 0);
        out.writeInt(s.speed);
        out.writeInt(s.currentLoopCount);
        out.writeInt(s.currentFrameId);
        out.writeInt(s.nextFrameId);
        out.writeInt(s.currentFrameDuration);
        out.writeInt(s.replayMode);
        out.writeByte(s.hardReset ? 1 : 0);
    }

    private static void mask(DataOutputStream out, SequenceMask m) throws Exception {
        out.writeInt(m.functionMask);
        out.writeByte(m.functionmaskCalculated ? 1 : 0);
        out.writeInt(m.currentFrame);
        out.writeInt(m.currentFrameset == null ? -1 : m.currentFrameset.groupId);
        out.writeInt(m.nextFrame);
        out.writeInt(m.nextFrameset == null ? -1 : m.nextFrameset.groupId);
    }

    private static boolean usable(int id) {
        SeqType type = seqs.get(id);
        if (type == null) return false;
        for (int[] list : new int[][] {type.frames, type.interfaceFrames}) {
            if (list == null) continue;
            for (int frame : list) {
                if (!sets.containsKey(frame >> 16)) return false;
            }
        }
        return true;
    }

    private static String trace(boolean tween, String steps) throws Exception {
        String[] parts = steps.split(";");
        for (String step : parts) {
            String[] p = step.split(",");
            if (p[0].equals("a") && !p[1].equals("-1") && !usable(Integer.parseInt(p[1]))) return "skip";
        }
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(raw);
        Seq seq = new Seq(tween);
        for (String step : parts) {
            String[] p = step.split(",");
            try {
                if (p[0].equals("a")) {
                    seq.animate(Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]), false, p[4].equals("1"));
                    out.writeByte(1);
                } else if (p[0].equals("p")) {
                    out.writeByte(seq.progressCycle(Integer.parseInt(p[1])) ? 2 : 3);
                } else {
                    out.writeByte(seq.isMaskCalculated() ? 4 : 5);
                    out.writeInt(seq.getFunctionMask());
                    mask(out, seq.currentMask);
                    if (seq.nextMask != null) mask(out, seq.nextMask);
                }
            } catch (Throwable error) {
                out.writeByte(9);
            }
            state(out, seq);
            if (!p[0].equals("m")) {
                try {
                    out.writeByte(seq.isMaskCalculated() ? 1 : 0);
                } catch (Throwable error) {
                    out.writeByte(9);
                }
            }
        }
        return digest(raw.toByteArray());
    }

    private static void put(LRUCache cache, Object value, long key) {
        cache.put(value, key);
    }

    public static void main(String[] args) throws Exception {
        Path jar = Path.of(args[0]);
        if (!hash(jar).equals(PINNED)) throw new IllegalStateException("jar differs from the pinned oracle");
        DataInputStream bundle = new DataInputStream(new java.io.BufferedInputStream(Files.newInputStream(Path.of(args[1])), 1 << 20));
        int count = bundle.readInt();
        for (int i = 0; i < count; i++) {
            int id = bundle.readInt();
            byte[] data = new byte[bundle.readInt()];
            bundle.readFully(data);
            baseData.put(id, data);
        }
        count = bundle.readInt();
        for (int i = 0; i < count; i++) {
            int group = bundle.readInt();
            int files = bundle.readInt();
            Map<Integer, byte[]> map = new HashMap<>();
            for (int f = 0; f < files; f++) {
                int id = bundle.readInt();
                byte[] data = new byte[bundle.readInt()];
                bundle.readFully(data);
                map.put(id, data);
            }
            setData.put(group, map);
        }
        count = bundle.readInt();
        for (int i = 0; i < count; i++) {
            int id = bundle.readInt();
            byte[] data = new byte[bundle.readInt()];
            bundle.readFully(data);
            seqData.put(id, data);
        }

        Field unsafeField = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
        SeqTypeList list = (SeqTypeList) unsafe.allocateInstance(SeqTypeList.class);
        seqCache = new LRUCache(1 << 20, 1 << 14);
        setCache = new LRUCache(1 << 20, 1 << 14);
        Field seqField = SeqTypeList.class.getDeclaredField("sequenceCache");
        Field setField = SeqTypeList.class.getDeclaredField("framesetCache");
        unsafe.putObject(list, unsafe.objectFieldOffset(seqField), seqCache);
        unsafe.putObject(list, unsafe.objectFieldOffset(setField), setCache);
        SeqTypeList.seqTypeList = list;

        List<String> cases = Files.readAllLines(Path.of(args[2]), StandardCharsets.US_ASCII);
        int[] counts = new int[4];
        int[] errors = new int[4];
        int skipped = 0;
        try (BufferedWriter writer = Files.newBufferedWriter(Path.of(args[3]), StandardCharsets.US_ASCII)) {
            for (String line : cases) {
                if (line.isEmpty()) continue;
                String[] f = line.split("\t");
                String result;
                int kind = "BFST".indexOf(f[0]);
                try {
                    ByteArrayOutputStream raw = new ByteArrayOutputStream();
                    DataOutputStream out = new DataOutputStream(raw);
                    if (f[0].equals("B")) {
                        base(out, makeBase(Integer.parseInt(f[1])));
                        result = digest(raw.toByteArray());
                    } else if (f[0].equals("F")) {
                        int group = Integer.parseInt(f[1]);
                        FrameSet set = makeSet(group);
                        sets.put(group, set);
                        put(setCache, set, group);
                        AnimFrame[] frames = set.frames;
                        out.writeInt(frames.length);
                        for (AnimFrame fr : frames) {
                            if (fr == null) {
                                out.writeByte(0);
                            } else {
                                out.writeByte(1);
                                frame(out, fr);
                            }
                        }
                        result = digest(raw.toByteArray());
                    } else if (f[0].equals("S")) {
                        int id = Integer.parseInt(f[1]);
                        SeqType type = new SeqType();
                        type.id = id;
                        type.seqtypelist = list;
                        type.decode(new Packet(seqData.get(id)));
                        type.postDecode();
                        seqs.put(id, type);
                        put(seqCache, type, id);
                        seqType(out, type);
                        result = digest(raw.toByteArray());
                    } else {
                        result = trace(f[1].equals("1"), f[2]);
                        if (result.equals("skip")) skipped++;
                    }
                } catch (Throwable error) {
                    result = "error";
                    errors[kind]++;
                }
                counts[kind]++;
                writer.write(line + "\t" + result + "\n");
            }
        }
        System.out.println("bases " + counts[0] + " (" + errors[0] + " errors) framesets " + counts[1] + " (" + errors[1]
            + " errors) seqtypes " + counts[2] + " (" + errors[2] + " errors) traces " + counts[3] + " (" + skipped + " skipped)");
        if (!hash(jar).equals(PINNED)) throw new IllegalStateException("jar changed during the run");
    }
}
