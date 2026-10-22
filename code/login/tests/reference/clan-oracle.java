import com.darkan.bot.Bot;
import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.cache.node.IntNode;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.cache.node.impl.LongNode;
import com.jagex.game.runetek5.cache.node.impl.ObjectNode;
import com.jagex.game.runetek5.client.Client;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.clanvar.ClanVarTypeList;
import com.jagex.game.runetek5.interaction.chat.clanchat.channel.ClanChannel;
import com.jagex.game.runetek5.interaction.chat.clanchat.channel.ClanChannelPlayer;
import com.jagex.game.runetek5.interaction.chat.clanchat.settings.ClanSettings;
import com.jagex.game.runetek5.network.BufferedConnectionContext;
import com.jagex.game.runetek5.network.Connection;
import com.jagex.game.runetek5.network.packet.PacketDecoder;
import com.jagex.game.runetek5.network.packet.ServerProt;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.Supplier;

class ClanOracle {
    private static final String[] POOL = {
        "Alice", "alice", "ALICE", "Bob Smith", "bob_smith", "Zed", "zed", "Ãndré", "Andre",
        "Ñandú", "Çedric", "Straße", "über", "Ÿo", "ÿo", "a", "A", "",
        "œuf", "€uro", "x y", "Mixéd", "µ", "Émile", "emile", "Zed ", "b", "B",
    };
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 _-àÉñßÿŸ";
    private static final String[] USER_PROBES = {
        "Alice", "ALICE", "alice", "ÃNDRÉ", "ÿo", "Ÿo", "straße", "STRASSE", "µ", "Μ", "Zed", "bob smith", "",
    };
    private static final String[] MEMBER_PROBES = {null, "", "Alice", "alice", "Bob Smith", "Zed", "a"};
    private static final int[][] FLAG_PROBES = {{0, 0, 31}, {0, 4, 15}, {1, 3, 7}, {2, 31, 31}, {0, 33, 40}, {-1, 0, 0}, {70000, 0, 0}};
    private static final int[] VAR_PROBE_KEYS = {0, 1, 2, 16};
    private static final int[][] VAR_BIT_PROBES = {{1, 0, 31}, {2, 4, 15}, {16, 3, 7}, {0, 33, 40}};
    private static final int[] VAR_KEYS = {0, 1, 2, 3, 4, 5, 8, 16, 17, 33, 1073741823, -1, -2147483647};
    private static final Charset CP1252 = Charset.forName("windows-1252");
    private static final ServerProt CF = ServerProt.CLANCHANNEL_FULL;
    private static final ServerProt CD = ServerProt.CLANCHANNEL_DELTA;
    private static final ServerProt SF = ServerProt.CLANSETTINGS_FULL;
    private static final ServerProt SD = ServerProt.CLANSETTINGS_DELTA;
    private static final long P = 0x0102030405060708L;

    private final Random random = new Random(0xc1a2c1a2L);
    private final BufferedConnectionContext context;
    private final StringBuilder out = new StringBuilder();
    private final Map<Integer, Object> typeLists = new HashMap<>();
    private final TreeMap<String, int[]> perPacket = new TreeMap<>();
    private int steps;
    private int packets;
    private int errors;
    private int overruns;
    private int sessionTick;
    private int varCount = -1;

    private ClanOracle(BufferedConnectionContext context) {
        this.context = context;
    }

    private static final class PayloadConnection extends Connection {
        private final byte[] payload;
        private int consumed;

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

    private static String guard(Supplier<String> body) {
        try {
            return body.get();
        } catch (RuntimeException error) {
            return "!" + error.getClass().getSimpleName();
        }
    }

