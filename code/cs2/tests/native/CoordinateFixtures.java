import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.PlayerWorldOps;
import java.nio.file.*;
import java.util.*;

public final class CoordinateFixtures {
    static final ClientScriptOpCode[] OPERATIONS = {
        ClientScriptOpCode.COORDX, ClientScriptOpCode.COORDY,
        ClientScriptOpCode.COORDZ, ClientScriptOpCode.MOVECOORD
    };
    static final int[] WIRES = {93, 298, 536, 48};
    static final int[] EDGES = {
        Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -268435457, -268435456,
        -16385, -16384, -16383, -17, -16, -9, -8, -1, 0, 1, 7, 8,
        15, 16, 17, 8191, 8192, 16382, 16383, 16384, 16385,
        268435455, 268435456, 268435457, Integer.MAX_VALUE - 1, Integer.MAX_VALUE
    };
    static final int[] FIELDS = {0, 1, 8191, 8192, 16382, 16383};
    static final long SEED = 0x727c520eL;
    record CoordinateCase(String name, ClientScriptOpCode opcode, int operand, int[] input) {}

    static void add(List<CoordinateCase> cases, String name, ClientScriptOpCode opcode, int operand, int... arguments) {
        int[] input = new int[arguments.length + 1];
        input[0] = 727;
        System.arraycopy(arguments, 0, input, 1, arguments.length);
        cases.add(new CoordinateCase(name, opcode, operand, input));
    }

    static List<CoordinateCase> cases() {
        var cases = new ArrayList<CoordinateCase>();
        var random = new Random(SEED);
        for (var opcode : OPERATIONS) {
            boolean move = opcode == ClientScriptOpCode.MOVECOORD;
            for (int i = 0; i < EDGES.length; i++) {
                if (move) {
                    for (int axis = 0; axis < 4; axis++) {
                        int[] values = new int[4];
                        values[axis] = EDGES[i];
                        add(cases, "edge-axis-" + axis + "-" + i, opcode, i, values);
                    }
                } else {
                    add(cases, "edge-" + i, opcode, i, EDGES[i]);
                    cases.add(new CoordinateCase("bare-edge-" + i, opcode, 255 - i, new int[] {EDGES[i]}));
                }
            }
            for (int bit = 0; bit < 32; bit++) {
                for (int kind = 0; kind < 6; kind++) {
                    int value = switch (kind) {
                        case 0 -> 1 << bit;
                        case 1 -> ~(1 << bit);
                        case 2 -> (1 << bit) - 1;
                        case 3 -> (1 << bit) + 1;
                        case 4 -> -(1 << bit) - 1;
                        default -> -(1 << bit) + 1;
                    };
                    if (move) {
                        for (int axis = 0; axis < 4; axis++) {
                            int[] values = {0x7fffbfff, -1, 1, -1};
                            values[axis] = value;
                            add(cases, "bit-" + bit + "-" + kind + "-axis-" + axis, opcode, bit * 7 + kind, values);
                        }
                    } else add(cases, "bit-" + bit + "-" + kind, opcode, bit * 7 + kind, value);
                }
            }
            for (int plane = 0; plane < 16; plane++) {
                for (int x : FIELDS) for (int z : FIELDS) {
                    int coordinate = (plane << 28) | (x << 14) | z;
                    String name = "packed-" + plane + "-" + x + "-" + z;
                    if (move) {
                        add(cases, name + "-identity", opcode, 0, coordinate, 0, 0, 0);
                        add(cases, name + "-carry", opcode, 1, coordinate, 1, 1, 1);
                        add(cases, name + "-borrow", opcode, 128, coordinate, -1, -1, -1);
                        add(cases, name + "-unbounded", opcode, 255, coordinate, 16384, 16, 16384);
                    } else add(cases, name, opcode, plane * 16 + x % 16, coordinate);
                }
            }
            if (move) {
                int[] edges = {Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -16384, -1, 0, 1, 7, 8, 15, 16, 16383, Integer.MAX_VALUE};
                int index = 0;
                for (int c : edges) for (int dx : edges) for (int dy : edges) for (int dz : edges) {
                    add(cases, "cross-" + index, opcode, index & 255, c, dx, dy, dz);
                    index++;
                }
            }
            for (int operand = 0; operand < 256; operand++) {
                int value = EDGES[operand % EDGES.length];
                if (move) add(cases, "byte-" + operand, opcode, operand, value, -16385, 15, 16384);
                else add(cases, "byte-" + operand, opcode, operand, value);
            }
            for (int n = 0; n < 1024; n++) {
                if (move) add(cases, "random-" + n, opcode, n & 255, random.nextInt(), random.nextInt(), random.nextInt(), random.nextInt());
                else add(cases, "random-" + n, opcode, n & 255, random.nextInt());
            }
            for (int size = 0; size < (move ? 4 : 1); size++) {
                for (int operand : new int[] {0, 255}) {
                    cases.add(new CoordinateCase("underflow-" + size + "-byte-" + operand, opcode, operand, Arrays.copyOf(EDGES, size)));
                }
            }
            for (int n = 0; n < 16; n++) {
                int[] input = new int[1000];
                input[0] = 727;
                for (int i = 1; i < input.length; i++) input[i] = random.nextInt();
                input[999] = EDGES[n];
                cases.add(new CoordinateCase("full-1000-" + n, opcode, n == 0 ? 0 : 255, input));
            }
        }
        return cases;
    }

