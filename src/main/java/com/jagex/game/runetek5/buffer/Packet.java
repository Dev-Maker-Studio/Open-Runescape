package com.jagex.game.runetek5.buffer;

import com.jagex.game.launch.Loader;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.staticmethods.StaticMethods;
import com.jagex.game.runetek5.tools.Hashing;
import com.jagex.game.runetek5.tools.Isaac;
import com.jagex.game.runetek5.utils.Utils;

import java.math.BigInteger;

/**
 * [J]
 * @author Steven
 *
 */
public class Packet extends Node {

    public byte[] buffer;
    public int currentPosition;

    public Packet(byte[] bytes_1) {
        this.buffer = bytes_1;
        this.currentPosition = 0;
    }

    public Packet(int size) {
        this.buffer = CircularBuffer.createBuffer(size);
        this.currentPosition = 0;
    }

    public static int calculateRequiredSize(byte[] bytes_0, int i_1, CharSequence charsequence_2) {
        int i_4 = charsequence_2.length();
        int i_5 = i_1;
        for (int i_6 = 0; i_6 < i_4; i_6++) {
            char var_7 = charsequence_2.charAt(i_6);
            if (var_7 <= 127) {
                bytes_0[i_5++] = (byte) var_7;
            } else if (var_7 <= 2047) {
                bytes_0[i_5++] = (byte) (0xc0 | var_7 >> 6);
                bytes_0[i_5++] = (byte) (0x80 | var_7 & 0x3f);
            } else {
                bytes_0[i_5++] = (byte) (0xe0 | var_7 >> 12);
                bytes_0[i_5++] = (byte) (0x80 | var_7 >> 6 & 0x3f);
                bytes_0[i_5++] = (byte) (0x80 | var_7 & 0x3f);
            }
        }
        return i_5 - i_1;
    }

    public void flush() {
        if (this.buffer != null) {
            CircularBuffer.stash(this.buffer);
        }
        this.buffer = null;
    }

    public void writeByte(int i_1) {
        this.buffer[++this.currentPosition - 1] = (byte) i_1;
    }

    public void writeIndex(int i_1) {
        this.buffer[this.currentPosition - i_1 - 1] = (byte) i_1;
    }

    public void writeLEShort(int i_1) {
        this.buffer[++this.currentPosition - 1] = (byte) i_1;
        this.buffer[++this.currentPosition - 1] = (byte) (i_1 >> 8);
    }

    public void writeInt(int i_1) {
        this.buffer[++this.currentPosition - 1] = (byte) (i_1 >> 24);
        this.buffer[++this.currentPosition - 1] = (byte) (i_1 >> 16);
        this.buffer[++this.currentPosition - 1] = (byte) (i_1 >> 8);
        this.buffer[++this.currentPosition - 1] = (byte) i_1;
    }

    public void writeShortLE(int i_1) {
        this.buffer[++this.currentPosition - 1] = (byte) i_1;
        this.buffer[++this.currentPosition - 1] = (byte) (i_1 >> 8);
    }

