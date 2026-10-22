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

byte[] archiveBytes(int regionId, boolean patterned) {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    for (int i = 0; i < 4 * 64 * 64; i++) {
        if (!patterned || (i + regionId) % 3 == 0) {
            bytes.write(0);
        } else {
            bytes.write(1);
            bytes.write((regionId * 17 + i * 73) & 255);
        }
    }
    return bytes.toByteArray();
}

long heightHash(MapLoader loader, int size) {
    long hash = 0xcbf29ce484222325L;
    for (int p = 0; p < 4; p++) {
        for (int x = 0; x <= size; x++) {
            for (int y = 0; y <= size; y++) {
                int height = loader.tileHeights[p][x][y];
                for (int b = 0; b < 4; b++) {
                    hash = (hash ^ ((height >>> (b * 8)) & 255)) * 0x100000001b3L;
                }
            }
        }
    }
    return hash;
}

void appendCase(StringBuilder rows, int caseId, int size, int baseX, int baseY, int missingPattern, boolean patterned) throws Exception {
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
    List<Integer> absent = new ArrayList<>();
    for (int rx = (chunkX - half) / 8; rx <= (chunkX + half) / 8; rx++) {
        for (int ry = (chunkY - half) / 8; ry <= (chunkY + half) / 8; ry++) {
            int id = (rx << 8) + ry;
            int index = ids.size();
            ids.add(id);
            boolean missing = missingPattern == 2 || missingPattern == 1 && index % 3 == 1;
            rows.append("region\t").append(caseId).append('\t').append(index).append('\t').append(id).append('\t').append(missing ? "absent" : "present").append('\n');
            if (missing) {
                absent.add(id);
            } else {
                byte[] payload = archiveBytes(id, patterned);
                Packet packet = new Packet(payload);
                loader.decodeTilesClient(packet, rx * 64 - baseX, ry * 64 - baseY, baseX, baseY, collision);
                if (packet.currentPosition != payload.length) {
                    throw new IllegalStateException("native did not consume the terrain payload");
                }
            }
        }
    }
    if (chunkY < 800) {
        for (int id : absent) {
            loader.clearAllPlanes((id >> 8) * 64 - baseX, (id & 255) * 64 - baseY);
        }
    }
    rows.append("case\t").append(caseId).append('\t').append(size).append('\t').append(baseX).append('\t').append(baseY).append('\t').append(missingPattern).append('\t').append(patterned ? "patterned" : "zero").append('\t').append(HexFormat.of().toHexDigits(heightHash(loader, size))).append('\n');
    int bx = 64 - baseX % 64;
    int by = 64 - baseY % 64;
    int[][] positions = {{0, 0}, {1, 1}, {size - 1, size - 1}, {size, size}, {size, 0}, {0, size}, {63, 63}, {64, 64}, {65, 65}, {bx - 1, by - 1}, {bx, by}, {bx + 1, by + 1}, {bx, 1}, {1, by}, {bx - 1, by}, {bx, by - 1}, {bx + 1, by}, {bx, by + 1}, {0, size - 1}, {size - 1, 0}};
    for (int p = 0; p < 4; p++) {
        for (int[] position : positions) {
            int x = position[0];
            int y = position[1];
            rows.append("sample\t").append(caseId).append('\t').append(p).append('\t').append(x).append('\t').append(y).append('\t').append(loader.tileHeights[p][x][y]).append('\n');
        }
    }
}

void generate() throws Exception {
    StringBuilder rows = new StringBuilder();
    byte[] jar = Files.readAllBytes(Path.of(System.getProperty("oracle.jar")));
    rows.append("jar-sha256\t").append(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(jar))).append('\n');
    int caseId = 0;
    for (int id = 0; id < 5; id++) {
        int size = MapSize.get(id).size;
        rows.append("map-size\t").append(id).append('\t').append(size).append('\n');
        for (int offset : new int[]{0, 8, 24, 56}) {
            for (int missing = 0; missing <= 2; missing++) {
                appendCase(rows, caseId++, size, 3200 + offset, 3200 + (offset + 24) % 64, missing, true);
            }
        }
        int half = size >> 4;
        for (int chunkY : new int[]{799, 800}) {
            for (int missing : new int[]{1, 2}) {
                appendCase(rows, caseId++, size, 3208, (chunkY - half) * 8, missing, false);
            }
        }
    }
    Files.writeString(Path.of(System.getProperty("oracle.output")), rows);
    System.out.println("NATIVE_ORACLE_OK rows=" + rows.toString().lines().count());
}

try { generate(); } catch (Throwable error) { error.printStackTrace(); System.exit(1); }
/exit
