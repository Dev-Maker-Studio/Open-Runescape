import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.MathStringOps;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class InversePowerFixtures {
    static final int[] EDGES = {
        Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -65536, -46341, -1000, -64,
        -8, -3, -2, -1, 0, 1, 2, 3, 4, 7, 8, 9, 15, 16, 25, 27, 31, 32,
        63, 64, 65, 80, 81, 82, 124, 125, 126, 215, 216, 217, 255, 256,
        257, 342, 343, 344, 511, 512, 513, 728, 729, 730, 999, 1000, 1001,
        1023, 1024, 1025, 4095, 4096, 4097, 15624, 15625, 15626,
        2147395599, 2147395600, 2147395601, Integer.MAX_VALUE - 1, Integer.MAX_VALUE
    };
    static final int[] ROOTS = {
        Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -65536, -33, -32, -31,
        -30, -16, -8, -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
        10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25,
        26, 27, 28, 29, 30, 31, 32, 33, 63, 64, 65, 65536,
        Integer.MAX_VALUE - 1, Integer.MAX_VALUE
    };
    static final long SEED = 0x727c5215L;
    record PowerCase(String name, int operand, int[] input) {}

    static CS2Executor executor() {
        var exec = new CS2Executor();
        exec.stringStackPtr = 1;
        exec.stringStack[0] = "inverse-power-string";
        exec.longStackPtr = 1;
        exec.longStack[0] = 727727727727L;
        exec.intLocals = new int[] {727};
        exec.objectLocals = new Object[] {"inverse-power-local"};
        exec.longLocals = new long[] {Long.MIN_VALUE};
        exec.intOpValues = new int[1];
        exec.instrPtr = 0;
        return exec;
    }

    static void unchanged(CS2Executor exec, int operand) {
        if (exec.stringStackPtr != 1 || !exec.stringStack[0].equals("inverse-power-string")
            || exec.longStackPtr != 1 || exec.longStack[0] != 727727727727L
            || exec.intLocals[0] != 727 || !exec.objectLocals[0].equals("inverse-power-local")
            || exec.longLocals[0] != Long.MIN_VALUE || exec.instrPtr != 0
            || exec.intOpValues[0] != operand) throw new AssertionError("Non-int state mutated");
    }

    static int evaluate(CS2Executor exec, int base, int root) {
        exec.intStackPtr = 3;
        exec.intStack[0] = 727;
        exec.intStack[1] = base;
        exec.intStack[2] = root;
        MathStringOps.dispatch(ClientScriptOpCode.INVPOW, exec);
        if (exec.intStackPtr != 2 || exec.intStack[0] != 727) throw new AssertionError("Boundary stack mutation");
        unchanged(exec, 0);
        return exec.intStack[1];
    }

    static void warm() {
        var exec = executor();
        var random = new Random(SEED);
        long checksum = 0;
        for (int i = 0; i < 200000; i++) {
            checksum += evaluate(exec, random.nextInt() & Integer.MAX_VALUE, 2 + i % 29);
        }
        System.out.println("Native INVPOW warmup checksum=" + checksum);
    }

    static int boundaries(Path output) throws Exception {
        var exec = executor();
        var bytes = new ByteArrayOutputStream();
        var rows = new DataOutputStream(bytes);
        var table = new StringBuilder("&[\n");
        int count = 0;
        int corrections = 0;
        for (int root = 2; root <= 30; root++) {
            for (int result = 2; ; result++) {
                long power = 1;
                for (int n = 0; n < root && power <= Integer.MAX_VALUE; n++) power *= result;
                if (power > Integer.MAX_VALUE) break;
                int base = (int) power;
                int lower = evaluate(exec, base - 1, root);
                int exact = evaluate(exec, base, root);
                int upper = evaluate(exec, base + 1, root);
                if (lower != result - 1 || upper != result || (exact != result && exact != result - 1)) {
                    throw new AssertionError("Unexpected native threshold " + base + "," + root + ":" + lower + "," + exact + "," + upper);
                }
                for (int value : new int[] {root, result, base, lower, exact, upper}) rows.writeInt(value);
                if (exact == result - 1) {
                    table.append("    (").append(root).append(", ").append(base).append("),\n");
                    corrections++;
                }
                count++;
            }
        }
        table.append("]\n");
        try (var stream = new DataOutputStream(Files.newOutputStream(output.resolve("inverse-power-boundaries.native.bin")))) {
            stream.writeInt(0x727c5215);
            stream.writeInt(count);
            stream.write(bytes.toByteArray());
        }
        Files.writeString(output.resolve("inverse_power_round_down.rs"), table);
        System.out.println("Native INVPOW perfect-power boundaries=" + count + ", dispatch cases=" + (count * 3) + ", round-down corrections=" + corrections);
        return count;
    }

    static void add(List<PowerCase> cases, String name, int operand, int base, int root) {
        cases.add(new PowerCase(name, operand, new int[] {727, base, root}));
    }

    static List<PowerCase> cases() {
        var cases = new ArrayList<PowerCase>();
        var random = new Random(SEED);
        int index = 0;
        for (int base : EDGES) for (int root : ROOTS) add(cases, "edge-" + index++, index & 255, base, root);
        for (int bit = 0; bit < 32; bit++) {
            for (int kind = 0; kind < 6; kind++) {
                int base = switch (kind) {
                    case 0 -> 1 << bit;
                    case 1 -> ~(1 << bit);
                    case 2 -> (1 << bit) - 1;
                    case 3 -> (1 << bit) + 1;
                    case 4 -> -(1 << bit) - 1;
                    default -> -(1 << bit) + 1;
                };
                for (int root : ROOTS) add(cases, "bit-" + bit + "-" + kind + "-" + root, kind, base, root);
            }
        }
        for (int operand = 0; operand < 256; operand++) {
            add(cases, "byte-" + operand, operand, EDGES[operand % EDGES.length], ROOTS[operand % ROOTS.length]);
        }
        for (int n = 0; n < 4096; n++) {
            add(cases, "random-wide-" + n, n & 255, random.nextInt(), random.nextInt());
            add(cases, "random-positive-" + n, n & 255, random.nextInt() & Integer.MAX_VALUE, 2 + n % 29);
        }
        for (int size = 0; size < 2; size++) for (int operand : new int[] {0, 255}) {
            cases.add(new PowerCase("underflow-" + size + "-byte-" + operand, operand, Arrays.copyOf(EDGES, size)));
        }
        for (int n = 0; n < 32; n++) {
            int[] input = new int[1000];
            input[0] = 727;
            for (int i = 1; i < input.length; i++) input[i] = random.nextInt();
            input[998] = EDGES[n];
            input[999] = ROOTS[n];
            cases.add(new PowerCase("full-1000-" + n, n & 255, input));
        }
        return cases;
    }

    static void nativeCases(Path output) throws Exception {
        var exec = executor();
        var expected = new StringBuilder();
        var names = new HashSet<String>();
        var cases = cases();
        for (var entry : cases) {
            if (!names.add(entry.name)) throw new AssertionError(entry.name);
            Arrays.fill(exec.intStack, 0);
            exec.intStackPtr = entry.input.length;
            System.arraycopy(entry.input, 0, exec.intStack, 0, entry.input.length);
            exec.intOpValues[0] = entry.operand;
            String result = "ok";
            try {
                MathStringOps.dispatch(ClientScriptOpCode.INVPOW, exec);
            } catch (RuntimeException error) {
                result = "error:" + error.getClass().getSimpleName() + ":" + (error.getMessage() == null ? "-" : error.getMessage());
            }
            unchanged(exec, entry.operand);
            expected.append(entry.name).append('\t').append(entry.operand).append('\t')
                .append(NativeFixtures.integers(entry.input)).append('\t').append(result)
                .append('\t').append(exec.intStackPtr).append('\t')
                .append(exec.intStackPtr < 0 ? "-" : NativeFixtures.integers(Arrays.copyOf(exec.intStack, exec.intStackPtr))).append('\n');
        }
        Files.writeString(output.resolve("inverse-power.native-cases.tsv"), expected);
        System.out.println("Native INVPOW direct cases=" + cases.size());
    }

    static void execution(Path output) throws Exception {
        var child = new NativeFixtures.Program(3, 1, 1, 2, 1, 1)
            .op(ClientScriptOpCode.PUSH_INT_LOCAL, 0).op(ClientScriptOpCode.PUSH_INT_LOCAL, 1)
            .op(ClientScriptOpCode.INVPOW, 255).op(ClientScriptOpCode.POP_INT_LOCAL, 2)
            .op(ClientScriptOpCode.PUSH_INT_LOCAL, 2).op(ClientScriptOpCode.PUSH_STRING_LOCAL, 0)
            .op(ClientScriptOpCode.PUSH_LONG_LOCAL, 0).op(ClientScriptOpCode.RETURN, 255);
        var childScript = NativeFixtures.fixture(output, "vm-inverse-power-child", child);
        childScript.pointer = 900776;
        var root = new NativeFixtures.Program(1, 1, 1, 0, 0, 0)
            .integer(727).op(ClientScriptOpCode.POP_INT_LOCAL, 0)
            .text("caller-inverse-power-local").op(ClientScriptOpCode.POP_STRING_LOCAL, 0)
            .wide(Long.MIN_VALUE + 727).op(ClientScriptOpCode.POP_LONG_LOCAL, 0)
            .integer(727).text("lower-inverse-power-string").wide(727727727727L);
        for (int operand = 0; operand < 256; operand++) {
            root.integer(EDGES[operand % EDGES.length]).integer(ROOTS[operand % ROOTS.length])
                .op(ClientScriptOpCode.INVPOW, operand);
        }
        for (int[] values : new int[][] {
            {64, 3}, {125, 3}, {1000, 3}, {Integer.MIN_VALUE, 1}, {-1, -1}, {-8, 3},
            {0, 0}, {1, 0}, {Integer.MAX_VALUE, 2}, {Integer.MAX_VALUE, 31}, {2, Integer.MIN_VALUE}
        }) {
            root.integer(values[0]).integer(values[1]).text("child-inverse-power-string").wide(Long.MAX_VALUE - 727)
                .op(ClientScriptOpCode.GOSUB_WITH_PARAMS, 900776);
        }
        root.op(ClientScriptOpCode.PUSH_INT_LOCAL, 0).op(ClientScriptOpCode.PUSH_STRING_LOCAL, 0)
            .op(ClientScriptOpCode.PUSH_LONG_LOCAL, 0).op(ClientScriptOpCode.RETURN);
        NativeFixtures.execution(output, "vm-inverse-power", NativeFixtures.fixture(output, "vm-inverse-power", root), Map.of(900776, childScript));
    }

    public static void main(String[] args) throws Exception {
        if (ClientScriptOpCode.INVPOW.opcode != 776 || ClientScriptOpCode.INVPOW.ordinal() != 547
            || ClientScriptOpCode.INVPOW.familyId != 10 || ClientScriptOpCode.INVPOW.hasIntConstant) throw new AssertionError("INVPOW metadata");
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        if (args.length > 1 && args[1].equals("warm")) warm();
        boundaries(output);
        nativeCases(output);
        execution(output);
    }
}
