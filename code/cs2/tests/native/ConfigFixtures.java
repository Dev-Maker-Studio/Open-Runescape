import com.jagex.game.runetek5.cache.node.IntNode;
import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.cache.node.impl.ObjectNode;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.defaults.InteractionDefaultsType;
import com.jagex.game.runetek5.config.item.ItemType;
import com.jagex.game.runetek5.config.item.ItemTypeList;
import com.jagex.game.runetek5.config.npc.NPCType;
import com.jagex.game.runetek5.config.npc.NpcTypeList;
import com.jagex.game.runetek5.config.param.ParamType;
import com.jagex.game.runetek5.config.param.ParamTypeList;
import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.ConfigTypeChatOps;
import com.jagex.game.runetek5.config.sequence.SeqType;
import com.jagex.game.runetek5.config.sequence.SeqTypeList;
import com.jagex.game.runetek5.config.struct.StructType;
import com.jagex.game.runetek5.config.struct.StructTypeList;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;

public final class ConfigFixtures {
    static final ClientScriptOpCode[] OPERATIONS = {
        ClientScriptOpCode.OC_NAME, ClientScriptOpCode.OC_OP, ClientScriptOpCode.OC_IOP, ClientScriptOpCode.OC_COST,
        ClientScriptOpCode.OC_STACKABLE, ClientScriptOpCode.OC_CERT, ClientScriptOpCode.OC_UNCERT,
        ClientScriptOpCode.OC_WEARPOS, ClientScriptOpCode.OC_WEARPOS2, ClientScriptOpCode.OC_WEARPOS3,
        ClientScriptOpCode.OC_MEMBERS, ClientScriptOpCode.OC_PARAM, ClientScriptOpCode.OC_ICURSOR,
        ClientScriptOpCode.OC_MULTISTACKSIZE, ClientScriptOpCode.OC_MINIMENU_COLOUR_OVERRIDDEN,
        ClientScriptOpCode.OC_MINIMENU_COLOUR, ClientScriptOpCode.NC_PARAM, ClientScriptOpCode.STRUCT_PARAM,
        ClientScriptOpCode.SEQ_PARAM
    };
    static final int[] ITEM_IDS = {0, 1, 2, 3, 4, 5, 6};
    static final int[] OTHER_IDS = {0, 1, 2};
    static final int[] PARAM_IDS = {0, 1, 2, 3, 4, 5, 9};
    static final int[] OPS = {-1, 0, 1, 2, 3, 4, 5, 6, 7};
    static final int[] COSTS = {0, 1, 50, 2147483647};

    record ParamSpec(int id, char type, int defaultInt, String name) {}
    record ItemSpec(int id, String name, int value, int stackable, boolean members, int certId, int certTemplate,
                    int wear, int wear2, int wear3, int multi, String[] ground, String[] inventory, int tooltip,
                    boolean hasTooltip, int cop1, int cid1, int cop2, int cid2, Map<Integer, Object> params) {}

    static Map<Integer, Object> map(Object... pairs) {
        var values = new TreeMap<Integer, Object>();
        for (int i = 0; i < pairs.length; i += 2) values.put((Integer) pairs[i], pairs[i + 1]);
        return values;
    }

    static List<ParamSpec> paramSpecs() {
        return List.of(new ParamSpec(0, 'i', 7, "null"), new ParamSpec(1, 's', 0, "dflt"), new ParamSpec(2, 'i', -1, "x"),
            new ParamSpec(3, 's', 5, "é€"), new ParamSpec(4, 's', 0, ""), new ParamSpec(5, 'i', 2147483647, "n"));
    }

