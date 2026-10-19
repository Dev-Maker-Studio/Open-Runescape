package com.rs.game.content.minigames.pest

import com.rs.engine.dialogue.sendOptionsDialogue
import com.rs.engine.quest.Quest
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onButtonClick
import com.rs.plugin.kts.onItemOnNpc
import com.rs.plugin.kts.onNpcClick
import com.rs.utils.DropSets
import com.rs.utils.drop.DropTable
import org.darkan.core.Constants
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varp
import org.darkan.core.model.Item

private const val PENDING_EXCHANGE = "pestControlExchange"
private const val VOID_LEVEL = 42
private const val VOID_PRAYER_LEVEL = 22
private const val XP_LEVEL = 25
private const val PACK_LEVEL = 25
private const val XP_LEVEL_DIVISOR = 600
private const val CHARM_COST = 2
private const val DEFLECTOR_CONQUEST_RATING = 1250
private const val ELITE_UPGRADE_COST = 100
private const val KORASI_SWORD_PRICE = 200_000
private const val HERB_PACK_MIN = 10
private const val HERB_PACK_MAX = 15
private const val SEED_PACK_ROLLS = 6
private val MINERAL_PACK_COAL = 18..25
private val MINERAL_PACK_IRON = 12..18
private val VOID_STATS = intArrayOf(Constants.ATTACK, Constants.STRENGTH, Constants.DEFENSE, Constants.HITPOINTS, Constants.RANGE, Constants.MAGIC)
private val ELITE_UPGRADES = mapOf(
    Obj.pest_void_knight_top to Obj.vkq3_reward_torso_black,
    Obj.dummy_pest_void_knight_top to Obj.vkq3_reward_torso_black,
    Obj.obj_8840 to Obj.vkq3_reward_legs_black,
)

private enum class Page(val container: Int, vararg val tabs: Int) {
    EXPERIENCE(Component.interface_1011.component_1011_63, Component.interface_1011.component_1011_20, Component.interface_1011.component_1011_73),
    EQUIPMENT(Component.interface_1011.component_1011_69, Component.interface_1011.component_1011_29, Component.interface_1011.component_1011_75),
    CONSUMABLES(Component.interface_1011.component_1011_70, Component.interface_1011.component_1011_24, Component.interface_1011.component_1011_31),
}

private enum class EquipmentReward(val button: Int, val obj: Int, val cost: Int, val needsLevels: Boolean) {
    MELEE_HELM(Component.interface_1011.component_1011_15, Obj.obj_11665, 200, true),
    RANGER_HELM(Component.interface_1011.component_1011_196, Obj.obj_11664, 200, true),
    MAGE_HELM(Component.interface_1011.component_1011_208, Obj.obj_11663, 200, true),
    TOP(Component.interface_1011.component_1011_220, Obj.pest_void_knight_top, 250, true),
    ROBE(Component.interface_1011.component_1011_232, Obj.obj_8840, 250, true),
    GLOVES(Component.interface_1011.component_1011_244, Obj.pest_void_knight_gloves, 150, true),
    MACE(Component.interface_1011.component_1011_256, Obj.pest_void_knight_mace, 250, true),
    DEFLECTOR(Component.interface_1011.component_1011_268, Obj.conq_reward_shield, 150, true),
    SEAL(Component.interface_1011.component_1011_280, Obj.obj_11666, 10, false),
}

private enum class XpBatch(val points: Int, val bonus: Double) { ONE(1, 1.0), TEN(10, 1.01), HUNDRED(100, 1.1) }

private enum class XpReward(val skill: Int, val multiplier: Int, vararg val buttons: Int) {
    ATTACK(Constants.ATTACK, 35, Component.interface_1011.component_1011_68, Component.interface_1011.component_1011_86, Component.interface_1011.component_1011_88),
    STRENGTH(Constants.STRENGTH, 35, Component.interface_1011.component_1011_100, Component.interface_1011.component_1011_102, Component.interface_1011.component_1011_104),
    DEFENCE(Constants.DEFENSE, 35, Component.interface_1011.component_1011_116, Component.interface_1011.component_1011_118, Component.interface_1011.component_1011_120),
    CONSTITUTION(Constants.HITPOINTS, 35, Component.interface_1011.component_1011_132, Component.interface_1011.component_1011_134, Component.interface_1011.component_1011_136),
    RANGED(Constants.RANGE, 32, Component.interface_1011.component_1011_148, Component.interface_1011.component_1011_150, Component.interface_1011.component_1011_152),
    MAGIC(Constants.MAGIC, 32, Component.interface_1011.component_1011_164, Component.interface_1011.component_1011_166, Component.interface_1011.component_1011_168),
    PRAYER(Constants.PRAYER, 18, Component.interface_1011.component_1011_180, Component.interface_1011.component_1011_182, Component.interface_1011.component_1011_184),
}

private enum class Pack(val button: Int, val cost: Int, val skill: Int, val label: String) {
    HERB(Component.interface_1011.component_1011_291, 30, Constants.HERBLORE, "herb pack"),
    MINERAL(Component.interface_1011.component_1011_302, 15, Constants.MINING, "mineral pack"),
    SEED(Component.interface_1011.component_1011_313, 15, Constants.FARMING, "seed pack"),
}