    static void nativeCases(Path output) throws Exception {
        var cases = cases();
        var executor = new CS2Executor();
        executor.stringStackPtr = 1;
        executor.stringStack[0] = "lower-coordinate-string";
        executor.longStackPtr = 1;
        executor.longStack[0] = 727727727727L;
        executor.intLocals = new int[] {727};
        executor.objectLocals = new Object[] {"coordinate-local"};
        executor.longLocals = new long[] {Long.MIN_VALUE};
        executor.intOpValues = new int[1];
        executor.instrPtr = 0;
        var expected = new StringBuilder();
        var names = new HashSet<String>();
        for (var entry : cases) {
            if (!names.add(entry.opcode.name() + ":" + entry.name)) throw new AssertionError(entry.name);
            Arrays.fill(executor.intStack, 0);
            executor.intStackPtr = entry.input.length;
            System.arraycopy(entry.input, 0, executor.intStack, 0, entry.input.length);
            executor.intOpValues[0] = entry.operand;
            String result = "ok";
            try {
                PlayerWorldOps.dispatch(entry.opcode, executor);
            } catch (RuntimeException error) {
                result = "error:" + error.getClass().getSimpleName() + ":" + (error.getMessage() == null ? "-" : error.getMessage());
            }
            if (executor.stringStackPtr != 1 || !executor.stringStack[0].equals("lower-coordinate-string")
                || executor.longStackPtr != 1 || executor.longStack[0] != 727727727727L
                || executor.intLocals[0] != 727 || !executor.objectLocals[0].equals("coordinate-local")
                || executor.longLocals[0] != Long.MIN_VALUE || executor.intOpValues[0] != entry.operand
                || executor.instrPtr != 0) throw new AssertionError(entry.name);
            expected.append(entry.name).append('\t').append(entry.opcode.name()).append('\t').append(entry.operand)
                .append('\t').append(NativeFixtures.integers(entry.input)).append('\t').append(result)
                .append('\t').append(executor.intStackPtr).append('\t')
                .append(executor.intStackPtr < 0 ? "-" : NativeFixtures.integers(Arrays.copyOf(executor.intStack, executor.intStackPtr))).append('\n');
        }
        Files.writeString(output.resolve("coordinate.native.tsv"), expected);
        for (var opcode : OPERATIONS) {
            long count = cases.stream().filter(entry -> entry.opcode == opcode).count();
            System.out.println("Native coordinate cases " + opcode.name() + "=" + count);
        }
        System.out.println("Generated " + cases.size() + " actual native coordinate dispatch cases");
    }

    static NativeFixtures.Program arguments(NativeFixtures.Program program) {
        for (int i = 0; i < 4; i++) program.op(ClientScriptOpCode.PUSH_INT_LOCAL, i);
        return program;
    }

    static void execution(Path output) throws Exception {
        var child = new NativeFixtures.Program(5, 1, 1, 4, 1, 1);
        arguments(child).op(ClientScriptOpCode.MOVECOORD, 255).op(ClientScriptOpCode.POP_INT_LOCAL, 4);
        for (int operand : new int[] {0, 1, 127, 128, 255}) {
            for (var opcode : new ClientScriptOpCode[] {ClientScriptOpCode.COORDX, ClientScriptOpCode.COORDY, ClientScriptOpCode.COORDZ}) {
                child.op(ClientScriptOpCode.PUSH_INT_LOCAL, 4).op(opcode, operand);
            }
        }
        child.op(ClientScriptOpCode.PUSH_INT_LOCAL, 4).op(ClientScriptOpCode.PUSH_STRING_LOCAL, 0)
            .op(ClientScriptOpCode.PUSH_LONG_LOCAL, 0).op(ClientScriptOpCode.RETURN, 255);
        var childScript = NativeFixtures.fixture(output, "vm-coordinates-child", child);
        childScript.pointer = 900048;
        var root = new NativeFixtures.Program(1, 1, 1, 0, 0, 0)
            .integer(727).op(ClientScriptOpCode.POP_INT_LOCAL, 0)
            .text("caller-coordinate-local").op(ClientScriptOpCode.POP_STRING_LOCAL, 0)
            .wide(Long.MIN_VALUE + 727).op(ClientScriptOpCode.POP_LONG_LOCAL, 0)
            .integer(727).text("lower-coordinate-string").wide(727727727727L);
        for (int operand = 0; operand < 256; operand++) {
            for (var opcode : OPERATIONS) {
                root.integer(EDGES[operand % EDGES.length]);
                if (opcode == ClientScriptOpCode.MOVECOORD) root.integer(-16385).integer(15).integer(16384);
                root.op(opcode, operand).op(ClientScriptOpCode.POP_INT_LOCAL, 0);
            }
        }
        root.integer(727).op(ClientScriptOpCode.POP_INT_LOCAL, 0);
        for (int[] values : new int[][] {
            {0x7fffffff, 1, 1, 1}, {0x80000000, -1, -1, -1},
            {0x0fffbfff, 1, 0, 1}, {-1, 16384, 16, 16384},
            {Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE}
        }) {
            for (int value : values) root.integer(value);
            root.text("child-coordinate-string").wide(Long.MAX_VALUE - 727)
                .op(ClientScriptOpCode.GOSUB_WITH_PARAMS, 900048);
        }
        root.op(ClientScriptOpCode.PUSH_INT_LOCAL, 0).op(ClientScriptOpCode.PUSH_STRING_LOCAL, 0)
            .op(ClientScriptOpCode.PUSH_LONG_LOCAL, 0).op(ClientScriptOpCode.RETURN);
        NativeFixtures.execution(output, "vm-coordinates", NativeFixtures.fixture(output, "vm-coordinates", root), Map.of(900048, childScript));
    }

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < OPERATIONS.length; i++) {
            if (OPERATIONS[i].opcode != WIRES[i] || OPERATIONS[i].familyId != 7 || OPERATIONS[i].hasIntConstant) {
                throw new AssertionError(OPERATIONS[i]);
            }
        }
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        nativeCases(output);
        execution(output);
    }
}
