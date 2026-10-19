package com.rs.game.content.skills.fletching

import com.rs.engine.dialogue.Conversation
import com.rs.engine.dialogue.statements.MakeXStatement
import com.rs.engine.dialogue.statements.MakeXStatement.MakeXType
import com.rs.game.content.Toolbelt
import com.rs.game.content.skills.woodcutting.bloodwoodTreeNear
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.actions.PlayerAction
import com.rs.game.tasks.Task
import com.rs.game.tasks.WorldTasks
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemClick
import com.rs.plugin.kts.onItemOnItem
import com.rs.plugin.kts.onLogin
import org.darkan.core.Constants
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varbit

private const val BLOODWOOD_LOGS = Obj.sniper_bloodwood_logs
private const val BAKRIMINEL_SHAFTS = Obj.sniper_bakriminel_bolt_shafts
private const val BAKRIMINEL_TIPS = Obj.sniper_bakriminel_bolt_tops
private const val BAKRIMINEL_BOLTS = Obj.sniper_bolts
private const val KNIFE = Obj.knife
private const val FLETCHING_LEVEL = 93
private const val SHAFTS_PER_LOG = 10
private const val BOLTS_PER_SET = 10

class BloodwoodFletching(private val product: Int, private var quantity: Int) : PlayerAction() {
    override fun start(player: Player): Boolean {
        if (!checkAll(player))
            return false
        player.anim(1248)
        setActionDelay(player, 2)
        return true
    }

    override fun process(player: Player): Boolean = checkAll(player)

    private fun checkAll(player: Player): Boolean {
        if (player.skills.getLevel(Constants.FLETCHING) < FLETCHING_LEVEL) {
            player.simpleDialogue("You need a Fletching level of $FLETCHING_LEVEL to fletch bakriminel bolts.")
            return false
        }
        if (!player.inventory.containsItem(KNIFE) && !Toolbelt.containsTool(player, KNIFE)) {
            player.simpleDialogue("You need a knife to fletch the bloodwood logs.")
            return false
        }
        if (bloodwoodTreeNear(player) == null) {
            player.simpleDialogue("You must be near a bloodwood tree to fletch these.")
            return false
        }
        return if (product == BAKRIMINEL_SHAFTS)
            player.inventory.containsItem(BLOODWOOD_LOGS, 1)
        else
            player.inventory.containsItem(BAKRIMINEL_SHAFTS, BOLTS_PER_SET) && player.inventory.containsItem(BAKRIMINEL_TIPS, BOLTS_PER_SET)
    }

    override fun processWithDelay(player: Player): Int {
        if (product == BAKRIMINEL_SHAFTS) {
            player.inventory.deleteItem(BLOODWOOD_LOGS, 1)
            player.inventory.addItem(BAKRIMINEL_SHAFTS, SHAFTS_PER_LOG)
            player.sendMessage("You carefully cut the bloodwood logs into bakriminel bolt shafts.", true)
        } else {
            player.inventory.deleteItem(BAKRIMINEL_SHAFTS, BOLTS_PER_SET)
            player.inventory.deleteItem(BAKRIMINEL_TIPS, BOLTS_PER_SET)
            player.inventory.addItem(BAKRIMINEL_BOLTS, BOLTS_PER_SET)
            player.skills.addXp(Constants.FLETCHING, 13.0 * BOLTS_PER_SET)
            player.vars.saveVarBit(Varbit.sniper_fletched_bolts, 1)
            player.sendMessage("You attach the bakriminel bolt tips to the bolt shafts.", true)
        }
        quantity--
        if (quantity <= 0)
            return -1
        player.anim(1248)
        return 2
    }

    override fun stop(player: Player) {
        setActionDelay(player, 3)
    }
}

