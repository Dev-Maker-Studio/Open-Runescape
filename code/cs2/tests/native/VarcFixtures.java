import com.jagex.game.runetek5.client.Client;
import com.jagex.game.runetek5.client.periodic.PulseEvent;
import com.jagex.game.runetek5.client.periodic.PulseEventHandler;
import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.CoreVmOps;
import com.jagex.game.runetek5.config.varc.VarcTypeList;
import com.jagex.game.runetek5.utils.Utils;
import java.nio.file.*;
import java.util.*;

public final class VarcFixtures {
    static final ClientScriptOpCode[] OPERATIONS = {
        ClientScriptOpCode.PUSH_VARC_INT, ClientScriptOpCode.POP_VARC_INT,
        ClientScriptOpCode.PUSH_VARC_STRING, ClientScriptOpCode.POP_VARC_STRING
    };
    static final int[] ORDINALS = {26, 27, 31, 32};
    static final int[] WIRES = {311, 510, 700, 21};
    static final int[] EDGES = {Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -65536, -1, 0, 1, 65535, Integer.MAX_VALUE - 1, Integer.MAX_VALUE};
    static final String[] TEXTS = {null, "", "plain", "a😀b", "😀😀", "\0é€Œ—Ÿ", "<col=ff00ff>x</col>", "line\n\ttab", "ΣİıßſK", "𐐀🚀"};
    static final long[] LONGS = {Long.MIN_VALUE, 727727727727L, Long.MAX_VALUE};
    static final long SEED = 0x727c5210L;
    record Step(ClientScriptOpCode opcode, int id, int[] ints, String[] strings, long[] longs) {}
    record VarcCase(String name, int[] ints, String[] strings, boolean[] persistent, boolean dirty, List<Step> steps) {}
    record State(String ints, String strings, String persistent, String dirty, String cache, String low) {}

    static int[] defaults(int size) {
        int[] values = new int[size];
        Arrays.fill(values, -1);
        return values;
    }

    static Step step(ClientScriptOpCode opcode, int id, Object value) {
        int[] ints = opcode == ClientScriptOpCode.POP_VARC_INT ? new int[] {727, (Integer) value} : new int[] {727};
        String[] strings = opcode == ClientScriptOpCode.POP_VARC_STRING ? new String[] {"lower-varc-string", (String) value} : new String[] {"lower-varc-string"};
        return new Step(opcode, id, ints, strings, LONGS.clone());
    }

    static VarcCase single(String name, ClientScriptOpCode opcode, int id, int initial, String text, boolean persistent, boolean dirty, Object value) {
        int[] ints = defaults(8);
        String[] strings = new String[8];
        boolean[] flags = new boolean[8];
        if (id >= 0 && id < 8) {
            ints[id] = initial;
            strings[id] = text;
            flags[id] = persistent;
        }
        return new VarcCase(name, ints, strings, flags, dirty, List.of(step(opcode, id, value)));
    }

