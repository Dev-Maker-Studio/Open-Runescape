import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.scene.CollisionMap;
import com.jagex.game.runetek5.scene.MapLoader;
import com.jagex.game.runetek5.scene.SettingsBits;
import com.jagex.game.runetek5.utils.MathUtils;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import sun.misc.Unsafe;

void setField(MapLoader loader, String name, Object value) throws Exception {
    Field field = MapLoader.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(loader, value);
}

MapLoader newLoader() throws Exception {
    Field field = Unsafe.class.getDeclaredField("theUnsafe");
    field.setAccessible(true);
    Unsafe allocator = (Unsafe) field.get(null);
    MapLoader loader = (MapLoader) allocator.allocateInstance(MapLoader.class);
    setField(loader, "regionSizeZ", 4);
    setField(loader, "regionSizeX", 64);
    setField(loader, "regionSizeY", 64);
    setField(loader, "overlayHidden", false);
    setField(loader, "regionMap", new SettingsBits(64, 64));
    setField(loader, "tileHeights", new int[4][65][65]);
    for (String name : new String[]{"underlayIds", "overlayIds", "overlayPathShapes", "overlayRotations"}) {
        setField(loader, name, new byte[4][64][64]);
    }
    return loader;
}

long hashByte(long hash, int value) {
    return (hash ^ (value & 255)) * 0x100000001b3L;
}

long archiveHash(MapLoader loader) {
    long hash = 0xcbf29ce484222325L;
    for (int p = 0; p < 4; p++) {
        for (int x = 0; x < 64; x++) {
            for (int y = 0; y < 64; y++) {
                int height = loader.tileHeights[p][x][y];
                for (int b = 0; b < 4; b++) {
                    hash = hashByte(hash, height >>> (b * 8));
                }
                hash = hashByte(hash, loader.getOverlayIds()[p][x][y]);
                hash = hashByte(hash, loader.getUnderlayIds()[p][x][y]);
                hash = hashByte(hash, loader.getRegionMap().tileFunctionMasks[p][x][y]);
                hash = hashByte(hash, loader.getOverlayPathShapes()[p][x][y]);
                hash = hashByte(hash, loader.getOverlayRotations()[p][x][y]);
            }
        }
    }
    return hash;
}

byte[] archiveBytes(boolean patterned) {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    for (int i = 0; i < 4 * 64 * 64; i++) {
        if (!patterned) {
            bytes.write(0);
            continue;
        }
        bytes.write(2 + i % 48);
        bytes.write((i * 73 + 128) & 255);
        bytes.write(50 + i % 32);
        bytes.write(82 + i % 174);
        if (i % 7 == 0) {
            bytes.write(2 + (i + 17) % 48);
            bytes.write(i & 255);
        }
        if (i % 11 == 0) {
            bytes.write(81);
            bytes.write(255);
        }
        if (i % 3 == 0) {
            bytes.write(0);
        } else {
            bytes.write(1);
            bytes.write(i & 255);
        }
    }
    return bytes.toByteArray();
}

void appendHeight(StringBuilder rows, int x, int y) throws Exception {
    MapLoader loader = newLoader();
    loader.decodeTile(new Packet(new byte[]{0}), 0, 0, 0, 0, 0, x, y, 0, false);
    rows.append("height\t").append(x).append('\t').append(y).append('\t').append(loader.tileHeights[0][0][0]).append('\n');
}

void generate() throws Exception {
    StringBuilder rows = new StringBuilder();
    byte[] jar = Files.readAllBytes(Path.of(System.getProperty("oracle.jar")));
    rows.append("jar-sha256\t").append(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(jar))).append('\n');
    for (int index : new int[]{0, 2048, 4096, 6144}) {
        rows.append("cosine\t").append(index).append('\t').append(MathUtils.COSINE[index]).append('\n');
    }
    for (int x = -932736; x <= -932720; x++) {
        for (int y = -556244; y <= -556232; y++) {
            appendHeight(rows, x, y);
        }
    }
    int[] extremes = {0, 1, -1, 3200, 3211, -1000000, Integer.MAX_VALUE, Integer.MIN_VALUE};
    for (int x : extremes) {
        for (int y : extremes) {
            appendHeight(rows, x, y);
        }
    }
    int seed = 727;
    for (int i = 0; i < 64; i++) {
        seed = seed * 1664525 + 1013904223;
        int x = seed;
        seed = seed * 1664525 + 1013904223;
        appendHeight(rows, x, seed);
    }
    String[] payloads = {"00", "0100", "0101", "0102", "01ff", "020000", "028000", "02ff00", "31ff00", "3200", "5100", "5200", "ff00", "028031ff325152ff0101", "02ff020000"};
    for (int p = 0; p < 4; p++) {
        for (String payload : payloads) {
            MapLoader loader = newLoader();
            for (int lower = 0; lower < p; lower++) {
                loader.decodeTile(new Packet(new byte[]{1, (byte)(7 + lower)}), lower, 0, 0, 0, 0, 3200, 3200, 0, false);
            }
            Packet packet = new Packet(HexFormat.of().parseHex(payload));
            loader.decodeTile(packet, p, 0, 0, 0, 0, 3200, 3200, 0, false);
            rows.append("tile\t").append(p).append('\t').append(payload).append('\t').append(packet.currentPosition).append('\t').append(loader.tileHeights[p][0][0]).append('\t').append(loader.getOverlayIds()[p][0][0] & 255).append('\t').append(loader.getUnderlayIds()[p][0][0] & 255).append('\t').append(loader.getRegionMap().tileFunctionMasks[p][0][0] & 255).append('\t').append(loader.getOverlayPathShapes()[p][0][0] & 255).append('\t').append(loader.getOverlayRotations()[p][0][0] & 255).append('\n');
        }
    }
    int[][] origins = {{3200, 3200}, {-1000000, -600000}, {Integer.MAX_VALUE - 32, Integer.MIN_VALUE + 10}};
    for (boolean patterned : new boolean[]{false, true}) {
        byte[] bytes = archiveBytes(patterned);
        for (int[] origin : origins) {
            MapLoader loader = newLoader();
            CollisionMap[] collision = new CollisionMap[4];
            for (int p = 0; p < 4; p++) {
                collision[p] = new CollisionMap();
                collision[p].collisionFlags = new int[64][64];
            }
            Packet packet = new Packet(bytes);
            loader.decodeTilesClient(packet, 0, 0, origin[0], origin[1], collision);
            rows.append("archive\t").append(patterned ? "patterned" : "zero").append('\t').append(origin[0]).append('\t').append(origin[1]).append('\t').append(packet.currentPosition).append('\t').append(HexFormat.of().toHexDigits(archiveHash(loader))).append('\n');
        }
    }
    Files.writeString(Path.of(System.getProperty("oracle.output")), rows);
    System.out.println("NATIVE_ORACLE_OK rows=" + rows.toString().lines().count());
}

try { generate(); } catch (Throwable error) { error.printStackTrace(); System.exit(1); }
/exit
