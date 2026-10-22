import com.jagex.game.runetek5.cache.node.lru.CacheableNode;
import com.jagex.game.runetek5.client.Client;
import com.jagex.game.runetek5.client.GameShell;
import com.jagex.game.runetek5.client.periodic.PulseEvent;
import com.jagex.game.runetek5.client.periodic.PulseEventHandler;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.param.ParamType;
import com.jagex.game.runetek5.config.param.ParamTypeList;
import com.jagex.game.runetek5.config.rsinterface.Component;
import com.jagex.game.runetek5.config.rsinterface.IFEvents;
import com.jagex.game.runetek5.config.rsinterface.Interface;
import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class ComponentOptionFixtures {
    static final String[] BARE = {
        "SETOP", "SETOPBASE", "SETOPCURSOR", "SETTARGETOPCURSOR", "SETOPCHAR", "SETOPTCHAR", "SETOPKEY", "SETOPTKEY",
        "SETPARAM_INT", "SETPARAM_STRING", "CLEAROPS", "SETTARGETCURSORS", "SETMOUSEOVERCURSOR", "GETOP", "GETOPBASE", "GETTARGETMASK"
    };
    static final int[] PARAM_IDS = {1, 2, 3, 4, 5, 6, 7, 8, 9};
    static final int[] PARAM_POOL = {1, 2, 3, 4, 5, 6, 7, 8, 9, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 10, -1, 12345};
    static final int[] SLOT_POOL = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 0, 11, 12, -1, -5, 20, Integer.MIN_VALUE, Integer.MAX_VALUE};
    static final int[] CURSOR_POOL = {0, 1, -1, 2, 5, 10, 11, 255, 256, -100, Integer.MAX_VALUE, Integer.MIN_VALUE, 0x7fff0000};
    static final int[] CHAR_POOL = {0, -1, 65, 97, 13, 32, 127, 128, 255, 256, 300, -300, 1, 2, Integer.MAX_VALUE, Integer.MIN_VALUE};
    static final int[] KEY_POOL = {0, 1, 13, 65, 84, 127, 128, 255, 256, 5, 82, -1, -2, Integer.MIN_VALUE};
    static final int[] MOD_POOL = {0, 1, 2, 4, 7, 8, -1, 255, 300, 128, Integer.MAX_VALUE};
    static final int[] VALUE_POOL = {0, 7, -1, 255, 1, 2, 100, Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 7, -1, 255};
    static final int[] SETTINGS_POOL = {0, -1, Integer.MAX_VALUE, Integer.MIN_VALUE, 0x7f << 11, 1 << 11, 0x55 << 11, 0x2a << 11, 0x1 << 17, 0x7f << 11 | 0xff, 5, 0x40 << 11};
    static final String[] TEXTS = {null, "", "a", "hello", "é€", "line\tbreak", "dflt", "x", "x".repeat(300)};
    static final long SEED = 0x727c5222L;
    static final int STEPS = 9000;
    static final Map<String, int[]> STATS = new TreeMap<>();

    static String[] names() {
        var list = new ArrayList<String>();
        for (var bare : BARE) {
            list.add("CC_" + bare);
            list.add("IF_" + bare);
        }
        list.add("CC_SETPARAM");
        list.add("CC_PARAM");
        return list.toArray(new String[0]);
    }

    static String kinds(String name) {
        boolean hash = name.startsWith("IF_");
        String bare = name.substring(name.indexOf('_') + 1);
        return switch (bare) {
            case "SETOP", "GETOP" -> "S";
            case "SETOPBASE", "CLEAROPS", "GETOPBASE", "GETTARGETMASK" -> "";
            case "SETOPCURSOR" -> "SC";
            case "SETTARGETOPCURSOR", "SETMOUSEOVERCURSOR" -> "C";
            case "SETOPCHAR" -> "SH";
            case "SETOPTCHAR" -> "H";
            case "SETOPKEY" -> hash ? "SKM" : "SKMKMKMKMKM";
            case "SETOPTKEY" -> "KM";
            case "SETPARAM_INT", "SETPARAM" -> "PV";
            case "SETPARAM_STRING", "PARAM" -> "P";
            case "SETTARGETCURSORS" -> "CC";
            default -> throw new IllegalStateException(name);
        };
    }

    static boolean wantsText(String name) {
        return name.endsWith("SETOP") || name.endsWith("SETOPBASE") || name.endsWith("SETPARAM_STRING");
    }

    static int pick(int[] pool, Random random) {
        return pool[random.nextInt(pool.length)];
    }

    static int[] generate(String name, Random random) {
        String kinds = kinds(name);
        boolean keyPairs = name.equals("CC_SETOPKEY");
        int validPairs = random.nextInt(6);
        int[] ints = new int[kinds.length()];
        for (int i = 0; i < ints.length; i++) {
            char kind = kinds.charAt(i);
            ints[i] = switch (kind) {
                case 'S' -> pick(SLOT_POOL, random);
                case 'C' -> pick(CURSOR_POOL, random);
                case 'H' -> pick(CHAR_POOL, random);
                case 'K' -> keyPairs ? ((i - 1) / 2 < validPairs ? Math.abs(pick(KEY_POOL, random) % 300) : (random.nextBoolean() ? -1 : pick(KEY_POOL, random))) : pick(KEY_POOL, random);
                case 'M' -> pick(MOD_POOL, random);
                case 'P' -> pick(PARAM_POOL, random);
                case 'V' -> pick(VALUE_POOL, random);
                default -> throw new IllegalStateException();
            };
        }
        return ints;
    }

    static String bytes(byte[] value) {
        if (value == null) return "null";
        var sb = new StringBuilder();
        for (int i = 0; i < value.length; i++) {
            if (i > 0) sb.append('/');
            sb.append(value[i]);
        }
        return sb.length() == 0 ? "empty" : sb.toString();
    }

    static String describe(Component c) {
        var fields = new ArrayList<String>();
        if (c.options == null) {
            fields.add("null");
        } else {
            var sb = new StringBuilder("" + c.options.length + "[");
            for (int i = 0; i < c.options.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(ComponentExtraFixtures.hex(c.options[i]));
            }
            fields.add(sb.append(']').toString());
        }
        fields.add(ComponentExtraFixtures.hex(c.opBase));
        if (c.opCursors == null) {
            fields.add("null");
        } else {
            var sb = new StringBuilder();
            for (int i = 0; i < c.opCursors.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(c.opCursors[i]);
            }
            fields.add(sb.toString());
        }
        fields.add("" + c.targetOverCursor);
        fields.add("" + c.targetLeaveCursor);
        fields.add("" + c.moveOverCursor);
        fields.add("" + c.targetOpCursor);
        if (c.keyCharCodes == null) {
            fields.add("null");
        } else {
            var sb = new StringBuilder();
            for (int i = 0; i < c.keyCharCodes.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(c.keyCharCodes[i]);
            }
            fields.add(sb.toString());
        }
        for (var arrays : new byte[][][] {c.keyTriggeringAction, c.keyTriggerParams}) {
            if (arrays == null) {
                fields.add("null");
            } else {
                var sb = new StringBuilder();
                for (int i = 0; i < arrays.length; i++) {
                    if (i > 0) sb.append(',');
                    sb.append(bytes(arrays[i]));
                }
                fields.add(sb.toString());
            }
        }
        fields.add(c.hasInteraction ? "1" : "0");
        fields.add("" + (c.clientParams == null ? 0 : c.clientParams.size()));
        var params = new StringBuilder();
        for (int id : PARAM_IDS) {
            if (params.length() > 0) params.append(',');
            params.append(c.getIntParam(id, 12345)).append('/').append(ComponentExtraFixtures.hex(c.getStringParam(id, "d")));
        }
        fields.add(params.toString());
        fields.add("" + c.targetParams.settingsHash);
        return String.join("\t", fields);
    }

    static void dump(Interface[] interfaces, int capacity, Map<String, String> out) {
        out.clear();
        for (int id = 0; id < capacity; id++) {
            var inter = interfaces[id];
            if (inter == null) continue;
            for (int index = 0; index < inter.components.length; index++) {
                var c = inter.components[index];
                if (c != null) out.put("i" + id + "." + index, describe(c));
            }
        }
    }

    static void dispatch(ClientScriptOpCode op, CS2Executor e) {
        String name = op.name();
        if (name.startsWith("IF_GET")) {
            InterfaceQueryOps.dispatch(op, e);
        } else if (name.startsWith("IF_")) {
            InterfaceSetterOps.dispatch(op, e);
        } else if (name.startsWith("CC_GET") || name.equals("CC_PARAM")) {
            ComponentQueryOps.dispatch(op, e);
        } else {
            ComponentSetterOps.dispatch(op, e);
        }
    }

    static ParamType paramType(int defaultInt, boolean text, String typeName) {
        var type = new ParamType();
        type.defaultInt = defaultInt;
        type.typeChar = text ? 's' : (char) 0;
        type.typeName = typeName;
        return type;
    }

    static void params(StringBuilder out) {
        var list = new ParamTypeList(null);
        Object[][] table = {
            {1, 0, false, null}, {2, 7, false, null}, {3, -1, false, null}, {4, 0, true, ""}, {5, 0, true, "dflt"},
            {6, 0, true, null}, {7, 255, false, null}, {8, 0, true, "x"}, {9, Integer.MIN_VALUE, false, null}
        };
        for (var row : table) {
            int id = (Integer) row[0];
            var type = paramType((Integer) row[1], (Boolean) row[2], (String) row[3]);
            list.cache.put(type, id);
            out.append("P\t").append(id).append('\t').append(type.defaultInt).append('\t').append((Boolean) row[2] ? "s" : "-").append('\t').append(ComponentExtraFixtures.hex(type.typeName)).append('\n');
        }
        TypeLists.PARAMS = list;
    }

    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        var out = new StringBuilder();
        var random = new Random(SEED);
        var names = names();
        var worlds = List.of(ComponentExtraFixtures.worldMain(), ComponentExtraFixtures.worldDense());
        var previous = new HashMap<String, String>();
        var current = new HashMap<String, String>();
        int total = 0;
        for (var w : worlds) {
            var interfaces = new Interface[w.capacity];
            var loaded = new boolean[w.capacity];
            Interface.INTERFACES = interfaces;
            Interface.INTERFACES_LOADED = loaded;
            GameShell.screenCanvasWidth = w.width;
            GameShell.screenCanvasHeight = w.height * ComponentExtraFixtures.inverse(-969250379);
            Client.Companion.setLastRenderedCycle(-2);
            Arrays.fill(Client.Companion.getIS_COMPONENT_ON_TOP(), false);
            ComponentExtraFixtures.build(w, out, interfaces, loaded);
            params(out);
            var e = new CS2Executor();
            e.hookedInterface1.reset();
            e.hookedInterface2.reset();
            var pool = ComponentExtraFixtures.hashPool(w);
            var valid = new ArrayList<Integer>();
            var placements = new ArrayList<int[]>();
            for (var entry : w.components.entrySet()) {
                int index = 0;
                for (var row : entry.getValue()) {
                    if (row != null) {
                        valid.add(Integer.parseInt(row[3]));
                        placements.add(new int[] {entry.getKey(), index});
                        int settings = SETTINGS_POOL[random.nextInt(SETTINGS_POOL.length)];
                        if (random.nextInt(3) == 0) {
                            interfaces[entry.getKey()].components[index].targetParams = new IFEvents(settings, -1);
                            out.append("S\t").append(entry.getKey()).append('\t').append(index).append('\t').append(settings).append('\n');
                        }
                    }
                    index++;
                }
            }
            dump(interfaces, w.capacity, previous);
            for (int step = 0; step < STEPS; step++) {
                if (step % 25 == 0) {
                    for (int secondary = 0; secondary < 2; secondary++) {
                        var hook = secondary == 0 ? e.hookedInterface1 : e.hookedInterface2;
                        if (random.nextInt(12) == 0) {
                            hook.reset();
                            out.append("H\t").append(secondary).append("\t-1\t-1\n");
                        } else {
                            var at = placements.get(random.nextInt(placements.size()));
                            hook.inter = interfaces[at[0]];
                            hook.comp = interfaces[at[0]].components[at[1]];
                            out.append("H\t").append(secondary).append('\t').append(at[0]).append('\t').append(at[1]).append('\n');
                        }
                    }
                }
                var name = names[random.nextInt(names.length)];
                var op = ClientScriptOpCode.valueOf(name);
                int operand = random.nextInt(2);
                var ints = generate(name, random);
                if (name.startsWith("IF_")) {
                    ints = Arrays.copyOf(ints, ints.length + 1);
                    ints[ints.length - 1] = random.nextInt(4) != 0 ? valid.get(random.nextInt(valid.size())) : pool.get(random.nextInt(pool.size()));
                }
                String[] texts = new String[0];
                if (wantsText(name)) texts = new String[] {TEXTS[random.nextInt(TEXTS.length)]};
                e.intStackPtr = ints.length;
                Arrays.fill(e.intStack, 0x5a5a5a5a);
                System.arraycopy(ints, 0, e.intStack, 0, ints.length);
                e.stringStackPtr = texts.length;
                Arrays.fill(e.stringStack, "stale");
                for (int i = 0; i < texts.length; i++) e.stringStack[i] = texts[i];
                e.hasIntOperand = operand == 1;
                String result = "ok";
                try {
                    dispatch(op, e);
                } catch (Throwable error) {
                    result = "error:" + error.getClass().getSimpleName();
                }
                var queued = new ArrayList<PulseEvent>();
                for (CacheableNode node = PulseEventHandler.LOW_PRIORITY.getFirst(); node != null; node = PulseEventHandler.LOW_PRIORITY.getFirst()) {
                    queued.add((PulseEvent) node);
                }
                var eventText = new StringBuilder();
                for (var event : queued) {
                    if (eventText.length() > 0) eventText.append(',');
                    eventText.append(ComponentExtraFixtures.eventKind(event.getType())).append(':').append((int) event.getValue());
                }
                ComponentExtraFixtures.unlinkAll(queued);
                out.append("T\t").append(w.name).append('\t').append(name).append('\t').append(operand).append('\t')
                    .append(ComponentExtraFixtures.stack(ints, ints.length)).append('\t').append(ComponentExtraFixtures.strings(texts, texts.length)).append('\n');
                out.append("R\t").append(result).append('\t').append(ComponentExtraFixtures.stack(e.intStack, e.intStackPtr)).append('\t')
                    .append(ComponentExtraFixtures.strings(Arrays.copyOf(e.stringStack, e.stringStack.length, String[].class), e.stringStackPtr)).append('\t').append(eventText.length() == 0 ? "-" : eventText).append('\t');
                out.append(ComponentExtraFixtures.hookText(e.hookedInterface1, interfaces, w.capacity)).append('\t').append(ComponentExtraFixtures.hookText(e.hookedInterface2, interfaces, w.capacity)).append('\n');
                dump(interfaces, w.capacity, current);
                var keys = new TreeSet<String>(current.keySet());
                keys.addAll(previous.keySet());
                for (var key : keys) {
                    var now = current.get(key);
                    if (!Objects.equals(now, previous.get(key))) out.append("D\t").append(key).append('\t').append(now == null ? "-" : now).append('\n');
                }
                previous.clear();
                previous.putAll(current);
                var stat = STATS.computeIfAbsent(name, k -> new int[2]);
                stat[result.equals("ok") ? 0 : 1]++;
                total++;
            }
        }
        out.append("Z\t").append(total).append('\n');
        Files.writeString(output.resolve("components-options.native.tsv"), out.toString(), StandardCharsets.UTF_8);
        var stats = new StringBuilder();
        for (var entry : STATS.entrySet()) stats.append(entry.getKey()).append('\t').append(entry.getValue()[0]).append('\t').append(entry.getValue()[1]).append('\n');
        Files.writeString(output.resolve("components-options.stats.tsv"), stats.toString(), StandardCharsets.UTF_8);
        System.exit(0);
    }
}
