package com.jagex.game.runetek5.buffer;

import com.jagex.game.launch.Loader;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.staticmethods.StaticMethods;
import com.jagex.game.runetek5.tools.Hashing;
import com.jagex.game.runetek5.tools.Isaac;
import com.jagex.game.runetek5.utils.Utils;

import java.math.BigInteger;

public class Packet extends Node {
    public byte[] buffer;
    public int currentPosition;

    public Packet(byte[] bytes) {
        this.buffer = bytes;
        this.currentPosition = 0;
    }

    public Packet(int size) {
        this.buffer = CircularBuffer.createBuffer(size);
        this.currentPosition = 0;
    }

    public static int calculateRequiredSize(byte[] bytes, int startPos, CharSequence chars) {
        int length = chars.length();
        int pos = startPos;

        for (int i = 0; i < length; i++) {
            char c = chars.charAt(i);
            if (c <= 127) {
                bytes[pos++] = (byte) c;
            } else if (c <= 2047) {
                bytes[pos++] = (byte) (0xc0 | c >> 6);
                bytes[pos++] = (byte) (0x80 | c & 0x3f);
            } else {
                bytes[pos++] = (byte) (0xe0 | c >> 12);
                bytes[pos++] = (byte) (0x80 | c >> 6 & 0x3f);
                bytes[pos++] = (byte) (0x80 | c & 0x3f);
            }
        }

        return pos - startPos;
    }

    public static int write_string(CharSequence str, int length, byte[] buffer, int position) {
        for (int i = 0; i < length; i++) {
            char c = str.charAt(i);
            if ((c > 0 && c < 128) || (c >= 160 && c <= 255))
                buffer[i + position] = (byte) c;
            else {
                switch (c) {
                    case 8364: buffer[i + position] = -128; break;
                    case 8218: buffer[i + position] = -126; break;
                    case 402: buffer[i + position] = -125; break;
                    case 8222: buffer[i + position] = -124; break;
                    case 8230: buffer[i + position] = -123; break;
                    case 8224: buffer[i + position] = -122; break;
                    case 8225: buffer[i + position] = -121; break;
                    case 710: buffer[i + position] = -120; break;
                    case 8240: buffer[i + position] = -119; break;
                    case 352: buffer[i + position] = -118; break;
                    case 8249: buffer[i + position] = -117; break;
                    case 338: buffer[i + position] = -116; break;
                    case 381: buffer[i + position] = -114; break;
                    case 8216: buffer[i + position] = -111; break;
                    case 8217: buffer[i + position] = -110; break;
                    case 8220: buffer[i + position] = -109; break;
                    case 8221: buffer[i + position] = -108; break;
                    case 8226: buffer[i + position] = -107; break;
                    case 8211: buffer[i + position] = -106; break;
                    case 8212: buffer[i + position] = -105; break;
                    case 732: buffer[i + position] = -104; break;
                    case 8482: buffer[i + position] = -103; break;
                    case 353: buffer[i + position] = -102; break;
                    case 8250: buffer[i + position] = -101; break;
                    case 339: buffer[i + position] = -100; break;
                    case 382: buffer[i + position] = -98; break;
                    case 376: buffer[i + position] = -97; break;
                    default: buffer[i + position] = 63; break;
                }
            }
        }

        return length;
    }

    public void flush() {
        if (this.buffer != null)
            CircularBuffer.stash(this.buffer);
        this.buffer = null;
    }

    public void writeBytes(byte[] bytes, int offset, int length) {
        for (int i = offset; i < offset + length; i++)
            this.buffer[this.currentPosition++] = bytes[i];
    }

    public void readBytes(byte[] bytes, int offset, int length) {
        for (int i = offset; i < offset + length; i++)
            bytes[i] = this.buffer[this.currentPosition++];
    }

    public void writeByte(int value) {
        this.buffer[this.currentPosition++] = (byte) value;
    }

    public void writeByteInverse(int value) {
        writeByte(-value);
    }

    public void writeByteAdd(int value) {
        writeByte(value + 128);
    }

    public void writeByteSubtract(int value) {
        writeByte(128 - value);
    }

