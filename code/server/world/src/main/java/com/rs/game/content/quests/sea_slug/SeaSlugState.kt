package com.rs.game.content.quests.sea_slug

import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.gameval.gen.Varp
import com.rs.engine.quest.Quest
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.npc.OwnedNPC
import com.rs.game.model.entity.player.Player
import world.gregs.voidps.type.Tile
import com.rs.plugin.kts.ownedNpc

internal val FISHERMEN = intArrayOf(Npc.fishplatform1_child, Npc.fishplatform2_child, Npc.fishplatform3_child)

private const val SALT_IN_WOUND_STARTED_VALUE = 5
private const val SALT_IN_WOUND_COMPLETE_VALUE = 250

internal val BEACH_ARRIVAL = Tile.of(2721, 3305, 0)
internal val PLATFORM_ARRIVAL = Tile.of(2781, 3274, 0)
internal val ISLAND_ARRIVAL = Tile.of(2801, 3319, 0)
internal val WALL_TILE = Tile.of(2768, 3289, 1)
internal val KENNITH_PLATFORM_TILE = Tile.of(2765, 3288, 1)
internal val HOLGART_PLATFORM_TILE = Tile.of(2781, 3273, 0)
internal val KENNITH_HOME_TILE = Tile.of(2711, 3282, 1)
internal val KENT_HOME_TILE = Tile.of(2730, 3282, 0)

private const val KENNITH_NPC_KEY = "seaSlugKennithNpc"
private const val HOLGART_NPC_KEY = "seaSlugHolgartNpc"
private const val KENT_NPC_KEY = "seaSlugKentNpc"
private const val PLATFORM_LOBSTERS_KEY = "seaSlugPlatformLobsters"

private val GIANT_LOBSTER_SPAWNS = listOf(
    Tile.of(2768, 3286, 0),
    Tile.of(2777, 3278, 0),
    Tile.of(2783, 3289, 0),
    Tile.of(2784, 3277, 0),
    Tile.of(2768, 3289, 1),
    Tile.of(2780, 3288, 1)
)

internal fun Player.isSeaSlugOwnedNpc(npc: NPC): Boolean =
    npc is OwnedNPC && npc.owner === this

internal fun Player.validateSeaSlugNpcOwnership(npc: NPC): Boolean {
    if (npc !is OwnedNPC || npc.owner === this) return true
    sendMessage("That creature belongs to another player's quest instance.")
    return false
}

internal fun Player.refreshSeaSlugState() {
    val stage = getQuestStage(Quest.SEA_SLUG)
    val slugMenaceStarted = isQuestStartedInSave(Quest.SLUG_MENACE, Varbit.slug2_main)
    val saltInWoundStarted = isQuestStartedInSave(
        Quest.SALT_IN_WOUND,
        Varbit.quest_slug3,
        SALT_IN_WOUND_STARTED_VALUE
    )
    val saltInWoundComplete = isQuestCompleteInSave(
        Quest.SALT_IN_WOUND,
        Varbit.quest_slug3,
        SALT_IN_WOUND_COMPLETE_VALUE
    )
    val relatedQuestOwnsTransforms = slugMenaceStarted ||
        isQuestStartedInSave(Quest.KENNITHS_CONCERNS, Varbit.ken_main) ||
        saltInWoundStarted
    val laterQuestOwnsTransport = slugMenaceStarted && !saltInWoundComplete

    if (!relatedQuestOwnsTransforms) {
        val clientStage = when (stage) {
            STAGE_OPENED_WALL -> 9
            STAGE_NEEDS_CRANE -> 10
            STAGE_KENNITH_RESCUED, STAGE_REPORTED_TO_CAROLINE -> 11
            else -> stage.coerceIn(STAGE_UNSTARTED, STAGE_COMPLETE)
        }
        vars.setVar(Varp.varp_159, clientStage)
    }

    refreshOwnedNpc(KENNITH_NPC_KEY, desiredKennith(stage, relatedQuestOwnsTransforms))
    refreshOwnedNpc(HOLGART_NPC_KEY, desiredHolgart(stage, laterQuestOwnsTransport))
    refreshOwnedNpc(KENT_NPC_KEY, desiredKent(stage, relatedQuestOwnsTransforms))
    refreshPlatformLobsters(slugMenaceStarted, saltInWoundComplete)
}