    private static String ints(int[] values) {
        if (values == null) {
            return "~";
        }
        StringBuilder b = new StringBuilder();
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                b.append(' ');
            }
            b.append(values[index]);
        }
        return b.toString();
    }

    private static String longs(long[] values) {
        if (values == null) {
            return "~";
        }
        StringBuilder b = new StringBuilder().append(values.length).append(':');
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                b.append(',');
            }
            b.append(values[index]);
        }
        return b.toString();
    }

    private static String intArray(int[] values) {
        if (values == null) {
            return "~";
        }
        StringBuilder b = new StringBuilder().append(values.length).append(':');
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                b.append(',');
            }
            b.append(values[index]);
        }
        return b.toString();
    }

    private static String bytes(byte[] values) {
        if (values == null) {
            return "~";
        }
        StringBuilder b = new StringBuilder().append(values.length).append(':');
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                b.append(',');
            }
            b.append(values[index]);
        }
        return b.toString();
    }

    private static String strings(String[] values) {
        if (values == null) {
            return "~";
        }
        StringBuilder b = new StringBuilder().append(values.length).append(':');
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                b.append(',');
            }
            b.append(enc(values[index]));
        }
        return b.toString();
    }

    private static String boxed(Object value) {
        return value == null ? "~" : value.toString();
    }

    private void typeList() {
        if (varCount < 0) {
            TypeLists.CLAN_VAR = null;
            return;
        }
        Object instance = typeLists.computeIfAbsent(varCount, size -> {
            try {
                ClanVarTypeList list = (ClanVarTypeList) UNSAFE_ALLOCATE.invoke(UNSAFE, ClanVarTypeList.class);
                Field field = ClanVarTypeList.class.getField("size");
                field.setAccessible(true);
                field.setInt(list, size);
                return list;
            } catch (ReflectiveOperationException error) {
                throw new AssertionError(error);
            }
        });
        TypeLists.CLAN_VAR = (ClanVarTypeList) instance;
    }

    private static void reset() {
        Client.CLAN_CHANNEL = null;
        Client.LISTENED_CLAN_CHANNEL = null;
        Client.CLAN_SETTINGS = null;
        Client.GUEST_CLAN_SETTINGS = null;
        Client.Companion.setLastClanChannelUpdateTick(0);
        Client.Companion.setLastClanSettingsUpdateTick(0);
        ClanVarTypeList.CLAN_VARS = null;
        Arrays.fill(Client.Companion.getCLAN_VAR_KEYS(), 0);
        Client.Companion.setCLAN_VAR_COUNTER(0);
    }

    private static String channel(ClanChannel c) {
        if (c == null) {
            return "~";
        }
        StringBuilder b = new StringBuilder();
        b.append("n=").append(enc(c.clanName)).append("|k=").append(c.minRankToKick).append("|g=").append(c.guestsTalk)
            .append("|c=").append(c.numPlayers).append("|p=").append(c.pointer).append("|u=").append(c.nextUpdateNumber).append("|S=");
        if (c.players == null) {
            b.append('~');
        } else {
            b.append(c.players.length).append(':');
            for (int index = 0; index < c.players.length; index++) {
                if (index > 0) {
                    b.append(',');
                }
                ClanChannelPlayer p = c.players[index];
                if (p == null) {
                    b.append('~');
                } else {
                    b.append(enc(p.name)).append('/').append(p.rank).append('/').append(p.world);
                }
            }
        }
        b.append("|X=").append(ints(c.sortedNameIndices));
        b.append("|H=").append(guard(() -> ints(c.getPlayersSortedByName())));
        for (String probe : USER_PROBES) {
            b.append(',').append(guard(() -> Integer.toString(c.getUserSlot(probe))));
        }
        return b.toString();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String variables(IterableNodeMap map) {
        if (map == null) {
            return "~";
        }
        StringBuilder b = new StringBuilder().append(map.nodes.length).append(':');
        for (int index = 0; index < map.nodes.length; index++) {
            if (index > 0) {
                b.append(',');
            }
            Node sentinel = map.nodes[index];
            boolean first = true;
            for (Node n = sentinel.next; n != sentinel; n = n.next) {
                if (!first) {
                    b.append('+');
                }
                first = false;
                b.append(n.pointer).append('/');
                if (n instanceof IntNode) {
                    b.append("i/").append(((IntNode) n).value);
                } else if (n instanceof LongNode) {
                    b.append("l/").append(((LongNode) n).bitpacked);
                } else if (n instanceof ObjectNode) {
                    b.append("s/").append(enc((String) ((ObjectNode) n).value));
                } else {
                    b.append("?/");
                }
            }
        }
        return b.toString();
    }

    private static String settings(ClanSettings s) {
        if (s == null) {
            return "~";
        }
        StringBuilder b = new StringBuilder();
        b.append("co=").append(s.currentOwner).append("|ro=").append(s.replacementOwner).append("|bc=").append(s.banCount)
            .append("|mc=").append(s.memberCount).append("|n=").append(enc(s.clanName)).append("|ag=").append(s.allowGuests ? 1 : 0)
            .append("|t=").append(s.talkRank).append("|k=").append(s.kickRank).append("|l=").append(s.lootshareRank)
            .append("|cs=").append(s.coinShare).append("|dv=").append(s.deltaVersion).append("|uc=").append(s.updateCount)
            .append("|cd=").append(s.creationDay).append("|uh=").append(s.useHashes ? 1 : 0).append("|un=").append(s.useNames ? 1 : 0)
            .append("|BH=").append(longs(s.bannedUserHashes)).append("|BN=").append(strings(s.bannedUserNames))
            .append("|MN=").append(strings(s.memberNames)).append("|MR=").append(bytes(s.memberRanks))
            .append("|MJ=").append(intArray(s.memberJoinDays)).append("|MH=").append(longs(s.memberHashes))
            .append("|MF=").append(intArray(s.clanFlags)).append("|V=").append(variables(s.variables))
            .append("|SA=").append(ints(s.sortedAffinedSlots));
        b.append("|H=").append(guard(() -> ints(s.getSortedAffinedSlots())));
        for (String probe : MEMBER_PROBES) {
            b.append(',').append(guard(() -> Integer.toString(s.getMemberIndex(probe))));
        }
        for (int[] probe : FLAG_PROBES) {
            b.append(',').append(guard(() -> Integer.toString(s.getClanFlagBitfield(probe[0], probe[1], probe[2]))));
        }
        for (int key : VAR_PROBE_KEYS) {
            b.append(',').append(boxed(s.getIntVar(key))).append('/').append(boxed(s.getLongVar(key))).append('/').append(enc(s.getStringVar(key)));
        }
        for (int[] probe : VAR_BIT_PROBES) {
            b.append(',').append(boxed(s.getVariableBitfield(probe[0], probe[1], probe[2])));
        }
        return b.toString();
    }

    private static String clanVars() {
        Object[] values = ClanVarTypeList.CLAN_VARS;
        if (values == null) {
            return "~";
        }
        StringBuilder b = new StringBuilder().append(values.length).append(':');
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                b.append(',');
            }
            Object v = values[index];
            if (v == null) {
                b.append('~');
            } else if (v instanceof Integer) {
                b.append('i').append(v);
            } else if (v instanceof Long) {
                b.append('l').append(v);
            } else {
                b.append('s').append(enc((String) v));
            }
        }
        return b.toString();
    }

    private static String ticks() {
        return "lc=" + Client.Companion.getLastClanChannelUpdateTick() + ";ls=" + Client.Companion.getLastClanSettingsUpdateTick();
    }

    private static String snapshot() {
        return ticks() + ";CC=" + channel(Client.CLAN_CHANNEL) + ";LC=" + channel(Client.LISTENED_CLAN_CHANNEL)
            + ";CS=" + settings(Client.CLAN_SETTINGS) + ";GS=" + settings(Client.GUEST_CLAN_SETTINGS)
            + ";V=" + clanVars() + ";K=" + ints(Client.Companion.getCLAN_VAR_KEYS()) + ";N=" + Client.Companion.getCLAN_VAR_COUNTER();
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
            byte[] raw = value.getBytes(CP1252);
            bytes.write(raw, 0, raw.length);
            bytes.write(0);
            return this;
        }

        Payload nstr(String value) {
            return value == null ? u8(0) : str(value);
        }

        Payload raw(byte[] value) {
            bytes.write(value, 0, value.length);
            return this;
        }

        Payload hash(long value) {
            return value == -1 ? u8(255) : i64(value);
        }

        byte[] done() {
            return bytes.toByteArray();
        }
    }

    private boolean apply(ServerProt packet, byte[] payload) throws Exception {
        require(payload.length < 60000, "payload too large");
        Arrays.fill(context.receivedBuffer.buffer, (byte) 0);
        context.receivedBuffer.currentPosition = 0;
        context.connection = new PayloadConnection(payload);
        context.currentPacket = packet;
        context.currentPacketSize = payload.length;
        sessionTick += 1 + random.nextInt(5);
        Client.Companion.setServerTick(sessionTick);
        typeList();
        String result = "ok";
        try {
            boolean decoded = PacketDecoder.decode(context);
            require(decoded, "decoder returned false");
            require(context.currentPacket == null, "packet not cleared");
        } catch (RuntimeException error) {
            result = error.getClass().getSimpleName();
            context.currentPacket = null;
        }
        boolean overrun = context.receivedBuffer.currentPosition > payload.length;
        if (overrun) {
            result = "overrun";
            overruns++;
        } else if (!result.equals("ok")) {
            errors++;
        }
        int[] counts = perPacket.computeIfAbsent(packet.name(), key -> new int[3]);
        counts[result.equals("ok") ? 0 : overrun ? 2 : 1]++;
        packets++;
        String countText = varCount < 0 ? "~" : Integer.toString(varCount);
        out.append("S\t").append(steps++).append('\t').append(packet.name()).append('\t').append(sessionTick).append('\t')
            .append(countText).append('\t').append(hex(payload)).append('\t').append(result).append('\t')
            .append(overrun ? ticks() : snapshot()).append('\n');
        if (overrun) {
            reset();
            out.append("R\n");
        }
        return overrun;
    }

    private void start(int count) {
        reset();
        varCount = count;
        out.append("R\n");
    }

    private int pick(int... values) {
        return values[random.nextInt(values.length)];
    }

    private String name() {
        if (random.nextInt(10) < 7) {
            return POOL[random.nextInt(POOL.length)];
        }
        int length = 1 + random.nextInt(12);
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < length; index++) {
            builder.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return builder.toString();
    }

    private String longName() {
        StringBuilder builder = new StringBuilder();
        int length = 60 + random.nextInt(60);
        for (int index = 0; index < length; index++) {
            builder.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return builder.toString();
    }

    private int rank() {
        int[] common = {0, 1, 2, 3, 4, 5, 100, 101, 102, 125, 126, 127, 128, 255, 0, 0, 1, 125};
        return random.nextInt(8) == 0 ? random.nextInt(256) : common[random.nextInt(common.length)];
    }

    private long hash() {
        long value = random.nextLong() & Long.MAX_VALUE;
        return value == 0 ? 1 : value;
    }

    private long loose() {
        long value = random.nextLong();
        if ((value >>> 56) == 255) {
            value &= Long.MAX_VALUE;
        }
        return value;
    }

    private int world() {
        return pick(0, 1, 2, 5, 301, 32768, 65535, random.nextInt(65536));
    }

    private int index(int count) {
        if (count > 0 && random.nextInt(100) < 85) {
            return random.nextInt(count);
        }
        return pick(0, count, count + 1, count + 3, 65535, random.nextInt(70));
    }

    private void channelAdd(Payload p, String name, int world, int rank, boolean ff) {
        p.u8(1);
        if (ff) {
            p.u8(255);
        } else {
            p.i64(loose());
        }
        p.nstr(name).u16(world).u8(rank).i64(random.nextLong());
    }

    private void channelUpdate(Payload p, int index, int rank, int world, String name) {
        p.u8(2).u16(index).u8(rank).u16(world).i64(random.nextLong()).str(name);
    }

    private void channelKick(Payload p, int index, boolean ff) {
        p.u8(3).u16(index).u8(random.nextInt(256));
        if (ff) {
            p.u8(255);
        } else {
            p.i64(loose());
        }
    }

    private void channelRights(Payload p, String name, int guests, int kick) {
        p.u8(4).nstr(name);
        if (name != null && !name.isEmpty()) {
            p.u8(random.nextInt(256)).u8(guests).u8(kick);
        }
    }

    private Payload channelHeader(boolean nonGuest, long pointer, long update) {
        return new Payload().u8(nonGuest ? 1 : 0).i64(pointer).i64(update);
    }

    private byte[] channelFull(boolean nonGuest, int flag, long pointer, long update, int count) {
        Payload p = new Payload().u8(nonGuest ? 1 : 0).u8(flag).i64(pointer).i64(update).str(name()).u8(random.nextInt(256)).u8(rank()).u8(rank()).u16(count);
        for (int slot = 0; slot < count; slot++) {
            if ((flag & 1) != 0) {
                p.i64(random.nextLong());
            }
            p.str(name()).u8(rank()).u16(world());
        }
        return p.done();
    }

    private byte[] channelDelta(boolean nonGuest, long pointer, long update, int count, int ops) {
        Payload p = channelHeader(nonGuest, pointer, update);
        int live = count;
        for (int op = 0; op < ops; op++) {
            switch (random.nextInt(12)) {
                case 0, 1, 2, 3 -> {
                    channelAdd(p, random.nextInt(8) == 0 ? null : name(), world(), rank(), random.nextBoolean());
                    live++;
                }
                case 4, 5 -> channelUpdate(p, index(live), rank(), world(), name());
                case 6, 7, 8 -> {
                    channelKick(p, index(live), random.nextBoolean());
                    live--;
                }
                case 9, 10 -> channelRights(p, random.nextInt(6) == 0 ? null : name(), rank(), rank());
                default -> p.u8(5 + random.nextInt(250));
            }
        }
        return p.u8(0).done();
    }

    private void settingsVariable(Payload p) {
        int key = VAR_KEYS[random.nextInt(VAR_KEYS.length)] & 0x3fffffff;
        int type = random.nextInt(4);
        p.i32((type << 30) | key);
        switch (type) {
            case 0 -> p.i32(random.nextInt());
            case 1 -> p.i64(random.nextLong());
            case 2 -> p.str(random.nextInt(6) == 0 ? longName() : name());
            default -> {
            }
        }
    }

    private byte[] settingsFull(boolean nonGuest, int version, int attr, int members, int bans) {
        Payload p = new Payload().u8(nonGuest ? 1 : 0).u8(version).u8(attr).i32(pick(0, 1, 2, 3, 5, -1, random.nextInt()))
            .i32(pick(0, 0, 1, 5000, random.nextInt(100000))).u16(members).u8(bans).str(name());
        if (version >= 4) {
            p.i32(random.nextInt());
        }
        p.u8(pick(0, 1, 1, 2)).u8(rank()).u8(rank()).u8(rank()).u8(rank());
        boolean hashes = (attr & 1) != 0;
        boolean names = (attr & 2) != 0;
        for (int member = 0; member < members; member++) {
            if (hashes) {
                p.i64(hash());
            }
            if (names) {
                String text = name();
                p.str(text.isEmpty() && random.nextInt(4) != 0 ? "q" : text);
            }
            p.u8(rank());
            if (version >= 2) {
                p.i32(pick(0, 1, -1, random.nextInt(), 255, 0x7fffffff));
            }
            if (version >= 5) {
                p.u16(pick(0, 1, 65535, random.nextInt(65536)));
            }
        }
        for (int ban = 0; ban < bans; ban++) {
            if (hashes) {
                p.i64(hash());
            }
            if (names) {
                String text = name();
                p.str(text.isEmpty() && random.nextInt(4) != 0 ? "q" : text);
            }
        }
        if (version >= 3) {
            int size = pick(0, 0, 1, 2, 3, 5, 8, 17, 20);
            p.u16(size);
            for (int variable = 0; variable < size; variable++) {
                settingsVariable(p);
            }
        }
        return p.done();
    }

    private void settingsHashName(Payload p, ClanSettings s) {
        boolean hashes = s != null ? s.useHashes : random.nextBoolean();
        boolean names = s != null ? s.useNames : random.nextBoolean();
        if (random.nextInt(10) == 0) {
            hashes = !hashes;
        }
        if (random.nextInt(10) == 0) {
            names = !names;
        }
        if (hashes) {
            if (random.nextInt(12) == 0) {
                p.i64(random.nextBoolean() ? 0 : loose());
            } else {
                p.i64(hash());
            }
        } else {
            p.u8(255);
        }
        p.nstr(names ? name() : null);
    }

    private int settingsIndex(ClanSettings s, boolean bans) {
        int count = s == null ? 3 : bans ? s.banCount : s.memberCount;
        return index(Math.max(count, 0));
    }

    private byte[] settingsDelta(int flag, ClanSettings s, int ops) {
        long version = random.nextInt(12) == 0 ? random.nextLong() : 0;
        int updates = s == null ? random.nextInt(6) : s.updateCount;
        if (random.nextInt(8) == 0) {
            updates += pick(1, -1, 7);
        }
        Payload p = new Payload().u8(flag).i64(version).i32(updates);
        for (int op = 0; op < ops; op++) {
            switch (random.nextInt(14)) {
                case 0 -> {
                    p.u8(3);
                    settingsHashName(p, s);
                }
                case 1 -> {
                    p.u8(1);
                    settingsHashName(p, s);
                }
                case 2 -> {
                    p.u8(13);
                    settingsHashName(p, s);
                    p.u16(pick(0, 1, 65535, random.nextInt(65536)));
                }
                case 3 -> p.u8(4).u8(pick(0, 1, 1, 2)).u8(rank()).u8(rank()).u8(rank()).u8(rank());
                case 4 -> p.u8(6).u16(settingsIndex(s, true));
                case 5 -> p.u8(5).u16(settingsIndex(s, false));
                case 6 -> p.u8(2).u16(settingsIndex(s, false)).u8(rank());
                case 7 -> p.u8(7).u16(settingsIndex(s, false)).i32(pick(0, 1, 3, -1, random.nextInt())).u8(pick(0, 1, 4, 8, 31, 32, 33, random.nextInt(256))).u8(pick(0, 1, 3, 7, 15, 31, 32, random.nextInt(256)));
                case 8 -> p.u8(8).i32(VAR_KEYS[random.nextInt(VAR_KEYS.length)]).i32(pick(0, 1, -1, random.nextInt()));
                case 9 -> p.u8(9).i32(VAR_KEYS[random.nextInt(VAR_KEYS.length)]).i64(pick(0, 1) == 0 ? 0L : random.nextLong());
                case 10 -> p.u8(10).i32(VAR_KEYS[random.nextInt(VAR_KEYS.length)]).str(random.nextInt(4) == 0 ? longName() : name());
                case 11 -> p.u8(11).i32(VAR_KEYS[random.nextInt(VAR_KEYS.length)]).i32(random.nextInt(8) == 0 ? random.nextInt() : random.nextInt(16)).u8(pick(0, 1, 4, 8, 31, 32, 40)).u8(pick(0, 1, 3, 7, 15, 31, 32, 40));
                case 12 -> p.u8(12).str(name()).i32(random.nextInt());
                default -> p.u8(14 + random.nextInt(240));
            }
        }
        return p.u8(0).done();
    }

    private byte[] mutate(byte[] payload) {
        if (payload.length == 0) {
            return payload;
        }
        byte[] copy = payload.clone();
        switch (random.nextInt(5)) {
            case 0 -> copy[random.nextInt(copy.length)] ^= (byte) (1 + random.nextInt(255));
            case 1 -> {
                return Arrays.copyOf(copy, random.nextInt(copy.length));
            }
            case 2 -> {
                byte[] longer = Arrays.copyOf(copy, copy.length + 1 + random.nextInt(6));
                for (int index = copy.length; index < longer.length; index++) {
                    longer[index] = (byte) random.nextInt(256);
                }
                return longer;
            }
            case 3 -> copy[random.nextInt(copy.length)] = (byte) pick(0, 255, 1);
            default -> copy[random.nextInt(copy.length)] = (byte) random.nextInt(256);
        }
        return copy;
    }

    private byte[] maybeMutate(byte[] payload) {
        return random.nextInt(100) < 9 ? mutate(payload) : payload;
    }

    private ClanChannel current(boolean nonGuest) {
        return nonGuest ? Client.CLAN_CHANNEL : Client.LISTENED_CLAN_CHANNEL;
    }

    private ClanSettings currentSettings(int flag) {
        return flag == 1 ? Client.CLAN_SETTINGS : Client.GUEST_CLAN_SETTINGS;
    }

    private void channelSession() throws Exception {
        int length = 3 + random.nextInt(8);
        for (int step = 0; step < length; step++) {
            boolean nonGuest = random.nextInt(4) != 0;
            ClanChannel live = current(nonGuest);
            int roll = random.nextInt(100);
            byte[] payload;
            ServerProt packet;
            if (roll < 22 || live == null && roll < 60) {
                packet = CF;
                payload = channelFull(nonGuest, pick(0, 1, 2, 3, 0, 1, 255), pick(1, 7) == 1 ? 1L : P, pick(0, 3, 7), pick(0, 1, 2, 3, 5, 6, 7, 12));
            } else if (roll < 28) {
                packet = CF;
                payload = new byte[] {(byte) pick(0, 1, 2)};
            } else {
                packet = CD;
                long pointer = live != null && random.nextInt(10) != 0 ? live.pointer : pick(1, 2) == 1 ? 1L : P;
                long update = live != null && random.nextInt(10) != 0 ? live.nextUpdateNumber : random.nextInt(4);
                payload = channelDelta(nonGuest, pointer, update, live == null ? 3 : live.numPlayers, 1 + random.nextInt(4));
            }
            apply(packet, maybeMutate(payload));
        }
    }

    private void settingsSession() throws Exception {
        int length = 3 + random.nextInt(9);
        for (int step = 0; step < length; step++) {
            int flag = pick(1, 1, 1, 0, 0, 2);
            ClanSettings live = currentSettings(flag);
            int roll = random.nextInt(100);
            if (roll < 22 || live == null && roll < 60) {
                int version = random.nextInt(25) == 0 ? pick(0, 6, 7, 255) : 1 + random.nextInt(5);
                int attr = random.nextInt(20) == 0 ? random.nextInt(256) : random.nextInt(4);
                int members = pick(0, 1, 2, 3, 4, 5, 6, 8);
                int bans = pick(0, 0, 1, 2, 3, 5);
                apply(SF, maybeMutate(settingsFull(flag == 1, version, attr, members, bans)));
            } else if (roll < 26) {
                apply(SF, new byte[] {(byte) pick(0, 1, 2)});
            } else {
                apply(SD, maybeMutate(settingsDelta(flag, live, 1 + random.nextInt(5))));
            }
        }
    }

    private void varSession() throws Exception {
        int length = 4 + random.nextInt(14);
        for (int step = 0; step < length; step++) {
            int key = pick(0, 1, 2, 3, 4, 5, 39, 40, 99, 100, 65535, random.nextInt(120));
            switch (random.nextInt(8)) {
                case 0 -> apply(ServerProt.VARCLAN_SET_BYTE, new Payload().u16(key).u8(pick(0, 1, 127, 128, 255, random.nextInt(256))).done());
                case 1 -> apply(ServerProt.VARCLAN_SET_INT, new Payload().i32(0).done().length == 0 ? new byte[0] : new Payload().u16(key).i32(pick(0, -1, random.nextInt())).done());
                case 2 -> apply(ServerProt.VARCLAN_SET_LONG, new Payload().u16(key).i64(pick(0, 1) == 0 ? 0L : random.nextLong()).done());
                case 3 -> apply(ServerProt.SET_CLAN_STRING, new Payload().u16(key).str(name()).done());
                case 4 -> apply(ServerProt.VARCLAN_ENABLE, new byte[0]);
                case 5 -> apply(ServerProt.VARCLAN_DISABLE, new byte[0]);
                case 6 -> apply(ServerProt.VARCLAN_SET_BYTE, maybeMutate(new Payload().u16(key).u8(random.nextInt(256)).done()));
                default -> apply(ServerProt.SET_CLAN_STRING, maybeMutate(new Payload().u16(key).str(name()).done()));
            }
        }
    }

    private void randomSessions(int count) throws Exception {
        for (int index = 0; index < count; index++) {
            start(pick(-1, 0, 5, 40, 100, 100));
            switch (random.nextInt(10)) {
                case 0, 1, 2, 3 -> channelSession();
                case 4, 5, 6, 7 -> settingsSession();
                case 8 -> varSession();
                default -> {
                    channelSession();
                    settingsSession();
                    varSession();
                }
            }
        }
    }

    private void emptyChannelSessions() throws Exception {
        start(-1);
        apply(CF, channelFull(true, 0, P, 7, 0));
        for (int round = 0; round < 3; round++) {
            Payload p = channelHeader(true, P, 7);
            channelKick(p, 0, true);
            apply(CD, p.u8(0).done());
            p = channelHeader(true, P, 7);
            channelAdd(p, "Alice", 5, 1, true);
            apply(CD, p.u8(0).done());
        }
        start(-1);
        apply(CF, channelFull(true, 0, P, 7, 1));
        Payload p = channelHeader(true, P, 7);
        channelKick(p, 0, false);
        channelKick(p, 0, true);
        channelAdd(p, "Zed", 1, 2, true);
        channelAdd(p, "Alice", 2, 3, true);
        apply(CD, p.u8(0).done());
        p = channelHeader(true, P, 8);
        channelKick(p, 5, true);
        apply(CD, p.u8(0).done());
        p = channelHeader(true, P, 8);
        for (int slot = 0; slot < 14; slot++) {
            channelAdd(p, slot % 3 == 0 ? null : "N" + slot, slot, slot, true);
        }
        apply(CD, p.u8(0).done());
        start(-1);
        apply(CF, channelFull(false, 3, P, 0, 4));
        p = channelHeader(false, P, 0);
        channelUpdate(p, 2, 9, 9, "Renamed");
        channelRights(p, "Other", 5, 6);
        channelRights(p, null, 7, 8);
        apply(CD, p.u8(0).done());
        p = channelHeader(false, P, 1);
        channelUpdate(p, 7, 1, 1, "x");
        apply(CD, p.u8(0).done());
        start(-1);
        apply(CF, channelFull(true, 1, P, 0, 1800));
        apply(CF, channelFull(true, 2, P, 0, 1200));
    }

    private void settingsEdgeSessions() throws Exception {
        for (int version = 0; version <= 6; version++) {
            for (int attr = 0; attr < 4; attr++) {
                start(-1);
                apply(SF, settingsFull(true, version, attr, 3, 2));
                apply(SD, settingsDelta(1, Client.CLAN_SETTINGS, 3));
            }
        }
        start(-1);
        apply(SF, settingsFull(true, 5, 3, 0, 0));
        Payload p = new Payload().u8(1).i64(0).i32(Client.CLAN_SETTINGS == null ? 0 : Client.CLAN_SETTINGS.updateCount);
        p.u8(5).u16(0);
        apply(SD, p.u8(0).done());
        p = new Payload().u8(1).i64(0).i32(Client.CLAN_SETTINGS == null ? 0 : Client.CLAN_SETTINGS.updateCount);
        p.u8(6).u16(0).u8(6).u16(0).u8(3).i64(5).nstr("n");
        apply(SD, p.u8(0).done());
        start(-1);
        apply(SF, settingsFull(true, 5, 0, 5, 0));
        for (int round = 0; round < 7; round++) {
            p = new Payload().u8(1).i64(0).i32(Client.CLAN_SETTINGS.updateCount);
            p.u8(5).u16(0).u8(1).u8(255).nstr(null);
            apply(SD, p.u8(0).done());
        }
        start(-1);
        apply(SF, settingsFull(true, 5, 2, 1, 0));
        for (int round = 0; round < 9; round++) {
            p = new Payload().u8(1).i64(0).i32(Client.CLAN_SETTINGS.updateCount);
            p.u8(1).u8(255).nstr("M" + round).u8(13).u8(255).nstr("E" + round).u16(round * 1000);
            p.u8(2).u16(round).u8(125).u8(2).u16(0).u8(126).u8(2).u16(1).u8(127).u8(2).u16(2).u8(125);
            apply(SD, p.u8(0).done());
        }
        for (int round = 0; round < 12; round++) {
            p = new Payload().u8(1).i64(0).i32(Client.CLAN_SETTINGS.updateCount);
            p.u8(5).u16(round % 3);
            apply(SD, p.u8(0).done());
        }
        start(-1);
        apply(SF, settingsFull(true, 5, 3, 1, 0));
        for (int round = 0; round < 9; round++) {
            p = new Payload().u8(1).i64(0).i32(Client.CLAN_SETTINGS.updateCount);
            p.u8(3).i64(100 + round).nstr("B" + round);
            apply(SD, p.u8(0).done());
        }
        for (int round = 0; round < 11; round++) {
            p = new Payload().u8(1).i64(0).i32(Client.CLAN_SETTINGS.updateCount);
            p.u8(6).u16(round % 4);
            apply(SD, p.u8(0).done());
        }
        start(-1);
        apply(SF, settingsFull(true, 5, 0, 4, 0));
        int[][] rankSets = {{125, 126, 127}, {0, 125, 125}, {5, 5, 5}, {127, 126, 125}, {126, 125, 127}, {255, 128, 0}};
        for (int[] ranks : rankSets) {
            for (int slot = 0; slot < ranks.length; slot++) {
                p = new Payload().u8(1).i64(0).i32(Client.CLAN_SETTINGS.updateCount);
                p.u8(2).u16(slot).u8(ranks[slot]);
                apply(SD, p.u8(0).done());
            }
        }
        start(-1);
        apply(SF, settingsFull(true, 5, 0, 3, 0));
        p = new Payload().u8(1).i64(0).i32(Client.CLAN_SETTINGS.updateCount);
        p.u8(8).i32(4).i32(5).u8(8).i32(4).i32(5).u8(8).i32(4).i32(6).u8(9).i32(4).i64(77).u8(9).i32(4).i64(77).u8(10).i32(4).str("s");
        p.u8(10).i32(4).str("s").u8(10).i32(4).str(longName()).u8(11).i32(4).i32(7).u8(2).u8(3).u8(11).i32(4).i32(7).u8(2).u8(3);
        p.u8(11).i32(5).i32(255).u8(4).u8(31).u8(11).i32(5).i32(255).u8(31).u8(31).u8(8).i32(5).i32(1).u8(11).i32(5).i32(3).u8(1).u8(1);
        p.u8(8).i32(-1).i32(1).u8(9).i32(-2147483647).i64(-1).u8(10).i32(1073741823).str("big");
        apply(SD, p.u8(0).done());
        p = new Payload().u8(1).i64(0).i32(Client.CLAN_SETTINGS.updateCount);
        p.u8(7).u16(0).i32(5).u8(2).u8(3).u8(7).u16(1).i32(255).u8(0).u8(31).u8(7).u16(0).i32(5).u8(2).u8(3).u8(7).u16(9).i32(1).u8(0).u8(1);
        apply(SD, p.u8(0).done());
    }

    private void varEdgeSessions() throws Exception {
        start(-1);
        apply(ServerProt.VARCLAN_SET_BYTE, new Payload().u16(0).u8(1).done());
        apply(ServerProt.VARCLAN_ENABLE, new byte[0]);
        apply(ServerProt.VARCLAN_DISABLE, new byte[0]);
        start(0);
        apply(ServerProt.VARCLAN_SET_INT, new Payload().u16(0).i32(1).done());
        apply(ServerProt.VARCLAN_ENABLE, new byte[0]);
        apply(ServerProt.SET_CLAN_STRING, new Payload().u16(0).str("a").done());
        start(10);
        for (int key = 0; key < 45; key++) {
            switch (key % 4) {
                case 0 -> apply(ServerProt.VARCLAN_SET_BYTE, new Payload().u16(key % 12).u8(key * 7).done());
                case 1 -> apply(ServerProt.VARCLAN_SET_INT, new Payload().u16(key % 12).i32(key * 1000003).done());
                case 2 -> apply(ServerProt.VARCLAN_SET_LONG, new Payload().u16(key % 12).i64(key * 0x100000001L).done());
                default -> apply(ServerProt.SET_CLAN_STRING, new Payload().u16(key % 12).str("v" + key).done());
            }
        }
        apply(ServerProt.VARCLAN_DISABLE, new byte[0]);
        apply(ServerProt.VARCLAN_SET_INT, new Payload().u16(3).i32(9).done());
        apply(ServerProt.VARCLAN_ENABLE, new byte[0]);
        apply(ServerProt.VARCLAN_DISABLE, new byte[0]);
    }

    private void truncations() throws Exception {
        long ptr = 0x1122334455667788L;
        byte[] chanSetup = new Payload().u8(1).u8(1).i64(ptr).i64(7).str("Clan").u8(1).u8(5).u8(3).u16(3)
            .i64(1).str("Alice").u8(1).u16(301).i64(2).str("bob").u8(2).u16(5).i64(3).str("Zed").u8(0).u16(65535).done();
        Payload cd = channelHeader(true, ptr, 7);
        channelAdd(cd, "Carl", 9, 3, false);
        channelAdd(cd, null, 1, 1, true);
        channelUpdate(cd, 1, 4, 12, "Bobby");
        channelKick(cd, 0, false);
        channelKick(cd, 0, true);
        channelRights(cd, "NewName", 1, 2);
        channelRights(cd, null, 0, 0);
        byte[] chanDelta = cd.u8(0).done();
        cutSessions(CF, chanSetup, null);
        cutSessions(CD, chanDelta, new Object[] {CF, chanSetup});
        byte[] setupFull = settingsFixed(5, 3);
        cutSessions(SF, setupFull, null);
        cutSessions(SF, settingsFixed(2, 1), null);
        cutSessions(SF, settingsFixed(3, 2), null);
        Payload sd = new Payload().u8(1).i64(0).i32(5);
        sd.u8(3).i64(900).nstr("Banned").u8(1).i64(901).nstr("NewM").u8(13).i64(902).nstr("OldM").u16(321);
        sd.u8(4).u8(1).u8(2).u8(3).u8(4).u8(5).u8(6).u16(0).u8(2).u16(1).u8(100).u8(7).u16(0).i32(3).u8(1).u8(4);
        sd.u8(8).i32(1).i32(2).u8(9).i32(2).i64(3).u8(10).i32(3).str("str").u8(11).i32(4).i32(5).u8(2).u8(3).u8(12).str("Renamed").i32(0);
        sd.u8(5).u16(0);
        byte[] settingsDelta = sd.u8(0).done();
        cutSessions(SD, settingsDelta, new Object[] {SF, setupFull});
        cutSessions(ServerProt.SET_CLAN_STRING, new Payload().u16(3).str("hello").done(), null);
        cutSessions(ServerProt.VARCLAN_SET_LONG, new Payload().u16(3).i64(5).done(), null);
        cutSessions(ServerProt.VARCLAN_SET_INT, new Payload().u16(3).i32(5).done(), null);
        cutSessions(ServerProt.VARCLAN_SET_BYTE, new Payload().u16(3).u8(5).done(), null);
    }

    private byte[] settingsFixed(int version, int attr) {
        Payload p = new Payload().u8(1).u8(version).u8(attr).i32(5).i32(1000).u16(3).u8(2).str("Clan");
        if (version >= 4) {
            p.i32(9);
        }
        p.u8(1).u8(1).u8(2).u8(3).u8(4);
        boolean hashes = (attr & 1) != 0;
        boolean names = (attr & 2) != 0;
        String[] member = {"Alice", "Bob", "Zed"};
        for (int index = 0; index < 3; index++) {
            if (hashes) {
                p.i64(1000 + index);
            }
            if (names) {
                p.str(member[index]);
            }
            p.u8(index == 0 ? 126 : index);
            if (version >= 2) {
                p.i32(index * 3);
            }
            if (version >= 5) {
                p.u16(index * 100);
            }
        }
        for (int index = 0; index < 2; index++) {
            if (hashes) {
                p.i64(2000 + index);
            }
            if (names) {
                p.str("Ban" + index);
            }
        }
        if (version >= 3) {
            p.u16(3).i32(1).i32(77).i32((1 << 30) | 2).i64(88).i32((2 << 30) | 3).str("text");
        }
        return p.done();
    }

    private void cutSessions(ServerProt packet, byte[] payload, Object[] setup) throws Exception {
        for (int cut = 0; cut <= payload.length; cut++) {
            start(-1);
            if (setup != null) {
                apply((ServerProt) setup[0], (byte[]) setup[1]);
            }
            apply(packet, Arrays.copyOf(payload, cut));
        }
    }

    public static void main(String[] args) throws Exception {
        require(args.length == 2, "Expected output directory and pinned jar");
        Path output = Path.of(args[0]);
        Path jar = Path.of(args[1]).toRealPath();
        require(Path.of(Client.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar), "Client was not loaded directly from the jar");
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
            ClanOracle oracle = new ClanOracle(context);
            oracle.emptyChannelSessions();
            oracle.settingsEdgeSessions();
            oracle.varEdgeSessions();
            oracle.truncations();
            oracle.randomSessions(700);
            oracle.out.append("Z\t").append(oracle.steps).append('\n');
            Files.writeString(output.resolve("clan-cases.tsv"), oracle.out, java.nio.charset.StandardCharsets.US_ASCII);
            System.out.println("Decoded " + oracle.packets + " native clan packets; exceptions=" + oracle.errors + "; overruns=" + oracle.overruns);
            for (var entry : oracle.perPacket.entrySet()) {
                System.out.println(entry.getKey() + " ok=" + entry.getValue()[0] + " error=" + entry.getValue()[1] + " overrun=" + entry.getValue()[2]);
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

    private static Object UNSAFE;
    private static java.lang.reflect.Method UNSAFE_ALLOCATE;
}
