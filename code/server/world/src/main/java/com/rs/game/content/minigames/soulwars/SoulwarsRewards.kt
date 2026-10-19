package com.rs.game.content.minigames.soulwars

import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.game.content.ItemConstants
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onButtonClick
import com.rs.plugin.kts.onItemOnNpc
import com.rs.plugin.kts.onNpcClick
import com.rs.utils.DropSets
import com.rs.utils.drop.DropTable
import org.darkan.core.Utils
import org.darkan.core.cache.ItemDefinitions
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varbit

private const val GAMBLE_COST = 2

private enum class Imbue(val obj: Int, val imbued: Int, val zealCost: Int) {
    GOLD_RING(Obj.gold_ring, Obj.mob_gold_ring, 2),
    SAPPHIRE_RING(Obj.sapphire_ring, Obj.mob_sapphire_ring, 5),
    EMERALD_RING(Obj.emerald_ring, Obj.mob_emerald_ring, 6),
    RUBY_RING(Obj.ruby_ring, Obj.mob_ruby_ring, 10),
    DIAMOND_RING(Obj.diamond_ring, Obj.mob_diamond_ring, 15),
    DRAGONSTONE_RING(Obj.dragonstone_ring, Obj.mob_dragonstone_ring, 20),
    LUNAR_RING(Obj.lunar_ring, Obj.mob_lunar_ring, 8),
    RING_OF_CHAROS(Obj.ring_of_charos_unlocked, Obj.mob_ring_of_charos_unlocked, 4),
    ONYX_RING(Obj.onyx_ring, Obj.mob_onyx_ring, 25),
    SEERS_RING(Obj.seer_ring, Obj.mob_seer_ring, 25),
    ARCHERS_RING(Obj.ranger_ring, Obj.mob_ranger_ring, 25),
    WARRIOR_RING(Obj.warrior_ring, Obj.mob_warrior_ring, 25),
    BERSERKER_RING(Obj.berzerker_ring, Obj.mob_berzerker_ring, 25);

    companion object {
        private val byObj = entries.flatMap { listOf(it.obj to it, it.imbued to it) }.toMap()
        fun forObj(obj: Int) = byObj[obj]
    }
}

private enum class XpReward(val component: Int, val skill: Int, val levelDivisor: Int, val xpMultiplier: Int) {
    ATTACK(Component.interface_276.component_276_37, Skills.ATTACK, 600, 525),
    STRENGTH(Component.interface_276.component_276_35, Skills.STRENGTH, 600, 525),
    DEFENCE(Component.interface_276.component_276_34, Skills.DEFENSE, 600, 525),
    CONSTITUTION(Component.interface_276.component_276_33, Skills.HITPOINTS, 600, 525),
    RANGED(Component.interface_276.component_276_36, Skills.RANGE, 600, 480),
    MAGIC(Component.interface_276.component_276_39, Skills.MAGIC, 600, 480),
    PRAYER(Component.interface_276.component_276_38, Skills.PRAYER, 600, 270),
    SLAYER(Component.interface_276.component_276_32, Skills.SLAYER, 349, 45);

    fun xpPerZeal(player: Player): Int {
        val level = player.skills.getLevelForXp(skill)
        return level * level / levelDivisor * xpMultiplier
    }

    companion object {
        private val byComponent = entries.associateBy { it.component }
        fun forComponent(component: Int) = byComponent[component]
    }
}

private enum class CharmReward(val component: Int, val charm: Int, val cost: Int, val amountAtCombat3: Double, val amountAtCombat138: Double) {
    GOLD(Component.interface_276.component_276_27, ItemConstants.CHARM_IDS[0], 4, 6.0, 52.0),
    GREEN(Component.interface_276.component_276_26, ItemConstants.CHARM_IDS[1], 5, 4.0, 34.0),
    CRIMSON(Component.interface_276.component_276_25, ItemConstants.CHARM_IDS[2], 12, 4.0, 34.0),
    BLUE(Component.interface_276.component_276_24, ItemConstants.CHARM_IDS[3], 30, 5.0, 45.0);

