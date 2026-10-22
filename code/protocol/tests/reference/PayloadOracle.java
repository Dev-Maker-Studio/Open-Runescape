import com.jagex.game.runetek5.buffer.Packet;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;

public final class PayloadOracle {
    private static final HexFormat HEX = HexFormat.of();
    private static final String[][] READS = {
        {"readUByte", "readByte", "readUByteAdd", "readByteAdd", "readUByteInverse", "readByteInverse", "readUByteSubtract", "readByteSubtract"},
        {"readUShort", "readShort", "readUShortLittle", "readShortLittle", "readUShortAdd", "readShortAdd", "readUShortAddLittle", "readShortAddLittle", "readSmart", "readUSmart"},
        {"readUMedium", "readMedium"},
        {"readInt", "readUIntLittle", "readUIntMiddle", "readUIntInverseMiddle", "readBigSmart", "readUBigSmart"},
        {"readLong"}
    };
    private static final String[][] WRITES = {
        {"writeByte", "writeByteAdd", "writeByteInverse", "writeByteSubtract"},
        {"writeShort", "writeShortLittle", "writeShortAdd", "writeShortAddLittle"},
        {"writeMedium"},
        {"writeInt", "writeIntLittle", "writeIntMiddle", "writeIntInverseMiddle"},
        {"writeLong"}
    };
    private static final byte[][] PATTERNS = {
        HEX.parseHex("000000000000000000"), HEX.parseHex("ffffffffffffffffff"),
        HEX.parseHex("800000008000000080"), HEX.parseHex("7fffffff7fffffff7f"),
        HEX.parseHex("0123456789abcdef55"), HEX.parseHex("fedcba9876543210aa"),
        HEX.parseHex("aa55aa55aa55aa55aa")
    };

    private static Packet packet(byte[] input) { return new Packet(input.clone()); }

    private static long read(Packet packet, String method) throws Exception {
        return ((Number) Packet.class.getMethod(method).invoke(packet)).longValue();
    }

    private static void write(Packet packet, String method, long value) throws Exception {
        if (method.equals("writeLong")) Packet.class.getMethod(method, long.class).invoke(packet, value);
        else Packet.class.getMethod(method, int.class).invoke(packet, (int) value);
    }

