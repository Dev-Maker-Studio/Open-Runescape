import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.client.Client;
import com.jagex.game.runetek5.client.login.LoginManager;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.*;
import com.jagex.game.runetek5.config.npc.NPCType;
import com.jagex.game.runetek5.config.varn.*;
import com.jagex.game.runetek5.vardomain.VarnCache;
import com.jagex.game.runetek5.entity.pathingentity.EntityTurnDirection;
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerAppearance;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.network.ConnectionInfo;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class PlayerStateFixtures {
    static final long SEED = 0x727c5227L;
    static final int[] EDGES = {0, 1, -1, 2, 5, 6, 7, 8, 9, 10, 24, 25, 26, 31, 32, 255, 256, 65535, 65536, Integer.MAX_VALUE, Integer.MIN_VALUE, -2, 100, 1000};
    static final int[] VALUES = {0, 1, -1, 2, 0x7fffffff, Integer.MIN_VALUE, 0x5aa5f00f, 255, 256, 65535, 0xff00ff00, 12345678, -98765, 99, 5, 9, 4, 10};
    static final ClientScriptOpCode[] PLAYER_OPS = {
        ClientScriptOpCode.MAP_MEMBERS, ClientScriptOpCode.STAFFMODLEVEL, ClientScriptOpCode.REBOOTTIMER, ClientScriptOpCode.MAP_WORLD,
        ClientScriptOpCode.RUNENERGY_VISIBLE, ClientScriptOpCode.RUNWEIGHT_VISIBLE, ClientScriptOpCode.PLAYERMOD, ClientScriptOpCode.PLAYERMODLEVEL,
        ClientScriptOpCode.PLAYERMEMBER, ClientScriptOpCode.COMLEVEL_ACTIVE, ClientScriptOpCode.GENDER, ClientScriptOpCode.PLAYERDEMO,
        ClientScriptOpCode.GET_SELFYANGLE, ClientScriptOpCode.SELF_PLAYER_UID
    };
    static final ClientScriptOpCode[] SKILL_OPS = {ClientScriptOpCode.STAT, ClientScriptOpCode.STAT_BASE, ClientScriptOpCode.STAT_VISIBLE_XP};
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
        var f = field(target.getClass(), name);
        unsafe.putObject(target, unsafe.objectFieldOffset(f), value);
    }

    static void putInt(Object target, String name, int value) throws Exception {
        var f = field(target.getClass(), name);
        unsafe.putInt(target, unsafe.objectFieldOffset(f), value);
    }

    static void putBoolean(Object target, String name, boolean value) throws Exception {
        var f = field(target.getClass(), name);
        unsafe.putBoolean(target, unsafe.objectFieldOffset(f), value);
    }

    static void putStatic(Class<?> type, String name, Object value) throws Exception {
        var f = field(type, name);
        unsafe.putObject(unsafe.staticFieldBase(f), unsafe.staticFieldOffset(f), value);
    }

    static int pick(Random random) {
        return VALUES[random.nextInt(VALUES.length)];
    }

    static void setStatic(Class<?> type, String name, Object value) throws Exception {
        type.getField(name).set(null, value);
    }

    static String stack(int[] values, int pointer) {
        if (pointer <= 0) return "-";
        var sb = new StringBuilder();
        for (int i = 0; i < pointer; i++) {
            if (i > 0) sb.append(',');
            sb.append(values[i]);
        }
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        var theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        unsafe = (sun.misc.Unsafe) theUnsafe.get(null);
        var out = new StringBuilder();
        var random = new Random(SEED);
        putStatic(Client.class, "SKILL_LEVEL_VISIBLE", new int[25]);
        putStatic(Client.class, "SKILL_LEVEL_ACTUAL", new int[25]);
        putStatic(Client.class, "SKILL_XP", new int[25]);
        TypeLists.VARN = new VarnTypeList(null);
        TypeLists.VARNBIT = new VarnBitTypeList(null);
        int stateCount = 14;
        var playerStates = new ArrayList<Runnable>();
        for (int s = 0; s < stateCount; s++) {
            int[] v = new int[25], a = new int[25], x = new int[25];
            for (int i = 0; i < 25; i++) {
                v[i] = pick(random);
                a[i] = pick(random);
                x[i] = pick(random);
                out.append("K\t").append(s).append('\t').append(i).append('\t').append(v[i]).append('\t').append(a[i]).append('\t').append(x[i]).append('\n');
            }
            int rights = s < 7 ? new int[] {0, 1, 2, 3, 255, -1, 1000}[s] : pick(random);
            int mod = s < 7 ? new int[] {4, 5, 7, 9, 10, 0, -5}[s] : pick(random);
            boolean member = random.nextBoolean(), world = random.nextBoolean();
            int reboot = pick(random), energy = pick(random), weight = pick(random), index = pick(random);
            String worldId = s % 5 == 4 ? "null" : Integer.toString(pick(random));
            String player = s % 4 == 3 ? "null" : pick(random) + "," + new String[] {"0", "1", "n"}[s % 3] + "," + pick(random);
            out.append("S\t").append(s).append('\t').append(rights).append('\t').append(mod).append('\t').append(member ? 1 : 0).append('\t').append(world ? 1 : 0)
                .append('\t').append(reboot).append('\t').append(energy).append('\t').append(weight).append('\t').append(worldId).append('\t').append(player).append('\t').append(index).append('\n');
            final int[] fv = v, fa = a, fx = x;
            playerStates.add(() -> {
                try {
                    System.arraycopy(fv, 0, Client.SKILL_LEVEL_VISIBLE, 0, 25);
                    System.arraycopy(fa, 0, Client.SKILL_LEVEL_ACTUAL, 0, 25);
                    System.arraycopy(fx, 0, Client.SKILL_XP, 0, 25);
                    Client.PLAYER_RIGHTS = rights;
                    Client.PLAYER_MOD_LEVEL = mod;
                    Client.IS_MEMBER = member;
                    Client.memberWorld = world;
                    Client.REBOOT_TIMER = reboot;
                    Client.RUN_ENERGY = energy;
                    Client.RUN_WEIGHT = weight;
                    PlayerUpdate.myPlayerIndex = index;
                    if (worldId.equals("null")) {
                        LoginManager.GAME_CONNECTION_INFO = null;
                    } else {
                        var info = new ConnectionInfo();
                        info.worldId = Integer.parseInt(worldId);
                        LoginManager.GAME_CONNECTION_INFO = info;
                    }
                    if (player.equals("null")) {
                        PlayerUpdate.MY_PLAYER = null;
                    } else {
                        var parts = player.split(",");
                        var entity = (PlayerEntity) unsafe.allocateInstance(PlayerEntity.class);
                        putInt(entity, "combatLevel", Integer.parseInt(parts[0]));
                        if (!parts[1].equals("n")) {
                            var appearance = (PlayerAppearance) unsafe.allocateInstance(PlayerAppearance.class);
                            putBoolean(appearance, "female", parts[1].equals("1"));
                            put(entity, "playerAppearance", appearance);
                        }
                        var yaw = new EntityTurnDirection();
                        yaw.rotation = Integer.parseInt(parts[2]);
                        put(entity, "yaw", yaw);
                        PlayerUpdate.MY_PLAYER = entity;
                    }
                } catch (Exception error) {
                    throw new RuntimeException(error);
                }
            });
        }
        int npcCount = 8;
        var varnIds = new ArrayList<Integer>();
        for (int id = 0; id < 30; id++) {
            varnIds.add(id);
            int kind = id % 4;
            if (kind != 3) {
                var type = new VarnType();
                type.typeChar = kind == 0 ? 'i' : kind == 1 ? 'j' : (char) 0;
                TypeLists.VARN.cache.put(type, (long) id);
                out.append("T\t").append(id).append('\t').append((int) type.typeChar).append('\n');
            }
        }
        int[][] spans = {{0, 0}, {0, 7}, {3, 10}, {0, 31}, {31, 31}, {5, 4}, {7, 3}, {16, 47}, {0, 32}, {1, 33}, {28, 255}, {255, 255}, {30, 31}, {8, 15}, {0, 30}, {2, 2}, {12, 40}};
        var bitIds = new ArrayList<Integer>();
        for (int i = 0; i < 50; i++) {
            int id = 200 + i;
            bitIds.add(id);
            if (i % 10 == 9) continue;
            int varn = random.nextInt(34);
            var span = spans[random.nextInt(spans.length)];
            var definition = new VarnBitType();
            definition.varnId = varn;
            definition.startBit = span[0];
            definition.endBit = span[1];
            TypeLists.VARNBIT.cache.put(definition, (long) id);
            out.append("B\t").append(id).append('\t').append(varn).append('\t').append(span[0]).append('\t').append(span[1]).append('\n');
        }
        var npcs = new ArrayList<NpcEntity>();
        for (int n = 0; n < npcCount; n++) {
            var npc = (NpcEntity) unsafe.allocateInstance(NpcEntity.class);
            var varns = new VarnCache();
            put(npc, "varns", varns);
            int[] max = new int[6], current = new int[6];
            for (int i = 0; i < 6; i++) {
                max[i] = pick(random);
                current[i] = pick(random);
            }
            put(npc, "maxStats", max);
            put(npc, "currentStats", current);
            String type = n % 4 == 3 ? "null" : Integer.toString(pick(random));
            if (!type.equals("null")) {
                var definition = (NPCType) unsafe.allocateInstance(NPCType.class);
                definition.type = Integer.parseInt(type);
                npc.definitions = definition;
            }
            out.append("N\t").append(n).append('\t').append(type).append('\t').append(Arrays.toString(max).replaceAll("[\\[\\] ]", "")).append('\t').append(Arrays.toString(current).replaceAll("[\\[\\] ]", "")).append('\n');
            for (int id : varnIds) {
                if (random.nextInt(3) == 0) {
                    int value = pick(random);
                    varns.setVarn(id, value);
                    out.append("V\t").append(n).append('\t').append(id).append('\t').append(value).append('\n');
                }
            }
            npcs.add(npc);
        }
        var cases = new ArrayList<int[]>();
        for (int s = 0; s < stateCount; s++) {
            for (int op = 0; op < PLAYER_OPS.length; op++) cases.add(new int[] {0, s, op, 0, 0, 0});
            for (int op = 0; op < SKILL_OPS.length; op++) {
                for (int id = -3; id < 29; id++) cases.add(new int[] {1, s, op, id, 0, 0});
                for (int id : EDGES) cases.add(new int[] {1, s, op, id, 0, 0});
                cases.add(new int[] {1, s, op, 0, 1, 0});
            }
        }
        for (int n = 0; n < npcCount; n++) {
            for (int entity = 0; entity < 3; entity++) {
                for (int id = -2; id < 34; id++) cases.add(new int[] {2, n, 0, id, entity, 0});
                for (int id : EDGES) cases.add(new int[] {2, n, 0, id, entity, 0});
                for (int id : bitIds) cases.add(new int[] {2, n, 1, id, entity, 0});
                for (int id : new int[] {-1, 0, 199, 250, 99999, Integer.MIN_VALUE, Integer.MAX_VALUE}) cases.add(new int[] {2, n, 1, id, entity, 0});
                cases.add(new int[] {2, n, 2, 0, entity, 0});
                for (int id = -2; id < 9; id++) cases.add(new int[] {2, n, 3, id, entity, 0});
                for (int id : EDGES) cases.add(new int[] {2, n, 3, id, entity, 0});
                cases.add(new int[] {2, n, 3, 0, entity, 1});
            }
        }
        Collections.shuffle(cases, random);
        var stats = new TreeMap<String, int[]>();
        int total = 0;
        for (var item : cases) {
            ClientScriptOpCode op;
            String entityName = "none";
            var e = new CS2Executor();
            e.intOpValues = new int[] {item[3]};
            e.instrPtr = 0;
            Arrays.fill(e.intStack, 0x5a5a5a5a);
            e.intStackPtr = 0;
            playerStates.get(item[0] == 2 ? 0 : item[1]).run();
            String state = Integer.toString(item[1]);
            if (item[0] == 0) {
                op = PLAYER_OPS[item[2]];
            } else if (item[0] == 1) {
                op = SKILL_OPS[item[2]];
                if (item[4] == 0) e.intStack[e.intStackPtr++] = item[3];
            } else {
                op = new ClientScriptOpCode[] {ClientScriptOpCode.GET_VARN_OLD, ClientScriptOpCode.GET_VARNBIT_OLD, ClientScriptOpCode.NPC_TYPE, ClientScriptOpCode.GET_NPC_STAT}[item[2]];
                e.currentEntity = item[4] == 0 ? npcs.get(item[1]) : item[4] == 1 ? (com.jagex.game.runetek5.entity.pathingentity.PathingEntity) (Object) unsafe.allocateInstance(PlayerEntity.class) : null;
                entityName = item[4] == 0 ? "npc" : item[4] == 1 ? "player" : "null";
                if (item[2] == 3 && item[5] == 0) e.intStack[e.intStackPtr++] = item[3];
            }
            int initial = e.intStackPtr;
            String result = "ok";
            try {
                if (op == ClientScriptOpCode.STAT || op == ClientScriptOpCode.STAT_BASE || op == ClientScriptOpCode.STAT_VISIBLE_XP) {
                    PlayerWorldOps.dispatch(op, e);
                } else if (item[0] == 2) {
                    if (op == ClientScriptOpCode.GET_VARN_OLD || op == ClientScriptOpCode.GET_VARNBIT_OLD) CoreVmOps.dispatch(op, e);
                    else DetailCapabilityOps.dispatch(op, e);
                } else {
                    PlayerWorldOps.dispatch(op, e);
                }
            } catch (Throwable error) {
                result = "error:" + error.getClass().getSimpleName();
            }
            out.append("C\t").append(op.name()).append('\t').append(item[0] == 2 ? "-" : state).append('\t').append(item[0] == 2 ? state : "-").append('\t').append(entityName).append('\t')
                .append(item[3]).append('\t').append(initial).append('\t').append(result).append('\t').append(stack(e.intStack, e.intStackPtr)).append('\n');
            stats.computeIfAbsent(op.name(), k -> new int[2])[result.equals("ok") ? 0 : 1]++;
            total++;
        }
        out.append("Z\t").append(total).append('\n');
        Files.writeString(output.resolve("player-state.native.tsv"), out.toString(), StandardCharsets.UTF_8);
        var summary = new StringBuilder();
        for (var entry : stats.entrySet()) summary.append(entry.getKey()).append('\t').append(entry.getValue()[0]).append('\t').append(entry.getValue()[1]).append('\n');
        Files.writeString(output.resolve("player-state.stats.tsv"), summary.toString(), StandardCharsets.UTF_8);
        System.exit(0);
    }
}
