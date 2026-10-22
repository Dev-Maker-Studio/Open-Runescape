import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.varp.*;
import com.jagex.game.runetek5.vardomain.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.MathStringOps;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class NativeFixtures {
    record Instruction(ClientScriptOpCode opcode, int operand, byte[] text, long wide, String target) {}
    record Case(int key, String target) {}

    static final class Program {
        final List<Instruction> code = new ArrayList<>();
        final Map<String, Integer> labels = new HashMap<>();
        final List<List<Case>> switches = new ArrayList<>();
        final int[] counts;
        byte[] name = new byte[0];

        Program(int... counts) { this.counts = counts.length == 0 ? new int[6] : counts; }
        Program op(ClientScriptOpCode opcode, int operand) { code.add(new Instruction(opcode, operand, null, 0, null)); return this; }
        Program op(ClientScriptOpCode opcode) { return op(opcode, 0); }
        Program integer(int value) { return op(ClientScriptOpCode.PUSH_CONSTANT_INT, value); }
        Program wide(long value) { code.add(new Instruction(ClientScriptOpCode.PUSH_LONG_CONSTANT, 0, null, value, null)); return this; }
        Program text(byte[] value) { code.add(new Instruction(ClientScriptOpCode.PUSH_CONSTANT_STRING, 0, value, 0, null)); return this; }
        Program text(String value) { return text(value.getBytes(StandardCharsets.ISO_8859_1)); }
        Program label(String label) { if (labels.put(label, code.size()) != null) throw new AssertionError(label); return this; }
        Program branch(ClientScriptOpCode opcode, String target) { code.add(new Instruction(opcode, 0, null, 0, target)); return this; }

        byte[] bytes() throws Exception {
            var bytes = new ByteArrayOutputStream();
            var out = new DataOutputStream(bytes);
            out.write(name); out.writeByte(0);
            for (int index = 0; index < code.size(); index++) {
                var instruction = code.get(index);
                out.writeShort(instruction.opcode.opcode);
                int operand = instruction.target == null ? instruction.operand : labels.get(instruction.target) - index - 1;
                if (instruction.opcode == ClientScriptOpCode.PUSH_CONSTANT_STRING) { out.write(instruction.text); out.writeByte(0); }
                else if (instruction.opcode == ClientScriptOpCode.PUSH_LONG_CONSTANT) out.writeLong(instruction.wide);
                else if (instruction.opcode.hasIntConstant) out.writeInt(operand);
                else out.writeByte(operand);
            }
            out.writeInt(code.size());
            for (int count : counts) out.writeShort(count);
            var block = new ByteArrayOutputStream();
            var tables = new DataOutputStream(block);
            tables.writeByte(switches.size());
            for (int table = 0; table < switches.size(); table++) {
                int tableIndex = table;
                int pc = -1;
                for (int i = 0; i < code.size(); i++) if (code.get(i).opcode == ClientScriptOpCode.SWITCH && code.get(i).operand == tableIndex) { pc = i; break; }
                tables.writeShort(switches.get(table).size());
                for (var entry : switches.get(table)) { tables.writeInt(entry.key); tables.writeInt(labels.get(entry.target) - pc - 1); }
            }
            out.write(block.toByteArray());
            out.writeShort(block.size());
            return bytes.toByteArray();
        }
    }

    static String hex(String value) { return HexFormat.of().formatHex(value.getBytes(StandardCharsets.UTF_8)); }
    static String string(Object value) { return value == null ? "null" : "hex:" + hex((String) value); }

    static CS2Script fixture(Path output, String name, Program program) throws Exception {
        var bytes = program.bytes();
        Files.write(output.resolve(name + ".bin"), bytes);
        var script = CS2Script.decodeScript(bytes);
        var expected = new StringBuilder();
        expected.append("name\t").append(string(script.scriptName)).append('\n');
        expected.append("counts");
        for (int count : program.counts) expected.append('\t').append(count);
        expected.append('\n');
        for (int i = 0; i < script.operations.length; i++) {
            var op = script.operations[i];
            expected.append(i).append('\t').append(op.name()).append('\t');
            if (op == ClientScriptOpCode.PUSH_CONSTANT_STRING) expected.append("string\t").append(string(script.stringOpValues[i]));
            else if (op == ClientScriptOpCode.PUSH_LONG_CONSTANT) expected.append("long\t").append(script.longOpValues[i]);
            else expected.append("int\t").append(script.intOpValues[i]);
            expected.append('\n');
        }
        if (script.switchMaps != null) {
            for (int i = 0; i < script.switchMaps.length; i++) {
                var keys = new TreeSet<Integer>();
                for (var entry : program.switches.get(i)) keys.add(entry.key);
                for (int key : keys) expected.append("switch\t").append(i).append('\t').append(key).append('\t').append(script.switchMaps[i].get((long) key).value).append('\n');
            }
        }
        Files.writeString(output.resolve(name + ".decode.tsv"), expected);
        return script;
    }

    static void execution(Path output, String name, CS2Script root, Map<Integer, CS2Script> children) throws Exception {
        CS2Executor.CS2_CACHE.reset();
        for (var entry : children.entrySet()) CS2Executor.CS2_CACHE.put(entry.getValue(), entry.getKey().longValue());
        var exec = new CS2Executor();
        exec.intLocals = new int[root.intLocalsCount];
        exec.objectLocals = new Object[root.stringLocalsCount];
        exec.longLocals = new long[root.longLocalsCount];
        CS2Executor.CURRENT_CS2_EXEC_IDX = 1;
        CS2Executor.executeScript(root, 200000, exec);
        if (CS2Executor.CURRENT_CS2_EXEC_IDX != 0 || exec.current != root || exec.instrPtr != root.operations.length - 1) throw new AssertionError(name);
        var expected = new StringBuilder("instructions\t" + CS2Executor.instructionCount + "\n");
        for (int i = 0; i < exec.intStackPtr; i++) expected.append("int\t").append(exec.intStack[i]).append('\n');
        for (int i = 0; i < exec.stringStackPtr; i++) expected.append("string\t").append(string(exec.stringStack[i])).append('\n');
        for (int i = 0; i < exec.longStackPtr; i++) expected.append("long\t").append(exec.longStack[i]).append('\n');
        for (int value : exec.intLocals) expected.append("local-int\t").append(value).append('\n');
        for (Object value : exec.objectLocals) expected.append("local-string\t").append(string(value)).append('\n');
        for (long value : exec.longLocals) expected.append("local-long\t").append(value).append('\n');
        for (int slot = 0; slot < 5; slot++) {
            expected.append("array\t").append(slot).append('\t').append(exec.globalArrayLengths[slot]);
            for (int i = 0; i < exec.globalArrayLengths[slot]; i++) expected.append('\t').append(exec.globalArrays[slot][i]);
            expected.append('\n');
        }
        Files.writeString(output.resolve(name + ".vm.tsv"), expected);
    }

    record NumericCase(ClientScriptOpCode opcode, int[] arguments) {}

    static int arity(ClientScriptOpCode opcode) {
        return switch (opcode) {
            case NOT -> 1;
            case SCALE -> 3;
            case INTERPOLATE -> 5;
            default -> 2;
        };
    }

    static String numericResult(CS2Executor executor, NumericCase entry) {
        executor.intStackPtr = entry.arguments.length;
        System.arraycopy(entry.arguments, 0, executor.intStack, 0, entry.arguments.length);
        try {
            MathStringOps.dispatch(entry.opcode, executor);
            if (executor.intStackPtr != 1) throw new AssertionError(entry.opcode);
            return Integer.toString(executor.intStack[0]);
        } catch (ArithmeticException error) {
            return "error:" + error.getClass().getSimpleName();
        }
    }

    static void numeric(Path output) throws Exception {
        var operations = new ClientScriptOpCode[] {
            ClientScriptOpCode.ADD, ClientScriptOpCode.SUB, ClientScriptOpCode.MULTIPLY,
            ClientScriptOpCode.DIVIDE, ClientScriptOpCode.INTERPOLATE, ClientScriptOpCode.ADDPERCENT,
            ClientScriptOpCode.SETBIT, ClientScriptOpCode.CLEARBIT, ClientScriptOpCode.TESTBIT,
            ClientScriptOpCode.MODULO, ClientScriptOpCode.POW, ClientScriptOpCode.AND,
            ClientScriptOpCode.OR, ClientScriptOpCode.MIN, ClientScriptOpCode.MAX,
            ClientScriptOpCode.SCALE, ClientScriptOpCode.NOT
        };
        int[] edges = { Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -65536, -46341, -33, -1, 0, 1, 2, 31, 32, 33, 46341, Integer.MAX_VALUE - 1, Integer.MAX_VALUE };
        var cases = new ArrayList<NumericCase>();
        var random = new Random(0x727c5201L);
        for (var opcode : operations) {
            if (arity(opcode) == 1) {
                for (int value : edges) cases.add(new NumericCase(opcode, new int[] {value}));
            } else if (arity(opcode) == 2) {
                for (int left : edges) for (int right : edges) cases.add(new NumericCase(opcode, new int[] {left, right}));
            }
            for (int i = 0; i < 64; i++) {
                var values = new int[arity(opcode)];
                for (int j = 0; j < values.length; j++) values[j] = random.nextInt();
                cases.add(new NumericCase(opcode, values));
            }
        }
        int[] scaleEdges = { Integer.MIN_VALUE, -65536, -3, -1, 0, 1, 3, 65536, Integer.MAX_VALUE };
        for (int numerator : scaleEdges) for (int denominator : scaleEdges) for (int value : scaleEdges) {
            cases.add(new NumericCase(ClientScriptOpCode.SCALE, new int[] {numerator, denominator, value}));
        }
        for (int[] values : new int[][] {
            {0, 100, 0, 10, 5}, {100, 0, 0, 10, 5}, {-100, 100, 0, 10, -5},
            {Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, 0},
            {0, Integer.MIN_VALUE, 0, -1, 1}, {Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 1, Integer.MAX_VALUE},
            {0, 3, 1, 1, 2}, {Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE},
            {1, -1, Integer.MAX_VALUE, Integer.MIN_VALUE, 0}
        }) cases.add(new NumericCase(ClientScriptOpCode.INTERPOLATE, values));
        int[] exponents = { Integer.MIN_VALUE, -33, -32, -31, -2, -1, 0, 1, 2, 3, 4, 5, 6, 9, 10, 15, 16, 30, 31, 32, 33, Integer.MAX_VALUE };
        for (int base : new int[] {-46340, -1000, -10, -5, -3, -2, 2, 3, 5, 10, 1000, 46340}) {
            for (int exponent : exponents) cases.add(new NumericCase(ClientScriptOpCode.POW, new int[] {base, exponent}));
        }
        var executor = new CS2Executor();
        var expected = new StringBuilder();
        var program = new Program();
        var samples = new EnumMap<ClientScriptOpCode, Integer>(ClientScriptOpCode.class);
        for (var entry : cases) {
            String result = numericResult(executor, entry);
            expected.append(entry.opcode.name()).append('\t');
            for (int i = 0; i < entry.arguments.length; i++) {
                if (i != 0) expected.append(',');
                expected.append(entry.arguments[i]);
            }
            expected.append('\t').append(result).append('\n');
            if (!result.startsWith("error:") && samples.getOrDefault(entry.opcode, 0) < 8) {
                for (int value : entry.arguments) program.integer(value);
                program.op(entry.opcode);
                samples.merge(entry.opcode, 1, Integer::sum);
            }
        }
        Files.writeString(output.resolve("numeric.native.tsv"), expected);
        program.op(ClientScriptOpCode.RETURN);
        execution(output, "vm-numeric", fixture(output, "vm-numeric", program), Map.of());
        var inverse = new StringBuilder();
        for (int base : new int[] {Integer.MIN_VALUE, -64, -8, -1, 0, 1, 2, 4, 8, 9, 16, 25, 27, 32, 64, 81, 100, 125, 216, 256, 343, 512, 729, 1000, 1024, 4096, 15625, Integer.MAX_VALUE}) {
            for (int root : new int[] {Integer.MIN_VALUE, -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 31, 32, Integer.MAX_VALUE}) {
                var entry = new NumericCase(ClientScriptOpCode.INVPOW, new int[] {base, root});
                inverse.append(base).append('\t').append(root).append('\t').append(numericResult(executor, entry)).append('\n');
            }
        }
        Files.writeString(output.resolve("inverse-power.native.tsv"), inverse);
        System.out.println("Generated " + cases.size() + " native numeric cases across " + operations.length + " supported operations");
    }

    record TextCase(ClientScriptOpCode opcode, int[] ints, String[] strings, long[] longs) {}

    static String utf16(String value) {
        if (value == null) return "null";
        var result = new StringBuilder("u");
        for (int i = 0; i < value.length(); i++) result.append(String.format(Locale.ROOT, "%04x", (int) value.charAt(i)));
        return result.toString();
    }

    static String integers(int[] values) {
        if (values.length == 0) return "-";
        var result = new StringJoiner(",");
        for (int value : values) result.add(Integer.toString(value));
        return result.toString();
    }

    static String longs(long[] values) {
        if (values.length == 0) return "-";
        var result = new StringJoiner(",");
        for (long value : values) result.add(Long.toString(value));
        return result.toString();
    }

    static String texts(String[] values) {
        if (values.length == 0) return "-";
        var result = new StringJoiner(",");
        for (String value : values) result.add(utf16(value));
        return result.toString();
    }

    static String textResult(CS2Executor executor, TextCase entry) {
        executor.intStackPtr = entry.ints.length;
        executor.stringStackPtr = entry.strings.length;
        executor.longStackPtr = entry.longs.length;
        System.arraycopy(entry.ints, 0, executor.intStack, 0, entry.ints.length);
        System.arraycopy(entry.strings, 0, executor.stringStack, 0, entry.strings.length);
        System.arraycopy(entry.longs, 0, executor.longStack, 0, entry.longs.length);
        try {
            MathStringOps.dispatch(entry.opcode, executor);
            if (executor.longStackPtr != 0) throw new AssertionError(entry.opcode);
            if (executor.intStackPtr == 1 && executor.stringStackPtr == 0) return "int:" + executor.intStack[0];
            if (executor.intStackPtr == 0 && executor.stringStackPtr == 1) return "string:" + utf16((String) executor.stringStack[0]);
            throw new AssertionError(entry.opcode + " " + executor.intStackPtr + " " + executor.stringStackPtr);
        } catch (RuntimeException error) {
            return "error:" + error.getClass().getSimpleName();
        }
    }

    static void textCase(List<TextCase> cases, ClientScriptOpCode opcode, String[] strings, int... ints) {
        cases.add(new TextCase(opcode, ints, strings, new long[0]));
    }

    static int characterResult(CS2Executor executor, ClientScriptOpCode opcode, int value) {
        executor.intStackPtr = 1;
        executor.stringStackPtr = 0;
        executor.longStackPtr = 0;
        executor.intStack[0] = value;
        MathStringOps.dispatch(opcode, executor);
        if (executor.intStackPtr != 1 || executor.stringStackPtr != 0 || executor.longStackPtr != 0) throw new AssertionError(opcode);
        return executor.intStack[0];
    }

    static void characters(Path output, Path tables) throws Exception {
        var all = new ByteArrayOutputStream();
        var expected = new DataOutputStream(all);
        var lowerBytes = new ByteArrayOutputStream();
        var upperBytes = new ByteArrayOutputStream();
        var lower = new DataOutputStream(lowerBytes);
        var upper = new DataOutputStream(upperBytes);
        var executor = new CS2Executor();
        var predicates = new ClientScriptOpCode[] {
            ClientScriptOpCode.CHAR_ISPRINTABLE, ClientScriptOpCode.CHAR_ISALPHANUMERIC,
            ClientScriptOpCode.CHAR_ISALPHA, ClientScriptOpCode.CHAR_ISNUMERIC
        };
        for (int value = 0; value <= 65535; value++) {
            int lowercase = characterResult(executor, ClientScriptOpCode.CHAR_TOLOWERCASE, value);
            int uppercase = characterResult(executor, ClientScriptOpCode.CHAR_TOUPPERCASE, value);
            expected.writeShort(lowercase);
            expected.writeShort(uppercase);
            int flags = 0;
            for (int i = 0; i < predicates.length; i++) flags |= characterResult(executor, predicates[i], value) << i;
            expected.writeByte(flags);
            if (lowercase != value) { lower.writeShort(value); lower.writeShort(lowercase); }
            if (uppercase != value) { upper.writeShort(value); upper.writeShort(uppercase); }
        }
        Files.write(output.resolve("characters.native.bin"), all.toByteArray());
        if (tables != null) {
            Files.createDirectories(tables);
            Files.write(tables.resolve("char-lower.bin"), lowerBytes.toByteArray());
            Files.write(tables.resolve("char-upper.bin"), upperBytes.toByteArray());
        }
        System.out.println("Generated all 65536 native UTF16 character cases; lowercase mappings=" + lowerBytes.size() / 4 + ", uppercase mappings=" + upperBytes.size() / 4);
    }

    static void strings(Path output) throws Exception {
        String[] edges = {null, "", "plain", "Runescape RuneScape runescape", "a😀b", "😀😀", "\0é€Œ—Ÿ", "<x>a<>b>c<tail", "ΣİıßſK", "<😀>é>😀<"};
        int[] numbers = {Integer.MIN_VALUE, -65537, -65536, -65535, -2, -1, 0, 1, 9, 10, 99, 100, 65535, 65536, Integer.MAX_VALUE};
        int[] units = {Integer.MIN_VALUE, -65537, -65536, -65535, -2, -1, 0, 32, 65, 255, 0x8000, 0xd7ff, 0xd800, 0xdbff, 0xdc00, 0xdfff, 0xe000, 0xffff, 0x10000, 0x10041, 0x1f600, Integer.MAX_VALUE};
        int[] starts = {Integer.MIN_VALUE, -5, -1, 0, 1, 2, 3, 4, 8, 99, Integer.MAX_VALUE};
        String[] searches = {null, "", "a", "b", "😀", "😀b", "<", "€", "é", "Runescape", "\0"};
        var cases = new ArrayList<TextCase>();
        var empty = new String[0];
        for (String text : edges) {
            for (String suffix : edges) textCase(cases, ClientScriptOpCode.APPEND, new String[] {text, suffix});
            for (int value : numbers) {
                textCase(cases, ClientScriptOpCode.APPEND_NUM, new String[] {text}, value);
                textCase(cases, ClientScriptOpCode.APPEND_SIGNNUM, new String[] {text}, value);
            }
            for (int value : units) textCase(cases, ClientScriptOpCode.APPEND_CHAR, new String[] {text}, value);
            for (var opcode : new ClientScriptOpCode[] {ClientScriptOpCode.ESCAPE, ClientScriptOpCode.REMOVETAGS, ClientScriptOpCode.STRING_LENGTH}) textCase(cases, opcode, new String[] {text});
            var indices = new LinkedHashSet<Integer>(List.of(Integer.MIN_VALUE, -1, 0, 1, 2, 3, 4, Integer.MAX_VALUE));
            int length = text == null ? 0 : text.length();
            indices.add(length - 1); indices.add(length); indices.add(length + 1);
            for (int start : indices) for (int end : indices) textCase(cases, ClientScriptOpCode.SUBSTRING, new String[] {text}, start, end);
            for (int start : starts) {
                for (int unit : units) textCase(cases, ClientScriptOpCode.STRING_INDEXOF_CHAR, new String[] {text}, unit, start);
                for (String search : searches) textCase(cases, ClientScriptOpCode.STRING_INDEXOF_STRING, new String[] {text, search}, start);
            }
        }
        for (String yes : edges) for (String no : edges) for (int flag : new int[] {Integer.MIN_VALUE, -1, 0, 1, 2, Integer.MAX_VALUE}) textCase(cases, ClientScriptOpCode.TEXT_SWITCH, new String[] {yes, no}, flag);
        for (int value : numbers) for (var opcode : new ClientScriptOpCode[] {ClientScriptOpCode.TOSTRING, ClientScriptOpCode.GET_COL_TAG}) textCase(cases, opcode, empty, value);
        var characterOps = new ClientScriptOpCode[] {
            ClientScriptOpCode.CHAR_ISPRINTABLE, ClientScriptOpCode.CHAR_ISALPHANUMERIC,
            ClientScriptOpCode.CHAR_ISALPHA, ClientScriptOpCode.CHAR_ISNUMERIC,
            ClientScriptOpCode.CHAR_TOLOWERCASE, ClientScriptOpCode.CHAR_TOUPPERCASE
        };
        for (int value : units) for (var opcode : characterOps) textCase(cases, opcode, empty, value);
        for (long value : new long[] {Long.MIN_VALUE, Long.MIN_VALUE + 1, -36, -35, -2, -1, 0, 1, 10, 35, 36, 37, Long.MAX_VALUE - 1, Long.MAX_VALUE}) cases.add(new TextCase(ClientScriptOpCode.CLANFORUMQFC_TOSTRING, new int[0], empty, new long[] {value}));
        var random = new Random(0x72757a16L);
        for (int i = 0; i < 256; i++) {
            int value = random.nextInt();
            for (var opcode : characterOps) textCase(cases, opcode, empty, value);
            for (var opcode : new ClientScriptOpCode[] {ClientScriptOpCode.APPEND_NUM, ClientScriptOpCode.APPEND_SIGNNUM}) textCase(cases, opcode, new String[] {edges[i % edges.length]}, value);
            for (var opcode : new ClientScriptOpCode[] {ClientScriptOpCode.TOSTRING, ClientScriptOpCode.GET_COL_TAG}) textCase(cases, opcode, empty, value);
            cases.add(new TextCase(ClientScriptOpCode.CLANFORUMQFC_TOSTRING, new int[0], empty, new long[] {random.nextLong()}));
        }
        var executor = new CS2Executor();
        var expected = new StringBuilder();
        for (var entry : cases) expected.append(entry.opcode.name()).append('\t').append(integers(entry.ints)).append('\t').append(texts(entry.strings)).append('\t').append(longs(entry.longs)).append('\t').append(textResult(executor, entry)).append('\n');
        Files.writeString(output.resolve("strings.native.tsv"), expected);
        var program = new Program(0, 1, 0, 0, 0, 0)
            .text("value=").integer(Integer.MIN_VALUE).op(ClientScriptOpCode.APPEND_NUM)
            .op(ClientScriptOpCode.PUSH_STRING_LOCAL, 0).text("!").op(ClientScriptOpCode.APPEND)
            .text("value=").integer(0).op(ClientScriptOpCode.APPEND_SIGNNUM)
            .integer(-1).op(ClientScriptOpCode.GET_COL_TAG)
            .integer(Integer.MIN_VALUE).op(ClientScriptOpCode.TOSTRING)
            .op(ClientScriptOpCode.PUSH_STRING_LOCAL, 0).text("no").integer(1).op(ClientScriptOpCode.TEXT_SWITCH)
            .text("<a> <b>").op(ClientScriptOpCode.ESCAPE)
            .text("letter:").integer(0x3a3).op(ClientScriptOpCode.APPEND_CHAR)
            .integer(0x20ac).op(ClientScriptOpCode.CHAR_ISPRINTABLE)
            .integer(0x130).op(ClientScriptOpCode.CHAR_ISALPHANUMERIC)
            .integer(0x10041).op(ClientScriptOpCode.CHAR_ISALPHA)
            .integer('5').op(ClientScriptOpCode.CHAR_ISNUMERIC)
            .text(new byte[] {(byte) 0xe9, (byte) 0x80}).op(ClientScriptOpCode.STRING_LENGTH)
            .text("abcdef").integer(1).integer(4).op(ClientScriptOpCode.SUBSTRING)
            .text("a<x>b>c<tail").op(ClientScriptOpCode.REMOVETAGS)
            .text("banana").integer('a').integer(2).op(ClientScriptOpCode.STRING_INDEXOF_CHAR)
            .text("banana").text("na").integer(0).op(ClientScriptOpCode.STRING_INDEXOF_STRING)
            .integer(0x130).op(ClientScriptOpCode.CHAR_TOLOWERCASE)
            .integer(0xdf).op(ClientScriptOpCode.CHAR_TOUPPERCASE)
            .wide(Long.MIN_VALUE).op(ClientScriptOpCode.CLANFORUMQFC_TOSTRING)
            .text("Rune").text("scape").op(ClientScriptOpCode.APPEND)
            .op(ClientScriptOpCode.RETURN);
        execution(output, "vm-text", fixture(output, "vm-text", program), Map.of());
        System.out.println("Generated " + cases.size() + " lossless native string cases across 20 candidate operations");
    }

    static PlayerVarDomain variableDomain() {
        TypeLists.VAR = new VarpTypeList(null);
        TypeLists.VARPBIT = new VarpbitTypeList(null);
        var domain = new PlayerVarDomain();
        domain.activeVars = new int[65536];
        domain.pendingVars = new int[65536];
        VarProviders.setPLAYER_VAR_PROVIDER(domain);
        return domain;
    }

    static void variableDefinition(int id, int varp, int start, int end) {
        var definition = new VarpbitType();
        definition.configIndex = varp;
        definition.startBit = start;
        definition.endBit = end;
        TypeLists.VARPBIT.cache.put(definition, id);
    }

    static CS2Executor variableExecution(CS2Script root) {
        CS2Executor.CS2_CACHE.reset();
        var exec = new CS2Executor();
        exec.intLocals = new int[root.intLocalsCount];
        exec.objectLocals = new Object[root.stringLocalsCount];
        exec.longLocals = new long[root.longLocalsCount];
        CS2Executor.CURRENT_CS2_EXEC_IDX = 1;
        CS2Executor.executeScript(root, 200000, exec);
        if (CS2Executor.CURRENT_CS2_EXEC_IDX != 0 || exec.current != root || exec.instrPtr != root.operations.length - 1) throw new AssertionError("variables");
        return exec;
    }

    static void variableCase(StringBuilder expected, PlayerVarDomain domain, ClientScriptOpCode opcode, int id, int varp, int start, int end, int initial, int pending, Integer value) throws Exception {
        TypeLists.VARPBIT.cache.reset();
        if (start >= 0) variableDefinition(id, varp, start, end);
        else if (opcode == ClientScriptOpCode.PUSH_VARBIT || opcode == ClientScriptOpCode.POP_VARBIT) TypeLists.VARPBIT.cache.put(new VarpbitType(), id);
        domain.activeVars[varp] = initial;
        domain.pendingVars[varp] = pending;
        var program = new Program().integer(727);
        if (value != null) program.integer(value);
        program.op(opcode, id).op(ClientScriptOpCode.RETURN);
        var exec = variableExecution(CS2Script.decodeScript(program.bytes()));
        if (exec.intStack[0] != 727 || exec.intStackPtr != (value == null ? 2 : 1)) throw new AssertionError(opcode);
        expected.append(opcode.name()).append('\t').append(id).append('\t').append(varp).append('\t').append(start).append('\t').append(end)
            .append('\t').append(initial).append('\t').append(pending).append('\t').append(value == null ? "-" : value)
            .append('\t').append(value == null ? exec.intStack[1] : "-")
            .append('\t').append(domain.activeVars[varp]).append('\t').append(domain.pendingVars[varp])
            .append('\t').append(CS2Executor.instructionCount).append('\n');
    }

    static void variables(Path output) throws Exception {
        var domain = variableDomain();
        var expected = new StringBuilder();
        var random = new Random(0x727c5204L);
        var spans = new ArrayList<int[]>();
        for (int start = 0; start < 32; start++) for (int end = start; end < 32; end++) spans.add(new int[] {start, end});
        for (int[] span : new int[][] {{32, 32}, {34, 41}, {31, 62}, {16, 47}, {224, 255}, {240, 255}, {255, 255}}) spans.add(span);
        for (int[] span : spans) {
            int start = span[0], end = span[1];
            int mask = PlayerVarDomain.BIT_MASKS[end - start];
            for (int initial : new int[] {Integer.MIN_VALUE, -1, 0x5aa5f00f, Integer.MAX_VALUE}) {
                variableCase(expected, domain, ClientScriptOpCode.PUSH_VARBIT, 1031, 65535, start, end, initial, initial ^ 0x55aa55aa, null);
            }
            var values = new LinkedHashSet<Integer>(List.of(Integer.MIN_VALUE, -1, 0, 1, mask, mask + 1, Integer.MAX_VALUE));
            for (int value : values) {
                int initial = random.nextInt();
                variableCase(expected, domain, ClientScriptOpCode.POP_VARBIT, 1031, 65535, start, end, initial, initial ^ 0x55aa55aa, value);
            }
        }
        for (int value : new int[] {Integer.MIN_VALUE, -1, 0, 1, 2, Integer.MAX_VALUE}) {
            variableCase(expected, domain, ClientScriptOpCode.PUSH_VAR, 65535, 65535, -1, -1, value, value ^ 0x55aa55aa, null);
            variableCase(expected, domain, ClientScriptOpCode.POP_VAR, 65535, 65535, -1, -1, ~value, value ^ 0x55aa55aa, value);
            variableCase(expected, domain, ClientScriptOpCode.PUSH_VARBIT, 999, 0, -1, -1, value, value ^ 0x55aa55aa, null);
            variableCase(expected, domain, ClientScriptOpCode.POP_VARBIT, 999, 0, -1, -1, ~value, value ^ 0x55aa55aa, value);
        }
        Files.writeString(output.resolve("variables.native.tsv"), expected);
        Arrays.fill(domain.activeVars, 0);
        Arrays.fill(domain.pendingVars, -1);
        TypeLists.VARPBIT.cache.reset();
        variableDefinition(1031, 2, 4, 11);
        variableDefinition(1032, 2, 34, 41);
        variableDefinition(1033, 2, 0, 31);
        TypeLists.VARPBIT.cache.put(new VarpbitType(), 999);
        var program = new Program()
            .op(ClientScriptOpCode.PUSH_VAR, 2).integer(0x76543210).op(ClientScriptOpCode.POP_VAR, 2)
            .op(ClientScriptOpCode.PUSH_VARBIT, 1031).integer(255).op(ClientScriptOpCode.POP_VARBIT, 1031)
            .op(ClientScriptOpCode.PUSH_VAR, 2).op(ClientScriptOpCode.PUSH_VARBIT, 1032)
            .integer(-1).op(ClientScriptOpCode.POP_VARBIT, 1031).op(ClientScriptOpCode.PUSH_VAR, 2)
            .integer(1).op(ClientScriptOpCode.POP_VARBIT, 999).op(ClientScriptOpCode.PUSH_VAR, 0)
            .integer(0).op(ClientScriptOpCode.POP_VARBIT, 1033).op(ClientScriptOpCode.PUSH_VAR, 2)
            .op(ClientScriptOpCode.RETURN);
        var exec = variableExecution(fixture(output, "vm-vars", program));
        var result = new StringBuilder("instructions\t" + CS2Executor.instructionCount + "\n");
        for (int i = 0; i < exec.intStackPtr; i++) result.append("int\t").append(exec.intStack[i]).append('\n');
        for (int id : new int[] {0, 2, 65535}) result.append("active\t").append(id).append('\t').append(domain.activeVars[id]).append('\n');
        Files.writeString(output.resolve("vm-vars.vm.tsv"), result);
        System.out.println("Generated " + expected.toString().lines().count() + " actual native executor variable cases across four hosted operations");
    }

    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        if (args.length > 1 && args[1].equals("--variables-only")) {
            variables(output);
            return;
        }
        var metadata = new StringBuilder();
        var all = new Program(3, 2, 1, 2, 1, 1);
        all.name = new byte[] {'R', 'u', 'n', 'e', 'S', 'c', 'a', 'p', 'e', ' ', 'n', (byte) 0x80, (byte) 0x81};
        var stringBytes = new ByteArrayOutputStream();
        stringBytes.write("runescape RuneScape Runescape RUNESCAPE ".getBytes(StandardCharsets.ISO_8859_1));
        for (int b = 128; b < 160; b++) stringBytes.write(b);
        for (var op : ClientScriptOpCode.values()) {
            metadata.append(op.ordinal()).append('\t').append(op.opcode).append('\t').append(op.name()).append('\t').append(op.hasIntConstant).append('\t').append(op.familyId).append('\n');
            if (op == ClientScriptOpCode.PUSH_CONSTANT_STRING) all.text(stringBytes.toByteArray());
            else if (op == ClientScriptOpCode.PUSH_LONG_CONSTANT) all.wide(Long.MIN_VALUE + 123);
            else all.op(op, op.hasIntConstant ? 0x81234567 : 255);
        }
        Files.writeString(output.resolve("opcodes.tsv"), metadata);
        fixture(output, "all-opcodes", all);
        var child = new Program(3, 2, 1, 2, 2, 1)
            .op(ClientScriptOpCode.PUSH_INT_LOCAL, 0).op(ClientScriptOpCode.PUSH_INT_LOCAL, 1)
            .op(ClientScriptOpCode.PUSH_STRING_LOCAL, 0).op(ClientScriptOpCode.PUSH_STRING_LOCAL, 1).op(ClientScriptOpCode.JOIN_STRING, 2)
            .op(ClientScriptOpCode.PUSH_LONG_LOCAL, 0).integer(2).op(ClientScriptOpCode.PUSH_ARRAY_INT, 4)
            .integer(1).op(ClientScriptOpCode.DEFINE_ARRAY, (4 << 16) | 115).op(ClientScriptOpCode.RETURN);
        var childScript = fixture(output, "vm-child", child);
        childScript.pointer = 900002;
        var root = new Program(1, 1, 1, 0, 0, 0)
            .integer(71).op(ClientScriptOpCode.POP_INT_LOCAL, 0).text("caller").op(ClientScriptOpCode.POP_STRING_LOCAL, 0).wide(72).op(ClientScriptOpCode.POP_LONG_LOCAL, 0)
            .integer(3).op(ClientScriptOpCode.DEFINE_ARRAY, (4 << 16) | 105).integer(2).integer(81).op(ClientScriptOpCode.POP_ARRAY_INT, 4)
            .integer(70).integer(11).integer(22).text("lower").text("first").text("second").wide(73).wide(74)
            .op(ClientScriptOpCode.GOSUB_WITH_PARAMS, 900002).integer(0).op(ClientScriptOpCode.PUSH_ARRAY_INT, 4)
            .op(ClientScriptOpCode.PUSH_INT_LOCAL, 0).op(ClientScriptOpCode.PUSH_STRING_LOCAL, 0).op(ClientScriptOpCode.PUSH_LONG_LOCAL, 0).op(ClientScriptOpCode.RETURN);
        var rootScript = fixture(output, "vm-root", root);
        rootScript.pointer = 900001;
        execution(output, "vm-root", rootScript, Map.of(900002, childScript));
        var flow = new Program();
        var intOps = new ClientScriptOpCode[] { ClientScriptOpCode.BRANCH_NOT, ClientScriptOpCode.BRANCH_EQUALS, ClientScriptOpCode.BRANCH_LESS_THAN, ClientScriptOpCode.BRANCH_GREATER_THAN, ClientScriptOpCode.BRANCH_LESS_THAN_OR_EQUALS, ClientScriptOpCode.BRANCH_GREATER_THAN_OR_EQUALS };
        var longOps = new ClientScriptOpCode[] { ClientScriptOpCode.LONG_BRANCH_NOT, ClientScriptOpCode.LONG_BRANCH_EQUALS, ClientScriptOpCode.LONG_BRANCH_LESS_THAN, ClientScriptOpCode.LONG_BRANCH_GREATER_THAN, ClientScriptOpCode.LONG_BRANCH_LESS_THAN_OR_EQUALS, ClientScriptOpCode.LONG_BRANCH_GREATER_THAN_OR_EQUALS };
        int label = 0;
        for (int equal = 0; equal < 2; equal++) {
            for (var op : intOps) {
                String yes = "yes" + label, end = "end" + label++;
                flow.integer(Integer.MIN_VALUE).integer(equal == 0 ? Integer.MAX_VALUE : Integer.MIN_VALUE).branch(op, yes).integer(0).branch(ClientScriptOpCode.BRANCH, end).label(yes).integer(1).label(end);
            }
            for (var op : longOps) {
                String yes = "yes" + label, end = "end" + label++;
                flow.wide(Long.MIN_VALUE).wide(equal == 0 ? Long.MAX_VALUE : Long.MIN_VALUE).branch(op, yes).integer(0).branch(ClientScriptOpCode.BRANCH, end).label(yes).integer(1).label(end);
            }
        }
        for (int value : new int[] {-1, 0, 1, 2}) for (var op : new ClientScriptOpCode[] { ClientScriptOpCode.BRANCH_IF_TRUE, ClientScriptOpCode.BRANCH_IF_FALSE }) {
            String yes = "yes" + label, end = "end" + label++;
            flow.integer(value).branch(op, yes).integer(0).branch(ClientScriptOpCode.BRANCH, end).label(yes).integer(1).label(end);
        }
        flow.integer(Integer.MIN_VALUE).op(ClientScriptOpCode.SWITCH, 0).integer(-900).branch(ClientScriptOpCode.BRANCH, "after-switch")
            .label("switch-first").integer(900).branch(ClientScriptOpCode.BRANCH, "after-switch")
            .label("switch-second").integer(-901).label("after-switch")
            .integer(1).op(ClientScriptOpCode.SWITCH, 1).integer(901)
            .integer(0).integer(1).integer(1).label("loop").branch(ClientScriptOpCode.BRANCH_IF_TRUE, "loop")
            .integer(9).op(ClientScriptOpCode.POP_INT_DISCARD).text("discard").op(ClientScriptOpCode.POP_STRING_DISCARD)
            .wide(9).op(ClientScriptOpCode.POP_LONG_DISCARD, -123).op(ClientScriptOpCode.RETURN, 255);
        flow.switches.add(List.of(new Case(Integer.MIN_VALUE, "switch-first"), new Case(Integer.MIN_VALUE, "switch-second"), new Case(-1, "switch-second")));
        flow.switches.add(List.of(new Case(99, "after-switch")));
        execution(output, "vm-flow", fixture(output, "vm-flow", flow), Map.of());
        var strings = new Program(0, 1, 0, 0, 0, 0)
            .op(ClientScriptOpCode.JOIN_STRING, 0).op(ClientScriptOpCode.PUSH_STRING_LOCAL, 0).op(ClientScriptOpCode.JOIN_STRING, 1)
            .text(stringBytes.toByteArray()).op(ClientScriptOpCode.JOIN_STRING, 2).op(ClientScriptOpCode.POP_STRING_LOCAL, 0)
            .op(ClientScriptOpCode.PUSH_STRING_LOCAL, 0).op(ClientScriptOpCode.RETURN);
        execution(output, "vm-strings", fixture(output, "vm-strings", strings), Map.of());
        var wide = new Program().wide(Long.MIN_VALUE).wide(Long.MAX_VALUE).wide(-1).wide(0).op(ClientScriptOpCode.RETURN);
        execution(output, "vm-longs", fixture(output, "vm-longs", wide), Map.of());
        numeric(output);
        characters(output, args.length > 1 ? Path.of(args[1]) : null);
        strings(output);
        variables(output);
        System.out.println("Generated metadata for " + ClientScriptOpCode.values().length + " opcodes and isolated Kotlin decoder/VM fixtures in " + output);
    }
}
