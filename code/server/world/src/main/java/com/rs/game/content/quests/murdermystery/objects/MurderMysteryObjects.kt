package com.rs.game.content.quests.murdermystery

import com.rs.engine.dialogue.HeadE.CALM_TALK
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.World
import com.rs.game.map.ChunkManager
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnObject
import com.rs.plugin.kts.onLogin
import com.rs.plugin.kts.onChunkEnter
import com.rs.plugin.kts.onObjectClick
import com.rs.plugin.kts.onPickupItem
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.model.Item
import world.gregs.voidps.type.Tile

const val MM_MURDERER_ANNA = 1
const val MM_MURDERER_BOB = 2
const val MM_MURDERER_CAROL = 3
const val MM_MURDERER_DAVID = 4
const val MM_MURDERER_ELIZABETH = 5
const val MM_MURDERER_FRANK = 6

const val MM_MURDERER_KEY = "murderMysteryMurderer"
const val MM_THREAD_TAKEN_KEY = "murderMysteryThreadTaken"
const val MM_POISON_EVIDENCE_FOUND = "murderMysteryPoisonEvidenceFound"

private val DAGGER_TILE = Tile.of(2746, 3578, 0)
private val PUNGENT_POT_TILE = Tile.of(2747, 3579, 0)

private data class EvidenceBarrel(val loc: Int, val tile: Tile, val itemId: Int, val dustedItemId: Int, val printItemId: Int, val foundText: String, val alreadyText: String)

private val EVIDENCE_BARRELS = listOf(
    EvidenceBarrel(Loc.murderbarrela, Tile.of(2733, 3575, 0), Obj.murdernecklace, Obj.murdernecklacedust, Obj.murderfingerprinta,
        "There's something shiny hidden at the bottom. You take Anna's Silver Necklace.", "I already have Anna's Necklace."),
    EvidenceBarrel(Loc.murderbarrelb, Tile.of(2735, 3579, 0), Obj.murdercup, Obj.murdercupdust, Obj.murderfingerprintb,
        "There's something shiny hidden at the bottom. You take Bob's silver cup.", "I already have Bob's cup."),
    EvidenceBarrel(Loc.murderbarrelc, Tile.of(2733, 3580, 1), Obj.murderbottle, Obj.murderbottledust, Obj.murderfingerprintc,
        "There's something shiny hidden at the bottom. You take Carol's silver bottle.", "I already have Carol's bottle."),
    EvidenceBarrel(Loc.murderbarreld, Tile.of(2733, 3577, 1), Obj.murderbook, Obj.murderbookdust, Obj.murderfingerprintd,
        "There's something shiny hidden at the bottom. You take David's silver book.", "I already have David's book."),
    EvidenceBarrel(Loc.murderbarrele, Tile.of(2747, 3581, 1), Obj.murderneedle, Obj.murderneedledust, Obj.murderfingerprinte,
        "There's something shiny hidden at the bottom. You take Elizabeth's silver needle.", "I already have Elizabeth's Needle."),
    EvidenceBarrel(Loc.murderbarrelf, Tile.of(2747, 3577, 1), Obj.murderpot, Obj.murderpotdust, Obj.murderfingerprintf,
        "There's something shiny hidden at the bottom. You take Frank's silver pot.", "I already have Frank's pot.")
)

private class PoisonArea(val suspect: Int, val ordinaryText: String, val murdererText: String, val innocentText: String)

