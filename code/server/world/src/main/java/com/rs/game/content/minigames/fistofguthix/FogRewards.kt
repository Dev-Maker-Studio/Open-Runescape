package com.rs.game.content.minigames.fistofguthix

import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.game.content.items.DegradeStages
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onButtonClick
import com.rs.utils.ItemConfig
import org.darkan.core.Utils
import org.darkan.core.cache.ItemDefinitions
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.model.Item

private val BUY_AMOUNTS = mapOf(2 to 1, 3 to 5, 4 to 10)
private const val OP_VALUE = 1
private const val OP_INFORMATION = 5

enum class FogReward(val component: Int, val obj: Int, val cost: Int, val recharge: Int, val stages: IntArray = IntArray(0), val broken: Int = -1, val breaksOnDeath: Boolean = false) {
    DRUIDIC_TOP(Component.interface_732.component_732_179, Obj.obj_12894, 300, 30, intArrayOf(Obj.hvh_guthix_body_80, Obj.hvh_guthix_body_60, Obj.hvh_guthix_body_40, Obj.hvh_guthix_body_20), Obj.hvh_guthix_body_broken),
    DRUIDIC_BOTTOM(Component.interface_732.component_732_180, Obj.obj_12901, 200, 20, intArrayOf(Obj.hvh_guthix_legs_80, Obj.hvh_guthix_legs_60, Obj.hvh_guthix_legs_40, Obj.hvh_guthix_legs_20), Obj.hvh_guthix_legs_broken),
    DRUIDIC_HOOD(Component.interface_732.component_732_181, Obj.obj_12887, 100, 10, intArrayOf(Obj.hvh_guthix_head_80, Obj.hvh_guthix_head_60, Obj.hvh_guthix_head_40, Obj.hvh_guthix_head_20), Obj.hvh_guthix_head_broken),
    COMBAT_TOP(Component.interface_732.component_732_182, Obj.obj_12971, 150, 15, intArrayOf(Obj.hvh_combat_body_80, Obj.hvh_combat_body_60, Obj.hvh_combat_body_40, Obj.hvh_combat_body_20), Obj.hvh_combat_body_broken),
    COMBAT_BOTTOM(Component.interface_732.component_732_183, Obj.obj_12978, 100, 10, intArrayOf(Obj.hvh_combat_legs_80, Obj.hvh_combat_legs_60, Obj.hvh_combat_legs_40, Obj.hvh_combat_legs_20), Obj.hvh_combat_legs_broken),
    COMBAT_HOOD(Component.interface_732.component_732_184, Obj.obj_12964, 50, 5, intArrayOf(Obj.hvh_combat_head_80, Obj.hvh_combat_head_60, Obj.hvh_combat_head_40, Obj.hvh_combat_head_20), Obj.hvh_combat_head_broken),
    BATTLE_TOP(Component.interface_732.component_732_185, Obj.obj_12873, 1500, 150, intArrayOf(Obj.hvh_ench_combat_body_80, Obj.hvh_ench_combat_body_60, Obj.hvh_ench_combat_body_40, Obj.hvh_ench_combat_body_20), Obj.hvh_ench_combat_body_broken),
    BATTLE_BOTTOM(Component.interface_732.component_732_186, Obj.obj_12880, 1000, 100, intArrayOf(Obj.hvh_ench_combat_legs_80, Obj.hvh_ench_combat_legs_60, Obj.hvh_ench_combat_legs_40, Obj.hvh_ench_combat_legs_20), Obj.hvh_ench_combat_legs_broken),
    BATTLE_HOOD(Component.interface_732.component_732_187, Obj.obj_12866, 250, 25, intArrayOf(Obj.hvh_ench_combat_head_80, Obj.hvh_ench_combat_head_60, Obj.hvh_ench_combat_head_40, Obj.hvh_ench_combat_head_20), Obj.hvh_ench_combat_head_broken),
    GREEN_COIF(Component.interface_732.component_732_188, Obj.obj_12936, 150, 15, intArrayOf(Obj.obj_12937, Obj.obj_12938, Obj.obj_12939, Obj.obj_12940), Obj.obj_12941),
    BLUE_COIF(Component.interface_732.component_732_189, Obj.obj_12943, 200, 20, intArrayOf(Obj.obj_12944, Obj.obj_12945, Obj.obj_12946, Obj.obj_12947), Obj.obj_12948),
    RED_COIF(Component.interface_732.component_732_190, Obj.obj_12950, 300, 30, intArrayOf(Obj.obj_12951, Obj.obj_12952, Obj.obj_12953, Obj.obj_12954), Obj.obj_12955),
    BLACK_COIF(Component.interface_732.component_732_191, Obj.obj_12957, 500, 50, intArrayOf(Obj.obj_12958, Obj.obj_12959, Obj.obj_12960, Obj.obj_12961), Obj.obj_12962),
    ROYAL_COIF(Component.interface_732.component_732_192, Obj.obj_24388, 650, 65, intArrayOf(Obj.obj_24389, Obj.obj_24390, Obj.obj_24391, Obj.obj_24392), Obj.obj_24393),
    BRONZE_GAUNTLETS(Component.interface_732.component_732_193, Obj.hvh_bronze_gauntlets, 15, 3, broken = Obj.hvh_bronze_gauntlets_broken, breaksOnDeath = true),
    IRON_GAUNTLETS(Component.interface_732.component_732_194, Obj.hvh_iron_gauntlets, 30, 5, broken = Obj.hvh_iron_gauntlets_broken, breaksOnDeath = true),
    STEEL_GAUNTLETS(Component.interface_732.component_732_195, Obj.hvh_steel_gauntlets, 50, 8, broken = Obj.hvh_steel_gauntlets_broken, breaksOnDeath = true),
    BLACK_GAUNTLETS(Component.interface_732.component_732_196, Obj.hvh_black_gauntlets, 75, 13, broken = Obj.hvh_black_gauntlets_broken, breaksOnDeath = true),
    MITHRIL_GAUNTLETS(Component.interface_732.component_732_1, Obj.hvh_mithril_gauntlets, 100, 17, broken = Obj.hvh_mithril_gauntlets_broken, breaksOnDeath = true),
    ADAMANT_GAUNTLETS(Component.interface_732.component_732_197, Obj.hvh_adamant_gauntlets, 150, 25, broken = Obj.hvh_adamant_gauntlets_broken, breaksOnDeath = true),
    RUNE_GAUNTLETS(Component.interface_732.component_732_198, Obj.hvh_rune_gauntlets, 200, 35, broken = Obj.hvh_rune_gauntlets_broken, breaksOnDeath = true),
    DRAGON_GAUNTLETS(Component.interface_732.component_732_199, Obj.hvh_dragon_gauntlets, 300, 50, broken = Obj.hvh_dragon_gauntlets_broken, breaksOnDeath = true),
    ADAMANT_SPIKESHIELD(Component.interface_732.component_732_200, Obj.obj_12908, 50, 5, intArrayOf(Obj.hvh_spiked_adam_sq_shield_80, Obj.hvh_spiked_adam_sq_shield_60, Obj.hvh_spiked_adam_sq_shield_40, Obj.hvh_spiked_adam_sq_shield_20), Obj.hvh_spiked_adam_sq_shield_broken),
    ADAMANT_BERSERKER(Component.interface_732.component_732_201, Obj.obj_12915, 100, 10, intArrayOf(Obj.hvh_spiked_adam_kite_shield_80, Obj.hvh_spiked_adam_kite_shield_60, Obj.hvh_spiked_adam_kite_shield_40, Obj.hvh_spiked_adam_kite_shield_20), Obj.hvh_spiked_adam_kite_shield_broken),
    RUNE_SPIKESHIELD(Component.interface_732.component_732_202, Obj.obj_12922, 200, 20, intArrayOf(Obj.hvh_spiked_rune_sq_shield_80, Obj.hvh_spiked_rune_sq_shield_60, Obj.hvh_spiked_rune_sq_shield_40, Obj.hvh_spiked_rune_sq_shield_20), Obj.hvh_spiked_rune_sq_shield_broken),
    RUNE_BERSERKER(Component.interface_732.component_732_203, Obj.obj_12929, 300, 30, intArrayOf(Obj.hvh_spiked_rune_kite_shield_80, Obj.hvh_spiked_rune_kite_shield_60, Obj.hvh_spiked_rune_kite_shield_40, Obj.hvh_spiked_rune_kite_shield_20), Obj.hvh_spiked_rune_kite_shield_broken),
    IRIT_GLOVES(Component.interface_732.component_732_204, Obj.hvh_irit_gloves, 75, 0),
    AVANTOE_GLOVES(Component.interface_732.component_732_0, Obj.hvh_avantoe_gloves, 100, 0),
    KWUARM_GLOVES(Component.interface_732.component_732_205, Obj.hvh_kwuarm_gloves, 200, 0),
    CADANTINE_GLOVES(Component.interface_732.component_732_206, Obj.hvh_cadantine_gloves, 200, 0),
    SWORDFISH_GLOVES(Component.interface_732.component_732_207, Obj.hvh_swordfish_gloves, 200, 0),
    SHARK_GLOVES(Component.interface_732.component_732_208, Obj.hvh_shark_gloves, 200, 0),
    DRAGON_SLAYER_GLOVES(Component.interface_732.component_732_209, Obj.hvh_dragon_slayer_gloves, 200, 0),
    AIR_GLOVES(Component.interface_732.component_732_210, Obj.hvh_air_gloves, 75, 0),
    WATER_GLOVES(Component.interface_732.component_732_211, Obj.hvh_water_gloves, 75, 0),
    EARTH_GLOVES(Component.interface_732.component_732_212, Obj.hvh_earth_gloves, 75, 0);

