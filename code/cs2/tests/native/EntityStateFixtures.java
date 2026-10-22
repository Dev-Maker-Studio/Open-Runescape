import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.client.Client;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.bas.BasType;
import com.jagex.game.runetek5.config.bas.BasTypeList;
import com.jagex.game.runetek5.config.npc.NPCType;
import com.jagex.game.runetek5.config.npc.NpcTypeList;
import com.jagex.game.runetek5.config.rsinterface.Interface;
import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.*;
import com.jagex.game.runetek5.config.varp.*;
import com.jagex.game.runetek5.entity.grounditem.GroundItem;
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity;
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.graphics.hint.HintDetails;
import com.jagex.game.runetek5.interaction.Friend;
import com.jagex.game.runetek5.interaction.FriendList;
import com.jagex.game.runetek5.interaction.chat.ChatBox;
import com.jagex.game.runetek5.movement.CoordinateSpace;
import com.jagex.game.runetek5.vardomain.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class EntityStateFixtures {
    static final long SEED = 0x727c5228L;
    static final int[] EDGES = {0, 1, -1, 2, 5, 31, 255, 65535, Integer.MAX_VALUE, Integer.MIN_VALUE, -2, 100, 1000};
    static final int[] HEIGHTS = {-32768, 0, 1, -1, 50, 200, -300, 32767, Integer.MAX_VALUE, Integer.MIN_VALUE, -32767};
    static final String[] NAMES = {"Zezima", "zezima", "ZEZIMA", "Bob", "bob", "", "Émile", "émile", "A B", "x", "Girl Power", "ß", "SS", "i", "İ"};
    static final String[] TEXTS = {"hello", "", "Hello World!", "éè 中文", "tab", "a", "A long overhead message"};
    static final ClientScriptOpCode[] OPS = {
        ClientScriptOpCode.GET_ENTITY_OVERLAY_HEIGHT, ClientScriptOpCode.GET_OBJ_OVERLAY_HEIGHT, ClientScriptOpCode.GET_ENTITY_SAY,
        ClientScriptOpCode.SETENTITYOVERHEADIF, ClientScriptOpCode.GETENTITYOVERHEADIF, ClientScriptOpCode.GET_DISPLAYNAME_WITHEXTRAS,
        ClientScriptOpCode.GET_NPC_NAME, ClientScriptOpCode.IS_NPC_ACTIVE, ClientScriptOpCode.IS_NPC_VISIBLE, ClientScriptOpCode.IS_TARGETED_ENTITY
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

    static void put(Object target, String name, Object value) throws Exception {
        unsafe.putObject(target, unsafe.objectFieldOffset(field(target.getClass(), name)), value);
    }

    static void putInt(Object target, String name, int value) throws Exception {
        unsafe.putInt(target, unsafe.objectFieldOffset(field(target.getClass(), name)), value);
    }

    static void putBoolean(Object target, String name, boolean value) throws Exception {
        unsafe.putBoolean(target, unsafe.objectFieldOffset(field(target.getClass(), name)), value);
    }

    static void putStatic(Class<?> type, String name, Object value) throws Exception {
        var f = field(type, name);
        unsafe.putObject(unsafe.staticFieldBase(f), unsafe.staticFieldOffset(f), value);
    }

    static String hex(String text) {
        if (text == null) return "-";
        var sb = new StringBuilder("x");
        for (byte b : text.getBytes(StandardCharsets.UTF_8)) sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }

    static String ints(int[] values, int pointer) {
        if (pointer <= 0) return "-";
        var sb = new StringBuilder();
        for (int i = 0; i < pointer; i++) {
            if (i > 0) sb.append(',');
            sb.append(values[i]);
        }
        return sb.toString();
    }

    static String strings(Object[] values, int pointer) {
        if (pointer <= 0) return "-";
        var sb = new StringBuilder();
        for (int i = 0; i < pointer; i++) {
            if (i > 0) sb.append(',');
            sb.append(hex((String) values[i]));
        }
        return sb.toString();
    }

    static String nullable(String text, Random random) {
        return random.nextInt(5) == 0 ? null : text;
    }

    static String pickName(Random random) {
        return NAMES[random.nextInt(NAMES.length)];
    }

    static int small(Random random, int bound) {
        return random.nextInt(bound);
    }

    static int wide(Random random) {
        return random.nextInt(6) == 0 ? EDGES[random.nextInt(EDGES.length)] : random.nextInt(200) - 5;
    }

    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        var theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        unsafe = (sun.misc.Unsafe) theUnsafe.get(null);
        var out = new StringBuilder();
        var random = new Random(SEED);

        TypeLists.VAR = new VarpTypeList(null);
        TypeLists.VARPBIT = new VarpbitTypeList(null);
        TypeLists.VARPBIT.cache = new LRUCache(1024);
        var domain = new PlayerVarDomain();
        domain.activeVars = new int[65536];
        domain.pendingVars = new int[65536];
        VarProviders.setPLAYER_VAR_PROVIDER(domain);
        int[][] bits = {{100, 3, 0, 2}, {101, 3, 4, 7}, {102, 4, 0, 31}, {103, 5, 8, 11}, {104, 6, 0, 0}};
        for (var bit : bits) {
            var definition = new VarpbitType();
            definition.configIndex = bit[1];
            definition.startBit = bit[2];
            definition.endBit = bit[3];
            TypeLists.VARPBIT.cache.put(definition, bit[0]);
            out.append("X\t").append(bit[0]).append('\t').append(bit[1]).append('\t').append(bit[2]).append('\t').append(bit[3]).append('\n');
        }
        for (int varp = 0; varp < 8; varp++) {
            int value = varp == 4 ? new int[] {-1, 2, 0, 7, Integer.MIN_VALUE, 1, 3}[random.nextInt(7)] : random.nextInt(24) - 2;
            if (varp == 3) value = random.nextInt(0x100);
            domain.activeVars[varp] = value;
            domain.pendingVars[varp] = value;
            out.append("V\t").append(varp).append('\t').append(value).append('\n');
        }

        var basList = (BasTypeList) unsafe.allocateInstance(BasTypeList.class);
        put(basList, "basCache", new LRUCache(64));
        TypeLists.BAS = basList;
        var basIds = new ArrayList<Integer>();
        for (int id = 1; id <= 8; id++) {
            var bas = new BasType();
            bas.iconHeightOverride = id % 3 == 0 ? -1 : random.nextInt(400) - 20;
            basList.basCache.put(bas, (long) id);
            basIds.add(id);
            out.append("A\t").append(id).append('\t').append(bas.iconHeightOverride).append('\n');
        }

        var typeList = (NpcTypeList) unsafe.allocateInstance(NpcTypeList.class);
        put(typeList, "typeCache", new LRUCache(64));
        TypeLists.NPC = typeList;
        int typeCount = 40;
        for (int id = 0; id < typeCount; id++) {
            var type = (NPCType) unsafe.allocateInstance(NPCType.class);
            type.type = id;
            put(type, "name", id % 11 == 7 ? "null" : "Npc " + pickName(random));
            putBoolean(type, "visible", random.nextInt(3) != 0);
            putInt(type, "iconHeight", random.nextInt(3) == 0 ? random.nextInt(300) : -1);
            putInt(type, "npcBasId", random.nextInt(3) == 0 ? -1 : 1 + random.nextInt(8));
            putInt(type, "varp", -1);
            putInt(type, "varpbit", -1);
            put(type, "npcTypeList", typeList);
            String alt = "none";
            int mode = id % 5;
            int[] alternatives = null;
            if (id >= 20) {
                int length = id % 6;
                alternatives = new int[length];
                for (int i = 0; i < length; i++) alternatives[i] = random.nextInt(4) == 0 ? -1 : random.nextInt(20);
                int kind = random.nextInt(3);
                if (kind == 0) putInt(type, "varp", random.nextInt(8));
                else if (kind == 1) putInt(type, "varpbit", 100 + random.nextInt(5));
                put(type, "alternativeIds", alternatives);
                var sb = new StringBuilder();
                for (int i = 0; i < length; i++) {
                    if (i > 0) sb.append(',');
                    sb.append(alternatives[i]);
                }
                alt = length == 0 ? "empty" : sb.toString();
            }
            typeList.typeCache.put(type, (long) id);
            out.append("T\t").append(id).append('\t').append(hex(getString(type, "name")))
                .append('\t').append(getBoolean(type, "visible") ? 1 : 0).append('\t').append(getInt(type, "iconHeight")).append('\t').append(getInt(type, "npcBasId"))
                .append('\t').append(alt).append('\t').append(getInt(type, "varp")).append('\t').append(getInt(type, "varpbit")).append('\n');
        }

        var friends = new ArrayList<String>();
        for (int i = 0; i < 4; i++) {
            var friend = (Friend) unsafe.allocateInstance(Friend.class);
            friend.displayName = NAMES[i * 2 % NAMES.length];
            FriendList.FRIENDS[i] = friend;
            friends.add(friend.displayName);
            out.append("F\t").append(hex(friend.displayName)).append('\n');
        }
        FriendList.FRIEND_COUNT = friends.size();
        putStatic(Interface.class, "INTERFACES_LOADED", new boolean[0]);

        var players = new ArrayList<PlayerEntity>();
        var npcs = new ArrayList<NpcEntity>();
        var grounds = new ArrayList<GroundItem>();
        int playerCount = 14, npcCount = 30, groundCount = 6;
        for (int n = 0; n < playerCount; n++) {
            var p = (PlayerEntity) unsafe.allocateInstance(PlayerEntity.class);
            put(p, "_absolutePosition", new CoordinateSpace());
            String username = nullable(pickName(random), random), display = nullable(pickName(random), random);
            String prefix = nullable(random.nextBoolean() ? "<col=ff0000>" : "Sir ", random), postfix = nullable(random.nextBoolean() ? " the Great" : "é", random);
            if (n == 0) username = null;
            if (n == 1) { prefix = null; postfix = null; }
            int render = random.nextInt(4) == 0 ? -1 : 1 + random.nextInt(8);
            if (n == 2) render = 99;
            int index = n < 4 ? new int[] {0, 1, 2047, -1}[n] : random.nextInt(2048);
            int loaded = n % 3 == 0 ? -1 : wide(random), toLoad = n % 4 == 0 ? loaded : wide(random);
            int miny = HEIGHTS[random.nextInt(HEIGHTS.length)];
            String text = random.nextInt(4) == 0 ? null : TEXTS[random.nextInt(TEXTS.length)];
            put(p, "username", username);
            put(p, "displayName", display);
            put(p, "prefixTitle", prefix);
            put(p, "postfixTitle", postfix);
            putInt(p, "renderEmote", render);
            putInt(p, "playerIndex", index);
            putInt(p, "loadedInterfaceId", loaded);
            putInt(p, "interfaceIdToLoad", toLoad);
            putInt(p, "modelMinY", miny);
            if (text != null || n % 5 == 4) {
                var hint = new HintDetails();
                hint.text = text;
                put(p, "hintDetails", hint);
            }
            players.add(p);
            out.append("P\t").append(n).append('\t').append(hex(username)).append('\t').append(hex(display)).append('\t').append(hex(prefix)).append('\t').append(hex(postfix))
                .append('\t').append(render).append('\t').append(index).append('\t').append(loaded).append('\t').append(toLoad).append('\t').append(miny).append('\t').append(hex(text)).append('\n');
        }
        for (int n = 0; n < npcCount; n++) {
            var npc = (NpcEntity) unsafe.allocateInstance(NpcEntity.class);
            put(npc, "_absolutePosition", new CoordinateSpace());
            String type = n % 9 == 8 ? "-" : Integer.toString(random.nextInt(typeCount));
            if (n < typeCount && n % 9 != 8 && n % 2 == 0) type = Integer.toString(n);
            String modified = nullable(pickName(random), random);
            int render = random.nextInt(3) == 0 ? 1 + random.nextInt(8) : -1;
            int index = random.nextInt(32768);
            int loaded = n % 3 == 0 ? -1 : wide(random), toLoad = n % 4 == 0 ? loaded : wide(random);
            int miny = HEIGHTS[random.nextInt(HEIGHTS.length)];
            String text = random.nextInt(3) == 0 ? null : TEXTS[random.nextInt(TEXTS.length)];
            if (!type.equals("-")) npc.definitions = typeList.list(Integer.parseInt(type));
            put(npc, "modifiedName", modified);
            putInt(npc, "currRenderAnim", render);
            putInt(npc, "playerIndex", index);
            putInt(npc, "loadedInterfaceId", loaded);
            putInt(npc, "interfaceIdToLoad", toLoad);
            putInt(npc, "modelMinY", miny);
            if (text != null || n % 5 == 4) {
                var hint = new HintDetails();
                hint.text = text;
                put(npc, "hintDetails", hint);
            }
            npcs.add(npc);
            out.append("N\t").append(n).append('\t').append(type).append('\t').append(hex(modified)).append('\t').append(render).append('\t').append(index).append('\t').append(loaded)
                .append('\t').append(toLoad).append('\t').append(miny).append('\t').append(hex(text)).append('\n');
        }
        for (int n = 0; n < groundCount; n++) {
            var item = (GroundItem) unsafe.allocateInstance(GroundItem.class);
            int offset = n < EDGES.length ? EDGES[n] : random.nextInt(500);
            putInt(item, "tileHeightOffset", offset);
            grounds.add(item);
            out.append("G\t").append(n).append('\t').append(offset).append('\n');
        }

        var specs = new HashMap<PathingEntity, int[]>();
        for (var p : players) specs.put(p, new int[] {getInt(p, "loadedInterfaceId"), getInt(p, "interfaceIdToLoad")});
        for (var n : npcs) specs.put(n, new int[] {getInt(n, "loadedInterfaceId"), getInt(n, "interfaceIdToLoad")});
        var cases = new ArrayList<int[]>();
        for (int op = 0; op < OPS.length; op++) {
            for (int n = 0; n < playerCount; n++) cases.add(new int[] {op, 0, n, 0});
            for (int n = 0; n < npcCount; n++) cases.add(new int[] {op, 1, n, 0});
            cases.add(new int[] {op, 2, 0, 0});
            for (int n = 0; n < groundCount; n++) cases.add(new int[] {op, 2, 0, n + 1});
        }
        for (int repeat = 0; repeat < 4; repeat++) {
            for (int op = 0; op < OPS.length; op++) {
                for (int n = 0; n < playerCount; n++) cases.add(new int[] {op, 0, n, 0});
                for (int n = 0; n < npcCount; n++) cases.add(new int[] {op, 1, n, 0});
            }
        }
        Collections.shuffle(cases, random);
        int total = 0;
        for (var item : cases) {
            var op = OPS[item[0]];
            var e = new CS2Executor();
            e.instrPtr = 0;
            Arrays.fill(e.intStack, 0x5a5a5a5a);
            e.intStackPtr = 0;
            PathingEntity current = item[1] == 0 ? players.get(item[2]) : item[1] == 1 ? npcs.get(item[2]) : null;
            if (current != null) {
                putInt(current, "loadedInterfaceId", specs.get(current)[0]);
                putInt(current, "interfaceIdToLoad", specs.get(current)[1]);
            }
            e.currentEntity = current;
            e.currentGroundItem = item[3] == 0 ? null : grounds.get(item[3] - 1);
            int expectedIndex = current == null ? 0 : current.getCS2Index();
            int target = random.nextInt(3) == 0 ? expectedIndex : random.nextInt(3) == 0 ? EDGES[random.nextInt(EDGES.length)] : random.nextInt(40) - 20;
            Client.ENTITY_TARGET = target;
            int filter = random.nextInt(5);
            ChatBox.PUBLIC_FILTER = filter;
            int mineKind = random.nextInt(4);
            var mine = (PlayerEntity) unsafe.allocateInstance(PlayerEntity.class);
            String mineName = mineKind == 0 ? null : NAMES[random.nextInt(NAMES.length)];
            put(mine, "displayName", mineName);
            PlayerUpdate.MY_PLAYER = mine;
            int argument = wide(random);
            if (op == ClientScriptOpCode.SETENTITYOVERHEADIF) e.intStack[e.intStackPtr++] = argument;
            int initial = e.intStackPtr;
            String result = "ok";
            try {
                DetailCapabilityOps.dispatch(op, e);
            } catch (Throwable error) {
                result = "error:" + error.getClass().getSimpleName();
            }
            int loaded = current == null ? 0 : getInt(current, "loadedInterfaceId");
            int toLoad = current == null ? 0 : getInt(current, "interfaceIdToLoad");
            out.append("C\t").append(op.name()).append('\t').append(item[1] == 0 ? "player" : item[1] == 1 ? "npc" : "none").append('\t').append(item[2]).append('\t').append(item[3] - 1)
                .append('\t').append(target).append('\t').append(filter).append('\t').append(hex(mineName)).append('\t').append(argument).append('\t').append(initial).append('\t').append(result)
                .append('\t').append(ints(e.intStack, e.intStackPtr)).append('\t').append(strings(e.stringStack, e.stringStackPtr)).append('\t').append(loaded).append('\t').append(toLoad).append('\n');
            total++;
        }
        out.append("Z\t").append(total).append('\n');
        Files.writeString(output.resolve("entity-state.native.tsv"), out.toString(), StandardCharsets.UTF_8);
        System.exit(0);
    }

    static int getInt(Object target, String name) throws Exception {
        return unsafe.getInt(target, unsafe.objectFieldOffset(field(target.getClass(), name)));
    }

    static boolean getBoolean(Object target, String name) throws Exception {
        return unsafe.getBoolean(target, unsafe.objectFieldOffset(field(target.getClass(), name)));
    }

    static String getString(Object target, String name) throws Exception {
        return (String) unsafe.getObject(target, unsafe.objectFieldOffset(field(target.getClass(), name)));
    }
}