private val POISON_AREAS = mapOf(
    Tile.of(2730, 3572, 0) to (Loc.murdercompost to PoisonArea(MM_MURDERER_ANNA,
        "It's a heap of compost.",
        "The compost is teeming with maggots. Somebody should really do something about it. It's certainly clear nobody's used poison here.",
        "There is a faint smell of poison behind the smell of the compost.")),
    Tile.of(2730, 3559, 0) to (Loc.murderhive to PoisonArea(MM_MURDERER_BOB,
        "It's a very old beehive.",
        "The beehive buzzes with activity. These bees definitely don't seem poisoned at all.",
        "The hive is empty. There are a few dead bees and a faint smell of poison.")),
    Tile.of(2736, 3573, 0) to (Loc.murderdrain to PoisonArea(MM_MURDERER_CAROL,
        "It's the drains from the kitchen.",
        "The drain is totally blocked. It really stinks. No, it REALLY smells bad. It's certainly clear nobody's cleaned it recently.",
        "The drain seems to have been recently cleaned. You can still smell the faint aroma of poison.")),
    Tile.of(2740, 3574, 1) to (Loc.murderweb to PoisonArea(MM_MURDERER_DAVID,
        "It looks like a spiders' nest of some kind...",
        "There is a spiders' nest here. You estimate there must be at least a few hundred spiders ready to hatch. It's certainly clear nobody's used poison here.",
        "A faint smell of poison and a few dead spiders is all that remains of the spiders nest.")),
    Tile.of(2746, 3573, 0) to (Loc.murdersign to PoisonArea(MM_MURDERER_FRANK,
        "The Sinclair Family Crest is hung up here.",
        "It looks like the Sinclair family crest but it is very dirty. You can barely make it out under all of the grime. It's certainly clear nobody's cleaned it recently.",
        "The Sinclair family crest. It's shiny and freshly polished and has a slight smell of poison."))
)

@ServerStartupEvent
fun mapMurderMysteryObjects() {
    onLogin { (player) -> syncMurderSceneEvidence(player) }
    onChunkEnter { (entity, _) ->
        if (entity is Player && entity.withinDistance(DAGGER_TILE, 32))
            syncMurderSceneEvidence(entity)
    }
    onPickupItem(Obj.murderweapon, tiles = arrayOf(DAGGER_TILE)) { e ->
        val player = e.player
        if (!murderMysteryStarted(player)) {
            e.cancelPickup()
            return@onPickupItem
        }
        if (player.inventory.containsItem(Obj.murderweapon, 1)) {
            e.cancelPickup()
            player.simpleDialogue("I already have the murder weapon.")
            return@onPickupItem
        }
        beginGatheringEvidence(player)
        player.simpleDialogue("This knife doesn't seem sturdy enough to have killed Lord Sinclair.")
    }

    onPickupItem(Obj.murderpot2, tiles = arrayOf(PUNGENT_POT_TILE)) { e ->
        val player = e.player
        if (!murderMysteryStarted(player)) {
            e.cancelPickup()
            return@onPickupItem
        }
        if (player.inventory.containsItem(Obj.murderpot2, 1)) {
            e.cancelPickup()
            player.simpleDialogue("I already have the poisoned pot.")
            return@onPickupItem
        }
        beginGatheringEvidence(player)
        player.simpleDialogue("It seems like Lord Sinclair was drinking from this before he died.")
    }

    onObjectClick(Loc.loc_26123, tiles = arrayOf(Tile.of(2748, 3577, 0))) { (player, _, option) ->
        if (option.equals("Break", ignoreCase = true)) {
            player.simpleDialogue("You don't want to damage evidence!")
            return@onObjectClick
        }
        investigateWindow(player)
    }

    onObjectClick(Loc.murderdoggatel, Loc.murderdoggater, tiles = arrayOf(Tile.of(2750, 3578, 0), Tile.of(2749, 3578, 0))) { (player) ->
        if (!murderMysteryStarted(player)) return@onObjectClick
        player.simpleDialogue("As you approach the gate the guard dog starts barking loudly at you. There is no way an intruder could have committed the murder. It must have been someone the dog knew to get past it quietly.")
    }

    onObjectClick(Loc.murdersacks, tiles = arrayOf(Tile.of(2731, 3582, 0), Tile.of(2732, 3582, 0))) { (player) ->
        if (!murderMysteryStarted(player)) return@onObjectClick
        beginGatheringEvidence(player)
        player.startConversation {
            simple("There's some flypaper in there. Should I take it?")
            options {
                op("Yes, it might be useful.") {
                    exec {
                        player.inventory.addItemDrop(Obj.murderpaper, 1)
                        player.sendMessage("You take a piece of fly paper. There is still plenty of fly paper left.")
                    }
                }
                op("No, I don't see any need for it.") {
                    exec { player.sendMessage("You leave the paper in the sack.") }
                }
            }
        }
    }

    EVIDENCE_BARRELS.forEach { barrel ->
        onObjectClick(barrel.loc, tiles = arrayOf(barrel.tile)) { (player) ->
            if (!murderMysteryStarted(player)) return@onObjectClick
            beginGatheringEvidence(player)
            if (player.inventory.containsItem(barrel.itemId, 1) || player.inventory.containsItem(barrel.dustedItemId, 1) || player.inventory.containsItem(barrel.printItemId, 1)) {
                player.sendMessage(barrel.alreadyText)
            } else {
                player.sendMessage(barrel.foundText)
                player.inventory.addItemDrop(barrel.itemId, 1)
            }
        }
    }

    onObjectClick(Loc.flourbarrel, tiles = arrayOf(Tile.of(2733, 3582, 0))) { (player) ->
        if (murderMysteryStarted(player)) takeFlour(player)
    }

    onItemOnObject(arrayOf(Loc.flourbarrel), arrayOf(Obj.pot_empty, Obj.murderpot2, Obj.murderpot, Obj.murderpotdust), tiles = arrayOf(Tile.of(2733, 3582, 0))) { (player, _, item) ->
        if (!murderMysteryStarted(player)) return@onItemOnObject
        when (item.id) {
            Obj.pot_empty -> takeFlour(player)
            Obj.murderpot2, Obj.murderpot, Obj.murderpotdust -> player.sendMessage("You probably shouldn't use evidence from a crime scene to keep flour in.")
        }
    }

    POISON_AREAS.forEach { (tile, entry) ->
        val (loc, area) = entry
        onObjectClick(loc, tiles = arrayOf(tile)) { (player) -> investigatePoisonArea(player, area) }
    }

    onObjectClick(Loc.murderfountain, tiles = arrayOf(Tile.of(2747, 3563, 0))) { (player) ->
        if (!murderMysteryStarted(player) || !player.getBool(MM_POISON_BUYERS_KNOWN)) {
            player.sendMessage("A fountain with large numbers of insects around the base.")
            return@onObjectClick
        }
        if (!knowsMurderMysteryPoisonClaim(player, MM_MURDERER_ELIZABETH)) {
            player.sendMessage("I should ask Elizabeth what she used the poison for before investigating this.")
            return@onObjectClick
        }
        beginPoisonInvestigation(player)
        if (ensureMurderer(player) == MM_MURDERER_ELIZABETH) {
            player.startConversation {
                simple("The fountain is swarming with mosquitos. There's a nest of them underneath the fountain.")
                player(CALM_TALK, "I hate mosquitos, they're so annoying!")
                simple("It's certainly clear nobody's used poison here.", extraFunctionality = Runnable {
                    recordPoisonEvidence(player, MM_MURDERER_ELIZABETH)
                })
            }
        } else {
            player.simpleDialogue("There are a lot of dead mosquitos around the base of the fountain. A faint smell of poison is in the air, but the water seems clean.")
        }
    }
}

