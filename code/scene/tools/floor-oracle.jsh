import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.flo.FloType;
import com.jagex.game.runetek5.config.flo.FloTypeList;
import com.jagex.game.runetek5.config.flu.FluType;
import com.jagex.game.runetek5.utils.ColorUtil;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import sun.misc.Unsafe;

FloTypeList newList() throws Exception {
    Field field = Unsafe.class.getDeclaredField("theUnsafe");
    field.setAccessible(true);
    Unsafe allocator = (Unsafe) field.get(null);
    FloTypeList list = (FloTypeList) allocator.allocateInstance(FloTypeList.class);
    list.lastLoadedOverlayId = -1200000007;
    return list;
}

void underlay(StringBuilder rows, String payload, String kind) {
    FluType value = new FluType();
    Packet packet = new Packet(HexFormat.of().parseHex(payload));
    value.decode(packet);
    rows.append(kind).append('\t').append(payload).append('\t').append(packet.currentPosition);
    for (int number : new int[]{value.rgb, value.texture, value.scale, value.shadowed ? 1 : 0, value.occlude ? 1 : 0, value.hue, value.saturation, value.lightness, value.hueMultiplier}) {
        rows.append('\t').append(number);
    }
    rows.append('\n');
}

void overlay(StringBuilder rows, int id, String payload, String kind) throws Exception {
    FloTypeList list = newList();
    FloType value = new FloType();
    value.overlayId = id;
    value.floTypeList = list;
    Packet packet = new Packet(HexFormat.of().parseHex(payload));
    value.decode(packet);
    value.postDecode();
    rows.append(kind).append('\t').append(Integer.toUnsignedString(id)).append('\t').append(payload).append('\t').append(packet.currentPosition);
    for (int number : new int[]{value.slot, value.tileColorHsl, value.texture, value.occlude ? 1 : 0, value.minimapColorHsl, value.textureScale, value.shadowed ? 1 : 0, value.blendsWithUnderlay ? 1 : 0, value.waterColor, value.waterFogDepth, value.waterIntensity, value.opcode20, value.unusedOpcode21, value.unusedOpcode22}) {
        rows.append('\t').append(number);
    }
    rows.append('\t').append(list.lastLoadedOverlayId).append('\n');
}

void color(StringBuilder rows, int rgb) {
    rows.append("rgb\t").append(Integer.toUnsignedString(rgb)).append('\t').append(ColorUtil.rgbToHsl24(rgb)).append('\n');
    underlay(rows, "01" + HexFormat.of().toHexDigits(rgb).substring(2) + "00", "underlay");
}

long hashInt(long hash, int value) {
    for (int shift = 0; shift < 32; shift += 8) {
        hash = (hash ^ ((value >>> shift) & 255)) * 0x100000001b3L;
    }
    return hash;
}

void colorHashes(StringBuilder rows) {
    FluType flu = new FluType();
    long rgbHash = 0xcbf29ce484222325L;
    long fluHash = 0xcbf29ce484222325L;
    for (int rgb = 0; rgb <= 0xffffff; rgb++) {
        rgbHash = hashInt(rgbHash, ColorUtil.rgbToHsl24(rgb));
        flu.calculateHsl16(rgb);
        fluHash = hashInt(fluHash, flu.hue);
        fluHash = hashInt(fluHash, flu.saturation);
        fluHash = hashInt(fluHash, flu.lightness);
        fluHash = hashInt(fluHash, flu.hueMultiplier);
    }
    rows.append("rgb-exhaustive\t16777216\t").append(HexFormat.of().toHexDigits(rgbHash)).append('\n');
    rows.append("underlay-exhaustive\t16777216\t").append(HexFormat.of().toHexDigits(fluHash)).append('\n');
}

