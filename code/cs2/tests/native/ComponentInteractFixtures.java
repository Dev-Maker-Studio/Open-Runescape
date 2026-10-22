import com.jagex.game.runetek5.client.Client;
import com.jagex.game.runetek5.client.GameShell;
import com.jagex.game.runetek5.client.GameState;
import com.jagex.game.runetek5.client.menu.MiniMenu;
import com.jagex.game.runetek5.client.menu.MiniMenuBuilderKt;
import com.jagex.game.runetek5.config.rsinterface.Component;
import com.jagex.game.runetek5.config.rsinterface.IFEvents;
import com.jagex.game.runetek5.config.rsinterface.Interface;
import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.*;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity;
import com.jagex.game.runetek5.network.packet.TcpPacket;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class ComponentInteractFixtures {
    static final String[] NAMES = {
        "IF_CREATE", "IF_DELETEALL", "CC_LOADENTITYMODEL", "IF_DEBUG_TARGET",
        "IF_DEBUG_BUTTON1", "IF_DEBUG_BUTTON2", "IF_DEBUG_BUTTON3", "IF_DEBUG_BUTTON4", "IF_DEBUG_BUTTON5",
        "IF_DEBUG_BUTTON6", "IF_DEBUG_BUTTON7", "IF_DEBUG_BUTTON8", "IF_DEBUG_BUTTON9", "IF_DEBUG_BUTTON10"
    };
    static final int[] ITEMS = {-1, -1, 0, 1, 995, 65535, 65536, -2, 123456, Integer.MIN_VALUE, 4000};
    static final int[] CURSORS = {-1, -1, 0, 5, 100, 65535, -7};
    static final int[] INTERFACE_IDS = {-1, 0, 1, 2, 3, 7, 100, 65535, 99999};
    static final int[] SLOT_VALUES = {-1, -1, -1, 0, 0, 1, 2, 3, 4, 5, 70000, -2, Integer.MAX_VALUE, Integer.MIN_VALUE, 255, 256, 32768, 65535, 65536};
    static final int[] CREATE_SLOTS = {-1, 0, 1, 2, 3, 4, 5, 8, 20, 70000, -2, Integer.MAX_VALUE, Integer.MIN_VALUE};
    static final long SEED = 0x7273c0deL;
    static final int STEPS = 4500;
    static final Map<String, int[]> STATS = new TreeMap<>();
    static final List<IFEvents> INJECTED = new ArrayList<>();
    static final Map<String, String> PREVIOUS = new HashMap<>();
    static final Map<String, String> CURRENT = new HashMap<>();

    static String[] selection() {
        return new String[] {
            Client.interfaceSelectionActive ? "1" : "0", "" + Client.Companion.getSelectedComponentHash(), "" + Client.Companion.getSelectedSlotId(),
            "" + MiniMenuBuilderKt.USE_OPTIONS_FLAGS, "" + MiniMenu.USE_INTERFACE_ID, "" + Client.Companion.getSelectedItemId(),
            "" + MiniMenu.useOnCursor, "" + Client.Companion.getDefaultMenuCursor()
        };
    }

    static void setSelection(String[] s) {
        Client.interfaceSelectionActive = s[0].equals("1");
        Client.Companion.setSelectedComponentHash(Integer.parseInt(s[1]));
        Client.Companion.setSelectedSlotId(Integer.parseInt(s[2]));
        MiniMenuBuilderKt.USE_OPTIONS_FLAGS = Integer.parseInt(s[3]);
        MiniMenu.USE_INTERFACE_ID = Integer.parseInt(s[4]);
        Client.Companion.setSelectedItemId(Integer.parseInt(s[5]));
        MiniMenu.useOnCursor = Integer.parseInt(s[6]);
        Client.Companion.setDefaultMenuCursor(Integer.parseInt(s[7]));
    }

    static String selectionLine(String[] s) {
        return "M\t" + String.join("\t", s) + "\n";
    }

    static boolean hashOk(int h, int capacity, boolean[] loaded) {
        for (int v : new int[] {h >>> 16, h >> 16}) {
            if (v >= 0 && v < capacity && !loaded[v]) return false;
        }
        return true;
    }

    static int settingsHash(Random random) {
        int h = random.nextInt();
        for (int bit = 1; bit <= 10; bit++) {
            if (random.nextInt(5) < 3) h |= 1 << bit;
            else h &= ~(1 << bit);
        }
        return h;
    }

    static int pick(Random random, int[] values) {
        return values[random.nextInt(values.length)];
    }

    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        var theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        var unsafe = (sun.misc.Unsafe) theUnsafe.get(null);
        var out = new StringBuilder();
        var random = new Random(SEED);
        var weights = new ArrayList<String>();
        for (var name : NAMES) {
            weights.add(name);
            if (name.equals("IF_CREATE") || name.equals("IF_DEBUG_TARGET")) {
                weights.add(name);
                weights.add(name);
            }
        }
        var labels = List.of("main", "dense", "mainb", "denseb");
        var total = 0;
        var consoleLines = new String[500];
        Arrays.fill(consoleLines, "");
        com.jagex.game.runetek5.client.input.console.Console.displayedOutput = consoleLines;
        Client.gameState = GameState.LOGGED_INGAME;
        var context = Client.GAME_CONNECTION_CONTEXT;
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
            for (var hash : ComponentLayerFixtures.hashPool(w)) if (hashOk(hash, w.capacity, loaded)) pool.add(hash);
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
            for (var entry : w.components.entrySet()) {
                int id = entry.getKey();
                var rows = entry.getValue();
                for (int index = 0; index < rows.size(); index++) {
                    if (rows.get(index) == null) continue;
                    if (random.nextInt(4) == 0) continue;
                    var c = interfaces[id].components[index];
                    int roll = random.nextInt(10);
                    String settingsText;
                    if (roll == 0) {
                        c.targetParams = null;
                        settingsText = "-\t0";
                    } else {
                        int h = settingsHash(random);
                        int iface = pick(random, INTERFACE_IDS);
                        c.targetParams = new IFEvents(h, iface);
                        settingsText = h + "\t" + iface;
                    }
                    c.containerItemId = pick(random, ITEMS);
                    c.targetOverCursor = pick(random, CURSORS);
                    c.targetLeaveCursor = pick(random, CURSORS);
                    out.append("S\t").append(id).append('\t').append(index).append('\t').append(settingsText).append('\t')
                        .append(c.containerItemId).append('\t').append(c.targetOverCursor).append('\t').append(c.targetLeaveCursor).append('\n');
                }
            }
            for (int n = 0; n < 14; n++) {
                int hash = valid.get(random.nextInt(valid.size()));
                int slot = random.nextInt(3) == 0 ? -1 : random.nextInt(5);
                int h = settingsHash(random);
                int iface = pick(random, INTERFACE_IDS);
                var node = new IFEvents(h, iface);
                Client.Companion.getICOMPONENT_SETTINGS_SLOTS().put(node, ((long) hash << 32) + slot);
                out.append("O\t").append(hash).append('\t').append(slot).append('\t').append(h).append('\t').append(iface).append('\n');
            }
            var inspected = selection();
            setSelection(new String[] {"0", "0", "-1", "0", "0", "-1", "-1", "-1"});
            out.append(selectionLine(selection()));
            ComponentLayerFixtures.dump(interfaces, w.capacity, PREVIOUS);
            var itemLengths = new HashMap<String, String>();
            for (int step = 0; step < STEPS; step++) {
                var name = weights.get(random.nextInt(weights.size()));
                var op = ClientScriptOpCode.valueOf(name);
                int operand = random.nextInt(2);
                int count = switch (name) {
                    case "IF_CREATE", "IF_DEBUG_TARGET" -> 3;
                    case "IF_DELETEALL" -> 1;
                    case "CC_LOADENTITYMODEL" -> 2;
                    default -> 3;
                };
                var ints = new int[count];
                int hash = random.nextInt(5) != 0 ? valid.get(random.nextInt(valid.size())) : pool.get(random.nextInt(pool.size()));
                if (name.equals("IF_CREATE") || name.equals("IF_DELETEALL") || name.equals("CC_LOADENTITYMODEL")) {
                    ints[0] = random.nextInt(8) == 0 ? (random.nextInt(2) == 0 ? hash & 0xffff : random.nextInt()) : hash;
                    if (name.equals("IF_CREATE")) {
                        ints[1] = random.nextInt(4) != 0 ? new int[] {1, 4, 5, 9}[random.nextInt(4)] : pick(random, new int[] {0, 1, 4, 5, 9, 10, -1, 255, 256, 3, 7});
                        ints[2] = random.nextInt(4) != 0 ? random.nextInt(5) : pick(random, CREATE_SLOTS);
                    } else if (name.equals("CC_LOADENTITYMODEL")) {
                        ints[1] = random.nextInt(2) == 0 ? 0 : random.nextInt();
                    }
                } else {
                    int wide = random.nextInt(4) == 0 ? hash >> 16 : hash >>> 16;
                    int low = random.nextInt(6) == 0 ? (random.nextInt(2) == 0 ? random.nextInt(8) : random.nextInt(70000)) : hash & 0xffff;
                    int actual = (wide << 16) | low;
                    if (!hashOk(actual, w.capacity, loaded)) {
                        wide = hash >>> 16;
                        low = hash & 0xffff;
                    }
                    ints[0] = wide;
                    ints[1] = low;
                    ints[2] = random.nextInt(3) == 0 ? -1 : random.nextInt(10) == 0 ? pick(random, SLOT_VALUES) : random.nextInt(5);
                    if (name.equals("IF_DEBUG_TARGET") && random.nextInt(2) == 0) {
                        setSelection(new String[] {
                            random.nextInt(3) == 0 ? "0" : "1", "" + (random.nextInt(4) != 0 ? valid.get(random.nextInt(valid.size())) : pool.get(random.nextInt(pool.size()))),
                            "" + (random.nextInt(3) == 0 ? -1 : random.nextInt(5)), "" + random.nextInt(128), "" + pick(random, INTERFACE_IDS),
                            "" + pick(random, ITEMS), "" + pick(random, CURSORS), "" + pick(random, CURSORS)
                        });
                        out.append(selectionLine(selection()));
                    }
                }
                String entity;
                int bind = random.nextInt(20);
                if (bind < 14) {
                    int id = loadedIds.get(random.nextInt(loadedIds.size()));
                    if (random.nextInt(3) != 0) {
                        var rows = w.components.get(id);
                        for (int tries = 0; tries < 6 && (rows.isEmpty() || rows.get(0) == null); tries++) {
                            id = loadedIds.get(random.nextInt(loadedIds.size()));
                            rows = w.components.get(id);
                        }
                    }
                    var player = (PlayerEntity) unsafe.allocateInstance(PlayerEntity.class);
                    player.inter = interfaces[id];
                    e.currentEntity = player;
                    entity = "" + id;
                } else if (bind < 17) {
                    var player = (PlayerEntity) unsafe.allocateInstance(PlayerEntity.class);
                    player.inter = null;
                    e.currentEntity = player;
                    entity = "u";
                } else {
                    e.currentEntity = null;
                    entity = "-";
                }
                var selectionBefore = selection();
                e.intStackPtr = count;
                Arrays.fill(e.intStack, 0x5a5a5a5a);
                System.arraycopy(ints, 0, e.intStack, 0, count);
                e.stringStackPtr = 0;
                Arrays.fill(e.stringStack, "stale");
                e.hasIntOperand = operand == 1;
                while (context.queuedPackets.popHead() != null) {
                }
                String result = "ok";
                try {
                    if (name.equals("IF_CREATE") || name.equals("IF_DELETEALL") || name.equals("CC_LOADENTITYMODEL")) {
                        DetailCapabilityOps.dispatch(op, e);
                    } else {
                        WorldMapUserDetailOps.dispatch(op, e);
                    }
                } catch (Throwable error) {
                    result = "error:" + error.getClass().getSimpleName();
                }
                var packetText = "-";
                var packets = new ArrayList<TcpPacket>();
                for (var node = context.queuedPackets.popHead(); node != null; node = context.queuedPackets.popHead()) packets.add((TcpPacket) node);
                if (packets.size() > 1) throw new IllegalStateException("packets " + packets.size());
                if (packets.size() == 1) {
                    var packet = packets.get(0);
                    var b = packet.buffer.buffer;
                    int option = Integer.parseInt(packet.packet.name().substring(5));
                    int packetHash = (b[1] & 255) | (b[2] & 255) << 8 | (b[3] & 255) << 16 | (b[4] & 255) << 24;
                    int item = ((b[5] & 255) << 8) | (b[6] & 255);
                    int slot = ((b[7] & 255) << 8) | ((b[8] - 128) & 255);
                    packetText = option + "\t" + packetHash + "\t" + item + "\t" + slot;
                    if (packet.capacity != 9) throw new IllegalStateException("capacity " + packet.capacity);
                }
                out.append("T\t").append(label).append('\t').append(name).append('\t').append(operand).append('\t')
                    .append(ComponentLayerFixtures.stack(ints, count)).append('\t').append(entity).append('\n');
                out.append("R\t").append(result).append('\t').append(ComponentLayerFixtures.stack(e.intStack, e.intStackPtr)).append('\t')
                    .append("-").append('\t').append("-").append('\t');
                out.append(ComponentLayerFixtures.hookText(e.hookedInterface1, interfaces, w.capacity)).append('\t')
                    .append(ComponentLayerFixtures.hookText(e.hookedInterface2, interfaces, w.capacity)).append('\n');
                out.append("P\t").append(packetText).append('\n');
                out.append("X\t").append(String.join("\t", selection())).append('\n');
                ComponentLayerFixtures.dump(interfaces, w.capacity, CURRENT);
                for (int id = 0; id < w.capacity; id++) {
                    var inter = interfaces[id];
                    if (inter == null) continue;
                    for (int index = 0; index < inter.components.length; index++) {
                        var c = inter.components[index];
                        if (c == null) continue;
                        CURRENT.put("n" + id + "." + index, c.itemSlots == null ? "-" : "" + c.itemSlots.length);
                    }
                }
                var keys = new TreeSet<String>(CURRENT.keySet());
                keys.addAll(PREVIOUS.keySet());
                for (var key : keys) {
                    var now = CURRENT.get(key);
                    if (!Objects.equals(now, PREVIOUS.get(key))) out.append("D\t").append(key).append('\t').append(now == null ? "-" : now).append('\n');
                }
                PREVIOUS.clear();
                PREVIOUS.putAll(CURRENT);
                var stat = STATS.computeIfAbsent(name, k -> new int[2]);
                stat[result.equals("ok") ? 0 : 1]++;
                total++;
            }
            Client.Companion.getICOMPONENT_SETTINGS_SLOTS().clear();
        }
        out.append("Z\t").append(total).append('\n');
        Files.writeString(output.resolve("components-interact.native.tsv"), out.toString(), StandardCharsets.UTF_8);
        var stats = new StringBuilder();
        for (var entry : STATS.entrySet()) stats.append(entry.getKey()).append('\t').append(entry.getValue()[0]).append('\t').append(entry.getValue()[1]).append('\n');
        Files.writeString(output.resolve("components-interact.stats.tsv"), stats.toString(), StandardCharsets.UTF_8);
        System.exit(0);
    }
}
