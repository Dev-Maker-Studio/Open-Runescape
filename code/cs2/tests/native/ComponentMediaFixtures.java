import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.cache.node.lru.CacheableNode;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.client.Client;
import com.jagex.game.runetek5.client.GameShell;
import com.jagex.game.runetek5.client.GameState;
import com.jagex.game.runetek5.client.periodic.PulseEvent;
import com.jagex.game.runetek5.client.periodic.PulseEventHandler;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.TypeResult;
import com.jagex.game.runetek5.config.item.ItemType;
import com.jagex.game.runetek5.config.item.ItemTypeList;
import com.jagex.game.runetek5.config.npc.NPCType;
import com.jagex.game.runetek5.config.npc.NpcTypeList;
import com.jagex.game.runetek5.config.rsinterface.Component;
import com.jagex.game.runetek5.config.rsinterface.HookRequest;
import com.jagex.game.runetek5.config.rsinterface.IFEvents;
import com.jagex.game.runetek5.config.rsinterface.Interface;
import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.*;
import com.jagex.game.runetek5.config.sequence.SeqType;
import com.jagex.game.runetek5.config.sequence.SeqTypeList;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.graphics.font.FontCombo;
import com.jagex.game.runetek5.graphics.font.FontMetrics;
import com.jagex.game.runetek5.graphics.model.MediaType;
import com.jagex.game.runetek5.graphics.sprite.NativeSprite;
import com.jagex.game.runetek5.graphics.sprite.StaticMedia;
import com.jagex.game.runetek5.network.packet.TcpPacket;
import com.jagex.game.runetek5.staticmethods.StaticMethods;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class ComponentMediaFixtures {
    static final String[] BASES = {
        "GETFONTMETRICS", "GETFONTGRAPHIC", "GETGRAPHICDIMENSIONS", "GETINVOBJECT", "GETINVCOUNT", "CALLONRESIZE",
        "GETCHARINDEXATPOS", "GETCHARPOSATINDEX", "NPC_SETCUSTOMBODYMODEL", "NPC_SETCUSTOMHEADMODEL", "NPC_SETCUSTOMRECOL",
        "NPC_SETCUSTOMRETEX", "SETMODELANIM", "SETVIDEO_GRAPHIC", "SETVIDEO_TEXT", "SETOBJECT", "SETOBJECT_NONUM",
        "SETOBJECT_WEARCOL", "SETOBJECT_WEARCOL_NONUM", "SETOBJECT_ALWAYSNUM", "SETOBJECT_WEARCOL_ALWAYSNUM",
        "SETNPCHEAD", "SETNPCMODEL", "SETPLAYERMODEL", "SETPLAYERHEAD_SELF", "SETPLAYERMODEL_SELF", "RESUME_PAUSEBUTTON"
    };
    static final int[] ITEMS = {-1, -1, 1, 2, 3, 4, 995, 0, 7, 65535, Integer.MIN_VALUE};
    static final int[] STACKS = {0, 1, 2, 99, -1, 100000, Integer.MAX_VALUE};
    static final int[] NPCS = {1, 2, 3, 4, 5, 1, 2, 3, -1, 0, 6, 99, 65536};
    static final int[] SEQS = {-1, -1, 1, 2, 3, 4, 0, 5, 999};
    static final int[] SLOTS = {0, 1, 2, 3, 4, 5, -1, 100};
    static final int[] VIDEOS = {-1, 0, 1, 5, 100000, -7};
    static final int[] PLAYERS = {0, 1, 2047, -1, 5000};
    static final int[] POSITIONS = {0, 1, 5, 20, 50, 200, -1, -5, 1000};
    static final int[] VALUES = {0, 1, 5, 100, 65535, 70000, -1, 32767, -32768};
    static final int[] FONTS = {-1, 9001, 9002, 9003, 9001, 9002, 4321, 0};
    static final int[] WIDTHS = {0, 1, 8, 47, 100, 400, -1};
    static final int[] SPACINGS = {0, 1, 5, -3, 20};
    static final int[] MEDIA = {1, 2, 2, 2, 6, 6, 6, 3, 5, 4, 8, 9, 7};
    static final int[] COUNTS = {0, 3, 9, 10, 11};
    static final int[] ITEM_IDS = {1, 2, 3, 4, 995};
    static final int[] INTERFACE_IDS = {-1, 0, 1, 2, 3, 7, 100, 65535, 99999};
    static final String[] TEXTS = {"", "a", "hello world", "Hello<br>world", "wide  text here and more words after", "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", "a<br>b<br>c", "ab cd ef gh ij kl mn"};
    static final long SEED = 0x7273c0deL ^ 0x4d454449L;
    static final int STEPS = 4500;
    static final Map<String, int[]> STATS = new TreeMap<>();
    static final Map<String, String> PREVIOUS = new HashMap<>();
    static final Map<String, String> CURRENT = new HashMap<>();

    static boolean hashOk(int h, int capacity, boolean[] loaded) {
        for (int v : new int[] {h >>> 16, h >> 16}) {
            if (v >= 0 && v < capacity && !loaded[v]) return false;
        }
        return true;
    }

    static int settingsHash(Random random) {
        int h = random.nextInt();
        for (int bit = 0; bit <= 10; bit++) {
            if (random.nextInt(5) < 3) h |= 1 << bit;
            else h &= ~(1 << bit);
        }
        return h;
    }

    static int pick(Random random, int[] values) {
        return values[random.nextInt(values.length)];
    }

    static int argumentCount(String base) {
        return switch (base) {
            case "GETCHARINDEXATPOS", "NPC_SETCUSTOMBODYMODEL", "NPC_SETCUSTOMHEADMODEL", "NPC_SETCUSTOMRECOL", "NPC_SETCUSTOMRETEX",
                "SETOBJECT", "SETOBJECT_NONUM", "SETOBJECT_WEARCOL", "SETOBJECT_WEARCOL_NONUM", "SETOBJECT_ALWAYSNUM", "SETOBJECT_WEARCOL_ALWAYSNUM" -> 2;
            case "GETCHARPOSATINDEX", "SETMODELANIM", "SETVIDEO_GRAPHIC", "SETVIDEO_TEXT", "SETNPCHEAD", "SETNPCMODEL", "SETPLAYERMODEL" -> 1;
            default -> 0;
        };
    }

    static int[] arguments(String base, Random random) {
        int count = argumentCount(base);
        var ints = new int[count];
        if (base.equals("GETCHARINDEXATPOS")) {
            ints[0] = pick(random, POSITIONS);
            ints[1] = pick(random, POSITIONS);
        } else if (base.equals("GETCHARPOSATINDEX")) {
            ints[0] = pick(random, POSITIONS);
        } else if (base.startsWith("NPC_SETCUSTOM")) {
            ints[0] = pick(random, SLOTS);
            ints[1] = pick(random, VALUES);
        } else if (base.startsWith("SETOBJECT")) {
            ints[0] = pick(random, ITEMS);
            ints[1] = pick(random, STACKS);
        } else if (base.equals("SETMODELANIM")) {
            ints[0] = pick(random, SEQS);
        } else if (base.startsWith("SETVIDEO")) {
            ints[0] = pick(random, VIDEOS);
        } else if (base.equals("SETNPCHEAD") || base.equals("SETNPCMODEL")) {
            ints[0] = pick(random, NPCS);
        } else if (base.equals("SETPLAYERMODEL")) {
            ints[0] = pick(random, PLAYERS);
        }
        return ints;
    }

    static void dispatch(ClientScriptOpCode op, CS2Executor e) {
        String name = op.name();
        if (name.equals("IF_CLOSE")) {
            DialogOps.dispatch(op, e);
        } else if (name.endsWith("RESUME_PAUSEBUTTON")) {
            ComponentLifecycleOps.dispatch(op, e);
        } else if (name.contains("NPC_SETCUSTOM")) {
            InterfaceQueryOps.dispatch(op, e);
        } else if (name.startsWith("CC_GET") || name.equals("CC_CALLONRESIZE")) {
            ComponentQueryOps.dispatch(op, e);
        } else if (name.startsWith("IF_GET") || name.equals("IF_CALLONRESIZE")) {
            InterfaceQueryOps.dispatch(op, e);
        } else if (name.startsWith("CC_")) {
            ComponentSetterOps.dispatch(op, e);
        } else {
            InterfaceSetterOps.dispatch(op, e);
        }
    }

    static String kind(int type) {
        return switch (type) {
            case 5 -> "anim";
            case 9 -> "item";
            case 17 -> "video";
            default -> ComponentLayerFixtures.eventKind(type);
        };
    }

    static String intsText(int[] values) {
        if (values == null) return "-";
        if (values.length == 0) return "";
        var sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(values[i]);
        }
        return sb.toString();
    }

    static String shortsText(short[] values) {
        if (values == null) return "-";
        if (values.length == 0) return "";
        var sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(values[i]);
        }
        return sb.toString();
    }

    static String extra(Component c) {
        var customisation = c.npcCustomisation;
        String custom = customisation == null ? "-" : customisation.uid + ":" + intsText(customisation.modelIds) + ":" + shortsText(customisation.recolorDst) + ":" + shortsText(customisation.retextureDst);
        return c.containerItemId + "\t" + c.itemStackSize + "\t" + c.renderStack.ordinal() + "\t" + c.animation + "\t" + c.videoFileId + "\t" + custom;
    }

    static void extras(Interface[] interfaces, int capacity, Map<String, String> out) {
        for (int id = 0; id < capacity; id++) {
            var inter = interfaces[id];
            if (inter == null) continue;
            for (int index = 0; index < inter.components.length; index++) {
                var c = inter.components[index];
                if (c == null) continue;
                out.put("n" + id + "." + index, c.itemSlots == null ? "-" : "" + c.itemSlots.length);
                out.put("x" + id + "." + index, extra(c));
                var children = c.slotChildren;
                if (children != null) {
                    for (int slot = 0; slot < children.length; slot++) {
                        var child = (Component) (Object) children[slot];
                        if (child != null) out.put("x" + id + "." + index + ".s" + slot, extra(child));
                    }
                }
            }
        }
        var adc = Client.activeDialogueContinue;
        out.put("x.adc", adc == null ? "-" : adc.idHash + ":" + adc.slotId);
    }

    static String spec(Object[] values) {
        var sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(',');
            if (values[i] instanceof Integer n) sb.append('i').append(n);
            else sb.append('s').append(ComponentLayerFixtures.hex((String) values[i]).replace("-", ""));
        }
        return sb.length() == 0 ? "-" : sb.toString();
    }

    static void inject(sun.misc.Unsafe unsafe, Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        unsafe.putObject(target, unsafe.objectFieldOffset(f), value);
    }

    static int[] randomInts(Random random, int bound) {
        int n = random.nextInt(bound);
        var values = new int[n];
        for (int i = 0; i < n; i++) values[i] = random.nextInt(70000) - 1000;
        return values;
    }

    static short[] randomShorts(Random random, int bound) {
        int n = random.nextInt(bound);
        var values = new short[n];
        for (int i = 0; i < n; i++) values[i] = (short) (random.nextInt(65536) - 32768);
        return values;
    }

    static String shortList(short[] values) {
        return values == null ? "-" : values.length == 0 ? "e" : shortsText(values);
    }

    static String intList(int[] values) {
        return values == null ? "-" : values.length == 0 ? "e" : intsText(values);
    }

    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        var unsafe = FontFixtures.unsafe();
        var out = new StringBuilder();
        var random = new Random(SEED);
        var names = new ArrayList<String>();
        for (var base : BASES) {
            names.add("CC_" + base);
            names.add("IF_" + base);
        }
        names.add("IF_CLOSE");
        var weights = new ArrayList<String>();
        for (var name : names) {
            weights.add(name);
            if (name.contains("SETOBJECT") || name.contains("NPC_SETCUSTOM") || name.contains("SETMODELANIM") || name.contains("RESUME") || name.contains("CALLONRESIZE")) weights.add(name);
        }
        var consoleLines = new String[500];
        Arrays.fill(consoleLines, "");
        com.jagex.game.runetek5.client.input.console.Console.displayedOutput = consoleLines;
        Client.gameState = GameState.LOGGED_INGAME;
        var context = Client.GAME_CONNECTION_CONTEXT;

        var fontBytes = new TreeMap<Integer, byte[]>();
        fontBytes.put(9001, FontFixtures.syntheticFont(1, true));
        fontBytes.put(9002, FontFixtures.syntheticFont(2, true));
        fontBytes.put(9003, FontFixtures.syntheticFont(3, false));
        var fontCombo = new FontCombo(FontFixtures.provider(Map.of()), FontFixtures.provider(Map.of()), null);
        for (var entry : fontBytes.entrySet()) {
            fontCombo.cache.put(new TypeResult(new Object(), new FontMetrics(entry.getValue())), (((long) entry.getKey()) << 1) | 1);
            out.append("G\t").append(entry.getKey()).append('\t').append(FontFixtures.hexBytes(entry.getValue())).append('\n');
        }
        StaticMethods.initializedFontcombos = fontCombo;
        var icons = new NativeSprite[FontFixtures.ICONS.length];
        for (int i = 0; i < icons.length; i++) icons[i] = new FontFixtures.FakeSprite(FontFixtures.ICONS[i]);
        StaticMedia.nameIconSprites = icons;
        out.append("V\t").append(intsText(FontFixtures.ICONS)).append('\n');

        var itemList = (ItemTypeList) unsafe.allocateInstance(ItemTypeList.class);
        inject(unsafe, itemList, "typeCache", new LRUCache(64));
        for (int id : ITEM_IDS) {
            var type = (ItemType) unsafe.allocateInstance(ItemType.class);
            type.modelRotationX = random.nextInt(2048);
            type.modelRotationY = random.nextInt(2048);
            type.modelRotationZ = random.nextInt(2048);
            type.modelOffsetX = random.nextInt(200) - 100;
            type.modelOffsetY = random.nextInt(200) - 100;
            type.modelZoom = random.nextInt(4) == 0 ? 0 : 400 + random.nextInt(2000);
            itemList.typeCache.put(type, id);
            out.append("J\t").append(id).append('\t').append(type.modelRotationX).append('\t').append(type.modelRotationY).append('\t').append(type.modelRotationZ)
                .append('\t').append(type.modelOffsetX).append('\t').append(type.modelOffsetY).append('\t').append(type.modelZoom).append('\n');
        }
        TypeLists.OBJ = itemList;

        var npcList = (NpcTypeList) unsafe.allocateInstance(NpcTypeList.class);
        inject(unsafe, npcList, "typeCache", new LRUCache(64));
        for (int id = 1; id <= 5; id++) {
            var type = (NPCType) unsafe.allocateInstance(NPCType.class);
            type.type = id;
            type.modelIds = id == 5 ? null : randomInts(random, 5);
            type.headModels = id == 4 ? null : randomInts(random, 4);
            type.recolorDst = id == 3 ? null : randomShorts(random, 4);
            type.retextureDst = id == 2 ? null : randomShorts(random, 4);
            npcList.typeCache.put(type, id);
            out.append("L\t").append(id).append('\t').append(intList(type.modelIds)).append('\t').append(intList(type.headModels)).append('\t')
                .append(shortList(type.recolorDst)).append('\t').append(shortList(type.retextureDst)).append('\n');
        }
        TypeLists.NPC = npcList;

        var seqList = (SeqTypeList) unsafe.allocateInstance(SeqTypeList.class);
        inject(unsafe, seqList, "sequenceCache", new LRUCache(64));
        for (int id = 1; id <= 4; id++) {
            var type = (SeqType) unsafe.allocateInstance(SeqType.class);
            type.id = id;
            int frames = 1 + random.nextInt(4);
            type.frames = new int[frames];
            type.durations = new int[frames];
            for (int i = 0; i < frames; i++) {
                type.frames[i] = random.nextInt(1000);
                type.durations[i] = 1 + random.nextInt(8);
            }
            type.loopType = 0;
            type.seqtypelist = seqList;
            seqList.sequenceCache.put(type, id);
            out.append("E\t").append(id).append('\n');
        }
        SeqTypeList.seqTypeList = seqList;
        TypeLists.SEQ = seqList;

        int myPlayer = 1 + random.nextInt(2000);
        PlayerUpdate.myPlayerIndex = myPlayer;
        out.append("Y\t").append(myPlayer).append('\n');

        var labels = List.of("main", "dense", "mainb", "denseb");
        var total = 0;
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
            Client.activeDialogueContinue = null;
            Client.OPEN_INTERFACES = new IterableNodeMap<>(8);
            CS2Executor.nextUniqueId = 0;
            while (Client.PENDING_HOOK_REQUESTS.popHead() != null) {
            }
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
                    var c = interfaces[id].components[index];
                    if (random.nextInt(4) != 0) {
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
                        out.append("S\t").append(id).append('\t').append(index).append('\t').append(settingsText).append('\n');
                    }
                    if (random.nextInt(3) == 0) {
                        int count = 1 + random.nextInt(3);
                        var values = new Object[count];
                        for (int i = 0; i < count; i++) values[i] = random.nextInt(3) == 0 ? (Object) TEXTS[random.nextInt(4)] : (Object) (random.nextInt(2) == 0 ? random.nextInt(100) : random.nextInt());
                        c.onResize = values;
                        out.append("H\t").append(id).append('\t').append(index).append('\t').append(spec(values)).append('\n');
                    }
                    if (random.nextInt(4) != 0) {
                        c.fontId = pick(random, FONTS);
                        c.width = pick(random, WIDTHS);
                        c.text = TEXTS[random.nextInt(TEXTS.length)];
                        c.lineSpacing = pick(random, SPACINGS);
                        c.mediaType = MediaType.Companion.forId(pick(random, MEDIA));
                        c.primaryMediaId = random.nextInt(4) != 0 ? pick(random, NPCS) : pick(random, ITEMS);
                        c.containerItemId = pick(random, ITEMS);
                        c.itemStackSize = pick(random, STACKS);
                        out.append("F\t").append(id).append('\t').append(index).append('\t').append(c.fontId).append('\t').append(c.width).append('\t')
                            .append(FontFixtures.hexBytes(c.text.getBytes(StandardCharsets.UTF_8))).append('\t').append(c.lineSpacing).append('\t')
                            .append(c.mediaType.getId()).append('\t').append(c.primaryMediaId).append('\t').append(c.containerItemId).append('\t').append(c.itemStackSize).append('\n');
                    }
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
            ComponentLayerFixtures.dump(interfaces, w.capacity, PREVIOUS);
            extras(interfaces, w.capacity, PREVIOUS);
            for (int step = 0; step < STEPS; step++) {
                var name = weights.get(random.nextInt(weights.size()));
                var op = ClientScriptOpCode.valueOf(name);
                int operand = random.nextInt(2);
                boolean closing = name.equals("IF_CLOSE");
                var base = closing ? "" : name.substring(3);
                boolean interfaceForm = name.startsWith("IF_") && !closing;
                var arguments = closing ? new int[0] : arguments(base, random);
                int count = arguments.length + (interfaceForm ? 1 : 0);
                var ints = new int[count];
                System.arraycopy(arguments, 0, ints, 0, arguments.length);
                if (interfaceForm) {
                    int hash = random.nextInt(5) != 0 ? valid.get(random.nextInt(valid.size())) : pool.get(random.nextInt(pool.size()));
                    ints[count - 1] = hash;
                }
                int hookCount = pick(random, COUNTS);
                e.hookRequestCount = hookCount;
                out.append("B\t").append(hookCount).append('\n');
                if (random.nextInt(2) == 0) {
                    Client.activeDialogueContinue = null;
                    out.append("A\t-\n");
                }
                e.intStackPtr = count;
                Arrays.fill(e.intStack, 0x5a5a5a5a);
                System.arraycopy(ints, 0, e.intStack, 0, count);
                e.stringStackPtr = 0;
                Arrays.fill(e.stringStack, "stale");
                e.hasIntOperand = operand == 1;
                com.jagex.game.runetek5.client.input.console.Console.consoleMessageCount = 0;
                while (context.queuedPackets.popHead() != null) {
                }
                String result = "ok";
                try {
                    dispatch(op, e);
                } catch (Throwable error) {
                    result = "error:" + error.getClass().getSimpleName();                }
                var queued = new ArrayList<PulseEvent>();
                for (CacheableNode node = PulseEventHandler.LOW_PRIORITY.getFirst(); node != null; node = PulseEventHandler.LOW_PRIORITY.getFirst()) queued.add((PulseEvent) node);
                var eventText = new StringBuilder();
                for (var event : queued) {
                    if (eventText.length() > 0) eventText.append(',');
                    int type = event.getType();
                    eventText.append(kind(type));
                    if (type == 18 || type == 19) eventText.append((int) (event.getValue() >> 32));
                    eventText.append(':').append((int) event.getValue());
                }
                ComponentLayerFixtures.unlinkAll(queued);
                var packetText = "-";
                var packets = new ArrayList<TcpPacket>();
                for (var node = context.queuedPackets.popHead(); node != null; node = context.queuedPackets.popHead()) packets.add((TcpPacket) node);
                if (packets.size() > 1) throw new IllegalStateException("packets " + packets.size());
                if (packets.size() == 1) {
                    var packet = packets.get(0);
                    var payload = Arrays.copyOfRange(packet.buffer.buffer, 1, packet.capacity);
                    packetText = packet.packet.name() + "\t" + FontFixtures.hexBytes(payload);
                }
                var consoleText = "-";
                int lines = com.jagex.game.runetek5.client.input.console.Console.consoleMessageCount;
                if (lines > 1) throw new IllegalStateException("console lines " + lines);
                if (lines == 1) consoleText = FontFixtures.hexBytes(com.jagex.game.runetek5.client.input.console.Console.displayedOutput[0].substring(10).getBytes(StandardCharsets.UTF_8));
                var requests = new StringBuilder();
                for (var node = Client.PENDING_HOOK_REQUESTS.popHead(); node != null; node = Client.PENDING_HOOK_REQUESTS.popHead()) {
                    var request = (HookRequest) node;
                    if (requests.length() > 0) requests.append('|');
                    requests.append(request.source.idHash).append(':').append(request.requestId).append(':').append(spec(request.params));
                }
                out.append("T\t").append(label).append('\t').append(name).append('\t').append(operand).append('\t')
                    .append(ComponentLayerFixtures.stack(ints, count)).append('\n');
                out.append("R\t").append(result).append('\t').append(ComponentLayerFixtures.stack(e.intStack, e.intStackPtr)).append('\t')
                    .append(consoleText).append('\t').append(eventText.length() == 0 ? "-" : eventText).append('\t');
                out.append(ComponentLayerFixtures.hookText(e.hookedInterface1, interfaces, w.capacity)).append('\t')
                    .append(ComponentLayerFixtures.hookText(e.hookedInterface2, interfaces, w.capacity)).append('\n');
                out.append("P\t").append(packetText).append('\n');
                out.append("Q\t").append(requests.length() == 0 ? "-" : requests).append('\n');
                ComponentLayerFixtures.dump(interfaces, w.capacity, CURRENT);
                extras(interfaces, w.capacity, CURRENT);
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
        Files.writeString(output.resolve("components-media.native.tsv"), out.toString(), StandardCharsets.UTF_8);
        var stats = new StringBuilder();
        for (var entry : STATS.entrySet()) stats.append(entry.getKey()).append('\t').append(entry.getValue()[0]).append('\t').append(entry.getValue()[1]).append('\n');
        Files.writeString(output.resolve("components-media.stats.tsv"), stats.toString(), StandardCharsets.UTF_8);
        System.exit(0);
    }
}