    static List<VarcCase> cases() {
        var cases = new ArrayList<VarcCase>();
        for (int id = 0; id < 8; id++) {
            cases.add(single("default-int-" + id, OPERATIONS[0], id, -1, null, false, false, null));
            cases.add(single("default-string-" + id, OPERATIONS[2], id, -1, null, false, false, null));
        }
        for (int id : new int[] {0, 1, 7}) for (int persistent = 0; persistent < 2; persistent++) for (int dirty = 0; dirty < 1; dirty++) {
            String suffix = "-id-" + id + "-persistent-" + persistent + "-dirty-" + dirty;
            for (int n = 0; n < EDGES.length; n++) {
                cases.add(single("read-int-" + n + suffix, OPERATIONS[0], id, EDGES[n], TEXTS[n], persistent != 0, dirty != 0, null));
                cases.add(single("write-int-" + n + suffix, OPERATIONS[1], id, ~EDGES[n], TEXTS[n], persistent != 0, dirty != 0, EDGES[n]));
                cases.add(single("same-int-" + n + suffix, OPERATIONS[1], id, EDGES[n], TEXTS[n], persistent != 0, dirty != 0, EDGES[n]));
            }
            for (int n = 0; n < TEXTS.length; n++) {
                cases.add(single("read-string-" + n + suffix, OPERATIONS[2], id, 100 + n, TEXTS[n], persistent != 0, dirty != 0, null));
                cases.add(single("write-string-" + n + suffix, OPERATIONS[3], id, 100 + n, "before", persistent != 0, dirty != 0, TEXTS[n]));
                cases.add(single("same-string-" + n + suffix, OPERATIONS[3], id, 100 + n, TEXTS[n], persistent != 0, dirty != 0, TEXTS[n]));
            }
        }
        var random = new Random(SEED);
        for (var opcode : OPERATIONS) for (int n = 0; n < 64; n++) {
            int initial = random.nextInt(), value = random.nextInt(), id = random.nextInt(8);
            String text = TEXTS[random.nextInt(TEXTS.length)];
            Object argument = opcode == OPERATIONS[1] ? Integer.valueOf(value) : opcode == OPERATIONS[3] ? text : null;
            cases.add(single("random-" + opcode.name() + "-" + n, opcode, id, initial, text, random.nextBoolean(), false, argument));
        }
        for (var opcode : OPERATIONS) for (int id : new int[] {Integer.MIN_VALUE, -1, 8, 65535, Integer.MAX_VALUE}) {
            cases.add(single("bounds-" + opcode.name() + "-" + id, opcode, id, -1, null, false, false,
                opcode == OPERATIONS[1] ? Integer.valueOf(42) : opcode == OPERATIONS[3] ? "bounds" : null));
        }
        for (var opcode : new ClientScriptOpCode[] {OPERATIONS[1], OPERATIONS[3]}) for (int id : new int[] {-1, 0, 7, 8}) {
            var empty = new Step(opcode, id, opcode == OPERATIONS[1] ? new int[0] : new int[] {727},
                opcode == OPERATIONS[3] ? new String[0] : new String[] {"lower-varc-string"}, LONGS.clone());
            cases.add(new VarcCase("underflow-" + opcode.name() + "-" + id, defaults(8), new String[8], new boolean[8], false, List.of(empty)));
        }
        for (var opcode : new ClientScriptOpCode[] {OPERATIONS[0], OPERATIONS[2]}) for (int id : new int[] {0, 8}) {
            int[] ints = opcode == OPERATIONS[0] ? new int[1000] : new int[] {727};
            String[] strings = opcode == OPERATIONS[2] ? new String[1000] : new String[] {"lower-varc-string"};
            Arrays.fill(ints, 727);
            Arrays.fill(strings, "lower-varc-string");
            cases.add(new VarcCase("overflow-" + opcode.name() + "-" + id, defaults(8), new String[8], new boolean[8], false,
                List.of(new Step(opcode, id, ints, strings, LONGS.clone()))));
        }
        for (boolean dirty : new boolean[] {false}) {
            cases.add(new VarcCase("short-flags-dirty-" + dirty, defaults(8), new String[8], new boolean[7], dirty,
                List.of(step(OPERATIONS[1], 7, 42), step(OPERATIONS[1], 7, 42), step(OPERATIONS[0], 7, null))));
            boolean[] flags = {false, true, false, true, false, false, false, true};
            cases.add(new VarcCase("coalesce-dirty-" + dirty, defaults(8), new String[8], flags, dirty, List.of(
                step(OPERATIONS[1], 3, 42), step(OPERATIONS[1], 1, Integer.MIN_VALUE), step(OPERATIONS[3], 3, null),
                step(OPERATIONS[3], 1, ""), step(OPERATIONS[1], 3, 42), step(OPERATIONS[3], 3, null),
                step(OPERATIONS[1], 7, Integer.MAX_VALUE), step(OPERATIONS[3], 7, "a😀b"),
                step(OPERATIONS[1], 0, -1), step(OPERATIONS[3], 0, "last"), step(OPERATIONS[1], 1, Integer.MAX_VALUE),
                step(OPERATIONS[3], 1, "nonempty"), step(OPERATIONS[3], 7, "a😀b"), step(OPERATIONS[1], 3, -42),
                step(OPERATIONS[0], 3, null), step(OPERATIONS[2], 3, null), step(OPERATIONS[0], 0, null), step(OPERATIONS[2], 1, null))));
        }
        boolean[] wideFlags = new boolean[258];
        wideFlags[257] = true;
        cases.add(new VarcCase("wide-id-257", defaults(258), new String[258], wideFlags, false, List.of(
            step(OPERATIONS[0], 257, null), step(OPERATIONS[2], 257, null), step(OPERATIONS[1], 257, Integer.MIN_VALUE),
            step(OPERATIONS[3], 257, "𐐀🚀"), step(OPERATIONS[0], 257, null), step(OPERATIONS[2], 257, null),
            step(OPERATIONS[1], 257, Integer.MIN_VALUE), step(OPERATIONS[3], 257, "𐐀🚀"))));
        cases.add(new VarcCase("independent-lengths", defaults(2), new String[8], new boolean[] {false, true}, false, List.of(
            step(OPERATIONS[3], 7, "string-only-id"), step(OPERATIONS[2], 7, null), step(OPERATIONS[0], 7, null), step(OPERATIONS[1], 7, 81))));
        return cases;
    }

