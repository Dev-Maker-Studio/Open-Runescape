import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.bas.BasType;
import com.jagex.game.runetek5.config.flo.FloTypeList;
import com.jagex.game.runetek5.config.flu.FluTypeList;
import com.jagex.game.runetek5.config.npc.NPCType;
import com.jagex.game.runetek5.config.object.ObjectShapes;
import com.jagex.game.runetek5.config.object.ObjectType;
import com.jagex.game.runetek5.config.object.ObjectTypeList;
import com.jagex.game.runetek5.config.sequence.SeqType;
import com.jagex.game.runetek5.config.sequence.Sequence;
import com.jagex.game.runetek5.entity.PathingEntitySpotAnimation;
import com.jagex.game.runetek5.entity.pathingentity.EntityTurnDirection;
import com.jagex.game.runetek5.entity.pathingentity.EntityUpdating;
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity;
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity;
import com.jagex.game.runetek5.math.Vector3Pool;
import com.jagex.game.runetek5.movement.CoordinateSpace;
import com.jagex.game.runetek5.movement.pathfinding.RouteFinder;
import com.jagex.game.runetek5.movement.pathfinding.RouteStrategies;
import com.jagex.game.runetek5.movement.pathfinding.RouteStrategy;
import com.jagex.game.runetek5.scene.CollisionMap;
import com.jagex.game.runetek5.scene.MapLoader;
import com.jagex.game.runetek5.scene.Scene;
import com.jagex.game.runetek5.scene.SettingsBits;
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

