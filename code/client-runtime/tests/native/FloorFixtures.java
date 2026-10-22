import com.jagex.game.runetek5.config.flo.FloType;
import com.jagex.game.runetek5.config.flo.FloTypeList;
import com.jagex.game.runetek5.config.flu.FluType;
import com.jagex.game.runetek5.config.flu.FluTypeList;
import com.jagex.game.runetek5.js5.Js5MasterIndex;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.js5.request.FileWorker;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.TreeMap;

public final class FloorFixtures {
    static byte[] bytes(int... values) {
        byte[] bytes = new byte[values.length];
        for (int i = 0; i < values.length; i++) bytes[i] = (byte) values[i];
        return bytes;
    }

    static byte[] archive(TreeMap<Integer, byte[]> files) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (byte[] file : files.values()) output.write(file);
        if (files.size() > 1) {
            DataOutputStream data = new DataOutputStream(output);
            int prior = 0;
            for (byte[] file : files.values()) {
                data.writeInt(file.length - prior);
                prior = file.length;
            }
            data.writeByte(1);
        }
        return AssetChainFixtures.container(output.toByteArray(), true);
    }

    static String underlay(int id, boolean present, FluType value) {
        return "{\"id\":" + id + ",\"present\":" + present + ",\"rgb\":" + value.rgb
            + ",\"texture\":" + value.texture + ",\"scale\":" + value.scale
            + ",\"shadowed\":" + value.shadowed + ",\"occlude\":" + value.occlude
            + ",\"hue\":" + value.hue + ",\"saturation\":" + value.saturation
            + ",\"lightness\":" + value.lightness + ",\"hueMultiplier\":" + value.hueMultiplier + '}';
    }

    static String overlay(int id, boolean present, FloType value) {
        return "{\"id\":" + id + ",\"present\":" + present + ",\"slot\":" + value.slot
            + ",\"tileColorHsl\":" + value.tileColorHsl + ",\"texture\":" + value.texture
            + ",\"occlude\":" + value.occlude + ",\"minimapColorHsl\":" + value.minimapColorHsl
            + ",\"textureScale\":" + value.textureScale + ",\"shadowed\":" + value.shadowed
            + ",\"blendsWithUnderlay\":" + value.blendsWithUnderlay + ",\"waterColor\":" + value.waterColor
            + ",\"waterFogDepth\":" + value.waterFogDepth + ",\"waterIntensity\":" + value.waterIntensity
            + ",\"opcode20\":" + value.opcode20 + ",\"unusedOpcode21\":" + value.unusedOpcode21
            + ",\"unusedOpcode22\":" + value.unusedOpcode22 + ",\"overlayId\":" + value.overlayId
            + ",\"sawOpcode8\":" + (id == 1 || id == 5) + '}';
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("output directory required");
        Path output = Path.of(args[0]);
        TreeMap<Integer, TreeMap<Integer, byte[]>> definitions = new TreeMap<>();
        definitions.put(0, new TreeMap<>());
        definitions.get(0).put(0, bytes(0));
        definitions.put(1, new TreeMap<>());
        definitions.get(1).put(0, bytes(1, 0x80, 0x20, 0x10, 2, 0xff, 0xff, 3, 0, 7, 4, 5, 0));
        definitions.get(1).put(3, bytes(0));
        definitions.get(1).put(65535, bytes(1, 0, 0, 0, 0));
        definitions.put(4, new TreeMap<>());
        definitions.get(4).put(1, bytes(1, 0xff, 0, 0xff, 8, 0));
        definitions.get(4).put(5, bytes(8, 11, 255, 9, 0xff, 0xff, 0));
        definitions.put(9, new TreeMap<>());
        definitions.get(9).put(0, bytes(0));
        definitions.get(9).put(7, bytes(0));
        TreeMap<Integer, byte[]> archives = new TreeMap<>();
        for (var entry : definitions.entrySet()) archives.put(entry.getKey(), archive(entry.getValue()));
        ByteArrayOutputStream referenceBytes = new ByteArrayOutputStream();
        DataOutputStream referenceData = new DataOutputStream(referenceBytes);
        referenceData.writeByte(6);
        referenceData.writeInt(727);
        referenceData.writeByte(3);
        referenceData.writeShort(definitions.size());
        int prior = 0;
        for (int id : definitions.keySet()) {
            referenceData.writeShort(id - prior);
            prior = id;
        }
        for (int id : definitions.keySet()) referenceData.writeInt(0);
        for (byte[] archive : archives.values()) referenceData.writeInt(AssetChainFixtures.crc(archive));
        for (byte[] archive : archives.values()) referenceData.write(AssetChainFixtures.whirlpool(archive));
        for (int id : definitions.keySet()) referenceData.writeInt(44);
        for (var files : definitions.values()) referenceData.writeShort(files.size());
        for (var files : definitions.values()) {
            prior = 0;
            for (int id : files.keySet()) {
                referenceData.writeShort(id - prior);
                prior = id;
            }
        }
        for (var files : definitions.values()) for (int id : files.keySet()) referenceData.writeInt(0);
        byte[] reference = AssetChainFixtures.container(referenceBytes.toByteArray(), true);
        Js5MasterIndex index = new Js5MasterIndex(reference, AssetChainFixtures.crc(reference), AssetChainFixtures.whirlpool(reference));
        FileWorker worker = new FileWorker() {
            public Js5MasterIndex getMasterIndex() { return index; }
            public byte[] requestArchive(int id) { return archives.get(id); }
            public void prefetch(int id) {}
            public int getProgress(int id) { return 100; }
        };
        Js5ResourceProvider provider = new Js5ResourceProvider(worker, false, 0);
        for (var archive : definitions.entrySet()) for (var file : archive.getValue().entrySet()) {
            if (!Arrays.equals(provider.getFile(archive.getKey(), file.getKey()), file.getValue())) {
                throw new IllegalStateException("native floor archive file mismatch");
            }
        }
        FluTypeList underlays = new FluTypeList(provider);
        FloTypeList overlays = new FloTypeList(provider);
        StringBuilder report = new StringBuilder("{\"underlayFileSlots\":");
        report.append(provider.getArchiveFileCount(1)).append(",\"overlayFileSlots\":").append(provider.getArchiveFileCount(4));
        report.append(",\"underlays\":[");
        boolean first = true;
        for (int id : new int[] {0, 2, 3, 65535}) {
            if (!first) report.append(',');
            first = false;
            report.append(underlay(id, definitions.get(1).containsKey(id), underlays.list(id)));
        }
        report.append("],\"overlays\":[");
        first = true;
        for (int id : new int[] {0, 1, 5, 65535}) {
            if (!first) report.append(',');
            first = false;
            report.append(overlay(id, definitions.get(4).containsKey(id), overlays.list(id)));
        }
        FloTypeList lazy = new FloTypeList(provider);
        report.append("],\"nativeLazyOverlayIds\":[5,1,5,0,65535],\"nativeLazyOpcode8Trace\":[");
        first = true;
        for (int id : new int[] {5, 1, 5, 0, 65535}) {
            lazy.list(id);
            if (!first) report.append(',');
            first = false;
            report.append(lazy.lastLoadedOverlayId);
        }
        ByteArrayOutputStream versionBytes = new ByteArrayOutputStream();
        DataOutputStream versionData = new DataOutputStream(versionBytes);
        versionData.writeByte(3);
        versionData.write(new byte[2 * 72]);
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
        wire.write(AssetChainFixtures.reply(255, 2, reference));
        for (int id : new int[] {1, 4}) wire.write(AssetChainFixtures.reply(2, id, archives.get(id)));
        byte[] encoded = wire.toByteArray();
        Files.write(output.resolve("floors-native.wire"), encoded);
        report.append("],\"wireSha256\":").append(AssetChainFixtures.quoted(AssetChainFixtures.hash(encoded))).append('}');
        System.out.println(report);
    }
}
