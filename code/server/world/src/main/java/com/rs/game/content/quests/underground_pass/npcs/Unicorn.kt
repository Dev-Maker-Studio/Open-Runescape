package com.rs.game.content.quests.underground_pass.npcs

import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Varbit
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.underground_pass.*
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.async.schedule
import world.gregs.voidps.type.Tile
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.instantiateNpc
import com.rs.plugin.kts.onItemOnNpc
import com.rs.plugin.kts.onNpcClick
import com.rs.plugin.kts.onObjectClick

private val BOULDER_CRASH_TILE = Tile.of(2397, 9603, 0)
private val UNICORN_CAGE_TILE = Tile.of(2397, 9605, 0)
private const val SMASHED_ROOM_X_OFFSET = -26

@ServerStartupEvent
fun mapUndergroundPassUnicorn() {
    instantiateNpc(Npc.boulder_upass, Npc.unicorn_upass) { id, tile -> UndergroundPassUnicornNpc(id, tile) }

    onNpcClick(Npc.boulder_upass) { (player) ->
        if (player.getQuestStage(Quest.UNDERGROUND_PASS) < STAGE_DESTROYED_ORBS) {
            player.sendMessage("You have not yet found a safe route to the boulder.")
            return@onNpcClick
        }
        player.sendMessage("It's too heavy to move with just your hands.")
    }

    onItemOnNpc(Npc.boulder_upass) { (player, item, npc) ->
        if (item.id != Obj.caverailing)
            return@onItemOnNpc
        if (player.getQuestStage(Quest.UNDERGROUND_PASS) < STAGE_DESTROYED_ORBS) {
            player.sendMessage("You have not yet found a safe route to the boulder.")
            return@onItemOnNpc
        }
        val attributes = player.questManager.getAttribs(Quest.UNDERGROUND_PASS)
        if (player.hasCompletedUndergroundPassUnicorn()) {
            player.sendMessage("The boulder has already crashed through the cage.")
            return@onItemOnNpc
        }

        val boulderStart = Tile.of(npc.tile)
        player.resetWalkSteps()
        player.lock()
        player.sendMessage("You use the railing as leverage to dislodge the boulder...", true)
        npc.anim(Seq.boulder_rolling)
        npc.forceMove(BOULDER_CRASH_TILE, Seq.boulder_rolling, 10, 70)
        player.schedule {
            wait(2)
            player.sendMessage("You hear a loud crash as the boulder reaches the bottom.", true)
            wait(1)
            attributes.setB("unicornDefeated", true)
            player.fadeScreen {
                player.tele(player.transform(SMASHED_ROOM_X_OFFSET, 0))
                player.unlockNextTick()
            }
            wait(4)
            npc.tele(boulderStart)
        }
    }

    onObjectClick(Loc.cave_railings3, tiles = arrayOf(UNICORN_CAGE_TILE)) { (player) ->
        if (player.getQuestStage(Quest.UNDERGROUND_PASS) < STAGE_DESTROYED_ORBS) {
            player.sendMessage("You find nothing useful.")
            return@onObjectClick
        }
        if (player.inventory.containsItem(Obj.caverailing) || player.bank.containsItem(Obj.caverailing)) {
            player.sendMessage("You search the cage but find nothing.")
            return@onObjectClick
        }
        player.anim(Seq.human_pickuptable)
        player.giveUniqueQuestItem(Obj.caverailing)
        player.sendMessage("You find a broken piece of railing lying on the floor. You take it.")
    }

    onObjectClick(Loc.unicorncage_destroyed_upass) { (player) ->
        if (!player.hasCompletedUndergroundPassUnicorn()) {
            player.sendMessage("The unicorn is still alive. You cannot safely reach the horn.")
            return@onObjectClick
        }
        if (player.inventory.containsItem(Obj.cave_unicorn_horn) || player.bank.containsItem(Obj.cave_unicorn_horn)) {
            player.sendMessage("You search the smashed cage but find nothing.")
            return@onObjectClick
        }
        player.anim(Seq.human_pickuptable)
        player.giveUniqueQuestItem(Obj.cave_unicorn_horn)
        player.sendMessage("You search the smashed cage and take the unicorn's horn.")
        player.vars.saveVarBit(Varbit.upass_unicorn_horn, 1)
        player.advanceUndergroundPass(STAGE_PASSED_UNICORN)
    }
}

private class UndergroundPassUnicornNpc(id: Int, tile: Tile) : NPC(id, tile) {
    override fun withinDistance(player: Player, distance: Int): Boolean {
        return !player.hasCompletedUndergroundPassUnicorn() && super.withinDistance(player, distance)
    }
}
