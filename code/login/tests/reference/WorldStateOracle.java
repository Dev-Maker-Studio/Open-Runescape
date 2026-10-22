import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.scene.MapSize;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class WorldStateOracle {
    private static Path output;
    private static final List<String> cases = new ArrayList<>();

    private static final class Bits {
        private final byte[] bytes = new byte[65535];
        private int position;

        void write(int count, int value) {
            for (int bit = count - 1; bit >= 0; bit--) {
                bytes[position / 8] |= (byte)(((value >>> bit) & 1) << (7 - position % 8));
                position++;
            }
        }

        byte[] finish() {
            byte[] result = Arrays.copyOf(bytes, (position + 7) / 8);
            if (position % 8 != 0) result[result.length - 1] |= (byte)((1 << (8 - position % 8)) - 1);
            return result;
        }
    }

    private static byte[] used(Packet packet) {
        return Arrays.copyOf(packet.buffer, packet.currentPosition);
    }

    private static byte[] own(int index, int plane, int x, int y) {
        Bits bits = new Bits();
        bits.write(30, (plane << 28) | (x << 14) | y);
        for (int other = 1; other < 2048; other++) {
            if (other == index) continue;
            bits.write(18, ((other & 3) << 16) | (((other * 37) & 255) << 8) | ((other * 73 + 255) & 255));
        }
        return bits.finish();
    }

    private static int[] key(int index) {
        return new int[]{Integer.MIN_VALUE + index, Integer.MAX_VALUE - index, 0x89abcdef ^ index, -index};
    }

    private static void keys(Packet packet, int count) {
        for (int index = 0; index < count; index++) {
            for (int value : key(index)) packet.writeInt(value);
        }
    }

    private static byte[] join(byte[] first, byte[] second) {
        byte[] bytes = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, bytes, first.length, second.length);
        return bytes;
    }

    private static byte[] standard(int sizeId, int cx, int cy, int force) {
        Packet packet = new Packet(65535);
        packet.writeByte(sizeId);
        packet.writeShort(cx);
        packet.writeShort(cy);
        packet.writeByte(force);
        int radius = MapSize.get(sizeId).size >> 4;
        int count = 0;
        for (int x = (cx - radius) / 8; x <= (cx + radius) / 8; x++) {
            for (int y = (cy - radius) / 8; y <= (cy + radius) / 8; y++) count++;
        }
        keys(packet, count);
        return used(packet);
    }

    private static int descriptor(int plane, int x, int y, int rotation, int lowBit) {
        return (plane << 24) | (x << 14) | (y << 3) | (rotation << 1) | lowBit;
    }

    private static int[] chunks(int sizeId, int pattern) {
        int span = MapSize.get(sizeId).size >> 3;
        int[] descriptors = new int[4 * span * span];
        Arrays.fill(descriptors, -1);
        if (pattern == 0) return descriptors;
        if (pattern == 2) {
            for (int index = 0; index < descriptors.length; index++) {
                descriptors[index] = descriptor(index % 4, (index / 256) * 8 + index % 8, (index % 256) * 8 + index % 8, index % 4, index % 2);
            }
            return descriptors;
        }
        descriptors[0] = 0;
        descriptors[1] = 1;
        descriptors[span + 2] = descriptor(3, 1023, 2047, 3, 1);
        descriptors[span * span] = descriptor(1, 10, 19, 2, 1);
        descriptors[span * span + 1] = descriptor(2, 9, 16, 1, 0);
        descriptors[2 * span * span + 5] = descriptor(0, 33, 65, 1, 1);
        descriptors[descriptors.length - 1] = descriptor(3, 9, 16, 3, 1);
        return descriptors;
    }

    private static List<Integer> regions(int[] chunks) {
        List<Integer> regions = new ArrayList<>();
        for (int chunk : chunks) {
            if (chunk == -1) continue;
            int x = (chunk >> 14) & 1023;
            int y = (chunk >> 3) & 2047;
            int id = ((x / 8) << 8) + y / 8;
            if (!regions.contains(id)) regions.add(id);
        }
        return regions;
    }

    private static byte[] dynamic(int sizeId, int cx, int cy, int force, int type, int pattern) {
        Packet packet = new Packet(65535);
        packet.writeByteInverse(force);
        packet.writeByteSubtract(type);
        packet.writeByteAdd(sizeId);
        packet.writeShortAdd(cy);
        packet.writeShort(cx);
        int[] chunks = chunks(sizeId, pattern);
        Bits bits = new Bits();
        for (int descriptor : chunks) {
            bits.write(1, descriptor == -1 ? 0 : 1);
            if (descriptor != -1) bits.write(26, descriptor);
        }
        byte[] prefix = join(used(packet), bits.finish());
        Packet keyPacket = new Packet(65535);
        keys(keyPacket, regions(chunks).size());
        return join(prefix, used(keyPacket));
    }

    private static byte[] snapshot(int playerIndex, int opcode, byte[] payload) {
        Packet.Bit packet = new Packet.Bit(payload.length);
        packet.buffer = payload.clone();
        Packet result = new Packet(100000);
        packet.initBitAccess();
        int own = packet.readBits(30);
        int plane = own >>> 28;
        int x = (own >>> 14) & 16383;
        int y = own & 16383;
        result.writeInt(playerIndex);
        result.writeInt(plane);
        result.writeInt(x);
        result.writeInt(y);
        result.writeInt(own);
        result.writeInt((plane << 28) | ((x >> 6) << 14) | (y >> 6));
        result.writeInt(2046);
        for (int index = 1; index < 2048; index++) {
            if (index == playerIndex) continue;
            int coordinates = packet.readBits(18);
            int otherPlane = coordinates >>> 16;
            int otherX = (coordinates >>> 8) & 255;
            int otherY = coordinates & 255;
            result.writeInt(index);
            result.writeInt(otherPlane);
            result.writeInt(otherX);
            result.writeInt(otherY);
            result.writeInt((otherX << 14) + otherY + (otherPlane << 28));
        }
        packet.finishBitAccess();
        result.writeInt(packet.currentPosition);
        int sizeId;
        int cx;
        int cy;
        int force;
        int type = 0;
        int span = 0;
        int[] descriptors = new int[0];
        List<Integer> regions = new ArrayList<>();
        if (opcode == 85) {
            sizeId = packet.readUByte();
            cx = packet.readUShort();
            cy = packet.readUShort();
            force = packet.readUByte() == 1 ? 1 : 0;
            int radius = MapSize.get(sizeId).size >> 4;
            for (int rx = (cx - radius) / 8; rx <= (cx + radius) / 8; rx++) {
                for (int ry = (cy - radius) / 8; ry <= (cy + radius) / 8; ry++) regions.add((rx << 8) + ry);
            }
        } else if (opcode == 51) {
            force = (packet.readUByteInverse() & 1) != 0 ? 1 : 0;
            int rawType = packet.readUByteSubtract();
            type = rawType >= 1 && rawType <= 4 ? rawType : 1;
            sizeId = packet.readUByteAdd();
            cy = packet.readUShortAdd();
            cx = packet.readUShort();
            span = MapSize.get(sizeId).size >> 3;
            descriptors = new int[4 * span * span];
            packet.initBitAccess();
            for (int index = 0; index < descriptors.length; index++) {
                descriptors[index] = packet.readBits(1) == 1 ? packet.readBits(26) : -1;
            }
            packet.finishBitAccess();
            regions = regions(descriptors);
        } else {
            throw new IllegalArgumentException("unsupported opcode");
        }
        int size = MapSize.get(sizeId).size;
        int bx = (cx - (size >> 4)) * 8;
        int by = (cy - (size >> 4)) * 8;
        result.writeInt(sizeId);
        result.writeInt(size);
        result.writeInt(cx);
        result.writeInt(cy);
        result.writeInt(force);
        result.writeInt(type);
        result.writeInt(bx);
        result.writeInt(by);
        result.writeInt(span);
        result.writeInt(descriptors.length);
        for (int descriptor : descriptors) result.writeInt(descriptor);
        int keyCount = (packet.buffer.length - packet.currentPosition) / 16;
        if (keyCount != regions.size()) throw new IllegalStateException("fake geometry/key mismatch");
        result.writeInt(regions.size());
        for (int region : regions) {
            result.writeInt(region);
            for (int index = 0; index < 4; index++) result.writeInt(packet.readInt());
        }
        result.writeInt(plane);
        result.writeInt(x - bx);
        result.writeInt(y - by);
        result.writeInt(packet.currentPosition);
        if (packet.currentPosition != payload.length) throw new IllegalStateException("unexpected fake trailing data");
        return used(result);
    }

    private static void save(String name, int playerIndex, int opcode, int plane, int x, int y, byte[] map) throws Exception {
        byte[] payload = join(own(playerIndex, plane, x, y), map);
        Files.write(output.resolve("world-state-" + name + ".payload"), payload);
        Files.write(output.resolve("world-state-" + name + ".native-values"), snapshot(playerIndex, opcode, payload));
        cases.add(name + "\t" + playerIndex + "\t" + opcode);
    }

    public static void main(String[] args) throws Exception {
        output = Path.of(args[0]);
        Files.createDirectories(output);
        save("standard-104", 1, 85, 2, 3201, 3210, standard(0, 400, 401, 1));
        save("standard-120", 1024, 85, 3, 16383, 16383, standard(1, 1024, 2039, 0));
        save("standard-136", 2047, 85, 0, 0, 0, standard(2, 18, 27, 255));
        save("standard-168", 31, 85, 1, 8000, 8100, standard(3, 1017, 1023, 2));
        save("standard-72", 512, 85, 0, 0, 0, standard(4, 0, 0, 1));
        save("standard-zero-truncate", 2, 85, 0, 1, 2, standard(0, 0, 0, 3));
        save("standard-168-truncate", 2046, 85, 3, 0, 16383, standard(3, 3, 3, 1));
        save("standard-boundary", 127, 85, 2, 16383, 16383, standard(0, 2041, 2041, 1));
        for (int size = 0; size < 5; size++) {
            save("dynamic-size-" + size, 1 + size * 500, 51, size % 4, 4567 + size, 8901 + size,
                    dynamic(size, 257 + size, 511 - size, new int[]{1, 2, 3, 128, 255}[size], size == 4 ? 255 : size + 1, 1));
        }
        save("dynamic-empty", 2047, 51, 3, 16383, 0, dynamic(4, 65535, 0, 0, 0, 0));
        save("dynamic-all", 1023, 51, 2, 100, 200, dynamic(3, 0, 65535, 1, 4, 2));
        save("dynamic-unknown-five", 9, 51, 1, 1, 2, dynamic(0, 128, 129, 254, 5, 1));
        save("dynamic-unknown-zero", 2040, 51, 0, 0, 0, dynamic(4, 0, 0, 255, 0, 1));
        Files.writeString(output.resolve("world-state-cases.tsv"), String.join("\n", cases) + "\n");
        System.out.println("Generated " + cases.size() + " fake world-state cases using native Packet.Bit, byte transforms and MapSize; no scene, cache or account state");
    }
}
