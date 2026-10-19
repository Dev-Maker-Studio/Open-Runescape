package com.rs.game.content.world.areas.tirannwn

import com.rs.engine.dialogue.HeadE.CHEERFUL
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.World
import com.rs.game.content.quests.regicide.STAGE_RETURN_TO_LATHAS
import com.rs.game.content.quests.regicide.cutscenes.playArianwynRevelation
import com.rs.game.content.quests.regicide.objects.enterWesternUndergroundPass
import com.rs.game.content.quests.regicide.objects.regicideDenseForest
import com.rs.game.content.skills.agility.Agility
import com.rs.game.content.skills.magic.Magic
import com.rs.game.content.world.areas.global.AgilityShortcuts
import com.rs.game.model.entity.player.Player
import com.rs.game.model.gameobject.GameObject
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemClick
import com.rs.plugin.kts.onItemOnNpc
import com.rs.plugin.kts.onNpcClick
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import world.gregs.voidps.type.Tile

@ServerStartupEvent
fun mapTirannwn() {
    onObjectClick(Loc.glo3_cave_entranceopen, Loc.glo3_cave_exit) { (player, obj) ->
        player.useStairs(if (obj.id == Loc.glo3_cave_exit) Tile.of(2389, 3193, 0) else Tile.of(3577, 4400, 0))
    }
    onObjectClick(Loc.glo3_stairs_2to1, Loc.glo3_stairs_1to2) { (player, obj) ->
        player.useStairs(if (obj.id == Loc.glo3_stairs_2to1) Tile.of(3546, 4581, 0) else Tile.of(3619, 4582, 0))
    }
    onObjectClick(Loc.glo3_stairs_0to1, Loc.glo3_stairs_1to0) { (player, obj) ->
        player.useStairs(if (obj.id == Loc.glo3_stairs_0to1) Tile.of(3545, 4577, 0) else Tile.of(3540, 4512, 0))
    }
    onObjectClick(Loc.loc_20719, Loc.glo3_vent_1) { (player, obj, option) ->
        player.useStairs(if (obj.id == Loc.loc_20719 && option == "Climb-down") Tile.of(3541, 4571, 0) else Tile.of(2375, 3181, 0))
    }
    onObjectClick(Loc.regicide_logbalance1_start, Loc.regicide_logbalance2_start, Loc.regicide_logbalance3_start) { (player, obj) ->
        if (!Agility.hasLevel(player, 45)) {
            player.sendMessage("You need 45 agility")
            return@onObjectClick
        }
        if (obj.id == Loc.regicide_logbalance3_start)
            AgilityShortcuts.walkLog(player, player.transform(0, if (player.y > obj.y) -7 else 7, 0), 6)
        else
            AgilityShortcuts.walkLog(player, player.transform(if (player.x > obj.x) -6 else 6, 0, 0), 4)
    }
    onObjectClick(Loc.loc_4006) { (player) -> enterWesternUndergroundPass(player) }
    onObjectClick(Loc.elf_village_treegate) { (player, obj) ->
        if (!player.isQuestComplete(Quest.ROVING_ELVES, "to navigate the forest."))
            return@onObjectClick
        Agility.handleObstacle(player, Seq.trollheim_wall_kit_climb_over, 1, player.transform(if (player.x < obj.x) 2 else -2, 0, 0), 0.0)
    }
    onObjectClick("Dense forest", checkDistance = false) { (player, obj) ->
        if (player.plane != obj.plane || player.getLongestDelta(obj.tile) > 4) {
            player.sendMessage("You can't reach that.")
            return@onObjectClick
        }
        if (!Agility.hasLevel(player, 56))
            return@onObjectClick
        player.regicideDenseForest(obj)
    }
    onObjectClick(Loc.overpass_gate_right, Loc.overpass_gate_left) { (player, obj) ->
        if (player.getQuestStage(Quest.REGICIDE) < STAGE_RETURN_TO_LATHAS && !player.isQuestComplete(Quest.REGICIDE)) {
            player.sendMessage("You can see no way to get past this.")
            return@onObjectClick
        }
        if (player.isLocked || player.y == obj.y)
            return@onObjectClick
        val leftX = if (obj.id == Loc.overpass_gate_left) obj.x else obj.x - 2
        val left = World.getObject(Tile.of(leftX, obj.y, obj.plane), obj.type) ?: return@onObjectClick
        val right = World.getObject(Tile.of(leftX + 2, obj.y, obj.plane), obj.type) ?: return@onObjectClick
        if (left.id != Loc.overpass_gate_left || right.id != Loc.overpass_gate_right)
            return@onObjectClick
        player.resetWalkSteps()
        player.lock(4)
        World.spawnObjectTemporary(GameObject(left, 83), 8, true)
        World.spawnObjectTemporary(GameObject(right, 83), 8, true)
        World.spawnObjectTemporary(GameObject(Loc.overpass_gate_left_open, left.type, left.rotation, left.tile), 8, true)
        World.spawnObjectTemporary(GameObject(Loc.overpass_gate_right_open, right.type, right.rotation, right.tile.transform(1, 0, 0)), 8, true)
        player.addWalkSteps(Tile.of(player.x.coerceIn(leftX + 1, leftX + 2), obj.y + if (player.y > obj.y) -1 else 1, obj.plane), 5, false)
    }
    onObjectClick(Loc.loc_9296, Loc.loc_9297) { (player, obj) ->
        val shortcut = CLIFF_SHORTCUTS[obj.tile] ?: return@onObjectClick
        if (!Agility.hasLevel(player, shortcut.first)) {
            player.sendMessage("You need level ${shortcut.first} agility to use this shortcut.")
            return@onObjectClick
        }
        player.forceMove(shortcut.second, if (obj.id == Loc.loc_9296) Seq.tog_climb_slope_down else Seq.tog_climb_slope_up, 0, 30)
    }
    onNpcClick("Arianwyn", options = arrayOf("Talk-to")) { (player, npc) ->
        if (npc.id == Npc.npc_1202 && player.getQuestStage(Quest.REGICIDE) == STAGE_RETURN_TO_LATHAS) {
            player.playArianwynRevelation()
            return@onNpcClick
        }
        if (!player.isQuestComplete(Quest.ROVING_ELVES, "to talk to Arianwyn about teleport seeds and crystal weaponry."))
            return@onNpcClick
        player.startConversation {
            npc(npc, CHEERFUL, "Hello ${player.displayName}. What is it you need?")
            options {
                op("Could you repair some seeds for me?") {
                    npc(npc, CHEERFUL, "Of course! If you have a seed you would like me to repair, use it on me.")
                }
                op("Could I buy a crystal bow?") {
                    npc(npc, CHEERFUL, "Yes, but it will cost you 1,000,000 coins.")
                    options("Buy a crystal bow for 1,000,000 coins?") {
                        op("Yes, I'd like to buy one.") { exec { player.buyCrystalWeapon(Obj.crystal_bow_new, 1000000) } }
                        op("No thanks, that's way too much for me.") {}
                    }
                }
                op("Could I buy a crystal shield?") {
                    npc(npc, CHEERFUL, "Yes, but it will cost you 750,000 coins.")
                    options("Buy a crystal shield for 750,000 coins?") {
                        op("Yes, I'd like to buy one.") { exec { player.buyCrystalWeapon(Obj.crystal_shield_new, 750000) } }
                        op("No thanks, that's way too much for me.") {}
                    }
                }
            }
        }
    }
    onItemOnNpc("Arianwyn") { e ->
        val player = e.player
        if (e.item.id == Obj.crystal_seed_old) {
            val repairs = player.crystalSeedRepairs
            val cost = when {
                repairs in 0..5 -> 1000000 - repairs * 200000
                repairs > 5 -> 200000
                else -> 1000000
            }
            player.sendOptionDialogue("Would you like to attune your crystal seed for $cost gold?") { ops ->
                ops.add("Yes, I'll pay $cost gold for a bow.") { player.attuneCrystalWeapon(Obj.crystal_bow_new, cost) }
                ops.add("Yes, I'll pay $cost gold for a shield.") { player.attuneCrystalWeapon(Obj.crystal_shield_new, cost) }
                ops.add("No, that's too much.")
            }
        } else if (e.item.id == Obj.obj_6103) {
            val count = player.inventory.getNumberOf(Obj.obj_6103)
            val cost = (750 - player.tinyCrystalSeedRepairs.coerceIn(0, 5) * 120) * count
            player.sendOptionDialogue("Repair all your teleport seeds for $cost?") { ops ->
                ops.add("Yes, please.") {
                    if (player.inventory.hasCoins(cost) && player.inventory.containsItem(Obj.obj_6103, count)) {
                        player.inventory.deleteItem(Obj.obj_6103, count)
                        player.inventory.removeCoins(cost)
                        player.inventory.addItem(Obj.obj_6099, count)
                        repeat(count) { player.incrementTinyCrystalSeedRepair() }
                    } else {
                        player.sendMessage("You don't have enough money.")
                    }
                }
                ops.add("No, thank you.")
            }
        }
    }
    onItemClick(Obj.obj_6099, Obj.obj_6100, Obj.obj_6101, Obj.obj_6102, options = arrayOf("Activate")) { e ->
        Magic.sendNormalTeleportSpell(e.player, Tile.of(2340, 3172, 0)) {
            e.item.id = when (e.item.id) {
                Obj.obj_6099 -> Obj.obj_6100
                Obj.obj_6100 -> Obj.obj_6101
                Obj.obj_6101 -> Obj.obj_6102
                else -> Obj.obj_6103
            }
            e.player.inventory.refresh(e.item.slot)
        }
    }
    onObjectClick(Loc.pog_canyon_sewer_entrance_lower_01, Loc.pog_canyon_sewer_entrance_lower_02, Loc.pog_canyon_sewer_entrance_lower_01_mirror) { (player) -> player.useStairs(-1, Tile.of(1989, 4174, 0), 1, 2, "You enter the murky cave...") }
    onObjectClick(Loc.pog_sewer_pipe_exit_bottom_02, Loc.pog_sewer_pipe_exit_bottom_03, Loc.pog_sewer_pipe_exit_bottom_01_mirror, Loc.pog_sewer_pipe_exit_bottom_02_mirror) { (player) -> player.useStairs(-1, Tile.of(2321, 3100, 0)) }
    onObjectClick(Loc.pog_sewer_grate_door_closed, tiles = arrayOf(Tile.of(2015, 4255, 1))) { (player) -> player.simpleDialogue("The room beyond the door is covered in gas, it is probably dangerous to go in there.") }
    onObjectClick(Loc.pog_sewer_ladder_top, Loc.pog_sewer_pipe_side_ladder) { (player, obj) ->
        val direction = if (obj.id == Loc.pog_sewer_ladder_top) -1 else 1
        val destination = if (obj.x == 2041 && obj.y in intArrayOf(4189, 4172)) player.transform(direction, 0, direction) else player.transform(0, direction, direction)
        player.useStairs(Seq.seq_827, destination)
    }
}

