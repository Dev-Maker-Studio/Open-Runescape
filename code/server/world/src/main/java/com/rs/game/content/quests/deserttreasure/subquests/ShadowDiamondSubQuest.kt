package com.rs.game.content.quests.deserttreasure

import com.rs.engine.dialogue.HeadE.*
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.model.entity.npc.OwnedNPC
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcDeath
import com.rs.plugin.kts.onLogin
import com.rs.plugin.kts.onObjectClick
import com.rs.plugin.kts.onPlayerStep
import org.darkan.core.Constants
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import world.gregs.voidps.type.Tile

private val SHADOW_ENTRANCE_TILE = Tile.of(2547, 3421, 0)
private val SHADOW_ENTRANCE_ARRIVAL_TILE = Tile.of(2547, 3422, 0)
private val SHADOW_INTERIOR_LADDER_TILE = Tile.of(2629, 5072, 0)
private val SHADOW_INTERIOR_ARRIVAL_TILE = Tile.of(2630, 5072, 0)
private val SECURE_CHEST_TILE = Tile.of(3169, 2967, 0)
private val DAMIS_SPAWN_TILE = Tile.of(2738, 5081, 0)

private val DAMIS_WARNING_ONE_TILES = (2650..2665).flatMap { x -> (5068..5085).map { y -> Tile.of(x, y, 0) } }.toTypedArray()
private val DAMIS_WARNING_TWO_TILES = (2670..2695).flatMap { x -> (5095..5115).map { y -> Tile.of(x, y, 0) } }.toTypedArray()
private val DAMIS_WARNING_THREE_TILES = (2700..2728).flatMap { x -> (5075..5108).map { y -> Tile.of(x, y, 0) } }.toTypedArray()

private val DAMIS_ROOM_TRIGGERS = (2733..2743).flatMap { x ->
    (5076..5086).map { y -> Tile.of(x, y, 0) }
}.toTypedArray()

const val RASOLO_DEAL_ACCEPTED = "dtRasoloDealAccepted"

private const val GILDED_CROSS_CLAIMED = "dtGildedCrossClaimed"
private const val DAMIS_ACTIVE = "dtDamisActive"
private const val DAMIS_FIRST_DEFEATED = "dtDamisFirstDefeated"
private const val DAMIS_DEFEATED = "dtDamisDefeated"
private const val SHADOW_DIAMOND_CLAIMED = "dtShadowDiamondClaimed"
private const val DAMIS_WARNING_ONE = "dtDamisWarningOne"
private const val DAMIS_WARNING_TWO = "dtDamisWarningTwo"
private const val DAMIS_WARNING_THREE = "dtDamisWarningThree"

private val DAMIS_SECOND_FORM_TAUNTS = arrayOf(
    "Your pathetic skill won't help you now!",
    "You are no match for my power!",
    "You will regret ever coming here!",
    "I will crush you utterly!",
    "I... am... invincible!",
    "Taste my blade and feel my wrath!"
)

@ServerStartupEvent
fun mapShadowDiamondDesertTreasure() {
    onLogin { (player) ->
        if (player.getBool(DAMIS_ACTIVE) && !player.getBool(DAMIS_DEFEATED)) {
            player.set(DAMIS_ACTIVE, false)
        }
    }

    onObjectClick(Loc.fd_bandit_shutchest, tiles = arrayOf(SECURE_CHEST_TILE)) { (player) ->
        openSecureChest(player)
    }

    onObjectClick(Loc.loc_6560, tiles = arrayOf(SHADOW_ENTRANCE_TILE)) { (player) ->
        if (!player.getBool(RASOLO_DEAL_ACCEPTED)) {
            player.sendMessage("You cannot find a way into the hidden dungeon.")
            return@onObjectClick
        }
        player.set(DAMIS_WARNING_ONE, false)
        player.set(DAMIS_WARNING_TWO, false)
        player.set(DAMIS_WARNING_THREE, false)
        player.packets.sendResetCamera()
        player.setForceNextMapLoadRefresh(true)
        player.useLadder(SHADOW_INTERIOR_ARRIVAL_TILE)
    }

    onObjectClick(Loc.di2_ladder_base, tiles = arrayOf(SHADOW_INTERIOR_LADDER_TILE)) { (player) ->
        player.useLadder(SHADOW_ENTRANCE_ARRIVAL_TILE)
    }

    onPlayerStep(DAMIS_WARNING_ONE_TILES) { (player) -> showDamisWarning(player, DAMIS_WARNING_ONE, "A voice seems to come from the walls around you;") }
    onPlayerStep(DAMIS_WARNING_TWO_TILES) { (player) -> showDamisWarning(player, DAMIS_WARNING_TWO, "'You... do not be... long in this... place...") }
    onPlayerStep(DAMIS_WARNING_THREE_TILES) { (player) -> showDamisWarning(player, DAMIS_WARNING_THREE, "Turn... back now, or... prepare... to meet your... doom...'") }

    onPlayerStep(DAMIS_ROOM_TRIGGERS) { (player) ->
        enterDamisRoom(player)
    }

    onNpcDeath(Npc.fd_damis_normal) { e ->
        val damis = e.npc as? OwnedNPC ?: return@onNpcDeath
        val player = damis.owner
        if (!player.getBool(DAMIS_ACTIVE) || player.getBool(DAMIS_DEFEATED))
            return@onNpcDeath

        player.set(DAMIS_FIRST_DEFEATED, true)
        damis.forceTalk("Now I show... you... my true... power!")

        val secondForm = Damis(player, Npc.fd_damis_tougher, damis.tile)
        secondForm.setAutoDespawnAtDistance(false)
        secondForm.combatTarget = player
        scheduleDamisTaunt(player, secondForm)
    }

    onNpcDeath(Npc.fd_damis_tougher) { e ->
        val damis = e.npc as? OwnedNPC ?: return@onNpcDeath
        val player = damis.owner
        if (!player.getBool(DAMIS_ACTIVE))
            return@onNpcDeath

        player.set(DAMIS_ACTIVE, false)
        player.set(DAMIS_DEFEATED, true)
        player.set(SHADOW_DIAMOND_CLAIMED, true)
        DesertTreasureDiamonds.giveShadowDiamond(player)
    }
}

