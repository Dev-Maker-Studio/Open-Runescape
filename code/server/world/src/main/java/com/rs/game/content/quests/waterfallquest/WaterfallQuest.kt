package com.rs.game.content.quests.waterfallquest

import com.rs.engine.quest.Quest
import com.rs.engine.quest.QuestHandler
import com.rs.engine.quest.QuestOutline
import com.rs.game.content.quests.rovingelves.GlarialsTombController
import com.rs.game.content.quests.rovingelves.validTombEquipment
import com.rs.game.content.world.doors.Doors
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.Hit.HitLook
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnObject
import com.rs.plugin.kts.onNpcClick
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.Constants
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import world.gregs.voidps.type.Tile

@QuestHandler(quest = Quest.WATERFALL_QUEST,
    startText = "Speak to Almera in her house north-east of the Baxtorian Falls.",
    itemsText = "Rope, 6 air runes, 6 earth runes, 6 water runes.",
    combatText = "None, but you must be able to survive some attacks from level 44 giant spiders and level 58 skeletons.",
    rewardsText = "13,750 Attack XP<br>13,750 Strength XP<br>2 gold bars<br>2 diamonds<br>40 mithril seeds<br>Access to the Waterfall Dungeon",
    completedStage = 6)
class WaterfallQuest : QuestOutline() {
    override fun getJournalLines(player: Player, stage: Int) = when (stage) {
        0 -> listOf("I can start this quest by speaking with Almera", "by the waterfall south of barbarian assault.")
        1 -> listOf("Almera is worried about her son Hudon. I should find", "him and see what he is up to.")
        2 -> listOf("Hudon mentioned some treasure he is looking for inside the", "waterfall.", "Perhaps I could find some information about this treasure", "somewhere nearby.")
        3 -> listOf("I read that there is a gnome named Golrie that may be able", "to help me unlock the secrets behind the Baxtorian treasure.", "Last he was seen was near Yanille by the Tree Gnome Village.")
        4 -> listOf("I freed Golrie from his prison underneath the Tree Gnome", "Village and found a small pebble that used to belong to", "the elf queen, Glarial.", "Maybe it could grant access to the waterfall somehow.")
        5 -> listOf("I have located the treasure inside the waterfall.", "Now to find a safe way to take the treasure without", "triggering the water trap and being washed outside.")
        6 -> listOf("", "", "QUEST COMPLETE!")
        else -> listOf("Invalid quest stage. Report this to an administrator.")
    }
    override fun complete(player: Player) {
        player.skills.addXpQuest(Constants.ATTACK, 13750.0)
        player.skills.addXpQuest(Constants.STRENGTH, 13750.0)
        player.inventory.addItem(Obj.mithril_seed, 40, true)
        player.inventory.addItem(Obj.diamond, 2, true)
        player.inventory.addItem(Obj.gold_bar, 2, true)
        sendQuestCompleteInterface(player, Obj.diamond)
    }
}

