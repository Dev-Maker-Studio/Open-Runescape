import com.jagex.game.runetek5.client.Client;
import com.jagex.game.runetek5.client.GameState;
import com.jagex.game.runetek5.client.login.AccountCreationResponseOpcodes;
import com.jagex.game.runetek5.client.login.AccountCreationStage;
import com.jagex.game.runetek5.client.login.CheckEmailRequest;
import com.jagex.game.runetek5.client.login.CreateAccountRequest;
import com.jagex.game.runetek5.client.login.LoginManager;
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor;
import com.jagex.game.runetek5.config.rsinterface.cs2.ClientScriptOpCode;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.CameraLoginPrefOps;
import com.jagex.game.runetek5.network.BufferedConnectionContext;
import com.jagex.game.runetek5.network.packet.TcpPacket;
import com.jagex.game.runetek5.staticmethods.StaticMethods;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class LoginEffectFixtures {
    static final long SEED = 0x727c5228L ^ 0x5eedL;
    static final int PER_OP = 130;
    static final int SEQUENCES = 900;
    static final int[] GAME_STATES = {5, 5, 5, 0, 0, 7, 7, 14, 19, 12, 6, 3, 3, 8, 4, 10, 17, 13, 18, 1, 2, 9, 11, 15, 16, 20, 21};
    static final int[] CREATE_VALUES = {-2, -3, 2, 3, 9, 10, 20, 21, 30, 31, 32, 33, 34, 38};
    static final int[] EMAIL_VALUES = {-2, -3, 2, 3, 21, 20};
    static final int[] RECEIVED_VALUES = {-1, -2, -3, -4, -5, 2, 3, 7, 9, 37};
    static final int[] SMALL = {-2, -1, 0, 1, 2, 3, 5, 35, 100};
    static final int[] WORLDS = {-1, 0, 1, 2, 301, 5, -2, 65536};
    static final int[] AGES = {0, 1, 12, 13, 14, 18, 100, 255, 256, 269, -1, -250, 1000, Integer.MAX_VALUE, Integer.MIN_VALUE};
    static final int[] STAGES = {0, 1, 2, 3, 5, 7, 9, 255, 256, -1, 300};
    static final int[] KEYS = {0x1234567, -0x7edcba98, 0x13579bdf, 0x2468ace0};
    static final byte[] ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 _-.@+'!~".getBytes(StandardCharsets.ISO_8859_1);

    static final Map<String, int[]> SIGNATURE = new LinkedHashMap<>();
    static {
        SIGNATURE.put("LOGIN_REQUEST", new int[] {0, 2});
        SIGNATURE.put("LOGIN_CONTINUE", new int[] {0, 0});
        SIGNATURE.put("LOGIN_RESETREPLY", new int[] {0, 0});
        SIGNATURE.put("LOGIN_CANCEL", new int[] {0, 0});
        SIGNATURE.put("LOGIN_INPROGRESS", new int[] {0, 0});
        SIGNATURE.put("LOGIN_LAST_TRANSFER_REPLY", new int[] {0, 0});
        SIGNATURE.put("LOGIN_REQUEST_SOCIAL_NETWORK", new int[] {1, 0});
        SIGNATURE.put("CREATE_AVAILABLEREQUEST", new int[] {0, 1});
        SIGNATURE.put("CREATE_CREATEREQUEST", new int[] {2, 2});
        SIGNATURE.put("CREATE_CONNECTREQUEST", new int[] {0, 0});
        SIGNATURE.put("CREATE_STEP_REACHED", new int[] {1, 0});
        SIGNATURE.put("CREATE_REPLY", new int[] {0, 0});
        SIGNATURE.put("CREATE_EMAIL_VALIDATE_REPLY", new int[] {0, 0});
        SIGNATURE.put("CREATE_CONNECT_REPLY", new int[] {0, 0});
        SIGNATURE.put("LOBBY_ENTERGAME", new int[] {0, 0});
        SIGNATURE.put("LOBBY_ENTERLOBBY", new int[] {0, 2});
        SIGNATURE.put("LOBBY_LEAVELOBBY", new int[] {0, 0});
        SIGNATURE.put("LOBBY_ENTERLOBBY_SOCIAL_NETWORK", new int[] {1, 0});
    }

    static int pick(Random random, int[] values) {
        return values[random.nextInt(values.length)];
    }

    static String hex(String value) {
        var bytes = value.getBytes(StandardCharsets.UTF_8);
        var text = new StringBuilder("x");
        for (byte b : bytes) text.append(String.format("%02x", b & 255));
        return text.toString();
    }

    static String hexOrNull(String value) {
        return value == null ? "n" : hex(value);
    }

    static String latin(Random random) {
        int kind = random.nextInt(8);
        if (kind == 0) return "";
        var text = new StringBuilder();
        int length = kind == 1 ? 1 : 1 + random.nextInt(14);
        for (int i = 0; i < length; i++) {
            if (random.nextInt(10) == 0) text.append((char) (160 + random.nextInt(96)));
            else text.append((char) (ALPHABET[random.nextInt(ALPHABET.length)] & 255));
        }
        return text.toString();
    }

    static String wide(Random random) {
        int kind = random.nextInt(12);
        var text = new StringBuilder();
        if (kind == 0) return "";
        if (kind == 1) return "a".repeat(320);
        if (kind == 2) return "a".repeat(321);
        if (kind == 3) return "😀".repeat(160);
        if (kind == 4) return "😀".repeat(161);
        if (kind == 5) return "😀" + "a".repeat(319);
        if (kind == 6) return "é".repeat(320) + (random.nextBoolean() ? "z" : "");
        if (kind == 7) return "中文" + latin(random);
        return latin(random);
    }

    static String credential(Random random, boolean wide) {
        if (random.nextInt(14) == 0) return null;
        return wide ? wide(random) : latin(random);
    }

    static String csv(int[] values, int count) {
        if (count == 0) return "-";
        var text = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) text.append(',');
            text.append(values[i]);
        }
        return text.toString();
    }

    static BufferedConnectionContext lobby() {
        return Client.LOBBY_CONNECTION_CONTEXT;
    }

    static BufferedConnectionContext game() {
        return Client.GAME_CONNECTION_CONTEXT;
    }

    static final class Pre {
        int gameState, loginStage, lobbyStage, selected;
        long token, nonce;
        boolean snLogin, stored, justCreated;
        String user, pass;
        int queue, detail, extra, lobbyResponse, worldResponse, lastResponse, lastArg1, lastArg2;
        int createStage;
        Integer createRequest, checkEmail, received;
        boolean lobbyOpen, gameOpen, lobbyDropped, gameDropped;
        int current;
        int tick, retry;
        boolean xteas;
        long[] base64;

        String render() {
            return String.join(",",
                "" + gameState, "" + loginStage, "" + lobbyStage, "" + selected, "" + token, "" + nonce,
                snLogin ? "1" : "0", stored ? "1" : "0", hex(user), hex(pass),
                "" + queue, "" + detail, "" + extra, "" + lobbyResponse, "" + worldResponse,
                "" + lastResponse, "" + lastArg1, "" + lastArg2, "" + createStage,
                createRequest == null ? "n" : "" + createRequest,
                checkEmail == null ? "n" : "" + checkEmail,
                received == null ? "n" : "" + received,
                lobbyOpen ? "1" : "0", gameOpen ? "1" : "0", lobbyDropped ? "1" : "0", gameDropped ? "1" : "0",
                "" + current, "" + tick, "" + retry, justCreated ? "1" : "0", xteas ? "1" : "0",
                base64 == null ? "n" : base64[0] + ":" + base64[1]);
        }
    }

    static int biasedState(Random random, String op) {
        boolean bias = random.nextInt(10) < 7;
        switch (op) {
            case "LOGIN_REQUEST", "LOGIN_REQUEST_SOCIAL_NETWORK", "LOBBY_ENTERLOBBY", "LOBBY_ENTERLOBBY_SOCIAL_NETWORK", "CREATE_CONNECTREQUEST":
                if (bias) return 5;
                break;
            case "LOBBY_ENTERGAME":
                if (bias) return 0;
                break;
            case "CREATE_AVAILABLEREQUEST", "CREATE_CREATEREQUEST", "CREATE_STEP_REACHED":
                if (bias) return 3;
                break;
            case "LOGIN_CANCEL":
                if (bias) return pick(random, new int[] {5, 0, 7, 6, 14, 19, 12, 10});
                break;
            default:
        }
        return pick(random, GAME_STATES);
    }

    static Pre randomPre(Random random, String op) {
        var pre = new Pre();
        pre.gameState = biasedState(random, op);
        pre.loginStage = random.nextInt(10) < 7 ? 0 : random.nextInt(20);
        if (op.equals("LOGIN_CONTINUE") && random.nextBoolean()) pre.loginStage = 9;
        if (op.equals("LOGIN_CANCEL") && random.nextInt(10) < 7) pre.loginStage = 1 + random.nextInt(19);
        pre.lobbyStage = random.nextInt(3);
        pre.selected = random.nextInt(10) < 6 ? -1 : pick(random, WORLDS);
        pre.token = random.nextInt(2) == 0 ? -1 : random.nextInt(6) == 0 ? random.nextLong() : random.nextInt(1000);
        pre.nonce = random.nextInt(3) == 0 ? 0 : random.nextInt(1000);
        pre.snLogin = random.nextBoolean();
        pre.stored = random.nextBoolean();
        pre.justCreated = random.nextInt(4) == 0;
        pre.user = latin(random);
        pre.pass = latin(random);
        pre.queue = random.nextInt(3) == 0 ? 0 : random.nextInt(50);
        pre.detail = random.nextInt(3) == 0 ? -1 : pick(random, SMALL);
        pre.extra = random.nextInt(3) == 0 ? -1 : pick(random, SMALL);
        pre.lobbyResponse = random.nextInt(3) == 0 ? -2 : pick(random, SMALL);
        pre.worldResponse = random.nextInt(3) == 0 ? -2 : pick(random, SMALL);
        pre.lastResponse = random.nextInt(3) == 0 ? -2 : pick(random, SMALL);
        pre.lastArg1 = random.nextInt(3) == 0 ? -1 : pick(random, SMALL);
        pre.lastArg2 = random.nextInt(3) == 0 ? -1 : pick(random, SMALL);
        int stage = random.nextInt(10);
        pre.createStage = stage < 7 ? 0 : stage < 9 ? 1 : 2;
        pre.createRequest = random.nextInt(4) == 0 ? null : pick(random, CREATE_VALUES);
        pre.checkEmail = random.nextInt(4) == 0 ? null : pick(random, EMAIL_VALUES);
        pre.received = random.nextInt(4) == 0 ? null : pick(random, RECEIVED_VALUES);
        pre.lobbyOpen = random.nextBoolean();
        pre.gameOpen = random.nextBoolean();
        pre.lobbyDropped = random.nextBoolean();
        pre.gameDropped = random.nextBoolean();
        pre.current = random.nextInt(10) < 2 ? 0 : 1 + random.nextInt(2);
        pre.tick = random.nextInt(5);
        pre.retry = random.nextInt(5);
        pre.xteas = random.nextInt(10) < 7;
        if (random.nextInt(8) == 0) {
            long token = random.nextInt(3) == 0 ? -1 - random.nextInt(5) : random.nextInt(1000);
            pre.base64 = new long[] {token, random.nextInt(1000)};
        }
        return pre;
    }

    static <T> T find(T[] values, java.util.function.ToIntFunction<T> value, int wanted) {
        for (T candidate : values) if (value.applyAsInt(candidate) == wanted) return candidate;
        throw new IllegalStateException("" + wanted);
    }

    static byte[] longs(long a, long b) {
        var out = new ByteArrayOutputStream();
        for (long value : new long[] {a, b}) for (int shift = 56; shift >= 0; shift -= 8) out.write((int) (value >> shift) & 255);
        return out.toByteArray();
    }

    static void apply(Pre pre) {
        Client.gameState = GameState.values()[pre.gameState];
        LoginManager.loginStage = LoginManager.LoginStage.values()[pre.loginStage];
        LoginManager.lobbyStage = LoginManager.LobbyStage.values()[pre.lobbyStage];
        StaticMethods.selectedWorldId = pre.selected;
        LoginManager.socialNetworkToken = pre.token;
        LoginManager.socialNetworkNonce = pre.nonce;
        LoginManager.socialNetworkLogin = pre.snLogin;
        StaticMethods.hasStoredCredentials = pre.stored;
        Client.hasJustCreatedAccount = pre.justCreated;
        StaticMethods.loginUsername = pre.user;
        StaticMethods.loginPassword = pre.pass;
        LoginManager.loginQueuePosition = pre.queue;
        LoginManager.loginResponseDetail = pre.detail;
        LoginManager.loginResponseExtraInfo = pre.extra;
        StaticMethods.lobbyLoginResponse = pre.lobbyResponse;
        StaticMethods.worldLoginResponse = pre.worldResponse;
        StaticMethods.lastDisconnectResponse = pre.lastResponse;
        StaticMethods.lastDisconnectArg1 = pre.lastArg1;
        StaticMethods.lastDisconnectArg2 = pre.lastArg2;
        AccountCreationStage.ACCOUNT_CREATION_STAGE = pre.createStage == 0 ? null : pre.createStage == 1 ? AccountCreationStage.REQUEST : AccountCreationStage.RESPONSE;
        AccountCreationStage.currentRequest = pre.createRequest == null ? null : find(CreateAccountRequest.values(), v -> v.responseCode, pre.createRequest);
        AccountCreationStage.checkemailRequeststatus = pre.checkEmail == null ? null : find(CheckEmailRequest.values(), v -> v.value, pre.checkEmail);
        AccountCreationStage.RECEIVED_RESPONSE = pre.received == null ? null : find(AccountCreationResponseOpcodes.values(), v -> v.opcode, pre.received);
        lobby().connection = pre.lobbyOpen ? new LoginEffectHooks.Stub("lobby") : null;
        game().connection = pre.gameOpen ? new LoginEffectHooks.Stub("game") : null;
        lobby().dropped = pre.lobbyDropped;
        game().dropped = pre.gameDropped;
        LoginManager.CURRENT_CONNECTION_CONTEXT = pre.current == 0 ? null : pre.current == 1 ? lobby() : game();
        LoginManager.connectionTickCount = pre.tick;
        LoginManager.connectionRetryCount = pre.retry;
        AccountCreationStage.LOGIN_XTEAS = pre.xteas ? KEYS.clone() : null;
        Client.base64Url = pre.base64 == null ? null : longs(pre.base64[0], pre.base64[1]);
        lobby().queuedPackets.remove_all();
        lobby().readDataSize = 0;
        game().queuedPackets.remove_all();
        LoginEffectHooks.LOG.clear();
    }

    static String post() {
        var pre = new Pre();
        pre.gameState = Client.gameState.ordinal();
        pre.loginStage = LoginManager.loginStage.ordinal();
        pre.lobbyStage = LoginManager.lobbyStage.ordinal();
        pre.selected = StaticMethods.selectedWorldId;
        pre.token = LoginManager.socialNetworkToken;
        pre.nonce = LoginManager.socialNetworkNonce;
        pre.snLogin = LoginManager.socialNetworkLogin;
        pre.stored = StaticMethods.hasStoredCredentials;
        pre.justCreated = Client.hasJustCreatedAccount;
        pre.user = StaticMethods.loginUsername;
        pre.pass = StaticMethods.loginPassword;
        pre.queue = LoginManager.loginQueuePosition;
        pre.detail = LoginManager.loginResponseDetail;
        pre.extra = LoginManager.loginResponseExtraInfo;
        pre.lobbyResponse = StaticMethods.lobbyLoginResponse;
        pre.worldResponse = StaticMethods.worldLoginResponse;
        pre.lastResponse = StaticMethods.lastDisconnectResponse;
        pre.lastArg1 = StaticMethods.lastDisconnectArg1;
        pre.lastArg2 = StaticMethods.lastDisconnectArg2;
        var stage = AccountCreationStage.ACCOUNT_CREATION_STAGE;
        pre.createStage = stage == null ? 0 : stage == AccountCreationStage.REQUEST ? 1 : 2;
        pre.createRequest = AccountCreationStage.currentRequest == null ? null : AccountCreationStage.currentRequest.responseCode;
        pre.checkEmail = AccountCreationStage.checkemailRequeststatus == null ? null : AccountCreationStage.checkemailRequeststatus.value;
        pre.received = AccountCreationStage.RECEIVED_RESPONSE == null ? null : AccountCreationStage.RECEIVED_RESPONSE.opcode;
        pre.lobbyOpen = lobby().connection != null;
        pre.gameOpen = game().connection != null;
        pre.lobbyDropped = lobby().dropped;
        pre.gameDropped = game().dropped;
        pre.current = LoginManager.CURRENT_CONNECTION_CONTEXT == null ? 0 : LoginManager.CURRENT_CONNECTION_CONTEXT == lobby() ? 1 : 2;
        pre.tick = LoginManager.connectionTickCount;
        pre.retry = LoginManager.connectionRetryCount;
        pre.xteas = AccountCreationStage.LOGIN_XTEAS != null;
        pre.base64 = Client.base64Url == null ? null : new long[] {readLong(Client.base64Url, 0), readLong(Client.base64Url, 8)};
        return pre.render();
    }

    static long readLong(byte[] data, int offset) {
        long value = 0;
        for (int i = 0; i < 8; i++) value = (value << 8) | (data[offset + i] & 255L);
        return value;
    }

    static void decrypt(byte[] data, int start, int end) {
        int blocks = (end - start) / 8;
        for (int b = 0; b < blocks; b++) {
            int at = start + b * 8;
            int v0 = (int) readLong(data, at - 0) >>> 0;
            v0 = ((data[at] & 255) << 24) | ((data[at + 1] & 255) << 16) | ((data[at + 2] & 255) << 8) | (data[at + 3] & 255);
            int v1 = ((data[at + 4] & 255) << 24) | ((data[at + 5] & 255) << 16) | ((data[at + 6] & 255) << 8) | (data[at + 7] & 255);
            int sum = -957401312;
            int delta = -1640531527;
            for (int round = 0; round < 32; round++) {
                v1 -= (((v0 << 4) ^ (v0 >>> 5)) + v0) ^ (sum + KEYS[(sum >>> 11) & 3]);
                sum -= delta;
                v0 -= (((v1 << 4) ^ (v1 >>> 5)) + v1) ^ (sum + KEYS[sum & 3]);
            }
            data[at] = (byte) (v0 >> 24);
            data[at + 1] = (byte) (v0 >> 16);
            data[at + 2] = (byte) (v0 >> 8);
            data[at + 3] = (byte) v0;
            data[at + 4] = (byte) (v1 >> 24);
            data[at + 5] = (byte) (v1 >> 16);
            data[at + 6] = (byte) (v1 >> 8);
            data[at + 7] = (byte) v1;
        }
    }

    static String readString(byte[] data, int[] at) {
        int start = at[0];
        while (data[at[0]] != 0) at[0]++;
        var text = new String(data, start, at[0] - start, StandardCharsets.ISO_8859_1);
        at[0]++;
        return text;
    }

    static void drain() {
        var ctx = lobby();
        var packets = new ArrayList<TcpPacket>();
        TcpPacket packet;
        while ((packet = (TcpPacket) ctx.queuedPackets.popHead()) != null) packets.add(packet);
        Collections.reverse(packets);
        for (var queued : packets) {
            byte[] data = queued.buffer.buffer;
            int id = data[0] & 255;
            int capacity = queued.capacity;
            if (id == 79) {
                if (capacity != 2) throw new IllegalStateException("stage packet size");
                LoginEffectHooks.LOG.add("stage:" + (data[1] & 255));
                continue;
            }
            int length = ((data[1] & 255) << 8) | (data[2] & 255);
            if (length != capacity - 3) throw new IllegalStateException("length " + length + " " + capacity);
            decrypt(data, 3, capacity);
            var at = new int[] {3};
            if (id == 102) {
                LoginEffectHooks.LOG.add("email:" + hex(readString(data, at)));
            } else if (id == 101) {
                var email = readString(data, at);
                var password = readString(data, at);
                int age = data[at[0]] & 255;
                int agreed = data[at[0] + 1];
                LoginEffectHooks.LOG.add("signup:" + hex(email) + ":" + hex(password) + ":" + age + ":" + agreed);
            } else {
                throw new IllegalStateException("packet " + id);
            }
        }
    }

    static final class Op {
        String name;
        int[] ints = new int[0];
        String[] strings = new String[0];
    }

    static Op randomOp(Random random, String name, boolean wideStrings) {
        var op = new Op();
        op.name = name;
        var signature = SIGNATURE.get(name);
        op.ints = new int[signature[0]];
        op.strings = new String[signature[1]];
        if (name.equals("CREATE_CREATEREQUEST")) {
            op.ints[0] = pick(random, AGES);
            op.ints[1] = random.nextInt(3) == 0 ? pick(random, new int[] {0, 1, 2, -1}) : random.nextInt(2);
        } else if (name.equals("CREATE_STEP_REACHED")) {
            op.ints[0] = pick(random, STAGES);
        } else {
            for (int i = 0; i < op.ints.length; i++) op.ints[i] = pick(random, WORLDS);
        }
        boolean credentials = name.equals("LOGIN_REQUEST") || name.equals("LOBBY_ENTERLOBBY");
        for (int i = 0; i < op.strings.length; i++) op.strings[i] = credential(random, wideStrings && credentials);
        if (random.nextInt(14) == 0) {
            if (op.strings.length > 0 && (op.ints.length == 0 || random.nextBoolean())) op.strings = Arrays.copyOf(op.strings, op.strings.length - 1);
            else if (op.ints.length > 0) op.ints = Arrays.copyOf(op.ints, op.ints.length - 1);
        }
        return op;
    }

    static String row(Pre pre, List<Op> ops) {
        var names = new StringBuilder();
        var ints = new StringBuilder();
        var strings = new StringBuilder();
        for (int i = 0; i < ops.size(); i++) {
            var op = ops.get(i);
            if (i > 0) {
                names.append(',');
                ints.append('|');
                strings.append('|');
            }
            names.append(op.name);
            ints.append(csv(op.ints, op.ints.length));
            if (op.strings.length == 0) strings.append('-');
            for (int s = 0; s < op.strings.length; s++) {
                if (s > 0) strings.append(',');
                strings.append(hexOrNull(op.strings[s]));
            }
        }
        var e = new CS2Executor();
        Arrays.fill(e.intStack, 0x5a5a5a5a);
        e.intStackPtr = 0;
        e.stringStackPtr = 0;
        e.intOpValues = new int[] {0};
        e.instrPtr = 0;
        apply(pre);
        boolean ok = true;
        var log = LoginEffectHooks.LOG;
        try {
            for (var op : ops) {
                for (int value : op.ints) e.intStack[e.intStackPtr++] = value;
                for (var value : op.strings) e.stringStack[e.stringStackPtr++] = value;
                CameraLoginPrefOps.dispatch(ClientScriptOpCode.valueOf(op.name), e);
                drain();
            }
        } catch (Throwable error) {
            ok = false;
            if (System.getProperty("fixture.debug") != null) error.printStackTrace();
        }
        var result = new StringBuilder("C\t").append(names).append('\t').append(ints).append('\t').append(strings)
            .append('\t').append(pre.render()).append('\t').append(ok ? "ok" : "error").append('\t');
        if (ok) {
            result.append(csv(e.intStack, Math.max(0, e.intStackPtr))).append('\t');
            if (e.stringStackPtr <= 0) result.append('-');
            for (int i = 0; i < e.stringStackPtr; i++) {
                if (i > 0) result.append(',');
                result.append(hexOrNull((String) e.stringStack[i]));
            }
            result.append('\t').append(post()).append('\t');
            result.append(log.isEmpty() ? "-" : String.join(",", log));
        } else {
            result.append("-\t-\t-\t-");
        }
        return result.toString();
    }

    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        var random = new Random(SEED);
        var names = new ArrayList<>(SIGNATURE.keySet());
        var lines = new ArrayList<String>();
        for (var name : names) {
            for (int i = 0; i < PER_OP; i++) {
                var pre = randomPre(random, name);
                lines.add(row(pre, List.of(randomOp(random, name, true))));
            }
        }
        for (int i = 0; i < SEQUENCES; i++) {
            int count = 2 + random.nextInt(3);
            var ops = new ArrayList<Op>();
            var first = names.get(random.nextInt(names.size()));
            var pre = randomPre(random, first);
            for (int k = 0; k < count; k++) ops.add(randomOp(random, k == 0 ? first : names.get(random.nextInt(names.size())), false));
            lines.add(row(pre, ops));
        }
        var text = new StringBuilder();
        for (var line : lines) text.append(line).append('\n');
        text.append("Z\t").append(lines.size()).append('\n');
        Files.writeString(output.resolve("login-effects.native.tsv"), text.toString());
        System.exit(0);
    }
}
