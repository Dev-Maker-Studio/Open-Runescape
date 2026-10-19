package com.rs.game.content.quests.rovingelves

import com.rs.engine.dialogue.HeadE.CALM_TALK
import com.rs.engine.dialogue.startConversation
import com.rs.game.content.ItemConstants.ItemDegrade
import com.rs.game.content.items.DEGRADE_CHARGES_KEY
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.model.Item

private val CRYSTAL_BOWS = setOf(Obj.crystal_bow_new, Obj.crystal_bow_deg)
private val CRYSTAL_SHIELDS = setOf(Obj.crystal_shield_new, Obj.crystal_shield_deg)
private val BOW_PRICES = listOf(900000, 720000, 540000, 360000, 180000)
private val SHIELD_PRICES = listOf(750000, 600000, 450000, 300000, 150000)
private val CHARGED_TELEPORT_SEEDS = setOf(Obj.obj_6099, Obj.obj_6100, Obj.obj_6101, Obj.obj_6102)

internal fun Player.chantCrystalItem(npc: NPC, item: Item) {
    if (rovingStage != COMPLETE || inventory.items[item.slot] !== item) return
    when (item.id) {
        Obj.crystal_seed_old -> startConversation {
            options("What would you like?") {
                op("A crystal bow.") { exec { offerCrystalChant(npc, item, true) } }
                op("A crystal shield.") { exec { offerCrystalChant(npc, item, false) } }
                op("Nothing, thanks.") {}
            }
        }
        in CHARGED_TELEPORT_SEEDS -> startConversation { npc(npc, CALM_TALK, "Oh, that teleport crystal is still enchanted.") }
        in CRYSTAL_BOWS -> offerCrystalChant(npc, item, true)
        in CRYSTAL_SHIELDS -> offerCrystalChant(npc, item, false)
    }
}

private fun Player.offerCrystalChant(npc: NPC, item: Item, bow: Boolean) {
    if (rovingStage != COMPLETE || inventory.items[item.slot] !== item) return
    val seed = item.id == Obj.crystal_seed_old
    if (!seed && !roving.getB("ilfeenEnchanted")) {
        startConversation { npc(npc, CALM_TALK, "I am, but you’ll need your own seed. After I've made you a new bow or shield once, I'll be able to chant your bows and shields back to full charges.") }
        return
    }
    val degrade = if (bow) ItemDegrade.CRYSTAL_BOW_NEW else ItemDegrade.CRYSTAL_SHIELD_NEW
    val max = degrade.defaultCharges
    val charges = if (seed) 0 else item.getMetaDataI(DEGRADE_CHARGES_KEY, max).coerceIn(0, max)
    if (charges == max) { simpleDialogue("This item is fully charged."); return }
    val repairCount = crystalSeedRepairs
    val price = (if (bow) BOW_PRICES else SHIELD_PRICES)[repairCount.coerceIn(0, 4)]
    val cost = ((price.toLong() * (max - charges) + max - 1) / max).toInt()
    val originalId = item.id
    startConversation {
        npc(npc, CALM_TALK, "I can enchant your ${if (seed) "crystal seed" else "crystal item"} for ${String.format("%,d", cost)} coins.")
        options {
            op("Yes, please.") { exec {
                if (rovingStage != COMPLETE || inventory.items[item.slot] !== item || item.id != originalId) return@exec
                if (crystalSeedRepairs != repairCount || (!seed && item.getMetaDataI(DEGRADE_CHARGES_KEY, charges) != charges)) {
                    offerCrystalChant(npc, item, bow)
                    return@exec
                }
                if (!inventory.hasCoins(cost)) {
                    startConversation { npc(npc, CALM_TALK, "You don't have enough coins to cover that cost, sorry.") }
                    return@exec
                }
                inventory.removeCoins(cost)
                item.changeId(if (bow) Obj.crystal_bow_deg else Obj.crystal_shield_deg)
                item.addMetaData(DEGRADE_CHARGES_KEY, max)
                inventory.refresh(item.slot)
                incrementCrystalSeedRepair()
                roving.setB("ilfeenEnchanted", true)
                sendMessage("The item is recharged.")
            } }
            op("No, thank you.") {}
        }
    }
}