    public void writeShort(int value) {
        writeByte(value >> 8);
        writeByte(value);
    }

    public void writeShortAdd(int value) {
        writeByte(value >> 8);
        writeByteAdd(value);
    }

    public void writeShortLittle(int value) {
        writeByte(value);
        writeByte(value >> 8);
    }

    public void writeShortAddLittle(int value) {
        writeByteAdd(value);
        writeByte(value >> 8);
    }

    public void writeMedium(int value) {
        writeByte(value >> 16);
        writeByte(value >> 8);
        writeByte(value);
    }

    public void writeInt(int value) {
        writeByte(value >> 24);
        writeByte(value >> 16);
        writeByte(value >> 8);
        writeByte(value);
    }

    public void writeIntLittle(int value) {
        writeByte(value);
        writeByte(value >> 8);
        writeByte(value >> 16);
        writeByte(value >> 24);
    }

    public void writeIntMiddle(int value) {
        writeByte(value >> 8);
        writeByte(value);
        writeByte(value >> 24);
        writeByte(value >> 16);
    }

    public void writeIntInverseMiddle(int value) {
        writeByte(value >> 16);
        writeByte(value >> 24);
        writeByte(value);
        writeByte(value >> 8);
    }

    public void write40BitLong(long value) {
        writeByte((int) (value >> 32));
        writeInt((int) value);
    }

    public void writeLong(long value) {
        writeByte((int) (value >> 56));
        writeByte((int) (value >> 48));
        writeByte((int) (value >> 40));
        writeByte((int) (value >> 32));
        writeByte((int) (value >> 24));
        writeByte((int) (value >> 16));
        writeByte((int) (value >> 8));
        writeByte((int) value);
    }

    public void writeVariableLengthLong(long value, int size) {
        size--;
        if (size >= 0 && size <= 7) {
            for (int shift = size * 8; shift >= 0; shift -= 8)
                writeByte((int) (value >> shift));
        } else
            throw new IllegalArgumentException();
    }

    public void writeString(String string) {
        int nullPos = string.indexOf(0);
        if (nullPos >= 0) throw new IllegalArgumentException("String contains null character");
        this.currentPosition += write_string(string, string.length(), this.buffer, this.currentPosition);
        writeByte(0);
    }

    public void writeJagString(String string) {
        int nullPos = string.indexOf(0);
        if (nullPos >= 0) throw new IllegalArgumentException("String contains null character");
        writeByte(0);
        this.currentPosition += write_string(string, string.length(), this.buffer, this.currentPosition);
        writeByte(0);
    }

    public void writePathReference(CharSequence chars) {
        int length = chars.length();
        int size = 0;
        for (int i = 0; i < length; i++) {
            char c = chars.charAt(i);
            if (c <= 127)
                size++;
            else if (c <= 2047)
                size += 2;
            else
                size += 3;
        }

        writeByte(0);
        writeFlags(size);
        this.currentPosition += calculateRequiredSize(this.buffer, this.currentPosition, chars);
    }

    public void writeFlags(int flags) {
        if ((flags & -128) != 0) {
            if ((flags & -16384) != 0) {
                if ((flags & -2097152) != 0) {
                    if ((flags & -268435456) != 0) {
                        writeByte(flags >>> 28 | 0x80);
                    }
                    writeByte(flags >>> 21 | 0x80);
                }
                writeByte(flags >>> 14 | 0x80);
            }
            writeByte(flags >>> 7 | 0x80);
        }
        writeByte(flags & 0x7f);
    }

    public void writeSmart(int data) {
        if (data >= 0 && data < 128) {
            writeByte(data);
        } else {
            if (data < 0 || data >= 32768) {
                throw new IllegalArgumentException();
            }
            writeShort(data + 32768);
        }
    }

    public byte readByte() {
        return this.buffer[this.currentPosition++];
    }

    public byte readByteInverse() {
        return (byte) (-readByte());
    }

    public byte readByteAdd() {
        return (byte) (readByte() - 128);
    }

    public byte readByteSubtract() {
        return (byte) (128 - readByte());
    }

    public int readUByte() {
        return readByte() & 0xff;
    }

