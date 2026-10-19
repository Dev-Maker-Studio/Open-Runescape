package com.rs.game.content.holidayevents.halloween.hw10

import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.game.World
import com.rs.game.tasks.WorldTasks
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.managers.EmotesManager.Emote
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onEmote
import com.rs.plugin.kts.onNpcClick
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import world.gregs.voidps.type.Tile
import java.time.LocalDate
import java.util.concurrent.TimeUnit

private class TrickOrTreater(val npc: Int, val tile: Tile, val child: Boolean)

private val TRICK_OR_TREATERS = listOf(
    TrickOrTreater(Npc.vm_school_boy_blonde, Tile.of(3218, 3433, 0), child = true),
    TrickOrTreater(Npc.vm_school_girl_blonde, Tile.of(3216, 3429, 0), child = true),
    TrickOrTreater(Npc.vm_school_boy_brunnete, Tile.of(3219, 3424, 0), child = true),
    TrickOrTreater(Npc.hw11_male_cosplayer_1, Tile.of(3218, 3435, 0), child = false),
    TrickOrTreater(Npc.hw11_female_cosplayer_2, Tile.of(3220, 3430, 0), child = false),
)

private val TREAT_COOLDOWN_MS = TimeUnit.MINUTES.toMillis(15)
private const val DAILY_TREATS = 10
private const val TRICK_RANGE = 3
private const val PUMPKIN_ONE_IN = 4
private const val MAX_SWEETS = 3
private const val LEASH = 2
private const val WANDER_TICKS = 5

private val CHILD_TREAT_LINES = listOf(
    "Ooh, a fellow trick-or-treater! Here, you can have one of mine.",
    "That's the scariest costume I've ever seen! ...Wait, is that just your face?",
    "Treat! Definitely treat. Don't trick me, I've only just got this sheet clean!",
)

private val spawnedTreaters = mutableListOf<NPC>()
private var wanderGeneration = 0

fun spawnTrickOrTreaters() {
    for (treater in TRICK_OR_TREATERS)
        spawnedTreaters += World.spawnNPC(treater.npc, treater.tile, false, true).apply { setRandomWalk(false) }
    val generation = ++wanderGeneration
    WorldTasks.scheduleTimer(WANDER_TICKS, WANDER_TICKS) { _ ->
        if (generation != wanderGeneration || spawnedTreaters.isEmpty())
            return@scheduleTimer false
        spawnedTreaters.forEachIndexed { i, npc ->
            if (npc.hasWalkSteps() || Utils.random(3) != 0)
                return@forEachIndexed
            val home = TRICK_OR_TREATERS[i].tile
            npc.addWalkSteps(home.transform(Utils.randomInclusive(-LEASH, LEASH), Utils.randomInclusive(-LEASH, LEASH), 0), LEASH * 2, true)
        }
        true
    }
}

fun removeTrickOrTreaters() {
    spawnedTreaters.forEach(NPC::finish)
    spawnedTreaters.clear()
}

@ServerStartupEvent
fun mapTrickOrTreaters() {
    onNpcClick(*TRICK_OR_TREATERS.filter { it.child }.map { it.npc }.toTypedArray<Any>(), options = arrayOf("Talk-to")) { (player, npc) ->
        if (npc in spawnedTreaters)
            trickOrTreat(player, npc, tricked = false)
    }

    onEmote(Emote.TRICK) { (player) ->
        if (!halloween2010Open)
            return@onEmote
        val npc = spawnedTreaters.filter { it.tile.withinDistance(player.tile, TRICK_RANGE) }.minByOrNull { Utils.getDistance(it.tile, player.tile) } ?: return@onEmote
        trickOrTreat(player, npc, tricked = true)
    }
}

private fun trickOrTreat(player: Player, npc: NPC, tricked: Boolean) {
    val treater = TRICK_OR_TREATERS[spawnedTreaters.indexOf(npc)]
    val edition = HW10_EDITION.key
    val lastKey = "hw10TreatAt:$edition:${spawnedTreaters.indexOf(npc)}"
    val dayKey = "hw10TreatDay:$edition"
    val countKey = "hw10TreatCount:$edition"
    val now = System.currentTimeMillis()
    val today = LocalDate.now().toEpochDay()
    val treatsToday = if (player.getL(dayKey, -1) == today) player.getL(countKey, 0) else 0
    val happy = if (treater.child) HeadE.CHILD_HAPPY_TALK else HeadE.CHEERFUL
    val cross = if (treater.child) HeadE.CHILD_ANGRY else HeadE.SKEPTICAL
    val opener = if (tricked) "Boo!" else "Trick or treat!"

    if (now - player.getL(lastKey, 0) < TREAT_COOLDOWN_MS) {
        player.startConversation {
            player(HeadE.HAPPY_TALKING, opener)
            npc(npc.id, cross, "You've already had a treat from me! Go haunt someone else for a bit.")
        }
        return
    }
    if (treatsToday >= DAILY_TREATS) {
        player.startConversation {
            player(HeadE.HAPPY_TALKING, opener)
            npc(npc.id, cross, "Greedy ghoul! You've had enough treats for one day. Your teeth will fall out!")
        }
        return
    }

    val pumpkin = Utils.random(PUMPKIN_ONE_IN) == 0
    val obj = if (pumpkin) Obj.pumpkin else Obj.trail_sweets
    val amount = if (pumpkin) 1 else Utils.randomInclusive(1, MAX_SWEETS)
    val hasRoom = player.inventory.freeSlots > 0 || (!pumpkin && player.inventory.containsItem(Obj.trail_sweets))
    if (!hasRoom) {
        player.startConversation {
            player(HeadE.HAPPY_TALKING, opener)
            npc(npc.id, happy, "Your pockets are stuffed! Come back when you've got room for treats.")
        }
        return
    }

    player.inventory.addItem(obj, amount)
    player.set(lastKey, now)
    player.set(dayKey, today)
    player.set(countKey, treatsToday + 1)
    val reply = when {
        !tricked -> CHILD_TREAT_LINES.random()
        treater.child -> "Eek! Okay, okay, you can have a treat! Just stop doing that!"
        else -> "Argh! You got me! Here, take this and leave my costume alone!"
    }
    player.startConversation {
        player(HeadE.HAPPY_TALKING, opener)
        npc(npc.id, if (tricked && treater.child) HeadE.CHILD_SCARED else happy, reply)
        item(obj, if (pumpkin) "You're given a pumpkin." else "You're given some purple sweets.")
    }
}
