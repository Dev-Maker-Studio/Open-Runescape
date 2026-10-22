import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.object.ObjectType;
import com.jagex.game.runetek5.config.object.ObjectTypeList;
import com.jagex.game.runetek5.scene.ground.GroundGL;
import com.jagex.game.runetek5.toolkit.OpenGlToolkit;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import sun.misc.Unsafe;

public final class StaticShadowOracle {
    private static final String PINNED = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048";
    private static final int PLANES = 4;
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

    private static final class Placement {
        int id, plane, x, y, kind, rotation;
    }

    private static final class Stats {
        long placements, missing, skippedTextures, walls, interacts, outOfScene, animated, modelDisplacements, outOfRange;
    }

    private static String hash(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }

    private static void checkSource(Path jar, Class<?> type) throws Exception {
        Path loaded = Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
        if (!jar.equals(loaded)) throw new IllegalArgumentException(type.getName() + " code source mismatch");
    }

    private static Map<Integer, ObjectType> definitions(Path bundle, Method decode, Method post) throws Exception {
        ObjectTypeList list = (ObjectTypeList) UNSAFE.allocateInstance(ObjectTypeList.class);
        list.playerIsMember = true;
        Field defaults = ObjectTypeList.class.getDeclaredField("defaultActions");
        UNSAFE.putObject(list, UNSAFE.objectFieldOffset(defaults), new String[] {null, null, null, null, null, "Examine"});
        Map<Integer, ObjectType> types = new HashMap<>();
        try (DataInputStream in = new DataInputStream(Files.newInputStream(bundle))) {
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                int id = in.readInt();
                int length = in.readInt();
                if (length < 0) continue;
                byte[] data = new byte[length];
                in.readFully(data);
                ObjectType type = new ObjectType();
                type.id = id;
                type.locTypeList = list;
                type.actions = list.defaultActions.clone();
                decode.invoke(type, new Packet(data));
                post.invoke(type);
                types.put(id, type);
            }
        }
        return types;
    }

    private static List<Placement> placements(Path work, String[] regions, int baseX, int baseY, int size) throws Exception {
        List<Placement> out = new ArrayList<>();
        for (String region : regions) {
            int id = Integer.parseInt(region);
            Path file = work.resolve("shadow-l" + id + ".bin");
            if (!Files.exists(file)) continue;
            int offsetX = (id >> 8) * 64 - baseX;
            int offsetY = (id & 255) * 64 - baseY;
            Packet buffer = new Packet(Files.readAllBytes(file));
            int locId = -1;
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
                    int sceneX = localX + offsetX;
                    int sceneY = localY + offsetY;
                    if (sceneX > 0 && sceneY > 0 && sceneX < size - 1 && sceneY < size - 1) {
                        Placement p = new Placement();
                        p.id = locId;
                        p.plane = plane;
                        p.x = sceneX;
                        p.y = sceneY;
                        p.kind = description >> 2;
                        p.rotation = description & 3;
                        out.add(p);
                    }
                }
            }
        }
        return out;
    }

    private static Integer size2d(Placement p) {
        long x = ((long) p.id * 2654435761L + (long) p.rotation * 40503L + (long) p.kind * 977L) & 0xFFFFFFFFL;
        x >>= 8;
        if (x % 7 == 0) return null;
        return (int) (x % 160);
    }

    private static boolean stationary(ObjectType type) {
        return !type.isAnimated() && type.alternativeIds == null && !type.hasAnimation && !type.transforms;
    }

    private static void wall(GroundGL ground, Placement p, boolean shadow, Stats stats) {
        int x = p.x;
        int y = p.y;
        boolean any = false;
        if (p.kind == 0) {
            any = true;
            if (shadow) {
                switch (p.rotation) {
                    case 0: ground.setTileShadow(x, y, 50); ground.setTileShadow(x, y + 1, 50); break;
                    case 1: ground.setTileShadow(x, y + 1, 50); ground.setTileShadow(x + 1, y + 1, 50); break;
                    case 2: ground.setTileShadow(x + 1, y, 50); ground.setTileShadow(x + 1, y + 1, 50); break;
                    case 3: ground.setTileShadow(x, y, 50); ground.setTileShadow(x + 1, y, 50); break;
                    default: break;
                }
            }
        } else if (p.kind == 1 || p.kind == 3) {
            any = true;
            if (shadow) {
                switch (p.rotation) {
                    case 0: ground.setTileShadow(x, y + 1, 50); break;
                    case 1: ground.setTileShadow(x + 1, y + 1, 50); break;
                    case 2: ground.setTileShadow(x + 1, y, 50); break;
                    case 3: ground.setTileShadow(x, y, 50); break;
                    default: break;
                }
            }
        }
        if (any && shadow) stats.walls++;
    }

    private static void interact(GroundGL ground, ObjectType type, Placement p, int size, Stats stats) {
        int sizeX = p.rotation != 1 && p.rotation != 3 ? type.sizeX : type.sizeY;
        int sizeY = p.rotation != 1 && p.rotation != 3 ? type.sizeY : type.sizeX;
        stats.interacts++;
        boolean stationary = stationary(type);
        if (!stationary) stats.animated++;
        int endX = p.x + sizeX - 1;
        int endY = p.y + sizeY - 1;
        if (!(p.x >= 0 && p.y >= 0 && endX < size && endY < size)) {
            stats.outOfScene++;
            return;
        }
        if (!(type.staticShadow)) return;
        int displacement = 15;
        if (stationary) {
            Integer model = size2d(p);
            if (model != null) {
                stats.modelDisplacements++;
                displacement = model / 4;
            }
        }
        if (displacement > 30) displacement = 30;
        for (int bx = 0; bx <= sizeX; bx++) {
            for (int by = 0; by <= sizeY; by++) {
                try {
                    ground.setTileShadow(bx + p.x, p.y + by, displacement);
                } catch (ArrayIndexOutOfBoundsException error) {
                    stats.outOfRange++;
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Path jar = Path.of(args[0]).toRealPath();
        Path work = Path.of(args[1]);
        String name = args[2];
        int baseX = Integer.parseInt(args[3]);
        int baseY = Integer.parseInt(args[4]);
        int size = Integer.parseInt(args[5]);
        String[] regions = args[6].split(",");
        String before = hash(jar);
        if (!before.equals(PINNED)) throw new IllegalArgumentException("Unexpected oracle JAR");
        checkSource(jar, GroundGL.class);
        checkSource(jar, ObjectType.class);
        checkSource(jar, Packet.class);
        Method decode = null;
        Method post = null;
        for (Method method : ObjectType.class.getDeclaredMethods()) {
            if (method.getName().startsWith("decodeType")) decode = method;
            if (method.getName().startsWith("postDecode")) post = method;
        }
        decode.setAccessible(true);
        post.setAccessible(true);
        Map<Integer, ObjectType> types = definitions(work.resolve("shadow-defs.bundle"), decode, post);
        List<Placement> placements = placements(work, regions, baseX, baseY, size);
        OpenGlToolkit toolkit = (OpenGlToolkit) UNSAFE.allocateInstance(OpenGlToolkit.class);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        StringBuilder text = new StringBuilder();
        int gridWidth = size + 1;
        for (int textures = 1; textures >= 0; textures--) {
            GroundGL[] grounds = new GroundGL[PLANES];
            for (int plane = 0; plane < PLANES; plane++) {
                int[][] heights = new int[gridWidth][gridWidth];
                grounds[plane] = new GroundGL(toolkit, 0, 7, size, size, 512, heights, heights);
            }
            Stats stats = new Stats();
            for (Placement p : placements) {
                stats.placements++;
                ObjectType type = types.get(p.id);
                if (type == null) {
                    stats.missing++;
                    continue;
                }
                if (textures == 0 && type.requiresTextures) {
                    stats.skippedTextures++;
                    continue;
                }
                if (p.plane >= PLANES) continue;
                GroundGL ground = grounds[p.plane];
                if (p.kind == 10 || p.kind == 11) {
                    interact(ground, type, p, size, stats);
                } else if (p.kind <= 3) {
                    wall(ground, p, type.staticShadow, stats);
                }
            }
            long nonZero = 0;
            for (int plane = 0; plane < PLANES; plane++) {
                byte[][] shadowing = grounds[plane].shadowing;
                for (int y = 0; y <= size; y++) for (int x = 0; x <= size; x++) {
                    bytes.write(shadowing[x][y]);
                    if (shadowing[x][y] != 0) nonZero++;
                }
                for (int y = 0; y <= size; y++) for (int x = 0; x <= size; x++) {
                    byte value = 0;
                    if (x >= 1 && x < size && y >= 1 && y < size) {
                        value = (byte) ((shadowing[x][y - 1] >> 2) + (shadowing[x][y] >> 1) + (shadowing[x - 1][y] >> 2) + (shadowing[x][y + 1] >> 3) + (shadowing[x + 1][y] >> 3));
                    }
                    bytes.write(value);
                }
            }
            text.append("STATS\t").append(name).append('\t').append(textures).append('\t')
                .append(stats.placements).append('\t').append(stats.missing).append('\t').append(stats.skippedTextures).append('\t')
                .append(stats.walls).append('\t').append(stats.interacts).append('\t').append(stats.outOfScene).append('\t')
                .append(stats.animated).append('\t').append(stats.modelDisplacements).append('\t').append(stats.outOfRange).append('\t').append(nonZero).append('\n');
        }
        Files.write(work.resolve("native-shadow-" + name + ".bin"), bytes.toByteArray());
        Files.writeString(work.resolve("native-shadow-" + name + ".tsv"), text.toString());
        if (!before.equals(hash(jar))) throw new IllegalStateException("Oracle JAR changed");
        System.out.println("STATIC_SHADOW_ORACLE " + name + " placements=" + placements.size() + " types=" + types.size());
    }
}
