package com.rs.game.content.quests.legendsquest

import com.rs.engine.pathfinder.routeEntityToTile
import com.rs.game.World
import com.rs.game.content.skills.woodcutting.Hatchet
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import com.rs.game.model.gameobject.GameObject
import com.rs.game.tasks.WorldTasks
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick
import com.rs.plugin.kts.onItemOnObject
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.model.ObjectShape
import world.gregs.voidps.type.Tile

private val QUEST_TOTEMS = arrayOf<Any>(Loc.loc_2144, Loc.lg_totem_pole_evil, Loc.lg_totem_pole_good)
private val TREE_STATES = mapOf(1 to Loc.yommitree_sapling, 2 to Loc.yommitree_adult, 3 to Loc.yommitree_felled, 4 to Loc.yommitree_trimmed, 5 to Loc.yommitree_totem)
private val TREE_OWNERS = mutableMapOf<Tile, Player>()
private val TREE_LOCATIONS = arrayOf<Any>(Loc.yommitree_sapling, Loc.yommitree_adult, Loc.yommitree_felled, Loc.yommitree_trimmed, Loc.yommitree_totem)
private val DEAD_TREES = arrayOf<Any>(Loc.yommitree_saplingdead, Loc.yommitree_adult_dead, Loc.yommitree_felled_rotten, Loc.yommitree_trimmed_rotten, Loc.yommitree_totem_rotten)

@ServerStartupEvent
fun mapLegendsTrees() {
    onItemOnObject(DEAD_TREES, Hatchet.entries.map { it.itemId }.toTypedArray<Any>()) { (player, obj) ->
        if (!player.inKharazi() || player.legendsStage < SEEDS || TREE_OWNERS.containsKey(obj.tile)) return@onItemOnObject
        val axe = Hatchet.getBest(player) ?: return@onItemOnObject
        if (!player.legendsLevel(Skills.WOODCUTTING, 50)) return@onItemOnObject
        player.anim(axe.animNormal())
        World.spawnObject(GameObject(Loc.fertilesoil, obj.type, obj.rotation, obj.tile))
        player.sendMessage("You clear away the rotten wood, revealing fertile soil.")
    }
    onItemOnObject(arrayOf(Loc.fertilesoil), arrayOf(Obj.yommiseeds_germ)) { (player, obj) ->
        if (!player.inKharazi() || player.legendsStage < WATER_RESTORED || player.legendsStage >= GIFT || player.legends.getI("boulders") == 0) { player.sendMessage("You must restore the sacred spring before growing the Yommi tree."); return@onItemOnObject }
        if (!player.legendsLevel(Skills.WOODCUTTING, 50) || !player.legendsLevel(Skills.HERBLORE, 45)) return@onItemOnObject
        if (!player.inventory.containsItem(Obj.goldbowlbless_pure)) { player.sendMessage("You need more pure water before you plant these seeds."); return@onItemOnObject }
        if (player.legends.getI("tree") > 0 && player.legends.getL("treeDue") <= System.currentTimeMillis()) clearYommiTree(player)
        if (player.legends.getI("tree") > 0 || TREE_OWNERS.containsKey(obj.tile)) { player.sendMessage("A Yommi tree is already growing here."); return@onItemOnObject }
        player.inventory.deleteItem(Obj.yommiseeds_germ, 1)
        if (!player.legendsTrial(Skills.HERBLORE, 45, 34)) { player.sendMessage("You plant the seed incorrectly. You will have to try again."); return@onItemOnObject }
        player.legends.setI("treeX", obj.x)
        player.legends.setI("treeY", obj.y)
        setYommiState(player, 1)
        player.sendMessage("You plant the Yommi tree seed in the soil. It starts to grow at a remarkable rate. It looks as if this Yommi tree needs to be watered.")
    }
    onItemOnObject(TREE_LOCATIONS, arrayOf(Obj.goldbowlbless_pure)) { (player, obj) ->
        if (!ownsTree(player, obj) || player.legends.getI("tree") != 1) return@onItemOnObject
        if (!player.replaceLegendsItem(Obj.goldbowlbless_pure, Obj.goldbowlbless_empty)) return@onItemOnObject
        setYommiState(player, 2)
        player.sendMessage("You water the Yommi tree from the Golden Bowl... It grows at a remarkable rate... Soon the tree stops growing. It looks tall enough now to make a good-sized totem pole.")
    }
    onObjectClick(*TREE_LOCATIONS) { (player, obj, option) ->
        val expected = when (obj.id) { Loc.yommitree_adult -> "Chop"; Loc.yommitree_felled -> "Trim-branches"; Loc.yommitree_trimmed -> "Carve"; Loc.yommitree_totem -> "Lift"; else -> null }
        if (option == expected) workYommiTree(player, obj)
    }
    onItemOnObject(TREE_LOCATIONS, Hatchet.entries.map { it.itemId }.toTypedArray<Any>()) { (player, obj) -> workYommiTree(player, obj) }
    onItemOnObject(QUEST_TOTEMS, arrayOf(Obj.thtotempole), checkDistance = false) { (player, obj) ->
        if (!player.inKharazi() || player.isLocked || !player.inventory.containsItem(Obj.thtotempole)) return@onItemOnObject
        if (player.legendsStage !in TOTEM..DEMON_DEFEATED) {
            player.sendMessage(if (player.legendsStage >= GIFT) "You have already replaced a corrupted totem." else "You must finish growing and carving your Yommi totem before replacing a corrupted totem.")
            return@onItemOnObject
        }
        val approach = listOf(obj.tile.transform(-1, 0, 0), obj.tile.transform(1, 0, 0), obj.tile.transform(0, -1, 0), obj.tile.transform(0, 1, 0))
            .sortedBy { it.distanceTo(player.tile) }.firstOrNull { tile ->
                World.floorFree(tile, 1) && routeEntityToTile(player, tile).let { !it.failed && !it.alternative }
            }
        if (approach == null) { player.sendMessage("You can't reach the totem from here."); return@onItemOnObject }
        player.walkToAndExecute(approach) { useLegendsTotem(player, obj) }
    }
    onObjectClick(*QUEST_TOTEMS, checkDistance = false) { (player, _, option) ->
        if (option == "Look") player.sendMessage(if (player.legendsStage >= GIFT) "This totem protects the Kharazi tribe from evil spirits." else "This totem has been corrupted by an evil spirit.")
    }
}

