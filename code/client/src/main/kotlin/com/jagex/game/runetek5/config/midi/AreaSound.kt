package com.jagex.game.runetek5.config.midi

import com.jagex.game.runetek5.config.midi.instrument.MusicSample
import com.jagex.game.runetek5.entity.AbstractInteractiveEntity
import com.jagex.game.runetek5.entity.SceneGraphEntity
import com.jagex.game.runetek5.sound.jagtheora.AreaSoundStream
import com.jagex.game.runetek5.sound.jagtheora.AudioBuffer

class AreaSound(
    @JvmField val type: Byte,

    @JvmField val soundId: Int,

    @JvmField val loops: Int,

    @JvmField var delay: Int,

    @JvmField val volume: Int,

    @JvmField val bitpackedPosition: Int,

    @JvmField val sampleRate: Int,

    sceneObject: SceneGraphEntity?
) {
    @JvmField var midiInstrument: MusicSample? = null
    @JvmField var buffer: AudioBuffer? = null
    @JvmField var soundEffect: SoundEffect? = null
    @JvmField var areaSoundStream: AreaSoundStream? = null

    @JvmField var startX: Int = -1

    @JvmField var startZ: Int = -1

    init {
        if (sceneObject is AbstractInteractiveEntity) {
            this.startX = sceneObject.startX.toInt()
            this.startZ = sceneObject.startZ.toInt()
        }
    }

    fun spoken(): Boolean = type.toInt() == 2 || type.toInt() == 3
}
