package com.jagex.game.runetek5.cache;

import java.io.EOFException;
import java.io.IOException;

import com.jagex.game.runetek5.client.window.WindowMode;

public class FileOnDisk {

    public static int anInt5357;
    public static FileOnDisk[] cacheFileIndices;
    public long currentPosition;
    public long aLong5351;
    public long initialCapacity;
    public int anInt5356;
    public long aLong5345 = -1L;
    public long occupiedSize;
    public long aLong5347 = -1L;
    public final JFS32 fs32;
    public byte[] aByteArray5350;
    public byte[] aByteArray5348;
    public int anInt5349;

    public FileOnDisk(JFS32 fileSystem, int capacity) throws IOException {
        this.fs32 = fileSystem;
        this.initialCapacity = this.occupiedSize = fileSystem.length();
        this.aByteArray5350 = new byte[capacity];
        this.aByteArray5348 = new byte[0];
        this.currentPosition = 0L;
    }

    public long getInitialSize() {
        return this.initialCapacity;
    }

    public void close() throws IOException {
        method7353();
        this.fs32.close();
    }

    public void setPosition(long position) throws IOException {
        if (position < 0L) {
            throw new IOException();
        } 
        this.currentPosition = position;
    }

    public void load0(byte[] buffer, int i_2, int i_3) throws IOException {
        try {
            if (i_3 + i_2 > buffer.length) {
                throw new ArrayIndexOutOfBoundsException(i_3 + i_2 - buffer.length);
            }

            if (this.aLong5345 != -1L && this.currentPosition >= this.aLong5345 && this.currentPosition + i_3 <= (this.anInt5356 * 675653231L) + this.aLong5345) {
                System.arraycopy(this.aByteArray5348, (int) (this.currentPosition - this.aLong5345), buffer, i_2, i_3);
                this.currentPosition += i_3;
                return;
            }

            long long_5 = this.currentPosition;
            int i_8 = i_3;
            int i_9;
            if (this.currentPosition >= this.aLong5347 && this.currentPosition < this.anInt5349 + this.aLong5347) {
                i_9 = (int) (this.anInt5349 - (this.currentPosition - this.aLong5347));
                if (i_9 > i_3) {
                    i_9 = i_3;
                }

                System.arraycopy(this.aByteArray5350, (int) (this.currentPosition - this.aLong5347), buffer, i_2, i_9);
                this.currentPosition += i_9;
                i_2 += i_9;
                i_3 -= i_9;
            }

            if (i_3 > this.aByteArray5350.length) {
                this.fs32.seek(this.currentPosition);

                for (this.aLong5351 = this.currentPosition; i_3 > 0; i_3 -= i_9) {
                    i_9 = this.fs32.getSeekOffset(buffer, i_2, i_3);
                    if (i_9 == -1) {
                        break;
                    }

                    this.aLong5351 += i_9;
                    this.currentPosition += i_9;
                    i_2 += i_9;
                }
            } else if (i_3 > 0) {
                seek();
                i_9 = Math.min(i_3, this.anInt5349);

                System.arraycopy(this.aByteArray5350, 0, buffer, i_2, i_9);
                i_2 += i_9;
                i_3 -= i_9;
                this.currentPosition += i_9;
            }

            if (this.aLong5345 != -1L) {
                if (this.aLong5345 > this.currentPosition && i_3 > 0) {
                    i_9 = i_2 + (int) (this.aLong5345 - this.currentPosition);
                    if (i_9 > i_3 + i_2) {
                        i_9 = i_3 + i_2;
                    }

                    while (i_2 < i_9) {
                        buffer[i_2++] = 0;
                        --i_3;
                        ++this.currentPosition;
                    }
                }

                long long_14 = -1L;
                long long_11 = -1L;
                if (this.aLong5345 >= long_5 && this.aLong5345 < i_8 + long_5) {
                    long_14 = this.aLong5345;
                } else if (long_5 >= this.aLong5345 && long_5 < (this.anInt5356 * 675653231L) + this.aLong5345) {
                    long_14 = long_5;
                }

                if ((this.anInt5356 * 675653231L) + this.aLong5345 > long_5 && (this.anInt5356 * 675653231L) + this.aLong5345 <= i_8 + long_5) {
                    long_11 = (this.anInt5356 * 675653231L) + this.aLong5345;
                } else if (i_8 + long_5 > this.aLong5345 && long_5 + i_8 <= this.aLong5345 + (this.anInt5356 * 675653231L)) {
                    long_11 = i_8 + long_5;
                }

                if (long_14 > -1L && long_11 > long_14) {
                    int i_13 = (int) (long_11 - long_14);
                    System.arraycopy(this.aByteArray5348, (int) (long_14 - this.aLong5345), buffer, (int) (long_14 - long_5) + i_2, i_13);
                    if (long_11 > this.currentPosition) {
                        i_3 = (int) (i_3 - (long_11 - this.currentPosition));
                        this.currentPosition = long_11;
                    }
                }
            }
        } catch (IOException ioexception17) {
            this.aLong5351 = -1L;
            throw ioexception17;
        }

        if (i_3 > 0) {
            throw new EOFException();
        }
    }

    public void load(byte[] bytes_1) throws IOException {
        load0(bytes_1, 0, bytes_1.length);
    }

