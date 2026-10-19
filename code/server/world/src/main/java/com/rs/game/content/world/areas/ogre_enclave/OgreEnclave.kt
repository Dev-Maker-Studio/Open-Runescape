package com.rs.game.content.world.areas.ogre_enclave

import com.rs.engine.dialogue.dialogue
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.World
import com.rs.game.content.quests.watchtower.STAGE_DISCOVER_SHAMANS
import com.rs.game.content.quests.watchtower.STAGE_FIND_SHAMAN_ENCLAVE
import com.rs.game.content.quests.watchtower.STAGE_RECOVER_CRYSTALS
import com.rs.game.content.quests.watchtower.WATCHTOWER_SHAMAN_VARBITS
import com.rs.game.content.quests.watchtower.recoverWatchtowerSkavidProgress
import com.rs.game.content.skills.mining.Pickaxe
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Spotanim
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.model.MoveType
import world.gregs.voidps.type.Tile

private const val SHAMAN_CAVE_GUARD_DISTRACTED = "watchtower_shaman_cave_guard_distracted"
private const val ENCLAVE_SCENE_SEEN = "watchtower_enclave_scene_seen"
private const val DALGROTH_CRYSTAL_OBTAINED = "watchtower_dalgroth_crystal_obtained"
private val ENCLAVE_ENTRANCE = Tile.of(2589, 9409, 0)
private val ENCLAVE_EXIT = Tile.of(2541, 3054, 0)

@ServerStartupEvent
fun mapOgreEnclave() {
    onObjectClick(Loc.shamanentrance) { (player) ->
        recoverWatchtowerSkavidProgress(player)
        if (player.isQuestComplete(Quest.WATCHTOWER)) {
            player.tele(ENCLAVE_ENTRANCE)
            return@onObjectClick
        }
        val stage = player.getQuestStage(Quest.WATCHTOWER)
        when {
            stage < STAGE_FIND_SHAMAN_ENCLAVE -> player.sendMessage("You should learn the skavid language and speak to the mad skavid before entering the enclave.")
            stage == STAGE_FIND_SHAMAN_ENCLAVE && !player.getBool(SHAMAN_CAVE_GUARD_DISTRACTED) -> player.startConversation {
                npc(Npc.enclave_guard, HeadE.CALM_TALK, "No you don't!")
            }
            stage == STAGE_FIND_SHAMAN_ENCLAVE -> player.fadeScreen {
                player.tele(ENCLAVE_ENTRANCE)
                player.setQuestStage(Quest.WATCHTOWER, STAGE_DISCOVER_SHAMANS)
                if (!player.getBool(ENCLAVE_SCENE_SEEN)) {
                    player.set(ENCLAVE_SCENE_SEEN, true)
                    playEnclaveScene(player)
                }
            }
            else -> player.tele(ENCLAVE_ENTRANCE)
        }
    }

    onObjectClick(Loc.di2_cave_wall_exit_light_brown, tiles = arrayOf(Tile.of(2598, 9470, 0))) { (player) -> player.tele(ENCLAVE_EXIT) }

    onNpcClick(*WATCHTOWER_SHAMAN_VARBITS.keys.toTypedArray<Any>(), options = arrayOf("Talk-to")) { (player, npc) ->
        if (!player.canEngageShaman(npc)) return@onNpcClick
        player.startConversation {
            npc(npc.getDefinitions(player).id, HeadE.ANGRY, "Grr! How dare you talk to us?")
            npc(npc.getDefinitions(player).id, HeadE.ANGRY, "Grr! How dare you talk to us?") {
                animateShamanAttack(npc, player)
                player.applyHit(Hit(npc, 200, Hit.HitLook.MAGIC_DAMAGE))
            }
        }
    }

    onNpcClick(*WATCHTOWER_SHAMAN_VARBITS.keys.toTypedArray<Any>(), options = arrayOf("Attack")) { (player, npc) ->
        if (!player.canEngageShaman(npc)) return@onNpcClick
        player.sendMessage("You attempt to attack the shaman...")
        player.startConversation {
            npc(npc.getDefinitions(player).id, HeadE.ANGRY, "You are no match for our magics!")
            npc(npc.getDefinitions(player).id, HeadE.ANGRY, "You are no match for our magics!") {
                animateShamanAttack(npc, player)
                player.sendMessage("A magic blast comes from the shaman.")
                player.sendMessage("You are badly injured by the blast.")
                player.applyHit(Hit(npc, maxOf(1, player.maxHitpoints / 2), Hit.HitLook.MAGIC_DAMAGE))
            }
        }
    }

    onObjectClick(Loc.rock_of_dalgroth) { (player, _, option) ->
        if (player.getQuestStage(Quest.WATCHTOWER) < STAGE_DISCOVER_SHAMANS) return@onObjectClick
        when {
            option.equals("Prospect", ignoreCase = true) -> prospectRock(player)
            option.equals("Mine", ignoreCase = true) -> mineRock(player)
        }
    }
}

private fun animateShamanAttack(npc: NPC, player: Player) {
    npc.faceEntityTile(player)
    npc.anim(Seq.chieftan_ogre_cast_spell)
    World.sendProjectile(npc, player, Spotanim.qip_watchtower_ogre_spell_travel, Pair(34, 16), 30, 5, 16)
}