    static String flags(boolean[] values) {
        var output = new StringJoiner(",");
        for (boolean value : values) output.add(value ? "1" : "0");
        return values.length == 0 ? "-" : output.toString();
    }

    static final class Events {
        final IdentityHashMap<PulseEvent, Integer> identities = new IdentityHashMap<>();
        final IdentityHashMap<PulseEvent, Long> deadlines = new IdentityHashMap<>();
        int next = 1;

        void validate(long before, long after, Integer type, Integer id) {
            var cache = Collections.newSetFromMap(new IdentityHashMap<PulseEvent, Boolean>());
            for (PulseEvent event : PulseEventHandler.PULSE_EVENT_CACHE) {
                if (!cache.add(event) || event.primary != 0 || event.secondary != 0 || event.tertiary != 0 || event.string != null
                    || event.indexReference < 0 || event.getType() < 1 || event.getType() > 2) throw new AssertionError("cache event");
                long deadline = event.executeAfter();
                Long previous = deadlines.get(event);
                if (type == null || event.getType() != type || event.getValue() != id.longValue()) {
                    if (previous == null || previous != deadline) throw new AssertionError("unchanged deadline");
                } else if (deadline < before + 500 || deadline > after + 500) throw new AssertionError("500ms deadline");
                deadlines.put(event, deadline);
            }
            var queued = Collections.newSetFromMap(new IdentityHashMap<PulseEvent, Boolean>());
            for (PulseEvent event : PulseEventHandler.LOW_PRIORITY) {
                if (!queued.add(event) || !cache.contains(event)) throw new AssertionError("queue identity");
                identities.computeIfAbsent(event, key -> next++);
            }
            if (!cache.equals(queued) || PulseEventHandler.HIGH_PRIORITY.size() != 0) throw new AssertionError("queue membership");
        }

        String encode(Iterable<PulseEvent> events) {
            var output = new StringJoiner(",");
            for (PulseEvent event : events) {
                Integer identity = identities.get(event);
                if (identity == null) throw new AssertionError("unobserved event");
                output.add(identity + ":" + event.getType() + ":" + event.getValue() + ":500");
            }
            return output.length() == 0 ? "-" : output.toString();
        }
    }

    static State state(Events events) {
        return new State(NativeFixtures.integers(VarcTypeList.VARC_INT), NativeFixtures.texts(VarcTypeList.VARC_STRING),
            flags(VarcTypeList.VARC_PERSISTENT), Client.clientIsActive ? "1" : "0",
            events.encode(PulseEventHandler.PULSE_EVENT_CACHE), events.encode(PulseEventHandler.LOW_PRIORITY));
    }

    static void reset(int[] ints, String[] strings, boolean[] persistent, boolean dirty) {
        PulseEventHandler.clearAllPulseEvents();
        VarcTypeList.VARC_INT = ints.clone();
        VarcTypeList.VARC_STRING = strings.clone();
        VarcTypeList.VARC_PERSISTENT = persistent.clone();
        Client.clientIsActive = dirty;
    }

    static String ints(CS2Executor exec) {
        return NativeFixtures.integers(Arrays.copyOf(exec.intStack, Math.max(0, Math.min(exec.intStack.length, exec.intStackPtr))));
    }

    static String strings(CS2Executor exec) {
        return NativeFixtures.texts(Arrays.copyOf(exec.stringStack, Math.max(0, Math.min(exec.stringStack.length, exec.stringStackPtr)), String[].class));
    }

    static String longs(CS2Executor exec) {
        return NativeFixtures.longs(Arrays.copyOf(exec.longStack, Math.max(0, Math.min(exec.longStack.length, exec.longStackPtr))));
    }