    private static String utf16(String text) {
        if (text == null) return "-";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) result.append(String.format("%04x", (int) text.charAt(i)));
        return result.toString();
    }

    private static long hashByte(long hash, int value) { return (hash ^ (value & 255)) * 0x100000001b3L; }

    private static long hashRead(long hash, long value, int position) {
        for (int shift = 0; shift < 64; shift += 8) hash = hashByte(hash, (int) (value >>> shift));
        return hashByte(hash, position);
    }

    private static long hashWrite(long hash, Packet packet) {
        for (int i = 0; i < packet.currentPosition; i++) hash = hashByte(hash, packet.buffer[i]);
        return hashByte(hash, packet.currentPosition);
    }

    public static void main(String[] args) throws Exception {
        StringBuilder rows = new StringBuilder();
        for (String[] group : READS) for (String method : group) {
            for (byte[] bytes : PATTERNS) {
                Packet packet = packet(bytes);
                long value = read(packet, method);
                rows.append("R\t").append(method).append('\t').append(HEX.formatHex(bytes)).append('\t')
                    .append(value).append('\t').append(packet.currentPosition).append('\n');
            }
        }
        long[] values = {0, 1, 63, 64, 127, 128, 255, 256, 32767, 32768, 65535, 65536,
            0x7fffff, 0x800000, 0xffffff, 0x12345678, 0x7fffffff, 0x80000000L, 0xffffffffL,
            -1, -128, -32768, -8388608, Integer.MIN_VALUE, 0x0123456789abcdefL, Long.MIN_VALUE, Long.MAX_VALUE};
        for (int group = 0; group < WRITES.length; group++) for (String method : WRITES[group]) {
            for (long value : values) {
                long limit = switch (group) { case 0 -> 255; case 1 -> 65535; case 2 -> 0xffffff; case 3 -> 0xffffffffL; default -> Long.MAX_VALUE; };
                long min = switch (group) { case 0 -> -128; case 1 -> -32768; case 2 -> -8388608; case 3 -> Integer.MIN_VALUE; default -> Long.MIN_VALUE; };
                if (value < min || value > limit) continue;
                Packet packet = new Packet(new byte[16]);
                write(packet, method, value);
                rows.append("W\t").append(method).append('\t').append(value).append('\t')
                    .append(HEX.formatHex(packet.buffer, 0, packet.currentPosition)).append('\t').append(packet.currentPosition).append('\n');
            }
        }
        for (int value : new int[] {0, 1, 63, 64, 127, 128, 255, 256, 16383, 16384, 32766, 32767}) {
            Packet packet = new Packet(new byte[2]);
            packet.writeSmart(value);
            rows.append("W\twriteSmart\t").append(value).append('\t')
                .append(HEX.formatHex(packet.buffer, 0, packet.currentPosition)).append('\t').append(packet.currentPosition).append('\n');
        }
        for (String method : new String[] {"readBigSmart", "readUBigSmart", "readSmart", "readUSmart"}) {
            for (String hex : new String[] {"00000000", "007f0000", "00800000", "7ffe0000", "7fff0000", "80000000", "80000001", "bfff0000", "c0000000", "ffff0000", "ffffffff"}) {
                Packet packet = packet(HEX.parseHex(hex));
                long value = read(packet, method);
                rows.append("R\t").append(method).append('\t').append(hex).append('\t')
                    .append(value).append('\t').append(packet.currentPosition).append('\n');
            }
        }
        for (byte[] bytes : PATTERNS) for (int offset = 0; offset < 8; offset++) for (int count = 1; count <= 32; count++) {
            Packet.Bit packet = new Packet.Bit(0);
            packet.buffer = bytes.clone();
            packet.currentPosition = 1;
            packet.initBitAccess();
            if (offset != 0) packet.readBits(offset);
            int value = packet.readBits(count);
            if (packet.currentPosition != 1) throw new AssertionError("bit access changed byte position");
            packet.finishBitAccess();
            rows.append("B\t").append(HEX.formatHex(bytes)).append("\t1\t").append(offset).append('\t')
                .append(count).append('\t').append(value).append('\t').append(packet.currentPosition).append('\n');
        }
        String[] texts = {"", "RuneScape", "\u0001\u007f ÿ", "€‚ƒ„…†‡ˆ‰Š‹ŒŽ‘’“”•–—˜™š›œžŸ", "\u0080\u0081\u009fĀ中", "😀𝄞?"};
        for (String text : texts) for (String method : new String[] {"writeString", "writeJagString"}) {
            Packet packet = new Packet(new byte[256]);
            Packet.class.getMethod(method, String.class).invoke(packet, text);
            rows.append("S\t").append(method).append('\t').append(utf16(text)).append('\t')
                .append(HEX.formatHex(packet.buffer, 0, packet.currentPosition)).append('\n');
        }
        for (int value = 0; value <= 255; value++) for (String method : new String[] {"readString", "readNullString", "readJagString"}) {
            byte[] bytes = method.equals("readJagString") ? new byte[] {0, (byte) value, 0, 85} : new byte[] {(byte) value, 0, 85};
            Packet packet = packet(bytes);
            String text = (String) Packet.class.getMethod(method).invoke(packet);
            rows.append("T\t").append(method).append('\t').append(HEX.formatHex(bytes)).append('\t')
                .append(utf16(text)).append('\t').append(packet.currentPosition).append('\n');
        }
        for (int group = 0; group < 2; group++) {
            int size = group + 1;
            int limit = 1 << (size * 8);
            for (String method : READS[group]) {
                long hash = 0xcbf29ce484222325L;
                Method nativeMethod = Packet.class.getMethod(method);
                for (int value = 0; value < limit; value++) {
                    byte[] bytes = size == 1 ? new byte[] {(byte) value} : new byte[] {(byte) (value >>> 8), (byte) value};
                    Packet packet = packet(bytes);
                    hash = hashRead(hash, ((Number) nativeMethod.invoke(packet)).longValue(), packet.currentPosition);
                }
                rows.append("H\tR\t").append(method).append('\t').append(Long.toUnsignedString(hash, 16)).append('\n');
            }
            for (String method : WRITES[group]) {
                long hash = 0xcbf29ce484222325L;
                Method nativeMethod = Packet.class.getMethod(method, int.class);
                for (int value = 0; value < limit; value++) {
                    Packet packet = new Packet(new byte[size]);
                    nativeMethod.invoke(packet, value);
                    hash = hashWrite(hash, packet);
                }
                rows.append("H\tW\t").append(method).append('\t').append(Long.toUnsignedString(hash, 16)).append('\n');
            }
        }
        long hash = 0xcbf29ce484222325L;
        for (int value = 0; value < 32768; value++) {
            Packet packet = new Packet(new byte[2]);
            packet.writeSmart(value);
            hash = hashWrite(hash, packet);
        }
        rows.append("H\tW\twriteSmart\t").append(Long.toUnsignedString(hash, 16)).append('\n');
        hash = 0xcbf29ce484222325L;
        for (int unit = 1; unit <= 65535; unit++) {
            if (unit >= 0xd800 && unit <= 0xdfff) continue;
            Packet packet = new Packet(new byte[2]);
            packet.writeString(String.valueOf((char) unit));
            hash = hashWrite(hash, packet);
        }
        rows.append("H\tS\twriteString\t").append(Long.toUnsignedString(hash, 16)).append('\n');
        Path out = Path.of(args[0]);
        Files.createDirectories(out);
        Files.writeString(out.resolve("payload-vectors.tsv"), rows, StandardCharsets.US_ASCII);
    }
}
