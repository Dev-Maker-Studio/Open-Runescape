package com.jagex.game.runetek5.config.midi.instrument;

/**
 * 
 * @author Brainpower 05/02/2023
 * https://www.cs.cmu.edu/afs/club/usr/cort/code/img2ktx/third_party/stb/stb_vorbis.c
 *
 */
public class MusicSamepleMapping {

	public final int submaps;
    public int mux;
    public final int[] submapFloors;
    public final int[] submapResidues;

    public MusicSamepleMapping() {
        MusicSample.getBits(16);
        this.submaps = MusicSample.getBit() != 0 ? MusicSample.getBits(4) + 1 : 1;
        if (MusicSample.getBit() != 0) {
            MusicSample.getBits(8);
        }

        MusicSample.getBits(2);
        if (this.submaps > 1) {
            this.mux = MusicSample.getBits(4);
        }

        this.submapFloors = new int[this.submaps];
        this.submapResidues = new int[this.submaps];

        for (int index = 0; index < this.submaps; index++) {
            MusicSample.getBits(8);
            this.submapFloors[index] = MusicSample.getBits(8);
            this.submapResidues[index] = MusicSample.getBits(8);
        }

    }
}