private fun Player.canEngageShaman(npc: NPC): Boolean {
    if (isQuestComplete(Quest.WATCHTOWER)) return false
    val stage = getQuestStage(Quest.WATCHTOWER)
    if (stage < STAGE_DISCOVER_SHAMANS || stage >= STAGE_RECOVER_CRYSTALS) return false
    return WATCHTOWER_SHAMAN_VARBITS[npc.id]?.let { vars.getVarBit(it) == 0 } ?: true
}

private fun playEnclaveScene(player: Player) {
    player.playCutscene { cs ->
        cs.setEndTile(ENCLAVE_ENTRANCE)
        cs.fadeIn(5)
        cs.dynamicRegion(ENCLAVE_ENTRANCE, 322, 1176, 4, 6, false)
        cs.npcCreate("watchtowerShaman", Npc.watchtower_cutscene_ogre_shaman, 16, 28, 0)
        cs.npcCreate("watchtowerDragon", Npc.watchtower_cutscene_blue_dragon, 16, 23, 0)
        cs.playerMove(13, 1, 0, MoveType.TELEPORT)
        cs.npcFaceNPC("watchtowerShaman", "watchtowerDragon")
        cs.npcFaceNPC("watchtowerDragon", "watchtowerShaman")
        cs.camPos(7, 25, 3000)
        cs.camLook(16, 26, 0)
        cs.fadeOut(5)
        cs.dialogue(dialogue { simple("Meanwhile, in the Shaman's enclave...") })
        cs.npcAnim("watchtowerDragon", Seq.dragon_update_09_firebreath_attack)
        cs.action(1) {
            World.sendProjectile(
                cs.getNPC("watchtowerDragon"),
                cs.getNPC("watchtowerShaman"),
                Spotanim.dragon_ranged_attacks,
                Pair(34, 16),
                30,
                5,
                16
            )
        }
        cs.delay(2)
        cs.npcTalk("watchtowerShaman", "Hur, hut, hur. Stoopid dragon!")
        cs.delay(2)
        cs.npcTalk("watchtowerShaman", "Take dis!")
        cs.npcAnim("watchtowerShaman", Seq.chieftan_ogre_cast_spell)
        cs.action(1) {
            World.sendProjectile(
                cs.getNPC("watchtowerShaman"), cs.getNPC("watchtowerDragon"),
                Spotanim.qip_watchtower_ogre_spell_travel, Pair(34, 16), 30, 5, 16
            )
        }
        cs.delay(2)
        cs.action(1) {
            val dragon = cs.getNPC("watchtowerDragon")
            val shaman = cs.getNPC("watchtowerShaman")
            if (dragon != null && shaman != null && !dragon.isDead) {
                dragon.spotAnim(Spotanim.qip_watchtower_ogre_spell_impact)
                dragon.applyHit(Hit(shaman, dragon.hitpoints, Hit.HitLook.TRUE_DAMAGE))
            }
        }
        cs.delay(6)
        cs.npcDestroy("watchtowerDragon")
        cs.delay(1)
        cs.npcTalk("watchtowerShaman", "Hur, hur, hur, hur!")
        cs.delay(3)
        cs.fadeIn(5)
        cs.npcDestroy("watchtowerShaman")
        cs.camPosReset()
        cs.fadeOut(5)
    }
}

private fun prospectRock(player: Player) {
    player.sendMessage("You examine the rock for ores...")
    if (player.getBool(DALGROTH_CRYSTAL_OBTAINED)) {
        player.sendMessage("The rock has a gap in which a crystal used to sit.")
        return
    }
    player.sendMessage("The rock contains a crystal!")
}

private fun mineRock(player: Player) {
    if (player.isQuestComplete(Quest.WATCHTOWER)) return
    if (player.skills.getLevel(Skills.MINING) < 40) {
        player.sendMessage("You need a Mining level of 40 to mine this rock.")
        return
    }
    if (player.getQuestStage(Quest.WATCHTOWER) < STAGE_RECOVER_CRYSTALS) {
        player.startConversation {
            player(HeadE.CONFUSED, "I can't touch it... Perhaps it is linked with the shamans in some strange way?")
        }
        return
    }
    if (player.inventory.containsItem(Obj.powering_crystal4, 1) ||
        player.bank.containsItem(Obj.powering_crystal4, 1) ||
        player.vars.getVarBit(Varbit.watchtower_pillar_4) != 0
    ) {
        player.startConversation {
            player(HeadE.CALM_TALK, "I have already this crystal. There is no benefit to getting another.")
        }
        return
    }
    val pickaxe = Pickaxe.getBest(player)
    if (pickaxe == null) {
        player.sendMessage("You need a pickaxe and the level required to use it to mine.")
        return
    }
    player.anim(pickaxe.animId)
    player.set(DALGROTH_CRYSTAL_OBTAINED, true)
    player.inventory.addItemDrop(Obj.powering_crystal4, 1)
    player.sendMessage("A crack appears in the rock and you prise a crystal out.")
}
