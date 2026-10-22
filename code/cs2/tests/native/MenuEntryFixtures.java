import com.jagex.game.runetek5.client.Client;
import com.jagex.game.runetek5.client.GameShell;
import com.jagex.game.runetek5.client.menu.MiniMenu;
import com.jagex.game.runetek5.client.menu.MiniMenuEntry;
import com.jagex.game.runetek5.config.rsinterface.Component;
import com.jagex.game.runetek5.config.rsinterface.Interface;
import com.jagex.game.runetek5.config.rsinterface.SubInterface;
import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.*;
import com.jagex.game.runetek5.config.rsinterface.itemcontainer.ItemContainer;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class MenuEntryFixtures {
    static final long SEED = 0x3e9e171L;
    static final int STEPS = 3000;
    static final int[] CLICKS = {57, 58, 1007, 25, 30, 57, 58, 1, 2, 24, 31, 1006, 0};
    static final int[] RAW = {0, -1, 1, 65535, 65536, 0x7fff0000, Integer.MIN_VALUE, (3 << 16) | 0xffff, (9 << 16), (2 << 16) | 17};
    static final Map<String, int[]> STATS = new TreeMap<>();

    static Field field(Class<?> type, String name) throws Exception {
        for (var c = type; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(name);
    }

    static void staticSet(Class<?> type, String name, Object value) throws Exception {
        var f = field(type, name);
        f.setAccessible(true);
        f.set(null, value);
    }

    static String entryText(MiniMenuEntry e) {
        return e.clickType + "\t" + e.param0 + "\t" + e.param1 + "\t" + e.itemId;
    }

    static MiniMenuEntry entry(int click, int p0, int p1, int item) {
        return new MiniMenuEntry("a", "", 0, click, item, 0L, p0, p1, false, false, 0L, true);
    }

    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        var out = new StringBuilder();
        var random = new Random(SEED);
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
            var tmp = new StringBuilder();
            ComponentLayerFixtures.build(w, tmp, interfaces, loaded);
            out.append(tmp.toString().replaceFirst("W\t" + w.name, "W\t" + label));
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
            var withLists = new ArrayList<Component>();
            for (var entry : w.components.entrySet()) {
                int id = entry.getKey();
                var rows = entry.getValue();
                for (int index = 0; index < rows.size(); index++) {
                    if (rows.get(index) == null) continue;
                    var c = interfaces[id].components[index];
                    if (random.nextInt(5) < 2) {
                        int n = random.nextInt(4);
                        var list = new int[n];
                        for (int i = 0; i < n; i++) list[i] = random.nextInt(6);
                        c.mouseLeaveArrayParams = list;
                        if (n > 0) withLists.add(c);
                        var text = new StringBuilder();
                        for (int i = 0; i < n; i++) text.append(i > 0 ? "," : "").append(list[i]);
                        out.append("L\t").append(id).append('\t').append(index).append('\t').append(n == 0 ? "-" : text).append('\n');
                    } else {
                        c.mouseLeaveArrayParams = null;
                    }
                }
            }
            var open = new com.jagex.game.runetek5.cache.node.IterableNodeMap<SubInterface>(8);
            staticSet(Client.class, "OPEN_INTERFACES", open);
            for (int step = 0; step < STEPS; step++) {
                if (step % 15 == 0) {
                    out.append("B\n");
                    MiniMenu.entryList.remove_all();
                    int count = random.nextInt(6);
                    for (int i = 0; i < count; i++) {
                        int p1 = random.nextInt(4) != 0 ? valid.get(random.nextInt(valid.size())) : pool.get(random.nextInt(pool.size()));
                        MiniMenu.entryList.addFirst(entry(CLICKS[random.nextInt(CLICKS.length)], random.nextInt(3) == 0 ? -1 : random.nextInt(6), p1, random.nextInt(4) - 1));
                    }
                    for (var node = MiniMenu.entryList.getFirst(); node != null; node = MiniMenu.entryList.next()) {
                        out.append("E\t").append(entryText((MiniMenuEntry) node)).append('\n');
                    }
                    MiniMenuEntry active = null;
                    var rigged = new int[] {-1, 0, 0, 0};
                    if (random.nextInt(5) != 0) {
                        int p1 = random.nextInt(4) != 0 ? valid.get(random.nextInt(valid.size())) : pool.get(random.nextInt(pool.size()));
                        int click = CLICKS[random.nextInt(CLICKS.length)];
                        int item = random.nextInt(4) - 1;
                        int p0 = random.nextInt(3) == 0 ? -1 : random.nextInt(6);
                        if (!withLists.isEmpty() && random.nextInt(5) < 3) {
                            var c = withLists.get(random.nextInt(withLists.size()));
                            p1 = c.idHash;
                            click = CLICKS[random.nextInt(5)];
                            item = random.nextInt(3);
                            p0 = random.nextInt(4);
                            rigged = new int[] {c.mouseLeaveArrayParams[random.nextInt(c.mouseLeaveArrayParams.length)], item, p0, random.nextInt(2)};
                        }
                        active = entry(click, p0, p1, item);
                    }
                    staticSet(MiniMenu.class, "activeEntry", active);
                    out.append("A\t").append(active == null ? "-" : entryText(active)).append('\n');
                    ItemContainer.CONTAINER_MAP.clear();
                    int containers = random.nextInt(5);
                    var made = new ArrayList<int[]>();
                    for (int i = 0; i < containers; i++) made.add(new int[] {random.nextInt(6), random.nextInt(2), random.nextInt(5)});
                    if (rigged[0] != -1) made.add(new int[] {rigged[0], rigged[3], 4});
                    for (var spec : made) {
                        int id = spec[0];
                        boolean other = spec[1] == 1;
                        for (int slot = 0; slot < spec[2]; slot++) {
                            int item = random.nextInt(5) - 1;
                            if (spec == made.get(made.size() - 1) && rigged[0] != -1 && slot == rigged[2]) item = rigged[1];
                            ItemContainer.setItemInContainer(id, slot, item, 1, other);
                        }
                        var container = ItemContainer.getContainer(id, other);
                        if (container == null) continue;
                        var items = new StringBuilder();
                        for (int slot = 0; slot < container.itemIds.length; slot++) items.append(slot > 0 ? "," : "").append(container.itemIds[slot]);
                        out.append("G\t").append(id).append('\t').append(other ? 1 : 0).append('\t').append(items).append('\n');
                    }
                }
                boolean menu = random.nextInt(8) != 0 ? true : false;
                staticSet(MiniMenu.class, "isOpen", menu);
                boolean invSlot = random.nextBoolean();
                String name = invSlot ? "GET_ACTIVE_MINIMENU_INVSLOT" : "MINIMENUOPEN";
                var ints = new int[invSlot ? 0 : 2];
                if (!invSlot) {
                    ints[0] = random.nextInt(5) != 0 ? valid.get(random.nextInt(valid.size())) : random.nextInt(3) == 0 ? RAW[random.nextInt(RAW.length)] : pool.get(random.nextInt(pool.size()));
                    ints[1] = random.nextInt(3) == 0 ? -1 : random.nextInt(8);
                    var listed = new ArrayList<MiniMenuEntry>();
                    for (var node = MiniMenu.entryList.getFirst(); node != null; node = MiniMenu.entryList.next()) listed.add((MiniMenuEntry) node);
                    if (!listed.isEmpty() && random.nextInt(2) == 0) {
                        var pick = listed.get(random.nextInt(listed.size()));
                        ints[0] = random.nextInt(3) == 0 ? pick.param1 : (pick.param1 >>> 16 << 16) | random.nextInt(8);
                        ints[1] = random.nextInt(3) == 0 ? -1 : pick.param0;
                    }
                }
                var e = new CS2Executor();
                Arrays.fill(e.intStack, 0x5a5a5a5a);
                e.intStackPtr = ints.length;
                System.arraycopy(ints, 0, e.intStack, 0, ints.length);
                e.stringStackPtr = 0;
                String result = "ok";
                try {
                    if (invSlot) PlayerWorldOps.INSTANCE.dispatch(ClientScriptOpCode.GET_ACTIVE_MINIMENU_INVSLOT, e);
                    else QuickChatDisplayOps.INSTANCE.dispatch(ClientScriptOpCode.MINIMENUOPEN, e);
                } catch (Throwable error) {
                    result = "error:" + error.getClass().getSimpleName();
                }
                out.append("T\t").append(label).append('\t').append(name).append('\t').append(menu ? 1 : 0).append('\t').append(ComponentLayerFixtures.stack(ints, ints.length)).append('\n');
                out.append("R\t").append(result).append('\t').append(ComponentLayerFixtures.stack(e.intStack, e.intStackPtr)).append('\n');
                STATS.computeIfAbsent(name, k -> new int[2])[result.equals("ok") ? 0 : 1]++;
                total++;
            }
        }
        out.append("Z\t").append(total).append('\n');
        Files.writeString(output.resolve("menu-entry.native.tsv"), out.toString(), StandardCharsets.UTF_8);
        var stats = new StringBuilder();
        for (var entry : STATS.entrySet()) stats.append(entry.getKey()).append('\t').append(entry.getValue()[0]).append('\t').append(entry.getValue()[1]).append('\n');
        Files.writeString(output.resolve("menu-entry.stats.tsv"), stats.toString(), StandardCharsets.UTF_8);
        System.exit(0);
    }
}
