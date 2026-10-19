package com.rs.game.content.skills.crafting.urns

import com.rs.engine.dialogue.startConversation
import com.rs.engine.dialogue.statements.MakeXStatement
import com.rs.game.model.entity.player.Player
import com.rs.game.model.item.ItemTooltips
import com.rs.game.model.item.TooltipSection
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemClick
import com.rs.plugin.kts.onItemOnItem
import com.rs.plugin.kts.onNpcDrop
import com.rs.plugin.kts.onXpDrop
import kotlin.math.min
import org.darkan.core.Constants
import org.darkan.core.Utils
import org.darkan.core.cache.ItemDefinitions
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.model.Item

private const val URN_XP_KEY = "xp"

private val filledBySkillXp = setOf(
    Constants.SMITHING, Constants.WOODCUTTING, Constants.FISHING, Constants.COOKING, Constants.MINING,
    Constants.FARMING, Constants.HUNTER, Constants.RUNECRAFTING
)

private fun activatorOption(urn: Urn) = "Add ${activatorName(urn)}"

private fun activatorName(urn: Urn) = ItemDefinitions.getDefs(urn.activator).name.lowercase()

private fun Player.addUrnActivator(urn: Urn, requested: Int) {
    val amount = minOf(requested, inventory.getNumberOf(urn.noRune), inventory.getNumberOf(urn.activator))
    if (amount <= 0) {
        sendMessage("You need ${Utils.addArticle(activatorName(urn))} to activate this urn.")
        return
    }
    inventory.deleteItem(urn.noRune, amount)
    inventory.deleteItem(urn.activator, amount)
    inventory.addItem(urn.empty, amount)
    skills.addXpLamp(urn.skill, amount.toDouble())
    anim(urn.runeIn)
}

private fun Player.urnFor(urns: List<Urn>): Urn? =
    urns.firstOrNull { inventory.containsOneItem(it.empty, it.started) }

private fun Player.addUrnXp(urn: Urn?, xp: Double): Boolean {
    urn ?: return false
    val started = getItemFromInv(urn.started)
    if (started != null) {
        fillUrn(urn, started, xp)
        return true
    }
    if (getItemFromInv(urn.empty) == null || !inventory.hasFreeSlots()) return false
    inventory.deleteItem(urn.empty, 1)
    sendMessage("<col=e69d00><shad=000000>You start a new urn.", true)
    inventory.addItem(Item(urn.started, 1).addMetaData(URN_XP_KEY, xp))
    return true
}

private fun Player.fillUrn(urn: Urn, started: Item, xp: Double) {
    val filled = min(started.getMetaDataD(URN_XP_KEY) + xp, urn.capacity)
    if (filled >= urn.capacity) {
        incrementCount("${started.name} filled")
        started.deleteMetaData()
        started.changeId(urn.full)
    } else {
        started.addMetaData(URN_XP_KEY, filled)
    }
    sendMessage("<col=e69d00><shad=000000>Your urn is filled ${urnPercent(filled, urn)}%.", true)
}

private fun urnPercent(xp: Double, urn: Urn) = Utils.formatDouble(xp / urn.capacity * 100.0)

@ServerStartupEvent
fun mapUrns() {
    onItemClick(*Urn.byFull.keys.toTypedArray(), options = arrayOf("Teleport urn")) { e ->
        val urn = Urn.byFull.getValue(e.item.id)
        e.player.inventory.deleteItem(e.item.id, 1)
        e.player.anim(urn.teleport)
        e.player.skills.addXpLamp(urn.skill, urn.teleportXp)
    }

    onItemClick(*Urn.byStarted.keys.toTypedArray(), options = arrayOf("Check level")) { e ->
        val urn = Urn.byStarted.getValue(e.item.id)
        e.player.sendMessage("The urn is filled ${urnPercent(e.item.getMetaDataD(URN_XP_KEY), urn)}%.")
    }

    val activatorOptions = Urn.entries.map { activatorOption(it) }.distinct().toTypedArray()
    onItemClick(*Urn.byNoRune.keys.toTypedArray(), options = activatorOptions) { e ->
        val urn = Urn.byNoRune.getValue(e.item.id)
        if (e.option == activatorOption(urn)) e.player.addUrnActivator(urn, e.player.inventory.getNumberOf(urn.noRune))
    }

    onItemOnItem(*Urn.byNoRune.keys.toIntArray()) { e ->
        val urn = Urn.byNoRune[e.item1.id] ?: Urn.byNoRune[e.item2.id] ?: return@onItemOnItem
        if (e.getUsedWith(urn.noRune).id != urn.activator) {
            e.player.sendMessage("You must use ${Utils.addArticle(activatorName(urn))} to activate this urn.")
            return@onItemOnItem
        }
        val most = min(e.player.inventory.getNumberOf(urn.noRune), e.player.inventory.getNumberOf(urn.activator))
        if (most <= 0) {
            e.player.addUrnActivator(urn, 0)
            return@onItemOnItem
        }
        e.player.startConversation {
            makeX(intArrayOf(urn.empty), most)
            exec { e.player.addUrnActivator(urn, MakeXStatement.getQuantity(e.player)) }
        }
    }

    onXpDrop { e ->
        if (e.skillId in filledBySkillXp) e.player.addUrnXp(e.player.urnFor(Urn.forSkill(e.skillId)), e.xp)
    }

    onNpcDrop(null, arrayOf<Any>(Obj.impious_ashes, Obj.accursed_ashes, Obj.infernal_ashes)) { e ->
        val (urns, xp) = when (e.item.id) {
            Obj.impious_ashes -> listOf(Urn.IMPIOUS, Urn.ACCURSED, Urn.INFERNAL) to 4.0
            Obj.accursed_ashes -> listOf(Urn.ACCURSED, Urn.INFERNAL) to 12.5
            else -> listOf(Urn.INFERNAL) to 62.5
        }
        if (e.player.addUrnXp(e.player.urnFor(urns), xp)) e.deleteItem()
    }

    ItemTooltips.metadata(URN_XP_KEY, TooltipSection.CONTENTS) { _, item, _ ->
        Urn.byStarted[item.id]?.let { "Filled: ${ItemTooltips.value("${urnPercent(item.getMetaDataD(URN_XP_KEY), it)}%")}" }
    }
}
