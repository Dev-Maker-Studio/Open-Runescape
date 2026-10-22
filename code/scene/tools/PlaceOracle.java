import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.scene.CollisionMap;
import com.jagex.game.runetek5.config.TypeResult;
import com.jagex.game.runetek5.config.flo.FloTypeList;
import com.jagex.game.runetek5.config.flu.FluTypeList;
import com.jagex.game.runetek5.config.material.MaterialTypeList;
import com.jagex.game.runetek5.config.object.ObjectType;
import com.jagex.game.runetek5.config.object.ObjectTypeList;
import com.jagex.game.runetek5.graphics.model.Mesh;
import com.jagex.game.runetek5.graphics.model.Model;
import com.jagex.game.runetek5.graphics.model.billboard.BillboardTypeList;
import com.jagex.game.runetek5.js5.Js5MasterIndex;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.scene.MapLoader;
import com.jagex.game.runetek5.scene.Scene;
import com.jagex.game.runetek5.scene.SettingsBits;
import com.jagex.game.runetek5.scene.ground.Ground;
import com.jagex.game.runetek5.scene.ground.GroundGL;
import com.jagex.game.runetek5.toolkit.OpenGlToolkit;
import com.jagex.game.runetek5.utils.ColorUtil;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;
import sun.misc.Unsafe;

public final class PlaceOracle {
    private static final String PINNED = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048";
    private static final long BASIS = 0xcbf29ce484222325L;
    private static final long PRIME = 0x100000001b3L;
    private static final int[] DX = {1, 0, -1, 0};
    private static final int[] DY = {0, -1, 0, 1};
    private static final int[] ADX = {1, -1, -1, 1};
    private static final int[] ADY = {-1, -1, 1, 1};
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

    private static final class World {
        String name;
        int sizeX;
        int sizeY;
        int baseX;
        int baseY;
        List<int[]> regions = new ArrayList<>();
    }

    private static final class Entity {
        int seq;
        int role;
        int plane;
        int x;
        int y;
        int locId;
        int shape;
        int rotation;
        char kind;
        int posX;
        int posY;
        int averageHeight;
        int sceneX;
        int sceneY;
        String outcome = "none";
        int count;
        long hx;
        long hy;
        long hz;
    }

    private static final class Tile {
        int wallId = -1;
        Entity primaryDecor;
        Entity secondaryDecor;
    }

    private static String hash(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }

    private static void checkSource(Path jar, Class<?> type) throws Exception {
        Path loaded = Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
        if (!jar.equals(loaded)) throw new IllegalArgumentException(type.getName() + " code source mismatch");
    }

    private static long hashInt(long hash, int value) {
        for (int shift = 0; shift < 32; shift += 8) hash = (hash ^ ((value >>> shift) & 255)) * PRIME;
        return hash;
    }

    private static Field find(Class<?> type, String name) throws Exception {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(type.getName() + "." + name);
    }

    private static void putObj(Object o, String name, Object value) throws Exception {
        UNSAFE.putObject(o, UNSAFE.objectFieldOffset(find(o.getClass(), name)), value);
    }

    private static void putInt(Object o, String name, int value) throws Exception {
        UNSAFE.putInt(o, UNSAFE.objectFieldOffset(find(o.getClass(), name)), value);
    }

    private static Object getObj(Object o, String name) throws Exception {
        return find(o.getClass(), name).get(o);
    }

    private static int getInt(Object o, String name) throws Exception {
        return find(o.getClass(), name).getInt(o);
    }

    private static Js5ResourceProvider provider(int archives, Map<Integer, byte[]> files) throws Exception {
        Js5MasterIndex index = (Js5MasterIndex) UNSAFE.allocateInstance(Js5MasterIndex.class);
        int[] counts = new int[archives];
        Object[][] data = new Object[archives][];
        for (Map.Entry<Integer, byte[]> entry : files.entrySet()) {
            counts[entry.getKey()] = 1;
            data[entry.getKey()] = new Object[] {entry.getValue()};
        }
        index.fileCounts = counts;
        Js5ResourceProvider provider = (Js5ResourceProvider) UNSAFE.allocateInstance(Js5ResourceProvider.class);
        provider.masterIndex = index;
        provider.archiveFiles = data;
        provider.archives = new Object[archives];
        return provider;
    }

