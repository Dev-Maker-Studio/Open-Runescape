package com.rs.game.content.quests.hazeel_cult.npcs

import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.World
import com.rs.game.content.quests.hazeel_cult.*
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.npc.OwnedNPC
import com.rs.game.model.entity.player.Player
import world.gregs.voidps.type.Tile
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onChunkEnter
import com.rs.plugin.kts.onNpcClick
import com.rs.plugin.kts.onNpcDeath
import com.rs.plugin.kts.ownedNpc

private val ALOMONE_SPAWN_TILE = Tile.of(2609, 9670, 0)

@ServerStartupEvent
fun mapAlomone() {
    onNpcClick(Npc.hazeelcult_alomone, Npc.hazeelcult_alomone_combat, options = arrayOf("Talk-to")) { (player, npc) ->
        if (npc !is OwnedNPC || npc.owner !== player) return@onNpcClick
        alomoneDialogue(player, npc)
    }
    onNpcDeath(Npc.hazeelcult_alomone_combat) { event -> handleAlomoneDeath(event.killer) }
    onChunkEnter { (entity) ->
        val player = entity as? Player ?: return@onChunkEnter
        ensureAlomoneSpawned(player)
    }
}

private fun alomoneIdFor(player: Player): Int =
    if (player.isCultSide()) Npc.hazeelcult_alomone else Npc.hazeelcult_alomone_combat

private fun ensureAlomoneSpawned(player: Player) {
    if (!player.withinDistance(ALOMONE_SPAWN_TILE, 14)) return
    val wanted = alomoneIdFor(player)
    val existing = findOwnedAlomone(player)
    if (existing != null) {
        if (existing.id == wanted) return
        existing.finish()
    }
    ownedNpc(player, wanted, ALOMONE_SPAWN_TILE) {
        setRandomWalk(false)
        isIgnoreNPCClipping = true
    }
}

private fun findOwnedAlomone(player: Player): OwnedNPC? =
    World.getNPCsInChunkRange(ALOMONE_SPAWN_TILE.getChunkId(), 2)
        .filterIsInstance<OwnedNPC>()
        .firstOrNull { (it.id == Npc.hazeelcult_alomone || it.id == Npc.hazeelcult_alomone_combat) && it.owner === player && !it.hasFinished() }

private fun alomoneDialogue(player: Player, npc: NPC) {
    val stage = player.questManager.getStage(Quest.HAZEEL_CULT)
    when {
        !player.isCultSide() && stage == STAGE_BRANCH_TASK -> player.startConversation {
            npc(npc, HeadE.ANGRY, "How did YOU get in here?")
            player(HeadE.ANGRY, "I've come for the Carnillean family armour. Hand it over, or face the consequences.")
            npc(npc, HeadE.ANGRY, "I thought I made it clear to the butler you could not be allowed to interfere with our mission.")
            player(HeadE.HAPPY_TALKING, "So, the butler's part of your cult? Thanks for telling me!")
            npc(npc, HeadE.ANGRY, "You won't live long enough to tell anyone! Prepare to die!") { npc.combatTarget = player }
        }
        !player.isCultSide() -> player.startConversation {
            npc(npc, HeadE.ANGRY, "You have crossed my path too many times. Prepare to die!") { npc.combatTarget = player }
        }
        stage == STAGE_ENTER_HIDEOUT -> player.startConversation {
            player(HeadE.CALM_TALK, "Hi there.")
            npc(npc, HeadE.HAPPY_TALKING, "Clivet told me of your desire to join our glorious attempt to revive the mighty Hazeel.")
            npc(npc, HeadE.CALM_TALK, "We require the ancient words that will restore his weakened body. Hazeel hid a powerful magical scroll within his home.")
            npc(npc, HeadE.CALM_TALK, "The words are hidden within the Carnillean mansion. Our agent Jones has been unable to locate the scroll.")
            npc(npc, HeadE.CALM_TALK, "Go back to the mansion and find the enchantment so that we can restore Hazeel!") {
                player.questManager.setStage(Quest.HAZEEL_CULT, STAGE_FIND_SCROLL)
            }
        }
        stage == STAGE_FIND_SCROLL -> player.startConversation {
            player(HeadE.CALM_TALK, "Hello Alomone.")
            npc(npc, HeadE.FRUSTRATED, "Hazeel has waited long enough to return! It is IMPERATIVE you find that spell!")
            player(HeadE.FRUSTRATED, "Yeah, yeah, I'm on it.")
        }
        stage == STAGE_REVIVAL && !player.isQuestComplete(Quest.HAZEEL_CULT) -> player.startConversation {
            npc(npc, HeadE.CALM_TALK, "The ritual was interrupted, but the words of restoration have already been spoken. Let us continue.") {
                hazeelRevivalCutscene(player)
            }
        }
        stage >= STAGE_RETURN_SCROLL && !player.isQuestComplete(Quest.HAZEEL_CULT) -> reviveHazeel(player, npc)
        else -> player.startConversation {
            player(HeadE.CALM_TALK, "Hello again.")
            npc(npc, HeadE.HAPPY_TALKING, "Now Lord Hazeel is returned unto us, we await his strength so that we may smite our enemies!")
        }
    }
}

private fun reviveHazeel(player: Player, npc: NPC) {
    player.startConversation {
        npc(npc, HeadE.CONFUSED, "Have you brought me the scroll of restoration to complete the ritual?")
        if (!player.inventory.containsOneItem(Obj.hazeel_scroll)) {
            player(HeadE.WORRIED, "Nope, sorry.")
            npc(npc, HeadE.ANGRY, "You must find it and bring it to me! Without it we cannot revive Hazeel!")
            return@startConversation
        }
        player(HeadE.HAPPY_TALKING, "Yep. Got it right here.")
        item(Obj.hazeel_scroll, "You show Alomone the scroll of Hazeel.")
        npc(npc, HeadE.HAPPY_TALKING, "Yes...YES! With the words in this scroll, our Lord shall return and bring destruction to our enemies!")
        npc(npc, HeadE.CALM_TALK, "Watch, adventurer, and witness the glorious rebirth of Hazeel!") {
            player.inventory.deleteItem(Obj.hazeel_scroll, 1)
            player.questManager.setStage(Quest.HAZEEL_CULT, STAGE_REVIVAL)
            hazeelRevivalCutscene(player)
        }
    }
}

private fun handleAlomoneDeath(killer: Entity?) {
    val player = killer as? Player ?: return
    val stage = player.questManager.getStage(Quest.HAZEEL_CULT)
    if (player.isCultSide() || stage !in STAGE_BRANCH_TASK..STAGE_BRANCH_PROGRESS) return
    if (player.inventory.containsOneItem(Obj.carnillean_armour) || player.bank.containsItem(Obj.carnillean_armour)) return
    player.inventory.addItemDrop(Obj.carnillean_armour, 1)
    player.questManager.setStage(Quest.HAZEEL_CULT, STAGE_BRANCH_PROGRESS)
    player.itemDialogue(Obj.carnillean_armour, "Alomone drops the Carnillean family armour.")
}
