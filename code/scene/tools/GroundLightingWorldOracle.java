import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.flo.FloTypeList;
import com.jagex.game.runetek5.config.flu.FluTypeList;
import com.jagex.game.runetek5.scene.CollisionMap;
import com.jagex.game.runetek5.scene.MapLoader;
import com.jagex.game.runetek5.scene.Scene;
import com.jagex.game.runetek5.scene.SettingsBits;
import com.jagex.game.runetek5.scene.ground.GroundGL;
import com.jagex.game.runetek5.toolkit.OpenGlToolkit;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import sun.misc.Unsafe;

public final class GroundLightingWorldOracle {
    private static final String PINNED = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048";
    private static final Unsafe UNSAFE;

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSAFE = (Unsafe) field.get(null);
        } catch (ReflectiveOperationException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    private static MapLoader loader(int planes, int size) throws Exception {
        Scene scene = (Scene) UNSAFE.allocateInstance(Scene.class);
        FloTypeList flo = (FloTypeList) UNSAFE.allocateInstance(FloTypeList.class);
        FluTypeList flu = (FluTypeList) UNSAFE.allocateInstance(FluTypeList.class);
        return new MapLoader(scene, planes, size, size, false, flo, flu, new SettingsBits(size, size));
    }

    private static CollisionMap[] clippings(int size) {
        CollisionMap[] result = new CollisionMap[4];
        for (int i = 0; i < 4; i++) {
            result[i] = new CollisionMap();
            result[i].sizeX = size;
            result[i].sizeY = size;
            result[i].collisionFlags = new int[size][size];
        }
        return result;
    }

    private static void decode(MapLoader loader, String directory, String prefix, String[] regions, int baseX, int baseY, int size) throws Exception {
        CollisionMap[] clipping = clippings(size);
        for (String region : regions) {
            int id = Integer.parseInt(region);
            Path file = Path.of(directory, prefix + id + ".bin");
            if (!Files.exists(file)) continue;
            int chunkX = (id >> 8) * 64 - baseX;
            int chunkY = (id & 255) * 64 - baseY;
            loader.decodeTilesClient(new Packet(Files.readAllBytes(file)), chunkX, chunkY, baseX, baseY, clipping);
        }
    }

    private static String grid(int[][] values, int size) {
        StringBuilder text = new StringBuilder();
        for (int y = 0; y <= size; y++) for (int x = 0; x <= size; x++) {
            if (text.length() > 0) text.append('\t');
            text.append(values[x][y]);
        }
        return text.toString();
    }

    public static void main(String[] args) throws Exception {
        Path jar = Path.of(args[0]).toRealPath();
        String directory = args[1];
        int baseX = Integer.parseInt(args[2]);
        int baseY = Integer.parseInt(args[3]);
        int size = Integer.parseInt(args[4]);
        String[] regions = args[5].split(",");
        boolean underwater = !args[6].equals("0");
        String before = hash(jar);
        if (!before.equals(PINNED)) throw new IllegalArgumentException("Unexpected oracle JAR");
        Path loaded = Path.of(GroundGL.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
        if (!jar.equals(loaded)) throw new IllegalArgumentException("GroundGL code source mismatch");
        Path loadedLoader = Path.of(MapLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
        if (!jar.equals(loadedLoader)) throw new IllegalArgumentException("MapLoader code source mismatch");
        MapLoader surface = loader(4, size);
        decode(surface, directory, "m", regions, baseX, baseY, size);
        int[][] position = surface.tileHeights[0];
        int[][] normal = position;
        if (underwater) {
            MapLoader below = loader(1, size);
            decode(below, directory, "um", regions, baseX, baseY, size);
            below.offsetTileHeights(position);
            normal = below.tileHeights[0];
        }
        OpenGlToolkit toolkit = (OpenGlToolkit) UNSAFE.allocateInstance(OpenGlToolkit.class);
        GroundGL ground = new GroundGL(toolkit, 0, 7, size, size, 512, position, normal);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        int gridWidth = size + 1;
        for (int z = 0; z <= size; z++) for (int x = 0; x <= size; x++) {
            out.writeInt(Integer.reverseBytes(Float.floatToRawIntBits(ground.lightingX[x][z])));
            out.writeInt(Integer.reverseBytes(Float.floatToRawIntBits(ground.lightingY[x][z])));
            out.writeInt(Integer.reverseBytes(Float.floatToRawIntBits(ground.lightingZ[x][z])));
        }
        long shadowNonZero = 0;
        for (int z = 0; z <= size; z++) for (int x = 0; x <= size; x++) {
            byte value = ground.shadowing[x][z];
            if (value != 0) shadowNonZero++;
            out.writeByte(value);
        }
        Files.write(Path.of(directory, "native-lighting.bin"), bytes.toByteArray());
        Files.writeString(Path.of(directory, "native-heights.tsv"), "surface\t" + grid(position, size) + "\nnormal\t" + grid(normal, size) + "\n");
        if (!before.equals(hash(jar))) throw new IllegalStateException("Oracle JAR changed");
        System.out.println("GROUND_LIGHTING_ORACLE grid=" + gridWidth * gridWidth + " underwater=" + underwater + " shadowNonZero=" + shadowNonZero);
    }

    private static String hash(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }
}
