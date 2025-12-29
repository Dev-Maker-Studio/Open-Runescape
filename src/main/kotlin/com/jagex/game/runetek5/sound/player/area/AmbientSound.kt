package com.jagex.game.runetek5.sound.player.area

import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.cache.node.NodeCollection
import com.jagex.game.runetek5.client.client
import com.jagex.game.runetek5.config.`object`.ObjectType
import com.jagex.game.runetek5.config.midi.instrument.MusicSample
import com.jagex.game.runetek5.config.npc.NPCType
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity
import com.jagex.game.runetek5.sound.jagtheora.Node_Sub15_Sub4
import com.jagex.game.runetek5.vardomain.VarProviders
import com.jagex.game.runetek5.sound.jagtheora.AreaSoundStream
import com.jagex.game.runetek5.sound.jagtheora.AudioBuffer

class AmbientSound : Node<AmbientSound>() {

    @JvmField
    var plane: Int = 0

    @JvmField
    var absEndY: Int = 0

    @JvmField
    var absStartY: Int = 0

    @JvmField
    var absEndX: Int = 0

    @JvmField
    var step: Int = 0

    @JvmField
    var npcEntity: NpcEntity? = null

    @JvmField
    var playerEntity: PlayerEntity? = null

    @JvmField
    var aNodeSub188087: MusicSample? = null

    @JvmField
    var hasTransformation: Boolean = false

    @JvmField
    var aNodeSub15Sub58096: AreaSoundStream? = null

    @JvmField
    var emittingLoc: ObjectType? = null

    @JvmField
    var aNodeSub26Sub1Sub18106: AudioBuffer? = null

    @JvmField
    var absStartX: Int = 0

    @JvmField
    var soundEffectId: Int = 0

    @JvmField
    var instrumentSoundEffect: Boolean = false

    @JvmField
    var ambientSoundMinHearDistance: Int = 0

    @JvmField
    var ambientSoundMaxHearDistance: Int = 0

    @JvmField
    var ambientSoundVolume: Int = 0

    @JvmField
    var minDelay: Int = 0

    @JvmField
    var maxDelay: Int = 0

    @JvmField
    var soundMinStep: Int = 0

    @JvmField
    var soundMaxStep: Int = 0

    @JvmField
    var ambientSoundGroupIds: IntArray? = null

    @JvmField
    var instrumentAmbientSound: Boolean = false

    @JvmField
    var soundEffect: AreaSoundStream? = null

    @JvmField
    var aNodeSub26Sub1Sub18082: AudioBuffer? = null

    @JvmField
    var instrumentEffect: MusicSample? = null

    @JvmField
    var animationType: Int = 0

    fun load() {
        val currentEffect = soundEffectId
        val currentTransformState = instrumentSoundEffect
        if (emittingLoc != null) {
            val loctype = emittingLoc!!.getMultiLoc(
                if (client.MAP_LOADING_STAGE == 4) VarProviders.CUTSCENE_ACTION_VAR_PROVIDER else VarProviders.PLAYER_VAR_PROVIDER
            )
            if (loctype != null) {
                soundEffectId = loctype.soundId
                instrumentSoundEffect = loctype.instrumentSoundEffect
                ambientSoundMinHearDistance = loctype.soundRadius shl 9
                ambientSoundVolume = loctype.soundVolume
                soundMinStep = loctype.soundMinInterval
                soundMaxStep = loctype.soundMaxInterval
                ambientSoundGroupIds = loctype.soundGroupIds
                instrumentAmbientSound = loctype.instrumentAmbientSound
                minDelay = loctype.ambientSoundMinDelay
                maxDelay = loctype.ambientSoundMaxDelay
            } else {
                soundEffectId = -1
                instrumentSoundEffect = false
                ambientSoundMinHearDistance = 0
                ambientSoundVolume = 0
                soundMinStep = 0
                soundMaxStep = 0
                ambientSoundGroupIds = null
                instrumentAmbientSound = false
                minDelay = 256
                maxDelay = 256
                ambientSoundMaxHearDistance = 0
            }
        } else if (npcEntity != null) {
            val transformedEffect = SoundEffectPlayer.getSoundEffectId(npcEntity!!)
            if (currentEffect != transformedEffect) {
                soundEffectId = transformedEffect
                var npcType = npcEntity!!.definitions
                if (npcType!!.alternativeIds != null) {
                    npcType = npcType.getMultiNpc(VarProviders.PLAYER_VAR_PROVIDER)
                }
                if (npcType != null) {
                    ambientSoundMinHearDistance = npcType!!.ambientSoundMinHearDistance shl 9
                    ambientSoundMaxHearDistance = npcType.ambientSoundMaxHearDistance shl 9
                    ambientSoundVolume = npcType.ambientSoundVolume
                    instrumentSoundEffect = npcType.instrumentSoundEffect
                    minDelay = npcType.ambientSoundMinDelay
                    maxDelay = npcType.ambientSoundMaxDelay
                } else {
                    ambientSoundMaxHearDistance = 0
                    ambientSoundMinHearDistance = 0
                    ambientSoundVolume = 0
                    instrumentSoundEffect = npcEntity!!.definitions!!.instrumentSoundEffect
                    minDelay = 256
                    maxDelay = 256
                }
            }
        } else if (playerEntity != null) {
            soundEffectId = SoundEffectPlayer.getSoundEffectId(playerEntity!!)
            instrumentSoundEffect = playerEntity!!.instrumentSoundEffect
            ambientSoundMinHearDistance = playerEntity!!.isNpc shl 9
            ambientSoundMaxHearDistance = 0
            ambientSoundVolume = playerEntity!!.ambientSoundVolume
            minDelay = 256
            maxDelay = 256
        }
        if (currentEffect != soundEffectId || currentTransformState != instrumentSoundEffect) {
            val effect = soundEffect
            if (effect != null) {
                gameSounds!!.unlink(effect)
                soundEffect = null
                aNodeSub26Sub1Sub18082 = null
                instrumentEffect = null
            }
        }
    }

