import com.jagex.game.runetek5.cache.node.IntNode;
import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.cache.node.impl.ObjectNode;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.item.ItemType;
import com.jagex.game.runetek5.config.item.ItemTypeList;
import com.jagex.game.runetek5.config.param.ParamType;
import com.jagex.game.runetek5.config.param.ParamTypeList;
import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.ConfigTypeChatOps;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.PlayerWorldOps;
import com.jagex.game.runetek5.config.rsinterface.itemcontainer.ItemContainer;
import com.jagex.game.runetek5.config.rsinterface.itemcontainer.ItemContainerType;
import com.jagex.game.runetek5.config.rsinterface.itemcontainer.ItemContainerTypeList;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;

public final class InventoryFixtures {
    record ParamSpec(int id, char type, int defaultInt, String name) {}
    record ItemSpec(int id, String name, boolean tradeable, int unknown6, int cert, int lend, int bind, Map<Integer, Object> params) {}
    record TypeSpec(int id, int defaultSize, int max, int[] ids, int[] amounts) {}
    record ContainerSpec(int id, boolean other, int[] ids, int[] amounts, String[] tooltips) {}

    static final int[] CONTAINERS = {-1, 0, 1, 2, 3, 4, 5, 6, 7, 100};
    static final int[] SLOTS = {-1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 2147483647};
    static final int[] ITEMS = {-1, 0, 1, 2, 3, 4, 5, 7, 9, 400};
    static final int[] PARAMS = {-1, 0, 1, 2, 3, 4, 5, 6};
    static final int FIRST_BULK = 24;
    static final int ITEM_COUNT = 324;

    static Map<Integer, Object> map(Object... pairs) {
        var values = new TreeMap<Integer, Object>();
        for (int i = 0; i < pairs.length; i += 2) values.put((Integer) pairs[i], pairs[i + 1]);
        return values;
    }

    static List<ParamSpec> paramSpecs() {
        var list = new ArrayList<ParamSpec>();
        list.add(new ParamSpec(-1, (char) 0, 0, "null"));
        list.add(new ParamSpec(0, 'i', 7, "null"));
        list.add(new ParamSpec(1, 's', 0, "dflt"));
        list.add(new ParamSpec(2, 'i', -1, "x"));
        list.add(new ParamSpec(3, 's', 5, "str"));
        list.add(new ParamSpec(4, 's', 0, ""));
        list.add(new ParamSpec(5, 'i', 2147483647, "n"));
        list.add(new ParamSpec(6, 'i', 3, "null"));
        list.add(new ParamSpec(7, 's', 3, "é€"));
        return list;
    }

