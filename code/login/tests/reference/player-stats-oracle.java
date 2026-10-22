import com.darkan.bot.Bot;
import com.jagex.game.runetek5.client.Client;
import com.jagex.game.runetek5.client.SkillConstants;
import com.jagex.game.runetek5.network.BufferedConnectionContext;
import com.jagex.game.runetek5.network.Connection;
import com.jagex.game.runetek5.network.packet.PacketDecoder;
import com.jagex.game.runetek5.network.packet.ServerProt;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

class PlayerStatsOracle {
    private static final int SNAPSHOT_BYTES = 444;
    private final BufferedConnectionContext context;
    private final ByteArrayOutputStream payloads = new ByteArrayOutputStream();
    private final ByteArrayOutputStream snapshots = new ByteArrayOutputStream();
    private final StringBuilder cases = new StringBuilder("order\tname\topcode\toffset\tlength\n");
    private final Set<String> names = new HashSet<>();
    private int count;

    private PlayerStatsOracle(BufferedConnectionContext context) {
        this.context = context;
    }

    private static final class PayloadConnection extends Connection {
        private final byte[] payload;
        private int consumed;
        private int reads;
        private int availabilityChecks;

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
            availabilityChecks++;
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

    private static byte[] snapshot(int consumed) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(SNAPSHOT_BYTES);
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(consumed);
            for (int value : Client.SKILL_XP) {
                output.writeInt(value);
            }
            for (int value : Client.SKILL_LEVEL_VISIBLE) {
                output.writeInt(value);
            }
            for (int value : Client.SKILL_LEVEL_ACTUAL) {
                output.writeInt(value);
            }
            output.writeInt(Client.RUN_ENERGY);
            output.writeInt(Client.RUN_WEIGHT);
            output.writeInt(Client.Companion.getSkillUpdateCycle());
            for (int value : Client.Companion.getRecentSkillUpdates()) {
                output.writeInt(value);
            }
        }
        require(bytes.size() == SNAPSHOT_BYTES, "Unexpected native snapshot shape");
        return bytes.toByteArray();
    }

    private void apply(String name, ServerProt packet, byte[] payload) throws IOException {
        require(names.add(name), "Duplicate case " + name);
        require(packet.getSize() == payload.length, "Payload disagrees with native registry");
        PayloadConnection connection = new PayloadConnection(payload);
        context.connection = connection;
        context.currentPacket = packet;
        context.currentPacketSize = payload.length;
        int readBefore = context.read;
        boolean decoded = PacketDecoder.decode(context);
        require(decoded, "Native decoder returned false for " + name);
        require(context.currentPacket == null, "Native decoder did not clear packet for " + name);
        require(context.lastPacket == packet, "Native decoder did not record packet for " + name);
        require(context.receivedBuffer.currentPosition == payload.length, "Native buffer consumption for " + name);
        require(connection.consumed == payload.length && connection.reads == 1, "Native connection consumption for " + name);
        require(connection.availabilityChecks == 1, "Native availability checks for " + name);
        require(context.read - readBefore == payload.length, "Native read accounting for " + name);
        require(context.pinger.address == null && context.pinger.ip == null, "Unexpected pinger target");
        cases.append(count).append('\t').append(name).append('\t').append(packet.getOpcode())
            .append('\t').append(payloads.size()).append('\t').append(payload.length).append('\n');
        payloads.write(payload);
        snapshots.write(snapshot(context.receivedBuffer.currentPosition));
        count++;
    }

    private void stat(String name, int skill, int xp, int visible) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(6);
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(xp);
            output.writeByte(skill);
            output.writeByte(visible);
        }
        apply(name, ServerProt.UPDATE_STAT, bytes.toByteArray());
    }

    private void energy(String name, int value) throws IOException {
        apply(name, ServerProt.RUN_ENERGY, new byte[] {(byte) value});
    }

    private void weight(String name, int value) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(2);
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeShort(value);
        }
        apply(name, ServerProt.PLAYER_WEIGHT, bytes.toByteArray());
    }

    private void generate() throws IOException {
        for (int skill = 0; skill < 25; skill++) {
            stat("initial-zero-skill-" + skill, skill, 0, 0);
        }
        for (int value = 0; value < 256; value++) {
            stat("visible-u8-" + value, 0, 0, value);
            energy("energy-u8-" + value, value);
        }
        int[] signedXp = {
            Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -16777216, -65536, -257, -256,
            -255, -129, -128, -127, -2, -1, 0, 1, 127, 128, 255, 256, 257,
            65535, 65536, 0x01020304, 0x10203040, Integer.MAX_VALUE - 1, Integer.MAX_VALUE
        };
        for (int skill : new int[] {0, 24}) {
            for (int index = 0; index < signedXp.length; index++) {
                stat("signed-xp-skill-" + skill + "-case-" + index, skill, signedXp[index], (index * 37) & 255);
            }
            for (int threshold = 0; threshold < SkillConstants.SKILL_XP_FOR_LEVEL.length; threshold++) {
                int xp = SkillConstants.SKILL_XP_FOR_LEVEL[threshold];
                stat("threshold-skill-" + skill + "-" + threshold + "-below", skill, xp - 1, threshold & 255);
                stat("threshold-skill-" + skill + "-" + threshold + "-equal", skill, xp, (threshold + 1) & 255);
                stat("threshold-skill-" + skill + "-" + threshold + "-above", skill, xp + 1, (threshold + 2) & 255);
            }
        }
        int[] weights = {
            -32768, -32767, -32513, -32512, -257, -256, -255, -129, -128, -127,
            -2, -1, 0, 1, 2, 127, 128, 129, 255, 256, 257, 0x0102, 0x0201,
            0x1234, 0x3412, 32766, 32767
        };
        for (int index = 0; index < weights.length; index++) {
            weight("signed-weight-case-" + index, weights[index]);
        }
        stat("repeat-first", 12, 42000, 42);
        stat("repeat-identical", 12, 42000, 42);
        stat("repeat-visible-only", 12, 42000, 255);
        stat("decrease-xp", 12, 2, 0);
        stat("decrease-identical", 12, 2, 0);
        stat("decrease-negative", 12, -1, 128);
        stat("restore-zero", 12, 0, 1);
        for (int index = 0; index < 97; index++) {
            stat("ring-wrap-" + index, (index * 7 + 3) % 25, index * 12345 - 500000, (index * 11) & 255);
            if (index % 16 == 0) {
                energy("ring-preserve-energy-" + index, index + 100);
                weight("ring-preserve-weight-" + index, index * 257 - 12000);
            }
        }
    }

    private static byte[] tables() throws IOException {
        require(SkillConstants.SKILL_MAX_LEVEL.length == 25, "Unexpected native skill caps length");
        require(SkillConstants.SKILL_XP_FOR_LEVEL.length == 120, "Unexpected native threshold length");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(580);
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            for (int value : SkillConstants.SKILL_MAX_LEVEL) {
                output.writeInt(value);
            }
            for (int value : SkillConstants.SKILL_XP_FOR_LEVEL) {
                output.writeInt(value);
            }
        }
        return bytes.toByteArray();
    }

    private static String registry() {
        StringBuilder result = new StringBuilder("name\topcode\tlength\n");
        ServerProt[] packets = {ServerProt.UPDATE_STAT, ServerProt.RUN_ENERGY, ServerProt.PLAYER_WEIGHT};
        int[] opcodes = {140, 64, 14};
        int[] lengths = {6, 1, 2};
        for (int index = 0; index < packets.length; index++) {
            ServerProt packet = packets[index];
            require(packet.getOpcode() == opcodes[index] && packet.getSize() == lengths[index], "Unexpected native registry entry");
            require(ServerProt.forId(packet.getOpcode()) == packet, "Native registry lookup mismatch");
            result.append(packet.name()).append('\t').append(packet.getOpcode()).append('\t').append(packet.getSize()).append('\n');
        }
        return result.toString();
    }

    public static void main(String[] args) throws Exception {
        require(args.length == 2, "Expected output directory and pinned jar");
        Path output = Path.of(args[0]);
        Path jar = Path.of(args[1]).toRealPath();
        require(Path.of(Client.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar), "Client was not loaded directly from pinned jar");
        BufferedConnectionContext context = null;
        try {
            Bot.ENABLED = false;
            require(Client.SKILL_XP.length == 25 && Client.SKILL_LEVEL_VISIBLE.length == 25 && Client.SKILL_LEVEL_ACTUAL.length == 25, "Unexpected native skills shape");
            require(Client.Companion.getRecentSkillUpdates().length == 32, "Unexpected native ring shape");
            byte[] initial = snapshot(0);
            require(Arrays.equals(initial, new byte[SNAPSHOT_BYTES]), "Native initial state was not zero");
            require(Client.GAME_CONNECTION_CONTEXT.connection == null && Client.LOBBY_CONNECTION_CONTEXT.connection == null, "Unexpected native connection");
            require(Client.GAME_CONNECTION_CONTEXT.pinger.address == null && Client.LOBBY_CONNECTION_CONTEXT.pinger.address == null, "Unexpected native ping address");
            Client.GAME_CONNECTION_CONTEXT.pinger.disable();
            Client.LOBBY_CONNECTION_CONTEXT.pinger.disable();
            context = new BufferedConnectionContext();
            context.pinger.disable();
            PlayerStatsOracle oracle = new PlayerStatsOracle(context);
            String nativeRegistry = registry();
            byte[] nativeTables = tables();
            oracle.generate();
            require(oracle.snapshots.size() == oracle.count * SNAPSHOT_BYTES, "Unexpected snapshots size");
            Files.write(output.resolve("player-stats-initial.native-values"), initial);
            Files.write(output.resolve("player-stats-tables.native-values"), nativeTables);
            Files.writeString(output.resolve("player-stats-registry.tsv"), nativeRegistry, StandardCharsets.US_ASCII);
            Files.write(output.resolve("player-stats-payloads.bin"), oracle.payloads.toByteArray());
            Files.write(output.resolve("player-stats-native-values.bin"), oracle.snapshots.toByteArray());
            Files.writeString(output.resolve("player-stats-cases.tsv"), oracle.cases, StandardCharsets.US_ASCII);
            System.out.println("Decoded " + oracle.count + " native player-stats packets; payload bytes=" + oracle.payloads.size() + "; snapshot bytes=" + oracle.snapshots.size());
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
