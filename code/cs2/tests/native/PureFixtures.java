import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.CameraLoginPrefOps;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.MathStringOps;
import java.nio.file.*;
import java.util.*;

public final class PureFixtures {
    static final long SEED = 0x727c5220L;
    static final int[] EDGES = {
        Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -2147483000, -1000000, -100000, -12000, -11746, -11745, -11744,
        -1000, -401, -400, -100, -13, -12, -2, -1, 0, 1, 2, 3, 4, 5, 11, 12, 13, 27, 28, 29, 30, 31, 32, 99, 100,
        101, 399, 400, 401, 1000, 1581, 1582, 1583, 1600, 1969, 1970, 1971, 1999, 2000, 2024, 2100, 2400, 9999,
        10000, 12000, 100000, 1000000, 2147483000, Integer.MAX_VALUE - 1, Integer.MAX_VALUE
    };
    static final String[] TEXTS = {
        "", "a", "A", "Hello, World!", "MIXED Case 123 <col=ff0000>TAG</col>", "ÀÉÎÕÜ",
        "İ", "İ", "Σ", "ΑΣ", "ΑΣΑ", "ΑΣ ", "Α Σ", "ΑΣ.",
        "ΑΣ́", "Α.Σ", "ΣΑ", "ẞ", "K", "Ω", "Å", "Ǆǅǆ",
        "𐐀", "𝐀", "😀ABC", "A𐐀B", "İİ", "ǅ", "ß", "ŉ",
        "Ἀᾈ", "ꞮꞰ", "ᲐᲑ", "ႠჅ", "ⅠⅯ", "ⒶⓏ", "ＡＺ",
        "\u0000\u0001\u007f\u0080\u009f", "  \t\n\r", "ΣΣΣ", "ΑΣΣ", "ΙͅΣ"
    };

    record Case(String name, ClientScriptOpCode opcode, int operand, int[] ints, String text, boolean hasText) {}

    static void add(List<Case> cases, String name, ClientScriptOpCode opcode, int operand, int... ints) {
        cases.add(new Case(name, opcode, operand, ints, null, false));
    }