private fun investigateWindow(player: Player) {
    if (!murderMysteryStarted(player)) {
        player.simpleDialogue("You need the guards' permission to do that.")
        return
    }
    beginGatheringEvidence(player)
    val thread = criminalThreadFor(player)
    player.sendMessage("Some thread seems to have been caught on a loose nail on the window.")
    if (player.inventory.containsItem(thread, 1)) {
        player.simpleDialogue("You have already taken the thread.")
        return
    }
    if (player.getBool(MM_THREAD_TAKEN_KEY)) {
        player.simpleDialogue("Lucky for you there's some thread left. You should be less careless in future.")
        player.inventory.addItemDrop(thread, 1)
        refreshMurderMysteryProgress(player)
        return
    }
    player.set(MM_THREAD_TAKEN_KEY, true)
    player.inventory.addItemDrop(thread, 1)
    refreshMurderMysteryProgress(player)
    player.simpleDialogue("You take the thread.")
}

private fun takeFlour(player: Player) {
    beginGatheringEvidence(player)
    if (!player.inventory.containsItem(Obj.pot_empty, 1)) {
        player.sendMessage("You need something to put the flour in.")
        return
    }
    player.inventory.deleteItem(Obj.pot_empty, 1)
    player.inventory.addItemDrop(Obj.pot_flour, 1)
    player.sendMessage("You take some flour from the barrel. There's still plenty of flour left.")
}

