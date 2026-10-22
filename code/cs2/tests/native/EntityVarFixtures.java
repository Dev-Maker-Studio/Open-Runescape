import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.*;
import com.jagex.game.runetek5.config.varp.*;
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity;
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.ConfigNodeMap;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class EntityVarFixtures {
    static final long SEED = 0x727c5226L;
    static final Map<String, int[]> STATS = new TreeMap<>();
    static final int[] EDGES = {0, 1, -1, 2, 31, 32, 255, 256, 65535, 65536, Integer.MAX_VALUE, Integer.MIN_VALUE, -2, 100, 1000};
    static final int[] VALUES = {0, 1, -1, 2, 0x7fffffff, Integer.MIN_VALUE, 0x5aa5f00f, 255, 256, 65535, 0xff00ff00, 12345678, -98765};

    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        var out = new StringBuilder();
        var random = new Random(SEED);
        var field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        var unsafe = (sun.misc.Unsafe) field.get(null);
        var player = (PlayerEntity) unsafe.allocateInstance(PlayerEntity.class);
        var configs = new ConfigNodeMap();
        unsafe.putObject(player, unsafe.objectFieldOffset(PlayerEntity.class.getField("playerConfigs")), configs);
        var npc = (PathingEntity) (Object) unsafe.allocateInstance(NpcEntity.class);
        TypeLists.VAR = new VarpTypeList(null);
        TypeLists.VARPBIT = new VarpbitTypeList(null);
        TypeLists.VARPBIT.cache = new LRUCache(1024);
        var storedIds = new ArrayList<Integer>();
        var typedIds = new ArrayList<Integer>();
        for (int id = 0; id < 40; id++) {
            int kind = id % 5;
            if (kind != 4) {
                var type = new VarpType();
                type.typeChar = kind == 0 || kind == 1 ? 'i' : kind == 2 ? 'j' : (char) 0;
                TypeLists.VAR.vardefinitionCache.put(type, (long) id);
                typedIds.add(id);
                out.append("T\t").append(id).append('\t').append((int) type.typeChar).append('\n');
            }
            if (id % 3 == 0) {
                int value = VALUES[random.nextInt(VALUES.length)];
                configs.put(id, value);
                storedIds.add(id);
                out.append("V\t").append(id).append('\t').append(value).append('\n');
            }
        }
        configs.put(-5, 77);
        out.append("V\t-5\t77\n");
        configs.put(65535, -3);
        out.append("V\t65535\t-3\n");
        var varbitIds = new ArrayList<Integer>();
        int[][] spans = {{0, 0}, {0, 7}, {3, 10}, {0, 31}, {31, 31}, {5, 4}, {7, 3}, {16, 47}, {0, 32}, {1, 33}, {28, 255}, {255, 255}, {30, 31}, {8, 15}, {0, 30}, {2, 2}, {12, 40}};
        for (int i = 0; i < 60; i++) {
            int id = 100 + i;
            varbitIds.add(id);
            if (i % 9 == 8) {
                TypeLists.VARPBIT.cache.put(new VarpbitType(), (long) id);
                out.append("A\t").append(id).append('\n');
                continue;
            }
            int varp = i % 4 == 0 ? storedIds.get(random.nextInt(storedIds.size())) : i % 4 == 1 ? typedIds.get(random.nextInt(typedIds.size())) : random.nextInt(45);
            var span = spans[random.nextInt(spans.length)];
            var definition = new VarpbitType();
            definition.configIndex = varp;
            definition.startBit = span[0];
            definition.endBit = span[1];
            TypeLists.VARPBIT.cache.put(definition, (long) id);
            out.append("B\t").append(id).append('\t').append(varp).append('\t').append(span[0]).append('\t').append(span[1]).append('\n');
        }
        var cases = new ArrayList<int[]>();
        for (int op = 0; op < 2; op++) {
            for (int entity = 0; entity < 3; entity++) {
                var ids = new ArrayList<Integer>();
                for (int id = 0; id < 45; id++) ids.add(id);
                for (int id : new int[] {-5, 65535}) ids.add(id);
                for (int id : EDGES) ids.add(id);
                for (int id : varbitIds) ids.add(id);
                for (int id = 160; id < 164; id++) ids.add(id);
                for (int id : ids) cases.add(new int[] {op, entity, id});
            }
        }
        Collections.shuffle(cases, random);
        int total = 0;
        for (var item : cases) {
            var op = item[0] == 0 ? ClientScriptOpCode.GET_VARP_OLD : ClientScriptOpCode.GET_VARPBIT_OLD;
            var e = new CS2Executor();
            e.currentEntity = item[1] == 0 ? player : item[1] == 1 ? npc : null;
            e.intOpValues = new int[] {item[2]};
            e.instrPtr = 0;
            e.intStackPtr = 0;
            Arrays.fill(e.intStack, 0x5a5a5a5a);
            String result = "ok";
            try {
                CoreVmOps.dispatch(op, e);
            } catch (Throwable error) {
                result = "error:" + error.getClass().getSimpleName();
            }
            String entity = item[1] == 0 ? "player" : item[1] == 1 ? "npc" : "null";
            out.append("C\t").append(op.name()).append('\t').append(entity).append('\t').append(item[2]).append('\t').append(result).append('\t')
                .append(ComponentLayerFixtures.stack(e.intStack, e.intStackPtr)).append('\n');
            var stat = STATS.computeIfAbsent(op.name(), k -> new int[2]);
            stat[result.equals("ok") ? 0 : 1]++;
            total++;
        }
        out.append("Z\t").append(total).append('\n');
        Files.writeString(output.resolve("entity-vars.native.tsv"), out.toString(), StandardCharsets.UTF_8);
        var stats = new StringBuilder();
        for (var entry : STATS.entrySet()) stats.append(entry.getKey()).append('\t').append(entry.getValue()[0]).append('\t').append(entry.getValue()[1]).append('\n');
        Files.writeString(output.resolve("entity-vars.stats.tsv"), stats.toString(), StandardCharsets.UTF_8);
        System.exit(0);
    }
}