    static List<ItemSpec> itemSpecs() {
        var list = new ArrayList<ItemSpec>();
        String[] noGround = {null, null, "Take", null, null, "Examine"};
        String[] noInventory = {null, null, null, null, "Drop"};
        list.add(new ItemSpec(0, "null", 1, 0, false, -1, -1, -1, -1, -1, -1, noGround, noInventory, 0, false, -1, -1, -1, -1, null));
        list.add(new ItemSpec(1, "Sword", 50, 1, false, 2, -1, 3, 4, 5, 100, new String[] {"a", "", "Take", null, "x", "Examine"},
            new String[] {"Wield", "Use", "é", "", "Drop"}, 0, false, 0, 11, 2, 12, map(0, 11, 1, "p", 4, "")));
        list.add(new ItemSpec(2, "Sword note", 0, 1, true, 1, 5, 0, 0, 0, 1, noGround, noInventory, 255, true, 1, 9, 1, 8, map(1, "note", 2, 0)));
        list.add(new ItemSpec(3, "Cert", 2147483647, 2, false, 4, -1, -1, 1, 2, 0, noGround, noInventory, 65280, false, 4, 3, 3, 2, map(5, -5)));
        list.add(new ItemSpec(4, "Memé", -3, 0, true, -1, 5, 9, 9, 9, -2, noGround, noInventory, 16711680, true, 2, 1, 2, 7, map(3, "str", 0, 2)));
        list.add(new ItemSpec(5, "Mismatch", 5, 1, true, 3, 3, 0, 0, 0, 0, noGround, noInventory, 1, false, -1, 5, -1, 6, map(0, "x", 1, 3, 2, 4)));
        list.add(new ItemSpec(6, "Members plain", 9, 0, true, -1, -1, 0, 0, 0, 0, noGround, noInventory, 7, false, -1, 0, -1, 0, null));
        return list;
    }

    static Map<Integer, Map<Integer, Object>> others(String kind) {
        var all = new TreeMap<Integer, Map<Integer, Object>>();
        all.put(0, null);
        all.put(1, map(0, 4, 1, "q", 2, -9, 3, "", 5, 6));
        all.put(2, map(0, "bad", 1, 3, 4, "e"));
        return all;
    }

    static String kind(Object value) {
        return value instanceof Integer ? "i" + value : "s" + NativeFixtures.hex((String) value);
    }

    static String params(Map<Integer, Object> params) {
        if (params == null) return "-";
        if (params.isEmpty()) return "empty";
        var entries = new ArrayList<String>();
        for (var entry : params.entrySet()) entries.add(entry.getKey() + "=" + kind(entry.getValue()));
        return String.join(";", entries);
    }

    static String actions(String[] actions) {
        var items = new ArrayList<String>();
        for (var action : actions) items.add(action == null ? "null" : "h" + NativeFixtures.hex(action));
        return String.join(",", items);
    }

    static final int COLOUR_MEMBERS = 0x123456;
    static final int COLOUR_FREE = 0x654321;

    static void definitions(Path output) throws Exception {
        var out = new StringBuilder();
        for (var spec : paramSpecs()) {
            out.append("param\t").append(spec.id).append('\t').append((int) spec.type).append('\t').append(spec.defaultInt)
                .append('\t').append(NativeFixtures.hex(spec.name)).append('\n');
        }
        for (var spec : itemSpecs()) {
            out.append("item\t").append(spec.id).append('\t').append(NativeFixtures.hex(spec.name)).append('\t').append(spec.value)
                .append('\t').append(spec.stackable).append('\t').append(spec.members).append('\t').append(spec.certId)
                .append('\t').append(spec.certTemplate).append('\t').append(spec.wear).append('\t').append(spec.wear2)
                .append('\t').append(spec.wear3).append('\t').append(spec.multi).append('\t').append(actions(spec.ground))
                .append('\t').append(actions(spec.inventory)).append('\t').append(spec.tooltip).append('\t').append(spec.hasTooltip)
                .append('\t').append(spec.cop1).append('\t').append(spec.cid1).append('\t').append(spec.cop2).append('\t').append(spec.cid2)
                .append('\t').append(params(spec.params)).append('\n');
        }
        for (var kind : new String[] {"npc", "seq", "struct"}) {
            for (var entry : others(kind).entrySet()) {
                out.append(kind).append('\t').append(entry.getKey()).append('\t').append(params(entry.getValue())).append('\n');
            }
        }
        out.append("colours\t").append(COLOUR_MEMBERS).append('\t').append(COLOUR_FREE).append('\n');
        Files.writeString(output.resolve("config-defs.tsv"), out);
    }