    static List<ItemSpec> itemSpecs() {
        var list = new ArrayList<ItemSpec>();
        list.add(new ItemSpec(0, "null", false, 0, -1, -1, -1, null));
        list.add(new ItemSpec(1, "Sword", true, 0, -1, -1, -1, map(0, 11, 1, "p", 2, 0, 5, -5)));
        list.add(new ItemSpec(2, "Sword note", true, 0, 1, 5, -1, map(1, "note", 2, 0)));
        list.add(new ItemSpec(3, "Great Sword", true, 0, -1, -1, -1, map(0, 11, 3, "str", 2, 1)));
        list.add(new ItemSpec(4, "Memé", false, 0, -1, -1, -1, map(0, 2, 3, "str", 5, 2)));
        list.add(new ItemSpec(5, "SWORD", true, 1, -1, -1, -1, map(0, 4)));
        list.add(new ItemSpec(6, "sword shield", true, 0, -1, -1, -1, map(0, "x", 1, "x", 2, 4)));
        list.add(new ItemSpec(7, "Éclair", true, 0, -1, 3, -1, map(0, 1)));
        list.add(new ItemSpec(8, "Zebra", true, 0, -1, -1, 2, map(0, 1)));
        list.add(new ItemSpec(9, "Éclair", true, 0, -1, -1, -1, map(0, 3, 2, 3, 1, "")));
        list.add(new ItemSpec(10, "Ärger", false, 0, -1, -1, -1, map(4, 5, 1, 2)));
        list.add(new ItemSpec(11, "Æon", true, 0, -1, -1, -1, map(4, "x", 0, -7)));
        list.add(new ItemSpec(12, "A", true, 0, -1, -1, -1, null));
        list.add(new ItemSpec(13, "a", true, 0, -1, -1, -1, map()));
        list.add(new ItemSpec(14, "AA", true, 0, -1, -1, -1, null));
        list.add(new ItemSpec(15, "aa", false, 0, -1, -1, -1, null));
        list.add(new ItemSpec(16, "Aa", true, 0, -1, -1, -1, null));
        list.add(new ItemSpec(17, "€uro", true, 0, -1, -1, -1, map(1, "dflt", 0, 7)));
        list.add(new ItemSpec(18, "", true, 0, -1, -1, -1, null));
        list.add(new ItemSpec(19, "Dup", true, 0, -1, -1, -1, map(0, 1)));
        list.add(new ItemSpec(20, "Dup", true, 0, -1, -1, -1, map(0, 1)));
        list.add(new ItemSpec(21, "Dup", false, 0, -1, -1, -1, map(0, 1)));
        list.add(new ItemSpec(22, "dup", true, 0, -1, -1, -1, map(0, 2)));
        list.add(new ItemSpec(23, "Dup", true, 0, 3, -1, -1, map(0, 1)));
        for (int id = FIRST_BULK; id < ITEM_COUNT; id++) {
            list.add(new ItemSpec(id, "Bulk " + id, id % 4 != 0, 0, -1, -1, -1, map(2, id % 3, 0, id % 2)));
        }
        return list;
    }

    static List<TypeSpec> typeSpecs() {
        var list = new ArrayList<TypeSpec>();
        list.add(new TypeSpec(1, 3, 28, new int[] {5, 7, 9}, new int[] {10, 20, 30}));
        list.add(new TypeSpec(2, 0, 4, null, null));
        list.add(new TypeSpec(3, 2, 8, new int[] {-1, 4}, new int[] {0, 7}));
        list.add(new TypeSpec(4, 2, 5, null, null));
        list.add(new TypeSpec(6, 0, 1, null, null));
        return list;
    }

    static List<ContainerSpec> containerSpecs() {
        var list = new ArrayList<ContainerSpec>();
        list.add(new ContainerSpec(1, false, new int[] {1, -1, 3, 3, 2, 400, 4, 1}, new int[] {10, 0, 3, 4, 2147483647, 9, -2, 5},
            new String[] {"tip", null, "é", "", "x", null, "y", "z"}));
        list.add(new ContainerSpec(1, true, new int[] {7, 8, 1}, new int[] {1, 2, 3}, new String[] {"o", null, ""}));
        list.add(new ContainerSpec(2, false, new int[] {6, 1}, new int[] {1, 1}, new String[] {null, "b"}));
        list.add(new ContainerSpec(3, false, new int[] {-1}, new int[] {0}, new String[] {null}));
        list.add(new ContainerSpec(-1, false, new int[] {1, 3}, new int[] {4, 5}, new String[] {"m1", "m2"}));
        list.add(new ContainerSpec(4, true, new int[] {5, 5, 9}, new int[] {5, 6, 7}, new String[] {"a", "b", "c"}));
        list.add(new ContainerSpec(0, true, new int[] {1, 1, -1, 9}, new int[] {1, 2, 3, 4}, new String[] {"z", null, null, null}));
        list.add(new ContainerSpec(6, false, new int[] {1, 1}, new int[] {2147483647, 1}, new String[] {null, null}));
        list.add(new ContainerSpec(5, false, new int[] {3, 3, 3, 3, 3, 3, 3, 3, 3}, new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9}, new String[9]));
        return list;
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