    public void method13068(long long_1, int i_3) {
        --i_3;
        if (i_3 >= 0 && i_3 <= 7) {
            for (int i_5 = i_3 * 8; i_5 >= 0; i_5 -= 8) {
                this.buffer[++this.currentPosition - 1] = (byte) ((int) (long_1 >> i_5));
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    public void writeJagString(String jagString) {
        int stringEndSignal = jagString.indexOf(0);
        if (stringEndSignal >= 0) {
            throw new IllegalArgumentException("");
        } else {
            this.buffer[++this.currentPosition - 1] = 0;
            this.currentPosition += Packet.write_string(jagString, jagString.length(), this.buffer, this.currentPosition);
            this.buffer[++this.currentPosition - 1] = 0;
        }
    }

    public void writePathReference(CharSequence charsequence) {
        int characterCount = charsequence.length();
        int i_4 = 0;
        for (int characterIndex = 0; characterIndex < characterCount; characterIndex++) {
            char character = charsequence.charAt(characterIndex);
            if (character <= 127) {
                ++i_4;
            } else if (character <= 2047) {
                i_4 += 2;
            } else {
                i_4 += 3;
            }
        }
        this.buffer[++this.currentPosition - 1] = 0;
        writeFlags(i_4);
        this.currentPosition += calculateRequiredSize(this.buffer, this.currentPosition, charsequence);
    }

    public void writeBytes(byte[] bytes_1, int i_2, int i_3) {
        for (int i_4 = i_2; i_4 < i_3 + i_2; i_4++) {
            this.buffer[++this.currentPosition - 1] = bytes_1[i_4];
        }
    }

    public void method13073(int i_1) {
        this.buffer[this.currentPosition - i_1 - 4] = (byte) (i_1 >> 24);
        this.buffer[this.currentPosition - i_1 - 3] = (byte) (i_1 >> 16);
        this.buffer[this.currentPosition - i_1 - 2] = (byte) (i_1 >> 8);
        this.buffer[this.currentPosition - i_1 - 1] = (byte) i_1;
    }

    public int readUnsignedShort128() {
        this.currentPosition += 2;
        return (this.buffer[this.currentPosition - 1] - 128 & 0xff) + ((this.buffer[this.currentPosition - 2] & 0xff) << 8);
    }

    public void writeByte128(int i_1) {
        this.buffer[++this.currentPosition - 1] = (byte) (i_1 + 128);
    }

    public void writeSmartShort(int data) {
        if (data >= 0 && data < 128) {
            writeByte(data);
        } else {
            if (data < 0 || data >= 32768) {
                throw new IllegalArgumentException();
            }
            writeShort(data + 32768);
        }
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

    public int readUnsignedByte() {
        return this.buffer[++this.currentPosition - 1] & 0xff;
    }

    public int readUnsignedShort() {
        this.currentPosition += 2;
        return (this.buffer[this.currentPosition - 1] & 0xff) + ((this.buffer[this.currentPosition - 2] & 0xff) << 8);
    }

    public int readShort() {
        this.currentPosition += 2;
        int i_2 = (this.buffer[this.currentPosition - 1] & 0xff) + ((this.buffer[this.currentPosition - 2] & 0xff) << 8);
        if (i_2 > 32767) {
            i_2 -= 65536;
        }
        return i_2;
    }

    public int read24BitUnsignedInteger() {
        this.currentPosition += 3;
        return ((this.buffer[this.currentPosition - 3] & 0xff) << 16) + (this.buffer[this.currentPosition - 1] & 0xff) + ((this.buffer[this.currentPosition - 2] & 0xff) << 8);
    }

    public int read24BitInteger() {
        this.currentPosition += 3;
        int i_2 = ((this.buffer[this.currentPosition - 3] & 0xff) << 16) + (this.buffer[this.currentPosition - 1] & 0xff) + ((this.buffer[this.currentPosition - 2] & 0xff) << 8);
        if (i_2 > 8388607) {
            i_2 -= 16777216;
        }
        return i_2;
    }

    public int g4() {
        this.currentPosition += 4;
        return (this.buffer[this.currentPosition - 4] & 0xff) + ((this.buffer[this.currentPosition - 3] & 0xff) << 8) + ((this.buffer[this.currentPosition - 2] & 0xff) << 16) + ((this.buffer[this.currentPosition - 1] & 0xff) << 24);
    }
    
    public int readInt() {
        this.currentPosition += 4;
        return ((this.buffer[this.currentPosition - 3] & 0xff) << 16) + (this.buffer[this.currentPosition - 1] & 0xff) + ((this.buffer[this.currentPosition - 2] & 0xff) << 8) + ((this.buffer[this.currentPosition - 4] & 0xff) << 24);
    }

    public long readLong() {
        long long_2 = readInt() & 0xffffffffL;
        long long_4 = readInt() & 0xffffffffL;
        return long_4 + (long_2 << 32);
    }

    public void writeShort(int i_1) {
        this.buffer[++this.currentPosition - 1] = (byte) (i_1 >> 8);
        this.buffer[++this.currentPosition - 1] = (byte) i_1;
    }

    public long bitpackLong(int size) {
        if (--size >= 0 && size <= 7) {
            int sizeShift = size * 8;
            long bitpacked;
            for (bitpacked = 0L; sizeShift >= 0; sizeShift -= 8) {
                bitpacked |= (this.buffer[++this.currentPosition - 1] & 0xffL) << sizeShift;
            }
            return bitpacked;
        } 
        throw new IllegalArgumentException();
    }

    public String readNullString() {
        if (this.buffer[this.currentPosition] == 0) {
            ++this.currentPosition;
            return null;
        }
        return readString();
    }

    public String readGJString() {
        byte firstByte = this.buffer[++this.currentPosition - 1];
        if (firstByte != 0) {
            throw new IllegalStateException("");
        } 
        int stringStartPosition = this.currentPosition;
        while (this.buffer[++this.currentPosition - 1] != 0) {
        }
        int stringByteCount = this.currentPosition - stringStartPosition - 1;
        return stringByteCount == 0 ? "" : Utils.readString(this.buffer, stringStartPosition, stringByteCount);
    }

    public int method13099() {
        byte b_2 = this.buffer[++this.currentPosition - 1];
        int i_3;
        for (i_3 = 0; b_2 < 0; b_2 = this.buffer[++this.currentPosition - 1]) {
            i_3 = (i_3 | b_2 & 0x7f) << 7;
        }
        return i_3 | b_2;
    }

    public void method13100(int[] ints_1) {
        int i_3 = this.currentPosition / 8;
        this.currentPosition = 0;
        for (int i_4 = 0; i_4 < i_3; i_4++) {
            int i_5 = readInt();
            int i_6 = readInt();
            int i_7 = -957401312;
            int i_8 = -1640531527;
            for (int i_9 = 32; i_9-- > 0; i_5 -= i_6 + (i_6 << 4 ^ i_6 >>> 5) ^ i_7 + ints_1[i_7 & 0x3]) {
                i_6 -= i_5 + (i_5 << 4 ^ i_5 >>> 5) ^ ints_1[i_7 >>> 11 & 0x3] + i_7;
                i_7 -= i_8;
            }
            this.currentPosition -= 8;
            writeInt(i_5);
            writeInt(i_6);
        }
    }

    public void encryptWithXtea(int[] keys, int start, int end) {
        int i_5 = this.currentPosition;
        this.currentPosition = start;
        int i_6 = (end - start) / 8;
        for (int i_7 = 0; i_7 < i_6; i_7++) {
            int i_8 = readInt();
            int i_9 = readInt();
            int i_10 = 0;
            int i_11 = -1640531527;
            for (int i_12 = 32; i_12-- > 0; i_9 += i_8 + (i_8 << 4 ^ i_8 >>> 5) ^ keys[i_10 >>> 11 & 0x3] + i_10) {
                i_8 += i_9 + (i_9 << 4 ^ i_9 >>> 5) ^ i_10 + keys[i_10 & 0x3];
                i_10 += i_11;
            }
            this.currentPosition -= 8;
            writeInt(i_8);
            writeInt(i_9);
        }
        this.currentPosition = i_5;
    }

    public void applyRSA() {
        int i_3 = this.currentPosition;
        this.currentPosition = 0;
        byte[] bytes_4 = new byte[i_3];
        readBytes(bytes_4, 0, i_3);
        BigInteger biginteger5 = new BigInteger(bytes_4);
        BigInteger biginteger6 = biginteger5.modPow(Loader.RSA_PUBLIC_EXPONENT, Loader.RSA_PUBLIC_MODULUS);
        byte[] bytes_7 = biginteger6.toByteArray();
        this.currentPosition = 0;
        writeShort(bytes_7.length);
        writeBytes(bytes_7, 0, bytes_7.length);
    }

    public int writeCrc(int start) {
        int crc = Hashing.getCrc32(this.buffer, start, this.currentPosition);
        writeInt(crc);
        return crc;
    }

    public boolean checkCrc() {
        this.currentPosition -= 4;
        int calcedCrc = Hashing.getCrc32(this.buffer, 0, this.currentPosition);
        int expectedCrc = readInt();
        return calcedCrc == expectedCrc;
    }

    public void writeByteC(int i_1) {
        this.buffer[++this.currentPosition - 1] = (byte) (-i_1);
    }

    public void write128Byte(int i_1) {
        this.buffer[++this.currentPosition - 1] = (byte) (128 - i_1);
    }

    public int readUnsigned128Byte() {
        return 128 - this.buffer[++this.currentPosition - 1] & 0xff;
    }

    public byte readByte128() {
        return (byte) (this.buffer[++this.currentPosition - 1] - 128);
    }

    public byte read128Byte() {
        return (byte) (128 - this.buffer[++this.currentPosition - 1]);
    }

    public void writeShort128(int i_1) {
        this.buffer[++this.currentPosition - 1] = (byte) (i_1 >> 8);
        this.buffer[++this.currentPosition - 1] = (byte) (i_1 + 128);
    }

    public void writeShortLE128(int i_1) {
        this.buffer[++this.currentPosition - 1] = (byte) (i_1 + 128);
        this.buffer[++this.currentPosition - 1] = (byte) (i_1 >> 8);
    }

    public int readShortLE() {
        this.currentPosition += 2;
        return ((this.buffer[this.currentPosition - 1] & 0xff) << 8) + (this.buffer[this.currentPosition - 2] & 0xff);
    }

    public int readUnsignedShortLE128() {
        this.currentPosition += 2;
        return ((this.buffer[this.currentPosition - 1] & 0xff) << 8) + (this.buffer[this.currentPosition - 2] - 128 & 0xff);
    }

    public int method13121() {
        this.currentPosition += 2;
        int i_2 = ((this.buffer[this.currentPosition - 1] & 0xff) << 8) + (this.buffer[this.currentPosition - 2] & 0xff);
        if (i_2 > 32767) {
            i_2 -= 65536;
        }
        return i_2;
    }

    public int readShort128() {
        this.currentPosition += 2;
        int i_2 = (this.buffer[this.currentPosition - 1] - 128 & 0xff) + ((this.buffer[this.currentPosition - 2] & 0xff) << 8);
        if (i_2 > 32767) {
            i_2 -= 65536;
        }
        return i_2;
    }

    public int readShortLE128() {
        this.currentPosition += 2;
        int i_2 = ((this.buffer[this.currentPosition - 1] & 0xff) << 8) + (this.buffer[this.currentPosition - 2] - 128 & 0xff);
        if (i_2 > 32767) {
            i_2 -= 65536;
        }
        return i_2;
    }

    public void writeIntLE(int i_1) {
        this.buffer[++this.currentPosition - 1] = (byte) i_1;
        this.buffer[++this.currentPosition - 1] = (byte) (i_1 >> 8);
        this.buffer[++this.currentPosition - 1] = (byte) (i_1 >> 16);
        this.buffer[++this.currentPosition - 1] = (byte) (i_1 >> 24);
    }

    public void writeIntV2(int i_1) {
        this.buffer[++this.currentPosition - 1] = (byte) (i_1 >> 16);
        this.buffer[++this.currentPosition - 1] = (byte) (i_1 >> 24);
        this.buffer[++this.currentPosition - 1] = (byte) i_1;
        this.buffer[++this.currentPosition - 1] = (byte) (i_1 >> 8);
    }

    public int readIntLE() {
        this.currentPosition += 4;
        return (this.buffer[this.currentPosition - 4] & 0xff) + ((this.buffer[this.currentPosition - 3] & 0xff) << 8) + ((this.buffer[this.currentPosition - 2] & 0xff) << 16) + ((this.buffer[this.currentPosition - 1] & 0xff) << 24);
    }

    public int readIntV1() {
        this.currentPosition += 4;
        return ((this.buffer[this.currentPosition - 2] & 0xff) << 24) + ((this.buffer[this.currentPosition - 4] & 0xff) << 8) + (this.buffer[this.currentPosition - 3] & 0xff) + ((this.buffer[this.currentPosition - 1] & 0xff) << 16);
    }

    public int readIntV2() {
        this.currentPosition += 4;
        return ((this.buffer[this.currentPosition - 1] & 0xff) << 8) + ((this.buffer[this.currentPosition - 4] & 0xff) << 16) + (this.buffer[this.currentPosition - 2] & 0xff) + ((this.buffer[this.currentPosition - 3] & 0xff) << 24);
    }

    public int method13132() {
        this.currentPosition += 3;
        return ((this.buffer[this.currentPosition - 1] & 0xff) << 8) + ((this.buffer[this.currentPosition - 3] & 0xff) << 16) + (this.buffer[this.currentPosition - 2] & 0xff);
    }

    public void readBytes(byte[] bytes_1, int i_2, int i_3) {
        for (int i_5 = i_2; i_5 < i_3 + i_2; i_5++) {
            bytes_1[i_5] = this.buffer[++this.currentPosition - 1];
        }
    }

    public void writeString(String string) {
        int stringEndSignal = string.indexOf(0);
        if (stringEndSignal >= 0) {
            throw new IllegalArgumentException("");
        } else {
            this.currentPosition += Packet.write_string(string, string.length(), this.buffer, this.currentPosition);
            this.buffer[++this.currentPosition - 1] = 0;
        }
    }

    public void writeIntV1(int i_1) {
        this.buffer[++this.currentPosition - 1] = (byte) (i_1 >> 8);
        this.buffer[++this.currentPosition - 1] = (byte) i_1;
        this.buffer[++this.currentPosition - 1] = (byte) (i_1 >> 24);
        this.buffer[++this.currentPosition - 1] = (byte) (i_1 >> 16);
    }

    public int readSignedSmart() {
        int i_2 = this.buffer[this.currentPosition] & 0xff;
        return i_2 < 128 ? readUnsignedByte() - 64 : readUnsignedShort() - 49152;
    }

    public int readSum() {
        int i_2 = 0;
        int i_3;
        for (i_3 = readUnsignedSmart(); i_3 == 32767; i_3 = readUnsignedSmart()) {
            i_2 += 32767;
        }
        i_2 += i_3;
        return i_2;
    }

    public int readUnsignedBigSmart() {
        return this.buffer[this.currentPosition] < 0 ? readInt() & 0x7fffffff : readUnsignedShort();
    }

    public int readBigSmart() {
        if (this.buffer[this.currentPosition] < 0) {
            return readInt() & 0x7fffffff;
        } else {
            int i_1 = readUnsignedShort();
            return i_1 == 32767 ? -1 : i_1;
        }
    }

    public int readUnsignedSmart() {
        int i_2 = this.buffer[this.currentPosition] & 0xff;
        return i_2 < 128 ? readUnsignedByte() : readUnsignedShort() - 32768;
    }

    public long read5ByteInteger() {
        long long_2 = readUnsignedByte() & 0xffffffffL;
        long long_4 = readInt() & 0xffffffffL;
        return (long_2 << 32) + long_4;
    }

    public void write24BitInt(int i_1) {
        this.buffer[++this.currentPosition - 1] = (byte) (i_1 >> 16);
        this.buffer[++this.currentPosition - 1] = (byte) (i_1 >> 8);
        this.buffer[++this.currentPosition - 1] = (byte) i_1;
    }

    public void write40BitLong(long long_1) {
        this.buffer[++this.currentPosition - 1] = (byte) ((int) (long_1 >> 32));
        this.buffer[++this.currentPosition - 1] = (byte) ((int) (long_1 >> 24));
        this.buffer[++this.currentPosition - 1] = (byte) ((int) (long_1 >> 16));
        this.buffer[++this.currentPosition - 1] = (byte) ((int) (long_1 >> 8));
        this.buffer[++this.currentPosition - 1] = (byte) ((int) long_1);
    }
    
    public void writeLong(long long_1) {
        this.buffer[++this.currentPosition - 1] = (byte) ((int) (long_1 >> 56));
        this.buffer[++this.currentPosition - 1] = (byte) ((int) (long_1 >> 48));
        this.buffer[++this.currentPosition - 1] = (byte) ((int) (long_1 >> 40));
        this.buffer[++this.currentPosition - 1] = (byte) ((int) (long_1 >> 32));
        this.buffer[++this.currentPosition - 1] = (byte) ((int) (long_1 >> 24));
        this.buffer[++this.currentPosition - 1] = (byte) ((int) (long_1 >> 16));
        this.buffer[++this.currentPosition - 1] = (byte) ((int) (long_1 >> 8));
        this.buffer[++this.currentPosition - 1] = (byte) ((int) long_1);
    }

    public String readString() {
        int i_1 = this.currentPosition;
        while (this.buffer[++this.currentPosition - 1] != 0) {
        }
        int i_2 = this.currentPosition - i_1 - 1;
        return i_2 == 0 ? "" : Utils.readString(this.buffer, i_1, i_2);
    }

    public int readUnsignedByte128() {
        return this.buffer[++this.currentPosition - 1] - 128 & 0xff;
    }

    public void put24BitInt(int i_1) {
        this.buffer[++this.currentPosition - 1] = (byte) i_1;
        this.buffer[++this.currentPosition - 1] = (byte) (i_1 >> 8);
        this.buffer[++this.currentPosition - 1] = (byte) (i_1 >> 16);
        this.buffer[++this.currentPosition - 1] = (byte) (i_1 >> 24);
    }

    public byte readByte() {
        return this.buffer[++this.currentPosition - 1];
    }

    public String method13248() {
        byte b_2 = this.buffer[++this.currentPosition - 1];
        if (b_2 != 0) {
            throw new IllegalStateException("");
        } else {
            int i_3 = method13099();
            if (i_3 + this.currentPosition > this.buffer.length) {
                throw new IllegalStateException("");
            } else {
                String string_4 = StaticMethods.method4824(this.buffer, this.currentPosition, i_3);
                this.currentPosition += i_3;
                return string_4;
            }
        }
    }

    public void decryptXteas(int[] ints_1, int i_3) {
        int i_5 = this.currentPosition;
        this.currentPosition = 5;
        int i_6 = (i_3 - 5) / 8;
        for (int i_7 = 0; i_7 < i_6; i_7++) {
            int i_8 = readInt();
            int i_9 = readInt();
            int i_10 = -957401312;
            int i_11 = -1640531527;
            for (int i_12 = 32; i_12-- > 0; i_8 -= i_9 + (i_9 << 4 ^ i_9 >>> 5) ^ i_10 + ints_1[i_10 & 0x3]) {
                i_9 -= i_8 + (i_8 << 4 ^ i_8 >>> 5) ^ ints_1[i_10 >>> 11 & 0x3] + i_10;
                i_10 -= i_11;
            }
            this.currentPosition -= 8;
            writeInt(i_8);
            writeInt(i_9);
        }
        this.currentPosition = i_5;
    }

    public byte readByteC() {
        return (byte) (-this.buffer[++this.currentPosition - 1]);
    }

    public int readUnsignedByteC() {
        return -this.buffer[++this.currentPosition - 1] & 0xff;
    }

    public long method13278() {
        long long_2 = g4() & 0xffffffffL;
        long long_4 = g4() & 0xffffffffL;
        return long_2 + (long_4 << 32);
    }

    public void method13281(int i_1) {
        this.buffer[this.currentPosition - i_1 - 2] = (byte) (i_1 >> 8);
        this.buffer[this.currentPosition - i_1 - 1] = (byte) i_1;
    }

    public static int write_string(CharSequence str, int length, byte[] buffer, int currentPosition) {
	    for (int index = 0; index < length; index++) {
	        char charAtPos = str.charAt(index);
	        if (charAtPos > 0 && charAtPos < 128 || charAtPos >= 160 && charAtPos <= 255) {
	            buffer[index + currentPosition] = (byte) charAtPos;
	        } else if (charAtPos == 8364) {
	            buffer[index + currentPosition] = -128;
	        } else if (charAtPos == 8218) {
	            buffer[index + currentPosition] = -126;
	        } else if (charAtPos == 402) {
	            buffer[index + currentPosition] = -125;
	        } else if (charAtPos == 8222) {
	            buffer[index + currentPosition] = -124;
	        } else if (charAtPos == 8230) {
	            buffer[index + currentPosition] = -123;
	        } else if (charAtPos == 8224) {
	            buffer[index + currentPosition] = -122;
	        } else if (charAtPos == 8225) {
	            buffer[index + currentPosition] = -121;
	        } else if (charAtPos == 710) {
	            buffer[index + currentPosition] = -120;
	        } else if (charAtPos == 8240) {
	            buffer[index + currentPosition] = -119;
	        } else if (charAtPos == 352) {
	            buffer[index + currentPosition] = -118;
	        } else if (charAtPos == 8249) {
	            buffer[index + currentPosition] = -117;
	        } else if (charAtPos == 338) {
	            buffer[index + currentPosition] = -116;
	        } else if (charAtPos == 381) {
	            buffer[index + currentPosition] = -114;
	        } else if (charAtPos == 8216) {
	            buffer[index + currentPosition] = -111;
	        } else if (charAtPos == 8217) {
	            buffer[index + currentPosition] = -110;
	        } else if (charAtPos == 8220) {
	            buffer[index + currentPosition] = -109;
	        } else if (charAtPos == 8221) {
	            buffer[index + currentPosition] = -108;
	        } else if (charAtPos == 8226) {
	            buffer[index + currentPosition] = -107;
	        } else if (charAtPos == 8211) {
	            buffer[index + currentPosition] = -106;
	        } else if (charAtPos == 8212) {
	            buffer[index + currentPosition] = -105;
	        } else if (charAtPos == 732) {
	            buffer[index + currentPosition] = -104;
	        } else if (charAtPos == 8482) {
	            buffer[index + currentPosition] = -103;
	        } else if (charAtPos == 353) {
	            buffer[index + currentPosition] = -102;
	        } else if (charAtPos == 8250) {
	            buffer[index + currentPosition] = -101;
	        } else if (charAtPos == 339) {
	            buffer[index + currentPosition] = -100;
	        } else if (charAtPos == 382) {
	            buffer[index + currentPosition] = -98;
	        } else if (charAtPos == 376) {
	            buffer[index + currentPosition] = -97;
	        } else {
	            buffer[index + currentPosition] = 63;
	        }
	    }
	    return length;
	}

	public static class Bit extends Packet {
        static final int[] anIntArray9610 = {0, 1, 3, 7, 15, 31, 63, 127, 255, 511, 1023, 2047, 4095, 8191, 16383, 32767, 65535, 131071, 262143, 524287, 1048575, 2097151, 4194303, 8388607, 16777215, 33554431, 67108863, 134217727, 268435455, 536870911, 1073741823, Integer.MAX_VALUE, -1};
        Isaac isaac;
        int anInt9608;

        public Bit(int i_1) {
            super(i_1);
        }

        public void setIsaacCipher(Isaac isaaccipher1) {
            this.isaac = isaaccipher1;
        }

        public boolean nextIsSmart() {
            int i_2 = this.buffer[this.currentPosition] - this.isaac.nextKey() & 0xff;
            return i_2 >= 128;
        }

        public int readEncryptedSmart() {
            int i_1 = this.buffer[++this.currentPosition - 1] - this.isaac.getNextKey() & 0xff;
            return i_1 < 128 ? i_1 : (i_1 - 128 << 8) + (this.buffer[++this.currentPosition - 1] - this.isaac.getNextKey() & 0xff);
        }

        public int readBits(int i_1) {
            int i_3 = this.anInt9608 >> 3;
            int i_4 = 8 - (this.anInt9608 & 0x7);
            int i_5 = 0;

            for (this.anInt9608 += i_1; i_1 > i_4; i_4 = 8) {
                i_5 += (this.buffer[i_3++] & anIntArray9610[i_4]) << i_1 - i_4;
                i_1 -= i_4;
            }

            if (i_4 == i_1) {
                i_5 += this.buffer[i_3] & anIntArray9610[i_4];
            } else {
                i_5 += this.buffer[i_3] >> i_4 - i_1 & anIntArray9610[i_1];
            }

            return i_5;
        }

        public void initBitAccess() {
            this.anInt9608 = this.currentPosition * 8;
        }

        public void readBytes(byte[] bytes_1, int i_3) {
            for (int i_5 = 0; i_5 < i_3; i_5++) {
                bytes_1[i_5] = (byte) (this.buffer[++this.currentPosition - 1] - this.isaac.getNextKey());
            }

        }

        public void finishBitAccess() {
            this.currentPosition = (this.anInt9608 + 7) / 8;
        }

        public int readableBits(int i_1) {
            return i_1 * 8 - this.anInt9608;
        }

        public void writeIsaacByte(int i_1) {
            this.buffer[++this.currentPosition - 1] = (byte) (i_1 + this.isaac.getNextKey());
        }

    }
}
