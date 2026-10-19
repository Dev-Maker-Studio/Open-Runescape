package com.rs.game.content.world.areas.runecrafting_guild

import com.rs.engine.dialogue.HeadE.CALM_TALK
import com.rs.engine.dialogue.HeadE.HAPPY_TALKING
import com.rs.engine.dialogue.startConversation
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick
import org.darkan.core.Utils
import org.darkan.core.cache.ItemDefinitions
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj

private enum class KorvakPouch(val pouch: Int, val degraded: Int, val repairCost: Int, val price: Int) {
    MEDIUM(Obj.rcu_pouch_medium, Obj.rcu_pouch_medium_degrade, 0, 0),
    LARGE(Obj.rcu_pouch_large, Obj.rcu_pouch_large_degrade, 9000, 25000),
    GIANT(Obj.rcu_pouch_giant, Obj.rcu_pouch_giant_degrade, 12000, 50000);

    val displayName get() = name.lowercase().replaceFirstChar(Char::uppercase) + " pouch"
}

private class OmniBinding(val ingredient: Int, val product: Int)

private val BINDINGS = listOf(OmniBinding(Obj.tiara, Obj.tiara_omni), OmniBinding(Obj.rcguild_staff_blank, Obj.rcguild_staff_omni))

@ServerStartupEvent
fun mapWizardKorvak() {
    onNpcClick(Npc.rcguild_wizard_korvac) { (player) -> talkToKorvak(player) }
}

private fun talkToKorvak(player: Player) = player.startConversation {
    val korvak = Npc.rcguild_wizard_korvac
    npc(korvak, HAPPY_TALKING, "Pouches! Talismans! What can I do for you?")
    options {
        opExec("Can you repair my pouches?") { repairPouches(player) }
        op("I'd like to buy a pouch.") {
            options {
                for (pouch in KorvakPouch.entries)
                    opExec(if (pouch.price == 0) "${pouch.displayName} (free replacement)" else "${pouch.displayName} (${Utils.formatNumber(pouch.price)} coins)") { buyPouch(player, pouch) }
                op("Never mind.")
            }
        }
        opExec("Can you bind my omni-talisman?") { bindOmniTalisman(player) }
        op("Never mind.")
    }
}

private fun repairPouches(player: Player) {
    val degraded = KorvakPouch.entries.filter { player.inventory.containsItem(it.degraded) }
    val cost = degraded.sumOf { it.repairCost }
    val korvak = Npc.rcguild_wizard_korvac
    player.startConversation {
        when {
            degraded.isEmpty() -> npc(korvak, CALM_TALK, "Your pouches look fine to me. Bring me any that start to fall apart.")
            !player.inventory.hasCoins(cost) -> npc(korvak, CALM_TALK, "Repairing those will cost ${Utils.formatNumber(cost)} coins, which you don't have with you.")
            else -> {
                npc(korvak, CALM_TALK, if (cost == 0) "There, good as new, and free of charge." else "There, good as new. That will be ${Utils.formatNumber(cost)} coins.")
                exec {
                    if (!player.inventory.hasCoins(cost) || degraded.any { !player.inventory.containsItem(it.degraded) }) return@exec
                    if (cost > 0) player.inventory.removeCoins(cost)
                    degraded.forEach {
                        player.inventory.deleteItem(it.degraded, 1)
                        player.inventory.addItem(it.pouch, 1)
                    }
                }
            }
        }
    }
}

private fun buyPouch(player: Player, pouch: KorvakPouch) {
    val korvak = Npc.rcguild_wizard_korvac
    player.startConversation {
        when {
            player.owns(pouch.pouch) || player.owns(pouch.degraded) -> npc(korvak, CALM_TALK, "You already have a ${pouch.displayName.lowercase()}. One is quite enough.")
            !player.inventory.hasFreeSlots() -> npc(korvak, CALM_TALK, "You don't have room to carry another pouch.")
            !player.inventory.hasCoins(pouch.price) -> npc(korvak, CALM_TALK, "A ${pouch.displayName.lowercase()} costs ${Utils.formatNumber(pouch.price)} coins, which you don't have with you.")
            else -> item(pouch.pouch, "Wizard Korvak hands you a ${pouch.displayName.lowercase()}.") {
                if (!player.inventory.hasCoins(pouch.price) || !player.inventory.hasFreeSlots()) return@item
                if (pouch.price > 0) player.inventory.removeCoins(pouch.price)
                player.inventory.addItem(pouch.pouch, 1)
            }
        }
    }
}

private fun bindOmniTalisman(player: Player) {
    val korvak = Npc.rcguild_wizard_korvac
    val binding = BINDINGS.firstOrNull { player.inventory.containsItem(it.ingredient) }
    player.startConversation {
        when {
            !player.inventory.containsItem(Obj.omni_talisman) -> npc(korvak, CALM_TALK, "Bring me an omni-talisman from Wizard Elriss, along with a tiara or a blank Runecrafting staff, and I'll bind them for you.")
            binding == null -> npc(korvak, CALM_TALK, "I need a plain tiara or a blank Runecrafting staff to bind that omni-talisman to.")
            else -> item(binding.product, "Wizard Korvak binds the omni-talisman to your ${ItemDefinitions.getDefs(binding.ingredient).name.lowercase()}.") {
                if (!player.inventory.containsItem(Obj.omni_talisman) || !player.inventory.containsItem(binding.ingredient)) return@item
                player.inventory.deleteItem(Obj.omni_talisman, 1)
                player.inventory.deleteItem(binding.ingredient, 1)
                player.inventory.addItem(binding.product, 1)
            }
        }
    }
}
