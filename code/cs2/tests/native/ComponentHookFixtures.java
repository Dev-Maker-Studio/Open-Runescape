import com.jagex.game.runetek5.client.Client;
import com.jagex.game.runetek5.client.GameShell;
import com.jagex.game.runetek5.config.rsinterface.Component;
import com.jagex.game.runetek5.config.rsinterface.IFEvents;
import com.jagex.game.runetek5.config.rsinterface.Interface;
import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class ComponentHookFixtures {
    static final String[] NAMES = {
        "CC_SETONCLICK",
        "CC_SETONHOLD",
        "CC_SETONRELEASE",
        "CC_SETONMOUSEOVER",
        "CC_SETONMOUSELEAVE",
        "CC_SETONDRAG",
        "CC_SETONTARGETLEAVE",
        "CC_SETONVARTRANSMIT",
        "CC_SETONTIMER",
        "CC_SETONOP",
        "CC_SETONDRAGCOMPLETE",
        "CC_SETONCLICKREPEAT",
        "CC_SETONMOUSEREPEAT",
        "CC_SETONINVTRANSMIT",
        "CC_SETONSTATTRANSMIT",
        "CC_SETONTARGETENTER",
        "CC_SETONSCROLLWHEEL",
        "CC_SETONCHATTRANSMIT",
        "CC_SETONKEY",
        "CC_SETONFRIENDTRANSMIT",
        "CC_SETONCLANCHANNELTRANSMIT",
        "CC_SETONMISCTRANSMIT",
        "CC_SETONDIALOGABORT",
        "CC_SETONSUBCHANGE",
        "CC_SETONSTOCKTRANSMIT",
        "CC_SETONCAMFINISHED",
        "CC_SETONRESIZE",
        "CC_SETONVARCTRANSMIT",
        "CC_SETONVARCSTRTRANSMIT",
        "CC_SETONOPT",
        "CC_SETONCLANSETTINGSTRANSMIT",
        "CC_SETLINKACTIVECLANCHANNEL",
        "CC_SETONVARCLANTRANSMIT",
        "CC_CLEARSCRIPTHOOKS",
        "IF_SETONCLICK",
        "IF_SETONHOLD",
        "IF_SETONRELEASE",
        "IF_SETONMOUSEOVER",
        "IF_SETONMOUSELEAVE",
        "IF_SETONDRAG",
        "IF_SETONTARGETLEAVE",
        "IF_SETONVARTRANSMIT",
        "IF_SETONTIMER",
        "IF_SETONOP",
        "IF_SETONDRAGCOMPLETE",
        "IF_SETONCLICKREPEAT",
        "IF_SETONMOUSEREPEAT",
        "IF_SETONINVTRANSMIT",
        "IF_SETONSTATTRANSMIT",
        "IF_SETONTARGETENTER",
        "IF_SETONSCROLLWHEEL",
        "IF_SETONCHATTRANSMIT",
        "IF_SETONKEY",
        "IF_SETONFRIENDTRANSMIT",
        "IF_SETONCLANTRANSMIT",
        "IF_SETONMISCTRANSMIT",
        "IF_SETONDIALOGABORT",
        "IF_SETONSUBCHANGE",
        "IF_SETONSTOCKTRANSMIT",
        "IF_SETONCAMFINISHED",
        "IF_SETONRESIZE",
        "IF_SETONVARCTRANSMIT",
        "IF_SETONVARCSTRTRANSMIT",
        "IF_SETONOPT",
        "IF_SETONCLANSETTINGSTRANSMIT",
        "IF_SETONCLANCHANNELTRANSMIT",
        "IF_SETONVARCLANTRANSMIT",
        "IF_CLEARSCRIPTHOOKS"
    };
    static final long SEED = 0x727c5223L;
    static final int STEPS = 9000;
    static final long STALE_LONG = 0x5a5a5a5a5a5a5a5aL;
    static final String[] TEXTS = {null, "", "a", "hello", "\u00e9\u20ac", "x".repeat(40)};
    static final long[] LONGS = {0L, 1L, -1L, Long.MAX_VALUE, Long.MIN_VALUE, 123456789012L, 0x5a5a5a5aL};
    static final int[] INTS = {0, 1, -1, 7, 255, 1000, -5, 65535, 1 << 20, Integer.MIN_VALUE};
    static final int[] SCRIPTS = {1, 2, 3, 100, 0, 12345, -1, -1, 1 << 20, -7};
    static final Map<String, int[]> STATS = new TreeMap<>();

    static boolean triggers(String name) {
        String bare = name.substring(3);
        return bare.equals("SETONVARTRANSMIT") || bare.equals("SETONINVTRANSMIT") || bare.equals("SETONSTATTRANSMIT") || bare.equals("SETONVARCTRANSMIT") || bare.equals("SETONVARCSTRTRANSMIT");
    }

    static boolean clear(String name) {
        return name.endsWith("CLEARSCRIPTHOOKS");
    }

    static String types(Random random) {
        int roll = random.nextInt(20);
        if (roll == 0) return null;
        if (roll == 1) return "";
        var sb = new StringBuilder();
        int length = random.nextInt(5);
        char[] alphabet = {'i', 'i', 's', '\u00a7', 'I', 'Y', 'o'};
        for (int i = 0; i < length; i++) sb.append(alphabet[random.nextInt(alphabet.length)]);
        if (random.nextInt(5) < 2) sb.append('Y');
        return sb.toString();
    }

    static final class Args {
        final List<Integer> ints = new ArrayList<>();
        final List<String> strings = new ArrayList<>();
        final List<Long> longs = new ArrayList<>();
    }

    static Args generate(String name, Random random) {
        var args = new Args();
        if (clear(name)) return args;
        String types = types(random);
        int mode = random.nextInt(10);
        if (mode == 0) {
            int n = random.nextInt(4);
            for (int i = 0; i < n; i++) args.ints.add(INTS[random.nextInt(INTS.length)]);
            int s = random.nextInt(3);
            for (int i = 0; i < s; i++) args.strings.add(TEXTS[random.nextInt(TEXTS.length)]);
            int l = random.nextInt(3);
            for (int i = 0; i < l; i++) args.longs.add(LONGS[random.nextInt(LONGS.length)]);
            args.strings.add(types);
            return args;
        }
        int size = 0;
        boolean wantsTriggers = types != null && types.endsWith("Y");
        if (wantsTriggers && triggers(name)) {
            int[] sizes = {0, 1, 2, 3, 3, -1, -3, 5, 40, 2000};
            size = sizes[random.nextInt(sizes.length)];
        }
        String effective = types;
        if (types != null && size > 0) effective = types.substring(0, types.length() - 1);
        if (types != null) {
            args.ints.add(SCRIPTS[random.nextInt(SCRIPTS.length)]);
            for (int i = 0; i < effective.length(); i++) {
                char kind = effective.charAt(i);
                if (kind == 's') args.strings.add(TEXTS[random.nextInt(TEXTS.length)]);
                else if (kind == '\u00a7') args.longs.add(LONGS[random.nextInt(LONGS.length)]);
                else args.ints.add(INTS[random.nextInt(INTS.length)]);
            }
        }
        if (wantsTriggers && triggers(name)) {
            for (int i = 0; i < Math.min(size, 40); i++) args.ints.add(INTS[random.nextInt(INTS.length)]);
            if (size > 40 && size != 2000) throw new IllegalStateException();
            args.ints.add(size);
        } else if (types != null && random.nextInt(4) == 0) {
            args.ints.add(INTS[random.nextInt(INTS.length)]);
        }
        if (mode == 1 && !args.ints.isEmpty()) args.ints.remove(random.nextInt(args.ints.size()));
        if (mode == 2 && !args.strings.isEmpty()) args.strings.remove(random.nextInt(args.strings.size()));
        if (mode == 3 && !args.longs.isEmpty()) args.longs.remove(random.nextInt(args.longs.size()));
        args.strings.add(types);
        return args;
    }

    static String value(Object o) {
        if (o instanceof Integer i) return "i" + i;
        if (o instanceof Long l) return "l" + l;
        if (o == null) return "snull";
        return "s" + NativeFixtures.hex((String) o);
    }

    static String script(Object[] a) {
        if (a == null) return "null";
        var sb = new StringBuilder();
        for (int i = 0; i < a.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(value(a[i]));
        }
        return sb.length() == 0 ? "empty" : sb.toString();
    }

    static String script(int[] a) {
        if (a == null) return "null";
        var sb = new StringBuilder();
        for (int i = 0; i < a.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(a[i]);
        }
        return sb.length() == 0 ? "empty" : sb.toString();
    }

    static String describe(Component c) {
        var out = new ArrayList<String>();
        out.add(c.hasScript ? "1" : "0");
        out.add(script(c.onClick));
        out.add(script(c.onClickRepeat));
        out.add(script(c.onRelease));
        out.add(script(c.onHold));
        out.add(script(c.onMouseOver));
        out.add(script(c.popupScript));
        out.add(script(c.onMouseLeaveScript));
        out.add(script(c.onDrag));
        out.add(script(c.onDragComplete));
        out.add(script(c.onTargetEnter));
        out.add(script(c.hookParams));
        out.add(script(c.onVarpTransmit));
        out.add(script(c.mouseLeaveScript));
        out.add(script(c.onStatTransmit));
        out.add(script(c.onKey));
        out.add(script(c.onScrollWheel));
        out.add(script(c.onTimer));
        out.add(script(c.params));
        out.add(script(c.onTargetLeave));
        out.add(script(c.onMouseMove));
        out.add(script(c.onChatTransmit));
        out.add(script(c.onKeyType));
        out.add(script(c.onFriendTransmit));
        out.add(script(c.onFriendsChatTransmit));
        out.add(script(c.onClanSettingsTransmit));
        out.add(script(c.onMiscTransmit));
        out.add(script(c.onStockTransmit));
        out.add(script(c.onSubClose));
        out.add(script(c.onSubChange));
        out.add(script(c.onCamFinished));
        out.add(script(c.onResize));
        out.add(script(c.onClanVarTransmit));
        out.add(script(c.onClanChannelTransmit));
        out.add(script(c.varps));
        out.add(script(c.mouseLeaveArrayParams));
        out.add(script(c.statTransmitFilter));
        out.add(script(c.keyPressArray));
        out.add(script(c.mouseWheelArray));
        return String.join("\t", out);
    }

    static String longs(long[] values, int pointer) {
        if (pointer <= 0) return "-";
        var sb = new StringBuilder();
        for (int i = 0; i < pointer; i++) {
            if (i > 0) sb.append(',');
            sb.append(values[i]);
        }
        return sb.toString();
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
        if (op.name().startsWith("IF_")) InterfaceSetterOps.dispatch(op, e);
        else ComponentSetterOps.dispatch(op, e);
    }

    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        var out = new StringBuilder();
        var random = new Random(SEED);
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
                var name = NAMES[random.nextInt(NAMES.length)];
                var op = ClientScriptOpCode.valueOf(name);
                int operand = random.nextInt(2);
                var generated = generate(name, random);
                if (name.startsWith("IF_")) {
                    generated.ints.add(random.nextInt(4) != 0 ? valid.get(random.nextInt(valid.size())) : pool.get(random.nextInt(pool.size())));
                }
                int[] ints = generated.ints.stream().mapToInt(Integer::intValue).toArray();
                String[] texts = generated.strings.toArray(new String[0]);
                long[] longValues = generated.longs.stream().mapToLong(Long::longValue).toArray();
                if (clear(name)) texts = new String[0];
                e.intStackPtr = ints.length;
                Arrays.fill(e.intStack, 0x5a5a5a5a);
                System.arraycopy(ints, 0, e.intStack, 0, ints.length);
                e.stringStackPtr = texts.length;
                Arrays.fill(e.stringStack, "stale");
                for (int i = 0; i < texts.length; i++) e.stringStack[i] = texts[i];
                e.longStackPtr = longValues.length;
                Arrays.fill(e.longStack, STALE_LONG);
                System.arraycopy(longValues, 0, e.longStack, 0, longValues.length);
                e.hasIntOperand = operand == 1;
                String result = "ok";
                try {
                    dispatch(op, e);
                } catch (Throwable error) {
                    result = "error:" + error.getClass().getSimpleName();
                }
                out.append("T\t").append(w.name).append('\t').append(name).append('\t').append(operand).append('\t')
                    .append(ComponentExtraFixtures.stack(ints, ints.length)).append('\t').append(ComponentExtraFixtures.strings(texts, texts.length)).append('\t').append(longs(longValues, longValues.length)).append('\n');
                out.append("R\t").append(result).append('\t').append(ComponentExtraFixtures.stack(e.intStack, e.intStackPtr)).append('\t')
                    .append(ComponentExtraFixtures.strings(Arrays.copyOf(e.stringStack, e.stringStack.length, String[].class), e.stringStackPtr)).append('\t')
                    .append(longs(e.longStack, e.longStackPtr)).append('\t').append("-").append('\t');
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
        Files.writeString(output.resolve("components-hooks.native.tsv"), out.toString(), StandardCharsets.UTF_8);
        var stats = new StringBuilder();
        for (var entry : STATS.entrySet()) stats.append(entry.getKey()).append('\t').append(entry.getValue()[0]).append('\t').append(entry.getValue()[1]).append('\n');
        Files.writeString(output.resolve("components-hooks.stats.tsv"), stats.toString(), StandardCharsets.UTF_8);
        System.exit(0);
    }
}
