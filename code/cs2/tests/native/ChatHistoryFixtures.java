import com.jagex.game.runetek5.client.ChatMessageType;
import com.jagex.game.runetek5.client.Client;
import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.*;
import com.jagex.game.runetek5.interaction.chat.ChatBox;
import com.jagex.game.runetek5.interaction.chat.ChatFilter;
import com.jagex.game.runetek5.interaction.chat.ChatLine;
import com.jagex.game.runetek5.network.BufferedConnectionContext;
import com.jagex.game.runetek5.network.packet.TcpPacket;
import com.jagex.game.runetek5.tools.Language;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class ChatHistoryFixtures {
    static final long SEED = 0x50c1a15L;
    static final String[] NAMES = {"Zezima", "zezima", "Mod Mark", "Bob", "", "x", "A", "Straße", "a b", "9", "_", "café"};
    static final String[] COMMENTS = {"", "short", "hello world", "x".repeat(79), "y".repeat(80), "z".repeat(81), "w".repeat(150), "a b c d e f g h i j k l m n o p q r s t u v w x y z a b c d e f g h i j k l m n o p q r s t u v w x y z"};
    static final int[] INDEX = {-1, 0, 1, 2, 3, 7, 49, 50, 98, 99, 100, 101, Integer.MIN_VALUE, Integer.MAX_VALUE};
    static final int[] WIDE = {0, 1, -1, 2, 3, 127, -128, 128, 255, 256, 1000, Integer.MIN_VALUE, Integer.MAX_VALUE, 0x12345678, -559038737};
    static final int[] COUNTS = {0, 1, 5, 17, 99, 100, -1, 250, Integer.MAX_VALUE};
    static final String[] GETTERS = {
        "CHAT_GETHISTORYTYPE", "CHAT_GETHISTORYPHRASE", "CHAT_GETPREVUID", "CHATLINE_GETUID", "CHATLINE_GETEFFECTFLAGS",
        "CHATLINE_GETSIMPLENAME", "CHAT_GETHISTORYMESSAGE", "CHAT_GETHISTORYCLAN", "CHAT_GETHISTORYNAME", "UNKNOWN_COMMAND_5019"
    };
    static final String[] NO_ARG = {"CHAT_GETFILTER_PUBLIC", "CHAT_GETFILTER_PRIVATE", "CHAT_GETFILTER_TRADE", "CHAT_GETHISTORYLENGTH"};
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

    static String name(Random random) {
        return random.nextInt(4) == 0 ? null : NAMES[random.nextInt(NAMES.length)];
    }

    static int pick(Random random, int[] values) {
        return values[random.nextInt(values.length)];
    }

    static String filterId() throws Exception {
        var filter = (ChatFilter) staticGet(ChatBox.class, "PRIVATE_FILTER");
        return filter == null ? "-1" : Integer.toString(filter.id);
    }

    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        var theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        unsafe = (sun.misc.Unsafe) theUnsafe.get(null);
        staticSet(Client.class, "CURRENT_LANGUAGE", Language.ENGLISH);
        var context = (BufferedConnectionContext) staticGet(Client.class, "GAME_CONNECTION_CONTEXT");
        var out = new StringBuilder();
        var random = new Random(SEED);
        var types = ChatMessageType.values();
        int stateCount = 16;
        var applies = new ArrayList<Runnable>();
        for (int s = 0; s < stateCount; s++) {
            var lines = new ChatLine[100];
            for (int i = 0; i < 100; i++) {
                if (random.nextInt(3) == 0) continue;
                var line = (ChatLine) unsafe.allocateInstance(ChatLine.class);
                var type = random.nextInt(12) == 0 ? null : types[random.nextInt(types.length)];
                set(line, "type", type);
                set(line, "identifier", pick(random, WIDE));
                set(line, "time", pick(random, WIDE));
                set(line, "effectFlags", pick(random, WIDE));
                set(line, "crownedName", name(random));
                set(line, "name", name(random));
                set(line, "simpleName", name(random));
                set(line, "clan", name(random));
                set(line, "quickchatMessageId", pick(random, WIDE));
                set(line, "message", name(random));
                lines[i] = line;
                out.append("L\t").append(s).append('\t').append(i).append('\t').append(type == null ? "~" : Integer.toString(type.getValue())).append('\t')
                    .append(line.identifier).append('\t').append(line.time).append('\t').append(line.effectFlags).append('\t').append(nullable(line.crownedName)).append('\t')
                    .append(nullable(line.name)).append('\t').append(nullable(line.simpleName)).append('\t').append(nullable(line.clan)).append('\t')
                    .append(line.quickchatMessageId).append('\t').append(nullable(line.message)).append('\n');
            }
            int count = pick(random, COUNTS);
            int publicFilter = pick(random, WIDE);
            var privateFilter = s % 4 == 3 ? null : ChatFilter.values()[random.nextInt(3)];
            int tradeFilter = pick(random, WIDE);
            out.append("H\t").append(s).append('\t').append(count).append('\t').append(publicFilter).append('\t').append(privateFilter == null ? -1 : privateFilter.id).append('\t').append(tradeFilter).append('\n');
            applies.add(() -> {
                try {
                    var target = (ChatLine[]) staticGet(ChatBox.class, "CHAT_LINES");
                    System.arraycopy(lines, 0, target, 0, 100);
                    staticSet(ChatBox.class, "NUM_CHAT_LINES", count);
                    staticSet(ChatBox.class, "PUBLIC_FILTER", publicFilter);
                    staticSet(ChatBox.class, "PRIVATE_FILTER", privateFilter);
                    staticSet(ChatBox.class, "TRADE_FILTER", tradeFilter);
                } catch (Exception error) {
                    throw new RuntimeException(error);
                }
            });
        }
        var cases = new ArrayList<Object[]>();
        for (int s = 0; s < stateCount; s++) {
            for (var op : NO_ARG) cases.add(new Object[] {op, s, new int[0], new String[0]});
            for (var op : GETTERS) {
                for (int index : INDEX) cases.add(new Object[] {op, s, new int[] {index}, new String[0]});
                for (int i = 0; i < 24; i++) cases.add(new Object[] {op, s, new int[] {random.nextInt(100)}, new String[0]});
            }
            for (int i = 0; i < 30; i++) {
                cases.add(new Object[] {"CHAT_SETFILTER", s, new int[] {pick(random, WIDE), random.nextInt(8) - 2, pick(random, WIDE)}, new String[0]});
            }
            for (int value : WIDE) cases.add(new Object[] {"CHAT_SETMODE", s, new int[] {value}, new String[0]});
            for (int i = 0; i < 30; i++) {
                var comment = random.nextInt(8) == 0 ? null : COMMENTS[random.nextInt(COMMENTS.length)];
                var reported = random.nextInt(8) == 0 ? null : NAMES[random.nextInt(NAMES.length)];
                cases.add(new Object[] {"CHAT_SENDABUSEREPORT", s, new int[] {pick(random, WIDE), pick(random, WIDE)}, new String[] {reported, comment}});
            }
        }
        Collections.shuffle(cases, random);
        var stats = new TreeMap<String, int[]>();
        int total = 0;
        staticSet(Client.class, "gameState", com.jagex.game.runetek5.client.GameState.UNK_4);
        for (var item : cases) {
            String op = (String) item[0];
            int state = (Integer) item[1];
            int[] intArgs = (int[]) item[2];
            String[] stringArgs = (String[]) item[3];
            applies.get(state).run();
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
                ConfigTypeChatOps.INSTANCE.dispatch(ClientScriptOpCode.valueOf(op), e);
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
            out.append("T\t").append(op).append('\t').append(state).append('\t').append(intArgs.length == 0 ? "-" : ints(intArgs, intArgs.length)).append('\t')
                .append(stringArgs.length == 0 ? "-" : strs.toString()).append('\t').append(result).append('\t').append(ints(e.intStack, e.intStackPtr)).append('\t')
                .append(strings(e.stringStack, e.stringStackPtr)).append('\t').append(packets.isEmpty() ? "-" : String.join("|", packets)).append('\t')
                .append(staticGet(ChatBox.class, "PUBLIC_FILTER")).append(',').append(filterId()).append(',').append(staticGet(ChatBox.class, "TRADE_FILTER")).append('\n');
            stats.computeIfAbsent(op, k -> new int[2])[result.equals("ok") ? 0 : 1]++;
            total++;
        }
        out.append("Z\t").append(total).append('\n');
        Files.writeString(output.resolve("chat-history.native.tsv"), out.toString(), StandardCharsets.UTF_8);
        var summary = new StringBuilder();
        for (var entry : stats.entrySet()) summary.append(entry.getKey()).append('\t').append(entry.getValue()[0]).append('\t').append(entry.getValue()[1]).append('\n');
        Files.writeString(output.resolve("chat-history.stats.tsv"), summary.toString(), StandardCharsets.UTF_8);
        System.exit(0);
    }
}
