import com.jagex.game.runetek5.client.Client;
import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.*;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.interaction.*;
import com.jagex.game.runetek5.interaction.chat.friendschat.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class SocialStateFixtures {
    static final long SEED = 0x50c1a15L;
    static final String[] NAMES = {"Zezima", "zezima", "ZEZIMA", "Mod Mark", "mod_mark", "Älice", "älice", "ÿes", "Ÿes", "Bob", "bob", "", "x", "A", "a", "µ", "Μ", "Straße", "STRASSE", "i", "I"};
    static final String[] QUERIES = {"Zezima", "zezima", "ZEZIMA", "Mod Mark", "MOD_MARK", "ÄLICE", "älice", "ŸES", "Bob", "<img=0>Bob", "<img=1>bob", "<img=2>Bob", "<img=0>", "<img=1>", "<img=0", "", "x", "X", "µ", "Μ", "Straße", "STRASSE", "nobody", "<img=0>Zezima", "<img=1>zezima", " Bob"};
    static final int[] INDEXES = {-3, -2, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 99, 100, 101, 199, 200, 255, Integer.MAX_VALUE, Integer.MIN_VALUE};
    static final int[] RANKS = {0, 1, 2, 3, 4, 5, 6, 7, 127, -1, -128, 100};
    static final ClientScriptOpCode[] PLAIN = {
        ClientScriptOpCode.FRIEND_COUNT, ClientScriptOpCode.IGNORE_COUNT, ClientScriptOpCode.CLAN_GETCHATCOUNT, ClientScriptOpCode.CLAN_GETCHATMINKICK,
        ClientScriptOpCode.CLAN_GETCHATRANK, ClientScriptOpCode.CLAN_GETCHATOWNERNAME, ClientScriptOpCode.PLAYERCOUNTRY
    };
    static final ClientScriptOpCode[] INDEXED = {
        ClientScriptOpCode.FRIEND_GETNAME, ClientScriptOpCode.FRIEND_GETWORLD, ClientScriptOpCode.FRIEND_GETRANK, ClientScriptOpCode.FRIEND_GETWORLDFLAGS,
        ClientScriptOpCode.FRIEND_GETWORLDNAME, ClientScriptOpCode.FRIEND_PLATFORM, ClientScriptOpCode.FRIEND_IS_REFERRER,
        ClientScriptOpCode.IGNORE_GETNAME, ClientScriptOpCode.IGNORE_GETNAME_UNFILTERED, ClientScriptOpCode.IGNORE_IS_TEMP,
        ClientScriptOpCode.CLAN_GETCHATUSERNAME, ClientScriptOpCode.CLAN_GETCHATUSERNAME_UNFILTERED, ClientScriptOpCode.CLAN_GETCHATUSERWORLD,
        ClientScriptOpCode.CLAN_GETCHATUSERRANK, ClientScriptOpCode.CLAN_GETCHATUSERWORLDNAME, ClientScriptOpCode.CLAN_ISSELF
    };
    static final ClientScriptOpCode[] TEXTUAL = {ClientScriptOpCode.FRIEND_TEST, ClientScriptOpCode.FRIEND_GETSLOTFROMNAME, ClientScriptOpCode.IGNORE_TEST};
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

    static String nullable(String value) {
        return value == null ? "~" : value;
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

    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        var theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        unsafe = (sun.misc.Unsafe) theUnsafe.get(null);
        var out = new StringBuilder();
        var random = new Random(SEED);
        int stateCount = 18;
        var applies = new ArrayList<Runnable>();
        for (int s = 0; s < stateCount; s++) {
            int loaded = s % 3;
            int friendCount = s % 6 == 5 ? 0 : 1 + random.nextInt(6);
            int ignoreCount = s % 7 == 6 ? 0 : 1 + random.nextInt(5);
            var friends = new Friend[friendCount];
            out.append("F\t").append(s).append('\t').append(loaded).append('\t').append(friendCount).append('\n');
            for (int i = 0; i < friendCount; i++) {
                var friend = new Friend();
                friend.displayName = pickName(random);
                friend.referrer = random.nextBoolean();
                friend.worldId = random.nextInt(300) - 5;
                friend.worldName = pickName(random);
                friend.friendsChatRank = RANKS[random.nextInt(RANKS.length)];
                friend.platform = random.nextInt(6) - 1;
                friend.previousDisplayName = pickName(random);
                friend.worldFlags = random.nextInt();
                friends[i] = friend;
                out.append("f\t").append(s).append('\t').append(friend.displayName).append('\t').append(friend.referrer ? 1 : 0).append('\t').append(friend.worldId)
                    .append('\t').append(friend.worldName).append('\t').append(friend.friendsChatRank).append('\t').append(friend.platform)
                    .append('\t').append(friend.previousDisplayName).append('\t').append(friend.worldFlags).append('\n');
            }
            var ignores = new Ignore[ignoreCount];
            out.append("I\t").append(s).append('\t').append(ignoreCount).append('\n');
            for (int i = 0; i < ignoreCount; i++) {
                var ignore = new Ignore();
                ignore.unfilteredUsername = pickNullable(random);
                ignore.displayName = pickNullable(random);
                ignore.temporary = random.nextBoolean();
                ignores[i] = ignore;
                out.append("i\t").append(s).append('\t').append(nullable(ignore.unfilteredUsername)).append('\t').append(nullable(ignore.displayName)).append('\t').append(ignore.temporary ? 1 : 0).append('\n');
            }
            String fcName = s % 4 == 3 ? null : pickName(random);
            String owner = s % 5 == 4 ? null : pickName(random);
            boolean playersNull = s % 6 == 2;
            int length = 1 + random.nextInt(6);
            int fcCount = s % 5 == 0 ? length + 2 : s % 5 == 1 ? Math.max(0, length - 1) : length;
            int kick = RANKS[random.nextInt(RANKS.length)];
            int myRank = RANKS[random.nextInt(RANKS.length)];
            int mine = s % 6;
            String myName = mine == 2 ? null : pickName(random);
            if (mine == 5) myName = "Zezima";
            int country = random.nextInt(200) - 3;
            out.append("C\t").append(s).append('\t').append(nullable(fcName)).append('\t').append(nullable(owner)).append('\t').append(playersNull ? 1 : 0).append('\t').append(length)
                .append('\t').append(fcCount).append('\t').append(kick).append('\t').append(myRank).append('\t').append(mine == 4 ? 0 : 1).append('\t').append(nullable(myName)).append('\t').append(country).append('\n');
            var players = new FriendsChatPlayer[length];
            for (int i = 0; i < length; i++) {
                if (random.nextInt(6) == 0) continue;
                var player = new FriendsChatPlayer();
                player.rank = (byte) RANKS[random.nextInt(RANKS.length)];
                player.username = i == 1 && s % 2 == 0 ? "Zezima" : pickNullable(random);
                player.displayName = pickNullable(random);
                player.worldName = pickNullable(random);
                player.worldId = random.nextInt(300) - 5;
                players[i] = player;
                out.append("p\t").append(s).append('\t').append(i).append('\t').append(player.rank).append('\t').append(nullable(player.username)).append('\t').append(nullable(player.displayName))
                    .append('\t').append(nullable(player.worldName)).append('\t').append(player.worldId).append('\n');
            }
            final String fFc = fcName, fOwner = owner, fMy = myName;
            applies.add(() -> {
                try {
                    Arrays.fill(FriendList.FRIENDS, null);
                    System.arraycopy(friends, 0, FriendList.FRIENDS, 0, friends.length);
                    FriendList.FRIEND_COUNT = friends.length;
                    FriendList.LOADED = loaded;
                    Arrays.fill(IgnoreList.IGNORED, null);
                    System.arraycopy(ignores, 0, IgnoreList.IGNORED, 0, ignores.length);
                    IgnoreList.IGNORE_COUNT = ignores.length;
                    FriendsChat.FC_NAME = fFc;
                    FriendsChat.FC_OWNER_NAME = fOwner;
                    FriendsChat.FC_PLAYERS = playersNull ? null : players.clone();
                    FriendsChat.FC_PLAYER_COUNT = fcCount;
                    FriendsChat.FC_MIN_RANK_CAN_KICK = (byte) kick;
                    FriendsChat.MY_PLAYER_FC_RANK = (byte) myRank;
                    Client.COUNTRY = country;
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
        var cases = new ArrayList<Object[]>();
        for (int s = 0; s < stateCount; s++) {
            for (var op : PLAIN) {
                cases.add(new Object[] {op, s, 0, null});
            }
            for (var op : INDEXED) {
                cases.add(new Object[] {op, s, 2, null});
                for (int index : INDEXES) cases.add(new Object[] {op, s, 1, index});
                for (int index = -2; index < 9; index++) cases.add(new Object[] {op, s, 1, index});
            }
            for (var op : TEXTUAL) {
                cases.add(new Object[] {op, s, 2, null});
                cases.add(new Object[] {op, s, 3, null});
                for (var query : QUERIES) cases.add(new Object[] {op, s, 4, query});
                for (var name : NAMES) cases.add(new Object[] {op, s, 4, name});
            }
        }
        Collections.shuffle(cases, random);
        var stats = new TreeMap<String, int[]>();
        int total = 0;
        for (var item : cases) {
            var op = (ClientScriptOpCode) item[0];
            int state = (Integer) item[1];
            int mode = (Integer) item[2];
            applies.get(state).run();
            var e = new CS2Executor();
            e.intOpValues = new int[] {0};
            e.instrPtr = 0;
            Arrays.fill(e.intStack, 0x5a5a5a5a);
            e.intStackPtr = 0;
            e.stringStackPtr = 0;
            String argument = "-";
            if (mode == 1) {
                e.intStack[e.intStackPtr++] = (Integer) item[3];
                argument = "i" + item[3];
            } else if (mode == 3) {
                e.stringStack[e.stringStackPtr++] = null;
                argument = "n";
            } else if (mode == 4) {
                e.stringStack[e.stringStackPtr++] = (String) item[3];
                argument = "s" + item[3];
            } else if (mode == 2) {
                argument = "u";
            }
            int initialInts = e.intStackPtr;
            String result = "ok";
            try {
                EnumEmailFriendOps.dispatch(op, e);
            } catch (Throwable error) {
                result = "error:" + error.getClass().getSimpleName();
            }
            out.append("T\t").append(op.name()).append('\t').append(state).append('\t').append(argument).append('\t').append(result).append('\t')
                .append(ints(e.intStack, e.intStackPtr)).append('\t').append(strings(e.stringStack, e.stringStackPtr)).append('\n');
            stats.computeIfAbsent(op.name(), k -> new int[2])[result.equals("ok") ? 0 : 1]++;
            total++;
        }
        out.append("Z\t").append(total).append('\n');
        Files.writeString(output.resolve("social-state.native.tsv"), out.toString(), StandardCharsets.UTF_8);
        var summary = new StringBuilder();
        for (var entry : stats.entrySet()) summary.append(entry.getKey()).append('\t').append(entry.getValue()[0]).append('\t').append(entry.getValue()[1]).append('\n');
        Files.writeString(output.resolve("social-state.stats.tsv"), summary.toString(), StandardCharsets.UTF_8);
        System.exit(0);
    }
}