void generate() throws Exception {
    StringBuilder rows = new StringBuilder();
    byte[] jar = Files.readAllBytes(Path.of(System.getProperty("oracle.jar")));
    rows.append("jar-sha256\t").append(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(jar))).append('\n');
    for (String payload : new String[]{"00", "0100000000", "01ffffff00", "02000000", "02000100", "02fffe00", "02ffff00", "03000000", "03000100", "03ffff00", "0400", "0500", "0404050500", "0112345602ffff030000040501abcdef02000103ffff00"}) {
        underlay(rows, payload, "underlay");
    }
    String[] overlayPayloads = {"00", "0100000000", "01ffffff00", "01ff00ff00", "020000", "02ff00", "03000000", "03000100", "03fffe00", "03ffff00", "0500", "0700000000", "07ffffff00", "07ff00ff00", "0800", "09000000", "09000100", "09ffff00", "0a00", "0b0000", "0bff00", "0c00", "0d00000000", "0dffffff00", "0e0000", "0eff00", "100000", "10ff00", "14000000", "14ffff00", "150000", "15ff00", "16000000", "16ffff00", "03000102ff00", "02ff03ffff00", "0100ff0002ff03000105070000ff0809ffff0a0bff0c0d1234560eff10ff14ffff15ff16ffff00", "0808080b010bff01ff00ff0100000007ff00ff07ffffff00"};
    for (int id : new int[]{0, 1, 255, 256, 65535, Integer.MAX_VALUE, Integer.MIN_VALUE, -1}) {
        for (String payload : overlayPayloads) {
            overlay(rows, id, payload, "overlay");
        }
    }
    int[] channels = {0, 1, 31, 63, 127, 128, 179, 180, 192, 193, 217, 218, 243, 244, 254, 255};
    for (int red : channels) {
        for (int green : channels) {
            for (int blue : channels) {
                color(rows, (red << 16) | (green << 8) | blue);
            }
        }
    }
    int seed = 727;
    for (int i = 0; i < 1024; i++) {
        seed = seed * 1664525 + 1013904223;
        color(rows, seed & 0xffffff);
    }
    for (int rgb : new int[]{-1, Integer.MIN_VALUE, Integer.MAX_VALUE, 0x12ff00ff}) {
        rows.append("rgb\t").append(Integer.toUnsignedString(rgb)).append('\t').append(ColorUtil.rgbToHsl24(rgb)).append('\n');
    }
    int[] hues = {Integer.MIN_VALUE, -1025, -257, -1, 0, 1, 3, 4, 63, 127, 255, 256, 1024, Integer.MAX_VALUE};
    int[] saturations = {Integer.MIN_VALUE, -1, 0, 1, 31, 32, 127, 255, 256, Integer.MAX_VALUE};
    int[] lightnesses = {Integer.MIN_VALUE, -1, 0, 1, 179, 180, 192, 193, 217, 218, 243, 244, 255, 256, Integer.MAX_VALUE};
    for (int hue : hues) {
        for (int saturation : saturations) {
            for (int lightness : lightnesses) {
                rows.append("pack\t").append(hue).append('\t').append(saturation).append('\t').append(lightness).append('\t').append(ColorUtil.hsl16to24(hue, saturation, lightness)).append('\n');
            }
        }
    }
    int[] colors = {Integer.MIN_VALUE, -2, -1, 0, 1, 127, 128, 1023, 1024, 65535, 0x12345678, Integer.MAX_VALUE};
    for (int colorA : colors) {
        for (int colorB : colors) {
            for (int blend : new int[]{Integer.MIN_VALUE, -1, 0, 1, 63, 64, 127, 128, 129, Integer.MAX_VALUE}) {
                rows.append("blend\t").append(colorA).append('\t').append(colorB).append('\t').append(blend).append('\t').append(ColorUtil.blend(colorA, colorB, blend)).append('\n');
            }
        }
    }
    underlay(rows, "0600", "underlay-relaxed");
    underlay(rows, "0004", "underlay-relaxed");
    overlay(rows, 727, "0400", "overlay-relaxed");
    overlay(rows, 727, "0f00", "overlay-relaxed");
    overlay(rows, 727, "0008", "overlay-relaxed");
    for (String payload : new String[]{"", "01", "0100", "010000", "02", "0200", "03", "0300", "04", "05"}) {
        try {
            new FluType().decode(new Packet(HexFormat.of().parseHex(payload)));
            throw new IllegalStateException("truncated underlay accepted");
        } catch (IndexOutOfBoundsException expected) {
            rows.append("underlay-truncated\t").append(payload).append('\t').append(expected.getClass().getSimpleName()).append('\n');
        }
    }
    for (String payload : new String[]{"", "01", "0100", "010000", "02", "03", "0300", "07", "0700", "070000", "08", "09", "0900", "0b", "0d", "0d00", "0d0000", "0e", "10", "14", "1400", "15", "16", "1600"}) {
        try {
            FloType value = new FloType();
            value.floTypeList = newList();
            value.decode(new Packet(HexFormat.of().parseHex(payload)));
            throw new IllegalStateException("truncated overlay accepted");
        } catch (IndexOutOfBoundsException expected) {
            rows.append("overlay-truncated\t").append(payload).append('\t').append(expected.getClass().getSimpleName()).append('\n');
        }
    }
    colorHashes(rows);
    Files.writeString(Path.of(System.getProperty("oracle.output")), rows);
    System.out.println("NATIVE_ORACLE_OK rows=" + rows.toString().lines().count());
}

try { generate(); } catch (Throwable error) { error.printStackTrace(); System.exit(1); }
/exit