private fun useLegendsTotem(player: Player, obj: GameObject) {
    if (player.hasFinished() || player.isDead || player.isLocked || !player.inKharazi() || !player.withinDistance(obj.tile, 1) || World.getObjectWithId(obj.tile, obj.id) == null || !player.inventory.containsItem(Obj.thtotempole) || player.legendsStage !in TOTEM..DEMON_DEFEATED) return
    if (activeLegendsFight(player)) { player.sendMessage("You must defeat the summoned guardians and demon before replacing the totem."); return }
    player.faceTile(obj.tile)
    if (player.legendsStage == TOTEM) { beginFinalBattle(player, obj.tile); return }
    player.lock(3)
    player.anim(Seq.human_push)
    WorldTasks.schedule(2) {
        if (player.hasFinished() || player.isDead || player.legendsStage != DEMON_DEFEATED || !player.withinDistance(obj.tile, 1) || !player.inventory.containsItem(Obj.thtotempole) || World.getObjectWithId(obj.tile, obj.id) == null) return@schedule
        player.inventory.deleteItem(Obj.thtotempole, 1)
        player.legends.setI("totemX", obj.x)
        player.legends.setI("totemY", obj.y)
        World.spawnObject(GameObject(Loc.lg_totem_pole_good, obj.type, obj.rotation, obj.tile))
        player.advanceLegends(GIFT)
        player.sendMessage("You remove the corrupted totem pole and replace it with the one you carved yourself. You feel a lightness in the air, as though the whole jungle were sighing in relief.")
        giveGildedTotem(player)
    }
}