public final class CollisionOracle {
    private static final String PINNED = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048";
    private static final Unsafe UNSAFE;
    private static final long SEED = 0x5EEDC0111DEAL;
    private static final int[] FLAG_CHOICES = {0, 0x100, 0x20000, 0x200000, 0x240000, 0x40000, 0x1, 0x2, 0x4, 0x8, 0x10000, 0x1000, 0x400, 0x200, 0x40000000};

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSAFE = (Unsafe) field.get(null);
        } catch (ReflectiveOperationException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    private static String hash(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }

    private static void checkSource(Path jar, Class<?> type) throws Exception {
        Path loaded = Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
        if (!jar.equals(loaded)) throw new IllegalArgumentException(type.getName() + " code source mismatch");
    }

    private static final class World {
        String name;
        int sizeX;
        int sizeY;
        int baseX;
        int baseY;
        List<int[]> regions = new ArrayList<>();
        CollisionMap[] maps;
    }

    private static final class Synth {
        String id;
        boolean clip;
        int offsetX;
        int offsetY;
        int sizeX;
        int sizeY;
        CollisionMap map;
    }

    private static final class Target {
        String name;
        CollisionMap map;
        boolean synthetic;
    }

    private static ObjectTypeList typeList() throws Exception {
        ObjectTypeList list = (ObjectTypeList) UNSAFE.allocateInstance(ObjectTypeList.class);
        list.playerIsMember = true;
        Field defaults = ObjectTypeList.class.getDeclaredField("defaultActions");
        UNSAFE.putObject(list, UNSAFE.objectFieldOffset(defaults), new String[] {null, null, null, null, null, "Examine"});
        return list;
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

    private static List<World> readWorlds(Path path) throws Exception {
        List<World> worlds = new ArrayList<>();
        for (String line : Files.readAllLines(path)) {
            if (line.isBlank()) continue;
            String[] f = line.trim().split(" ");
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

    private static ObjectType decodeType(ObjectTypeList list, int id, byte[] data, Method decode, Method post) {
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

    private static void applyLoc(CollisionMap map, ObjectType type, int x, int y, int rotation, int shape) {
        int sizeX = type.sizeX;
        int sizeY = type.sizeY;
        if (rotation == 1 || rotation == 3) {
            sizeX = type.sizeY;
            sizeY = type.sizeX;
        }
        boolean notAlt = !type.ignoreClipOnAltRoute;
        if (shape == 22) {
            if (type.blocksMovement == 1) map.flagBlockedDecoration(x, y);
        } else if (shape == 10 || shape == 11) {
            if (type.blocksMovement != 0) map.flagBlockedObject(x, y, sizeX, sizeY, type.blocksProjectiles, notAlt);
        } else if (shape >= 12 && shape <= 21) {
            if (type.blocksMovement != 0) map.flagBlockedObject(x, y, sizeX, sizeY, type.blocksProjectiles, notAlt);
        } else if (shape >= 0 && shape <= 3) {
            if (type.blocksMovement != 0) map.flagBoundary(x, y, shape, rotation, type.blocksProjectiles, notAlt);
        } else if (shape == 9) {
            if (type.blocksMovement != 0) map.flagBlockedObject(x, y, sizeX, sizeY, type.blocksProjectiles, notAlt);
        }
    }

    private static int[] buildWorld(World world, Path work, Map<Integer, byte[]> bundle, TreeSet<Integer> used, Method decode, Method post, ObjectTypeList list, Map<Integer, ObjectType> cache) throws Exception {
        MapLoader loader = loader(world.sizeX, world.sizeY);
        world.maps = new CollisionMap[4];
        for (int i = 0; i < 4; i++) world.maps[i] = CollisionMap.createClipMap(world.sizeX, world.sizeY);
        for (int[] region : world.regions) {
            byte[] data = Files.readAllBytes(work.resolve("coll-m" + region[0] + ".bin"));
            loader.decodeTilesClient(new Packet(data), region[2], region[3], world.baseX, world.baseY, world.maps);
        }
        byte[][][] masks = loader.getRegionMap().tileFunctionMasks;
        for (int plane = 0; plane < 4; plane++) {
            for (int x = 0; x < world.sizeX; x++) {
                for (int y = 0; y < world.sizeY; y++) {
                    if ((masks[plane][x][y] & 0x1) != 0) {
                        int height = plane;
                        if ((masks[1][x][y] & 0x2) != 0) height = plane - 1;
                        if (height >= 0) world.maps[height].setUnwalkable(x, y);
                    }
                }
            }
        }
        int placements = 0;
        int applied = 0;
        for (int[] region : world.regions) {
            Path file = work.resolve("coll-l" + region[1] + ".bin");
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
                    int localY = packed & 0x3f;
                    int localX = (packed >> 6) & 0x3f;
                    int plane = packed >> 12;
                    int description = buffer.readUByte();
                    int shape = description >> 2;
                    int rotation = description & 0x3;
                    int sceneX = localX + region[2];
                    int sceneY = localY + region[3];
                    if (sceneX > 0 && sceneY > 0 && sceneX < world.sizeX - 1 && sceneY < world.sizeY - 1) {
                        placements++;
                        used.add(locId);
                        int collisionPlane = plane;
                        if ((masks[1][sceneX][sceneY] & 0x2) == 2) collisionPlane = plane - 1;
                        if (collisionPlane >= 0 && bundle != null && bundle.containsKey(locId)) {
                            ObjectType type;
                            if (cache.containsKey(locId)) {
                                type = cache.get(locId);
                            } else {
                                type = decodeType(list, locId, bundle.get(locId), decode, post);
                                cache.put(locId, type);
                            }
                            if (type != null) {
                                applyLoc(world.maps[collisionPlane], type, sceneX, sceneY, rotation, shape);
                                applied++;
                            }
                        }
                    }
                }
            }
        }
        return new int[] {placements, applied};
    }

    private static byte[] flagBytes(CollisionMap[] maps) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        for (CollisionMap map : maps) {
            for (int x = 0; x < map.sizeX; x++) for (int y = 0; y < map.sizeY; y++) out.writeInt(map.collisionFlags[x][y]);
        }
        return bytes.toByteArray();
    }

    private static boolean chance(Random random, int percent) {
        return random.nextInt(100) < percent;
    }

    private static int coord(Random random, int low, int high, int wild) {
        if (chance(random, wild)) return low - 40 + random.nextInt(high - low + 81);
        return low - 3 + random.nextInt(high - low + 7);
    }

    private static String runOp(CollisionMap map, String[] f) {
        try {
            switch (f[0]) {
                case "reset": map.reset(); break;
                case "ubscene": {
                    int w = Integer.parseInt(f[1]);
                    int h = Integer.parseInt(f[2]);
                    for (int x = 0; x < w; x++) for (int y = 0; y < h; y++) map.unblock(x, y);
                    break;
                }
                case "fbo": map.flagBlockedObject(i(f, 1), i(f, 2), i(f, 3), i(f, 4), i(f, 5) != 0, i(f, 6) != 0); break;
                case "ufb": map.unflagBlocked(i(f, 1), i(f, 2), i(f, 3), i(f, 4), i(f, 5), i(f, 6) != 0, i(f, 7) != 0); break;
                case "fb": map.flagBoundary(i(f, 1), i(f, 2), i(f, 3), i(f, 4), i(f, 5) != 0, i(f, 6) != 0); break;
                case "ufbd": map.unflagBoundary(i(f, 1), i(f, 2), i(f, 3), i(f, 4), i(f, 5) != 0, i(f, 6) != 0); break;
                case "fd": map.flagBlockedDecoration(i(f, 1), i(f, 2)); break;
                case "ufd": map.unflagFloorDecoration(i(f, 1), i(f, 2)); break;
                case "uw": map.setUnwalkable(i(f, 1), i(f, 2)); break;
                case "ub": map.unblock(i(f, 1), i(f, 2)); break;
                case "af": map.addFlag(i(f, 1), i(f, 2), i(f, 3)); break;
                case "rf": map.removeFlag(i(f, 1), i(f, 2), i(f, 3)); break;
                default: throw new IllegalArgumentException(f[0]);
            }
            return "ok";
        } catch (ArrayIndexOutOfBoundsException error) {
            return "EXC";
        }
    }

    private static int i(String[] f, int index) {
        return Integer.parseInt(f[index]);
    }

    private static String randomOp(Random random, Synth synth) {
        int ox = synth.offsetX;
        int oy = synth.offsetY;
        int w = synth.sizeX;
        int h = synth.sizeY;
        int lowX = ox;
        int highX = ox + w - 1;
        int lowY = oy;
        int highY = oy + h - 1;
        int pick = random.nextInt(100);
        int x = coord(random, lowX, highX, 5);
        int y = coord(random, lowY, highY, 5);
        int sizeX = 1 + random.nextInt(4);
        int sizeY = 1 + random.nextInt(4);
        int rot = random.nextInt(4);
        int blocks = random.nextInt(2);
        int alt = random.nextInt(2);
        if (pick < 20) return "fbo " + x + " " + y + " " + sizeX + " " + sizeY + " " + blocks + " " + alt;
        if (pick < 28) return "ufb " + x + " " + y + " " + sizeX + " " + sizeY + " " + rot + " " + blocks + " " + alt;
        if (pick < 58) {
            int type = chance(random, 90) ? random.nextInt(4) : random.nextInt(23);
            return "fb " + x + " " + y + " " + type + " " + rot + " " + blocks + " " + alt;
        }
        if (pick < 66) {
            int type = chance(random, 90) ? random.nextInt(4) : random.nextInt(23);
            return "ufbd " + x + " " + y + " " + type + " " + rot + " " + blocks + " " + alt;
        }
        if (pick < 74) return "fd " + x + " " + y;
        if (pick < 78) return "ufd " + x + " " + y;
        if (pick < 84) return "uw " + x + " " + y;
        if (pick < 88) return "ub " + x + " " + y;
        if (pick < 94) return "af " + (x - ox) + " " + (y - oy) + " " + (1 << random.nextInt(31));
        return "rf " + (x - ox) + " " + (y - oy) + " " + (1 << random.nextInt(31));
    }

    private static List<Target> buildSynthetics(Random random, StringBuilder text, int count) {
        List<Target> targets = new ArrayList<>();
        int[] offsets = {0, -1, 5};
        for (int n = 0; n < count; n++) {
            Synth synth = new Synth();
            synth.id = "Y" + n;
            synth.clip = n % 2 == 0;
            if (synth.clip) {
                int sx = 20 + random.nextInt(91);
                int sy = 20 + random.nextInt(91);
                synth.map = CollisionMap.createClipMap(sx, sy);
                synth.offsetX = -1;
                synth.offsetY = -1;
                synth.sizeX = sx + 6;
                synth.sizeY = sy + 6;
                text.append("M ").append(synth.id).append(" clip 0 0 ").append(sx).append(' ').append(sy).append('\n');
            } else {
                synth.offsetX = offsets[random.nextInt(3)];
                synth.offsetY = offsets[random.nextInt(3)];
                synth.sizeX = 20 + random.nextInt(91);
                synth.sizeY = 20 + random.nextInt(91);
                synth.map = new CollisionMap();
                synth.map.offsetX = synth.offsetX;
                synth.map.offsetY = synth.offsetY;
                synth.map.sizeX = synth.sizeX;
                synth.map.sizeY = synth.sizeY;
                synth.map.collisionFlags = new int[synth.sizeX][synth.sizeY];
                text.append("M ").append(synth.id).append(" raw ").append(synth.offsetX).append(' ').append(synth.offsetY).append(' ').append(synth.sizeX).append(' ').append(synth.sizeY).append('\n');
            }
            List<String> ops = new ArrayList<>();
            if (synth.clip && chance(random, 40)) ops.add("reset");
            if (synth.clip && chance(random, 80)) ops.add("ubscene " + (synth.sizeX - 6) + " " + (synth.sizeY - 6));
            int total = 150 + random.nextInt(451);
            for (int k = 0; k < total; k++) ops.add(randomOp(random, synth));
            for (String op : ops) {
                String result = runOp(synth.map, op.split(" "));
                text.append("O ").append(synth.id).append(' ').append(op).append(" | ").append(result).append('\n');
            }
            Target target = new Target();
            target.name = synth.id;
            target.map = synth.map;
            target.synthetic = true;
            targets.add(target);
        }
        return targets;
    }

    private static int[] pickSource(Random random, CollisionMap map, int wild) {
        for (int attempt = 0; attempt < 40; attempt++) {
            int x = map.offsetX + random.nextInt(map.sizeX);
            int y = map.offsetY + random.nextInt(map.sizeY);
            int ax = x - map.offsetX;
            int ay = y - map.offsetY;
            int flags = map.collisionFlags[ax][ay];
            if ((flags & 0x240100) == 0 && !chance(random, wild)) return new int[] {x, y};
        }
        return new int[] {map.offsetX - 3 + random.nextInt(map.sizeX + 6), map.offsetY - 3 + random.nextInt(map.sizeY + 6)};
    }

    private static int[] pickNear(Random random, int[] src, int range) {
        int span = chance(random, 12) ? 70 : range;
        return new int[] {src[0] - span + random.nextInt(2 * span + 1), src[1] - span + random.nextInt(2 * span + 1)};
    }

    private static String routeQuery(Random random, Target target, ObjectShapes[] shapes) {
        CollisionMap map = target.map;
        int kind = pickKind(random);
        int[] src = pickSource(random, map, target.synthetic ? 10 : 6);
        int[] dst = pickNear(random, src, 40);
        int size = chance(random, 45) ? 1 : chance(random, 36) ? 2 : 3 + random.nextInt(5);
        int dsx = 1 + random.nextInt(3);
        int dsy = 1 + random.nextInt(3);
        int shape = random.nextInt(23);
        int rot = random.nextInt(4);
        boolean alt = random.nextInt(2) == 1;
        int lenPick = random.nextInt(100);
        int len = lenPick < 60 ? 50 : lenPick < 70 ? 1 : lenPick < 80 ? 3 : 25;
        RouteStrategy strategy;
        switch (kind) {
            case 0: strategy = RouteStrategies.forFixedTile(dst[0], dst[1]); break;
            case 1: strategy = RouteStrategies.forGroundItem(dst[0], dst[1]); break;
            case 2: strategy = RouteStrategies.forPlayer(dst[0], dst[1], dsx, dsy); break;
            case 3: strategy = RouteStrategies.forNpc(dst[0], dst[1], dsx, dsy); break;
            case 4: strategy = RouteStrategies.forWallObject(dst[0], dst[1], dsx, dsy, shapes[shape], rot); break;
            default: strategy = RouteStrategies.forInteractiveObject(dst[0], dst[1], shapes[shape], rot); break;
        }
        int[] stepsX = new int[len];
        int[] stepsY = new int[len];
        String result;
        try {
            result = Integer.toString(RouteFinder.findRoute(src[0], src[1], size, strategy, map, alt, stepsX, stepsY));
        } catch (RuntimeException error) {
            result = "EXC";
        }
        StringBuilder steps = new StringBuilder();
        for (int k = 0; k < len; k++) {
            if (stepsX[k] != 0 || stepsY[k] != 0) {
                if (steps.length() > 0) steps.append(',');
                steps.append(k).append(':').append(stepsX[k]).append(':').append(stepsY[k]);
            }
        }
        if (steps.length() == 0) steps.append('-');
        return String.join("\t", target.name, Integer.toString(kind), Integer.toString(src[0]), Integer.toString(src[1]), Integer.toString(size),
            Integer.toString(dst[0]), Integer.toString(dst[1]), Integer.toString(dsx), Integer.toString(dsy), Integer.toString(shape), Integer.toString(rot),
            alt ? "1" : "0", Integer.toString(len), result, Integer.toString(RouteFinder.exitX), Integer.toString(RouteFinder.exitY), steps.toString());
    }

    private static int pickKind(Random random) {
        int pick = random.nextInt(100);
        if (pick < 10) return 0;
        if (pick < 20) return 1;
        if (pick < 45) return 2;
        if (pick < 65) return 3;
        if (pick < 80) return 4;
        return 5;
    }

    private static int pickFlag(Random random) {
        if (chance(random, 15)) return random.nextInt() & 0x7fffffff;
        return FLAG_CHOICES[random.nextInt(FLAG_CHOICES.length)];
    }

    private static String reachQuery(Random random, Target target) {
        CollisionMap map = target.map;
        int kind = random.nextInt(4);
        int[] src = pickSource(random, map, 5);
        int[] dst = pickNear(random, src, 4);
        int size = 1 + random.nextInt(3);
        int dsx = 1 + random.nextInt(3);
        int dsy = 1 + random.nextInt(3);
        {
            switch (kind) {
                case 0: {
                    int shape = random.nextInt(23);
                    int rot = random.nextInt(4);
                    String value = attempt(() -> map.canReachLoc(src[0], src[1], size, dst[0], dst[1], shape, rot));
                    return String.join("\t", "rl", target.name, Integer.toString(src[0]), Integer.toString(src[1]), Integer.toString(size), Integer.toString(dst[0]), Integer.toString(dst[1]), Integer.toString(shape), Integer.toString(rot), value);
                }
                case 1: {
                    int flag = pickFlag(random);
                    String value = attempt(() -> map.canReachNpc(src[0], src[1], size, dst[0], dst[1], dsx, dsy, flag));
                    return String.join("\t", "rn", target.name, Integer.toString(src[0]), Integer.toString(src[1]), Integer.toString(size), Integer.toString(dst[0]), Integer.toString(dst[1]), Integer.toString(dsx), Integer.toString(dsy), Integer.toString(flag), value);
                }
                case 2: {
                    int flag = pickFlag(random);
                    int ssx = 1 + random.nextInt(3);
                    int ssy = 1 + random.nextInt(3);
                    String value = attempt(() -> map.walkable(src[0], src[1], ssx, ssy, dst[0], dst[1], dsx, dsy, flag));
                    return String.join("\t", "rw", target.name, Integer.toString(src[0]), Integer.toString(src[1]), Integer.toString(ssx), Integer.toString(ssy), Integer.toString(dst[0]), Integer.toString(dst[1]), Integer.toString(dsx), Integer.toString(dsy), Integer.toString(flag), value);
                }
                default: {
                    int ssx = 1 + random.nextInt(4);
                    int ssy = 1 + random.nextInt(4);
                    String value = attempt(() -> CollisionMap.canReach(src[0], src[1], ssx, ssy, dst[0], dst[1], dsx, dsy));
                    return String.join("\t", "rc", "-", Integer.toString(src[0]), Integer.toString(src[1]), Integer.toString(ssx), Integer.toString(ssy), Integer.toString(dst[0]), Integer.toString(dst[1]), Integer.toString(dsx), Integer.toString(dsy), value);
                }
            }
        }
    }

    private static String attempt(java.util.function.BooleanSupplier call) {
        try {
            return Boolean.toString(call.getAsBoolean());
        } catch (RuntimeException error) {
            return "EXC";
        }
    }

    private static Field find(Class<?> type, String name) throws Exception {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static void putInt(Object o, String name, int value) throws Exception {
        UNSAFE.putInt(o, UNSAFE.objectFieldOffset(find(o.getClass(), name)), value);
    }

    private static void putBool(Object o, String name, boolean value) throws Exception {
        UNSAFE.putBoolean(o, UNSAFE.objectFieldOffset(find(o.getClass(), name)), value);
    }

    private static void putObj(Object o, String name, Object value) throws Exception {
        UNSAFE.putObject(o, UNSAFE.objectFieldOffset(find(o.getClass(), name)), value);
    }

    private static int getInt(Object o, String name) throws Exception {
        return UNSAFE.getInt(o, UNSAFE.objectFieldOffset(find(o.getClass(), name)));
    }

    private static Object getObj(Object o, String name) throws Exception {
        return UNSAFE.getObject(o, UNSAFE.objectFieldOffset(find(o.getClass(), name)));
    }

    private static String movementCases(Random random, int cases, int iterations) throws Exception {
        Vector3Pool.initVectorStack();
        StringBuilder out = new StringBuilder();
        int written = 0;
        int skipped = 0;
        int[] moveTypes = {-1, 0, 1, 2, 3};
        BasType bas = PathingEntity.DEFAULT_BASTYPE;
        for (int n = 0; n < cases; n++) {
            int size = chance(random, 70) ? 1 : 1 + random.nextInt(5);
            int stepsLeft = chance(random, 8) ? 0 : 1 + random.nextInt(25);
            int previous = chance(random, 30) ? 0 : random.nextInt(8);
            int stalled = chance(random, 60) ? 0 : random.nextInt(5);
            int tileX = 100 + random.nextInt(3000);
            int tileY = 100 + random.nextInt(3000);
            int[] stepX = new int[25];
            int[] stepY = new int[25];
            int[] stepType = new int[25];
            int cx = tileX + random.nextInt(3) - 1;
            int cy = tileY + random.nextInt(3) - 1;
            for (int k = 24; k >= 0; k--) {
                stepX[k] = cx;
                stepY[k] = cy;
                stepType[k] = moveTypes[random.nextInt(moveTypes.length)];
                cx += random.nextInt(3) - 1;
                cy += random.nextInt(3) - 1;
            }
            int offsetX = 0;
            int offsetY = 0;
            int mode = random.nextInt(100);
            if (mode < 25) {
                offsetX = random.nextInt(2201) - 1100;
                offsetY = random.nextInt(2201) - 1100;
            } else if (mode < 32) {
                offsetX = random.nextInt(20001) - 10000;
                offsetY = random.nextInt(20001) - 10000;
            } else if (mode < 60) {
                offsetX = random.nextInt(65) - 32;
                offsetY = random.nextInt(65) - 32;
            }
            float x = 512f * tileX + size * 256f + offsetX + (chance(random, 15) ? 0.25f * random.nextInt(4) : 0f);
            float y = random.nextInt(200000) - 100000 + 0.5f * random.nextInt(2);
            float z = 512f * tileY + size * 256f + offsetY + (chance(random, 15) ? 0.25f * random.nextInt(4) : 0f);
            boolean delay = chance(random, 60);
            int accumulator = chance(random, 50) ? 0 : random.nextInt(40000);
            int lastYaw = random.nextInt(80001) - 40000;
            int turnPick = random.nextInt(100);
            int turnDirection = turnPick < 30 ? 0 : turnPick < 50 ? 256 : 1 + random.nextInt(1024);
            int faceEntity = chance(random, 20) ? random.nextInt(100) : -1;
            int[] yaw = {random.nextInt(80001) - 40000, chance(random, 50) ? 0 : random.nextInt(601) - 300};
            int[] roll = {random.nextInt(80001) - 40000, chance(random, 50) ? 0 : random.nextInt(601) - 300};
            int[] pitch = {random.nextInt(80001) - 40000, chance(random, 50) ? 0 : random.nextInt(601) - 300};
            int turningTicks = random.nextInt(10);
            boolean cutscene = chance(random, 25);
            int yawAcc = chance(random, 40) ? 0 : 1 + random.nextInt(200);
            int yawMax = random.nextInt(301);
            int rollAcc = chance(random, 50) ? 0 : 1 + random.nextInt(100);
            int rollMax = random.nextInt(201);
            int rollTarget = random.nextInt(4001) - 2000;
            int pitchAcc = chance(random, 50) ? 0 : 1 + random.nextInt(100);
            int pitchMax = random.nextInt(201);
            int pitchTarget = random.nextInt(4001) - 2000;
            int unused = chance(random, 60) ? -1 : 1 + random.nextInt(255);
            boolean hasSeq = chance(random, 50);
            int interrupt = random.nextInt(3);
            int moveType = random.nextInt(3);
            int speed = chance(random, 70) ? 0 : 1 + random.nextInt(5);
            bas.yawAcceleration = yawAcc;
            bas.yawMaxVelocity = yawMax;
            bas.rollAcceleration = rollAcc;
            bas.rollMaxVelocity = rollMax;
            bas.rollTargetAngle = rollTarget;
            bas.pitchAcceleration = pitchAcc;
            bas.pitchMaxVelocity = pitchMax;
            bas.pitchTargetAngle = pitchTarget;
            bas.unused = unused;
            NpcEntity entity = (NpcEntity) UNSAFE.allocateInstance(NpcEntity.class);
            putBool(entity, "calculateAbsolutePosition", true);
            putBool(entity, "calculateNegatedPosition", true);
            putBool(entity, "projectionUpdate", true);
            putObj(entity, "basePosition", new CoordinateSpace());
            putObj(entity, "_absolutePosition", new CoordinateSpace());
            putObj(entity, "_negatedPosition", new CoordinateSpace());
            entity.getBasePosition().coords.x = x;
            entity.getBasePosition().coords.y = y;
            entity.getBasePosition().coords.z = z;
            putObj(entity, "yaw", new EntityTurnDirection());
            putObj(entity, "roll", new EntityTurnDirection());
            putObj(entity, "pitch", new EntityTurnDirection());
            EntityTurnDirection yawObj = (EntityTurnDirection) getObj(entity, "yaw");
            EntityTurnDirection rollObj = (EntityTurnDirection) getObj(entity, "roll");
            EntityTurnDirection pitchObj = (EntityTurnDirection) getObj(entity, "pitch");
            yawObj.rotation = yaw[0];
            yawObj.velocity = yaw[1];
            rollObj.rotation = roll[0];
            rollObj.velocity = roll[1];
            pitchObj.rotation = pitch[0];
            pitchObj.velocity = pitch[1];
            int[] baseX = new int[25];
            int[] baseY = new int[25];
            byte[] types = new byte[25];
            for (int k = 0; k < 25; k++) {
                baseX[k] = stepX[k];
                baseY[k] = stepY[k];
                types[k] = (byte) stepType[k];
            }
            putObj(entity, "movementBaseX", baseX);
            putObj(entity, "movementBaseY", baseY);
            putObj(entity, "movementTypes", types);
            putInt(entity, "movementStepsLeft", stepsLeft);
            putInt(entity, "previousMovementStepsLeft", previous);
            putInt(entity, "stalledStepCount", stalled);
            putInt(entity, "walkSpeedAccumulator", accumulator);
            putInt(entity, "lastYawRotation", lastYaw);
            putInt(entity, "turnDirection", turnDirection);
            putInt(entity, "turningTicks", turningTicks);
            putInt(entity, "faceEntity", faceEntity);
            putInt(entity, "sizeInSquares", size);
            Sequence sequence = (Sequence) UNSAFE.allocateInstance(Sequence.class);
            if (hasSeq) {
                SeqType seqType = (SeqType) UNSAFE.allocateInstance(SeqType.class);
                seqType.interruptType = interrupt;
                seqType.moveType = moveType;
                sequence.type = seqType;
            }
            sequence.speed = speed;
            putObj(entity, "currentSequence", sequence);
            PathingEntitySpotAnimation[] spots = new PathingEntitySpotAnimation[4];
            for (int k = 0; k < 4; k++) {
                spots[k] = (PathingEntitySpotAnimation) UNSAFE.allocateInstance(PathingEntitySpotAnimation.class);
                putInt(spots[k], "spotAnimationId", -1);
            }
            putObj(entity, "spotAnimations", spots);
            entity.currRenderAnim = -1;
            entity.faceX = -1;
            entity.faceY = -1;
            NPCType definitions = (NPCType) UNSAFE.allocateInstance(NPCType.class);
            definitions.delayMovement = delay;
            definitions.npcBasId = -1;
            definitions.alternativeIds = null;
            entity.definitions = definitions;
            StringBuilder line = new StringBuilder();
            line.append(n).append('\t').append(Float.floatToRawIntBits(x)).append('\t').append(Float.floatToRawIntBits(y)).append('\t').append(Float.floatToRawIntBits(z));
            int[] header = {size, delay ? 1 : 0, stepsLeft, previous, stalled, accumulator, lastYaw, turnDirection, faceEntity, yaw[0], yaw[1], roll[0], roll[1], pitch[0], pitch[1], turningTicks, cutscene ? 1 : 0,
                yawAcc, yawMax, rollAcc, rollMax, rollTarget, pitchAcc, pitchMax, pitchTarget, unused, hasSeq ? 1 : 0, interrupt, moveType, speed};
            for (int value : header) line.append('\t').append(value);
            for (int k = 0; k < 25; k++) line.append('\t').append(stepX[k]).append('\t').append(stepY[k]).append('\t').append(stepType[k]);
            StringBuilder snaps = new StringBuilder();
            boolean failed = false;
            for (int step = 0; step < iterations && !failed; step++) {
                try {
                    EntityUpdating.updatePosition(entity, cutscene);
                    int mt = EntityUpdating.currentMovementType;
                    int tdi = EntityUpdating.turnDirectionIndex;
                    int turned = -999;
                    if (getInt(entity, "faceEntity") == -1) turned = EntityUpdating.turnTo(entity);
                    float ex = entity.getBasePosition().coords.x;
                    float ey = entity.getBasePosition().coords.y;
                    float ez = entity.getBasePosition().coords.z;
                    int[] snap = {mt, tdi, turned, Float.floatToRawIntBits(ex), Float.floatToRawIntBits(ey), Float.floatToRawIntBits(ez), getInt(entity, "movementStepsLeft"),
                        getInt(entity, "previousMovementStepsLeft"), getInt(entity, "stalledStepCount"), getInt(entity, "walkSpeedAccumulator"), getInt(entity, "lastYawRotation"),
                        yawObj.rotation, yawObj.velocity, rollObj.rotation, rollObj.velocity, pitchObj.rotation, pitchObj.velocity, getInt(entity, "turningTicks")};
                    if (step > 0) snaps.append(';');
                    for (int k = 0; k < snap.length; k++) {
                        if (k > 0) snaps.append(',');
                        snaps.append(snap[k]);
                    }
                } catch (Throwable error) {
                    failed = true;
                }
            }
            if (failed) {
                skipped++;
                continue;
            }
            line.append('\t').append(snaps);
            out.append(line).append('\n');
            written++;
        }
        System.err.println("movement cases written=" + written + " skipped=" + skipped);
        return out.toString();
    }

    public static void main(String[] args) throws Exception {
        Path jar = Path.of(args[0]).toRealPath();
        Path work = Path.of(args[1]);
        String mode = args[2];
        String before = hash(jar);
        if (!before.equals(PINNED)) throw new IllegalArgumentException("Unexpected oracle JAR");
        checkSource(jar, Packet.class);
        checkSource(jar, MapLoader.class);
        checkSource(jar, CollisionMap.class);
        checkSource(jar, RouteFinder.class);
        checkSource(jar, EntityUpdating.class);
        checkSource(jar, ObjectType.class);
        Method decode = null;
        Method post = null;
        for (Method method : ObjectType.class.getDeclaredMethods()) {
            if (method.getName().startsWith("decodeType")) decode = method;
            if (method.getName().startsWith("postDecode")) post = method;
        }
        decode.setAccessible(true);
        post.setAccessible(true);
        boolean full = mode.equals("full");
        Map<Integer, byte[]> bundle = full ? readBundle(work.resolve("defs.bundle")) : null;
        ObjectTypeList list = typeList();
        Map<Integer, ObjectType> cache = new HashMap<>();
        TreeSet<Integer> used = new TreeSet<>();
        List<World> worlds = readWorlds(work.resolve("scenes.txt"));
        StringBuilder summary = new StringBuilder();
        for (World world : worlds) {
            int[] counts = buildWorld(world, work, bundle, used, decode, post, list, cache);
            summary.append(world.name).append(" placements=").append(counts[0]).append(" applied=").append(counts[1]).append(' ');
            if (full) Files.write(work.resolve("native-scene-" + world.name + ".flags"), flagBytes(world.maps));
        }
        if (!full) {
            StringBuilder ids = new StringBuilder();
            for (int id : used) ids.append(id).append('\n');
            Files.writeString(work.resolve("used-ids.txt"), ids.toString(), StandardCharsets.US_ASCII);
        } else {
            Random random = new Random(SEED);
            ObjectShapes[] shapes = new ObjectShapes[23];
            for (ObjectShapes value : ObjectShapes.values()) {
                if (value.shape >= 0 && value.shape < 23 && shapes[value.shape] == null) shapes[value.shape] = value;
            }
            StringBuilder synthText = new StringBuilder();
            List<Target> synthetic = buildSynthetics(random, synthText, 40);
            Files.writeString(work.resolve("native-synth.txt"), synthText.toString(), StandardCharsets.US_ASCII);
            CollisionMap[] synthMaps = new CollisionMap[synthetic.size()];
            for (int k = 0; k < synthMaps.length; k++) synthMaps[k] = synthetic.get(k).map;
            Files.write(work.resolve("native-synth.flags"), flagBytes(synthMaps));
            List<Target> real = new ArrayList<>();
            for (World world : worlds) {
                for (int plane = 0; plane < 4; plane++) {
                    Target target = new Target();
                    target.name = world.name + ":" + plane;
                    target.map = world.maps[plane];
                    real.add(target);
                }
            }
            StringBuilder routes = new StringBuilder();
            int realQueries = 3600;
            int syntheticQueries = 2400;
            for (int k = 0; k < realQueries; k++) routes.append(routeQuery(random, real.get(random.nextInt(real.size())), shapes)).append('\n');
            for (int k = 0; k < syntheticQueries; k++) routes.append(routeQuery(random, synthetic.get(random.nextInt(synthetic.size())), shapes)).append('\n');
            Files.writeString(work.resolve("native-routes.tsv"), routes.toString(), StandardCharsets.US_ASCII);
            List<Target> all = new ArrayList<>(real);
            all.addAll(synthetic);
            StringBuilder reach = new StringBuilder();
            for (int k = 0; k < 4000; k++) reach.append(reachQuery(random, all.get(random.nextInt(all.size())))).append('\n');
            Files.writeString(work.resolve("native-reach.tsv"), reach.toString(), StandardCharsets.US_ASCII);
            Files.writeString(work.resolve("native-movement.tsv"), movementCases(random, 1000, 8), StandardCharsets.US_ASCII);
        }
        if (!before.equals(hash(jar))) throw new IllegalStateException("Oracle JAR changed");
        System.out.println("COLLISION_ORACLE ok " + mode + " " + summary.toString().trim());
    }
}
