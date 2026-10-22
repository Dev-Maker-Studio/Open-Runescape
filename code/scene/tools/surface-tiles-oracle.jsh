import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.scene.CollisionMap;
import com.jagex.game.runetek5.scene.MapLoader;
import com.jagex.game.runetek5.scene.MapSize;
import com.jagex.game.runetek5.scene.SettingsBits;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import sun.misc.Unsafe;

Object field(MapLoader loader, String name) throws Exception {
    Field field = MapLoader.class.getDeclaredField(name);
    field.setAccessible(true);
    return field.get(loader);
}

void setField(MapLoader loader, String name, Object value) throws Exception {
    Field field = MapLoader.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(loader, value);
}

MapLoader newLoader(int size) throws Exception {
    Field field = Unsafe.class.getDeclaredField("theUnsafe");
    field.setAccessible(true);
    Unsafe allocator = (Unsafe) field.get(null);
    MapLoader loader = (MapLoader) allocator.allocateInstance(MapLoader.class);
    setField(loader, "regionSizeZ", 4);
    setField(loader, "regionSizeX", size);
    setField(loader, "regionSizeY", size);
    setField(loader, "overlayHidden", false);
    setField(loader, "regionMap", new SettingsBits(size, size));
    setField(loader, "tileHeights", new int[4][size + 1][size + 1]);
    for (String name : new String[]{"underlayIds", "overlayIds", "overlayPathShapes", "overlayRotations"}) {
        setField(loader, name, new byte[4][size][size]);
    }
    return loader;
}

byte[] archiveBytes(int regionId) {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    for (int p = 0; p < 4; p++) {
        for (int x = 0; x < 64; x++) {
            for (int y = 0; y < 64; y++) {
                int index = (p * 64 + x) * 64 + y;
                int seed = regionId * 37 + p * 101 + x * 13 + y * 29;
                if ((index + regionId) % 11 != 0) {
                    bytes.write(2);
                    bytes.write(255);
                    bytes.write(82);
                    bytes.write(50);
                    boolean zeroOverlay = (index + regionId) % 13 == 0;
                    int tag = zeroOverlay ? 49 : 2 + (regionId + p * 17 + x * 7 + y * 13) % 48;
                    int overlay = zeroOverlay ? 0 : (regionId * 19 + p * 61 + x * 37 + y * 73) & 255;
                    int underlay = 1 + (regionId * 11 + p * 43 + x * 17 + y * 23) % 174;
                    int flags = 1 + (regionId * 7 + p * 11 + x * 5 + y * 3) % 32;
                    bytes.write(tag);
                    bytes.write(overlay);
                    bytes.write(81 + underlay);
                    bytes.write(49 + flags);
                }
                bytes.write(1);
                bytes.write(seed & 255);
            }
        }
    }
    return bytes.toByteArray();
}

byte[] snapshot(MapLoader loader, int size) throws Exception {
    byte[][][][] attributes = {
        (byte[][][]) field(loader, "underlayIds"),
        (byte[][][]) field(loader, "overlayIds"),
        ((SettingsBits) field(loader, "regionMap")).tileFunctionMasks,
        (byte[][][]) field(loader, "overlayPathShapes"),
        (byte[][][]) field(loader, "overlayRotations")
    };
    byte[] captured = new byte[4 * size * size * 5];
    int index = 0;
    for (int p = 0; p < 4; p++) {
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (byte[][][] attribute : attributes) {
                    captured[index++] = attribute[p][x][y];
                }
            }
        }
    }
    return captured;
}

long tileHash(byte[] captured) {
    long hash = 0xcbf29ce484222325L;
    for (byte value : captured) {
        hash = (hash ^ (value & 255)) * 0x100000001b3L;
    }
    return hash;
}

