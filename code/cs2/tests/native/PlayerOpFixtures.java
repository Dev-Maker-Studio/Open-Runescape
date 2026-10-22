import com.jagex.game.runetek5.client.Client;
import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.*;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.interaction.chat.ChatBox;
import com.jagex.game.runetek5.network.BufferedConnectionContext;
import com.jagex.game.runetek5.network.packet.TcpPacket;
import com.jagex.game.runetek5.tools.Language;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class PlayerOpFixtures {
    static final long SEED = 0x70a1e40L;
    static final String[] LANGS = {"EN", "DE", "FR", "PT", "NL", "ES"};
    static final String[] NAMES = {"Zezima", "zezima", "ZEZIMA", "Mod Mark", "Bob", "", "x", "A", "a", "Straße", "STRASSE", "café", "CAFÉ", "é", "É", "9", "a b", "A B", "i", "I", "ı", "İ"};
    static final int[] OPS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, -1, 255, 65536};
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

    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        var theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        unsafe = (sun.misc.Unsafe) theUnsafe.get(null);
        var context = (BufferedConnectionContext) staticGet(Client.class, "GAME_CONNECTION_CONTEXT");
        staticSet(Client.class, "gameState", com.jagex.game.runetek5.client.GameState.UNK_4);
        var random = new Random(SEED);
        var out = new StringBuilder();
        var states = new ArrayList<Object[]>();
        for (int s = 0; s < 60; s++) {
            boolean self = random.nextInt(6) != 0;
            int count = random.nextInt(7);
            var entries = new ArrayList<Object[]>();
            for (int i = 0; i < count; i++) {
                int index = 1 + random.nextInt(2047);
                int kind = random.nextInt(10);
                String name = random.nextInt(8) == 0 ? null : NAMES[random.nextInt(NAMES.length)];
                entries.add(new Object[] {index, kind == 0 ? "N" : kind == 1 ? "S" : "P", name});
            }
            states.add(new Object[] {self, entries, LANGS[random.nextInt(LANGS.length)], s % 11 == 0 ? "~" : null});
        }
        for (int s = 0; s < states.size(); s++) {
            var state = states.get(s);
            var sb = new StringBuilder("H\t").append(s).append('\t').append((Boolean) state[0] ? 1 : 0).append('\t').append(state[2]);
            for (var entry : (List<Object[]>) state[1]) {
                sb.append('\t').append(entry[0]).append(':').append(entry[1]).append(':').append(nullable((String) entry[2]));
            }
            out.append(sb).append('\n');
        }
        var cases = new ArrayList<Object[]>();
        for (int s = 0; s < states.size(); s++) {
            var entries = (List<Object[]>) states.get(s)[1];
            var wanted = new ArrayList<String>(Arrays.asList(NAMES));
            for (var entry : entries) wanted.add((String) entry[2]);
            for (var name : wanted) {
                for (int i = 0; i < 4; i++) cases.add(new Object[] {"OPPLAYER", s, OPS[random.nextInt(OPS.length)], name});
                cases.add(new Object[] {"OPPLAYER", s, 1 + random.nextInt(9), name});
            }
            for (var name : wanted) {
                for (int i = 0; i < 3; i++) {
                    int active = random.nextInt(5) == 0 ? 0 : 1;
                    int flags = random.nextInt(4) == 0 ? random.nextInt(128) : (random.nextInt(4) << 3) | random.nextInt(4);
                    int[] hashes = {0, 1, 65536, 0x7fff0000, -65536, 0x00010003, -1, 0x00ff00ff};
                    cases.add(new Object[] {"OPPLAYERT", s, 0, name, active + ":" + flags + ":" + (random.nextInt(8) - 2) + ":" + hashes[random.nextInt(hashes.length)] + ":" + (random.nextInt(4) == 0 ? -1 : random.nextInt(30000))});
                }
            }
            cases.add(new Object[] {"IF_CLOSE", s, 0, null});
        }
        Collections.shuffle(cases, random);
        var stats = new TreeMap<String, int[]>();
        int total = 0;
        int lastState = -1;
        for (var item : cases) {
            String op = (String) item[0];
            int s = (Integer) item[1];
            var state = states.get(s);
            Arrays.fill(PlayerUpdate.players, null);
            Arrays.fill(PlayerUpdate.OUT_PLAYER_INDICES, 0);
            PlayerEntity me = null;
            if ((Boolean) state[0]) {
                me = (PlayerEntity) unsafe.allocateInstance(PlayerEntity.class);
                set(me, "displayName", "Me");
                set(me, "movementBaseX", new int[] {-1});
                set(me, "movementBaseY", new int[] {-1});
                set(me, "sizeInSquares", 1);
            }
            PlayerUpdate.MY_PLAYER = me;
            int n = 0;
            for (var entry : (List<Object[]>) state[1]) {
                int index = (Integer) entry[0];
                String kind = (String) entry[1];
                if (kind.equals("N")) {
                    PlayerUpdate.OUT_PLAYER_INDICES[n++] = index;
                    continue;
                }
                PlayerEntity entity;
                if (kind.equals("S") && me != null) {
                    entity = me;
                } else {
                    entity = (PlayerEntity) unsafe.allocateInstance(PlayerEntity.class);
                    set(entity, "displayName", entry[2]);
                }
                set(entity, "movementBaseX", new int[] {(index * 7) % 200 - 20});
                set(entity, "movementBaseY", new int[] {(index * 13) % 150 - 10});
                set(entity, "sizeInSquares", 1 + index % 3);
                PlayerUpdate.players[index] = entity;
                PlayerUpdate.OUT_PLAYER_INDICES[n++] = index;
            }
            PlayerUpdate.NUM_OUT_PLAYER_INDICES = n;
            staticSet(Client.class, "CURRENT_LANGUAGE", language((String) state[2]));
            context.clearAllQueuedPackets();
            int linesBefore = ChatBox.NUM_CHAT_LINES;
            var e = new CS2Executor();
            e.intOpValues = new int[] {0};
            e.instrPtr = 0;
            Arrays.fill(e.intStack, 0x5a5a5a5a);
            e.intStackPtr = 0;
            e.stringStackPtr = 0;
            String result = "ok";
            String argument = "-";
            String selectionAfter = "-";
            String route = "-";
            var routes = com.jagex.game.runetek5.movement.pathfinding.RouteStrategies.PLAYER;
            routes.destinationX = -77;
            routes.destinationY = -77;
            routes.destinationSizeX = -77;
            routes.destinationSizeY = -77;
            staticSet(Client.class, "interfaceSelectionActive", false);
            staticSet(Client.class, "defaultMenuCursor", 5);
            staticSet(Client.class, "selectedItemId", 7);
            Class<?> builder = Class.forName("com.jagex.game.runetek5.client.menu.MiniMenuBuilderKt");
            staticSet(builder, "USE_OPTIONS_FLAGS", 0);
            staticSet(com.jagex.game.runetek5.config.rsinterface.Interface.class, "INTERFACES", new com.jagex.game.runetek5.config.rsinterface.Interface[0]);
            try {
                if (op.equals("OPPLAYER")) {
                    e.stringStack[e.stringStackPtr++] = (String) item[3];
                    e.intStack[e.intStackPtr++] = (Integer) item[2];
                    argument = item[2] + "|" + nullable((String) item[3]);
                }
                if (op.equals("OPPLAYERT")) {
                    var parts = ((String) item[4]).split(":");
                    e.stringStack[e.stringStackPtr++] = (String) item[3];
                    staticSet(Client.class, "interfaceSelectionActive", parts[0].equals("1"));
                    staticSet(builder, "USE_OPTIONS_FLAGS", Integer.parseInt(parts[1]));
                    staticSet(Client.class, "selectedSlotId", Integer.parseInt(parts[2]));
                    staticSet(Client.class, "selectedComponentHash", Integer.parseInt(parts[3]));
                    staticSet(Client.class, "selectedItemId", Integer.parseInt(parts[4]));
                    argument = nullable((String) item[3]) + "|" + item[4];
                }
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
            if (op.equals("OPPLAYERT")) {
                selectionAfter = ((Boolean) staticGet(Client.class, "interfaceSelectionActive") ? 1 : 0) + ":" + staticGet(Client.class, "selectedItemId") + ":" + staticGet(Client.class, "defaultMenuCursor");
                route = routes.destinationX == -77 ? "-" : routes.destinationX + ":" + routes.destinationY + ":" + routes.destinationSizeX + ":" + routes.destinationSizeY;
            }
            int added = ChatBox.NUM_CHAT_LINES - linesBefore;
            String message = added == 0 ? "-" : added + ":" + ChatBox.CHAT_LINES[0].message;
            out.append("T\t").append(op).append('\t').append(s).append('\t').append(argument).append('\t').append(result).append('\t')
                .append(ints(e.intStack, e.intStackPtr)).append('\t').append(packets.isEmpty() ? "-" : String.join("|", packets)).append('\t')
                .append(message).append('\t').append(selectionAfter).append('\t').append(route).append('\n');
            stats.computeIfAbsent(op, k -> new int[2])[result.equals("ok") ? 0 : 1]++;
            total++;
        }
        out.append("Z\t").append(total).append('\n');
        Files.writeString(output.resolve("player-op.native.tsv"), out.toString(), StandardCharsets.UTF_8);
        var summary = new StringBuilder();
        for (var entry : stats.entrySet()) summary.append(entry.getKey()).append('\t').append(entry.getValue()[0]).append('\t').append(entry.getValue()[1]).append('\n');
        Files.writeString(output.resolve("player-op.stats.tsv"), summary.toString(), StandardCharsets.UTF_8);
        System.exit(0);
    }
}
