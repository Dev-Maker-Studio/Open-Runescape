import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.tools.Isaac;
import com.jagex.game.runetek5.systeminfo.SystemInfo;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public final class LoginOracle {
    private static Path output;
    private static final int[] SEED = {1, -2, 0x12345678, Integer.MIN_VALUE};

    private static void save(String name, byte[] bytes) throws Exception {
        Files.write(output.resolve(name), bytes);
    }

    private static byte[] used(Packet packet) {
        return Arrays.copyOf(packet.buffer, packet.currentPosition);
    }

    private static byte[] pattern(int length, int multiplier, int addend) {
        byte[] bytes = new byte[length];
        for (int index = 0; index < length; index++) bytes[index] = (byte)(index * multiplier + addend);
        return bytes;
    }

    private static byte[] rsa(String name, byte[] plain) throws Exception {
        Packet packet = new Packet(1024);
        packet.writeBytes(plain, 0, plain.length);
        packet.applyRSA();
        save(name + ".plain", plain);
        save(name + ".rsa", used(packet));
        return used(packet);
    }

    private static void lobby(String name, boolean tokenIdentity, boolean emptyTokens) throws Exception {
        Packet secure = new Packet(518);
        secure.writeByte(10);
        for (int seed : SEED) secure.writeInt(seed);
        secure.writeLong(0L);
        secure.writeString("OFFLINE_ONLY_€");
        secure.writeLong(0xfedcba9876543210L);
        secure.writeLong(0x0123456789abcdefL);
        save(name + ".secure", used(secure));
        secure.applyRSA();
        Packet request = new Packet(4096);
        request.writeByte(19);
        request.writeShort(0);
        int payloadStart = request.currentPosition;
        request.writeInt(727);
        request.writeInt(1);
        request.writeBytes(secure.buffer, 0, secure.currentPosition);
        int xteaStart = request.currentPosition;
        request.writeByte(tokenIdentity ? 0 : 1);
        if (tokenIdentity) request.writeLong(0x8877665544332211L);
        else request.writeString("offline_Œ_727");
        request.writeByte(0);
        request.writeByte(2);
        byte[] random24 = pattern(24, 7, 3);
        request.writeBytes(random24, 0, random24.length);
        request.writeString(emptyTokens ? "" : "FAKE_AFFILIATE_™");
        byte[] preferences = pattern(35, 11, 27);
        request.writeByte(preferences.length);
        request.writeBytes(preferences, 0, preferences.length);
        request.writeString(emptyTokens ? "" : "FAKE_LOGIN_€");
        request.writeInt(0x89abcdef);
        request.writeInt(0x76543210);
        request.writeString(emptyTokens ? "" : "FAKE_JS5_ÿ");
        for (int index = 0; index < 36; index++) request.writeInt(index * 0x1234567 + Integer.MIN_VALUE);
        save(name + ".xtea", Arrays.copyOfRange(request.buffer, xteaStart, request.currentPosition));
        request.encryptXtea(SEED, xteaStart, request.currentPosition);
        request.writeShortLength(request.currentPosition - payloadStart);
        save(name + ".wire", used(request));
    }

    private static void field(SystemInfo info, String name, Object value) throws Exception {
        java.lang.reflect.Field field = SystemInfo.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(info, value);
    }

    private static byte[] machine() throws Exception {
        java.lang.reflect.Constructor<?> constructor = sun.reflect.ReflectionFactory.getReflectionFactory()
            .newConstructorForSerialization(SystemInfo.class, Object.class.getDeclaredConstructor());
        SystemInfo info = (SystemInfo)constructor.newInstance();
        field(info, "operatingSystemType", 3);
        field(info, "x64os", true);
        field(info, "osVendor", 9);
        field(info, "javaVersionType", 4);
        field(info, "javaBuild", 17);
        field(info, "javaSubBuild", 2);
        field(info, "javaUpdate", 3);
        field(info, "idk", false);
        field(info, "maxMem", 4096);
        field(info, "processors", 8);
        field(info, "ram", 0xabcdef);
        field(info, "cpuClock", 4200);
        field(info, "gpuModel", "FAKE_GPU_€");
        field(info, "gpuDriverVersion", "FAKE_DRIVER_™");
        field(info, "dwDirectxVersion", "12.1");
        field(info, "dxLetterVersion", "A");
        field(info, "dxDriverMonth", 10);
        field(info, "dxDriverYear", 2026);
        field(info, "cpuType", "FAKE_CPU");
        field(info, "cpuData", "OFFLINE_CPU_Œ");
        field(info, "cpuCores", 16);
        field(info, "cpuFamilyId", 254);
        field(info, "cpuFeatureFlags", new int[] {0x89abcdef, 0x12345678, Integer.MIN_VALUE});
        field(info, "cpuModelInfo", 0xfedcba98);
        Packet encoded = new Packet(info.encode());
        info.writeMachineInformation(encoded);
        if (encoded.currentPosition != encoded.buffer.length) throw new IllegalStateException("native machine length");
        save("machine-v6.bytes", used(encoded));
        return used(encoded);
    }

    private static void world(String name, boolean reconnecting, boolean tokenIdentity, String supplementary) throws Exception {
        Packet secure = new Packet(518);
        secure.writeByte(10);
        for (int seed : SEED) secure.writeInt(seed);
        secure.writeLong(0L);
        secure.writeString("FAKE_LOBBY_ISSUED_TOKEN_727");
        secure.writeLong(0xfedcba9876543210L);
        secure.writeLong(0x0123456789abcdefL);
        save(name + ".secure", used(secure));
        secure.applyRSA();
        Packet request = new Packet(4096);
        request.writeByte(16);
        request.writeShort(0);
        int start = request.currentPosition;
        request.writeInt(727);
        request.writeInt(1);
        request.writeByte(reconnecting ? 1 : 0);
        request.writeBytes(secure.buffer, 0, secure.currentPosition);
        int xteaStart = request.currentPosition;
        request.writeByte(tokenIdentity ? 0 : 1);
        if (tokenIdentity) request.writeLong(0x8877665544332211L);
        else request.writeString("offline_Œ_727");
        request.writeByte(2);
        request.writeShort(1280);
        request.writeShort(720);
        request.writeByte(4);
        byte[] random = pattern(24, 7, 3);
        request.writeBytes(random, 0, random.length);
        request.writeString("FAKE_AFFILIATE_™");
        request.writeInt(0x89abcdef);
        byte[] preferences = pattern(35, 11, 27);
        request.writeByte(preferences.length);
        request.writeBytes(preferences, 0, preferences.length);
        byte[] machine = machine();
        request.writeBytes(machine, 0, machine.length);
        request.writeInt(0xfedcba98);
        request.writeLong(0x8877665544332211L);
        request.writeString("FAKE_CLIENT_LOGIN_€");
        request.writeByte(supplementary == null ? 0 : 1);
        if (supplementary != null) request.writeString(supplementary);
        request.writeByte(1);
        request.writeByte(0);
        request.writeByte(1);
        request.writeByte(7);
        request.writeInt(0x76543210);
        request.writeString("FAKE_WORLD_JS5_™");
        request.writeByte(1);
        request.writeInt(42);
        for (int index = 0; index < 36; index++) request.writeInt(index * 0x1234567 + Integer.MIN_VALUE);
        save(name + ".xtea", Arrays.copyOfRange(request.buffer, xteaStart, request.currentPosition));
        request.encryptXtea(SEED, xteaStart, request.currentPosition);
        request.writeShortLength(request.currentPosition - start);
        save(name + ".wire", used(request));
    }

    private static void success(String name, Packet payload) throws Exception {
        if (payload.currentPosition > 255) throw new IllegalStateException("native success length");
        save(name + ".payload", used(payload));
        Packet wire = new Packet(257);
        wire.writeByte(2);
        wire.writeByte(payload.currentPosition);
        wire.writeBytes(payload.buffer, 0, payload.currentPosition);
        save(name + ".wire", used(wire));
    }

    private static void lobbySuccess(String name, boolean sentinelWorld, boolean noncanonicalBooleans) throws Exception {
        Packet packet = new Packet(255);
        packet.writeByte(2);
        packet.writeByte(1);
        packet.writeByte(noncanonicalBooleans ? 2 : 1);
        packet.writeMedium(0xfedcba);
        packet.writeByte(254);
        packet.writeByte(0);
        packet.writeByte(1);
        packet.writeLong(Long.MAX_VALUE);
        packet.write40BitLong(0xfedcba9876L);
        packet.writeByte(0x83);
        packet.writeInt(0x89abcdef);
        packet.writeByte(noncanonicalBooleans ? 255 : 1);
        packet.writeInt(0x76543210);
        packet.writeShort(0xfedc);
        packet.writeShort(42);
        packet.writeShort(0x4321);
        packet.writeInt(0xc0000201);
        packet.writeString("FAKE_LOBBY_ISSUED_TOKEN_727");
        packet.writeByte(3);
        packet.writeShort(0x1234);
        packet.writeShort(0x5678);
        packet.writeByte(1);
        packet.writeJagString("offline_Œ_727");
        packet.writeByte(5);
        packet.writeInt(0x87654321);
        packet.writeByte(0);
        packet.writeShort(sentinelWorld ? 65535 : 42);
        packet.writeJagString("offline.invalid");
        success(name, packet);
        int length = packet.currentPosition;
        packet.currentPosition = 0;
        Packet decoded = new Packet(1024);
        decoded.writeByte(packet.readUByte());
        decoded.writeByte(packet.readUByte());
        decoded.writeByte(packet.readUByte() == 1 ? 1 : 0);
        decoded.writeInt(packet.readMedium());
        decoded.writeInt((byte)packet.readUByte());
        decoded.writeByte(packet.readUByte() == 1 ? 1 : 0);
        decoded.writeByte(packet.readUByte() == 1 ? 1 : 0);
        decoded.writeLong(packet.readLong());
        decoded.writeLong(packet.read40BitLong());
        decoded.writeByte(packet.readUByte());
        decoded.writeInt(packet.readInt());
        decoded.writeByte(packet.readUByte() == 1 ? 1 : 0);
        decoded.writeInt(packet.readInt());
        decoded.writeShort(packet.readUShort());
        decoded.writeShort(packet.readUShort());
        decoded.writeShort(packet.readUShort());
        decoded.writeInt(packet.readInt());
        decoded.writeString(packet.readString());
        decoded.writeByte(packet.readUByte());
        decoded.writeShort(packet.readUShort());
        decoded.writeShort(packet.readUShort());
        decoded.writeByte(packet.readUByte() == 1 ? 1 : 0);
        decoded.writeJagString(packet.readJagString());
        decoded.writeByte(packet.readUByte());
        decoded.writeInt(packet.readInt());
        decoded.writeByte(packet.readUByte() == 1 ? 1 : 0);
        int worldId = packet.readUShort();
        decoded.writeInt(worldId == 65535 ? -1 : worldId);
        decoded.writeJagString(packet.readJagString());
        if (packet.currentPosition != length) throw new IllegalStateException("native lobby decode consumed length");
        save(name + ".native-values", used(decoded));
    }

    private static void worldSuccess(String name, String owner, boolean noncanonicalBooleans) throws Exception {
        Packet packet = new Packet(255);
        packet.writeByte(2);
        packet.writeByte(1);
        packet.writeByte(noncanonicalBooleans ? 2 : 1);
        packet.writeByte(0);
        packet.writeByte(1);
        packet.writeByte(0);
        packet.writeShort(0xfedc);
        packet.writeByte(noncanonicalBooleans ? 255 : 1);
        packet.writeMedium(0x80abcd);
        packet.writeByte(1);
        packet.writeString(owner);
        success(name, packet);
        int length = packet.currentPosition;
        packet.currentPosition = 0;
        Packet decoded = new Packet(1024);
        decoded.writeByte(packet.readUByte());
        decoded.writeByte(packet.readUByte());
        for (int index = 0; index < 4; index++) decoded.writeByte(packet.readUByte() == 1 ? 1 : 0);
        decoded.writeShort(packet.readUShort());
        decoded.writeByte(packet.readUByte() == 1 ? 1 : 0);
        decoded.writeInt(packet.readMedium());
        decoded.writeByte(packet.readUByte() == 1 ? 1 : 0);
        decoded.writeString(packet.readString());
        if (packet.currentPosition != length) throw new IllegalStateException("native world decode consumed length");
        save(name + ".native-values", used(decoded));
    }

    public static void main(String[] args) throws Exception {
        output = Path.of(args[0]);
        Files.createDirectories(output);
        world("world-normal", false, false, "FAKE_EXTRA_ÿ");
        world("world-reconnecting", true, true, null);
        world("world-empty-supplement", false, false, "");
        lobbySuccess("lobby-success", false, false);
        lobbySuccess("lobby-success-sentinel", true, true);
        worldSuccess("world-success", "FAKE_MAP_OWNER_€", false);
        worldSuccess("world-success-noncanonical", "", true);
        worldSuccess("world-success-maximum", "M".repeat(241), false);
        Packet handshake = new Packet(1);
        handshake.writeByte(14);
        save("handshake.wire", used(handshake));
        byte[] input = pattern(2059, 37, 11);
        save("xtea.input", input);
        for (boolean zero : new boolean[] {false, true}) {
            int[] seed = zero ? new int[4] : SEED;
            Packet packet = new Packet(input.clone());
            packet.currentPosition = input.length;
            packet.encryptXtea(seed, 3, 2054);
            save(zero ? "xtea-zero.encrypted" : "xtea-mixed.encrypted", packet.buffer);
            Packet full = new Packet(input.clone());
            full.currentPosition = input.length;
            full.encryptXtea(seed, 0, input.length);
            save(zero ? "xtea-zero-full.encrypted" : "xtea-mixed-full.encrypted", full.buffer);
            full.currentPosition = input.length;
            full.decryptFullXtea(seed);
            if (!Arrays.equals(full.buffer, input)) throw new IllegalStateException("native XTEA round trip");
        }
        rsa("rsa-zero", new byte[] {0});
        rsa("rsa-one", new byte[] {1});
        rsa("rsa-negative", new byte[] {(byte)0x80});
        rsa("rsa-minus-one", new byte[] {(byte)0xff});
        rsa("rsa-positive", new byte[] {0, (byte)0x80});
        rsa("rsa-maximum", pattern(518, 37, 10));
        for (int value = 2; value < 65536; value++) {
            Packet packet = new Packet(256);
            byte[] plain = new byte[] {0, (byte)(value >>> 8), (byte)value};
            packet.writeBytes(plain, 0, plain.length);
            packet.applyRSA();
            if (packet.currentPosition == 131 && packet.buffer[2] == 0) {
                rsa("rsa-sign-padding", plain);
                break;
            }
        }
        StringBuilder text = new StringBuilder();
        for (int value = 1; value < 128; value++) text.append((char)value);
        text.append("€‚ƒ„…†‡ˆ‰Š‹ŒŽ‘’“”•–—˜™š›œžŸ");
        for (int value = 160; value < 256; value++) text.append((char)value);
        Packet string = new Packet(1024);
        string.writeString(text.toString());
        save("cp1252.string", used(string));
        Packet jag = new Packet(1024);
        jag.writeJagString(text.toString());
        save("cp1252.jag", used(jag));
        string.currentPosition = 0;
        jag.currentPosition = 0;
        if (!string.readString().equals(text.toString()) || !jag.readJagString().equals(text.toString())) {
            throw new IllegalStateException("native strings round trip");
        }
        lobby("lobby-username", false, false);
        lobby("lobby-token", true, false);
        lobby("lobby-empty-tokens", false, true);
        for (boolean incoming : new boolean[] {false, true}) {
            int[] seed = SEED.clone();
            if (incoming) for (int index = 0; index < 4; index++) seed[index] += 50;
            Isaac cipher = new Isaac(seed);
            Packet words = new Packet(520 * 4);
            for (int index = 0; index < 520; index++) words.writeInt(cipher.getNextKey());
            save(incoming ? "isaac-incoming.words" : "isaac-outgoing.words", used(words));
        }
    }
}
