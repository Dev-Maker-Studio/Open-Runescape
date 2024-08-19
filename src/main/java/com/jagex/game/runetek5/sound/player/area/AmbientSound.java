package com.jagex.game.runetek5.sound.player.area;

import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.cache.node.NodeCollection;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.object.ObjectType;
import com.jagex.game.runetek5.config.midi.instrument.MusicSample;
import com.jagex.game.runetek5.config.npc.NPCType;
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity;
import com.jagex.game.runetek5.sound.jagtheora.Node_Sub15_Sub4;
import com.jagex.game.runetek5.vardomain.VarProviders;
import com.jagex.game.runetek5.sound.jagtheora.AreaSoundStream;
import com.jagex.game.runetek5.sound.jagtheora.AudioBuffer;

public class AmbientSound extends Node<AmbientSound> {

	public static final NodeCollection LOC_AMBIENT_SOUNDS = new NodeCollection();
    public static final NodeCollection NPC_AMBIENT_SOUNDS = new NodeCollection();
    public static final IterableNodeMap PLAYER_AMBIENT_SOUNDS = new IterableNodeMap(16);
    public int plane;
    public int absEndY;
    public int absStartY;
    public int absEndX;
    public int step;
    public NpcEntity npcEntity;
    public PlayerEntity playerEntity;
    public MusicSample aNodeSub188087;
    public boolean hasTransformation;
    public AreaSoundStream aNodeSub15Sub58096;
    public ObjectType emittingLoc;
    public AudioBuffer aNodeSub26Sub1Sub18106;
    public int absStartX;
    public int soundEffectId;
    public boolean instrumentSoundEffect;
    public int ambientSoundMinHearDistance;
    public int ambientSoundMaxHearDistance;
    public int ambientSoundVolume;
    public int minDelay;
    public int maxDelay;
    public int soundMinStep;
    public int soundMaxStep;
    public int[] ambientSoundGroupIds;
    public boolean instrumentAmbientSound;
    public AreaSoundStream soundEffect;
    public AudioBuffer aNodeSub26Sub1Sub18082;
    public MusicSample instrumentEffect;
    public int animationType;
	public static Node_Sub15_Sub4 gameSounds;

    public static void update() {
        AmbientSound ambientSound;
        for (ambientSound = (AmbientSound) LOC_AMBIENT_SOUNDS.getFirst(); ambientSound != null; ambientSound = (AmbientSound) LOC_AMBIENT_SOUNDS.next()) {
            if (ambientSound.hasTransformation) {
                ambientSound.load();
            }
        }
        for (ambientSound = (AmbientSound) NPC_AMBIENT_SOUNDS.getFirst(); ambientSound != null; ambientSound = (AmbientSound) NPC_AMBIENT_SOUNDS.next()) {
            if (ambientSound.hasTransformation) {
                ambientSound.load();
            }
        }
    }

    public void load() {
        int currentEffect = this.soundEffectId;
        boolean currentTransformState = this.instrumentSoundEffect;
        if (this.emittingLoc != null) {
            ObjectType loctype = this.emittingLoc.getMultiLoc(client.MAP_LOADING_STAGE == 4 ? VarProviders.CUTSCENE_ACTION_VAR_PROVIDER : VarProviders.PLAYER_VAR_PROVIDER);
            if (loctype != null) {
                this.soundEffectId = loctype.soundId;
                this.instrumentSoundEffect = loctype.instrumentSoundEffect;
                this.ambientSoundMinHearDistance = loctype.soundRadius << 9;
                this.ambientSoundVolume = loctype.soundVolume;
                this.soundMinStep = loctype.soundMinInterval;
                this.soundMaxStep = loctype.soundMaxInterval;
                this.ambientSoundGroupIds = loctype.soundGroupIds;
                this.instrumentAmbientSound = loctype.instrumentAmbientSound;
                this.minDelay = loctype.ambientSoundMinDelay;
                this.maxDelay = loctype.ambientSoundMaxDelay;
            } else {
                this.soundEffectId = -1;
                this.instrumentSoundEffect = false;
                this.ambientSoundMinHearDistance = 0;
                this.ambientSoundVolume = 0;
                this.soundMinStep = 0;
                this.soundMaxStep = 0;
                this.ambientSoundGroupIds = null;
                this.instrumentAmbientSound = false;
                this.minDelay = 256;
                this.maxDelay = 256;
                this.ambientSoundMaxHearDistance = 0;
            }
        } else if (this.npcEntity != null) {
            int transformedEffect = SoundEffectPlayer.getSoundEffectId(this.npcEntity);
            if (currentEffect != transformedEffect) {
                this.soundEffectId = transformedEffect;
                NPCType npcType = this.npcEntity.definitions;
                if (npcType.alternativeIds != null) {
                    npcType = npcType.getMultiNpc(VarProviders.PLAYER_VAR_PROVIDER);
                }
                if (npcType != null) {
                    this.ambientSoundMinHearDistance = npcType.ambientSoundMinHearDistance << 9;
                    this.ambientSoundMaxHearDistance = npcType.ambientSoundMaxHearDistance << 9;
                    this.ambientSoundVolume = npcType.ambientSoundVolume;
                    this.instrumentSoundEffect = npcType.instrumentSoundEffect;
                    this.minDelay = npcType.ambientSoundMinDelay;
                    this.maxDelay = npcType.ambientSoundMaxDelay;
                } else {
                    this.ambientSoundMaxHearDistance = 0;
                    this.ambientSoundMinHearDistance = 0;
                    this.ambientSoundVolume = 0;
                    this.instrumentSoundEffect = this.npcEntity.definitions.instrumentSoundEffect;
                    this.minDelay = 256;
                    this.maxDelay = 256;
                }
            }
        } else if (this.playerEntity != null) {
            this.soundEffectId = SoundEffectPlayer.getSoundEffectId(this.playerEntity);
            this.instrumentSoundEffect = this.playerEntity.instrumentSoundEffect;
            this.ambientSoundMinHearDistance = this.playerEntity.isNpc << 9;
            this.ambientSoundMaxHearDistance = 0;
            this.ambientSoundVolume = this.playerEntity.ambientSoundVolume;
            this.minDelay = 256;
            this.maxDelay = 256;
        }
        if ((currentEffect != this.soundEffectId || currentTransformState != this.instrumentSoundEffect) && this.soundEffect != null) {
            AmbientSound.gameSounds.unlink(this.soundEffect);
            this.soundEffect = null;
            this.aNodeSub26Sub1Sub18082 = null;
            this.instrumentEffect = null;
        }
    }

