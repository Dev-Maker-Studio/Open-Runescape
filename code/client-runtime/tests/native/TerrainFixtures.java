import com.jagex.game.runetek5.js5.Js5MasterIndex;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.js5.request.FileWorker;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.TreeMap;

public final class TerrainFixtures {
    static String name(int region) {
        return "m" + (region >> 8) + "_" + (region & 255);
    }

    static byte[] terrain(int height, boolean suffix) {
        byte[] bytes = new byte[16385 + (suffix ? 3 : 0)];
        bytes[0] = 1;
        bytes[1] = (byte) height;
        if (suffix) {
            bytes[16385] = (byte) 0xfe;
            bytes[16386] = (byte) 0x81;
            bytes[16387] = 0x22;
        }
        return bytes;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("output directory required");
        Path output = Path.of(args[0]);
        int[] regions = {0, 0x1234, 65535};
        int[] ids = {7, 32000, 62001};
        int[] heights = {1, 7, 255};
        TreeMap<Integer, byte[]> archives = new TreeMap<>();
        TreeMap<Integer, byte[]> plains = new TreeMap<>();
        for (int i = 0; i < regions.length; i++) {
            byte[] plain = terrain(heights[i], i == 2);
            plains.put(ids[i], plain);
            archives.put(ids[i], AssetChainFixtures.container(plain, true));
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream table = new DataOutputStream(bytes);
        table.writeByte(6);
        table.writeInt(727);
        table.writeByte(3);
        table.writeShort(ids.length);
        int prior = 0;
        for (int id : ids) {
            table.writeShort(id - prior);
            prior = id;
        }
        for (int region : regions) table.writeInt(Js5ResourceProvider.jaghash(name(region)));
        for (byte[] archive : archives.values()) table.writeInt(AssetChainFixtures.crc(archive));
        for (byte[] archive : archives.values()) table.write(AssetChainFixtures.whirlpool(archive));
        for (int id : ids) table.writeInt(44);
        for (int id : ids) table.writeShort(1);
        for (int id : ids) table.writeShort(0);
        for (int id : ids) table.writeInt(0);
        byte[] reference = AssetChainFixtures.container(bytes.toByteArray(), true);
        Js5MasterIndex index = new Js5MasterIndex(reference, AssetChainFixtures.crc(reference), AssetChainFixtures.whirlpool(reference));
        FileWorker worker = new FileWorker() {
            public Js5MasterIndex getMasterIndex() { return index; }
            public byte[] requestArchive(int id) { return archives.get(id); }
            public void prefetch(int id) {}
            public int getProgress(int id) { return 100; }
        };
        Js5ResourceProvider provider = new Js5ResourceProvider(worker, false, 0);
        StringBuilder report = new StringBuilder("{\"cases\":[");
        int[] cases = {0, 1, 255, 256, 257, 0x1234, 12850, 32767, 32768, 65280, 65534, 65535};
        for (int i = 0; i < cases.length; i++) {
            int region = cases[i];
            String name = name(region);
            int id = provider.getArchiveId(name);
            if (i != 0) report.append(',');
            report.append("{\"regionId\":").append(region)
                .append(",\"hash\":").append(Js5ResourceProvider.jaghash(name))
                .append(",\"archiveId\":").append(id).append('}');
        }
        report.append("],\"regions\":[");
        for (int i = 0; i < regions.length; i++) {
            int id = provider.getArchiveId(name(regions[i]));
            if (id != ids[i] || !Arrays.equals(provider.getFile(id, 0), plains.get(id))) {
                throw new IllegalStateException("native terrain name lookup or file split mismatch");
            }
            if (i != 0) report.append(',');
            report.append("{\"regionId\":").append(regions[i])
                .append(",\"archiveId\":").append(id)
                .append(",\"fileBytes\":").append(plains.get(id).length)
                .append(",\"height\":").append(heights[i]).append('}');
        }
        ByteArrayOutputStream versionBytes = new ByteArrayOutputStream();
        DataOutputStream versionData = new DataOutputStream(versionBytes);
        versionData.writeByte(6);
        versionData.write(new byte[5 * 72]);
        versionData.writeInt(AssetChainFixtures.crc(reference));
        versionData.writeInt(727);
        versionData.write(AssetChainFixtures.whirlpool(reference));
        byte[] signature = new byte[128];
        Arrays.fill(signature, (byte) 0x5a);
        versionData.write(signature);
        byte[] version = AssetChainFixtures.container(versionBytes.toByteArray(), false);
        ByteArrayOutputStream wire = new ByteArrayOutputStream();
        wire.write(new byte[109]);
        wire.write(AssetChainFixtures.reply(255, 255, version));
        wire.write(AssetChainFixtures.reply(255, 5, reference));
        for (int id : new int[] {62001, 32000, 7}) wire.write(AssetChainFixtures.reply(5, id, archives.get(id)));
        byte[] encoded = wire.toByteArray();
        Files.write(output.resolve("terrain-native.wire"), encoded);
        report.append("],\"wireSha256\":").append(AssetChainFixtures.quoted(AssetChainFixtures.hash(encoded))).append('}');
        System.out.println(report);
    }
}
