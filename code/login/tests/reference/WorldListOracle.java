import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.client.login.LoginManager;
import com.jagex.game.runetek5.network.world.WorldDescriptor;
import com.jagex.game.runetek5.network.world.WorldList;
import com.jagex.game.runetek5.network.world.WorldMeta;
import com.jagex.game.runetek5.staticmethods.StaticMethods;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public final class WorldListOracle {
    private static Path output;

    private static byte[] used(Packet packet) {
        return Arrays.copyOf(packet.buffer, packet.currentPosition);
    }

    private static void save(String name, byte[] bytes) throws Exception {
        Files.write(output.resolve("world-list-" + name), bytes);
    }

    private static void reset() {
        WorldMeta.WORLD_LIST_BUFFER = null;
        WorldMeta.WORLD_LIST_SIZE = 0;
        WorldMeta.WORLD_LIST_SIZEPLUS1 = 0;
        WorldMeta.WORLD_LIST_DESCRIPTORS = null;
        WorldMeta.WORLDS_LOADED = false;
        WorldDescriptor.WORLD_LIST_START = 0;
        LoginManager.WORLDS = null;
        StaticMethods.WORLD_LIST_VERSION = 0;
    }

    private static byte[] snapshot() {
        Packet packet = new Packet(100000);
        packet.writeByte(WorldMeta.WORLDS_LOADED ? 1 : 0);
        if (WorldMeta.WORLDS_LOADED) packet.writeInt(StaticMethods.WORLD_LIST_VERSION);
        int locations = LoginManager.WORLDS == null ? 0 : LoginManager.WORLDS.length;
        packet.writeInt(locations);
        for (int index = 0; index < locations; index++) {
            WorldMeta location = LoginManager.WORLDS[index];
            packet.writeInt(location.countryId);
            packet.writeJagString(location.activity);
        }
        packet.writeInt(WorldDescriptor.WORLD_LIST_START);
        packet.writeInt(WorldMeta.WORLD_LIST_SIZEPLUS1);
        int slots = WorldMeta.WORLD_LIST_DESCRIPTORS == null ? 0 : WorldMeta.WORLD_LIST_DESCRIPTORS.length;
        packet.writeInt(slots);
        packet.writeInt(WorldMeta.WORLD_LIST_SIZE);
        for (int rawId = 0; rawId < slots; rawId++) {
            WorldDescriptor world = WorldMeta.WORLD_LIST_DESCRIPTORS[rawId];
            if (world == null) continue;
            packet.writeInt(rawId);
            packet.writeInt(world.worldNumber);
            packet.writeInt(world.worldIndex);
            packet.writeInt(world.flags);
            packet.writeJagString(world.unknown);
            packet.writeJagString(world.ipAddress);
            packet.writeByte(world.hasPort() ? 1 : 0);
            if (world.hasPort()) packet.writeInt(world.port);
            packet.writeInt(world.playerCount);
        }
        return used(packet);
    }

    private static void descriptor(Packet packet, int id, int location, int flags, String activity, String host, int port) {
        packet.writeSmart(id);
        packet.writeByte(location);
        packet.writeInt(flags);
        packet.writeJagString(activity);
        packet.writeJagString(host);
        if ((flags & 0x40000000) != 0) packet.writeInt(port);
    }

    private static byte[] full(int start, int port) {
        Packet packet = new Packet(20000);
        packet.writeByte(2);
        packet.writeByte(1);
        packet.writeSmart(2);
        packet.writeSmart(200);
        packet.writeJagString("OFFLINE_€_Œ");
        packet.writeSmart(1);
        packet.writeJagString("");
        packet.writeSmart(start);
        packet.writeSmart(start + 130);
        packet.writeSmart(2);
        descriptor(packet, 128, 1, 0x80000002, "FAKE_™_ÿ", "second.invalid", 0);
        descriptor(packet, 2, 0, 0x4000000d, "FAKE_ACTIVITY", "first.invalid", port);
        packet.writeInt(0x89abcdef);
        packet.writeSmart(128);
        packet.writeShort(65535);
        packet.writeSmart(2);
        packet.writeShort(321);
        return used(packet);
    }

    private static byte[] counts(int hasWorlds, int firstId, int firstCount, int secondId, int secondCount) {
        Packet packet = new Packet(128);
        packet.writeByte(2);
        packet.writeByte(hasWorlds);
        packet.writeSmart(firstId);
        packet.writeShort(firstCount);
        packet.writeSmart(secondId);
        packet.writeShort(secondCount);
        return used(packet);
    }

    private static byte[] payload(int refresh, byte[] chunk) {
        byte[] result = new byte[chunk.length + 1];
        result[0] = (byte)refresh;
        System.arraycopy(chunk, 0, result, 1, chunk.length);
        return result;
    }

    private static void decodePacket(byte[] payload) {
        WorldList.INSTANCE.decode(payload[0] == 1, Arrays.copyOfRange(payload, 1, payload.length));
    }

    private static void fullFixture(String name, byte[] data) throws Exception {
        reset();
        byte[] payload = payload(1, data);
        save(name + ".payload", payload);
        if (!WorldDescriptor.decodeWorldList(data)) throw new IllegalStateException("world-list mode");
        save(name + ".native-values", snapshot());
    }

    public static void main(String[] args) throws Exception {
        output = Path.of(args[0]);
        fullFixture("basic", full(0, 43594));
        byte[] basic = snapshot();
        byte[] undefined = full(0, 43594);
        for (int index = 0; index < undefined.length; index++) {
            if (index > 0 && undefined[index - 1] == '_' && (undefined[index] & 255) == 128) {
                undefined[index] = (byte)129;
                break;
            }
        }
        fullFixture("undefined-cp1252", undefined);
        reset();
        WorldDescriptor.decodeWorldList(full(0, 43594));
        byte[] update = counts(0, 2, 65534, 128, 42);
        save("counts.payload", payload(1, update));
        if (!WorldDescriptor.decodeWorldList(update)) throw new IllegalStateException("counts mode");
        save("counts.native-values", snapshot());
        byte[] noncanonical = counts(255, 128, 17, 2, 65535);
        save("noncanonical.payload", payload(1, noncanonical));
        if (!WorldDescriptor.decodeWorldList(noncanonical)) throw new IllegalStateException("noncanonical mode");
        save("noncanonical.native-values", snapshot());
        reset();
        WorldDescriptor.decodeWorldList(full(0, 43594));
        byte[] holes = counts(0, 1, 7, 128, 65535);
        save("sparse-counts.payload", payload(1, holes));
        WorldDescriptor.decodeWorldList(holes);
        save("sparse-counts.native-values", snapshot());
        fullFixture("nonzero", full(100, Integer.MIN_VALUE));
        Packet zeroSlots = new Packet(16);
        zeroSlots.writeByte(2);
        zeroSlots.writeByte(1);
        zeroSlots.writeSmart(0);
        zeroSlots.writeSmart(1);
        zeroSlots.writeSmart(0);
        zeroSlots.writeSmart(0);
        zeroSlots.writeInt(Integer.MIN_VALUE);
        fullFixture("zero-slots", used(zeroSlots));
        reset();
        byte[] empty = {2, 0};
        save("empty.payload", payload(1, empty));
        WorldDescriptor.decodeWorldList(empty);
        save("empty.native-values", snapshot());
        for (int pieces = 2; pieces <= 3; pieces++) {
            reset();
            byte[] bytes = full(0, 43594);
            for (int index = 0; index < pieces; index++) {
                int begin = index * bytes.length / pieces;
                int end = (index + 1) * bytes.length / pieces;
                byte[] chunk = payload(index == pieces - 1 ? 1 : 255, Arrays.copyOfRange(bytes, begin, end));
                save("split" + pieces + "-" + index + ".payload", chunk);
                decodePacket(chunk);
                if (index < pieces - 1 && WorldMeta.WORLDS_LOADED) throw new IllegalStateException("premature refresh");
            }
            if (!Arrays.equals(basic, snapshot())) throw new IllegalStateException("chunk snapshot mismatch");
            save("split" + pieces + ".native-values", snapshot());
        }
    }
}