    public int readUByteInverse() {
        return -readByte() & 0xff;
    }

    public int readUByteAdd() {
        return (readByte() - 128) & 0xff;
    }

    public int readUByteSubtract() {
        return (128 - readByte()) & 0xff;
    }

    public int readShort() {
        int value = ((readByte() & 0xff) << 8) | (readByte() & 0xff);
        if (value > 32767)
            value -= 65536;
        return value;
    }

    public int readShortAdd() {
        int value = ((readByte() & 0xff) << 8) | ((readByte() - 128) & 0xff);
        if (value > 32767)
            value -= 65536;
        return value;
    }

    public int readShortLittle() {
        int value = (readByte() & 0xff) | ((readByte() & 0xff) << 8);
        if (value > 32767)
            value -= 65536;
        return value;
    }

    public int readShortAddLittle() {
        int value = ((readByte() - 128) & 0xff) | ((readByte() & 0xff) << 8);
        if (value > 32767)
            value -= 65536;
        return value;
    }

    public int readUShort() {
        return ((readByte() & 0xff) << 8) | (readByte() & 0xff);
    }

    public int readUShortAdd() {
        return ((readByte() & 0xff) << 8) | ((readByte() - 128) & 0xff);
    }

    public int readUShortLittle() {
        return (readByte() & 0xff) | ((readByte() & 0xff) << 8);
    }

    public int readUShortAddLittle() {
        return ((readByte() - 128) & 0xff) | ((readByte() & 0xff) << 8);
    }

    public int readMedium() {
        int value = ((readByte() & 0xff) << 16) | ((readByte() & 0xff) << 8) | (readByte() & 0xff);
        if (value > 8388607)
            value -= 16777216;
        return value;
    }

    public int readUMedium() {
        return ((readByte() & 0xff) << 16) | ((readByte() & 0xff) << 8) | (readByte() & 0xff);
    }

    public int readUMediumAlternateOrder() {
        return ((readByte() & 0xff) << 16) | (readByte() & 0xff) | ((readByte() & 0xff) << 8);
    }

    public int readInt() {
        return ((readByte() & 0xff) << 24) | ((readByte() & 0xff) << 16) | ((readByte() & 0xff) << 8) | (readByte() & 0xff);
    }

    public int readUIntLittle() {
        return (readByte() & 0xff) | ((readByte() & 0xff) << 8) | ((readByte() & 0xff) << 16) | ((readByte() & 0xff) << 24);
    }

    public int readUIntMiddle() {
        return ((readByte() & 0xff) << 8) | (readByte() & 0xff) | ((readByte() & 0xff) << 24) | ((readByte() & 0xff) << 16);
    }

    public int readUIntInverseMiddle() {
        return ((readByte() & 0xff) << 16) | ((readByte() & 0xff) << 24) | (readByte() & 0xff) | ((readByte() & 0xff) << 8);
    }

    public long readLong() {
        long high = readInt() & 0xffffffffL;
        long low = readInt() & 0xffffffffL;
        return (high << 32) | low;
    }

    public long read40BitLong() {
        long high = readUByte() & 0xffffffffL;
        long low = readInt() & 0xffffffffL;
        return (high << 32) | low;
    }

    public long readULongLittle() {
        long low = readUIntLittle() & 0xffffffffL;
        long high = readUIntLittle() & 0xffffffffL;
        return low + (high << 32);
    }

    public long bitpackLong(int size) {
        if (--size >= 0 && size <= 7) {
            int shift = size * 8;
            long value = 0L;
            while (shift >= 0) {
                value |= (readByte() & 0xffL) << shift;
                shift -= 8;
            }
            return value;
        }
        throw new IllegalArgumentException();
    }

    public String readString() {
        int start = this.currentPosition;
        while (readByte() != 0) {}
        int length = this.currentPosition - start - 1;
        return length == 0 ? "" : Utils.readString(this.buffer, start, length);
    }

    public String readNullString() {
        if (this.buffer[this.currentPosition] == 0) {
            this.currentPosition++;
            return null;
        }
        return readString();
    }

