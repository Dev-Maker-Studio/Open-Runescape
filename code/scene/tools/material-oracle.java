import com.jagex.game.runetek5.config.material.MaterialType;
import com.jagex.game.runetek5.config.material.MaterialTypeList;
import com.jagex.game.runetek5.js5.Js5MasterIndex;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import sun.misc.Unsafe;

class MaterialOracle {
    static final int[] WIDTHS = {1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 4, 1};
    static final StringBuilder ROWS = new StringBuilder();
    static Unsafe allocator;

    static MaterialTypeList decode(byte[] bytes) throws Exception {
        Js5MasterIndex index = (Js5MasterIndex) allocator.allocateInstance(Js5MasterIndex.class);
        index.fileCounts = new int[]{1};
        Js5ResourceProvider provider = (Js5ResourceProvider) allocator.allocateInstance(Js5ResourceProvider.class);
        provider.masterIndex = index;
        provider.archiveFiles = new Object[][]{new Object[]{bytes}};
        provider.archives = new Object[1];
        return new MaterialTypeList(provider, provider, provider);
    }

    static int[] fields(MaterialType value) {
        return new int[]{value.detailsOnly ? 1 : 0, value.isSmall ? 1 : 0,
            value.skipTriangles ? 1 : 0, value.brightness, value.alpha, value.effectId,
            value.effectParam1, value.colorHsl, value.textureSpeedU, value.textureSpeedV,
            value.aBool2087 ? 1 : 0, value.isBrickTile ? 1 : 0, value.mipmapping,
            value.repeatS ? 1 : 0, value.repeatT ? 1 : 0, value.hdr ? 1 : 0,
            value.combineMode, value.effectParam2, value.effectCombiner};
    }

    static void appendFields(MaterialType value) {
        for (int field : fields(value)) {
            ROWS.append('\t').append(field);
        }
        ROWS.append('\n');
    }