    static IterableNodeMap<Node<?>> nodes(Map<Integer, Object> params) {
        if (params == null) return null;
        var nodes = new IterableNodeMap<Node<?>>(16);
        for (var entry : params.entrySet()) {
            Node<?> node = entry.getValue() instanceof Integer number ? new IntNode(number) : new ObjectNode(entry.getValue());
            nodes.put(node, (long) entry.getKey());
        }
        return nodes;
    }

    static sun.misc.Unsafe unsafe() throws Exception {
        var field = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (sun.misc.Unsafe) field.get(null);
    }

    static void set(sun.misc.Unsafe unsafe, Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        unsafe.putObject(target, unsafe.objectFieldOffset(field), value);
    }

    static void install() throws Exception {
        var unsafe = unsafe();
        var params = (ParamTypeList) unsafe.allocateInstance(ParamTypeList.class);
        set(unsafe, params, "cache", new LRUCache(64));
        for (var spec : paramSpecs()) {
            var type = new ParamType();
            type.typeChar = spec.type;
            type.defaultInt = spec.defaultInt;
            type.typeName = spec.name;
            params.cache.put(type, (long) spec.id);
        }
        for (int id : PARAM_IDS) {
            boolean owned = paramSpecs().stream().anyMatch(spec -> spec.id == id);
            if (!owned) params.cache.put(new ParamType(), (long) id);
        }
        TypeLists.PARAMS = params;
        var items = (ItemTypeList) unsafe.allocateInstance(ItemTypeList.class);
        set(unsafe, items, "typeCache", new LRUCache(64));
        for (var spec : itemSpecs()) {
            var type = new ItemType();
            type.name = spec.name;
            type.value = spec.value;
            type.stackable = spec.stackable;
            type.membersOnly = spec.members;
            type.certId = spec.certId;
            type.certTemplateId = spec.certTemplate;
            type.wearPos = spec.wear;
            type.wearPos2 = spec.wear2;
            type.wearPos3 = spec.wear3;
            type.multiStackSize = spec.multi;
            type.groundActions = spec.ground.clone();
            type.inventoryActions = spec.inventory.clone();
            type.tooltipColor = spec.tooltip;
            type.hasTooltipColor = spec.hasTooltip;
            type.customCursorOp1 = spec.cop1;
            type.customCursorId1 = spec.cid1;
            type.customCursorOp2 = spec.cop2;
            type.customCursorId2 = spec.cid2;
            type.parameters = nodes(spec.params);
            items.typeCache.put(type, (long) spec.id);
        }
        TypeLists.OBJ = items;
        var npcs = (NpcTypeList) unsafe.allocateInstance(NpcTypeList.class);
        set(unsafe, npcs, "typeCache", new LRUCache(64));
        for (var entry : others("npc").entrySet()) {
            var type = new NPCType();
            type.parameters = nodes(entry.getValue());
            npcs.typeCache.put(type, (long) entry.getKey());
        }
        TypeLists.NPC = npcs;
        var seqs = (SeqTypeList) unsafe.allocateInstance(SeqTypeList.class);
        set(unsafe, seqs, "sequenceCache", new LRUCache(64));
        for (var entry : others("seq").entrySet()) {
            var type = new SeqType();
            type.parameters = nodes(entry.getValue());
            seqs.sequenceCache.put(type, (long) entry.getKey());
        }
        TypeLists.SEQ = seqs;
        var structs = (StructTypeList) unsafe.allocateInstance(StructTypeList.class);
        var array = new StructType[3];
        for (var entry : others("struct").entrySet()) {
            var type = new StructType();
            type.nodeMap = nodes(entry.getValue());
            array[entry.getKey()] = type;
        }
        set(unsafe, structs, "cache", array);
        TypeLists.STRUCT = structs;
        var defaults = (InteractionDefaultsType) unsafe.allocateInstance(InteractionDefaultsType.class);
        defaults.tooltipColorP2p = COLOUR_MEMBERS;
        defaults.tooltipColorF2p = COLOUR_FREE;
        TypeLists.INTERACTION_DEFAULTS = defaults;
    }

