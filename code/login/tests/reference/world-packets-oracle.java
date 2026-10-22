import com.darkan.bot.Bot;
import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.client.Client;
import com.jagex.game.runetek5.encoding.Huffman;
import com.jagex.game.runetek5.encoding.HuffmanList;
import com.jagex.game.runetek5.interaction.Ignore;
import com.jagex.game.runetek5.interaction.IgnoreList;
import com.jagex.game.runetek5.interaction.chat.ChatBox;
import com.jagex.game.runetek5.interaction.chat.ChatLine;
import com.jagex.game.runetek5.interaction.chat.clanchat.channel.ClanChannel;
import com.jagex.game.runetek5.client.periodic.PulseEvent;
import com.jagex.game.runetek5.client.periodic.PulseEventHandler;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.network.BufferedConnectionContext;
import com.jagex.game.runetek5.network.Connection;
import com.jagex.game.runetek5.network.packet.PacketDecoder;
import com.jagex.game.runetek5.network.packet.ServerProt;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

class WorldPacketsOracle {
    private static final String[] FIXED = {
        "VARBIT_SMALL", "VARBIT_LARGE", "CLIENT_SETVARC_SMALL", "CLIENT_SETVARC_LARGE",
        "IF_SETMODEL", "IF_SETANGLE", "IF_SETPLAYERMODEL_OTHER", "IF_SETPLAYERHEAD_OTHER", "IF_SETNPCHEAD",
        "IF_SETPLAYERHEAD_IGNOREWORN", "IF_SETPLAYERMODEL", "IF_SETPLAYERHEAD", "IF_SETSCROLLPOS", "IF_SETCOLOR",
        "IF_SETCLICKMASK", "IF_SETPOSITION", "IF_SETHIDE", "IF_SETANIM", "IF_SETRECOL", "IF_SETRETEX",
        "IF_SETTEXTANTIMACRO", "IF_SETTEXTFONT", "IF_SETGRAPHIC", "OUTDATED_INTERFACE_PULSE_EVENT",
        "OUTDATED_SET_THEORA_STRING_SOMETHING", "MINIMAP_FLAG", "IF_SETITEM", "IF_CLOSESUB", "IF_MOVESUB",
        "IF_SETTARGETPARAM", "IF_RESETTARGETPARAM", "IF_OPENTOP", "IF_OPENSUB", "IF_OPENSUB_ACTIVE_PLAYER",
        "IF_OPENSUB_ACTIVE_NPC", "IF_OPENSUB_ACTIVE_OBJECT", "IF_OPENSUB_ACTIVE_GROUNDITEM",
        "TRIGGER_ONDIALOGABORT", "CAM_SMOOTHRESET", "CAM_RESET", "RESET_ALL_ANIMATIONS", "RESET_SOUNDS",
        "FRIENDLIST_LOADED", "PING", "SET_DRAW_ORDER", "CAM_FORCEANGLE", "CAM_LOOKAT", "CAM_MOVETO", "CAM_SHAKE",
        "SET_CAMERA_TARGET", "SET_TARGET", "REDUCE_ATTACK_PRIORITY", "SHOW_FACE_HERE", "VORBIS_SOUND",
        "VORBIS_PRELOAD_SOUND", "MUSIC_TRACK", "MUSIC_EFFECT", "SET_SONG_VOLUME", "SET_VORBIS_SOUND",
        "STOP_VORBIS_STREAM", "PRELOAD_SONG", "SOUND_SYNTH", "VORBIS_SPEECH_SOUND", "PLAY_SONG_AT_POSITION",
        "SPOT_ANIM_SPECIFIC", "REQUEST_FPS", "UPDATE_REBOOT_TIMER", "UPDATE_DOB", "JCOINS_UPDATE",
        "LOYALTY_UPDATE", "IDENTIFY_HOST_NAME", "APPLY_DEBUG", "UPDATE_UID192", "CREATE_ACCOUNT_REPLY",
        "CREATE_CHECK_EMAIL_REPLY", "DEPRECATED_52_CLIENTPACKET_97", "CHAT_FILTER_SETTINGS_PRIVATECHAT",
        "CHAT_FILTER_SETTINGS", "ANIMATE_NPC", "BLOCK_MINIMAP_STATE", "OBJ_ANIM_SPECIFIC", "PROJANIM_SPECIFIC",
        "UPDATE_ZONE_PARTIAL_FOLLOWS", "UPDATE_ZONE_FULL_FOLLOWS", "GROUND_ITEM_COUNT", "SPOT_ANIM",
        "MAP_PROJANIM", "MAP_PROJANIM_HALFSQ", "MIDI_SONG_LOCATION", "OBJ_ANIM", "GROUND_ITEM_REVEAL",
        "CREATE_GROUND_ITEM", "REMOVE_GROUND_ITEM", "CREATE_OBJECT", "DESTROY_OBJECT", "OBJECT_PREFETCH",
    };
    private static final String[] STRINGS = {
        "", "a", "Hello", "Straße über", "x y z", "iiisss", "s", "ssi", "i", "%1%2", "http://x.example/",
    };

    private final Random random = new Random(0x57a5c0deL);
    private final BufferedConnectionContext context;
    private final StringBuilder out = new StringBuilder();
    private final TreeMap<String, int[]> perPacket = new TreeMap<>();
    private int cases;

