package com.rs.game.content.quests.regicide.objects

import com.rs.engine.dialogue.HeadE.CALM_TALK
import com.rs.engine.quest.Quest
import org.darkan.core.gameval.gen.Seq
import com.rs.game.World
import com.rs.game.content.quests.regicide.*
import com.rs.game.content.quests.regicide.cutscenes.playIsafdarArrival
import com.rs.game.content.quests.regicide.cutscenes.playArianwynRevelation
import com.rs.game.model.entity.async.schedule
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.npc.OwnedNPC
import com.rs.game.model.entity.player.Player
import com.rs.game.model.gameobject.GameObject
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onChunkEnter
import com.rs.plugin.kts.onLogin
import com.rs.plugin.kts.onNpcDeath
import com.rs.plugin.kts.onObjectClick
import com.rs.plugin.kts.onPlayerStep
import com.rs.plugin.kts.ownedNpc
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import world.gregs.voidps.type.Tile

private val VOYAGE_EAST = Tile.of(2009, 4710, 1)
private val VOYAGE_WEST = Tile.of(2341, 9620, 0)
val ISAFDAR_ENTRY: Tile = Tile.of(2312, 3216, 0)
private val WESTERN_CAVE_ENTRY = Tile.of(2314, 9624, 0)
internal const val TYRAS_GUARD = "regicideTyrasGuard"

@ServerStartupEvent
fun mapRegicideTravel() {
    onObjectClick(Loc.regicide_voyage_temple_well1, Loc.regicide_voyage_temple_well2) { (player, obj) ->
        if (!player.isQuestStarted(Quest.REGICIDE)) {
            player.sendMessage("You must speak to King Lathas and start Regicide before entering Iban's temple again.")
            return@onObjectClick
        }
        player.sendMessage("You climb into the well.")
        player.fadeScreen { player.tele(if (obj.id == Loc.regicide_voyage_temple_well1) VOYAGE_WEST else VOYAGE_EAST) }
    }
    onObjectClick(Loc.loc_4007) { (player) ->
        player.fadeScreen {
            player.resetWalkSteps()
            player.tele(ISAFDAR_ENTRY)
            if (player.getQuestStage(Quest.REGICIDE) == STAGE_ENTER_ISAFDAR)
                player.playIsafdarArrival()
        }
    }
    onObjectClick(Loc.regicide_old_camp_footprints) { (player) ->
        when (player.getQuestStage(Quest.REGICIDE)) {
            in STAGE_UNSTARTED..STAGE_GET_PENDANT -> player.sendMessage("You are unable to make anything of the markings.")
            STAGE_FIND_TRACKS, STAGE_REPORT_TRACKS -> {
                player.sendMessage("You try to follow the foot prints but they lead into impassable woodland.")
                player.advanceRegicide(STAGE_REPORT_TRACKS)
            }
            else -> player.sendMessage("The tracks lead through the dense wood, you can follow them now.")
        }
    }
    onNpcDeath(Npc.regicide_old_camp_guard) { (_, npc) ->
        val guard = npc as? OwnedNPC ?: return@onNpcDeath
        val player = guard.owner
        if (player.tempAttribs.getO<NPC>(TYRAS_GUARD) === npc) {
            player.tempAttribs.removeO<NPC>(TYRAS_GUARD)
            if (player.getQuestStage(Quest.REGICIDE) == STAGE_FOLLOW_TRACKS)
                player.advanceRegicide(STAGE_FIND_TYRAS_CAMP)
        }
    }
    val arrivalTiles = (2310..2314).flatMap { x -> (3212..3217).map { y -> Tile.of(x, y, 0) } }.toTypedArray()
    onPlayerStep(arrivalTiles) { (player) ->
        if (!player.isLocked && player.getQuestStage(Quest.REGICIDE) == STAGE_ENTER_ISAFDAR)
            player.playIsafdarArrival()
    }
    val castleTiles = (2565..2590).flatMap { x -> (3285..3306).map { y -> Tile.of(x, y, 0) } }.toTypedArray()
    onPlayerStep(castleTiles) { (player) -> player.enterRegicideCastleGrounds() }
    onChunkEnter { event -> event.player?.refreshRegicideArea() }
    onLogin { (player) -> player.refreshRegicideArea() }
}

fun enterWesternUndergroundPass(player: Player) {
    if (player.isQuestStarted(Quest.REGICIDE))
        player.useStairs(WESTERN_CAVE_ENTRY)
    else
        player.playerDialogue(CALM_TALK, "I don't think I should go through here. I don't know where I'll end up.")
}

