package com.rs.game.content.quests.deserttreasure.objects

import com.rs.engine.quest.Quest
import com.rs.game.World
import com.rs.game.model.gameobject.GameObject
import org.darkan.core.map.pathfinder.Direction
import org.darkan.core.gameval.gen.Loc
import com.rs.game.content.quests.deserttreasure.STAGE_DIAMONDS_OF_AZZANADRA
import com.rs.game.content.quests.deserttreasure.ownsActiveNpc
import com.rs.game.content.quests.deserttreasure.Dessous
import org.darkan.core.gameval.gen.Seq
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnObject
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import world.gregs.voidps.type.Tile

private val DESSOUS_TOMB = Tile.of(3568, 3401, 0)
private val DESSOUS_EMERGENCE_TILE = Tile.of(3570, 3402, 0)
private const val TOMB_OPEN_TICKS = 20

@ServerStartupEvent
fun mapDessousTomb() {
    onItemOnObject(
        objectNamesOrIds = arrayOf("Vampyre tomb"),
        itemNamesOrIds = arrayOf(
            Obj.fd_silver_pot_blood,
            Obj.fd_silver_pot_blood_blessed,
            Obj.fd_silver_pot_blood_garlic,
            Obj.fd_silver_pot_blood_garlic_blessed,
            Obj.fd_silver_pot_blood_garlic_spiced,
            Obj.fd_silver_pot_blood_spiced,
            Obj.fd_silver_pot_blood_spiced_blessed,
            Obj.fd_silver_pot_blood_garlic_spiced_blessed
        )
    ) { (player, obj, item) ->
        if (obj.x != DESSOUS_TOMB.x || obj.y != DESSOUS_TOMB.y || obj.plane != DESSOUS_TOMB.plane)
            return@onItemOnObject
        if (player.isLocked) return@onItemOnObject
        useBloodPotOnTomb(player, obj, item.id)
    }
}

private fun useBloodPotOnTomb(player: Player, tomb: GameObject, itemId: Int) {
    if (player.getQuestStage(Quest.DESERT_TREASURE) < STAGE_DIAMONDS_OF_AZZANADRA) {
        player.sendMessage("Nothing interesting happens.")
        return
    }
    if (player.ownsActiveNpc(Npc.blooddiamond_vampirewarrior)) {
        player.sendMessage("Dessous has already been disturbed.")
        return
    }
    if (!player.inventory.containsItem(itemId, 1)) {
        player.sendMessage("You need a prepared pot of blood to lure Dessous.")
        return
    }

    val landingTile = player.tile.transform(0, 1, 0)
    player.stopAll()
    player.faceTile(DESSOUS_EMERGENCE_TILE)
    player.lock(5)
    World.spawnObjectTemporary(GameObject(Loc.vampire_big_grave_withblood, tomb.type, tomb.rotation, tomb.tile), TOMB_OPEN_TICKS)
    player.inventory.deleteItem(itemId, 1)
    val blessed = itemId == Obj.fd_silver_pot_blood_garlic_spiced_blessed
    val blessedPot = itemId in intArrayOf(Obj.fd_silver_pot_blood_blessed, Obj.fd_silver_pot_blood_garlic_blessed, Obj.fd_silver_pot_blood_spiced_blessed, Obj.fd_silver_pot_blood_garlic_spiced_blessed)
    player.inventory.addItem(if (blessedPot) Obj.fd_silver_pot_blessed else Obj.fd_silver_pot, 1)
    player.sendMessage("You pour the blood from the pot onto the tomb. A terrible presence stirs beneath the ground...")
    player.tasks.schedule(2) {
        if (player.hasFinished() || player.isDead) return@schedule
        Dessous(player, DESSOUS_EMERGENCE_TILE, blessed).apply {
            setAutoDespawnAtDistance(false)
            forceTalk("Hssssssssssss")
            resetWalkSteps()
            forceMove(DESSOUS_EMERGENCE_TILE, landingTile, Direction.NORTH, Seq.human_longjump, 0, 90, true) {
                anim(-1)
                combatTarget = player
            }
        }
    }
}
