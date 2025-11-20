package com.jagex.game.runetek5.sound.player.sequence;

import com.jagex.game.runetek5.client.PreferenceManager;
import com.jagex.game.runetek5.config.midi.AreaSound;
import com.jagex.game.runetek5.config.sequence.SeqType;
import com.jagex.game.runetek5.entity.SceneGraphEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.math.Vector3;
import com.jagex.game.runetek5.sound.player.area.AreaSoundPlayer;
import com.jagex.game.runetek5.utils.MathUtils;

public class SequenceSoundPlayer {

	public static void playSequenceSound(SeqType seqtype, int frameId, SceneGraphEntity sceneObject) {
	    if (AreaSoundPlayer.AREA_SOUND_CACHE_SIZE < 50 && seqtype != null && seqtype.frameSounds != null && frameId < seqtype.frameSounds.length
	    		&& seqtype.frameSounds[frameId] != null && sceneObject.plane == PlayerUpdate.MY_PLAYER.plane) {
	        int bitpacked = seqtype.frameSounds[frameId][0];
	        int soundId = bitpacked >> 8;
	        int timesRepeat = bitpacked >> 5 & 0x7;
	        int positionOffset = bitpacked & 0x1f;
	        if (seqtype.frameSounds[frameId].length > 1) {
	            int random = (int) (Math.random() * seqtype.frameSounds[frameId].length);
	            if (random > 0) {
	                soundId = seqtype.frameSounds[frameId][random];
	            }
	        }
	
	        int sampleRate = 256;
	        if (seqtype.soundMaxDelay != null && seqtype.soundMinDelay != null) {
	            sampleRate = (int) (Math.random() * (seqtype.soundMinDelay[frameId] - seqtype.soundMaxDelay[frameId])) + seqtype.soundMaxDelay[frameId];
	        }
	
	        int volume = seqtype.frameSoundVolume == null ? 255 : seqtype.frameSoundVolume[frameId];
	        if (positionOffset == 0) {
	            if (sceneObject == PlayerUpdate.MY_PLAYER) {
	                if (seqtype.vorbis) {
	                    AreaSoundPlayer.playSoundVorbis(soundId, timesRepeat, 0, volume, false, sampleRate);
	                } else {
	                    AreaSoundPlayer.playSoundSynth(soundId, timesRepeat, 0, volume, sampleRate);
	                }
	            }
	        } else {
	            if (sceneObject == PlayerUpdate.MY_PLAYER) {
	                if (PreferenceManager.preferences.soundEffectVolume.getValue() == 0) {
	                    return;
	                }
	            } else if (PreferenceManager.preferences.ambientSoundVolume.getValue() == 0) {
	                return;
	            }
	
	            Vector3 absolute = sceneObject.getAbsolutePosition().coords;
	            int sceneX = (int) absolute.x - 256 >> 9;
	            int sceneZ = (int) absolute.z - 256 >> 9;
	            int bitpackedPosition = sceneObject == PlayerUpdate.MY_PLAYER ? 0 : positionOffset + (sceneZ << 8) + (sceneObject.plane << 24) + (sceneX << 16);
	            AreaSoundPlayer.AREA_SOUND_CACHE[++AreaSoundPlayer.AREA_SOUND_CACHE_SIZE - 1] = new AreaSound((byte) (seqtype.vorbis ? 2 : 1), soundId, timesRepeat, 0, volume, bitpackedPosition, sampleRate, sceneObject);
	        }
	    }
	
	}

	public static void playInterfaceSequenceSound(SeqType seqType, int frameId) {
	    if (AreaSoundPlayer.AREA_SOUND_CACHE_SIZE < 50 && seqType != null && seqType.frameSounds != null
	    		&& frameId < seqType.frameSounds.length && seqType.frameSounds[frameId] != null) {
	        int bitpacked = seqType.frameSounds[frameId][0];
	        int soundId = bitpacked >> 8;
	        int timesRepeated = bitpacked >> 5 & 0x7;
	        int randomSoundIndex;
	        if (seqType.frameSounds[frameId].length > 1) {
	            randomSoundIndex = (int) (Math.random() * seqType.frameSounds[frameId].length);
	            if (randomSoundIndex > 0) {
	                soundId = seqType.frameSounds[frameId][randomSoundIndex];
	            }
	        }
	        int sampleRate = 256;
	        if (seqType.soundMaxDelay != null && seqType.soundMinDelay != null) {
	            sampleRate = MathUtils.calculateSampleRate(seqType.soundMaxDelay[frameId], seqType.soundMinDelay[frameId]);
	        }
	        int volume = seqType.frameSoundVolume == null ? 255 : seqType.frameSoundVolume[frameId];
	        if (seqType.vorbis) {
	        	AreaSoundPlayer.playSoundVorbis(soundId, timesRepeated, 0, volume, false, sampleRate);
	        } else {
	        	AreaSoundPlayer.playSoundSynth(soundId, timesRepeated, 0, volume, sampleRate);
	        }
	    }
	}

}