    static String tooltips(String[] values) {
        var items = new ArrayList<String>();
        for (var value : values) items.add(value == null ? "null" : "h" + NativeFixtures.hex(value));
        return String.join(",", items);
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

    static void definitions(Path output) throws Exception {
        var out = new StringBuilder();
        for (var spec : paramSpecs()) {
            out.append("param\t").append(spec.id).append('\t').append((int) spec.type).append('\t').append(spec.defaultInt)
                .append('\t').append(NativeFixtures.hex(spec.name)).append('\n');
        }
        for (var spec : itemSpecs()) {
            out.append("item\t").append(spec.id).append('\t').append(NativeFixtures.hex(spec.name)).append('\t').append(spec.tradeable)
                .append('\t').append(spec.unknown6).append('\t').append(spec.cert).append('\t').append(spec.lend)
                .append('\t').append(spec.bind).append('\t').append(params(spec.params)).append('\n');
        }
        for (var spec : typeSpecs()) {
            out.append("ctype\t").append(spec.id).append('\t').append(spec.defaultSize).append('\t').append(spec.max).append('\t')
                .append(spec.ids == null ? "-" : NativeFixtures.integers(spec.ids)).append('\t')
                .append(spec.amounts == null ? "-" : NativeFixtures.integers(spec.amounts)).append('\n');
        }
        for (var spec : containerSpecs()) {
            out.append("container\t").append(spec.id).append('\t').append(spec.other ? 1 : 0).append('\t')
                .append(NativeFixtures.integers(spec.ids)).append('\t').append(NativeFixtures.integers(spec.amounts)).append('\t')
                .append(tooltips(spec.tooltips)).append('\n');
        }
        Files.writeString(output.resolve("inventory-defs.tsv"), out);
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
        TypeLists.PARAMS = params;
        var items = (ItemTypeList) unsafe.allocateInstance(ItemTypeList.class);
        set(unsafe, items, "typeCache", new LRUCache(4096));
        unsafe.putInt(items, unsafe.objectFieldOffset(ItemTypeList.class.getDeclaredField("totalItemCount")), ITEM_COUNT);
        for (var spec : itemSpecs()) {
            var type = new ItemType();
            type.name = spec.name;
            type.tradeable = spec.tradeable;
            type.unknownInt6 = spec.unknown6;
            type.certTemplateId = spec.cert;
            type.lendTemplateId = spec.lend;
            type.bindTemplateId = spec.bind;
            type.groundActions = new String[6];
            type.inventoryActions = new String[6];
            type.parameters = nodes(spec.params);
            items.typeCache.put(type, (long) spec.id);
        }
        TypeLists.OBJ = items;
        var containers = (ItemContainerTypeList) unsafe.allocateInstance(ItemContainerTypeList.class);
        set(unsafe, containers, "cached", new LRUCache(64));
        var seen = new HashSet<Integer>();
        for (var spec : typeSpecs()) {
            var type = new ItemContainerType();
            type.defaultItemSize = spec.defaultSize;
            type.maxItemSize = spec.max;
            type.itemIds = spec.ids;
            type.amounts = spec.amounts;
            containers.cached.put(type, (long) spec.id);
            seen.add(spec.id);
        }
        for (int id : CONTAINERS) {
            if (seen.add(id)) containers.cached.put(new ItemContainerType(), (long) id);
        }
        TypeLists.CONTAINER = containers;
        for (var spec : containerSpecs()) {
            var container = new ItemContainer();
            container.itemIds = spec.ids.clone();
            container.amounts = spec.amounts.clone();
            container.tooltips = spec.tooltips.clone();
            ItemContainer.CONTAINER_MAP.put(container, spec.id | (spec.other ? (long) Integer.MIN_VALUE : 0L));
        }
    }

    static final StringBuilder OUT = new StringBuilder();
    static int cases = 0;
    static final Map<String, Integer> ERRORS = new TreeMap<>();
    static final Map<String, Integer> COUNTS = new TreeMap<>();

    static void scenario(String name) {
        OUT.append("S\t").append(name).append('\n');
        CS2QueryResults.INSTANCE.clear();
        CS2QueryResults.ids = null;
        CS2QueryResults.count = 0;
        CS2QueryResults.cursor = 0;
    }

    static int step(ClientScriptOpCode opcode, int[] ints, String[] strings) {
        var executor = new CS2Executor();
        Arrays.fill(executor.intStack, 0);
        Arrays.fill(executor.stringStack, null);
        executor.intStackPtr = ints.length;
        System.arraycopy(ints, 0, executor.intStack, 0, ints.length);
        executor.stringStackPtr = strings.length;
        System.arraycopy(strings, 0, executor.stringStack, 0, strings.length);
        String result = "ok";
        try {
            switch (opcode) {
                case OC_FIND, OC_FINDNEXT, OC_FINDRESTART, ITEM_FIND_PARAMINT, ITEM_FIND_PARAMSTR -> ConfigTypeChatOps.dispatch(opcode, executor);
                default -> PlayerWorldOps.dispatch(opcode, executor);
            }
        } catch (RuntimeException error) {
            result = "error:" + error.getClass().getSimpleName();
        }
        if (executor.intStackPtr < 0 || executor.stringStackPtr < 0) throw new AssertionError(opcode.name());
        var out = new String[executor.stringStackPtr];
        for (int i = 0; i < out.length; i++) out[i] = (String) executor.stringStack[i];
        OUT.append("C\t").append(opcode.name()).append('\t').append(NativeFixtures.integers(ints)).append('\t')
            .append(NativeFixtures.texts(strings)).append('\t').append(result).append('\t')
            .append(NativeFixtures.integers(Arrays.copyOf(executor.intStack, executor.intStackPtr))).append('\t')
            .append(NativeFixtures.texts(out)).append('\n');
        cases++;
        COUNTS.merge(opcode.name(), 1, Integer::sum);
        if (!result.equals("ok")) ERRORS.merge(opcode.name() + ":" + result, 1, Integer::sum);
        return executor.intStackPtr > 0 ? executor.intStack[executor.intStackPtr - 1] : 0;
    }

    static final String[] NONE = {};

    static void direct() {
        scenario("direct");
        for (var opcode : new ClientScriptOpCode[] {ClientScriptOpCode.INV_GETOBJ, ClientScriptOpCode.INV_GETNUM, ClientScriptOpCode.INVOTHER_GETOBJ,
            ClientScriptOpCode.INVOTHER_GETNUM, ClientScriptOpCode.INV_GETTOOLTIP, ClientScriptOpCode.INV_OTHER_GETTOOLTIP}) {
            for (int container : CONTAINERS) for (int slot : SLOTS) step(opcode, new int[] {container, slot}, NONE);
        }
        for (var opcode : new ClientScriptOpCode[] {ClientScriptOpCode.INV_TOTAL, ClientScriptOpCode.INVOTHER_TOTAL}) {
            for (int container : CONTAINERS) for (int item : ITEMS) step(opcode, new int[] {container, item}, NONE);
        }
        for (var opcode : new ClientScriptOpCode[] {ClientScriptOpCode.INV_SIZE, ClientScriptOpCode.INV_FREESPACE}) {
            for (int container : CONTAINERS) step(opcode, new int[] {container}, NONE);
        }
        for (int container : CONTAINERS) for (int item : ITEMS) step(ClientScriptOpCode.INV_STOCKBASE, new int[] {container, item}, NONE);
        for (var opcode : new ClientScriptOpCode[] {ClientScriptOpCode.INV_TOTALPARAM, ClientScriptOpCode.INV_TOTALPARAM_STACK}) {
            for (int container : CONTAINERS) for (int param : PARAMS) step(opcode, new int[] {container, param}, NONE);
        }
    }

    static void follow(int count) {
        int reads = count < 0 ? 2 : Math.min(count, 6) + 1;
        for (int i = 0; i < reads; i++) step(ClientScriptOpCode.OC_FINDNEXT, new int[0], NONE);
        step(ClientScriptOpCode.OC_FINDRESTART, new int[0], NONE);
        for (int i = 0; i < 2; i++) step(ClientScriptOpCode.OC_FINDNEXT, new int[0], NONE);
    }

    static void sequences() {
        String[] names = {"", "sword", "SWORD", "é", "É", "a", "aa", "dup", "bulk", "bulk 1", "bulk 2", "bulk 29", "€", "zzz", "e", "null", "ec", "ma", "Great", "éclair"};
        scenario("plain");
        step(ClientScriptOpCode.OC_FINDNEXT, new int[0], NONE);
        step(ClientScriptOpCode.OC_FINDRESTART, new int[0], NONE);
        step(ClientScriptOpCode.OC_FINDNEXT, new int[0], NONE);
        for (var name : names) {
            for (int tradeable : new int[] {0, 1, 2}) {
                int count = step(ClientScriptOpCode.OC_FIND, new int[] {tradeable}, new String[] {name});
                follow(count);
            }
        }
        step(ClientScriptOpCode.OC_FIND, new int[] {0}, new String[] {null});
        step(ClientScriptOpCode.OC_FINDNEXT, new int[0], NONE);
        scenario("paramint");
        String[] shorter = {"", "sword", "dup", "bulk", "bulk 1", "éclair", "a"};
        int[] queries = {11, 2, -1, 0, 1, 7, 2147483647, 3, 4, -7, -5};
        for (var name : shorter) {
            for (int tradeable : new int[] {0, 1}) {
                for (int param : new int[] {-1, 0, 1, 2, 3, 4, 5, 6, 7}) {
                    for (int query : queries) {
                        int count = step(ClientScriptOpCode.ITEM_FIND_PARAMINT, new int[] {tradeable, param, query}, new String[] {name});
                        step(ClientScriptOpCode.OC_FINDNEXT, new int[0], NONE);
                        if (query == 11 || query == 1) follow(count);
                    }
                }
            }
        }
        step(ClientScriptOpCode.ITEM_FIND_PARAMINT, new int[] {1, 0, 1}, new String[] {null});
        scenario("paramstr");
        String[] texts = {"p", "str", "", "dflt", "x", "note", "é", "é€", "n"};
        for (var name : shorter) {
            for (int tradeable : new int[] {0, 1}) {
                for (int param : new int[] {-1, 0, 1, 2, 3, 4, 5, 7}) {
                    for (var text : texts) {
                        int count = step(ClientScriptOpCode.ITEM_FIND_PARAMSTR, new int[] {tradeable, param}, new String[] {name, text});
                        step(ClientScriptOpCode.OC_FINDNEXT, new int[0], NONE);
                        if (text.equals("str") || text.isEmpty()) follow(count);
                    }
                }
            }
        }
        step(ClientScriptOpCode.ITEM_FIND_PARAMSTR, new int[] {1, 1}, new String[] {null, "p"});
        step(ClientScriptOpCode.ITEM_FIND_PARAMSTR, new int[] {1, 1}, new String[] {"sword", null});
        step(ClientScriptOpCode.ITEM_FIND_PARAMSTR, new int[] {1, 1}, new String[] {null, null});
        scenario("mixed");
        for (int round = 0; round < 3; round++) {
            step(ClientScriptOpCode.OC_FIND, new int[] {1}, new String[] {"sword"});
            step(ClientScriptOpCode.OC_FINDNEXT, new int[0], NONE);
            step(ClientScriptOpCode.ITEM_FIND_PARAMINT, new int[] {1, 1, 5}, new String[] {"bulk"});
            step(ClientScriptOpCode.OC_FINDNEXT, new int[0], NONE);
            step(ClientScriptOpCode.ITEM_FIND_PARAMSTR, new int[] {1, 0}, new String[] {"bulk", "p"});
            step(ClientScriptOpCode.OC_FINDNEXT, new int[0], NONE);
            step(ClientScriptOpCode.OC_FIND, new int[] {0}, new String[] {"bulk"});
            step(ClientScriptOpCode.OC_FINDNEXT, new int[0], NONE);
            step(ClientScriptOpCode.ITEM_FIND_PARAMINT, new int[] {1, 0, 3}, new String[] {"sword"});
            step(ClientScriptOpCode.OC_FINDNEXT, new int[0], NONE);
        }
    }

    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        definitions(output);
        install();
        direct();
        sequences();
        Files.writeString(output.resolve("inventory.native.tsv"), OUT.toString());
        System.out.println("Native inventory cases " + COUNTS);
        System.out.println("Native inventory error classes " + ERRORS);
        System.out.println("Generated " + cases + " actual native inventory dispatch cases");
        System.exit(0);
    }
}