private enum class Charm(val obj: Int, val one: Int, val ten: Int, val all: Int) {
    SPINNER(Obj.lore_pest_spinner_token, Component.interface_1011.component_1011_324, Component.interface_1011.component_1011_326, Component.interface_1011.component_1011_328),
    TORCHER(Obj.lore_pest_torcher_token, Component.interface_1011.component_1011_339, Component.interface_1011.component_1011_341, Component.interface_1011.component_1011_343),
    RAVAGER(Obj.lore_pest_ravager_token, Component.interface_1011.component_1011_354, Component.interface_1011.component_1011_356, Component.interface_1011.component_1011_358),
    SHIFTER(Obj.lore_pest_shifter_token, Component.interface_1011.component_1011_369, Component.interface_1011.component_1011_371, Component.interface_1011.component_1011_373);

    fun amount(button: Int, player: Player) = when (button) {
        one -> 1
        ten -> 10
        all -> player.commendations / CHARM_COST
        else -> 0
    }
}

private class Exchange(val cost: Int, val description: String, val refusal: () -> String?, val deliver: () -> Unit)

private val equipmentByButton = EquipmentReward.entries.associateBy { it.button }
private val xpByButton = XpReward.entries.flatMap { reward -> reward.buttons.mapIndexed { index, button -> button to (reward to XpBatch.entries[index]) } }.toMap()
private val packByButton = Pack.entries.associateBy { it.button }
private val charmByButton = Charm.entries.flatMap { charm -> listOf(charm.one, charm.ten, charm.all).map { it to charm } }.toMap()
private val pageByTab = Page.entries.flatMap { page -> page.tabs.map { it to page } }.toMap()

@ServerStartupEvent
fun mapCommendationExchange() {
    onNpcClick(Npc.conq_tutorial_npc, "Void Knight", options = arrayOf("Exchange")) { (player) -> player.openCommendationExchange() }

    onButtonClick(Interface.interface_1011) { e ->
        val player = e.player
        val button = e.component
        pageByTab[button]?.let { return@onButtonClick player.showPage(it) }
        equipmentByButton[button]?.let { return@onButtonClick player.offer(equipmentExchange(player, it)) }
        xpByButton[button]?.let { (reward, batch) -> return@onButtonClick player.offer(xpExchange(player, reward, batch)) }
        packByButton[button]?.let { return@onButtonClick player.offer(packExchange(player, it)) }
        charmByButton[button]?.let { return@onButtonClick player.offer(charmExchange(player, it, it.amount(button, player))) }
        when (button) {
            Component.interface_1011.component_1011_383 -> player.confirmExchange()
            Component.interface_1011.component_1011_385 -> player.hideConfirmation()
            Component.interface_1011.component_1011_51 -> player.closeInterfaces()
        }
    }

    onNpcClick(Npc.npc_11681) { (player) ->
        if (!player.isQuestComplete(Quest.VOID_STARES_BACK, "to buy Korasi's sword")) return@onNpcClick
        player.sendOptionsDialogue("Would you like to buy Korasi's sword for ${Utils.formatNumber(KORASI_SWORD_PRICE)} coins?") {
            opExec("Yes, that sounds like a fair price.") {
                if (!player.inventory.hasCoins(KORASI_SWORD_PRICE)) {
                    player.sendMessage("You don't have enough money for that.")
                    return@opExec
                }
                player.inventory.removeCoins(KORASI_SWORD_PRICE)
                player.inventory.addItemDrop(Obj.vkq3_korasi_sword_reward, 1)
            }
            op("Never mind.")
        }
    }

    onItemOnNpc(Npc.npc_11681) { (player, item) ->
        val elite = ELITE_UPGRADES[item.id] ?: return@onItemOnNpc
        if (!player.isQuestComplete(Quest.VOID_STARES_BACK, "to upgrade void knight armour")) return@onItemOnNpc
        if (player.commendations < ELITE_UPGRADE_COST) {
            player.sendMessage("You need $ELITE_UPGRADE_COST Void Knight commendations to upgrade void knight armour.")
            return@onItemOnNpc
        }
        player.sendOptionsDialogue("Upgrade this piece to elite for $ELITE_UPGRADE_COST commendations?") {
            opExec("Yes, upgrade my armour.") {
                if (player.commendations < ELITE_UPGRADE_COST || player.inventory.getItem(item.slot)?.id != item.id) return@opExec
                player.commendations -= ELITE_UPGRADE_COST
                item.id = elite
                player.inventory.refresh(item.slot)
            }
            op("Never mind.")
        }
    }
}

fun Player.openCommendationExchange() {
    interfaceManager.sendInterface(Interface.interface_1011)
    vars.setVar(Varp.varp_1875, DEFLECTOR_CONQUEST_RATING)
    refreshCommendations()
    hideConfirmation()
    showPage(Page.EXPERIENCE)
}

