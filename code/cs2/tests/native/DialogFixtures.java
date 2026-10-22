import com.jagex.game.runetek5.client.Client;
import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.*;
import com.jagex.game.runetek5.network.BufferedConnectionContext;
import com.jagex.game.runetek5.network.packet.TcpPacket;
import com.jagex.game.runetek5.tools.Language;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class DialogFixtures {
    static final long SEED = 0x50c1a15L;
    static final String[] TEXTS = {"", "0", "1", "123", "-5", "+7", "2147483647", "2147483648", "-2147483648", "-2147483649", "12a", "-", "+", "1 2", " 5", "00012", "0x10", "99999999999", "--1", "+-1", "Zezima", "Mod Mark", "x", "caf\u00e9", "Stra\u00dfe", "k".repeat(100), "m".repeat(250), "n".repeat(300), "5-", "1e5", "9223372036854775807", "-0", "007", "\u00e9"};
    static final int[] WIDE = {0, 1, -1, 2, 3, 127, -128, 128, 255, 256, 1000, 65535, 65536, -65536, Integer.MIN_VALUE, Integer.MAX_VALUE, 0x12345678, -559038737};
    static final String[] TEXT_OPS = {"RESUME_COUNTDIALOG", "RESUME_NAMEDIALOG", "RESUME_STRINGDIALOG", "RESUME_CLANFORUMQFCDIALOG"};
    static final String[] INT_OPS = {"RESUME_OBJDIALOG", "RESUME_HSLDIALOG"};
    static sun.misc.Unsafe unsafe;

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

    static String nullable(String value) {
        return value == null ? "~" : value;
    }

    static String ints(int[] values, int pointer) {
        if (pointer <= 0) return "-";
        var sb = new StringBuilder();
        for (int i = 0; i < pointer; i++) {
            if (i > 0) sb.append(',');
            sb.append(values[i]);
        }
        return sb.toString();
    }

    static String strings(Object[] values, int pointer) {
        if (pointer <= 0) return "-";
        var sb = new StringBuilder();
        for (int i = 0; i < pointer; i++) {
            if (i > 0) sb.append('|');
            sb.append(values[i] == null ? "~" : "\"" + values[i] + "\"");
        }
        return sb.toString();
    }

    static String hex(TcpPacket packet) {
        var sb = new StringBuilder();
        for (int i = 0; i < packet.capacity; i++) sb.append(String.format("%02x", packet.buffer.buffer[i] & 0xff));
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        staticSet(Client.class, "CURRENT_LANGUAGE", Language.ENGLISH);
        var context = (BufferedConnectionContext) staticGet(Client.class, "GAME_CONNECTION_CONTEXT");
        var out = new StringBuilder();
        var cases = new ArrayList<Object[]>();
        for (var op : TEXT_OPS) {
            for (var text : TEXTS) cases.add(new Object[] {op, new int[0], new String[] {text}});
            cases.add(new Object[] {op, new int[0], new String[] {null}});
            cases.add(new Object[] {op, new int[0], new String[0]});
        }
        for (var op : INT_OPS) {
            for (int value : WIDE) cases.add(new Object[] {op, new int[] {value}, new String[0]});
            cases.add(new Object[] {op, new int[0], new String[0]});
        }
        var stats = new TreeMap<String, int[]>();
        int total = 0;
        staticSet(Client.class, "gameState", com.jagex.game.runetek5.client.GameState.UNK_4);
        for (var item : cases) {
            String op = (String) item[0];
            int[] intArgs = (int[]) item[1];
            String[] stringArgs = (String[]) item[2];
            context.clearAllQueuedPackets();
            var e = new CS2Executor();
            e.intOpValues = new int[] {0};
            e.instrPtr = 0;
            Arrays.fill(e.intStack, 0x5a5a5a5a);
            e.intStackPtr = 0;
            e.stringStackPtr = 0;
            String result = "ok";
            try {
                for (int value : intArgs) e.intStack[e.intStackPtr++] = value;
                for (var text : stringArgs) e.stringStack[e.stringStackPtr++] = text;
                DialogOps.INSTANCE.dispatch(ClientScriptOpCode.valueOf(op), e);
            } catch (Throwable error) {
                result = "error:" + error.getClass().getSimpleName();
            }
            var packets = new ArrayList<String>();
            var node = context.queuedPackets.getFirst();
            while (node != null) {
                packets.add(hex((TcpPacket) node));
                node = context.queuedPackets.getNext();
            }
            var strs = new StringBuilder();
            for (int i = 0; i < stringArgs.length; i++) {
                if (i > 0) strs.append('|');
                strs.append(nullable(stringArgs[i]));
            }
            out.append("T\t").append(op).append('\t').append(intArgs.length == 0 ? "-" : ints(intArgs, intArgs.length)).append('\t')
                .append(stringArgs.length == 0 ? "<none>" : strs.toString()).append('\t').append(result).append('\t').append(ints(e.intStack, e.intStackPtr)).append('\t')
                .append(strings(e.stringStack, e.stringStackPtr)).append('\t').append(packets.isEmpty() ? "-" : String.join("|", packets)).append('\n');
            stats.computeIfAbsent(op, k -> new int[2])[result.equals("ok") ? 0 : 1]++;
            total++;
        }
        out.append("Z\t").append(total).append('\n');
        Files.writeString(output.resolve("dialog.native.tsv"), out.toString(), StandardCharsets.UTF_8);
        var summary = new StringBuilder();
        for (var entry : stats.entrySet()) summary.append(entry.getKey()).append('\t').append(entry.getValue()[0]).append('\t').append(entry.getValue()[1]).append('\n');
        Files.writeString(output.resolve("dialog.stats.tsv"), summary.toString(), StandardCharsets.UTF_8);
        System.exit(0);
    }
}