fun syncMurderSceneEvidence(player: Player) {
    if (!murderMysteryStarted(player)) return
    if (!player.inventory.containsItem(Obj.murderweapon, 1) &&
        !player.inventory.containsItem(Obj.murderweapondust, 1) &&
        !player.inventory.containsItem(Obj.murderfingerprint1, 1) &&
        !player.inventory.containsItem(Obj.murderfingerprint, 1))
        spawnMurderSceneEvidence(player, Obj.murderweapon, DAGGER_TILE)
    if (!player.inventory.containsItem(Obj.murderpot2, 1))
        spawnMurderSceneEvidence(player, Obj.murderpot2, PUNGENT_POT_TILE)
}

private fun spawnMurderSceneEvidence(player: Player, itemId: Int, tile: Tile) {
    val chunk = ChunkManager.getChunk(tile.zone.id)
    if (chunk.getGroundItem(itemId, tile, player.uuid) == null)
        World.addGroundItemNoExpire(Item(itemId, 1), tile, player)
}

fun removeMurderSceneEvidence(player: Player) {
    listOf(Obj.murderweapon to DAGGER_TILE, Obj.murderpot2 to PUNGENT_POT_TILE).forEach { (itemId, tile) ->
        ChunkManager.getChunk(tile.zone.id).getGroundItem(itemId, tile, player.uuid)?.let {
            World.removeGroundItem(it)
        }
    }
}

fun murderMysteryStarted(player: Player): Boolean = player.getQuestStage(Quest.MURDER_MYSTERY) in STAGE_SOMETHINGS_AMISS until STAGE_COMPLETE

private fun beginGatheringEvidence(player: Player) {
    val stage = player.getQuestStage(Quest.MURDER_MYSTERY)
    if (stage == STAGE_SOMETHINGS_AMISS || stage == STAGE_QUESTIONING_INHABITANTS)
        player.setQuestStage(Quest.MURDER_MYSTERY, STAGE_GATHERING_EVIDENCE)
}

fun ensureMurderer(player: Player): Int {
    val existing = player.getI(MM_MURDERER_KEY, 0)
    if (existing in MM_MURDERER_ANNA..MM_MURDERER_FRANK) return existing
    val murderer = (MM_MURDERER_ANNA..MM_MURDERER_FRANK).random()
    player.set(MM_MURDERER_KEY, murderer)
    return murderer
}

fun criminalThreadFor(player: Player): Int = when (ensureMurderer(player)) {
    MM_MURDERER_ANNA, MM_MURDERER_DAVID -> Obj.murderthreadg
    MM_MURDERER_BOB, MM_MURDERER_CAROL -> Obj.murderthreadr
    MM_MURDERER_ELIZABETH, MM_MURDERER_FRANK -> Obj.murderthreadb
    else -> error("Invalid Murder Mystery murderer state.")
}

private fun investigatePoisonArea(player: Player, area: PoisonArea) {
    if (!murderMysteryStarted(player) || !player.getBool(MM_POISON_BUYERS_KNOWN)) {
        player.sendMessage(area.ordinaryText)
        return
    }
    if (!knowsMurderMysteryPoisonClaim(player, area.suspect)) {
        player.sendMessage("I should ask the suspect what they used the poison for before investigating this.")
        return
    }
    beginPoisonInvestigation(player)
    if (ensureMurderer(player) == area.suspect) {
        player.simpleDialogue(area.murdererText)
        recordPoisonEvidence(player, area.suspect)
    } else {
        player.simpleDialogue(area.innocentText)
    }
}

private fun beginPoisonInvestigation(player: Player) {
    if (player.getQuestStage(Quest.MURDER_MYSTERY) == STAGE_VISITING_POISON_SALESMAN)
        player.setQuestStage(Quest.MURDER_MYSTERY, STAGE_INVESTIGATING_POISONED_AREAS)
}

private fun recordPoisonEvidence(player: Player, suspect: Int) {
    if (!murderMysteryStarted(player) || !knowsMurderMysteryPoisonClaim(player, suspect)) return
    if (ensureMurderer(player) == suspect) {
        player.set(MM_POISON_EVIDENCE_FOUND, true)
        refreshMurderMysteryProgress(player)
    }
}