private val CLIFF_SHORTCUTS = mapOf(
    Tile.of(2333, 3252, 0) to (85 to Tile.of(2338, 3253, 0)),
    Tile.of(2338, 3282, 0) to (68 to Tile.of(2338, 3286, 0)),
    Tile.of(2346, 3299, 0) to (59 to Tile.of(2344, 3294, 0)),
    Tile.of(2337, 3253, 0) to (85 to Tile.of(2332, 3252, 0)),
    Tile.of(2338, 3285, 0) to (68 to Tile.of(2338, 3281, 0)),
    Tile.of(2344, 3295, 0) to (59 to Tile.of(2346, 3300, 0))
)

private fun Player.buyCrystalWeapon(item: Int, cost: Int) {
    if (!inventory.hasFreeSlots()) {
        sendMessage("You need a free inventory slot.")
        return
    }
    if (!inventory.hasCoins(cost)) {
        sendMessage("You don't have enough money.")
        return
    }
    inventory.removeCoins(cost)
    inventory.addItem(item, 1)
}

private fun Player.attuneCrystalWeapon(item: Int, cost: Int) {
    if (inventory.hasCoins(cost) && inventory.containsItem(Obj.crystal_seed_old)) {
        inventory.deleteItem(Obj.crystal_seed_old, 1)
        inventory.removeCoins(cost)
        inventory.addItem(item, 1)
        incrementCrystalSeedRepair()
    } else {
        sendMessage("You don't have enough money.")
    }
}
