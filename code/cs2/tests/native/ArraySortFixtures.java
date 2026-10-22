import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.DetailCapabilityOps;
import java.nio.file.*;
import java.util.*;

public final class ArraySortFixtures {
    record SortCase(String name, int count, int keySlot, int valueSlot, int[] keys, int[] values, int arity) {}

    static String arrays(CS2Executor exec) {
        var result = new StringJoiner("|");
        for (int slot = 0; slot < 5; slot++) result.add(NativeFixtures.integers(Arrays.copyOf(exec.globalArrays[slot], exec.globalArrayLengths[slot])));
        return result.toString();
    }

    static String stack(CS2Executor exec) {
        return NativeFixtures.integers(Arrays.copyOf(exec.intStack, exec.intStackPtr));
    }

    static void sortCase(List<SortCase> cases, String name, int count, int keySlot, int valueSlot, int[] keys, int[] values) {
        cases.add(new SortCase(name, count, keySlot, valueSlot, keys, values, 4));
    }

    static void cases(Path output) throws Exception {
        var cases = new ArrayList<SortCase>();
        int[] edges = {Integer.MAX_VALUE, 0, Integer.MIN_VALUE, 3, 0, Integer.MAX_VALUE, -1, Integer.MIN_VALUE, 3, 0, -1};
        int[] ids = new int[edges.length];
        for (int i = 0; i < ids.length; i++) ids[i] = 100 + i;
        for (int count = 0; count <= edges.length; count++) sortCase(cases, "edges-prefix-" + count, count, 0, 4, edges, ids);
        for (int key : new int[] {Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE - 1, Integer.MAX_VALUE}) {
            int[] keys = new int[33];
            int[] values = new int[keys.length];
            Arrays.fill(keys, key);
            for (int i = 0; i < values.length; i++) values[i] = i;
            sortCase(cases, "equal-" + key, keys.length, 0, 4, keys, values);
        }
        sortCase(cases, "reverse-slots", edges.length, 4, 0, ids, edges);
        sortCase(cases, "different-lengths", 3, 0, 4, new int[] {3, 1, 2, -999}, new int[] {30, 10, 20, 888, 777, 666});
        for (int count : new int[] {Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -100, -1, 0, 1, 2, 3, 5000, 5001, Integer.MAX_VALUE}) {
            sortCase(cases, "count-" + count, count, 0, 4, new int[] {2, 1}, new int[] {20, 10});
        }
        for (int count : new int[] {Integer.MIN_VALUE, -1, 0, 1, 2, 3}) {
            sortCase(cases, "alias-" + count, count, 0, 0, new int[] {2, 1}, new int[] {20, 10});
        }
        sortCase(cases, "empty-negative", -1, 0, 4, new int[0], new int[0]);
        sortCase(cases, "empty-zero", 0, 0, 4, new int[0], new int[0]);
        sortCase(cases, "empty-minimum", Integer.MIN_VALUE, 0, 4, new int[0], new int[0]);
        for (int slot : new int[] {Integer.MIN_VALUE, -1, 5, Integer.MAX_VALUE}) {
            for (int count : new int[] {Integer.MIN_VALUE, -1, 0, 1, 3}) {
                sortCase(cases, "invalid-key-" + slot + "-" + count, count, slot, 5, new int[] {2, 1}, new int[] {20, 10});
                sortCase(cases, "invalid-value-" + slot + "-" + count, count, 0, slot, new int[] {2, 1}, new int[] {20, 10});
            }
        }
        sortCase(cases, "key-length-before-value-slot", 3, 0, -1, new int[] {2, 1}, new int[] {20, 10});
        sortCase(cases, "value-length", 3, 0, 4, new int[] {3, 2, 1}, new int[] {20, 10});
        for (int arity = 0; arity < 3; arity++) cases.add(new SortCase("underflow-" + arity, 2, 0, 4, new int[] {2, 1}, new int[] {20, 10}, arity));
        var random = new Random(0x727c5287L);
        for (int n = 0; n < 256; n++) {
            int length = random.nextInt(96);
            int[] keys = new int[length];
            int[] values = new int[length];
            for (int i = 0; i < length; i++) {
                keys[i] = switch (n % 4) {
                    case 0 -> random.nextInt(5) - 2;
                    case 1 -> random.nextBoolean() ? Integer.MAX_VALUE : Integer.MIN_VALUE;
                    case 2 -> random.nextInt();
                    default -> length - i;
                };
                values[i] = i;
            }
            sortCase(cases, "random-" + n, random.nextInt(length + 1), 0, 4, keys, values);
        }
        for (int kind = 0; kind < 8; kind++) {
            int[] keys = new int[5000];
            int[] values = new int[5000];
            for (int i = 0; i < keys.length; i++) {
                keys[i] = switch (kind) {
                    case 0 -> Integer.MAX_VALUE;
                    case 1 -> Integer.MIN_VALUE;
                    case 2 -> 0;
                    case 3 -> i;
                    case 4 -> 5000 - i;
                    case 5 -> i % 2 == 0 ? Integer.MIN_VALUE : Integer.MAX_VALUE;
                    case 6 -> random.nextInt(7) - 3;
                    default -> random.nextInt();
                };
                values[i] = i;
            }
            sortCase(cases, "stress-" + kind, 5000, 0, 4, keys, values);
        }
        var expected = new StringBuilder();
        for (var entry : cases) {
            var exec = new CS2Executor();
            exec.globalArrayLengths[0] = entry.keys.length;
            exec.globalArrayLengths[4] = entry.values.length;
            System.arraycopy(entry.keys, 0, exec.globalArrays[0], 0, entry.keys.length);
            System.arraycopy(entry.values, 0, exec.globalArrays[4], 0, entry.values.length);
            int[] arguments = entry.arity == 4 ? new int[] {727, entry.count, entry.keySlot, entry.valueSlot} : Arrays.copyOf(new int[] {entry.count, entry.keySlot, entry.valueSlot}, entry.arity);
            exec.intStackPtr = arguments.length;
            System.arraycopy(arguments, 0, exec.intStack, 0, arguments.length);
            expected.append(entry.name).append('\t').append(entry.count).append('\t').append(entry.keySlot).append('\t').append(entry.valueSlot)
                .append('\t').append(NativeFixtures.integers(exec.globalArrayLengths)).append('\t').append(arrays(exec)).append('\t').append(stack(exec)).append('\t');
            String result = "ok";
            try {
                DetailCapabilityOps.dispatch(ClientScriptOpCode.ARRAY_SORT, exec);
            } catch (RuntimeException error) {
                result = "error:" + error.getClass().getSimpleName() + ":" + (error.getMessage() == null ? "-" : error.getMessage());
            }
            expected.append(result).append('\t').append(stack(exec)).append('\t').append(arrays(exec)).append('\n');
        }
        Files.writeString(output.resolve("array-sort.native.tsv"), expected);
        System.out.println("Generated " + cases.size() + " native ARRAY_SORT cases, including eight 5000-pair stress vectors");
    }

