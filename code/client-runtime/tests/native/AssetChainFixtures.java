import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Script;
import com.jagex.game.runetek5.js5.Js5Compression;
import com.jagex.game.runetek5.js5.Js5MasterIndex;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.js5.Whirlpool;
import com.jagex.game.runetek5.js5.request.FileWorker;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.TreeMap;
import java.util.zip.CRC32;
import java.util.zip.GZIPOutputStream;

public final class AssetChainFixtures {
    static int crc(byte[] bytes) {
        CRC32 crc = new CRC32();
        crc.update(bytes);
        return (int) crc.getValue();
    }

    static byte[] whirlpool(byte[] bytes) {
        return Whirlpool.calculateDigest(bytes, 0, bytes.length);
    }

    static byte[] container(byte[] plain, boolean gzip) throws Exception {
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        if (gzip) {
            try (GZIPOutputStream encoded = new GZIPOutputStream(payload)) {
                encoded.write(plain);
            }
        } else {
            payload.write(plain);
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(bytes);
        data.writeByte(gzip ? 2 : 0);
        data.writeInt(payload.size());
        if (gzip) data.writeInt(plain.length);
        data.write(payload.toByteArray());
        byte[] result = bytes.toByteArray();
        if (!Arrays.equals(Js5Compression.decompress(result), plain)) {
            throw new IllegalStateException("native container decode mismatch");
        }
        return result;
    }

    static byte[] reference(byte[] archive) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(bytes);
        data.writeByte(6);
        data.writeInt(7);
        data.writeByte(2);
        data.writeShort(1);
        data.writeShort(2163);
        data.writeInt(crc(archive));
        data.write(whirlpool(archive));
        data.writeInt(44);
        data.writeShort(1);
        data.writeShort(0);
        return container(bytes.toByteArray(), true);
    }

