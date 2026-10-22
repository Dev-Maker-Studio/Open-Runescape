import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.*;
import com.jagex.game.runetek5.entity.BoundingBoxPolygon;
import com.jagex.game.runetek5.entity.gameobject.AnimatedGameObject;
import com.jagex.game.runetek5.entity.gameobject.AnimatedInteractiveObject;
import com.jagex.game.runetek5.entity.gameobject.AnimatedWall;
import com.jagex.game.runetek5.entity.gameobject.AnimatedWallDecoration;
import com.jagex.game.runetek5.entity.gameobject.StationaryGroundDecoration;
import com.jagex.game.runetek5.entity.gameobject.StationaryInteractiveObject;
import com.jagex.game.runetek5.entity.gameobject.StationaryWall;
import com.jagex.game.runetek5.entity.gameobject.StationaryWallDecoration;
import com.jagex.game.runetek5.entity.grounditem.GroundItem;
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity;
import com.jagex.game.runetek5.graphics.model.ModelSM;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class BoundsFixtures {
    static final long SEED = 0x727c5229L;
    static final int PER_OP = 2500;
    static final int[] EDGES = {0, 1, -1, 2, -2, 100, -100, 512, -512, 32767, -32768, 32768, -32769, 65535, 65536, 1 << 20, -(1 << 20), 1 << 30, -(1 << 30), Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE - 1, Integer.MIN_VALUE + 1};
    static final String[] OPS = {"GET_ENTITY_BOUNDING_BOX", "GET_LOC_BOUNDING_BOX", "GET_OBJ_BOUNDING_BOX", "GET_LOC_OVERLAY_HEIGHT"};
    static sun.misc.Unsafe unsafe;

    static void putInt(Object target, String name, int value) throws Exception {
        unsafe.putInt(target, unsafe.objectFieldOffset(SceneFixtures.field(target.getClass(), name)), value);
    }

    static int getInt(Object target, String name) throws Exception {
        return unsafe.getInt(target, unsafe.objectFieldOffset(SceneFixtures.field(target.getClass(), name)));
    }

    static void putShort(Object target, String name, short value) throws Exception {
        unsafe.putShort(target, unsafe.objectFieldOffset(SceneFixtures.field(target.getClass(), name)), value);
    }

    static short getShort(Object target, String name) throws Exception {
        return unsafe.getShort(target, unsafe.objectFieldOffset(SceneFixtures.field(target.getClass(), name)));
    }

    static void putBool(Object target, String name, boolean value) throws Exception {
        unsafe.putBoolean(target, unsafe.objectFieldOffset(SceneFixtures.field(target.getClass(), name)), value);
    }

    static boolean getBool(Object target, String name) throws Exception {
        return unsafe.getBoolean(target, unsafe.objectFieldOffset(SceneFixtures.field(target.getClass(), name)));
    }

    static void putObj(Object target, String name, Object value) throws Exception {
        unsafe.putObject(target, unsafe.objectFieldOffset(SceneFixtures.field(target.getClass(), name)), value);
    }

    static Object getObj(Object target, String name) throws Exception {
        return unsafe.getObject(target, unsafe.objectFieldOffset(SceneFixtures.field(target.getClass(), name)));
    }

    static int value(Random random) {
        return switch (random.nextInt(5)) {
            case 0 -> EDGES[random.nextInt(EDGES.length)];
            case 1 -> random.nextInt();
            default -> random.nextInt(4000) - 2000;
        };
    }

    static int sizeValue(Random random) {
        return switch (random.nextInt(6)) {
            case 0 -> EDGES[random.nextInt(EDGES.length)];
            case 1 -> random.nextInt(2000) - 200;
            default -> random.nextInt(60);
        };
    }

    static Object randomBoxes(Random random) throws Exception {
        if (random.nextInt(15) == 0) return null;
        int count = random.nextInt(5) == 0 ? 0 : 1 + random.nextInt(4);
        var array = (Object[]) Array.newInstance(BoundingBoxPolygon.class, count);
        for (int i = 0; i < count; i++) {
            if (random.nextInt(60) == 0) continue;
            var box = (BoundingBoxPolygon) unsafe.allocateInstance(BoundingBoxPolygon.class);
            boolean near = random.nextInt(3) != 0;
            putBool(box, "positionCalculated", random.nextInt(5) != 0);
            putInt(box, "left", near ? random.nextInt(800) - 400 : value(random));
            putInt(box, "right", near ? random.nextInt(800) - 400 : value(random));
            putInt(box, "top", near ? random.nextInt(800) - 400 : value(random));
            putInt(box, "bottom", near ? random.nextInt(800) - 400 : value(random));
            putInt(box, "size", sizeValue(random));
            array[i] = box;
        }
        return array;
    }

    static String boxText(Object entity) throws Exception {
        if (entity == null) return "B|n";
        var array = (Object[]) getObj(entity, "boundingBoxPolygons");
        if (array == null) return "B|y|n";
        if (array.length == 0) return "B|y|e";
        var sb = new StringBuilder("B|y|");
        for (int i = 0; i < array.length; i++) {
            if (i > 0) sb.append(';');
            var box = array[i];
            if (box == null) {
                sb.append('x');
                continue;
            }
            sb.append(getBool(box, "positionCalculated") ? 1 : 0).append(',')
                .append(getInt(box, "left")).append(',')
                .append(getInt(box, "right")).append(',')
                .append(getInt(box, "top")).append(',')
                .append(getInt(box, "bottom")).append(',')
                .append(getInt(box, "size"));
        }
        return sb.toString();
    }

    static int[] randomColumn(Random random, int count, boolean y) {
        int roll = random.nextInt(20);
        if (roll == 0) return null;
        int length = count;
        if (roll == 1) length = Math.max(0, count - 1 - random.nextInt(3));
        else if (roll == 2) length = count + random.nextInt(4);
        else if (roll == 3) length = 0;
        var out = new int[Math.max(0, length)];
        for (int i = 0; i < out.length; i++) {
            if (y) {
                int mode = random.nextInt(8);
                out[i] = mode == 0 ? EDGES[random.nextInt(EDGES.length)] : mode == 1 ? random.nextInt() : random.nextInt(3000) - 1500;
            } else {
                out[i] = random.nextInt(2000) - 1000;
            }
        }
        return out;
    }

    static ModelSM randomModel(Random random) throws Exception {
        var model = (ModelSM) unsafe.allocateInstance(ModelSM.class);
        int count = random.nextInt(8) == 0 ? EDGES[random.nextInt(EDGES.length)] % 40 : random.nextInt(14);
        if (random.nextInt(25) == 0) count = Integer.MIN_VALUE;
        if (random.nextInt(40) == 0) count = Integer.MAX_VALUE;
        putInt(model, "maxDepth", count);
        int bounded = count < 0 || count > 64 ? random.nextInt(14) : count;
        putObj(model, "verticesX", randomColumn(random, bounded, false));
        putObj(model, "verticesY", randomColumn(random, bounded, true));
        putObj(model, "verticesZ", randomColumn(random, bounded, false));
        boolean updated = random.nextInt(4) == 0;
        putBool(model, "boundsUpdated", updated);
        short minY = (short) (random.nextInt(3) == 0 ? random.nextInt(65536) - 32768 : random.nextInt(600) - 300);
        putShort(model, "minY", minY);
        return model;
    }

    static String columnText(Object column) {
        if (column == null) return "n";
        var array = (int[]) column;
        if (array.length == 0) return "e";
        var sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(array[i]);
        }
        return sb.toString();
    }

    static String modelText(Object model) throws Exception {
        return (getBool(model, "boundsUpdated") ? 1 : 0) + ":" + getShort(model, "minY") + ":" + getInt(model, "maxDepth")
            + ":" + columnText(getObj(model, "verticesX")) + ":" + columnText(getObj(model, "verticesY")) + ":" + columnText(getObj(model, "verticesZ"));
    }

    static Object heightTarget;
    static String heightKind;

    static Object makeHeightTarget(Random random) throws Exception {
        int kind = random.nextInt(7);
        if (kind < 4) {
            Object entity;
            String field;
            switch (kind) {
                case 0 -> {
                    entity = unsafe.allocateInstance(StationaryGroundDecoration.class);
                    field = "model";
                }
                case 1 -> {
                    entity = unsafe.allocateInstance(StationaryWall.class);
                    field = "model";
                }
                case 2 -> {
                    entity = unsafe.allocateInstance(StationaryWallDecoration.class);
                    field = "model";
                }
                default -> {
                    entity = unsafe.allocateInstance(StationaryInteractiveObject.class);
                    field = "interactiveModel";
                }
            }
            heightKind = field;
            if (random.nextInt(8) != 0) putObj(entity, field, randomModel(random));
            return entity;
        }
        var animated = (AnimatedGameObject) unsafe.allocateInstance(AnimatedGameObject.class);
        int min = switch (random.nextInt(4)) {
            case 0 -> EDGES[random.nextInt(EDGES.length)];
            case 1 -> random.nextInt();
            default -> random.nextInt(2000) - 1000;
        };
        putInt(animated, "modelMinY", min);
        Object entity;
        switch (kind) {
            case 4 -> {
                entity = unsafe.allocateInstance(AnimatedWall.class);
                putObj(entity, "gameObject", animated);
                heightKind = "gameObject";
            }
            case 5 -> {
                entity = unsafe.allocateInstance(AnimatedWallDecoration.class);
                putObj(entity, "wallDecoration", animated);
                heightKind = "wallDecoration";
            }
            default -> {
                entity = unsafe.allocateInstance(AnimatedInteractiveObject.class);
                putObj(entity, "activeGameObject", animated);
                heightKind = "activeGameObject";
            }
        }
        return entity;
    }

    static String heightText(Object entity) throws Exception {
        if (entity == null) return "H|n";
        if (heightKind.equals("model") || heightKind.equals("interactiveModel")) {
            var model = getObj(entity, heightKind);
            return model == null ? "H|m|n" : "H|m|" + modelText(model);
        }
        return "H|a|" + getInt(getObj(entity, heightKind), "modelMinY");
    }

    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        var theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        unsafe = (sun.misc.Unsafe) theUnsafe.get(null);
        SceneFixtures.unsafe = unsafe;
        var random = new Random(SEED);
        var out = new StringBuilder();
        int total = 0;
        var stats = new TreeMap<String, int[]>();
        for (var name : OPS) {
            var op = ClientScriptOpCode.valueOf(name);
            for (int n = 0; n < PER_OP; n++) {
                var e = new CS2Executor();
                Arrays.fill(e.intStack, 0x5a5a5a5a);
                e.intStackPtr = 0;
                e.intOpValues = new int[] {0};
                e.instrPtr = 0;
                Object subject = null;
                boolean absent = random.nextInt(20) == 0;
                switch (name) {
                    case "GET_ENTITY_BOUNDING_BOX" -> {
                        if (!absent) {
                            var npc = (NpcEntity) unsafe.allocateInstance(NpcEntity.class);
                            putObj(npc, "boundingBoxPolygons", randomBoxes(random));
                            e.currentEntity = npc;
                            subject = npc;
                        }
                    }
                    case "GET_LOC_BOUNDING_BOX" -> {
                        if (!absent) {
                            var loc = random.nextBoolean() ? unsafe.allocateInstance(StationaryWall.class) : unsafe.allocateInstance(StationaryGroundDecoration.class);
                            putObj(loc, "boundingBoxPolygons", randomBoxes(random));
                            e.currentLocation = (com.jagex.game.runetek5.entity.gameobject.GameObject) loc;
                            subject = loc;
                        }
                    }
                    case "GET_OBJ_BOUNDING_BOX" -> {
                        if (!absent) {
                            var item = (GroundItem) unsafe.allocateInstance(GroundItem.class);
                            putObj(item, "boundingBoxPolygons", randomBoxes(random));
                            e.currentGroundItem = item;
                            subject = item;
                        }
                    }
                    default -> {
                        if (!absent) {
                            subject = makeHeightTarget(random);
                            e.currentLocation = (com.jagex.game.runetek5.entity.gameobject.GameObject) subject;
                        }
                    }
                }
                boolean heights = name.equals("GET_LOC_OVERLAY_HEIGHT");
                var before = heights ? heightText(subject) : boxText(subject);
                String result = "ok";
                try {
                    DetailCapabilityOps.dispatch(op, e);
                } catch (Throwable error) {
                    result = "error:" + error.getClass().getSimpleName();
                }
                var after = heights ? heightText(subject) : boxText(subject);
                out.append("C\t").append(name).append("\t-\t").append(before).append('\t').append(result).append('\t')
                    .append(result.equals("ok") ? SceneFixtures.stack(e.intStack, e.intStackPtr) : "-").append('\t').append(after).append('\n');
                stats.computeIfAbsent(name, k -> new int[2])[result.equals("ok") ? 0 : 1]++;
                total++;
            }
        }
        out.append("Z\t").append(total).append('\n');
        Files.writeString(output.resolve("bounds.native.tsv"), out.toString(), StandardCharsets.UTF_8);
        var summary = new StringBuilder();
        for (var entry : stats.entrySet()) summary.append(entry.getKey()).append('\t').append(entry.getValue()[0]).append('\t').append(entry.getValue()[1]).append('\n');
        Files.writeString(output.resolve("bounds.stats.tsv"), summary.toString(), StandardCharsets.UTF_8);
        System.exit(0);
    }
}