    static void nativeCases(Path output) throws Exception {
        var result = new StringBuilder();
        var names = new HashSet<String>();
        int count = 0;
        for (var entry : cases()) {
            if (!names.add(entry.name)) throw new AssertionError(entry.name);
            reset(entry.ints, entry.strings, entry.persistent, entry.dirty);
            var events = new Events();
            var exec = new CS2Executor();
            exec.intLocals = new int[] {727, Integer.MIN_VALUE};
            exec.objectLocals = new Object[] {"varc-local", null};
            exec.longLocals = new long[] {Long.MIN_VALUE};
            exec.intOpValues = new int[1];
            exec.instrPtr = 0;
            for (int index = 0; index < entry.steps.size(); index++) {
                var step = entry.steps.get(index);
                Arrays.fill(exec.intStack, 0);
                Arrays.fill(exec.stringStack, null);
                Arrays.fill(exec.longStack, 0);
                System.arraycopy(step.ints, 0, exec.intStack, 0, step.ints.length);
                System.arraycopy(step.strings, 0, exec.stringStack, 0, step.strings.length);
                System.arraycopy(step.longs, 0, exec.longStack, 0, step.longs.length);
                exec.intStackPtr = step.ints.length;
                exec.stringStackPtr = step.strings.length;
                exec.longStackPtr = step.longs.length;
                exec.intOpValues[0] = step.id;
                State before = state(events);
                String status = "ok";
                long start = Utils.time();
                try {
                    CoreVmOps.dispatch(step.opcode, exec);
                } catch (RuntimeException error) {
                    status = "error:" + error.getClass().getSimpleName() + ":" + (error.getMessage() == null ? "-" : error.getMessage());
                }
                long end = Utils.time();
                Integer type = step.opcode == OPERATIONS[1] ? Integer.valueOf(1) : step.opcode == OPERATIONS[3] ? Integer.valueOf(2) : null;
                events.validate(start, end, type, step.id);
                State after = state(events);
                if (!Arrays.equals(exec.intLocals, new int[] {727, Integer.MIN_VALUE})
                    || !Arrays.equals(exec.objectLocals, new Object[] {"varc-local", null}) || exec.longLocals[0] != Long.MIN_VALUE
                    || exec.intOpValues[0] != step.id || exec.instrPtr != 0 || !Arrays.equals(step.longs, Arrays.copyOf(exec.longStack, exec.longStackPtr))) throw new AssertionError(entry.name);
                if ((step.opcode == OPERATIONS[0] || step.opcode == OPERATIONS[1])
                    && (exec.stringStackPtr != step.strings.length || !strings(exec).equals(NativeFixtures.texts(step.strings)))) throw new AssertionError("cross string stack");
                if ((step.opcode == OPERATIONS[2] || step.opcode == OPERATIONS[3])
                    && (exec.intStackPtr != step.ints.length || !ints(exec).equals(NativeFixtures.integers(step.ints)))) throw new AssertionError("cross int stack");
                result.append(String.join("\t", entry.name, Integer.toString(index), step.opcode.name(), Integer.toString(step.id),
                    NativeFixtures.integers(step.ints), NativeFixtures.texts(step.strings), NativeFixtures.longs(step.longs),
                    before.ints, before.strings, before.persistent, before.dirty, before.cache, before.low, status,
                    Integer.toString(exec.intStackPtr), ints(exec), Integer.toString(exec.stringStackPtr), strings(exec),
                    Integer.toString(exec.longStackPtr), longs(exec), after.ints, after.strings, after.dirty, after.cache, after.low)).append('\n');
                count++;
            }
        }
        Files.writeString(output.resolve("varc.native.tsv"), result);
        System.out.println("Generated " + count + " actual native VARC dispatch steps in " + names.size() + " cases");
    }

    static void stateRecord(StringBuilder result, String phase, State state) {
        result.append(phase).append("\tints\t").append(state.ints).append('\n');
        result.append(phase).append("\tstrings\t").append(state.strings).append('\n');
        result.append(phase).append("\tpersistent\t").append(state.persistent).append('\n');
        result.append(phase).append("\tdirty\t").append(state.dirty).append('\n');
        result.append(phase).append("\tcache\t").append(state.cache).append('\n');
        result.append(phase).append("\tlow-priority\t").append(state.low).append('\n');
    }