private fun Player.showPage(page: Page) {
    for (other in Page.entries)
        packets.setIFHidden(Utils.interfaceIdFromHash(other.container), Utils.componentIdFromHash(other.container), other != page)
}

private fun Player.offer(exchange: Exchange) {
    exchange.refusal()?.let {
        sendMessage(it)
        return
    }
    if (commendations < exchange.cost) {
        sendMessage("You need ${exchange.cost} commendations to exchange for ${exchange.description}.")
        return
    }
    tempAttribs.setO<Exchange>(PENDING_EXCHANGE, exchange)
    packets.setIFText(Component.interface_1011.component_1011_380, "Are you sure you wish to exchange ${exchange.cost} Commendations in return for ${exchange.description}?")
    packets.setIFHidden(Utils.interfaceIdFromHash(Component.interface_1011.component_1011_71), Utils.componentIdFromHash(Component.interface_1011.component_1011_71), false)
}

private fun Player.hideConfirmation() {
    tempAttribs.removeO<Exchange>(PENDING_EXCHANGE)
    packets.setIFHidden(Utils.interfaceIdFromHash(Component.interface_1011.component_1011_71), Utils.componentIdFromHash(Component.interface_1011.component_1011_71), true)
}

private fun Player.confirmExchange() {
    val exchange = tempAttribs.getO<Exchange>(PENDING_EXCHANGE)
    hideConfirmation()
    if (exchange == null) return
    exchange.refusal()?.let {
        sendMessage(it)
        return
    }
    if (commendations < exchange.cost) {
        sendMessage("You don't have enough commendations remaining to complete this exchange.")
        return
    }
    commendations -= exchange.cost
    incrementCount("Pest Control commendations spent", exchange.cost)
    exchange.deliver()
}

private fun equipmentExchange(player: Player, reward: EquipmentReward) = Exchange(reward.cost, Utils.addArticle(Item(reward.obj).name), refusal = {
    when {
        reward.needsLevels && (VOID_STATS.any { player.skills.getLevelForXp(it) < VOID_LEVEL } || player.skills.getLevelForXp(Constants.PRAYER) < VOID_PRAYER_LEVEL) ->
            "You need level $VOID_LEVEL Attack, Strength, Defence, Constitution, Ranged and Magic, and level $VOID_PRAYER_LEVEL Prayer, to exchange for Void Knight equipment."
        !player.inventory.hasFreeSlots() -> "You don't have enough inventory space."
        else -> null
    }
}) { player.inventory.addItem(reward.obj, 1) }

private fun xpExchange(player: Player, reward: XpReward, batch: XpBatch): Exchange {
    val level = player.skills.getLevelForXp(reward.skill)
    val xp = (level * level / XP_LEVEL_DIVISOR) * reward.multiplier * batch.points * batch.bonus
    return Exchange(batch.points, "${Utils.formatNumber(xp.toInt())} ${Skills.SKILL_NAME[reward.skill]} XP", refusal = {
        if (player.skills.getLevelForXp(reward.skill) < XP_LEVEL) "You need a ${Skills.SKILL_NAME[reward.skill]} level of $XP_LEVEL to exchange for experience." else null
    }) { player.skills.addXp(reward.skill, xp) }
}

private fun packExchange(player: Player, pack: Pack) = Exchange(pack.cost, "a ${pack.label}", refusal = {
    when {
        player.skills.getLevelForXp(pack.skill) < PACK_LEVEL -> "You need a ${Skills.SKILL_NAME[pack.skill]} level of $PACK_LEVEL to exchange for a ${pack.label}."
        !player.inventory.hasFreeSlots() -> "You don't have enough inventory space."
        else -> null
    }
}) {
    when (pack) {
        Pack.HERB -> repeat(Utils.random(HERB_PACK_MIN, HERB_PACK_MAX)) {
            DropTable.calculateDrops(player, DropSets.getDropSet("herb")).forEach { player.inventory.addItemDrop(noted(it.id), it.amount) }
        }
        Pack.MINERAL -> {
            player.inventory.addItemDrop(Obj.cert_coal, Utils.random(MINERAL_PACK_COAL.first, MINERAL_PACK_COAL.last))
            player.inventory.addItemDrop(Obj.cert_iron_ore, Utils.random(MINERAL_PACK_IRON.first, MINERAL_PACK_IRON.last))
        }
        Pack.SEED -> repeat(SEED_PACK_ROLLS) {
            DropTable.calculateDrops(player, DropSets.getDropSet("nest_shit_seed")).forEach { player.inventory.addItemDrop(it) }
        }
    }
}

private fun charmExchange(player: Player, charm: Charm, amount: Int): Exchange {
    val item = Item(charm.obj, amount.coerceAtLeast(1))
    return Exchange(item.amount * CHARM_COST, "${Utils.formatNumber(item.amount)} x ${item.name}", refusal = {
        if (!player.inventory.hasRoomFor(item)) "You don't have enough inventory space." else null
    }) { player.inventory.addItem(item) }
}

private fun noted(obj: Int) = Item(obj).definitions.noteId.takeIf { it > 0 } ?: obj
