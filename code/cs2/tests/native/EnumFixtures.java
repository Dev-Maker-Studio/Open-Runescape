import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.enumtype.EnumType;
import com.jagex.game.runetek5.config.enumtype.EnumTypeList;
import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.EnumEmailFriendOps;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;

public final class EnumFixtures {
    static final ClientScriptOpCode[] OPERATIONS = {
        ClientScriptOpCode.ENUM, ClientScriptOpCode.ENUM_STRING, ClientScriptOpCode.ENUM_HASOUTPUT,
        ClientScriptOpCode.ENUM_HASOUTPUT_STRING, ClientScriptOpCode.ENUM_GETOUTPUTCOUNT,
        ClientScriptOpCode.ENUM_GETREVERSECOUNT, ClientScriptOpCode.ENUM_GETREVERSECOUNT_STRING,
        ClientScriptOpCode.ENUM_GETREVERSEINDEX, ClientScriptOpCode.ENUM_GETREVERSEINDEX_STRING
    };
    static final int[] WIRES = {540, 820, 60, 840, 533, 149, 438, 904, 86};
    static final int[] IDS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 99, -1};
    static final int[] KEY_TYPES = {99, 105, 115, 0};
    static final int[] VALUE_TYPES = {105, 115, 0};
    static final int[] KEYS = {-5, -2, -1, 0, 1, 2, 3, 5, 8, 9, 100, Integer.MAX_VALUE};
    static final int[] VALUES = {-5, 0, 1, 5, 7, 10, 20, 30, 100, 4};
    static final int[] INDEXES = {-1, 0, 1, 2, 5, 70};
    static final String[] TEXTS = {null, "a", "b", "c", "", "x", "y", "é", "none", "dflt", "10", "str", "€"};
    static final long SEED = 0x727c5212L;

    record Spec(int id, char key, char value, String text, int number, int size, Object[] values, Map<Integer, Object> parameters) {}

    static Object[] array(int length, Object... pairs) {
        Object[] values = new Object[length];
        for (int i = 0; i < pairs.length; i += 2) values[(Integer) pairs[i]] = pairs[i + 1];
        return values;
    }

    static Map<Integer, Object> map(Object... pairs) {
        var values = new HashMap<Integer, Object>();
        for (int i = 0; i < pairs.length; i += 2) values.put((Integer) pairs[i], pairs[i + 1]);
        return values;
    }

    static List<Spec> specs() {
        var list = new ArrayList<Spec>();
        list.add(new Spec(0, (char) 0, (char) 0, "null", 0, 0, null, null));
        list.add(new Spec(1, 'i', 'i', "null", 7, 5, array(8, 0, 10, 2, 20, 3, 10, 5, 30, 7, 10), null));
        list.add(new Spec(2, 'i', 's', "dflt", 0, 5, array(6, 0, "a", 2, "b", 3, "a", 4, "", 5, "c"), null));
        list.add(new Spec(3, 'i', 'i', "null", -3, 4, null, map(5, 100, -2, 100, 9, 7, 0, 0)));
        list.add(new Spec(4, 'i', 's', "none", 0, 4, null, map(3, "x", 1, "y", 8, "x", -5, "é")));
        list.add(new Spec(5, 'c', 'i', "null", 4, 3, array(70, 0, 5, 1, 5, 69, 5), null));
        list.add(new Spec(6, 'i', 's', "null", 0, 2, array(3, 0, 5, 2, 5), null));
        list.add(new Spec(7, 'i', 'i', "null", 2, 2, null, map(1, "str", 2, 4)));
        list.add(new Spec(8, 'i', 'i', "null", 9, 3, null, null));
        list.add(new Spec(9, 'i', 'i', "null", 0, 0, array(2, 0, 1, 1, 2), null));
        list.add(new Spec(10, 'c', 'i', "null", 0, 4, null, map(2, 10, 1, 10, 100, 20, 0, 30)));
        list.add(new Spec(11, 'i', 's', "null", 0, 2, array(3, 0, "10", 2, "str"), null));
        return list;
    }

    static String kind(Object value) {
        return value instanceof Integer ? "i" + value : "s" + NativeFixtures.hex((String) value);
    }

    static void definitions(Path output, List<Spec> specs) throws Exception {
        var out = new StringBuilder();
        for (var spec : specs) {
            out.append(spec.id).append('\t').append((int) spec.key).append('\t').append((int) spec.value)
                .append('\t').append(NativeFixtures.hex(spec.text)).append('\t').append(spec.number)
                .append('\t').append(spec.size).append('\t');
            var entries = new ArrayList<String>();
            if (spec.values != null) {
                out.append("values\t").append(spec.values.length).append('\t');
                for (int i = 0; i < spec.values.length; i++) if (spec.values[i] != null) entries.add(i + "=" + kind(spec.values[i]));
            } else if (spec.parameters != null) {
                out.append("params\t").append(0).append('\t');
                for (var key : new TreeSet<>(spec.parameters.keySet())) entries.add(key + "=" + kind(spec.parameters.get(key)));
            } else out.append("none\t0\t");
            out.append(entries.isEmpty() ? "-" : String.join(";", entries)).append('\n');
        }
        Files.writeString(output.resolve("enum-defs.tsv"), out);
    }

    static void install(List<Spec> specs) throws Exception {
        var unsafeField = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        var unsafe = (sun.misc.Unsafe) unsafeField.get(null);
        var list = (EnumTypeList) unsafe.allocateInstance(EnumTypeList.class);
        Field cache = EnumTypeList.class.getDeclaredField("cache");
        unsafe.putObject(list, unsafe.objectFieldOffset(cache), new com.jagex.game.runetek5.cache.node.lru.LRUCache(128));
        var owned = new HashSet<Integer>();
        for (var spec : specs) {
            var type = new EnumType();
            type.keyType = spec.key;
            type.valueType = spec.value;
            type.defaultStringValue = spec.text;
            type.defaultIntValue = spec.number;
            type.size = spec.size;
            type.values = spec.values == null ? null : spec.values.clone();
            type.parameters = spec.parameters == null ? null : new HashMap<>(spec.parameters);
            list.cache.put(type, (long) spec.id);
            owned.add(spec.id);
        }
        for (int id : IDS) if (!owned.contains(id)) list.cache.put(new EnumType(), (long) id);
        TypeLists.ENUM = list;
    }

    record EnumCase(String name, ClientScriptOpCode opcode, int[] ints, String[] strings) {}

    static void add(List<EnumCase> cases, String name, ClientScriptOpCode opcode, int[] ints, String... strings) {
        int[] full = new int[ints.length + 1];
        full[0] = 727;
        System.arraycopy(ints, 0, full, 1, ints.length);
        String[] text = new String[strings.length + 1];
        text[0] = "lower-enum-string";
        System.arraycopy(strings, 0, text, 1, strings.length);
        cases.add(new EnumCase(name, opcode, full, text));
    }

    static String suffix(int index) { return "-" + index; }

    static List<EnumCase> cases() {
        var cases = new ArrayList<EnumCase>();
        var random = new Random(SEED);
        int n = 0;
        for (int id : IDS) {
            for (int key : KEYS) {
                for (int kt : KEY_TYPES) for (int vt : VALUE_TYPES) add(cases, "enum" + suffix(n++), ClientScriptOpCode.ENUM, new int[] {kt, vt, id, key});
                add(cases, "enum-string" + suffix(n++), ClientScriptOpCode.ENUM_STRING, new int[] {id, key});
                for (int vt : VALUE_TYPES) add(cases, "hasoutput" + suffix(n++), ClientScriptOpCode.ENUM_HASOUTPUT, new int[] {vt, id, key});
            }
            add(cases, "count" + suffix(n++), ClientScriptOpCode.ENUM_GETOUTPUTCOUNT, new int[] {id});
            for (int value : VALUES) {
                for (int vt : VALUE_TYPES) {
                    add(cases, "revcount" + suffix(n++), ClientScriptOpCode.ENUM_GETREVERSECOUNT, new int[] {vt, id, value});
                    for (int kt : KEY_TYPES) for (int index : INDEXES) {
                        add(cases, "revindex" + suffix(n++), ClientScriptOpCode.ENUM_GETREVERSEINDEX, new int[] {vt, kt, id, value, index});
                    }
                }
            }
            for (String text : TEXTS) {
                add(cases, "hasoutput-string" + suffix(n++), ClientScriptOpCode.ENUM_HASOUTPUT_STRING, new int[] {id}, text);
                add(cases, "revcount-string" + suffix(n++), ClientScriptOpCode.ENUM_GETREVERSECOUNT_STRING, new int[] {id}, text);
                for (int kt : KEY_TYPES) for (int index : INDEXES) {
                    add(cases, "revindex-string" + suffix(n++), ClientScriptOpCode.ENUM_GETREVERSEINDEX_STRING, new int[] {kt, id, index}, text);
                }
            }
        }
        for (int i = 0; i < 2000; i++) {
            int id = IDS[random.nextInt(IDS.length)];
            int key = random.nextInt(14) - 3;
            int kt = KEY_TYPES[random.nextInt(KEY_TYPES.length)];
            int vt = VALUE_TYPES[random.nextInt(VALUE_TYPES.length)];
            add(cases, "random-enum" + suffix(n++), ClientScriptOpCode.ENUM, new int[] {kt, vt, id, key});
            add(cases, "random-revindex" + suffix(n++), ClientScriptOpCode.ENUM_GETREVERSEINDEX, new int[] {vt, kt, id, random.nextInt(40) - 5, random.nextInt(4)});
            add(cases, "random-revindex-string" + suffix(n++), ClientScriptOpCode.ENUM_GETREVERSEINDEX_STRING, new int[] {kt, id, random.nextInt(4)}, TEXTS[random.nextInt(TEXTS.length)]);
        }
        for (var opcode : OPERATIONS) {
            int[] tail = switch (opcode) {
                case ENUM -> new int[] {105, 105, 1, 2};
                case ENUM_STRING -> new int[] {2, 0};
                case ENUM_HASOUTPUT -> new int[] {105, 1, 10};
                case ENUM_HASOUTPUT_STRING, ENUM_GETREVERSECOUNT_STRING -> new int[] {2};
                case ENUM_GETOUTPUTCOUNT -> new int[] {1};
                case ENUM_GETREVERSECOUNT -> new int[] {105, 1, 10};
                case ENUM_GETREVERSEINDEX -> new int[] {105, 105, 1, 10, 1};
                default -> new int[] {105, 2, 1};
            };
            boolean text = opcode == ClientScriptOpCode.ENUM_HASOUTPUT_STRING || opcode == ClientScriptOpCode.ENUM_GETREVERSECOUNT_STRING
                || opcode == ClientScriptOpCode.ENUM_GETREVERSEINDEX_STRING;
            int[] input = new int[1000];
            input[0] = 727;
            for (int i = 1; i < 1000 - tail.length; i++) input[i] = random.nextInt();
            System.arraycopy(tail, 0, input, 1000 - tail.length, tail.length);
            String[] strings = new String[text ? 1000 : 999];
            strings[0] = "lower-enum-string";
            for (int i = 1; i < strings.length; i++) strings[i] = "s" + i;
            if (text) strings[999] = "a";
            cases.add(new EnumCase("full-" + opcode.name(), opcode, input, strings));
        }
        return cases;
    }

    static void nativeCases(Path output) throws Exception {
        var cases = cases();
        var executor = new CS2Executor();
        executor.longStackPtr = 1;
        executor.longStack[0] = 727727727727L;
        executor.intLocals = new int[] {727};
        executor.objectLocals = new Object[] {"enum-local"};
        executor.longLocals = new long[] {Long.MIN_VALUE};
        executor.intOpValues = new int[1];
        executor.instrPtr = 0;
        var expected = new StringBuilder();
        var names = new HashSet<String>();
        var errors = new TreeMap<String, Integer>();
        for (var entry : cases) {
            if (!names.add(entry.name)) throw new AssertionError(entry.name);
            Arrays.fill(executor.intStack, 0);
            Arrays.fill(executor.stringStack, null);
            executor.intStackPtr = entry.ints.length;
            System.arraycopy(entry.ints, 0, executor.intStack, 0, entry.ints.length);
            executor.stringStackPtr = entry.strings.length;
            System.arraycopy(entry.strings, 0, executor.stringStack, 0, entry.strings.length);
            String result = "ok";
            try {
                EnumEmailFriendOps.dispatch(entry.opcode, executor);
            } catch (RuntimeException error) {
                result = "error:" + error.getClass().getSimpleName() + ":" + (error.getMessage() == null ? "-" : error.getMessage());
            }
            if (!result.equals("ok")) errors.merge(entry.opcode.name() + ":" + result.split(":")[1], 1, Integer::sum);
            if (executor.longStackPtr != 1 || executor.longStack[0] != 727727727727L || executor.intLocals[0] != 727
                || !executor.objectLocals[0].equals("enum-local") || executor.longLocals[0] != Long.MIN_VALUE
                || executor.instrPtr != 0 || executor.intStackPtr < 0 || executor.stringStackPtr < 0) throw new AssertionError(entry.name);
            var strings = new String[executor.stringStackPtr];
            for (int i = 0; i < strings.length; i++) strings[i] = (String) executor.stringStack[i];
            expected.append(entry.name).append('\t').append(entry.opcode.name()).append('\t')
                .append(NativeFixtures.integers(entry.ints)).append('\t').append(NativeFixtures.texts(entry.strings))
                .append('\t').append(result).append('\t')
                .append(NativeFixtures.integers(Arrays.copyOf(executor.intStack, executor.intStackPtr))).append('\t')
                .append(NativeFixtures.texts(strings)).append('\n');
        }
        Files.writeString(output.resolve("enum.native.tsv"), expected);
        for (var opcode : OPERATIONS) {
            long count = cases.stream().filter(entry -> entry.opcode == opcode).count();
            System.out.println("Native enum cases " + opcode.name() + "=" + count);
        }
        System.out.println("Native enum error classes " + errors);
        System.out.println("Generated " + cases.size() + " actual native enum dispatch cases");
    }

    static NativeFixtures.Program push(NativeFixtures.Program program, ClientScriptOpCode opcode, int[] ints, String text) {
        if (text != null && (opcode == ClientScriptOpCode.ENUM_HASOUTPUT_STRING || opcode == ClientScriptOpCode.ENUM_GETREVERSECOUNT_STRING
            || opcode == ClientScriptOpCode.ENUM_GETREVERSEINDEX_STRING)) program.text(text);
        for (int value : ints) program.integer(value);
        return program.op(opcode, 0);
    }

    static void execution(Path output) throws Exception {
        var child = new NativeFixtures.Program(3, 1, 0, 2, 0, 0);
        child.integer(105).integer(105).op(ClientScriptOpCode.PUSH_INT_LOCAL, 0).op(ClientScriptOpCode.PUSH_INT_LOCAL, 1)
            .op(ClientScriptOpCode.ENUM, 0).op(ClientScriptOpCode.POP_INT_LOCAL, 2)
            .op(ClientScriptOpCode.PUSH_INT_LOCAL, 0).op(ClientScriptOpCode.PUSH_INT_LOCAL, 1).op(ClientScriptOpCode.ENUM_STRING, 0)
            .op(ClientScriptOpCode.PUSH_INT_LOCAL, 2).op(ClientScriptOpCode.RETURN, 255);
        var childScript = NativeFixtures.fixture(output, "vm-enums-child", child);
        childScript.pointer = 900049;
        var root = new NativeFixtures.Program(1, 1, 1, 0, 0, 0)
            .integer(727).text("lower-enum-string").wide(727727727727L);
        push(root, ClientScriptOpCode.ENUM, new int[] {105, 105, 1, 2}, null);
        push(root, ClientScriptOpCode.ENUM, new int[] {105, 105, 1, 1}, null);
        push(root, ClientScriptOpCode.ENUM, new int[] {105, 105, 1, 99}, null);
        push(root, ClientScriptOpCode.ENUM, new int[] {105, 115, 2, 3}, null);
        push(root, ClientScriptOpCode.ENUM, new int[] {105, 115, 2, 1}, null);
        push(root, ClientScriptOpCode.ENUM, new int[] {105, 105, 3, 9}, null);
        push(root, ClientScriptOpCode.ENUM, new int[] {105, 115, 4, 8}, null);
        push(root, ClientScriptOpCode.ENUM, new int[] {0, 0, 99, 5}, null);
        push(root, ClientScriptOpCode.ENUM, new int[] {105, 115, 6, 2}, null);
        push(root, ClientScriptOpCode.ENUM_STRING, new int[] {99, 5}, null);
        push(root, ClientScriptOpCode.ENUM_STRING, new int[] {1, 2}, null);
        push(root, ClientScriptOpCode.ENUM_STRING, new int[] {4, -5}, null);
        push(root, ClientScriptOpCode.ENUM_STRING, new int[] {2, 100}, null);
        push(root, ClientScriptOpCode.ENUM_HASOUTPUT, new int[] {105, 1, 10}, null);
        push(root, ClientScriptOpCode.ENUM_HASOUTPUT, new int[] {105, 1, 11}, null);
        push(root, ClientScriptOpCode.ENUM_HASOUTPUT, new int[] {105, 3, 100}, null);
        push(root, ClientScriptOpCode.ENUM_HASOUTPUT, new int[] {105, 9, 1}, null);
        push(root, ClientScriptOpCode.ENUM_HASOUTPUT_STRING, new int[] {2}, "a");
        push(root, ClientScriptOpCode.ENUM_HASOUTPUT_STRING, new int[] {2}, "zz");
        push(root, ClientScriptOpCode.ENUM_HASOUTPUT_STRING, new int[] {4}, "é");
        push(root, ClientScriptOpCode.ENUM_GETOUTPUTCOUNT, new int[] {1}, null);
        push(root, ClientScriptOpCode.ENUM_GETOUTPUTCOUNT, new int[] {9}, null);
        push(root, ClientScriptOpCode.ENUM_GETOUTPUTCOUNT, new int[] {99}, null);
        push(root, ClientScriptOpCode.ENUM_GETREVERSECOUNT, new int[] {105, 1, 10}, null);
        push(root, ClientScriptOpCode.ENUM_GETREVERSECOUNT, new int[] {105, 3, 100}, null);
        push(root, ClientScriptOpCode.ENUM_GETREVERSECOUNT, new int[] {105, 1, 77}, null);
        push(root, ClientScriptOpCode.ENUM_GETREVERSECOUNT, new int[] {105, 9, 1}, null);
        push(root, ClientScriptOpCode.ENUM_GETREVERSECOUNT_STRING, new int[] {2}, "a");
        push(root, ClientScriptOpCode.ENUM_GETREVERSECOUNT_STRING, new int[] {4}, "x");
        push(root, ClientScriptOpCode.ENUM_GETREVERSECOUNT_STRING, new int[] {4}, "nothing");
        push(root, ClientScriptOpCode.ENUM_GETREVERSEINDEX, new int[] {105, 105, 1, 10, 0}, null);
        push(root, ClientScriptOpCode.ENUM_GETREVERSEINDEX, new int[] {105, 105, 1, 10, 2}, null);
        push(root, ClientScriptOpCode.ENUM_GETREVERSEINDEX, new int[] {105, 105, 3, 100, 0}, null);
        push(root, ClientScriptOpCode.ENUM_GETREVERSEINDEX, new int[] {105, 105, 3, 100, 1}, null);
        push(root, ClientScriptOpCode.ENUM_GETREVERSEINDEX, new int[] {105, 99, 10, 10, 1}, null);
        push(root, ClientScriptOpCode.ENUM_GETREVERSEINDEX_STRING, new int[] {105, 2, 1}, "a");
        push(root, ClientScriptOpCode.ENUM_GETREVERSEINDEX_STRING, new int[] {105, 4, 1}, "x");
        push(root, ClientScriptOpCode.ENUM_GETREVERSEINDEX_STRING, new int[] {105, 4, 0}, "x");
        for (int[] pair : new int[][] {{1, 0}, {1, 2}, {1, 5}, {3, 9}, {3, 5}}) {
            root.integer(pair[0]).integer(pair[1]).op(ClientScriptOpCode.GOSUB_WITH_PARAMS, 900049);
        }
        root.op(ClientScriptOpCode.RETURN);
        NativeFixtures.execution(output, "vm-enums", NativeFixtures.fixture(output, "vm-enums", root), Map.of(900049, childScript));
    }

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < OPERATIONS.length; i++) {
            if (OPERATIONS[i].opcode != WIRES[i] || OPERATIONS[i].familyId != 8 || OPERATIONS[i].hasIntConstant) {
                throw new AssertionError(OPERATIONS[i]);
            }
        }
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        var specs = specs();
        definitions(output, specs);
        install(specs);
        nativeCases(output);
        execution(output);
    }
}
