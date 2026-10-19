package com.rs.game.content.skills.prayer.cremation

import com.rs.engine.dialogue.Conversation
import com.rs.engine.dialogue.statements.MakeXStatement
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnItem
import com.rs.plugin.kts.onItemOnObject
import com.rs.plugin.kts.onLogin
import com.rs.plugin.kts.onObjectClick
import com.rs.utils.DropSets
import com.rs.utils.drop.DropTable
import org.darkan.core.Constants
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Varbit
import world.gregs.voidps.type.Tile

@ServerStartupEvent
fun mapCremation() {
    val oils = intArrayOf(Obj.sacred_oil4, Obj.sacred_oil3, Obj.sacred_oil2, Obj.sacred_oil1)
    onItemOnItem(oils, PyreLog.values().map { it.baseLog }.toIntArray()) { e ->
        val log = PyreLog.forBaseLog(e.getUsedWith(*oils).id) ?: return@onItemOnItem
        if (e.player.skills.getLevel(Constants.FIREMAKING) < log.level) {
            e.player.sendMessage("You need a Firemaking level of ${log.level} to pyre these logs.")
            return@onItemOnItem
        }
        e.player.startConversation(Conversation(e.player)
            .addNext(MakeXStatement(intArrayOf(log.itemId), e.player.inventory.getNumberOf(log.baseLog)))
            .addNext(Runnable { e.player.actionManager.setAction(CraftPyreLogs(log)) }))
    }
    onLogin { (player) ->
        player.vars.setVarBit(Varbit.myq4_columbarium_entrance_vis, 1)
        player.vars.setVarBit(Varbit.myq4_columbarium_tally_vis, 1)
    }
    onObjectClick(Loc.loc_30621, Loc.myq4_columbarium_stairs_lower) { (player, obj) ->
        player.useStairs(if (obj.id == Loc.loc_30621) Tile.of(3422, 9965, 0) else Tile.of(3425, 9899, 0))
    }
    val emptyPyres = arrayOf<Any>(Loc.temple_pyre, Loc.myq4_pyre)
    onItemOnObject(emptyPyres, PyreLog.values().map { it.itemId as Any }.toTypedArray()) { (player, obj, item) ->
        val log = PyreLog.forId(item.id) ?: return@onItemOnObject
        player.inventory.deleteItem(log.itemId, 1)
        Pyre(player, obj, log, obj.id == Loc.temple_pyre).createReplace()
        player.anim(Seq.human_reachforladdertop)
        player.sendMessage("You put some logs on the pyre.")
    }
    onObjectClick(*emptyPyres) { (player, obj) ->
        val log = PyreLog.values().firstOrNull { player.inventory.containsItem(it.itemId, 1) }
        if (log == null) player.sendMessage("You need at least 1 pyre log to use the funeral pyre.")
        else {
            player.inventory.deleteItem(log.itemId, 1)
            Pyre(player, obj, log, obj.id == Loc.temple_pyre).createReplace()
            player.anim(Seq.human_reachforladdertop)
            player.sendMessage("You put some logs on the pyre.")
        }
    }
    val withoutCorpse = PyreLog.values().flatMap { listOf(it.shadeNoCorpse as Any, it.vyreNoCorpse as Any) }.toTypedArray()
    onItemOnObject(withoutCorpse, Corpse.values().flatMap { corpse -> corpse.itemIds.map { it as Any } }.toTypedArray()) { (player, obj, item) ->
        val pyre = obj as? Pyre ?: return@onItemOnObject
        val corpse = Corpse.forId(item.id) ?: return@onItemOnObject
        if (!pyre.ownedBy(player)) {
            player.sendMessage("That's not your pyre!")
            return@onItemOnObject
        }
        if (!pyre.setCorpse(corpse)) player.sendMessage("The logs aren't strong enough to burn this type of corpse.")
        else {
            player.inventory.deleteItem(item.id, 1)
            player.anim(Seq.human_reachforladdertop)
            player.sendMessage(if (pyre.isShadePyre) "You place the shade's remains on the logs." else "You place the remains on the logs.")
        }
    }
    val withCorpse = PyreLog.values().flatMap { listOf(it.shadeCorpse as Any, it.vyreCorpse as Any) }.toTypedArray()
    onObjectClick(*withCorpse) { (player, obj) -> (obj as? Pyre)?.light(player) }
    onItemOnObject(withCorpse, arrayOf(Obj.tinderbox)) { (player, obj) -> (obj as? Pyre)?.light(player) }
    onObjectClick(Loc.myq4_columbarium_wall_active, Loc.myq4_columbarium_wall_active_m) { (player) ->
        if (!player.inventory.containsItem(Obj.myq4_columbarium_key_inv, 1)) player.sendMessage("It looks securely locked.")
        else openColumbariumRecess(player)
    }
    onItemOnObject(arrayOf(Loc.myq4_columbarium_wall_active, Loc.myq4_columbarium_wall_active_m), arrayOf(Obj.myq4_columbarium_key_inv)) { (player) -> openColumbariumRecess(player) }
}

private fun openColumbariumRecess(player: Player) {
    if (!player.inventory.containsItem(Obj.myq4_columbarium_key_inv, 1)) return
    player.lock(2)
    player.inventory.deleteItem(Obj.myq4_columbarium_key_inv, 1)
    player.anim(Seq.human_reachforladdertop)
    DropTable.calculateDrops(player, DropSets.getDropSet("columbarium_safe")).filterNotNull().forEach { player.inventory.addItemDrop(it) }
}