private fun openSecureChest(player: Player) {
    if (player.getQuestStage(Quest.DESERT_TREASURE) != STAGE_DIAMONDS_OF_AZZANADRA ||
        !player.getBool(RASOLO_DEAL_ACCEPTED)
    ) {
        player.simpleDialogue("These bandits are hostile enough without me trying to rob them!")
        return
    }

    if (player.getBool(GILDED_CROSS_CLAIMED)) {
        player.simpleDialogue("You find nothing of interest inside the chest.")
        return
    }

    if (player.skills.getLevelForXp(Constants.THIEVING) < 53) {
        player.sendMessage("You need a Thieving level of 53 to pick this lock.")
        return
    }

    if (!player.inventory.containsOneItem(Obj.lockpick)) {
        player.sendMessage("You need a lockpick to attempt to open the chest.")
        return
    }

    player.startConversation {
        simple("Your skill as a thief allows you to see some kind of elaborate booby trapped locking mechanism on this chest.")
        options("Try to open chest?") {
            op("Yes") {
                exec { attemptSecureChest(player) }
            }
            op("No") { }
        }
    }
}

private fun attemptSecureChest(player: Player) {
    if (!player.inventory.containsOneItem(Obj.lockpick)) {
        player.sendMessage("You need a lockpick to attempt to open the chest.")
        return
    }

    player.faceTile(SECURE_CHEST_TILE)
    player.anim(Seq.human_pickuptable)
    player.sendMessage("You attempt to pick the first lock...")

    if (!rollLockpick(player)) {
        failSecureChest(player)
        return
    }
    player.sendMessage("...and successfully pick the first lock.")
    player.sendMessage("You attempt to pick the second lock...")

    if (!rollLockpick(player)) {
        failSecureChest(player)
        return
    }
    player.sendMessage("...and successfully pick the second lock.")
    player.sendMessage("You attempt to pick the final lock...")

    if (!rollLockpick(player)) {
        failSecureChest(player)
        return
    }

    player.sendMessage("...and manage to pick the final lock.")
    player.skills.addXp(Constants.THIEVING, 150.0)
    player.inventory.addItemDrop(Obj.fd_sword_cross, 1)
    player.set(GILDED_CROSS_CLAIMED, true)
    player.simpleDialogue("Inside the chest, hidden under some rags, you find a Gilded Cross.")
}

private fun failSecureChest(player: Player) {
    player.sendMessage("...and fail. The locking mechanism has reset itself.")
    player.inventory.deleteItem(Obj.lockpick, 1)
    player.sendMessage("Your lock pick snapped while attempting to pick the lock.")
}

private fun rollLockpick(player: Player): Boolean {
    val level = player.skills.getLevel(Constants.THIEVING)
    return Utils.skillSuccess(level, player.auraManager.thievingMul, 47, 128)
}

private fun showDamisWarning(player: Player, key: String, message: String) {
    if (player.getQuestStage(Quest.DESERT_TREASURE) != STAGE_DIAMONDS_OF_AZZANADRA ||
        !player.getBool(RASOLO_DEAL_ACCEPTED) || player.getBool(DAMIS_DEFEATED) || player.getBool(key)
    ) return
    player.set(key, true)
    player.sendMessage(message)
}

private fun enterDamisRoom(player: Player) {
    if (player.getQuestStage(Quest.DESERT_TREASURE) != STAGE_DIAMONDS_OF_AZZANADRA ||
        !player.getBool(RASOLO_DEAL_ACCEPTED)
    ) return

    if (player.getBool(DAMIS_DEFEATED)) {
        if (!DesertTreasureDiamonds.hasShadowDiamond(player)) {
            player.simpleDialogue("The Diamond of Shadow seems to have mystically found its way back here...")
            DesertTreasureDiamonds.giveShadowDiamond(player)
        }
        return
    }

    if (player.getBool(DAMIS_ACTIVE))
        return

    player.set(DAMIS_ACTIVE, true)

    val damisId = if (player.getBool(DAMIS_FIRST_DEFEATED)) Npc.fd_damis_tougher else Npc.fd_damis_normal
    val damis = Damis(player, damisId, DAMIS_SPAWN_TILE)
    damis.setAutoDespawnAtDistance(false)
    damis.forceTalk("You should... have listened to me!")
    damis.combatTarget = player
    if (damisId == Npc.fd_damis_tougher)
        scheduleDamisTaunt(player, damis)
}

private fun scheduleDamisTaunt(player: Player, damis: OwnedNPC) {
    player.tasks.schedule(20) {
        if (damis.hasFinished() || damis.isDead || damis.owner !== player || player.getBool(DAMIS_DEFEATED))
            return@schedule

        damis.forceTalk(DAMIS_SECOND_FORM_TAUNTS[Utils.random(DAMIS_SECOND_FORM_TAUNTS.size - 1)])
        scheduleDamisTaunt(player, damis)
    }
}