fun Player.regicideDenseForest(obj: GameObject) {
    val tracks = obj.x in 2232..2238 && obj.y == 3148
    val camp = obj.x == 2187 && obj.y in 3163..3169
    val stage = getQuestStage(Quest.REGICIDE)
    if (stage < STAGE_FOLLOW_TRACKS) {
        sendMessage("You can see no way to get past this.")
        return
    }
    if (stage == STAGE_FOLLOW_TRACKS && camp) {
        spawnRegicideGuard()
        npcDialogue(Npc.regicide_old_camp_guard, CALM_TALK, "You'll have to kill me first.")
        return
    }
    if (tracks) {
        if (isLocked)
            return
        val destination = Tile.of(if (x >= 2235) 2230 else 2241, 3150, 0)
        if (!World.floorAndWallsFree(destination, 1)) {
            sendMessage("You cannot find a clear path through the forest from here.")
            return
        }
        forceMove(destination, Seq.regicide_tightfit, 0, 60) {
            resetWalkSteps()
            anim(-1)
            if (getQuestStage(Quest.REGICIDE) == STAGE_FOLLOW_TRACKS && destination.x < 2235)
                spawnRegicideGuard()
        }
        return
    }
    if (isLocked)
        return
    val horizontal = obj.rotation % 2 == 1
    val width = if (horizontal) obj.definitions.sizeY else obj.definitions.sizeX
    val height = if (horizontal) obj.definitions.sizeX else obj.definitions.sizeY
    val destination = if (horizontal)
        Tile.of(if (x < obj.x) obj.x + width else obj.x - 1, y.coerceIn(obj.y, obj.y + height - 1), obj.plane)
    else
        Tile.of(x.coerceIn(obj.x, obj.x + width - 1), if (y < obj.y) obj.y + height else obj.y - 1, obj.plane)
    if (!World.floorAndWallsFree(destination, 1)) {
        sendMessage("You cannot find a clear path through the forest from here.")
        return
    }
    forceMove(destination, Seq.regicide_tightfit, 0, 60) {
        resetWalkSteps()
        anim(-1)
        if (camp && getQuestStage(Quest.REGICIDE) == STAGE_FIND_TYRAS_CAMP && y < 3166)
            advanceRegicide(STAGE_REPORT_CAMP)
    }
    return
}

private fun Player.spawnRegicideGuard() {
    val existing = tempAttribs.getO<OwnedNPC>(TYRAS_GUARD)
    if (existing != null && !existing.hasFinished() && !existing.isDead) {
        existing.forceTalk("I remember you! You're the one who's working with them wicked elves. I will make you pay for that mistake.")
        existing.setCombatTarget(this)
        return
    }
    val spawnTile = getNearestTeleTile(1) ?: return
    val guard = ownedNpc(this, Npc.regicide_old_camp_guard, spawnTile) {
        setRandomWalk(false)
        setCombatTarget(this@spawnRegicideGuard)
        forceTalk("I see you making nice with that elf, traitor!")
    }
    tempAttribs.setO<OwnedNPC>(TYRAS_GUARD, guard)
}

private fun Player.inRegicideCastleGrounds() = plane == 0 && x in 2565..2590 && y in 3285..3306

private fun Player.enterRegicideCastleGrounds() {
    if (!inRegicideCastleGrounds() || getQuestStage(Quest.REGICIDE) != STAGE_RETURN_TO_LATHAS)
        return
    schedule("RegicideCastleRevelation") {
        wait { hasFinished() || !isLocked || !inRegicideCastleGrounds() || getQuestStage(Quest.REGICIDE) != STAGE_RETURN_TO_LATHAS }
        if (!hasFinished() && !isLocked && inRegicideCastleGrounds() && getQuestStage(Quest.REGICIDE) == STAGE_RETURN_TO_LATHAS)
            playArianwynRevelation()
    }
}

private fun Player.refreshRegicideArea() {
    if (!isQuestStarted(Quest.REGICIDE))
        return
    if (plane == 0 && withinDistance(ISAFDAR_ENTRY, 3) && getQuestStage(Quest.REGICIDE) == STAGE_ENTER_ISAFDAR && !isLocked)
        playIsafdarArrival()
    if (plane == 0 && x in 2218..2232 && y in 3144..3160 && getQuestStage(Quest.REGICIDE) == STAGE_FOLLOW_TRACKS && !isLocked)
        spawnRegicideGuard()
    if (plane == 0 && x in 2175..2200 && y in 3137..3158 && getQuestStage(Quest.REGICIDE) == STAGE_FIND_TYRAS_CAMP)
        advanceRegicide(STAGE_REPORT_CAMP)
    enterRegicideCastleGrounds()
    val spawn = when {
        plane == 0 && x in 2180..2220 && y in 3235..3275 -> Npc.npc_1182 to Tile.of(2205, 3259, 0)
        plane == 1 && x in 1995..2025 && y in 4695..4725 -> Npc.caveguide7 to Tile.of(2012, 4712, 1)
        else -> null
    } ?: return
    schedule("RegicideAreaNpc") {
        wait(2)
        if (hasFinished() || !withinDistance(spawn.second, 24))
            return@schedule
        val existing = World.getNPCsInChunkRange(spawn.second.getChunkId(), 2).any {
            it.id == spawn.first && !it.hasFinished() && (it !is OwnedNPC || it.owner === this@refreshRegicideArea)
        }
        if (!existing) {
            val tile = safeRegicideTile(spawn.second) ?: return@schedule
            ownedNpc(this@refreshRegicideArea, spawn.first, tile) {
                setRandomWalk(false)
                setAutoDespawnAtDistance(false)
            }
        }
    }
}

fun safeRegicideTile(preferred: Tile): Tile? {
    if (World.floorAndWallsFree(preferred, 1))
        return preferred
    for (distance in 1..3)
        for (dx in -distance..distance)
            for (dy in -distance..distance) {
                val tile = preferred.transform(dx, dy, 0)
                if (World.floorAndWallsFree(tile, 1))
                    return tile
            }
    return null
}
