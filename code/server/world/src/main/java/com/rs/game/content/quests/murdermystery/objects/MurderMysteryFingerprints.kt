package com.rs.game.content.quests.murdermystery

import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnItem
import org.darkan.core.gameval.gen.Obj

private class DustableEvidence(val item: Int, val dusted: Int, val print: Int, val dustMessages: Array<String>, val liftMessages: Array<String>)

private val DUSTABLE_EVIDENCE = listOf(
    DustableEvidence(Obj.murderweapon, Obj.murderweapondust, Obj.murderfingerprint1,
        arrayOf("You sprinkle a small amount of flour on the murder weapon.", "The murder weapon is now coated with a thin layer of flour."),
        arrayOf("You use the flypaper on the floury dagger.", "You have a clean impression of the murderer's finger prints.")),
    DustableEvidence(Obj.murdernecklace, Obj.murdernecklacedust, Obj.murderfingerprinta,
        arrayOf("You sprinkle the flour on Anna's necklace.", "The necklace is now coated with a thin layer of flour."),
        arrayOf("You use the flypaper on the flour covered necklace.", "You have a clean impression of Anna's finger prints.")),
    DustableEvidence(Obj.murdercup, Obj.murdercupdust, Obj.murderfingerprintb,
        arrayOf("You sprinkle the flour on Bob's cup.", "The cup is now coated with a thin layer of flour."),
        arrayOf("You use the flypaper on the flour covered cup.", "You have a clean impression of Bob's finger prints.")),
    DustableEvidence(Obj.murderbottle, Obj.murderbottledust, Obj.murderfingerprintc,
        arrayOf("You sprinkle the flour on Carol's bottle.", "The bottle is now coated with a thin layer of flour."),
        arrayOf("You use the flypaper on the flour covered bottle.", "You have a clean impression of Carol's finger prints.")),
    DustableEvidence(Obj.murderbook, Obj.murderbookdust, Obj.murderfingerprintd,
        arrayOf("You sprinkle the flour on David's book.", "The Book is now coated with a thin layer of flour."),
        arrayOf("You use the flypaper on the flour covered book.", "You have a clean impression of David's finger prints.")),
    DustableEvidence(Obj.murderneedle, Obj.murderneedledust, Obj.murderfingerprinte,
        arrayOf("You sprinkle the flour on Elizabeth's needle.", "The Needle is now coated with a thin layer of flour."),
        arrayOf("You use the flypaper on the flour covered needle.", "You have a clean impression of Elizabeth's finger prints.")),
    DustableEvidence(Obj.murderpot, Obj.murderpotdust, Obj.murderfingerprintf,
        arrayOf("You sprinkle the flour on Frank's pot", "The Pot is now coated with a thin layer of flour."),
        arrayOf("You use the flypaper on the flour covered pot.", "You have a clean impression of Frank's finger prints."))
)

private class SuspectPrint(val print: Int, val murderer: Int, val name: String)

private val SUSPECT_PRINTS = listOf(
    SuspectPrint(Obj.murderfingerprinta, MM_MURDERER_ANNA, "Anna"),
    SuspectPrint(Obj.murderfingerprintb, MM_MURDERER_BOB, "Bob"),
    SuspectPrint(Obj.murderfingerprintc, MM_MURDERER_CAROL, "Carol"),
    SuspectPrint(Obj.murderfingerprintd, MM_MURDERER_DAVID, "David"),
    SuspectPrint(Obj.murderfingerprinte, MM_MURDERER_ELIZABETH, "Elizabeth"),
    SuspectPrint(Obj.murderfingerprintf, MM_MURDERER_FRANK, "Frank")
)

@ServerStartupEvent
fun mapMurderMysteryFingerprints() {
    onItemOnItem(intArrayOf(Obj.pot_flour), (DUSTABLE_EVIDENCE.map { it.item } + Obj.murderpot2).toIntArray()) { e ->
        val player = e.player
        if (!murderMysteryStarted(player)) return@onItemOnItem
        if (e.usedWith(Obj.pot_flour, Obj.murderpot2)) {
            player.inventory.deleteItem(Obj.pot_flour, 1)
            player.inventory.addItem(Obj.pot_empty, 1)
            player.sendMessage("You sprinkle a small amount of flour on the strange smelling pot.")
            player.sendMessage("The surface isn't shiny enough to take a fingerprint from.")
            return@onItemOnItem
        }
        val evidence = DUSTABLE_EVIDENCE.firstOrNull { e.usedWith(Obj.pot_flour, it.item) } ?: return@onItemOnItem
        player.inventory.deleteItem(evidence.item, 1)
        player.inventory.deleteItem(Obj.pot_flour, 1)
        player.inventory.addItem(evidence.dusted, 1)
        player.inventory.addItem(Obj.pot_empty, 1)
        evidence.dustMessages.forEach(player::sendMessage)
    }

    onItemOnItem(intArrayOf(Obj.murderpaper), DUSTABLE_EVIDENCE.map { it.dusted }.toIntArray()) { e ->
        val player = e.player
        if (!murderMysteryStarted(player)) return@onItemOnItem
        val evidence = DUSTABLE_EVIDENCE.firstOrNull { e.usedWith(Obj.murderpaper, it.dusted) } ?: return@onItemOnItem
        player.inventory.deleteItem(evidence.dusted, 1)
        player.inventory.deleteItem(Obj.murderpaper, 1)
        player.inventory.addItem(evidence.item, 1)
        player.inventory.addItem(evidence.print, 1)
        evidence.liftMessages.forEach(player::sendMessage)
    }

    onItemOnItem(intArrayOf(Obj.murderfingerprint1), SUSPECT_PRINTS.map { it.print }.toIntArray()) { e ->
        val player = e.player
        if (!murderMysteryStarted(player)) return@onItemOnItem
        val suspect = SUSPECT_PRINTS.firstOrNull { e.usedWith(Obj.murderfingerprint1, it.print) } ?: return@onItemOnItem
        if (ensureMurderer(player) == suspect.murderer) {
            player.sendMessage("The finger prints are an exact match to ${suspect.name}'s.")
            player.inventory.deleteItem(Obj.murderfingerprint1, 1)
            player.inventory.addItem(Obj.murderfingerprint, 1)
            refreshMurderMysteryProgress(player)
        } else {
            player.sendMessage("They don't seem to be the same. I guess that clears ${suspect.name} of the crime. You destroy the useless fingerprint.")
            player.inventory.deleteItem(suspect.print, 1)
        }
    }
}