	public static void unlinkEntitySound(NpcEntity entity) {
	    for (AmbientSound ambientSound = (AmbientSound) NPC_AMBIENT_SOUNDS.getFirst(); ambientSound != null; ambientSound = (AmbientSound) NPC_AMBIENT_SOUNDS.next()) {
	        if (ambientSound.npcEntity == entity) {
	            if (ambientSound.soundEffect != null) {
	                gameSounds.unlink(ambientSound.soundEffect);
	                ambientSound.soundEffect = null;
	            }
	            ambientSound.unlink();
	            break;
	        }
	    }
	}

	public static void unlinkLocSound(int z, int x, int y, ObjectType locType) {
	    for (AmbientSound soundNode = (AmbientSound) LOC_AMBIENT_SOUNDS.getFirst(); soundNode != null; soundNode = (AmbientSound) LOC_AMBIENT_SOUNDS.next()) {
	        if (z == soundNode.plane && soundNode.absStartX == x << 9 && y << 9 == soundNode.absStartY && soundNode.emittingLoc.id == locType.id) {
	            if (soundNode.soundEffect != null) {
	                gameSounds.unlink(soundNode.soundEffect);
	                soundNode.soundEffect = null;
	            }
	            if (soundNode.aNodeSub15Sub58096 != null) {
	                gameSounds.unlink(soundNode.aNodeSub15Sub58096);
	                soundNode.aNodeSub15Sub58096 = null;
	            }
	            soundNode.unlink();
	            break;
	        }
	    }
	}

	public static void unlinkSounds(boolean logout) {
	    AmbientSound ambientSound;
	    for (ambientSound = (AmbientSound) LOC_AMBIENT_SOUNDS.getFirst(); ambientSound != null; ambientSound = (AmbientSound) LOC_AMBIENT_SOUNDS.next()) {
	        if (ambientSound.soundEffect != null) {
	            gameSounds.unlink(ambientSound.soundEffect);
	            ambientSound.soundEffect = null;
	        }
	        if (ambientSound.aNodeSub15Sub58096 != null) {
	            gameSounds.unlink(ambientSound.aNodeSub15Sub58096);
	            ambientSound.aNodeSub15Sub58096 = null;
	        }
	        ambientSound.unlink();
	    }
	    if (logout) {
	        for (ambientSound = (AmbientSound) NPC_AMBIENT_SOUNDS.getFirst(); ambientSound != null; ambientSound = (AmbientSound) NPC_AMBIENT_SOUNDS.next()) {
	            if (ambientSound.soundEffect != null) {
	                gameSounds.unlink(ambientSound.soundEffect);
	                ambientSound.soundEffect = null;
	            }
	            ambientSound.unlink();
	        }
	        for (ambientSound = (AmbientSound) PLAYER_AMBIENT_SOUNDS.first(); ambientSound != null; ambientSound = (AmbientSound) PLAYER_AMBIENT_SOUNDS.next()) {
	            if (ambientSound.soundEffect != null) {
	                gameSounds.unlink(ambientSound.soundEffect);
	                ambientSound.soundEffect = null;
	            }
	            ambientSound.unlink();
	        }
	    }
	}

