package com.jagex.game.runetek5.cache;

import java.io.EOFException;
import java.io.IOException;

public class Decompressor {

    public static final byte[] buffer = new byte[520];
    public final FileOnDisk dataFile;
    public final FileOnDisk idxFile;
    public int sizeCap = 65000;
    public final int index;

    public Decompressor(int index, FileOnDisk dataFile, FileOnDisk idxFile) {
    	this.index = index;
        this.dataFile = dataFile;
        this.idxFile = idxFile;
        this.sizeCap = 1500000;
    }

    public boolean put(int file, byte[] data, int size) {
        synchronized (this.dataFile) {
            if (size >= 0 && size <= this.sizeCap) {
                boolean successful = put(file, data, size, true);
                if (!successful) {
                    successful = put(file, data, size, false);
                }
                return successful;
            } 
            throw new IllegalArgumentException();
        }
    }

    public boolean put(int file, byte[] data, int size, boolean seek) {
        synchronized (this.dataFile) {
            try {
                int sector;
                if (seek) {
                    if (this.idxFile.getInitialSize() < (file * 6L + 6)) {
                        return false;
                    }
                    this.idxFile.setPosition(file * 6L);
                    this.idxFile.load0(buffer, 0, 6);
                    sector = (buffer[5] & 0xff) + ((buffer[3] & 0xff) << 16) + ((buffer[4] & 0xff) << 8);
                    if (sector <= 0 || sector > this.dataFile.getInitialSize() / 520L) {
                        return false;
                    }
                } else {
                    sector = (int) ((this.dataFile.getInitialSize() + 519L) / 520L);
                    if (sector == 0) {
                        sector = 1;
                    }
                }
                buffer[0] = (byte) (size >> 16);
                buffer[1] = (byte) (size >> 8);
                buffer[2] = (byte) size;
                buffer[3] = (byte) (sector >> 16);
                buffer[4] = (byte) (sector >> 8);
                buffer[5] = (byte) sector;
                this.idxFile.setPosition(file * 6L);
                this.idxFile.write(buffer, 0, 6);
                int part = 0;
                int i_10 = 0;
                while (true) {
                    if (part < size) {
                        label150:
                        {
                            int nextSector = 0;
                            int currentIndex;
                            if (seek) {
                                this.dataFile.setPosition(sector * 520L);
                                int currentPart;
                                int currentFile;
                                if (file > 65535) {
                                    try {
                                        this.dataFile.load0(buffer, 0, 10);
                                    } catch (EOFException eofexception) {
                                        break label150;
                                    }
                                    currentIndex = ((buffer[1] & 0xff) << 16) + ((buffer[0] & 0xff) << 24) + (buffer[3] & 0xff) + ((buffer[2] & 0xff) << 8);
                                    currentPart = (buffer[5] & 0xff) + ((buffer[4] & 0xff) << 8);
                                    nextSector = (buffer[8] & 0xff) + ((buffer[7] & 0xff) << 8) + ((buffer[6] & 0xff) << 16);
                                    currentFile = buffer[9] & 0xff;
                                } else {
                                    try {
                                        this.dataFile.load0(buffer, 0, 8);
                                    } catch (EOFException eofexception) {
                                        break label150;
                                    }
                                    currentIndex = (buffer[1] & 0xff) + ((buffer[0] & 0xff) << 8);
                                    currentPart = (buffer[3] & 0xff) + ((buffer[2] & 0xff) << 8);
                                    nextSector = ((buffer[5] & 0xff) << 8) + ((buffer[4] & 0xff) << 16) + (buffer[6] & 0xff);
                                    currentFile = buffer[7] & 0xff;
                                }
                                if (currentIndex != file || currentPart != i_10 || currentFile != this.index) {
                                    return false;
                                }
                                if (nextSector < 0 || nextSector > this.dataFile.getInitialSize() / 520L) {
                                    return false;
                                }
                            }
                            if (nextSector == 0) {
                                seek = false;
                                nextSector = (int) ((this.dataFile.getInitialSize() + 519L) / 520L);
                                if (nextSector == 0) {
                                    nextSector++;
                                }
                                if (sector == nextSector) {
                                    nextSector++;
                                }
                            }
                            if (size - part <= 512) {
                                nextSector = 0;
                            }
                            if (file > 65535) {
                                buffer[0] = (byte) (file >> 24);
                                buffer[1] = (byte) (file >> 16);
                                buffer[2] = (byte) (file >> 8);
                                buffer[3] = (byte) file;
                                buffer[4] = (byte) (i_10 >> 8);
                                buffer[5] = (byte) i_10;
                                buffer[6] = (byte) (nextSector >> 16);
                                buffer[7] = (byte) (nextSector >> 8);
                                buffer[8] = (byte) nextSector;
                                buffer[9] = (byte) this.index;
                                this.dataFile.setPosition(sector * 520L);
                                this.dataFile.write(buffer, 0, 10);
                                currentIndex = size - part;
                                if (currentIndex > 510) {
                                    currentIndex = 510;
                                }
                            } else {
                                buffer[0] = (byte) (file >> 8);
                                buffer[1] = (byte) file;
                                buffer[2] = (byte) (i_10 >> 8);
                                buffer[3] = (byte) i_10;
                                buffer[4] = (byte) (nextSector >> 16);
                                buffer[5] = (byte) (nextSector >> 8);
                                buffer[6] = (byte) nextSector;
                                buffer[7] = (byte) this.index;
                                this.dataFile.setPosition(sector * 520L);
                                this.dataFile.write(buffer, 0, 8);
                                currentIndex = size - part;
                                if (currentIndex > 512) {
                                    currentIndex = 512;
                                }
                            }
                            this.dataFile.write(data, part, currentIndex);
                            part += currentIndex;
                            sector = nextSector;
                            i_10++;
                            continue;
                        }
                    }
                    return true;
                }
            } catch (IOException ioexception) {
                return false;
            }
        }
    }