    static List<Case> cases() {
        var cases = new ArrayList<Case>();
        var random = new Random(SEED);
        var lower = ClientScriptOpCode.LOWERCASE;
        for (int i = 0; i < TEXTS.length; i++) cases.add(new Case("text-" + i, lower, i & 255, new int[0], TEXTS[i], true));
        cases.add(new Case("null", lower, 0, new int[0], null, true));
        var chunk = new StringBuilder();
        int chunkIndex = 0;
        for (int unit = 0; unit < 65536; unit++) {
            if (unit >= 0xd800 && unit <= 0xdfff) continue;
            String one = String.valueOf((char) unit);
            chunk.append(one);
            if (chunk.length() == 128) {
                cases.add(new Case("unit-chunk-" + chunkIndex++, lower, chunkIndex & 255, new int[0], chunk.toString(), true));
                chunk.setLength(0);
            }
            if (Character.isUpperCase(unit) || Character.isTitleCase(unit) || unit == 0x3a3 || Character.toLowerCase(unit) != unit) {
                cases.add(new Case("unit-" + unit, lower, unit & 255, new int[0], one, true));
                cases.add(new Case("pad-" + unit, lower, 1, new int[0], "x" + one + "Y" + one, true));
                cases.add(new Case("sigma-" + unit, lower, 2, new int[0], "Α" + one + "Σ" + one + "Σ ", true));
            }
        }
        if (chunk.length() > 0) cases.add(new Case("unit-chunk-" + chunkIndex, lower, 0, new int[0], chunk.toString(), true));
        chunk.setLength(0);
        for (int point = 0x10000; point <= 0x10ffff; point++) {
            chunk.appendCodePoint(point);
            if (chunk.codePointCount(0, chunk.length()) == 128 || point == 0x10ffff) {
                if (point < 0x20000 || (point >> 7) % 16 == 0) cases.add(new Case("point-chunk-" + point, lower, point & 255, new int[0], chunk.toString(), true));
                chunk.setLength(0);
            }
            if (Character.isUpperCase(point) || Character.isTitleCase(point) || Character.toLowerCase(point) != point) {
                cases.add(new Case("point-" + point, lower, point & 255, new int[0], new String(Character.toChars(point)), true));
                cases.add(new Case("point-pad-" + point, lower, 1, new int[0], "Σ" + new String(Character.toChars(point)) + "Σ", true));
            }
        }
        for (int n = 0; n < 2000; n++) {
            var builder = new StringBuilder();
            int length = random.nextInt(40);
            for (int i = 0; i < length; i++) {
                int pick = random.nextInt(6);
                int point = switch (pick) {
                    case 0 -> 'A' + random.nextInt(26);
                    case 1 -> 0x391 + random.nextInt(0x40);
                    case 2 -> 0x410 + random.nextInt(0x40);
                    case 3 -> 0x1e00 + random.nextInt(0x200);
                    case 4 -> 0x10400 + random.nextInt(0x50);
                    default -> 0x20 + random.nextInt(0xe0);
                };
                builder.appendCodePoint(point);
            }
            cases.add(new Case("random-" + n, lower, n & 255, new int[0], builder.toString(), true));
        }
        var leap = ClientScriptOpCode.DATE_ISLEAPYEAR;
        for (int year = -5000; year <= 8000; year++) add(cases, "year-" + year, leap, year & 255, year);
        for (int i = 0; i < EDGES.length; i++) add(cases, "year-edge-" + i, leap, i, EDGES[i]);
        for (int n = 0; n < 2000; n++) add(cases, "year-random-" + n, leap, n & 255, random.nextInt());
        var minutes = ClientScriptOpCode.DATE_MINUTES_FROMRUNEDAY;
        for (int day = -20000; day <= 30000; day += 7) add(cases, "days-" + day, minutes, day & 255, day);
        for (int i = 0; i < EDGES.length; i++) {
            add(cases, "days-edge-" + i, minutes, i, EDGES[i]);
            for (int d = -2; d <= 2; d++) add(cases, "days-edge-" + i + "-" + d, minutes, i, EDGES[i] + d);
        }
        for (int n = 0; n < 4000; n++) add(cases, "days-random-" + n, minutes, n & 255, random.nextInt());
        var from = ClientScriptOpCode.DATE_RUNEDAY_FROMDATE;
        int[] years = {-4713, -401, -400, -101, -100, -4, -1, 0, 1, 3, 4, 5, 100, 400, 1000, 1500, 1580, 1581, 1583, 1584, 1600, 1700, 1900, 1969, 1970, 1971, 1999, 2000, 2001, 2024, 2100, 2400, 3000, 9999, 100000};
        for (int year : years) {
            for (int month = -14; month <= 26; month += 1) {
                for (int day : new int[] {-40, -31, -1, 0, 1, 2, 15, 28, 29, 30, 31, 32, 60, 366, 400}) {
                    add(cases, "grid-" + year + "-" + month + "-" + day, from, (month + 14) & 255, day, month, year);
                }
            }
        }
        for (int year = 1581; year <= 1583; year++) {
            for (int month = -3; month <= 14; month++) {
                for (int day = -45; day <= 75; day++) add(cases, "cutover-" + year + "-" + month + "-" + day, from, day & 255, day, month, year);
            }
        }
        for (int year = 1966; year <= 1972; year++) {
            for (int month = 0; month < 12; month++) {
                for (int day = 1; day <= 31; day++) add(cases, "epoch-" + year + "-" + month + "-" + day, from, day, day, month, year);
            }
        }
        for (int i = 0; i < EDGES.length; i++) {
            for (int j = 0; j < EDGES.length; j += 3) {
                add(cases, "edge-day-" + i + "-" + j, from, 0, EDGES[i], 5, 2000 + (EDGES[j] % 3000));
                add(cases, "edge-month-" + i + "-" + j, from, 1, 15, EDGES[i], 2000 + (EDGES[j] % 3000));
                add(cases, "edge-year-" + i + "-" + j, from, 2, 15, EDGES[j] % 24, EDGES[i]);
            }
        }
        for (int n = 0; n < 6000; n++) add(cases, "random-narrow-" + n, from, n & 255, random.nextInt(800) - 200, random.nextInt(60) - 20, random.nextInt(5000) - 1000);
        for (int n = 0; n < 4000; n++) add(cases, "random-wide-" + n, from, n & 255, random.nextInt(), random.nextInt(), random.nextInt());
        for (int n = 0; n < 2000; n++) add(cases, "random-years-" + n, from, n & 255, random.nextInt(60) - 10, random.nextInt(40) - 10, random.nextInt() / (1 + random.nextInt(2000000)));
        for (var opcode : new ClientScriptOpCode[] {leap, minutes}) {
            cases.add(new Case("underflow-" + opcode.name(), opcode, 0, new int[0], null, false));
        }
        for (int size = 0; size < 3; size++) cases.add(new Case("underflow-from-" + size, from, 0, Arrays.copyOf(new int[] {1, 2}, size), null, false));
        return cases;
    }

