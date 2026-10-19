package com.rs.game.content.quests.rovingelves

import com.rs.game.World
import com.rs.game.content.containsTool
import com.rs.game.model.entity.async.schedule
import com.rs.game.model.entity.player.Player
import com.rs.game.model.gameobject.GameObject
import com.rs.game.tasks.WorldTasks
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onChunkEnter
import com.rs.plugin.kts.onItemClick
import com.rs.plugin.kts.onLogin
import com.rs.plugin.kts.onNpcDeath
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.model.Item
import org.darkan.core.model.ObjectShape
import world.gregs.voidps.type.Tile

@ServerStartupEvent
fun mapRovingElvesObjects() {
    onNpcDeath(Npc.roving_mossgiant) { (killer, npc) ->
        val player = killer as? Player ?: return@onNpcDeath
        if (player.rovingStage == FIND_SEED && player.inGlarialsTomb() && !player.containsItem(Obj.roving_old_consecration_seed))
            World.addGroundItem(Item(Obj.roving_old_consecration_seed, 1), npc.tile, player)
    }
    onItemClick(Obj.roving_new_consecration_seed, options = arrayOf("Plant")) { (player) -> player.plantRovingSeed() }
    onObjectClick(Loc.roving_fire_remains) { (player) ->
        player.simpleDialogue("The firepit is still warm, there must be travellers about. Maybe I should look for them.")
    }
    onLogin { (player) -> player.schedule { wait(2); player.refreshRovingArea() } }
    onChunkEnter { event -> event.player?.let { player -> player.schedule { wait(2); player.refreshRovingArea() } } }
}

internal fun Player.inGlarialsTomb() = plane == 0 && x in 2496..2559 && y in 9792..9855
private fun Player.inGlarialsNewTomb() = plane in 0..1 && x in 2561..2570 && y in 9902..9918

private fun Player.refreshRovingArea() {
    if (hasFinished() || isDead) return
    if (inGlarialsTomb() && controllerManager.controller == null) controllerManager.startController(GlarialsTombController())
    if (inGlarialsNewTomb() && rovingStage == ENCHANTED) vars.setVarBit(Varbit.roving_entered_waterfall_to_plant_seed, 1)
}

private fun Player.plantRovingSeed() {
    if (rovingStage != ENCHANTED || !inventory.containsItem(Obj.roving_new_consecration_seed, 1)) return
    if (!inGlarialsNewTomb()) { sendMessage("This seed may only be planted close to Glarial's remains."); return }
    if (!inventory.containsItem(Obj.spade, 1) && !containsTool(Obj.spade)) { sendMessage("You need a spade to plant the seed."); return }
    if (isLocked) return
    val plantingTile = tile
    stopAll()
    lock(8)
    val growth = GameObject(Loc.roving_crystal_growth, ObjectShape.SCENERY_INTERACT, 0, plantingTile)
    schedule {
        try {
            anim(Seq.seq_830)
            sendMessage("You dig a small hole with your spade.")
            wait(2)
            if (rovingStage != ENCHANTED || tile != plantingTile || !inventory.containsItem(Obj.roving_new_consecration_seed, 1)) return@schedule
            inventory.deleteItem(Obj.roving_new_consecration_seed, 1)
            sendMessage("You drop the crystal seed in the hole.")
            setQuestStage(ROVING, PLANTED)
            World.spawnObjectTemporary(growth, 20, false)
            WorldTasks.schedule(1) {
                if (World.isSpawnedObject(growth)) World.sendObjectAnimation(growth, Seq.roving_crystal_growth_anim)
            }
            wait(6)
        } finally {
            unlock()
        }
    }
}
