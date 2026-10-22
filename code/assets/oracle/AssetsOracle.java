import com.jagex.game.runetek5.js5.Js5Compression;
import com.jagex.game.runetek5.js5.Js5MasterIndex;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.js5.Whirlpool;
import com.jagex.game.runetek5.js5.request.FileWorker;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.GZIPOutputStream;

public final class AssetsOracle {
    static Path output;

    static byte[] container(int mode, byte[] plain) throws Exception {
        byte[] compressed;
        if (mode == 0) {
            compressed = plain;
        } else if (mode == 2) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
                gzip.write(plain);
            }
            compressed = bytes.toByteArray();
        } else {
            Path source = output.resolve(".bzip-input");
            Path encoded = output.resolve(".bzip-output");
            Files.write(source, plain);
            int status = new ProcessBuilder("bzip2", "-1c", source.toString())
                .redirectOutput(encoded.toFile()).start().waitFor();
            if (status != 0) throw new IOException("bzip2 encoding failed");
            byte[] standard = Files.readAllBytes(encoded);
            if (!Arrays.equals(Arrays.copyOf(standard, 4), new byte[] {66, 90, 104, 49})) {
                throw new IOException("unexpected BZip2 header");
            }
            compressed = Arrays.copyOfRange(standard, 4, standard.length);
            Files.delete(source);
            Files.delete(encoded);
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(bytes);
        data.writeByte(mode);
        data.writeInt(compressed.length);
        if (mode != 0) data.writeInt(plain.length);
        data.write(compressed);
        return bytes.toByteArray();
    }

    static int crc(byte[] bytes) {
        CRC32 crc = new CRC32();
        crc.update(bytes);
        return (int) crc.getValue();
    }

    static void smart(DataOutputStream data, int protocol, int value) throws Exception {
        if (protocol >= 7 && value >= 32768) data.writeInt(value | 0x80000000);
        else data.writeShort(value);
    }

    static byte[] table(int protocol, int flags) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(bytes);
        data.writeByte(protocol);
        if (protocol >= 6) data.writeInt(0xfedcba98);
        data.writeByte(flags);
        smart(data, protocol, 2);
        smart(data, protocol, 3);
        smart(data, protocol, 39997);
        if ((flags & 1) != 0) {
            data.writeInt(0x80000003);
            data.writeInt(0x80009c40);
        }
        data.writeInt(0x89abcdef);
        data.writeInt(0x10203040);
        if ((flags & 2) != 0) {
            data.write(Whirlpool.calculateDigest(new byte[] {3}, 0, 1));
            data.write(Whirlpool.calculateDigest(new byte[] {40}, 0, 1));
        }
        data.writeInt(-1);
        data.writeInt(123456789);
        smart(data, protocol, 3);
        smart(data, protocol, 1);
        smart(data, protocol, 0);
        smart(data, protocol, 7);
        smart(data, protocol, 39993);
        smart(data, protocol, 2);
        if ((flags & 1) != 0) {
            data.writeInt(0xfedc0000);
            data.writeInt(0xfedc0007);
            data.writeInt(0xfedc9c40);
            data.writeInt(0xfedc0002);
        }
        return bytes.toByteArray();
    }

    static byte[] snapshot(Js5MasterIndex index) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(bytes);
        data.writeInt(index.revision);
        data.writeInt(index.validArchiveCount);
        for (int id : index.validArchiveIds) {
            data.writeInt(id);
            data.writeInt(index.crcs[id]);
            data.writeInt(index.versions[id]);
            data.writeBoolean(index.nameHashes != null);
            if (index.nameHashes != null) data.writeInt(index.nameHashes[id]);
            data.writeBoolean(index.whirlpool != null);
            if (index.whirlpool != null) data.write(index.whirlpool[id]);
            data.writeInt(index.archiveFileCount[id]);
            for (int position = 0; position < index.archiveFileCount[id]; position++) {
                int fileId = index.validFileIds[id] == null ? position : index.validFileIds[id][position];
                data.writeInt(fileId);
                data.writeBoolean(index.fileNameHashes != null);
                if (index.fileNameHashes != null) data.writeInt(index.fileNameHashes[id][fileId]);
            }
        }
        return bytes.toByteArray();
    }

    static void compressedFixture(String name, int mode, byte[] plain) throws Exception {
        byte[] encoded = container(mode, plain);
        byte[] decoded = Js5Compression.decompress(encoded);
        if (!Arrays.equals(plain, decoded)) throw new IOException("native decode mismatch " + name);
        Files.write(output.resolve(name + ".container"), encoded);
        Files.write(output.resolve(name + ".native"), decoded);
        Files.write(output.resolve(name + ".whirlpool"), Whirlpool.calculateDigest(encoded, 0, encoded.length));
    }

    static byte[] archive() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(bytes);
        data.write(new byte[] {10, 11, 12, 20, 30, 31, 40, 41, 50, 51, 52, 53, 60, 61});
        for (int delta : new int[] {3, -2, 1, 2, 2, -2}) data.writeInt(delta);
        data.writeByte(2);
        return bytes.toByteArray();
    }

    public static void main(String[] args) throws Exception {
        output = Path.of(args[0]);
        Files.createDirectories(output);
        compressedFixture("raw", 0, new byte[] {74, 83, 53, 0, (byte) 128, (byte) 255});
        compressedFixture("gzip", 2, archive());
        byte[] large = new byte[210137];
        for (int i = 0; i < large.length; i++) large[i] = (byte) (i * 73 + i / 251);
        compressedFixture("bzip2-multiblock", 1, large);
        for (int mode : new int[] {0, 1, 2}) compressedFixture("empty-" + mode, mode, new byte[0]);
        for (int protocol = 5; protocol <= 7; protocol++) {
            for (int flags = 0; flags <= 3; flags++) {
                byte[] plain = table(protocol, flags);
                byte[] encoded = container(2, plain);
                Js5MasterIndex index = new Js5MasterIndex(encoded, crc(encoded), Whirlpool.calculateDigest(encoded, 0, encoded.length));
                String name = "table-" + protocol + "-" + flags;
                Files.write(output.resolve(name + ".payload"), plain);
                Files.write(output.resolve(name + ".native"), snapshot(index));
                if (protocol == 7 && flags == 3) {
                    final byte[] archiveContainer = container(2, archive());
                    FileWorker worker = new FileWorker() {
                        public Js5MasterIndex getMasterIndex() { return index; }
                        public byte[] requestArchive(int id) { return archiveContainer; }
                        public void prefetch(int id) {}
                        public int getProgress(int id) { return 100; }
                    };
                    Js5ResourceProvider provider = new Js5ResourceProvider(worker, false, 1);
                    if (!provider.ensureMasterIndexLoaded()) throw new IOException("native table not loaded");
                    provider.archives[3] = archiveContainer;
                    for (int id : new int[] {0, 7, 40000}) {
                        byte[] file = provider.getFile(3, id);
                        Files.write(output.resolve("archive-file-" + id + ".native"), file);
                    }
                }
            }
        }
        System.out.println("Native JS5 oracle completed: 6 containers, 12 tables, 3 split files");
    }
}