private fun treeTile(player: Player) = Tile.of(player.legends.getI("treeX"), player.legends.getI("treeY"), 0)
private fun ownsTree(player: Player, obj: GameObject): Boolean {
    if (player.legends.getI("tree") <= 0 || obj.tile != treeTile(player) || TREE_OWNERS[obj.tile] !== player) {
        player.sendMessage("This isn't your Yommi tree.")
        return false
    }
    if (System.currentTimeMillis() >= player.legends.getL("treeDue")) {
        clearYommiTree(player)
        player.sendMessage("Your Yommi tree has withered. You will need to plant another seed.")
        return false
    }
    return true
}

private fun setYommiState(player: Player, state: Int) {
    player.legends.setI("tree", state)
    player.legends.setL("treeDue", System.currentTimeMillis() + 60000L)
    val tile = treeTile(player)
    TREE_OWNERS[tile] = player
    World.spawnObject(GameObject(TREE_STATES.getValue(state), ObjectShape.SCENERY_INTERACT, 0, tile))
    scheduleYommiExpiry(player)
}

private fun scheduleYommiExpiry(player: Player) {
    val tile = treeTile(player)
    val due = player.legends.getL("treeDue")
    val delay = ((due - System.currentTimeMillis()).coerceAtLeast(0L) / 600L + 1L).toInt()
    WorldTasks.schedule(delay) {
        if (TREE_OWNERS[tile] === player && player.legends.getI("tree") > 0 && player.legends.getL("treeDue") == due) {
            clearYommiTree(player)
            if (!player.hasFinished()) player.sendMessage("Your Yommi tree has withered. You will need to plant another seed.")
        }
    }
}

internal fun restoreYommiTree(player: Player) {
    val state = player.legends.getI("tree")
    if (state <= 0) return
    if (player.legends.getL("treeDue") <= System.currentTimeMillis()) { clearYommiTree(player); return }
    val tile = treeTile(player)
    val owner = TREE_OWNERS[tile]
    if (owner != null && owner.username != player.username) { player.legends.setI("tree", 0); return }
    TREE_OWNERS[tile] = player
    if (owner !== player) scheduleYommiExpiry(player)
    if (World.getObjectWithId(tile, TREE_STATES.getValue(state)) == null) World.spawnObject(GameObject(TREE_STATES.getValue(state), ObjectShape.SCENERY_INTERACT, 0, tile))
}

internal fun clearYommiTree(player: Player) {
    val tile = treeTile(player)
    if (tile.x == 0 || tile.y == 0) return
    if (TREE_OWNERS[tile]?.username == player.username) {
        TREE_OWNERS.remove(tile)
        World.spawnObject(GameObject(Loc.fertilesoil, ObjectShape.SCENERY_INTERACT, 0, tile))
    }
    player.legends.setI("tree", 0)
    player.legends.setL("treeDue", 0L)
}

private fun workYommiTree(player: Player, obj: GameObject) {
    if (!ownsTree(player, obj) || player.isLocked) return
    val state = player.legends.getI("tree")
    if (state == 1) { player.sendMessage("It looks as if this Yommi tree needs to be watered."); return }
    if (state == 5) {
        if (!player.inventory.hasFreeSlots()) { player.sendMessage("You need a free inventory slot to lift the totem."); return }
        if (!player.inventory.addItem(Obj.thtotempole, 1)) return
        clearYommiTree(player)
        player.advanceLegends(TOTEM)
        player.sendMessage("This totem pole looks very heavy... But you manage to lift it.")
        return
    }
    val strongAxes = arrayOf(Hatchet.RUNE, Hatchet.DRAGON, Hatchet.INFERNO)
    val axe = strongAxes.lastOrNull { (player.inventory.containsItem(it.itemId) || player.equipment.weaponId == it.itemId) && player.skills.getLevel(Skills.WOODCUTTING) >= it.useLevel }
    if (axe == null) { player.sendMessage("You need a rune axe or better to work the Yommi tree."); return }
    if (!player.legendsLevel(Skills.WOODCUTTING, 50)) return
    player.anim(axe.animNormal())
    player.lock(2)
    setYommiState(player, state + 1)
    player.sendMessage(when (state) { 2 -> "You chop the Yommi tree down."; 3 -> "You professionally wield your axe and trim the branches from the Yommi tree."; else -> "You professionally wield your axe as you carve a wonderful totem pole from the Yommi tree trunk." })
}