    public void write(byte[] bytes_1, int i_2, int i_3) throws IOException {
        try {
            if (i_3 + this.currentPosition > this.initialCapacity) {
                this.initialCapacity = this.currentPosition + i_3;
            }

            if (this.aLong5345 != -1L && (this.currentPosition < this.aLong5345 || this.currentPosition > (this.anInt5356 * 675653231L) + this.aLong5345)) {
                method7353();
            }

            if (this.aLong5345 != -1L && this.currentPosition + i_3 > this.aLong5345 + this.aByteArray5348.length) {
                int i_5 = (int) (this.aByteArray5348.length - (this.currentPosition - this.aLong5345));
                System.arraycopy(bytes_1, i_2, this.aByteArray5348, (int) (this.currentPosition - this.aLong5345), i_5);
                this.currentPosition += i_5;
                i_2 += i_5;
                i_3 -= i_5;
                this.anInt5356 = this.aByteArray5348.length * -1070827377;
                method7353();
            }

            if (i_3 > this.aByteArray5348.length) {
                if (this.currentPosition != this.aLong5351) {
                    this.fs32.seek(this.currentPosition);
                    this.aLong5351 = this.currentPosition;
                }

                this.fs32.write(bytes_1, i_2, i_3);
                this.aLong5351 += i_3;
                if (this.aLong5351 > this.occupiedSize) {
                    this.occupiedSize = this.aLong5351;
                }

                long long_10 = -1L;
                long long_7 = -1L;
                if (this.currentPosition >= this.aLong5347 && this.currentPosition < this.aLong5347 + this.anInt5349) {
                    long_10 = this.currentPosition;
                } else if (this.aLong5347 >= this.currentPosition && this.aLong5347 < i_3 + this.currentPosition) {
                    long_10 = this.aLong5347;
                }

                if (i_3 + this.currentPosition > this.aLong5347 && this.currentPosition + i_3 <= this.aLong5347 + this.anInt5349) {
                    long_7 = i_3 + this.currentPosition;
                } else if (this.aLong5347 + this.anInt5349 > this.currentPosition && this.aLong5347 + this.anInt5349 <= this.currentPosition + i_3) {
                    long_7 = this.anInt5349 + this.aLong5347;
                }

                if (long_10 > -1L && long_7 > long_10) {
                    int i_9 = (int) (long_7 - long_10);
                    System.arraycopy(bytes_1, (int) (long_10 + i_2 - this.currentPosition), this.aByteArray5350, (int) (long_10 - this.aLong5347), i_9);
                }

                this.currentPosition += i_3;
            } else if (i_3 > 0) {
                if (this.aLong5345 == -1L) {
                    this.aLong5345 = this.currentPosition;
                }

                System.arraycopy(bytes_1, i_2, this.aByteArray5348, (int) (this.currentPosition - this.aLong5345), i_3);
                this.currentPosition += i_3;
                if (this.currentPosition - this.aLong5345 > (this.anInt5356 * 675653231L)) {
                    this.anInt5356 = (int) (this.currentPosition - this.aLong5345) * -1070827377;
                }
            }

        } catch (IOException ioexception13) {
            this.aLong5351 = -1L;
            throw ioexception13;
        }
    }

    public void method7353() throws IOException {
        if (this.aLong5345 != -1L) {
            if (this.aLong5345 != this.aLong5351) {
                this.fs32.seek(this.aLong5345);
                this.aLong5351 = this.aLong5345;
            }
            this.fs32.write(this.aByteArray5348, 0, this.anInt5356 * 675653231);
            this.aLong5351 += 675653231L * this.anInt5356;
            if (this.aLong5351 > this.occupiedSize) {
                this.occupiedSize = this.aLong5351;
            }
            long long_2 = -1L;
            long long_4 = -1L;
            if (this.aLong5345 >= this.aLong5347 && this.aLong5345 < this.anInt5349 + this.aLong5347) {
                long_2 = this.aLong5345;
            } else if (this.aLong5347 >= this.aLong5345 && this.aLong5347 < this.aLong5345 + (this.anInt5356 * 675653231L)) {
                long_2 = this.aLong5347;
            }
            if ((this.anInt5356 * 675653231L) + this.aLong5345 > this.aLong5347 && (this.anInt5356 * 675653231L) + this.aLong5345 <= this.anInt5349 + this.aLong5347) {
                long_4 = (this.anInt5356 * 675653231L) + this.aLong5345;
            } else if (this.aLong5347 + this.anInt5349 > this.aLong5345 && this.anInt5349 + this.aLong5347 <= this.aLong5345 + (this.anInt5356 * 675653231L)) {
                long_4 = this.aLong5347 + this.anInt5349;
            }
            if (long_2 > -1L && long_4 > long_2) {
                int i_6 = (int) (long_4 - long_2);
                System.arraycopy(this.aByteArray5348, (int) (long_2 - this.aLong5345), this.aByteArray5350, (int) (long_2 - this.aLong5347), i_6);
            }
            this.aLong5345 = -1L;
            this.anInt5356 = 0;
        }

    }

    public void seek() throws IOException {
        this.anInt5349 = 0;
        if (this.currentPosition != this.aLong5351) {
            this.fs32.seek(this.currentPosition);
            this.aLong5351 = this.currentPosition;
        }
        int offset;
        for (this.aLong5347 = this.currentPosition; this.anInt5349 < this.aByteArray5350.length; this.anInt5349 += offset) {
            int length = this.aByteArray5350.length - this.anInt5349;
            if (length > 200000000) {
                length = 200000000;
            }
            offset = this.fs32.getSeekOffset(this.aByteArray5350, this.anInt5349, length);
            if (offset == -1) {
                break;
            }
            this.aLong5351 += offset;
        }
    }

}