    static void execution(Path output) throws Exception {
        var child = new NativeFixtures.Program(1, 2, 1, 1, 1, 1)
            .op(ClientScriptOpCode.PUSH_INT_LOCAL, 0).op(OPERATIONS[1], 1)
            .op(ClientScriptOpCode.PUSH_STRING_LOCAL, 0).op(OPERATIONS[3], 1)
            .op(ClientScriptOpCode.PUSH_STRING_LOCAL, 1).op(OPERATIONS[3], 3)
            .op(OPERATIONS[0], 1).op(OPERATIONS[2], 1).op(OPERATIONS[2], 3)
            .op(ClientScriptOpCode.PUSH_LONG_LOCAL, 0).op(ClientScriptOpCode.RETURN);
        var childScript = NativeFixtures.fixture(output, "vm-varcs-child", child);
        childScript.pointer = 900026;
        var root = new NativeFixtures.Program(1, 2, 1, 0, 0, 0)
            .integer(727).op(ClientScriptOpCode.POP_INT_LOCAL, 0)
            .text("caller-varc-local").op(ClientScriptOpCode.POP_STRING_LOCAL, 0)
            .wide(Long.MIN_VALUE + 727).op(ClientScriptOpCode.POP_LONG_LOCAL, 0)
            .integer(727).text("lower-varc-string").wide(727727727727L)
            .op(OPERATIONS[0], 0).op(OPERATIONS[2], 0)
            .integer(Integer.MIN_VALUE).op(OPERATIONS[1], 0)
            .integer(Integer.MIN_VALUE).op(OPERATIONS[1], 0)
            .integer(Integer.MAX_VALUE).op(OPERATIONS[1], 257)
            .text("").op(OPERATIONS[3], 0)
            .op(ClientScriptOpCode.PUSH_STRING_LOCAL, 1).op(OPERATIONS[3], 3)
            .op(OPERATIONS[2], 3).text("root-text").op(OPERATIONS[3], 257)
            .integer(-1).text("child-first").wide(Long.MAX_VALUE - 727).op(ClientScriptOpCode.GOSUB_WITH_PARAMS, 900026)
            .integer(0).text("").wide(Long.MIN_VALUE).op(ClientScriptOpCode.GOSUB_WITH_PARAMS, 900026)
            .op(OPERATIONS[0], 0).op(OPERATIONS[0], 1).op(OPERATIONS[0], 257)
            .op(OPERATIONS[2], 0).op(OPERATIONS[2], 1).op(OPERATIONS[2], 3).op(OPERATIONS[2], 257)
            .op(ClientScriptOpCode.PUSH_INT_LOCAL, 0).op(ClientScriptOpCode.PUSH_STRING_LOCAL, 0)
            .op(ClientScriptOpCode.PUSH_STRING_LOCAL, 1).op(ClientScriptOpCode.PUSH_LONG_LOCAL, 0).op(ClientScriptOpCode.RETURN);
        var rootScript = NativeFixtures.fixture(output, "vm-varcs", root);
        boolean[] persistent = new boolean[258];
        persistent[1] = true;
        persistent[257] = true;
        reset(defaults(258), new String[258], persistent, false);
        var events = new Events();
        var state = new StringBuilder();
        stateRecord(state, "before", state(events));
        long before = Utils.time();
        NativeFixtures.execution(output, "vm-varcs", rootScript, Map.of(900026, childScript));
        long after = Utils.time();
        for (PulseEvent event : PulseEventHandler.PULSE_EVENT_CACHE) {
            if (event.executeAfter() < before + 500 || event.executeAfter() > after + 500 || event.indexReference < 0
                || event.primary != 0 || event.secondary != 0 || event.tertiary != 0 || event.string != null) throw new AssertionError("VM deadline or payload");
            events.deadlines.put(event, event.executeAfter());
        }
        events.validate(before, after, null, null);
        stateRecord(state, "after", state(events));
        Files.writeString(output.resolve("vm-varcs.state.tsv"), state);
    }

    public static void main(String[] args) {
        int status = 1;
        try {
            for (int i = 0; i < OPERATIONS.length; i++) {
                var opcode = OPERATIONS[i];
                if (opcode.ordinal() != ORDINALS[i] || opcode.opcode != WIRES[i] || opcode.familyId != 0 || !opcode.hasIntConstant) throw new AssertionError(opcode);
            }
            var output = Path.of(args[0]);
            Files.createDirectories(output);
            nativeCases(output);
            execution(output);
            PulseEventHandler.clearAllPulseEvents();
            status = 0;
        } catch (Throwable error) {
            error.printStackTrace();
        } finally {
            System.exit(status);
        }
    }
}