    private static MaterialTypeList materials(byte[] payload) throws Exception {
        Js5ResourceProvider provider = provider(1, Map.of(0, payload));
        return new MaterialTypeList(provider, provider, provider);
    }

    private static ObjectTypeList typeList(Js5ResourceProvider meshes) throws Exception {
        ObjectTypeList list = (ObjectTypeList) UNSAFE.allocateInstance(ObjectTypeList.class);
        list.playerIsMember = true;
        list.preferenceMask = 0;
        list.meshCombineCache = new Mesh[4];
        list.locresult = new TypeResult(null, null);
        putObj(list, "cache", new LRUCache(256));
        putObj(list, "modelsByShapeCache", new LRUCache(500));
        putObj(list, "typeresultCache", new LRUCache(30));
        putObj(list, "uidToModelCache", new LRUCache(50));
        putObj(list, "defaultActions", new String[] {null, null, null, null, null, "Examine"});
        putObj(list, "meshResource", meshes);
        return list;
    }

    private static OpenGlToolkit toolkit(MaterialTypeList materials) throws Exception {
        OpenGlToolkit toolkit = (OpenGlToolkit) UNSAFE.allocateInstance(OpenGlToolkit.class);
        putObj(toolkit, "materialProvider", materials);
        return toolkit;
    }

    private static Ground ground(int sizeX, int sizeY, int[][] heights) throws Exception {
        Ground ground = (Ground) UNSAFE.allocateInstance(GroundGL.class);
        putInt(ground, "width", sizeX);
        putInt(ground, "length", sizeY);
        putObj(ground, "tileHeights", heights);
        putInt(ground, "tileUnits", 512);
        putInt(ground, "tileScale", 9);
        return ground;
    }

