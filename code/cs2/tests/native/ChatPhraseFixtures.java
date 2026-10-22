import com.jagex.game.runetek5.client.Client;
import com.jagex.game.runetek5.config.quickchat.QuickChatMessage;
import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.QuickChatDisplayOps;
import com.jagex.game.runetek5.network.BufferedConnectionContext;
import com.jagex.game.runetek5.network.packet.TcpPacket;
import com.jagex.game.runetek5.tools.Language;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;

public final class ChatPhraseFixtures {
    static final long SEED = 0xc4a7b4a5eL;
    static final int[] MESSAGES = {-1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 69, 70, 32768, 32769, 32770, 32771, 32772, 32773, 65537};
    static final int[] INDICES = {-1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 100};
    static final int[] VALUES = {0, 1, -1, 127, 128, 255, 256, 65535, 65536, 0x7fffffff, Integer.MIN_VALUE, 0x12345678, -0x12345678, 0x7e, 100000};
    static final String[] NAMES = {"Zezima", "", "x", "Älice", "Café", "a-b c", "日本語", "€uro", "ÿ", "a\0b"};
    static final ClientScriptOpCode[] CHANNEL = {
        ClientScriptOpCode.ACTIVECHATPHRASE_SENDPUBLIC, ClientScriptOpCode.ACTIVECHATPHRASE_SENDCLAN,
        ClientScriptOpCode.ACTIVECHATPHRASE_SENDCLANCHANNEL_AFFINED, ClientScriptOpCode.ACTIVECHATPHRASE_SENDCLANCHANNEL_LISTENED
    };

    record Case(String name, ClientScriptOpCode opcode, boolean bare, int[] ints, String[] strings) {}

    static Object staticGet(String name) throws Exception {
        Field f = Client.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(null);
    }

    static void staticSet(String name, Object value) throws Exception {
        Field f = Client.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(null, value);
    }

    static String hex(TcpPacket packet) {
        var sb = new StringBuilder();
        for (int i = 0; i < packet.capacity; i++) sb.append(String.format("%02x", packet.buffer.buffer[i] & 0xff));
        return sb.toString();
    }

    static String state(CS2Executor e) {
        QuickChatMessage m = e.currentQuickChatMessage;
        if (m == null) return "~";
        var sb = new StringBuilder().append(m.id).append(':').append(m.message == null ? "~" : "m").append(':');
        if (m.dynamicValues == null) return sb.append('~').toString();
        if (m.dynamicValues.length == 0) return sb.append('-').toString();
        for (int i = 0; i < m.dynamicValues.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(m.dynamicValues[i]);
        }
        return sb.toString();
    }

    static void add(List<Case> cases, ClientScriptOpCode opcode, boolean bare, String[] strings, int... ints) {
        int[] full = new int[ints.length + (bare ? 0 : 1)];
        if (!bare) full[0] = 727;
        System.arraycopy(ints, 0, full, bare ? 0 : 1, ints.length);
        String[] lower = bare ? strings : new String[strings.length + 1];
        if (!bare) {
            lower[0] = "lower-string";
            System.arraycopy(strings, 0, lower, 1, strings.length);
        }
        cases.add(new Case(opcode.name().toLowerCase() + "-" + cases.size(), opcode, bare, full, lower));
    }

    static void sends(List<Case> cases, boolean bare, Random random, String[] names) {
        for (var opcode : CHANNEL) add(cases, opcode, bare, new String[0]);
        for (var name : names) add(cases, ClientScriptOpCode.ACTIVECHATPHRASE_SENDPRIVATE, bare, new String[] {name});
    }

