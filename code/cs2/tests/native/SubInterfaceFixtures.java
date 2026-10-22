import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.client.Client;
import com.jagex.game.runetek5.config.rsinterface.IFEvents;
import com.jagex.game.runetek5.config.rsinterface.SubInterface;
import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class SubInterfaceFixtures {
    static final long SEED = 0x5ab1c11L;
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

    static Object staticGet(Class<?> type, String name) throws Exception {
        var f = field(type, name);
        f.setAccessible(true);
        return f.get(null);
    }

    static void staticSet(Class<?> type, String name, Object value) throws Exception {
        var f = field(type, name);
        f.setAccessible(true);
        f.set(null, value);
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

    @SuppressWarnings("unchecked")
    static String table(IterableNodeMap<?> map) {
        var parts = new ArrayList<String>();
        for (int bucket = 0; bucket < map.size; bucket++) {
            var head = map.nodes[bucket];
            for (Node<Object> node = head.next; node != head; node = node.next) {
                var sub = (SubInterface) (Object) node;
                parts.add((int) node.pointer + ":" + sub.subInterfaceId + ":" + sub.overlay);
            }
        }
        return parts.isEmpty() ? "-" : String.join(",", parts);
    }

    @SuppressWarnings("unchecked")
    static String settings(IterableNodeMap<?> map) {
        var parts = new ArrayList<String>();
        for (int bucket = 0; bucket < map.size; bucket++) {
            var head = map.nodes[bucket];
            for (Node<Object> node = head.next; node != head; node = node.next) {
                var events = (IFEvents) (Object) node;
                parts.add((int) (node.pointer >> 32) + ":" + (int) node.pointer + ":" + events.settingsHash + ":" + events.interfaceId);
            }
        }
        Collections.sort(parts);
        return parts.isEmpty() ? "-" : String.join(",", parts);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        var theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        unsafe = (sun.misc.Unsafe) theUnsafe.get(null);
        staticSet(Client.class, "gameState", com.jagex.game.runetek5.client.GameState.UNK_4);
        staticSet(com.jagex.game.runetek5.config.rsinterface.Interface.class, "INTERFACES", new com.jagex.game.runetek5.config.rsinterface.Interface[0]);
        staticSet(com.jagex.game.runetek5.config.rsinterface.Interface.class, "INTERFACES_LOADED", new boolean[0]);
        var random = new Random(SEED);
        var out = new StringBuilder();
        var states = new ArrayList<Object[]>();
        for (int s = 0; s < 400; s++) {
            int count = random.nextInt(9);
            var used = new LinkedHashMap<Integer, int[]>();
            for (int i = 0; i < count; i++) {
                int parent = (random.nextInt(7) << 16) | random.nextInt(12);
                if (random.nextInt(12) == 0) parent = random.nextInt(3) == 0 ? -1 : random.nextInt();
                int overlay = random.nextInt(4) == 0 ? random.nextInt(3) : 3;
                used.put(parent, new int[] {random.nextInt(7), overlay});
            }
            var set = new ArrayList<int[]>();
            int settingCount = random.nextInt(7);
            var seen = new HashSet<String>();
            for (int i = 0; i < settingCount; i++) {
                int[] row = {(random.nextInt(7) << 16) | random.nextInt(12), random.nextInt(5), random.nextInt(1 << 20), random.nextInt(7)};
                if (seen.add(row[0] + ":" + row[1])) set.add(row);
            }
            states.add(new Object[] {used, set});
            var sb = new StringBuilder("H\t").append(s).append('\t');
            var parts = new ArrayList<String>();
            for (var entry : used.entrySet()) parts.add(entry.getKey() + ":" + entry.getValue()[0] + ":" + entry.getValue()[1]);
            sb.append(parts.isEmpty() ? "-" : String.join(",", parts)).append('\t');
            parts.clear();
            for (var entry : set) parts.add(entry[0] + ":" + entry[1] + ":" + entry[2] + ":" + entry[3]);
            sb.append(parts.isEmpty() ? "-" : String.join(",", parts));
            out.append(sb).append('\n');
        }
        var cases = new ArrayList<Object[]>();
        for (int s = 0; s < states.size(); s++) {
            var used = (Map<Integer, int[]>) states.get(s)[0];
            var parents = new ArrayList<Integer>(used.keySet());
            for (int i = 0; i < 6; i++) {
                int parent = !parents.isEmpty() && random.nextInt(4) != 0 ? parents.get(random.nextInt(parents.size())) : (random.nextInt(7) << 16) | random.nextInt(12);
                int sub = random.nextInt(3) == 0 && used.containsKey(parent) ? used.get(parent)[0] : random.nextInt(7);
                cases.add(new Object[] {"IF_OPENSUBCLIENT", s, parent, sub});
            }
            for (int i = 0; i < 6; i++) {
                int parent = !parents.isEmpty() && random.nextInt(5) != 0 ? parents.get(random.nextInt(parents.size())) : (random.nextInt(7) << 16) | random.nextInt(12);
                cases.add(new Object[] {"IF_CLOSESUBCLIENT", s, parent, 0});
            }
        }
        Collections.shuffle(cases, random);
        var stats = new TreeMap<String, int[]>();
        int total = 0;
        for (var item : cases) {
            String op = (String) item[0];
            int s = (Integer) item[1];
            var state = states.get(s);
            var open = new IterableNodeMap<SubInterface>(8);
            for (var entry : ((Map<Integer, int[]>) state[0]).entrySet()) {
                open.put(new SubInterface(entry.getValue()[0], entry.getValue()[1]), (long) entry.getKey());
            }
            var slots = (IterableNodeMap<IFEvents>) staticGet(Client.class, "ICOMPONENT_SETTINGS_SLOTS");
            slots.clear();
            for (var entry : (List<int[]>) state[1]) {
                slots.put(new IFEvents(entry[2], entry[3]), ((long) entry[0] << 32) + entry[1]);
            }
            staticSet(Client.class, "OPEN_INTERFACES", open);
            staticSet(Client.class, "activeDialogueContinue", null);
            var e = new CS2Executor();
            e.intOpValues = new int[] {0};
            e.instrPtr = 0;
            Arrays.fill(e.intStack, 0x5a5a5a5a);
            e.intStackPtr = 0;
            e.stringStackPtr = 0;
            String result = "ok";
            e.intStack[e.intStackPtr++] = (Integer) item[2];
            String argument = String.valueOf(item[2]);
            if (op.equals("IF_OPENSUBCLIENT")) {
                e.intStack[e.intStackPtr++] = (Integer) item[3];
                argument += ":" + item[3];
            }
            try {
                DialogOps.INSTANCE.dispatch(ClientScriptOpCode.valueOf(op), e);
            } catch (Throwable error) {
                result = "error:" + error.getClass().getSimpleName();
            }
            out.append("T\t").append(op).append('\t').append(s).append('\t').append(argument).append('\t').append(result).append('\t')
                .append(ints(e.intStack, e.intStackPtr)).append('\t').append(table(open)).append('\t').append(settings(slots)).append('\n');
            stats.computeIfAbsent(op, k -> new int[2])[result.equals("ok") ? 0 : 1]++;
            total++;
        }
        out.append("Z\t").append(total).append('\n');
        Files.writeString(output.resolve("sub-interface.native.tsv"), out.toString(), StandardCharsets.UTF_8);
        var summary = new StringBuilder();
        for (var entry : stats.entrySet()) summary.append(entry.getKey()).append('\t').append(entry.getValue()[0]).append('\t').append(entry.getValue()[1]).append('\n');
        Files.writeString(output.resolve("sub-interface.stats.tsv"), summary.toString(), StandardCharsets.UTF_8);
        System.exit(0);
    }
}
