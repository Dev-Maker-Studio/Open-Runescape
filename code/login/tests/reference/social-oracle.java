import com.darkan.bot.Bot;
import com.jagex.game.runetek5.client.Client;
import com.jagex.game.runetek5.client.login.LoginManager;
import com.jagex.game.runetek5.client.login.LoginStateUpdate;
import com.jagex.game.runetek5.entity.EntityNode;
import com.jagex.game.runetek5.entity.EntityNodeList;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.interaction.FriendList;
import com.jagex.game.runetek5.interaction.Friend;
import com.jagex.game.runetek5.interaction.Ignore;
import com.jagex.game.runetek5.interaction.IgnoreList;
import com.jagex.game.runetek5.interaction.chat.friendschat.FriendsChat;
import com.jagex.game.runetek5.interaction.chat.friendschat.FriendsChatPlayer;
import com.jagex.game.runetek5.network.BufferedConnectionContext;
import com.jagex.game.runetek5.network.Connection;
import com.jagex.game.runetek5.network.ConnectionInfo;
import com.jagex.game.runetek5.network.packet.PacketDecoder;
import com.jagex.game.runetek5.network.packet.ServerProt;
import com.jagex.game.runetek5.utils.TextUtils;
import com.jagex.game.runetek5.utils.Utils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

class SocialOracle {
    private static final String[] POOL = {
        "Alice", "alice", "ALICE", "Bob Smith", "bob_smith", "Bob-Smith", "#Raw", "#raw",
        "Zed", "[Mod]x", "Ãndré", "Andre", "Ñandú", "Çedric", "Straße",
        "ThisNameIsTooLongToFormat", "   ", "_x_", "x y", "", "Ya\u009f", "ÿo", "a",
        "Hello!", "Mixéd", "zed", "Zed ", "über", "oòó", "12345678901",
        "123456789012", "1234567890123",
    };
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 _-#[]!àÉñßÿŸ";
    private static final int[] WORLDS = {1, 2, 5, 301, 32768, 65535};

    private final Random random = new Random(0x50c1a15L);
    private final BufferedConnectionContext context;
    private final StringBuilder out = new StringBuilder();
    private int steps;
    private int packets;
    private int errors;
    private final TreeMap<String, int[]> perPacket = new TreeMap<>();
    private int sessionTick;

    private SocialOracle(BufferedConnectionContext context) {
        this.context = context;
    }

    private static final class PayloadConnection extends Connection {
        private final byte[] payload;
        private int consumed;
        private int reads;

        private PayloadConnection(byte[] payload) {
            this.payload = payload;
        }

        @Override
        public void write(byte[] buffer, int length) {
            throw new AssertionError("Unexpected native connection write");
        }

        @Override
        public int getInstreamCapacityLeft() {
            return payload.length - consumed;
        }

        @Override
        public boolean available(int byteCount) {
            if (byteCount != payload.length || consumed != 0) {
                throw new AssertionError("Unexpected native availability check");
            }
            return true;
        }

        @Override
        public void close() {
        }

        @Override
        public void drop() {
            throw new AssertionError("Unexpected native connection drop");
        }

        @Override
        public int read(byte[] buffer, int offset, int length) {
            if (offset != 0 || length != payload.length || consumed != 0) {
                throw new AssertionError("Unexpected native connection read");
            }
            System.arraycopy(payload, 0, buffer, offset, length);
            consumed += length;
            reads++;
            return length;
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static boolean safe(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == ' ' || c == '_' || c == '-' || c == '#' || c == '.' || c == '!';
    }

    private static String enc(String value) {
        if (value == null) {
            return "~";
        }
        StringBuilder builder = new StringBuilder("'");
        for (int index = 0; index < value.length(); index++) {
            char c = value.charAt(index);
            if (safe(c)) {
                builder.append(c);
            } else {
                builder.append(String.format("%%%04x", (int) c));
            }
        }
        return builder.toString();
    }

    private static String hex(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte value : bytes) {
            builder.append(String.format("%02x", value & 255));
        }
        return builder.toString();
    }

