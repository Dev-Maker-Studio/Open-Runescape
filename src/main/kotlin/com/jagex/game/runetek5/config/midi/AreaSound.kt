package com.jagex.game.runetek5.config.midi

import com.jagex.game.runetek5.config.midi.instrument.MusicSample
import com.jagex.game.runetek5.entity.AbstractInteractiveEntity
import com.jagex.game.runetek5.entity.SceneGraphEntity
import com.jagex.game.runetek5.sound.jagtheora.AreaSoundStream
import com.jagex.game.runetek5.sound.jagtheora.AudioBuffer

/**
 * Represents an area sound effect that plays at a specific location in the game world.
 * Can be positional audio tied to scene objects.
 *
 * @author Brainpower - 29/01/2023
 */
class AreaSound(
    /** Type of sound (2 or 3 indicates spoken audio) */
    @JvmField val type: Byte,

    /** ID of the sound to play */
    @JvmField val soundId: Int,

    /** Number of times to loop the sound */
    @JvmField val loops: Int,

    /** Delay before playing the sound */
    @JvmField var delay: Int,

    /** Volume level of the sound */
    @JvmField val volume: Int,

    /** Bitpacked position data */
    @JvmField val bitpackedPosition: Int,

    /** Sample rate for playback */
    @JvmField val sampleRate: Int,

    sceneObject: SceneGraphEntity?
) {
    @JvmField var midiInstrument: MusicSample? = null
    @JvmField var buffer: AudioBuffer? = null
    @JvmField var soundEffect: SoundEffect? = null
    @JvmField var areaSoundStream: AreaSoundStream? = null

    /** Starting X coordinate in the world */
    @JvmField var startX: Int = -1

    /** Starting Z coordinate in the world */
    @JvmField var startZ: Int = -1

    init {
        if (sceneObject is AbstractInteractiveEntity) {
            this.startX = sceneObject.startX.toInt()
            this.startZ = sceneObject.startZ.toInt()
        }
    }

    /**
     * Returns true if this sound is spoken dialogue (types 2 or 3)
     */
    fun spoken(): Boolean = type.toInt() == 2 || type.toInt() == 3
}