    fun amount(player: Player): Int {
        val combat = player.skills.combatLevelWithSummoning.toDouble()
        return (amountAtCombat3 + (amountAtCombat138 - amountAtCombat3) * (combat - 3.0) / (138.0 - 3.0)).toInt()
    }

    companion object {
        private val byComponent = entries.associateBy { it.component }
        fun forComponent(component: Int) = byComponent[component]
    }
}

private enum class PetReward(val component: Int, val requiredObj: Int, val pet: Int, val cost: Int) {
    CREEPING_HAND(Component.interface_276.component_276_6, Obj.poh_trophydrop_crawlinghand, Obj.snp_interface_hand, 5),
    MINITRICE(Component.interface_276.component_276_14, Obj.poh_trophydrop_cockatrice, Obj.snp_interface_cockatrice, 25),
    BABY_BASILISK(Component.interface_276.component_276_10, Obj.poh_trophydrop_basilisk, Obj.snp_interface_basilisk, 40),
    BABY_KURASK(Component.interface_276.component_276_12, Obj.poh_trophydrop_kurask, Obj.snp_interface_kurask, 70),
    ABYSSAL_MINION(Component.interface_276.component_276_16, Obj.poh_trophydrop_abyssaldemon, Obj.snp_interface_abbyd, 85),
    TZREK_JAD(Component.interface_276.component_276_81, Obj.tzhaar_cape_fire, Obj.lore_jad, 100);

    companion object {
        private val byComponent = entries.associateBy { it.component }
        fun forComponent(component: Int) = byComponent[component]
    }
}

@ServerStartupEvent
fun mapSoulWarsRewards() {
    onNpcClick(Npc.npc_8527, Npc.npc_8525, options = arrayOf("Rewards")) { (player) ->
        player.interfaceManager.sendInterface(Interface.interface_276)
        refreshZeal(player)
    }

    onButtonClick(Interface.interface_276) { e ->
        val player = e.player
        when (e.opNum) {
            1 -> describe(player, e.component)
            2, 3, 4 -> buy(player, e.component, e.opNum)
        }
        refreshZeal(player)
    }

    onItemOnNpc(Npc.npc_8527, Npc.npc_8525) { (player, item, npc) ->
        val imbue = Imbue.forObj(item.id)
            ?: return@onItemOnNpc player.startConversation { npc(npc.id, HeadE.CALM_TALK, "I can't imbue that. Bring me a ring to imbue. I can also un-imbue a ring for you if you bring me an imbued ring.") }
        val slot = item.slot
        if (item.id == imbue.imbued) {
            player.startConversation {
                npc(npc.id, HeadE.CALM_TALK, "Are you sure you'd like me to remove your enchantment? You won't be refunded any Zeal for this process.")
                options {
                    opExec("Yes, please restore my ring to its original state.") { swapRing(player, slot, imbue.imbued, imbue.obj) }
                    op("Nevermind.")
                }
            }
            return@onItemOnNpc
        }
        player.startConversation {
            npc(npc.id, HeadE.CALM_TALK, "I can imbue that ring for you for ${imbue.zealCost} Zeal. Is that alright with you?")
            options {
                opExec("Yes, please imbue my ring.") {
                    if (player.soulWarsZeal < imbue.zealCost)
                        return@opExec player.npcDialogue(npc.id, HeadE.CALM_TALK, "You don't have enough Zeal.")
                    if (swapRing(player, slot, imbue.obj, imbue.imbued))
                        player.soulWarsZeal -= imbue.zealCost
                }
                op("Nevermind.")
            }
        }
    }
}

private fun swapRing(player: Player, slot: Int, from: Int, to: Int): Boolean {
    val ring = player.inventory.getItem(slot)?.takeIf { it.id == from } ?: return false
    ring.id = to
    player.inventory.refresh(slot)
    return true
}

