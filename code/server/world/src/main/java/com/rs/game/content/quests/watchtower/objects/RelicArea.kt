package com.rs.game.content.quests.watchtower.objects

import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Seq
import com.rs.game.content.skills.agility.Agility
import com.rs.engine.quest.Quest
import com.rs.game.tasks.WorldTasks
import com.rs.game.content.quests.watchtower.STAGE_FIND_WAY_INTO_GUTANOTH
import world.gregs.voidps.type.Tile
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnObject
import com.rs.plugin.kts.onItemOnItem
import com.rs.plugin.kts.onObjectClick

private val MAINLAND_TREE = Tile.of(2501, 3085, 0)
private val GREW_ISLAND_ROPE = Tile.of(2511, 3090, 0)
private val WEST_ROPE = Tile.of(2499, 3087, 0)
private val WEST_ISLAND_LANDING = Tile.of(2505, 3087, 0)
private val NORTH_MAINLAND_LANDING = Tile.of(2511, 3096, 0)
private val TOBAN_CHEST_TILE = Tile.of(2575, 3031, 0)

private const val OG_TASK_STARTED = "watchtowerOgTaskStarted"

@ServerStartupEvent
fun mapWatchtowerRelicArea() {
    mapGrewCrossing()
    mapTobanChest()
    onItemOnItem(intArrayOf(Obj.relicpart1, Obj.relicpart2, Obj.relicpart3),
        intArrayOf(Obj.relicpart1, Obj.relicpart2, Obj.relicpart3)) { event ->
        val player = event.player
        player.sendMessage("I think these fit together, but I can't seem to make them fit.")
        player.sendMessage("I am going to need someone with experience to help me with this.")
    }
}

private fun mapGrewCrossing() {
    onItemOnObject(
        objectNamesOrIds = arrayOf(Loc.tree_forropeswing1, Loc.tree_ropeswing4_norope),
        itemNamesOrIds = arrayOf(Obj.rope),
        tiles = arrayOf(MAINLAND_TREE, WEST_ROPE),
        checkDistance = true
    ) { (player) ->
        if (player.getQuestStage(Quest.WATCHTOWER) < STAGE_FIND_WAY_INTO_GUTANOTH) {
            player.sendMessage("You have no reason to go over there.")
            return@onItemOnObject
        }

        player.sendMessage("You tie the rope to the tree...")
        player.inventory.deleteItem(Obj.rope, 1)
        player.resetWalkSteps()
        player.faceTile(WEST_ISLAND_LANDING)
        Agility.handleObstacle(player, Seq.human_ropeswing_long, 3, WEST_ISLAND_LANDING, 0.0)
        player.sendMessage("You skilfully swing across.")
    }

    onObjectClick(
        Loc.tree_ropeswing3,
        tiles = arrayOf(GREW_ISLAND_ROPE),
        checkDistance = true
    ) { (player) ->
        player.resetWalkSteps()
        player.faceTile(NORTH_MAINLAND_LANDING)
        Agility.handleObstacle(player, Seq.human_ropeswing_long, 3, NORTH_MAINLAND_LANDING, 0.0)
    }

    onObjectClick(Loc.tree_ropeswing4, tiles = arrayOf(WEST_ROPE)) { (player) ->
        player.resetWalkSteps()
        player.faceTile(WEST_ISLAND_LANDING)
        Agility.handleObstacle(player, Seq.human_ropeswing_long, 3, WEST_ISLAND_LANDING, 0.0)
    }
}

private fun mapTobanChest() {
    onObjectClick(
        Loc.tobanchest,
        Loc.tobanchestopen,
        tiles = arrayOf(TOBAN_CHEST_TILE),
        checkDistance = true
    ) { (player, obj, option) ->
        if (option == "Shut") {
            obj.changeId(Loc.tobanchest)
            return@onObjectClick
        }

        if (!player.getBool(OG_TASK_STARTED)) {
            player.sendMessage("The chest is locked.")
            return@onObjectClick
        }

        if (player.inventory.containsItem(Obj.stolen_gold, 1) ||
            player.bank.containsItem(Obj.stolen_gold, 1) || player.getBool("watchtowerOgTaskComplete")
        ) {
            player.sendMessage("The chest is empty.")
            return@onObjectClick
        }

        if (!player.inventory.containsItem(Obj.toban_key, 1)) {
            player.sendMessage("The chest is locked.")
            return@onObjectClick
        }

        if (!player.inventory.hasFreeSlots()) {
            player.sendMessage("You don't have enough inventory space.")
            return@onObjectClick
        }

        player.faceObject(obj)
        player.anim(Seq.human_openchest)
        player.lock(2)
        obj.setIdTemporary(Loc.tobanchestopen, 10)
        player.inventory.deleteItem(Obj.toban_key, 1)
        player.sendMessage("You use the key Og gave you.")
        WorldTasks.schedule(1) {
            player.inventory.addItemDrop(Obj.stolen_gold, 1)
            player.sendMessage("You find a stash of gold inside.")
        }
    }
}
