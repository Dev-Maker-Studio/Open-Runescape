import com.jagex.game.runetek5.client.Client;
import com.jagex.game.runetek5.client.GameState;
import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.*;
import com.jagex.game.runetek5.encoding.Huffman;
import com.jagex.game.runetek5.encoding.HuffmanList;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.network.BufferedConnectionContext;
import com.jagex.game.runetek5.network.packet.TcpPacket;
import com.jagex.game.runetek5.tools.Language;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class ChatSendFixtures {
    static final long SEED = 0xc4a75e1dL;
    static final String[] COLORS = {
        "yellow:", "red:", "green:", "cyan:", "purple:", "white:", "flash1:", "flash2:", "flash3:", "glow1:", "glow2:", "glow3:"
    };
    static final String[][] COLOR_NAMES = {
        {"yellow:", "gelb:", "jaune:", "amarelo:"}, {"red:", "rot:", "rouge:", "vermelho:"}, {"green:", "grün:", "vert:", "verde:"},
        {"cyan:", "blaugrün:", "cyan:", "cyan:"}, {"purple:", "lila:", "violet:", "roxo:"}, {"white:", "weiss:", "blanc:", "branco:"},
        {"flash1:", "blinken1:", "clignotant1:", "flash1:"}, {"flash2:", "blinken2:", "clignotant2:", "flash2:"},
        {"flash3:", "blinken3:", "clignotant3:", "brilho3:"}, {"glow1:", "leuchten1:", "brillant1:", "brilho1:"},
        {"glow2:", "leuchten2:", "brillant2:", "brilho2:"}, {"glow3:", "leuchten3:", "brillant3:", "brilho3:"}
    };
    static final String[][] EFFECT_NAMES = {
        {"wave:", "welle:", "ondulation:", "onda:"}, {"wave2:", "welle2:", "ondulation2:", "onda2:"},
        {"shake:", "schütteln:", "tremblement:", "tremor:"}, {"scroll:", "scrollen:", "déroulement:", "rolagem:"},
        {"slide:", "gleiten:", "glissement:", "deslizamento:"}
    };
    static final String[] BODIES = {
        "", "hello", "Hello World", "x", "a b c", "  spaced  ", "numbers 1234567890", "punct, !?.;'\"-_()", "café é ü ß",
        "UPPER CASE", "colon: inside", "red", "wave", ":", "x".repeat(60), "y".repeat(120), "z".repeat(250), "w".repeat(400),
        "the quick brown fox jumps over the lazy dog", "e".repeat(255), "q".repeat(300)
    };
    static final String[] NAMES = {"Zezima", "zezima", "Mod Mark", "Bob", "", "x", "A", "Straße", "a b", "9", "_", "café"};
    static final String[] LANGS = {"null", "EN", "DE", "FR", "PT", "NL", "ES"};
    static final byte[] TABLE_A = new byte[256];
    static sun.misc.Unsafe unsafe;

    static {
        for (int i = 0; i < 256; i++) {
            TABLE_A[i] = (byte) (i < 64 ? 7 : i < 128 ? 8 : 9);
        }
    }

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

    static Language language(String name) {
        return switch (name) {
            case "EN" -> Language.ENGLISH;
            case "DE" -> Language.GERMAN;
            case "FR" -> Language.FRENCH;
            case "PT" -> Language.PORTUGESE;
            case "NL" -> Language.DUTCH;
            case "ES" -> Language.SPANISH;
            default -> null;
        };
    }

    static String publicMessage(Random random) {
        var sb = new StringBuilder();
        int lang = random.nextInt(4);
        if (random.nextInt(5) != 0) {
            var name = COLOR_NAMES[random.nextInt(COLOR_NAMES.length)][lang];
            sb.append(random.nextBoolean() ? name : random.nextBoolean() ? name.toUpperCase() : Character.toUpperCase(name.charAt(0)) + name.substring(1));
        }
        if (random.nextInt(3) != 0) {
            var name = EFFECT_NAMES[random.nextInt(EFFECT_NAMES.length)][random.nextInt(4)];
            sb.append(random.nextBoolean() ? name : name.toUpperCase());
        }
        sb.append(BODIES[random.nextInt(BODIES.length)]);
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        var theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        unsafe = (sun.misc.Unsafe) theUnsafe.get(null);
        HuffmanList.HUFFMAN = new Huffman(TABLE_A);
        var context = (BufferedConnectionContext) staticGet(Client.class, "GAME_CONNECTION_CONTEXT");
        staticSet(Client.class, "gameState", GameState.UNK_4);
        var out = new StringBuilder();
        var random = new Random(SEED);
        int[][] gates = {{0, 0, 0, 0}, {0, 1, 0, 0}, {0, 1, 1, 0}, {0, 0, 0, 1}, {0, 1, 1, 1}, {2, 1, 0, 1}, {1, 0, 0, 0}, {7, 1, 0, 1}};
        String[][] players = {
            null, {"~", "~", "~", "~"}, {"Zezima", "Zezima", "~", "~"}, {"Zezima", "zezima", "Sir ", " the Great"},
            {"Mod Mark", "~", "~", "~"}, {"Bob", "Bob 2", "Lord ", "~"}, {"Bob", "~", "~", " Esq"}, {"", "", "", ""}
        };
        var states = new ArrayList<Object[]>();
        for (var gate : gates) {
            for (var lang : LANGS) {
                var player = players[random.nextInt(players.length)];
                states.add(new Object[] {gate, lang, player});
            }
        }
        for (int s = 0; s < states.size(); s++) {
            var state = states.get(s);
            var gate = (int[]) state[0];
            var player = (String[]) state[2];
            out.append("H\t").append(s).append('\t').append(gate[0]).append('\t').append(gate[1]).append('\t').append(gate[2]).append('\t')
                .append(gate[3]).append('\t').append(state[1]).append('\t').append(player == null ? "-" : "P\t" + String.join("\t", player)).append('\n');
        }
        var cases = new ArrayList<Object[]>();
        for (int s = 0; s < states.size(); s++) {
            for (var op : new String[] {"CHAT_PLAYERNAME", "CHAT_PLAYERNAME_UNFILTERED"}) {
                cases.add(new Object[] {op, s, new String[0]});
            }
            for (int i = 0; i < 40; i++) cases.add(new Object[] {"CHAT_SENDPUBLIC", s, new String[] {publicMessage(random)}});
            for (var body : BODIES) cases.add(new Object[] {"CHAT_SENDPUBLIC", s, new String[] {body}});
            cases.add(new Object[] {"CHAT_SENDPUBLIC", s, new String[] {null}});
            for (int i = 0; i < 14; i++) {
                var recipient = random.nextInt(8) == 0 ? null : NAMES[random.nextInt(NAMES.length)];
                var message = random.nextInt(8) == 0 ? null : BODIES[random.nextInt(BODIES.length)];
                cases.add(new Object[] {"CHAT_SENDPRIVATE", s, new String[] {recipient, message}});
            }
        }
        Collections.shuffle(cases, random);
        var stats = new TreeMap<String, int[]>();
        int total = 0;
        for (var item : cases) {
            String op = (String) item[0];
            int state = (Integer) item[1];
            String[] stringArgs = (String[]) item[2];
            var config = states.get(state);
            var gate = (int[]) config[0];
            var player = (String[]) config[2];
            Client.PLAYER_RIGHTS = gate[0];
            Client.USERDETAIL_QUICKCHAT = gate[1] != 0;
            Client.VERIFIED_EMAIL_ADDRESS = gate[2] != 0;
            Client.IS_QUICKCHAT_ONLY = gate[3] != 0;
            staticSet(Client.class, "CURRENT_LANGUAGE", language((String) config[1]));
            if (player == null) {
                PlayerUpdate.MY_PLAYER = null;
            } else {
                var entity = (PlayerEntity) unsafe.allocateInstance(PlayerEntity.class);
                set(entity, "username", player[0].equals("~") ? null : player[0]);
                set(entity, "displayName", player[1].equals("~") ? null : player[1]);
                set(entity, "prefixTitle", player[2].equals("~") ? null : player[2]);
                set(entity, "postfixTitle", player[3].equals("~") ? null : player[3]);
                PlayerUpdate.MY_PLAYER = entity;
            }
            context.clearAllQueuedPackets();
            var e = new CS2Executor();
            e.intOpValues = new int[] {0};
            e.instrPtr = 0;
            Arrays.fill(e.intStack, 0x5a5a5a5a);
            e.intStackPtr = 0;
            e.stringStackPtr = 0;
            String result = "ok";
            try {
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
            out.append("T\t").append(op).append('\t').append(state).append('\t').append(stringArgs.length == 0 ? "-" : strs.toString()).append('\t')
                .append(result).append('\t').append(strings(e.stringStack, e.stringStackPtr)).append('\t')
                .append(packets.isEmpty() ? "-" : String.join("|", packets)).append('\n');
            stats.computeIfAbsent(op, k -> new int[2])[result.equals("ok") ? 0 : 1]++;
            total++;
        }
        out.append("Z\t").append(total).append('\n');
        Files.writeString(output.resolve("chat-send.native.tsv"), out.toString(), StandardCharsets.UTF_8);
        var summary = new StringBuilder();
        for (var entry : stats.entrySet()) summary.append(entry.getKey()).append('\t').append(entry.getValue()[0]).append('\t').append(entry.getValue()[1]).append('\n');
        Files.writeString(output.resolve("chat-send.stats.tsv"), summary.toString(), StandardCharsets.UTF_8);
        System.exit(0);
    }
}
