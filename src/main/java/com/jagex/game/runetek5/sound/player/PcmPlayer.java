package com.jagex.game.runetek5.sound.player;

import java.util.Arrays;

import com.jagex.game.runetek5.cache.node.impl.Node_Sub26;
import com.jagex.game.runetek5.sound.jagtheora.PcmStream;
import com.jagex.game.runetek5.tools.TimeTools;
import com.jagex.game.runetek5.utils.Utils;

public class PcmPlayer {

    public PcmStream currentPcmStream;
    public int[] bitpackedSoundData;
    public int sampleRate;
    public int capacity;
    public long timeLastCycle = Utils.time();
    public long nextBufferSwapTime;
    public boolean enabled = true;
    public int dataConsumed;
    public int currentOffset;
    public int dataLeftLastCycle;
    public long nextUpdate;
    public int lastDataConsumed;
    public boolean shutdown;
    public int anInt3117;
    public final int anInt3123 = 32;
    public final PcmStream[] activeStreams = new PcmStream[8];
    public final PcmStream[] inactiveStreams = new PcmStream[8];
    
	public synchronized void setCurrentPcmStream(PcmStream currentPcmStream) {
        this.currentPcmStream = currentPcmStream;
    }

    public synchronized void cycle() {
        if (!this.shutdown) {
            long currentTime = Utils.time();

            try {
                if (currentTime > this.timeLastCycle + 6000L) {
                    this.timeLastCycle = currentTime - 6000L;
                }

                while (currentTime > this.timeLastCycle + 5000L) {
                    method4373();
                    this.timeLastCycle += 256000 / PcmPlayerManager.sampleRate;
                    currentTime = Utils.time();
                }
            } catch (Exception ex) {
                this.timeLastCycle = currentTime;
            }

            if (this.bitpackedSoundData != null) {
                try {
                    if (this.nextBufferSwapTime != 0L) {
                        if (currentTime < this.nextBufferSwapTime) {
                            return;
                        }

                        play(this.capacity);
                        this.nextBufferSwapTime = 0L;
                        this.enabled = true;
                    }

                    int dataLeft = dataLeft();
                    if (this.dataLeftLastCycle - dataLeft > this.dataConsumed) {
                        this.dataConsumed = this.dataLeftLastCycle - dataLeft;
                    }

                    int offset = this.sampleRate + this.currentOffset;
                    if (offset + 256 > 16384) {
                        offset = 16128;
                    }

                    if (offset + 256 > this.capacity) {
                        this.capacity += 1024;
                        if (this.capacity > 16384) {
                            this.capacity = 16384;
                        }

                        close();
                        play(this.capacity);
                        dataLeft = 0;
                        this.enabled = true;
                        if (offset + 256 > this.capacity) {
                            offset = this.capacity - 256;
                            this.currentOffset = offset - this.sampleRate;
                        }
                    }

                    while (dataLeft < offset) {
                        method4337(this.bitpackedSoundData);
                        writeToBuffer();
                        dataLeft += 256;
                    }

                    if (currentTime > this.nextUpdate) {
                        if (!this.enabled) {
                            if (this.dataConsumed == 0 && this.lastDataConsumed == 0) {
                                close();
                                this.nextBufferSwapTime = currentTime + 2000L;
                                return;
                            }

                            this.currentOffset = Math.min(this.lastDataConsumed, this.dataConsumed);
                            this.lastDataConsumed = this.dataConsumed;
                        } else {
                            this.enabled = false;
                        }

                        this.dataConsumed = 0;
                        this.nextUpdate = currentTime + 2000L;
                    }

                    this.dataLeftLastCycle = dataLeft;
                } catch (Exception exception) {
                    close();
                    this.nextBufferSwapTime = currentTime + 2000L;
                }
            }
        }

    }

    public synchronized void start() {
        this.enabled = true;

        try {
            open();
        } catch (Exception exception) {
            close();
            this.nextBufferSwapTime = Utils.time() + 2000L;
        }
    }

    public synchronized void shutdown() {
        if (PcmPlayerManager.runnable != null) {
            boolean readyToExit = true;

            for (int index = 0; index < 2; index++) {
                if (this == PcmPlayerManager.runnable.monoSterioPlayers[index]) {
                    PcmPlayerManager.runnable.monoSterioPlayers[index] = null;
                }

                if (PcmPlayerManager.runnable.monoSterioPlayers[index] != null) {
                    readyToExit = false;
                }
            }

            if (readyToExit) {
                PcmPlayerManager.runnable.exit = true;

                while (PcmPlayerManager.runnable.running) {
                    TimeTools.sleep(50L);
                }

                PcmPlayerManager.runnable = null;
            }
        }

        close();
        this.bitpackedSoundData = null;
        this.shutdown = true;
    }

