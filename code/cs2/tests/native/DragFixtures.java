import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.client.Client;
import com.jagex.game.runetek5.client.GameShell;
import com.jagex.game.runetek5.client.menu.MiniMenu;
import com.jagex.game.runetek5.config.rsinterface.Component;
import com.jagex.game.runetek5.config.rsinterface.IFEvents;
import com.jagex.game.runetek5.config.rsinterface.Interface;
import com.jagex.game.runetek5.config.rsinterface.SubInterface;
import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.*;
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceRenderer;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class DragFixtures {
    static final long SEED = 0x6a6a6a5L;
    static final int STEPS = 3000;
    static final int[] OFFSETS = {0, 1, -1, 5, 100, -100, 640, Integer.MAX_VALUE, Integer.MIN_VALUE};
    static final Map<String, int[]> STATS = new TreeMap<>();
    static final Component FRAME = new Component();

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

    static String key(Component c) {
        return c == null ? "-" : c == FRAME ? "F" : c.idHash + ":" + c.slotId;
    }

    static List<Component> all(Interface[] interfaces, int capacity) {
        var list = new ArrayList<Component>();
        for (int id = 0; id < capacity; id++) {
            var inter = interfaces[id];
            if (inter == null) continue;
            for (var c : inter.components) {
                if (c == null) continue;
                list.add(c);
                if (c.slotChildren != null) {
                    for (var child : c.slotChildren) if (child != null) list.add((Component) (Object) child);
                }
            }
        }
        return list;
    }

    static String state() throws Exception {
        return key((Component) staticGet(Client.class, "draggingComponent")) + "\t" + key((Component) staticGet(Client.class, "selectedComponent")) + "\t"
            + staticGet(Client.class, "mouseX") + "\t" + staticGet(Client.class, "mouseY") + "\t" + ((Boolean) staticGet(Client.class, "isDragThresholdMet") ? 1 : 0)
            + "\t" + staticGet(InterfaceRenderer.class, "dragTickCount") + "\t" + ((Boolean) staticGet(MiniMenu.class, "isOpen") ? 1 : 0);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        var out = new StringBuilder();
        var random = new Random(SEED);
        staticSet(Client.class, "frameComponent", FRAME);
        Client.gameState = com.jagex.game.runetek5.client.GameState.LOGGED_INGAME;
        var labels = List.of("main", "dense", "mainb", "denseb");
        int total = 0;
        for (int worldIndex = 0; worldIndex < 4; worldIndex++) {
            var w = worldIndex % 2 == 0 ? ComponentLayerFixtures.worldMain() : ComponentLayerFixtures.worldDense();
            var label = labels.get(worldIndex);
            var interfaces = new Interface[w.capacity];
            var loaded = new boolean[w.capacity];
            Interface.INTERFACES = interfaces;
            Interface.INTERFACES_LOADED = loaded;
            GameShell.screenCanvasWidth = w.width;
            GameShell.screenCanvasHeight = w.height * ComponentLayerFixtures.inverse(-969250379);
            Client.Companion.setLastRenderedCycle(-2);
            Arrays.fill(Client.Companion.getIS_COMPONENT_ON_TOP(), false);
            Client.Companion.getICOMPONENT_SETTINGS_SLOTS().clear();
            var tmp = new StringBuilder();
            ComponentLayerFixtures.build(w, tmp, interfaces, loaded);
            out.append(tmp.toString().replaceFirst("W\t" + w.name, "W\t" + label));
            var e = new CS2Executor();
            e.hookedInterface1.reset();
            e.hookedInterface2.reset();
            var valid = new ArrayList<Integer>();
            for (var entry : w.components.entrySet()) {
                for (var row : entry.getValue()) if (row != null) valid.add(Integer.parseInt(row[3]));
            }
            var pool = new ArrayList<Integer>();
            for (var hash : ComponentLayerFixtures.hashPool(w)) {
                boolean ok = true;
                for (int v : new int[] {hash >>> 16, hash >> 16}) if (v >= 0 && v < w.capacity && !loaded[v]) ok = false;
                if (ok) pool.add(hash);
            }
            var loadedIds = new ArrayList<>(w.components.keySet());
            for (int secondary = 0; secondary < 2; secondary++) {
                var hook = secondary == 0 ? e.hookedInterface1 : e.hookedInterface2;
                int id = loadedIds.get(random.nextInt(loadedIds.size()));
                var rows = w.components.get(id);
                int index = rows.isEmpty() ? 0 : random.nextInt(rows.size());
                if (!rows.isEmpty() && rows.get(index) != null) {
                    hook.inter = interfaces[id];
                    hook.comp = interfaces[id].components[index];
                    out.append("K\t").append(secondary).append('\t').append(id).append('\t').append(index).append('\n');
                }
            }
            var every = all(interfaces, w.capacity);
            for (var entry : w.components.entrySet()) {
                int id = entry.getKey();
                var rows = entry.getValue();
                for (int index = 0; index < rows.size(); index++) {
                    if (rows.get(index) == null) continue;
                    var c = interfaces[id].components[index];
                    int roll = random.nextInt(10);
                    String settingsText;
                    if (roll == 0) {
                        c.targetParams = null;
                        settingsText = "-\t0";
                    } else {
                        int h = random.nextInt();
                        h &= ~(0x7 << 18);
                        h |= (random.nextInt(5) == 0 ? random.nextInt(8) : random.nextInt(4)) << 18;
                        if (random.nextInt(6) == 0) h |= 1 << 23;
                        else h &= ~(1 << 23);
                        c.targetParams = new IFEvents(h, 0);
                        settingsText = h + "\t0";
                    }
                    c.depthChildOverride = null;
                    String over = "-";
                    if (random.nextInt(4) == 0) {
                        var pick = every.get(random.nextInt(every.size()));
                        c.depthChildOverride = pick;
                        over = pick.idHash + ":" + pick.slotId;
                    }
                    out.append("S\t").append(id).append('\t').append(index).append('\t').append(settingsText).append('\t').append(over).append('\n');
                }
            }
            for (int n = 0; n < 10; n++) {
                int hash = valid.get(random.nextInt(valid.size()));
                int slot = random.nextInt(3) == 0 ? -1 : random.nextInt(5);
                int h = random.nextInt();
                h &= ~(0x7 << 18);
                h |= random.nextInt(4) << 18;
                if (random.nextInt(6) == 0) h |= 1 << 23;
                else h &= ~(1 << 23);
                var node = new IFEvents(h, 0);
                Client.Companion.getICOMPONENT_SETTINGS_SLOTS().put(node, ((long) hash << 32) + slot);
                out.append("O\t").append(hash).append('\t').append(slot).append('\t').append(h).append('\t').append(0).append('\n');
            }
            var open = new IterableNodeMap<SubInterface>(8);
            var used = new HashSet<Integer>();
            int openCount = random.nextInt(6);
            for (int n = 0; n < openCount; n++) {
                int parent = valid.get(random.nextInt(valid.size()));
                if (!used.add(parent)) continue;
                int sub = loadedIds.get(random.nextInt(loadedIds.size()));
                open.put(new SubInterface(sub, 3), (long) parent);
                out.append("V\t").append(parent).append('\t').append(sub).append('\n');
            }
            staticSet(Client.class, "OPEN_INTERFACES", open);
            for (int step = 0; step < STEPS; step++) {
                boolean cc = random.nextBoolean();
                String name = cc ? "CC_DRAGPICKUP" : "IF_DRAGPICKUP";
                var op = ClientScriptOpCode.valueOf(name);
                int operand = random.nextInt(2);
                int count = cc ? 2 : 3;
                var ints = new int[count];
                ints[0] = OFFSETS[random.nextInt(OFFSETS.length)];
                ints[1] = OFFSETS[random.nextInt(OFFSETS.length)];
                if (!cc) ints[2] = random.nextInt(5) != 0 ? valid.get(random.nextInt(valid.size())) : pool.get(random.nextInt(pool.size()));
                Component dragging = random.nextInt(4) == 0 ? every.get(random.nextInt(every.size())) : null;
                Component selected = random.nextInt(3) == 0 ? null : random.nextInt(3) == 0 ? FRAME : every.get(random.nextInt(every.size()));
                staticSet(Client.class, "draggingComponent", dragging);
                staticSet(Client.class, "selectedComponent", selected);
                staticSet(Client.class, "mouseX", -5);
                staticSet(Client.class, "mouseY", -6);
                staticSet(Client.class, "isDragThresholdMet", true);
                staticSet(InterfaceRenderer.class, "dragTickCount", 9);
                staticSet(MiniMenu.class, "isOpen", random.nextInt(6) == 0);
                var before = state();
                e.intStackPtr = count;
                Arrays.fill(e.intStack, 0x5a5a5a5a);
                System.arraycopy(ints, 0, e.intStack, 0, count);
                e.hasIntOperand = operand == 1;
                String result = "ok";
                try {
                    DialogOps.INSTANCE.dispatch(op, e);
                } catch (Throwable error) {
                    result = "error:" + error.getClass().getSimpleName();
                }
                out.append("T\t").append(label).append('\t').append(name).append('\t').append(operand).append('\t')
                    .append(ComponentLayerFixtures.stack(ints, count)).append('\t').append(before).append('\n');
                out.append("R\t").append(result).append('\t').append(ComponentLayerFixtures.stack(e.intStack, e.intStackPtr)).append('\t').append(state()).append('\n');
                var stat = STATS.computeIfAbsent(name, k -> new int[2]);
                stat[result.equals("ok") ? 0 : 1]++;
                total++;
            }
        }
        out.append("Z\t").append(total).append('\n');
        Files.writeString(output.resolve("components-drag.native.tsv"), out.toString(), StandardCharsets.UTF_8);
        var stats = new StringBuilder();
        for (var entry : STATS.entrySet()) stats.append(entry.getKey()).append('\t').append(entry.getValue()[0]).append('\t').append(entry.getValue()[1]).append('\n');
        Files.writeString(output.resolve("components-drag.stats.tsv"), stats.toString(), StandardCharsets.UTF_8);
        System.exit(0);
    }
}