    static List<Case> cases() {
        var random = new Random(SEED);
        var cases = new ArrayList<Case>();
        var base = new String[] {"Zezima", "", "a".repeat(250), "a".repeat(257), "Älice", "日本語", "a\0b"};
        sends(cases, false, random, base);
        add(cases, ClientScriptOpCode.ACTIVECHATPHRASE_SENDPRIVATE, false, new String[] {null});
        add(cases, ClientScriptOpCode.ACTIVECHATPHRASE_SETDYNAMICINT, false, new String[0], 0, 5);
        add(cases, ClientScriptOpCode.ACTIVECHATPHRASE_SETDYNAMICOBJ, false, new String[0], 0, 5);
        add(cases, ClientScriptOpCode.ACTIVECHATPHRASE_SETDYNAMICINT, true, new String[0], 0, 5);
        add(cases, ClientScriptOpCode.ACTIVECHATPHRASE_SETDYNAMICINT, true, new String[0], 5);
        add(cases, ClientScriptOpCode.ACTIVECHATPHRASE_PREPARE, true, new String[0]);
        sends(cases, false, random, base);
        sends(cases, true, random, base);
        add(cases, ClientScriptOpCode.ACTIVECHATPHRASE_SETDYNAMICINT, false, new String[0], 0, 5);
        for (int id : MESSAGES) {
            add(cases, ClientScriptOpCode.ACTIVECHATPHRASE_PREPARE, false, new String[0], id);
            sends(cases, false, random, base);
            for (var opcode : new ClientScriptOpCode[] {ClientScriptOpCode.ACTIVECHATPHRASE_SETDYNAMICINT, ClientScriptOpCode.ACTIVECHATPHRASE_SETDYNAMICOBJ}) {
                for (int index : INDICES) {
                    add(cases, opcode, false, new String[0], index, VALUES[random.nextInt(VALUES.length)]);
                }
            }
            for (int index = 0; index < 12; index++) {
                int value = VALUES[random.nextInt(VALUES.length)];
                add(cases, ClientScriptOpCode.ACTIVECHATPHRASE_SETDYNAMICINT, false, new String[0], index, value);
                if (index % 3 == 2) sends(cases, false, random, base);
            }
            var wide = new ArrayList<String>(Arrays.asList(base));
            if (id == 10 || id == 3 || id == 4) for (int length = 236; length <= 262; length++) wide.add("a".repeat(length));
            sends(cases, false, random, wide.toArray(new String[0]));
            add(cases, ClientScriptOpCode.ACTIVECHATPHRASE_SETDYNAMICINT, true, new String[0], 0);
            add(cases, ClientScriptOpCode.ACTIVECHATPHRASE_SETDYNAMICOBJ, true, new String[0]);
            add(cases, ClientScriptOpCode.ACTIVECHATPHRASE_SENDPRIVATE, true, new String[0]);
        }
        add(cases, ClientScriptOpCode.ACTIVECHATPHRASE_PREPARE, true, new String[0]);
        sends(cases, false, random, base);
        add(cases, ClientScriptOpCode.ACTIVECHATPHRASE_SETDYNAMICINT, false, new String[0], 0, 5);
        add(cases, ClientScriptOpCode.ACTIVECHATPHRASE_PREPARE, false, new String[0], 10);
        add(cases, ClientScriptOpCode.ACTIVECHATPHRASE_SETDYNAMICOBJ, false, new String[0], 10, 5);
        add(cases, ClientScriptOpCode.ACTIVECHATPHRASE_SETDYNAMICOBJ, false, new String[0], 0, 5);
        return cases;
    }

    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        staticSet("CURRENT_LANGUAGE", Language.ENGLISH);
        staticSet("gameState", com.jagex.game.runetek5.client.GameState.UNK_4);
        QuickChatFixtures.install(
            QuickChatFixtures.provider(QuickChatFixtures.messageCategories(), QuickChatFixtures.messages()),
            QuickChatFixtures.provider(QuickChatFixtures.menuCategories(), QuickChatFixtures.menuMessages()));
        var context = (BufferedConnectionContext) staticGet("GAME_CONNECTION_CONTEXT");
        var executor = new CS2Executor();
        executor.intOpValues = new int[1];
        executor.instrPtr = 0;
        var cases = cases();
        var out = new StringBuilder();
        var stats = new TreeMap<String, int[]>();
        int packetsTotal = 0;
        for (var entry : cases) {
            context.clearAllQueuedPackets();
            Arrays.fill(executor.intStack, 0x5a5a5a5a);
            Arrays.fill(executor.stringStack, null);
            executor.intStackPtr = entry.ints.length;
            System.arraycopy(entry.ints, 0, executor.intStack, 0, entry.ints.length);
            executor.stringStackPtr = entry.strings.length;
            System.arraycopy(entry.strings, 0, executor.stringStack, 0, entry.strings.length);
            String result = "ok";
            try {
                QuickChatDisplayOps.dispatch(entry.opcode, executor);
            } catch (Throwable error) {
                result = "error:" + error.getClass().getSimpleName();
            }
            var packets = new ArrayList<String>();
            var node = context.queuedPackets.getFirst();
            while (node != null) {
                packets.add(hex((TcpPacket) node));
                node = context.queuedPackets.getNext();
            }
            packetsTotal += packets.size();
            if (!entry.bare && result.equals("ok") && (executor.intStackPtr != 1 || executor.intStack[0] != 727
                || executor.stringStackPtr != 1 || !"lower-string".equals(executor.stringStack[0]))) throw new AssertionError(entry.name);
            var ints = Arrays.copyOf(executor.intStack, Math.max(executor.intStackPtr, 0));
            var strings = new String[Math.max(executor.stringStackPtr, 0)];
            for (int i = 0; i < strings.length; i++) strings[i] = (String) executor.stringStack[i];
            out.append("T\t").append(entry.name).append('\t').append(entry.opcode.name()).append('\t')
                .append(NativeFixtures.integers(entry.ints)).append('\t').append(NativeFixtures.texts(entry.strings)).append('\t')
                .append(result).append('\t').append(NativeFixtures.integers(ints)).append('\t').append(NativeFixtures.texts(strings)).append('\t')
                .append(packets.isEmpty() ? "-" : String.join("|", packets)).append('\t').append(state(executor)).append('\n');
            stats.computeIfAbsent(entry.opcode.name(), k -> new int[2])[result.equals("ok") ? 0 : 1]++;
        }
        out.append("Z\t").append(cases.size()).append('\n');
        Files.writeString(output.resolve("chat-phrase.native.tsv"), out.toString());
        var summary = new StringBuilder();
        for (var item : stats.entrySet()) summary.append(item.getKey()).append('\t').append(item.getValue()[0]).append('\t').append(item.getValue()[1]).append('\n');
        Files.writeString(output.resolve("chat-phrase.stats.tsv"), summary.toString());
        System.out.println("Native chat phrase cases " + cases.size() + " packets " + packetsTotal);
        System.exit(0);
    }
}
