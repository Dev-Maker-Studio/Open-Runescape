import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.bas.BasType;
import com.jagex.game.runetek5.config.bas.BasTypeList;
import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.*;
import com.jagex.game.runetek5.rendering.MessageBox;
import com.jagex.game.runetek5.systeminfo.SystemInfo;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class SmallOpFixtures {
    static sun.misc.Unsafe unsafe;
    static final int[] ANCHORS = {0, 1, 2, 3, -1, 5, 100, 0, 1, 2};

    static Field field(Class<?> type, String name) throws Exception {
        for (var c = type; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(name);
    }

    static void set(Object target, String name, Object value) throws Exception {
        var f = field(target.getClass(), name);
        f.setAccessible(true);
        f.set(target, value);
    }

    static Object staticGet(Class<?> type, String name) throws Exception {
        var f = field(type, name);
        f.setAccessible(true);
        return f.get(null);
    }

    static void staticSet(Class<?> type, String name, Object value) throws Exception {
        var f = field(type, name);
        f.setAccessible(true);
        f.set(null, value);
    }

    static String stack(CS2Executor e) {
        var sb = new StringBuilder();
        for (int i = 0; i < Math.max(0, e.intStackPtr); i++) sb.append(i > 0 ? "," : "").append(e.intStack[i]);
        return sb.length() == 0 ? "-" : sb.toString();
    }

    static CS2Executor executor(int[] values) {
        var e = new CS2Executor();
        Arrays.fill(e.intStack, 0x5a5a5a5a);
        e.intStackPtr = 0;
        e.stringStackPtr = 0;
        e.intOpValues = new int[] {0};
        e.instrPtr = 0;
        for (int v : values) e.intStack[e.intStackPtr++] = v;
        return e;
    }

    static String list(int[] values) {
        if (values == null) return "~";
        if (values.length == 0) return "-";
        var sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) sb.append(i > 0 ? "," : "").append(values[i]);
        return sb.toString();
    }

    static String boxState() throws Exception {
        var x = staticGet(MessageBox.class, "anchorX");
        var y = staticGet(MessageBox.class, "anchorY");
        var sb = new StringBuilder();
        sb.append(x == null ? "-" : ((Enum<?>) x).ordinal()).append(',').append(y == null ? "-" : ((Enum<?>) y).ordinal());
        for (var name : new String[] {"paddingLeft", "paddingTop", "minimalTextWidth", "minimalTextHeight", "outlineCornerSpriteId", "outlineStraightSideSpriteId", "filledBackgroundSpriteId", "textColor", "boxId"}) {
            sb.append(',').append(staticGet(MessageBox.class, name));
        }
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        var theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        unsafe = (sun.misc.Unsafe) theUnsafe.get(null);
        var random = new Random(0x5a11095L);
        var out = new StringBuilder();
        int total = 0;
        var info = (SystemInfo) unsafe.allocateInstance(SystemInfo.class);
        staticSet(SystemInfo.class, "SYSTEM_INFO", info);
        for (int build : new int[] {-1, 0, 5, 6, 7, 8, 17, 100}) {
            for (int update : new int[] {-1, 0, 9, 10, 11, 100}) {
                set(info, "javaBuild", build);
                set(info, "javaUpdate", update);
                var e = executor(new int[0]);
                String result = "ok";
                try {
                    QuickChatDisplayOps.dispatch(ClientScriptOpCode.JAVA_VERSION_SUPPORTED, e);
                } catch (Throwable error) {
                    result = "error:" + error.getClass().getSimpleName();
                }
                out.append("J\t").append(build).append('\t').append(update).append('\t').append(result).append('\t').append(stack(e)).append('\n');
                total++;
            }
        }
        for (int n = 0; n < 300; n++) {
            var given = new int[11];
            for (int i = 0; i < 11; i++) given[i] = random.nextInt(4) == 0 ? random.nextInt() : random.nextInt(1000);
            given[0] = ANCHORS[random.nextInt(ANCHORS.length)];
            given[1] = ANCHORS[random.nextInt(ANCHORS.length)];
            staticSet(MessageBox.class, "anchorX", null);
            staticSet(MessageBox.class, "anchorY", null);
            for (var name : new String[] {"paddingLeft", "paddingTop", "minimalTextWidth", "minimalTextHeight", "outlineCornerSpriteId", "outlineStraightSideSpriteId", "filledBackgroundSpriteId", "textColor", "boxId"}) staticSet(MessageBox.class, name, -7);
            var e = executor(given);
            String result = "ok";
            try {
                DialogOps.dispatch(ClientScriptOpCode.SETUP_MESSAGEBOX, e);
            } catch (Throwable error) {
                result = "error:" + error.getClass().getSimpleName();
            }
            var args11 = new StringBuilder();
            for (int i = 0; i < 11; i++) args11.append(i > 0 ? "," : "").append(given[i]);
            out.append("M\t").append(args11).append('\t').append(result).append('\t').append(stack(e)).append('\t').append(boxState()).append('\n');
            total++;
        }
        var bases = (BasTypeList) unsafe.allocateInstance(BasTypeList.class);
        set(bases, "basCache", new LRUCache(64));
        TypeLists.BAS = bases;
        var described = new ArrayList<String>();
        for (int id = 0; id < 40; id++) {
            var type = new BasType();
            int mode = random.nextInt(6);
            int stand = random.nextInt(3) == 0 ? -1 : random.nextInt(5000);
            type.standSequenceId = stand;
            int count = random.nextInt(5);
            var sequences = new int[count];
            var chances = new int[count];
            for (int i = 0; i < count; i++) {
                sequences[i] = random.nextInt(9000);
                chances[i] = random.nextInt(4) == 0 ? chances.length > 0 && i > 0 ? chances[i - 1] : 5 : random.nextInt(100);
            }
            switch (mode) {
                case 0 -> {
                }
                case 1 -> {
                    type.randomStandSequences = new int[0];
                    type.randomStandSequenceChances = new int[0];
                }
                case 2 -> {
                    type.randomStandSequences = sequences;
                    type.randomStandSequenceChances = null;
                }
                case 3 -> {
                    type.randomStandSequences = sequences;
                    type.randomStandSequenceChances = Arrays.copyOf(chances, Math.max(0, count - 1 - random.nextInt(2)));
                }
                default -> {
                    type.randomStandSequences = sequences;
                    type.randomStandSequenceChances = chances;
                }
            }
            bases.basCache.put(type, (long) id);
            described.add(stand + ";" + list(type.randomStandSequences) + ";" + list(type.randomStandSequenceChances));
            out.append("D\t").append(id).append('\t').append(described.get(id)).append('\n');
        }
        for (int n = 0; n < 400; n++) {
            int id = random.nextInt(8) == 0 ? 40 + random.nextInt(5) : random.nextInt(40);
            var e = executor(new int[] {id});
            String result = "ok";
            try {
                ConfigTypeChatOps.dispatch(ClientScriptOpCode.BAS_GETANIM_READY, e);
            } catch (Throwable error) {
                result = "error:" + error.getClass().getSimpleName();
            }
            out.append("B\t").append(id).append('\t').append(result).append('\t').append(stack(e)).append('\n');
            total++;
        }
        var e = executor(new int[] {9, 8});
        QuickChatDisplayOps.dispatch(ClientScriptOpCode.NOTIFY_ACCOUNTCREATESTARTED, e);
        out.append("N\t").append(stack(e)).append('\n');
        total++;
        out.append("Z\t").append(total).append('\n');
        Files.createDirectories(Path.of(args[0]));
        Files.writeString(Path.of(args[0]).resolve("small-ops.native.tsv"), out.toString(), StandardCharsets.UTF_8);
        System.exit(0);
    }
}
