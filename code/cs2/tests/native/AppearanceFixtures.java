import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.identitykit.IdkType;
import com.jagex.game.runetek5.config.identitykit.IdkTypeList;
import com.jagex.game.runetek5.config.item.ItemType;
import com.jagex.game.runetek5.config.item.ItemTypeCustomisation;
import com.jagex.game.runetek5.config.item.ItemTypeList;
import com.jagex.game.runetek5.config.rsinterface.Component;
import com.jagex.game.runetek5.config.rsinterface.Interface;
import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.*;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerAppearance;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class AppearanceFixtures {
    static final long SEED = 0x727a9e4aL;
    static final int[] EDGES = {0, 1, -1, 2, 255, 65535, 0x40000000, Integer.MAX_VALUE, Integer.MIN_VALUE, -2, 1000, 0x3fffffff};
    static final int[] ITEMS = {0, 1, 2, 3, 100, 4151, 65535, 12345};
    static final int[] KITS = {0, 1, 2, 3, 7, 50, 255, 1000, 65535};
    static final int[] PARTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, -1, 16, 99, Integer.MIN_VALUE};
    static final ClientScriptOpCode[] OPS = {
        ClientScriptOpCode.SETOBJ, ClientScriptOpCode.BASEIDKIT, ClientScriptOpCode.BASECOLOUR, ClientScriptOpCode.SETGENDER
    };
    static sun.misc.Unsafe unsafe;

    static Field field(Class<?> type, String name) throws Exception {
        for (var c = type; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(name);
    }

    static long offset(Object target, String name) throws Exception {
        return unsafe.objectFieldOffset(field(target.getClass(), name));
    }

    static void put(Object target, String name, Object value) throws Exception {
        unsafe.putObject(target, offset(target, name), value);
    }

    static Object get(Object target, String name) throws Exception {
        return unsafe.getObject(target, offset(target, name));
    }

    static void putInt(Object target, String name, int value) throws Exception {
        unsafe.putInt(target, offset(target, name), value);
    }

    static int getInt(Object target, String name) throws Exception {
        return unsafe.getInt(target, offset(target, name));
    }

    static void putLong(Object target, String name, long value) throws Exception {
        unsafe.putLong(target, offset(target, name), value);
    }

    static long getLong(Object target, String name) throws Exception {
        return unsafe.getLong(target, offset(target, name));
    }

    static void putBoolean(Object target, String name, boolean value) throws Exception {
        unsafe.putBoolean(target, offset(target, name), value);
    }

    static boolean getBoolean(Object target, String name) throws Exception {
        return unsafe.getBoolean(target, offset(target, name));
    }

    static String ints(int[] values) {
        if (values == null) return "-";
        if (values.length == 0) return "e";
        var sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(values[i]);
        }
        return sb.toString();
    }

    static String shorts(short[] values) {
        if (values == null) return "-";
        if (values.length == 0) return "e";
        var sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(values[i]);
        }
        return sb.toString();
    }

    static String stack(int[] values, int pointer) {
        if (pointer <= 0) return "-";
        return ints(Arrays.copyOf(values, pointer));
    }

    static int[] randomInts(Random random, int length) {
        var values = new int[length];
        for (int i = 0; i < length; i++) values[i] = wide(random);
        return values;
    }

    static short[] randomShorts(Random random) {
        int kind = random.nextInt(4);
        if (kind == 0) return null;
        var values = new short[kind == 1 ? 0 : 1 + random.nextInt(4)];
        for (int i = 0; i < values.length; i++) values[i] = (short) (random.nextInt(5) == 0 ? EDGES[random.nextInt(EDGES.length)] : random.nextInt(70000) - 2000);
        return values;
    }

    static int wide(Random random) {
        return random.nextInt(4) == 0 ? EDGES[random.nextInt(EDGES.length)] : random.nextInt(300) - 20;
    }

    static String modifiers(Object[] mods) {
        if (mods == null) return "-";
        if (mods.length == 0) return "e";
        var sb = new StringBuilder();
        for (int i = 0; i < mods.length; i++) {
            if (i > 0) sb.append(';');
            if (mods[i] == null) {
                sb.append('-');
                continue;
            }
            try {
                var m = mods[i];
                sb.append(ints((int[]) get(m, "maleBody"))).append('/').append(ints((int[]) get(m, "femaleBody"))).append('/')
                    .append(ints((int[]) get(m, "maleHead"))).append('/').append(ints((int[]) get(m, "femaleHead"))).append('/')
                    .append(shorts((short[]) get(m, "recolDst"))).append('/').append(shorts((short[]) get(m, "retexDst")));
            } catch (Exception error) {
                throw new RuntimeException(error);
            }
        }
        return sb.toString();
    }

    static String state(PlayerEntity player) throws Exception {
        var appearance = (PlayerAppearance) get(player, "playerAppearance");
        if (appearance == null) return "-\t-\t-\t-\t-";
        return ints((int[]) get(appearance, "appearance")) + "\t" + ints((int[]) get(appearance, "recolorDstIndices")) + "\t"
            + (getBoolean(appearance, "female") ? 1 : 0) + "\t" + getLong(appearance, "playerAppearanceHash") + "\t" + getInt(appearance, "basId");
    }

    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        var theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        unsafe = (sun.misc.Unsafe) theUnsafe.get(null);
        var out = new StringBuilder();
        var random = new Random(SEED);

        var idkList = (IdkTypeList) unsafe.allocateInstance(IdkTypeList.class);
        put(idkList, "cache", new LRUCache(64));
        for (int id : KITS) idkList.cache.put(new IdkType(), (long) id);
        TypeLists.IDK = idkList;
        var itemList = (ItemTypeList) unsafe.allocateInstance(ItemTypeList.class);
        put(itemList, "typeCache", new LRUCache(64));
        for (int id : ITEMS) itemList.typeCache.put((ItemType) unsafe.allocateInstance(ItemType.class), (long) id);
        TypeLists.OBJ = itemList;

        int total = 0;
        for (int n = 0; n < 2400; n++) {
            var op = OPS[n % OPS.length];
            var e = new CS2Executor();
            e.instrPtr = 0;
            Arrays.fill(e.intStack, 0x5a5a5a5a);
            boolean hasMine = random.nextInt(12) != 0;
            boolean hasAppearance = random.nextInt(8) != 0;
            PlayerEntity mine = null;
            if (hasMine) {
                mine = (PlayerEntity) unsafe.allocateInstance(PlayerEntity.class);
                if (hasAppearance) {
                    var appearance = new PlayerAppearance();
                    int lengthKind = random.nextInt(12);
                    int[] parts = lengthKind == 0 ? null : randomInts(random, lengthKind == 1 ? 0 : lengthKind == 2 ? 3 : lengthKind == 3 ? 12 : lengthKind == 4 ? 20 : 16);
                    if (parts != null && random.nextBoolean()) for (int i = 0; i < parts.length; i++) if (random.nextBoolean()) parts[i] = random.nextBoolean() ? 0 : (random.nextInt(9000) | 0x40000000);
                    int recolorKind = random.nextInt(10);
                    int[] recolor = recolorKind == 0 ? null : randomInts(random, recolorKind == 1 ? 0 : recolorKind == 2 ? 5 : recolorKind == 3 ? 9 : recolorKind == 4 ? 12 : 10);
                    Object[] mods = null;
                    int modKind = random.nextInt(5);
                    if (modKind > 0) {
                        mods = new Object[modKind == 1 ? 0 : modKind];
                        for (int i = 0; i < mods.length; i++) {
                            if (random.nextInt(4) == 0) continue;
                            var m = (ItemTypeCustomisation) unsafe.allocateInstance(ItemTypeCustomisation.class);
                            put(m, "maleBody", randomInts(random, 3));
                            put(m, "femaleBody", randomInts(random, 3));
                            put(m, "maleHead", randomInts(random, 2));
                            put(m, "femaleHead", randomInts(random, 2));
                            put(m, "recolDst", randomShorts(random));
                            put(m, "retexDst", randomShorts(random));
                            mods[i] = m;
                        }
                        var typed = new ItemTypeCustomisation[mods.length];
                        for (int i = 0; i < mods.length; i++) typed[i] = (ItemTypeCustomisation) mods[i];
                        put(appearance, "meshModifiers", typed);
                    }
                    put(appearance, "appearance", parts);
                    put(appearance, "recolorDstIndices", recolor);
                    putBoolean(appearance, "female", random.nextBoolean());
                    putInt(appearance, "basId", wide(random));
                    putLong(appearance, "playerAppearanceHash", random.nextLong());
                    put(mine, "playerAppearance", appearance);
                }
            }
            PlayerUpdate.MY_PLAYER = mine;
            String before = mine == null ? "-\t-\t-\t-\t-" : state(mine);
            String modsBefore = "-";
            if (mine != null && get(mine, "playerAppearance") != null) modsBefore = modifiers((Object[]) get(get(mine, "playerAppearance"), "meshModifiers"));
            int[] arguments;
            if (op == ClientScriptOpCode.SETOBJ) {
                int slot = random.nextInt(3) == 0 ? random.nextInt(24) - 4 : random.nextInt(16);
                int item = random.nextInt(4) == 0 ? -1 : ITEMS[random.nextInt(ITEMS.length)];
                arguments = new int[] {slot, item};
            } else if (op == ClientScriptOpCode.BASEIDKIT) {
                int part = PARTS[random.nextInt(PARTS.length)];
                arguments = new int[] {part, KITS[random.nextInt(KITS.length)]};
            } else if (op == ClientScriptOpCode.BASECOLOUR) {
                arguments = new int[] {random.nextInt(3) == 0 ? random.nextInt(20) - 5 : random.nextInt(10), wide(random)};
            } else {
                arguments = new int[] {wide(random)};
            }
            int supplied = random.nextInt(25) == 0 ? random.nextInt(arguments.length) : arguments.length;
            int skip = arguments.length - supplied;
            for (int i = 0; i < supplied; i++) e.intStack[i] = arguments[skip + i];
            e.intStackPtr = supplied;
            String result = "ok";
            try {
                ComponentLifecycleOps.dispatch(op, e);
            } catch (Throwable error) {
                result = "error:" + error.getClass().getSimpleName();
            }
            String after = mine == null ? "-\t-\t-\t-\t-" : state(mine);
            out.append("C\t").append(op.name()).append('\t').append(hasMine ? 1 : 0).append('\t').append(before).append('\t').append(modsBefore)
                .append('\t').append(stack(Arrays.copyOfRange(arguments, skip, arguments.length), supplied))
                .append('\t').append(result).append('\t').append(stack(e.intStack, e.intStackPtr)).append('\t').append(after).append('\n');
            total++;
        }

        int[][] layouts = {
            {1 << 16, 1 << 16 | 1, 1 << 16 | 2, 1 << 16 | 3},
            {2 << 16, -1, 2 << 16 | 2},
            {-1, 3 << 16 | 1},
            {}
        };
        var interfaces = new Interface[layouts.length + 1];
        for (int i = 0; i < layouts.length; i++) {
            var components = new Component[layouts[i].length];
            for (int c = 0; c < components.length; c++) {
                if (layouts[i][c] == -1) continue;
                components[c] = (Component) unsafe.allocateInstance(Component.class);
                putInt(components[c], "idHash", layouts[i][c]);
            }
            interfaces[i + 1] = new Interface(false, components);
            var sb = new StringBuilder();
            for (int c = 0; c < layouts[i].length; c++) {
                if (c > 0) sb.append(',');
                sb.append(layouts[i][c]);
            }
            out.append("I\t").append(i + 1).append('\t').append(layouts[i].length == 0 ? "e" : sb.toString()).append('\n');
        }
        int[] pool = {0, 1, 2, 3, 4, -1, 1 << 16, 1 << 16 | 1, 1 << 16 | 3, 1 << 16 | 4, 1 << 16 | 65535, 2 << 16, 2 << 16 | 1, 2 << 16 | 2, 2 << 16 | 3,
            3 << 16, 3 << 16 | 1, 4 << 16, 5 << 16, 1 << 16 | 0xffff, Integer.MIN_VALUE, Integer.MAX_VALUE, -65536, 7 << 16 | 1, 1 << 16 | 2};
        int visTotal = 0;
        for (int n = 0; n < 900; n++) {
            var e = new CS2Executor();
            e.instrPtr = 0;
            Arrays.fill(e.intStack, 0x5a5a5a5a);
            boolean secondary = random.nextBoolean();
            e.hasIntOperand = secondary;
            String entity;
            int kind = random.nextInt(20);
            PlayerEntity player = null;
            int bound = 0;
            if (kind < 15) {
                int id = 1 + random.nextInt(layouts.length);
                bound = id;
                player = (PlayerEntity) unsafe.allocateInstance(PlayerEntity.class);
                put(player, "inter", interfaces[id]);
                entity = Integer.toString(id);
            } else if (kind < 18) {
                player = (PlayerEntity) unsafe.allocateInstance(PlayerEntity.class);
                put(player, "inter", null);
                entity = "u";
            } else {
                entity = "-";
            }
            e.currentEntity = player;
            String seeded = "-";
            for (var hook : new CS2Interface[] {e.hookedInterface1, e.hookedInterface2}) hook.reset();
            if (random.nextInt(3) == 0) {
                int id = 1 + random.nextInt(2);
                int index = random.nextInt(2);
                if (interfaces[id].components[index] == null) index = 0;
                var hook = secondary == (random.nextInt(4) != 0) ? e.hookedInterface2 : e.hookedInterface1;
                hook.inter = interfaces[id];
                hook.comp = interfaces[id].components[index];
                seeded = (hook == e.hookedInterface2 ? "2" : "1") + ":" + id + ":" + index;
            }
            int component = pool[random.nextInt(pool.length)];
            if (bound != 0 && random.nextBoolean()) component = bound << 16 | random.nextInt(5);
            int supplied = random.nextInt(30) == 0 ? 0 : 1;
            if (supplied == 1) e.intStack[0] = component;
            e.intStackPtr = supplied;
            String result = "ok";
            try {
                DetailCapabilityOps.dispatch(ClientScriptOpCode.GET_NPC_VISLEVEL, e);
            } catch (Throwable error) {
                result = "error:" + error.getClass().getSimpleName();
            }
            out.append("V\t").append(secondary ? 1 : 0).append('\t').append(entity).append('\t').append(supplied == 1 ? component : "-").append('\t').append(seeded)
                .append('\t').append(result).append('\t').append(stack(e.intStack, e.intStackPtr)).append('\t').append(hookText(e.hookedInterface1, interfaces))
                .append('\t').append(hookText(e.hookedInterface2, interfaces)).append('\n');
            visTotal++;
        }
        out.append("Z\t").append(total).append('\t').append(visTotal).append('\n');
        Files.writeString(output.resolve("appearance.native.tsv"), out.toString(), StandardCharsets.UTF_8);
        System.exit(0);
    }

    static String hookText(CS2Interface hook, Interface[] interfaces) throws Exception {
        if (hook.comp == null) return "-";
        int id = -1;
        for (int i = 1; i < interfaces.length; i++) if (interfaces[i] == hook.inter) id = i;
        return id + ":" + getInt(hook.comp, "idHash");
    }
}