    static byte[] version(byte[] reference) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(bytes);
        data.writeByte(13);
        data.write(new byte[12 * 72]);
        data.writeInt(crc(reference));
        data.writeInt(7);
        data.write(whirlpool(reference));
        byte[] signature = new byte[128];
        Arrays.fill(signature, (byte) 0x5a);
        data.write(signature);
        return container(bytes.toByteArray(), false);
    }

    static byte[] reply(int index, int archive, byte[] container) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(bytes);
        data.writeByte(index);
        data.writeInt(archive);
        data.write(container, 0, 5);
        int position = 10;
        for (int i = 5; i < container.length; i++) {
            if (position == 512) {
                data.writeByte(255);
                position = 1;
            }
            data.writeByte(container[i]);
            position++;
        }
        return bytes.toByteArray();
    }

    static String hash(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    static String hash(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var input = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    static String quoted(String text) {
        StringBuilder out = new StringBuilder("\"");
        for (char value : text.toCharArray()) {
            if (value == '"' || value == '\\') out.append('\\').append(value);
            else if (value < 32) out.append(String.format("\\u%04x", (int) value));
            else out.append(value);
        }
        return out.append('"').toString();
    }

    static String scriptReport(CS2Script script) {
        TreeMap<String, Integer> opcodes = new TreeMap<>();
        for (var opcode : script.operations) opcodes.merge(opcode.name(), 1, Integer::sum);
        StringBuilder out = new StringBuilder("{");
        out.append("\"instructions\":").append(script.operations.length);
        out.append(",\"localInts\":").append(script.intLocalsCount);
        out.append(",\"localStrings\":").append(script.stringLocalsCount);
        out.append(",\"localLongs\":").append(script.longLocalsCount);
        out.append(",\"argumentInts\":").append(script.intArgsCount);
        out.append(",\"argumentStrings\":").append(script.stringArgsCount);
        out.append(",\"argumentLongs\":").append(script.longArgsCount);
        out.append(",\"switchTables\":").append(script.switchMaps == null ? 0 : script.switchMaps.length);
        out.append(",\"opcodes\":{");
        boolean first = true;
        for (var entry : opcodes.entrySet()) {
            if (!first) out.append(',');
            first = false;
            out.append(quoted(entry.getKey())).append(':').append(entry.getValue());
        }
        return out.append("}}").toString();
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2 || args.length > 3) {
            throw new IllegalArgumentException("<fixture-dir> <vm-flow.bin> [generator-source.java]");
        }
        Path output = Path.of(args[0]);
        Path input = Path.of(args[1]);
        Path generator = args.length == 3 ? Path.of(args[2]) : output.resolve("../native/AssetChainFixtures.java");
        Path jar = Path.of(Js5MasterIndex.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        if (!Files.isRegularFile(jar) || !Files.isRegularFile(generator)) {
            throw new IllegalArgumentException("existing native jar and generator source required");
        }
        if (Files.size(input) > 1024 * 1024) throw new IllegalArgumentException("script byte limit");
        byte[] plain = Files.readAllBytes(input);
        byte[] archive = container(plain, true);
        byte[] reference = reference(archive);
        byte[] version = version(reference);
        Js5MasterIndex index = new Js5MasterIndex(reference, crc(reference), whirlpool(reference));
        if (index.revision != 7 || index.validArchiveCount != 1 || index.validArchiveIds[0] != 2163
            || index.crcs[2163] != crc(archive) || index.versions[2163] != 44
            || !Arrays.equals(index.whirlpool[2163], whirlpool(archive))) {
            throw new IllegalStateException("native reference validation mismatch");
        }
        FileWorker worker = new FileWorker() {
            public Js5MasterIndex getMasterIndex() { return index; }
            public byte[] requestArchive(int id) { return archive; }
            public void prefetch(int id) {}
            public int getProgress(int id) { return 100; }
        };
        Js5ResourceProvider provider = new Js5ResourceProvider(worker, false, 1);
        if (!provider.ensureMasterIndexLoaded()) throw new IllegalStateException("native index unavailable");
        provider.archives[2163] = archive;
        byte[] nativeFile = provider.getFile(2163, 0);
        if (!Arrays.equals(plain, nativeFile)) throw new IllegalStateException("native archive split mismatch");
        CS2Script script = CS2Script.decodeScript(nativeFile);
        byte[] versionWire = reply(255, 255, version);
        byte[] referenceWire = reply(255, 12, reference);
        byte[] archiveWire = reply(12, 2163, archive);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write(new byte[109]);
        stream.write(versionWire);
        stream.write(referenceWire);
        stream.write(archiveWire);
        byte[] wire = stream.toByteArray();
        String report = "{\"revision\":727,\"subrevision\":1,\"prefetchCount\":27,\"index\":12,\"archive\":2163"
            + ",\"referenceRevision\":" + index.revision + ",\"archiveRevision\":" + index.versions[2163]
            + ",\"referenceCrcVerified\":true,\"referenceWhirlpoolVerified\":true,\"archiveCrcVerified\":true"
            + ",\"archiveWhirlpoolVerified\":true,\"authenticated\":false,\"containerBytes\":" + archive.length
            + ",\"expandedBytes\":" + plain.length + ",\"fileSlots\":" + provider.getArchiveFileCount(2163)
            + ",\"files\":[{\"id\":0,\"bytes\":" + nativeFile.length
            + ",\"script\":" + scriptReport(script) + "}]}";
        String provenance = "{\"source\":\"Synthetic gzip JS5 chain; no cache, network, account or client launch\""
            + ",\"nativeValidation\":[\"Js5Compression.decompress\",\"Js5MasterIndex\",\"Whirlpool.calculateDigest\",\"Js5ResourceProvider.getFile\",\"CS2Script.decodeScript\",\"Js5ResourceProvider.getArchiveFileCount\"]"
            + ",\"input\":" + quoted(input.getFileName().toString()) + ",\"inputSha256\":" + quoted(hash(plain))
            + ",\"wireSha256\":" + quoted(hash(wire)) + ",\"generatorSha256\":" + quoted(hash(generator))
            + ",\"jar\":" + quoted(jar.getFileName().toString()) + ",\"jarSha256\":" + quoted(hash(jar))
            + ",\"opaqueSignatureBytes\":128,\"signatureAuthenticated\":false,\"versionEntries\":13"
            + ",\"handshakeBytes\":109,\"versionWireBytes\":" + versionWire.length
            + ",\"referenceWireBytes\":" + referenceWire.length + ",\"archiveWireBytes\":" + archiveWire.length + "}";
        Files.createDirectories(output);
        Files.write(output.resolve("asset-chain.wire"), wire);
        Files.writeString(output.resolve("asset-chain-native.json"), "{\"report\":" + report + ",\"provenance\":" + provenance + "}\n");
        System.out.println("Native asset chain generated: " + wire.length + " wire bytes, " + script.operations.length + " instructions");
    }
}
