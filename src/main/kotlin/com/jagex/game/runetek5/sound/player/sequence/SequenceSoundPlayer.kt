package com.jagex.game.runetek5.sound.player.sequence

import com.jagex.game.runetek5.client.PreferenceManager
import com.jagex.game.runetek5.config.midi.AreaSound
import com.jagex.game.runetek5.config.sequence.SeqType
import com.jagex.game.runetek5.entity.SceneGraphEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.math.Vector3
import com.jagex.game.runetek5.sound.player.area.AreaSoundPlayer
import com.jagex.game.runetek5.utils.MathUtils

object SequenceSoundPlayer {

    @JvmStatic
    fun playSequenceSound(seqtype: SeqType?, frameId: Int, sceneObject: SceneGraphEntity) {
        if (AreaSoundPlayer.AREA_SOUND_CACHE_SIZE < 50 && seqtype != null && seqtype.frameSounds != null && frameId < seqtype.frameSounds.size
            && seqtype.frameSounds[frameId] != null && sceneObject.plane == PlayerUpdate.MY_PLAYER.plane
        ) {
            val bitpacked = seqtype.frameSounds[frameId]!![0]
            var soundId = bitpacked shr 8
            val timesRepeat = bitpacked shr 5 and 0x7
            val positionOffset = bitpacked and 0x1f
            if (seqtype.frameSounds[frameId]!!.size > 1) {
                val random = (Math.random() * seqtype.frameSounds[frameId]!!.size).toInt()
                if (random > 0) {
                    soundId = seqtype.frameSounds[frameId]!![random]
                }
            }

            var sampleRate = 256
            if (seqtype.soundMaxDelay != null && seqtype.soundMinDelay != null) {
                sampleRate = (Math.random() * (seqtype.soundMinDelay[frameId] - seqtype.soundMaxDelay[frameId])).toInt() + seqtype.soundMaxDelay[frameId]
            }

            val volume = seqtype.frameSoundVolume?.get(frameId) ?: 255
            if (positionOffset == 0) {
                if (sceneObject == PlayerUpdate.MY_PLAYER) {
                    if (seqtype.vorbis) {
                        AreaSoundPlayer.playSoundVorbis(soundId, timesRepeat, 0, volume, false, sampleRate)
                    } else {
                        AreaSoundPlayer.playSoundSynth(soundId, timesRepeat, 0, volume, sampleRate)
                    }
                }
            } else {
                if (sceneObject == PlayerUpdate.MY_PLAYER) {
                    if (PreferenceManager.preferences.soundEffectVolume.getValue() == 0) {
                        return
                    }
                } else if (PreferenceManager.preferences.ambientSoundVolume.getValue() == 0) {
                    return
                }

                val absolute: Vector3 = sceneObject.getAbsolutePosition().coords
                val sceneX = ((absolute.x.toInt() - 256) shr 9)
                val sceneZ = ((absolute.z.toInt() - 256) shr 9)
                val bitpackedPosition = if (sceneObject == PlayerUpdate.MY_PLAYER) 0 else positionOffset + (sceneZ shl 8) + (sceneObject.plane.toInt() shl 24) + (sceneX shl 16)
                AreaSoundPlayer.AREA_SOUND_CACHE[++AreaSoundPlayer.AREA_SOUND_CACHE_SIZE - 1] = AreaSound((if (seqtype.vorbis) 2 else 1).toByte(), soundId, timesRepeat, 0, volume, bitpackedPosition, sampleRate, sceneObject)
            }
        }
    }

    @JvmStatic
    fun playInterfaceSequenceSound(seqType: SeqType?, frameId: Int) {
        if (AreaSoundPlayer.AREA_SOUND_CACHE_SIZE < 50 && seqType != null && seqType.frameSounds != null
            && frameId < seqType.frameSounds.size && seqType.frameSounds[frameId] != null
        ) {
            val bitpacked = seqType.frameSounds[frameId]!![0]
            var soundId = bitpacked shr 8
            val timesRepeated = bitpacked shr 5 and 0x7
            if (seqType.frameSounds[frameId]!!.size > 1) {
                val randomSoundIndex = (Math.random() * seqType.frameSounds[frameId]!!.size).toInt()
                if (randomSoundIndex > 0) {
                    soundId = seqType.frameSounds[frameId]!![randomSoundIndex]
                }
            }
            var sampleRate = 256
            if (seqType.soundMaxDelay != null && seqType.soundMinDelay != null) {
                sampleRate = MathUtils.calculateSampleRate(seqType.soundMaxDelay[frameId], seqType.soundMinDelay[frameId])
            }
            val volume = seqType.frameSoundVolume?.get(frameId) ?: 255
            if (seqType.vorbis) {
                AreaSoundPlayer.playSoundVorbis(soundId, timesRepeated, 0, volume, false, sampleRate)
            } else {
                AreaSoundPlayer.playSoundSynth(soundId, timesRepeated, 0, volume, sampleRate)
            }
        }
    }
}