    public String readJagString() {
        byte firstByte = readByte();
        if (firstByte != 0) throw new IllegalStateException("First byte in JagString must be 0");
        int start = this.currentPosition;
        while (readByte() != 0) {}
        int length = this.currentPosition - start - 1;
        return length == 0 ? "" : Utils.readString(this.buffer, start, length);
    }

    public String readPathReference() {
        byte firstByte = readByte();
        if (firstByte != 0) throw new IllegalStateException("First byte must be 0");
        int length = readVarInt();
        if (length + this.currentPosition > this.buffer.length) throw new IllegalStateException("String too long");
        String result = StaticMethods.method4824(this.buffer, this.currentPosition, length);
        this.currentPosition += length;
        return result;
    }

    public int readSmart() {
        int first = this.buffer[this.currentPosition] & 0xff;
        return first < 128 ? readUByte() - 64 : readUShort() - 49152;
    }

    public int readUSmart() {
        int first = this.buffer[this.currentPosition] & 0xff;
        return first < 128 ? readUByte() : readUShort() - 32768;
    }

    public int readBigSmart() {
        if (this.buffer[this.currentPosition] < 0)
            return readInt() & 0x7fffffff;
        int value = readUShort();
        return value == 32767 ? -1 : value;
    }

    public int readUBigSmart() {
        return this.buffer[this.currentPosition] < 0 ? readInt() & 0x7fffffff : readUShort();
    }

    public int readSum() {
        int sum = 0;
        int value;
        do {
            value = readUSmart();
            sum += value;
        } while (value == 32767);

        return sum;
    }

    public int readVarInt() {
        int value = 0;
        byte b;
        do {
            b = readByte();
            value = (value | b & 0x7f) << 7;
        } while (b < 0);

        value = value >> 7;
        return value | b;
    }

    public void writeIndex(int length) {
        this.buffer[this.currentPosition - length - 1] = (byte) length;
    }

    public void writeIntLength(int length) {
        this.buffer[this.currentPosition - length - 4] = (byte) (length >> 24);
        this.buffer[this.currentPosition - length - 3] = (byte) (length >> 16);
        this.buffer[this.currentPosition - length - 2] = (byte) (length >> 8);
        this.buffer[this.currentPosition - length - 1] = (byte) length;
    }

    public void writeShortLength(int length) {
        this.buffer[this.currentPosition - length - 2] = (byte) (length >> 8);
        this.buffer[this.currentPosition - length - 1] = (byte) length;
    }

    public void encryptXtea(int[] keys, int start, int end) {
        int originalPos = this.currentPosition;
        this.currentPosition = start;
        int blocks = (end - start) / 8;

        for (int i = 0; i < blocks; i++) {
            int v0 = readInt();
            int v1 = readInt();
            int sum = 0;
            int delta = -1640531527;

            for (int j = 32; j > 0; j--) {
                v0 += (((v1 << 4) ^ (v1 >>> 5)) + v1) ^ (sum + keys[sum & 3]);
                sum += delta;
                v1 += (((v0 << 4) ^ (v0 >>> 5)) + v0) ^ (sum + keys[(sum >>> 11) & 3]);
            }

            this.currentPosition -= 8;
            writeInt(v0);
            writeInt(v1);
        }

        this.currentPosition = originalPos;
    }

    public void decryptFullXtea(int[] keys) {
        int blocks = this.currentPosition / 8;
        this.currentPosition = 0;

        for (int i = 0; i < blocks; i++) {
            int v0 = readInt();
            int v1 = readInt();
            int sum = -957401312;
            int delta = -1640531527;

            for (int j = 32; j > 0; j--) {
                v1 -= (((v0 << 4) ^ (v0 >>> 5)) + v0) ^ (sum + keys[(sum >>> 11) & 3]);
                sum -= delta;
                v0 -= (((v1 << 4) ^ (v1 >>> 5)) + v1) ^ (sum + keys[sum & 3]);
            }

            this.currentPosition -= 8;
            writeInt(v0);
            writeInt(v1);
        }
    }