    private static Map<Integer, byte[]> readBundle(Path path) throws Exception {
        Map<Integer, byte[]> result = new HashMap<>();
        try (InputStream raw = Files.newInputStream(path); DataInputStream in = new DataInputStream(new java.io.BufferedInputStream(raw))) {
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                int id = in.readInt();
                int length = in.readInt();
                if (length < 0) {
                    result.put(id, null);
                } else {
                    byte[] data = new byte[length];
                    in.readFully(data);
                    result.put(id, data);
                }
            }
        }
        return result;
    }

    private static MapLoader loader(int sizeX, int sizeY) throws Exception {
        Scene scene = (Scene) UNSAFE.allocateInstance(Scene.class);
        FloTypeList flo = (FloTypeList) UNSAFE.allocateInstance(FloTypeList.class);
        FluTypeList flu = (FluTypeList) UNSAFE.allocateInstance(FluTypeList.class);
        return new MapLoader(scene, 4, sizeX, sizeY, false, flo, flu, new SettingsBits(sizeX, sizeY));
    }

    private static List<World> readWorlds(Path path, String only) throws Exception {
        List<World> worlds = new ArrayList<>();
        for (String line : Files.readAllLines(path)) {
            if (line.isBlank()) continue;
            String[] f = line.trim().split(" ");
            if (only != null && !f[0].equals(only)) continue;
            World world = new World();
            world.name = f[0];
            world.sizeX = Integer.parseInt(f[1]);
            world.sizeY = Integer.parseInt(f[2]);
            world.baseX = Integer.parseInt(f[3]);
            world.baseY = Integer.parseInt(f[4]);
            for (String entry : f[5].split(",")) {
                String[] p = entry.split(":");
                world.regions.add(new int[] {Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3])});
            }
            worlds.add(world);
        }
        return worlds;
    }

    private static Method decode;
    private static Method post;

    private static ObjectType decodeType(ObjectTypeList list, int id, byte[] data) {
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
            return type;
        } catch (Throwable error) {
            return null;
        }
    }

    private static final class Placement {
        int plane;
        int x;
        int y;
        int locId;
        int shape;
        int rotation;
    }

    private static List<Placement> placements(World world, Path work, int[][][] masks) throws Exception {
        List<Placement> out = new ArrayList<>();
        for (int[] region : world.regions) {
            Path file = work.resolve("shadow-l" + region[1] + ".bin");
            if (!Files.exists(file)) continue;
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
                    int description = buffer.readUByte();
                    Placement p = new Placement();
                    p.y = (packed & 0x3f) + region[3];
                    p.x = ((packed >> 6) & 0x3f) + region[2];
                    p.plane = packed >> 12;
                    p.shape = description >> 2;
                    p.rotation = description & 3;
                    p.locId = locId;
                    if (p.x > 0 && p.y > 0 && p.x < world.sizeX - 1 && p.y < world.sizeY - 1) out.add(p);
                }
            }
        }
        return out;
    }

    private static long[] vertexHashes(Object model) throws Exception {
        int n = getInt(model, "numVertices");
        long[] hashes = {BASIS, BASIS, BASIS};
        String[] names = {"verticesX", "verticesY", "verticesZ"};
        for (int a = 0; a < 3; a++) {
            int[] array = (int[]) getObj(model, names[a]);
            hashes[a] = hashInt(hashes[a], n);
            for (int i = 0; i < n; i++) hashes[a] = hashInt(hashes[a], array == null ? 0 : array[i]);
        }
        return hashes;
    }

    private static final class State {
        final World world;
        final ObjectTypeList list;
        final OpenGlToolkit toolkit;
        final Ground[] grounds;
        final int[][][] heights;
        final Map<Integer, ObjectType> types = new HashMap<>();
        final Map<Long, Tile> tiles = new HashMap<>();
        final List<Entity> entities = new ArrayList<>();
        final Map<Integer, byte[]> defs;
        int seq;
        int modelErrors;
        String firstError;

        State(World world, ObjectTypeList list, OpenGlToolkit toolkit, Ground[] grounds, int[][][] heights, Map<Integer, byte[]> defs) {
            this.world = world;
            this.list = list;
            this.toolkit = toolkit;
            this.grounds = grounds;
            this.heights = heights;
            this.defs = defs;
        }

        ObjectType type(int id) {
            if (!types.containsKey(id)) types.put(id, decodeType(list, id, defs.get(id)));
            return types.get(id);
        }

        Tile tile(int plane, int x, int y, boolean create) {
            long key = ((long) plane << 40) | ((long) x << 20) | y;
            Tile tile = tiles.get(key);
            if (tile == null && create) {
                tile = new Tile();
                tiles.put(key, tile);
            }
            return tile;
        }
    }

    private static Entity make(State s, Placement p, ObjectType type, int sceneX, int sceneY, int avg, int role, char kind, int shape, int rotation, int posX, int posY, boolean stationary) throws Exception {
        Entity e = new Entity();
        e.seq = s.seq;
        e.role = role;
        e.plane = p.plane;
        e.x = p.x;
        e.y = p.y;
        e.locId = p.locId;
        e.shape = shape;
        e.rotation = rotation;
        e.kind = stationary ? kind : 'A';
        e.posX = posX;
        e.posY = posY;
        e.averageHeight = avg;
        e.sceneX = sceneX;
        e.sceneY = sceneY;
        if (stationary) {
            try {
                Ground current = s.grounds[p.plane];
                Ground next = p.plane < 3 ? s.grounds[p.plane + 1] : null;
                TypeResult result = type.getPair(s.toolkit, 2048, shape, rotation, current, next, sceneX, avg, sceneY, false, null);
                if (result == null || result.mainObject == null) {
                    e.outcome = "null";
                } else {
                    long[] h = vertexHashes(result.mainObject);
                    e.outcome = "ok";
                    e.count = getInt(result.mainObject, "numVertices");
                    e.hx = h[0];
                    e.hy = h[1];
                    e.hz = h[2];
                }
            } catch (Throwable error) {
                e.outcome = "error";
                s.modelErrors++;
                if (s.firstError == null) {
                    java.io.StringWriter text = new java.io.StringWriter();
                    error.printStackTrace(new java.io.PrintWriter(text));
                    s.firstError = text.toString();
                }
            }
        }
        s.entities.add(e);
        return e;
    }

    private static void offsetDecor(State s, int plane, int x, int y, int displacement) {
        Tile tile = s.tile(plane, x, y, false);
        if (tile == null) return;
        if (tile.primaryDecor != null) {
            tile.primaryDecor.posX = (short) (displacement * tile.primaryDecor.posX / 64);
            tile.primaryDecor.posY = (short) (displacement * tile.primaryDecor.posY / 64);
        }
        if (tile.secondaryDecor != null) {
            tile.secondaryDecor.posX = (short) (displacement * tile.secondaryDecor.posX / 64);
            tile.secondaryDecor.posY = (short) (displacement * tile.secondaryDecor.posY / 64);
        }
    }

    private static boolean addWall(State s, Placement p, ObjectType type, int sceneX, int sceneY, int avg, boolean stationary) throws Exception {
        int shape = p.shape;
        int rot = p.rotation;
        Tile tile;
        if (shape == 0) {
            make(s, p, type, sceneX, sceneY, avg, 0, 'W', 0, rot, 0, 0, stationary);
            tile = s.tile(p.plane, p.x, p.y, true);
            tile.wallId = type.id;
            if (type.decorDisplacement != 64) offsetDecor(s, p.plane, p.x, p.y, type.decorDisplacement);
            return true;
        }
        if (shape == 1 || shape == 3) {
            make(s, p, type, sceneX, sceneY, avg, 0, 'W', shape, rot, 0, 0, stationary);
            tile = s.tile(p.plane, p.x, p.y, true);
            tile.wallId = type.id;
            return true;
        }
        if (shape == 2) {
            make(s, p, type, sceneX, sceneY, avg, 0, 'W', 2, rot + 4, 0, 0, stationary);
            make(s, p, type, sceneX, sceneY, avg, 1, 'W', 2, (rot + 1) & 3, 0, 0, stationary);
            tile = s.tile(p.plane, p.x, p.y, true);
            tile.wallId = type.id;
            if (type.decorDisplacement != 64) offsetDecor(s, p.plane, p.x, p.y, type.decorDisplacement);
            return true;
        }
        if (shape == 9) {
            make(s, p, type, sceneX, sceneY, avg, 0, 'I', 9, rot, 0, 0, stationary);
            s.tile(p.plane, p.x, p.y, true);
            if (type.decorDisplacement != 64) offsetDecor(s, p.plane, p.x, p.y, type.decorDisplacement);
            return true;
        }
        return false;
    }

    private static int wallDisplacement(State s, Placement p) {
        Tile tile = s.tile(p.plane, p.x, p.y, false);
        if (tile == null || tile.wallId < 0) return -1;
        ObjectType wall = s.type(tile.wallId);
        return wall == null ? -1 : wall.decorDisplacement;
    }

    private static void addWallDecoration(State s, Placement p, ObjectType type, int sceneX, int sceneY, int avg, boolean stationary) throws Exception {
        int shape = p.shape;
        int rot = p.rotation;
        Entity primary;
        Entity secondary = null;
        int wall = wallDisplacement(s, p);
        if (shape == 4) {
            primary = make(s, p, type, sceneX, sceneY, avg, 0, 'D', 4, rot, 0, 0, stationary);
        } else if (shape == 5) {
            int d = wall >= 0 ? wall + 1 : 65;
            primary = make(s, p, type, sceneX, sceneY, avg, 0, 'D', 5, rot, d * DX[rot], d * DY[rot], stationary);
        } else if (shape == 6) {
            int d = wall >= 0 ? wall / 2 + 1 : 33;
            primary = make(s, p, type, sceneX, sceneY, avg, 0, 'D', 6, rot + 4, stationary ? d * DX[rot] : d * ADX[rot], stationary ? d * DY[rot] : d * ADY[rot], stationary);
        } else if (shape == 7) {
            primary = make(s, p, type, sceneX, sceneY, avg, 0, 'D', 7, ((rot + 2) & 3) + 4, 0, 0, stationary);
        } else if (shape == 8) {
            int d = wall >= 0 ? wall / 2 + 1 : 33;
            primary = make(s, p, type, sceneX, sceneY, avg, 0, 'D', 8, rot + 4, d * ADX[rot], d * ADY[rot], stationary);
            secondary = make(s, p, type, sceneX, sceneY, avg, 1, 'D', 8, ((rot + 2) & 3) + 4, 0, 0, stationary);
        } else {
            return;
        }
        Tile tile = s.tile(p.plane, p.x, p.y, true);
        tile.primaryDecor = primary;
        tile.secondaryDecor = secondary;
    }

    private static void addObject(State s, Placement p) throws Exception {
        ObjectType type = s.type(p.locId);
        if (type == null) return;
        int sizeX = type.sizeX;
        int sizeY = type.sizeY;
        if (p.rotation == 1 || p.rotation == 3) {
            sizeX = type.sizeY;
            sizeY = type.sizeX;
        }
        int xA;
        int xB;
        int yA;
        int yB;
        if (sizeX + p.x <= s.world.sizeX) {
            xA = (sizeX >> 1) + p.x;
            xB = ((sizeX + 1) >> 1) + p.x;
        } else {
            xA = p.x;
            xB = p.x + 1;
        }
        if (sizeY + p.y <= s.world.sizeY) {
            yA = p.y + (sizeY >> 1);
            yB = p.y + ((sizeY + 1) >> 1);
        } else {
            yA = p.y;
            yB = p.y + 1;
        }
        int[][] h = s.heights[p.plane];
        int avg = (h[xA][yA] + h[xB][yA] + h[xA][yB] + h[xB][yB]) >> 2;
        int sceneX = (p.x << 9) + (sizeX << 8);
        int sceneY = (p.y << 9) + (sizeY << 8);
        boolean stationary = !type.isAnimated() && type.alternativeIds == null && !type.hasAnimation && !type.transforms;
        int shape = p.shape;
        s.seq++;
        if (shape == 22) {
            make(s, p, type, sceneX, sceneY, avg, 0, 'G', 22, p.rotation, 0, 0, stationary);
        } else if (shape != 10 && shape != 11) {
            if (shape < 12 || shape > 21) {
                if (!addWall(s, p, type, sceneX, sceneY, avg, stationary)) addWallDecoration(s, p, type, sceneX, sceneY, avg, stationary);
            } else {
                make(s, p, type, sceneX, sceneY, avg, 0, 'I', shape, p.rotation, 0, 0, stationary);
            }
        } else {
            make(s, p, type, sceneX, sceneY, avg, 0, 'I', 10, shape == 11 ? p.rotation + 4 : p.rotation, 0, 0, stationary);
        }
    }

    private static void modelIds(ObjectType type, int shape, TreeSet<Integer> out) {
        if (type.shapes == null || type.objectModelIds == null) return;
        int wanted = shape >= 4 && shape <= 8 ? 4 : (shape == 11 ? 10 : shape);
        for (int i = 0; i < type.shapes.length; i++) {
            if (type.shapes[i] == wanted) for (int id : type.objectModelIds[i]) out.add(id);
        }
    }

    public static void main(String[] args) throws Exception {
        Path jar = Path.of(args[0]).toRealPath();
        Path work = Path.of(args[1]);
        String mode = args[2];
        String only = args.length > 3 && !args[3].equals("-") ? args[3] : null;
        String jarBefore = hash(jar);
        if (!jarBefore.equals(PINNED)) throw new IllegalArgumentException("Unexpected oracle JAR");
        for (Class<?> type : new Class<?>[] {Packet.class, MapLoader.class, ObjectType.class, ObjectTypeList.class, Mesh.class, GroundGL.class, OpenGlToolkit.class, Model.class}) checkSource(jar, type);
        for (Method method : ObjectType.class.getDeclaredMethods()) {
            if (method.getName().startsWith("decodeType")) decode = method;
            if (method.getName().startsWith("postDecode")) post = method;
        }
        decode.setAccessible(true);
        post.setAccessible(true);
        boolean full = mode.equals("full");
        Path library = null;
        String libBefore = null;
        if (full) {
            library = Path.of(args[4]).toRealPath();
            libBefore = hash(library);
            System.load(library.toString());
            Class<?> holder = Class.forName(Math.class.getName() + "$RandomNumberGeneratorHolder");
            Field randomField = holder.getDeclaredField("randomNumberGenerator");
            randomField.setAccessible(true);
            ((Random) randomField.get(null)).setSeed(0);
            ColorUtil.calculateHslToRgbTable();
            BillboardTypeList.initialize(provider(1, new HashMap<>()));
        }
        Map<Integer, byte[]> defs = readBundle(work.resolve("defs.bundle"));
        List<World> worlds = readWorlds(work.resolve("scenes.txt"), only);
        StringBuilder summary = new StringBuilder();
        TreeSet<Integer> modelIds = new TreeSet<>();
        StringBuilder out = new StringBuilder();
        Map<Integer, byte[]> models = full ? readBundle(work.resolve("models.bundle")) : null;
        String materialSha = null;
        for (World world : worlds) {
            MapLoader loader = loader(world.sizeX, world.sizeY);
            CollisionMapHolder.fill(world, loader, work);
            int[][][] heights = loader.tileHeights;
            int[][][] masks = null;
            List<Placement> all = placements(world, work, masks);
            if (!full) {
                ObjectTypeList plain = typeList(null);
                for (Placement p : all) {
                    ObjectType type = decodeType(plain, p.locId, defs.get(p.locId));
                    if (type != null) modelIds(type, p.shape, modelIds);
                }
                summary.append(world.name).append(" placements=").append(all.size()).append(' ');
                continue;
            }
            byte[] payload = Files.readAllBytes(work.resolve("materials.bin"));
            materialSha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload));
            MaterialTypeList table = materials(payload);
            int top = 0;
            for (int id : models.keySet()) top = Math.max(top, id);
            ObjectTypeList list = typeList(provider(top + 1, models));
            OpenGlToolkit toolkit = toolkit(table);
            Ground[] grounds = new Ground[4];
            for (int plane = 0; plane < 4; plane++) grounds[plane] = ground(world.sizeX, world.sizeY, heights[plane]);
            State state = new State(world, list, toolkit, grounds, heights, defs);
            for (Placement p : all) {
                addObject(state, p);
            }
            for (Entity e : state.entities) {
                out.append("E\t").append(world.name).append('\t').append(e.seq).append('\t').append(e.role).append('\t').append(e.plane).append('\t').append(e.x).append('\t').append(e.y)
                    .append('\t').append(e.locId).append('\t').append(e.shape).append('\t').append(e.rotation).append('\t').append(e.kind).append('\t').append(e.posX).append('\t').append(e.posY)
                    .append('\t').append(e.averageHeight).append('\t').append(e.sceneX).append('\t').append(e.sceneY).append('\t').append(e.outcome).append('\t').append(e.count)
                    .append('\t').append(Long.toUnsignedString(e.hx)).append('\t').append(Long.toUnsignedString(e.hy)).append('\t').append(Long.toUnsignedString(e.hz)).append('\n');
            }
            for (int plane = 0; plane < 4; plane++) {
                long hh = BASIS;
                for (int[] column : heights[plane]) for (int v : column) hh = hashInt(hh, v);
                out.append("H\t").append(world.name).append('\t').append(plane).append('\t').append(Long.toUnsignedString(hh)).append('\n');
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream dout = new DataOutputStream(bytes);
            for (int plane = 0; plane < 4; plane++) for (int[] column : heights[plane]) for (int v : column) dout.writeInt(v);
            Files.write(work.resolve("place-heights-" + world.name + ".bin"), bytes.toByteArray());
            int ok = 0;
            int nulls = 0;
            for (Entity e : state.entities) {
                if (e.outcome.equals("ok")) ok++;
                if (e.outcome.equals("null")) nulls++;
            }
            summary.append(world.name).append(" placements=").append(all.size()).append(" entities=").append(state.entities.size()).append(" ok=").append(ok).append(" null=").append(nulls).append(" errors=").append(state.modelErrors).append(' ');
            if (state.firstError != null) System.err.println(state.firstError);
        }
        if (!full) {
            StringBuilder ids = new StringBuilder();
            for (int id : modelIds) ids.append(id).append('\n');
            Files.writeString(work.resolve("used-model-ids.txt"), ids.toString(), StandardCharsets.US_ASCII);
        } else {
            Files.writeString(work.resolve("native-place.tsv"), out.toString(), StandardCharsets.US_ASCII);
            Files.writeString(work.resolve("place-material-sha.txt"), materialSha + "\n", StandardCharsets.US_ASCII);
            if (!libBefore.equals(hash(library))) throw new IllegalStateException("Native library changed");
        }
        if (!jarBefore.equals(hash(jar))) throw new IllegalStateException("Oracle JAR changed");
        System.out.println("PLACE_ORACLE ok " + mode + " " + summary.toString().trim());
    }

    private static final class CollisionMapHolder {
        static void fill(World world, MapLoader loader, Path work) throws Exception {
            CollisionMap[] maps = new CollisionMap[4];
            for (int i = 0; i < 4; i++) maps[i] = CollisionMap.createClipMap(world.sizeX, world.sizeY);
            for (int[] region : world.regions) {
                Path mapFile = work.resolve("place-m" + region[0] + ".bin");
                if (!Files.exists(mapFile)) continue;
                byte[] data = Files.readAllBytes(mapFile);
                loader.decodeTilesClient(new Packet(data), region[2], region[3], world.baseX, world.baseY, maps);
            }
        }
    }
}
