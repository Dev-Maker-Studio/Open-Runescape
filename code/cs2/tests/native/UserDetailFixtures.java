import com.jagex.game.runetek5.cache.FileOnDisk;
import com.jagex.game.runetek5.cache.node.QueueIterator;
import com.jagex.game.runetek5.client.Client;
import com.jagex.game.runetek5.client.login.LoginManager;
import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.WorldMapUserDetailOps;
import com.jagex.game.runetek5.network.HostNameIdentifier;
import com.jagex.game.runetek5.network.packet.PacketDecoder;
import com.jagex.game.runetek5.utils.Utils;
import java.nio.file.*;
import java.util.*;

/**
 * Drives the real WorldMapUserDetailOps.dispatch for the USERDETAIL_LOBBY_* and USERDETAIL_DOB
 * operations and the three BOOST_ADVERT_* operations. The lobby login statics of LoginManager,
 * Client, FileOnDisk and QueueIterator are injected, the hostname identifier is a real object with a
 * preset host and Utils.time() is injected.
 */
public final class UserDetailFixtures {
    static final long FAR = 1L << 62;
    static final StringBuilder out = new StringBuilder();
    static final TreeMap<String, Integer> counts = new TreeMap<>();
    static int total;

    static final ClientScriptOpCode[] OPS = {
        ClientScriptOpCode.USERDETAIL_LOBBY_MEMBERSHIP, ClientScriptOpCode.USERDETAIL_LOBBY_RECOVERYDAY,
        ClientScriptOpCode.USERDETAIL_LOBBY_UNREADMESSAGES, ClientScriptOpCode.USERDETAIL_LOBBY_LASTLOGINDAY,
        ClientScriptOpCode.USERDETAIL_LOBBY_LASTLOGINADDRESS, ClientScriptOpCode.USERDETAIL_LOBBY_EMAILSTATUS,
        ClientScriptOpCode.USERDETAIL_LOBBY_CCEXPIRY, ClientScriptOpCode.USERDETAIL_LOBBY_GRACEEXPIRY,
        ClientScriptOpCode.USERDETAIL_LOBBY_DOBREQUESTED, ClientScriptOpCode.USERDETAIL_DOB,
        ClientScriptOpCode.USERDETAIL_LOBBY_MEMBERSSTATS, ClientScriptOpCode.USERDETAIL_LOBBY_PLAYAGE,
        ClientScriptOpCode.USERDETAIL_LOBBY_JCOINS_BALANCE, ClientScriptOpCode.USERDETAIL_LOBBY_LOYALTY_ENABLED,
        ClientScriptOpCode.USERDETAIL_LOBBY_LOYALTY_BALANCE, ClientScriptOpCode.BOOST_ADVERT_EXISTS,
        ClientScriptOpCode.BOOST_ADVERT_AVAILABLE, ClientScriptOpCode.BOOST_ADVERT_LAUNCH,
    };

    static final long[] LONGS = {0, 1, -1, 59999, 60000, 60001, -59999, -60000, -60001, 1L << 40, -(1L << 40),
        Long.MAX_VALUE, Long.MIN_VALUE, 1_700_000_000_000L, 1_700_000_000_000L + 3_600_000, 253402300799000L};
    static final int[] INTS = {0, 1, -1, 2, 255, 256, 65535, 65536, 1 << 20, 16777215, Integer.MAX_VALUE, Integer.MIN_VALUE, 12345, -777};
    static final String[] HOSTS = {null, "", "localhost", "ip-10-0-0-1.example.org", "café.€.example", "A\u0000B", "üœ™"};

    static long pick(Random r, long[] pool) {
        return pool[r.nextInt(pool.length)];
    }

    static int pick(Random r, int[] pool) {
        return pool[r.nextInt(pool.length)];
    }

    static String bool(boolean value) {
        return value ? "1" : "0";
    }