    public void decryptXtea(int[] keys, int length) {
        int originalPos = this.currentPosition;
        this.currentPosition = 5;
        int blocks = (length - 5) / 8;

        for (int i = 0; i < blocks; i++) {
            int v0 = readInt();
            int v1 = readInt();
            int sum = -957401312;
            int delta = -1640531527;

            for (int j = 32; j > 0; j--) {
                v1 -= (((v0 << 4) ^ (v0 >>> 5)) + v0) ^ (sum + keys[(sum >>> 11) & 3]);
                sum -= delta;
                v0 -= (((v1 << 4) ^ (v1 >>> 5)) + v1) ^ (sum + keys[sum & 3]);
            }

            this.currentPosition -= 8;
            writeInt(v0);
            writeInt(v1);
        }

        this.currentPosition = originalPos;
    }

    public void applyRSA() {
        int length = this.currentPosition;
        this.currentPosition = 0;
        byte[] data = new byte[length];
        readBytes(data, 0, length);

        BigInteger bigInt = new BigInteger(data);
        BigInteger encryptedBigInt = bigInt.modPow(Loader.RSA_PUBLIC_EXPONENT, Loader.RSA_PUBLIC_MODULUS);
        byte[] encryptedData = encryptedBigInt.toByteArray();

        this.currentPosition = 0;
        writeShort(encryptedData.length);
        writeBytes(encryptedData, 0, encryptedData.length);
    }

    public int writeCrc(int start) {
        int crc = Hashing.getCrc32(this.buffer, start, this.currentPosition);
        writeInt(crc);
        return crc;
    }

    public boolean checkCrc() {
        this.currentPosition -= 4;
        int calculated = Hashing.getCrc32(this.buffer, 0, this.currentPosition);
        int expected = readInt();
        return calculated == expected;
    }

    public static class Bit extends Packet {
        private static final int[] BIT_MASKS = {
                0, 1, 3, 7, 15, 31, 63, 127, 255, 511, 1023, 2047, 4095,
                8191, 16383, 32767, 65535, 131071, 262143, 524287, 1048575,
                2097151, 4194303, 8388607, 16777215, 33554431, 67108863,
                134217727, 268435455, 536870911, 1073741823, Integer.MAX_VALUE, -1
        };

        private Isaac isaac;
        private int bitPosition;

        public Bit(int size) {
            super(size);
        }

        public void setIsaacCipher(Isaac cipher) {
            this.isaac = cipher;
        }

        public boolean nextIsSmart() {
            int value = this.buffer[this.currentPosition] - this.isaac.nextKey() & 0xff;
            return value >= 128;
        }

        public int readEncryptedSmart() {
            int value = this.buffer[this.currentPosition++] - this.isaac.getNextKey() & 0xff;
            return value < 128 ? value :
                    (value - 128 << 8) + (this.buffer[this.currentPosition++] - this.isaac.getNextKey() & 0xff);
        }

        public void readBytes(byte[] bytes, int length) {
            for (int i = 0; i < length; i++) {
                bytes[i] = (byte) (this.buffer[this.currentPosition++] - this.isaac.getNextKey());
            }
        }

        public void writeIsaacByte(int value) {
            this.buffer[this.currentPosition++] = (byte) (value + this.isaac.getNextKey());
        }

        public void initBitAccess() {
            this.bitPosition = this.currentPosition * 8;
        }

        public void finishBitAccess() {
            this.currentPosition = (this.bitPosition + 7) / 8;
        }

        public int readBits(int numBits) {
            int bytePos = this.bitPosition >> 3;
            int bitOffset = 8 - (this.bitPosition & 0x7);
            int value = 0;

            this.bitPosition += numBits;

            while (numBits > bitOffset) {
                value += (this.buffer[bytePos++] & BIT_MASKS[bitOffset]) << (numBits - bitOffset);
                numBits -= bitOffset;
                bitOffset = 8;
            }

            if (numBits == bitOffset) {
                value += this.buffer[bytePos] & BIT_MASKS[bitOffset];
            } else {
                value += (this.buffer[bytePos] >> (bitOffset - numBits)) & BIT_MASKS[numBits];
            }

            return value;
        }

        public int readableBits(int byteCount) {
            return byteCount * 8 - this.bitPosition;
        }
    }
}