private fun Player.desiredKennith(stage: Int, relatedQuestOwnsTransforms: Boolean): Pair<Int, Tile>? = when {
    isAtFishingPlatform() && stage in STAGE_FIND_HOLGART until STAGE_KENNITH_RESCUED -> Npc.kennith_platform to KENNITH_PLATFORM_TILE
    isAtWitchaven() && stage >= STAGE_REPORTED_TO_CAROLINE && !relatedQuestOwnsTransforms -> Npc.kennith_platform to KENNITH_HOME_TILE
    else -> null
}

private fun Player.desiredHolgart(stage: Int, laterQuestOwnsTransport: Boolean): Pair<Int, Tile>? = when {
    isAtFishingPlatform() && stage >= STAGE_BOAT_REPAIRED && !laterQuestOwnsTransport -> Npc.holgartplatform to HOLGART_PLATFORM_TILE
    else -> null
}

private fun Player.desiredKent(stage: Int, relatedQuestOwnsTransforms: Boolean): Pair<Int, Tile>? = when {
    isAtWitchaven() && stage >= STAGE_REPORTED_TO_CAROLINE && !relatedQuestOwnsTransforms -> Npc.kent to KENT_HOME_TILE
    else -> null
}

private fun Player.refreshOwnedNpc(key: String, desired: Pair<Int, Tile>?) {
    val current = tempAttribs.getO<OwnedNPC>(key)
    if (desired == null) {
        if (current != null && !current.hasFinished()) current.finish()
        tempAttribs.removeO<OwnedNPC>(key)
        return
    }
    if (current != null && !current.hasFinished() && current.id == desired.first && current.tile == desired.second)
        return
    if (current != null && !current.hasFinished()) current.finish()
    tempAttribs.setO<OwnedNPC>(key, ownedNpc(this, desired.first, desired.second) {
        isAutoDespawnAtDistance = false
        setRandomWalk(false)
    })
}

private fun Player.refreshPlatformLobsters(slugMenaceStarted: Boolean, saltInWoundComplete: Boolean) {
    val desired = if (isAtFishingPlatform() && slugMenaceStarted && !saltInWoundComplete)
        GIANT_LOBSTER_SPAWNS
    else
        emptyList()
    val current = tempAttribs.getO<List<OwnedNPC>>(PLATFORM_LOBSTERS_KEY).orEmpty()
    if (current.size == desired.size && current.zip(desired).all { (npc, tile) ->
            !npc.hasFinished() && npc.id == Npc.slug2_giant_lobster_peaceful && npc.tile == tile
        }) return

    current.filterNot { it.hasFinished() }.forEach { it.finish() }
    if (desired.isEmpty()) {
        tempAttribs.removeO<List<OwnedNPC>>(PLATFORM_LOBSTERS_KEY)
        return
    }
    tempAttribs.setO<List<OwnedNPC>>(PLATFORM_LOBSTERS_KEY, desired.map { tile ->
        ownedNpc(this, Npc.slug2_giant_lobster_peaceful, tile) {
            isAutoDespawnAtDistance = false
            setRandomWalk(false)
        }
    })
}

private fun Player.isQuestStartedInSave(quest: Quest, progressVarbit: Int, startedValue: Int = 1): Boolean =
    (quest.isImplemented && isQuestStarted(quest)) || vars.getVarBit(progressVarbit) >= startedValue

private fun Player.isQuestCompleteInSave(quest: Quest, progressVarbit: Int, completeValue: Int): Boolean =
    (quest.isImplemented && isQuestComplete(quest)) || vars.getVarBit(progressVarbit) >= completeValue

internal fun Player.isAtFishingPlatform() = x in 2758..2799 && y in 3260..3295
internal fun Player.isAtKentsIsland() = x in 2788..2807 && y in 3308..3330
internal fun Player.isAtWitchaven() = x in 2695..2740 && y in 3270..3320