@ServerStartupEvent
fun mapWaterfallQuest() {
    onNpcClick("Hudon", checkDistance = false, options = arrayOf("Talk-to")) { (player, npc) ->
        npc.resetWalkSteps()
        player.resetWalkSteps()
        player.faceEntityTile(npc)
        npc.faceEntityTile(player)
        player.startConversation(HudonD(player, npc.id))
    }
    onNpcClick(Npc.almera_waterfall_quest) { (player) -> player.startConversation(AlmeraD(player)) }
    onNpcClick(Npc.golrie_waterfall_quest, Npc.roving_golrie) { (player) -> player.startConversation(GolrieD(player)) }
    onObjectClick(Loc.di2_boxes3, tiles = arrayOf(Tile.of(2593, 9881, 0))) { (player) ->
        player.sendMessage("You search the crate.")
        if (player.inventory.containsItem(Obj.baxtorian_key_waterfall_quest, 1)) player.sendMessage("You find nothing of interest.")
        else { player.inventory.addItem(Obj.baxtorian_key_waterfall_quest, 1); player.sendMessage("You find an old key.") }
    }
    onObjectClick(Loc.ladder_from_cellar_directional) { (player) -> player.useLadder(player.transform(0, -6400)) }
    onObjectClick(Loc.lograft_waterfall_quest, Loc.golrie_crate_waterfall_quest, Loc.roving_golrie_ladder_from_cellar, Loc.roving_golrie_ladder_to_cellar,
        Loc.waterfall_swim_point, Loc.overhanging_tree1_waterfall_quest, Loc.di2_chestopen_dark, Loc.di2_glarials_tomb_waterfall_quest,
        Loc.barrel_waterfall_quest, Loc.baxtorian_chalice_waterfall_quest, Loc.gl_waterfall_ledge_door, Loc.baxtorian_door_2_waterfall_quest,
        Loc.golrie_gate_waterfall_quest, Loc.bookcase_waterfall_quest) { (player, obj) ->
        when (obj.id) {
            Loc.lograft_waterfall_quest -> { player.sendMessage("You board the log raft and crash on a small spit of land."); player.tele(Tile.of(2512, 3481, 0)) }
            Loc.golrie_crate_waterfall_quest -> {
                if (player.getQuestStage(Quest.WATERFALL_QUEST) >= 3 && !player.inventory.containsItem(Obj.golrie_key_waterfall_quest, 1)) {
                    player.sendMessage("You find a large old key."); player.inventory.addItem(Obj.golrie_key_waterfall_quest, 1)
                } else player.sendMessage("You find nothing interesting.")
            }
            Loc.roving_golrie_ladder_from_cellar -> player.useStairs(Seq.human_reachforladder, player.transform(0, -6400), 1, 2)
            Loc.roving_golrie_ladder_to_cellar -> player.useStairs(Seq.human_reachforladder, player.transform(0, 6400), 1, 2)
            Loc.waterfall_swim_point -> { player.sendMessage("You try to swim down the river but you get swept away and over the waterfall."); player.waterfallWashOut() }
            Loc.overhanging_tree1_waterfall_quest -> { player.sendMessage("You slip from the tree and fall down the waterfall."); player.waterfallWashOut() }
            Loc.di2_chestopen_dark -> {
                if (player.inventory.containsItem(Obj.glarials_amulet_waterfall_quest, 1)) player.sendMessage("The chest is empty.")
                else { player.sendMessage("You find Glarial's amulet."); player.inventory.addItem(Obj.glarials_amulet_waterfall_quest, 1) }
            }
            Loc.di2_glarials_tomb_waterfall_quest -> {
                if (player.inventory.containsItem(Obj.glarials_urn_full_waterfall_quest, 1)) player.sendMessage("The tomb is empty.")
                else { player.sendMessage("You find Glarial's urn."); player.inventory.addItem(Obj.glarials_urn_full_waterfall_quest, 1) }
            }
            Loc.barrel_waterfall_quest -> { player.sendMessage("You climb in the barrel and safely land at the bottom of the waterfall."); player.tele(Tile.of(2531, 3413, 0)) }
            Loc.baxtorian_chalice_waterfall_quest -> { player.sendMessage("A powerful rush of water floods out of the cave and sweeps you down river."); player.waterfallWashOut() }
            Loc.gl_waterfall_ledge_door -> {
                if (player.equipment.amuletId == Obj.glarials_amulet_waterfall_quest || player.inventory.containsItem(Obj.glarials_amulet_waterfall_quest, 1)) player.tele(Tile.of(2575, 9862, 0))
                else { player.sendMessage("A powerful rush of water floods out of the cave and sweeps you down river."); player.waterfallWashOut() }
            }
            Loc.baxtorian_door_2_waterfall_quest, Loc.golrie_gate_waterfall_quest -> player.sendMessage("The door is locked.")
            Loc.bookcase_waterfall_quest -> {
                if (player.getQuestStage(Quest.WATERFALL_QUEST) >= 2 && !player.inventory.containsItem(Obj.obj_292, 1)) {
                    player.inventory.addItem(Obj.obj_292, 1); player.sendMessage("You find an old book on the subject of Baxtorian!")
                } else player.sendMessage("You find nothing of interest.")
            }
        }
    }
    onItemOnObject(arrayOf(Loc.golrie_gate_waterfall_quest, Loc.glarials_tombstone_waterfall_quest, Loc.baxtorian_door_2_waterfall_quest,
        Loc.stonepillar_small_waterfall_quest, Loc.loc_2006, Loc.baxtorian_chalice_waterfall_quest, Loc.overhanging_tree1_waterfall_quest)) { (player, obj, item) ->
        when {
            item.id == Obj.rope && obj.id == Loc.overhanging_tree1_waterfall_quest -> { player.sendMessage("You carefully climb down the tree using your rope."); player.tele(Tile.of(2511, 3463, 0)) }
            item.id == Obj.glarials_urn_full_waterfall_quest && obj.id == Loc.baxtorian_chalice_waterfall_quest && !player.isQuestComplete(Quest.WATERFALL_QUEST) -> {
                if (player.getQuestStage(Quest.WATERFALL_QUEST) == 5) {
                    player.questManager.completeQuest(Quest.WATERFALL_QUEST)
                    player.tele(player.transform(-38, 1))
                } else player.sendMessage("I don't know how you got in here, but you shouldn't be.")
            }
            item.id == Obj.glarials_amulet_waterfall_quest && obj.id == Loc.loc_2006 && !player.isQuestComplete(Quest.WATERFALL_QUEST) -> {
                val attributes = player.questManager.getAttribs(Quest.WATERFALL_QUEST)
                if (listOf("wfWaterRunes", "wfAirRunes", "wfEarthRunes").all { attributes.getI(it) >= 6 }) {
                    player.tele(player.transform(38, -1))
                    player.sendMessage("You place the necklace on the statue.")
                    player.sendMessage("You hear a loud rumble beneath your feet.")
                    player.sendMessage("The ground raises right in front of you!")
                    player.inventory.deleteItem(Obj.glarials_amulet_waterfall_quest, 1)
                    player.setQuestStage(Quest.WATERFALL_QUEST, 5)
                } else { player.sendMessage("Water fills the cave and flushes you out and down the river."); player.waterfallWashOut() }
            }
            item.id in setOf(Obj.waterrune, Obj.airrune, Obj.earthrune) && obj.id == Loc.stonepillar_small_waterfall_quest -> {
                player.sendMessage("The rune stone disappears in a puff of smoke.")
                player.inventory.deleteItem(item.id, 1)
                player.questManager.getAttribs(Quest.WATERFALL_QUEST).incI(when (item.id) { Obj.waterrune -> "wfWaterRunes"; Obj.airrune -> "wfAirRunes"; else -> "wfEarthRunes" })
            }
            item.id == Obj.glarials_pebble_waterfall_quest && obj.id == Loc.glarials_tombstone_waterfall_quest -> {
                if (player.getQuestStage(Quest.WATERFALL_QUEST) < 4) player.sendMessage("Nothing interesting happens.")
                else if (player.validTombEquipment()) {
                    player.sendMessage("You place the pebble in the gravestone's small indent.")
                    player.sendMessage("It fits perfectly.")
                    player.sendMessage("You hear a loud creak.")
                    player.sendMessage("The gravestone slides back to reveal a ladder going down.")
                    player.tele(Tile.of(2556, 9844, 0))
                    player.controllerManager.startController(GlarialsTombController())
                }
            }
            item.id == Obj.golrie_key_waterfall_quest && obj.id == Loc.golrie_gate_waterfall_quest -> {
                if (player.x == 2515 && player.y in 9575..9576) {
                    player.sendMessage("You unlock the door and go inside.")
                    player.tele(player.transform(0, if (player.y == 9575) 1 else -1))
                }
            }
            item.id == Obj.baxtorian_key_waterfall_quest && obj.id == Loc.baxtorian_door_2_waterfall_quest -> {
                Doors.handleDoor(player, obj); player.sendMessage("You unlock the door and go inside.")
            }
        }
    }
    onItemOnObject(arrayOf(Loc.crossing_rock_waterfall_quest), arrayOf(Obj.rope), checkDistance = false) { (player) ->
        if (player.x == 2512 && player.y == 3476) { player.sendMessage("You throw the rope over the rock and carefully pull yourself safely to land."); player.tele(Tile.of(2511, 3467, 0)) }
        else player.sendMessage("You are too far away to do this.")
    }
}

private fun Player.waterfallWashOut() {
    tele(Tile.of(2531, 3413, 0))
    applyHit(Hit(this, Utils.random(20, 56), HitLook.TRUE_DAMAGE))
}
