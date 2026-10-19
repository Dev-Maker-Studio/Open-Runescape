package com.jagex.game.runetek5.config.midi.instrument

/**
 *
 * @author Brainpower 05/02/2023
 * https://www.cs.cmu.edu/afs/club/usr/cort/code/img2ktx/third_party/stb/stb_vorbis.c
 *
 */
class MusicSamepleMapping {

    @JvmField val submaps: Int
    @JvmField var mux: Int = 0
    @JvmField val submapFloors: IntArray
    @JvmField val submapResidues: IntArray

    init {
        MusicSample.getBits(16)
        this.submaps = if (MusicSample.getBit() != 0) MusicSample.getBits(4) + 1 else 1
        if (MusicSample.getBit() != 0) {
            MusicSample.getBits(8)
        }

        MusicSample.getBits(2)
        if (this.submaps > 1) {
            this.mux = MusicSample.getBits(4)
        }

        this.submapFloors = IntArray(this.submaps)
        this.submapResidues = IntArray(this.submaps)

        for (index in 0 until this.submaps) {
            MusicSample.getBits(8)
            this.submapFloors[index] = MusicSample.getBits(8)
            this.submapResidues[index] = MusicSample.getBits(8)
        }
    }
}