void appendCase(StringBuilder rows, int caseId, int size, int baseX, int baseY, int missingPattern) throws Exception {
    MapLoader loader = newLoader(size);
    CollisionMap[] collision = new CollisionMap[4];
    for (int p = 0; p < 4; p++) {
        collision[p] = new CollisionMap();
        collision[p].collisionFlags = new int[size][size];
    }
    int half = size >> 4;
    int chunkX = baseX / 8 + half;
    int chunkY = baseY / 8 + half;
    List<Integer> ids = new ArrayList<>();
    for (int rx = (chunkX - half) / 8; rx <= (chunkX + half) / 8; rx++) {
        for (int ry = (chunkY - half) / 8; ry <= (chunkY + half) / 8; ry++) {
            int id = (rx << 8) + ry;
            int index = ids.size();
            ids.add(id);
            boolean missing = missingPattern == 2 || missingPattern == 1 && index % 3 == 1;
            rows.append("region\t").append(caseId).append('\t').append(index).append('\t').append(id).append('\t').append(missing ? "absent" : "present").append('\n');
            if (!missing) {
                byte[] payload = archiveBytes(id);
                Packet packet = new Packet(payload);
                loader.decodeTilesClient(packet, rx * 64 - baseX, ry * 64 - baseY, baseX, baseY, collision);
                if (packet.currentPosition != payload.length) {
                    throw new IllegalStateException("native did not consume the terrain payload");
                }
            }
        }
    }
    byte[] captured = snapshot(loader, size);
    rows.append("case\t").append(caseId).append('\t').append(size).append('\t').append(baseX).append('\t').append(baseY).append('\t').append(missingPattern).append('\t').append(HexFormat.of().toHexDigits(tileHash(captured))).append('\n');
    int bx = 64 - baseX % 64;
    int by = 64 - baseY % 64;
    int[][] positions = {{0, 0}, {1, 1}, {size - 1, size - 1}, {63, 63}, {64, 64}, {65, 65}, {bx - 1, by - 1}, {bx, by}, {bx + 1, by + 1}, {bx, 1}, {1, by}, {bx - 1, by}, {bx, by - 1}, {bx + 1, by}, {bx, by + 1}, {0, size - 1}, {size - 1, 0}, {size / 2, size / 3}, {2, 3}, {3, 2}};
    for (int p = 0; p < 4; p++) {
        for (int[] position : positions) {
            int x = position[0];
            int y = position[1];
            rows.append("sample\t").append(caseId).append('\t').append(p).append('\t').append(x).append('\t').append(y);
            int index = ((p * size + x) * size + y) * 5;
            for (int attribute = 0; attribute < 5; attribute++) {
                rows.append('\t').append(captured[index + attribute] & 255);
            }
            rows.append('\n');
        }
    }
}

void generate() throws Exception {
    StringBuilder rows = new StringBuilder();
    byte[] jar = Files.readAllBytes(Path.of(System.getProperty("oracle.jar")));
    String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(jar));
    if (!digest.equals("78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048")) {
        throw new IllegalStateException("unexpected native oracle jar " + digest);
    }
    rows.append("jar-sha256\t").append(digest).append('\n');
    rows.append("capture\tMapLoader.decodeTilesClient\tpre-bridge\tplane/x/y\tunderlay/overlay/render_flags/shape/rotation\n");
    int caseId = 0;
    for (int id = 0; id < 5; id++) {
        int size = MapSize.get(id).size;
        rows.append("map-size\t").append(id).append('\t').append(size).append('\n');
        for (int dx : new int[]{0, 8, 24, 56}) {
            for (int dy : new int[]{0, 8, 24, 56}) {
                appendCase(rows, caseId++, size, 3200 + dx, 3200 + dy, 0);
            }
            for (int missing : new int[]{1, 2}) {
                appendCase(rows, caseId++, size, 3200 + dx, 3200 + (dx + 24) % 64, missing);
            }
        }
        appendCase(rows, caseId++, size, 0, 0, 0);
        int maxBase = ((16383 - (size >> 4) * 16) / 8) * 8;
        appendCase(rows, caseId++, size, maxBase, maxBase, 0);
    }
    Files.writeString(Path.of(System.getProperty("oracle.output")), rows);
    System.out.println("SURFACE_TILES_NATIVE_ORACLE_OK cases=" + caseId + " rows=" + rows.toString().lines().count());
}

try { generate(); } catch (Throwable error) { error.printStackTrace(); System.exit(1); }
/exit