    public byte[] decompress(int i_1) {
        synchronized (this.dataFile) {
            try {
                if (this.idxFile.getInitialSize() < (i_1 * 6L + 6)) {
                    return null;
                }
                this.idxFile.setPosition(i_1 * 6L);
                this.idxFile.load0(buffer, 0, 6);
                int i_18 = ((buffer[0] & 0xff) << 16) + (buffer[2] & 0xff) + ((buffer[1] & 0xff) << 8);
                int i_6 = (buffer[5] & 0xff) + ((buffer[3] & 0xff) << 16) + ((buffer[4] & 0xff) << 8);
                if (i_18 > this.sizeCap) {
                    return null;
                }
                if (i_6 > 0 && i_6 <= this.dataFile.getInitialSize() / 520L) {
                    byte[] bytes_24 = new byte[i_18];
                    int i_8 = 0;
                    int i_9 = 0;
                    while (i_8 < i_18) {
                        if (i_6 == 0) {
                            return null;
                        }
                        this.dataFile.setPosition(i_6 * 520L);
                        int i_19 = i_18 - i_8;
                        byte b_11;
                        int i_12;
                        int i_13;
                        int i_14;
                        int i_15;
                        if (i_1 > 65535) {
                            if (i_19 > 510) {
                                i_19 = 510;
                            }
                            b_11 = 10;
                            this.dataFile.load0(buffer, 0, b_11 + i_19);
                            i_12 = ((buffer[1] & 0xff) << 16) + ((buffer[0] & 0xff) << 24) + (buffer[3] & 0xff) + ((buffer[2] & 0xff) << 8);
                            i_13 = (buffer[5] & 0xff) + ((buffer[4] & 0xff) << 8);
                            i_14 = (buffer[8] & 0xff) + ((buffer[7] & 0xff) << 8) + ((buffer[6] & 0xff) << 16);
                            i_15 = buffer[9] & 0xff;
                        } else {
                            if (i_19 > 512) {
                                i_19 = 512;
                            }
                            b_11 = 8;
                            this.dataFile.load0(buffer, 0, b_11 + i_19);
                            i_12 = (buffer[1] & 0xff) + ((buffer[0] & 0xff) << 8);
                            i_13 = (buffer[3] & 0xff) + ((buffer[2] & 0xff) << 8);
                            i_14 = ((buffer[5] & 0xff) << 8) + ((buffer[4] & 0xff) << 16) + (buffer[6] & 0xff);
                            i_15 = buffer[7] & 0xff;
                        }
                        if (i_12 == i_1 && i_13 == i_9 && i_15 == this.index) {
                            if (i_14 >= 0 && i_14 <= this.dataFile.getInitialSize() / 520L) {
                                int i_20 = i_19 + b_11;
                                for (int i_17 = b_11; i_17 < i_20; i_17++) {
                                    bytes_24[i_8++] = buffer[i_17];
                                }
                                i_6 = i_14;
                                ++i_9;
                                continue;
                            }
                            return null;
                        }
                        return null;
                    }
                    byte[] bytes_4 = bytes_24;
                    return bytes_4;
                }
            } catch (IOException ioexception22) {
                return null;
            }
            return null;
        }
    }

    public String toString() {
        return "" + this.index;
    }
}