private fun refreshZeal(player: Player) = player.vars.setVarBit(Varbit.snp_zeal, player.soulWarsZeal)

private fun describe(player: Player, component: Int) {
    XpReward.forComponent(component)?.let {
        return player.sendMessage("You will receive ${Utils.formatNumber(it.xpPerZeal(player))} ${Skills.SKILL_NAME[it.skill]} experience per Zeal exchanged.")
    }
    CharmReward.forComponent(component)?.let {
        return player.sendMessage("You need ${it.cost} Zeal to buy ${it.amount(player)} ${ItemDefinitions.getDefs(it.charm).name.lowercase()}s.")
    }
    PetReward.forComponent(component)?.let {
        return player.sendMessage("You need a ${ItemDefinitions.getDefs(it.requiredObj).name.lowercase()} and ${it.cost} Zeal to buy a ${ItemDefinitions.getDefs(it.pet).name}.")
    }
    if (component == Component.interface_276.component_276_8)
        player.sendMessage("You need $GAMBLE_COST Zeal to gamble.")
}

private fun buy(player: Player, component: Int, opNum: Int) {
    XpReward.forComponent(component)?.let { return buyXp(player, it, opNum) }
    CharmReward.forComponent(component)?.let { return buyCharms(player, it) }
    PetReward.forComponent(component)?.let { return buyPet(player, it) }
    if (component == Component.interface_276.component_276_8)
        gamble(player)
}

private fun buyXp(player: Player, reward: XpReward, opNum: Int) {
    val zealCost = when (opNum) {
        3 -> 10
        4 -> 100
        else -> 1
    }
    val xpPerZeal = reward.xpPerZeal(player)
    if (xpPerZeal <= 0)
        return player.sendMessage("Your ${Skills.SKILL_NAME[reward.skill]} level is too low to gain experience from Zeal.")
    if (player.soulWarsZeal < zealCost)
        return player.sendMessage("You don't have enough Zeal.")
    player.soulWarsZeal -= zealCost
    player.skills.addXp(reward.skill, xpPerZeal.toDouble() * zealCost)
}

private fun buyCharms(player: Player, reward: CharmReward) {
    if (player.soulWarsZeal < reward.cost)
        return player.sendMessage("You need ${reward.cost} Zeal to buy ${reward.amount(player)} ${ItemDefinitions.getDefs(reward.charm).name.lowercase()}s.")
    player.soulWarsZeal -= reward.cost
    player.inventory.addItemDrop(reward.charm, reward.amount(player))
}

private fun buyPet(player: Player, reward: PetReward) {
    if (player.soulWarsZeal < reward.cost || !player.inventory.containsItem(reward.requiredObj))
        return player.sendMessage("You need a ${ItemDefinitions.getDefs(reward.requiredObj).name.lowercase()} and ${reward.cost} Zeal to buy a ${ItemDefinitions.getDefs(reward.pet).name}.")
    player.soulWarsZeal -= reward.cost
    player.inventory.deleteItem(reward.requiredObj, 1)
    player.inventory.addItemDrop(reward.pet, 1)
}

private fun gamble(player: Player) {
    if (player.soulWarsZeal < GAMBLE_COST)
        return player.sendMessage("You need $GAMBLE_COST Zeal to gamble.")
    player.soulWarsZeal -= GAMBLE_COST
    DropTable.calculateDrops(player, DropSets.getDropSet("sw_gamble")).forEach { player.inventory.addItemDrop(it) }
    if (Utils.random(50) == 0) {
        player.npcDialogue(Npc.snp_imp_post_nom, HeadE.CHEERFUL, "Oi, guv! I'm feeling generous so I've conscripted one of my friends to gather charms for you.")
        player.inventory.addItemDrop(Obj.obj_25350, 1)
    }
}
