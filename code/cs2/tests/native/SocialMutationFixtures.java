import com.jagex.game.runetek5.client.Client;
import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.*;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.interaction.*;
import com.jagex.game.runetek5.interaction.chat.ChatBox;
import com.jagex.game.runetek5.interaction.chat.friendschat.*;
import com.jagex.game.runetek5.network.BufferedConnectionContext;
import com.jagex.game.runetek5.network.packet.TcpPacket;
import com.jagex.game.runetek5.tools.Language;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class SocialMutationFixtures {
    static final long SEED = 0x50c1a15L;
    static final String[] NAMES = {"Zezima", "zezima", "ZEZIMA", "Mod Mark", "mod_mark", "Älice", "älice", "ÿes", "Ÿes", "Bob", "bob", "", "x", "A", "a", "µ", "Μ", "Straße", "STRASSE", "i", "I", "#hash", "#Hash", " Bob", "Bob_", "Café", "naïve", "a-b", "€uro", "“quote”", "Zezima2", "ab cd ef gh ij", "abcdefghijkl", "abcdefghijklm", "<img=0>Bob", "日本語"};
    static final int[] LONG_LENGTHS = {14, 100, 250, 256, 257, 258, 259, 260, 261, 300};
    static final int[] RANKS = {0, 1, 2, 3, 4, 5, 6, 7, 127, -1, -128, 100, 128, 255, 1000, Integer.MIN_VALUE, Integer.MAX_VALUE};
    static final int[] SETRANK_RANKS = {0, 1, 5, 7, 127, -1, 255, 1000, Integer.MIN_VALUE, Integer.MAX_VALUE, -128, 128};
    static final ClientScriptOpCode[] STRING_OPS = {
        ClientScriptOpCode.FRIEND_ADD, ClientScriptOpCode.FRIEND_DEL, ClientScriptOpCode.IGNORE_ADD, ClientScriptOpCode.IGNORE_DEL,
        ClientScriptOpCode.IGNORE_ADD_TEMP, ClientScriptOpCode.CLAN_KICKUSER, ClientScriptOpCode.CLAN_JOINCHAT
    };
    static final ClientScriptOpCode[] PLAIN = {ClientScriptOpCode.CLAN_LEAVECHAT, ClientScriptOpCode.CLAN_GETCHATDISPLAYNAME};
    static final String[] FC_NAMES = {"zezima", "Zezima", "mod mark", "a", "abcdefghijkl", "x_y", "9lives", "ab12cd34ef56", "!!", "", "toolongnameforchannel", "Älice"};
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

    static void put(Object target, String name, Object value) throws Exception {
        var f = field(target.getClass(), name);
        unsafe.putObject(target, unsafe.objectFieldOffset(f), value);
    }

    static Object staticGet(String name) throws Exception {
        var f = field(Client.class, name);
        f.setAccessible(true);
        return f.get(null);
    }

    static void staticSet(String name, Object value) throws Exception {
        var f = field(Client.class, name);
        f.setAccessible(true);
        f.set(null, value);
    }

    static String nullable(String value) {
        return value == null ? "~" : value;
    }

    static String esc(String value) {
        return value == null ? "~" : value.replace("\0", "\\0");
    }

    static String pickName(Random random) {
        return NAMES[random.nextInt(NAMES.length)];
    }

    static String pickNullable(Random random) {
        return random.nextInt(4) == 0 ? null : pickName(random);
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

    static String friendsSnapshot() {
        var sb = new StringBuilder();
        if (FriendList.FRIEND_COUNT > 20) {
            int n = FriendList.FRIEND_COUNT;
            return "#" + n + ":" + FriendList.FRIENDS[0].displayName + "," + FriendList.FRIENDS[n - 1].displayName;
        }
        for (int i = 0; i < FriendList.FRIEND_COUNT; i++) {
            if (i > 0) sb.append(',');
            sb.append(FriendList.FRIENDS[i].displayName);
        }
        return sb.length() == 0 ? "-" : sb.toString();
    }

    static String ignoresSnapshot() {
        var sb = new StringBuilder();
        if (IgnoreList.IGNORE_COUNT > 20) {
            int n = IgnoreList.IGNORE_COUNT;
            return "#" + n + ":" + IgnoreList.IGNORED[0].unfilteredUsername + "," + IgnoreList.IGNORED[n - 1].unfilteredUsername;
        }
        for (int i = 0; i < IgnoreList.IGNORE_COUNT; i++) {
            if (i > 0) sb.append(',');
            sb.append(nullable(IgnoreList.IGNORED[i].unfilteredUsername));
        }
        return sb.length() == 0 ? "-" : sb.toString();
    }

    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        var theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        unsafe = (sun.misc.Unsafe) theUnsafe.get(null);
        staticSet("CURRENT_LANGUAGE", Language.ENGLISH);
        var context = (BufferedConnectionContext) staticGet("GAME_CONNECTION_CONTEXT");
        var out = new StringBuilder();
        var random = new Random(SEED);
        int stateCount = 30;
        var applies = new ArrayList<Runnable>();
        for (int s = 0; s < stateCount; s++) {
            int friendCount = s == 26 ? 200 : s == 27 ? 199 : s % 6 == 5 ? 0 : 1 + random.nextInt(6);
            int ignoreCount = s == 28 ? 100 : s == 29 ? 99 : s % 7 == 6 ? 0 : 1 + random.nextInt(5);
            var friends = new Friend[friendCount];
            out.append("F\t").append(s).append('\t').append(friendCount).append('\n');
            for (int i = 0; i < friendCount; i++) {
                var friend = new Friend();
                friend.displayName = friendCount >= 199 ? "f" + i : pickName(random);
                friend.worldName = "w";
                friend.previousDisplayName = friendCount >= 199 ? "p" + i : pickName(random);
                friends[i] = friend;
                out.append("f\t").append(s).append('\t').append(friend.displayName).append('\t').append(friend.previousDisplayName).append('\n');
            }
            var ignores = new Ignore[ignoreCount];
            out.append("I\t").append(s).append('\t').append(ignoreCount).append('\n');
            for (int i = 0; i < ignoreCount; i++) {
                var ignore = new Ignore();
                boolean wide = ignoreCount >= 99;
                ignore.unfilteredUsername = wide ? "i" + i : (s % 5 == 4 ? pickNullable(random) : pickName(random));
                ignore.displayName = wide ? "d" + i : pickNullable(random);
                ignore.temporary = random.nextBoolean();
                ignores[i] = ignore;
                out.append("i\t").append(s).append('\t').append(nullable(ignore.unfilteredUsername)).append('\t').append(nullable(ignore.displayName)).append('\t').append(ignore.temporary ? 1 : 0).append('\n');
            }
            String fcName = s % 4 == 3 ? null : FC_NAMES[random.nextInt(FC_NAMES.length)];
            boolean playersNull = s % 3 == 2;
            int mine = s % 6;
            String myName = mine == 2 ? null : pickName(random);
            if (mine == 5) myName = "Zezima";
            if (mine == 3) myName = "Bob";
            out.append("C\t").append(s).append('\t').append(nullable(fcName)).append('\t').append(playersNull ? 1 : 0).append('\t').append(mine == 4 ? 0 : 1).append('\t').append(nullable(myName)).append('\n');
            final String fFc = fcName, fMy = myName;
            applies.add(() -> {
                try {
                    Arrays.fill(FriendList.FRIENDS, null);
                    System.arraycopy(friends, 0, FriendList.FRIENDS, 0, friends.length);
                    FriendList.FRIEND_COUNT = friends.length;
                    FriendList.LOADED = 2;
                    Arrays.fill(IgnoreList.IGNORED, null);
                    System.arraycopy(ignores, 0, IgnoreList.IGNORED, 0, ignores.length);
                    IgnoreList.IGNORE_COUNT = ignores.length;
                    FriendsChat.FC_NAME = fFc;
                    FriendsChat.FC_PLAYERS = playersNull ? null : new FriendsChatPlayer[2];
                    if (mine == 4) {
                        PlayerUpdate.MY_PLAYER = null;
                    } else {
                        var entity = (PlayerEntity) unsafe.allocateInstance(PlayerEntity.class);
                        put(entity, "displayName", fMy);
                        PlayerUpdate.MY_PLAYER = entity;
                    }
                } catch (Exception error) {
                    throw new RuntimeException(error);
                }
            });
        }
        var names = new ArrayList<String>(Arrays.asList(NAMES));
        for (int length : LONG_LENGTHS) names.add("a".repeat(length));
        names.add("a\0b");
        var cases = new ArrayList<Object[]>();
        for (int s = 0; s < stateCount; s++) {
            for (var op : PLAIN) cases.add(new Object[] {op, s, "-", null, null});
            for (var op : STRING_OPS) {
                cases.add(new Object[] {op, s, "u", null, null});
                cases.add(new Object[] {op, s, "n", null, null});
                for (var name : names) cases.add(new Object[] {op, s, "s", null, name});
            }
            var op = ClientScriptOpCode.FRIEND_SETRANK;
            cases.add(new Object[] {op, s, "u", null, null});
            cases.add(new Object[] {op, s, "n", 3, null});
            for (var name : names) {
                cases.add(new Object[] {op, s, "S", null, name});
                for (int rank : SETRANK_RANKS) cases.add(new Object[] {op, s, "r", rank, name});
            }
        }
        Collections.shuffle(cases, random);
        var stats = new TreeMap<String, int[]>();
        int total = 0;
        for (var item : cases) {
            var op = (ClientScriptOpCode) item[0];
            int state = (Integer) item[1];
            String mode = (String) item[2];
            applies.get(state).run();
            context.clearAllQueuedPackets();
            staticSet("gameState", com.jagex.game.runetek5.client.GameState.UNK_4);
            staticSet("serverTick", 7);
            staticSet("lastFriendIgnoreUpdateTick", -1);
            int linesBefore = ChatBox.NUM_CHAT_LINES;
            var e = new CS2Executor();
            e.intOpValues = new int[] {0};
            e.instrPtr = 0;
            Arrays.fill(e.intStack, 0x5a5a5a5a);
            e.intStackPtr = 0;
            e.stringStackPtr = 0;
            String argument = "-";
            switch (mode) {
                case "s" -> {
                    e.stringStack[e.stringStackPtr++] = (String) item[4];
                    argument = "s" + esc((String) item[4]);
                }
                case "S" -> {
                    e.stringStack[e.stringStackPtr++] = (String) item[4];
                    argument = "S" + esc((String) item[4]);
                }
                case "r" -> {
                    e.intStack[e.intStackPtr++] = (Integer) item[3];
                    e.stringStack[e.stringStackPtr++] = (String) item[4];
                    argument = "r" + item[3] + ":" + esc((String) item[4]);
                }
                case "n" -> {
                    if (item[3] != null) {
                        e.intStack[e.intStackPtr++] = (Integer) item[3];
                        argument = "r" + item[3] + ":~";
                    } else {
                        argument = "n";
                    }
                    e.stringStack[e.stringStackPtr++] = null;
                }
                case "u" -> argument = "u";
                default -> {
                }
            }
            String result = "ok";
            try {
                EnumEmailFriendOps.dispatch(op, e);
            } catch (Throwable error) {
                result = "error:" + error.getClass().getSimpleName();
            }
            var packets = new ArrayList<String>();
            var node = context.queuedPackets.getFirst();
            while (node != null) {
                packets.add(hex((TcpPacket) node));
                node = context.queuedPackets.getNext();
            }
            int added = ChatBox.NUM_CHAT_LINES - linesBefore;
            String message = added == 0 ? "-" : added + ":" + ChatBox.CHAT_LINES[0].message;
            int tick = (Integer) staticGet("lastFriendIgnoreUpdateTick");
            out.append("T\t").append(op.name()).append('\t').append(state).append('\t').append(argument).append('\t').append(result).append('\t')
                .append(ints(e.intStack, e.intStackPtr)).append('\t').append(strings(e.stringStack, e.stringStackPtr)).append('\t')
                .append(packets.isEmpty() ? "-" : String.join("|", packets)).append('\t').append(message).append('\t').append(tick == 7 ? 1 : 0).append('\t')
                .append(friendsSnapshot()).append('\t').append(ignoresSnapshot()).append('\n');
            stats.computeIfAbsent(op.name(), k -> new int[2])[result.equals("ok") ? 0 : 1]++;
            total++;
        }
        out.append("Z\t").append(total).append('\n');
        Files.writeString(output.resolve("social-mutations.native.tsv"), out.toString(), StandardCharsets.UTF_8);
        var summary = new StringBuilder();
        for (var entry : stats.entrySet()) summary.append(entry.getKey()).append('\t').append(entry.getValue()[0]).append('\t').append(entry.getValue()[1]).append('\n');
        Files.writeString(output.resolve("social-mutations.stats.tsv"), summary.toString(), StandardCharsets.UTF_8);
        System.exit(0);
    }
}