	public static void unlinkPlayerSound(PlayerEntity player) {
	    AmbientSound ambientSound = (AmbientSound) PLAYER_AMBIENT_SOUNDS.get(player.playerIndex);
	    if (ambientSound != null) {
	        if (ambientSound.soundEffect != null) {
	            gameSounds.unlink(ambientSound.soundEffect);
	            ambientSound.soundEffect = null;
	        }
	        ambientSound.unlink();
	    }
	}

	public static void createAmbientSoundEffect(int effectPlane, int baseX, int baseY, int rotation, ObjectType locType, NpcEntity npc, PlayerEntity player) {
	    AmbientSound soundEffectNode = new AmbientSound();
	    soundEffectNode.plane = effectPlane;
	    soundEffectNode.absStartX = baseX << 9;
	    soundEffectNode.absStartY = baseY << 9;
	    if (locType != null) {
	        soundEffectNode.emittingLoc = locType;
	        int sizeX = locType.sizeX;
	        int sizeY = locType.sizeY;
	        if (rotation == 1 || rotation == 3) {
	            sizeX = locType.sizeY;
	            sizeY = locType.sizeX;
	        }
	        soundEffectNode.absEndX = sizeX + baseX << 9;
	        soundEffectNode.absEndY = sizeY + baseY << 9;
	        soundEffectNode.soundEffectId = locType.soundId;
	        soundEffectNode.instrumentSoundEffect = locType.instrumentSoundEffect;
	        soundEffectNode.ambientSoundMinHearDistance = locType.soundRadius << 9;
	        soundEffectNode.ambientSoundVolume = locType.soundVolume;
	        soundEffectNode.soundMinStep = locType.soundMinInterval;
	        soundEffectNode.soundMaxStep = locType.soundMaxInterval;
	        soundEffectNode.ambientSoundGroupIds = locType.soundGroupIds;
	        soundEffectNode.instrumentAmbientSound = locType.instrumentAmbientSound;
	        soundEffectNode.minDelay = locType.ambientSoundMinDelay;
	        soundEffectNode.maxDelay = locType.ambientSoundMaxDelay;
	        soundEffectNode.ambientSoundMaxHearDistance = locType.ambientSoundMaxHearDistance << 9;
	        if (locType.alternativeIds != null) {
	            soundEffectNode.hasTransformation = true;
	            soundEffectNode.load();
	        }
	        if (soundEffectNode.ambientSoundGroupIds != null) {
	            soundEffectNode.step = soundEffectNode.soundMinStep + (int) (Math.random() * (soundEffectNode.soundMaxStep - soundEffectNode.soundMinStep));
	        }
	        LOC_AMBIENT_SOUNDS.addFirst(soundEffectNode);
	    } else if (npc != null) {
	        soundEffectNode.npcEntity = npc;
	        NPCType npcType = npc.definitions;
	        if (npcType.alternativeIds != null) {
	            soundEffectNode.hasTransformation = true;
	            npcType = npcType.getMultiNpc(VarProviders.PLAYER_VAR_PROVIDER);
	        }
	        if (npcType != null) {
	            soundEffectNode.absEndX = baseX + npcType.size << 9;
	            soundEffectNode.absEndY = baseY + npcType.size << 9;
	            soundEffectNode.soundEffectId = SoundEffectPlayer.getSoundEffectId(npc);
	            soundEffectNode.instrumentSoundEffect = npcType.instrumentSoundEffect;
	            soundEffectNode.ambientSoundMinHearDistance = npcType.ambientSoundMinHearDistance << 9;
	            soundEffectNode.ambientSoundVolume = npcType.ambientSoundVolume;
	            soundEffectNode.minDelay = npcType.ambientSoundMinDelay;
	            soundEffectNode.maxDelay = npcType.ambientSoundMaxDelay;
	            soundEffectNode.ambientSoundMaxHearDistance = npcType.ambientSoundMaxHearDistance << 9;
	        }
	        NPC_AMBIENT_SOUNDS.addFirst(soundEffectNode);
	    } else if (player != null) {
	        soundEffectNode.playerEntity = player;
	        soundEffectNode.absEndX = baseX + player.getSize() << 9;
	        soundEffectNode.absEndY = baseY + player.getSize() << 9;
	        soundEffectNode.soundEffectId = SoundEffectPlayer.getSoundEffectId(player);
	        soundEffectNode.instrumentSoundEffect = player.instrumentSoundEffect;
	        soundEffectNode.ambientSoundMinHearDistance = player.isNpc << 9;
	        soundEffectNode.ambientSoundVolume = player.ambientSoundVolume;
	        soundEffectNode.minDelay = 256;
	        soundEffectNode.maxDelay = 256;
	        soundEffectNode.ambientSoundMaxHearDistance = 0;
	        PLAYER_AMBIENT_SOUNDS.put(soundEffectNode, player.playerIndex);
	    }
	}
}
