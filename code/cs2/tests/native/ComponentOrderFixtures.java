import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.client.Client;
import com.jagex.game.runetek5.client.GameShell;
import com.jagex.game.runetek5.config.rsinterface.Component;
import com.jagex.game.runetek5.config.rsinterface.Interface;
import com.jagex.game.runetek5.config.rsinterface.SubInterface;
import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class ComponentOrderFixtures {
    static final String[] NAMES = {
        "CC_CREATE", "CC_DELETE", "CC_DELETEALL", "CC_FIND", "IF_FIND",
        "CC_SENDTOFRONT", "CC_SENDTOBACK", "IF_SENDTOFRONT", "IF_SENDTOBACK",
        "IF_HASSUB", "IF_HASSUBMODAL", "IF_HASSUBOVERLAY"
    };
    static final int[] WEIGHTS = {3, 1, 1, 3, 1, 4, 4, 2, 2, 2, 2, 2};
    static final int[] SUB_IDS = {2, 3, 6, 99, 0, -1, 65535, 7};
    static final int[] TYPES = {0, 1, 4, 5, 9, 10, -1, 255, 256, 3, 7};
    static final int[] SLOTS = {-1, 0, 1, 2, 3, 4, 5, 8, 20, 70000, -2, Integer.MAX_VALUE, Integer.MIN_VALUE};
    static final long SEED = 0x727c5225L;
    static final int STEPS = 7000;
    static final Map<String, int[]> STATS = new TreeMap<>();

    static int argumentCount(String name) {
        return switch (name) {
            case "CC_CREATE" -> 3;
            case "CC_DELETE", "CC_SENDTOFRONT", "CC_SENDTOBACK" -> 0;
            case "CC_DELETEALL", "IF_FIND", "IF_SENDTOFRONT", "IF_SENDTOBACK", "IF_HASSUB" -> 1;
            default -> 2;
        };
    }

    static void dispatch(ClientScriptOpCode op, CS2Executor e) {
        String name = op.name();
        if (name.startsWith("IF_HAS")) {
            InterfaceQueryOps.dispatch(op, e);
        } else {
            ComponentLifecycleOps.dispatch(op, e);
        }
    }

    static String label(Component c) {
        return c == null ? "_" : c.idHash + "/" + c.slotId;
    }

    static String list(Component[] values) {
        if (values == null) return "-";
        var sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(label(values[i]));
        }
        return sb.toString();
    }

    static String describe(Component c) {
        return c.idHash + ":" + c.parentId + ":" + c.slotId + "|" + list((Component[]) (Object) c.slotChildren) + "|" + list(c.itemSlots);
    }

    static void dump(Interface[] interfaces, int capacity, Map<String, String> out) {
        out.clear();
        for (int id = 0; id < capacity; id++) {
            var inter = interfaces[id];
            if (inter == null) continue;
            out.put("i" + id + ".sub", list(inter.getSubComponents()));
            for (int index = 0; index < inter.components.length; index++) {
                var c = inter.components[index];
                if (c == null) continue;
                out.put("i" + id + "." + index, describe(c));
                var children = c.slotChildren;
                if (children != null) {
                    for (int slot = 0; slot < children.length; slot++) {
                        var child = (Component) (Object) children[slot];
                        if (child != null) out.put("i" + id + "." + index + ".s" + slot, describe(child));
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        var out = new StringBuilder();
        var random = new Random(SEED);
        var weights = new ArrayList<String>();
        for (int i = 0; i < NAMES.length; i++) for (int n = 0; n < WEIGHTS[i]; n++) weights.add(NAMES[i]);
        var worlds = List.of(ComponentLayerFixtures.worldMain(), ComponentLayerFixtures.worldDense());
        var previous = new HashMap<String, String>();
        var current = new HashMap<String, String>();
        int total = 0;
        for (var w : worlds) {
            var interfaces = new Interface[w.capacity];
            var loaded = new boolean[w.capacity];
            Interface.INTERFACES = interfaces;
            Interface.INTERFACES_LOADED = loaded;
            GameShell.screenCanvasWidth = w.width;
            GameShell.screenCanvasHeight = w.height * ComponentLayerFixtures.inverse(-969250379);
            Client.Companion.setLastRenderedCycle(-2);
            Arrays.fill(Client.Companion.getIS_COMPONENT_ON_TOP(), false);
            ComponentLayerFixtures.build(w, out, interfaces, loaded);
            var open = new IterableNodeMap<SubInterface>(8);
            Client.OPEN_INTERFACES = open;
            var pool = ComponentLayerFixtures.hashPool(w);
            var valid = new ArrayList<Integer>();
            for (var entry : w.components.entrySet()) {
                for (var row : entry.getValue()) if (row != null) valid.add(Integer.parseInt(row[3]));
            }
            var subs = new LinkedHashMap<Integer, Integer>();
            for (int hash : valid) {
                if (random.nextInt(2) == 0) {
                    int sub = SUB_IDS[random.nextInt(SUB_IDS.length)];
                    int overlay = random.nextInt(2);
                    open.put(new SubInterface(sub, overlay), (long) hash);
                    subs.put(hash, sub);
                    out.append("O\t").append(hash).append('\t').append(sub).append('\t').append(overlay).append('\n');
                }
            }
            var e = new CS2Executor();
            e.hookedInterface1.reset();
            e.hookedInterface2.reset();
            for (int secondary = 0; secondary < 2; secondary++) {
                var hook = secondary == 0 ? e.hookedInterface1 : e.hookedInterface2;
                var ids = new ArrayList<>(w.components.keySet());
                int id = ids.get(random.nextInt(ids.size()));
                var rows = w.components.get(id);
                int index = rows.isEmpty() ? 0 : random.nextInt(rows.size());
                if (!rows.isEmpty() && rows.get(index) != null) {
                    hook.inter = interfaces[id];
                    hook.comp = interfaces[id].components[index];
                    out.append("K\t").append(secondary).append('\t').append(id).append('\t').append(index).append('\n');
                }
            }
            dump(interfaces, w.capacity, previous);
            for (int step = 0; step < STEPS; step++) {
                var name = weights.get(random.nextInt(weights.size()));
                var op = ClientScriptOpCode.valueOf(name);
                int operand = random.nextInt(2);
                int count = argumentCount(name);
                var ints = new int[count];
                int hash = random.nextInt(4) != 0 ? valid.get(random.nextInt(valid.size())) : pool.get(random.nextInt(pool.size()));
                switch (name) {
                    case "CC_CREATE" -> {
                        ints[0] = hash;
                        ints[1] = random.nextInt(4) != 0 ? new int[] {1, 4, 5, 9}[random.nextInt(4)] : TYPES[random.nextInt(TYPES.length)];
                        ints[2] = random.nextInt(4) != 0 ? random.nextInt(5) : SLOTS[random.nextInt(SLOTS.length)];
                    }
                    case "CC_FIND" -> {
                        ints[0] = hash;
                        ints[1] = random.nextInt(3) == 0 ? -1 : random.nextInt(8) == 0 ? SLOTS[random.nextInt(SLOTS.length)] : random.nextInt(5);
                    }
                    case "IF_FIND", "CC_DELETEALL", "IF_SENDTOFRONT", "IF_SENDTOBACK", "IF_HASSUB" -> ints[0] = hash;
                    case "IF_HASSUBMODAL", "IF_HASSUBOVERLAY" -> {
                        ints[0] = hash;
                        Integer actual = subs.get(hash);
                        ints[1] = actual != null && random.nextInt(3) != 0 ? actual : SUB_IDS[random.nextInt(SUB_IDS.length)];
                    }
                    default -> {
                    }
                }
                e.intStackPtr = ints.length;
                Arrays.fill(e.intStack, 0x5a5a5a5a);
                System.arraycopy(ints, 0, e.intStack, 0, ints.length);
                e.stringStackPtr = 0;
                Arrays.fill(e.stringStack, "stale");
                e.hasIntOperand = operand == 1;
                String result = "ok";
                try {
                    dispatch(op, e);
                } catch (Throwable error) {
                    result = "error:" + error.getClass().getSimpleName();
                }
                out.append("T\t").append(w.name).append('\t').append(name).append('\t').append(operand).append('\t')
                    .append(ComponentLayerFixtures.stack(ints, ints.length)).append('\t').append('-').append('\n');
                out.append("R\t").append(result).append('\t').append(ComponentLayerFixtures.stack(e.intStack, e.intStackPtr)).append('\t').append('-').append('\t').append('-').append('\t');
                out.append(ComponentLayerFixtures.hookText(e.hookedInterface1, interfaces, w.capacity)).append('\t').append(ComponentLayerFixtures.hookText(e.hookedInterface2, interfaces, w.capacity)).append('\n');
                dump(interfaces, w.capacity, current);
                var keys = new TreeSet<String>(current.keySet());
                keys.addAll(previous.keySet());
                for (var key : keys) {
                    var now = current.get(key);
                    if (!Objects.equals(now, previous.get(key))) out.append("D\t").append(key).append('\t').append(now == null ? "-" : now).append('\n');
                }
                previous.clear();
                previous.putAll(current);
                var stat = STATS.computeIfAbsent(name, k -> new int[2]);
                stat[result.equals("ok") ? 0 : 1]++;
                total++;
            }
        }
        out.append("Z\t").append(total).append('\n');
        Files.writeString(output.resolve("components-order.native.tsv"), out.toString(), StandardCharsets.UTF_8);
        var stats = new StringBuilder();
        for (var entry : STATS.entrySet()) stats.append(entry.getKey()).append('\t').append(entry.getValue()[0]).append('\t').append(entry.getValue()[1]).append('\n');
        Files.writeString(output.resolve("components-order.stats.tsv"), stats.toString(), StandardCharsets.UTF_8);
        System.exit(0);
    }
}
