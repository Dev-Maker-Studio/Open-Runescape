package com.jagex.game.runetek5.config.midi;

import com.jagex.game.runetek5.buffer.Packet;

class Envelope {
    int form;
    int start;
    int end;
    public int critical;
    public int phaseIndex;
    public int step;
    public int amplitude;
    public int ticks;
    public int numPhases = 2;
    public int[] phaseDuration = new int[2];
    public int[] phasePeak = new int[2];

    Envelope() {
        this.phaseDuration[1] = 65535;
        this.phasePeak[1] = 65535;
    }

    void decode(Packet buffer) {
        this.form = buffer.readUByte();
        this.start = buffer.readInt();
        this.end = buffer.readInt();
        decodeShape(buffer);
    }

    void reset() {
        this.critical = 0;
        this.phaseIndex = 0;
        this.step = 0;
        this.amplitude = 0;
        this.ticks = 0;
    }

    void decodeShape(Packet buffer) {
        this.numPhases = buffer.readUByte();
        this.phaseDuration = new int[this.numPhases];
        this.phasePeak = new int[this.numPhases];

        for (int i = 0; i < this.numPhases; i++) {
            this.phaseDuration[i] = buffer.readUShort();
            this.phasePeak[i] = buffer.readUShort();
        }

    }

    int step(int period) {
        if (this.ticks >= this.critical) {
            this.amplitude = this.phasePeak[this.phaseIndex++] << 15;
            if (this.phaseIndex >= this.numPhases) {
                this.phaseIndex = this.numPhases - 1;
            }

            this.critical = (int) (this.phaseDuration[this.phaseIndex] / 65536.0D * period);
            if (this.critical > this.ticks) {
                this.step = ((this.phasePeak[this.phaseIndex] << 15) - this.amplitude) / (this.critical - this.ticks);
            }
        }

        this.amplitude += this.step;
        this.ticks++;
        return this.amplitude - this.step >> 15;
    }

}
