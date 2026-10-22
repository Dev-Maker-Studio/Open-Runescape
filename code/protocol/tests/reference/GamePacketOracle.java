import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.network.packet.ClientProt;
import com.jagex.game.runetek5.network.packet.ServerProt;
import com.jagex.game.runetek5.tools.Isaac;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class GamePacketOracle {
    private record Spec(int ordinal, int wire, String name, int size, Boolean tick) {}
    private record Frame(Spec spec, byte[] payload) {}

    private static int[] seed() {
        return new int[] {1, -2, 0x12345678, Integer.MIN_VALUE};
    }

    private static Packet.Bit buffer(int capacity, Isaac cipher) {
        Packet.Bit packet = new Packet.Bit(0);
        packet.buffer = new byte[capacity];
        packet.setIsaacCipher(cipher);
        return packet;
    }

    private static void opcode(Packet.Bit packet, int wire, boolean client) {
        if (client && wire > 255) throw new IllegalArgumentException("client opcode exceeds one byte");
        if (!client && wire >= 128) packet.writeIsaacByte((wire >>> 8) + 128);
        packet.writeIsaacByte(wire);
    }

    private static int readOpcode(Packet.Bit packet, boolean client) {
        if (!client) return packet.readEncryptedSmart();
        byte[] value = new byte[1];
        packet.readBytes(value, 1);
        return value[0] & 255;
    }

    private static void generate(Path out, String name, List<Spec> specs, int unknown, boolean client) throws Exception {
        StringBuilder registry = new StringBuilder();
        for (Spec spec : specs) {
            registry.append(spec.ordinal()).append('\t').append(spec.wire()).append('\t')
                .append(spec.name()).append('\t').append(spec.size()).append('\t')
                .append(spec.tick() == null ? "-" : spec.tick()).append('\n');
        }
        Files.writeString(out.resolve(name + ".tsv"), registry, StandardCharsets.US_ASCII);
        int firstShort = specs.stream().filter(s -> s.size() == -2).findFirst().orElseThrow().ordinal();
        Isaac cipher = new Isaac(seed());
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        List<Frame> frames = new ArrayList<>();
        int[] byteLengths = {0, 1, 127, 255};
        int[] shortLengths = {0, 1, 256, 513};
        for (int round = 0; round < 4; round++) {
            for (Spec spec : specs) {
                int choice = (round + spec.ordinal()) % 4;
                int length = switch (spec.size()) {
                    case -1 -> byteLengths[choice];
                    case -2 -> round == 3 && spec.ordinal() == firstShort ? 65535 : shortLengths[choice];
                    default -> spec.size();
                };
                if (length < 0) throw new IllegalStateException("unsupported native size");
                byte[] payload = new byte[length];
                for (int i = 0; i < length; i++) payload[i] = (byte)(round * 37 + spec.ordinal() * 13 + i * 19);
                Packet.Bit packet = buffer(length + 4, cipher);
                opcode(packet, spec.wire(), client);
                if (spec.size() == -1) packet.writeByte(length);
                if (spec.size() == -2) packet.writeShort(length);
                packet.writeBytes(payload, 0, payload.length);
                stream.write(packet.buffer, 0, packet.currentPosition);
                frames.add(new Frame(spec, payload));
            }
        }
        byte[] wire = stream.toByteArray();
        Files.write(out.resolve(name + ".wire"), wire);
        Packet.Bit suffix = buffer(5, cipher);
        opcode(suffix, unknown, client);
        suffix.writeByte(255);
        suffix.writeByte(255);
        suffix.writeByte(255);
        byte[] unknownWire = Arrays.copyOf(suffix.buffer, suffix.currentPosition);
        Files.write(out.resolve(name + ".unknown.wire"), unknownWire);
        Packet.Bit reader = buffer(wire.length + unknownWire.length, new Isaac(seed()));
        System.arraycopy(wire, 0, reader.buffer, 0, wire.length);
        System.arraycopy(unknownWire, 0, reader.buffer, wire.length, unknownWire.length);
        StringBuilder decoded = new StringBuilder();
        for (int index = 0; index < frames.size(); index++) {
            Frame frame = frames.get(index);
            boolean smart = !client && reader.nextIsSmart();
            if (!client && reader.nextIsSmart() != smart) throw new AssertionError("peek consumed ISAAC");
            int actual = readOpcode(reader, client);
            if (actual != frame.spec().wire() || smart != (!client && actual >= 128)) throw new AssertionError("opcode mismatch");
            int length = switch (frame.spec().size()) {
                case -1 -> reader.readUByte();
                case -2 -> reader.readUShort();
                default -> frame.spec().size();
            };
            byte[] payload = new byte[length];
            reader.readBytes(payload, 0, length);
            if (!Arrays.equals(payload, frame.payload())) throw new AssertionError("payload mismatch");
            decoded.append(index).append('\t').append(actual).append('\t').append(length).append('\n');
        }
        if (reader.currentPosition != wire.length || readOpcode(reader, client) != unknown) {
            throw new AssertionError("unknown opcode mismatch");
        }
        Files.writeString(out.resolve(name + ".frames.tsv"), decoded, StandardCharsets.US_ASCII);
    }

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args[0]);
        Files.createDirectories(out);
        List<Spec> client = new ArrayList<>();
        for (ClientProt packet : ClientProt.values()) {
            client.add(new Spec(packet.ordinal(), packet.id, packet.name(), packet.size, packet.tickProcessed));
        }
        List<Spec> server = new ArrayList<>();
        for (ServerProt packet : ServerProt.values()) {
            if (ServerProt.forId(packet.getOpcode()) != packet) throw new AssertionError("native lookup mismatch");
            server.add(new Spec(packet.ordinal(), packet.getOpcode(), packet.name(), packet.getSize(), null));
        }
        if (client.stream().anyMatch(s -> s.wire() == 104) || ServerProt.forId(161) != null) {
            throw new AssertionError("unknown fixture opcode is registered");
        }
        generate(out, "game-client", client, 104, true);
        generate(out, "game-server", server, 161, false);
        Packet.Bit high = buffer(2, new Isaac(seed()));
        opcode(high, 128, true);
        opcode(high, 0, true);
        Files.write(out.resolve("game-client.high-unknown.wire"), high.buffer);
        high.currentPosition = 0;
        high.setIsaacCipher(new Isaac(seed()));
        if (readOpcode(high, true) != 128 || high.currentPosition != 1 || readOpcode(high, true) != 0) {
            throw new AssertionError("client opcode incorrectly treated as smart");
        }
    }
}