private class BloodwoodFletchingD(player: Player, private val product: Int) : Conversation(player) {
    init {
        val input = if (product == BAKRIMINEL_SHAFTS) BLOODWOOD_LOGS else BAKRIMINEL_SHAFTS
        val max = player.inventory.getNumberOf(input).coerceAtLeast(1)
        val makeX = addNext(
            MakeXStatement(
                MakeXType.MAKE,
                max,
                "Choose how many you wish to make,<br>then click on the item to begin.",
                intArrayOf(product)
            )
        )
        makeX.addNext {
            var quantity = MakeXStatement.getQuantity(player)
            if (product == BAKRIMINEL_SHAFTS) {
                quantity = quantity.coerceAtMost(player.inventory.getNumberOf(BLOODWOOD_LOGS))
                if (quantity <= 0) {
                    player.simpleDialogue("You don't have any bloodwood logs to fletch.")
                    return@addNext
                }
            } else {
                quantity = minOf(
                    quantity,
                    player.inventory.getNumberOf(BAKRIMINEL_SHAFTS) / BOLTS_PER_SET,
                    player.inventory.getNumberOf(BAKRIMINEL_TIPS) / BOLTS_PER_SET
                )
                if (quantity <= 0) {
                    player.simpleDialogue("You need ten bakriminel bolt shafts and ten bakriminel bolt tips to make a set of bolts.")
                    return@addNext
                }
            }
            player.actionManager.setAction(BloodwoodFletching(product, quantity))
        }
        create()
    }
}

private fun startBloodwoodFletching(player: Player, product: Int) {
    if (player.skills.getLevel(Constants.FLETCHING) < FLETCHING_LEVEL) {
        player.simpleDialogue("You need a Fletching level of $FLETCHING_LEVEL to fletch bakriminel bolts.")
        return
    }
    if (!player.inventory.containsItem(KNIFE) && !Toolbelt.containsTool(player, KNIFE)) {
        player.simpleDialogue("You need a knife to fletch the bloodwood logs.")
        return
    }
    if (bloodwoodTreeNear(player) == null) {
        player.simpleDialogue("You must be near a bloodwood tree to fletch these.")
        return
    }
    player.startConversation(BloodwoodFletchingD(player, product))
}

@ServerStartupEvent
fun registerBloodwoodFletching() {
    onItemOnItem(intArrayOf(KNIFE), intArrayOf(BLOODWOOD_LOGS)) { e ->
        startBloodwoodFletching(e.player, BAKRIMINEL_SHAFTS)
    }
    onItemClick(BLOODWOOD_LOGS, options = arrayOf("Fletch")) { e ->
        startBloodwoodFletching(e.player, BAKRIMINEL_SHAFTS)
    }
    onItemOnItem(intArrayOf(BAKRIMINEL_SHAFTS), intArrayOf(BAKRIMINEL_TIPS)) { e ->
        startBloodwoodFletching(e.player, BAKRIMINEL_BOLTS)
    }
}

@ServerStartupEvent
fun registerBloodwoodLogCrumbling() {
    onLogin { e ->
        val player = e.player
        WorldTasks.scheduleLooping(object : Task() {
            override fun run() {
                if (player.hasFinished()) {
                    stop()
                    return
                }
                val logs = player.inventory.getNumberOf(BLOODWOOD_LOGS)
                val shafts = player.inventory.getNumberOf(BAKRIMINEL_SHAFTS)
                if (logs <= 0 && shafts <= 0)
                    return
                if (bloodwoodTreeNear(player) != null)
                    return
                if (logs > 0) {
                    player.inventory.deleteItem(BLOODWOOD_LOGS, logs)
                    player.sendMessage("The wood of the bloodwood tree crumbles to sawdust without the magic of the tree to sustain it.", true)
                }
                if (shafts > 0) {
                    player.inventory.deleteItem(BAKRIMINEL_SHAFTS, shafts)
                    player.sendMessage("The bakriminel bolt shafts crumble to sawdust without the magic of the tree to sustain them.", true)
                }
            }
        }, 0, 5)
    }
}
