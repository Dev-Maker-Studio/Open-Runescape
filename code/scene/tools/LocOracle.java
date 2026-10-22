import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.IntNode;
import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.cache.node.impl.ObjectNode;
import com.jagex.game.runetek5.config.object.ObjectType;
import com.jagex.game.runetek5.config.object.ObjectTypeList;
import com.jagex.game.runetek5.js5.Js5Compression;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import sun.misc.Unsafe;

public final class LocOracle {
    private static final String PINNED = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048";
    private static final Unsafe UNSAFE;
    private static final int[][] KEYS = {
        {0x01234567, 0x89abcdef, 0xfedcba98, 0x76543210},
        {-1, 0, Integer.MIN_VALUE, Integer.MAX_VALUE},
        {1, 2, 3, 4},
    };

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSAFE = (Unsafe) field.get(null);
        } catch (ReflectiveOperationException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    private static String hash(byte[] data) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
    }

    private static String hash(Path path) throws Exception {
        return hash(Files.readAllBytes(path));
    }

    private static void checkSource(Path jar, Class<?> type) throws Exception {
        Path loaded = Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
        if (!jar.equals(loaded)) throw new IllegalArgumentException(type.getName() + " code source mismatch");
    }

    private static String text(String value) {
        if (value == null) return "null";
        StringBuilder out = new StringBuilder("u");
        for (int i = 0; i < value.length(); i++) {
            if (i > 0) out.append('.');
            out.append(Integer.toHexString(value.charAt(i)));
        }
        return out.toString();
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

    private static String bytes(byte[] values) {
        if (values == null) return "null";
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) out.append(',');
            out.append(values[i]);
        }
        return out.append(']').toString();
    }

    private static int flag(boolean value) {
        return value ? 1 : 0;
    }

    private static String param(Node<?> node) {
        if (node instanceof IntNode) return "i" + ((IntNode) node).value;
        return text((String) ((ObjectNode) node).value);
    }

    private static String canonical(ObjectType t) {
        List<String> f = new ArrayList<>();
        f.add("id=" + t.id);
        f.add("name=" + text(t.name));
        if (t.actions == null) {
            f.add("actions=null");
        } else {
            StringBuilder a = new StringBuilder("actions=[");
            for (int i = 0; i < t.actions.length; i++) {
                if (i > 0) a.append(',');
                a.append(text(t.actions[i]));
            }
            f.add(a.append(']').toString());
        }
        f.add("shapes=" + bytes(t.shapes));
        if (t.objectModelIds == null) {
            f.add("modelIds=null");
        } else {
            StringBuilder m = new StringBuilder("modelIds=[");
            for (int i = 0; i < t.objectModelIds.length; i++) {
                if (i > 0) m.append(',');
                m.append(ints(t.objectModelIds[i]));
            }
            f.add(m.append(']').toString());
        }
        f.add("sizeX=" + t.sizeX);
        f.add("sizeY=" + t.sizeY);
        f.add("blocksMovement=" + t.blocksMovement);
        f.add("blocksProjectiles=" + flag(t.blocksProjectiles));
        f.add("hasActions=" + t.hasActions);
        f.add("delayShading=" + flag(t.delayShading));
        f.add("occlusionMode=" + t.occlusionMode);
        f.add("decorDisplacement=" + t.decorDisplacement);
        f.add("inverted=" + flag(t.inverted));
        f.add("staticShadow=" + flag(t.staticShadow));
        f.add("forceDisplayDecoration=" + flag(t.forceDisplayDecoration));
        f.add("ignoreClipOnAltRoute=" + flag(t.ignoreClipOnAltRoute));
        f.add("supportsItems=" + t.supportsItems);
        f.add("soundId=" + t.soundId);
        f.add("soundRadius=" + t.soundRadius);
        f.add("soundMinInterval=" + t.soundMinInterval);
        f.add("soundMaxInterval=" + t.soundMaxInterval);
        f.add("soundGroupIds=" + ints(t.soundGroupIds));
        f.add("alternativeIds=" + ints(t.alternativeIds));
        f.add("requiresTextures=" + flag(t.requiresTextures));
        f.add("dynamicShadow=" + flag(t.dynamicShadow));
        f.add("replaySequence=" + flag(t.replaySequence));
        f.add("members=" + flag(t.members));
        f.add("mapIconRotates=" + flag(t.mapIconRotates));
        f.add("hasAnimation=" + flag(t.hasAnimation));
        f.add("primaryCursorActionIndex=" + t.primaryCursorActionIndex);
        f.add("primaryCursor=" + t.primaryCursor);
        f.add("secondaryCursorActionIndex=" + t.secondaryCursorActionIndex);
        f.add("secondaryCursor=" + t.secondaryCursor);
        f.add("mapIconRotation=" + t.mapIconRotation);
        f.add("mapIconId=" + t.mapIconId);
        f.add("soundVolume=" + t.soundVolume);
        f.add("mapIconFlipped=" + flag(t.mapIconFlipped));
        f.add("mapCategoryId=" + t.mapCategoryid);
        f.add("quests=" + ints(t.quests));
        f.add("groundDecorationHeight=" + t.groundDecorationHeight);
        f.add("instrumentSoundEffect=" + flag(t.instrumentSoundEffect));
        f.add("instrumentAmbientSound=" + flag(t.instrumentAmbientSound));
        f.add("cullY=" + t.cullY);
        f.add("cullXZ=" + t.cullXZ);
        f.add("ambientSoundMaxDelay=" + t.ambientSoundMaxDelay);
        f.add("ambientSoundMinDelay=" + t.ambientSoundMinDelay);
        f.add("transforms=" + flag(t.transforms));
        f.add("ambientSoundMaxHearDistance=" + t.ambientSoundMaxHearDistance);
        f.add("dynamicTint=" + flag(t.dynamicTint));
        f.add("groundContourType=" + t.groundContourType);
        f.add("animations=" + ints(t.animations));
        f.add("ambient=" + t.ambient);
        f.add("contrast=" + t.contrast);
        f.add("recolorSrc=" + shorts(t.recolorSrc));
        f.add("recolorDst=" + shorts(t.recolorDst));
        f.add("retextureSrc=" + shorts(t.retextureSrc));
        f.add("retextureDst=" + shorts(t.retextureDst));
        f.add("recolorPalette=" + bytes(t.recolorDPalette));
        f.add("resizeX=" + t.resizeX);
        f.add("resizeY=" + t.resizeY);
        f.add("resizeZ=" + t.resizeZ);
        f.add("offsetX=" + t.offsetX);
        f.add("offsetY=" + t.offsetY);
        f.add("offsetZ=" + t.offsetZ);
        f.add("varpbitId=" + t.varpbitId);
        f.add("varpId=" + t.varpId);
        f.add("groundContourModifier=" + t.goundContourModifier);
        f.add("animationOdds=" + ints(t.animationOdds));
        f.add("tintHue=" + t.tintHue);
        f.add("tintSaturation=" + t.tintSaturation);
        f.add("tintLightness=" + t.tintLightness);
        f.add("tintOpacity=" + t.tintOpacity);
        f.add("shadowOffsetX=" + t.shadowOffsetX);
        f.add("shadowOffsetY=" + t.shadowOffsetY);
        f.add("shadowOffsetZ=" + t.shadowOffsetZ);
        IterableNodeMap<Node<?>> params = t.parameters;
        if (params == null) {
            f.add("parameters=null");
            f.add("parameterFirst=null");
        } else {
            StringBuilder all = new StringBuilder("parameters=[");
            StringBuilder first = new StringBuilder("parameterFirst=[");
            List<Long> seen = new ArrayList<>();
            boolean allFirst = true;
            boolean firstFirst = true;
            for (Node<?> node = params.first(); node != null; node = params.next()) {
                if (!allFirst) all.append(',');
                allFirst = false;
                all.append(node.pointer).append(':').append(param(node));
                if (!seen.contains(node.pointer)) {
                    seen.add(node.pointer);
                    if (!firstFirst) first.append(',');
                    firstFirst = false;
                    first.append(node.pointer).append(':').append(param(params.get(node.pointer)));
                }
            }
            f.add(all.append(']').toString());
            f.add(first.append(']').toString());
        }
        return String.join("\t", f);
    }

    private static ObjectTypeList typeList(boolean member) throws Exception {
        ObjectTypeList list = (ObjectTypeList) UNSAFE.allocateInstance(ObjectTypeList.class);
        list.playerIsMember = member;
        Field defaults = ObjectTypeList.class.getDeclaredField("defaultActions");
        UNSAFE.putObject(list, UNSAFE.objectFieldOffset(defaults), new String[] {null, null, null, null, null, "Examine"});
        return list;
    }

    private static String decodeType(ObjectTypeList list, int id, byte[] data, Method decode, Method post) {
        try {
            ObjectType type = new ObjectType();
            type.id = id;
            type.locTypeList = list;
            type.actions = list.defaultActions.clone();
            if (data != null) decode.invoke(type, new Packet(data));
            post.invoke(type);
            if (type.ignoreClipOnAltRoute) {
                type.blocksMovement = 0;
                type.blocksProjectiles = false;
            }
            if (!list.playerIsMember && type.members) {
                type.actions = null;
                type.quests = null;
            }
            return canonical(type);
        } catch (Throwable error) {
            return "error";
        }
    }

    private static void placements(Path work, String[] regions, PrintStream out) throws Exception {
        for (String region : regions) {
            for (String kind : new String[] {"l", "ul"}) {
                Path file = work.resolve(kind + region + ".container");
                if (!Files.exists(file)) continue;
                byte[] payload = Js5Compression.decompress(Files.readAllBytes(file));
                out.println(region + "\t" + kind + "\tPAYLOAD\t" + payload.length + "\t" + hash(payload));
                Packet buffer = new Packet(payload);
                int locId = -1;
                int count = 0;
                String failure = null;
                try {
                    while (true) {
                        int idOffset = buffer.readSum();
                        if (idOffset == 0) break;
                        locId += idOffset;
                        int packed = 0;
                        while (true) {
                            int positionOffset = buffer.readUSmart();
                            if (positionOffset == 0) break;
                            packed += positionOffset - 1;
                            int localY = packed & 0x3f;
                            int localX = (packed >> 6) & 0x3f;
                            int plane = packed >> 12;
                            int description = buffer.readUByte();
                            out.println(region + "\t" + kind + "\t" + locId + "\t" + plane + "\t" + localX + "\t" + localY + "\t" + (description >> 2) + "\t" + (description & 3));
                            count++;
                        }
                    }
                } catch (Throwable error) {
                    failure = error.getClass().getSimpleName();
                }
                out.println(region + "\t" + kind + "\tEND\t" + count + "\t" + (failure == null ? "ok" : "error"));
            }
        }
    }

    private static void xtea(Path work, String[] regions, PrintStream out) throws Exception {
        int cases = 0;
        for (String region : regions) {
            Path file = work.resolve("l" + region + ".container");
            if (!Files.exists(file)) continue;
            byte[] plain = Files.readAllBytes(file);
            byte[] expected = Js5Compression.decompress(plain);
            for (int k = 0; k < KEYS.length; k++) {
                byte[] copy = Arrays.copyOf(plain, plain.length);
                Packet packet = new Packet(copy);
                packet.encryptXtea(KEYS[k], 5, copy.length);
                byte[] encrypted = Arrays.copyOf(copy, copy.length);
                Files.write(work.resolve("xtea-l" + region + "-" + k + ".bin"), encrypted);
                Packet back = new Packet(Arrays.copyOf(encrypted, encrypted.length));
                back.decryptXtea(KEYS[k], encrypted.length);
                boolean restored = Arrays.equals(back.buffer, plain);
                boolean decoded = Arrays.equals(Js5Compression.decompress(back.buffer), expected);
                boolean changed = !Arrays.equals(encrypted, plain);
                out.println("XTEA\t" + region + "\t" + k + "\t" + encrypted.length + "\t" + hash(encrypted) + "\trestored=" + restored + "\tdecoded=" + decoded + "\tchanged=" + changed);
                if (!restored || !decoded) throw new IllegalStateException("xtea roundtrip failed");
                cases++;
            }
        }
        for (int k = 0; k < KEYS.length; k++) {
            out.println("KEY\t" + k + "\t" + KEYS[k][0] + "\t" + KEYS[k][1] + "\t" + KEYS[k][2] + "\t" + KEYS[k][3]);
        }
        out.println("XTEA_CASES\t" + cases);
    }

    private static void definitions(Path bundle, Path output, Method decode, Method post) throws Exception {
        ObjectTypeList member = typeList(true);
        ObjectTypeList guest = typeList(false);
        int total = 0;
        int errors = 0;
        try (DataInputStream in = new DataInputStream(Files.newInputStream(bundle));
             PrintStream out = new PrintStream(new BufferedOutputStream(Files.newOutputStream(output), 1 << 20), false, StandardCharsets.US_ASCII)) {
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                int id = in.readInt();
                int length = in.readInt();
                byte[] data = null;
                if (length >= 0) {
                    data = new byte[length];
                    in.readFully(data);
                }
                String a = decodeType(member, id, data, decode, post);
                String b = decodeType(guest, id, data, decode, post);
                out.println(id + "\t1\t" + a);
                out.println(id + "\t0\t" + b);
                total += 2;
                if (a.equals("error")) errors++;
                if (b.equals("error")) errors++;
            }
        }
        System.out.println("DEFINITIONS total=" + total + " errors=" + errors);
    }

    public static void main(String[] args) throws Exception {
        Path jar = Path.of(args[0]).toRealPath();
        Path work = Path.of(args[1]);
        String[] regions = args[2].isEmpty() ? new String[0] : args[2].split(",");
        String before = hash(jar);
        if (!before.equals(PINNED)) throw new IllegalArgumentException("Unexpected oracle JAR");
        checkSource(jar, Packet.class);
        checkSource(jar, ObjectType.class);
        checkSource(jar, Js5Compression.class);
        Method decode = null;
        Method post = null;
        for (Method method : ObjectType.class.getDeclaredMethods()) {
            if (method.getName().startsWith("decodeType")) decode = method;
            if (method.getName().startsWith("postDecode")) post = method;
        }
        decode.setAccessible(true);
        post.setAccessible(true);
        try (PrintStream out = new PrintStream(Files.newOutputStream(work.resolve("native-locs.tsv")), false, StandardCharsets.US_ASCII)) {
            placements(work, regions, out);
        }
        try (PrintStream out = new PrintStream(Files.newOutputStream(work.resolve("native-xtea.tsv")), false, StandardCharsets.US_ASCII)) {
            xtea(work, regions, out);
        }
        Path bundle = work.resolve("defs.bundle");
        if (Files.exists(bundle)) definitions(bundle, work.resolve("native-defs.tsv"), decode, post);
        if (!before.equals(hash(jar))) throw new IllegalStateException("Oracle JAR changed");
        System.out.println("LOC_ORACLE ok");
    }
}