    val chargedIds: IntArray get() = intArrayOf(obj, *stages) + if (broken != -1) intArrayOf(broken) else IntArray(0)

    companion object {
        private val byComponent = entries.associateBy { it.component }
        private val byCharged = entries.flatMap { reward -> reward.chargedIds.map { it to reward } }.toMap()
        fun forComponent(component: Int) = byComponent[component]
        fun forCharged(id: Int) = byCharged[id]
    }
}

object FogRewards {
    fun open(player: Player) {
        player.interfaceManager.sendInterface(Interface.interface_732)
        player.sendMessage("You have ${Utils.formatNumber(player.inventory.getNumberOf(Obj.hvh_token))} Fist of Guthix tokens.")
    }

    fun buy(player: Player, reward: FogReward, amount: Int) {
        val name = ItemDefinitions.getDefs(reward.obj).name
        val affordable = (player.inventory.getNumberOf(Obj.hvh_token) / reward.cost).coerceAtMost(amount)
        val space = player.inventory.freeSlots
        val bought = affordable.coerceAtMost(space)
        when {
            affordable == 0 -> player.sendMessage("You need ${reward.cost} tokens to buy $name.")
            bought == 0 -> player.sendMessage("You don't have enough inventory space.")
            else -> {
                player.inventory.deleteItem(Obj.hvh_token, bought * reward.cost)
                player.inventory.addItem(reward.obj, bought)
                player.sendMessage("You buy ${if (bought == 1) Utils.addArticle(name.lowercase()) else "$bought ${name.lowercase()}"} for ${Utils.formatNumber(bought * reward.cost)} tokens.")
            }
        }
    }