    public void method4337(int[] bitpackedSoundData) {
        int bufferLength = 256;
        if (PcmPlayerManager.sterio) {
            bufferLength = 512;
        }

        Arrays.fill(bitpackedSoundData, 0, bufferLength, 0);
        this.anInt3117 -= 256;
        if (this.currentPcmStream != null && this.anInt3117 <= 0) {
            this.anInt3117 += PcmPlayerManager.sampleRate >> 4;
            method4338(this.currentPcmStream);
            method4339(this.currentPcmStream, this.currentPcmStream.getPriority());
            int i_4 = 0;
            int i_5 = 255;

            int i_6;
            PcmStream inactive;
            label107:
            for (i_6 = 7; i_5 != 0; --i_6) {
                int i_16;
                int i_17;
                if (i_6 < 0) {
                    i_16 = i_6 & 0x3;
                    i_17 = -(i_6 >> 2);
                } else {
                    i_16 = i_6;
                    i_17 = 0;
                }

                for (int i_9 = i_5 >>> i_16 & 0x11111111; i_9 != 0; i_9 >>>= 4) {
                    if ((i_9 & 0x1) != 0) {
                        i_5 &= ~(1 << i_16);
                        inactive = null;
                        PcmStream activeStream = this.activeStreams[i_16];

                        label101:
                        while (true) {
                            while (true) {
                                if (activeStream == null) {
                                    break label101;
                                }

                                Node_Sub26 class282Sub2612 = activeStream.buffer;
                                if (class282Sub2612 != null && class282Sub2612.anInt7690 > i_17) {
                                    i_5 |= 1 << i_16;
                                    inactive = activeStream;
                                    activeStream = activeStream.nextStream;
                                } else {
                                    activeStream.fill = true;
                                    int i_13 = activeStream.method12228();
                                    i_4 += i_13;
                                    if (class282Sub2612 != null) {
                                        class282Sub2612.anInt7690 += i_13;
                                    }

                                    if (i_4 >= this.anInt3123) {
                                        break label107;
                                    }

                                    PcmStream class282Sub1514 = activeStream.getFirstStream();
                                    if (class282Sub1514 != null) {
                                        for (int i_15 = activeStream.streamPriority; class282Sub1514 != null; class282Sub1514 = activeStream.getNextStream()) {
                                            method4339(class282Sub1514, i_15 * class282Sub1514.getPriority() >> 8);
                                        }
                                    }

                                    PcmStream active = activeStream.nextStream;
                                    activeStream.nextStream = null;
                                    if (inactive == null) {
                                        this.activeStreams[i_16] = active;
                                    } else {
                                        inactive.nextStream = active;
                                    }

                                    if (active == null) {
                                        this.inactiveStreams[i_16] = inactive;
                                    }

                                    activeStream = active;
                                }
                            }
                        }
                    }

                    i_16 += 4;
                    ++i_17;
                }
            }

            for (i_6 = 0; i_6 < 8; i_6++) {
                PcmStream class282Sub157 = this.activeStreams[i_6];
                PcmStream[] arr_8 = this.activeStreams;
                this.inactiveStreams[i_6] = null;

                for (arr_8[i_6] = null; class282Sub157 != null; class282Sub157 = inactive) {
                    inactive = class282Sub157.nextStream;
                    class282Sub157.nextStream = null;
                }
            }
        }

        if (this.anInt3117 < 0) {
            this.anInt3117 = 0;
        }

        if (this.currentPcmStream != null) {
            this.currentPcmStream.fill(bitpackedSoundData, 0, 256);
        }

        this.timeLastCycle = Utils.time();
    }

    public void method4339(PcmStream next, int priority) {
        int i_3 = priority >> 5;
        PcmStream inactive = this.inactiveStreams[i_3];
        if (inactive == null) {
            this.activeStreams[i_3] = next;
        } else {
            inactive.nextStream = next;
        }

        this.inactiveStreams[i_3] = next;
        next.streamPriority = priority;
    }

    public int dataLeft() {
        return this.capacity;
    }

    public void writeToBuffer() {
    	// NOOP
    }

    public void close() {
    	// NOOP
    }

    public void open() throws Exception {
    	// NOOP
    }

    public void play(int i_1) throws Exception {
    	// NOOP
    }

    public void initialize() {
    	// NOOP
    }

    public void method4373() {
        this.anInt3117 -= 256;
        if (this.anInt3117 < 0) {
            this.anInt3117 = 0;
        }

        if (this.currentPcmStream != null) {
            this.currentPcmStream.skip(256);
        }

    }

	public static void method4338(PcmStream stream) {
	    stream.fill = false;
	    if (stream.buffer != null) {
	        stream.buffer.anInt7690 = 0;
	    }
	
	    for (PcmStream patchStream = stream.getFirstStream(); patchStream != null; patchStream = stream.getNextStream()) {
	        method4338(patchStream);
	    }
	
	}

}