    static String integers(int[] values, int length) {
        if (length == 0) return "-";
        var builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            if (i > 0) builder.append(',');
            builder.append(values[i]);
        }
        return builder.toString();
    }

    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        var cases = cases();
        var executor = new CS2Executor();
        executor.longStackPtr = 1;
        executor.longStack[0] = 727727727727L;
        executor.intOpValues = new int[1];
        var out = new StringBuilder();
        var names = new HashSet<String>();
        var counts = new TreeMap<String, int[]>();
        for (var entry : cases) {
            if (!names.add(entry.opcode.name() + ":" + entry.name)) throw new AssertionError(entry.name);
            Arrays.fill(executor.intStack, 0x5a5a5a5a);
            Arrays.fill(executor.stringStack, "stale");
            executor.intStackPtr = entry.ints.length;
            System.arraycopy(entry.ints, 0, executor.intStack, 0, entry.ints.length);
            executor.stringStackPtr = entry.hasText ? 1 : 0;
            if (entry.hasText) executor.stringStack[0] = entry.text;
            executor.intOpValues[0] = entry.operand;
            String result = "ok";
            try {
                if (entry.opcode == ClientScriptOpCode.LOWERCASE) MathStringOps.dispatch(entry.opcode, executor);
                else CameraLoginPrefOps.dispatch(entry.opcode, executor);
            } catch (RuntimeException error) {
                result = "error:" + error.getClass().getSimpleName();
            }
            if (executor.longStackPtr != 1 || executor.longStack[0] != 727727727727L || executor.intOpValues[0] != entry.operand) throw new AssertionError(entry.name);
            boolean underflow = executor.intStackPtr < 0;
            String strings = executor.stringStackPtr == 1 ? NativeFixtures.string(executor.stringStack[0]) : executor.stringStackPtr == 0 ? "-" : "ptr" + executor.stringStackPtr;
            out.append(entry.name).append('\t').append(entry.opcode.name()).append('\t').append(entry.operand).append('\t')
                .append(integers(entry.ints, entry.ints.length)).append('\t').append(entry.hasText ? NativeFixtures.string(entry.text) : "-").append('\t')
                .append(result).append('\t').append(executor.intStackPtr).append('\t')
                .append(underflow ? "-" : integers(executor.intStack, executor.intStackPtr)).append('\t').append(strings).append('\n');
            counts.computeIfAbsent(entry.opcode.name() + "\t" + (result.equals("ok") ? "ok" : result), key -> new int[1])[0]++;
        }
        var table = new java.io.ByteArrayOutputStream();
        var tableOut = new java.io.DataOutputStream(table);
        for (int point = 0x10000; point <= 0x10ffff; point++) {
            executor.intStackPtr = 0;
            executor.stringStackPtr = 1;
            executor.stringStack[0] = new String(Character.toChars(point));
            MathStringOps.dispatch(ClientScriptOpCode.LOWERCASE, executor);
            String lowered = (String) executor.stringStack[0];
            if (lowered.codePointCount(0, lowered.length()) != 1) throw new AssertionError(point);
            int mapped = lowered.codePointAt(0);
            if (mapped != point) {
                tableOut.writeInt(point);
                tableOut.writeInt(mapped);
            }
        }
        Files.write(output.resolve("lower-supplementary.bin"), table.toByteArray());
        Files.writeString(output.resolve("pure.native.tsv"), out.toString());
        var stats = new StringBuilder();
        for (var entry : counts.entrySet()) stats.append(entry.getKey()).append('\t').append(entry.getValue()[0]).append('\n');
        Files.writeString(output.resolve("pure.stats.tsv"), stats.toString());
        System.out.println("Generated " + cases.size() + " pure oracle cases");
        System.exit(0);
    }
}