    record Case(String name, ClientScriptOpCode opcode, int[] ints, String[] strings) {}

    static void add(List<Case> cases, ClientScriptOpCode opcode, int... ints) {
        int[] full = new int[ints.length + 1];
        full[0] = 727;
        System.arraycopy(ints, 0, full, 1, ints.length);
        cases.add(new Case(opcode.name().toLowerCase() + "-" + cases.size(), opcode, full, new String[] {"lower-config-string"}));
    }

    static List<Case> cases() {
        var cases = new ArrayList<Case>();
        for (var opcode : OPERATIONS) {
            switch (opcode) {
                case OC_OP, OC_IOP, OC_ICURSOR -> {
                    for (int id : ITEM_IDS) for (int op : OPS) add(cases, opcode, id, op);
                }
                case OC_PARAM -> {
                    for (int id : ITEM_IDS) for (int param : PARAM_IDS) add(cases, opcode, id, param);
                }
                case NC_PARAM, STRUCT_PARAM, SEQ_PARAM -> {
                    for (int id : OTHER_IDS) for (int param : PARAM_IDS) add(cases, opcode, id, param);
                }
                default -> {
                    for (int id : ITEM_IDS) add(cases, opcode, id);
                }
            }
        }
        return cases;
    }

    static void nativeCases(Path output) throws Exception {
        var cases = cases();
        var executor = new CS2Executor();
        executor.longStackPtr = 1;
        executor.longStack[0] = 727727727727L;
        executor.intLocals = new int[] {727};
        executor.objectLocals = new Object[] {"config-local"};
        executor.longLocals = new long[] {Long.MIN_VALUE};
        executor.intOpValues = new int[1];
        executor.instrPtr = 0;
        var expected = new StringBuilder();
        var errors = new TreeMap<String, Integer>();
        var counts = new TreeMap<String, Integer>();
        for (var entry : cases) {
            Arrays.fill(executor.intStack, 0);
            Arrays.fill(executor.stringStack, null);
            executor.intStackPtr = entry.ints.length;
            System.arraycopy(entry.ints, 0, executor.intStack, 0, entry.ints.length);
            executor.stringStackPtr = entry.strings.length;
            System.arraycopy(entry.strings, 0, executor.stringStack, 0, entry.strings.length);
            String result = "ok";
            try {
                ConfigTypeChatOps.dispatch(entry.opcode, executor);
            } catch (RuntimeException error) {
                result = "error:" + error.getClass().getSimpleName();
            }
            counts.merge(entry.opcode.name(), 1, Integer::sum);
            if (!result.equals("ok")) errors.merge(entry.opcode.name() + ":" + result, 1, Integer::sum);
            if (executor.longStackPtr != 1 || executor.longStack[0] != 727727727727L || executor.intLocals[0] != 727
                || executor.instrPtr != 0 || executor.intStackPtr < 0 || executor.stringStackPtr < 0) throw new AssertionError(entry.name);
            var strings = new String[executor.stringStackPtr];
            for (int i = 0; i < strings.length; i++) strings[i] = (String) executor.stringStack[i];
            expected.append(entry.name).append('\t').append(entry.opcode.name()).append('\t')
                .append(NativeFixtures.integers(entry.ints)).append('\t').append(NativeFixtures.texts(entry.strings))
                .append('\t').append(result).append('\t')
                .append(NativeFixtures.integers(Arrays.copyOf(executor.intStack, executor.intStackPtr))).append('\t')
                .append(NativeFixtures.texts(strings)).append('\n');
        }
        Files.writeString(output.resolve("config.native.tsv"), expected);
        System.out.println("Native config cases " + counts);
        System.out.println("Native config error classes " + errors);
        System.out.println("Generated " + cases.size() + " actual native config dispatch cases");
    }

    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        definitions(output);
        install();
        nativeCases(output);
    }
}
