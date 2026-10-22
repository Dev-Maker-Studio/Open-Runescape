import com.jagex.game.runetek5.client.Client;
import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.CameraLoginPrefOps;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.MathStringOps;
import com.jagex.game.runetek5.tools.Language;
import com.jagex.game.runetek5.utils.CalendarUtil;
import com.jagex.game.runetek5.utils.ColorUtil;
import com.jagex.game.runetek5.utils.Utils;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;

/**
 * Drives the real MathStringOps and CameraLoginPrefOps dispatch for the clock, random, hsv and
 * date operations. Math.random is seeded (the JDK holder generator is reached by reflection) so the
 * draw the real class consumed is known; Utils.time() and the local zone are injected.
 */
public final class ClockFixtures {
    static final long FAR = 1L << 62;
    static java.util.Random generator;
    static final StringBuilder out = new StringBuilder();
    static final TreeMap<String, Integer> counts = new TreeMap<>();
    static final TreeMap<String, Integer> errors = new TreeMap<>();
    static int serial;

    record Case(ClientScriptOpCode opcode, int[] ints, long millis, long offset, int language, long seed, boolean table) {}

    static void seed(long seed) {
        generator.setSeed(seed);
    }

    static void clock(long millis) {
        Utils.lastSystemTime = FAR;
        Utils.timeOffset = millis - FAR;
    }

    static boolean isDate(ClientScriptOpCode opcode) {
        return switch (opcode) {
            case DATE_MINUTES, DATE_RUNEDAY, DATE_YEAR, DATE_RUNEDAY_TODATE -> true;
            default -> false;
        };
    }

    static void run(Case entry) {
        var executor = new CS2Executor();
        var opcode = entry.opcode;
        Arrays.fill(executor.intStack, 0);
        Arrays.fill(executor.stringStack, null);
        System.arraycopy(entry.ints, 0, executor.intStack, 0, entry.ints.length);
        executor.intStackPtr = entry.ints.length;
        executor.stringStackPtr = 0;
        executor.longStackPtr = 0;
        clock(entry.millis);
        CalendarUtil.LOCAL_CALENDAR.setTimeZone(new SimpleTimeZone((int) entry.offset, "fixed"));
        Client.CURRENT_LANGUAGE = entry.language < 0 ? null : Language.getLanguage(entry.language);
        if (!entry.table && ColorUtil.getHsvToRgbTable() != null) {
            savedTable = ColorUtil.getHsvToRgbTable();
            ColorUtil.setHsvToRgbTable(null);
        } else if (entry.table && ColorUtil.getHsvToRgbTable() == null) {
            ColorUtil.setHsvToRgbTable(savedTable);
        }
        seed(entry.seed);
        var predicted = new java.util.Random(entry.seed);
        double first = predicted.nextDouble();
        double second = predicted.nextDouble();
        String result = "ok";
        try {
            if (isDate(opcode)) CameraLoginPrefOps.dispatch(opcode, executor);
            else MathStringOps.dispatch(opcode, executor);
        } catch (RuntimeException error) {
            result = "error:" + error.getClass().getSimpleName();
        }
        double next = Math.random();
        int draws;
        if (next == first) draws = 0;
        else if (next == second) draws = 1;
        else throw new AssertionError("unexpected random consumption " + opcode + " " + Arrays.toString(entry.ints));
        counts.merge(opcode.name(), 1, Integer::sum);
        if (!result.equals("ok")) errors.merge(opcode.name() + ":" + result, 1, Integer::sum);
        if (executor.longStackPtr != 0 || executor.intStackPtr < 0 || executor.stringStackPtr < 0) throw new AssertionError(opcode.name());
        var strings = new String[executor.stringStackPtr];
        for (int i = 0; i < strings.length; i++) strings[i] = (String) executor.stringStack[i];
        out.append(opcode.name().toLowerCase()).append('-').append(serial++).append('\t').append(opcode.name()).append('\t')
            .append(NativeFixtures.integers(entry.ints)).append('\t').append(entry.millis).append('\t').append(entry.offset)
            .append('\t').append(entry.language).append('\t').append(Double.doubleToLongBits(first)).append('\t')
            .append(entry.table ? 1 : 0).append('\t').append(draws).append('\t').append(result).append('\t')
            .append(NativeFixtures.integers(Arrays.copyOf(executor.intStack, executor.intStackPtr))).append('\t')
            .append(NativeFixtures.texts(strings)).append('\n');
    }

    static int[] savedTable;