    private WorldPacketsOracle(BufferedConnectionContext context) {
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

    private static byte[] cp1252(String value) {
        byte[] raw = value.getBytes(java.nio.charset.Charset.forName("windows-1252"));
        byte[] result = Arrays.copyOf(raw, raw.length + 1);
        return result;
    }

    private static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        for (byte[] part : parts) {
            stream.writeBytes(part);
        }
        return stream.toByteArray();
    }

    private byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        int mode = random.nextInt(6);
        for (int index = 0; index < length; index++) {
            switch (mode) {
                case 0 -> bytes[index] = 0;
                case 1 -> bytes[index] = (byte) 0xff;
                case 2 -> bytes[index] = (byte) (random.nextBoolean() ? 0x80 : 0x7f);
                default -> bytes[index] = (byte) random.nextInt(256);
            }
        }
        return bytes;
    }

    private void drain() {
        PulseEventHandler.clearAllPulseEvents();
    }

    private static PulseEvent popNext() {
        PulseEvent event = (PulseEvent) PulseEventHandler.HIGH_PRIORITY.getNext();
        if (event == null) {
            event = (PulseEvent) PulseEventHandler.LOW_PRIORITY.getNext();
        }
        if (event != null) {
            event.unlink();
            event.unlinkSub();
        }
        return event;
    }

    private String pulses() {
        List<String> events = new ArrayList<>();
        PulseEvent event;
        while ((event = popNext()) != null) {
            events.add(event.getType() + "," + event.getValue() + "," + event.primary + "," + event.secondary + "," + event.tertiary + "," + enc(event.string));
        }
        return events.isEmpty() ? "-" : String.join(";", events);
    }

    private String lastStatus;
    private int lastConsumed;
    private int lastPlayerIndex;
    private int forcedPlayerIndex = -1;

    private void execute(ServerProt packet, byte[] payload) {
        require(payload.length < 60000, "payload too large");
        PulseEventHandler.clearAllPulseEvents();
        Client.Companion.setVarVerifyId(0);
        int playerIndex = random.nextInt(2048);
        if (forcedPlayerIndex >= 0) {
            playerIndex = forcedPlayerIndex;
        }
        PlayerUpdate.myPlayerIndex = playerIndex;
        Arrays.fill(context.receivedBuffer.buffer, (byte) 0);
        context.receivedBuffer.currentPosition = 0;
        context.connection = new PayloadConnection(payload);
        context.currentPacket = packet;
        context.currentPacketSize = payload.length;
        String status = "ok";
        try {
            boolean decoded = PacketDecoder.decode(context);
            require(decoded || packet == ServerProt.PING || packet == ServerProt.CUTSCENE, "decoder returned false for " + packet.name() + " size " + payload.length);
            require(context.currentPacket == null, "packet not cleared");
        } catch (java.io.IOException error) {
            throw new AssertionError("Unexpected native IO", error);
        } catch (RuntimeException error) {
            status = "x" + error.getClass().getSimpleName();
            context.currentPacket = null;
        }
        int consumed = context.receivedBuffer.currentPosition;
        if (consumed > payload.length) {
            status = "overrun";
        }
        lastStatus = status;
        lastConsumed = consumed;
        lastPlayerIndex = playerIndex;
    }

    private void apply(ServerProt packet, byte[] payload) {
        execute(packet, payload);
        String status = lastStatus;
        int consumed = lastConsumed;
        int playerIndex = lastPlayerIndex;
        int[] counts = perPacket.computeIfAbsent(packet.name(), key -> new int[3]);
        counts[status.equals("ok") ? 0 : status.equals("overrun") ? 2 : 1]++;
        out.append("C\t").append(packet.name()).append('\t').append(hex(payload)).append('\t').append(status).append('\t')
            .append(consumed).append('\t').append(playerIndex).append('\t').append(Client.Companion.getVarVerifyId()).append('\t')
            .append(pulses()).append('\n');
        cases++;
        drain();
    }

    private void fixedCases(ServerProt packet) {
        int size = packet.getSize();
        for (int round = 0; round < 24; round++) {
            apply(packet, randomBytes(size));
        }
        byte[] sample = randomBytes(size);
        for (int cut = 0; cut < size; cut++) {
            apply(packet, Arrays.copyOf(sample, cut));
        }
        apply(packet, concat(sample, new byte[] {7}));
        apply(packet, concat(sample, randomBytes(3)));
    }

    private void stringCases(ServerProt packet, byte[][] shapes) {
        for (byte[] shape : shapes) {
            apply(packet, shape);
            apply(packet, concat(shape, new byte[] {1}));
            for (int cut = 0; cut < shape.length; cut++) {
                apply(packet, Arrays.copyOf(shape, cut));
            }
        }
    }

    private byte[] s(String value) {
        return cp1252(value);
    }

    private byte[] r(int length) {
        return randomBytes(length);
    }

    private void variableCases() {
        for (String text : STRINGS) {
            byte[] str = s(text);
            stringCases(ServerProt.PROCESS_DEV_CONSOLE_COMMAND, new byte[][] {str});
            stringCases(ServerProt.WINDOW_CALL_WITH_ARGS, new byte[][] {str});
            stringCases(ServerProt.OPEN_SOCIAL_NETWORK_URL, new byte[][] {str});
            stringCases(ServerProt.CLIENT_SETVARCSTR_SMALL, new byte[][] {concat(str, r(2))});
            stringCases(ServerProt.CLIENT_SETVARCSTR_LARGE, new byte[][] {concat(r(2), str)});
            stringCases(ServerProt.IF_SETTEXT, new byte[][] {concat(str, r(4))});
            stringCases(ServerProt.PLAYER_OPTION, new byte[][] {concat(str, r(1), r(2), r(1))});
            stringCases(ServerProt.OPEN_URL, new byte[][] {concat(new byte[] {0}, str), concat(new byte[] {1}, str, s("fb"))});
            stringCases(ServerProt.QUICK_HOP_WORLDS, new byte[][] {concat(r(2), str, r(1))});
            stringCases(ServerProt.GAME_MESSAGE, new byte[][] {
                concat(new byte[] {5}, r(4), new byte[] {0}, str),
                concat(new byte[] {5}, r(4), new byte[] {1}, str, s("m")),
                concat(new byte[] {5}, r(4), new byte[] {3}, str, s("nf"), s("m")),
                concat(new byte[] {(byte) 0x80, 0x10}, r(4), new byte[] {2}, str),
            });
            stringCases(ServerProt.RUN_CS2_SCRIPT, new byte[][] {
                concat(s(""), r(4)),
                concat(s("i"), r(4), r(4)),
                concat(s("s"), s("z"), r(4)),
                concat(s("sis"), s("q"), r(4), s("w"), r(4)),
                concat(s("iss"), s("q"), s("w"), r(4), r(4)),
            });
        }
        for (int size : new int[] {0, 1, 2, 3, 4, 5, 8}) {
            apply(ServerProt.SET_CURSOR, r(size));
        }
        apply(ServerProt.SET_CURSOR, concat(s("hi"), r(2)));
        apply(ServerProt.SET_CURSOR, concat(s(""), new byte[] {(byte) 0xff, (byte) 0xff}));
        apply(ServerProt.SET_CURSOR, concat(s("hi"), r(2), r(1)));
    }

    private static final String[] CHAT_USERS = {"Zezima", "Bad Guy", "Straße", "x"};
    private static final String[] CHAT_DISPLAYS = {"zezima", "Ignored Dude", "ÄÖ", "Bad Guy"};
    private static final String[] CHAT_MESSAGES = {"", "hi", "Hello World", "a<b>c", "TEST 123 !", "€ ‚ Straße", "HELLO 12<>"};
    private static final int[][] CHAT_HASHES = {{0, 1}, {1, 2}, {0, 0}, {0x1234, 0x56789a}, {0xffff, 0xffffff}};
    private static final long[] CHAT_SEED = {1L, (1L << 32) + 2};
    private static final byte[][] TABLES = new byte[4][];
    private static final Huffman[] HUFFMANS = new Huffman[4];

    private static class ChatState {
        int table;
        int flags;
        long[] seed;
        int seedIndex;
        String[][] ignore = new String[0][];
        String member;
        String guest;
    }

    private static void buildTables() {
        byte[] one = new byte[256];
        Arrays.fill(one, (byte) 8);
        byte[] two = new byte[256];
        for (int index = 0; index < 256; index++) {
            two[index] = (byte) (index >= 32 && index < 96 ? 7 : index < 192 ? 8 : 0);
        }
        byte[] three = new byte[256];
        for (int index = 0; index < 100; index++) {
            three[index] = 8;
        }
        TABLES[1] = one;
        TABLES[2] = two;
        TABLES[3] = three;
        for (int id = 1; id <= 3; id++) {
            HUFFMANS[id] = new Huffman(TABLES[id]);
        }
    }

    private static byte[] u8(int value) {
        return new byte[] {(byte) value};
    }

    private static byte[] u16(int value) {
        return new byte[] {(byte) (value >> 8), (byte) value};
    }

    private static byte[] u24(int value) {
        return new byte[] {(byte) (value >> 16), (byte) (value >> 8), (byte) value};
    }

    private static byte[] i64(long value) {
        byte[] bytes = new byte[8];
        for (int index = 0; index < 8; index++) {
            bytes[index] = (byte) (value >> (56 - 8 * index));
        }
        return bytes;
    }

    private static byte[] usmart(int value) {
        return value < 128 ? u8(value) : u16(value + 32768);
    }

    private static long base37(String name) {
        String alphabet = "_abcdefghijklmnopqrstuvwxyz0123456789";
        long value = 0;
        for (char c : name.toLowerCase().toCharArray()) {
            value = value * 37 + alphabet.indexOf(c);
        }
        return value;
    }

    private static byte[] encodeMessage(int table, String text) {
        try {
            byte[] raw = text.getBytes(java.nio.charset.Charset.forName("windows-1252"));
            byte[] out = new byte[raw.length * 2 + 16];
            int length = HUFFMANS[table].decompress(raw, 0, raw.length, out, 0);
            return concat(usmart(raw.length), Arrays.copyOf(out, length));
        } catch (RuntimeException error) {
            return null;
        }
    }

    private byte[] messageTail(int table) {
        byte[] encoded = null;
        if (random.nextInt(10) < 7) {
            encoded = encodeMessage(table, CHAT_MESSAGES[random.nextInt(CHAT_MESSAGES.length)]);
        }
        if (encoded != null) {
            return encoded;
        }
        int[] sizes = {0, 1, 5, 200, 32767, 40000};
        return concat(usmart(Math.min(sizes[random.nextInt(sizes.length)], 32767)), randomBytes(random.nextInt(20)));
    }

    private byte[] chatShape(ServerProt packet, ChatState state) {
        String user = CHAT_USERS[random.nextInt(CHAT_USERS.length)];
        String display = CHAT_DISPLAYS[random.nextInt(CHAT_DISPLAYS.length)];
        int displayFlag = random.nextInt(3);
        int[] hash = CHAT_HASHES[random.nextInt(CHAT_HASHES.length)];
        int rights = random.nextInt(5);
        byte[] tail = messageTail(state.table);
        byte[] tailed = concat(u16(hash[0]), u24(hash[1]), u8(rights), tail);
        long[] owners = {0L, 1L, 37L, 1234567890123L, 6582952005840035280L, 6582952005840035281L, -5L, base37("zezima"), base37("dark_lord"), base37("a_b_c")};
        if (packet == ServerProt.MESSAGE_PRIVATE_ECHO) {
            return concat(s(user), tail);
        } else if (packet == ServerProt.MESSAGE_CLANCHANNEL) {
            return concat(u8(random.nextInt(3)), s(user), tailed);
        }
        byte[] head = concat(u8(displayFlag), s(user), displayFlag == 1 ? s(display) : new byte[0]);
        if (packet == ServerProt.MESSAGE_FRIENDS_CHAT) {
            return concat(head, i64(owners[random.nextInt(owners.length)]), tailed);
        }
        return concat(head, tailed);
    }

    private void chatEnvironment(ChatState state) {
        HuffmanList.initialize(HUFFMANS[state.table]);
        Client.USERDETAIL_QUICKCHAT = (state.flags & 4) != 0;
        Client.VERIFIED_EMAIL_ADDRESS = (state.flags & 2) != 0;
        Client.IS_QUICKCHAT_ONLY = (state.flags & 1) != 0;
        Client.CLAN_CHANNEL = state.member == null ? null : clanChannel(state.member);
        Client.LISTENED_CLAN_CHANNEL = state.guest == null ? null : clanChannel(state.guest);
        for (int index = 0; index < 100; index++) {
            IgnoreList.IGNORED[index] = null;
        }
        for (int index = 0; index < state.ignore.length; index++) {
            Ignore ignore = new Ignore();
            ignore.unfilteredUsername = state.ignore[index][0];
            ignore.displayName = state.ignore[index][1];
            IgnoreList.IGNORED[index] = ignore;
        }
        IgnoreList.IGNORE_COUNT = state.ignore.length;
        long[] hashes = Client.Companion.getCHAT_MESSAGE_UNIQUE_HASHES();
        Arrays.fill(hashes, 0L);
        if (state.seed != null) {
            System.arraycopy(state.seed, 0, hashes, 0, state.seed.length);
        }
        Client.Companion.setChatMessageHashIndex(state.seedIndex);
        ChatBox.reset();
    }

    private void chatApply(ServerProt packet, ChatState state, byte[] payload) {
        chatEnvironment(state);
        long[] hashes = Client.Companion.getCHAT_MESSAGE_UNIQUE_HASHES();
        execute(packet, payload);
        String result = "-";
        if (ChatBox.NUM_CHAT_LINES > 0) {
            ChatLine line = ChatBox.CHAT_LINES[0];
            result = line.type.name() + "," + line.effectFlags + "," + enc(line.crownedName) + "," + enc(line.name) + "," + enc(line.simpleName) + "," + enc(line.clan) + "," + line.quickchatMessageId + "," + enc(line.message);
        }
        StringBuilder ignoreText = new StringBuilder();
        for (String[] entry : state.ignore) {
            ignoreText.append(ignoreText.length() == 0 ? "" : ",").append(enc(entry[0])).append('|').append(enc(entry[1]));
        }
        String seedText = state.seed == null ? "-" : Arrays.stream(state.seed).mapToObj(Long::toString).reduce((a, b) -> a + "," + b).get();
        int[] counts = perPacket.computeIfAbsent(packet.name(), key -> new int[3]);
        counts[lastStatus.equals("ok") ? 0 : lastStatus.equals("overrun") ? 2 : 1]++;
        out.append("M\t").append(packet.name()).append('\t').append(hex(payload)).append('\t').append(lastStatus).append('\t')
            .append(lastConsumed).append('\t').append(state.table).append('\t').append(state.flags).append('\t')
            .append(seedText).append('\t').append(state.seedIndex).append('\t')
            .append(ignoreText.length() == 0 ? "-" : ignoreText).append('\t')
            .append(enc(state.member)).append('\t').append(enc(state.guest)).append('\t').append(result).append('\t')
            .append(Client.Companion.getChatMessageHashIndex()).append('\t').append(hashes[state.seedIndex]).append('\n');
        cases++;
        drain();
    }

    private static ClanChannel clanChannel(String name) {
        byte[] bytes = concat(u8(3), i64(7), i64(9), cp1252(name), u8(0), u8(0), u8(0), u16(0));
        return new ClanChannel(new Packet(bytes));
    }

    private ChatState[] chatStates() {
        ChatState[] states = new ChatState[6];
        for (int index = 0; index < states.length; index++) {
            states[index] = new ChatState();
        }
        states[0].table = 1;
        states[0].member = "Clan A";
        states[0].guest = "Guest B";
        states[1].table = 2;
        states[1].ignore = new String[][] {{"Bad Guy", "Bad Guy"}};
        states[1].guest = "G";
        states[2].table = 3;
        states[2].flags = 4;
        states[2].member = "M";
        states[3].table = 1;
        states[3].flags = 6;
        states[3].ignore = new String[][] {{"zezima", null}, {null, "Ignored Dude"}};
        states[3].member = "";
        states[3].guest = "";
        states[4].table = 2;
        states[4].flags = 1;
        states[4].seed = CHAT_SEED;
        states[4].seedIndex = 98;
        states[4].member = "Seeded";
        states[4].guest = "Seeded";
        states[5].table = 1;
        states[5].ignore = new String[][] {{"STRASSE", "äö"}, {"Straße", "ÄÖ"}};
        states[5].seed = CHAT_SEED;
        states[5].seedIndex = 99;
        states[5].member = "Wrap";
        states[5].guest = "Wrap";
        return states;
    }

    private void chatCases() {
        ServerProt[] packets = {ServerProt.MESSAGE_PRIVATE, ServerProt.MESSAGE_PRIVATE_ECHO, ServerProt.MESSAGE_CLANCHANNEL, ServerProt.MESSAGE_FRIENDS_CHAT};
        ChatState[] states = chatStates();
        for (ServerProt packet : packets) {
            for (int stateIndex = 0; stateIndex < states.length; stateIndex++) {
                ChatState state = states[stateIndex];
                for (int round = 0; round < 60; round++) {
                    byte[] shape = chatShape(packet, state);
                    chatApply(packet, state, shape);
                    chatApply(packet, state, concat(shape, new byte[] {1}));
                    if (round < 8 && stateIndex % 2 == 0) {
                        for (int cut = 0; cut < shape.length; cut++) {
                            chatApply(packet, state, Arrays.copyOf(shape, cut));
                        }
                    }
                }
            }
        }
    }

    private static Object UNSAFE;
    private static java.lang.reflect.Method UNSAFE_ALLOCATE;

    private static Object alloc(Class<?> type) {
        try {
            return UNSAFE_ALLOCATE.invoke(UNSAFE, type);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }

    private static void setField(Object target, String name, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }

    private static final class StubWorker extends com.jagex.game.runetek5.js5.request.FileWorker {
        @Override
        public com.jagex.game.runetek5.js5.Js5MasterIndex getMasterIndex() {
            return null;
        }

        @Override
        public byte[] requestArchive(int archiveId) {
            throw new AssertionError("Unexpected archive request");
        }

        @Override
        public void prefetch(int fileId) {
            throw new AssertionError("Unexpected prefetch");
        }

        @Override
        public int getProgress(int fileId) {
            return 0;
        }
    }

    private static final class SceneState extends ChatState {
        int myIndex;
        int baseX;
        int baseY;
        int sprites;
        int duration = 3;
        String[][] players = new String[0][];
    }

    private static final com.jagex.game.runetek5.graphics.hint.HintArrow[] SENTINEL_ARROWS = new com.jagex.game.runetek5.graphics.hint.HintArrow[8];
    private static final com.jagex.game.runetek5.graphics.hint.HintTrail[] SENTINEL_TRAILS = new com.jagex.game.runetek5.graphics.hint.HintTrail[9];

    private static void sceneStubs() {
        com.jagex.game.runetek5.config.defaults.EntityDefaults defaults = (com.jagex.game.runetek5.config.defaults.EntityDefaults) alloc(com.jagex.game.runetek5.config.defaults.EntityDefaults.class);
        Client.Companion.setENTITY_DEFAULTS(defaults);
        com.jagex.game.runetek5.config.Resource.MODEL = new com.jagex.game.runetek5.js5.Js5ResourceProvider(new StubWorker(), false, 0);
        com.jagex.game.runetek5.config.Resource.CUTSCENE = new com.jagex.game.runetek5.js5.Js5ResourceProvider(new StubWorker(), false, 0);
        com.jagex.game.runetek5.rendering.Renderers.activeToolkit = (com.jagex.game.runetek5.toolkit.AbstractToolkit) alloc(com.jagex.game.runetek5.toolkit.SafeModeToolkit.class);
        com.jagex.game.runetek5.scene.MapRegion region = (com.jagex.game.runetek5.scene.MapRegion) alloc(com.jagex.game.runetek5.scene.MapRegion.class);
        region.scene = (com.jagex.game.runetek5.scene.Scene) alloc(com.jagex.game.runetek5.scene.Scene.class);
        setField(region, "tileSettingsBits", new com.jagex.game.runetek5.scene.SettingsBits(0, 0));
        com.jagex.game.runetek5.config.Resource.mapRegion = region;
        for (int index = 0; index < SENTINEL_ARROWS.length; index++) {
            SENTINEL_ARROWS[index] = new com.jagex.game.runetek5.graphics.hint.HintArrow();
        }
        for (int index = 0; index < SENTINEL_TRAILS.length; index++) {
            SENTINEL_TRAILS[index] = (com.jagex.game.runetek5.graphics.hint.HintTrail) alloc(com.jagex.game.runetek5.graphics.hint.HintTrail.class);
        }
    }

    private static com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity[] scenePlayers(SceneState state) {
        com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity[] created = new com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity[state.players.length];
        Arrays.fill(PlayerUpdate.players, null);
        PlayerUpdate.MY_PLAYER = null;
        for (int index = 0; index < state.players.length; index++) {
            String[] spec = state.players[index];
            com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity player = (com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity) alloc(com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity.class);
            player.username = spec[1];
            player.displayName = spec[2];
            player.playerAppearance = spec[3].equals("1") ? (com.jagex.game.runetek5.entity.pathingentity.player.PlayerAppearance) alloc(com.jagex.game.runetek5.entity.pathingentity.player.PlayerAppearance.class) : null;
            player.prefixTitle = spec[4];
            player.postfixTitle = spec[5];
            int slot = Integer.parseInt(spec[0]);
            PlayerUpdate.players[slot] = player;
            if (slot == state.myIndex) {
                PlayerUpdate.MY_PLAYER = player;
            }
            created[index] = player;
        }
        return created;
    }

    private String playersText(SceneState state) {
        StringBuilder text = new StringBuilder();
        for (String[] spec : state.players) {
            text.append(text.length() == 0 ? "" : ",").append(spec[0]).append('|').append(enc(spec[1])).append('|').append(enc(spec[2])).append('|').append(spec[3]).append('|').append(enc(spec[4])).append('|').append(enc(spec[5]));
        }
        return text.length() == 0 ? "-" : text.toString();
    }

    private static String arrowText() {
        StringBuilder text = new StringBuilder();
        for (int slot = 0; slot < 8; slot++) {
            com.jagex.game.runetek5.graphics.hint.HintArrow arrow = PlayerUpdate.HINT_ARROWS[slot];
            if (arrow == SENTINEL_ARROWS[slot]) {
                continue;
            }
            text.append(text.length() == 0 ? "" : ";").append(slot).append(':');
            if (arrow == null) {
                text.append("null");
            } else {
                text.append(arrow.targetType).append(',').append(arrow.arrowSprite).append(',').append(arrow.x).append(',').append(arrow.y).append(',').append(arrow.plane).append(',').append(arrow.height).append(',').append(arrow.distance).append(',').append(arrow.targetIndex).append(',').append(arrow.denominator).append(',').append(arrow.modelId);
            }
        }
        return text.length() == 0 ? "-" : text.toString();
    }

    private static String trailText() {
        StringBuilder text = new StringBuilder();
        for (int slot = 0; slot < 9; slot++) {
            com.jagex.game.runetek5.graphics.hint.HintTrail trail = Client.Companion.getHINT_TRAILS()[slot];
            if (trail == SENTINEL_TRAILS[slot]) {
                continue;
            }
            text.append(text.length() == 0 ? "" : ";").append(slot).append(':');
            if (trail == null) {
                text.append("null");
            } else {
                text.append(trail.modelId).append(',').append(trail.length);
                for (int index = 0; index < trail.length; index++) {
                    text.append(',').append(trail.placementX[index]).append(':').append(trail.placementY[index]);
                }
            }
        }
        return text.length() == 0 ? "-" : text.toString();
    }

    private static String cutsceneText() {
        int[][] xteas = com.jagex.game.runetek5.scene.MapRegion.CUTSCENE_MAP_XTEAS;
        Packet buffer = com.jagex.game.runetek5.config.cutscene.CutsceneTypeList.CUTSCENE_BUFFER;
        if (xteas == null || buffer == null) {
            return "-";
        }
        StringBuilder text = new StringBuilder().append(Client.Companion.getCURRENT_CUTSCENE()).append('|');
        for (int[] xtea : xteas) {
            text.append(xtea[0]).append(',').append(xtea[1]).append(',').append(xtea[2]).append(',').append(xtea[3]).append(';');
        }
        text.append('|').append(hex(Arrays.copyOf(buffer.buffer, buffer.currentPosition)));
        return text.toString();
    }

    private void sceneApply(ServerProt packet, SceneState state, byte[] payload) {
        chatEnvironment(state);
        com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity[] created = scenePlayers(state);
        com.jagex.game.runetek5.graphics.sprite.StaticMedia.hintHeadiconSprites = new com.jagex.game.runetek5.graphics.sprite.NativeSprite[state.sprites];
        com.jagex.game.runetek5.config.defaults.EntityDefaults defaults = Client.Companion.getENTITY_DEFAULTS();
        defaults.playerMessageDuration = state.duration;
        com.jagex.game.runetek5.scene.MapRegion region = com.jagex.game.runetek5.config.Resource.mapRegion;
        region.baseCoordinates = new com.jagex.game.runetek5.movement.CoordGrid(0, state.baseX, state.baseY);
        System.arraycopy(SENTINEL_ARROWS, 0, PlayerUpdate.HINT_ARROWS, 0, 8);
        System.arraycopy(SENTINEL_TRAILS, 0, Client.Companion.getHINT_TRAILS(), 0, 9);
        com.jagex.game.runetek5.scene.MapRegion.CUTSCENE_MAP_XTEAS = null;
        com.jagex.game.runetek5.config.cutscene.CutsceneTypeList.CUTSCENE_BUFFER = null;
        forcedPlayerIndex = state.myIndex;
        try {
            execute(packet, payload);
        } finally {
            forcedPlayerIndex = -1;
        }
        String result = "-";
        if (packet == ServerProt.MESSAGE_PUBLIC) {
            StringBuilder text = new StringBuilder();
            for (int index = 0; index < created.length; index++) {
                com.jagex.game.runetek5.graphics.hint.HintDetails hint = created[index].hintDetails;
                if (hint != null && hint.text != null) {
                    text.append(text.length() == 0 ? "" : ";").append(state.players[index][0]).append(':').append(enc(hint.text)).append(',').append(hint.colorType).append(',').append(hint.drawType).append(',').append(hint.ticksLeft).append(',').append(hint.ticksVisible);
                }
            }
            String line = "-";
            if (ChatBox.NUM_CHAT_LINES > 0) {
                ChatLine chat = ChatBox.CHAT_LINES[0];
                line = chat.type.name() + "," + chat.effectFlags + "," + enc(chat.crownedName) + "," + enc(chat.name) + "," + enc(chat.simpleName) + "," + enc(chat.clan) + "," + chat.quickchatMessageId + "," + enc(chat.message);
            }
            result = (text.length() == 0 ? "-" : text) + "|" + line;
        } else if (packet == ServerProt.HINT_ARROW) {
            result = arrowText();
        } else if (packet == ServerProt.HINT_TRAIL) {
            result = trailText();
        } else if (packet == ServerProt.CUTSCENE) {
            result = cutsceneText();
        }
        StringBuilder ignoreText = new StringBuilder();
        for (String[] entry : state.ignore) {
            ignoreText.append(ignoreText.length() == 0 ? "" : ",").append(enc(entry[0])).append('|').append(enc(entry[1]));
        }
        int[] counts = perPacket.computeIfAbsent(packet.name(), key -> new int[3]);
        counts[lastStatus.equals("ok") ? 0 : lastStatus.equals("overrun") ? 2 : 1]++;
        out.append("S\t").append(packet.name()).append('\t').append(hex(payload)).append('\t').append(lastStatus).append('\t')
            .append(lastConsumed).append('\t').append(state.table).append('\t').append(state.flags).append('\t')
            .append(ignoreText.length() == 0 ? "-" : ignoreText).append('\t').append(state.myIndex).append('\t')
            .append(state.baseX).append('\t').append(state.baseY).append('\t').append(state.sprites).append('\t')
            .append(state.duration).append('\t').append(com.jagex.game.runetek5.client.GameShell.gameCyclesPerSecond()).append('\t')
            .append(playersText(state)).append('\t').append(result).append('\n');
        cases++;
        drain();
    }

    private static String[] spec(int index, String user, String display, boolean appearance, String prefix, String postfix) {
        return new String[] {Integer.toString(index), user, display, appearance ? "1" : "0", prefix, postfix};
    }

    private SceneState[] sceneStates() {
        SceneState[] states = new SceneState[5];
        for (int index = 0; index < states.length; index++) {
            states[index] = new SceneState();
        }
        states[0].table = 1;
        states[0].myIndex = 7;
        states[0].baseX = 3200;
        states[0].baseY = 3200;
        states[0].sprites = 8;
        states[0].players = new String[][] {spec(7, "Zezima", "zezima", true, null, null), spec(100, "Bad Guy", "Ignored Dude", true, "Sir ", " the Great"), spec(2047, "x", "x", true, "", ""), spec(5, "Straße", null, true, null, null), spec(6, "NoLook", "nolook", false, null, null)};
        states[1].table = 2;
        states[1].myIndex = 100;
        states[1].baseX = 0;
        states[1].baseY = 65535;
        states[1].sprites = 3;
        states[1].duration = 5;
        states[1].ignore = new String[][] {{"Bad Guy", "Bad Guy"}, {"zezima", "zezima"}};
        states[1].players = new String[][] {spec(100, "Bad Guy", "Bad Guy", true, "<", ">"), spec(7, "Zezima", "zezima", true, "a<b>", "c"), spec(0, "Zero", "ÄÖ", true, null, "!")};
        states[2].table = 3;
        states[2].flags = 4;
        states[2].myIndex = 0;
        states[2].baseX = 1234;
        states[2].baseY = 4321;
        states[2].sprites = 1;
        states[2].players = new String[][] {spec(0, "Me", "me", true, null, null), spec(1, "Other", "other", true, null, null)};
        states[3].table = 1;
        states[3].flags = 6;
        states[3].myIndex = 1;
        states[3].baseX = 64;
        states[3].baseY = 64;
        states[3].sprites = 255;
        states[3].ignore = new String[][] {{"zezima", null}, {null, "Ignored Dude"}};
        states[3].players = new String[][] {spec(1, "Me", "me", true, "P ", " S"), spec(100, "Bad Guy", "Ignored Dude", true, null, null), spec(7, "Zezima", "zezima", true, null, null)};
        states[4].table = 2;
        states[4].flags = 1;
        states[4].myIndex = 2000;
        states[4].baseX = 9000;
        states[4].baseY = 0;
        states[4].sprites = 0;
        states[4].players = new String[][] {spec(2000, "Me", "me", true, null, null), spec(7, "Zezima", "zezima", true, null, null)};
        return states;
    }

    private void sceneCases() {
        sceneStubs();
        SceneState[] states = sceneStates();
        for (SceneState state : states) {
            for (int round = 0; round < 120; round++) {
                int pick = random.nextInt(state.players.length + 2);
                int idx = pick < state.players.length ? Integer.parseInt(state.players[pick][0]) : pick == state.players.length ? state.myIndex : 1500 + random.nextInt(500);
                if (idx > 2047) {
                    idx = 2046;
                }
                int effects = random.nextInt(65536);
                if (random.nextInt(4) != 0) {
                    effects &= 0x7fff;
                }
                byte[] shape = concat(u16(idx), u16(effects), u8(random.nextInt(4)), messageTail(state.table));
                sceneApply(ServerProt.MESSAGE_PUBLIC, state, shape);
                sceneApply(ServerProt.MESSAGE_PUBLIC, state, concat(shape, new byte[] {1}));
                if (round < 10) {
                    for (int cut = 0; cut < shape.length; cut++) {
                        sceneApply(ServerProt.MESSAGE_PUBLIC, state, Arrays.copyOf(shape, cut));
                    }
                }
            }
            for (int type = 0; type < 32; type++) {
                for (int slot = 0; slot < 8; slot += 3) {
                    int[] sprites = {0, Math.max(state.sprites - 1, 0), state.sprites, 255};
                    int sprite = sprites[random.nextInt(sprites.length)];
                    byte[] shape;
                    if (type == 0) {
                        shape = u8(slot << 5);
                    } else if (type == 1 || type == 10) {
                        shape = concat(u8(slot << 5 | type), u8(sprite), u16(random.nextInt(65536)), u16(random.nextInt(65536)), randomBytes(4), randomBytes(4));
                    } else if (type <= 6) {
                        shape = concat(u8(slot << 5 | type), u8(sprite), u8(random.nextInt(4)), u16(state.baseX + random.nextInt(104)), u16(state.baseY - random.nextInt(2) * random.nextInt(104)), u8(random.nextInt(256)), u16(random.nextInt(65536)), randomBytes(4));
                    } else {
                        shape = concat(u8(slot << 5 | type), u8(sprite), randomBytes(4));
                    }
                    sceneApply(ServerProt.HINT_ARROW, state, shape);
                    sceneApply(ServerProt.HINT_ARROW, state, concat(shape, new byte[] {1}));
                    sceneApply(ServerProt.HINT_ARROW, state, concat(u8(slot << 5 | type), u8(sprite), randomBytes(random.nextInt(20))));
                    for (int cut = 0; cut < shape.length; cut++) {
                        sceneApply(ServerProt.HINT_ARROW, state, Arrays.copyOf(shape, cut));
                    }
                }
            }
            int[] models = {0, 5, 32766, 32767, 32768 + 7, 0x7fffffff};
            int[] slots = {0, 3, 8, 9, 255};
            int[] lengths = {-64, -1, 0, 1, 2, 5, 63, 64, 100};
            for (int slot : slots) {
                for (int model : models) {
                    for (int length : lengths) {
                        byte[] id = model < 32768 ? u16(model) : new byte[] {(byte) (0x80 | model >>> 24), (byte) (model >> 16), (byte) (model >> 8), (byte) model};
                        byte[] size = length + 64 < 128 ? u8(length + 64) : u16(length + 49152);
                        int pairs = Math.max(length, 0);
                        byte[] shape = concat(u8(slot), id, size, u16(state.baseX + random.nextInt(50)), u16(state.baseY + random.nextInt(50)), randomBytes(pairs * 2));
                        sceneApply(ServerProt.HINT_TRAIL, state, shape);
                        if (length >= 0 && length <= 5 && slot < 9) {
                            sceneApply(ServerProt.HINT_TRAIL, state, concat(shape, new byte[] {1}));
                            for (int cut = 0; cut < shape.length; cut++) {
                                sceneApply(ServerProt.HINT_TRAIL, state, Arrays.copyOf(shape, cut));
                            }
                        }
                    }
                }
            }
            int[] counts = {0, 1, 2, 3, 6, 40};
            int[] sizes = {0, 1, 7, 255};
            for (int count : counts) {
                for (int size : sizes) {
                    byte[] shape = concat(u16(random.nextInt(65536)), u16(count), randomBytes(count * 16), u8(size), randomBytes(size));
                    sceneApply(ServerProt.CUTSCENE, state, shape);
                    sceneApply(ServerProt.CUTSCENE, state, concat(shape, new byte[] {1}));
                    if (count <= 1 && size <= 7) {
                        for (int cut = 0; cut < shape.length; cut++) {
                            sceneApply(ServerProt.CUTSCENE, state, Arrays.copyOf(shape, cut));
                        }
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        require(args.length == 2, "Expected output directory and pinned jar");
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        java.lang.reflect.Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        UNSAFE = unsafeField.get(null);
        UNSAFE_ALLOCATE = unsafeClass.getMethod("allocateInstance", Class.class);
        Path output = Path.of(args[0]);
        Path jar = Path.of(args[1]).toRealPath();
        require(Path.of(Client.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar), "Client was not loaded directly from the jar");
        BufferedConnectionContext context = null;
        try {
            Bot.ENABLED = false;
            Client.GAME_CONNECTION_CONTEXT.pinger.disable();
            Client.LOBBY_CONNECTION_CONTEXT.pinger.disable();
            context = Client.GAME_CONNECTION_CONTEXT;
            context.pinger.disable();
            WorldPacketsOracle oracle = new WorldPacketsOracle(context);
            for (String name : FIXED) {
                oracle.fixedCases(ServerProt.valueOf(name));
            }
            oracle.variableCases();
            buildTables();
            for (int id = 1; id <= 3; id++) {
                oracle.out.append("H\t").append(id).append('\t').append(hex(TABLES[id])).append('\n');
            }
            oracle.chatCases();
            oracle.sceneCases();
            oracle.out.append("Z\t").append(oracle.cases).append('\n');
            Files.writeString(output.resolve("world-packets-cases.tsv"), oracle.out, StandardCharsets.US_ASCII);
            System.out.println("Decoded " + oracle.cases + " native world packets");
            for (var entry : oracle.perPacket.entrySet()) {
                System.out.println(entry.getKey() + " ok=" + entry.getValue()[0] + " exception=" + entry.getValue()[1] + " overrun=" + entry.getValue()[2]);
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
