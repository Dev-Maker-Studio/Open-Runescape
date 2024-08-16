package com.jagex.game.runetek5.sound.jagtheora;

import com.jagex.game.runetek5.buffer.Packet;

/**
 * 
 * @author Brainpower - 28/03/2022
 * Naming from OpenOSRS
 *
 */
public class MidiFileReader {

    public static final byte[] aByteArray784 = {(byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 2, (byte) 0, (byte) 1, (byte) 2, (byte) 1, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
    public int division;
    public int anInt790;
    public int[] trackStarts;
    public long aLong786;
    public int[] startPositionTrackBuffer;
    public int[] trackLengths;
    public int[] anIntArray796;
    public final Packet buffer = new Packet(null);

    public MidiFileReader(byte[] bytes_1) {
        decode(bytes_1);
    }

    public MidiFileReader() {
    }

    public void getTrackLength(int i_1) {
        int i_2 = this.buffer.method13099();
        this.trackLengths[i_1] += i_2;
    }

    public int getPrioritizedTrack() {
        int i_1 = this.startPositionTrackBuffer.length;
        int i_2 = -1;
        int i_3 = Integer.MAX_VALUE;

        for (int i_4 = 0; i_4 < i_1; i_4++) {
            if (this.startPositionTrackBuffer[i_4] >= 0 && this.trackLengths[i_4] < i_3) {
                i_2 = i_4;
                i_3 = this.trackLengths[i_4];
            }
        }

        return i_2;
    }

    public void decode(byte[] data) {
        this.buffer.buffer = data;
        this.buffer.currentPosition = 10;
        int count = this.buffer.readUnsignedShort();
        this.division = this.buffer.readUnsignedShort();
        this.anInt790 = 500000;
        this.trackStarts = new int[count];

        int offset;
        for (int index = 0; index < count; this.buffer.currentPosition += offset) {
            int action = this.buffer.readInt();
            offset = this.buffer.readInt();
            if (action == 1297379947) {
                this.trackStarts[index++] = this.buffer.currentPosition;
            }
        }

        this.aLong786 = 0L;
        this.startPositionTrackBuffer = new int[count];

        System.arraycopy(this.trackStarts, 0, this.startPositionTrackBuffer, 0, count);

        this.trackLengths = new int[count];
        this.anIntArray796 = new int[count];
    }

    public void clear() {
        this.buffer.buffer = null;
        this.trackStarts = null;
        this.startPositionTrackBuffer = null;
        this.trackLengths = null;
        this.anIntArray796 = null;
    }

    public int getTrackCount() {
        return this.startPositionTrackBuffer.length;
    }

    public void getGotoTrack(int track) {
        this.buffer.currentPosition = this.startPositionTrackBuffer[track];
    }

    public void setTrackDone() {
        this.buffer.currentPosition = -1;
    }

    public int readMessage(int track) {
        int flags = decodeTrackFlags(track);
        return flags;
    }

    public long method1403(int i_1) {
        return this.aLong786 + (long) i_1 * this.anInt790;
    }

    public void markTracePosition(int i_1) {
        this.startPositionTrackBuffer[i_1] = this.buffer.currentPosition;
    }

    public boolean isDone() {
        int i_1 = this.startPositionTrackBuffer.length;

        for (int i_2 = 0; i_2 < i_1; i_2++) {
            if (this.startPositionTrackBuffer[i_2] >= 0) {
                return false;
            }
        }

        return true;
    }

    public void method1406(long long_1) {
        this.aLong786 = long_1;
        int i_3 = this.startPositionTrackBuffer.length;

        for (int i_4 = 0; i_4 < i_3; i_4++) {
            this.trackLengths[i_4] = 0;
            this.anIntArray796[i_4] = 0;
            this.buffer.currentPosition = this.trackStarts[i_4];
            getTrackLength(i_4);
            this.startPositionTrackBuffer[i_4] = this.buffer.currentPosition;
        }

    }

    public boolean dataLoaded() {
        return this.buffer.buffer != null;
    }

    public int decodeTrackFlags(int track, int initialFlags) {
        if (initialFlags == 255) {
            int i_7 = this.buffer.readUnsignedByte();
            int offset = this.buffer.method13099();
            if (i_7 == 47) {
                this.buffer.currentPosition += offset;
                return 1;
            } else if (i_7 == 81) {
                int i_5 = this.buffer.read24BitUnsignedInteger();
                offset -= 3;
                int length = this.trackLengths[track];
                this.aLong786 += (long) length * (this.anInt790 - i_5);
                this.anInt790 = i_5;
                this.buffer.currentPosition += offset;
                return 2;
            } else {
                this.buffer.currentPosition += offset;
                return 3;
            }
        } 
        byte b_3 = aByteArray784[initialFlags - 128];
        int flags = initialFlags;
        if (b_3 >= 1) {
            flags = initialFlags | this.buffer.readUnsignedByte() << 8;
        }

        if (b_3 >= 2) {
            flags |= this.buffer.readUnsignedByte() << 16;
        }

        return flags;
    }

    public int decodeTrackFlags(int track) {
        byte b_2 = this.buffer.buffer[this.buffer.currentPosition];
        int i_5;
        if (b_2 < 0) {
            i_5 = b_2 & 0xff;
            this.anIntArray796[track] = i_5;
            this.buffer.currentPosition++;
        } else {
            i_5 = this.anIntArray796[track];
        }

        if (i_5 != 240 && i_5 != 247) {
            return decodeTrackFlags(track, i_5);
        } 
        int offset = this.buffer.method13099();
        if (i_5 == 247 && offset > 0) {
            int flags = this.buffer.buffer[this.buffer.currentPosition] & 0xff;
            if (flags >= 241 && flags <= 243 || flags == 246 || flags == 248 || flags >= 250 && flags <= 252 || flags == 254) {
                this.buffer.currentPosition++;
                this.anIntArray796[track] = flags;
                return decodeTrackFlags(track, flags);
            }
        }

        this.buffer.currentPosition += offset;
        return 0;
    }

}
