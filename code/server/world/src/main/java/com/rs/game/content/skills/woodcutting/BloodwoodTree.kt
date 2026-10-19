package com.rs.game.content.skills.woodcutting

import com.rs.game.World
import com.rs.game.content.skills.farming.FarmPatch
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.actions.Action
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import com.rs.game.model.gameobject.GameObject
import com.rs.game.tasks.Task
import com.rs.game.tasks.WorldTasks
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onLogin
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.Constants
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varbit

private const val BLOODWOOD_LOGS = Obj.sniper_bloodwood_logs

// State 0 is a grown tree on some sites and rocks or debris on others.
private const val GROWN = 1
private const val STUMP = 2

private const val FELL_CHANCE = 16

private const val REGROW_TICKS = 50

private val STATE = intArrayOf(
    Varbit.sniper_bloodtree_1, Varbit.sniper_bloodtree_2, Varbit.sniper_bloodtree_3,
    Varbit.sniper_bloodtree_4, Varbit.sniper_bloodtree_5, Varbit.sniper_bloodtree_6,
)

private val REGROW = intArrayOf(
    Varbit.sniper_bloodtree_time_1, Varbit.sniper_bloodtree_time_2, Varbit.sniper_bloodtree_time_3,
    Varbit.sniper_bloodtree_time_4, Varbit.sniper_bloodtree_time_5, Varbit.sniper_bloodtree_time_6,
)

// The client resolves the varbit transform itself, so the handler sees these placeholder ids.
private val TRUNK_SITES = mapOf(
    Loc.loc_70001 to 0, Loc.loc_70002 to 1, Loc.loc_70003 to 2,
    Loc.loc_70004 to 3, Loc.loc_70005 to 4, Loc.loc_70006 to 5,
)

private val BLOODWOOD_TREE_IDS = intArrayOf(
    Loc.sniper_bloodrot_tree_trunk, Loc.sniper_bloodrot_tree_trunk_snow,
    Loc.sniper_bloodrot_tree_stump, Loc.sniper_bloodrot_tree_stump_snow,
)

private val DECORATION_TRUNK_IDS = intArrayOf(Loc.sniper_bloodrot_tree_trunk, Loc.sniper_bloodrot_tree_trunk_snow)

fun bloodwoodTreeNear(player: Player): GameObject? {
    for (obj in World.getBaseObjectsInChunkRange(player.chunkId, 1)) {
        val id = obj.getDefinitions(player).id
        if (id in BLOODWOOD_TREE_IDS && obj.tile.distanceTo(player.tile) <= 2)
            return obj
    }
    return null
}

private class BloodwoodWoodcutting(private val tree: GameObject, private val site: Int) : Action() {
    private var hatchet: Hatchet? = null

    override fun start(entity: Entity): Boolean {
        val player = entity as? Player ?: return false
        if (player.vars.getVarBit(STATE[site]) == STUMP)
            return false
        if (player.skills.getLevel(Constants.WOODCUTTING) < TreeType.BLOODWOOD.level) {
            player.sendMessage("You need a Woodcutting level of ${TreeType.BLOODWOOD.level} to chop down this tree.")
            return false
        }
        hatchet = Hatchet.getBest(player)
        if (hatchet == null) {
            player.sendMessage("You do not have a hatchet which you have the woodcutting level to use.")
            return false
        }
        if (!player.inventory.hasFreeSlots()) {
            player.sendMessage("Your inventory is too full to hold any more bloodwood logs.")
            return false
        }
        entity.faceObject(tree)
        player.sendMessage("You swing your hatchet at the tree.", true)
        setActionDelay(entity, 4)
        return true
    }

    override fun process(entity: Entity): Boolean {
        val player = entity as? Player ?: return false
        entity.anim(hatchet!!.getAnim(TreeType.BLOODWOOD))
        return player.vars.getVarBit(STATE[site]) != STUMP && bloodwoodTreeNear(player) != null
    }

    override fun processWithDelay(entity: Entity): Int {
        val player = entity as Player
        val level = player.skills.getLevel(Constants.WOODCUTTING) + player.getInvisibleSkillBoost(Skills.WOODCUTTING)
        if (TreeType.BLOODWOOD.rollSuccess(player.auraManager.woodcuttingMul, level, hatchet!!)) {
            player.inventory.addItemDrop(BLOODWOOD_LOGS, 1)
            player.skills.addXp(Constants.WOODCUTTING, TreeType.BLOODWOOD.xp)
            player.sendMessage("You get some bloodwood logs.", true)
            if (Utils.random(FELL_CHANCE) == 0) {
                fell(player, site)
                entity.anim(-1)
                return -1
            }
        }
        return 3
    }

    override fun stop(entity: Entity) {
        setActionDelay(entity, 4)
    }
}

private fun fell(player: Player, site: Int) {
    player.vars.saveVarBit(STATE[site], STUMP)
    player.vars.saveVarBit(REGROW[site], REGROW_TICKS)
    player.vars.saveVarBit(Varbit.sniper_cut_tree, 1)
    player.sendMessage("The bloodwood tree crashes to the ground, leaving a stump.", true)
}

private fun regrow(player: Player, site: Int) {
    player.vars.saveVarBit(STATE[site], GROWN)
    player.vars.saveVarBit(REGROW[site], 0)
}

private fun tickTrees(player: Player) {
    for (site in STATE.indices) {
        if (player.vars.getVarBit(STATE[site]) != STUMP)
            continue
        val remaining = player.vars.getVarBit(REGROW[site]) - 1
        if (remaining <= 0)
            regrow(player, site)
        else
            player.vars.saveVarBit(REGROW[site], remaining)
    }
}

private fun catchUp(player: Player) {
    for (site in STATE.indices) {
        val state = player.vars.getVarBit(STATE[site])
        if (state == 0) {
            player.vars.saveVarBit(STATE[site], GROWN)
            continue
        }
        if (state != STUMP)
            continue
        val remaining = player.vars.getVarBit(REGROW[site]) - 1
        if (remaining <= 0)
            regrow(player, site)
        else
            player.vars.saveVarBit(REGROW[site], remaining)
    }
}

@ServerStartupEvent
fun registerBloodwoodTrees() {
    onObjectClick(
        Loc.loc_70001, Loc.loc_70002, Loc.loc_70003, Loc.loc_70004, Loc.loc_70005, Loc.loc_70006,
    ) { e ->
        if (e.option != "Chop down")
            return@onObjectClick
        val player = e.player
        if (player.vars.getVarBit(Varbit.sniper_spoken_to_mami_rimba) == 0) {
            player.sendMessage("You must speak to Mami Rimba before you can cut a bloodwood tree.", true)
            return@onObjectClick
        }
        val site = TRUNK_SITES[e.objectId] ?: return@onObjectClick
        if (player.vars.getVarBit(STATE[site]) == STUMP)
            return@onObjectClick
        player.actionManager.setAction(BloodwoodWoodcutting(e.obj, site))
    }

    onObjectClick(*DECORATION_TRUNK_IDS.toTypedArray()) { e ->
        if (e.option != "Chop down")
            return@onObjectClick
        e.player.sendMessage("That's Mami Rimba's private tree.", true)
    }

    onLogin { e ->
        val player = e.player
        catchUp(player)
        WorldTasks.scheduleLooping(object : Task() {
            override fun run() {
                if (player.hasFinished()) {
                    stop()
                    return
                }
                tickTrees(player)
            }
        }, FarmPatch.FARMING_TICK, FarmPatch.FARMING_TICK)
    }
}