    static void add(List<Case> cases, ClientScriptOpCode opcode, long millis, long offset, int language, long seed, boolean table, int... ints) {
        cases.add(new Case(opcode, ints, millis, offset, language, seed, table));
    }

    static final long DAY = 86_400_000L;
    static final long[] OFFSETS = {0, 5 * 3_600_000L, -8 * 3_600_000L, 5 * 3_600_000L + 45 * 60_000L, 14 * 3_600_000L, -12 * 3_600_000L};

    static List<Case> cases() throws Exception {
        var cases = new ArrayList<Case>();
        long[] seeds = {1, 2, 3, 42, 727, 123456789L, -5, 0x7fffffffffffL};
        int[] randomValues = {0, 1, 2, 3, 10, 100, 1000, 65535, Integer.MAX_VALUE, -1, -2, -100, Integer.MIN_VALUE, Integer.MAX_VALUE - 1, 255, 256};
        for (var opcode : new ClientScriptOpCode[] {ClientScriptOpCode.RANDOM, ClientScriptOpCode.RANDOMINC}) {
            for (int value : randomValues) for (long seed : seeds) add(cases, opcode, 0, 0, 0, seed, false, value);
        }
        int[] pitches = {0, 1, 10, 100, 256, 699, 700, 701, 800, 2147483647, -1, -100, -700, -701, -800, -2147483648, 2147483646, 65536};
        for (int down : pitches) for (int up : pitches) {
            for (long seed : new long[] {1, 2, 3, 99}) add(cases, ClientScriptOpCode.RANDOM_SOUND_PITCH, 0, 0, 0, seed, false, down, up);
        }
        // hsv: the real seeded table is shipped separately
        var inputs = new TreeSet<Integer>();
        for (int i = 0; i < 65536; i += 97) inputs.add(i);
        for (int i : new int[] {0, 1, 2, 63, 64, 65, 126, 127, 128, 0x7f, 0x80, 0x3ff, 0x400, 0xffff, 0x10000, 0x10001, -1, -2, -128,
            Integer.MIN_VALUE, Integer.MAX_VALUE, 0x7fff, 0x8000, 0x1fc00, 0xfc00, 0x7e00, 0x3f80, 0x3f7f}) inputs.add(i);
        var random = new java.util.Random(7);
        for (int i = 0; i < 400; i++) inputs.add(random.nextInt());
        for (int value : inputs) add(cases, ClientScriptOpCode.HSVTORGB, 0, 0, 0, 1, true, value);
        add(cases, ClientScriptOpCode.HSVTORGB, 0, 0, 0, 1, false, 5);
        add(cases, ClientScriptOpCode.HSVTORGB, 0, 0, 0, 1, false, 0);
        // anchor dates come from the real calendar
        var runedays = new TreeSet<Integer>();
        int[] years = {-4713, -4712, -401, -400, -101, -100, -5, -4, -2, -1, 0, 1, 2, 3, 4, 5, 99, 100, 101, 200, 400, 1000, 1499, 1500,
            1581, 1582, 1583, 1584, 1600, 1700, 1800, 1900, 1969, 1970, 1971, 2000, 2012, 2024, 2038, 2100, 2400, 5000, 9999, 100000};
        int[] months = {0, 1, 2, 9, 11};
        int[] days = {1, 5, 15, 29, 31};
        for (int year : years) for (int month : months) for (int day : days) {
            long noon = CalendarUtil.getGmtNoon(day, month, year);
            long runeday = Math.floorDiv(noon, DAY) - 11745;
            for (int shift = -1; shift <= 1; shift++) if (shift == 0 || year == 1582 || year == 1 || year == 0 || year == 1970) runedays.add((int) (runeday + shift));
        }
        for (int i : new int[] {0, 1, -1, -11745, -11746, -153171, -153172, -153173, -153174, -153175, -730906, -730907, -730908,
            Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE - 1, Integer.MIN_VALUE + 1, Integer.MAX_VALUE - 11745, Integer.MAX_VALUE - 11746,
            Integer.MIN_VALUE - 11745, 20000, 20454, 21000}) runedays.add(i);
        int index = 0;
        for (int runeday : runedays) {
            add(cases, ClientScriptOpCode.DATE_RUNEDAY_TODATE, 0, OFFSETS[index % OFFSETS.length], 0, 1, false, runeday);
            if (index % 5 == 0) add(cases, ClientScriptOpCode.DATE_RUNEDAY_TODATE, 0, OFFSETS[(index + 3) % OFFSETS.length], 0, 1, false, runeday);
            add(cases, ClientScriptOpCode.FROMDATE, 0, OFFSETS[(index + 1) % OFFSETS.length], index % 6, 1, false, runeday);
            add(cases, ClientScriptOpCode.FROMDATE, 0, OFFSETS[(index + 2) % OFFSETS.length], (index + 3) % 6, 1, false, runeday);
            index++;
        }
        add(cases, ClientScriptOpCode.FROMDATE, 0, 0, -1, 1, false, 0);
        add(cases, ClientScriptOpCode.FROMDATE, 0, 0, 6, 1, false, 0);
        // minutes and clock reads
        var millis = new TreeSet<Long>();
        int step = 0;
        for (long runeday : runedays) {
            if (step++ % 4 != 0 && Math.abs(runeday) < Integer.MAX_VALUE - 20000) continue;
            long noon = (runeday + 11745) * DAY + DAY / 2;
            millis.add(noon);
            millis.add(noon - DAY / 2);
            millis.add(noon + DAY / 2 - 1);
        }
        for (long value : new long[] {0, -1, 1, 59_999, 60_000, -60_000, -60_001, 86_399_999, DAY, -DAY, -DAY - 1, 1_761_000_000_000L, 1_791_000_000_000L,
            4_102_444_800_000L, -62_135_769_600_000L, -62_135_769_600_001L, -62_167_219_200_000L, -62_167_219_200_001L, 253_402_300_799_999L,
            253_402_300_800_000L, 3_000_000_000_000_000L, -3_000_000_000_000_000L, (long) Integer.MAX_VALUE * 60_000L, (long) Integer.MIN_VALUE * 60_000L,
            ((long) Integer.MAX_VALUE + 1) * 60_000L, ((long) Integer.MIN_VALUE - 1) * 60_000L}) millis.add(value);
        index = 0;
        for (long value : millis) {
            add(cases, ClientScriptOpCode.DATE_MINUTES, value, 0, 0, 1, false);
            add(cases, ClientScriptOpCode.DATE_RUNEDAY, value, 0, 0, 1, false);
            add(cases, ClientScriptOpCode.DATE_YEAR, value, OFFSETS[index % OFFSETS.length], 0, 1, false);
            index++;
        }
        var minutes = new TreeSet<Integer>();
        for (long value : millis) {
            long minute = Math.floorDiv(value, 60_000L);
            if (minute >= Integer.MIN_VALUE && minute <= Integer.MAX_VALUE) {
                minutes.add((int) minute);
                minutes.add((int) minute - 1);
            }
        }
        for (int value : new int[] {0, 1, -1, 59, 60, 1439, 1440, 1441, -1440, -1441, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE - 1, Integer.MIN_VALUE + 1}) minutes.add(value);
        index = 0;
        for (int value : minutes) {
            add(cases, ClientScriptOpCode.FORMAT_DATETIME_FROM_MINUTES, 0, OFFSETS[index % OFFSETS.length], index % 6, 1, false, value);
            if (index % 40 == 0) {
                for (int language = 0; language <= 5; language++) {
                    if (language != index % 6) add(cases, ClientScriptOpCode.FORMAT_DATETIME_FROM_MINUTES, 0, 0, language, 1, false, value);
                }
            }
            index++;
        }
        add(cases, ClientScriptOpCode.FORMAT_DATETIME_FROM_MINUTES, 0, 0, -1, 1, false, 0);
        return cases;
    }

    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        Field holder = Class.forName("java.lang.Math$RandomNumberGeneratorHolder").getDeclaredField("randomNumberGenerator");
        holder.setAccessible(true);
        generator = (java.util.Random) holder.get(null);
        seed(20120727L);
        ColorUtil.calculateHsvToRgbTable();
        int[] table = ColorUtil.getHsvToRgbTable();
        if (table == null || table.length != 65536) throw new AssertionError("hsv table");
        savedTable = table;
        var buffer = ByteBuffer.allocate(table.length * 4);
        for (int value : table) buffer.putInt(value);
        Files.write(output.resolve("hsv.native.bin"), buffer.array());
        var cases = cases();
        for (var entry : cases) run(entry);
        Files.writeString(output.resolve("clock.native.tsv"), out);
        System.out.println("Native clock cases " + counts);
        System.out.println("Native clock error classes " + errors);
        System.out.println("Generated " + cases.size() + " actual native clock dispatch cases");
        System.out.flush();
        Runtime.getRuntime().halt(0);
    }
}