    @SuppressWarnings("unchecked")
    private static EntityNodeList<LoginStateUpdate> queue() {
        try {
            java.lang.reflect.Method method = Client.class.getDeclaredMethod("access$getPLAYER_LOGINSTATE_UPDATES$cp");
            method.setAccessible(true);
            return (EntityNodeList<LoginStateUpdate>) method.invoke(null);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }

    private static void reset() {
        Arrays.fill(FriendList.FRIENDS, null);
        FriendList.FRIEND_COUNT = 0;
        FriendList.LOADED = 0;
        Arrays.fill(IgnoreList.IGNORED, null);
        IgnoreList.IGNORE_COUNT = 0;
        FriendsChat.FC_NAME = null;
        FriendsChat.FC_OWNER_NAME = null;
        FriendsChat.FC_PLAYER_COUNT = 0;
        FriendsChat.FC_PLAYERS = null;
        FriendsChat.FC_MIN_RANK_CAN_KICK = 0;
        FriendsChat.MY_PLAYER_FC_RANK = 0;
        Client.Companion.setLastFriendIgnoreUpdateTick(0);
        Client.Companion.setLastFriendsChatUpdateTick(0);
        EntityNodeList<LoginStateUpdate> queue = queue();
        while (queue.get() != null) {
        }
    }

    private static String snapshot() {
        StringBuilder b = new StringBuilder();
        b.append("lf=").append(Client.Companion.getLastFriendIgnoreUpdateTick());
        b.append(";lc=").append(Client.Companion.getLastFriendsChatUpdateTick());
        b.append(";fl=").append(FriendList.LOADED).append(";fn=").append(FriendList.FRIEND_COUNT).append(";F=");
        for (int index = 0; index < FriendList.FRIEND_COUNT; index++) {
            Friend f = FriendList.FRIENDS[index];
            if (index > 0) {
                b.append(',');
            }
            b.append(enc(f.displayName)).append('|').append(enc(f.previousDisplayName)).append('|').append(enc(f.worldName))
                .append('|').append(f.friendsChatRank).append('|').append(f.platform).append('|').append(f.referrer ? 1 : 0)
                .append('|').append(f.worldId).append('|').append(f.worldFlags);
        }
        b.append(";ic=").append(IgnoreList.IGNORE_COUNT).append(";I=");
        boolean first = true;
        for (int index = 0; index < IgnoreList.IGNORED.length; index++) {
            Ignore i = IgnoreList.IGNORED[index];
            if (i != null) {
                if (!first) {
                    b.append(',');
                }
                first = false;
                b.append(index).append(':').append(enc(i.unfilteredUsername)).append('|').append(enc(i.displayName)).append('|').append(i.temporary ? 1 : 0);
            }
        }
        b.append(";fcn=").append(enc(FriendsChat.FC_NAME)).append(";fco=").append(enc(FriendsChat.FC_OWNER_NAME))
            .append(";fcc=").append(FriendsChat.FC_PLAYER_COUNT).append(";fck=").append(FriendsChat.FC_MIN_RANK_CAN_KICK)
            .append(";fcr=").append(FriendsChat.MY_PLAYER_FC_RANK).append(";P=");
        if (FriendsChat.FC_PLAYERS == null) {
            b.append('~');
        } else {
            first = true;
            for (int index = 0; index < FriendsChat.FC_PLAYERS.length; index++) {
                FriendsChatPlayer p = FriendsChat.FC_PLAYERS[index];
                if (p != null) {
                    if (!first) {
                        b.append(',');
                    }
                    first = false;
                    b.append(index).append(':').append(p.rank).append('|').append(enc(p.username)).append('|').append(enc(p.displayName))
                        .append('|').append(enc(p.worldName)).append('|').append(p.worldId).append('|').append(enc(p.textName));
                }
            }
        }
        b.append(";Q=");
        EntityNodeList<LoginStateUpdate> queue = queue();
        first = true;
        for (LoginStateUpdate u = queue.getFirst(); u != null; u = queue.getNext()) {
            if (!first) {
                b.append(',');
            }
            first = false;
            b.append(enc(u.username)).append('|').append(u.actionType);
        }
        return b.toString();
    }

    private static final class Payload {
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        Payload u8(int value) {
            bytes.write(value);
            return this;
        }

        Payload u16(int value) {
            bytes.write(value >> 8);
            bytes.write(value);
            return this;
        }

        Payload i32(int value) {
            u16(value >>> 16);
            return u16(value & 0xffff);
        }

        Payload i64(long value) {
            i32((int) (value >>> 32));
            return i32((int) value);
        }

        Payload str(String value) {
            byte[] raw = value.getBytes(StandardCharsets.ISO_8859_1);
            bytes.write(raw, 0, raw.length);
            bytes.write(0);
            return this;
        }

        byte[] done() {
            return bytes.toByteArray();
        }
    }

    private void apply(ServerProt packet, byte[] payload, int world, int me) throws Exception {
        PayloadConnection connection = new PayloadConnection(payload);
        context.connection = connection;
        context.currentPacket = packet;
        context.currentPacketSize = payload.length;
        sessionTick += 1 + random.nextInt(5);
        Client.Companion.setServerTick(sessionTick);
        String result = "ok";
        try {
            boolean decoded = PacketDecoder.decode(context);
            require(decoded, "decoder returned false");
            require(context.currentPacket == null, "packet not cleared");
        } catch (NullPointerException | ArrayIndexOutOfBoundsException error) {
            result = error.getClass().getSimpleName();
            context.currentPacket = null;
            errors++;
        }
        if (payload.length > 0) {
            require(result.equals("ok") ? context.receivedBuffer.currentPosition == payload.length : true, "buffer not consumed for " + packet);
        }
        int[] counts = perPacket.computeIfAbsent(packet.name(), key -> new int[2]);
        counts[result.equals("ok") ? 0 : 1]++;
        packets++;
        String worldText = world < 0 ? "~" : Integer.toString(world);
        String meText = me == -2 ? "!" : me == -1 ? "~" : enc(POOL[me % POOL.length]);
        out.append("S\t").append(steps++).append('\t').append(packet.name()).append('\t').append(sessionTick).append('\t')
            .append(worldText).append('\t').append(meText).append('\t').append(hex(payload)).append('\t').append(result)
            .append('\t').append(snapshot()).append('\n');
    }

    private String name() {
        if (random.nextInt(10) < 7) {
            return POOL[random.nextInt(POOL.length)];
        }
        int length = 1 + random.nextInt(14);
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < length; index++) {
            builder.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return builder.toString();
    }

    private String friendName() {
        if (FriendList.FRIEND_COUNT > 0 && random.nextBoolean()) {
            return FriendList.FRIENDS[random.nextInt(FriendList.FRIEND_COUNT)].displayName;
        }
        return name();
    }

    private int worldValue(int gameWorld) {
        int pick = random.nextInt(10);
        if (pick < 3) {
            return 0;
        }
        if (pick < 5) {
            return gameWorld;
        }
        if (pick < 8) {
            return WORLDS[random.nextInt(WORLDS.length)];
        }
        return random.nextInt(65536);
    }

    private void friendEntry(Payload payload, int gameWorld) {
        boolean warn = random.nextInt(100) < 15;
        String display = friendName();
        String previous = warn ? friendName() : name();
        int world = worldValue(gameWorld);
        payload.u8(warn ? 1 : random.nextInt(8) == 0 ? 2 : 0).str(display).str(previous).u16(world).u8(random.nextInt(256)).u8(random.nextInt(2));
        if (world > 0) {
            payload.str(random.nextInt(4) == 0 ? "" : "World " + world).u8(random.nextInt(256)).i32(random.nextInt());
        }
    }

    private void friendStatus(int gameWorld, int world, int me, int entries) throws Exception {
        Payload payload = new Payload();
        for (int index = 0; index < entries; index++) {
            friendEntry(payload, gameWorld);
        }
        apply(ServerProt.FRIEND_STATUS, payload.done(), world, me);
    }

    private void updateIgnoreList(int world, int me, int count) throws Exception {
        Payload payload = new Payload().u8(count);
        for (int index = 0; index < count; index++) {
            payload.str(name()).str(name());
        }
        apply(ServerProt.UPDATE_IGNORE_LIST, payload.done(), world, me);
    }

    private void addIgnore(int world, int me) throws Exception {
        int flags = random.nextInt(4);
        String username = name();
        String display = name();
        if ((flags & 1) == 1 && IgnoreList.IGNORE_COUNT > 0 && IgnoreList.IGNORE_COUNT <= 100 && random.nextInt(4) != 0) {
            Ignore existing = IgnoreList.IGNORED[random.nextInt(IgnoreList.IGNORE_COUNT)];
            if (existing != null && existing.unfilteredUsername != null) {
                display = existing.unfilteredUsername;
            }
        }
        apply(ServerProt.ADD_IGNORE, new Payload().u8(flags).str(username).str(display).done(), world, me);
    }

    private static long encodeBase37(String text) {
        long result = 0;
        for (int index = 0; index < text.length(); index++) {
            char c = text.charAt(index);
            result *= 37L;
            if (c >= 'a' && c <= 'z') {
                result += c - 'a' + 1;
            } else if (c >= '0' && c <= '9') {
                result += c - '0' + 27;
            }
        }
        return result;
    }

    private long channelName() {
        int pick = random.nextInt(10);
        if (pick < 6) {
            int length = 1 + random.nextInt(12);
            StringBuilder builder = new StringBuilder();
            for (int index = 0; index < length; index++) {
                builder.append("abcdefghijklmnopqrstuvwxyz0123456789".charAt(random.nextInt(36)));
            }
            return encodeBase37(builder.toString());
        }
        long[] fixed = {0, -1, 37, 74, 1, 36, 6582952005840035281L, 6582952005840035280L, Long.MAX_VALUE, Long.MIN_VALUE, 123456789L};
        if (pick < 8) {
            return fixed[random.nextInt(fixed.length)];
        }
        return random.nextLong();
    }

    private void friendsChat(int world, int me, int count, boolean forceNamed) throws Exception {
        if (count < 0) {
            apply(ServerProt.FRIENDS_CHAT_CHANNEL, new byte[0], world, me);
            return;
        }
        Payload payload = new Payload().str(name());
        boolean owner = random.nextBoolean();
        payload.u8(owner ? 1 : random.nextInt(3) == 0 ? 5 : 0);
        if (owner) {
            payload.str(name());
        }
        payload.i64(channelName()).u8(random.nextInt(256)).u8(count);
        if (count != 255) {
            for (int index = 0; index < count; index++) {
                String display = forceNamed ? POOL[index % 12] : name();
                boolean changed = random.nextInt(4) == 0;
                payload.str(display).u8(changed ? 1 : random.nextInt(5) == 0 ? 3 : 0);
                if (changed) {
                    payload.str(forceNamed ? POOL[(index * 7 + 3) % 12] : name());
                }
                payload.u16(worldValue(1)).u8(random.nextInt(256)).str("W" + random.nextInt(50));
            }
        }
        apply(ServerProt.FRIENDS_CHAT_CHANNEL, payload.done(), world, me);
    }

    private void singleUser(int world, int me, int removeBias) throws Exception {
        String display;
        int worldId = worldValue(1);
        int count = FriendsChat.FC_PLAYER_COUNT;
        if (FriendsChat.FC_PLAYERS != null && count > 0 && count <= FriendsChat.FC_PLAYERS.length && random.nextBoolean()) {
            FriendsChatPlayer existing = FriendsChat.FC_PLAYERS[random.nextInt(count)];
            display = existing != null && existing.username != null ? existing.username : name();
            if (existing != null && random.nextInt(3) != 0) {
                worldId = existing.worldId;
            }
        } else {
            display = name();
        }
        boolean changed = random.nextInt(4) == 0;
        boolean removing = random.nextInt(100) < removeBias;
        int rank = removing ? -128 : random.nextInt(255) - 127;
        Payload payload = new Payload().str(random.nextBoolean() ? display : name()).u8(changed ? 1 : 0);
        if (changed) {
            payload.str(display);
        } else {
            payload = new Payload().str(display).u8(0);
        }
        payload.u16(worldId).u8(rank & 255);
        if (!removing) {
            payload.str("W" + random.nextInt(50));
        }
        apply(ServerProt.UPDATE_FRIENDCHAT_CHANNEL_SINGLEUSER, payload.done(), world, me);
    }

    private void install(int world, int me) throws Exception {
        if (world < 0) {
            LoginManager.GAME_CONNECTION_INFO = null;
        } else {
            ConnectionInfo info = new ConnectionInfo();
            info.worldId = world;
            LoginManager.GAME_CONNECTION_INFO = info;
        }
        if (me == -2) {
            PlayerUpdate.MY_PLAYER = null;
        } else {
            PlayerEntity player = (PlayerEntity) UNSAFE_ALLOCATE.invoke(UNSAFE, PlayerEntity.class);
            player.displayName = me == -1 ? null : POOL[me % POOL.length];
            PlayerUpdate.MY_PLAYER = player;
        }
    }

    private static Object UNSAFE;
    private static java.lang.reflect.Method UNSAFE_ALLOCATE;

    private void session(int kind) throws Exception {
        reset();
        int world = random.nextInt(20) == 0 ? -1 : WORLDS[random.nextInt(WORLDS.length)];
        int me = random.nextInt(20) == 0 ? -2 : random.nextInt(25) == 0 ? -1 : random.nextInt(POOL.length);
        if (kind >= 0) {
            world = 5;
            me = 0;
        }
        install(world, me);
        sessionTick = random.nextInt(1000);
        int gameWorld = world < 0 ? 5 : world;
        out.append("R\t").append(steps).append('\n');
        switch (kind) {
            case 0 -> {
                friendStatus(gameWorld, world, me, 205);
                friendStatus(gameWorld, world, me, 5);
                friendStatus(gameWorld, world, me, 40);
            }
            case 1 -> {
                updateIgnoreList(world, me, 100);
                addIgnore(world, me);
                addIgnore(world, me);
                updateIgnoreList(world, me, 101);
                addIgnore(world, me);
                updateIgnoreList(world, me, 255);
                addIgnore(world, me);
                addIgnore(world, me);
                addIgnore(world, me);
            }
            case 2 -> {
                friendsChat(world, me, 100, true);
                singleUser(world, me, 0);
                singleUser(world, me, 0);
                singleUser(world, me, 50);
                friendsChat(world, me, 101, false);
                singleUser(world, me, 50);
                singleUser(world, me, 0);
                friendsChat(world, me, 255, false);
                friendsChat(world, me, 100, false);
                friendsChat(world, me, -1, false);
                singleUser(world, me, 100);
                singleUser(world, me, 0);
                singleUser(world, me, 0);
            }
            case 3 -> {
                friendsChat(world, me, 101, false);
                singleUser(world, me, 100);
                singleUser(world, me, 100);
                singleUser(world, me, 0);
                friendsChat(world, me, 12, true);
                singleUser(world, me, 100);
                singleUser(world, me, 100);
                singleUser(world, me, 100);
            }
            default -> {
                int length = 4 + random.nextInt(12);
                for (int step = 0; step < length; step++) {
                    switch (random.nextInt(9)) {
                        case 0, 1 -> friendStatus(gameWorld, world, me, 1 + random.nextInt(6));
                        case 2 -> apply(ServerProt.FRIENDLIST_LOADED, new byte[0], world, me);
                        case 3 -> updateIgnoreList(world, me, random.nextInt(8));
                        case 4 -> addIgnore(world, me);
                        case 5 -> friendsChat(world, me, random.nextInt(6) == 0 ? -1 : random.nextInt(8), false);
                        default -> singleUser(world, me, 30);
                    }
                }
            }
        }
    }

    private static String rawDecode(byte[] raw) {
        return new String(raw, StandardCharsets.ISO_8859_1);
    }

    private void textCases() {
        List<String> inputs = new ArrayList<>(Arrays.asList(POOL));
        for (int unit = 0; unit < 0x260; unit++) {
            String single = String.valueOf((char) unit);
            inputs.add(single);
            inputs.add("a" + single + "b");
            inputs.add(single + "  ");
        }
        for (int unit : new int[] {0x2028, 0x2029, 0x3000, 0xfeff, 0xfffd, 0xffff, 0x1e9e}) {
            inputs.add("a" + (char) unit);
        }
        inputs.add("😀");
        inputs.add("ab😀cd");
        inputs.add("____");
        inputs.add("-_ x _-");
        inputs.add("abcdefghijkl");
        inputs.add("abcdefghijklm");
        inputs.add("  abcdefghijkl  ");
        inputs.add("  abcdefghijklm  ");
        for (int index = 0; index < 400; index++) {
            inputs.add(name());
        }
        for (String input : inputs) {
            out.append("T\t").append(enc(input)).append('\t').append(enc(TextUtils.formatDisplayname(input))).append('\n');
        }
        long[] longs = {0, 1, 36, 37, 38, 74, -1, Long.MIN_VALUE, Long.MAX_VALUE, 6582952005840035281L, 6582952005840035280L, 6582952005840035279L, 6582952005840035282L};
        List<Long> values = new ArrayList<>();
        for (long value : longs) {
            values.add(value);
        }
        for (int index = 0; index < 600; index++) {
            values.add(channelName());
            values.add((long) (1 + random.nextInt(2000000)));
        }
        for (long value : values) {
            out.append("N\t").append(value).append('\t').append(enc(Utils.nameForLong(value))).append('\n');
        }
    }

    public static void main(String[] args) throws Exception {
        require(args.length == 2, "Expected output directory and pinned jar");
        Path output = Path.of(args[0]);
        Path jar = Path.of(args[1]).toRealPath();
        require(Path.of(Client.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar), "Client was not loaded directly from pinned jar");
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field field = unsafeClass.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        UNSAFE = field.get(null);
        UNSAFE_ALLOCATE = unsafeClass.getMethod("allocateInstance", Class.class);
        BufferedConnectionContext context = null;
        try {
            Bot.ENABLED = false;
            Client.GAME_CONNECTION_CONTEXT.pinger.disable();
            Client.LOBBY_CONNECTION_CONTEXT.pinger.disable();
            context = new BufferedConnectionContext();
            context.pinger.disable();
            SocialOracle oracle = new SocialOracle(context);
            oracle.textCases();
            for (int kind = 0; kind < 4; kind++) {
                oracle.session(kind);
            }
            for (int index = 0; index < 300; index++) {
                oracle.session(-1);
            }
            oracle.out.append("Z\t").append(oracle.steps).append('\n');
            Files.writeString(output.resolve("social-cases.tsv"), oracle.out, StandardCharsets.US_ASCII);
            System.out.println("Decoded " + oracle.packets + " native social packets; exceptions=" + oracle.errors);
            for (var entry : oracle.perPacket.entrySet()) {
                System.out.println(entry.getKey() + " ok=" + entry.getValue()[0] + " error=" + entry.getValue()[1]);
            }
        } finally {
            if (context != null) {
                context.closeConnection();
                context.pinger.disable();
            }
            Client.GAME_CONNECTION_CONTEXT.pinger.disable();
            Client.LOBBY_CONNECTION_CONTEXT.pinger.disable();
            if (Client.Companion.getPING_REQUESTER() != null) {
                Client.Companion.getPING_REQUESTER().shutdown();
            }
        }
    }
}