    static NativeFixtures.Program define(NativeFixtures.Program program, int slot, int[] values) {
        program.integer(values.length).op(ClientScriptOpCode.DEFINE_ARRAY, (slot << 16) | 105);
        for (int i = 0; i < values.length; i++) program.integer(i).integer(values[i]).op(ClientScriptOpCode.POP_ARRAY_INT, slot);
        return program;
    }

    static void execution(Path output) throws Exception {
        var child = new NativeFixtures.Program(3, 0, 0, 3, 0, 0)
            .op(ClientScriptOpCode.PUSH_INT_LOCAL, 0).op(ClientScriptOpCode.PUSH_INT_LOCAL, 1).op(ClientScriptOpCode.PUSH_INT_LOCAL, 2)
            .op(ClientScriptOpCode.ARRAY_SORT, 255).op(ClientScriptOpCode.RETURN);
        var childScript = NativeFixtures.fixture(output, "vm-sort-child", child);
        childScript.pointer = 900087;
        var root = new NativeFixtures.Program();
        define(root, 0, new int[] {Integer.MAX_VALUE, 2, 2, Integer.MIN_VALUE, 0, Integer.MAX_VALUE, -1, -999});
        define(root, 4, new int[] {0, 1, 2, 3, 4, 5, 6, 777, 888});
        root.integer(727).integer(7).integer(0).integer(4).op(ClientScriptOpCode.GOSUB_WITH_PARAMS, 900087)
            .integer(0).op(ClientScriptOpCode.PUSH_ARRAY_INT, 0).integer(7).op(ClientScriptOpCode.PUSH_ARRAY_INT, 0)
            .integer(7).op(ClientScriptOpCode.PUSH_ARRAY_INT, 4)
            .integer(7).integer(4).integer(0).op(ClientScriptOpCode.ARRAY_SORT, 1).op(ClientScriptOpCode.RETURN);
        NativeFixtures.execution(output, "vm-sort", NativeFixtures.fixture(output, "vm-sort", root), Map.of(900087, childScript));
    }

    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        cases(output);
        execution(output);
    }
}