    private fun needsRecharge(item: Item): FogReward? {
        val reward = FogReward.forCharged(item.id) ?: return null
        if (reward.recharge <= 0) return null
        return reward.takeIf { item.id != reward.obj || item.getMetaData() != null }
    }

    fun offerRecharge(player: Player, npcId: Int) {
        val worn = player.inventory.items.array().filterNotNull().mapNotNull { item -> needsRecharge(item)?.let { item to it } }
        val cost = worn.sumOf { it.second.recharge }
        player.startConversation {
            when {
                worn.isEmpty() -> npc(npcId, HeadE.CALM_TALK, "You don't have anything of mine that needs recharging. Bring the items in your pack, not on your body.")
                player.inventory.getNumberOf(Obj.hvh_token) < cost -> npc(npcId, HeadE.CALM_TALK, "Recharging those would cost ${Utils.formatNumber(cost)} tokens, and you don't have enough.")
                else -> {
                    npc(npcId, HeadE.CALM_TALK, "Recharging ${worn.size} item${if (worn.size == 1) "" else "s"} will cost ${Utils.formatNumber(cost)} tokens.")
                    options {
                        opExec("Recharge them.") { recharge(player) }
                        op("Not now.")
                    }
                }
            }
        }
    }

    private fun recharge(player: Player) {
        for (slot in 0 until player.inventory.items.size) {
            val item = player.inventory.getItem(slot) ?: continue
            val reward = needsRecharge(item) ?: continue
            if (player.inventory.getNumberOf(Obj.hvh_token) < reward.recharge) return player.sendMessage("You run out of tokens.")
            player.inventory.deleteItem(Obj.hvh_token, reward.recharge)
            player.inventory.items.set(slot, Item(reward.obj, item.amount))
            player.inventory.refresh(slot)
        }
        player.sendMessage("Reggie recharges your items.")
    }

    private fun inform(player: Player, reward: FogReward) {
        val name = ItemDefinitions.getDefs(reward.obj).name
        val recharge = if (reward.recharge > 0) " and ${reward.recharge} to recharge" else ""
        player.sendMessage("$name costs ${reward.cost} tokens$recharge. ${ItemConfig.get(reward.obj).getExamine(Item(reward.obj))}")
    }

    fun registerDegradeStages() {
        for (reward in FogReward.entries) {
            if (reward.stages.isNotEmpty()) DegradeStages.register(reward.obj, reward.stages, reward.broken)
            if (reward.breaksOnDeath) DegradeStages.registerDeathBreak(reward.obj, reward.broken)
        }
    }

    fun click(player: Player, component: Int, opNum: Int) {
        val reward = FogReward.forComponent(component) ?: return
        when (opNum) {
            OP_VALUE -> player.sendMessage("${ItemDefinitions.getDefs(reward.obj).name} costs ${reward.cost} Fist of Guthix tokens.")
            OP_INFORMATION -> inform(player, reward)
            else -> BUY_AMOUNTS[opNum]?.let { buy(player, reward, it) }
        }
    }
}

@ServerStartupEvent
fun mapFogRewards() {
    FogRewards.registerDegradeStages()
    onButtonClick(Interface.interface_732) { e -> FogRewards.click(e.player, e.component, e.opNum) }
}