    companion object {
        @JvmField
        val LOC_AMBIENT_SOUNDS = NodeCollection()

        @JvmField
        val NPC_AMBIENT_SOUNDS = NodeCollection()

        @JvmField
        val PLAYER_AMBIENT_SOUNDS: IterableNodeMap<AmbientSound> = IterableNodeMap(16)

        @JvmField
        var gameSounds: Node_Sub15_Sub4? = null

        @JvmStatic
        fun update() {
            var ambientSound = LOC_AMBIENT_SOUNDS.first() as AmbientSound?
            while (ambientSound != null) {
                if (ambientSound.hasTransformation) {
                    ambientSound.load()
                }
                ambientSound = LOC_AMBIENT_SOUNDS.next() as AmbientSound?
            }
            ambientSound = NPC_AMBIENT_SOUNDS.first() as AmbientSound?
            while (ambientSound != null) {
                if (ambientSound.hasTransformation) {
                    ambientSound.load()
                }
                ambientSound = NPC_AMBIENT_SOUNDS.next() as AmbientSound?
            }
        }

        @JvmStatic
        fun unlinkEntitySound(entity: NpcEntity) {
            var ambientSound = NPC_AMBIENT_SOUNDS.first() as AmbientSound?
            while (ambientSound != null) {
                if (ambientSound.npcEntity == entity) {
                    val effect = ambientSound.soundEffect
                    if (effect != null) {
                        gameSounds!!.unlink(effect)
                        ambientSound.soundEffect = null
                    }
                    ambientSound.unlink()
                    break
                }
                ambientSound = NPC_AMBIENT_SOUNDS.next() as AmbientSound?
            }
        }

        @JvmStatic
        fun unlinkLocSound(z: Int, x: Int, y: Int, locType: ObjectType) {
            var soundNode = LOC_AMBIENT_SOUNDS.first() as AmbientSound?
            while (soundNode != null) {
                if (z == soundNode.plane && soundNode.absStartX == x shl 9 && y shl 9 == soundNode.absStartY && soundNode.emittingLoc!!.id == locType.id) {
                    val effect = soundNode.soundEffect
                    if (effect != null) {
                        gameSounds!!.unlink(effect)
                        soundNode.soundEffect = null
                    }
                    val effect2 = soundNode.aNodeSub15Sub58096
                    if (effect2 != null) {
                        gameSounds!!.unlink(effect2)
                        soundNode.aNodeSub15Sub58096 = null
                    }
                    soundNode.unlink()
                    break
                }
                soundNode = LOC_AMBIENT_SOUNDS.next() as AmbientSound?
            }
        }

        @JvmStatic
        fun unlinkSounds(logout: Boolean) {
            var ambientSound = LOC_AMBIENT_SOUNDS.first() as AmbientSound?
            while (ambientSound != null) {
                val effect = ambientSound.soundEffect
                if (effect != null) {
                    gameSounds!!.unlink(effect)
                    ambientSound.soundEffect = null
                }
                val effect2 = ambientSound.aNodeSub15Sub58096
                if (effect2 != null) {
                    gameSounds!!.unlink(effect2)
                    ambientSound.aNodeSub15Sub58096 = null
                }
                ambientSound.unlink()
                ambientSound = LOC_AMBIENT_SOUNDS.next() as AmbientSound?
            }
            if (logout) {
                ambientSound = NPC_AMBIENT_SOUNDS.first() as AmbientSound?
                while (ambientSound != null) {
                    val effect = ambientSound.soundEffect
                    if (effect != null) {
                        gameSounds!!.unlink(effect)
                        ambientSound.soundEffect = null
                    }
                    ambientSound.unlink()
                    ambientSound = NPC_AMBIENT_SOUNDS.next() as AmbientSound?
                }
                ambientSound = PLAYER_AMBIENT_SOUNDS.first() as AmbientSound?
                while (ambientSound != null) {
                    val effect = ambientSound.soundEffect
                    if (effect != null) {
                        gameSounds!!.unlink(effect)
                        ambientSound.soundEffect = null
                    }
                    ambientSound.unlink()
                    ambientSound = PLAYER_AMBIENT_SOUNDS.next() as AmbientSound?
                }
            }
        }

        @JvmStatic
        fun unlinkPlayerSound(player: PlayerEntity) {
            val ambientSound = PLAYER_AMBIENT_SOUNDS.get(player.playerIndex.toLong()) as AmbientSound?
            if (ambientSound != null) {
                val effect = ambientSound.soundEffect
                if (effect != null) {
                    gameSounds!!.unlink(effect)
                    ambientSound.soundEffect = null
                }
                ambientSound.unlink()
            }
        }

        @JvmStatic
        fun createAmbientSoundEffect(
            effectPlane: Int,
            baseX: Int,
            baseY: Int,
            rotation: Int,
            locType: ObjectType?,
            npc: NpcEntity?,
            player: PlayerEntity?
        ) {
            val soundEffectNode = AmbientSound()
            soundEffectNode.plane = effectPlane
            soundEffectNode.absStartX = baseX shl 9
            soundEffectNode.absStartY = baseY shl 9
            if (locType != null) {
                soundEffectNode.emittingLoc = locType
                var sizeX = locType.sizeX
                var sizeY = locType.sizeY
                if (rotation == 1 || rotation == 3) {
                    sizeX = locType.sizeY
                    sizeY = locType.sizeX
                }
                soundEffectNode.absEndX  = ((sizeX + baseX).toInt()) shl 9
                soundEffectNode.absEndY  = ((sizeY + baseY).toInt()) shl 9
                soundEffectNode.soundEffectId = locType.soundId
                soundEffectNode.instrumentSoundEffect = locType.instrumentSoundEffect
                soundEffectNode.ambientSoundMinHearDistance = locType.soundRadius shl 9
                soundEffectNode.ambientSoundVolume = locType.soundVolume
                soundEffectNode.soundMinStep = locType.soundMinInterval
                soundEffectNode.soundMaxStep = locType.soundMaxInterval
                soundEffectNode.ambientSoundGroupIds = locType.soundGroupIds
                soundEffectNode.instrumentAmbientSound = locType.instrumentAmbientSound
                soundEffectNode.minDelay = locType.ambientSoundMinDelay
                soundEffectNode.maxDelay = locType.ambientSoundMaxDelay
                soundEffectNode.ambientSoundMaxHearDistance = locType.ambientSoundMaxHearDistance shl 9
                if (locType.alternativeIds != null) {
                    soundEffectNode.hasTransformation = true
                    soundEffectNode.load()
                }
                if (soundEffectNode.ambientSoundGroupIds != null) {
                    soundEffectNode.step = soundEffectNode.soundMinStep + (Math.random() * (soundEffectNode.soundMaxStep - soundEffectNode.soundMinStep)).toInt()
                }
                LOC_AMBIENT_SOUNDS.addFirst(soundEffectNode)
            } else if (npc != null) {
                soundEffectNode.npcEntity = npc
                var npcType = npc.definitions
                if (npcType!!.alternativeIds != null) {
                    soundEffectNode.hasTransformation = true
                    npcType = npcType.getMultiNpc(VarProviders.PLAYER_VAR_PROVIDER)
                }
                if (npcType != null) {
                    soundEffectNode.absEndX = ((baseX + npcType!!.size).toInt()) shl 9
                    soundEffectNode.absEndY  = ((baseY + npcType.size).toInt()) shl 9
                    soundEffectNode.soundEffectId = SoundEffectPlayer.getSoundEffectId(npc)
                    soundEffectNode.instrumentSoundEffect = npcType.instrumentSoundEffect
                    soundEffectNode.ambientSoundMinHearDistance = npcType.ambientSoundMinHearDistance shl 9
                    soundEffectNode.ambientSoundVolume = npcType.ambientSoundVolume
                    soundEffectNode.minDelay = npcType.ambientSoundMinDelay
                    soundEffectNode.maxDelay = npcType.ambientSoundMaxDelay
                    soundEffectNode.ambientSoundMaxHearDistance = npcType.ambientSoundMaxHearDistance shl 9
                }
                NPC_AMBIENT_SOUNDS.addFirst(soundEffectNode)
            } else if (player != null) {
                soundEffectNode.playerEntity = player
                soundEffectNode.absEndX = ((baseX + player.getSize()).toInt()) shl 9
                soundEffectNode.absEndY  = ((baseY + player.getSize()).toInt()) shl 9
                soundEffectNode.soundEffectId = SoundEffectPlayer.getSoundEffectId(player)
                soundEffectNode.instrumentSoundEffect = player.instrumentSoundEffect
                soundEffectNode.ambientSoundMinHearDistance = player.isNpc shl 9
                soundEffectNode.ambientSoundVolume = player.ambientSoundVolume
                soundEffectNode.minDelay = 256
                soundEffectNode.maxDelay = 256
                soundEffectNode.ambientSoundMaxHearDistance = 0
                PLAYER_AMBIENT_SOUNDS.put(soundEffectNode, player.playerIndex.toLong())
            }
        }
    }
}
