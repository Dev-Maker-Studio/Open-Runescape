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

public final class ComponentQueryFixtures {
    static final String[] NAMES = {
        "CC_GETPARENTLAYER", "IF_GETPARENTLAYER", "IF_GET_GAMESCREEN",
        "IF_DEBUG_GETOPENIFCOUNT", "IF_DEBUG_GETOPENIFID", "IF_DEBUG_GETNAME",
        "IF_DEBUG_GETCOMCOUNT", "IF_DEBUG_GETCOMNAME", "IF_DEBUG_GETSERVERTRIGGERS"
    };
    static final int[] WEIGHTS = {4, 6, 2, 2, 4, 4, 3, 5, 5};
    static final int[] SUB_IDS = {2, 3, 1, 4, 6, 5, 99, 0, -1, 7, 2, 3};
    static final String[] LABELS = {null, "", "foo:bar", "nocolon", ":x", "ab:c:d", "é€:z", "toolbar:button"};
    static final int[] EDGE = {Integer.MAX_VALUE, Integer.MIN_VALUE, -2, 65535, 65536, 70000};
    static final long SEED = 0x727c5227L;
    static final int STEPS = 3500;
    static final Map<String, int[]> STATS = new TreeMap<>();

    static int argumentCount(String name) {
        return switch (name) {
            case "IF_GETPARENTLAYER", "IF_DEBUG_GETOPENIFID", "IF_DEBUG_GETNAME", "IF_DEBUG_GETCOMCOUNT" -> 1;
            case "IF_DEBUG_GETCOMNAME", "IF_DEBUG_GETSERVERTRIGGERS" -> 2;
            default -> 0;
        };
    }

    static void dispatch(ClientScriptOpCode op, CS2Executor e) {
        String name = op.name();
        if (name.equals("CC_GETPARENTLAYER")) {
            ComponentQueryOps.dispatch(op, e);
        } else if (name.equals("IF_GETPARENTLAYER")) {
            InterfaceQueryOps.dispatch(op, e);
        } else if (name.equals("IF_GET_GAMESCREEN")) {
            DetailCapabilityOps.dispatch(op, e);
        } else {
            WorldMapUserDetailOps.dispatch(op, e);
        }
    }

    static int pick(Random random, int capacity) {
        int roll = random.nextInt(10);
        if (roll < 7) return random.nextInt(capacity + 1);
        if (roll < 8) return -1;
        return EDGE[random.nextInt(EDGE.length)];
    }