    static void state(long now, long end, long correction, boolean sub, int recovery, int messages, int lastDay, String host,
                      int email, int cc, int grace, boolean dobRequested, int dob, int stats, int playAge, int jcoins,
                      boolean loyalty, int balance) throws Exception {
        Utils.lastSystemTime = FAR;
        Utils.timeOffset = now - FAR;
        ClientStateFixtures.setNumber(LoginManager.class, "MEMBERSHIP_END", end);
        ClientStateFixtures.setNumber(LoginManager.class, "membershipTimeCorrection", correction);
        ClientStateFixtures.setNumber(LoginManager.class, "IS_MEMBER_SUBSCRIPTION", sub ? 1 : 0);
        ClientStateFixtures.setNumber(LoginManager.class, "RECOVERYQUESTIONSSETDATE", recovery);
        ClientStateFixtures.setNumber(LoginManager.class, "MESSAGES", messages);
        ClientStateFixtures.setNumber(LoginManager.class, "LASTLOGINLDAY", lastDay);
        ClientStateFixtures.setNumber(LoginManager.class, "EMAIL_STATUS", email);
        ClientStateFixtures.setNumber(LoginManager.class, "lobbyCreditCardExpiry", cc);
        ClientStateFixtures.setNumber(FileOnDisk.class, "lobbyGraceExpiry", grace);
        ClientStateFixtures.setNumber(LoginManager.class, "isDobRequested", dobRequested ? 1 : 0);
        ClientStateFixtures.setNumber(LoginManager.class, "PLAYER_DOB", dob);
        ClientStateFixtures.setNumber(Client.class, "lobbyMembershipStatus", stats);
        ClientStateFixtures.setNumber(LoginManager.class, "lobbyPlayAge", playAge);
        ClientStateFixtures.setNumber(Client.class, "JCOINS", jcoins);
        ClientStateFixtures.setNumber(QueueIterator.class, "LOYALTY_ENABLED", loyalty ? 1 : 0);
        ClientStateFixtures.setNumber(LoginManager.class, "LOYALTY_BALANCE", balance);
        HostNameIdentifier identifier = null;
        if (host != null) {
            identifier = new HostNameIdentifier(0x7f000001);
            identifier.host = host;
        }
        ClientStateFixtures.setObject(PacketDecoder.class, "HOSTNAME_IDENTIFIER", identifier);
        out.append("S\t").append(now).append('\t').append(end).append('\t').append(correction).append('\t').append(bool(sub))
            .append('\t').append(recovery).append('\t').append(messages).append('\t').append(lastDay).append('\t')
            .append(NativeFixtures.utf16(host)).append('\t').append(email).append('\t').append(cc).append('\t').append(grace)
            .append('\t').append(bool(dobRequested)).append('\t').append(dob).append('\t').append(stats).append('\t')
            .append(playAge).append('\t').append(jcoins).append('\t').append(bool(loyalty)).append('\t').append(balance).append('\n');
    }

    static boolean takesString(ClientScriptOpCode op) {
        return op == ClientScriptOpCode.BOOST_ADVERT_EXISTS || op == ClientScriptOpCode.BOOST_ADVERT_AVAILABLE
            || op == ClientScriptOpCode.BOOST_ADVERT_LAUNCH;
    }

    static void run(ClientScriptOpCode op, String argument) {
        var exec = new CS2Executor();
        exec.intStackPtr = 0;
        exec.stringStackPtr = 0;
        exec.longStackPtr = 0;
        if (takesString(op)) exec.stringStack[exec.stringStackPtr++] = argument;
        WorldMapUserDetailOps.dispatch(op, exec);
        var strings = new String[exec.stringStackPtr];
        for (int i = 0; i < strings.length; i++) strings[i] = (String) exec.stringStack[i];
        out.append("C\t").append(op.name()).append('\t').append(takesString(op) ? NativeFixtures.utf16(argument) : "-").append('\t')
            .append(NativeFixtures.integers(Arrays.copyOf(exec.intStack, exec.intStackPtr))).append('\t')
            .append(NativeFixtures.texts(strings)).append('\n');
        counts.merge(op.name(), 1, Integer::sum);
        total++;
    }

    static void all(Random r) {
        for (var op : OPS) run(op, takesString(op) ? HOSTS[r.nextInt(HOSTS.length)] : null);
    }

    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        ClientStateFixtures.unsafe = FontFixtures.unsafe();
        var r = new Random(727);
        state(0, 0, 0, false, 0, 0, 0, null, 0, 0, 0, false, 0, 0, 0, 0, false, 0);
        all(r);
        for (int round = 0; round < 400; round++) {
            long now = round < 40 ? pick(r, LONGS) : 1_700_000_000_000L + r.nextInt(1_000_000_000);
            long end = round < 40 ? pick(r, LONGS) : now + r.nextInt(200_000_000) - 100_000_000L;
            long correction = r.nextInt(3) == 0 ? pick(r, LONGS) : r.nextInt(2_000_000) - 1_000_000L;
            state(now, end, correction, r.nextBoolean(), pick(r, INTS), pick(r, INTS), pick(r, INTS), HOSTS[r.nextInt(HOSTS.length)],
                pick(r, INTS), pick(r, INTS), pick(r, INTS), r.nextBoolean(), pick(r, INTS), pick(r, INTS), pick(r, INTS),
                pick(r, INTS), r.nextBoolean(), pick(r, INTS));
            all(r);
        }
        out.append("Z\t").append(total).append('\n');
        Files.writeString(output.resolve("userdetail.native.tsv"), out);
        System.out.println("Native userdetail cases " + counts);
        System.out.println("Generated " + total + " actual native userdetail dispatch cases");
        System.out.flush();
        Runtime.getRuntime().halt(0);
    }
}
