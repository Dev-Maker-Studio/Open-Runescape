import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Script;
import com.jagex.game.runetek5.js5.Js5MasterIndex;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.TreeMap;

public final class ScriptGraphFixtures {
    static void smart(DataOutputStream out, int value) throws Exception {
        if (value >= 32768) out.writeInt(value | 0x80000000);
        else out.writeShort(value);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("<output-dir> <cs2-fixtures-dir> <generator-source>");
        Path output = Path.of(args[0]);
        Path input = Path.of(args[1]);
        Path source = Path.of(args[2]);
        TreeMap<Integer, byte[]> plain = new TreeMap<>();
        plain.put(900001, Files.readAllBytes(input.resolve("vm-root.bin")));
        plain.put(900002, Files.readAllBytes(input.resolve("vm-child.bin")));
        TreeMap<Integer, byte[]> archives = new TreeMap<>();
        for (var entry : plain.entrySet()) archives.put(entry.getKey(), AssetChainFixtures.container(entry.getValue(), true));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(bytes);
        data.writeByte(7);
        data.writeInt(7);
        data.writeByte(2);
        smart(data, archives.size());
        int prior = 0;
        for (int id : archives.keySet()) { smart(data, id - prior); prior = id; }
        for (byte[] archive : archives.values()) data.writeInt(AssetChainFixtures.crc(archive));
        for (byte[] archive : archives.values()) data.write(AssetChainFixtures.whirlpool(archive));
        for (int id : archives.keySet()) data.writeInt(44);
        for (int id : archives.keySet()) smart(data, 1);
        for (int id : archives.keySet()) smart(data, 0);
        byte[] reference = AssetChainFixtures.container(bytes.toByteArray(), true);
        Js5MasterIndex index = new Js5MasterIndex(reference, AssetChainFixtures.crc(reference), AssetChainFixtures.whirlpool(reference));
        if (index.validArchiveCount != 2 || !Arrays.equals(index.validArchiveIds, new int[]{900001, 900002})) throw new IllegalStateException("native sparse archive identity mismatch");
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write(new byte[109]);
        stream.write(AssetChainFixtures.reply(255, 255, AssetChainFixtures.version(reference)));
        stream.write(AssetChainFixtures.reply(255, 12, reference));
        StringBuilder reports = new StringBuilder("[");
        boolean first = true;
        for (var entry : archives.entrySet()) {
            int id = entry.getKey();
            byte[] archive = entry.getValue();
            if (index.crcs[id] != AssetChainFixtures.crc(archive) || !Arrays.equals(index.whirlpool[id], AssetChainFixtures.whirlpool(archive))) throw new IllegalStateException("native archive metadata mismatch");
            CS2Script script = CS2Script.decodeScript(plain.get(id));
            if (!first) reports.append(',');
            first = false;
            reports.append("{\"id\":").append(id).append(",\"bytes\":").append(plain.get(id).length)
                .append(",\"sha256\":").append(AssetChainFixtures.quoted(AssetChainFixtures.hash(plain.get(id))))
                .append(",\"script\":").append(AssetChainFixtures.scriptReport(script)).append('}');
            stream.write(AssetChainFixtures.reply(12, id, archive));
        }
        reports.append(']');
        byte[] wire = stream.toByteArray();
        Path jar = Path.of(Js5MasterIndex.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        String provenance = "{\"source\":\"Synthetic gzip transitive CS2 chain; existing native reference and script decoders\""
            + ",\"wireSha256\":" + AssetChainFixtures.quoted(AssetChainFixtures.hash(wire))
            + ",\"generatorSha256\":" + AssetChainFixtures.quoted(AssetChainFixtures.hash(source))
            + ",\"helperSha256\":" + AssetChainFixtures.quoted(AssetChainFixtures.hash(source.resolveSibling("AssetChainFixtures.java")))
            + ",\"jarSha256\":" + AssetChainFixtures.quoted(AssetChainFixtures.hash(jar))
            + ",\"scripts\":" + reports + "}";
        Files.createDirectories(output);
        Files.write(output.resolve("script-graph.wire"), wire);
        Files.writeString(output.resolve("script-graph-native.json"), provenance + "\n");
        System.out.println("Native script graph generated: " + wire.length + " bytes, " + archives.size() + " scripts");
    }
}