    static int componentIndex(Random random) {
        int roll = random.nextInt(10);
        if (roll < 7) return random.nextInt(11);
        if (roll < 8) return -1;
        return EDGE[random.nextInt(EDGE.length)];
    }

    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        var out = new StringBuilder();
        var random = new Random(SEED);
        var weights = new ArrayList<String>();
        for (int i = 0; i < NAMES.length; i++) for (int n = 0; n < WEIGHTS[i]; n++) weights.add(NAMES[i]);
        var variants = new Object[][] {
            {ComponentLayerFixtures.worldMain(), -1, false},
            {ComponentLayerFixtures.worldMain(), 5, true},
            {ComponentLayerFixtures.worldDense(), -1, true},
            {ComponentLayerFixtures.worldDense(), 7, false},
        };
        int total = 0;
        int variant = 0;
        for (var entry : variants) {
            var w = (ComponentLayerFixtures.World) entry[0];
            int base = (Integer) entry[1];
            boolean screen = (Boolean) entry[2];
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
            Client.BASE_WINDOW_ID = base;
            out.append("B\t").append(base).append('\n');
            var valid = new ArrayList<Integer>();
            for (var entryRows : w.components.entrySet()) {
                var rows = entryRows.getValue();
                for (int i = 0; i < rows.size(); i++) if (rows.get(i) != null) valid.add((entryRows.getKey() << 16) | i);
            }
            var pool = ComponentLayerFixtures.hashPool(w);
            for (int hash : valid) {
                var c = interfaces[hash >> 16].components[hash & 0xffff];
                String label = LABELS[random.nextInt(LABELS.length)];
                int triggers = random.nextInt(3) == 0 ? 0 : random.nextInt();
                c.name = label;
                c.serverTriggers = triggers;
                out.append("Q\t").append(hash >> 16).append('\t').append(hash & 0xffff).append('\t')
                    .append(label == null ? "null" : "hex:" + NativeFixtures.hex(label)).append('\t').append(triggers).append('\n');
            }
            Client.GAME_SCREEN_INTERFACE = null;
            if (screen) {
                int hash = valid.get(random.nextInt(valid.size()));
                Client.GAME_SCREEN_INTERFACE = interfaces[hash >> 16].components[hash & 0xffff];
                out.append("G\t").append(hash >> 16).append('\t').append(hash & 0xffff).append('\n');
            }
            var subs = new ArrayList<Integer>(valid);
            Collections.shuffle(subs, random);
            int opened = 0;
            for (int hash : subs) {
                if (random.nextInt(2) == 0 || opened < 2) {
                    int sub = SUB_IDS[random.nextInt(SUB_IDS.length)];
                    int overlay = random.nextInt(2);
                    open.put(new SubInterface(sub, overlay), (long) hash);
                    opened++;
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
            int openCount = opened + (base != -1 ? 1 : 0);
            for (int step = 0; step < STEPS; step++) {
                var name = weights.get(random.nextInt(weights.size()));
                var op = ClientScriptOpCode.valueOf(name);
                int operand = random.nextInt(2);
                if (name.equals("CC_GETPARENTLAYER") && random.nextInt(3) == 0) {
                    var hook = operand == 1 ? e.hookedInterface2 : e.hookedInterface1;
                    var ids = new ArrayList<>(w.components.keySet());
                    int id = ids.get(random.nextInt(ids.size()));
                    var rows = w.components.get(id);
                    int index = random.nextInt(rows.size() + 1) % Math.max(rows.size(), 1);
                    if (!rows.isEmpty() && rows.get(index) != null) {
                        hook.inter = interfaces[id];
                        hook.comp = interfaces[id].components[index];
                        out.append("H\t").append(operand).append('\t').append(id).append('\t').append(index).append('\n');
                    }
                }
                int count = argumentCount(name);
                var ints = new int[count];
                switch (name) {
                    case "IF_GETPARENTLAYER" -> ints[0] = random.nextInt(4) != 0 ? valid.get(random.nextInt(valid.size())) : pool.get(random.nextInt(pool.size()));
                    case "IF_DEBUG_GETOPENIFID" -> ints[0] = random.nextInt(8) != 0 ? random.nextInt(openCount + 3) - (random.nextInt(8) == 0 ? 2 : 0) : EDGE[random.nextInt(EDGE.length)];
                    case "IF_DEBUG_GETNAME", "IF_DEBUG_GETCOMCOUNT" -> ints[0] = pick(random, w.capacity);
                    case "IF_DEBUG_GETCOMNAME", "IF_DEBUG_GETSERVERTRIGGERS" -> {
                        ints[0] = random.nextInt(6) != 0 && !valid.isEmpty() ? valid.get(random.nextInt(valid.size())) >> 16 : pick(random, w.capacity);
                        ints[1] = componentIndex(random);
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
                out.append("T\t").append(w.name).append(variant).append('\t').append(name).append('\t').append(operand).append('\t')
                    .append(ComponentLayerFixtures.stack(ints, ints.length)).append('\t').append('-').append('\n');
                out.append("R\t").append(result).append('\t').append(ComponentLayerFixtures.stack(e.intStack, e.intStackPtr)).append('\t')
                    .append(ComponentLayerFixtures.strings(Arrays.copyOf(e.stringStack, e.stringStack.length, String[].class), e.stringStackPtr)).append('\t').append('-').append('\t');
                out.append(ComponentLayerFixtures.hookText(e.hookedInterface1, interfaces, w.capacity)).append('\t').append(ComponentLayerFixtures.hookText(e.hookedInterface2, interfaces, w.capacity)).append('\n');
                var stat = STATS.computeIfAbsent(name, k -> new int[2]);
                stat[result.equals("ok") ? 0 : 1]++;
                total++;
            }
            variant++;
        }
        out.append("Z\t").append(total).append('\n');
        Files.writeString(output.resolve("components-query.native.tsv"), out.toString(), StandardCharsets.UTF_8);
        var stats = new StringBuilder();
        for (var entry : STATS.entrySet()) stats.append(entry.getKey()).append('\t').append(entry.getValue()[0]).append('\t').append(entry.getValue()[1]).append('\n');
        Files.writeString(output.resolve("components-query.stats.tsv"), stats.toString(), StandardCharsets.UTF_8);
        System.exit(0);
    }
}