    static byte[] payload(int[] presence, int[][] values) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bytes.write(presence.length >>> 8);
        bytes.write(presence.length);
        for (int flag : presence) {
            bytes.write(flag);
        }
        for (int column = 0; column < WIDTHS.length; column++) {
            for (int id = 0; id < presence.length; id++) {
                if (presence[id] == 1) {
                    for (int shift = (WIDTHS[column] - 1) * 8; shift >= 0; shift -= 8) {
                        bytes.write(values[id][column] >>> shift);
                    }
                }
            }
        }
        return bytes.toByteArray();
    }

    static MaterialTypeList table(String name, int[] presence, int[][] values) throws Exception {
        byte[] bytes = payload(presence, values);
        MaterialTypeList list = decode(bytes);
        ROWS.append("table\t").append(name).append('\t').append(HexFormat.of().formatHex(bytes))
            .append('\t').append(list.getSize()).append('\n');
        for (int id = 0; id < list.getSize(); id++) {
            if (list.getMaterials()[id] != null) {
                ROWS.append("material\t").append(name).append('\t').append(id);
                appendFields(list.getMaterial(id));
            }
        }
        return list;
    }

    static void lookup(String name, MaterialTypeList list, int id) {
        ROWS.append("lookup\t").append(name).append('\t').append(id).append('\t');
        try {
            list.getMaterial(id);
            ROWS.append("present\n");
        } catch (RuntimeException error) {
            ROWS.append(error.getClass().getSimpleName()).append('\n');
        }
    }

    static long hashInt(long hash, int value) {
        for (int shift = 0; shift < 32; shift += 8) {
            hash = (hash ^ ((value >>> shift) & 255)) * 0x100000001b3L;
        }
        return hash;
    }

    static int denseValue(int id, int column) {
        if (column == 7) {
            return id;
        }
        if (column == 17) {
            return id * 1664525 + 1013904223;
        }
        return (id + column * 17) & 255;
    }

    static void denseMaximum() throws Exception {
        int[] presence = new int[65535];
        Arrays.fill(presence, 1);
        int[][] values = new int[presence.length][19];
        for (int id = 0; id < values.length; id++) {
            for (int column = 0; column < WIDTHS.length; column++) {
                values[id][column] = denseValue(id, column);
            }
        }
        byte[] bytes = payload(presence, values);
        MaterialTypeList list = decode(bytes);
        long hash = hashInt(0xcbf29ce484222325L, list.getSize());
        for (int id = 0; id < list.getSize(); id++) {
            for (int value : fields(list.getMaterial(id))) {
                hash = hashInt(hash, value);
            }
        }
        ROWS.append("dense-maximum\t").append(bytes.length).append('\t').append(list.getSize())
            .append('\t').append(HexFormat.of().toHexDigits(hash)).append('\n');
        lookup("dense-maximum", list, 65534);
        lookup("dense-maximum", list, 65535);
    }

    static void truncations(String name, byte[] bytes) throws Exception {
        for (int length = 0; length < bytes.length; length++) {
            try {
                decode(Arrays.copyOf(bytes, length));
                throw new IllegalStateException("truncated table accepted: " + name + ":" + length);
            } catch (IndexOutOfBoundsException expected) {
                ROWS.append("truncated\t").append(name).append('\t').append(length)
                    .append('\t').append(expected.getClass().getSimpleName()).append('\n');
            }
        }
        byte[] trailing = Arrays.copyOf(bytes, bytes.length + 2);
        trailing[bytes.length] = (byte) 255;
        trailing[bytes.length + 1] = (byte) 127;
        MaterialTypeList list = decode(trailing);
        ROWS.append("native-trailing\t").append(name).append('\t')
            .append(HexFormat.of().formatHex(trailing)).append('\t').append(list.getSize()).append('\n');
    }

    public static void main(String[] args) throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        allocator = (Unsafe) field.get(null);
        byte[] jar = Files.readAllBytes(Path.of(args[0]));
        ROWS.append("jar-sha256\t").append(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(jar))).append('\n');
        ROWS.append("defaults");
        appendFields(new MaterialType());
        table("empty", new int[0], new int[0][19]);
        table("zero", new int[]{1}, new int[1][19]);
        int[] flags = new int[256];
        for (int id = 0; id < flags.length; id++) {
            flags[id] = id;
        }
        MaterialTypeList sparse = table("presence-bytes", flags, new int[256][19]);
        for (int id : new int[]{Integer.MIN_VALUE, -1, 0, 1, 2, 128, 154, 255, 256, Integer.MAX_VALUE}) {
            lookup("presence-bytes", sparse, id);
        }
        for (int value = 0; value < 256; value++) {
            int[][] values = new int[4][19];
            for (int id = 0; id < 4; id++) {
                for (int column = 0; column < WIDTHS.length; column++) {
                    values[id][column] = (value + column * 17 + id * 67) & 255;
                }
                values[id][7] = value * 257 + id * 12345;
                values[id][17] = value * 1664525 + 1013904223 + id * 0x40000000;
            }
            table("bytes-" + value, new int[]{1, value, 0, 1}, values);
        }
        int[] shorts = {0, 1, 127, 128, 255, 256, 32767, 32768, 65534, 65535};
        for (int value : shorts) {
            int[][] values = new int[1][19];
            values[0][7] = value;
            table("hsl-" + value, new int[]{1}, values);
        }
        int[] ints = {Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -65536, -1, 0, 1, 65535, Integer.MAX_VALUE - 1, Integer.MAX_VALUE};
        for (int value : ints) {
            int[][] values = new int[1][19];
            values[0][17] = value;
            table("param2-" + value, new int[]{1}, values);
        }
        int[] ids = new int[155];
        ids[128] = 1;
        ids[154] = 1;
        MaterialTypeList addressed = table("id-addressing", ids, new int[155][19]);
        for (int id : new int[]{-1, 0, 127, 128, 129, 153, 154, 155}) {
            lookup("id-addressing", addressed, id);
        }
        for (int effect = -256; effect <= 511; effect++) {
            ROWS.append("opaque\t").append(effect).append('\t')
                .append(MaterialTypeList.isOpaqueEffect(effect) ? 1 : 0).append('\n');
        }
        for (int effect : new int[]{Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            ROWS.append("opaque\t").append(effect).append('\t')
                .append(MaterialTypeList.isOpaqueEffect(effect) ? 1 : 0).append('\n');
        }
        table("sparse-truncation", new int[]{0, 1, 255, 1, 2}, new int[5][19]);
        truncations("empty", payload(new int[0], new int[0][19]));
        truncations("zero", payload(new int[]{1}, new int[1][19]));
        truncations("sparse-truncation", payload(new int[]{0, 1, 255, 1, 2}, new int[5][19]));
        denseMaximum();
        Files.writeString(Path.of(args[1]), ROWS);
        System.out.println("NATIVE_MATERIAL_ORACLE_OK rows=" + ROWS.toString().lines().count());
    }
}
